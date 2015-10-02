/**
 * Copyright 2000-2009 DFKI GmbH.
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.io.MaryCARTWriter;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.fst.AlignerTrainer;
import marytts.fst.StringPair;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;

import org.apache.log4j.BasicConfigurator;

import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.C45PruneableClassifierTreeWithUnary;
import weka.classifiers.trees.j48.TreeConverter;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 
 * This class is a generic approach to predict a phone sequence from a grapheme sequence.
 * 
 * the normal sequence of steps is: 1) initialize the trainer with a phone set and a locale
 * 
 * 2) read in the lexicon, preserve stress if you like
 * 
 * 3) make some alignment iterations (usually 5-10)
 * 
 * 4) train the trees and save them in wagon format in a specified directory
 * 
 * see main method for an example.
 * 
 * Apply the model using TrainedLTS
 * 
 * @author benjaminroth
 *
 */

public class LTSTrainer extends AlignerTrainer {

	protected AllophoneSet phSet;

	protected int context;
	protected boolean convertToLowercase;
	protected boolean considerStress;

	/**
	 * Create a new LTSTrainer.
	 * 
	 * @param aPhSet
	 *            the allophone set to use.
	 * @param convertToLowercase
	 *            whether to convert all graphemes to lowercase, using the locale of the allophone set.
	 * @param considerStress
	 *            indicator if stress is preserved
	 * @param context
	 *            context
	 */
	public LTSTrainer(AllophoneSet aPhSet, boolean convertToLowercase, boolean considerStress, int context) {
		super();
		this.phSet = aPhSet;
		this.convertToLowercase = convertToLowercase;
		this.considerStress = considerStress;
		this.context = context;
		BasicConfigurator.configure();
	}

	/**
	 * Train the tree, using binary decision nodes.
	 * 
	 * @param minLeafData
	 *            the minimum number of instances that have to occur in at least two subsets induced by split
	 * @return bigTree
	 * @throws IOException
	 *             IOException
	 */
	public CART trainTree(int minLeafData) throws IOException {

		Map<String, List<String[]>> grapheme2align = new HashMap<String, List<String[]>>();
		for (String gr : this.graphemeSet) {
			grapheme2align.put(gr, new ArrayList<String[]>());
		}

		Set<String> phChains = new HashSet<String>();

		// for every alignment pair collect counts
		for (int i = 0; i < this.inSplit.size(); i++) {

			StringPair[] alignment = this.getAlignment(i);

			for (int inNr = 0; inNr < alignment.length; inNr++) {

				// System.err.println(alignment[inNr]);

				// quotation signs needed to represent empty string
				String outAlNr = "'" + alignment[inNr].getString2() + "'";

				// TODO: don't consider alignments to more than three characters
				if (outAlNr.length() > 5)
					continue;

				phChains.add(outAlNr);

				// storing context and target
				String[] datapoint = new String[2 * context + 2];

				for (int ct = 0; ct < 2 * context + 1; ct++) {
					int pos = inNr - context + ct;

					if (pos >= 0 && pos < alignment.length) {
						datapoint[ct] = alignment[pos].getString1();
					} else {
						datapoint[ct] = "null";
					}

				}

				// set target
				datapoint[2 * context + 1] = outAlNr;

				// add datapoint
				grapheme2align.get(alignment[inNr].getString1()).add(datapoint);
			}
		}

		// for conversion need feature definition file
		FeatureDefinition fd = this.graphemeFeatureDef(phChains);

		int centerGrapheme = fd.getFeatureIndex("att" + (context + 1));

		List<CART> stl = new ArrayList<CART>(fd.getNumberOfValues(centerGrapheme));

		for (String gr : fd.getPossibleValues(centerGrapheme)) {
			System.out.println("      Training decision tree for: " + gr);
			logger.debug("      Training decision tree for: " + gr);

			ArrayList<Attribute> attributeDeclarations = new ArrayList<Attribute>();

			// attributes with values
			for (int att = 1; att <= context * 2 + 1; att++) {

				// ...collect possible values
				ArrayList<String> attVals = new ArrayList<String>();

				String featureName = "att" + att;

				for (String usableGrapheme : fd.getPossibleValues(fd.getFeatureIndex(featureName))) {
					attVals.add(usableGrapheme);
				}

				attributeDeclarations.add(new Attribute(featureName, attVals));
			}

			List<String[]> datapoints = grapheme2align.get(gr);

			// maybe training is faster with targets limited to grapheme
			Set<String> graphSpecPh = new HashSet<String>();
			for (String[] dp : datapoints) {
				graphSpecPh.add(dp[dp.length - 1]);
			}

			// targetattribute
			// ...collect possible values
			ArrayList<String> targetVals = new ArrayList<String>();
			for (String phc : graphSpecPh) {// todo: use either fd of phChains
				targetVals.add(phc);
			}
			attributeDeclarations.add(new Attribute(TrainedLTS.PREDICTED_STRING_FEATURENAME, targetVals));

			// now, create the dataset adding the datapoints
			Instances data = new Instances(gr, attributeDeclarations, 0);

			// datapoints
			for (String[] point : datapoints) {

				Instance currInst = new DenseInstance(data.numAttributes());
				currInst.setDataset(data);

				for (int i = 0; i < point.length; i++) {

					currInst.setValue(i, point[i]);
				}

				data.add(currInst);
			}

			// Make the last attribute be the class
			data.setClassIndex(data.numAttributes() - 1);

			// build the tree without using the J48 wrapper class
			// standard parameters are:
			// binary split selection with minimum x instances at the leaves, tree is pruned, confidenced value, subtree raising,
			// cleanup, don't collapse
			// Here is used a modifed version of C45PruneableClassifierTree that allow using Unary Classes (see Issue #51)
			C45PruneableClassifierTree decisionTree;
			try {
				decisionTree = new C45PruneableClassifierTreeWithUnary(new BinC45ModelSelection(minLeafData, data, true), true,
						0.25f, true, true, false);
				decisionTree.buildClassifier(data);
			} catch (Exception e) {
				throw new RuntimeException("couldn't train decisiontree using weka: ", e);
			}

			CART maryTree = TreeConverter.c45toStringCART(decisionTree, fd, data);

			stl.add(maryTree);
		}

		DecisionNode.ByteDecisionNode rootNode = new DecisionNode.ByteDecisionNode(centerGrapheme, stl.size(), fd);
		for (CART st : stl) {
			rootNode.addDaughter(st.getRootNode());
		}

		Properties props = new Properties();
		props.setProperty("lowercase", String.valueOf(convertToLowercase));
		props.setProperty("stress", String.valueOf(considerStress));
		props.setProperty("context", String.valueOf(context));

		CART bigTree = new CART(rootNode, fd, props);

		return bigTree;
	}

	/**
	 * 
	 * Convenience method to save files to graph2phon.wagon and graph2phon.pfeats in a specified directory with UTF-8 encoding.
	 * 
	 * @param tree
	 *            tree
	 * @param saveTreefile
	 *            saveTreefile
	 * @throws IOException
	 *             IOException
	 */
	public void save(CART tree, String saveTreefile) throws IOException {
		MaryCARTWriter mcw = new MaryCARTWriter();
		mcw.dumpMaryCART(tree, saveTreefile);
	}

	private FeatureDefinition graphemeFeatureDef(Set<String> phChains) throws IOException {

		String lineBreak = System.getProperty("line.separator");

		StringBuilder fdString = new StringBuilder("ByteValuedFeatureProcessors");
		fdString.append(lineBreak);

		// add attribute features
		for (int att = 1; att <= context * 2 + 1; att++) {
			fdString.append("att").append(att);

			for (String gr : this.graphemeSet) {
				fdString.append(" ").append(gr);
			}
			fdString.append(lineBreak);
		}
		fdString.append("ShortValuedFeatureProcessors").append(lineBreak);

		// add class features
		fdString.append(TrainedLTS.PREDICTED_STRING_FEATURENAME);

		for (String ph : phChains) {
			fdString.append(" ").append(ph);
		}

		fdString.append(lineBreak);

		fdString.append("ContinuousFeatureProcessors").append(lineBreak);

		BufferedReader featureReader = new BufferedReader(new StringReader(fdString.toString()));

		return new FeatureDefinition(featureReader, false);
	}

	/**
	 * 
	 * reads in a lexicon in text format, lines are of the kind:
	 * 
	 * graphemechain | phonechain | otherinformation
	 * 
	 * Stress is optionally preserved, marking the first vowel of a stressed syllable with "1".
	 * 
	 * @param lexicon
	 *            reader with lines of lexicon
	 * @param splitPattern
	 *            a regular expression used for identifying the field separator in each line.
	 * @throws IOException
	 *             IOException
	 */
	public void readLexicon(BufferedReader lexicon, String splitPattern) throws IOException {

		String line;

		while ((line = lexicon.readLine()) != null) {
			String[] lineParts = line.trim().split(splitPattern);
			String graphStr = lineParts[0];
			if (convertToLowercase)
				graphStr = graphStr.toLowerCase(phSet.getLocale());
			graphStr = graphStr.replaceAll("['-.]", "");

			// remove all secondary stress markers
			String phonStr = lineParts[1].replaceAll(",", "");
			String[] syllables = phonStr.split("-");
			List<String> separatedPhones = new ArrayList<String>();
			List<String> separatedGraphemes = new ArrayList<String>();
			String currPh;
			for (String syl : syllables) {
				boolean stress = false;
				if (syl.startsWith("'")) {
					syl = syl.substring(1);
					stress = true;
				}
				for (Allophone ph : phSet.splitIntoAllophones(syl)) {
					currPh = ph.name();
					if (stress && considerStress && ph.isVowel()) {
						currPh += "1";
						stress = false;
					}
					separatedPhones.add(currPh);
				}// ... for each allophone
			}

			for (int i = 0; i < graphStr.length(); i++) {
				this.graphemeSet.add(graphStr.substring(i, i + 1));
				separatedGraphemes.add(graphStr.substring(i, i + 1));
			}
			this.addAlreadySplit(separatedGraphemes, separatedPhones);
		}
		// Need one entry for the "null" grapheme, which maps to the empty string:
		this.addAlreadySplit(new String[] { "null" }, new String[] { "" });
	}

	/**
	 * reads in a lexicon in text format, lines are of the kind:
	 * 
	 * graphemechain | phonechain | otherinformation
	 * 
	 * Stress is optionally preserved, marking the first vowel of a stressed syllable with "1".
	 * 
	 * @param lexicon
	 *            lexicon
	 */
	public void readLexicon(HashMap<String, String> lexicon) {

		Iterator<String> it = lexicon.keySet().iterator();
		while (it.hasNext()) {
			String graphStr = it.next();

			// remove all secondary stress markers
			String phonStr = lexicon.get(graphStr).replaceAll(",", "");
			if (convertToLowercase)
				graphStr = graphStr.toLowerCase(phSet.getLocale());
			graphStr = graphStr.replaceAll("['-.]", "");

			String[] syllables = phonStr.split("-");
			List<String> separatedPhones = new ArrayList<String>();
			List<String> separatedGraphemes = new ArrayList<String>();
			String currPh;
			for (String syl : syllables) {
				boolean stress = false;
				if (syl.startsWith("'")) {
					syl = syl.substring(1);
					stress = true;
				}
				for (Allophone ph : phSet.splitIntoAllophones(syl)) {
					currPh = ph.name();
					if (stress && considerStress && ph.isVowel()) {
						currPh += "1";
						stress = false;
					}
					separatedPhones.add(currPh);
				}// ... for each allophone
			}

			for (int i = 0; i < graphStr.length(); i++) {
				this.graphemeSet.add(graphStr.substring(i, i + 1));
				separatedGraphemes.add(graphStr.substring(i, i + 1));
			}
			this.addAlreadySplit(separatedGraphemes, separatedPhones);
		}
		// Need one entry for the "null" grapheme, which maps to the empty string:
		this.addAlreadySplit(new String[] { "null" }, new String[] { "" });
	}

	public static void main(String[] args) throws IOException, MaryConfigurationException {

		String phFileLoc = "/Users/benjaminroth/Desktop/mary/english/phone-list-engba.xml";

		// initialize trainer
		LTSTrainer tp = new LTSTrainer(AllophoneSet.getAllophoneSet(phFileLoc), true, true, 2);

		BufferedReader lexReader = new BufferedReader(new InputStreamReader(new FileInputStream(
				"/Users/benjaminroth/Desktop/mary/english/sampa-lexicon.txt"), "ISO-8859-1"));

		// read lexicon for training
		tp.readLexicon(lexReader, "\\\\");

		// make some alignment iterations
		for (int i = 0; i < 5; i++) {
			System.out.println("iteration " + i);
			tp.alignIteration();

		}

		CART st = tp.trainTree(100);

		tp.save(st, "/Users/benjaminroth/Desktop/mary/english/trees/");

	}

}
