package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * HTK Phone Recogniser
 * @author Sathish Chandra Pammi
 */

public class HTKPhoneRecogniser extends VoiceImportComponent {
        
        private DatabaseLayout db;        
        private File rootDir;
        private File htkTest;
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
        private BasenameList bnlist;
        
        public final String HTDIR = "HTKPhoneRecogniser.htDir";
        public final String HTKDIR = "HTKPhoneRecogniser.htkDir";
        public final String OUTLABDIR = "HTKPhoneRecogniser.outputLabDir";
        public final String MAXITER = "HTKPhoneRecogniser.maxNoOfIterations";
        public String INTONISEDDIR =  "HTKPhoneRecogniser.intonisedXMLDir";
        public String PHONEMEXML = "HTKPhoneRecogniser.phonemeXMLFile";
        public String SPITER  = "HTKPhoneRecogniser.shortPauseIteration";
        public String HTKTESTDIR = "HTKPhoneRecogniser.htkTestDirectory";
        public String HTKTESTWAV = "HTKPhoneRecogniser.htkTestWaveDirectory";
        
        public final String getName(){
            return "HTKPhoneRecogniser";
        }
        
       public SortedMap getDefaultProps(DatabaseLayout db){
           this.db = db;
           if (props == null){
               props = new TreeMap();
               String htkdir = System.getProperty("HTKDIR");
               String phonemeXml;
               locale = db.getProp(db.LOCALE);
               if ( htkdir == null ) {
                   htkdir = "/project/mary/htk/";
               }
               props.put(HTKDIR,htkdir);
               
               props.put(HTDIR,db.getProp(db.ROOTDIR)
                       +System.getProperty("file.separator")
                       +"htk"
                       +System.getProperty("file.separator"));
               
               props.put(HTKTESTDIR,db.getProp(db.ROOTDIR)
                       +System.getProperty("file.separator")
                       +"htk-test"
                       +System.getProperty("file.separator"));
               props.put(HTKTESTWAV,getProp(HTKTESTDIR)
                       +System.getProperty("file.separator")
                       +"wav"
                       +System.getProperty("file.separator"));
               props.put(INTONISEDDIR, getProp(HTKTESTDIR)
                       +System.getProperty("file.separator")
                       +"intonisedXML"
                       +System.getProperty("file.separator"));
               
               if(locale.startsWith("de")){
                   phonemeXml = db.getProp(db.MARYBASE)
                           +File.separator+"lib"+File.separator+"modules"
                           +File.separator+"de"+File.separator+"cap"+File.separator+"phoneme-list-de.xml";
               }
               else{
                   phonemeXml = db.getProp(db.MARYBASE)
                           +File.separator+"lib"+File.separator+"modules"
                           +File.separator+"en"+File.separator+"cap"+File.separator+"phoneme-list-en.xml";
               }
               props.put(PHONEMEXML, phonemeXml);
               
           }
           return props;
       }
       
       protected void setupHelp(){
           props2Help = new TreeMap();
           props2Help.put(HTKDIR,"directory containing the local installation of HTK Labeller"); 
           props2Help.put(HTDIR,"directory containing all files used for training and labeling. Will be created if it does not exist.");
           props2Help.put(INTONISEDDIR, "directory containing the acoustic params.");
           props2Help.put(PHONEMEXML, "Phoneme XML file for given language.");
           props2Help.put(HTKTESTDIR,"directory contains required files for phonerecogniser.");
           props2Help.put(HTKTESTWAV,"This directory contains test utterances.");       
           
       }
        
        public void initialiseComp() 
        {
           
           dictionary = new TreeMap<String, TreeMap<String,String>>();
           File textDir = new File(getProp(HTKTESTDIR)+File.separator+"text");
           File wavDir = new File(getProp(HTKTESTDIR)+File.separator+"wav");
           if (!textDir.exists() || !wavDir.exists()){
               String error = "\nCould not find Test files: wav files or text files\n";
                      error += "1. Create 'htk-test' directory in voicebuilding directory\n";
                      error += "2. Place test utterences in 'htk-test/wav' \n";
                      error += "3. Place test utterence transcriptions in 'htk-test/text' \n";
               throw new Error(error);
           }
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
            bnlist = new BasenameList(getProp(HTKTESTDIR)+File.separator+"text"+File.separator,db.getProp(db.TEXTEXT));
            System.out.println("Preparing voice database for labelling using HTK :");
            //get the voicename        
            voicename = db.getProp(db.VOICENAME);
            //make new directories htk and etc
            htkTest= new File(getProp(HTKTESTDIR)+File.separator);
            // get the output directory of files used by HTK 
            outputDir = htkTest.getAbsolutePath()+File.separator+"etc";
            allophoneSet = AllophoneSet.getAllophoneSet(getProp(PHONEMEXML));
            File dictFile = new File(db.getProp(db.ROOTDIR)+File.separator+"htk"+File.separator+"etc"+File.separator+"htk.dict");
            
            // part 1: HTK basic setup and create required files
            
            // setup the HTK directory 
            System.out.println("Setting up HTKTEST directory ...");
            setup();
            System.out.println(" ... done.");
            // create required files for HTK
            createRequiredFiles();
            intonisedXMLExtractor();
            getPhoneSequence();
            
            //part 2: Feature Extraction using HCopy
            System.out.println("Feature Extraction:");
            featureExtraction();
            System.out.println("... Done.");
            
            //Part 3: Force align with HVite 
            System.out.println("HTK Results:");
            hviteRecognise();
            hResults();
            System.out.println("... Done.");
            
            return true;
        }
        
        
       /**
        * Setup the HTK directory
        * @throws IOException, InterruptedException
        */
        private void setup() throws IOException,InterruptedException{
            
            //File lab = new File(htkTest.getAbsolutePath()+"/lab");
            //call setup of HTK in this directory
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to htk directory and setup Directory Structure 
            pw.print("( cd "+htkTest.getAbsolutePath()
                    +"; mkdir wav"
                    +"; mkdir text"
                    +"; mkdir feat"
                    +"; mkdir config"
                    +"; mkdir etc"
                    +"; mkdir intonisedXML"
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
        }
       

        
        /**
         * Create all required files(config files)  
         * @throws Exception
         */
        private void createRequiredFiles() throws Exception{
            
            // Create an input file to HTK for Feature Extraction
            File file = new File(getProp(HTKTESTDIR)+File.separator+"etc"+File.separator+"featEx.list");
            PrintWriter pw = new PrintWriter(new FileWriter(file));
            for (int i=0; i<bnlist.getLength(); i++) {
                //System.out.println( "    " + bnlist.getName(i) );
                String input = getProp(HTKTESTWAV)+File.separator
                            +bnlist.getName(i)+db.getProp(db.WAVEXT);
                String output = getProp(HTKTESTDIR)+File.separator+"feat"+File.separator+bnlist.getName(i)+".mfcc";
                pw.println(input+" "+output);
            }
            pw.flush();
            pw.close();
            
            // creating list of testing files
            file = new File(getProp(HTKTESTDIR)+File.separator+"etc"+File.separator+"htkTest.list");
            pw = new PrintWriter(new FileWriter(file));
            for (int i=0; i<bnlist.getLength(); i++) {
                //System.out.println( "    " + bnlist.getName(i) );
                String mFile = getProp(HTKTESTDIR)+File.separator+"feat"+File.separator+bnlist.getName(i)+".mfcc";
                pw.println(mFile);
            }
            pw.flush();
            pw.close();
                
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
            String listFile = getProp(HTKTESTDIR)+File.separator
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
         * Force Align database for Automatic labels 
         * @throws Exception
         */
        private void  hviteRecognise() throws Exception{
            
            String hvite = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HVite";
            File htkFile = new File(hvite);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            String configFile = getProp(HTDIR)+File.separator
                +"config"+File.separator+"htkTrain.conf";
            String listFile = getProp(HTKTESTDIR)+File.separator
                +"etc"+File.separator+"htkTest.list";
            String phoneList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.list";
            String hmmDef = getProp(HTDIR)+File.separator
                +"hmm"+File.separator
                +"hmm-final"+File.separator+"hmmdefs";
            String macros = getProp(HTDIR)+File.separator
                +"hmm"+File.separator
                +"hmm-final"+File.separator+"macros";
            String recoMlf = getProp(HTKTESTDIR)+File.separator
                +"htk.recos.mlf";
            String netFile = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.net";
            String phoneAugDict = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.aug.phone.dict";
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +hvite+" -T 1 -C "+configFile+" -i "+recoMlf
                    +" -S "+listFile+" -H "+macros+" -H "+hmmDef
                    +" -w "+netFile
                    +" "+phoneAugDict+" "+phoneList
                    +"; exit )\n");
            
            pw.println("( "
                    +hvite+" -T 1 -C "+configFile+" -i "+recoMlf
                    +" -S "+listFile+" -H "+macros+" -H "+hmmDef
                    +" -w "+netFile
                    +" "+phoneAugDict+" "+phoneList
                    +"; exit )\n");
            
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
            
            
        }
        
        /**
         * Phone Recognition Accuracy  
         * @throws Exception
         */
        private void  hResults() throws Exception{
            
            String hresults = getProp(HTKDIR)+File.separator
            +"bin"+File.separator+"HResults";
            File htkFile = new File(hresults);
            if (!htkFile.exists()) {
                throw new RuntimeException("File "+htkFile.getAbsolutePath()+" does not exist");
            }
            
            
            String phoneList = getProp(HTDIR)+File.separator
                +"etc"+File.separator+"htk.phone.list";
            String recoMlf = getProp(HTKTESTDIR)+File.separator
                +"htk.recos.mlf";
            String phoneMlf = getProp(HTKTESTDIR)+File.separator
                +"etc"+File.separator+"htk.phones.mlf";
            
            Runtime rtime = Runtime.getRuntime();
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            System.out.println("( "
                    +" cd "+getProp(HTKTESTDIR)
                    +"; "
                    +hresults+" -I "+phoneMlf
                    +" "+phoneList+" "+recoMlf+" > htk.results.txt" 
                    +"; exit )\n");
            pw.println("( "
                    +" cd "+getProp(HTKTESTDIR)
                    +"; "
                    +hresults+" -I "+phoneMlf
                    +" "+phoneList+" "+recoMlf+" > htk.results.txt" 
                    +"; exit )\n");
            pw.flush();
            //shut down
            pw.close();
            process.waitFor();
            process.exitValue();
           
            String result = FileUtils.getFileAsString(new File(
                    getProp(HTKTESTDIR)+File.separator+"htk.results.txt"),"ASCII");
            
            System.out.println("RESULTS : \n"+result);
            
        }
        
      private void  intonisedXMLExtractor() throws Exception{
          IntonisedXMLExtractor ixe = new IntonisedXMLExtractor();
          ixe.getDefaultProps(db);
          ixe.initialiseComp();
          //String iDir =  getProp(HTKTESTDIR)+File.separator+"text";
          String iDir =  "htk-test"+File.separator+"text"+File.separator;
          String oDir =  getProp(INTONISEDDIR) + File.separator;
          for (int i=0; i<bnlist.getLength(); i++) {
              ixe.computeFeaturesFor( bnlist.getName(i), iDir, oDir);
          }
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
            for (int i=0; i<bnlist.getLength(); i++) {
                transLabelOut.println("\"*/"+bnlist.getName(i)+labExt+"\"");
                transLabelOut1.println("\"*/"+bnlist.getName(i)+labExt+"\"");
                //phoneSeq = getSingleLine(bnlist.getName(i));
                phoneSeq = getLineFromXML(bnlist.getName(i), false);
                transLabelOut.println(phoneSeq.trim());
                phoneSeq = getLineFromXML(bnlist.getName(i), true);
                transLabelOut1.println(phoneSeq.trim());
                //System.out.println( "    " + bnlist.getName(i) );
                           
            }
            transLabelOut.flush();
            transLabelOut.close();
            transLabelOut1.flush();
            transLabelOut1.close();
            
        }
        
        
        /**
         * Get phoneme sequence from a single feature file
         * @param basename
         * @return String
         * @throws Exception
         */
        private String getLineFromXML(String basename, boolean spause) throws Exception {
            
            String line;
            String phoneSeq;
            Matcher matcher;
            Pattern pattern;
            StringBuffer alignBuff = new StringBuffer();
            //alignBuff.append(basename);
            DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder  = factory.newDocumentBuilder();
            Document doc = builder.parse( new File( getProp(INTONISEDDIR)+"/"+basename+xmlExt ) );
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList tokens = (NodeList) xpath.evaluate("//t | //boundary", doc, XPathConstants.NODESET);
            
            alignBuff.append(collectTranscription(tokens));
            phoneSeq = "!ENTER";
            
            phoneSeq += alignBuff.toString();
            
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
            if(spause != true){
                pattern = Pattern.compile("ssil");
                matcher = pattern.matcher(phoneSeq);
                phoneSeq = matcher.replaceAll("");
            }
            

            phoneSeq += " !EXIT";
            phoneSeq += " .";
            
            pattern = Pattern.compile("\\s+");
            matcher = pattern.matcher(phoneSeq);
            phoneSeq = matcher.replaceAll("\n");
            
            
            return phoneSeq;
        }
        
        /**
         * 
         * This computes a string of phonetic symbols out of an intonised mary xml:
         * - standard phonemes are taken from "ph" attribute
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
            
            // get original phoneme String
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
                            }// ... for each phoneme
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
          
          /** the replace is done for the labels: phoneme, prev_phoneme and next_phoneme */
          
          /** DE (replacements in German phoneme set) */     
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
          /** EN (replacements in English phoneme set) */
          else if (lab.contentEquals("r=") )
              s = "rr"; 
          
          return s;
            
        }
        
        
        /** Translation table for labels which are incompatible with HTK or shell filenames
         * See common_routines.pl in HTS training.
         * In this function the phonemes as used internally in HTSEngine are changed
         * back to the Mary TTS set, this function is necessary when correcting the 
         * actual durations of AcousticPhonemes.
         * @param lab
         * @return String
         */
        public String replaceBackTrickyPhones(String lab){
          String s = lab;
          /** DE (replacements in German phoneme set) */     
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
          /** EN (replacements in English phoneme set) */
          else if (lab.contentEquals("rr") )
              s = "r="; 
          
          //System.out.println("LAB=" + s);
          
          return s;
            
        }
        
        
}
