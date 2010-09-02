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

public class HMMVoicePackager extends VoicePackager {


    /** HMM Voice-specific parameters, these are parameters used during models training
    if using MGC: 
             gamma=0  alpha=0.42 linear gain (default)
    if using LSP: gamma>0 
        LSP: gamma=1  alpha=0.0  linear gain/log gain 
    Mel-LSP: gamma=1  alpha=0.42 log gain
    MGC-LSP: gamma=3  alpha=0.42 log gain  */
    private String alpha;
    private String gamma;    
    private String logGain;

    /** Parameter beta for postfiltering  */    
    private String beta;
       
    /** Tree files and TreeSet object */
    private String treeDurFile;
    private String treeLf0File;
    private String treeMcpFile;
    private String treeStrFile;
        
    /** HMM pdf model files and ModelSet object */
    private String pdfDurFile;
    private String pdfLf0File;
    private String pdfMcpFile;
    private String pdfStrFile;
    
    /** GV pdf files*/
    /** Global variance file, it contains one global mean vector and one global diagonal covariance vector */
    private String useGV;
    private String maxMgcGvIter;
    private String maxLf0GvIter;
    private String pdfLf0GVFile; 
    private String pdfMcpGVFile;  
    private String pdfStrGVFile;  
            
    /** Variables for mixed excitation */
    private String useMixExc;
    private String mixFiltersFile;
    private String numFilters;
    
   /** Example context feature file (TARGETFEATURES in MARY) */
    public String featuresFileExample;
    
    private String FeaFile;
    /** trickyPhones file if any, this file could have been created durin makeQuestions and makeLabels
     * if it was created, because there are tricky phones in the allophones set, then it should be in
     * voiceDIR/mary/trickyPhones.txt */
    private String trickyPhonesFile;
    private boolean trickyPhones;
    
    /** Variables for allowing the use of external prosody */
    private String useAcousticModels;
    
    public HMMVoicePackager() {
        super("HMMVoicePackager");
        alpha = name + ".alpha";
        gamma = name + ".gamma";
        logGain = name + ".logGain";
        beta = name + ".beta";
        treeDurFile = name + ".Ftd";
        treeLf0File = name + ".Ftf";
        treeMcpFile = name + ".Ftm";
        treeStrFile = name + ".Fts";
        pdfDurFile = name + ".Fmd";
        pdfLf0File = name + ".Fmf";
        pdfMcpFile = name + ".Fmm";
        pdfStrFile = name + ".Fms";
        useGV = name + ".useGV";
        maxMgcGvIter = name + ".maxMgcGvIter";
        maxLf0GvIter = name + ".maxLf0GvIter";
        pdfLf0GVFile = name + ".Fgvf";
        pdfMcpGVFile = name + ".Fgvm";
        pdfStrGVFile = name + ".Fgvs";
        useMixExc = name + ".useMixExc";
        mixFiltersFile = name + ".Fif";
        numFilters = name + ".in";
        featuresFileExample = "";
        FeaFile = name + ".FeaFile";
        trickyPhonesFile = name + ".trickyPhonesFile";
        trickyPhones = false;
        useAcousticModels = name + ".useAcousticModels";
    }

    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    @Override
    public SortedMap<String,String> getDefaultProps(DatabaseLayout databaseLayout){
        this.db = databaseLayout;
       if (props == null){
           props = new TreeMap<String,String>();
           
           String rootdir = db.getProp(db.ROOTDIR);
           String voiceDescription = "A " + db.getProp(db.GENDER) + " " + db.getProp(db.LOCALE) + " Hidden semi-Markov model voice";
           //"A [gender], [language (expressive)] Hidden semi-Markov model voice: (name of the speaker)."
           
           props.put(VOICETYPE, "hsmm");
           props.put(LICENSEURL, "http://mary.dfki.de/download/by-nd-3.0.html");
           props.put(VOICEDESCRIPTION, voiceDescription);
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    protected void setupHelp(){
        props2Help = new TreeMap<String, String>();
        
        props2Help.put(VOICETYPE, "voice type; one of <b>unit selection</b>, <b>HSMM</b>, <b>FDPSOLA</b>, <b>HNM</b>"
                + " (note that support for FDPSOLA and HNM are experimental!)");
        props2Help.put(LICENSEURL, "URL of the license agreement for this voice"
                + " (<a href=\"http://creativecommons.org/licenses/by-nd/3.0/\">cc-by-nd</a> by default)");
        props2Help.put(VOICEDESCRIPTION, "short text describing this voice");
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
    
    @Override
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

        return files;
      } catch (Exception e) {
          e.printStackTrace();
          return null;
      }
      
    }
    
    
    @Override
    protected File createVoiceConfig(HashMap<String, File> files) throws FileNotFoundException {
            String fileSeparator = System.getProperty("file.separator");            
            
            String rootDir = db.getProp(db.ROOTDIR);
            String maryVersion = db.getProp(db.MARYBASEVERSION);
            /* create the config file */
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
             
             configOut.println(locale+"-voice.version = " + maryVersion + "\n");
             configOut.println("voice.version = " + maryVersion + "\n");
             
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
             configOut.println("requires.marybase.version = " + maryVersion + "\n"+
                     "requires."+locale+".version = " + maryVersion + "\n"+
                     "requires."+locale+".download = http://mary.dfki.de/download/mary-install-4.x.x.jar\n"+
                     "requires.hmm.version = " + maryVersion + "\n");
                
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
                      voiceHeader+".Ftd = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(treeDurFile))+"\n"+
                      voiceHeader+".Ftf = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(treeLf0File))+"\n"+
                      voiceHeader+".Ftm = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(treeMcpFile)));
              if( new File(rootDir + getProp(treeStrFile)).exists())
                configOut.println(voiceHeader+".Fts = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(treeStrFile)));
              configOut.println("\n# Information about means and variances PDFs \n"+
                      voiceHeader+".Fmd = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfDurFile))+"\n"+
                      voiceHeader+".Fmf = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfLf0File))+"\n"+
                      voiceHeader+".Fmm = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfMcpFile)));
              if( new File(rootDir + getProp(pdfStrFile)).exists())
               configOut.println(voiceHeader+".Fms = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfStrFile)));
              
              configOut.println("\n# Information about Global Mean and Variance PDFs");
              configOut.println(voiceHeader+".useGV = "+ getProp(useGV));
              configOut.println(voiceHeader+".maxMgcGvIter = "+ getProp(maxMgcGvIter));
              configOut.println(voiceHeader+".maxLf0GvIter = "+ getProp(maxLf0GvIter));
              if( new File(rootDir + getProp(pdfLf0GVFile)).exists())
                  configOut.println(voiceHeader+".Fgvf = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfLf0GVFile)));
              if( new File(rootDir + getProp(pdfMcpGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvm = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfMcpGVFile)));
              if( new File(rootDir + getProp(pdfStrGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvs = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(pdfStrGVFile)));
              
              configOut.println("\n# A context features file example for start-up testing.\n" +
                      voiceHeader+".FeaFile = MARY_BASE/lib/voices/"+voicename+"/"+ FileUtils.getFileName(featuresFileExample));
              
              configOut.println("\n# Tricky phones file in case there were problematic phones during training, empty otherwise.");
              if(trickyPhones) {                 
                 configOut.println(voiceHeader+".trickyPhonesFile = MARY_BASE/lib/voices/"+voicename+"/"+ FileUtils.getFileName(getProp(trickyPhonesFile)));
              } else {
                 configOut.println(voiceHeader+".trickyPhonesFile = ");  
              }
              
              configOut.println("\n# Information about Mixed Excitation");
              configOut.println(voiceHeader + ".useMixExc = " + getProp(useMixExc) + "\n");
              if( new File(rootDir + getProp(treeStrFile)).exists()) {
                configOut.println("# Filters for mixed excitation \n" +
                                "# File format: one column with all the taps, where the number of taps per filter = numTaps/numFilters \n" +
                                voiceHeader+".Fif = MARY_BASE/lib/voices/"+voicename+"/" + FileUtils.getFileName(getProp(mixFiltersFile))+"\n"+
                                "# Number of filters in bandpass bank\n" +
                                voiceHeader+".in = " + getProp(numFilters) + "\n");                                 
              }
              
              configOut.println("# Information about acousticModels (if true allows prosody modification specified in MARYXML input)");
              configOut.println(voiceHeader+".useAcousticModels = "+ getProp(useAcousticModels) + "\n");
                           
              configOut.println("# acoustic models to use (HMM models or carts from other voices can be specified)\n" +
                                "#(uncoment to allow prosody modification specified in MARYXML input)");
              if( Boolean.valueOf(getProp(useAcousticModels)).booleanValue() )
                configOut.println(voiceHeader+".acousticModels = duration F0\n");
              else                           
                configOut.println("#" + voiceHeader+".acousticModels = duration F0\n");
        
              configOut.println(voiceHeader+".duration.model = hmm\n"+
                          voiceHeader+".duration.data = " + configFileNameLong + "\n"+
                          voiceHeader+".duration.attribute = d\n\n" +
                          voiceHeader+".F0.model = hmm\n"+
                          voiceHeader+".F0.data = " + configFileNameLong + "\n"+
                          voiceHeader+".F0.attribute = f0");                
              
              configOut.println();
              
              configOut.close();
          
        return configFile;
    }
}
