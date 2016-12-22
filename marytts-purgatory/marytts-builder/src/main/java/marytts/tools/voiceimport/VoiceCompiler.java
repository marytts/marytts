/**
 * 
 */
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;
import marytts.util.io.StreamGobbler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;

import com.twmacinta.util.MD5;

/**
 * @author marc
 *
 */
public class VoiceCompiler extends VoiceImportComponent {

	// constants to access filenames in database component properties and organize file list:

	public static final String CARTFILE = "CARTBuilder.cartFile";

	public static final String DURTREE = "DurationCARTTrainer.durTree";

	public static final String F0LEFTTREE = "F0CARTTrainer.f0LeftTreeFile";

	public static final String F0MIDTREE = "F0CARTTrainer.f0MidTreeFile";

	public static final String F0RIGHTTREE = "F0CARTTrainer.f0RightTreeFile";

	public static final String HALFPHONEFEATSAC = "AcousticFeatureFileWriter.acFeatureFile";

	public static final String HALFPHONEFEATDEFAC = "AcousticFeatureFileWriter.acFeatDef";

	public static final String HALFPHONEUNITS = "HalfPhoneUnitfileWriter.unitFile";

	public static final String JOINCOSTFEATS = "JoinCostFileMaker.joinCostFile";

	public static final String JOINCOSTWEIGHTS = "JoinCostFileMaker.weightsFile";

	public static final String PHONEFEATDEF = "PhoneFeatureFileWriter.weightsFile";

	public static final String WAVETIMELINE = "WaveTimelineMaker.waveTimeline";

	public static final String BASETIMELINE = "BasenameTimelineMaker.timelineFile";

	public final String COMPILEDIR = getName() + ".compileDir";

	public final String MVN = getName() + ".mavenBin";

	protected MavenVoiceCompiler compiler;

	/**
	 * 
	 */
	public VoiceCompiler() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#compute()
	 */
	@Override
	public boolean compute() throws Exception {

		File compileDir = new File(getProp(COMPILEDIR));
		compiler = createCompiler(compileDir);

		if (!isUnitSelectionVoice()) {
			mapFeatures();
		}

		logger.info("Creating directories");
		compiler.createDirectories();

		logger.info("Copying template files");
		compiler.copyTemplateFiles();

		logger.info("Copying voice files");
		compiler.copyVoiceFiles();

		logger.info("Compiling with Maven");
		compiler.compileWithMaven();

		// logger.info("Creating component description file");
		// compiler.createComponentFile();
		logger.info("done.");

		return true;
	}

	protected void mapFeatures() throws Exception {
		throw new IllegalStateException("This method should not be called for unit selection voices, "
				+ " and hmm-based voices should extend it.");
	}

	protected boolean isUnitSelectionVoice() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getDefaultProps(marytts.tools.voiceimport.DatabaseLayout)
	 */
	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(COMPILEDIR, new File(db.getVoiceFileDir(), "voice-" + getVoiceName(db)).getAbsolutePath());
			props.put(MVN, "/usr/bin/mvn");
		}
		return props;
	}

	protected String getVoiceName(DatabaseLayout db) {
		return db.getVoiceName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getName()
	 */
	@Override
	public String getName() {
		return "VoiceCompiler";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#getProgress()
	 */
	@Override
	public int getProgress() {
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see marytts.tools.voiceimport.VoiceImportComponent#setupHelp()
	 */
	@Override
	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(COMPILEDIR, "The directory in which the files for compiling the voice will be copied.");
		props2Help.put(MVN, "The path to the Maven binary (i.e., mvn).");
	}

	protected Map<String, String> getExtraVariableSubstitutionMap() {
		return null;
	}

	protected File[] getFilesForResources() {
		String[] propsResources = new String[] { CARTFILE, DURTREE, F0LEFTTREE, F0MIDTREE, F0RIGHTTREE, HALFPHONEFEATDEFAC,
				JOINCOSTWEIGHTS };
		File[] filesForResources = new File[propsResources.length];
		for (int i = 0; i < propsResources.length; i++) {
			filesForResources[i] = new File(db.getProperty(propsResources[i]));
		}
		return filesForResources;
	}

	protected File[] getFilesForFilesystem() {
		String[] propsFilesystem = new String[] { HALFPHONEFEATSAC, HALFPHONEUNITS, JOINCOSTFEATS, BASETIMELINE, WAVETIMELINE };
		File[] filesForFilesystem = new File[propsFilesystem.length];
		for (int i = 0; i < propsFilesystem.length; i++) {
			filesForFilesystem[i] = new File(db.getProperty(propsFilesystem[i]));
		}
		return filesForFilesystem;
	}

	protected MavenVoiceCompiler createCompiler(File compileDir) {
		File[] filesForResources = getFilesForResources();
		File[] filesForFilesystem = getFilesForFilesystem();
		Map<String, String> extraVariablesToSubstitute = getExtraVariableSubstitutionMap();
		return new MavenVoiceCompiler(getProp(MVN), compileDir, getVoiceName(db), db.getMaryVersion(), db.getLocale(),
				db.getGender(), db.getDomain(), db.getSamplingRate(), isUnitSelectionVoice(), filesForResources,
				filesForFilesystem, extraVariablesToSubstitute);
	}

	public static class MavenVoiceCompiler {
		protected File compileDir;
		protected String voiceName;
		protected String voiceVersion;
		protected Locale locale;
		protected String gender;
		protected String domain;
		protected int samplingRate;
		protected boolean isUnitSelectionVoice;
		protected File[] filesForResources;
		protected File[] filesForFilesystem;
		protected Map<String, String> extraVariablesToSubstitute;

		protected StrSubstitutor substitutor;
		protected File mainJavaDir;
		protected File mainResourcesDir;
		protected File nonPackagedResourcesDir;
		protected File mainDescriptionsDir;
		protected File metaInfDir;
		protected File testJavaDir;
		protected File libVoiceDir;
		protected String mvn;

		/**
		 * @deprecated Use constructor with path to Maven instead.
		 * @param compileDir
		 *            compileDir
		 * @param voiceName
		 *            voiceName
		 * @param voiceVersion
		 *            voiceVersion
		 * @param locale
		 *            locale
		 * @param gender
		 *            gender
		 * @param domain
		 *            domain
		 * @param samplingRate
		 *            samplingRate
		 * @param isUnitSelectionVoice
		 *            isUnitSelectionVoice
		 * @param filesForResources
		 *            filesForResources
		 * @param filesForFilesystem
		 *            filesForFilesystem
		 * @param extraVariablesToSubstitute
		 *            extraVariablesToSubstitute
		 */
		@Deprecated
		public MavenVoiceCompiler(File compileDir, String voiceName, String voiceVersion, Locale locale, String gender,
				String domain, int samplingRate, boolean isUnitSelectionVoice, File[] filesForResources,
				File[] filesForFilesystem, Map<String, String> extraVariablesToSubstitute) {
			this("mvn", compileDir, voiceName, voiceVersion, locale, gender, domain, samplingRate, isUnitSelectionVoice,
					filesForResources, filesForFilesystem, extraVariablesToSubstitute);
		}

		public MavenVoiceCompiler(String mvn, File compileDir, String voiceName, String voiceVersion, Locale locale,
				String gender, String domain, int samplingRate, boolean isUnitSelectionVoice, File[] filesForResources,
				File[] filesForFilesystem, Map<String, String> extraVariablesToSubstitute) {
			this.mvn = mvn;
			this.compileDir = compileDir;
			this.voiceName = voiceName.replaceAll("[^\\w\\-]", "");
			this.voiceVersion = voiceVersion;
			this.locale = locale;
			this.gender = gender;
			this.domain = domain;
			this.samplingRate = samplingRate;
			this.isUnitSelectionVoice = isUnitSelectionVoice;
			this.substitutor = new StrSubstitutor(getVariableSubstitutionMap(extraVariablesToSubstitute));

			this.filesForResources = filesForResources;
			this.filesForFilesystem = filesForFilesystem;
		}

		protected Map<String, String> getVariableSubstitutionMap(Map<String, String> extra) {
			Map<String, String> m = new HashMap<String, String>();
			m.put("MARYVERSION", voiceVersion);
			m.put("VOICENAME", voiceName);
			m.put("LOCALE", MaryUtils.locale2xmllang(locale));
			m.put("LANG", locale.getLanguage());
			m.put("DISPLAYLANG", locale.getDisplayLanguage());
			m.put("GENDER", gender);
			m.put("DOMAIN", domain);
			m.put("SAMPLINGRATE", String.valueOf(samplingRate));
			m.put("PACKAGE", getPackageName());
			m.put("VOICECLASS", isUnitSelectionVoice ? "marytts.unitselection.UnitSelectionVoice" : "marytts.htsengine.HMMVoice");
			if (extra != null) {
				m.putAll(extra);
			}
			return m;
		}

		public void createDirectories() throws IOException {
			if (compileDir.exists()) {
				FileUtils.deleteDirectory(compileDir);
			}
			compileDir.mkdir();
			String packageName = getPackageName();
			mainJavaDir = new File(compileDir.getAbsolutePath() + "/src/main/java/marytts/voice/" + packageName);
			mainJavaDir.mkdirs();
			mainResourcesDir = new File(compileDir.getAbsolutePath() + "/src/main/resources/marytts/voice/" + packageName);
			mainResourcesDir.mkdirs();
			nonPackagedResourcesDir = new File(compileDir.getAbsolutePath() + "/src/non-packaged-resources");
			nonPackagedResourcesDir.mkdirs();
			mainDescriptionsDir = new File(compileDir.getAbsolutePath() + "/src/main/descriptors");
			mainDescriptionsDir.mkdirs();
			metaInfDir = new File(compileDir.getAbsolutePath() + "/src/main/resources/META-INF/services");
			metaInfDir.mkdirs();
			testJavaDir = new File(compileDir.getAbsolutePath() + "/src/test/java/marytts/voice/" + packageName);
			testJavaDir.mkdirs();
			if (filesForFilesystem != null && filesForFilesystem.length > 0) {
				libVoiceDir = new File(compileDir.getAbsolutePath() + "/lib/voices/" + voiceName);
				libVoiceDir.mkdir();
			}
		}

		public void copyTemplateFiles() throws IOException {
			copyWithVarSubstitution("pom.xml", new File(compileDir, "pom.xml"));
			copyWithVarSubstitution("generateComponentFile.groovy", new File(nonPackagedResourcesDir,
					"generateComponentFile.groovy"));
			copyWithVarSubstitution("installable.xml", new File(mainDescriptionsDir, "installable.xml"));
			copyWithVarSubstitution("Config.java", new File(mainJavaDir, "Config.java"));
			copyWithVarSubstitution("LoadVoiceIT.java", new File(testJavaDir, "LoadVoiceIT.java"));
			copyWithVarSubstitution("marytts.config.MaryConfig", new File(metaInfDir, "marytts.config.MaryConfig"));
			if (isUnitSelectionVoice) {
				copyWithVarSubstitution("unitselection-voice.config", getConfigFile());
			} else {
				copyWithVarSubstitution("hsmm-voice.config", getConfigFile());
			}
		}

		public void setFilesForResources(File[] filesForResources) {
			this.filesForResources = filesForResources;
		}

		public File getConfigFile() {
			return new File(mainResourcesDir, "voice.config");
		}

		public File getMainResourcesDir() {
			return mainResourcesDir;
		}

		private void copyWithVarSubstitution(String resourceName, File destination, StrSubstitutor... moreSubstitutors)
				throws IOException {
			String resource = marytts.util.io.FileUtils.getStreamAsString(
					getClass().getResourceAsStream("templates/" + resourceName), "UTF-8");
			String resourceWithReplacements = substitutor.replace(resource);
			for (StrSubstitutor more : moreSubstitutors) {
				resourceWithReplacements = more.replace(resourceWithReplacements);
			}
			PrintWriter out = new PrintWriter(destination, "UTF-8");
			out.print(resourceWithReplacements);
			out.close();
		}

		public void copyVoiceFiles() throws IOException {

			if (filesForResources != null) {
				for (File f : filesForResources) {
					FileUtils.copyFileToDirectory(f, mainResourcesDir);
				}
			}

			if (filesForFilesystem != null) {
				for (File f : filesForFilesystem) {
					FileUtils.copyFileToDirectory(f, libVoiceDir);
				}
			}
		}

		public void compileWithMaven() throws IOException, InterruptedException {
			Process maven = Runtime.getRuntime().exec(this.mvn + " verify", null, compileDir);
			StreamGobbler merr = new StreamGobbler(maven.getErrorStream(), "maven err");
			StreamGobbler mout = new StreamGobbler(maven.getInputStream(), "maven out");
			merr.start();
			mout.start();
			int result = maven.waitFor();
			if (result != 0) {
				throw new IOException("Maven compilation did not succeed -- check console for details.");
			}
		}

		// public void createComponentFile() throws IOException {
		// String zipFileName = substitutor.replace("voice-${VOICENAME}-${MARYVERSION}.zip");
		// File zipFile = new File(compileDir.getAbsolutePath()+"/target/"+zipFileName);
		// String zipFileMd5Hash = MD5.asHex(MD5.getHash(zipFile));
		// Map<String, String> compMap = new HashMap<String, String>();
		// compMap.put("MD5", zipFileMd5Hash);
		// compMap.put("FILESIZE", String.valueOf(zipFile.length()));
		// StrSubstitutor compSubst = new StrSubstitutor(compMap);
		// String componentFileName = substitutor.replace("voice-${VOICENAME}-${MARYVERSION}-component.xml");
		// File componentFile = new File(compileDir.getAbsolutePath()+"/target/"+componentFileName);
		// copyWithVarSubstitution("component.xml", componentFile, compSubst);
		// }

		public String getPackageName() {
			return toPackageName(voiceName);
		}
	}

	/**
	 * Convert an arbitrary string into a valid java package name, as follows: - any characters that are not alphanumeric or
	 * underscore are deleted; - if the first character after a deleted one is a letter, it is capitalised. - if the first
	 * character is not a letter, we prepend a "V" for "voice".
	 * 
	 * @param voiceName
	 *            voiceName
	 * @return result in string format
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
			String firstChar = part.substring(0, 1);
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

}
