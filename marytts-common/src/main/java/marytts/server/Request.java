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
package marytts.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryCache;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.io.FileUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A request consists of input data, a desired output data type and the means to process the input data into the data of the
 * output type.<br>
 * <br>
 * A request is used as follows. First, its basic properties are set in the constructor, such as its input and output types.
 * Second, the input data is provided to the request either by directly setting it (<code>setInputData()</code>) or by reading it
 * from a Reader (<code>readInputData()</code>). Third, the request is processed (<code>process()</code>). Finally, the output
 * data is either accessed directly (<code>getOutputData()</code>) or written to an output stream (<code>writeOutputData</code>).
 */
public class Request {
	protected MaryDataType inputType;
	protected MaryDataType outputType;
	protected String outputTypeParams;
	protected AudioFileFormat audioFileFormat;
	protected AppendableSequenceAudioInputStream appendableAudioStream;
	protected Locale defaultLocale;
	protected Voice defaultVoice;
	protected String defaultStyle;
	protected String defaultEffects;

	protected int id;
	protected Logger logger;
	protected MaryData inputData;
	protected MaryData outputData;
	protected boolean streamAudio = false;;
	protected boolean abortRequested = false;

	// Keep track of timing info for each module
	// (map MaryModule onto Long)
	protected Set<MaryModule> usedModules;
	protected Map<MaryModule, Long> timingInfo;

	public Request(MaryDataType inputType, MaryDataType outputType, Locale defaultLocale, Voice defaultVoice,
			String defaultEffects, String defaultStyle, int id, AudioFileFormat audioFileFormat) {
		this(inputType, outputType, defaultLocale, defaultVoice, defaultEffects, defaultStyle, id, audioFileFormat, false, null);
	}

	public Request(MaryDataType inputType, MaryDataType outputType, Locale defaultLocale, Voice defaultVoice,
			String defaultEffects, String defaultStyle, int id, AudioFileFormat audioFileFormat, boolean streamAudio,
			String outputTypeParams) {
		if (!inputType.isInputType())
			throw new IllegalArgumentException("not an input type: " + inputType.name());
		if (!outputType.isOutputType())
			throw new IllegalArgumentException("not an output type: " + outputType.name());
		this.inputType = inputType;
		this.outputType = outputType;
		this.defaultLocale = defaultLocale;
		this.defaultVoice = defaultVoice;
		this.defaultEffects = defaultEffects;
		this.defaultStyle = defaultStyle;
		this.id = id;
		this.audioFileFormat = audioFileFormat;
		this.streamAudio = streamAudio;
		if (outputType == MaryDataType.get("AUDIO")) {
			if (audioFileFormat == null)
				throw new NullPointerException("audio file format is needed for output type AUDIO");
			this.appendableAudioStream = new AppendableSequenceAudioInputStream(audioFileFormat.getFormat(), null);
		} else {
			this.appendableAudioStream = null;
		}
		this.logger = MaryUtils.getLogger("R " + id);
		this.outputTypeParams = outputTypeParams;
		this.inputData = null;
		this.outputData = null;
		StringBuilder info = new StringBuilder("New request (input type \"" + inputType.name() + "\", output type \""
				+ outputType.name());
		if (this.defaultVoice != null)
			info.append("\", voice \"" + this.defaultVoice.getName());
		if (this.defaultEffects != null && this.defaultEffects != "")
			info.append("\", effect \"" + this.defaultEffects);
		if (this.defaultStyle != null && this.defaultStyle != "")
			info.append("\", style \"" + this.defaultStyle);
		if (audioFileFormat != null)
			info.append("\", audio \"" + audioFileFormat.getType().toString() + "\"");
		if (streamAudio)
			info.append(", streaming");
		info.append(")");
		logger.info(info.toString());

		// Keep track of timing info for each module
		// (map MaryModule onto Long)
		usedModules = new LinkedHashSet<MaryModule>();
		timingInfo = new HashMap<MaryModule, Long>();
	}

	public MaryDataType getInputType() {
		return inputType;
	}

	public MaryDataType getOutputType() {
		return outputType;
	}

	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public Voice getDefaultVoice() {
		return defaultVoice;
	}

	public String getDefaultStyle() {
		return defaultStyle;
	}

	public String getDefaultEffects() {
		return defaultEffects;
	}

	public int getId() {
		return id;
	}

	public AudioFileFormat getAudioFileFormat() {
		return audioFileFormat;
	}

	public AppendableSequenceAudioInputStream getAudio() {
		return appendableAudioStream;
	}

	public boolean getStreamAudio() {
		return streamAudio;
	}

	/**
	 * Inform this request that any further processing does not make sense.
	 */
	public void abort() {
		logger.info("Requesting abort.");
		abortRequested = true;
	}

	/**
	 * Set the input data directly, in case it is already in the form of a MaryData object.
	 * 
	 * @param inputData
	 *            inputData
	 */
	public void setInputData(MaryData inputData) {
		if (inputData != null && inputData.getType() != inputType) {
			throw new IllegalArgumentException("Input data has wrong data type (expected " + inputType.toString() + ", got "
					+ inputData.getType().toString());
		}
		if (defaultVoice == null) {
			defaultVoice = Voice.getSuitableVoice(inputData);
		}
		// assert defaultVoice != null;

		if (inputData.getDefaultVoice() == null) {
			inputData.setDefaultVoice(defaultVoice);
		}
		inputData.setDefaultStyle(defaultStyle);
		inputData.setDefaultEffects(defaultEffects);

		this.inputData = inputData;
	}

	/**
	 * Read the input data from a Reader.
	 * 
	 * @param inputReader
	 *            inputReader
	 * @throws Exception
	 *             Exception
	 */
	public void readInputData(Reader inputReader) throws Exception {
		String inputText = FileUtils.getReaderAsString(inputReader);
		setInputData(inputText);
	}

	public void setInputData(String inputText) throws Exception {
		inputData = new MaryData(inputType, defaultLocale);
		inputData.setWarnClient(true); // log warnings to client
		// For RAWMARYXML, a validating parse is not possible
		// because RAWMARYXML is not tokenised, so it does not yet
		// fulfill the MaryXML Schema constraints.
		if (inputType == MaryDataType.get("RAWMARYXML")) {
			inputData.setValidating(false);
		} else if (inputType.isMaryXML()) {
			inputData.setValidating(MaryProperties.getBoolean("maryxml.validate.input"));
		}
		inputData.setData(inputText);
		if (defaultVoice == null) {
			defaultVoice = Voice.getSuitableVoice(inputData);
		}
		// assert defaultVoice != null;
		inputData.setDefaultVoice(defaultVoice);
		inputData.setDefaultStyle(defaultStyle);
		inputData.setDefaultEffects(defaultEffects);
	}

	/**
	 * Process the input data to produce the output data.
	 * 
	 * @throws Exception
	 *             Exception
	 * @see #getOutputData for direct access to the resulting output data
	 * @see #writeOutputData for writing the output data to a stream
	 */
	public void process() throws Exception {
		assert Mary.currentState() == Mary.STATE_RUNNING;
		long startTime = System.currentTimeMillis();
		if (inputData == null)
			throw new NullPointerException("Input data is not set.");
		if (inputType.isXMLType() && inputData.getDocument() == null)
			throw new NullPointerException("Input data contains no XML document.");
		if (inputType.isMaryXML() && !inputData.getDocument().getDocumentElement().hasAttribute("xml:lang"))
			throw new IllegalArgumentException("Mandatory attribute xml:lang is missing from maryxml document element.");

		NodeList inputDataList;
		MaryData rawmaryxml;
		// Is inputdata of a type that must be converted to RAWMARYXML?
		if (outputType.name().equals("PRAAT_TEXTGRID")) { // never chunk for PRAAT_TEXTGRID
			outputData = processOrLookupOneChunk(inputData, outputType, outputTypeParams);
			return;
		} else if (inputType.isTextType() && inputType.name().startsWith("TEXT") || inputType.isXMLType()
				&& !inputType.isMaryXML()) {
			// Convert to RAWMARYXML
			rawmaryxml = processOrLookupOneChunk(inputData, MaryDataType.get("RAWMARYXML"), null);

			// assert rawmaryxml.getDefaultVoice() != null;
			inputDataList = splitIntoChunks(rawmaryxml);
		} else if (inputType.equals(MaryDataType.get("RAWMARYXML"))) {
			rawmaryxml = inputData;
			inputDataList = splitIntoChunks(inputData);
		} else {
			// other input data types are processed as a whole
			outputData = processOrLookupOneChunk(inputData, outputType, outputTypeParams);
			// assert outputData.getDefaultVoice() != null;
			if (outputType == MaryDataType.AUDIO) {
				assert appendableAudioStream != null;
				appendableAudioStream.append(outputData.getAudio());
				appendableAudioStream.doneAppending();
			}
			return;
		}
		assert rawmaryxml != null && rawmaryxml.getType().equals(MaryDataType.get("RAWMARYXML"))
				&& rawmaryxml.getDocument() != null;
		moveBoundariesIntoParagraphs(rawmaryxml.getDocument());

		// Now the beyond-RAWMARYXML processing:
		outputData = new MaryData(outputType, defaultLocale);
		outputData.setDefaultVoice(defaultVoice);
		outputData.setDefaultStyle(defaultStyle);
		outputData.setDefaultEffects(defaultEffects);
		if (outputType.isMaryXML()) {
			// use the input or intermediate MaryXML document
			// as the starting point for MaryXML output types,
			// in order to gradually enrich them:
			outputData.setDocument(rawmaryxml.getDocument());
		} else if (outputType.equals(MaryDataType.get("AUDIO"))) {
			outputData.setAudio(appendableAudioStream);
			outputData.setAudioFileFormat(audioFileFormat);
		}
		int len = inputDataList.getLength();
		for (int i = 0; i < len && !abortRequested; i++) {
			Element currentInputParagraph = (Element) inputDataList.item(i);
			assert currentInputParagraph.getTagName().equals(MaryXML.PARAGRAPH);
			NodeList outputNodeList = null;
			// Only process paragraph if there is any text below it:
			if (MaryDomUtils.getPlainTextBelow(currentInputParagraph).trim().equals("")) {
				outputNodeList = currentInputParagraph.getChildNodes();
			} else { // process "real" data:
				MaryData oneInputData = extractParagraphAsMaryData(rawmaryxml, currentInputParagraph);
				// assert oneInputData.getDefaultVoice() != null;
				MaryData oneOutputData = processOrLookupOneChunk(oneInputData, outputType, outputTypeParams);
				// assert oneOutputData.getDefaultVoice() != null;
				if (outputType.isMaryXML()) {
					NodeList outParagraphList = oneOutputData.getDocument().getDocumentElement()
							.getElementsByTagName(MaryXML.PARAGRAPH);
					// This does not hold for Tibetan:
					// assert outParagraphList.getLength() == 1;
					outputNodeList = outParagraphList;
				} else { // output is not MaryXML, e.g. text or audio
					assert outputData != null;
					outputData.append(oneOutputData);
				}
			}
			if (outputType.isMaryXML()) {
				assert outputNodeList != null;
				// And now replace the paragraph in-place:
				MaryDomUtils.replaceElement(currentInputParagraph, outputNodeList);
			}
		}
		long stopTime = System.currentTimeMillis();
		logger.info("Request processed in " + (stopTime - startTime) + " ms.");
		for (MaryModule m : usedModules) {
			logger.info("   " + m.name() + " took " + timingInfo.get(m) + " ms");
		}
		if (appendableAudioStream != null)
			appendableAudioStream.doneAppending();
	}

	/**
	 * Convert the given data into the requested output type, either by looking it up in the cache or by actually processing it.
	 * 
	 * @param oneInputData
	 *            the input data to convert
	 * @param oneOutputType
	 *            the output type to convert to
	 * @param outputParams
	 *            the output parameters
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
	private MaryData processOrLookupOneChunk(MaryData oneInputData, MaryDataType oneOutputType, String outputParams)
			throws TransformerConfigurationException, FileNotFoundException, TransformerException, IOException, Exception {
		if (logger.getEffectiveLevel().equals(Level.DEBUG)
				&& (oneInputData.getType().isTextType() || oneInputData.getType().isXMLType())) {
			logger.debug("Now converting the following input data from " + oneInputData.getType() + " to " + oneOutputType + ":");
			ByteArrayOutputStream dummy = new ByteArrayOutputStream();
			oneInputData.writeTo(dummy);
			// side effect: writeTo() writes to log if debug
		}
		Locale locale = determineLocale(oneInputData);
		assert locale != null;

		MaryCache cache = null;
		if (MaryProperties.getBoolean("cache")) {
			cache = MaryCache.getCache();
		}

		if (cache == null) {
			return processOneChunk(oneInputData, oneOutputType, outputParams, locale);
		}

		String inputtype = null;
		String outputtype = null;
		String localeString = null;
		String voice = null;
		String inputtext = null;

		// try to look up the requested result in the cache:
		inputtype = oneInputData.getType().name();
		outputtype = oneOutputType.name();
		ByteArrayOutputStream sw = new ByteArrayOutputStream();
		oneInputData.writeTo(sw);
		inputtext = new String(sw.toByteArray(), "UTF-8");
		voice = defaultVoice != null ? defaultVoice.getName() : null;
		localeString = locale.toString();

		if (oneOutputType.isTextType()) {
			try {
				String outputtext = cache.lookupText(inputtype, outputtype, localeString, voice, outputParams, defaultStyle,
						defaultEffects, inputtext);
				if (outputtext != null) {
					MaryData outData = new MaryData(oneOutputType, locale);
					ByteArrayInputStream sr = new ByteArrayInputStream(outputtext.getBytes());
					outData.readFrom(sr);
					sr.close();
					outData.setDefaultVoice(defaultVoice);
					outData.setDefaultStyle(defaultStyle);
					outData.setDefaultEffects(defaultEffects);
					return outData;
				}
			} catch (Exception e) {
				logger.warn("Problem looking up text in cache", e);
			}
		} else if (outputtype.equals("AUDIO")) {
			try {
				byte[] wavFileData = cache.lookupAudio(inputtype, localeString, voice, outputParams, defaultStyle,
						defaultEffects, inputtext);
				if (wavFileData != null) {
					AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavFileData));
					MaryData outData = new MaryData(oneOutputType, locale);
					outData.setAudio(ais);
					outData.setAudioFileFormat(audioFileFormat);
					return outData;
				}
			} catch (Exception e) {
				logger.warn("Problem looking up audio in cache", e);
			}
		} else {
			// logger.debug("Don't know how to cache data of type '"+outputtype+"'");
		}

		// Couldn't get it from cache, need to process
		if (oneOutputType.equals(MaryDataType.AUDIO) || oneOutputType.equals(MaryDataType.REALISED_ACOUSTPARAMS)
				|| oneOutputType.equals(MaryDataType.REALISED_DURATIONS)) {
			// Special case: when we generate AUDIO, we also remember REALISED_ACOUSTPARAMS and REALISED_DURATIONS formats and
			// vice versa
			MaryData audioData = processOneChunk(oneInputData, MaryDataType.AUDIO, outputParams, locale);
			MaryData realisedAcoustparams = processOneChunk(audioData, MaryDataType.REALISED_ACOUSTPARAMS, outputParams, locale);
			MaryData realisedDurations = processOneChunk(audioData, MaryDataType.REALISED_DURATIONS, outputParams, locale);
			insertAudioIntoCache(cache, inputtype, localeString, voice, outputParams, inputtext, audioData);
			insertTextIntoCache(cache, inputtype, MaryDataType.REALISED_ACOUSTPARAMS.name(), localeString, voice, outputParams,
					inputtext, realisedAcoustparams);
			insertTextIntoCache(cache, inputtype, MaryDataType.REALISED_DURATIONS.name(), localeString, voice, outputParams,
					inputtext, realisedDurations);
			if (oneOutputType.equals(MaryDataType.AUDIO))
				return audioData;
			else if (oneOutputType.equals(MaryDataType.REALISED_ACOUSTPARAMS))
				return realisedAcoustparams;
			return realisedDurations;
		} else { // simple, straightforward processing of output
			MaryData oneOutputData = processOneChunk(oneInputData, oneOutputType, outputParams, locale);
			// Remember the processing result in the cache
			if (oneOutputType.isTextType()) {
				insertTextIntoCache(cache, inputtype, outputtype, localeString, voice, outputParams, inputtext, oneOutputData);
			} else {
				logger.debug("Don't know how to cache data of type '" + outputtype + "'");
			}
			return oneOutputData;
		}
	}

	private void insertAudioIntoCache(MaryCache cache, String inputtype, String localeString, String voice, String outputParams,
			String inputtext, MaryData currentData) throws IOException, SQLException, UnsupportedAudioFileException {
		AppendableSequenceAudioInputStream as = (AppendableSequenceAudioInputStream) currentData.getAudio();
		assert as != appendableAudioStream;
		as.doneAppending();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2 * (int) as.getFrameLength() + 100);
		AudioSystem.write(as, AudioFileFormat.Type.WAVE, baos);
		byte[] wavFileData = baos.toByteArray();
		cache.insertAudio(inputtype, localeString, voice, outputParams, defaultStyle, defaultEffects, inputtext, wavFileData);
		AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavFileData));
		currentData.setAudio(ais);
	}

	private void insertTextIntoCache(MaryCache cache, String inputtype, String outputtype, String localeString, String voice,
			String outputParams, String inputtext, MaryData currentData) {
		try {
			ByteArrayOutputStream sw = new ByteArrayOutputStream();
			currentData.writeTo(sw);
			String outputtext = new String(sw.toByteArray(), "UTF-8");
			cache.insertText(inputtype, outputtype, localeString, voice, outputParams, defaultStyle, defaultEffects, inputtext,
					outputtext);
		} catch (Exception e) {
			logger.warn("Problem inserting text into cache", e);
		}
	}

	private MaryData processOneChunk(MaryData oneInputData, MaryDataType oneOutputType, String outputParams, Locale locale)
			throws Exception, TransformerConfigurationException, FileNotFoundException, TransformerException, IOException {
		logger.debug("Determining which modules to use");
		List<MaryModule> neededModules = ModuleRegistry.modulesRequiredForProcessing(oneInputData.getType(), oneOutputType,
				locale, oneInputData.getDefaultVoice());
		// Now neededModules contains references to the needed modules,
		// in the order in which they are to process the data.
		if (neededModules == null) {
			// The modules we have cannot be combined such that
			// the outputType can be generated from the inputData type.
			String message = "No known way of generating output (" + oneOutputType.name() + ") from input("
					+ oneInputData.getType().name() + "), no processing path through modules.";
			throw new UnsupportedOperationException(message);
		}
		usedModules.addAll(neededModules);
		logger.info("Handling request using the following modules:");
		for (MaryModule m : neededModules) {
			logger.info("- " + m.name() + " (" + m.getClass().getName() + ")");
		}
		MaryData currentData = oneInputData;
		for (MaryModule m : neededModules) {
			if (abortRequested)
				break;
			if (m.getState() == MaryModule.MODULE_OFFLINE) {
				// This should happen only in command line mode:
				assert MaryProperties.needProperty("server").compareTo("commandline") == 0;
				logger.info("Starting module " + m.name());
				m.startup();
				assert m.getState() == MaryModule.MODULE_RUNNING;
			}
			long moduleStartTime = System.currentTimeMillis();
			// Let synthesis know which audio format to produce:
			// (this isn't nice -- instead, we could add a reference
			// to the Request to each MaryData, and look up request-specific
			// settings such as default voice and audio file format type
			// from where it is required.)
			if (m.outputType() == MaryDataType.get("AUDIO")) {
				currentData.setAudioFileFormat(audioFileFormat);
				currentData.setAudio(new AppendableSequenceAudioInputStream(audioFileFormat.getFormat(), null));
			}
			// TODO: The following hack makes sure that the Synthesis module gets outputParams. Make this more general and robust.
			if (m.outputType() == oneOutputType || m.outputType() == MaryDataType.AUDIO) {
				currentData.setOutputParams(outputParams);
			}
			if (logger.getEffectiveLevel().equals(Level.DEBUG)
					&& (currentData.getType().isTextType() || currentData.getType().isXMLType())) {
				logger.debug("Handing the following data to the next module:");
				ByteArrayOutputStream dummy = new ByteArrayOutputStream();
				currentData.writeTo(dummy);
				// side effect: writeTo() writes to log if debug
			}
			logger.info("Next module: " + m.name());
			MaryData outData = null;
			try {
				outData = m.process(currentData);
			} catch (Exception e) {
				throw new Exception("Module " + m.name() + ": Problem processing the data.", e);
			}

			if (outData == null) {
				throw new NullPointerException("Module " + m.name() + " returned null. This should not happen.");
			}
			outData.setDefaultVoice(defaultVoice);
			outData.setDefaultStyle(defaultStyle);
			outData.setDefaultEffects(defaultEffects);

			currentData = outData;
			long moduleStopTime = System.currentTimeMillis();
			long delta = moduleStopTime - moduleStartTime;
			Long soFar = timingInfo.get(m);
			if (soFar != null)
				timingInfo.put(m, new Long(soFar.longValue() + delta));
			else
				timingInfo.put(m, new Long(delta));
			if (MaryRuntimeUtils.veryLowMemoryCondition()) {
				logger.info("Very low memory condition detected (only " + MaryUtils.availableMemory()
						+ " bytes left). Triggering garbage collection.");
				Runtime.getRuntime().gc();
				logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
			}
		}
		if (currentData.getType() == MaryDataType.AUDIO) {
			AudioInputStream ais = currentData.getAudio();
			assert ais != null;
			assert ais instanceof AppendableSequenceAudioInputStream;
			((AppendableSequenceAudioInputStream) ais).doneAppending();
		}
		return currentData;
	}

	/**
	 * Split the entire rawmaryxml document into individual paragraph elements. Any text not enclosed by a paragraph in the input
	 * will be enclosed by a new paragraph element, which is then included in the return.
	 * 
	 * @param rawmaryxml
	 *            the maryxml document to split into paragraph chunks; this input document will be modified!
	 * @return a nodelist containing the paragraph elements in the modified rawmaryxml document.
	 */
	private NodeList splitIntoChunks(MaryData rawmaryxml) {
		// TODO: replace this with code that combines this and the splitting functionality of Synthesis: one chunk is one locale
		// and one voice
		if (rawmaryxml == null)
			throw new NullPointerException("Received null data");
		if (rawmaryxml.getType() != MaryDataType.get("RAWMARYXML"))
			throw new IllegalArgumentException("Expected data of type RAWMARYXML, got " + rawmaryxml.getType());
		if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
			logger.debug("Now splitting the following RAWMARYXML data into chunks:");
			ByteArrayOutputStream dummy = new ByteArrayOutputStream();
			try {
				rawmaryxml.writeTo(dummy);
			} catch (Exception ex) {
				logger.debug(ex);
			}
			// side effect: writeTo() writes to log if debug
		}

		Document doc = rawmaryxml.getDocument();
		Element root = doc.getDocumentElement();
		// First, make sure there are no "dangling" text nodes
		// or empty elements
		// that are not enclosed by any paragraph elements.
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
		// TODO: clean up the following code wrt checks for non-TEXT nodes
		Node firstNode = null;
		Node lastNode = null;
		Node currentNode = null;
		while ((currentNode = tw.nextNode()) != null) {
			// Ignore whitespace-only nodes:
			if (currentNode.getNodeType() == Node.TEXT_NODE) {
				Text currentTextNode = (Text) currentNode;
				if (currentTextNode.getData().trim().length() == 0)
					continue;
			}
			if (!MaryDomUtils.hasAncestor(currentNode, MaryXML.PARAGRAPH)) {
				// Outside paragraphs:
				if (firstNode == null)
					firstNode = currentNode;
				lastNode = currentNode;
			} else {
				// a node below a paragraph -- enclose any previous text
				if (firstNode != null) { // have something to enclose
					String first;
					if (firstNode.getNodeType() == Node.TEXT_NODE) {
						first = ((Text) firstNode).getData();
					} else {
						first = firstNode.getNodeName();
					}
					String last;
					if (lastNode.getNodeType() == Node.TEXT_NODE) {
						last = ((Text) lastNode).getData();
					} else {
						last = lastNode.getNodeName();
					}
					logger.debug("Found text node below paragraph; enclosing from '" + first + "' to '" + last + "'");
					MaryDomUtils.encloseNodesWithNewElement(firstNode, lastNode, MaryXML.PARAGRAPH);
					firstNode = null;
					lastNode = null;
				}
			}
		}
		// any leftovers to enclose?
		if (firstNode != null) { // have something to enclose
			String first;
			if (firstNode.getNodeType() == Node.TEXT_NODE) {
				first = ((Text) firstNode).getData();
			} else {
				first = firstNode.getNodeName();
			}
			String last;
			if (lastNode.getNodeType() == Node.TEXT_NODE) {
				last = ((Text) lastNode).getData();
			} else {
				last = lastNode.getNodeName();
			}
			logger.debug("Found text node below paragraph; enclosing from '" + first + "' to '" + last + "'");
			MaryDomUtils.encloseNodesWithNewElement(firstNode, lastNode, MaryXML.PARAGRAPH);
		}
		return doc.getElementsByTagName(MaryXML.PARAGRAPH);
		/*
		 * // Second, for each paragraph, create a separate MaryData. tw = ((DocumentTraversal)doc).createTreeWalker(root,
		 * NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.PARAGRAPH), false); Element paragraph = null; while ((paragraph =
		 * (Element) tw.nextNode()) != null) { MaryData md = extractParagraphAsMaryData(rawmaryxml, paragraph); result.add(md);
		 * 
		 * if (logger.getEffectiveLevel().equals(Level.DEBUG)) { logger.debug("Created chunk:"); ByteArrayOutputStream dummy = new
		 * ByteArrayOutputStream(); try { md.writeTo(dummy); } catch (Exception ex) { logger.debug(ex); } // side effect:
		 * writeTo() writes to log if debug }
		 * 
		 * } return result;
		 */
	}

	/**
	 * Move all the boundary elements outside of paragraphs into paragraphs.
	 * 
	 * @param rawmaryxml
	 *            rawmaryxml
	 */
	private static void moveBoundariesIntoParagraphs(Document rawmaryxml) {
		if (rawmaryxml == null) {
			throw new NullPointerException("Received null rawmaryxml");
		}
		TreeWalker paraTW = ((DocumentTraversal) rawmaryxml).createTreeWalker(rawmaryxml.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.PARAGRAPH), true);
		TreeWalker tw = ((DocumentTraversal) rawmaryxml).createTreeWalker(rawmaryxml.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT, new NameNodeFilter(new String[] { MaryXML.PARAGRAPH, MaryXML.BOUNDARY }), true);
		// Find first paragraph, to see if there are any leading boundaries
		Element firstParagraph = (Element) paraTW.nextNode();
		if (firstParagraph == null) {
			throw new NullPointerException("Document does not have a paragraph");
		}
		tw.setCurrentNode(firstParagraph);
		// Now move any leading boundaries to the left of into the first paragraph
		Element boundary = null;
		while ((boundary = (Element) tw.previousNode()) != null) {
			assert boundary.getTagName().equals(MaryXML.BOUNDARY);
			firstParagraph.insertBefore(boundary, firstParagraph.getFirstChild());
			tw.setCurrentNode(firstParagraph);
		}
		// Now, for each paragraph, all the boundaries following it are shifted inside it
		tw.setCurrentNode(firstParagraph);
		Element paragraph = firstParagraph;
		Element current = null;
		while ((current = (Element) tw.nextNode()) != null) {
			if (current.getTagName().equals(MaryXML.PARAGRAPH)) {
				paragraph = current;
			} else { // current is a boundary
				if (!DomUtils.hasAncestor(current, MaryXML.PARAGRAPH)) {
					// current is a boundary outside a paragraph
					paragraph.appendChild(current);
				}
			}
		}
	}

	/**
	 * For a given maryxml document, extract one paragraph element as a separate document, including any parent nodes around the
	 * paragraph element.
	 * 
	 * @param maryxml
	 *            maryxml
	 * @param paragraph
	 *            paragraph
	 */
	private static MaryData extractParagraphAsMaryData(MaryData maryxml, Element paragraph) {
		if (!maryxml.getType().isMaryXML()) {
			throw new IllegalArgumentException("Expected MaryXML data");
		}
		String rootLanguage = maryxml.getDocument().getDocumentElement().getAttribute("xml:lang");
		Document newDoc = MaryXML.newDocument();
		Element newRoot = newDoc.getDocumentElement();
		// Now import not only the paragraph itself (with substructure,
		// but also all the nodes above it (without substructure).
		Element importedInner = (Element) newDoc.importNode(paragraph, true);
		Element toImport = (Element) paragraph.getParentNode();
		while (!toImport.getTagName().equals(MaryXML.MARYXML)) {
			Element imported = (Element) newDoc.importNode(toImport, false);
			imported.appendChild(importedInner);
			importedInner = imported;
			toImport = (Element) toImport.getParentNode();
		}
		newRoot.appendChild(importedInner);
		// Determine the language of the new document.
		String language = rootLanguage;
		Element voice = (Element) MaryDomUtils.getAncestor(paragraph, MaryXML.VOICE);
		if (voice != null) {
			if (voice.hasAttribute("xml:lang")) {
				language = voice.getAttribute("xml:lang");
			} else if (voice.hasAttribute("name")) {
				String name = voice.getAttribute("name");
				Voice v = Voice.getVoice(name);
				if (v != null && v.getLocale() != null)
					language = MaryUtils.locale2xmllang(v.getLocale());
			}
		}
		newRoot.setAttribute("xml:lang", language);
		MaryData md = new MaryData(maryxml.getType(), MaryUtils.string2locale(language));
		Voice dVoice = maryxml.getDefaultVoice();
		if (dVoice != null) {
			md.setDefaultVoice(dVoice);
		}
		// md.setAudioEffects(maryxml.getAudioEffects());
		md.setDocument(newDoc);
		return md;
	}

	/**
	 * For a given instance of MaryData, determine the locale -- either from the data type, or, if it is not specified there, from
	 * the XML document root element's attribute "xml:lang".
	 * 
	 * @param data
	 *            data
	 */
	private Locale determineLocale(MaryData data) {
		Locale locale = null;
		if (data.getType().isXMLType()) {
			// We can determine the locale if the document element
			// has an xml:lang attribute.
			Document doc = data.getDocument();
			if (doc != null) {
				Element docEl = doc.getDocumentElement();
				if (docEl != null) {
					String langCode = docEl.getAttribute("xml:lang");
					if (!langCode.equals("")) {
						locale = MaryUtils.string2locale(langCode);
					}
				}
			}
		}
		assert defaultLocale != null;
		return locale != null ? locale : defaultLocale;
	}

	/**
	 * Direct access to the output data.
	 * 
	 * @return outputdata
	 */
	public MaryData getOutputData() {
		return outputData;
	}

	/**
	 * Write the output data to the specified OutputStream.
	 * 
	 * @param outputStream
	 *            outputStream
	 * @throws Exception
	 *             Exception
	 */
	public void writeOutputData(OutputStream outputStream) throws Exception {
		if (outputData == null) {
			throw new NullPointerException("No output data -- did process() succeed?");
		}
		if (outputStream == null)
			throw new NullPointerException("cannot write to null output stream");
		// Safety net: if the output is not written within a certain amount of
		// time, give up. This prevents our thread from being locked forever if an
		// output deadlock occurs (happened very rarely on Java 1.4.2beta).
		final OutputStream os = outputStream;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			public void run() {
				logger.warn("Timeout occurred while writing output. Forcefully closing output stream.");
				try {
					os.close();
				} catch (IOException ioe) {
					logger.warn(ioe);
				}
			}
		};
		int timeout = MaryProperties.getInteger("modules.timeout", 10000);
		if (outputType.equals(MaryDataType.get("AUDIO"))) {
			// This means either a lot of data (for WAVE etc.) or a lot of processing
			// effort (for MP3), so allow for a lot of time:
			timeout *= 5;
		}
		timer.schedule(timerTask, timeout);
		try {
			outputData.writeTo(os);
		} catch (Exception e) {
			timer.cancel();
			throw e;
		}
		timer.cancel();
	}

}
