/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsresources.AppendableSequenceAudioInputStream;
import org.jsresources.SequenceAudioInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.LoggingErrorHandler;
import de.dfki.lt.mary.util.MaryNormalisedWriter;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.ReaderSplitter;
import de.dfki.lt.mary.util.UncloseableBufferedReader;

/**
 * A representation of any type of mary data, be it input, intermediate or
 * output data. The "technical" representation of the read data is hidden from
 * the caller, but can be accessed on request.
 * Internally, the data is appropriately represented according to this data's
 * type, i.e. as a String containing plain text, an XML DOM tree, or an
 * input stream containing audio data.
 * @author Marc Schr&ouml;der
 */
public class MaryData {
    private MaryDataType type;
    // Only one of the following data representations should be non-null
    // for a given instance; which one depends on our type.
    private Document xmlDocument = null;
    private String plainText = null;
    private AudioInputStream audio = null;
    private AudioFileFormat audioFileFormat = null;
    private List utterances = null; // List of Utterance objects
    private Logger logger = Logger.getLogger("IO");

    // for plainText, allow additional information:
    private Voice defaultVoice = null;
    private String defaultStyle = "";
    private String defaultEffects = "";

    // The following XML I/O helpers are only initialised
    // if actually needed.
    private MaryNormalisedWriter writer = null;
    private DocumentBuilderFactory factory = null;
    private DocumentBuilder docBuilder = null;
    private StringBuffer buf = null;

    private boolean doValidate;
    private boolean doWarnClient = false;

    public MaryData(MaryDataType type) {
        this(type, false);
    }

    public MaryData(MaryDataType type, boolean createStubDocument) {
        if (type == null)
            throw new NullPointerException("Received null type for MaryData");
        this.type = type;
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
        if (doValidate != this.doValidate) {
            this.doValidate = doValidate;
            if (factory != null) {
                initialiseXMLParser(); // re-initialise
            }
        }
    }

    public boolean getWarnClient() {
        return doWarnClient;
    }
    public void setWarnClient(boolean doWarnClient) {
        if (doWarnClient != this.doWarnClient) {
            this.doWarnClient = doWarnClient;
            if (docBuilder != null) {
                // Following code copied from initialiseXMLParser():
                if (doWarnClient) {
                    // Use custom error handler:
                    docBuilder.setErrorHandler(
                        new LoggingErrorHandler(
                            Thread.currentThread().getName() + " client." + type.name() + " parser"));
                } else {
                    docBuilder.setErrorHandler(null);
                }
            }
        }
    }

    private void initialiseXMLParser() throws ParserConfigurationException {
        factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(true);
        factory.setNamespaceAware(true);
        factory.setValidating(doValidate);
        if (doValidate) {
            factory.setIgnoringElementContentWhitespace(true);
            try {
                factory.setAttribute(
                    "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    "http://www.w3.org/2001/XMLSchema");
                // Specify other factory configuration settings
                factory.setAttribute(
                    "http://java.sun.com/xml/jaxp/properties/schemaSource",
                    MaryProperties.localSchemas());
            } catch (Exception x) {
                // This can happen if the parser does not support JAXP 1.2
                logger.warn("Cannot use Schema validation -- turning off validation.");
                factory.setValidating(false);
            }
        }
        docBuilder = factory.newDocumentBuilder();
        docBuilder.setEntityResolver(new org.xml.sax.EntityResolver() {
            public InputSource resolveEntity (String publicId, String systemId)
            {
                if (systemId.equals("http://mary.dfki.de/lib/Sable.v0_2.dtd")) {
                    try {
                        // return a local copy of the sable dtd:
                        String localSableDTD = MaryProperties.maryBase() +
                            File.separator + "lib" +
                            File.separator + "Sable.v0_2.mary.dtd";
                        return new InputSource(new FileReader(localSableDTD));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local Sable.v0_2.mary.dtd");
                    }
                } else if (systemId.equals("http://mary.dfki.de/lib/sable-latin.ent")) {
                    try {
                        // return a local copy of the sable dtd:
                        String localFilename = MaryProperties.maryBase() +
                            File.separator + "lib" +
                            File.separator + "sable-latin.ent";
                        return new InputSource(new FileReader(localFilename));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local sable-latin.ent");
                    }
                } else if (systemId.equals("http://mary.dfki.de/lib/apml.dtd")
                		|| !systemId.startsWith("http")&&systemId.endsWith("apml.dtd")) {
                    try {
                        // return a local copy of the apml dtd:
                        String localFilename = MaryProperties.maryBase() +
                            File.separator + "lib" +
                            File.separator + "apml.dtd";
                        return new InputSource(new FileReader(localFilename));
                    } catch (FileNotFoundException e) {
                        logger.warn("Cannot find local apml.dtd");
                    }
                }
                // else, use the default behaviour:
                return null;
            }
        });
        if (doWarnClient) {
            // Use custom error handler:
            docBuilder.setErrorHandler(
                new LoggingErrorHandler(Thread.currentThread().getName() + " client." + type.name() + " parser"));
        } else {
            docBuilder.setErrorHandler(null);
        }
    }

    public MaryDataType type() {
        return type;
    }

    /**
     * Read data from input stream <code>is</code>,
     * in the appropriate way as determined by our <code>type</code>.
     */
    public void readFrom(InputStream is)
        throws
            ParserConfigurationException,
            SAXException,
            IOException,
            TransformerConfigurationException,
            TransformerException {
        readFrom(is, null);
    }

    /**
      * Read data from input stream <code>is</code>,
      * in the appropriate way as determined by our <code>type</code>.
     * @param is the InputStream from which to read.
     * @param endMarker a string marking end of file. If this is null, read until
     * end-of-file; if it is non-null, read up to (and including) the first line containing
     * the end marker string. This will be ignored for audio data.
      */
    public void readFrom(InputStream is, String endMarker)
        throws
            ParserConfigurationException,
            SAXException,
            IOException,
            TransformerConfigurationException,
            TransformerException {
        if (type.isUtterances())
            throw new IOException("Cannot read into utterance-based data type!");

        if (type.isXMLType() || type.isTextType())
            readFrom(new InputStreamReader(is, "UTF-8"), endMarker);
        else { // audio
            // ignore endMarker
            setAudio((AudioInputStream) is);
        }
    }

    /**
     * Read data from reader <code>r</code>
     * in the appropriate way as determined by our <code>type</code>.
     * Only XML and Text data can be read from a reader, audio data cannot.
     * "Helpers" needed to read the data, such as XML parser objects,
     * are created when they are needed.
     * If doWarnClient is set to true, warning and error messages related
     * to XML parsing are logged to the log category connected to the client
     * from which this request originated.
     */
    public void readFrom(Reader from)
        throws
            ParserConfigurationException,
            SAXException,
            IOException,
            TransformerConfigurationException,
            TransformerException {
        readFrom(from, null);
    }

    /**
     * Read data from reader <code>r</code>
     * in the appropriate way as determined by our <code>type</code>.
     * Only XML and Text data can be read from a reader, audio data cannot.
     * "Helpers" needed to read the data, such as XML parser objects,
     * are created when they are needed.
     * If doWarnClient is set to true, warning and error messages related
     * to XML parsing are logged to the log category connected to the client
     * from which this request originated.
     * @param from the Reader from which to read.
     * @param endMarker a string marking end of file. If this is null, read until
     * end-of-file; if it is non-null, read up to (and including) the first line containing
     * the end marker string.
     */
    public void readFrom(Reader from, String endMarker)
        throws
            ParserConfigurationException,
            SAXException,
            IOException,
            TransformerConfigurationException,
            TransformerException {
        if (type.isUtterances())
            throw new IOException("Cannot read into utterance-based data type!");

        // For the case that the data to be read it is not
        // followed by end-of-file, we use a ReaderSplitter which
        // provides a reader artificially "inserting" an end-of-file
        // after a line containing the pattern given in endMarker.
        Reader r = from;
        if (endMarker != null) {
            ReaderSplitter fromSplitter = new ReaderSplitter(from, endMarker);
            r = fromSplitter.nextReader();
        }
        if (type.isXMLType()) {
            if (factory == null) {
                initialiseXMLParser();
            }
            // The XML parser closes its input stream when it reads EOF.
            // In the case of a socket, this closes the socket altogether,
            // so we cannot send data back! Therefore, use a subclass of
            // BufferedReader that simply ignores the close() call.
            UncloseableBufferedReader ubr = new UncloseableBufferedReader(r);
            if (doValidate) {
                logger.debug("Reading XML input (validating)...");
            } else {
                logger.debug("Reading XML input (non-validating)...");
            }
            xmlDocument = docBuilder.parse(new InputSource(ubr));
            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                if (writer == null)
                    writer = new MaryNormalisedWriter();
                ByteArrayOutputStream debugOut = new ByteArrayOutputStream();
                writer.output(xmlDocument, debugOut);
                logger.debug("Read XML input:\n" + debugOut.toString());
            }
        } else if (type.isTextType()) {
            // Plain text is read until end-of-file?
            if (buf == null)
                buf = new StringBuffer(1000);
            else
                buf.setLength(0);
            BufferedReader br = new BufferedReader(r);
            String line = null;
            while ((line = br.readLine()) != null) {
                buf.append(line);
                buf.append(System.getProperty("line.separator"));
                logger.debug("Reading text input: " + line);
            }
            plainText = buf.toString();
        } else { // audio -- cannot read this from a reader
            throw new IOException("Illegal attempt to read audio data from a character Reader");
        }
    }

    /**
     * Write our internal representation to output stream <code>os</code>,
     * in the appropriate way as determined by our <code>type</code>.
     */
    public void writeTo(OutputStream os)
        throws TransformerConfigurationException, FileNotFoundException, TransformerException, IOException, Exception {
        if (type.isUtterances())
            throw new IOException("Cannot write out utterance-based data type!");

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
        	logger.debug("Writing audio output, frame length "+audio.getFrameLength());
            AudioSystem.write(audio, audioFileFormat.getType(), os);
            os.flush();
        }
    }
    /**
     * Write our internal representation to writer <code>w</code>,
     * in the appropriate way as determined by our <code>type</code>.
     * Only XML and Text data can be written to a writer, audio data cannot.
     * "Helpers" needed to read the data, such as XML parser objects,
     * are created when they are needed.
     */
    public void writeTo(Writer w)
        throws TransformerConfigurationException, FileNotFoundException, TransformerException, IOException, Exception {
        if (type.isUtterances())
            throw new IOException("Cannot write out utterance-based data type!");
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
        } else if (type.isUtterances()) {
            return utterances;
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
     * Set the audio data. This will discard any previously set audio data. If audio data is to be
     * appended, consider appendAudio().
     * @param audio 
     */
    public void setAudio(AudioInputStream audio) {
        this.audio = audio;
    }

    public List getUtterances() {
        return utterances;
    }

    public void setUtterances(List utterances) {
        this.utterances = utterances;
    }

    public void setDefaultVoice(Voice voice) {
        if (voice == null)
            logger.warn("received null default voice");
        // check that voice locale fits before accepting the voice:
        Locale voiceLocale = null;
        if (voice != null) voiceLocale = voice.getLocale();
        Locale docLocale = type().getLocale();
        if (docLocale == null && type().isXMLType() && getDocument() != null
                && getDocument().getDocumentElement().hasAttribute("xml:lang")) {
            docLocale = MaryUtils.string2locale(getDocument().getDocumentElement().getAttribute("xml:lang"));
        }
        if (docLocale != null && voiceLocale != null &&
                !(MaryUtils.subsumes(docLocale, voiceLocale) || MaryUtils.subsumes(voiceLocale, docLocale))) {
            logger.warn("Voice `"+voice.getName()+"' does not match document locale `"+docLocale+"' -- ignoring!");
            return;
        }
        this.defaultVoice = voice;
    }

    public Voice getDefaultVoice() {
        return defaultVoice;
    }

    public void setDefaultStyle(String style)
    {
        defaultStyle = style;
    }
    
    public String getDefaultStyle()
    {
        return defaultStyle;
    }
    
    public void setDefaultEffects(String effects)
    {
        defaultEffects = effects;
    }
    
    public String getDefaultEffects()
    {
        return defaultEffects;
    }
    
    /**
     * The audio file format is required only for data types serving as input
     * to modules producing AUDIO data (e.g., MBROLA data), as well as for the
     * AUDIO data itself. It should be set by the calling code before passing
     * the data to the module producing AUDIO data.
     */

    public void setAudioFileFormat(AudioFileFormat audioFileFormat) {
        this.audioFileFormat = audioFileFormat;
    }

    public AudioFileFormat getAudioFileFormat() {
        return audioFileFormat;
    }
    
    public void append(MaryData md)
    {
        if (md == null)
            throw new NullPointerException("Received null marydata");
        if (!md.type().equals(this.type()))
            throw new IllegalArgumentException("Cannot append mary data of type `" +
              md.type().name() + "' to mary data of type `" + this.type().name() + "'");
        if (type().isXMLType()) {
            NodeList kids = md.getDocument().getDocumentElement().getChildNodes();
            logger.debug("Appending " + kids.getLength() + " nodes to MaryXML structure");
            Element docEl = this.getDocument().getDocumentElement();
            for (int i=0; i<kids.getLength(); i++) {
                docEl.appendChild(this.getDocument().importNode(kids.item(i), true));
            }
        } else if (type().isTextType()) {
            // Attention: XML type is a text type!
            this.plainText = this.plainText + "\n\n" + md.getPlainText();
        } else if (type().equals(MaryDataType.get("AUDIO"))) {
            appendAudio(md.getAudio());
        } else {
            throw new UnsupportedOperationException("Cannot append two mary data items of type `"
             + type() + "'");
        }
    }

    /**
     * For audio data, append more audio data to the one currently present. If no audio data is set yet,
     * this call is equivalent to setAudio().
     * @param audioToAppend the new audio data to append
     */
    public void appendAudio(AudioInputStream audioToAppend)
    {
        if (this.audio == null) setAudio(audioToAppend);
        else if (this.audio instanceof AppendableSequenceAudioInputStream)
            ((AppendableSequenceAudioInputStream)this.audio).append(audioToAppend);
        else
            this.audio = new SequenceAudioInputStream(this.audio.getFormat(), Arrays.asList(new AudioInputStream[] {this.audio, audioToAppend}));
    }
}
