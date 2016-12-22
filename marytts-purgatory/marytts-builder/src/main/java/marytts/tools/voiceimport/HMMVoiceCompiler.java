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

	/**
	 * HMM Voice-specific parameters, these are parameters used during models training if using MGC: gamma=0 alpha=0.42 linear
	 * gain (default) if using LSP: gamma&gt;0 LSP: gamma=1 alpha=0.0 linear gain/log gain Mel-LSP: gamma=1 alpha=0.42 log gain
	 * MGC-LSP: gamma=3 alpha=0.42 log gain
	 */
	public static final String alpha = "HMMVoiceConfigure.freqWarp";
	public static final String gamma = "HMMVoiceConfigure.gamma";
	public static final String logGain = "HMMVoiceConfigure.lnGain";

	/** Sampling frequency and frame period have to be specified (sampling freq is included in the general config) */
	public static final String samplingRate = "HMMVoiceConfigure.sampFreq";
	public static final String framePeriod = "HMMVoiceConfigure.frameShift";

	/** The following files depend on the version number and question number defined during configuration and training */
	public static final String questionNumber = "HMMVoiceConfigure.questionsNum";
	public static final String versionNumber = "HMMVoiceConfigure.version";

	/** Tree files and TreeSet object */
	public static String treeDurFile;
	public static String treeLf0File;
	public static String treeMcpFile;
	public static String treeStrFile;

	/** HMM pdf model files and ModelSet object */
	public static String pdfDurFile;
	public static String pdfLf0File;
	public static String pdfMcpFile;
	public static String pdfStrFile;

	/** Global variance files */
	public static String pdfLf0GvFile;
	public static String pdfMcpGvFile;
	public static String pdfStrGvFile;

	/** Variables for mixed excitation */
	public static final String mixFiltersFile = "HMMVoiceConfigure.strFilterFileName";
	public static String mixFiltersFileLocation;
	public static final String numFilters = "HMMVoiceConfigure.strOrder";

	/** Example context feature file (TARGETFEATURES in MARY) */
	public static String featuresFileExample = "mary/features_example.pfeats";

	public String FeaFile;
	/**
	 * trickyPhones file if any, this file could have been created during makeQuestions and makeLabels if it was created, because
	 * there are tricky phones in the allophones set, then it should be in voiceDIR/mary/trickyPhones.txt
	 */
	public static final String trickyPhonesFile = "HMMVoiceMakeData.trickyPhonesFile";
	public static final String hmmFeaturesMapFile = "HMMVoiceMakeData.featureListMapFile";

	/** Mapping in case of using alias names for extra features during training */
	Map<String, String> actualFeatureNames = new HashMap<String, String>();

	/**
	 * 
	 */
	public HMMVoiceCompiler() {
	}

	/**
	 * @throws IOException
	 *             IOException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	@Override
	protected void mapFeatures() throws IOException, FileNotFoundException {
		String rootDir = db.getProp(DatabaseLayout.ROOTDIR);
		// First find a features file example
		getFeatureFileExample();

		/* Substitute question number and version number */
		String vnum = db.getProperty(versionNumber);
		String qnum = db.getProperty(questionNumber);

		/** Tree files and TreeSet object */
		treeDurFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/tree-dur.inf";
		treeLf0File = "hts/voices/qst" + qnum + "/ver" + vnum + "/tree-lf0.inf";
		treeMcpFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/tree-mgc.inf";
		treeStrFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/tree-str.inf";

		/** HMM pdf model files and ModelSet object */
		pdfDurFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/dur.pdf";
		pdfLf0File = "hts/voices/qst" + qnum + "/ver" + vnum + "/lf0.pdf";
		pdfMcpFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/mgc.pdf";
		pdfStrFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/str.pdf";

		/** Global variance files */
		pdfLf0GvFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/gv-lf0.pdf";
		pdfMcpGvFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/gv-mgc.pdf";
		pdfStrGvFile = "hts/voices/qst" + qnum + "/ver" + vnum + "/gv-str.pdf";

		/** Filter file for mixed excitation */
		mixFiltersFileLocation = "hts/data/" + db.getProperty(mixFiltersFile);

		// Set files for resources
		// Now I know the names of the resources so I can set the files for resources on the maven compiler
		String[] filenamesResources = new String[] { rootDir + treeDurFile, rootDir + treeLf0File, rootDir + treeMcpFile,
				rootDir + treeStrFile, rootDir + pdfDurFile, rootDir + pdfLf0File, rootDir + pdfMcpFile, rootDir + pdfStrFile,
				rootDir + pdfLf0GvFile, rootDir + pdfMcpGvFile, rootDir + pdfStrGvFile, rootDir + mixFiltersFileLocation,
				rootDir + featuresFileExample, rootDir + db.getProperty(trickyPhonesFile) };
		File[] filesForResources = new File[filenamesResources.length];
		for (int i = 0; i < filenamesResources.length; i++) {
			filesForResources[i] = new File(filenamesResources[i]);
		}
		compiler.setFilesForResources(filesForResources);

		// Before setting the tree files, we need to check if they contain aliases for the extra features used for training
		// if so there must be a file mary/hmmFeaturesMap.txt which has to be used to convert back the feature names
		// Check if features map was used
		String feasMapFileName = rootDir + db.getProperty(hmmFeaturesMapFile);
		System.out.println("Checking if aliases for extra features used for training were used: checking if file exist -->"
				+ feasMapFileName);
		File featuresMap = new File(feasMapFileName);

		if (featuresMap.exists()) {
			// convert back the features in all tree files: treeDurFile, treeLf0File, treeMcpFile, treeStrFile
			System.out.println("convert back the features in all tree files: treeDurFile, treeLf0File, treeMcpFile, treeStrFile");
			loadFeaturesMap(feasMapFileName);

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
	protected String getVoiceName(DatabaseLayout db) {
		return db.getVoiceName() + "-hsmm";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getName()
	 */
	@Override
	public String getName() {
		return "HMMVoiceCompiler";
	}

	@Override
	protected Map<String, String> getExtraVariableSubstitutionMap() {
		Map<String, String> m = new HashMap<String, String>();
		m.put("FRAMEPERIOD", String.valueOf(db.getProperty(framePeriod)));

		m.put("ALPHA", String.valueOf(db.getProperty(alpha)));
		m.put("GAMMA", String.valueOf(db.getProperty(gamma)));
		m.put("LOGGAIN", String.valueOf(db.getProperty(logGain)));

		// The filters file name includes the "filters/" directory, we need here just the file name
		m.put("MIXEXCFILTERFILE", db.getProperty(mixFiltersFile).substring(8));
		m.put("NUMMIXEXCFILTERS", String.valueOf(db.getProperty(numFilters)));

		return m;
	}

	@Override
	protected File[] getFilesForResources() {
		// at this point, initialisation, I still do not know where
		// exactly the files will be
		/*
		 * String[] filenamesResources = new String[] { treeDurFile, treeLf0File, treeMcpFile, treeStrFile, pdfDurFile,
		 * pdfLf0File, pdfMcpFile, pdfStrFile, pdfLf0GvFile, pdfMcpGvFile, pdfStrGvFile, mixFiltersFileLocation,
		 * featuresFileExample, db.getProperty(trickyPhonesFile) }; File[] filesForResources = new
		 * File[filenamesResources.length]; for (int i=0; i<filenamesResources.length; i++) { filesForResources[i] = new
		 * File(filenamesResources[i]); } return filesForResources;
		 */
		return null;
	}

	@Override
	protected File[] getFilesForFilesystem() {
		return null;
	}

	protected void getFeatureFileExample() throws IOException {
		String fileExample = null;
		String rootDir = db.getProp(DatabaseLayout.ROOTDIR);
		logger.info("Getting a context feature file example in phonefeatures/");

		/*
		 * copy one example of MARY context features file, it can be one of the files used for testing in phonefeatures/*.pfeats
		 */
		File dirPhonefeatures = new File(rootDir + "phonefeatures/");
		if (dirPhonefeatures.exists() && dirPhonefeatures.list().length > 0) {
			String[] feaFiles = dirPhonefeatures.list();
			fileExample = feaFiles[0];
			File in = new File(rootDir + "phonefeatures/" + fileExample);

			if (in.isDirectory()) {
				logger.info("HMMVoiceConfigure.adaptScripts = " + db.getProperty("HMMVoiceConfigure.adaptScripts"));
				/* If adaptive training then look for an example in the first directory */
				if (in.exists() && in.list().length > 0) {
					FileUtils.copyFile(new File(rootDir, "phonefeatures/" + fileExample + "/" + in.list()[0]), new File(rootDir,
							featuresFileExample));
				}
			} else if (in.exists()) {
				FileUtils.copyFile(in, new File(rootDir, featuresFileExample));
			} else {
				System.out.println("Problem copying one example of context features, the directory phonefeatures/ is empty(?)");
				throw new IOException();
			}

		} else {
			System.out.println("Problem copying one example of context features, the directory does not exist.");
			throw new IOException();
		}
	}

	/**
	 * Replace the aliases for features used during training
	 * 
	 * @param treeFileName
	 *            a HTS tree file
	 * @throws IOException
	 *             IOException
	 */
	private void replaceBackFeatureNames(String treeFileName) throws IOException {

		BufferedReader s = null;
		FileWriter outputStream;

		// ---outputStream.write(hmm_tts.getRealisedDurations());
		// ---outputStream.close();
		String line;
		// read the file until the symbol the delimits an state is found
		try {
			// output file to copy the result
			outputStream = new FileWriter(treeFileName + ".tmp");

			// read lines of tree-*.inf fileName
			s = new BufferedReader(new InputStreamReader(new FileInputStream(treeFileName)));
			logger.info("load: reading " + treeFileName);

			// skip questions section, but copy the lines on the temporary output
			while ((line = s.readLine()) != null) {
				outputStream.write(line + "\n");
				if (line.indexOf("QS") < 0)
					break; /* a new state is indicated by {*}[2], {*}[3], ... */
			}

			StringTokenizer sline;
			String buf1, buf2, buf3, buf4;
			while ((line = s.readLine()) != null) {
				// System.out.println("line: " + line);
				if (line.indexOf("{") >= 0 || line.indexOf("}") >= 0 || line.length() == 0) { /*
																							 * this is the indicator of a new
																							 * state-tree
																							 */
					outputStream.write(line + "\n");
				} else {
					sline = new StringTokenizer(line);
					buf1 = sline.nextToken();
					buf2 = sline.nextToken();
					String[] fea = buf2.split("=");
					buf3 = sline.nextToken();
					buf4 = sline.nextToken();
					// System.out.format("newLine:  %s %s=%s\t\t%s\t%s\n", buf1, replaceBack(fea[0]), fea[1], buf3, buf4);
					outputStream
							.write(" " + buf1 + " " + replaceBack(fea[0]) + "=" + fea[1] + "\t\t" + buf3 + "\t" + buf4 + "\n");
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
	 * 
	 * @param fileName
	 *            fileName
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	private Map<String, String> loadFeaturesMap(String fileName) throws FileNotFoundException {

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
	 * 
	 * @param lab
	 *            replaced label
	 * @return s
	 */
	public String replaceBack(String lab) {

		String s = lab;

		if (actualFeatureNames.containsKey(lab)) {
			s = actualFeatureNames.get(lab);
		}
		return s;
	}

}
