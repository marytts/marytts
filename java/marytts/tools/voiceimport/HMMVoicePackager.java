package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.twmacinta.util.MD5;

import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;

public class HMMVoicePackager extends VoiceImportComponent {
        
    private DatabaseLayout db;
    private String name = "HMMVoicePackager";
    
    protected final String VOICETYPE = name + ".voiceType";
    protected final String EXAMPLETEXT = name + ".exampleText";
    protected final String LICENSEURL = name + ".licenseUrl";
    
    
    /** HMM Voice-specific parameters, these are parameters used during models training
    if using MGC: 
             gamma=0  alpha=0.42 linear gain (default)
    if using LSP: gamma>0 
        LSP: gamma=1  alpha=0.0  linear gain/log gain 
    Mel-LSP: gamma=1  alpha=0.42 log gain
    MGC-LSP: gamma=3  alpha=0.42 log gain  */
    public final String alpha   = name+".alpha";
    public final String gamma   = name+".gamma";    
    public final String logGain = name+".logGain";

    /** Parameter beta for postfiltering  */    
    public final String beta    = name+".beta";
       
    /** Tree files and TreeSet object */
    public final String treeDurFile = name+".Ftd";
    public final String treeLf0File = name+".Ftf";
    public final String treeMcpFile = name+".Ftm";
    public final String treeStrFile = name+".Fts";
        
    /** HMM pdf model files and ModelSet object */
    public final String pdfDurFile = name+".Fmd";
    public final String pdfLf0File = name+".Fmf";
    public final String pdfMcpFile = name+".Fmm";
    public final String pdfStrFile = name+".Fms";
    
    /** GV pdf files*/
    /** Global variance file, it contains one global mean vector and one global diagonal covariance vector */
    public final String useGV        = name+".useGV";
    public final String maxMgcGvIter = name+".maxMgcGvIter";
    public final String maxLf0GvIter = name+".maxLf0GvIter";
    public final String pdfLf0GVFile = name+".Fgvf"; 
    public final String pdfMcpGVFile = name+".Fgvm";  
    public final String pdfStrGVFile = name+".Fgvs";  
            
    /** Variables for mixed excitation */
    public final String useMixExc      = name+".useMixExc";
    public final String mixFiltersFile = name+".Fif";
    public final String numFilters     = name+".in";
    
   /** Example context feature file (TARGETFEATURES in MARY) */
    public String featuresFileExample = "";
    
    public final String FeaFile = name+".FeaFile";
    /** trickyPhones file if any, this file could have been created durin makeQuestions and makeLabels
     * if it was created, because there are tricky phones in the allophones set, then it should be in
     * voiceDIR/mary/trickyPhones.txt */
    public final String trickyPhonesFile = name+".trickyPhonesFile";
    public boolean trickyPhones = false;
    
    /** Variables for allowing the use of external prosody */
    public final String useAcousticModels  = name+".useAcousticModels";
    
    //public final String createZipFile = name+".createZipFile";
    //public final String zipCommand = name+".zipCommand";

    public String getName(){
        return name;
    }

    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           
           String rootdir = db.getProp(db.ROOTDIR);
           
           props.put(VOICETYPE, "hsmm");
           props.put(EXAMPLETEXT, "examples.text");
           props.put(LICENSEURL, "http://creativecommons.org/licenses/by-nd/3.0/");
           
           props.put(alpha, "0.42");
           props.put(beta, "0.0");
           props.put(gamma, "0");
           props.put(logGain, "false");
           props.put(treeDurFile, "hts/voices/qst001/ver1/tree-dur.inf"); 
           props.put(treeLf0File, "hts/voices/qst001/ver1/tree-lf0.inf");
           props.put(treeMcpFile, "hts/voices/qst001/ver1/tree-mgc.inf");
           props.put(treeStrFile, "hts/voices/qst001/ver1/tree-str.inf");
           props.put(pdfDurFile, "hts/voices/qst001/ver1/dur.pdf"); 
           props.put(pdfLf0File, "hts/voices/qst001/ver1/lf0.pdf"); 
           props.put(pdfMcpFile, "hts/voices/qst001/ver1/mgc.pdf"); 
           props.put(pdfStrFile, "hts/voices/qst001/ver1/str.pdf");
           props.put(useGV, "true");
           props.put(maxMgcGvIter, "200");
           props.put(maxLf0GvIter, "200");
           props.put(pdfLf0GVFile, "hts/data/gv/gv-lf0-littend.pdf"); 
           props.put(pdfMcpGVFile, "hts/data/gv/gv-mgc-littend.pdf"); 
           props.put(pdfStrGVFile, "hts/data/gv/gv-str-littend.pdf");
           props.put(useAcousticModels, "true");
           props.put(useMixExc, "true");
           props.put(mixFiltersFile, "hts/data/filters/mix_excitation_filters.txt"); 
           props.put(numFilters, "5");           
           props.put(trickyPhonesFile, "mary/trickyPhones.txt");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        
        props2Help.put(VOICETYPE, "voice type; one of <b>unit selection</b>, <b>FDPSOLA</b>, <b>HNM</b>"
                + " (note that support for FDPSOLA and HNM are experimental!)");
        props2Help.put(EXAMPLETEXT, "file containing example text (for limited domain voices only)");
        props2Help.put(LICENSEURL, "URL of the license agreement for this voice"
                + " (<a href=\"http://creativecommons.org/licenses/by-nd/3.0/\">cc-by-nd</a> by default)");
        
        props2Help.put(alpha, "Training parameter: Frequency wrapping coefficient. 0.42 for mel frequency.");
        props2Help.put(beta, "Postfiltering coefficient, -0.8 - 0.8");
        props2Help.put(gamma, "Training parameter: gamma=0 for MGC, gamma>0 for LSP");
        props2Help.put(logGain, "Training parameter: use log gain / linear gain, default for MGC logGain=false");
        props2Help.put(treeDurFile, "durations tree file"); 
        props2Help.put(treeLf0File, "log F0 tree file");
        props2Help.put(treeMcpFile, "Mel-cepstral (mcp or Mel-generalized cepstral mgc, HTS Version 2.0.1 used mgc) tree file");
        props2Help.put(treeStrFile, "Bandpass voicing strengths tree file (optional: used for mixed excitation)");
        props2Help.put(pdfDurFile, "Duration means and variances PDF file"); 
        props2Help.put(pdfLf0File, "Log F0 means and variances PDF file"); 
        props2Help.put(pdfMcpFile, "Mel-cepstral (or Mel-generalized cepstral mgc) means and variances PDF file"); 
        props2Help.put(pdfStrFile, "Bandpass voicing strengths means and variances PDF file (optional: used for mixed excitation)");
        props2Help.put(useGV, "Use global variance in parameter generation (true/false)");
        props2Help.put(maxMgcGvIter, "max. number of iterations for MGC optimisation (200)");
        props2Help.put(maxLf0GvIter, "max. number of iterations for LF0 optimisation (200)");
        props2Help.put(pdfLf0GVFile, "Global variance for Log F0, mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfMcpGVFile, "Global variance for Mel-cepstral (or Mel-generalized cepstral mgc) mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfStrGVFile, "Global variance for Bandpass voicing strengths mean and (diagonal) variance PDF file (optional: used for mixed excitation)");
        props2Help.put(useMixExc, "Use mixed excitation in speech generation (true/false)");
        props2Help.put(mixFiltersFile, "Filter taps of bandpass filters for mixed excitation (optional: used for mixed excitation)"); 
        props2Help.put(numFilters, "Number of filters in bandpass bank, default 5 filters (optional: used for mixed excitation)");
        props2Help.put(trickyPhonesFile, "list of aliases for tricky phones, so HTK-HHEd command can handle them. (This file" +
                      " is created automatically by HMMVoiceMakeData if aliases are necessary, otherwise it will not be created.)");
        props2Help.put(useAcousticModels, "Use useAcousticModels: (true/alse), if true it will generate prosody parameters using the specified acoustic models and it allows to " +
        "modify prosody according to the tags in MARYXML");
        
    }

    public boolean compute() {
        // (1) gather files required by this voice in a convenient structure:
        HashMap<String, File> files = getVoiceDataFiles();

        // (2) create config file and add it to the files:
        try {
            File configFile = createVoiceConfig(files);
            files.put("CONFIG", configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        // (3) create zip file and component file (directly at their destination, MARYBASE/download):
        try {
            File zipFile = createZipFile(files);
            createComponentFile(zipFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        logger.info("The voice package and component file have been successfully created and placed in " + getMaryBase()
                + "download");
        logger.info("IMPORTANT: You must run the MARY Component Installer to install the voice!");
        return true;
    }

    
    protected HashMap<String, File> getVoiceDataFiles() {
      try{  
        File in, out;
        String rootDir = db.getProp(db.ROOTDIR);
        HashMap<String, File> files = new HashMap<String, File>();
        
        in = new File(rootDir + getProp(treeDurFile));
        files.put(treeDurFile, in);
        
        in = new File(rootDir + getProp(treeLf0File));
        files.put(treeLf0File, in);
        
        in = new File(rootDir + getProp(treeMcpFile));
        files.put(treeMcpFile, in);
        
        /* optional file for mixed excitation */
        in = new File(rootDir + getProp(treeStrFile));
        if(in.exists()) {
          files.put(treeStrFile, in);      
        }
        in = new File(rootDir + getProp(pdfDurFile));
        files.put(pdfDurFile, in);
        
        in = new File(rootDir + getProp(pdfLf0File));
        files.put(pdfLf0File, in);
        
        in = new File(rootDir + getProp(pdfMcpFile));
        files.put(pdfMcpFile, in);
        
        in = new File(rootDir + getProp(pdfStrFile));
        if(in.exists()) {
          files.put(pdfStrFile, in);  
        }                   
        /* global variance files */
        in = new File(rootDir + getProp(pdfMcpGVFile));
        if(in.exists()) {
          files.put(pdfMcpGVFile, in);   
        }
        in = new File(rootDir + getProp(pdfLf0GVFile));
        if(in.exists()) {
          files.put(pdfLf0GVFile, in);   
        }
        in = new File(rootDir + getProp(pdfStrGVFile));
        if(in.exists()) {
          files.put(pdfStrGVFile, in);   
        }            
        in = new File(rootDir + getProp(mixFiltersFile));
        files.put(mixFiltersFile, in);
        
        // if there is a trickyPhones file
        in = new File(rootDir + getProp(trickyPhonesFile));
        if(in.exists()){
          //out = new File(newVoiceDir + getFileName(getProp(trickyPhonesFile)));
          //FileUtils.copy(in,out);
          files.put(trickyPhonesFile, in);  
          trickyPhones = true;
        }
        
        /* copy one example of MARY context features file, it can be one of the 
         * files used for testing in phonefeatures/*.pfeats*/
        File dirPhonefeatures  = new File(rootDir + "phonefeatures/");
        if( dirPhonefeatures.exists() && dirPhonefeatures.list().length > 0 ){ 
          String[] feaFiles = dirPhonefeatures.list();
          in = new File(rootDir + "phonefeatures/"+feaFiles[0]);
          //out = new File(newVoiceDir + getFileName(feaFiles[0]));
          //FileUtils.copy(in,out);
          files.put(FeaFile, in);
          featuresFileExample = rootDir+"phonefeatures/"+feaFiles[0];
        } else{
           System.out.println("Problem copying one example of context features, the directory phonefeatures/ is empty or directory does not exist.");
           throw new IOException();
        }
           
        /*
        for (String property : properties) {
            String fileName = getProperty(property);
            File file = new File(fileName);
            files.put(property, file);
        }
        */
        return files;
      } catch (Exception e) {
          e.printStackTrace();
          return null;
      }
      
    }
    

    private void createExampleText(File exampleTextFile) throws IOException{
        try{
            //just take the first three transcript files as example text
            PrintWriter exampleTextOut = new PrintWriter(new FileWriter(exampleTextFile),true);
            for (int i=0;i<3;i++){
                String basename = bnl.getName(i);
                BufferedReader transIn = new BufferedReader(new InputStreamReader(new FileInputStream(new File(
                                                   db.getProp(db.TEXTDIR) + basename+db.getProp(db.TEXTEXT)))));
                String text = transIn.readLine();
                transIn.close();            
                exampleTextOut.println(text);
            }
            exampleTextOut.close();
            
        } catch (Exception e){
            System.out.println("Error creating example text file "
                    +exampleTextFile.getName());
            throw new IOException();
        }
        
    }
    
    
    private File createVoiceConfig(HashMap<String, File> files)  throws FileNotFoundException {
        
            
            String fileSeparator = System.getProperty("file.separator");            
            
            String rootDir = db.getProp(db.ROOTDIR);
            /* create the config file */
            //System.out.println("Creating config file ... ");
            //Normalise locale: (e.g., if user set en-US, change it to en_US)
            //String locale = MaryUtils.string2locale(db.getProp(db.LOCALE)).toString();
            String locale = getVoiceLocale();
            
            
            // open the config file for writing:
            String configFileName = String.format("%s-%s.config", getVoiceLocale(), getVoiceName());
            String configFileNameLong = "MARY_BASE" + fileSeparator + "conf" + fileSeparator + configFileName;
            
            logger.info("Creating voice configuration file " + configFileName);
            File configFile = new File(getVoiceFileDir() + configFileName);
            PrintWriter configOut = new PrintWriter(configFile);            
            // CHECK: don't we need UTF-8?
            //PrintWriter configOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(configFileName)),"UTF-8"),true);
            
            
            File in;            
            String voicename = db.getProp(db.VOICENAME).toLowerCase();
            //print the header
            configOut.println("#Auto-generated config file for voice "+voicename+"\n");
            //print name and version info
             configOut.println("name = " + voicename);
             
             configOut.println(locale+"-voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             
             //print providing info
             configOut.println("# Declare \"group names\" as component that other components can require.\n"+
                        "# These correspond to abstract \"groups\" of which this component is an instance.\n"+
                        "provides = \\\n         "+locale+"-voice \\\n" + "         hmm-voice\n");             
             configOut.println("# List the dependencies, as a whitespace-separated list.\n"+
                     "# For each required component, an optional minimum version and an optional\n"+
                     "# download url can be given.\n"+
                     "# We can require a component by name or by an abstract \"group name\"\n"+ 
                     "# as listed under the \"provides\" element.\n"+
                     "requires = \\\n   "+locale+" \\\n   marybase \n");
             configOut.println("requires.marybase.version = 4.0.0\n"+
                     "requires."+locale+".version = 4.0.0\n"+
                     "requires."+locale+".download = http://mary.dfki.de/download/mary-install-4.x.x.jar\n"+
                     "requires.hmm.version = 4.0.0\n");
                
             //now follow the module settings
              configOut.println("####################################################################\n"+
                      "####################### Module settings  ###########################\n"+
                      "####################################################################\n"+
                      "# For keys ending in \".list\", values will be appended across config files,\n"+
                      "# so that .list keys can occur in several config files.\n"+
                      "# For all other keys, values will be copied to the global config, so\n"+
                      "# keys should be unique across config files.\n");              
              configOut.println("hmm.voices.list = \\\n   " + voicename + "\n");
              
              
              String voiceHeader = "voice."+voicename;
              
              //wants-to-be-default value
              configOut.println("# If this setting is not present, a default value of 0 is assumed.\n"+
                      voiceHeader+".wants.to.be.default = 0\n");
      
              //properties of the voice
              configOut.println("# Set your voice specifications\n"+
                      voiceHeader+".gender = "+db.getProp(db.GENDER).toLowerCase()+"\n"+
                      voiceHeader+".locale = "+ locale +"\n"+
                      voiceHeader+".domain = "+db
                      .getProp(db.DOMAIN).toLowerCase()+"\n"+
                      voiceHeader+".samplingRate = "+db.getProp(db.SAMPLINGRATE)+"\n");
              
                     
              //voice data
              configOut.println("# HMM Voice-specific parameters \n" +
                    "# parameters used during models training \n" +
                    "# MGC: stage=gamma=0 alpha=0.42 linear gain (default) \n" +
                    "# LSP: gamma>0  \n" +
                    "#          LSP: gamma=1 alpha=0.0  linear gain/log gain \n" +
                    "#      Mel-LSP: gamma=1 alpha=0.42 log gain \n" +
                    "#      MGC-LSP: gamma=3 alpha=0.42 log gain \n" +
                    voiceHeader+".alpha = " + getProp(alpha) + "\n" +
                    voiceHeader+".gamma = " + getProp(gamma) + "\n" +
                    voiceHeader+".logGain = " + getProp(logGain) + "\n");
              
              configOut.println("# Parameter beta for postfiltering \n" +
                      voiceHeader+".beta = " + getProp(beta) + "\n"); 
              
              configOut.println("# HMM Voice-specific files\n# Information about trees\n"+
                      voiceHeader+".Ftd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeDurFile))+"\n"+
                      voiceHeader+".Ftf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeLf0File))+"\n"+
                      voiceHeader+".Ftm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeMcpFile)));
              if( new File(rootDir + getProp(treeStrFile)).exists())
                configOut.println(voiceHeader+".Fts = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeStrFile)));
              configOut.println("\n# Information about means and variances PDFs \n"+
                      voiceHeader+".Fmd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfDurFile))+"\n"+
                      voiceHeader+".Fmf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0File))+"\n"+
                      voiceHeader+".Fmm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpFile)));
              if( new File(rootDir + getProp(pdfStrFile)).exists())
               configOut.println(voiceHeader+".Fms = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrFile)));
              
              configOut.println("\n# Information about Global Mean and Variance PDFs");
              configOut.println(voiceHeader+".useGV = "+ getProp(useGV));
              configOut.println(voiceHeader+".maxMgcGvIter = "+ getProp(maxMgcGvIter));
              configOut.println(voiceHeader+".maxLf0GvIter = "+ getProp(maxLf0GvIter));
              if( new File(rootDir + getProp(pdfLf0GVFile)).exists())
                  configOut.println(voiceHeader+".Fgvf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0GVFile)));
              if( new File(rootDir + getProp(pdfMcpGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpGVFile)));
              if( new File(rootDir + getProp(pdfStrGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvs = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrGVFile)));
              
              configOut.println("\n# A context features file example for start-up testing.\n" +
                      voiceHeader+".FeaFile = MARY_BASE/lib/voices/"+voicename+"/"+getFileName(featuresFileExample));
              
              configOut.println("\n# Tricky phones file in case there were problematic phones during training, empty otherwise.");
              if(trickyPhones) {                 
                 configOut.println(voiceHeader+".trickyPhonesFile = MARY_BASE/lib/voices/"+voicename+"/"+getFileName(getProp(trickyPhonesFile)));
              } else {
                 configOut.println(voiceHeader+".trickyPhonesFile = ");  
              }
              
              configOut.println("\n# Information about Mixed Excitation");
              configOut.println(voiceHeader + ".useMixExc = " + getProp(useMixExc) + "\n");
              if( new File(rootDir + getProp(treeStrFile)).exists()) {
                configOut.println("# Filters for mixed excitation \n" +
                                "# File format: one column with all the taps, where the number of taps per filter = numTaps/numFilters \n" +
                                voiceHeader+".Fif = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(mixFiltersFile))+"\n"+
                                "# Number of filters in bandpass bank\n" +
                                voiceHeader+".in = " + getProp(numFilters) + "\n");                                 
              }
              
              configOut.println("# Information about acousticModels (if true allows prosody modification specified in MARYXML input)");
              configOut.println(voiceHeader+".useAcousticModels = "+ getProp(useAcousticModels) + "\n");
                           
              configOut.println("# acoustic models to use (HMM models or carts from other voices can be specified)\n"+
                      voiceHeader+".acousticModels = duration F0\n\n" +
                      voiceHeader+".duration.model = hmm\n"+
                      voiceHeader+".duration.data = " + configFileNameLong + "\n"+
                      voiceHeader+".duration.attribute = d\n\n" +
                      voiceHeader+".F0.model = hmm\n"+
                      voiceHeader+".F0.data = " + configFileNameLong + "\n"+
                      voiceHeader+".F0.attribute = f0\n");        
              
              configOut.println();
              
              configOut.close();
        
        
        return configFile;
    }
    
    /**
     * Create zip file containing all of the voice files (including the config file, which should be in <b>files</b>).
     * 
     * @param files
     *            &lt;property, File&gt; Map, e.g. "WaveTimelineMaker.waveTimeline" &rarr;
     *            File("VOICE_DIR/mary/timeline_waves.mry")
     * @return the zip File object
     * @throws Exception
     */
    protected File createZipFile(HashMap<String, File> files) throws Exception {
        // TODO this should probably be optimized by using buffered Readers and Writer:
        byte[] buffer = new byte[4096];

        // initialize zip file:
        String zipFileName = String.format("%s-%s.zip", getVoiceLocale(), getVoiceName());
        logger.info("Creating voice package " + zipFileName);
        File zipFile = new File(getMaryBase() + "download" + File.separator + zipFileName);
        FileOutputStream outputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile));

        // TODO this doesn't explicitly create each ancestor of the voicePath as a separate directory entry in the zip file, but
        // that doesn't seem necessary:
        String voicePath = "lib" + File.separator + "voices" + File.separator + getVoiceName() + File.separator;

        // iterate over files:
        for (String key : files.keySet()) {
            File file = files.get(key);

            // make new entry in zip file, with the appropriate target path:
            logger.debug("Deflating file " + file);
            if (key.equals("CONFIG")) {
                zipStream.putNextEntry(new ZipEntry("conf" + File.separator + file.getName()));
            } else {
                zipStream.putNextEntry(new ZipEntry(voicePath + file.getName()));
            }

            // open data file for reading:
            FileInputStream inputStream = new FileInputStream(file);
            int len;
            // and stream its contents into zip file:
            while ((len = inputStream.read(buffer)) > 0) {
                zipStream.write(buffer, 0, len);
            }

            // complete entry and close data file:
            zipStream.closeEntry();
            inputStream.close();
        }

        // close zip file:
        zipStream.close();

        return zipFile;
    }
    
    /**
     * Create component file for this voice. This includes various metadata, including the zip file name and MD5 hash, and several
     * other attributes.
     * 
     * @param zipFile
     * @throws Exception
     */
    protected void createComponentFile(File zipFile) throws Exception {

        logger.info("Hashing voice package");
        String zipFileMd5Hash = MD5.asHex(MD5.getHash(zipFile));

        String componentFileName = String.format("%s-%s-component.xml", getVoiceLocale(), getVoiceName());
        logger.info("Creating component file " + componentFileName);
        File componentFile = new File(getMaryBase() + File.separator + "download" + File.separator + componentFileName);
        PrintWriter out = new PrintWriter(componentFile);

        // avoid overhead of XML handling by generating the XML with raw strings:
        out.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.format("<marytts-install xmlns=\"http://mary.dfki.de/installer\">\n");
        out.format("    <voice gender=\"%s\" locale=\"%s\" name=\"%s\" type=\"%s\" version=\"%s\">\n", getVoiceGender(),
                getVoiceLocale(), getVoiceName(), getProp(VOICETYPE), getMaryVersion());
        out.format("        <description></description>\n");
        out.format("        <license href=\"%s\"/>\n", getProp(LICENSEURL));
        out.format("        <package filename=\"%s\"\n", zipFile.getName());
        out.format("            md5sum=\"%s\" size=\"%d\">\n", zipFileMd5Hash, zipFile.length());
        out.format("            <location href=\"http://mary.dfki.de/download/%s/\"/>\n", getMaryVersion());
        out.format("        </package>\n");
        out.format("        <depends language=\"%s\" version=\"%s\"/>\n", getVoiceLocale(), getMaryVersion());
        out.format("    </voice>\n");
        out.format("</marytts-install>\n");

        out.close();
    }


    public String getVoiceLocale() {
        return MaryUtils.string2locale(db.getProp(db.LOCALE)).toString();
    }

    public String getVoiceName() {
        return db.getProp(db.VOICENAME).toLowerCase();
    }
    
    public String getMaryBase() {
        return db.getProp(db.MARYBASE) + File.separator;
    }
    
    public String getVoiceFileDir() {
        return db.getProp(db.FILEDIR);
    }
    
    public String getMaryVersion() {
        return db.getProp(db.MARYBASEVERSION);
    }
    public String getVoiceGender() {
        return db.getProp(db.GENDER).toLowerCase();
    }
    
    /**
     * Given a file name with path it return the file name
     * @param fileNameWithPath
     * @return
     */
    private String getFileName(String fileNameWithPath) {
       String str;
       int i;
       
       i = fileNameWithPath.lastIndexOf("/");
       str = fileNameWithPath.substring(i+1); 
       
       return str;
        
    }
    
 
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }


}
