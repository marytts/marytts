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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsresources.AppendableSequenceAudioInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * A request consists of input data, a desired output data type and the means
 * to process the input data into the data of the output type.<br> <br> A
 * request is used as follows. First, its basic properties are set in the
 * constructor, such as its input and output types. Second, the input data is
 * provided to the request either by directly setting it
 * (<code>setInputData()</code>) or by reading it from a Reader
 * (<code>readInputData()</code>). Third, the request is processed
 * (<code>process()</code>). Finally, the output data is either accessed
 * directly (<code>getOutputData()</code>) or written to an output stream
 * (<code>writeOutputData</code>).
 */
public class Request {
    private MaryDataType inputType;
    private MaryDataType outputType;
    private AudioFileFormat audioFileFormat;
    private AppendableSequenceAudioInputStream appendableAudioStream;
    private Voice defaultVoice;
    private String defaultStyle;
    private String defaultEffects;
    
    private int id;
    private Logger logger;
    private MaryData inputData;
    private MaryData outputData;
    private boolean streamAudio = false;;
    private boolean abortRequested = false;

    // Keep track of timing info for each module
    // (map MaryModule onto Long)
    private Set<MaryModule> usedModules;
    private Map<MaryModule,Long> timingInfo;

    public Request(MaryDataType inputType, MaryDataType outputType, 
                   Voice defaultVoice, String defaultEffects, String defaultStyle,
                   int id, AudioFileFormat audioFileFormat)
    {
    	this(inputType, outputType, defaultVoice, defaultEffects, defaultStyle, id, audioFileFormat, false);
    }
    
    public Request(MaryDataType inputType, MaryDataType outputType, 
                   Voice defaultVoice, String defaultEffects, String defaultStyle,
                   int id, AudioFileFormat audioFileFormat, boolean streamAudio)
    {
        if (!inputType.isInputType())
            throw new IllegalArgumentException("not an input type: " + inputType.name());
        if (!outputType.isOutputType())
            throw new IllegalArgumentException("not an output type: " + outputType.name());
        this.inputType = inputType;
        this.outputType = outputType;
        this.defaultVoice = defaultVoice;
        this.defaultEffects = defaultEffects;
        this.defaultStyle = defaultStyle;
        this.id = id;
        this.audioFileFormat = audioFileFormat;
        this.streamAudio = streamAudio;
        if (outputType == MaryDataType.get("AUDIO")) {
            if (audioFileFormat == null)
                throw new NullPointerException("audio file format is needed for output type AUDIO");
            this.appendableAudioStream = new AppendableSequenceAudioInputStream(audioFileFormat.getFormat(), new ArrayList());
        } else {
            this.appendableAudioStream = null;
        }
        this.logger = Logger.getLogger("R " + id);
        this.inputData = null;
        this.outputData = null;
        StringBuffer info =
            new StringBuffer(
                "New request (input type \"" + inputType.name() + "\", output type \"" + outputType.name());
        if (this.defaultVoice != null)
            info.append("\", voice \"" + this.defaultVoice.getName());
        if (this.defaultEffects!=null && this.defaultEffects!="")
            info.append("\", effect \"" + this.defaultEffects);
        if (this.defaultStyle!=null && this.defaultStyle!="")
            info.append("\", style \"" + this.defaultStyle);
        if (audioFileFormat != null)
            info.append("\", audio \"" + audioFileFormat.getType().toString()+ "\"");
        if (streamAudio)
        	info.append(", streaming");
        info.append(")");
        logger.info(info.toString());
        
        // Keep track of timing info for each module
        // (map MaryModule onto Long)
        usedModules = new LinkedHashSet<MaryModule>();
        timingInfo = new HashMap<MaryModule,Long>();
    }

    public MaryDataType getInputType() {
        return inputType;
    }
    public MaryDataType getOutputType() {
        return outputType;
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

    public boolean getStreamAudio()
    {
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
     * Set the input data directly, in case it is already in the form
     * of a MaryData object.
     */
    public void setInputData(MaryData inputData) {
        if (inputData != null && inputData.type() != inputType) {
            throw new IllegalArgumentException(
                "Input data has wrong data type (expected "
                    + inputType.toString()
                    + ", got "
                    + inputData.type().toString());
        }
        if (defaultVoice == null) {
            defaultVoice = Voice.getSuitableVoice(inputData);
        }
        assert defaultVoice != null;

        if (inputData.getDefaultVoice() == null) {
            inputData.setDefaultVoice(defaultVoice);
        }
        inputData.setDefaultStyle(defaultStyle);
        inputData.setDefaultEffects(defaultEffects);

        this.inputData = inputData;
    }

    /**
     * Read the input data from a Reader.
     */
    public void readInputData(Reader inputReader) throws Exception {
        inputData = new MaryData(inputType);
        inputData.setWarnClient(true); // log warnings to client
        // For RAWMARYXML, a validating parse is not possible
        // because RAWMARYXML is not tokenised, so it does not yet
        // fulfill the MaryXML Schema constraints.
        if (inputType == MaryDataType.get("RAWMARYXML")) {
            inputData.setValidating(false);
        } else if (inputType.isMaryXML()) {
            inputData.setValidating(MaryProperties.getBoolean("maryxml.validate.input"));
        }
        inputData.readFrom(inputReader, null); // null = read until end-of-file
        if (defaultVoice == null) {
            defaultVoice = Voice.getSuitableVoice(inputData);
        }
        assert defaultVoice != null;
        inputData.setDefaultVoice(defaultVoice);
        inputData.setDefaultStyle(defaultStyle);
        inputData.setDefaultEffects(defaultEffects);
    }

    /**
     * Process the input data to produce the output data.
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
        if (inputType.isTextType() && inputType.name().startsWith("TEXT")
            || inputType.isXMLType() && !inputType.isMaryXML()) {
            // Convert to RAWMARYXML
            rawmaryxml = processOneChunk(inputData, MaryDataType.get("RAWMARYXML"));
            
            assert rawmaryxml.getDefaultVoice() != null;
            inputDataList = splitIntoChunks(rawmaryxml);
        } else if (inputType.equals(MaryDataType.get("RAWMARYXML"))) {
            rawmaryxml = inputData;
            inputDataList = splitIntoChunks(inputData);
        } else {
            // other input data types are processed as a whole
            outputData = processOneChunk(inputData, outputType);
            assert outputData.getDefaultVoice() != null;
            if (appendableAudioStream != null) appendableAudioStream.doneAppending();
            return;
        }
        assert rawmaryxml != null && rawmaryxml.type().equals(MaryDataType.get("RAWMARYXML"))
        && rawmaryxml.getDocument() != null;
        moveBoundariesIntoParagraphs(rawmaryxml.getDocument()); 
        
        // Now the beyond-RAWMARYXML processing:
        if (outputType.isMaryXML()) {
            outputData = new MaryData(outputType);
            // use the input or intermediate MaryXML document
            // as the starting point for MaryXML output types,
            // in order to gradually enrich them:
            outputData.setDocument(rawmaryxml.getDocument());
            outputData.setDefaultVoice(defaultVoice);
            outputData.setDefaultStyle(defaultStyle);
            outputData.setDefaultEffects(defaultEffects);
        }
        int len = inputDataList.getLength();
        for (int i=0; i<len && !abortRequested; i++) {
            Element currentInputParagraph = (Element) inputDataList.item(i);
            assert currentInputParagraph.getTagName().equals(MaryXML.PARAGRAPH);
            NodeList outputNodeList = null;
            // Only process paragraph if there is any text below it:
            if (MaryDomUtils.getPlainTextBelow(currentInputParagraph).trim().equals("")) {
                outputNodeList = currentInputParagraph.getChildNodes();
            } else { // process "real" data:
                MaryData oneInputData = extractParagraphAsMaryData(rawmaryxml, currentInputParagraph);
                assert oneInputData.getDefaultVoice() != null;
                MaryData oneOutputData = processOneChunk(oneInputData, outputType);
                assert oneOutputData.getDefaultVoice() != null;
                if (outputType.isMaryXML()) {
                    NodeList outParagraphList = oneOutputData.getDocument().getDocumentElement().getElementsByTagName(MaryXML.PARAGRAPH);
                    // This does not hold for Tibetan:
                    //assert outParagraphList.getLength() == 1;
                    outputNodeList = outParagraphList;
                } else { // output is not MaryXML, e.g. text or audio
                    if (outputData == null || outputData.type().equals(MaryDataType.get("AUDIO"))) {
                        // Appending is done elsewhere for audio
                        outputData = oneOutputData;
                    } else {
                        outputData.append(oneOutputData);
                    }
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
        if (appendableAudioStream != null) appendableAudioStream.doneAppending();
    }

    /**
     * @param oneInputData the input data to convert
     * @param oneOuputType the output type to convert to
     * @param usedModules for the record, the modules used (will be added to)
     * @param timingInfo for the record, the processing time used for each module (will be added to)
     */
    private MaryData processOneChunk(MaryData oneInputData, MaryDataType oneOutputType) 
    throws TransformerConfigurationException, FileNotFoundException, TransformerException, IOException, Exception {
        if (logger.getEffectiveLevel().equals(Level.DEBUG)
            && (oneInputData.type().isTextType() || oneInputData.type().isXMLType())) {
            logger.debug("Now converting the following input data from "
                    + oneInputData.type() + " to " + oneOutputType + ":");
            ByteArrayOutputStream dummy = new ByteArrayOutputStream();
            oneInputData.writeTo(dummy);
            // side effect: writeTo() writes to log if debug
        }
        Locale locale = determineLocale(oneInputData);
        assert locale != null;
        logger.debug("Determining which modules to use");
        List<MaryModule> neededModules = Mary.modulesRequiredForProcessing(oneInputData.type(), oneOutputType, locale, oneInputData.getDefaultVoice());
        // Now neededModules contains references to the needed modules,
        // in the order in which they are to process the data.
        if (neededModules == null) {
            // The modules we have cannot be combined such that
            // the outputType can be generated from the inputData type.
            String message = "No known way of generating output from input -- " + "no processing path through modules.";
            throw new UnsupportedOperationException(message);
        }
        usedModules.addAll(neededModules);
        MaryModule m = null;
        logger.info("Handling request using the following modules:");
        for (Iterator it = neededModules.iterator(); it.hasNext(); ) {
            m = (MaryModule) it.next();
            logger.info("- " + m.name() + " (" + m.getClass().getName() + ")");
        }
        MaryData currentData = oneInputData;
        for (Iterator it = neededModules.iterator(); it.hasNext() && !abortRequested;) {
            m = (MaryModule) it.next();
            if (m.getState() == MaryModule.MODULE_OFFLINE) {
                // This should happen only in non-server mode:
                assert MaryProperties.needBoolean("server") == false;
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
                currentData.setAudio(appendableAudioStream);
            }
            if (logger.getEffectiveLevel().equals(Level.DEBUG)
                && (currentData.type().isTextType() || currentData.type().isXMLType())) {
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
                timingInfo.put(m, new Long(soFar.longValue()+delta));
            else
                timingInfo.put(m, new Long(delta));
            if (MaryUtils.veryLowMemoryCondition()) {
                logger.info("Very low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
                Runtime.getRuntime().gc();
                logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
            }
        }
        return currentData;
    }

    /**
     * Split the entire rawmaryxml document into individual paragraph elements.
     * Any text not enclosed by a paragraph in the input will be enclosed
     * by a new paragraph element, which is then included in the return.
     * @param rawmaryxml the maryxml document to split into paragraph chunks;
     * this input document will be modified!
     * @return a nodelist containing the paragraph elements in the modified rawmaryxml document. 
     */
    private NodeList splitIntoChunks(MaryData rawmaryxml)
    {
        if (rawmaryxml == null)
            throw new NullPointerException("Received null data");
        if (rawmaryxml.type() != MaryDataType.get("RAWMARYXML"))
            throw new IllegalArgumentException("Expected data of type RAWMARYXML, got " + rawmaryxml.type());
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
        TreeWalker tw = ((DocumentTraversal)doc).
            createTreeWalker(root, NodeFilter.SHOW_TEXT, null, false);
        // TODO: clean up the following code wrt checks for non-TEXT nodes
        Node firstNode = null;
        Node lastNode = null;
        Node currentNode = null;
        while ((currentNode = tw.nextNode()) != null) {
            // Ignore whitespace-only nodes:
            if (currentNode.getNodeType() == Node.TEXT_NODE) {
                Text currentTextNode = (Text) currentNode;
                if (currentTextNode.getData().trim().length() == 0) continue;
            }
            if (!MaryDomUtils.hasAncestor(currentNode, MaryXML.PARAGRAPH)) {
                // Outside paragraphs:
                if (firstNode == null) firstNode = currentNode;
                lastNode = currentNode;                    
            } else {
                // a node below a paragraph -- enclose any previous text
                if (firstNode != null) { // have something to enclose
                    String first;
                    if (firstNode.getNodeType() == Node.TEXT_NODE) {
                        first = ((Text)firstNode).getData();
                    } else {
                        first = firstNode.getNodeName();
                    }
                    String last;
                    if (lastNode.getNodeType() == Node.TEXT_NODE) {
                        last = ((Text)lastNode).getData();
                    } else {
                        last = lastNode.getNodeName();
                    }
                    logger.debug("Found text node below paragraph; enclosing from '"
                            + first + "' to '" + last + "'");
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
                first = ((Text)firstNode).getData();
            } else {
                first = firstNode.getNodeName();
            }
            String last;
            if (lastNode.getNodeType() == Node.TEXT_NODE) {
                last = ((Text)lastNode).getData();
            } else {
                last = lastNode.getNodeName();
            }
            logger.debug("Found text node below paragraph; enclosing from '"
                    + first + "' to '" + last + "'");
            MaryDomUtils.encloseNodesWithNewElement(firstNode, lastNode, MaryXML.PARAGRAPH);
        }
        return doc.getElementsByTagName(MaryXML.PARAGRAPH);
/*        
        // Second, for each paragraph, create a separate MaryData.
        tw = ((DocumentTraversal)doc).createTreeWalker(root, NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(MaryXML.PARAGRAPH), false);
        Element paragraph = null;
        while ((paragraph = (Element) tw.nextNode()) != null) {
            MaryData md = extractParagraphAsMaryData(rawmaryxml, paragraph);
            result.add(md);

            if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
                logger.debug("Created chunk:");
                ByteArrayOutputStream dummy = new ByteArrayOutputStream();
                try {
                    md.writeTo(dummy);
                } catch (Exception ex) {
                    logger.debug(ex);
                }
                // side effect: writeTo() writes to log if debug
            }

        }
        return result;
*/
    }
    
    
    /**
     * Move all the boundary elements outside of paragraphs into paragraphs.
     * @param rawmaryxml
     */
    private void moveBoundariesIntoParagraphs(Document rawmaryxml)
    {
        if (rawmaryxml == null) {
            throw new NullPointerException("Received null rawmaryxml");
        }
        TreeWalker paraTW = ((DocumentTraversal)rawmaryxml).createTreeWalker
        (rawmaryxml.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(MaryXML.PARAGRAPH), true);
        TreeWalker tw = ((DocumentTraversal)rawmaryxml).createTreeWalker
        (rawmaryxml.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] {MaryXML.PARAGRAPH, MaryXML.BOUNDARY}), true);
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
     * For a given maryxml document, extract one paragraph element as
     * a separate document, including any parent nodes around the paragraph
     * element.
     * @param maryxml
     * @param paragraph
     * @return
     */
    private MaryData extractParagraphAsMaryData(MaryData maryxml, Element paragraph)
    {
        if (!maryxml.type().isMaryXML()) {
            throw new IllegalArgumentException("Expected MaryXML data");
        }
        MaryData md = new MaryData(maryxml.type());
        String rootLanguage = maryxml.getDocument().getDocumentElement().getAttribute("xml:lang");
        md.setDefaultVoice(maryxml.getDefaultVoice());
        //md.setAudioEffects(maryxml.getAudioEffects());
        Document newDoc = MaryXML.newDocument();
        md.setDocument(newDoc);
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
                    language = v.getLocale().getLanguage();
            }
        }
        newRoot.setAttribute("xml:lang", language);
        return md;
    }

    /**
     * For a given instance of MaryData, determine the locale -- either
     * from the data type, or, if it is not specified there, from the
     * XML document root element's attribute "xml:lang". 
     * @param data 
     * @return
     */
    private Locale determineLocale(MaryData data) {
        Locale locale = data.type().getLocale();
        if (locale == null) {
        	if (data.type().isXMLType()) {
	            // We can determine the locale if the document element
	            // has an xml:lang attribute.
	            Document doc = data.getDocument();
	            if (doc != null) {
	                Element docEl = doc.getDocumentElement();
	                if (docEl != null) {
	                    String langCode = docEl.getAttribute("xml:lang");
	                    if (langCode.equals("")) {
	                        logger.debug("XML root element does not have an xml:lang attribute -- assuming English");
	                        locale = Locale.ENGLISH;
	                    } else {
	                        locale = MaryUtils.string2locale(langCode);
	                    }
	                }
	            }
        	} else if (data.type().name().equals("TEXT")) {
	            Voice voice = data.getDefaultVoice();
	            if (voice != null) locale = voice.getLocale();
        	}
        }

        // If we get here and still do not have a locale, it is usually
        // an error (but not always, e.g. not for MBROLA input data)
        if (locale == null) {
            locale = Locale.getDefault();
            logger.warn("Received null locale for data of type " + data.type().name() +
            " -- setting to default " + locale);
        }
        return locale;
    }

    /**
     * Direct access to the output data.
     */
    public MaryData getOutputData() {
        return outputData;
    }

    /**
     * Write the output data to the specified OutputStream.
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
