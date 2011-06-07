/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import marytts.client.MaryClient;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Automatic Labelling using HTK labeller
 * @author Sathish Chandra Pammi
 */

public class HTKLabeler extends VoiceImportComponent {
        
        private DatabaseLayout db;        
        private File rootDir;
        private File htk;
        private String voicename;
        private String outputDir;
        protected String xmlExt = ".xml";
        protected String labExt = ".lab";
        protected MaryClient mary;
        private int progress = -1;
        private String locale;
        protected String maryInputType;
        protected String maryOutputType;
        protected int percent = 0;
        protected File intonisedXMLDir;
        protected Map<String, TreeMap<String, String>> dictionary;
        protected AllophoneSet allophoneSet;
        protected int MAX_ITERATIONS = 15;
        protected int SP_ITERATION = 5;
        protected int noIterCompleted = 0;
        
        
        public final String HTDIR = "HTKLabeler.htDir";
        public final String HTKDIR = "HTKLabeler.htkDir";
        public final String OUTLABDIR = "HTKLabeler.outputLabDir";
        public final String MAXITER = "HTKLabeler.maxNoOfIterations";
        public String INTONISEDDIR =  "HTKLabeler.intonisedXMLDir";
        public String PHONEMEXML = "HTKLabeler.phoneXMLFile";
        public String SPITER  = "HTKLabeler.shortPauseIteration";
        
        
        public final String getName(){
            return "HTKLabeler";
        }
        
       public SortedMap getDefaultProps(DatabaseLayout db){
           this.db = db;
           if (props == null){
               props = new TreeMap();
               String htkdir = System.getProperty("HTKDIR");
               String phoneXml;
               locale = db.getProp(db.LOCALE);
               if ( htkdir == null ) {
                   htkdir = "/project/mary/htk/";
               }
               props.put(HTKDIR,htkdir);
               
               props.put(HTDIR,db.getProp(db.ROOTDIR)
                       +System.getProperty("file.separator")
                       +"htk"
                       +System.getProperty("file.separator"));
               props.put(INTONISEDDIR, db.getProp(db.ROOTDIR)
                       +System.getProperty("file.separator")
                       +"intonisedXML"
                       +System.getProperty("file.separator"));
               
               if(locale.startsWith("de")){
                   phoneXml = db.getProp(db.MARYBASE)
                           +File.separator+"lib"+File.separator+"modules"
                           +File.separator+"de"+File.separator+"cap"+File.separator+"phone-list-de.xml";
               }
               else{
                   phoneXml = db.getProp(db.MARYBASE)
                           +File.separator+"lib"+File.separator+"modules"
                           +File.separator+"en"+File.separator+"cap"+File.separator+"phone-list-en.xml";
               }
               props.put(PHONEMEXML, phoneXml);
               
               props.put(OUTLABDIR, db.getProp(db.ROOTDIR)
                       +"lab"
                       +System.getProperty("file.separator"));
               props.put(MAXITER,"20");
               props.put(SPITER,"5");
              
               
           }
           return props;
       }
       
       protected void setupHelp(){
           props2Help = new TreeMap();
           props2Help.put(HTKDIR,"directory containing the local installation of HTK Labeller"); 
           props2Help.put(HTDIR,"directory containing all files used for training and labeling. Will be created if it does not exist.");
           props2Help.put(INTONISEDDIR, "directory containing the acoustic params.");
           props2Help.put(PHONEMEXML, "Phone XML file for given language.");
           props2Help.put(OUTLABDIR, "Directory to store generated lebels from HTK.");
           //props2Help.put(INITHTKDIR,"If you provide a path to previous HTK Directory, Models will intialize with those models. other wise HTK Models will build with Flat-Start Initialization");
           //props2Help.put(RETRAIN,"true - Do re-training by initializing with given models. false - Do just Decoding");
           props2Help.put(MAXITER,"Maximum number of iterations used for training");
           props2Help.put(SPITER,"Iteration number at which short-pause model need to insert.");
           
       }
        
        public void initialiseComp() 
        {
           
           dictionary = new TreeMap<String, TreeMap<String,String>>();
           
           intonisedXMLDir = new File(getProp(INTONISEDDIR));
           if (!intonisedXMLDir.exists()){
               System.out.print(INTONISEDDIR+" "+getProp(INTONISEDDIR)
                       +" does not exist; ");
               if (!intonisedXMLDir.mkdir()){
                   throw new Error("Could not create INTONISEDDIR");
               }
               System.out.print("Created successfully.\n");
           } 
           
        }
        
        /**
         * Do the computations required by this component.
         * 
         * @return true on success, false on failure
         */
        public boolean compute() throws Exception{
            
            File htkFile = new File(getProp(HTKDIR)+File.separator+"bin"+File.separator+"HInit");
            if (!htkFile.exists()) {
                throw new IOException("HTK path setting is wrong. Because file "+htkFile.getAbsolutePath()+" does not exist");
            }
            
            MAX_ITERATIONS = Integer.valueOf((getProp(MAXITER)));
            SP_ITERATION   = Integer.valueOf((getProp(SPITER)));
   
            System.out.println("Preparing voice database for labelling using HTK :");
            //get the voicename        
            voicename = db.getProp(db.VOICENAME);
            //make new directories htk and etc
            htk = new File(getProp(HTDIR));
            // get the output directory of files used by HTK 
            outputDir = htk.getAbsolutePath()+"/etc";
            allophoneSet = AllophoneSet.getAllophoneSet(getProp(PHONEMEXML));
            File dictFile = new File(db.getProp(db.ROOTDIR)+File.separator+"htk"+File.separator+"etc"+File.separator+"htk.dict");
            
            // part 1: HTK basic setup and create required files
            
            // setup the HTK directory 
            System.out.println("Setting up HTK directory ...");
            setup();
            System.out.println(" ... done.");
            // create required files for HTK
            createRequiredFiles();
            // creating phone dictionary. phone to phone mapping
            createPhoneDictionary();
            // Extract phone sequence from intonisedXML files
            getPhoneSequence();
            
            //part 2: Feature Extraction using HCopy
            System.out.println("Feature Extraction:");
            featureExtraction();
            System.out.println("... Done.");
            
            
            //Part 3:  Initilize Flat-start initialisation
            System.out.println("HTK Training:");
            initialiseHTKTrain();
            createTrainFile();
            
            //Part 4: training with HERest 
            herestTraining();
            System.out.println("... Done.");
            
            //Part 5: Force align with HVite 
            System.out.println("HTK Align:");
            hviteAligning();
            System.out.println("... Done.");
            
            htkExtraModels();
            
            System.out.println("Generating Labels in required format...");
            getProperLabelFormat();
            System.out.println(" ... done.");
            System.out.println("Label file Generation Successfully completed using HTK !"); 
         
            
            return true;
        }
        
        
       /**
        * Setup the HTK directory
        * @throws IOException, InterruptedException
        */
        private void setup() throws IOException,InterruptedException{
            
            htk.mkdir();
            File lab = new File(htk.getAbsolutePath()+"/lab");
            //call setup of HTK in this directory
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to htk directory and setup Directory Structure 
            pw.print("( cd "+htk.getAbsolutePath()
                    +"; mkdir hmm"
                    +"; mkdir etc"
                    +"; mkdir feat"
                    +"; mkdir config"
                    +"; mkdir lab"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
        }
       

        
        /**
         * Creating phone dictionary (one-one mapping)
         * @throws Exception
         */
        private void createPhoneDictionary() throws Exception{
            
            //System.out.println( "Computing AcousticParams for " + bnl.getLength() + " files" );
            PrintWriter transLabelOut = new PrintWriter(
                    new FileOutputStream (new File(getProp(HTDIR)+File.separator
                            +"etc"+File.separator
                            +"htk"+".phone.dict")));
            PrintWriter phoneListOut = new PrintWriter(
                    new FileOutputStream (new File(getProp(HTDIR)+File.separator
                            +"etc"+File.separator
                            +"htk"+".phone.list")));
            PrintWriter phoneListOut1 = new PrintWriter(
                    new FileOutputStream (new File(getProp(HTDIR)+File.separator
                            +"etc"+File.separator
                            +"htk"+".phone2.list")));
            String phoneSeq; 
            //transLabelOut.println("#!MLF!#");
            Set<String> phonesList = allophoneSet.getAllophoneNames();
            Iterator<String> it = phonesList.iterator();
            while(it.hasNext()){
                //System.out.println(it.next());
                String phon = it.next();
               
                if( phon.equals("_")){
                    continue;
                    //phon = "sp";
                }
                phon = replaceTrickyPhones(phon);
                 transLabelOut.println(phon+" "+phon);
                phoneListOut.println(phon);
                phoneListOut1.println(phon);
            }
            transLabelOut.println("sil"+" "+"sil");
            phoneListOut.println("sil");
            phoneListOut1.println("sil");
            transLabelOut.println("ssil"+" "+"ssil");
            phoneListOut1.println("ssil");
            transLabelOut.flush();
            transLabelOut.close();
            phoneListOut.flush();
            phoneListOut.close();
            phoneListOut1.flush();
            phoneListOut1.close();
            
        }
        
      
        
        /**
         * Create all required files(config files and HMM prototypes) for HTK Training 
         * @throws Exception
         */
        private void createRequiredFiles() throws Exception{
            
            // Creating mkphones0.led file, which insert and delete pauses 
            File file = new File(getProp(HTDIR)+File.separator+"config"+File.separator+"mkphone0.led");
            PrintWriter pw = new PrintWriter(new FileWriter(file));
            pw.println("EX");
            pw.println("IS sil sil");
            //pw.println("DE sp"); // Short pause modelling
            pw.flush();
            pw.close();
            
            // creating a HTK Feature Extraction config file
            file = new File(getProp(HTDIR)+File.separator+"config"+File.separator+"featEx.conf");
            pw = new PrintWriter(new FileWriter(file));
            pw.println("SOURCEFORMAT = WAV             # Gives the format of speech files ");
            pw.println("TARGETKIND = MFCC_0        #Identifier of the coefficients to use");
            pw.println("WINDOWSIZE = 100000.0         # = 10 ms = length of a time frame");
            pw.println("TARGETRATE = 50000.0         # = 5 ms = frame periodicity");
            pw.println("NUMCEPS = 12                  # Number of MFCC coeffs (here from c1 to c12)");
            pw.println("USEHAMMING = T                # Use of Hamming funtion for windowing frames");
            pw.println("PREEMCOEF = 0.97              # Pre-emphasis coefficient");
            pw.println("NUMCHANS = 26                 # Number of filterbank channels");
            pw.println("CEPFILTER = 22                # Length of ceptral filtering");

            pw.flush();
            pw.close();
            
            
            //creating a HTK Training initialise config file
            file = new File(getProp(HTDIR)+File.separator+"config"+File.separator+"htkTrain.conf");
            pw = new PrintWriter(new FileWriter(file));
            
            pw.println("TARGETKIND = MFCC_0        #Identifier of the coefficients to use");
            pw.println("PARAMETERKIND = MFCC_0");
            pw.println("WINDOWSIZE = 100000.0         # = 10 ms = length of a time frame");
            pw.println("TARGETRATE = 50000.0         # = 5 ms = frame periodicity");
            pw.println("NUMCEPS = 12                  # Number of MFCC coeffs (here from c1 to c12)");
            pw.println("USEHAMMING = T                # Use of Hamming funtion for windowing frames");
            pw.println("PREEMCOEF = 0.97              # Pre-emphasis coefficient");
            pw.println("NUMCHANS = 26                 # Number of filterbank channels");
            pw.println("CEPFILTER = 22                # Length of ceptral filtering");

            pw.flush();
            pw.close(); 
            
            // Create an input file to HTK for Feature Extraction
            file = new File(getProp(HTDIR)+File.separator+"etc"+File.separator+"featEx.list");
            pw = new PrintWriter(new FileWriter(file));
            for (int i=0; i<bnl.getLength(); i++) {
                //System.out.println( "    " + bnl.getName(i) );
                String input = db.getProp(db.ROOTDIR)+File.separator
                            +db.getProp(db.WAVDIR)+File.separator+bnl.getName(i)
                            +db.getProp(db.WAVEXT);
                String output = getProp(HTDIR)+File.separator+"feat"+File.separator+bnl.getName(i)+".mfcc";
                pw.println(input+" "+output);
            }
            pw.flush();
            pw.close();
            
            // creating list of training files
            file = new File(getProp(HTDIR)+File.separator+"etc"+File.separator+"htkTrain.list");
            pw = new PrintWriter(new FileWriter(file));
            for (int i=0; i<bnl.getLength(); i++) {
                //System.out.println( "    " + bnl.getName(i) );
                String mFile = getProp(HTDIR)+File.separator+"feat"+File.separator+bnl.getName(i)+".mfcc";
                pw.println(mFile);
            }
            pw.flush();
            pw.close();
            
            //creating a hmm protofile
            int vectorSize = 13;
            int numStates = 5;
            file = new File(getProp(HTDIR)+File.separator+"config"+File.separator+"htk.proto");
            pw = new PrintWriter(new FileWriter(file));
            pw.println("<BeginHMM>");
            pw.println("<NumStates> "+numStates+" <VecSize> "+vectorSize+" <MFCC_0>");
            for(int i=2;i<numStates;i++){
                pw.println("<State> "+i);
                pw.println("<Mean> "+vectorSize);
                for(int j=0;j<vectorSize;j++){
                    pw.print(" 0.0");
                }
                pw.println();
                pw.println("<Variance> "+vectorSize);
                for(int j=0;j<vectorSize;j++){
                    pw.print(" 1.0");
                }
                pw.println();
                
            }
            pw.println("<TransP> "+numStates);
            pw.println("0.0 1.0 0.0 0.0 0.0");
            pw.println("0.0 0.6 0.4 0.0 0.0");
            pw.println("0.0 0.0 0.6 0.4 0.0");
            pw.println("0.0 0.0 0.0 0.7 0.3");
            pw.println("0.0 0.0 0.0 0.0 1.0");
            pw.println("<EndHMM>");
            pw.flush();
            pw.close();
            
            // Creating Silence modeling config file
            
//          creating a hmm protofile
           
            file = new File(getProp(HTDIR)+File.separator+"config"+File.separator+"sil.hed");
            pw = new PrintWriter(new FileWriter(file));
            
            pw.println("AT 2 4 0.2 {sil.transP}\n");
            pw.println("AT 4 2 0.2 {sil.transP}\n");
            //pw.println("AT 1 3 0.3 {ssil.transP}\n");
            //pw.println("TI silst {sil.state[3],ssil.state[2]}\n");
            pw.println("AT 2 4 0.2 {ssil.transP}\n");
            pw.println("AT 4 2 0.2 {ssil.transP}\n");
            
            pw.flush();
            pw.close();
            
        }
        
        /**
         * create phone master label file
         * @throws Exception
         */
        private void createPhoneMLFile() throws Exception{
            String hled = getProp(HTKDIR)+File.separator
                    +"bin"+File.separator+"HLEd";
            File htkFile = new File(hled);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String dict = getProp(HTDIR)+File.separator
                    +"etc"+File.separator+"htk.dict";
            String phoneMLF = getProp(HTDIR)+File.separator
                    +"etc"+File.separator+"htk.phones.mlf";
            String wordsMLF = getProp(HTDIR)+File.separator
                    +"etc"+File.separator+"htk.words.mlf";
            String mkphoneLED = getProp(HTDIR)+File.separator
                    +"config"+File.separator+"mkphone0.led";
                
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hled+" -l '*' -d "+dict+" -i "
                    +phoneMLF+" "+mkphoneLED+" "+wordsMLF
                    +"; exit )\n");
            
            pw.print("( "
                    +hled+" -l '*' -d "+dict+" -i "
                    +phoneMLF+" "+mkphoneLED+" "+wordsMLF
                    //+"; "
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
        }
        
        /**
         * Feature Extraction for HTK Training 
         * @throws Exception
         */
        private void featureExtraction() throws Exception {
            
            String hcopy = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HCopy";
            File htkFile = new File(hcopy);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"featEx.conf";
            String listFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"featEx.list";
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hcopy+" -T 1 -C "+configFile+" -S "+listFile
                    +"; exit )\n");
            pw.print("( "
                    +hcopy+" -T 1 -C "+configFile+" -S "+listFile+" > log.txt"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
        }
        
        /**
         * Initialize HTK Training process
         * @throws Exception
         */
        private void initialiseHTKTrain() throws Exception{
            
            String hcompv = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HCompV";
            File htkFile = new File(hcompv);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"htkTrain.conf";
            String listFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htkTrain.list";
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            
            System.out.println("( cd "+getProp(HTDIR)
                    +" ; mkdir hmm/hmm-dummy ; "
                    +" mkdir hmm/hmm-final ; "
                    +hcompv+" -C "+configFile+" -f 0.01 -m -S "+listFile
                    +" -M "+getProp(HTDIR)+File.separator+"hmm/hmm-dummy "
                    +getProp(HTDIR)+File.separator+"config"+File.separator+"htk.proto"+" > log.txt"
                    +"; exit )\n");
            pw.print("( cd "+getProp(HTDIR)
                    +" ; mkdir hmm/hmm-dummy ; "
                    +" mkdir hmm/hmm-final ; "
                    +hcompv+" -C "+configFile+" -f 0.01 -m -S "+listFile
                    +" -M "+getProp(HTDIR)+File.separator+"hmm/hmm-dummy "
                    +getProp(HTDIR)+File.separator+"config"+File.separator+"htk.proto"+" > log.txt"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
            
        }
        
        /**
         * Create HMMs for each phone from Global HMMs 
         * @throws Exception
         */
        private void createTrainFile() throws Exception {
            
            String script;
            String hmmDir = getProp(HTDIR)+File.separator
            +"hmm"+File.separator;
            
            /**TODO:
             * Replace below 'gawk' script with Java method.
             */
            
            script = "mkdir hmm/hmm0\n"
                +"head -3 hmm/hmm-dummy/htk > hmm/hmm0/hmmdefs\n"
                +"for s in `cat etc/htk.phone.list`\n"
                +"do\n"
                +"echo \"~h \\\"$s\\\"\" >> hmm/hmm0/hmmdefs\n"
                +"gawk '/BEGINHMM/,/ENDHMM/ { print $0 }' hmm/hmm-dummy/htk >> hmm/hmm0/hmmdefs\n"
                +"done\n";
            // creating list of training files
            File file = new File(getProp(HTDIR)+File.separator+"etc"+File.separator+"htkTrainScript.sh");
            PrintWriter pw = new PrintWriter(new FileWriter(file));
            pw.println(script);
            pw.flush();
            pw.close();
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            
            System.out.println("( cd "+getProp(HTDIR)
                    +"; sh etc"+File.separator+"htkTrainScript.sh"
                    +"; exit )\n");
            pw.print("( cd "+getProp(HTDIR)
                    +"; sh etc"+File.separator+"htkTrainScript.sh"
                    +"; exit )\n");
            
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
            PrintWriter macroFile = new PrintWriter(
                    new FileOutputStream (new File(hmmDir+"hmm0"+File.separator+"macros")));
            macroFile.println("~o\n"+"<VecSize> 13\n"+"<MFCC_0>");
            macroFile.println(FileUtils.getFileAsString(new File(hmmDir+"hmm-dummy"+File.separator+"vFloors"), "ASCII"));
            macroFile.flush();
            macroFile.close();
            
        }
        
        /**
         * Flat-start initialization for automatic labeling
         * @throws Exception
         */
        private void herestTraining() throws Exception{
            
            String herest = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HERest";
            String hhed = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HHEd";
            
            File htkFile = new File(herest);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"htkTrain.conf";
            String hhedconf = getProp(HTDIR)+File.separator
            +"config"+File.separator+"sil.hed";
            String trainList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htkTrain.list";
            String phoneList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.list";
            
            String hmmDir = getProp(HTDIR)+File.separator
            +"hmm"+File.separator;
            String phoneMlf = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.mlf";
            
            
            
           for(int i=1;i<=MAX_ITERATIONS;i++){
                
               System.out.println("Iteration number: "+i);
                
               File hmmItDir = new File(hmmDir+"hmm"+i);
               if(!hmmItDir.exists()) hmmItDir.mkdir();
                
                
               Runtime rtime = Runtime.getRuntime();
                //get a shell
               Process process = rtime.exec("/bin/bash");
                //get an output stream to write to the shell
               PrintWriter pw = new PrintWriter(
                       new OutputStreamWriter(process.getOutputStream()));
               
               if(i == SP_ITERATION){
                   insertShortPause(i);
                   String oldMacro = hmmDir+"hmm"+(i-1)+File.separator+"macros";
                   String newMacro = hmmDir+"hmm"+i+File.separator+"macros";
                   FileUtils.copy(oldMacro,newMacro);
                   continue;
               }
               
               if(i == (SP_ITERATION+1)){
                   
                   phoneMlf = getProp(HTDIR)+File.separator
                           +"etc"+File.separator+"htk.phones2.mlf";
                   phoneList = getProp(HTDIR)+File.separator
                           +"etc"+File.separator+"htk.phone2.list";
                   
                   System.out.println("( "
                           +hhed+" -H "+hmmDir+"hmm"+(i-1)+File.separator+"macros"
                           +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"
                           +" -M "+hmmDir+"hmm"+i+" "+hhedconf+" "+phoneList
                           +"; exit )\n");
                   pw.println("( "
                           +hhed+" -H "+hmmDir+"hmm"+(i-1)+File.separator+"macros"
                           +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"
                           +" -M "+hmmDir+"hmm"+i+" "+hhedconf+" "+phoneList
                           +"; exit )\n");
                   pw.flush();
                   //shut down
                   pw.close();
                   process.waitFor();
                   process.exitValue();
                   continue;
               }
                
              System.out.println("( "
                        +herest+" -C "+configFile+" -I "+phoneMlf
                        +" -t 250.0 150.0 1000.0"
                        +" -S "+trainList
                        +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"macros"
                        +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"+" -M "+hmmDir+"hmm"+i+" "+phoneList
                        +"; exit )\n");
              
              pw.println("( "
                        +herest+" -C "+configFile+" -I "+phoneMlf
                        +" -t 250.0 150.0 1000.0"
                        +" -S "+trainList
                        +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"macros"
                        +" -H "+hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"+" -M "+hmmDir+"hmm"+i+" "+phoneList
                        +"; exit )\n");
              pw.flush();
              //shut down
              pw.close();
              process.waitFor();
              process.exitValue();
            }
           String oldMacro = hmmDir+"hmm"+MAX_ITERATIONS+File.separator+"macros";
           String newMacro = hmmDir+"hmm-final"+File.separator+"macros";
           FileUtils.copy(oldMacro,newMacro);
           
           String oldHmmdefs = hmmDir+"hmm"+MAX_ITERATIONS+File.separator+"hmmdefs";
           String newHmmdefs = hmmDir+"hmm-final"+File.separator+"hmmdefs";
           FileUtils.copy(oldHmmdefs, newHmmdefs);
           
       }
          
 
        private void insertShortPause(int i) throws Exception{
            String hmmDir = getProp(HTDIR)+File.separator
            +"hmm"+File.separator;
            boolean okprint = false;
            boolean silprint = false;
            
            String line, spHmmDef="";
            // File hmmDef = new File(hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs");
            BufferedReader hmmDef
                    = new BufferedReader(
                            new FileReader(
                                    hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"));
            while((line = hmmDef.readLine()) != null){
                
                if(line.matches("^.*\"sil\".*$")){
                    okprint = true;
                    spHmmDef +=  "~h \"ssil\"\n";
                    continue;
                }
                
                if (okprint && line.matches("^.*ENDHMM.*$")){
                    spHmmDef += line+"\n";
                    continue;
                }
                
                if (okprint) {
                    spHmmDef += line+"\n";
                 }
            }
            hmmDef.close();
            
            hmmDef = new BufferedReader(
                     new FileReader(
                            hmmDir+"hmm"+(i-1)+File.separator+"hmmdefs"));
            PrintWriter newHmmDef = new PrintWriter(
                     new FileWriter(hmmDir+"hmm"+i+File.separator+"hmmdefs"));
                        
            while((line = hmmDef.readLine()) != null){
                newHmmDef.println(line.trim());
            }
            newHmmDef.println(spHmmDef);
            newHmmDef.flush();
            newHmmDef.close();
            hmmDef.close();
            
            
        }
        
        
        /**
         * Force Align database for Automatic labels 
         * @throws Exception
         */
        private void  hviteAligning() throws Exception{
            
            String hvite = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HVite";
            File htkFile = new File(hvite);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"htkTrain.conf";
            String listFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htkTrain.list";
            String phoneList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone2.list";
            String hmmDef = getProp(HTDIR)+File.separator
                +"hmm"+File.separator
                +"hmm-final"+File.separator+"hmmdefs";
            String macros = getProp(HTDIR)+File.separator
                +"hmm"+File.separator
                +"hmm-final"+File.separator+"macros";
            String phoneMlf = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones2.mlf";
            String alignedMlf = getProp(HTDIR)+File.separator
                +"aligned.mlf";
            String phoneDict = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.dict";
            String labDir = getProp(HTDIR)+File.separator+"lab";
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hvite+" -b sil -l "+labDir+" -o W -C "+configFile
                    +" -a -H "+macros+" -H "+hmmDef+" -i "+alignedMlf+" -m -t 250.0 -y lab" 
                    +" -I "+phoneMlf+" -S "+listFile
                    +" "+phoneDict+" "+phoneList
                    +"; exit )\n");
            
            pw.println("( "
                    +hvite+" -b sil -l "+labDir+" -o W -C "+configFile
                    +" -a -H "+macros+" -H "+hmmDef+" -i "+alignedMlf+" -m -t 250.0 -y lab" 
                    +" -I "+phoneMlf+" -S "+listFile
                    +" "+phoneDict+" "+phoneList
                    +"; exit )\n");
            
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
            
        }
        
        
      private void  htkExtraModels() throws Exception{
            
            String hlstats = getProp(HTKDIR)+File.separator
                +"bin"+File.separator+"HLStats";
            String hbuild = getProp(HTKDIR)+File.separator
                +"bin"+File.separator+"HBuild";
            
            File htkFile = new File(hlstats);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"htkTrain.conf";
            String bigFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.big";
            String phoneList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.list";
            String phoneMlf = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.mlf";
            String phoneDict = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.dict";
            String phoneAugDict = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.aug.phone.dict";
            String phoneAugList = getProp(HTDIR)+File.separator
            +"etc"+File.separator+"htk.aug.phone.list";
        
            String netFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.net";
    
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hlstats+" -T 1 -C "+configFile+" -b "+bigFile
                    +" -o "+phoneList+" "+phoneMlf
                    +"; exit )\n");
            
            pw.println("( "
                    +hlstats+" -T 1 -C "+configFile+" -b "+bigFile
                    +" -o "+phoneList+" "+phoneMlf
                    +"; exit )\n");
            
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
            String fileDict = FileUtils.getFileAsString(new File(phoneDict), "ASCII");
            PrintWriter augPhoneDict = new PrintWriter(new FileWriter(phoneAugDict));
            augPhoneDict.println("!ENTER sil");
            augPhoneDict.print(fileDict);
            augPhoneDict.println("!EXIT sil");
            augPhoneDict.flush();
            augPhoneDict.close();
            
            String fileList = FileUtils.getFileAsString(new File(phoneList), "ASCII");
            PrintWriter augPhoneList = new PrintWriter(new FileWriter(phoneAugList));
            augPhoneList.println("!ENTER");
            augPhoneList.print(fileList);
            augPhoneList.println("!EXIT");
            augPhoneList.flush();
            augPhoneList.close();
            
            
            rtime = Runtime.getRuntime();
            //get a shell
            process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hbuild+" -T 1 -C "+configFile+" -n "+bigFile
                    +" "+phoneAugList+" "+netFile
                    +"; exit )\n");
            
            pw.println("( "
                    +hbuild+" -T 1 -C "+configFile+" -n "+bigFile
                    +" "+phoneAugList+" "+netFile
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
                    new FileOutputStream (new File(outputDir+"/"+"htk.phones.mlf")));
            PrintWriter transLabelOut1 = new PrintWriter(
                    new FileOutputStream (new File(outputDir+"/"+"htk.phones2.mlf")));
            
            String phoneSeq; 
            transLabelOut.println("#!MLF!#");
            transLabelOut1.println("#!MLF!#");
            for (int i=0; i<bnl.getLength(); i++) {
                transLabelOut.println("\"*/"+bnl.getName(i)+labExt+"\"");
                transLabelOut1.println("\"*/"+bnl.getName(i)+labExt+"\"");
                //phoneSeq = getSingleLine(bnl.getName(i));
                phoneSeq = getLineFromXML(bnl.getName(i), false);
                transLabelOut.println(phoneSeq.trim());
                phoneSeq = getLineFromXML(bnl.getName(i), true);
                transLabelOut1.println(phoneSeq.trim());

                //System.out.println( "    " + bnl.getName(i) );
                           
            }
            transLabelOut.flush();
            transLabelOut.close();
            transLabelOut1.flush();
            transLabelOut1.close();
            
        }
        
        
        /**
         * Get phone sequence from a single feature file
         * @param basename
         * @return String
         * @throws Exception
         */
        private String getLineFromXML(String basename, boolean spause) throws Exception {
            
            String line;
            String phoneSeq;
            Matcher matcher;
            Pattern pattern;
            StringBuilder alignBuff = new StringBuilder();
            //alignBuff.append(basename);
            DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder  = factory.newDocumentBuilder();
            Document doc = builder.parse( new File( getProp(INTONISEDDIR)+"/"+basename+xmlExt ) );
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList tokens = (NodeList) xpath.evaluate("//t | //boundary", doc, XPathConstants.NODESET);
            
            alignBuff.append(collectTranscription(tokens));
            phoneSeq = alignBuff.toString();
            pattern = Pattern.compile("pau ssil ");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("sil ");
            
            pattern = Pattern.compile(" ssil pau$");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll(" sil");
            
            /* TODO: Extra code need to write
             * to maintain minimum number of short sil.
             * or consider word boundaries as ssil.
             */ 
            pattern = Pattern.compile("vssil");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll(" ");
            
            // checking
            if(!spause){
                pattern = Pattern.compile("ssil");
                matcher = pattern.matcher(phoneSeq);
                phoneSeq = matcher.replaceAll("");
            }
            
            phoneSeq += " .";
            
            pattern = Pattern.compile("\\s+");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("\n");
            
            
            return phoneSeq;
        }
        
        /**
         * 
         * This computes a string of phonetic symbols out of an intonised mary xml:
         * - standard phones are taken from "ph" attribute
         * @param tokens
         * @return
         */
        private String collectTranscription(NodeList tokens) {

            // TODO: make delims argument
            // String Tokenizer devides transcriptions into syllables
            // syllable delimiters and stress symbols are retained
            String delims = "',-";
            
            // String storing the original transcription begins with a pause
            String orig =  " pau " ;
            
            // get original phone String
            for (int tNr = 0; tNr < tokens.getLength() ; tNr++ ){
                
                Element token = (Element) tokens.item(tNr);
                
                // only look at it if there is a sampa to change
                if ( token.hasAttribute("ph") ){                   
                    
                    String sampa = token.getAttribute("ph");
        
                    List<String> sylsAndDelims = new ArrayList<String>();
                    StringTokenizer sTok = new StringTokenizer(sampa, delims, true);
                    
                    while(sTok.hasMoreElements()){
                        String currTok = sTok.nextToken();
                        
                        if (delims.indexOf(currTok) == -1) {
                            // current Token is no delimiter
                            for (Allophone ph : allophoneSet.splitIntoAllophones(currTok)){
                                // orig += ph.name() + " ";
                                if(ph.name().trim().equals("_")) continue;
                                orig += replaceTrickyPhones(ph.name().trim()) + " "; 
                            }// ... for each phone
                        }// ... if no delimiter
                    }// ... while there are more tokens    
                }
                    
                // TODO: simplify
                if ( token.getTagName().equals("t") ){
                                    
                    // if the following element is no boundary, insert a non-pause delimiter
                    if (tNr == tokens.getLength()-1 || 
                        !((Element) tokens.item(tNr+1)).getTagName().equals("boundary") ){
                            orig += "vssil "; // word boundary
                            
                        }
                                                           
                } else if ( token.getTagName().equals("boundary")){
                                    
                        orig += "ssil "; // phrase boundary

                } else {
                    // should be "t" or "boundary" elements
                    assert(false);
                }
                            
            }// ... for each t-Element
            orig += "pau";
            return orig;
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
            
            File labelFile = new File(getProp(HTDIR)+File.separator
            +"lab"+File.separator+basename+labExt);
            if(!labelFile.exists()){
                System.err.println("WARNING: "+basename+" label file not created with HTK.");
                return;
            }
            
            BufferedReader labelIn = new BufferedReader(
                    new InputStreamReader(new FileInputStream(labelFile)));
            
            
            PrintWriter labelOut = new PrintWriter(
                    new FileOutputStream (new File(labDir+"/"+basename+labExt)));
            
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
        * To convert HTK Label format to MARY lab format
        * @throws Exception
        */ 
       private void getProperLabelFormat() throws Exception{
           String alignedMlf = getProp(HTDIR)+File.separator
               +"aligned.mlf";
           BufferedReader htkLab = new  BufferedReader(new FileReader(alignedMlf));
           //File labDir = new File(getProp(OUTLABDIR));
           File labDir = new File(getProp(HTDIR)+File.separator+"lab");
           if(!labDir.exists()) labDir.mkdir();
           
           String header = htkLab.readLine().trim();
           if(!header.equals("#!MLF!#")){
               System.err.println("Header format not supported");
               throw new RuntimeException("Header format not supported");
           }
           String line;
           while((line=htkLab.readLine()) != null){
               line = line.trim();
               String fileName = line.substring(1, line.length()-1);
               //line.replaceAll("\"", "");
               //System.err.println("LINE: "+fileName);
               
               PrintWriter pw = new PrintWriter(new FileWriter(fileName));
               pw.println("#");
               while(true){
                   String nline = htkLab.readLine().trim();
                   if(nline.equals(".")) break;
                   StringTokenizer st = new StringTokenizer(nline);
                   st.nextToken();
                   Double tStamp = Double.parseDouble(st.nextToken().trim());
                   String phoneSeg = replaceBackTrickyPhones(st.nextToken().trim());
                   if(phoneSeg.equals("sil") || phoneSeg.equals("ssil"))
                       phoneSeg = "_";
                   pw.println(tStamp/10000000 + " 125 " + phoneSeg);
               }
               
               pw.flush();
               pw.close();
               
           }
           
           for (int i=0; i<bnl.getLength(); i++) {
               
               convertSingleLabelFile(bnl.getName(i));               
               //System.out.println( "    " + bnl.getName(i) );
               
           }
       }
        
        /**
         * Converting text to RAWMARYXML with Locale
         * @param locale
         * @return
         */
        public static String getMaryXMLHeaderWithInitialBoundary(String locale)
        {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<maryxml version=\"0.4\"\n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
                "xml:lang=\"" + locale + "\">\n" +
                "<boundary duration=\"100\"/>\n";
            
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

        
        /** Translation table for labels which are incompatible with HTK or shell filenames
         * See common_routines.pl in HTS training.
         * @param lab
         * @return String
         */
        public String replaceTrickyPhones(String lab){
          String s = lab;
          
          /** the replace is done for the labels: phone, prev_phone and next_phone */
          
          /** DE (replacements in German phone set) */     
          if(lab.contentEquals("6") )
            s = "ER6";
          else if (lab.contentEquals("=6") )
            s = "ER66";
          else if (lab.contentEquals("2:") )
              s = "EU22";
          else if (lab.contentEquals("2") )
              s = "EU2";
          else if (lab.contentEquals("9") )
              s = "EU9";
          else if (lab.contentEquals("9~") )
              s = "UM9";
          else if (lab.contentEquals("e~") )
              s = "IMe";
          else if (lab.contentEquals("a~") )
              s = "ANa";
          else if (lab.contentEquals("o~") )
              s = "ONo";
          else if (lab.contentEquals("?") )
              s = "gstop";
          /** EN (replacements in English phone set) */
          else if (lab.contentEquals("r=") )
              s = "rr"; 
          
          return s;
            
        }
        
        
        /** Translation table for labels which are incompatible with HTK or shell filenames
         * See common_routines.pl in HTS training.
         * In this function the phones as used internally in HTSEngine are changed
         * back to the Mary TTS set, this function is necessary when correcting the 
         * actual durations of AcousticPhonemes.
         * @param lab
         * @return String
         */
        public String replaceBackTrickyPhones(String lab){
          String s = lab;
          /** DE (replacements in German phone set) */     
          if(lab.contentEquals("ER6") )
            s = "6";
          else if (lab.contentEquals("ER66") )   /* CHECK ??? */
            s = "=6";
          else if (lab.contentEquals("EU2") )
              s = "2";
          else if (lab.contentEquals("EU22") )
              s = "2:";
          else if (lab.contentEquals("EU9") )
              s = "9";
          else if (lab.contentEquals("UM9") )
              s = "9~";
          else if (lab.contentEquals("IMe") )
              s = "e~";
          else if (lab.contentEquals("ANa") )
              s = "a~";
          else if (lab.contentEquals("ONo") )
              s = "o~";
          else if (lab.contentEquals("gstop") )
              s = "?";
          /** EN (replacements in English phone set) */
          else if (lab.contentEquals("rr") )
              s = "r="; 
          
          //System.out.println("LAB=" + s);
          
          return s;
            
        }
        
        
}

