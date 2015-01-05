package marytts.tools.voiceimport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DatabaseLayoutTest {
	File dummyConfigFile;
	DatabaseLayout db;

	private static File createDummyConfigFile(TemporaryFolder parent) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				DatabaseLayoutTest.class.getResourceAsStream("database.config"), "UTF-8"));
		File conf = parent.newFile("dummy.config");
		PrintWriter out = new PrintWriter(conf, "UTF-8");
		String line;
		String defaultPath = Pattern.quote("/home/test/my_voice");
		File root = parent.getRoot();
		String currentPath = root.getAbsolutePath().replace("\\", "\\\\");
		while ((line = br.readLine()) != null) {
			line = line.replaceAll(defaultPath, currentPath);
			out.println(line);
		}
		br.close();
		out.close();
		return conf;
	}

	@Rule
	public TemporaryFolder dummyVoiceDir = new TemporaryFolder();

	@Before
	public void setUp() throws Exception {
		dummyConfigFile = createDummyConfigFile(dummyVoiceDir);
		assertTrue(dummyConfigFile.length() > 0);
		dummyVoiceDir.newFolder("wav");
		db = new DatabaseLayout(dummyConfigFile, new VoiceImportComponent[] { new AllophonesExtractor(),
				new BasenameTimelineMaker(), new EHMMLabeler() });
	}

	@Test
	public void isInitialized() {
		assertTrue(db.isInitialized());
		assertNotNull(db.getAllophoneSet());
		assertNotNull(db.getLocale());
	}

}
