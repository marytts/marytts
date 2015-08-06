package marytts.tools.upgrade;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.cloudbees.diff.Diff;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import marytts.tools.install.InstallFileParser;
import marytts.tools.install.VoiceComponentDescription;

public class UpgradeTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Test
	public void convertHmmConfig() throws Exception {
		// setup SUT
		URL componentUrl = UpgradeTest.class.getResource("/marytts/tools/upgrade/cmu-slt-4-component.xml");
		InstallFileParser parser = new InstallFileParser(componentUrl);
		List<VoiceComponentDescription> voiceDescriptions = parser.getVoiceDescriptions();
		String packageFilename = voiceDescriptions.get(0).getPackageFilename();
		Mary4To5VoiceConverter converter = new Mary4To5VoiceConverter(voiceDescriptions, new File(packageFilename));

		// exercise
		converter.loadConfigFromStream(UpgradeTest.class.getResourceAsStream("en_US-cmu-slt-hsmm-4.x.config"));
		converter.updateConfig("CmuSltHsmm");
		File convertedConfigFile = tempDir.newFile("converted.config");
		converter.saveConfig(convertedConfigFile);

		// verify
		InputStream expectedConfigStream = UpgradeTest.class.getResourceAsStream("en_US-cmu-slt-hsmm-5.config");
		File expectedConfigFile = tempDir.newFile("expected.config");
		FileUtils.copyInputStreamToFile(expectedConfigStream, expectedConfigFile);
		boolean ignoreWhiteSpace = false;
		Diff diff = Diff.diff(convertedConfigFile, expectedConfigFile, ignoreWhiteSpace);
		String uniDiff = diff.toUnifiedDiff(expectedConfigFile.getPath(), convertedConfigFile.getPath(), new FileReader(
				expectedConfigFile), new FileReader(convertedConfigFile), 2);

		assertThat("Config differs from expectation as follows:" + IOUtils.LINE_SEPARATOR + uniDiff, diff, is(empty()));
	}

}
