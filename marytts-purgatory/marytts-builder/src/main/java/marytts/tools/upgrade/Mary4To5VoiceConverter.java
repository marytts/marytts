package marytts.tools.upgrade;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.twmacinta.util.MD5;

import marytts.Version;
import marytts.exceptions.MaryConfigurationException;
import marytts.tools.install.InstallFileParser;
import marytts.tools.install.VoiceComponentDescription;
import marytts.tools.voiceimport.VoiceCompiler;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;
import marytts.util.io.FileUtils;

public class Mary4To5VoiceConverter {

	private static final String EOL = IOUtils.LINE_SEPARATOR_WINDOWS;

	/**
	 * The list of property suffixes which can read from a resource file.
	 */
	private static final String[] PROPS_FOR_RESOURCES = new String[] { "Ftd", "Ftf", "Ftm", "Fts", "Fmd", "Fmf", "Fmm", "Fms",
			"Fgvf", "Fgvm", "Fgvs", "FeaFile", "trickyPhonesFile", "Fif", "targetCostWeights", "joinCostWeights", "cartFile",
			"duration.data", "F0.data", "midF0.data", "rightF0.data", "exampleTextFile", };

	private static final String[] PROPS_FOR_FILESYSTEM = new String[] { "featureFile", "joinCostFile", "unitsFile",
			"audioTimelineFile", "basenameTimeline", "vocalization.unitfile", "vocalization.timeline",
			"vocalization.featurefile", "vocalization.featureDefinitionFile", "vocalization.intonationfile",
			"vocalization.mlsafeaturefile", "vocalization.mixedexcitationfilter",
			"vocalization.intonation.featureDefinitionFile",
			// 4.0 prosody carts:
			"duration.cart", "duration.featuredefinition", "f0.cart.left", "f0.cart.mid", "f0.cart.right", "f0.featuredefinition" };

	/**
	 * The list of property suffixes which should be dropped when upgrading the config file.
	 */
	private static final String[] SUFFIXES_TO_DROP_UNITSEL = new String[] {};
	private static final String[] SUFFIXES_TO_DROP_HMM = new String[] { "F0.data", "duration.data", };
	private static final String[] PROPS_TO_DROP = new String[] { "requires", "provides", "voice.version", "en_US-voice.version",
			"de-voice.version", "it-voice.version", "requires.marybase.version", "requires.hmm.version",
			"requires.en_US.version", "requires.de.version", "requires.it.version", "requires.en_US.download",
			"requires.de.download", "requires.it.download", };

	private Logger logger;
	private VoiceComponentDescription voiceDescription;
	private File mary4Zip;
	private Properties config;
	private List<String> originalConfig;

	private File extractedDir;
	private File compileDir;
	private String domain;
	private int samplingRate;
	private File[] filesForResources;
	private File[] filesForFilesystem;

	VoiceCompiler.MavenVoiceCompiler compiler;

	public Mary4To5VoiceConverter(List<VoiceComponentDescription> voiceDescriptions, File voiceZip) {
		voiceDescription = null;
		mary4Zip = voiceZip;
		for (VoiceComponentDescription d : voiceDescriptions) {
			if (d.getPackageFilename().equals(mary4Zip.getName())) {
				voiceDescription = d;
				break;
			}
		}
		if (voiceDescription == null) {
			throw new IllegalArgumentException("No matching voice description for file " + mary4Zip.getName());
		}
		if (!MaryUtils.isLog4jConfigured()) {
			BasicConfigurator.configure();
		}
		logger = Logger.getLogger(this.getClass());
		logger.info(voiceDescription.getName() + " " + voiceDescription.getVersion() + " (" + voiceDescription.getLocale() + " "
				+ voiceDescription.getGender() + ")");
	}

	private void convert() throws Exception {
		logger.info("converting...");
		File rootDir = mary4Zip.getParentFile();
		extractedDir = new File(rootDir, voiceDescription.getName() + "-" + voiceDescription.getVersion());
		logger.debug("... extracting archive to " + extractedDir.getPath());
		if (extractedDir.exists()) {
			logger.debug("Folder " + extractedDir.getPath() + " exists, trying to delete...");
			extractedDir.delete();
		}
		FileUtils.unzipArchive(mary4Zip, extractedDir);

		loadConfig(findConfigFile());

		compileDir = new File(rootDir, voiceDescription.getName() + "-" + Version.specificationVersion() + "-maven");

		domain = config.getProperty(getPropertyPrefix() + "domain");
		samplingRate = Integer.parseInt(config.getProperty(getPropertyPrefix() + "samplingRate"));

		filesForResources = getFilesForResources();
		filesForFilesystem = getFilesForFilesystem();
		Map<String, String> extraVariablesToSubstitute = null;

		compiler = new VoiceCompiler.MavenVoiceCompiler(compileDir, getVoiceName(), Version.specificationVersion(),
				voiceDescription.getLocale(), voiceDescription.getGender(), domain, samplingRate, isUnitSelectionVoice(),
				filesForResources, filesForFilesystem, extraVariablesToSubstitute);

		logger.debug("Creating directories");
		compiler.createDirectories();

		logger.debug("Copying template files");
		compiler.copyTemplateFiles();

		updateConfig(compiler.getPackageName());
		saveConfig(compiler.getConfigFile());

		logger.debug("Copying voice files");
		compiler.copyVoiceFiles();

		if (!isUnitSelectionVoice()) {
			logger.debug("Converting HMM PDF files from Mary 4.0 to Mary 5.0 format");
			convertMary4ToMary5HmmPdfFiles(compiler.getMainResourcesDir());
		}

		logger.debug("Compiling with Maven");
		compiler.compileWithMaven();

		String convertedZipFilename = getFilenamePrefix() + ".zip";
		File convertedZipFile = new File(compileDir + "/target/" + convertedZipFilename);
		if (!convertedZipFile.exists()) {
			throw new IOException("Maven should have created file " + convertedZipFile.getAbsolutePath()
					+ " but file does not exist.");
		}

		updateVoiceDescription(rootDir, convertedZipFile);

		File finalZipFile = new File(rootDir, convertedZipFilename);
		if (finalZipFile.exists()) {
			finalZipFile.delete();
		}
		boolean success = convertedZipFile.renameTo(finalZipFile);
		if (!success) {
			throw new IOException("Failure trying to move " + convertedZipFile.getAbsolutePath() + " to "
					+ finalZipFile.getAbsolutePath());
		}

	}

	protected void saveConfig(File configFile) throws IOException {
		saveConfigToStream(new BufferedOutputStream(new FileOutputStream(configFile)));
	}

	private boolean isEmpty(String line) {
		return line.trim().isEmpty();
	}

	private boolean isComment(String line) {
		return line.trim().startsWith("#");

	}

	protected void saveConfigToStream(OutputStream out) throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
		StringBuilder comments = new StringBuilder();
		try {
			for (String line : originalConfig) {
				if (isEmpty(line) || isComment(line)) {
					comments.append(line).append(EOL);
					continue;
				}
				String key = new StringTokenizer(line.trim()).nextToken();
				if (config.containsKey(key)) {
					pw.print(comments.toString());
					pw.print(key + " = " + config.getProperty(key) + EOL);
					if (key.equals("name")) {
						pw.print("locale = " + config.getProperty("locale") + EOL);
					}
				}
				comments = new StringBuilder();
			}
		} finally {
			pw.flush();
			pw.close();
		}
	}

	private File[] getFilesForFilesystem() throws IOException {
		return getFilesFromProperties(PROPS_FOR_FILESYSTEM);
	}

	private File[] getFilesForResources() throws IOException {
		return getFilesFromProperties(PROPS_FOR_RESOURCES);
	}

	private File[] getFilesFromProperties(String[] propertySuffixes) throws IOException {
		ArrayList<File> files = new ArrayList<File>();
		for (String suffix : propertySuffixes) {
			String key = getPropertyPrefix() + suffix;
			if (config.containsKey(key)) {
				String value = config.getProperty(key);
				if (!value.startsWith("MARY_BASE")) {
					throw new IOException("Property '" + key + "' should hold a filename but the value is '" + value + "'");
				}
				value = value.replace("MARY_BASE", extractedDir.getAbsolutePath());
				File f = new File(value);
				if (!f.exists()) {
					throw new IOException("Config file refers to non-existing file '" + f.getAbsolutePath() + "'");
				}
				files.add(f);
			}
		}
		return files.toArray(new File[0]);
	}

	protected void updateConfig(String packageName) {
		updatePropsForResources(packageName);
		dropOutdatedProps();
		addNewProps();
	}

	private void addNewProps() {
		config.setProperty("locale", voiceDescription.getLocale().toString());

	}

	private void dropOutdatedProps() {
		String[] suffixesToDrop = isUnitSelectionVoice() ? SUFFIXES_TO_DROP_UNITSEL : SUFFIXES_TO_DROP_HMM;
		for (String suffix : suffixesToDrop) {
			String key = getPropertyPrefix() + suffix;
			config.remove(key);
		}
		for (String prop : PROPS_TO_DROP) {
			config.remove(prop);
		}
	}

	private void updatePropsForResources(String packageName) {
		String oldPrefix = "MARY_BASE/lib/voices/(.*)/";
		String newPrefix = "jar:/marytts/voice/" + packageName + "/";
		for (String suffix : PROPS_FOR_RESOURCES) {
			String key = getPropertyPrefix() + suffix;
			if (config.containsKey(key)) {
				String value = config.getProperty(key);
				value = value.replaceFirst(oldPrefix, newPrefix);
				config.setProperty(key, value);
			}
		}
	}

	private File findConfigFile() throws IOException {
		File confDir = new File(extractedDir, "conf");
		if (!confDir.isDirectory()) {
			throw new IOException("Expected directory " + confDir.getAbsolutePath() + " doesn't exist.");
		}
		File[] confFiles = confDir.listFiles();
		if (confFiles.length != 1) {
			throw new IOException("Conf directory " + confDir.getAbsolutePath()
					+ " should contain exactly one config file but contains " + confFiles.length);
		}
		return confFiles[0];
	}

	protected void loadConfig(File configFile) throws IOException {
		FileInputStream configStream = new FileInputStream(configFile);
		InputStream bufferedConfigStream = IOUtils.toBufferedInputStream(configStream);
		loadConfigFromStream(bufferedConfigStream);
	}

	protected void loadConfigFromStream(InputStream in) throws IOException {
		config = new Properties();
		try {
			byte[] byteArray = IOUtils.toByteArray(in);
			config.load(new ByteArrayInputStream(byteArray));
			originalConfig = IOUtils.readLines(new ByteArrayInputStream(byteArray), "UTF-8");
		} finally {
			in.close();
		}
	}

	/**
	 * Returns true for a unit selection voice, false for an HMM-based voice.
	 * 
	 * @return true if config.containsKey("unitselection.voices.list"), false if config.containsKey("hmm.voices.list")
	 * @throws UnsupportedOperationException
	 *             if the voice is neither a unit selection nor an HMM-based voice.
	 */
	protected boolean isUnitSelectionVoice() throws UnsupportedOperationException {
		if (config.containsKey("unitselection.voices.list")) {
			return true;
		} else if (config.containsKey("hmm.voices.list")) {
			return false;
		} else {
			throw new UnsupportedOperationException(
					"The voice is neither a unit selection voice nor an HMM-based voice -- cannot convert to MARY 5 format.");
		}
	}

	private String getVoiceNameFromConfig() {
		if (isUnitSelectionVoice()) {
			return config.getProperty("unitselection.voices.list");
		}
		return config.getProperty("hmm.voices.list");
	}

	private String getVoiceNameFromVoiceDescription() {
		return voiceDescription.getName();
	}

	protected String getVoiceName() {
		String voiceNameFromConfig = getVoiceNameFromConfig();
		String voiceNameFromVoiceDescription = getVoiceNameFromVoiceDescription();
		if (!voiceNameFromConfig.equals(voiceNameFromVoiceDescription)) {
			logger.warn("Name discrepancy: component.xml says '" + voiceNameFromVoiceDescription + "', config file says '"
					+ voiceNameFromConfig + "'");
		}
		return voiceNameFromVoiceDescription;
	}

	protected String getPropertyPrefix() {
		return "voice." + getVoiceNameFromConfig() + ".";
	}

	private String getFilenamePrefix() {
		return "voice-" + voiceDescription.getName() + "-" + Version.specificationVersion();
	}

	private void updateVoiceDescription(File rootDir, File packageFile) throws MalformedURLException,
			ParserConfigurationException, MaryConfigurationException, IOException {
		logger.debug("writing new voice description...");
		voiceDescription.setVersion(Version.specificationVersion());
		voiceDescription.setDependsVersion(Version.specificationVersion());
		voiceDescription.setPackageFilename(packageFile.getName());
		voiceDescription.setPackageMD5Sum(computeMD5(packageFile));
		voiceDescription.setPackageSize((int) packageFile.length());
		voiceDescription.removeAllLocations();
		voiceDescription.addLocation(URI.create("http://mary.dfki.de/download/" + Version.specificationVersion() + "/").toURL());
		Document doc = voiceDescription.createComponentXML();
		File newVoiceDescriptionFile = new File(rootDir, getFilenamePrefix() + "-component.xml");
		DomUtils.document2File(doc, newVoiceDescriptionFile);
		logger.debug("... created " + newVoiceDescriptionFile.getPath());
	}

	private String computeMD5(File packageFile) throws IOException {
		return MD5.asHex(MD5.getHash(packageFile));
	}

	private void convertMary4ToMary5HmmPdfFiles(File mainResourcesDir) throws Exception {

		File list[] = mainResourcesDir.listFiles();
		for (File f : list) {
			// if mainResources dir contains f0.pdf mgc.pdf str.pdf and
			if (f.getName().contains("dur.pdf") || f.getName().contains("lf0.pdf") || f.getName().contains("mgc.pdf")
					|| f.getName().contains("str.pdf")) {
				logger.debug("converting file: " + f.getName());
				convertPdfBinaryFile(f);
			}
			// if mainResource contains gv-lf0-littend.pdf, gv-mgc-littend.pdf and gv-str-littend.pdf
			else if (f.getName().contains("gv-lf0-littend.pdf") || f.getName().contains("gv-mgc-littend.pdf")
					|| f.getName().contains("gv-str-littend.pdf")) {
				logger.debug("converting file: " + f.getName());
				convertGvBinaryFile(f);
			}
		}

	}

	/**
	 * Converts format from pdf Mary format 4 to Mary 5, the converted file will have the same input name
	 * 
	 * @param pdfInFile
	 *            pdfInFile
	 * @throws Exception
	 *             Exception
	 */
	public void convertPdfBinaryFile(File pdfInFile) throws Exception {
		int i, j, k, l;
		boolean lf0 = false;

		String pdfInFileString = pdfInFile.getName();
		// the destination file name will be the same as the input file so
		String pdfOutFile = pdfInFile.getAbsolutePath();
		String path = pdfInFile.getParent();
		// I make a copy or the original file
		FileUtils.copy(pdfInFile.getAbsolutePath(), path + "/tmp");
		pdfInFile = new File(path + "/tmp");

		DataInputStream dataIn;
		DataOutputStream dataOut;

		dataIn = new DataInputStream(new BufferedInputStream(new FileInputStream(pdfInFile)));
		// numMSDFlag
		int numMSDFlag = 0;
		// numStream
		int numStream = 1; // 1 for mgc, str, mag or dur
		// vectorSize
		int vectorSize;
		// numDurPdf
		int numPdf[];

		logger.debug("Reading: from file " + pdfInFileString);
		float pdf[]; // pdf[vectorSize];
		float fval;
		int numState = 5;

		// ---------------------------------------------------------------------------------
		// ------------ Read header --------------------------------------------------------
		// ---------------------------------------------------------------------------------
		vectorSize = dataIn.readInt();
		if (pdfInFileString.contains("lf0.pdf")) {
			numStream = vectorSize;
			vectorSize = 4; // vectorSize = 4 --> [1]:mean f0, [2]:var f0, [3]:voiced weight, [4]:unvoiced weight
			lf0 = true;
			numMSDFlag = 1;
		} else if (pdfInFileString.contains("dur.pdf")) {
			/* 2*nstate because the vector size for duration is the number of states */
			// pdf = new double[1][numDurPdf][1][2*numState]; // just one state and one stream
			numState = 1;
			numStream = 5;
		}
		logger.debug("vectorSize(r) = " + vectorSize + " numMSDFlag=" + numMSDFlag + " numStream=" + numStream + " numState="
				+ numState);

		/* Now we need the number of pdf's for each state */
		numPdf = new int[numState];
		for (i = 0; i < numState; i++) {
			numPdf[i] = dataIn.readInt();
			logger.debug("loadPdfs(r): numPdf[state:" + i + "]=" + numPdf[i]);
			if (numPdf[i] < 0)
				throw new Exception("loadPdfs: #pdf at state " + i + " must be positive value.");
		}
		pdf = new float[2 * vectorSize];

		dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pdfOutFile)));
		// This is the format in version 2.0
		// numMSDFlag
		dataOut.writeInt(numMSDFlag);
		// numStream
		dataOut.writeInt(numStream);
		// vectorSize (for lf0 vectorsize is the same numStream)
		if (lf0)
			dataOut.writeInt(numStream);
		else
			dataOut.writeInt(vectorSize);
		// numPdf per state
		for (i = 0; i < numState; i++) {
			dataOut.writeInt(numPdf[i]);
		}

		// ---------------------------------------------------------------------------------
		// -------------- Now read the data ------------------------------------------------
		// in the old version the mean vector goes first and then the cov
		// in the new version the mean and cov elements of the vector are one after another
		// ---------------------------------------------------------------------------------
		if (lf0) {
			/* read pdfs (mean, variance). (2*vectorSize because mean and diag variance */
			/* are allocated in only one vector. */
			for (i = 0; i < numState; i++) {
				for (j = 0; j < numPdf[i]; j++) {
					for (k = 0; k < numStream; k++) {
						for (l = 0; l < vectorSize; l++) {
							fval = dataIn.readFloat();
							// NOTE: Here (hts_engine v1.04) the order seem to be the same as before
							dataOut.writeFloat(fval);
						}

					}
				}
				// System.out.println("New pdf  j=" + j);
			}
		} else {
			/* read pdfs (mean, variance). (2*vectorSize because mean and diag variance */
			/* are allocated in only one vector. */
			for (i = 0; i < numState; i++) {
				for (j = 0; j < numPdf[i]; j++) {
					for (k = 0; k < (2 * vectorSize); k++) {
						pdf[k] = dataIn.readFloat();
					}
					for (k = 0; k < vectorSize; k++) {
						dataOut.writeFloat(pdf[k]);
						dataOut.writeFloat(pdf[k + vectorSize]);
					}
				}
				// System.out.println("New pdf  j=" + j);
			}

		}
		dataIn.close();
		dataOut.close();
		pdfInFile.delete();
		logger.debug("Updated format in file " + pdfOutFile);

	}

	/**
	 * Converts file format from gv Mary format 4 to Mary 5, the converted file will have the same input name
	 * 
	 * @param gvInFile
	 *            gvInFile
	 * @throws IOException
	 *             IOException
	 */
	public void convertGvBinaryFile(File gvInFile) throws IOException {
		int i;
		String gvInFileString = gvInFile.getName();
		// the destination file name will be the same as the input file so
		String gvOutFile = gvInFile.getAbsolutePath();
		String path = gvInFile.getParent();
		// I make a copy or the original file
		FileUtils.copy(gvInFile.getAbsolutePath(), path + "/tmp");
		gvInFile = new File(path + "/tmp");

		DataInputStream dataIn;
		DataOutputStream dataOut;
		dataIn = new DataInputStream(new BufferedInputStream(new FileInputStream(gvInFile)));

		// int numMix = data_in.readShort(); /* --NOT USED -- first short is the number of mixtures in Gaussian model */
		int order = dataIn.readShort(); /* second short is the order of static vector */
		float gvmean[] = new float[order]; /* allocate memory of this size */
		float gvcov[] = new float[order];
		logger.debug("Reading from file " + gvInFileString + " order=" + order);

		for (i = 0; i < order; i++) {
			gvmean[i] = dataIn.readFloat();
			// System.out.format("gvmean[%d]=%f\n",i,gvmean[i]);
		}
		for (i = 0; i < order; i++) {
			gvcov[i] = dataIn.readFloat();
			// System.out.format("gvcov[%d]=%f\n",i,gvcov[i]);
		}
		dataIn.close();

		dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(gvOutFile)));
		/* This is the format in version 2.0 */
		// numMSDFlag
		dataOut.writeInt(0);
		// numStream
		dataOut.writeInt(1);
		// vectorSize
		dataOut.writeInt(order);
		// numDurPdf
		dataOut.writeInt(1);

		for (i = 0; i < order; i++) {
			dataOut.writeFloat(gvmean[i]);
			dataOut.writeFloat(gvcov[i]);
		}

		dataOut.close();
		gvInFile.delete();
		logger.debug("Updated format in file " + gvOutFile);

	}

	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}

		File componentsFile = new File(args[0]);
		if (!componentsFile.exists()) {
			System.err.println("No component file: " + args[0]);
			usage();
			System.exit(1);
		}

		List<VoiceComponentDescription> voiceDescriptions = null;
		try {
			InstallFileParser parser = new InstallFileParser(componentsFile.toURI().toURL());
			voiceDescriptions = parser.getVoiceDescriptions();
		} catch (Exception e) {
			e.printStackTrace();
			usage();
			System.exit(1);
		}

		for (int i = 1; i < args.length; i++) {
			File voiceZip = new File(args[i]);
			if (!voiceZip.exists()) {
				System.err.println("No such voice file: " + args[i]);
				usage();
				System.exit(1);
			}
			try {
				new Mary4To5VoiceConverter(voiceDescriptions, voiceZip).convert();
			} catch (Exception e) {
				e.printStackTrace();
				usage();
				System.exit(1);
			}
		}

	}

	private static void usage() {
		System.err.println("Usage:");
		System.err.println("java " + Mary4To5VoiceConverter.class.getName()
				+ " mary-components.xml mary-voice-file-4.3.0.zip [more voice files...]");
	}
}
