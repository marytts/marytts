/**
 *
 */
package marytts.tools.newlanguage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.io.InputStream;

import marytts.modeling.cart.CART;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.nlp.phonemiser.AllophoneSet;


import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Test case for LTSTrainer
 *
 * @author fabio
 *
 */
public class LTSTrainerTest {

	@Test
	public void test() {
		InputStream alloStream = LTSTrainerTest.class.getResourceAsStream("allophones.it.xml");
		AllophoneSet allophoneset;
		try {

			allophoneset = AllophoneSet.getAllophoneSet(alloStream, "test");

			// Initialize trainer
			LTSTrainer tp = new LTSTrainer(allophoneset, true, true, 2);
			InputStream lexStream = LTSTrainerTest.class.getResourceAsStream("LTS_test.it.txt");

			BufferedReader lexReader = new BufferedReader(new InputStreamReader(lexStream, "UTF-8"));

			// read lexicon for training
			tp.readLexicon(lexReader, "\\s");

			// make some alignment iterations
			for (int i = 0; i < 5; i++) {
				System.out.println("iteration " + i);
				tp.alignIteration();
			}
			CART st = tp.trainTree(10);

			// Temp file for test
			String OutputFilename = "LTS_test.tree";
			File tempFile = File.createTempFile(OutputFilename, ".tmp");
			System.out.println("Writing tree in tempFile: " + tempFile.getAbsolutePath() + tempFile.getName());

			tp.save(st, tempFile.getAbsolutePath() + tempFile.getName());

			Assert.assertTrue(tempFile.exists());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail(e.toString());
		} catch (MaryConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
}
