package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

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
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * Automatic Labelling using EHMM labeller
 * @author Sathish Chandra Pammi
 */

public class EHMMLabeler extends VoiceImportComponent {
        
        private DatabaseLayout db;
        private MaryClient mary;
        private File rootDir;
        private File ehmm;
        private String voicename;
        private String outputDir;
     
        private int progress;
        private String locale;
        
        public final String EDIR = "EHMMLabeler.eDir";
        public final String EHMMDIR = "EHMMLabeler.ehmmDir";
        public final String TRANSCRIPTFILE = "EHMMLabeler.transcriptFile";
        public final String MARYSERVERHOST = "EHMMLabeler.maryServerHost";
        public final String MARYSERVERPORT = "EHMMLabeler.maryServerPort";
        
         public final String getName(){
            return "EHMMLabeler";
        }
        
       public SortedMap getDefaultProps(DatabaseLayout db){
           this.db = db;
           if (props == null){
               props = new TreeMap();
               String ehmmdir = System.getProperty("EHMMDIR");
               if ( ehmmdir == null ) {
                   ehmmdir = "/project/mary/Festival/festvox/src/ehmm/";
               }
               props.put(EHMMDIR,ehmmdir);
               
               props.put(EDIR,db.getProp(db.ROOTDIR)
                            +"ehmm"
                            +System.getProperty("file.separator"));
               props.put(TRANSCRIPTFILE,db.getProp(db.ROOTDIR)
                            +"txt.done.data");
               props.put(MARYSERVERHOST,"localhost");
               props.put(MARYSERVERPORT,"59125");
           }
           return props;
       }
       
       protected void setupHelp(){
           props2Help = new TreeMap();
           props2Help.put(EHMMDIR,"directory containing the local installation of EHMM Labeller"); 
           props2Help.put(EDIR,"directory containing all files used for training and labeling. Will be created if it does not exist.");
           props2Help.put(TRANSCRIPTFILE,"file containing the transcripts in festvox format");
           props2Help.put(MARYSERVERHOST,"the host were the Mary server is running, default: \"localhost\"");
           props2Help.put(MARYSERVERPORT,"the port were the Mary server is listening, default: \"59125\"");
       }
        
        public void initialiseComp()
        {
            progress = 0;
            locale = db.getProp(db.LOCALE);
        }
        
        /**
         * Do the computations required by this component.
         * 
         * @return true on success, false on failure
         */
        public boolean compute() throws Exception{
            
            progress = 0;
                                    
            System.out.println("Preparing voice database for labelling using EHMM :");
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            //get the voicename        
            voicename = db.getProp(db.VOICENAME);
            //make new directories ehmm and etc
            ehmm = new File(getProp(EDIR));
            // get the output directory of files used by EHMM 
            outputDir = ehmm.getAbsolutePath()+"/etc";
            
            
            /* setup the EHMM directory */
            System.out.println("Setting up EHMM directory ...");
            setup();
            System.out.println(" ... done.");
            progress = 1;
            
                        
            /* read in the transcriptions, 
             * build up dictionary and phone set; 
             * dump the transcriptions */
            //dictionary 
            Map dictionary = new HashMap();
            //set of phones
            Set phones = new HashSet();
            //fill dictionary and phone set, dump transcriptions   
            System.out.println("Building dictionary, phone set and dumping transcriptions ...");
            buildDictAndDumpTrans(dictionary,phones);
            System.out.println(" ... done.");
            progress = 45;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            /* dump the filenames */
            System.out.println("Dumping required files ....");
            dumpRequiredFiles();
            System.out.println(" ... done.");           
            progress = 47;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            /* Computing Features (MFCCs) for EHMM */
            System.out.println("Computing MFCCs ...");
            computeFeatures();
            System.out.println(" ... done.");
            progress = 50;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Scaling Feature Vectors ...");
            scaleFeatures();
            System.out.println(" ... done.");
            progress = 53;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Intializing EHMM Model ...");
            intializeEHMMModels();
            System.out.println(" ... done.");
            progress = 55;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("EHMM baum-welch re-estimation ...");
            System.out.println("It may take more time (may be 1 or 2 days) depending on voice database ...");
            baumWelchEHMM();            
            System.out.println(" ... done.");
            progress = 90;
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Aligning EHMM for labelling ...");
            System.out.println("And Copying label files into lab directory ...");
            alignEHMM();
            System.out.println(" ... done.");
  
            System.out.println("Label file Generation Successfully completed using EHMM !"); 
            progress = 100;
            
            return true;
        }
        
        
       /**
        * Setup the EHMM directory
        * @throws IOException, InterruptedException
        */
        private void setup() throws IOException,InterruptedException{
            
            ehmm.mkdir();
            File lab = new File(ehmm.getAbsolutePath()+"/lab");
            //call setup of EHMM in this directory
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to ehmm directory and setup Directory Structure 
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; mkdir feat"
                    +"; mkdir etc"
                    +"; mkdir mod"
                    +"; mkdir lab"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            PrintWriter settings = new PrintWriter(
                    new FileOutputStream (new File(outputDir+"/"+voicename+".featSettings")));
            
            
            // Feature Settings required for EHMM Training
            settings.println("WaveDir: "+db.getProp(db.ROOTDIR)+"/wav \n"
                    +"HeaderBytes: 44 \n"
                    +"SamplingFreq: 16000 \n"
                    +"FrameSize: 160 \n"
                    +"FrameShift: 80 \n"
                    +"Lporder: 12 \n"
                    +"CepsNum: 16 \n"
                    +"FeatDir: "+getProp(EDIR)+"/feat \n"
                    +"Ext: .wav \n");
            settings.flush();
            settings.close();
            }
       
        /**
         * Creating Required files for EHMM Training
         * @throws IOException, InterruptedException
         */
        private void dumpRequiredFiles()throws IOException,InterruptedException{
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to ehmm directory and create required files for EHMM 
            System.out.println("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/phfromutt.pl "
                    +outputDir+"/"+voicename+"_train.transcription "
                    +outputDir+"/"+voicename+".phoneList 5"
                    +"; perl "+getProp(EHMMDIR)+"/bin/getwavlist.pl "
                    +outputDir+"/"+voicename+"_train.transcription "
                    +outputDir+"/"+voicename+".waveList"
                    +"; exit )\n");
            
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/phfromutt.pl "
                    +outputDir+"/"+voicename+"_train.transcription "
                    +outputDir+"/"+voicename+".phoneList 5 > log.txt"
                    +"; perl "+getProp(EHMMDIR)+"/bin/getwavlist.pl "
                    +outputDir+"/"+voicename+"_train.transcription "
                    +outputDir+"/"+voicename+".waveList >> log.txt"
                    +"; exit )\n");
            
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
                  
        }
        
        /**
         * Computing Features Required files for EHMM Training
         * @throws IOException, InterruptedException
         */
        private void computeFeatures()throws IOException,InterruptedException{
  
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( cd "+ehmm.getAbsolutePath()
                    +"; "+getProp(EHMMDIR)+"/bin/FeatureExtraction "
                    +outputDir+"/"+voicename+".featSettings "
                    +outputDir+"/"+voicename+".waveList >> log.txt"  
                    +"; perl "+getProp(EHMMDIR)+"/bin/comp_dcep.pl "
                    +outputDir+"/"+voicename+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat mfcc ft 0 0 >> log.txt "
                  +"; exit )\n");
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; "+getProp(EHMMDIR)+"/bin/FeatureExtraction "
                    +outputDir+"/"+voicename+".featSettings "
                    +outputDir+"/"+voicename+".waveList >> log.txt"  
                    +"; perl "+getProp(EHMMDIR)+"/bin/comp_dcep.pl "
                    +outputDir+"/"+voicename+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat mfcc ft 0 0 >> log.txt"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue(); 
        
        }
        
        /**
         * Scaling Features for EHMM Training
         * @throws IOException, InterruptedException
         */
        private void scaleFeatures()throws IOException,InterruptedException{
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/scale_feat.pl "
                    +outputDir+"/"+voicename+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat "+ehmm.getAbsolutePath()+"/mod ft 4 >> log.txt"
                    +"; exit )\n");
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/scale_feat.pl "
                    +outputDir+"/"+voicename+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat "+ehmm.getAbsolutePath()+"/mod ft 4 >> log.txt"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
        }
        
        /**
         * Initializing EHMM Models
         * @throws IOException, InterruptedException
         */
     private void intializeEHMMModels()throws IOException,InterruptedException{

         Runtime rtime = Runtime.getRuntime();
         //get a shell
         Process process = rtime.exec("/bin/bash");
         //get an output stream to write to the shell
         PrintWriter pw = new PrintWriter(
                 new OutputStreamWriter(process.getOutputStream()));
         pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; perl "+getProp(EHMMDIR)+"/bin/seqproc.pl "
                 +outputDir+"/"+voicename+"_train.transcription "
                 +outputDir+"/"+voicename+".phoneList 2 2 13 >> log.txt"
                 +"; exit )\n");
         pw.flush();
         //shut down
         pw.close();
         process.waitFor();
         process.exitValue();
     
            
        }
        
     
     /**
      * Training EHMM Models
      * @throws IOException, InterruptedException
      */
     private void baumWelchEHMM() throws IOException,InterruptedException{
    
         Runtime rtime = Runtime.getRuntime();
         //get a shell
         Process process = rtime.exec("/bin/bash");
         //get an output stream to write to the shell
         PrintWriter pw = new PrintWriter(
                 new OutputStreamWriter(process.getOutputStream()));
         
         System.out.println("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                 +outputDir+"/"+voicename+".phoneList.int "
                 +outputDir+"/"+voicename+"_train.transcription.int 1 0 "
                 +ehmm.getAbsolutePath()+"/feat "
                 +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                 +"; exit )\n");
         
         pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                 +outputDir+"/"+voicename+".phoneList.int "
                 +outputDir+"/"+voicename+"_train.transcription.int 1 0 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                 +"; exit )\n");
         // 1 0 ehmm/feat ft ehmm/mod 0 0 0
         pw.flush();
         //shut down
         pw.close();
         process.waitFor();
         process.exitValue();    
         
            
     }
     
     /**
      * Aligning EHMM and Label file generation 
      * @throws IOException, InterruptedException
      */
     private void alignEHMM() throws IOException,InterruptedException{
  
         Runtime rtime = Runtime.getRuntime();
         //get a shell
         Process process = rtime.exec("/bin/bash");
         //get an output stream to write to the shell
         PrintWriter pw = new PrintWriter(
                 new OutputStreamWriter(process.getOutputStream()));
         
         System.out.println("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/edec "
                 +outputDir+"/"+voicename+".phoneList.int "
                 +outputDir+"/"+voicename+"_train.transcription.int 1 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +outputDir+"/"+voicename+".featSettings "
                 +ehmm.getAbsolutePath()+"/mod >> log.txt"
                 +"; perl "+getProp(EHMMDIR)+"/bin/sym2nm.pl "
                 +ehmm.getAbsolutePath()+"/lab "
                 +outputDir+"/"+voicename+".phoneList.int >> log.txt"
                 +"; exit )\n");
         
         pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/edec "
                 +outputDir+"/"+voicename+".phoneList.int "
                 +outputDir+"/"+voicename+"_train.transcription.int 1 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +outputDir+"/"+voicename+".featSettings "
                 +ehmm.getAbsolutePath()+"/mod >> log.txt"
                 +"; perl "+getProp(EHMMDIR)+"/bin/sym2nm.pl "
                 +ehmm.getAbsolutePath()+"/lab "
                 +outputDir+"/"+voicename+".phoneList.int >> log.txt"
                 +"; exit )\n");
         
         pw.flush();
         //shut down
         pw.close();
         process.waitFor();
         process.exitValue();     
         
     }
     
        /**
         * Build a dictionary and dump the transcription file
         * @param dictionary the dictionary to be filled
         * @param phones the phone set to be filled
         * @throws Exception
         */
        private void buildDictAndDumpTrans(Map dictionary, Set phones) throws Exception {
            //build a new MaryClient
            mary = getMaryClient();
            String inputFormat = "RAWMARYXML"; 
            String outputFormat = "ACOUSTPARAMS";
            
            //open etc/txt.done.data (transcription in)
            BufferedReader transIn = new BufferedReader(
                    new InputStreamReader(new FileInputStream(getProp(TRANSCRIPTFILE)),"UTF-8"));
            
            //open transcription file used for training
            PrintWriter transTrainOut = new PrintWriter(
                    new FileOutputStream (new File(outputDir+"/"+voicename+"_train.transcription")));
            
            //open transcription file used for labeling
            PrintWriter transLabelOut = new PrintWriter(
                    new FileOutputStream (new File(outputDir+"/"+voicename+".align")));
            //write beginning of labeling transcription file
            transLabelOut.println("*align_all*");

            //for the progress bar: calculate the progress of each transcription
            long nextPercentAfter = Math.round(1/(48.0/bnl.getLength()));
            int index = 1;
            
            //store the filenames
            ArrayList filenameList = new ArrayList();
            
            //loop through the transcriptions in txt.done.data;
            //for each transcription, get a segment representation with MARY client
            //then go through segment representation 
            //and collect words and phones 
            //for dictionary and phone set
            String line = transIn.readLine();
            boolean first = true;
            while (line != null){
                StringTokenizer tok = new StringTokenizer(line);
                //discard first token
                tok.nextToken();
                //next token is filename, 
                //put it in filename list
                String nextFilename = tok.nextToken();
                System.out.println(nextFilename);
                filenameList.add(nextFilename);
                
                    //transcription is everything between " "
                    String nextTrans = line.substring(line.indexOf("\"")+1,line.lastIndexOf("\""));
                    nextTrans = getMaryXMLHeader(locale)
                            + nextTrans + "</maryxml>";
                    //System.out.println(nextTrans);
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    //process and dump
                    mary.process(nextTrans, inputFormat, outputFormat, null, null, os);
                    //read into mary data object                
                    MaryData maryData = new MaryData(MaryDataType.get(outputFormat));
                    maryData.readFrom(new ByteArrayInputStream(os.toByteArray()));
                    Document doc = maryData.getDocument();
                    
                    //go through the tokens
                    NodeIterator tokensIt = ((DocumentTraversal)doc).createNodeIterator(doc, 
                            NodeFilter.SHOW_ELEMENT,
                            new NameNodeFilter(MaryXML.TOKEN), 
                            false);
                    Element token = null;
                    //collect the words
                    int numTokens = 0;
                    StringBuffer trainBuff = new StringBuffer();
                    StringBuffer alignBuff = new StringBuffer();
                    boolean sentenceBoundary = false;
                    //trainBuff.append(" SIL ");
                    while ((token = (Element) tokensIt.nextNode()) != null) {
                        //get the word 
                        String word = MaryDomUtils.tokenText(token).toUpperCase();
                        String pos = token.getAttribute("pos");
                        if (word.equals(""))
                            continue;                    
                        if (pos.equals("$PUNCT") || pos.equals("$,")){
                        //if the word is a punctuation,
                            if (sentenceBoundary){                            
                                //if there was a punctuation before,
                                //there is an error in the transcript
                                Element tokenParent = (Element) token.getParentNode();
                                if (!tokenParent.getLocalName().equals("mtu")){                                  
                                    StringBuffer xmlBuf = new StringBuffer();
                                    getXMLAsString(doc,xmlBuf);
                                    System.out.println(xmlBuf.toString());
                                    System.out.println("Problem with token "+word
                                            +" in sentence "+nextFilename);
                                    throw new IllegalArgumentException("Error in transcript of "
                                        +nextFilename+": multiple sentence boundary markers");
                                }
                            }                        
                            //else append sentence end marker
                            //trainBuff.append(" </s>");
                            trainBuff.append(" pau");
                            sentenceBoundary = true;
                            continue;
                        } else {
                            //skip all other non-words
                            if ((!Character.isLetter(word.charAt(0)))
                                        && word.length()<2)
                            continue;
                        }
                        //Convert ' to Q
                        word = word.replace('\'','Q');
                        //append word to transcription string
                        if (sentenceBoundary){
                            //there was a sentence boundary before
                            //append sentence start marker
                            //trainBuff.append(" SIL");
                            //trainBuff.append(" <s>");
                            sentenceBoundary = false;
                        }
                        else if(trainBuff.length() > 0) {
                            trainBuff.append(" ssil");
                        } 
                        
                            //go through the phones
                            NodeList phoneNodes = token.getElementsByTagName(MaryXML.PHONE);
     
                            for (int j=0; j<phoneNodes.getLength(); j++) {
                                //get the next phone node
                                Element phoneNode = (Element) phoneNodes.item(j);
                                //get the phone
                                String phone = phoneNode.getAttribute("p");
                                //convert the phone to EHMM format
                                phone = convertPhone(phone);
                                if (!dictionary.containsKey(phone)){
                                    //store word and pronounciation in dictionary:
                                    //build new pronounciation List
                                    List phoneList = new ArrayList();
                                    phoneList.add(phone);
                                    //add
                                    dictionary.put(phone,phoneList);
                                }
                                //append the phone to the other phones 
                                //phoneList.add(phone);
                                //add the phone to the phone set if not already there
                                phones.add(phone);
                                trainBuff.append(" "+phone);
                                alignBuff.append(" "+phone);
                            }
                        //}//end of if not word in dictionary
                        numTokens++;
                    } //end of loop through tokens
                    //System.out.println("NumTokens: "+numTokens);
                    //print transcription to transcription out
                    transTrainOut.println(nextFilename+" pau"+trainBuff.toString());
                    //transTrainOut.println(nextFilename+trainBuff.toString());
                    
                    if (first){
                        transLabelOut.print(alignBuff.toString().trim());
                        //System.out.println(transBuff.toString());
                        first = false;
                    } else {
                        transLabelOut.print("\n"+alignBuff.toString().trim());
                        //System.out.println(transBuff.toString());
                    }
                line = transIn.readLine();
                //for the progress bar
                if (index == nextPercentAfter){
                    //next percent is due
                    progress++;
                    index = 1;
                } else {
                    index++;
                }
            } //end of loop through lines of txt.done.data

            //close the streams
            transIn.close();
            transTrainOut.flush();
            transTrainOut.close();
            transLabelOut.flush();
            transLabelOut.close();
            
  
        }
        
        /**
         * Convert the given xml-node and its subnodes to Strings
         * and collect them in the given Stringbuffer
         * 
         * @param motherNode the xml-node
         * @param ppText the Stringbuffer
         */
        private void getXMLAsString(Node motherNode,StringBuffer ppText){
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
        
        
        
        /**
         * Get a maryxml header with the given locale
         * @param locale the locale
         * @return the maryxml header
         */
        private String getMaryXMLHeader(String locale)
        {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<maryxml version=\"0.4\"\n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
                "xml:lang=\"" + locale + "\">\n";
            
        }

        /**
         * Get a new MARY client
         * 
         * @return the MARY client
         */
        private MaryClient getMaryClient()throws IOException
        {
            if (mary == null) {
                try{
                    mary = new MaryClient(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT)));        
                } catch (IOException e){
                    throw new IOException("Could not connect to Maryserver at "
                            +getProp(MARYSERVERHOST)+" "+getProp(MARYSERVERPORT));
                }
            }
            return mary;
        }
        
        /**
        * Convert the given phone to Tools format
        * 
        * @param phone the phone
        * @return the converted phone
        */
        private String convertPhone(String phone){
            //since EHMM is not case sensitive, 
            //convert everything to uppercase
            //and mark originally uppercase characters with *
            char[] phoneChars = phone.toCharArray();
            StringBuffer convertedPhone = new StringBuffer();
            for (int i=0;i<phoneChars.length;i++){
                char phoneChar = phoneChars[i];
                if (Character.isLetter(phoneChar)){
                    if (Character.isLowerCase(phoneChar)){
                        //convert to uppercase
                        convertedPhone.append(Character.toUpperCase(phoneChar));
                    } else {
                        //insert * before the char
                        convertedPhone.append("*"+phoneChar);
                    }
                } else {
                    //just append non-letter signs
                    convertedPhone.append(phoneChar);
                }
            }
            return convertedPhone.toString();
        }
        
        /**
         * Provide the progress of computation, in percent, or -1 if
         * that feature is not implemented.
         * @return -1 if not implemented, or an integer between 0 and 100.
         */
        public int getProgress()
        {
            return progress;
        }

}
