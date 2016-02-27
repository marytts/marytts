/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.datatypes;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.data.audio.SequenceAudioInputStream;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryNormalisedWriter;
import marytts.util.io.FileUtils;
import marytts.util.io.ReaderSplitter;
import marytts.util.string.StringUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Objects;

/**
 * A representation of any type of mary data, be it input, intermediate or output data. The "technical" representation of the read
 * data is hidden from the caller, but can be accessed on request. Internally, the data is appropriately represented according to
 * this data's type, i.e. as a String containing plain text, an XML DOM tree, or an input stream containing audio data.
 * 
 * @author Marc Schr&ouml;der
 */
public class MaryData {
	private MaryDataType type;
	private Locale locale;
	private String outputParams = null;
	// Only one of the following data representations should be non-null
	// for a given instance; which one depends on our type.
	private Document xmlDocument = null;
	private String plainText = null;
	private AudioInputStream audio = null;
	private AudioFileFormat audioFileFormat = null;
	private Logger logger = MaryUtils.getLogger("IO");

	// for plainText, allow additional information:
	private Voice defaultVoice = null;
	private String defaultStyle = "";
	private String defaultEffects = "";

	// The following XML I/O helpers are only initialised
	// if actually needed.
	private MaryNormalisedWriter writer = null;

	private boolean doValidate;
	private boolean doWarnClient = false;

	public MaryData(MaryDataType type, Locale locale) {
		this(type, locale, false);
	}

	public MaryData(MaryDataType type, Locale locale, boolean createStubDocument) {
		if (type == null)
			throw new NullPointerException("Received null type for MaryData");
		this.type = type;
		this.locale = locale;
		// The following is the default setting for module output (we suppose
		// that for the input data, setValidating() is called as appropriate):
		doValidate = MaryProperties.getBoolean("maryxml.validate.modules", false);
		if (createStubDocument && type.isMaryXML()) {
			xmlDocument = MaryXML.newDocument();
		}
	}

	public boolean getValidating() {
		return doValidate;
	}

	public void setValidating(boolean doValidate) throws ParserConfigurationException {
		this.doValidate = doValidate;
	}

	@Deprecated
	public boolean getWarnClient() {
		return doWarnClient;
	}

	@Deprecated
	public void setWarnClient(boolean doWarnClient) {
	}

	public MaryDataType getType() {
		return type;
	}

	public Locale getLocale() {
		return locale;
	}

	/**
	 * Read data from input stream <code>is</code>, in the appropriate way as determined by our <code>type</code>.
	 * 
	 * @param is
	 *            is
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws TransformerException
	 *             TransformerException
	 */
	public void readFrom(InputStream is) throws ParserConfigurationException, SAXException, IOException,
			TransformerConfigurationException, TransformerException {
		readFrom(is, null);
	}

	/**
	 * Read data from input stream <code>is</code>, in the appropriate way as determined by our <code>type</code>.
	 * 
	 * @param is
	 *            the InputStream from which to read.
	 * @param endMarker
	 *            a string marking end of file. If this is null, read until end-of-file; if it is non-null, read up to (and
	 *            including) the first line containing the end marker string. This will be ignored for audio data.
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws TransformerException
	 *             TransformerException
	 */
	public void readFrom(InputStream is, String endMarker) throws ParserConfigurationException, SAXException, IOException,
			TransformerConfigurationException, TransformerException {
		if (type.isXMLType() || type.isTextType())
			readFrom(new InputStreamReader(is, "UTF-8"), endMarker);
		else { // audio
				// ignore endMarker
			setAudio((AudioInputStream) is);
		}
	}

	/**
	 * Read data from reader <code>r</code> in the appropriate way as determined by our <code>type</code>. Only XML and Text data
	 * can be read from a reader, audio data cannot.
	 * 
	 * @param from
	 *            from
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public void readFrom(Reader from) throws ParserConfigurationException, SAXException, IOException {
		String inputData = FileUtils.getReaderAsString(from);
		setData(inputData);
	}

	/**
	 * Read data from reader <code>r</code> in the appropriate way as determined by our <code>type</code>. Only XML and Text data
	 * can be read from a reader, audio data cannot. "Helpers" needed to read the data, such as XML parser objects, are created
	 * when they are needed. If doWarnClient is set to true, warning and error messages related to XML parsing are logged to the
	 * log category connected to the client from which this request originated.
	 * 
	 * @param from
	 *            the Reader from which to read.
	 * @param endMarker
	 *            a string marking end of file. If this is null, read until end-of-file; if it is non-null, read up to (and
	 *            including) the first line containing the end marker string.
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws SAXException
	 *             SAXException
	 * @throws IOException
	 *             IOException
	 */
	public void readFrom(Reader from, String endMarker) throws ParserConfigurationException, SAXException, IOException {
		// For the case that the data to be read it is not
		// followed by end-of-file, we use a ReaderSplitter which
		// provides a reader artificially "inserting" an end-of-file
		// after a line containing the pattern given in endMarker.
		Reader r = from;
		if (endMarker != null) {
			ReaderSplitter fromSplitter = new ReaderSplitter(from, endMarker);
			r = fromSplitter.nextReader();
		}
		readFrom(r);

	}

	/**
	 * Set the content data of this MaryData object from the given String. For XML data ({@link MaryDataType#isXMLType()}), parse
	 * the String representation of the data into a DOM tree.
	 * 
	 * @param dataString
	 *            string representation of the input data.
	 * @throws ParserConfigurationException
	 *             ParserConfigurationException
	 * @throws IOException
	 *             IOException
	 * @throws SAXException
	 *             SAXException
	 * @throws IllegalArgumentException
	 *             if this method is called for MaryDataTypes that are neither text nor XML.
	 */
	public void setData(String dataString) throws ParserConfigurationException, SAXException, IOException {
		// First, some data cleanup:
		dataString = StringUtils.purgeNonBreakingSpaces(dataString);
		// Now, deal with it.
		if (type.isXMLType()) {
			logger.debug("Parsing XML input (" + (doValidate ? "" : "non-") + "validating): " + dataString);
			xmlDocument = DomUtils.parseDocument(dataString, doValidate);
		} else if (type.isTextType()) {
			logger.debug("Setting text input: " + dataString);
			plainText = dataString;
		} else {
			throw new IllegalArgumentException("Cannot set data of type " + type + " from a string");
		}
	}

	/**
	 * Write our internal representation to output stream <code>os</code>, in the appropriate way as determined by our
	 * <code>type</code>.
	 * 
	 * @param os
	 *            os
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @throws TransformerException
	 *             TransformerException
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	public void writeTo(OutputStream os) throws TransformerConfigurationException, FileNotFoundException, TransformerException,
			IOException, Exception {
		if (type.isXMLType()) {
			if (writer == null)
				writer = new MaryNormalisedWriter();
			if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
				ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
				writer.output(xmlDocument, debugOut);
				logger.debug(debugOut.toString());
			}
			writer.output(xmlDocument, new BufferedOutputStream(os));
		} else if (type.isTextType()) { // caution: XML types are text types!
			writeTo(new OutputStreamWriter(os, "UTF-8"));
		} else { // audio
			logger.debug("Writing audio output, frame length " + audio.getFrameLength());
			AudioSystem.write(audio, audioFileFormat.getType(), os);
			os.flush();
			os.close();
		}
	}

	/*
	 * public void writeTo(HttpResponse response) throws TransformerConfigurationException, FileNotFoundException,
	 * TransformerException, IOException, Exception { if (type.isUtterances()) throw new
	 * IOException("Cannot write out utterance-based data type!");
	 * 
	 * if (type.isXMLType()) { if (writer == null) writer = new MaryNormalisedWriter(); if
	 * (logger.getEffectiveLevel().equals(Level.DEBUG)) { ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
	 * writer.output(xmlDocument, debugOut); logger.debug(debugOut.toString()); }
	 * 
	 * //writer.output(xmlDocument, new BufferedOutputStream(os));
	 * 
	 * ByteArrayOutputStream os = new ByteArrayOutputStream(); writer.output(xmlDocument, new BufferedOutputStream(os));
	 * NByteArrayEntity body = new NByteArrayEntity(os.toByteArray()); body.setContentType("text/html; charset=UTF-8");
	 * response.setEntity(body); } else if (type.isTextType()) // caution: XML types are text types! { //writeTo(new
	 * OutputStreamWriter(os, "UTF-8"));
	 * 
	 * ByteArrayOutputStream os = new ByteArrayOutputStream(); writeTo(new OutputStreamWriter(os, "UTF-8")); NByteArrayEntity body
	 * = new NByteArrayEntity(os.toByteArray()); body.setContentType("text/html; charset=UTF-8"); response.setEntity(body); } else
	 * // audio { logger.debug("Writing audio output, frame length "+audio.getFrameLength()); //AudioSystem.write(audio,
	 * audioFileFormat.getType(), os); //os.flush();
	 * 
	 * ByteArrayOutputStream os = new ByteArrayOutputStream(); AudioSystem.write(audio, audioFileFormat.getType(), os);
	 * os.flush();
	 * 
	 * MaryHttpServerUtils.toHttpResponse(os.toByteArray(), response); } }
	 */

	/**
	 * Write our internal representation to writer <code>w</code>, in the appropriate way as determined by our <code>type</code>.
	 * Only XML and Text data can be written to a writer, audio data cannot. "Helpers" needed to read the data, such as XML parser
	 * objects, are created when they are needed.
	 * 
	 * @param w
	 *            w
	 * @throws TransformerConfigurationException
	 *             TransformerConfigurationException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 * @throws TransformerException
	 *             TransformerException
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	public void writeTo(Writer w) throws TransformerConfigurationException, FileNotFoundException, TransformerException,
			IOException, Exception {
		if (type.isXMLType()) {
			throw new IOException("Better write XML data to an OutputStream, not to a Writer");
		} else if (type.isTextType()) { // caution: XML types are text types!
			w.write(plainText);
			w.flush();
			logger.debug("Writing Text output:\n" + plainText);
		} else { // audio - cannot write this to a writer
			throw new Exception("Illegal attempt to write audio data to a character Writer");
		}
	}

	public Object getData() {
		if (type.isXMLType()) {
			return xmlDocument;
		} else if (type.isTextType()) {
			return plainText;
		} else { // audio
			return audio;
		}
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		this.plainText = plainText;
	}

	public Document getDocument() {
		return xmlDocument;
	}

	public void setDocument(Document xmlDocument) {
		this.xmlDocument = xmlDocument;
	}

	public AudioInputStream getAudio() {
		return audio;
	}

	/**
	 * Set the audio data. This will discard any previously set audio data. If audio data is to be appended, consider
	 * appendAudio().
	 * 
	 * @param audio
	 *            audio
	 */
	public void setAudio(AudioInputStream audio) {
		this.audio = audio;
	}

	public void setDefaultVoice(Voice voice) {
		if (voice == null) {
			return;
		}
		// check that voice locale fits before accepting the voice:
		Locale voiceLocale = null;
		voiceLocale = voice.getLocale();
		Locale docLocale = getLocale();
		if (docLocale == null && getType().isXMLType() && getDocument() != null
				&& getDocument().getDocumentElement().hasAttribute("xml:lang")) {
			docLocale = MaryUtils.string2locale(getDocument().getDocumentElement().getAttribute("xml:lang"));
		}
		if (docLocale != null && voiceLocale != null
				&& !(MaryUtils.subsumes(docLocale, voiceLocale) || MaryUtils.subsumes(voiceLocale, docLocale))) {
			logger.warn("Voice `" + voice.getName() + "' does not match document locale `" + docLocale + "' -- ignoring!");
		}
		this.defaultVoice = voice;
	}

	public Voice getDefaultVoice() {
		return defaultVoice;
	}

	public void setDefaultStyle(String style) {
		defaultStyle = style;
	}

	public String getDefaultStyle() {
		return defaultStyle;
	}

	public void setDefaultEffects(String effects) {
		defaultEffects = effects;
	}

	public String getDefaultEffects() {
		return defaultEffects;
	}

	/**
	 * The audio file format is required only for data types serving as input to modules producing AUDIO data (e.g., MBROLA data),
	 * as well as for the AUDIO data itself. It should be set by the calling code before passing the data to the module producing
	 * AUDIO data.
	 * 
	 * @param audioFileFormat
	 *            audioFileFormat
	 */

	public void setAudioFileFormat(AudioFileFormat audioFileFormat) {
		this.audioFileFormat = audioFileFormat;
	}

	public AudioFileFormat getAudioFileFormat() {
		return audioFileFormat;
	}

	public void append(MaryData md) {
		if (md == null)
			throw new NullPointerException("Received null marydata");
		if (!md.getType().equals(this.getType()))
			throw new IllegalArgumentException("Cannot append mary data of type `" + md.getType().name()
					+ "' to mary data of type `" + this.getType().name() + "'");
		if (getType().isXMLType()) {
			NodeList kids = md.getDocument().getDocumentElement().getChildNodes();
			logger.debug("Appending " + kids.getLength() + " nodes to MaryXML structure");
			Element docEl = this.getDocument().getDocumentElement();
			for (int i = 0; i < kids.getLength(); i++) {
				docEl.appendChild(this.getDocument().importNode(kids.item(i), true));
			}
		} else if (getType().isTextType()) {
			// Attention: XML type is a text type!
			if (this.plainText == null) {
				this.plainText = md.getPlainText();
			} else {
				this.plainText = this.plainText + "\n\n" + md.getPlainText();
			}
		} else if (getType().equals(MaryDataType.get("AUDIO"))) {
			appendAudio(md.getAudio());
		} else {
			throw new UnsupportedOperationException("Cannot append two mary data items of type `" + getType() + "'");
		}
	}

	/**
	 * For audio data, append more audio data to the one currently present. If no audio data is set yet, this call is equivalent
	 * to setAudio().
	 * 
	 * @param audioToAppend
	 *            the new audio data to append
	 */
	public void appendAudio(AudioInputStream audioToAppend) {
		if (this.audio == null)
			setAudio(audioToAppend);
		else if (this.audio instanceof AppendableSequenceAudioInputStream)
			((AppendableSequenceAudioInputStream) this.audio).append(audioToAppend);
		else
			this.audio = new SequenceAudioInputStream(this.audio.getFormat(), Arrays.asList(new AudioInputStream[] { this.audio,
					audioToAppend }));
	}

	public void setOutputParams(String params) {
		this.outputParams = params;
	}

	public String getOutputParams() {
		return outputParams;
	}

	public String toString() {
		return Objects.toStringHelper(this).add("type", getType()).add("locale", getLocale())
				.add("output parameters", getOutputParams()).add("data", getData())
				.add("document", DomUtils.serializeToString(getDocument())).add("validating", getValidating())
				.add("plain text", getPlainText()).add("audio", getAudio()).add("audio file format", getAudioFileFormat())
				.toString();
	}
}
