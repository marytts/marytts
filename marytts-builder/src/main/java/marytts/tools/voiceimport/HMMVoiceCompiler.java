package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;


/**
 * @author marc, marcela
 *
 */
public class HMMVoiceCompiler extends VoiceCompiler {
	
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

	
	/**
	 * 
	 */
	public HMMVoiceCompiler() {
	}


	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	@Override
	protected void mapFeatures() throws IOException, FileNotFoundException {
		// First find a features file example
		getFeatureFileExample();
		System.out.println("featuresFileExample=" + featuresFileExample + "\nfeaturesFileExampleLocation=" + featuresFileExampleLocation);
		
        // Before setting the tree files, we need to check if they contain aliases for the extra features used for training
        // if so there must be a file mary/hmmFeaturesMap.txt which has to be used to convert back the feature names
        // Check if features map was used
		String rootDir = db.getProp(DatabaseLayout.ROOTDIR);
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
	}

	@Override
	protected boolean isUnitSelectionVoice() {
		return false;
	}
	
	@Override
	protected String getCompileDirProp() {
		return "HMMVoiceCompiler.compileDir";
	}

	@Override
	protected String getVoiceName(DatabaseLayout db) {
		return db.getVoiceName()+"-hsmm";
	}

	@Override
	protected void copyVoiceFiles() throws IOException {
		if (isUnitSelectionVoice()) {
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




	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getName()
	 */
	@Override
	public String getName() {
		return "HMMVoiceCompiler";
	}

	@Override
	protected Map<String, String> getVariableSubstitutionMap() {
		Map<String, String> m = super.getVariableSubstitutionMap();
		m.put("FRAMEPERIOD", String.valueOf(db.getProperty(framePeriod)));
		
		m.put("ALPHA", String.valueOf(db.getProperty(alpha)));
		m.put("GAMMA", String.valueOf(db.getProperty(gamma)));
		m.put("LOGGAIN", String.valueOf(db.getProperty(logGain)));
				
		m.put("MIXEXCFILTERFILE", String.valueOf(mixFiltersFile));
		m.put("NUMMIXEXCFILTERS", String.valueOf(numFilters));		
				
		return m;
	}


	
	protected void getFeatureFileExample() throws IOException {	
		String fileExample=null;
		String rootDir = db.getProp(DatabaseLayout.ROOTDIR);
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
      
      BufferedReader s = null; 
      FileWriter outputStream;
      
      //---outputStream.write(hmm_tts.getRealisedDurations());
      //---outputStream.close();
      String line;
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
              throw new IOException("LoadTreeSet: ", e);
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
             throw e;
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
