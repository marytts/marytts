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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import marytts.client.MaryClient;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.server.Mary;
import marytts.util.dom.MaryDomUtils;
import marytts.util.http.Address;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

/**
 * Selects sentences from a given set using the greedy algorithm. At each step, the most useful sentence is added to the set of
 * selected sentences. Selection is stopped when the stop criterion is reached. Usefulness of sentences is determined by
 * CoverageDefinition.
 * 
 * @author Anna Hunecke
 *
 */
public class SelectionFunction {

	// maximum number of sentences to select
	private int maxNumSents;
	// the vectors that are selected next
	private byte[] selectedVectors;
	// the filename of the sentence that is selected next
	// private String selectedBasename;
	private int selectedIdSentence;
	// the usefulness of the selected sentence
	private double selectedUsefulness;

	// if true, algorithm stop after maxNumSents are selected
	private boolean stopNumSentences;
	// if true, algorithm stops when maximum coverage of
	// simple diphones is reached
	private boolean stopSimpleDiphones;
	// if true, algorithm stops when maximum coverage of
	// clustered diphones is reached
	private boolean stopClusteredDiphones;
	// if true, algorithm stops when maximum coverage of
	// simple prosody is reached
	private boolean stopSimpleProsody;
	// if true, algorithm stops when maximum coverage of
	// clustered prosody is reached
	private boolean stopClusteredProsody;
	// if true, print information to command line
	private boolean verbose;

	/**
	 * Build a new Selection Function
	 * 
	 */
	public SelectionFunction() {
	}

	/**
	 * Check, if given stop criterion is okay. At the same time, initialise stop criterion as this SelectionFunction's stop
	 * criterion
	 * 
	 * @param stopString
	 *            the stop criterion
	 * @return true if stopString can be parsed, false otherwise
	 */
	public boolean stopIsOkay(String stopString) {
		// set all stop criteria to false
		stopNumSentences = false;
		stopSimpleDiphones = false;
		stopClusteredDiphones = false;
		stopSimpleProsody = false;
		stopClusteredProsody = false;
		// split the stopString
		System.out.println("\nChecking stop criterion:");
		String[] split = stopString.split(" ");
		int i = 0;
		while (split.length > i) {
			if (split[i].startsWith("numSentences")) {
				// criterion is numSentences
				stopNumSentences = true;
				if (split.length > i + 1) {
					// read in the maximum number of sentences
					maxNumSents = Integer.parseInt(split[++i]);
				} else {
					// maximum number of sentences is missing - can not parse
					return false;
				}
				System.out.println("Stop: num sentences " + maxNumSents);
			} else {
				if (split[i].equals("simpleDiphones")) {
					// stop criterion is simpleDiphones
					stopSimpleDiphones = true;
					System.out.println("Stop: simpleDiphones");
				} else {
					if (split[i].equals("clusteredDiphones")) {
						// stop criterion is clusteredDiphones
						stopClusteredDiphones = true;
						System.out.println("Stop: clusteredDiphones");
					} else {
						if (split[i].equals("simpleProsody")) {
							// stop criterion is simpleProsody
							stopSimpleProsody = true;
							System.out.println("Stop: simpleProsody");
						} else {
							if (split[i].equals("clusteredProsody")) {
								// stop criterion is clusteredProsody
								stopClusteredProsody = true;
								System.out.println("Stop: clusteredProsody");
							} else {
								// unknown stop criterion - can not parse
								return false;
							}
						}
					}
				}
			}
			i++;
		}
		// everything allright
		return true;
	}

	/**
	 * Select a set of vectors according to their usefulness which is defined by the coverageDefinition. Stop, when the stop
	 * criterion is reached
	 * 
	 * @param selectedIdSents
	 *            the list of selected id sentences
	 * @param unwantedIdSents
	 *            the list of unwanted id sentences
	 * @param coverageDefinition
	 *            the coverage definition for the feature vectors
	 * @param logFile
	 *            the logFile to document the progress
	 * @param cfProvider
	 *            the list of filenames of the sentences
	 * @param verboseSelect
	 *            if true, get vectors from coverage definition, if false, read vectors from disk
	 * @param wikiToDB
	 *            wikiToDB
	 * @throws Exception
	 *             Exception
	 */
	public void select(Set<Integer> selectedIdSents, Set<Integer> unwantedIdSents, CoverageDefinition coverageDefinition,
			PrintWriter logFile, CoverageFeatureProvider cfProvider, boolean verboseSelect, DBHandler wikiToDB) // throws
																												// IOException
			throws Exception {
		this.verbose = verboseSelect;
		int sentIndex = selectedIdSents.size() + 1;
		selectedVectors = null;
		DateFormat fullDate = new SimpleDateFormat("HH_mm_ss");

		// create the selectedSentences table
		// while the stop criterion is not reached
		while (!stopCriterionIsReached(selectedIdSents, coverageDefinition)) {

			// select the next sentence
			// selectNext(coverageDefinition, logFile, sentIndex, basenameList, vectorArray);
			boolean haveSelected = selectNext(selectedIdSents, unwantedIdSents, coverageDefinition, cfProvider);

			if (haveSelected) {
				assert selectedIdSentence >= 0;
				selectedIdSents.add(selectedIdSentence);

				// print information
				String msg = "Sentence " + sentIndex + " (" + selectedIdSentence + "), score: " + selectedUsefulness;
				if (verbose) {
					System.out.println(msg);
				}
				logFile.println(msg);
				logFile.flush();
			} else {
				// nothing more to select
				// System.out.println("Nothing more to select");
				logFile.println("Nothing more to select");
				break;
			}
			// the selected sentences will be marked as selected=true in the DB
			Date date = new Date();
			System.out.println("  " + sentIndex + " selectedId=" + selectedIdSentence + "  " + fullDate.format(date));
			// Mark the sentence as selected in dbselection
			wikiToDB.setSentenceRecord(selectedIdSentence, "selected", true);
			// Insert selected sentence in table
			wikiToDB.insertSelectedSentence(selectedIdSentence, false);

			// add the selected sentence to the set
			// selectedFilenames.add(selectedBasename);
			// selectedIdSents.add(selectedIdSentence); already done in selectNext
			// update coverageDefinition
			coverageDefinition.updateCover(selectedVectors);
			sentIndex++;
		}
		// print out total number of sentences
		sentIndex--;
		System.out.println("Total number of selected sentences in TABLE: " + wikiToDB.getSelectedSentencesTableName() + " = "
				+ sentIndex);

		int sel[] = wikiToDB.getIdListOfType("dbselection", "selected=true and unwanted=false");

		if (sel != null) {
			// saving sentences in a file
			System.out.println("Saving selected sentences in ./selected.log");
			PrintWriter selectedLog = new PrintWriter(new FileWriter(new File("./selected.log")));

			System.out.println("Saving selected sentences and transcriptions in ./selected_text_transcription.log");
			PrintWriter selected_tra_Log = new PrintWriter(new FileWriter(new File("./selected_text_transcription.log")));

			String str;
			for (int i = 0; i < sel.length; i++) {
				// not sure if we need to make another table???
				// str = wikiToDB.getSentence("selectedSentences", sel[i]);
				str = wikiToDB.getDBSelectionSentence(sel[i]);
				// System.out.println("id=" + sel[i] + str);
				selectedLog.println(sel[i] + " " + str);
				selected_tra_Log.println(sel[i] + " " + str);
				selected_tra_Log.println(sel[i] + " <" + transcribe(str, "it") + ">");
			}
			selectedLog.close();
			selected_tra_Log.close();

			logFile.println("Total number of sentences : " + sentIndex);
		} else
			System.out.println("No selected sentences to save.");

	}

	/*
	 * Utility method for get the Transcription of the selected sentences It makes use of a started builtin MARY TTS
	 */
	static String transcribe(String ptext, String plocale) throws Exception {
		String inputType = "TEXT";
		String outputType = "ALLOPHONES";
		String audioType = null;
		String defaultVoiceName = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Mary.process(ptext, inputType, outputType, plocale, audioType, defaultVoiceName, null, null, null, baos);

		// read into mary data object
		MaryData maryData = new MaryData(MaryDataType.ALLOPHONES, null);
		maryData.readFrom(new ByteArrayInputStream(baos.toByteArray()));
		Document doc = maryData.getDocument();
		assert doc != null : "null sentence";

		TreeWalker phWalker = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE);
		Element ph;
		String lTranscription = "";
		while ((ph = (Element) phWalker.nextNode()) != null) {
			lTranscription = lTranscription + ph.getAttribute("p") + ' ';
		}
		lTranscription = lTranscription.substring(0, lTranscription.length() - 1);
		// System.out.println('<' + lTranscription + '>');
		return lTranscription;
	}

	/**
	 * Select the next sentence
	 * 
	 * @param coverageDefinition
	 *            the coverage definition
	 * @param selectedIdSents
	 *            the index of the next sentence
	 * @param unwantedIdSents
	 *            the list unwanted sentences
	 * @param cfProvider
	 *            cf provider
	 * @throws IOException
	 *             IOException
	 * @return true if a sentence was selected, false otherwise
	 */
	private boolean selectNext(Set<Integer> selectedIdSents, Set<Integer> unwantedIdSents, CoverageDefinition coverageDefinition,
			CoverageFeatureProvider cfProvider) throws IOException {
		// TODO: MS, May 2011 -- I have refactored this code but could not test it. Bad me.

		selectedIdSentence = -1;
		selectedUsefulness = -1;

		// Loop over all sentences in the cfProvider to find the most useful one.
		// For speed reasons, we need to be a bit smart: if coverage features are not in memory,
		// we bulk-load a chunk of them at a time.
		if (cfProvider instanceof InMemoryCFProvider) {
			// already in memory, can loop through all
			determineMostUsefulSentence(selectedIdSents, unwantedIdSents, coverageDefinition, cfProvider);
		} else {
			assert cfProvider instanceof DatabaseCFProvider;
			DatabaseCFProvider dbCfProvider = (DatabaseCFProvider) cfProvider;
			int chunkSize = 100000;
			for (int c = 0, max = dbCfProvider.getNumSentences(); c < max; c += chunkSize) {
				int len = Math.min(chunkSize, max - c);
				CoverageFeatureProvider chunk = dbCfProvider.getFeaturesInMemory(c, len);
				determineMostUsefulSentence(selectedIdSents, unwantedIdSents, coverageDefinition, chunk);
			}
		}
		return selectedIdSentence >= 0;
	}

	/**
	 * @param selectedIdSents
	 *            selectedIdSents
	 * @param unwantedIdSents
	 *            unwantedIdSents
	 * @param coverageDefinition
	 *            coverageDefinition
	 * @param cfProvider
	 *            cfProvider
	 */
	private void determineMostUsefulSentence(Set<Integer> selectedIdSents, Set<Integer> unwantedIdSents,
			CoverageDefinition coverageDefinition, CoverageFeatureProvider cfProvider) {
		for (int l = 0, num = cfProvider.getNumSentences(); l < num; l++) {
			int id = cfProvider.getID(l);
			// skip previously selected or excluded sentences:
			if (selectedIdSents.contains(id) || unwantedIdSents.contains(id)) {
				continue;
			}
			byte[] nextFeatVects = cfProvider.getCoverageFeatures(l);
			// calculate how useful the feature vectors are
			double usefulness = coverageDefinition.usefulnessOfFVs(nextFeatVects);

			if (usefulness > selectedUsefulness) {
				// the current sentence is (currently) the best sentence to add
				selectedIdSentence = id;
				selectedVectors = nextFeatVects;
				selectedUsefulness = usefulness;
			}
			if (usefulness == -1.0) {
				unwantedIdSents.add(id);
				// idSentenceList[i] = -1; // Here the sentence should be marked as unwanted?
				// System.out.println("unwanted id=" + id);
			}
		}
	}

	/**
	 * Determine if the stop criterion is reached
	 * 
	 * @param sentences
	 *            the list of selected sentences
	 * @param covDef
	 *            the coverageDefinition
	 * @return true, if stop criterion is reached, false otherwise
	 */
	private boolean stopCriterionIsReached(Set<Integer> sentences, CoverageDefinition covDef) {

		if (stopNumSentences && sentences.size() >= maxNumSents)
			// if we have the maximum number of sentences
			// stop selecting immediately
			return true;

		// other stop criteria can be combined
		boolean result = false;
		if (stopSimpleDiphones && covDef.reachedMaxSimpleDiphones())
			result = true;
		if (stopSimpleProsody) {
			if (covDef.reachedMaxSimpleProsody()) {
				if (!stopSimpleDiphones && !stopClusteredDiphones) {
					// set result to true only if we do not have to consider
					// the simpleDiphones or clusteredDiphones stop criterion
					result = true;
				}
				// else result remains false/true, depending on what the
				// test result for simpleDiphones/clusteredDiphones was
			} else {
				// set the result to false, no matter what the result for
				// simpleDiphones/clusteredDiphones was
				result = false;
			}
		}
		return result;
	}

	/**
	 * Read the feature vectors from disk for a given filename
	 * 
	 * @param basename
	 *            the file from which to read from
	 * @return the feature vectors from the file
	 * @throws IOException
	 *             IOException
	 */
	private byte[] getNextFeatureVectors1(String basename) throws IOException {
		// open the file
		FileInputStream fis = new FileInputStream(new File(basename));
		// read the first 4 bytes and combine them to get the number
		// of feature vectors
		byte[] vlength = new byte[4];
		fis.read(vlength);
		int numFeatVects = (((vlength[0] & 0xff) << 24) | ((vlength[1] & 0xff) << 16) | ((vlength[2] & 0xff) << 8) | (vlength[3] & 0xff));
		// read the content of the file into a byte array
		byte[] vectorBuf = new byte[4 * numFeatVects];
		fis.read(vectorBuf);
		fis.close();
		// return the feature vectors
		return vectorBuf;
	}

}
