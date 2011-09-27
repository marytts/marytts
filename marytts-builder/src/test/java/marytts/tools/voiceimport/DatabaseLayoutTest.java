package marytts.tools.voiceimport;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DatabaseLayoutTest {
	File dummyVoiceDir;
	File dummyConfigFile;
	DatabaseLayout db;
	
	private static File createDummyConfigFile(File parent) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(DatabaseLayoutTest.class.getResourceAsStream("database.config"), "UTF-8"));
		File conf = new File(parent, "dummy.config");
		PrintWriter out = new PrintWriter(conf, "UTF-8");
		String line;
		String defaultPath = Pattern.quote("/home/test/my_voice");
		String currentPath = parent.getAbsolutePath();
		while ((line = br.readLine()) != null) {
			line = line.replaceAll(defaultPath, currentPath);
			out.println(line);
		}
		br.close();
		out.close();
		return conf;
	}
	
	
	@Before
	public void setUp() throws Exception {
		dummyVoiceDir = new File(FileUtils.getTempDirectory(), "dummy_voice");
		if (dummyVoiceDir.exists()) {
			FileUtils.deleteDirectory(dummyVoiceDir);
		}
		dummyVoiceDir.mkdir();
		dummyConfigFile = createDummyConfigFile(dummyVoiceDir);
		assertTrue(dummyConfigFile.length() > 0);
		File wavDir = new File(dummyVoiceDir, "wav");
		wavDir.mkdir();
		db = new DatabaseLayout(dummyConfigFile, new VoiceImportComponent[] {
			new AllophonesExtractor(),
			new BasenameTimelineMaker(),
			new EHMMLabeler()
		});
	}
	
	@Test
	public void isInitialized() {
		assertTrue(db.isInitialized());
		assertNotNull(db.getAllophoneSet());
		assertNotNull(db.getLocale());
	}
	
	
	@After
	public void tearDown() throws Exception {
		dummyConfigFile.delete();
		FileUtils.deleteDirectory(dummyVoiceDir);
	}

}
