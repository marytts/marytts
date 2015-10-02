/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import marytts.features.FeatureDefinition;

/**
 * Builds and manages the cover sets
 * 
 * @author Anna Hunecke
 *
 */
public class CoverageDefinition {

	/* cover sets for simple diphones */
	private CoverNode simpleCover;
	/* weights of the different levels of the cover set */
	private double phoneLevelWeight;
	private double diphoneLevelWeight;
	private double prosodyLevelWeight;
	/* use simple or clustered cover set for usefulness computation */
	private boolean simpleDiphones;
	/* consider frequency when computing usefulness */
	private boolean considerFrequency;
	/*
	 * the actual setting of the frequency (setting only considered when considerFrequency is true)
	 */
	private String frequencySetting;
	/* consider the length of a sentence when computing usefulness */
	private boolean considerSentenceLength;
	/*
	 * max/min sentence length a selected sentence is allowed to have (settings only considered when considerSentenceLength is
	 * true)
	 */
	private int maxSentLengthAllowed;
	private int minSentLengthAllowed;
	/*
	 * number by which the wanted weight of a node is divided each time a new feature vector is added to the node
	 */
	private double wantedWeightDecrease;
	/* the index of the four features in the feature vector */
	private int phoneFeatIndex;
	private int diphoneFeatIndex;
	// private int phoneClassesIndex; // CHECK IF THIS FEATURE WILL NOT BE USED ANY MORE???
	private int prosodyIndex;
	// number of target features used, (phone, next_phone, selection_prosody = 3 )
	private int numTargetFeaturesUsed;

	/* the number of possible prosody feature values */
	private int numProsodyValues;

	/* the number of possible phones */
	private int numPhoneValues;
	/* the number of possible phones minus the phones to ignore */
	private int numPhoneValuesMinusIgnored;
	/* the number of possible simple diphones */
	private int numPossibleSimpleDiphones;
	/* the number of feature vectors in the cover set */
	private int numSelectedFeatVects;
	/* the number of tokens in the corpus */
	private int numTokens;
	/* the number of simple diphones types in the corpus */
	private int numSimpleDiphoneTypes;
	/* the number of simple feature vector types in the corpus */
	private int numSimpleFeatVectTypes;
	/* average/max/min sentence length in the corpus */
	private double averageSentLength;
	private int maxSentLength;
	private int minSentLength;
	/* the number of sentences in the cover set */
	private int numSentencesInCover;
	/* max/min sentence length in the cover set */
	private int maxSentLengthInCover;
	private int minSentLengthInCover;
	/*
	 * maximum sizes of simple/clustered cover (=number of Leaves)
	 */
	private int numSimpleLeaves;
	/* the phone coverage of the corpus */
	private double possiblePhoneCoverage;
	/* the simple diphone coverage of the corpus */
	private double possibleSimpleDiphoneCoverage;
	/* the overall (=phone+simpleDiphone+prosody) coverage of the corpus */
	private double possibleOverallSimpleCoverage;
	/* the phone types in the corpus */
	private Set<String> possiblePhoneTypes;
	/*
	 * keep track of the coverage development over time by adding a the current coverage value each time the cover is updated
	 */
	private List phoneCoverageInTime;
	private List diphoneCoverageInTime;
	private List overallCoverageInTime;
	/* set of covered phones/simple diphones/clustered diphones */
	private Set<String> phonesInCover;
	private Set<String> simpleDiphonesInCover;
	/* number of simple prosodic variations in cover */
	private int numSimpleFeatVectsInCover;
	/* the featureDefinition for the feature vectors */
	private final FeatureDefinition featDef;
	/* the number of sentences in the corpus */
	private int numSentences;
	/* the phones that are not in the corpus and have to be ignored */
	private Set<Integer> phonesToIgnore;
	/* the possible phone values */
	private String[] possiblePhoneArray;
	/* the possible next phone values */
	private String[] possibleNextPhoneArray;
	/* the possible next phone values */
	private String[] possibleProsodyArray;
	private String[][] possibleDiphones;
	private String[][][] possibleDiphonesProsody;
	/* For printing statistics, count the number of occurrences of each diphone in the text corpus */
	private int[][] diphoneFrequencies;
	private int[] phoneFrequencies;

	private CoverageFeatureProvider cfProvider;

	/**
	 * Build a new coverage definition and read in the config file
	 * 
	 * 
	 * 
	 * @param featDef
	 *            the feature definition for the vectors
	 * @param cfProvider
	 *            coverage feature provider
	 * @param configFile
	 *            optionally, the coverage config file name. if this is null, default settings will be used.
	 * @throws Exception
	 *             Exception
	 */
	public CoverageDefinition(FeatureDefinition featDef, CoverageFeatureProvider cfProvider, String configFile) throws Exception {
		this.featDef = featDef;
		this.cfProvider = cfProvider;

		readConfigFile(featDef, configFile);
		setupFeatureIndexes();
		initializeVariables();
	}

	/**
	 * @param featDef
	 *            featDef
	 * @param configFile
	 *            configFile
	 * @throws Exception
	 *             Exception
	 */
	private void readConfigFile(FeatureDefinition featDef, String configFile) throws Exception {
		try {
			InputStream inStream;
			if (configFile != null) {
				inStream = new FileInputStream(new File(configFile));
			} else {
				inStream = getClass().getResourceAsStream("covDef.config");
			}
			BufferedReader configIn = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
			String line;
			int numparams = 0;
			// loop over the lines of the config file
			while ((line = configIn.readLine()) != null) {
				if (!line.startsWith("#") && !line.equals("")) {
					StringTokenizer tok = new StringTokenizer(line);
					String key = tok.nextToken();
					String value = tok.nextToken();
					if (key.equals("simpleDiphones")) {
						if (value.equals("true")) {
							simpleDiphones = true;
						} else {
							simpleDiphones = false;
						}
						numparams++;
						continue;
					}
					if (key.equals("frequency")) {
						if (value.equals("none")) {
							considerFrequency = false;
						} else {
							considerFrequency = true;
							frequencySetting = value;
						}
						numparams++;
						continue;
					}
					if (key.equals("sentenceLength")) {
						if (value.equals("none")) {
							considerSentenceLength = false;
						} else {
							considerSentenceLength = true;
							maxSentLengthAllowed = Integer.parseInt(value);
							minSentLengthAllowed = Integer.parseInt(tok.nextToken());
						}
						numparams++;
						continue;
					}
					if (key.equals("wantedWeight")) {
						phoneLevelWeight = Double.parseDouble(value);
						diphoneLevelWeight = Double.parseDouble(tok.nextToken());
						prosodyLevelWeight = Double.parseDouble(tok.nextToken());
						numparams++;
						continue;
					}
					if (key.equals("wantedWeightDecrease")) {
						wantedWeightDecrease = Double.parseDouble(value);
						numparams++;
						continue;
					}
					if (key.equals("missingPhones")) {

						phonesToIgnore = new HashSet<Integer>();
						phonesToIgnore.add(0); // The non-existing phone "0"
						// phoneFeatIndex = featDef.getFeatureIndex("phone");
						// phonesToIgnore.add(
						// new Integer(featDef.getFeatureValueAsByte(phoneFeatIndex,value)));
						while (tok.hasMoreTokens()) {
							phonesToIgnore.add(new Integer(featDef.getFeatureValueAsByte(phoneFeatIndex, tok.nextToken())));

						}

						numparams++;
					}

				}
			}
			if (numparams < 6) {
				throw new Exception("Error reading coverage Definition Config File: " + configFile + " there are only "
						+ numparams + " instead of 6 settings");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Could not read coverage Definition Config File: " + configFile);
		}
	}

	/**
	 * 
	 */
	private void setupFeatureIndexes() {
		System.out.println("TARGETFEATURES used:");
		numTargetFeaturesUsed = 0;
		for (int i = 0; i < featDef.getNumberOfFeatures(); i++) {
			if (featDef.getFeatureName(i).contentEquals("phone")) {
				phoneFeatIndex = featDef.getFeatureIndex("phone");
				numTargetFeaturesUsed++;
				System.out.println("  feature(" + i + ")=" + featDef.getFeatureName(i));
			} else if (featDef.getFeatureName(i).contentEquals("next_phone")) {
				diphoneFeatIndex = featDef.getFeatureIndex("next_phone");
				numTargetFeaturesUsed++;
				System.out.println("  feature(" + i + ")=" + featDef.getFeatureName(i));
			} else if (featDef.getFeatureName(i).contentEquals("selection_prosody")) {
				prosodyIndex = featDef.getFeatureIndex("selection_prosody");
				numTargetFeaturesUsed++;
				System.out.println("  feature(" + i + ")=" + featDef.getFeatureName(i));
			} else
				System.out.println("  NO implementation in CoverageDefinition for the feature =" + featDef.getFeatureName(i));
		}

		numPhoneValues = featDef.getNumberOfValues(phoneFeatIndex);
		numPhoneValuesMinusIgnored = numPhoneValues - phonesToIgnore.size() - 1;
		numPossibleSimpleDiphones = numPhoneValuesMinusIgnored * (numPhoneValuesMinusIgnored + 1);
		numProsodyValues = featDef.getNumberOfValues(prosodyIndex);

		possiblePhoneArray = featDef.getPossibleValues(phoneFeatIndex);
		possibleNextPhoneArray = featDef.getPossibleValues(diphoneFeatIndex);
		possibleProsodyArray = featDef.getPossibleValues(prosodyIndex);

		// For efficiency, we build all strings once -- much better than doing it all the time:
		possibleDiphones = new String[possiblePhoneArray.length][possibleNextPhoneArray.length];
		possibleDiphonesProsody = new String[possiblePhoneArray.length][possibleNextPhoneArray.length][possibleProsodyArray.length];
		for (int i = 0; i < possiblePhoneArray.length; i++) {
			for (int j = 0; j < possibleNextPhoneArray.length; j++) {
				String diphone = possiblePhoneArray[i] + "_" + possibleNextPhoneArray[j];
				possibleDiphones[i][j] = diphone;
				for (int k = 0; k < possibleProsodyArray.length; k++) {
					possibleDiphonesProsody[i][j][k] = diphone + "_" + possibleProsodyArray[k];
				}
			}
		}
	}

	/**
	 * 
	 */
	private void initializeVariables() {
		// initialise several variables
		numSelectedFeatVects = 0;
		numSentencesInCover = 0;
		maxSentLengthInCover = 0;
		minSentLengthInCover = 20;
		phoneCoverageInTime = new ArrayList();
		diphoneCoverageInTime = new ArrayList();
		overallCoverageInTime = new ArrayList();
		phonesInCover = new HashSet<String>();
		simpleDiphonesInCover = new HashSet<String>();
		numSimpleFeatVectsInCover = 0;
	}

	/**
	 * Compute the coverage of the corpus, build and fill the cover sets. This will iterate once through the entire corpus, to
	 * compute the maximally achievable coverage with this corpus.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public void initialiseCoverage() throws IOException {
		// stuff used for counting the phones and diphones
		possiblePhoneTypes = new HashSet<String>();
		Set<String> simpleFeatVectTypes = new HashSet<String>();

		int numPhoneTypes = 0;
		numSimpleDiphoneTypes = 0;
		numTokens = 0;
		averageSentLength = 0.0;
		maxSentLength = 0;
		minSentLength = 20;

		phoneFrequencies = new int[possiblePhoneArray.length];
		diphoneFrequencies = new int[possiblePhoneArray.length][possibleNextPhoneArray.length];

		// build Cover
		buildCover();

		numSentences = cfProvider.getNumSentences();

		int tenPercent = Math.max(numSentences / 10, 1);

		// loop over the feature vectors
		System.out.println("\nAnalysing feature vectors of " + numSentences + " sentences:");
		for (int index = 0; index < numSentences; index++) {

			if ((index % tenPercent) == 0 && index != 0) {
				int percentage = index / tenPercent;
				System.out.print(" " + percentage + "0% ");
			}
			// for each vector, get the values for the relevant features
			// add them to the list of possible values

			byte[] features = cfProvider.getCoverageFeatures(index); // the feature vectors of one sentence
			if (features == null) {
				System.err.println("WARNING: null features for sentence id " + cfProvider.getID(index) + " -- skipping");
				continue;
			}
			int numFeatVects = features.length / numTargetFeaturesUsed;

			// compute statistics of sentence length
			averageSentLength += numFeatVects;
			if (numFeatVects > maxSentLength)
				maxSentLength = numFeatVects;
			if (numFeatVects < minSentLength)
				minSentLength = numFeatVects;

			// System.out.println("Analysing feature vectors of sentence id " + cfProvider.getID(index) + ":  numFeatVects=" +
			// numFeatVects);

			// Loop over all feature vectors in current sentence:
			for (int i = 0; i < numFeatVects; i++) {
				numTokens++;

				// first deal with current phone
				// byte nextPhonebyte = getVectorValue(vectorBuf,i,phoneFeatIndex);
				byte nextPhonebyte = features[i * numTargetFeaturesUsed + phoneFeatIndex];

				// System.out.println("i=" + i + " phone="
				// + featDef.getFeatureValueAsString(myphoneFeatIndex,nextPhonebyte));

				// add 1 to the frequency value of the phone
				phoneFrequencies[nextPhonebyte]++;

				// deal with current diphone
				// byte nextnextPhonebyte = getVectorValue(vectorBuf,i,diphoneFeatIndex);
				byte nextnextPhonebyte = features[i * numTargetFeaturesUsed + diphoneFeatIndex];
				String simpleDiphone = possibleDiphones[nextPhonebyte][nextnextPhonebyte];

				// add 1 to the frequency value of the diphone
				diphoneFrequencies[nextPhonebyte][nextnextPhonebyte]++;

				// deal with current diphone
				// byte prosodyValue = getVectorValue(vectorBuf,i,prosodyIndex);
				byte prosodyValue = features[i * numTargetFeaturesUsed + prosodyIndex];
				simpleFeatVectTypes.add(possibleDiphonesProsody[nextPhonebyte][nextnextPhonebyte][prosodyValue]);

				// save feature vector in simple diphone tree
				// CoverLeaf leaf = goDownTree(vectorBuf,i,false);
				// leaf.addPossibleInstance();
				CoverLeaf leaf = (CoverLeaf) simpleCover.children[nextPhonebyte].children[nextnextPhonebyte].children[prosodyValue];
				leaf.maxNumFeatVects++;

			}
		}
		System.out.println(" 100% ");

		// count phones
		numPhoneTypes = getPhonesInCorpus().size();

		// count diphones
		numSimpleDiphoneTypes = 0;
		for (int i = 0; i < possibleDiphones.length; i++) {
			for (int j = 0; j < possibleDiphones[i].length; j++) {
				if (diphoneFrequencies[i][j] > 0) {
					numSimpleDiphoneTypes++;
				}
			}
		}

		// compute average sentence length
		averageSentLength = averageSentLength / (double) numSentences;

		// calculate cover size
		numPossibleSimpleDiphones = numPhoneValuesMinusIgnored * (numPhoneValuesMinusIgnored + 1);
		numSimpleLeaves = numPossibleSimpleDiphones * numProsodyValues;

		// number of feature vector types
		numSimpleFeatVectTypes = simpleFeatVectTypes.size();
		// compute coverage of corpus
		possiblePhoneCoverage = (double) numPhoneTypes / (double) (numPhoneValuesMinusIgnored);
		possibleSimpleDiphoneCoverage = numSimpleDiphoneTypes / (double) numPossibleSimpleDiphones;
		possibleOverallSimpleCoverage = (double) numSimpleFeatVectTypes / (double) numSimpleLeaves;

		// calculate relative frequency for each node
		// rel. freq. = freq / all tokens
		if (simpleDiphones) {
			computeRelativeFrequency(simpleCover, numTokens);
		}

	}

	/**
	 * Build the trees that represent the cover sets
	 *
	 */
	private void buildCover() {
		simpleCover = new CoverNode((byte) numPhoneValues, wantedWeightDecrease);

		// compute all possible combinations
		for (int k = 0; k < possiblePhoneArray.length; k++) {
			if (phonesToIgnore.contains(new Integer(k)))
				continue;
			// find out the index of the current phone
			byte nextIndex = (byte) k;

			// add a node for the phonetic identity of the next phone
			CoverNode nextSimpleChild = new CoverNode((byte) numPhoneValues, wantedWeightDecrease);

			// set the weight that determines how many instances
			// are wanted of this phone
			nextSimpleChild.setWantedWeight(phoneLevelWeight);

			simpleCover.addChild(nextSimpleChild, nextIndex);

			byte numGrandChildren = nextSimpleChild.getNumChildren();
			// go through the grandchildren of simpleCover
			for (byte i = 0; i < numGrandChildren; i++) {
				// each grandchild is a prosody node
				CoverNode prosodyNode = new CoverNode((byte) numProsodyValues, wantedWeightDecrease);

				// set the weight that determines how many instances
				// are wanted of this diphone
				prosodyNode.setWantedWeight(diphoneLevelWeight);
				nextSimpleChild.addChild(prosodyNode, i);
				// go through the children of the prosody node
				for (byte j = 0; j < numProsodyValues; j++) {
					// each child is a leaf
					CoverLeaf prosodyChild = new CoverLeaf(wantedWeightDecrease);
					// set the weight that determines how many instances
					// are wanted of this prosody variation
					prosodyChild.setWantedWeight(prosodyLevelWeight);
					prosodyNode.addChild(prosodyChild, j);
				}
			}
		}

	}

	/**
	 * Get descriptive statistics for the full corpus. These values are independent of the selection of any sentences.
	 * 
	 * @return cs
	 */
	public CoverageStatistics getCorpusStatistics() {
		CoverageStatistics cs = new CoverageStatistics();
		cs.coveredPhones = getPhonesInCorpus();
		cs.allPhones = getAllPhones();
		cs.coveredDiphones = getDiphonesInCorpus();
		cs.numPossibleDiphones = numPossibleSimpleDiphones;
		cs.numCoveredDiphonesWithProsody = numSimpleFeatVectTypes;
		cs.numPossibleDiphonesWithProsody = numSimpleLeaves;
		cs.numTokens = numTokens;
		return cs;
	}

	public Set<String> getPhonesInCorpus() {
		Set<String> phones = new TreeSet<String>();
		int phoneFeatureIndex = featDef.getFeatureIndex("phone");
		for (int i = 0; i < phoneFrequencies.length; i++) {
			if (phoneFrequencies[i] > 0) {
				phones.add(featDef.getFeatureValueAsString(phoneFeatureIndex, i));
			}
		}
		return phones;
	}

	public Set<String> getAllPhones() {
		Set<String> phones = new TreeSet<String>();
		int phoneFeatureIndex = featDef.getFeatureIndex("phone");
		int i = 0;
		for (String ph : featDef.getPossibleValues(phoneFeatureIndex)) {
			if (!phonesToIgnore.contains(i)) {
				phones.add(ph);
			}
			i++;
		}
		return phones;
	}

	public Set<String> getDiphonesInCorpus() {
		Set<String> diphones = new HashSet<String>();
		for (int i = 0; i < phoneFrequencies.length; i++) {
			for (int j = 0; j < phoneFrequencies.length; j++) {
				if (diphoneFrequencies[i][j] > 0) {
					diphones.add(possibleDiphones[i][j]);
				}
			}
		}
		return diphones;
	}

	/**
	 * Go down the cover tree according to the values in the feature vector
	 * 
	 * @param simpleDiphones
	 *            if true, go down simple cover tree, else go down clustered cover tree
	 * @param vectors
	 *            the feature vectors
	 * @param index
	 *            the index of the current feature vector
	 * @param addNewFeatureVector
	 *            if true, decrease wantedWeights of the nodes you pass
	 * @return the leaf that you arrived at
	 */
	private CoverLeaf goDownTree(byte[] vectors, int index, boolean addNewFeatureVector) {
		// go down to phone level
		// byte nextIndex = getVectorValue(vectors,index,phoneFeatIndex);
		byte nextIndex = vectors[index * numTargetFeaturesUsed + phoneFeatIndex];
		// CoverNode nextNode = simpleCover.getChild(nextIndex);
		CoverNode nextNode = simpleCover.children[nextIndex];

		if (addNewFeatureVector)
			nextNode.decreaseWantedWeight();

		// go down to diphone level
		// nextIndex = getVectorValue(vectors,index,diphoneFeatIndex);
		nextIndex = vectors[index * numTargetFeaturesUsed + diphoneFeatIndex];
		// nextNode = nextNode.getChild(nextIndex);
		nextNode = nextNode.children[nextIndex];
		if (addNewFeatureVector)
			nextNode.decreaseWantedWeight();

		// go down to prosody level
		// nextIndex = getVectorValue(vectors,index,prosodyIndex);
		nextIndex = vectors[index * numTargetFeaturesUsed + prosodyIndex];
		// nextNode = nextNode.getChild(nextIndex);
		nextNode = nextNode.children[nextIndex];
		if (addNewFeatureVector)
			nextNode.decreaseWantedWeight();

		if (!(nextNode instanceof CoverLeaf)) {
			// something went wrong
			throw new Error("Went down cover tree for feature vector" + " and did not end up on leaf!");
		}
		return (CoverLeaf) nextNode;

	}

	/**
	 * Compute the relative frequency of each node in the corpus
	 * 
	 * @param node
	 *            the node to compute the frequency for
	 * @param allTokens
	 *            total number of tokens in the corpus
	 * @return the frequency for the given node
	 */
	private double computeRelativeFrequency(CoverNode node, double allTokens) {
		double freq = 0;
		if (node instanceof CoverLeaf) {
			// compute the relative frequency for this leaf
			int numPossInstances = ((CoverLeaf) node).maxNumFeatVects();
			freq = (double) numPossInstances / allTokens;
			if (considerFrequency) {
				if (frequencySetting.equals("1minus")) {
					node.setFrequencyWeight(1 - freq);
				} else {
					if (frequencySetting.equals("inverse")) {
						node.setFrequencyWeight(1 / freq);
					} else {
						node.setFrequencyWeight(freq);
					}
				}
			}
		} else {
			// frequency is the sum of the frequency of the children
			byte numChildren = node.getNumChildren();
			// go through children
			for (byte i = 0; i < numChildren; i++) {
				CoverNode child = node.getChild(i);
				if (child == null)
					continue;
				freq += computeRelativeFrequency(child, allTokens);
			}
			if (considerFrequency) {
				if (frequencySetting.equals("1minus")) {
					node.setFrequencyWeight(1 - freq);
				} else {
					if (frequencySetting.equals("inverse")) {
						node.setFrequencyWeight(1 / freq);
					} else {
						node.setFrequencyWeight(freq);
					}
				}
			}
		}
		return freq;

	}

	/**
	 * Print a statistic of the unit distribution in the corpus
	 * 
	 * @param out
	 *            the print writer to print to
	 * @throws Exception
	 *             Exception
	 */
	public void printTextCorpusStatistics(PrintWriter out) throws Exception {
		DecimalFormat df = new DecimalFormat("0.00000");
		out.println("*********************" + "\n* Unit distribution *" + "\n*********************\n\n");

		/* print out the sentence length statistics */
		out.println("Number of sentences : " + numSentences);
		out.println("Average sentence length : " + averageSentLength);
		out.println("Maximum sentence length : " + maxSentLength);
		out.println("Minimum sentence length : " + minSentLength);

		/* print out coverage statistics */
		CoverageStatistics stats = getCorpusStatistics();
		out.println(stats);

		/*
		 * out.println("\nClustered Coverage:"); out.println("phones: "+df.format(possiblePhoneCoverage));
		 * out.println("diphones: "+df.format(possibleClusteredDiphoneCoverage));
		 * out.println("overall: "+df.format(possibleOverallClusteredCoverage)+"\n\n");
		 */

		if (possiblePhoneCoverage < 1) {
			out.println("The following phones are missing: ");
			for (int k = 1; k < possiblePhoneArray.length; k++) {
				String nextPhone = possiblePhoneArray[k];
				if (phonesToIgnore.contains(new Integer(k)))
					continue;
				if (!possiblePhoneTypes.contains(nextPhone)) {
					out.print(nextPhone + " ");
				}
			}
			out.print("\n");
		}

		out.println("\n\nDiphones and their frequencies :\n");
		out.println("Simple diphones:\n");
		printDiphones(out);

		out.flush();
		out.close();
	}

	/**
	 * Print the settings of the config file
	 * 
	 * @param out
	 *            the PrintWriter to print to
	 */
	public void printSettings(PrintWriter out) {
		/* print out setings */
		out.println("\nSettings of Coverage Definition:");
		out.println("simpleDiphones " + Boolean.toString(simpleDiphones));
		if (considerFrequency) {
			out.println("frequency " + frequencySetting);
		} else {
			out.println("frequency none");
		}
		out.println("considerSentenceLength " + Boolean.toString(considerSentenceLength));

		out.println("phoneLevelWeight " + phoneLevelWeight);
		out.println("diphoneLevelWeight " + diphoneLevelWeight);
		out.println("prosodyLevelWeight " + prosodyLevelWeight);
		out.println("divideWantedWeightBy " + wantedWeightDecrease);
		if (considerSentenceLength) {
			out.println("maxSentenceLength " + maxSentLengthAllowed);
			out.println("minSentenceLength " + minSentLengthAllowed);
		}

	}

	/**
	 * Print the diphone distribution of the corpus
	 * 
	 * @param out
	 *            the PrintWriter to print to
	 * @param ph2Frequency
	 *            maps from diphones to their frequency
	 */
	private void printDiphones(PrintWriter out) {
		DecimalFormat df = new DecimalFormat("0.00000");
		// Sort phones according to their frequencies
		TreeMap<Integer, List<String>> freq2Diphones = new TreeMap<Integer, List<String>>(Collections.reverseOrder());
		Map<Integer, Double> freq2Prob = new HashMap<Integer, Double>();
		for (int i = 0; i < possibleDiphones.length; i++) {
			for (int j = 0; j < possibleDiphones[i].length; j++) {
				String diphone = possibleDiphones[i][j];
				int freq = diphoneFrequencies[i][j];
				if (!freq2Diphones.containsKey(freq)) {
					List<String> phoneList = new ArrayList<String>();
					phoneList.add(diphone);
					freq2Diphones.put(freq, phoneList);
					double prob = (double) freq * 100.0 / (double) numTokens;
					freq2Prob.put(freq, prob);
				} else {
					List<String> phoneList = freq2Diphones.get(freq);
					phoneList.add(diphone);
				}
			}
		}

		// output phones and their frequencies
		Set<Integer> frequencies = freq2Diphones.keySet();
		for (Integer nextFreq : frequencies) {
			Double nextProb = freq2Prob.get(nextFreq);
			List<String> nextPhoneList = freq2Diphones.get(nextFreq);
			for (int i = 0; i < nextPhoneList.size(); i++) {
				out.print(nextPhoneList.get(i));
				out.print(" : ");
				out.print(nextFreq);
				out.print(", ");
				out.println(df.format(nextProb));
			}
		}
	}

	/**
	 * Print statistics of the selected sentences and a table of coverage development over time
	 * 
	 * @param distributionFile
	 *            the file to print the statistics to
	 * @param developmentFile
	 *            the file to print the coverage development to
	 * @param logDevelopment
	 *            if true, print development file
	 * @throws Exception
	 *             Exception
	 */
	public void printSelectionDistribution(String distributionFile, String developmentFile, boolean logDevelopment)
			throws Exception {
		PrintWriter disOut = new PrintWriter(new FileWriter(new File(distributionFile)));
		/* print settings */
		DecimalFormat df = new DecimalFormat("0.00000");
		disOut.println("\nSettings of Coverage Definition:");
		disOut.println("simpleDiphones " + Boolean.toString(simpleDiphones));
		if (considerFrequency) {
			disOut.println("frequency " + frequencySetting);
		} else {
			disOut.println("frequency none");
		}
		disOut.println("considerSentenceLength " + Boolean.toString(considerSentenceLength));
		disOut.println("phoneLevelWeight " + phoneLevelWeight);
		disOut.println("diphoneLevelWeight " + diphoneLevelWeight);
		disOut.println("prosodyLevelWeight " + prosodyLevelWeight);
		disOut.println("maxSentenceLength " + maxSentLengthAllowed);
		disOut.println("minSentenceLength " + minSentLengthAllowed);
		disOut.println("divideWantedWeightBy " + wantedWeightDecrease);

		/* print results */
		disOut.println("\nResults:");
		disOut.println("Num sent in cover : " + numSentencesInCover);
		double avSentLength = (double) numSelectedFeatVects / (double) numSentencesInCover;
		disOut.println("Avg sent length : " + df.format(avSentLength));
		disOut.println("Max sent length : " + maxSentLengthInCover);
		disOut.println("Min sent length : " + minSentLengthInCover);

		/* print distribution info */
		double phoneCov = (double) phonesInCover.size() / (double) numPhoneValuesMinusIgnored;
		double simpleDiphoneCov = (double) simpleDiphonesInCover.size() / (double) numPossibleSimpleDiphones;
		double overallSimpleCov = (double) numSimpleFeatVectsInCover / (double) numSimpleLeaves;
		// double clusteredDiphoneCov = (double)clusteredDiphonesInCover.size()/(double)numPossibleClusteredDiphones;
		// double overallClusteredCov = (double)numClusteredFeatVectsInCover/(double)numClusteredLeaves;

		disOut.println("phones: " + df.format(phoneCov) + " (" + df.format(possiblePhoneCoverage) + ")");

		disOut.println("Simple Coverage:");
		disOut.println("phones: " + df.format(phoneCov) + " (" + df.format(possiblePhoneCoverage) + ")");
		disOut.println("diphones: " + df.format(simpleDiphoneCov) + " (" + df.format(possibleSimpleDiphoneCoverage) + ")");
		disOut.println("overall: " + df.format(overallSimpleCov) + " (" + df.format(possibleOverallSimpleCoverage) + ")");

		/*
		 * disOut.println("Clustered Coverage:");
		 * //disOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
		 * disOut.println("diphones: "+df.format(clusteredDiphoneCov) +" ("+df.format(possibleClusteredDiphoneCoverage)+")");
		 * disOut.println("overall: "+df.format(overallClusteredCov) +" ("+df.format(possibleOverallClusteredCoverage)+")");
		 */
		disOut.flush();
		disOut.close();

		/* print coverage development over time */
		if (logDevelopment) {
			PrintWriter devOut = new PrintWriter(new FileWriter(new File(developmentFile)));
			devOut.println("\toverall coverage\tdiphone coverage\tphone coverage");
			for (int i = 0; i < overallCoverageInTime.size(); i++) {
				devOut.print(i + "\t" + df.format(overallCoverageInTime.get(i)) + "\t" + df.format(diphoneCoverageInTime.get(i))
						+ "\t" + df.format(phoneCoverageInTime.get(i)) + "\n");
			}
			devOut.flush();
			devOut.close();
		}
	}

	public void printResultToLog(PrintWriter logOut) {
		/* print settings */
		DecimalFormat df = new DecimalFormat("0.00000");
		logOut.println("simpleDiphones " + Boolean.toString(simpleDiphones));
		if (considerFrequency) {
			logOut.println("frequency " + frequencySetting);
		} else {
			logOut.println("frequency none");
		}
		logOut.println("considerSentenceLength " + Boolean.toString(considerSentenceLength));
		logOut.println("phoneLevelWeight " + phoneLevelWeight);
		logOut.println("diphoneLevelWeight " + diphoneLevelWeight);
		logOut.println("prosodyLevelWeight " + prosodyLevelWeight);
		logOut.println("maxSentenceLength " + maxSentLengthAllowed);
		logOut.println("minSentenceLength " + minSentLengthAllowed);
		logOut.println("divideWantedWeightBy " + wantedWeightDecrease);

		logOut.println("\nNum sent in cover : " + numSentencesInCover);
		double avSentLength = (double) numSelectedFeatVects / (double) numSentencesInCover;
		logOut.println("Avg sent length : " + df.format(avSentLength));
		logOut.println("Max sent length : " + maxSentLengthInCover);
		logOut.println("Min sent length : " + minSentLengthInCover);

		/* print distribution info */
		double phoneCov = (double) phonesInCover.size() / (double) numPhoneValuesMinusIgnored;
		double simpleDiphoneCov = (double) simpleDiphonesInCover.size() / (double) numPossibleSimpleDiphones;
		double overallSimpleCov = (double) numSimpleFeatVectsInCover / (double) numSimpleLeaves;

		logOut.println("phones: " + df.format(phoneCov) + " (" + df.format(possiblePhoneCoverage) + ")");

		logOut.println("Simple Coverage:");
		// logOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
		logOut.println("diphones: " + df.format(simpleDiphoneCov) + " (" + df.format(possibleSimpleDiphoneCoverage) + ")");
		logOut.println("overall: " + df.format(overallSimpleCov) + " (" + df.format(possibleOverallSimpleCoverage) + ")");

		/*
		 * logOut.println("Clustered Coverage:");
		 * //logOut.println("phones: "+df.format(phoneCov)+" ("+df.format(possiblePhoneCoverage)+")");
		 * logOut.println("diphones: "+df.format(clusteredDiphoneCov) +" ("+df.format(possibleClusteredDiphoneCoverage)+")");
		 * logOut.println("overall: "+df.format(overallClusteredCov) +" ("+df.format(possibleOverallClusteredCoverage)+")\n\n");
		 */

	}

	/**
	 * Add the feature vectors for one sentence to the cover
	 * 
	 * @param features
	 *            the feature vectors to add
	 */
	public void updateCover(byte[] features) {
		int numFeatVects = features.length / numTargetFeaturesUsed;
		// loop through the feature vectors
		for (int i = 0; i < numFeatVects; i++) {

			/* update simpleCover */
			CoverLeaf leaf = goDownTree(features, i, true);
			// if this is the first feature vector in this leaf
			// decrease cover size
			if (leaf.getNumFeatureVectors() == 0) {
				numSimpleFeatVectsInCover++;
			}
			leaf.addFeatureVector();
			String phone = possiblePhoneArray[getVectorValue(features, i, phoneFeatIndex)];
			// update coverage statistics
			String diphone = phone + "_" + possibleNextPhoneArray[getVectorValue(features, i, diphoneFeatIndex)];

			phonesInCover.add(phone);
			simpleDiphonesInCover.add(diphone);

		}
		// update phone coverage statistics
		double phoneCoverage = (double) phonesInCover.size() / (double) numPhoneValuesMinusIgnored;
		phoneCoverageInTime.add(new Double(phoneCoverage));
		// update diphone and overall coverage statistics
		if (simpleDiphones) {
			double diphoneCoverage = (double) simpleDiphonesInCover.size() / (double) numPossibleSimpleDiphones;
			double overallCoverage = (double) numSimpleFeatVectsInCover / (double) numSimpleLeaves;
			diphoneCoverageInTime.add(new Double(diphoneCoverage));
			overallCoverageInTime.add(new Double(overallCoverage));
		}
		// compute statistics of sentence length
		numSentencesInCover++;
		if (numFeatVects > maxSentLengthInCover)
			maxSentLengthInCover = numFeatVects;
		if (numFeatVects < minSentLengthInCover)
			minSentLengthInCover = numFeatVects;
		numSelectedFeatVects += numFeatVects;
	}

	/**
	 * Check if cover has maximum simple diphone coverage
	 * 
	 * @return true if cover has maximum simple diphone coverage
	 */
	public boolean reachedMaxSimpleDiphones() {
		return simpleDiphonesInCover.size() >= numSimpleDiphoneTypes;
	}

	/**
	 * Check if cover has maximum simple prosody coverage
	 * 
	 * @return true if cover has maximum simple prosody coverage
	 */
	public boolean reachedMaxSimpleProsody() {
		return numSimpleFeatVectsInCover == numSimpleFeatVectTypes;
	}

	/**
	 * Get the usefulness of the given feature vectors Usefulness of a feature vector is defined as the sum of the score for the
	 * feature vectors on all levels of the tree. On each level, the score is the product of the two weights of the node. The
	 * first weight reflects the frequency/ inverted frequency of the value associated with the node in the corpus (&rarr;
	 * frequencyWeight). The second weight reflects how much an instance of a feature vector containing the associated value is
	 * wanted in the cover (&rarr; wantedWeight).
	 * 
	 * @param featureVectors
	 *            the feature vectors
	 * @return the usefulness
	 */
	public double usefulnessOfFVs(byte[] featureVectors) {
		double usefulness = 0.0;
		// int numFeatureVectors = featureVectors.length/4;
		int numFeatureVectors = featureVectors.length / numTargetFeaturesUsed;
		if (considerSentenceLength) {
			// too long sentences are useless
			if (numFeatureVectors > maxSentLengthAllowed)
				return -1.0;
			// too short sentences are useless as well
			if (numFeatureVectors < minSentLengthAllowed)
				return -1.0;
		}
		// loop over the feature vectors
		// System.out.print("Usefulness = ");
		// we cannot trust that all bytes in the feature vector are meaningful -- therefore,
		// it is not guaranteed that numFeatureVectors * numTargetFeaturesUsed == featureVectors.length!!
		for (int pos = 0, max = numFeatureVectors * numTargetFeaturesUsed; pos < max; pos += numTargetFeaturesUsed) {

			double u = 0;
			// get the associated leaf
			// go down to phone level
			// byte nextIndex = getVectorValue(featureVectors,i,phoneFeatIndex);
			byte nextIndex = featureVectors[pos + phoneFeatIndex];
			CoverNode nextNode = simpleCover.children[nextIndex];

			// double relFreq = nextNode.getFrequencyWeight();
			// double wantedWeight = nextNode.getWantedWeight();
			// System.out.print(" +"+relFreq+"*"+wantedWeight);
			// u += nextNode.frequencyWeight * nextNode.wantedWeight;
			u += nextNode.usefulness;
			// go down to diphone level
			// nextIndex = getVectorValue(featureVectors,i,diphoneFeatIndex);
			nextIndex = featureVectors[pos + diphoneFeatIndex];
			nextNode = nextNode.children[nextIndex];
			// relFreq = nextNode.getFrequencyWeight();
			// wantedWeight = nextNode.getWantedWeight();
			// System.out.print(" +"+relFreq+"*"+wantedWeight);
			// u += nextNode.frequencyWeight * nextNode.wantedWeight;
			u += nextNode.usefulness;
			// go down to prosody level
			// nextIndex = getVectorValue(featureVectors,i,prosodyIndex);
			nextIndex = featureVectors[pos + prosodyIndex];
			nextNode = nextNode.children[nextIndex];
			// relFreq = nextNode.getFrequencyWeight();
			// wantedWeight = nextNode.getWantedWeight();
			// System.out.print(" +"+relFreq+"*"+wantedWeight+"\n");
			// u += nextNode.frequencyWeight * nextNode.wantedWeight;
			u += nextNode.usefulness;
			usefulness += u;
		}
		// System.out.print(" = "+usefulness+"\n");
		return usefulness / (double) numFeatureVectors;
	}

	public CoverageFeatureProvider getCoverageFeatureProvider() {
		return cfProvider;
	}

	public byte getVectorValue(byte[] vectors, int vectorIndex, int valueIndex) {
		// byte result = vectors[vectorIndex*4+valueIndex];
		return vectors[vectorIndex * numTargetFeaturesUsed + valueIndex];
	}

	/**
	 * Print the cover sets to the given file
	 * 
	 * @param filename
	 *            the file to print to
	 * @throws Exception
	 *             Exception
	 */
	public void writeCoverageBin(String filename) throws Exception {
		DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(filename)));
		/* print all the relevant information */
		out.writeInt(numTokens);
		out.writeInt(numSimpleDiphoneTypes);
		out.writeInt(numSimpleFeatVectTypes);
		out.writeDouble(averageSentLength);
		out.writeInt(maxSentLength);
		out.writeInt(minSentLength);
		out.writeInt(numSimpleLeaves);
		out.writeDouble(possiblePhoneCoverage);
		out.writeDouble(possibleSimpleDiphoneCoverage);
		out.writeDouble(possibleOverallSimpleCoverage);
		out.writeInt(numSentences);
		/* print the coverage tree */
		writeTreeBin(out, simpleCover);
		out.flush();
		out.close();
	}

	/**
	 * Print the cover tree
	 * 
	 * @param out
	 *            the output stream to write to
	 * @param cover
	 *            the tree to print
	 * @throws IOException
	 *             IOException
	 */
	private void writeTreeBin(DataOutputStream out, CoverNode cover) throws IOException {

		// go down to phone level
		byte numChildren = cover.getNumChildren();
		double frequencyWeight = cover.getFrequencyWeight();
		double wantedWeight = cover.getWantedWeight();
		double wantedWeightDecrease = cover.getWantedWeightDecrease();
		out.writeByte(numChildren);
		for (byte i = 0; i < numChildren; i++) {
			if (phonesToIgnore.contains(new Integer(i)))
				continue;
			CoverNode phoneNode = cover.getChild(i);
			frequencyWeight = phoneNode.getFrequencyWeight();
			wantedWeight = phoneNode.getWantedWeight();
			wantedWeightDecrease = phoneNode.getWantedWeightDecrease();
			byte numNextChildren = phoneNode.getNumChildren();
			out.writeByte(numNextChildren);
			// go down to diphone level
			for (byte j = 0; j < numNextChildren; j++) {
				CoverNode diphoneNode = phoneNode.getChild(j);
				// go down to prosody level
				byte numNextNextChildren = diphoneNode.getNumChildren();
				out.writeByte(numNextNextChildren);
				for (byte k = 0; k < numNextNextChildren; k++) {
					CoverLeaf nextLeaf = (CoverLeaf) diphoneNode.getChild(k);
					int numVectors = nextLeaf.maxNumFeatVects();
					frequencyWeight = nextLeaf.getFrequencyWeight();
					wantedWeight = nextLeaf.getWantedWeight();
					wantedWeightDecrease = nextLeaf.getWantedWeightDecrease();
					out.writeInt(numVectors);
				}

			}

		}

	}

	/**
	 * Read the cover sets from the given file
	 * 
	 * @param filename
	 *            the file containing the cover sets
	 * @param idSentenceList
	 *            the id of the sentence list
	 * @throws Exception
	 *             Exception
	 */
	// public void readCoverageBin(String filename, FeatureDefinition fDef, String[] basenames)throws Exception{
	public void readCoverageBin(String filename, int[] idSentenceList) throws Exception {

		DataInputStream in = new DataInputStream(new FileInputStream(new File(filename)));
		/* read all the relevant information */
		numTokens = in.readInt();
		numSimpleDiphoneTypes = in.readInt();
		numSimpleFeatVectTypes = in.readInt();
		averageSentLength = in.readDouble();
		maxSentLength = in.readInt();
		minSentLength = in.readInt();
		numSimpleLeaves = in.readInt();
		possiblePhoneCoverage = in.readDouble();
		possibleSimpleDiphoneCoverage = in.readDouble();
		possibleOverallSimpleCoverage = in.readDouble();
		numSentences = in.readInt();
		/* print the coverage tree */
		readTreeBin(in);
		in.close();
		System.out.print("Num Tokens: " + numTokens + "\n");
	}

	/**
	 * Read a cover tree
	 * 
	 * @param in
	 *            the inputstream to read from
	 * @param isSimpleCover
	 *            if true, build the cover tree fro simpleDiphones
	 * @throws Exception
	 *             Exception
	 */
	private void readTreeBin(DataInputStream in) throws Exception {
		byte numChildren = in.readByte();
		double wantedWeight = 0.0;
		CoverNode cover = new CoverNode(numChildren, wantedWeightDecrease, wantedWeight);
		for (byte i = 0; i < numChildren; i++) {
			if (phonesToIgnore.contains(new Integer(i)))
				continue;
			byte nextNumChildren = in.readByte();

			CoverNode diphoneNode = new CoverNode(nextNumChildren, wantedWeightDecrease, phoneLevelWeight);
			cover.addChild(diphoneNode, i);

			for (byte j = 0; j < nextNumChildren; j++) {
				byte nextNextNumChildren = in.readByte();
				CoverNode prosodyNode = new CoverNode(nextNextNumChildren, wantedWeightDecrease, diphoneLevelWeight);
				diphoneNode.addChild(prosodyNode, j);

				for (byte k = 0; k < nextNextNumChildren; k++) {
					int numVectors = in.readInt();
					CoverLeaf leafNode = new CoverLeaf(wantedWeightDecrease, prosodyLevelWeight, numVectors);
					prosodyNode.addChild(leafNode, k);
				}

			}

		}
		computeRelativeFrequency(cover, numTokens);
		simpleCover = cover;
	}

	/**
	 * A node in the cover tree Represents a feature. Number of children is the number of possible values.
	 * 
	 * @author Anna Hunecke
	 *
	 */
	class CoverNode {

		/* children of this node */
		protected CoverNode[] children;
		/* number of children of this node */
		private byte numChildren;
		/*
		 * how much is this node and its children wanted in the cover
		 */
		protected double wantedWeight;
		/*
		 * frequency/inverted frequency of the node in the corpus
		 */
		protected double frequencyWeight;
		/* number by which the wantedWeight is divided */
		protected double wantedWeightDecrease;

		/* usefulness is the product of wantedWeight and frequencyWeight. It is here purely for efficiency reasons. */
		protected double usefulness;

		/**
		 * Build a new CoverNode Set frequency weight to 1
		 */
		public CoverNode() {
			frequencyWeight = 1;
			usefulness = 0;
		}

		/**
		 * Build a new CoverNode
		 * 
		 * @param numChildren
		 *            the number of children
		 * @param wantedWeightDecrease
		 *            the value by which the wanted weight is divided
		 * @param wantedWeight
		 *            the wanted weight
		 */
		public CoverNode(byte numChildren, double wantedWeightDecrease, double wantedWeight) {
			this.numChildren = numChildren;
			children = new CoverNode[numChildren];
			this.wantedWeightDecrease = wantedWeightDecrease;
			this.wantedWeight = wantedWeight;
			frequencyWeight = 1;
			usefulness = frequencyWeight * this.wantedWeight;
		}

		/**
		 * Build a new CoverNode
		 * 
		 * @param values
		 *            the number of values
		 * @param wantedWeightDecrease
		 *            the wantedWeightDecrease
		 */
		public CoverNode(byte values, double wantedWeightDecrease) {
			children = new CoverNode[values];
			numChildren = (byte) children.length;
			frequencyWeight = 1;
			this.wantedWeightDecrease = wantedWeightDecrease;
			usefulness = 0;
		}

		/**
		 * Add a new child
		 * 
		 * @param child
		 *            the child
		 * @param value
		 *            the position of the child in the children array
		 */
		public void addChild(CoverNode child, byte value) {
			children[value] = child;
		}

		/**
		 * Get a child
		 * 
		 * @param value
		 *            the position of the child in the children array
		 * @return the child (null, if there is no child at this position)
		 */
		public CoverNode getChild(byte value) {
			return children[value];
		}

		/**
		 * Get the number of children
		 * 
		 * @return the number of children
		 */
		public byte getNumChildren() {
			return numChildren;
		}

		/**
		 * Set the wantedWeight
		 * 
		 * @param wantedWeight
		 *            the new wantedWeight
		 */
		public void setWantedWeight(double wantedWeight) {
			this.wantedWeight = wantedWeight;
			usefulness = this.wantedWeight * frequencyWeight;
		}

		/**
		 * Get the wantedWeight
		 * 
		 * @return the wantedWeight
		 */
		public double getWantedWeight() {
			return wantedWeight;
		}

		/**
		 * Get the wantedWeightDecrease
		 * 
		 * @return the wantedWeightDecrease
		 */
		public double getWantedWeightDecrease() {
			return wantedWeightDecrease;
		}

		/**
		 * Decrease the wantedWeight by dividing it by wantedWeightDecrease
		 *
		 */
		public void decreaseWantedWeight() {
			wantedWeight = wantedWeight / wantedWeightDecrease;
			usefulness = frequencyWeight * wantedWeight;
		}

		/**
		 * Set the frequencyWeight
		 * 
		 * @param frequencyWeight
		 *            the new frequencyWeight
		 */
		public void setFrequencyWeight(double frequencyWeight) {
			this.frequencyWeight = frequencyWeight;
			usefulness = this.frequencyWeight * wantedWeight;
		}

		/**
		 * Get the frequencyWeight
		 * 
		 * @return the frequencyWeight
		 */
		public double getFrequencyWeight() {
			return frequencyWeight;
		}

	}

	/**
	 * A leaf in the cover tree. Collects the feature vectors that belong to the path that leads to the leaf.
	 * 
	 * @author Anna Hunecke
	 *
	 */
	class CoverLeaf extends CoverNode {

		/* the number of feature vectors in this node */
		private int numFeatVects;
		/*
		 * the maximimum number of feature vectors that could be in this node (according to the corpus)
		 */
		private int maxNumFeatVects;

		/**
		 * Build a new cover leaf
		 * 
		 * @param wantedWeightDecrease
		 *            the wantedWeightDecrease
		 */
		public CoverLeaf(double wantedWeightDecrease) {
			super();
			numFeatVects = 0;
			maxNumFeatVects = 0;
			this.wantedWeightDecrease = wantedWeightDecrease;
			frequencyWeight = 1;
		}

		/**
		 * Build a new CoverLeaf
		 * 
		 * @param wantedWeightDecrease
		 *            the wantedWeightDecrease
		 * @param wantedWeight
		 *            the wanted weight
		 * @param maxNumFeatVects
		 *            maximum number of feature vectors that can be collected in this leaf
		 */
		public CoverLeaf(double wantedWeightDecrease, double wantedWeight, int maxNumFeatVects) {
			this.wantedWeightDecrease = wantedWeightDecrease;
			this.wantedWeight = wantedWeight;
			this.maxNumFeatVects = maxNumFeatVects;
			frequencyWeight = 1;
		}

		/**
		 * Add a new feature vector
		 * 
		 * @param featureVector
		 *            the new feature vector
		 */
		public void addFeatureVector() {
			numFeatVects++;
		}

		/**
		 * Increase the maximum number of feature vectors by one (because we have seen a feature vector for this node in the
		 * corpus)
		 */
		public void addPossibleInstance() {
			maxNumFeatVects++;
		}

		/**
		 * Get the number of feature vectors of this node
		 * 
		 * @return the number of feature vectors
		 */
		public int getNumFeatureVectors() {
			return numFeatVects;
		}

		/**
		 * Get the maximum number of feature vectors of this node
		 * 
		 * @return the maximum number of feature vectors
		 */
		public int maxNumFeatVects() {
			return maxNumFeatVects;
		}
	}

	public static class CoverageStatistics {
		public Set<String> coveredPhones;
		public Set<String> allPhones;
		public Set<String> coveredDiphones;
		public int numPossibleDiphones;
		public int numCoveredDiphonesWithProsody;
		public int numPossibleDiphonesWithProsody;
		public int numTokens;

		@Override
		public String toString() {
			StringWriter sw = new StringWriter();
			PrintWriter out = new PrintWriter(sw);
			out.println("\nSimple Coverage:");
			out.printf(Locale.US, "phones: %.5f\n", coveredPhones.size() / (double) allPhones.size());
			out.printf(Locale.US, "diphones: %.5f\n", coveredDiphones.size() / (double) numPossibleDiphones);
			out.printf(Locale.US, "overall: %.5f\n", numCoveredDiphonesWithProsody / (double) numPossibleDiphonesWithProsody);

			if (coveredPhones.size() < allPhones.size()) {
				out.println("The following phones are missing: ");
				for (String ph : allPhones) {
					if (!coveredPhones.contains(ph)) {
						out.print(ph + " ");
					}
				}
				out.println();
				out.println("The following phones are present: ");
				for (String ph : coveredPhones) {
					out.print(ph + " ");
				}
				out.println();
			}

			/* print out the diphone statistics */
			out.println("\n");
			out.println("Number of diphones seen      : " + numTokens);
			out.println("Number of different diphones : " + coveredDiphones.size() + " out of a theoretical "
					+ numPossibleDiphones);

			out.close();
			return sw.toString();

		}
	}
}
