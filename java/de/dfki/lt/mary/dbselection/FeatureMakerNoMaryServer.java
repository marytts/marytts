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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.de.phonemiser.Inflection;
import de.dfki.lt.mary.modules.de.*;
import de.dfki.lt.mary.modules.XML2UttAcoustParams;
import de.usaar.coli.fst.FSTLookup;
import de.dfki.lt.mary.modules.TargetFeatureLister;


/**
 * Takes text and converts to features
 * 
 * @author Anna Hunecke
 *
 */

public class FeatureMakerNoMaryServer extends FeatureMakerMaryServer{
    
    
    public static void main(String[] args)throws Exception{

        System.out.println("FeatureMaker started...");
        /* check the arguments */
        if (!readArgs(args)){
            printUsage();
            return;
        }
        /* load the modules */
        if (!loadModules()){
            return;
        }

        /* read in the basenames */
        BufferedReader basenameIn = 
            new BufferedReader(
                    new FileReader(
                            new File(textFiles)));
        String line;
        List basenames = new ArrayList();
        while ((line=basenameIn.readLine())!= null){
            if (line.equals("")) continue;
            basenames.add(line.trim());
        }

        /* start the Credibility Checker */
        unreliableLog = 
            new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(unreliableLogFile),true)),true);   

        /* create output dir */
        File featOutDir = new File(featOutDirName);
        if (!featOutDir.exists()) featOutDir.mkdir();
        
        File sentOutDir = new File(sentOutDirName);
        if (!sentOutDir.exists()) sentOutDir.mkdir();
        
        /* read in the list of already processed files */
        List alreadyDone = readInDoneDirs(doneFileName);
 
        /* open the file to write the basenames to */
         PrintWriter basenamesOut = 
            new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    new File(basenamesOutFile),true)),true);   
        
        /* loop over the files */
        System.out.println("Looping over files...");
        for (Iterator it = basenames.iterator();it.hasNext();){
            String filename = (String) it.next();
            //continue, if we already processed this sentence
            if (alreadyDone.remove(filename)) continue;
            
            System.out.println(filename);
            doneOut.println(filename);
            //open the article file
            BufferedReader fileIn =
                new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(filename),"UTF-8"));
            //store whole article in one buffer
            StringBuffer fileBuf = new StringBuffer();
            while((line= fileIn.readLine())!=null){   
                if (line != ""){
                    fileBuf.append(line+"\n");
                }
            }
            fileIn.close();
            String text = fileBuf.toString();
            if (text.equals("")) continue;
            //process the article in a different thread
            MaryCallerThread mct = new MaryCallerThread(text);
            mct.start();

            // allow the separate thread to process a limited time span
            mct.join(timeOutAfter);

            // check if there was a timeout
            if(!(mct.isFinished())){
                // resolution was stopped due to time out
                mct.interrupt();
                mct.join();
                System.out.println("Timeout when processing article "+filename);
                doneOut.println(filename);
                continue;
            }
            if (!mct.wasSuccessful()){
                System.out.println("Could not process article "+filename);
                doneOut.println(filename);
                continue;
            }
            mct = null;
            if (sentenceList.size() == 0) continue;
            File newFeatDir;
            File newSentDir;
            String f = filename.substring(0, filename.lastIndexOf('.'));
            f = f.substring(f.lastIndexOf('/'),f.length());
            try{
                newFeatDir = new File(featOutDirName+"/"+f);
                newFeatDir.mkdir();
                newSentDir = new File(sentOutDirName+"/"+f);
                newSentDir.mkdir();
            }catch(Exception e){
            	//the featOutDir is full
                //choose a different output dir
                outDirIndex++;
                featOutDirName = featOutDirName+outDirIndex;
                sentOutDirName = sentOutDirName+outDirIndex;
                //make new feature and sentence directories
                File newDir = new File(featOutDirName);
                if (!newDir.exists()) newDir.mkdir();
                newDir = new File(sentOutDirName);
                if (!newDir.exists()) newDir.mkdir();
                //make new directory for the current text
                newFeatDir = new File(featOutDirName+"/"+f);
                newFeatDir.mkdir();
                newSentDir = new File(sentOutDirName+"/"+f);
                newSentDir.mkdir();
            }
            int index=0;
            boolean wroteNothing = true;
            for (Iterator it2=sentenceList.iterator();it2.hasNext();){
                try{
                    String sentence = (String) it2.next();
                    MaryData d = 
                        processSentence(sentence,filename);
                    if (d==null) continue;
                    /* get and dump the features of the sentence */  
                    getFeatures(newFeatDir+"/"+f+"_"+index+".feats",d);     
                    /* dump the sentence */
                    dumpSentence(newSentDir+"/"+f+"_"+index+".txt",sentence);
                    basenamesOut.println(newFeatDir+"/"+f+"_"+index+".feats");
                    wroteNothing = false;
                    index++;
                }catch (Exception e){
                    System.out.println("Error processing sentence "
                            +newFeatDir+"_"+index+" :");
                    e.printStackTrace();
                    index++;
                }
            }//end of loop over sentences
            if (wroteNothing){
                //no feature files have actually been created
                //remove directory
                newFeatDir.delete();
                newSentDir.delete();
            }
        } //end of loop over articles             
        doneOut.close();
        basenamesOut.close();
        System.out.println("Done");
    }//end of main method
    
    
    protected static void printUsage(){
        System.out.println("Usage:\n"
                +"java -cp $CLASSPATH -ea -Dendorsed.dirs=$MARYBASE/lib/endorsed "
                +"-Dmary.base=$MARYBASE "
                +"de.dfki.lt.mary.dbselection.FeatureMakerMaryServer\n"
                +"Please see readme file for details of starting this program\n\n"
                +"Arguments:\n"
                +"-textFiles <file>: File containing the list of text files to be "
                +"processed. Default: textFiles.txt\n\n"
                +"-doneFile <file>: File containing the list of files that have already "
                +"been processed. This file is created automatically during the "
                +"run of the program. Default: done.txt\n\n"
                +"-featureDir <file>: Directory where the features are stored. "
                +"Default: features1. Per default, appropriate sentence files are stored "
                +"in sentences1. The index of feature/sentence dir is increased when the feature "
                +"dir is full.\n\n"
                +"-timeOut <time in ms>: The time in milliseconds the Mary server is allowed "
                +"to split the text of a file into sentence. After the limit is exceeded, "
                +"processing on this file is stopped, and the program continues to the "
                +"next file. Default 30000ms\n\n"
                +"-unreliableLog <file>: Logfile for the unreliable sentence. "
                +"Default: unreliableSents.log\n\n"
                +"-credibility <setting>: Setting that determnines what kind of sentences "
                +"are regarded as credible. There are two settings: strict and lax. With "
                +"setting strict, only those sentences that contain words in the lexicon "
                +"or words that were transcribed by the preprocessor are regarded as credible; "
                +"the other sentences as unreliable. With setting lax, also those words that "
                +"are transcribed with the Denglish and the compound module are regarded credible. "
                +"Default: strict\n\n"
                +"-basenames <file>: File containing the list of feature files that can be "
                +"used in the selection algorithm. Default: basenames.lst\n");
                }

    
      /**
     * Process one sentences from text to target features
     * 
     * @param nextSentence the sentence
     * @param filename the file containing the sentence
     * @return the result of the processing as MaryData object
     */
    protected static MaryData processSentence(String nextSentence,
            String filename){
        //do a bit of normalization
        nextSentence = nextSentence.replaceAll("\\\\","").trim();
        nextSentence = nextSentence.replaceAll("\\s/\\s","").trim();
        nextSentence = nextSentence.replaceAll("^/\\s","").trim();
        MaryData d = new MaryData(MaryDataType.get("TEXT_DE"));
        boolean usefulSentence = true;
        try{                    
            
            InputStream is = new ByteArrayInputStream(nextSentence.getBytes("UTF-8"));
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
            d = phonemise(d);
            if (d==null) 
                usefulSentence = false;
            if (usefulSentence) {
                // Prosody
                d = prosody.process(d);
                // Postlex
                d = postLex.process(d);
                // KlattDurationModeller
                d = kDurMod.process(d);
                // ContourGenerator
                d = contGen.process(d);
                // XML2Utt AcoustParams
                d = xml2AcoustParams.process(d);
                // TargetFeatureLister
                d = targetFeatList.process(d);
                //System.out.println(nextSentence+":\n"+d.getPlainText());
            }
        } catch (Exception e){
            e.printStackTrace();
            if (d!=null){  
                if (d.getPlainText()!=null){
                    System.out.println("Error processing sentence "
                            +filename
                            +": \""+nextSentence+"\":\n"+d.getPlainText()
                            +"; skipping sentence");
                } else {
                    if (d.getDocument() != null){
                        StringBuffer docBuf = new StringBuffer();
                        getXMLAsString(d.getDocument(),docBuf);
                        System.out.println("Error processing sentence "
                                +": \""+nextSentence+"\":\n"+docBuf.toString()
                                +"; skipping sentence");
                    } else {
                        if (d.getUtterances() != null){
                            List utterances = d.getUtterances();
                            Iterator it = utterances.iterator();
                            System.out.println("Error processing sentence "
                                    +": \""+nextSentence+"\":\n");
                            while (it.hasNext()) {
                                Utterance utterance = (Utterance) it.next();
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                utterance.dump(pw, 2, "", true); // padding, justRelations
                                System.out.println(sw.toString());
                            }
                            System.out.println("; skipping sentence");
                        }else {
                            System.out.println("Error processing sentence "
                                    +filename
                                    +": \""+nextSentence+"\"; skipping sentence");                        
                        }
                    }
                }
            } else {
                System.out.println("Error processing sentence "
                        +filename
                        +": \""+nextSentence+"\"; skipping sentence");                        
            }
            return null;
        }
        catch (AssertionError ae){
            ae.printStackTrace();
            System.out.println("Error processing sentence "
                    +filename
                    +": \""+nextSentence+"\"; skipping sentence");
            return null;
        }
         if (usefulSentence){
            return d;
        } else {
            unreliableLog.println(filename+": \""+nextSentence
                    +" : is unreliable");
            return null;
        }
        
        
    }
    
    /**
     * Load the Mary modules needed for processing
     * 
     * @return true, if there was no Exception
     */
    protected static boolean loadModules(){
        try{
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
        postLex = new Postlex();
        postLex.startup();
        System.out.println("- Loading KlattDurationModeller");
        kDurMod = new KlattDurationModeller();
        kDurMod.startup();
        System.out.println("- Loading ContourGenerator");
        contGen = new ContourGenerator();
        contGen.startup();
        System.out.println("- Loading XML2UttAcoustParams");
        xml2AcoustParams = new XML2UttAcoustParams();
        xml2AcoustParams.startup();
        System.out.println("- Loading TargetFeatureLister");
        targetFeatList = 
            new TargetFeatureLister(MaryDataType.get("TARGETFEATURES_DE"), 
            "german.targetfeaturelister");
        targetFeatList.startup();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    
    /**
     * Split the content of the file
     * into separate sentences
     * 
     * @param file the file
     * @return true, if successful
     * @throws Exception
     */
    protected static boolean splitIntoSentences(String file)throws Exception{
        sentenceList = new ArrayList();
        Document doc = processText(file);
        if (doc == null) return false;
        NodeList sentences = doc.getElementsByTagName("s");   

        for (int j=0;j<sentences.getLength();j++){
            Node nextSentence = sentences.item(j);
            //ignore all non-element children
            if (!(nextSentence instanceof Element)) continue; 
            sentence = null;
            //get the tokens
            NodeList tokens = nextSentence.getChildNodes();
            for (int k=0;k<tokens.getLength();k++){
                Node nextToken = tokens.item(k);
                //ignore all non-element children
                if (!(nextToken instanceof Element)) continue; 
                collectTokens(nextToken);                            
            }
            if (sentence!=null){
                sentenceList.add(sentence.toString());  
            } else {
                //ignore
                //System.out.println("NULL SENTENCE!!!");
            }
        } 
        return true;
    }

    
    
    /**
     * Process the given text with the MaryClient
     * from Text to Chunked
     * 
     * @param textString the text to process
     * @return the resulting XML-Document
     * @throws Exception
     */
    protected static Document processText(String textString) throws Exception{
        try{  
            MaryData d = new MaryData(MaryDataType.get("TEXT_DE"));                        
            InputStream is = new ByteArrayInputStream(textString.getBytes("UTF-8"));
            d.readFrom(is);
            //TextToMaryXML
            d = ttMaryXML.process(d);
            //JTokeniser
            d = jTok.process(d);
            //Preprocess
            d = pProc.process(d);
            //Shprot
            d = shProt.process(d);
            return d.getDocument();
        } catch (Exception e){
            e.printStackTrace();
            return null;            
        }
    }
    
    /**
     * Class for processing one chunk of text
     * 
     * @author Anna
     *
     */
    static class MaryCallerThread extends Thread{

        protected String text;
        protected boolean finished;
        protected boolean successful;
        
        /**
         * Build a new MaryCallerThread
         * 
         * @param file the file to process
         */
        public MaryCallerThread(String text){
            this.text = text;
            finished = false;
            successful = false;
            setName("mary caller");
        }

        /**
         * Process the file
         */
        public void run(){
            try{
                successful = splitIntoSentences(text);
            }catch(Exception e){
                e.printStackTrace();
                throw new Error("Error processing text");
            }
            finished = true;
        }

        public boolean isFinished(){
            return finished;
        }

        public boolean wasSuccessful(){
            return successful;
        }
    }
    
}
