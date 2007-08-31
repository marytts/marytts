/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.dbselection;

import java.io.*;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.de.phonemiser.Inflection;
import de.dfki.lt.mary.modules.de.*;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.usaar.coli.fst.FSTLookup;

/**
 * 
 * 
 * @author Anna Hunecke
 *
 */
public class WikipediaSentenceChecker{
    
    protected static FSTLookup lexicon;
    protected static FSTLookup englishWords;
    protected static Map userdict;
    protected static Inflection inflection;
    protected static JPhonemiser jPhon;
    protected static TextToMaryXML ttMaryXML;
    protected static JTokeniser jTok;
    protected static Preprocess pProc;
    protected static Shprot shProt;
    protected static InformationStructure infoStruct;
    protected static Prosody prosody;
    protected static PrintWriter doneOut;
    
    public static void main(String[] args){
        
        /* first sort out the files and their names */
        String doneDirsTextName = "done.txt";
        String basenameFile = "sentenceNames.lst";
        String goodBasenameFile = "basenamesLessStrict.lst";
        
        /* Read in the basenames */
        System.out.println("Reading sentence names ...");
        String[] basenames;
        try{
            BufferedReader basenamesIn =
                new BufferedReader(
                        new FileReader(
                                new File(basenameFile)));
            String line = basenamesIn.readLine();
            int numBasenames = Integer.parseInt(line);
            basenames = new String[numBasenames];
            int i=0;
            while((line=basenamesIn.readLine())!=null){
                if (line.equals("")) continue;
                basenames[i] = line.trim();
                i++;
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error reading basenames");
        }
        
        PrintWriter basenamesOut;
        try{
            basenamesOut = 
                new PrintWriter(
                        new FileWriter(
                                new File(goodBasenameFile),true),true);
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error opening basenameOutput");
        }
        /* now loop over the book directories */
        List goodBasenames = new ArrayList();
        List badBasenames = new ArrayList();
        long time = System.currentTimeMillis();
        try{
            List alreadyDone = readInDoneDirs(doneDirsTextName);
            System.out.println("Loading modules ...");
            loadModules();            
            
            System.out.println("Processing sentences...");
            int tenPercent = basenames.length/10; 
            for (int j=0;j<basenames.length;j++){ 
                //if ((j % tenPercent) == 0 && j!=0){
                //  int percentage = j/tenPercent;
                // System.out.print(" "+percentage+"0%");
                //}
                /* read in the sentence */
                String name = basenames[j];
                
                if (alreadyDone.remove(name)){
                    basenames[j] = null;
                    name = null;
                    continue;
                }
                System.out.println(name);
                
                BufferedReader sentIn;
                try{ 
                    sentIn= new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(name),"UTF-8"));
                } catch (FileNotFoundException fnfe){
                    System.out.println("File not found : "+name);
                    continue;
                }
                String sent = sentIn.readLine();
                sentIn.close();
                /* normalise sentence */
                if (sent.startsWith(".")) 
                    sent = sent.substring(1,sent.length()).trim();
                if (sent.matches(".*/.*")) 
                    sent = sent.replaceAll("/","").trim();
                /* process the sentence */
                //System.out.println("Processing sentence "+sentences[j].getName()
                //      +": \""+sent+"\"");
                MaryData d = new MaryData(MaryDataType.get("TEXT_DE"));
                boolean goodSentence = false;
                doneOut.println(name);
                try{                    
                    InputStream is = new ByteArrayInputStream(sent.getBytes("UTF-8"));
                    d.readFrom(is);
                    //TextToMaryXML
                    d = ttMaryXML.process(d);
                    //JTokeniser
                    d = jTok.process(d);
                    //Preprocess
                    d = pProc.process(d);
                    //Shprot
                    d = shProt.process(d);
                    //InfoStruct
                    d = infoStruct.process(d);
                    //JPhonemiser
                    goodSentence = processDoc(d);
                } catch (Exception e){
                    e.printStackTrace();
                    System.out.println("Error processing sentence "
                            +name
                            +": \""+sent+"\"; skipping sentence");
                    continue;
                }
                catch (AssertionError ae){
                    ae.printStackTrace();
                    System.out.println("Error processing sentence "
                            +name
                            +": \""+sent+"\"; skipping sentence");
                    continue;
                }
                if (goodSentence){
                    basenamesOut.println(name);
                } 
                
                
            }//end of loop over book directories
            doneOut.close();
            
            long duration = System.currentTimeMillis() - time;
            System.out.println("Processing sentences took "+duration);
            
            basenamesOut.close();
            
            
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error!");
        }
        System.out.println("Done!");        
    }    
    
    protected static void loadModules() throws Exception{
        System.out.println("Loading modules:");
        MaryProperties.readProperties();
        System.out.println("- Loading JPhonemiser");
        jPhon = new JPhonemiser();
        jPhon.startup();
        
        String basePath = MaryProperties.maryBase() + File.separator +
        "lib" + File.separator + "modules" + File.separator + "de" + File.separator + "lexicon" +
        File.separator;
        
        String lexiconFilename = basePath + "mary-lexicon.fst";
        lexicon = new FSTLookup(lexiconFilename, "ISO-8859-1");
        if (MaryProperties.getBoolean("german.phonemiser.useenglish")) {
            String englishWordsFilename = basePath + "english-words.fst";
            englishWords = new FSTLookup(englishWordsFilename, "ISO-8859-1");
        }
        userdict = Collections.synchronizedMap(new HashMap());
        String userdictFilename = basePath + "userdict.txt";
        BufferedReader bir = new BufferedReader(
                new FileReader(new File(userdictFilename)));
        String line;
        while ((line = bir.readLine()) != null) {
            // Ignore empty lines and comments:
            if (line.trim().equals("") || line.startsWith("#"))
                continue;
            StringTokenizer st = new StringTokenizer(line, "\\");
            // In the userdict file, the first field is the graphemic form,
            // the second is the phonemic form; any further fields are ignored.
            //userdict.put(st.nextToken(), st.nextToken()); Jochen Test:
            userdict.put(st.nextToken(), st.nextToken());
        }
        inflection = new Inflection();   
        System.out.println("- Loading TextToMaryXML");
        ttMaryXML = new TextToMaryXML();
        ttMaryXML.startup();
        System.out.println("- Loading JTokeniser");
        jTok = new JTokeniser();
        jTok.startup();
        System.out.println("- Loading Preprocess");
        pProc = new Preprocess();
        pProc.startup();
        System.out.println("- Loading Shprot");
        shProt = new Shprot();
        shProt.startup();
        System.out.println("- Loading InformationStructure");
        infoStruct = new InformationStructure();
        infoStruct.startup();
        System.out.println("- Loading Prosody");
        prosody = new Prosody();
        prosody.startup();
        System.out.println("- Loading Postlex");
    }
    
    
    protected static boolean processDoc(MaryData d){
        
        Document doc = d.getDocument();
        // StringBuffer ppText = new StringBuffer();
        //    getXMLAsString(doc,ppText);
        //    System.out.println(ppText.toString());
        inflection.determineEndings(doc);
        NodeIterator it = ((DocumentTraversal)doc).
        createNodeIterator(doc, NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(MaryXML.TOKEN), false);
        Element t = null;
        boolean result = true;
        while ((t = (Element) it.nextNode()) != null) {
            String text;
            boolean isEnglish = false;
            if (t.hasAttribute("xml:lang") && t.getAttribute("xml:lang").equals("en")) {
                isEnglish = true;
            }
            // Do not touch tokens for which a transcription is already
            // given (exception: transcription contains a '*' character:
            if (t.hasAttribute("sampa") &&
                    t.getAttribute("sampa").indexOf('*') == -1) {
                continue;
            }
            
            if (t.hasAttribute("pos") &&
                    t.getAttribute("pos").startsWith("$")){
                continue;
            }
            
            if (t.hasAttribute("sounds_like"))
                text = t.getAttribute("sounds_like");
            else
                text = MaryDomUtils.tokenText(t);
            if (text != null && !text.equals("")) {
                // If text consists of several parts (e.g., because that was
                // inserted into the sounds_like attribute), each part
                // is transcribed separately.
                StringBuffer sampa = new StringBuffer();
                StringTokenizer st = new StringTokenizer(text, " -");
                boolean breakLoop = false;
                while (st.hasMoreTokens() && !breakLoop) {
                    String graph = st.nextToken();
                    StringBuffer helper = new StringBuffer();
                    String phon = null;
                    if (isEnglish) {
                        phon = jPhon.phonemiseEn(graph, helper);
                    }
                    if (phon == null) {
                        graph = MaryUtils.normaliseUnicodeLetters(text, Locale.GERMAN);
                        String lookup = tryLookup(graph,helper);
                        if (lookup == null){
                            //replace ß by ss
                            String newGraph = graph.replaceAll("ß","ss");
                            //replace th by t
                            newGraph = newGraph.replaceAll("th","t");
                            //try to lookup again
                            lookup = tryLookup(newGraph,helper);
                        }
                        if (lookup != null){
                            phon = lookup;
                        } else {
                            phon = jPhon.phonemise(graph, helper);
                            String method = graph.toString();
                            if (method.equals("rules")){                            
                                //phonemised with rules
                                result = false;
                                breakLoop = true;
                            }
                            //else phonemised with phonemiseDenglish or Compound     
                        }
                    } else {
                        //phonemised with English dictionary/rules
                        result = false;
                        breakLoop = true;
                    }
                    
                    if (sampa.length() == 0) { // first part
                        // The g2pMethod of the combined beast is
                        sampa.append(phon);
                    } else { // following parts
                        sampa.append("-");
                        // Reduce primary to secondary stress:
                        sampa.append(phon.replace('\'', ','));                        
                    }
                }//end of while loop over String Tokenizer tokens
                
                if (breakLoop) break;
                
                if (sampa != null && !sampa.toString().equals("")) {
                    jPhon.setSAMPA(t, sampa.toString());
                } else {
                    //unable to phonemise
                    result = false;
                    break;
                }
            } else { //not (text != null && !text.equals(""))
                System.out.println(t.getLocalName());
            }
        } //end of while loop over elements        
        return result;
    }
    
    protected static String tryLookup(String text, StringBuffer g2pMethod){
        
        String result = jPhon.userdictLookup(text);		
        if (result != null) {
            g2pMethod.append("userdict");
            return result;
        }
        result = jPhon.lexiconLookup(text);
        if (result != null) 
            g2pMethod.append("lexicon");
        
        return result;
    }
    
    private static List readInDoneDirs(String doneDirsTextName) throws Exception{
        File doneDirsText = new File(doneDirsTextName);
        List doneList = new ArrayList();
        
        if (doneDirsText.exists()){
            
            BufferedReader doneIn =
                new BufferedReader(new FileReader(doneDirsText));
            String line;
            
            while((line=doneIn.readLine()) != null){
                doneList.add(line.trim());
            }
            doneIn.close();
        } 
        
        doneOut = new PrintWriter(new FileWriter(doneDirsText,true),true);
        return doneList;
    }
    
    
    /**
     * Convert the given xml-node and its subnodes to Strings
     * and collect them in the given Stringbuffer
     * 
     * @param motherNode the xml-node
     * @param ppText the Stringbuffer
     */
    private static void getXMLAsString(Node motherNode,StringBuffer ppText){
        NodeList children = motherNode.getChildNodes();
        for (int i=0;i<children.getLength();i++){
            Node nextChild = children.item(i);
            String name = nextChild.getLocalName();
            if (name == null){
                continue;
            }          
            
            ppText.append("<"+name);
            if (nextChild instanceof Element){
                if (nextChild.hasAttributes()){
                    NamedNodeMap atts = nextChild.getAttributes();
                    for (int j=0;j<atts.getLength();j++){
                        String nextAtt = atts.item(j).getNodeName();
                        ppText.append(" "+nextAtt+"=\""
                                +((Element)nextChild).getAttribute(nextAtt)
                                +"\"");
                    }
                }
                
            }
            if (name.equals("boundary")){
                ppText.append("/>\n");
                continue;
            }
            ppText.append(">\n");
            if (name.equals("t")){
                ppText.append(MaryDomUtils.tokenText((Element)nextChild)
                        +"\n</t>\n");                
            } else {
                if (nextChild.hasChildNodes()){
                    getXMLAsString(nextChild,ppText);
                } 
                ppText.append("</"+name+">\n");
            }
        }
    }
    
}
