/**
 * Copyright 2008 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.newlanguage;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import marytts.cart.CART;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.MaryCARTWriter;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.AlignerTrainer;
import marytts.fst.FSTLookup;
import marytts.fst.TransducerTrie;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.util.MaryUtils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * The LexiconCreator is the base class for creating the files needed to run the phonemiser component for a new language. From a
 * list of phonetically transcribed words, the class will create:
 * <ul>
 * <li>a lexicon file, efficiently stored as a Finite State Transducer;</li>
 * <li>a letter-to-sound prediction file, as a decision tree in MARY format.</li>
 * </ul>
 * 
 * The input file is expected to contain data in the following format:
 * <code>grapheme | ' a l - l o - p h o n e s | (optional-part-of-speech)</code> Hereby, the allophones must correspond to a
 * defined allophone set, given in the constructor. The file's encoding is expected to be UTF-8. Subclasses of LexiconCreator can
 * override prepareLexicon() to provide data in this format.
 * 
 * @see AllophoneSet
 * @author marc
 *
 */
public class LexiconCreator {
	protected Logger logger;
	protected AllophoneSet allophoneSet;
	protected String lexiconFilename;
	protected String fstFilename;
	protected String ltsFilename;
	protected boolean convertToLowercase;
	protected boolean predictStress;
	protected int context;

	/**
	 * Initialise a new lexicon creator. Letter to sound rules built with this lexicon creator will convert graphemes to lowercase
	 * before prediction, using the locale given in the allophone set; letter-to-sound rules will also predict stress; a context
	 * of 2 characters to the left and to the right of the current character will be used as predictive features.
	 * 
	 * @param allophoneSet
	 *            this specifies the set of phonetic symbols that can be used in the lexicon, and provides the locale of the
	 *            lexicon
	 * @param lexiconFilename
	 *            where to find the plain-text lexicon
	 * @param fstFilename
	 *            where to create the compressed lexicon FST file
	 * @param ltsFilename
	 *            where to create the letter-to-sound prediction tree.
	 */
	public LexiconCreator(AllophoneSet allophoneSet, String lexiconFilename, String fstFilename, String ltsFilename) {
		this(allophoneSet, lexiconFilename, fstFilename, ltsFilename, true, true, 2);
	}

	/**
	 * Initialize a new lexicon creator.
	 * 
	 * @param allophoneSet
	 *            this specifies the set of phonetic symbols that can be used in the lexicon, and provides the locale of the
	 *            lexicon
	 * @param lexiconFilename
	 *            where to find the plain-text lexicon
	 * @param fstFilename
	 *            where to create the compressed lexicon FST file
	 * @param ltsFilename
	 *            where to create the letter-to-sound prediction tree.
	 * @param convertToLowercase
	 *            if true, Letter to sound rules built with this lexicon creator will convert graphemes to lowercase before
	 *            prediction, using the locale given in the allophone set.
	 * @param predictStress
	 *            if true, letter-to-sound rules will predict stress.
	 * @param context
	 *            the number of characters to the left and to the right of the current character will be used as predictive
	 *            features.
	 */
	public LexiconCreator(AllophoneSet allophoneSet, String lexiconFilename, String fstFilename, String ltsFilename,
			boolean convertToLowercase, boolean predictStress, int context) {
		this.allophoneSet = allophoneSet;
		this.lexiconFilename = lexiconFilename;
		this.fstFilename = fstFilename;
		this.ltsFilename = ltsFilename;
		this.convertToLowercase = convertToLowercase;
		this.predictStress = predictStress;
		this.context = context;
		this.logger = MaryUtils.getLogger("LexiconCreator");
	}

	/**
	 * This base implementation does nothing. Subclasses can override this method to prepare a lexicon in the expected format,
	 * which should then be found at lexiconFilename.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	protected void prepareLexicon() throws IOException {
	}

	protected void compileFST() throws IOException {
		logger.info("Compressing into FST:");
		logger.info(" - aligning graphemes and allophones...");
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
		AlignerTrainer at = new AlignerTrainer(false, true);
		at.readLexicon(br, "\\s*\\|\\s*");
		br.close();

		// make some alignment iterations
		for (int i = 0; i < 4; i++) {
			logger.info("     iteration " + (i + 1));
			at.alignIteration();
		}
		logger.info(" - entering alignments in trie...");
		TransducerTrie t = new TransducerTrie();
		for (int i = 0, size = at.lexiconSize(); i < size; i++) {
			t.add(at.getAlignment(i));
			t.add(at.getInfoAlignment(i));
		}
		logger.info(" - minimizing trie...");
		t.computeMinimization();
		logger.info(" - writing transducer to disk...");
		File of = new File(fstFilename);
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
		t.writeFST(os, "UTF-8");
		os.flush();
		os.close();
	}

	protected void testFST() throws IOException {
		List<String> testGraphemes = new ArrayList<String>();
		List<String> testAllophones = new ArrayList<String>();
		List<String> testPos = new ArrayList<String>();
		int N = 100; // every N'th entry is put into tests...
		loadTestWords(testGraphemes, testAllophones, testPos, N);
		logger.info(" - looking up " + testGraphemes.size() + " test words...");
		FSTLookup fst = new FSTLookup(fstFilename);
		for (int i = 0, max = testGraphemes.size(); i < max; i++) {
			String key = testGraphemes.get(i);
			String expected = testAllophones.get(i);
			String[] result = fst.lookup(key);
			if (testPos.get(i) != null) {
				String key2 = key + testPos.get(i);
				String[] result2 = fst.lookup(key2);
				if (!expected.equals(result2[0]))
					logger.info("    " + key2 + " -> " + Arrays.toString(result2) + " (expected: " + expected + ")");
				// in addition, expected should be one of the results of a lookup without pos
				boolean found = false;
				for (String r : result) {
					if (expected.equals(r)) {
						found = true;
						break;
					}
				}
				if (!found)
					logger.info("    " + key + " -> " + Arrays.toString(result) + " (expected: " + expected + ")");
			} else {
				if (!expected.equals(result[0]))
					logger.info("    " + key + " -> " + Arrays.toString(result) + " (expected: " + expected + ")");
			}
		}
		logger.info("...done!\n");
	}

	private void loadTestWords(List<String> testGraphemes, List<String> testAllophones, List<String> testPos, int N)
			throws UnsupportedEncodingException, FileNotFoundException, IOException {
		int n = 0;
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
		String line;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("\\s*\\|\\s*");
			String graphemes = parts[0];
			String allophones = parts[1];
			String pos = (parts.length > 2 && parts[2].length() > 0) ? parts[2] : null;
			n++;
			if (n == N) {
				testGraphemes.add(graphemes);
				testAllophones.add(allophones);
				testPos.add(pos);
				n = 0;
			}
		}
	}

	protected void compileLTS() throws IOException {
		logger.info("Training letter-to-sound rules...");
		// initialize trainer
		LTSTrainer tp = new LTSTrainer(allophoneSet, convertToLowercase, predictStress, context);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));

		logger.info(" - reading lexicon...");
		// read lexicon for training
		tp.readLexicon(br, "\\s*\\|\\s*");

		logger.info(" - aligning...");
		// make some alignment iterations
		for (int i = 0; i < 5; i++) {
			logger.info("     iteration " + (i + 1));
			tp.alignIteration();

		}
		logger.info(" - training decision tree...");
		CART st = tp.trainTree(10);
		logger.info(" - saving...");
		// new MARY cart format:
		MaryCARTWriter mcw = new MaryCARTWriter();
		mcw.dumpMaryCART(st, ltsFilename);

		// Alternative ways of saving the CART would be:
		// MARY cart text format:
		// PrintWriter pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.tree.txt", "UTF-8");
		// mcw.toTextOut(st, pw);
		// pw.close();
		// old wagon cart, text and binary format:
		// WagonCARTWriter wcw = new WagonCARTWriter();
		// wcw.dumpWagonCART(st, "lib/modules/en/us/lexicon/cmudict.lts.wagontree.binary");
		// pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.wagontree.txt", "UTF-8");
		// wcw.toTextOut(st, pw);
		// pw.close();
		// For all of these, it would also be necessary to separately save the feature definition:
		// pw = new PrintWriter("lib/modules/en/us/lexicon/cmudict.lts.pfeats", "UTF-8");
		// st.getFeatureDefinition().writeTo(pw, false);
		// pw.close();

	}

	protected void testLTS() throws IOException, MaryConfigurationException {
		List<String> testGraphemes = new ArrayList<String>();
		List<String> testAllophones = new ArrayList<String>();
		List<String> testPos = new ArrayList<String>();
		int N = 100; // every N'th entry is put into tests...
		loadTestWords(testGraphemes, testAllophones, testPos, N);

		logger.info(" - loading LTS rules...");
		MaryCARTReader cartReader = new MaryCARTReader();
		CART st = cartReader.load(ltsFilename);
		TrainedLTS lts = new TrainedLTS(allophoneSet, st);

		logger.info(" - looking up " + testGraphemes.size() + " test words...");
		int max = testGraphemes.size();
		int correct = 0;
		for (int i = 0; i < max; i++) {
			String key = testGraphemes.get(i);
			String expected = testAllophones.get(i);
			String result = lts.syllabify(lts.predictPronunciation(key));
			if (!expected.equals(result))
				logger.info("    " + key + " -> " + result + " (expected: " + expected + ")");
			else
				correct++;
		}
		logger.info("   for " + correct + " out of " + max + " prediction is identical to lexicon entry.");
		logger.info("...done!\n");
	}

	public void createLexicon() throws Exception {
		prepareLexicon();
		compileFST();
		testFST();
		System.gc();
		compileLTS();
		testLTS();
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		PatternLayout layout = new PatternLayout("%d %m\n");
		BasicConfigurator.configure(new ConsoleAppender(layout));
		AllophoneSet allophoneSet = AllophoneSet.getAllophoneSet(args[0]);
		String lexiconFilename = args[1];
		String fstFilename = args[2];
		String ltsFilename = args[3];
		LexiconCreator lc = new LexiconCreator(allophoneSet, lexiconFilename, fstFilename, ltsFilename);
		lc.createLexicon();
	}

}
