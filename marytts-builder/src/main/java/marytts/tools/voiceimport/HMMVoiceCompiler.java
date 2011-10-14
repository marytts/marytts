package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import marytts.util.io.StreamGobbler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import com.twmacinta.util.MD5;


/**
 * @author marc, marcela
 *
 */
public class HMMVoiceCompiler extends VoiceImportComponent {

	public static final String COMPILEDIR = "VoiceCompiler.compileDir";
	
    // constants to access filenames in database component properties and organize file list:
	
    /** HMM Voice-specific parameters, these are parameters used during models training
    if using MGC: 
             gamma=0  alpha=0.42 linear gain (default)
    if using LSP: gamma>0 
        LSP: gamma=1  alpha=0.0  linear gain/log gain 
    Mel-LSP: gamma=1  alpha=0.42 log gain
    MGC-LSP: gamma=3  alpha=0.42 log gain  */
    public static final String alpha = "HMMVoiceConfigure.freqWarp";
    public static final String gamma = "HMMVoiceConfigure.gamma";    
    public static final String logGain = "HMMVoiceConfigure.lnGain";    
    
    /** Sampling frequency and frame period have to be specified (sampling freq is included in the general config) */
    public static final String samplingRate = "HMMVoiceConfigure.sampFreq";
    public static final String framePeriod = "HMMVoiceConfigure.frameShift";
       
    /** Tree files and TreeSet object */
    // It should be better something like:
    // public static final String treeDurFile = "HMMVoiceMakeVoice.treeDurFile";
    // and then define in that module the name of the files and where they should , it depends on the version ver1----
    public static final String treeDurFile = "hts/voices/qst001/ver1/tree-dur.inf";
    public static final String treeLf0File = "hts/voices/qst001/ver1/tree-lf0.inf";
    public static final String treeMcpFile = "hts/voices/qst001/ver1/tree-mgc.inf";
    public static final String treeStrFile = "hts/voices/qst001/ver1/tree-str.inf";
        
    /** HMM pdf model files and ModelSet object */
    public static final String pdfDurFile = "hts/voices/qst001/ver1/dur.pdf";
    public static final String pdfLf0File = "hts/voices/qst001/ver1/lf0.pdf";
    public static final String pdfMcpFile = "hts/voices/qst001/ver1/mgc.pdf";
    public static final String pdfStrFile = "hts/voices/qst001/ver1/str.pdf";
    
    public static final String pdfLf0GvFile = "hts/voices/qst001/ver1/gv-lf0.pdf"; 
    public static final String pdfMcpGvFile = "hts/voices/qst001/ver1/gv-mgc.pdf";  
    public static final String pdfStrGvFile = "hts/voices/qst001/ver1/gv-str.pdf";
            
    /** Variables for mixed excitation */
    public static final String mixFiltersFileLocation = "hts/data/filters/mix_excitation_filters.txt";
    public static final String mixFiltersFile = "mix_excitation_filters.txt";
    public static final String numFilters = "5";
    
   /** Example context feature file (TARGETFEATURES in MARY) */
    public String featuresFileExampleLocation = "mary/features_example.pfeas";
    public static final String featuresFileExample = "features_example.pfeas";
 
    
    public String FeaFile;
    /** trickyPhones file if any, this file could have been created during makeQuestions and makeLabels
     * if it was created, because there are tricky phones in the allophones set, then it should be in
     * voiceDIR/mary/trickyPhones.txt */
    public static final String trickyPhonesFileLocation = "mary/trickyPhones.txt";
    public static final String trickyPhonesFile = "HMMVoiceMakeData.trickyPhonesFile";
    public static final String hmmFeaturesMapFileLocation = "HMMVoiceMakeData.featureListMapFile";
    // the following does not work ???
    public static final String hmmFeaturesMapFile = "HMMVoiceMakeData.featureListMapFile";
    
    
    /** Mapping in case of using alias names for extra features during training */
    Map<String,String> actualFeatureNames = new HashMap<String, String>();

	protected StrSubstitutor substitutor;
	protected File compileDir;
	protected File mainJavaDir;
	protected File mainResourcesDir;
	protected File mainDescriptionsDir;
	protected File metaInfDir;
	protected File testJavaDir;
	protected File libVoiceDir;
	
	/**
	 * 
	 */
	public HMMVoiceCompiler() {
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#compute()
	 */
	@Override
	public boolean compute() throws Exception {

		// First find a features file example
		getFeatureFileExample();
		System.out.println("featuresFileExample=" + featuresFileExample + "\nfeaturesFileExampleLocation=" + featuresFileExampleLocation);
		
        // Before setting the tree files, we need to check if they contain aliases for the extra features used for training
        // if so there must be a file mary/hmmFeaturesMap.txt which has to be used to convert back the feature names
        // Check if features map was used
		String rootDir = db.getProp(db.ROOTDIR);
        System.out.println("Checking if aliases for extra features used for training were used: checking if file exist -->" + rootDir + getProp(hmmFeaturesMapFile));
        //File featuresMap = new File(rootDir + getProp(hmmFeaturesMapFile));
        File featuresMap = new File(rootDir + "mary/hmmFeaturesMap.txt");
        if(featuresMap.exists()) {
          // convert back the features in all tree files: treeDurFile, treeLf0File, treeMcpFile, treeStrFile
        	System.out.println("convert back the features in all tree files: treeDurFile, treeLf0File, treeMcpFile, treeStrFile");	
          //loadFeaturesMap(rootDir + getProp(hmmFeaturesMapFile));
          loadFeaturesMap(rootDir + "mary/hmmFeaturesMap.txt");	
          /*replaceBackFeatureNames(rootDir + getProp(treeDurFile));
          replaceBackFeatureNames(rootDir + getProp(treeLf0File));
          replaceBackFeatureNames(rootDir + getProp(treeMcpFile));
          replaceBackFeatureNames(rootDir + getProp(treeStrFile)); */
          
          replaceBackFeatureNames(rootDir + treeDurFile);
          replaceBackFeatureNames(rootDir + treeLf0File);
          replaceBackFeatureNames(rootDir + treeMcpFile);
          replaceBackFeatureNames(rootDir + treeStrFile);
        }        
        
		logger.info("Creating directories");
		createDirectories();
		logger.info("Copying template files");
		copyTemplateFiles();
		logger.info("Copying voice files");
		copyVoiceFiles();
		logger.info("Compiling with Maven");
		compileWithMaven();
		logger.info("Creating component description file");
		createComponentFile();
		logger.info("done.");
		
		return true;
	}

	private void createComponentFile() throws IOException {
		String zipFileName = substitutor.replace("voice-${VOICENAME}-${MARYVERSION}.zip");
		File zipFile = new File(compileDir.getAbsolutePath()+"/target/"+zipFileName);
        String zipFileMd5Hash = MD5.asHex(MD5.getHash(zipFile));
        Map<String, String> compMap = new HashMap<String, String>();
        compMap.put("MD5", zipFileMd5Hash);
        compMap.put("FILESIZE", String.valueOf(zipFile.length()));
        StrSubstitutor compSubst = new StrSubstitutor(compMap);
		String componentFileName = substitutor.replace("voice-${VOICENAME}-${MARYVERSION}-component.xml");
		File componentFile = new File(compileDir.getAbsolutePath()+"/target/"+componentFileName);
        copyWithVarSubstitution("component.xml", componentFile, compSubst);
	}

	private void compileWithMaven() throws IOException, InterruptedException {
		Process maven = Runtime.getRuntime().exec("mvn verify", null, compileDir);
		StreamGobbler merr = new StreamGobbler(maven.getErrorStream(), "maven err");
		StreamGobbler mout = new StreamGobbler(maven.getInputStream(), "maven out");
		merr.start();
		mout.start();
		int result = maven.waitFor();
		if (result != 0) {
			throw new IOException("Maven compilation did not succeed -- check console for details.");
		}
	}

	protected void createDirectories() throws IOException {
		 if (compileDir.exists()) {
			 FileUtils.deleteDirectory(compileDir);
		 }
		 compileDir.mkdir();
		 String packageName = toPackageName(db.getVoiceName());
		 mainJavaDir = new File(compileDir.getAbsolutePath()+"/src/main/java/marytts/voice/"+packageName);
		 mainJavaDir.mkdirs();
		 mainResourcesDir = new File(compileDir.getAbsolutePath()+"/src/main/resources/marytts/voice/"+packageName);
		 mainResourcesDir.mkdirs();
		 mainDescriptionsDir = new File(compileDir.getAbsolutePath()+"/src/main/descriptors");
		 mainDescriptionsDir.mkdirs();
		 metaInfDir = new File(compileDir.getAbsolutePath()+"/src/main/resources/META-INF/services");
		 metaInfDir.mkdirs();
		 testJavaDir = new File(compileDir.getAbsolutePath()+"/src/test/java/marytts/voice/"+packageName);
		 testJavaDir.mkdirs();

	}
	
	protected boolean isHmmVoice() {
		return true;
	}
	

	protected void copyVoiceFiles() throws IOException {
		if (!isHmmVoice()) {
			throw new IllegalStateException("This method should only be called for hmm voices");
		}		
		
		String[] filesForResourceDir = new String[] {
		    treeDurFile, treeLf0File, treeMcpFile, treeStrFile, pdfDurFile, pdfLf0File, pdfMcpFile, pdfStrFile, 
		    pdfLf0GvFile, pdfMcpGvFile, pdfStrGvFile, mixFiltersFileLocation, featuresFileExampleLocation, trickyPhonesFileLocation				
		};
		for (String prop : filesForResourceDir) {			
			//System.out.println(prop + "-->" + mainResourcesDir);
			FileUtils.copyFileToDirectory(new File(prop), mainResourcesDir);			
		}
	}

	protected void copyTemplateFiles() throws IOException {
		copyWithVarSubstitution("pom.xml", new File(compileDir, "pom.xml"));
		copyWithVarSubstitution("installable.xml", new File(mainDescriptionsDir, "installable.xml"));			
		copyWithVarSubstitution("Config.java", new File(mainJavaDir, "Config.java"));				
		copyWithVarSubstitution("LoadVoiceIT.java", new File(testJavaDir, "LoadVoiceIT.java"));
		copyWithVarSubstitution("marytts.config.MaryConfig", new File(metaInfDir, "marytts.config.MaryConfig"));
		if (isHmmVoice()) {
			copyWithVarSubstitution("hsmm-voice.config", new File(mainResourcesDir, "voice.config"));
		}
	}

	private void copyWithVarSubstitution(String resourceName, File destination, StrSubstitutor... moreSubstitutors) throws IOException {
		String resource = marytts.util.io.FileUtils.getStreamAsString(getClass().getResourceAsStream("templates/"+resourceName), "UTF-8");
		String resourceWithReplacements = substitutor.replace(resource);
		for (StrSubstitutor more : moreSubstitutors) {
			resourceWithReplacements = more.replace(resourceWithReplacements);
		}
		PrintWriter out = new PrintWriter(destination, "UTF-8");
		out.print(resourceWithReplacements);
		out.close();
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getDefaultProps(marytts.tools.voiceimport.DatabaseLayout)
	 */
	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
        if (props == null) {
            props = new TreeMap<String, String>();
            props.put(COMPILEDIR, new File(db.getVoiceFileDir(), "voice-"+db.getVoiceName()).getAbsolutePath());
        }
        return props;
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getName()
	 */
	@Override
	public String getName() {
		return "VoiceCompiler";
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getProgress()
	 */
	@Override
	public int getProgress() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#setupHelp()
	 */
	@Override
	protected void setupHelp() {
        props2Help = new TreeMap<String, String>();
        props2Help.put(COMPILEDIR, "The directory in which the files for compiling the voice will be copied.");
    }

	@Override
	protected void initialiseComp() throws Exception {
		substitutor = new StrSubstitutor(getVariableSubstitutionMap());
		compileDir = new File(getProp(COMPILEDIR));
	}

	protected Map<String, String> getVariableSubstitutionMap() {
		Map<String, String> m = new HashMap<String, String>();
		m.put("MARYVERSION", db.getMaryVersion());
		m.put("VOICENAME", db.getVoiceName());
		m.put("LOCALE", db.getLocale().toString());
		m.put("LANG", db.getLocale().getLanguage());
		m.put("GENDER", db.getGender());
		m.put("DOMAIN", db.getDomain());
		
		m.put("SAMPLINGRATE", String.valueOf(db.getSamplingRate()));
		m.put("FRAMEPERIOD", String.valueOf(db.getProperty(framePeriod)));
		
		m.put("ALPHA", String.valueOf(db.getProperty(alpha)));
		m.put("GAMMA", String.valueOf(db.getProperty(gamma)));
		m.put("LOGGAIN", String.valueOf(db.getProperty(logGain)));
				
		m.put("MIXEXCFILTERFILE", String.valueOf(mixFiltersFile));
		m.put("NUMMIXEXCFILTERS", String.valueOf(numFilters));		
				
		m.put("PACKAGE", toPackageName(db.getVoiceName()));
		m.put("VOICECLASS", isHmmVoice() ? "marytts.htsengine.HMMVoice" : "marytts.unitselection.UnitSelectionVoice");
		return m;
	}

	/**
	 * Convert an arbitrary string into a valid java package name, as follows:
	 * - any characters that are not alphanumeric or underscore are deleted;
	 * - if the first character after a deleted one is a letter, it is capitalised.
	 * - if the first character is not a letter, we prepend a "V" for "voice".
	 * @param voiceName
	 * @return
	 */
	public static String toPackageName(String voiceName) {
		String regexLCLetter = "[a-z]";
		String regexLetter = "[a-zA-Z]";
		String invalidChars = "[^_a-zA-Z0-9]";
		String[] parts = voiceName.split(invalidChars);
		StringBuilder result = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			String firstChar =  part.substring(0, 1);
			if (Pattern.matches(regexLCLetter, firstChar)) {
				result.append(firstChar.toUpperCase()).append(part.substring(1));
			} else {
				result.append(part);
			}
		}
		if (!Pattern.matches(regexLetter, result.subSequence(0, 1))) {
			result.insert(0, "V");
		}
		return result.toString();
	}

	
	protected void getFeatureFileExample() throws IOException {	
		String fileExample=null;
		String rootDir = db.getProp(db.ROOTDIR);
        /* copy one example of MARY context features file, it can be one of the 
         * files used for testing in phonefeatures/*.pfeats*/	
        File dirPhonefeatures  = new File(rootDir + "phonefeatures/");
        if( dirPhonefeatures.exists() && dirPhonefeatures.list().length > 0 ){ 
          String[] feaFiles = dirPhonefeatures.list();
          fileExample = feaFiles[0];
          if(fileExample==null){
        	System.out.println("Problem copying one example of context features, the directory phonefeatures/ is empty");
            throw new IOException();  
          } else {
        	 FileUtils.copyFile(new File("phonefeatures/"+fileExample), new File("mary/"+featuresFileExample)); 
          }
        } else{
           System.out.println("Problem copying one example of context features, the directory does not exist.");
           throw new IOException();
        }         
	}
	
    /**
     * Replace the aliases for features used during training
     * @param treeFileName a HTS tree file
     * @throws IOException
     */
    private void replaceBackFeatureNames(String treeFileName) throws IOException{
      
      int i, j, length, state;
      BufferedReader s = null; 
      FileWriter outputStream;
      
      //---outputStream.write(hmm_tts.getRealisedDurations());
      //---outputStream.close();
      String line, aux;
      // read the file until the symbol the delimits an state is found
      try {   
        // output file to copy the result
        outputStream = new FileWriter(treeFileName + ".tmp");
        
        // read lines of tree-*.inf fileName 
        s = new BufferedReader(new InputStreamReader(new FileInputStream(treeFileName)));
        logger.info("load: reading " + treeFileName);
            
        // skip questions section, but copy the lines on the temporary output
        while((line = s.readLine()) != null) {
          outputStream.write(line + "\n");
          if (line.indexOf("QS") < 0 ) break;   /* a new state is indicated by {*}[2], {*}[3], ... */
        }
        
        StringTokenizer sline;
        String buf1, buf2, buf3, buf4;
        while((line = s.readLine()) != null) {
          //System.out.println("line: " + line);
          if(line.indexOf("{") >= 0 || line.indexOf("}") >= 0 || line.length()==0){ /* this is the indicator of a new state-tree */
            outputStream.write(line + "\n");           
          } else {              
            sline = new StringTokenizer(line);
            buf1 = sline.nextToken();
            buf2 = sline.nextToken();            
            String [] fea = buf2.split("=");            
            buf3 = sline.nextToken();            
            buf4 = sline.nextToken();
            //System.out.format("newLine:  %s %s=%s\t\t%s\t%s\n", buf1, replaceBack(fea[0]), fea[1], buf3, buf4);            
            outputStream.write(" " + buf1 + " " + replaceBack(fea[0]) + "=" + fea[1] + "\t\t" + buf3 + "\t" + buf4 + "\n");            
          }
        }
        outputStream.close();
        System.out.println("Features alises replaced in file: " + treeFileName + ".tmp");  
        // now replace the file
        FileUtils.copyFile(new File(treeFileName + ".tmp"), new File(treeFileName)); 
        System.out.println("Copied file: " + treeFileName + ".tmp" + "  to: " + treeFileName + "\n"); 
        
       } catch (IOException e) {
              logger.debug("FileNotFoundException: " + e.getMessage());
              throw new IOException("LoadTreeSet: " + e.getMessage());
      }
   
    }
    
    /**
     * Load mapping of features from file
     * @param fileName 
     * @throws FileNotFoundException
     */
    private Map<String,String> loadFeaturesMap(String fileName) throws FileNotFoundException{
        
        Scanner aliasList = null;
        try {
          aliasList = new Scanner(new BufferedReader(new FileReader(fileName)));
          String line;
          logger.info("loading features map from file: " + fileName);
          while (aliasList.hasNext()) {
            line = aliasList.nextLine();
            String[] fea = line.split(" ");
                      
            actualFeatureNames.put(fea[1], fea[0]);
            logger.info("  " + fea[0] + " -->  " + fea[1]);             
          }
          if (aliasList != null) { 
            aliasList.close();
          }       
        } catch (FileNotFoundException e) {
            logger.debug("loadTrickyPhones:  " + e.getMessage());
             throw new FileNotFoundException();
        } 
        return actualFeatureNames;
    }
 

    
    /**
     * Replace label with information in the global map list actualFeatureNames
     * @param lab replaced label
     * @return
     */
    public String replaceBack(String lab){
      
      String s = lab;
        
      if( actualFeatureNames.containsKey(lab)){
         s = actualFeatureNames.get(lab); 
      } 
      return s;        
    }
    
    

	
	
}
