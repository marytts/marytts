/**
 * 
 */
package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;


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

	public static  final String CARTFILE = "CARTBuilder.cartFile";

	public static  final String DURTREE = "DurationCARTTrainer.durTree";

	public static  final String F0LEFTTREE = "F0CARTTrainer.f0LeftTreeFile";

	public static  final String F0MIDTREE = "F0CARTTrainer.f0MidTreeFile";

	public static  final String F0RIGHTTREE = "F0CARTTrainer.f0RightTreeFile";

	public static  final String HALFPHONEFEATSAC = "AcousticFeatureFileWriter.acFeatureFile";

	public static  final String HALFPHONEFEATDEFAC = "AcousticFeatureFileWriter.acFeatDef";

	public static  final String HALFPHONEUNITS = "HalfPhoneUnitfileWriter.unitFile";

	public static  final String JOINCOSTFEATS = "JoinCostFileMaker.joinCostFile";

	public static  final String JOINCOSTWEIGHTS = "JoinCostFileMaker.weightsFile";

	public static  final String PHONEFEATDEF = "PhoneFeatureFileWriter.weightsFile";

    public static  final String WAVETIMELINE = "WaveTimelineMaker.waveTimeline";

    public static  final String BASETIMELINE = "BasenameTimelineMaker.timelineFile";

	
	
	
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
	public VoiceCompiler() {
	}

	/* (non-Javadoc)
	 * @see marytts.tools.voiceimport.VoiceImportComponent#compute()
	 */
	@Override
	public boolean compute() throws Exception {

		if (!isUnitSelectionVoice()) {
			mapFeatures();
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

	protected void mapFeatures() throws Exception {
		throw new IllegalStateException("This method should not be called for unit selection voices, "+
				" and hmm-based voices should extend it.");
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
		 String packageName = toPackageName(getVoiceName(db));
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
		 if (isUnitSelectionVoice()) {
			 libVoiceDir = new File(compileDir.getAbsolutePath()+"/lib/voices/"+getVoiceName(db));
			 libVoiceDir.mkdir();
		 }
	}
	
	protected boolean isUnitSelectionVoice() {
		return true;
	}

	protected void copyVoiceFiles() throws IOException {
		if (!isUnitSelectionVoice()) {
			throw new IllegalStateException("This method should only be called for unit selection voices");
		}
		
		String[] filesForResourceDir = new String[] {
			CARTFILE, DURTREE, F0LEFTTREE, F0MIDTREE, F0RIGHTTREE, HALFPHONEFEATDEFAC, JOINCOSTWEIGHTS
		};
		for (String prop : filesForResourceDir) {
			FileUtils.copyFileToDirectory(new File(db.getProperty(prop)), mainResourcesDir);
		}

		String[] filesForLibVoiceDir = new String[] {
			HALFPHONEFEATSAC, HALFPHONEUNITS, JOINCOSTFEATS, BASETIMELINE, WAVETIMELINE	
		};
		for (String prop : filesForLibVoiceDir) {
			FileUtils.copyFileToDirectory(new File(db.getProperty(prop)), libVoiceDir);
		}
	}

	protected void copyTemplateFiles() throws IOException {
		copyWithVarSubstitution("pom.xml", new File(compileDir, "pom.xml"));
		copyWithVarSubstitution("installable.xml", new File(mainDescriptionsDir, "installable.xml"));
		copyWithVarSubstitution("Config.java", new File(mainJavaDir, "Config.java"));
		copyWithVarSubstitution("LoadVoiceIT.java", new File(testJavaDir, "LoadVoiceIT.java"));
		copyWithVarSubstitution("marytts.config.MaryConfig", new File(metaInfDir, "marytts.config.MaryConfig"));
		if (isUnitSelectionVoice()) {
			copyWithVarSubstitution("unitselection-voice.config", new File(mainResourcesDir, "voice.config"));
		} else {
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
            props.put(getCompileDirProp(), new File(db.getVoiceFileDir(), "voice-"+getVoiceName(db)).getAbsolutePath());
        }
        return props;
	}

	protected String getVoiceName(DatabaseLayout db) {
		return db.getVoiceName();
	}

	protected String getCompileDirProp() {
		return "VoiceCompiler.compileDir";
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
        props2Help.put(getCompileDirProp(), "The directory in which the files for compiling the voice will be copied.");
    }

	@Override
	protected void initialiseComp() throws Exception {
		substitutor = new StrSubstitutor(getVariableSubstitutionMap());
		compileDir = new File(getProp(getCompileDirProp()));
	}

	protected Map<String, String> getVariableSubstitutionMap() {
		Map<String, String> m = new HashMap<String, String>();
		m.put("MARYVERSION", db.getMaryVersion());
		m.put("VOICENAME", getVoiceName(db));
		m.put("LOCALE", db.getLocale().toString());
		m.put("LANG", db.getLocale().getLanguage());
		m.put("GENDER", db.getGender());
		m.put("DOMAIN", db.getDomain());
		m.put("SAMPLINGRATE", String.valueOf(db.getSamplingRate()));
		m.put("PACKAGE", toPackageName(getVoiceName(db)));
		m.put("VOICECLASS", isUnitSelectionVoice() ? "marytts.unitselection.UnitSelectionVoice" : "marytts.htsengine.HMMVoice");
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
	
	

}
