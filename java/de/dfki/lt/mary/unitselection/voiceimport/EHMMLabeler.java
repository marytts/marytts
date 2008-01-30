package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;
import java.util.regex.*;

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
        private File rootDir;
        private File ehmm;
        private String voicename;
        private String outputDir;
        protected String featsExt = ".pfeats";
        protected String labExt = ".lab";
        
        private int progress = -1;
        private String locale;
        
        public final String EDIR = "EHMMLabeler.eDir";
        public final String EHMMDIR = "EHMMLabeler.ehmmDir";
        public final String FEATUREDIR = "EHMMLabeler.featureDir";
        public final String OUTLABDIR = "EHMMLabeler.outputLabDir";
        public final String INITEHMMDIR = "EHMMLabeler.startEHMMModelDir";
        public final String RETRAIN = "EHMMLabeler.reTrainFlag";
        
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
               props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                       +"phonefeatures"
                       +System.getProperty("file.separator"));
               props.put(OUTLABDIR, db.getProp(db.ROOTDIR)
                       +"lab"
                       +System.getProperty("file.separator"));
               props.put(INITEHMMDIR,"/");
               props.put(RETRAIN,"false");
           }
           return props;
       }
       
       protected void setupHelp(){
           props2Help = new TreeMap();
           props2Help.put(EHMMDIR,"directory containing the local installation of EHMM Labeller"); 
           props2Help.put(EDIR,"directory containing all files used for training and labeling. Will be created if it does not exist.");
           props2Help.put(FEATUREDIR, "directory containing the phone features.");
           props2Help.put(OUTLABDIR, "Directory to store generated lebels from EHMM.");
           props2Help.put(INITEHMMDIR,"If you provide a path to previous EHMM Directory, Models will intialize with those models. other wise EHMM Models will build with Flat-Start Initialization");
           props2Help.put(RETRAIN,"true - Do re-training by initializing with given models. false - Do just Decoding");
       }
        
        public void initialiseComp()
        {
           locale = db.getProp(db.LOCALE);
        }
        
        /**
         * Do the computations required by this component.
         * 
         * @return true on success, false on failure
         */
        public boolean compute() throws Exception{
            
            File ehmmFile = new File(getProp(EHMMDIR)+"/bin/ehmm");
            if (!ehmmFile.exists()) {
                throw new IOException("EHMM path setting is wrong. Because file "+ehmmFile.getAbsolutePath()+" does not exist");
            }
                                    
            System.out.println("Preparing voice database for labelling using EHMM :");
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            //get the voicename        
            voicename = db.getProp(db.VOICENAME);
            //make new directories ehmm and etc
            ehmm = new File(getProp(EDIR));
            // get the output directory of files used by EHMM 
            outputDir = ehmm.getAbsolutePath()+"/etc";
            
            // setup the EHMM directory 
           
            System.out.println("Setting up EHMM directory ...");
            setup();
            System.out.println(" ... done.");
            
            //Getting Phone Sequence for Force Alignment    
            System.out.println("Getting Phone Sequence from Phone Features...");
            getPhoneSequence();
            System.out.println(" ... done.");
           
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            // dump the filenames 
            System.out.println("Dumping required files ....");
            dumpRequiredFiles();
            System.out.println(" ... done.");           
            
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            // Computing Features (MFCCs) for EHMM 
            System.out.println("Computing MFCCs ...");
            computeFeatures();
            System.out.println(" ... done.");
            
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Scaling Feature Vectors ...");
            scaleFeatures();
            System.out.println(" ... done.");
            
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Intializing EHMM Model ...");
            intializeEHMMModels();
            System.out.println(" ... done.");
            
            baumWelchEHMM();            
            
            System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
            System.out.println("Aligning EHMM for labelling ...");
            alignEHMM();
            
//            System.out.println("And Copying label files into lab directory ...");
//            getProperLabelFormat();
//            System.out.println(" ... done.");
            
            System.out.println("Label file Generation Successfully completed using EHMM !"); 
            
            
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
                    new FileOutputStream (new File(outputDir+"/"+"ehmm"+".featSettings")));
            
            
            // Feature Settings required for EHMM Training
            settings.println("WaveDir: "+db.getProp(db.ROOTDIR)+"/"+db.getProp(db.WAVDIR)+" \n"
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
                    +outputDir+"/"+"ehmm"+".align "
                    +outputDir+"/"+"ehmm"+".phoneList 5"
                    +"; perl "+getProp(EHMMDIR)+"/bin/getwavlist.pl "
                    +outputDir+"/"+"ehmm"+".align "
                    +outputDir+"/"+"ehmm"+".waveList"
                    +"; exit )\n");
            
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/phfromutt.pl "
                    +outputDir+"/"+"ehmm"+".align "
                    +outputDir+"/"+"ehmm"+".phoneList 5 > log.txt"
                    +"; perl "+getProp(EHMMDIR)+"/bin/getwavlist.pl "
                    +outputDir+"/"+"ehmm"+".align "
                    +outputDir+"/"+"ehmm"+".waveList >> log.txt"
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
                    +outputDir+"/"+"ehmm"+".featSettings "
                    +outputDir+"/"+"ehmm"+".waveList >> log.txt"  
                    +"; perl "+getProp(EHMMDIR)+"/bin/comp_dcep.pl "
                    +outputDir+"/"+"ehmm"+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat mfcc ft 0 0 >> log.txt "
                  +"; exit )\n");
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; "+getProp(EHMMDIR)+"/bin/FeatureExtraction "
                    +outputDir+"/"+"ehmm"+".featSettings "
                    +outputDir+"/"+"ehmm"+".waveList >> log.txt"  
                    +"; perl "+getProp(EHMMDIR)+"/bin/comp_dcep.pl "
                    +outputDir+"/"+"ehmm"+".waveList "
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
                    +outputDir+"/"+"ehmm"+".waveList "
                    +ehmm.getAbsoluteFile()+"/feat "+ehmm.getAbsolutePath()+"/mod ft 4 >> log.txt"
                    +"; exit )\n");
            pw.print("( cd "+ehmm.getAbsolutePath()
                    +"; perl "+getProp(EHMMDIR)+"/bin/scale_feat.pl "
                    +outputDir+"/"+"ehmm"+".waveList "
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
         
         
         if(getProp(INITEHMMDIR).equals("/")){
         
             
             pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; perl "+getProp(EHMMDIR)+"/bin/seqproc.pl "
                 +outputDir+"/"+"ehmm"+".align "
                 +outputDir+"/"+"ehmm"+".phoneList 2 2 13 >> log.txt"
                 +"; exit )\n");
         }
         else{
             
            
             
             File modelFile = new File(getProp(INITEHMMDIR)+"/mod/model101.txt");
             if (!modelFile.exists()) {
                 throw new IOException("Model file "+modelFile.getAbsolutePath()+" does not exist");
             }
             
             pw.print("( cd "+ehmm.getAbsolutePath()
                     +"; "+"cp "+getProp(INITEHMMDIR)+"/etc/ehmm.phoneList "
                     +outputDir
                     +"; "+"cp "+getProp(INITEHMMDIR)+"/mod/model101.txt "
                     +getProp(EDIR)+"/mod/ "
                     +"; perl "+getProp(EHMMDIR)+"/bin/seqproc.pl "
                     +outputDir+"/"+"ehmm"+".align "
                     +outputDir+"/"+"ehmm"+".phoneList 2 2 13 >> log.txt"
                     +"; exit )\n");
             
         }
         
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
         
         if(getProp(INITEHMMDIR).equals("/")){
             
         System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
         System.out.println("EHMM baum-welch re-estimation ...");
         System.out.println("It may take more time (may be 1 or 2 days) depending on voice database ...");
             
         System.out.println("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                 +outputDir+"/"+"ehmm"+".phoneList.int "
                 +outputDir+"/"+"ehmm"+".align.int 1 0 "
                 +ehmm.getAbsolutePath()+"/feat ft"
                 +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                 +"; exit )\n");
         
         pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                 +outputDir+"/"+"ehmm"+".phoneList.int "
                 +outputDir+"/"+"ehmm"+".align.int 1 0 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                 +"; exit )\n");
         
         }
         else if(getProp(RETRAIN).equals("true")){
             
             System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
             System.out.println("EHMM baum-welch re-estimation ... Re-Training... ");
             System.out.println("It may take more time (may be 1 or 2 days) depending on voice database ...");
               
             System.out.println("( cd "+ehmm.getAbsolutePath()
                     +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                     +outputDir+"/"+"ehmm"+".phoneList.int "
                     +outputDir+"/"+"ehmm"+".align.int 1 1 "
                     +ehmm.getAbsolutePath()+"/feat ft"
                     +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                     +"; exit )\n");
             
             pw.print("( cd "+ehmm.getAbsolutePath()
                     +"; "+getProp(EHMMDIR)+"/bin/ehmm "
                     +outputDir+"/"+"ehmm"+".phoneList.int "
                     +outputDir+"/"+"ehmm"+".align.int 1 1 "
                     +ehmm.getAbsolutePath()+"/feat ft "
                     +ehmm.getAbsolutePath()+"/mod 0 0 0 >> log.txt"
                     +"; exit )\n");  
             
             
         }
         
         pw.flush();
         //shut down
         pw.close();
         process.waitFor();
         process.exitValue();    
         System.out.println(".... Done.");
         
            
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
                 +outputDir+"/"+"ehmm"+".phoneList.int "
                 +outputDir+"/"+"ehmm"+".align.int 1 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +outputDir+"/"+"ehmm"+".featSettings "
                 +ehmm.getAbsolutePath()+"/mod >> log.txt"
                 +"; perl "+getProp(EHMMDIR)+"/bin/sym2nm.pl "
                 +ehmm.getAbsolutePath()+"/lab "
                 +outputDir+"/"+"ehmm"+".phoneList.int >> log.txt"
                 +"; exit )\n");
         
         pw.print("( cd "+ehmm.getAbsolutePath()
                 +"; "+getProp(EHMMDIR)+"/bin/edec "
                 +outputDir+"/"+"ehmm"+".phoneList.int "
                 +outputDir+"/"+"ehmm"+".align.int 1 "
                 +ehmm.getAbsolutePath()+"/feat ft "
                 +outputDir+"/"+"ehmm"+".featSettings "
                 +ehmm.getAbsolutePath()+"/mod >> log.txt"
                 +"; perl "+getProp(EHMMDIR)+"/bin/sym2nm.pl "
                 +ehmm.getAbsolutePath()+"/lab "
                 +outputDir+"/"+"ehmm"+".phoneList.int >> log.txt"
                 +"; exit )\n");
         
         pw.flush();
         //shut down
         pw.close();
         process.waitFor();
         process.exitValue();     
         
     }
     
        /**
         * Create phone sequence file, which is 
         * used for Alignment
         * @throws Exception
         */
     
        private void getPhoneSequence() throws Exception {
                        
            
            // open transcription file used for labeling
            PrintWriter transLabelOut = new PrintWriter(
                    new FileOutputStream (new File(outputDir+"/"+"ehmm"+".align")));
            
            String phoneSeq; 
            
            for (int i=0; i<bnl.getLength(); i++) {
                           
                phoneSeq = getSingleLine(bnl.getName(i));
                transLabelOut.println(phoneSeq.trim());

                //System.out.println( "    " + bnl.getName(i) );
                           
            }
            transLabelOut.flush();
            transLabelOut.close();
            
        }
        
        /**
         * Get phoneme sequence from a single feature file
         * @param basename
         * @return String
         * @throws Exception
         */
        private String getSingleLine(String basename) throws Exception {
            
            String line;
            String featName;
            String phone;
            String phoneSeq;
            int lineCount = 0;
            int featCount = 0;
            boolean featStart = false;
            Matcher matcher;
            
            Pattern pattern; 
            StringBuffer alignBuff = new StringBuffer();
            alignBuff.append(basename);
            
            BufferedReader transIn = new BufferedReader(
                    new InputStreamReader(new FileInputStream(getProp(FEATUREDIR)+"/"+basename+featsExt))); 
           
            for(lineCount = 0 ;(line = transIn.readLine()) != null; lineCount++){
                
                if(line.startsWith("mary_segs_from_word_start")){
                    featCount = lineCount - 1;                 
                }
                
                // to get a line break 
                if(line.equals("")){
                    if(featStart) break;
                    else{
                        featStart = true;
                        continue;
                    }
                }
                
                if(featStart){
                    String[] feats = line.split(" ");
                   
                    
                    // This module for Short SIL insertion 
                    // at each word boundary. 
                    
                   if(locale.startsWith("en")){
                       
                    if(feats[0].equals("_")){
                        alignBuff.append(" _");
                        continue;
                     }

                    if(feats[featCount].equals("0") && (!alignBuff.toString().endsWith("_"))){
                         alignBuff.append(" _");
                         }
                   } 
                     
                    alignBuff.append(" "+feats[0]);
                }           
            }
            
            transIn.close();
            
            /* Use Regular Expressions to change
             * phoneme sequence such that it supports
             * 
             * 1. Start-End Silences and Short silences
             * (at punctuations) differently.
             *   
             * 2. So that Start-End Silences modeling 
             * is different from Short SIL modelling 
             * 
             */
            
            phoneSeq = alignBuff.toString();
            pattern = Pattern.compile(" _");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll(" ssil");
            
            pattern = Pattern.compile(" ssil");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceFirst(" pau");
            
            pattern = Pattern.compile(" ssil$");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceFirst(" pau");
            
            return phoneSeq;
        }
        
        /**
         * Post processing Step to convert Label files
         * to MARY supportable format
         * @throws Exception
         */        
        private void getProperLabelFormat() throws Exception {
            
            
            for (int i=0; i<bnl.getLength(); i++) {
            
                convertSingleLabelFile(bnl.getName(i));               
                System.out.println( "    " + bnl.getName(i) );
                
            }
        }
        
        /**
         * Post Processing single Label file
         * @param basename
         * @throws Exception
         */
        private void convertSingleLabelFile(String basename) throws Exception {
            
            String line;
            String previous, current;
            String regexp = "\\spau|\\sssil";

            //Compile regular expression
            Pattern pattern = Pattern.compile(regexp);

            File labDir = new File(getProp(OUTLABDIR));
            if(!labDir.exists()){
                labDir.mkdir();
            }
            
            PrintWriter labelOut = new PrintWriter(
                    new FileOutputStream (new File(labDir+"/"+basename+labExt)));
            
            
            BufferedReader labelIn = new BufferedReader(
                    new InputStreamReader(new FileInputStream(getProp(EDIR)+"/lab/"+basename+labExt)));
            
            previous = labelIn.readLine();
                                  
            while((line = labelIn.readLine()) != null){

                //Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(line);
                current = matcher.replaceAll(" _");
                
                if(previous.endsWith("_") && current.endsWith("_")){
                    previous = current;
                    continue;
                }
                                             
                labelOut.println(previous);
                previous = current;
                
            }
            
            labelOut.println(previous);
            labelOut.flush();
            labelOut.close();
            labelIn.close();
                        
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
