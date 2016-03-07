package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;


import org.testng.Assert;
import org.testng.annotations.*;


public class DatabaseLayoutTest {
	File dummyConfigFile;
	DatabaseLayout db;

	private static File createDummyConfigFile(File dir)
        throws IOException
    {
		BufferedReader br = new BufferedReader(new InputStreamReader(
                                                   DatabaseLayoutTest.class.getResourceAsStream("database.config"), "UTF-8"));
		File conf = new File(dir, "dummy.config");
		PrintWriter out = new PrintWriter(conf, "UTF-8");
		String line;
		String defaultPath = Pattern.quote("/home/test/my_voice");
		String currentPath = dir.getAbsolutePath().replace("\\", "\\\\");
		while ((line = br.readLine()) != null) {
			line = line.replaceAll(defaultPath, currentPath);
			out.println(line);
		}
		br.close();
		out.close();
		return conf;
	}

	@BeforeMethod
	public void setUp() throws Exception {
        File temp = File.createTempFile("dummyVoiceDir","");
        temp.delete();
        temp.mkdir();

		dummyConfigFile = createDummyConfigFile(temp);
		Assert.assertTrue(dummyConfigFile.length() > 0);
		(new File(temp.getPath(), "wav")).mkdir();
		db = new DatabaseLayout(dummyConfigFile, new VoiceImportComponent[] { new AllophonesExtractor(),
                                                                              new BasenameTimelineMaker(), new EHMMLabeler() });
	}

	@Test
	public void isInitialized() {
		Assert.assertTrue(db.isInitialized());
		Assert.assertNotNull(db.getAllophoneSet());
		Assert.assertNotNull(db.getLocale());
	}

}
