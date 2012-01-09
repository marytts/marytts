package marytts.tools.upgrade;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import marytts.exceptions.MaryConfigurationException;
import marytts.tools.install.InstallFileParser;
import marytts.tools.install.VoiceComponentDescription;
import marytts.tools.voiceimport.VoiceCompiler;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;
import marytts.util.io.FileUtils;

public class Mary4To5VoiceConverter {

	private static final String NEW_VERSION = "5.0-SNAPSHOT";
	
	/**
	 * The list of property suffixes which can read from a resource file. 
	 */
	private static final String[] PROPS_FOR_RESOURCES = new String[] {
		"Ftd", "Ftf", "Ftm", "Fts", 
		"Fmd", "Fmf", "Fmm", "Fms",
		"Fgvf", "Fgvm", "Fgvs",
		"FeaFile",
		"trickyPhonesFile",
		"Fif",
	};
	
	/**
	 * The list of property suffixes which should be dropped when upgrading the config file.
	 */
	private static final String[] PROPS_TO_DROP = new String[] {
		"duration.data", "F0.data"
	};
	
	private Logger logger;
	private VoiceComponentDescription voiceDescription;
	private File mary4Zip;
	private Properties config;
	private String propertyPrefix;
	
	private File extractedDir;
	private File compileDir;
	private String voiceName;
	private Locale locale;
	private String gender;
	private String domain;
	private int samplingRate;
	private boolean isUnitSelectionVoice;
	private File[] filesForResources;
	private File[] filesForFilesystem;
	
	VoiceCompiler.MavenVoiceCompiler compiler;
	
	public Mary4To5VoiceConverter(
			List<VoiceComponentDescription> voiceDescriptions, File voiceZip) {
		voiceDescription = null;
		mary4Zip = voiceZip;
		for (VoiceComponentDescription d : voiceDescriptions) {
			if (d.getPackageFilename().equals(mary4Zip.getName())) {
				voiceDescription = d;
				break;
			}
		}
		if (voiceDescription == null) {
			throw new IllegalArgumentException("No matching voice description for file "+mary4Zip.getName());
		}
		if (!MaryUtils.isLog4jConfigured()) {
			BasicConfigurator.configure();
		}
		logger = Logger.getLogger(this.getClass());
		logger.info(voiceDescription.getName()+" "+voiceDescription.getVersion()+" ("+voiceDescription.getLocale()+" "+voiceDescription.getGender()+")");
	}
	
	private void convert() throws Exception {
		logger.info("converting...");
		extractedDir = new File(mary4Zip.getParentFile(), voiceDescription.getName()+"-"+voiceDescription.getVersion());
		logger.debug("... extracting archive to "+extractedDir.getPath());
		FileUtils.unzipArchive(mary4Zip, extractedDir);
		
		
		loadConfig(findConfigFile());
		
		compileDir = new File(mary4Zip.getParentFile(), voiceDescription.getName()+"-"+NEW_VERSION+"-maven");
		voiceName = voiceDescription.getName();

		String voiceNameFromConfig;
		if (config.containsKey("unitselection.voices.list")) {
			voiceNameFromConfig = config.getProperty("unitselection.voices.list");
			isUnitSelectionVoice = true;
		} else if (config.containsKey("hmm.voices.list")) {
			voiceNameFromConfig = config.getProperty("hmm.voices.list");
			isUnitSelectionVoice = false;
		} else {
			throw new UnsupportedOperationException("The voice '"+voiceName+"' is neither a unit selection voice nor an HMM-based voice -- cannot convert to MARY 5 format.");
		}
		if (!voiceName.equals(voiceNameFromConfig)) {
			logger.warn("Name discrepancy: component.xml says '"+voiceName+"', config file says '"+voiceNameFromConfig+"'");
		}
		propertyPrefix = "voice."+voiceNameFromConfig+".";
		
		locale = voiceDescription.getLocale();
		gender = voiceDescription.getGender();
		domain = config.getProperty(propertyPrefix+"domain");
		samplingRate = Integer.parseInt(config.getProperty(propertyPrefix+"samplingRate"));
		
		filesForResources = getFilesForResources();
		filesForFilesystem = getFilesForFilesystem();
		Map<String, String> extraVariablesToSubstitute = null;
		
		compiler = new VoiceCompiler.MavenVoiceCompiler(compileDir, voiceName, NEW_VERSION, locale, gender, domain, samplingRate, isUnitSelectionVoice, filesForResources, filesForFilesystem, extraVariablesToSubstitute);

		logger.debug("Creating directories");
		compiler.createDirectories();
		
		logger.debug("Copying template files");
		compiler.copyTemplateFiles();

		updateConfig();
		saveConfig(compiler.getConfigFile());
		
		
		logger.debug("Copying voice files");
		compiler.copyVoiceFiles();
		
		logger.debug("Compiling with Maven");
		compiler.compileWithMaven();
		
		String newZipFilename = getFilenamePrefix()+".zip";
		
		updateVoiceDescription();
	}

	private void saveConfig(File configFile) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(configFile));
		try {
			config.store(out, null);
		} finally {
			out.close();
		}
	}

	private File[] getFilesForFilesystem() {
		return null;
	}

	private File[] getFilesForResources() throws IOException {
		ArrayList<File> files = new ArrayList<File>();
		for (String suffix : PROPS_FOR_RESOURCES) {
			String key = propertyPrefix+suffix;
			if (config.containsKey(key)) {
				String value = config.getProperty(key);
				if (!value.startsWith("MARY_BASE")) {
					throw new IOException("Property '"+key+"' should hold a filename but the value is '"+value+"'");
				}
				value = value.replace("MARY_BASE", extractedDir.getAbsolutePath());
				File f = new File(value);
				if (!f.exists()) {
					throw new IOException("Config file refers to non-existing file '"+f.getAbsolutePath()+"'");
				}
				files.add(f);
			}
		}
		return files.toArray(new File[0]);
	}
	
	
	private void updateConfig() {
		updatePropsForResources();
		dropOutdatedProps();
		addNewProps();
	}

	private void addNewProps() {
		config.setProperty("locale", locale.toString());
		
	}

	private void dropOutdatedProps() {
		for (String suffix : PROPS_TO_DROP) {
			String key = propertyPrefix+suffix;
			if (config.containsKey(key)) {
				config.remove(key);
			}
		}
	}

	private void updatePropsForResources() {
		String oldPrefix = "MARY_BASE/lib/voices/(.*)/";
		String newPrefix = "jar:/marytts/voice/"+compiler.getPackageName()+"/";
		for (String suffix : PROPS_FOR_RESOURCES) {
			String key = propertyPrefix+suffix;
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
			throw new IOException("Expected directory "+confDir.getAbsolutePath()+" doesn't exist.");
		}
		File[] confFiles = confDir.listFiles();
		if (confFiles.length != 1) {
			throw new IOException("Conf directory "+confDir.getAbsolutePath()+" should contain exactly one config file but contains "+confFiles.length);
		}
		return confFiles[0];
	}

	private void loadConfig(File configFile) throws IOException {
		config = new Properties();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(configFile));
		try {
			config.load(in);
		} finally {
			in.close();
		}
		
	}

	private String getFilenamePrefix() {
		return "mary-"+voiceDescription.getName()+"-"+NEW_VERSION;
	}

	private void updateVoiceDescription()
			throws MalformedURLException, ParserConfigurationException,
			MaryConfigurationException, IOException {
		logger.debug("writing new voice description...");
		voiceDescription.setVersion(NEW_VERSION);
		voiceDescription.setDependsVersion(NEW_VERSION);
		voiceDescription.setPackageFilename(getFilenamePrefix()+".zip");
		voiceDescription.removeAllLocations();
		voiceDescription.addLocation(URI.create("http://mary.dfki.de/download/"+NEW_VERSION+"/").toURL());
		Document doc = voiceDescription.createComponentXML();
		File newVoiceDescriptionFile = new File(mary4Zip.getParentFile(), getFilenamePrefix() + "-component.xml");
		DomUtils.document2File(doc, newVoiceDescriptionFile);
		logger.debug("... created "+newVoiceDescriptionFile.getPath());
	}

	
	

	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		
		File componentsFile = new File(args[0]);
		if (!componentsFile.exists()) {
			System.err.println("No component file: "+args[0]);
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
		
		for (int i=1; i<args.length; i++) {
			File voiceZip = new File(args[i]);
			if (!voiceZip.exists()) {
				System.err.println("No such voice file: "+args[i]);
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
		System.err.println("java "+Mary4To5VoiceConverter.class.getName()+" mary-components.xml mary-voice-file-4.3.0.zip [more voice files...]");
	}
}
