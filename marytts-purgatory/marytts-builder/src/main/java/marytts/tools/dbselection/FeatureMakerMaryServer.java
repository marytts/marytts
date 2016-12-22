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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import marytts.client.http.MaryHttpClient;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.features.FeatureDefinition;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Takes text and converts to features Needs a running Mary server
 * 
 * @author Anna Hunecke
 *
 */
public class FeatureMakerMaryServer {
	// locale
	private static String locale; // using locale we should be able to get the default voice.
	// the Mary Client connected to the server
	protected static MaryHttpClient mary;
	// protected static MaryClient mary;

	// stores result of credibility check for current sentence
	protected static boolean usefulSentence;
	protected static boolean unknownWords;
	protected static boolean strangeSymbols;

	// feature definition, features for selection and their indexes
	protected static FeatureDefinition featDef;
	protected static Vector<String> selectionFeature;
	protected static int[] selectionFeatureIndex;

	// host of the Mary server
	protected static String maryHost;
	// port of the Mary server
	protected static String maryPort;

	// if true, credibility is strict, else crebibility is lax
	protected static boolean strictReliability;

	protected static int numSentences = 0;
	protected static int numUnreliableSentences = 0;

	protected static DBHandler wikiToDB;
	// mySql database
	private static String mysqlHost = null;
	private static String mysqlDB = null;
	private static String mysqlUser = null;
	private static String mysqlPasswd = null;

	public static void main(String[] args) throws Exception {
		boolean test = false;
		String dateStringIni = "";
		String dateStringEnd = "";
		DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
		Date dateIni = new Date();
		dateStringIni = fullDate.format(dateIni);

		/* check the arguments */
		if (!readArgs(args)) {
			printUsage();
			return;
		}

		System.out.println("\nFeatureMaker started...");
		/* Start the Mary client */
		System.setProperty("server.host", maryHost);
		System.setProperty("server.port", maryPort);
		mary = new MaryHttpClient(true);
		// mary = new MaryClient();

		/* Here the DB connection is open */
		wikiToDB = new DBHandler(locale);
		wikiToDB.createDBConnection(mysqlHost, mysqlDB, mysqlUser, mysqlPasswd);

		// check if table exists, if exists already ask user if delete or re-use
		char c;
		boolean result = false, processCleanTextRecords = true;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);

		String table = wikiToDB.getDBselectionTableName();
		if (wikiToDB.tableExist(table)) {
			System.out.print("    TABLE = \"" + table + "\" already exists, should it be deleted (y/n)?");
			try {
				String s = br.readLine();
				if (s.contentEquals("y")) {
					wikiToDB.createDataBaseSelectionTable();
				} else {
					System.out.print("    ADDING sentences TO EXISTING dbselection TABLE \"" + table + "\" (y/n)?");
					s = br.readLine();
					if (s.contentEquals("y"))
						processCleanTextRecords = true;
					else {
						processCleanTextRecords = false;
						System.out
								.print("    please check the \"locale\" prefix of the dbselection TABLE you want to create or add to.");
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		} else {
			System.out.print("    TABLE = \"" + table + "\" does not exist, it will be created.");
			wikiToDB.createDataBaseSelectionTable();
		}

		if (processCleanTextRecords) {
			// Get the set of id for unprocessed records in clean_text
			// this will be useful when the process is stoped and then resumed
			System.out.println("\nGetting list of unprocessed clean_text records from " + wikiToDB.getCleanTextTableName());
			int textId[];
			textId = wikiToDB.getUnprocessedTextIds();
			System.out.println("Number of unprocessed clean_text records to process --> [" + textId.length + "]");
			String text;

			Vector<String> sentenceList; // this will be the list of sentences in each clean_text
			String targetFeatures = "";
			int i, j;

			// get a list separated by spaces of the target features to extract
			for (i = 0; i < selectionFeature.size(); i++)
				targetFeatures += selectionFeature.elementAt(i) + " ";
			/* loop over the text records in clean_text table of wiki */
			// once procesed the clean_text records are marked as processed=true, so here retrieve
			// the next clean_text record untill all are processed.
			System.out.println("Looping over unprocessed clean_text records from wikipedia...");
			System.out.println("TARGETFEATURES to extract: " + targetFeatures);
			System.out.println("Starting time:" + dateStringIni + "\n");

			for (i = 0; i < textId.length; i++) {
				// get next unprocessed text
				text = wikiToDB.getCleanText(textId[i]);
				System.out.println("Processing(" + i + ") text id=" + textId[i] + " text length=" + text.length());
				sentenceList = splitIntoSentences(text, textId[i], test);

				if (sentenceList != null) {
					int index = 0;
					// loop over the sentences
					int numSentencesInText = 0;
					String newSentence;
					byte feas[]; // for directly saving a vector of bytes as BLOB in mysql DB
					for (j = 0; j < sentenceList.size(); j++) {
						newSentence = sentenceList.elementAt(j);
						MaryData d = processSentence(newSentence, textId[i], targetFeatures);
						if (d != null) {
							// get the features of the sentence
							feas = getFeatures(d);
							// Insert in the database the new sentence and its features.
							numSentencesInText++;
							if (!test)
								wikiToDB.insertSentence(newSentence, feas, true, false, false, textId[i]);
							feas = null;
						}
					}// end of loop over list of sentences
					sentenceList.clear();
					sentenceList = null;

					numSentences += numSentencesInText;
					System.out.println("Inserted " + numSentencesInText + " sentences from text id=" + textId[i]
							+ " (Total reliable = " + numSentences + ") \n");
				} // if sentenceList is not null
			} // end of loop over articles
			wikiToDB.closeDBConnection();

			Date dateEnd = new Date();
			dateStringEnd = fullDate.format(dateEnd);
			System.out.println("numSentencesInText;=" + numSentences);
			System.out.println("Start time:" + dateStringIni + "  End time:" + dateStringEnd);
			System.out.println("Done");

		} else {
			wikiToDB.closeDBConnection();
			System.out.println("FeatureMakerMaryServer terminated.");
		}

	}// end of main method

	/**
	 * Print usage of this program
	 *
	 */
	protected static void printUsage() {
		System.out.println("\nUsage: " + "java FeatureMakerMaryServer -locale language -mysqlHost host -mysqlUser user\n"
				+ "                 -mysqlPasswd passwd -mysqlDB wikiDB\n"
				+ "                 [-maryHost localhost -maryPort 59125 -reliability strict]\n"
				+ "                 [-featuresForSelection phone,next_phone,selection_prosody]\n\n"
				+ "  required: This program requires a MARY server running and an already created cleanText table in the DB. \n"
				+ "            The cleanText table can be created with the WikipediaProcess program. \n"
				+ "  default/optional: [-maryHost localhost -maryPort 59125]\n"
				+ "  default/optional: [-featuresForSelection phone,next_phone,selection_prosody] (features separated by ,) \n"
				+ "  optional: [-reliability [strict|lax]]\n\n"
				+ "  -reliability: setting that determines what kind of sentences \n"
				+ "  are regarded as credible. There are two settings: strict and lax. With \n"
				+ "  setting strict, only those sentences that contain words in the lexicon \n"
				+ "  or words that were transcribed by the preprocessor can be selected for the synthesis script; \n"
				+ "  the other sentences as unreliable. With setting lax (default), also those words that \n"
				+ "  are transcribed with the letter to sound component can be selected. \n\n");

	}

	private static void printParameters() {
		System.out.println("FeatureMakerMaryServer parameters:" +

		"\n  -maryHost " + maryHost + "\n  -maryPort " + maryPort + "\n  -locale " + locale + "\n  -mysqlHost " + mysqlHost
				+ "\n  -mysqlUser " + mysqlUser + "\n  -mysqlPasswd " + mysqlPasswd + "\n  -mysqlDB " + mysqlDB);

		if (strictReliability)
			System.out.println("  -reliability strict");
		else
			System.out.println("  -reliability lax");

		System.out.print("  -featuresForselection ");
		int i = 0;
		for (i = 0; i < selectionFeature.size() - 1; i++)
			System.out.print(selectionFeature.elementAt(i) + ",");
		System.out.println(selectionFeature.elementAt(i));
	}

	/**
	 * Read and parse the command line args
	 * 
	 * @param args
	 *            the args
	 * @return true, if successful, false otherwise
	 */
	protected static boolean readArgs(String[] args) {
		// initialise default values
		maryHost = "localhost";
		maryPort = "59125";
		locale = null;
		strictReliability = false; // per default, allow system to select sentences with unknown words
		featDef = null;
		selectionFeature = new Vector<String>();
		selectionFeature.add("phone");
		selectionFeature.add("next_phone");
		selectionFeature.add("selection_prosody");

		// now parse the args
		if (args.length >= 10) {
			for (int i = 0; i < args.length; i++) {

				if (args[i].equals("-locale") && args.length >= i + 1)
					locale = args[++i];

				else if (args[i].equals("-maryHost") && args.length >= i + 1)
					maryHost = args[++i];

				else if (args[i].equals("-maryPort") && args.length >= i + 1)
					maryPort = args[++i];

				else if (args[i].equals("-reliability") && args.length >= i + 1) {
					String credibilitySetting = args[++i];
					if (credibilitySetting.equals("strict"))
						strictReliability = true;
					else {
						if (credibilitySetting.equals("lax"))
							strictReliability = false;
						else
							System.out.println("Unknown argument for credibility " + credibilitySetting);
					}
				}

				else if (args[i].contentEquals("-featuresForSelection") && args.length >= (i + 1)) {
					selectionFeature.clear();
					String selection = args[++i];
					String feas[] = selection.split(",");
					for (int k = 0; k < feas.length; k++)
						selectionFeature.add(feas[k]);
				}

				// mysql database parameters
				else if (args[i].contentEquals("-mysqlHost") && args.length >= (i + 1))
					mysqlHost = args[++i];

				else if (args[i].contentEquals("-mysqlUser") && args.length >= (i + 1))
					mysqlUser = args[++i];

				else if (args[i].contentEquals("-mysqlPasswd") && args.length >= (i + 1))
					mysqlPasswd = args[++i];

				else if (args[i].contentEquals("-mysqlDB") && args.length >= (i + 1))
					mysqlDB = args[++i];

				else { // unknown argument
					System.out.println("\nOption not known: " + args[i]);
					return false;
				}

			}
		} else
			// arguments less than 12
			return false;

		if (mysqlHost == null || mysqlUser == null || mysqlPasswd == null || mysqlDB == null) {
			System.out.println("\nMissing mysql parameters.\n");
			printParameters();
			return false;
		}
		if (locale == null) {
			System.out.println("\nPlease specify locale = wikipedia language.\n");
			printParameters();
			return false;
		}

		printParameters();
		return true;
	}

	/**
	 * Process one sentences from text to target features
	 * 
	 * @param nextSentence
	 *            the sentence
	 * @param textId
	 *            the file containing the sentence
	 * @param feas
	 *            target features names separated by space (ex. "phone next_phone selection_prosody")
	 * @return the result of the processing as MaryData object
	 */
	protected static MaryData processSentence(String nextSentence, int textId, String feas) {
		// do a bit of normalization
		StringBuilder docBuf = null;
		nextSentence = nextSentence.replaceAll("\\\\", "").trim();
		nextSentence = nextSentence.replaceAll("\\s/\\s", "").trim();
		nextSentence = nextSentence.replaceAll("^/\\s", "").trim();
		MaryData d = null;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			// process and dump
			mary.process(nextSentence, "TEXT", "TARGETFEATURES", locale, null, null, null, null, feas, os);

			d = new MaryData(MaryDataType.TARGETFEATURES, null);

			d.readFrom(new ByteArrayInputStream(os.toByteArray()));

			// System.out.println("TARGETFEATURES:\n" + d.getPlainText());

		} catch (Exception e) {
			e.printStackTrace();
			if (d != null) {
				if (d.getPlainText() != null) {
					System.out.println("Error processing sentence " + textId + ": \"" + nextSentence + "\":\n" + d.getPlainText()
							+ "; skipping sentence");
				} else {
					if (d.getDocument() != null) {
						docBuf = new StringBuilder();
						getXMLAsString(d.getDocument(), docBuf);
						System.out.println("Error processing sentence " + ": \"" + nextSentence + "\":\n" + docBuf.toString()
								+ "; skipping sentence");
					} else {
						System.out.println("Error processing sentence " + textId + ": \"" + nextSentence
								+ "\"; skipping sentence");
					}
				}
			} else {
				System.out.println("Error processing sentence from textId=" + textId + ": \"" + nextSentence
						+ "\"; skipping sentence");
			}
			return null;
		} catch (AssertionError ae) {
			ae.printStackTrace();
			System.out.println("Error processing sentence from textId=" + textId + ": \"" + nextSentence
					+ "\"; skipping sentence");
			return null;
		}

		docBuf = null;
		return d;

	}

	/**
	 * Process the given text with the MaryClient from Text to Chunked
	 * 
	 * @param textString
	 *            the text to process
	 * @param id
	 *            id
	 * @return the resulting XML-Document
	 * @throws Exception
	 *             Exception
	 */
	protected static Document phonemiseText(String textString, int id) throws Exception {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			// process and dump
			mary.process(textString, "TEXT", "PHONEMES", locale, null, null, os);

			// read into mary data object
			MaryData maryData = new MaryData(MaryDataType.PHONEMES, null);

			maryData.readFrom(new ByteArrayInputStream(os.toByteArray()));

			return maryData.getDocument();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("PhonemiseText: problem processing text id=" + id);
			return null;
		}

	}

	/**
	 * Process the target features and print them to the given file
	 * 
	 * 
	 * 
	 * @param d
	 *            the target features as Mary Data object
	 * @throws Exception
	 *             Exception
	 * @return feasVector
	 */
	protected static byte[] getFeatures(MaryData d) throws Exception {

		BufferedReader featsDis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(d.getPlainText().getBytes())));

		String line;
		// The first time the feaDef is null, so then load the featureDefinition and the indexes of the features
		// used for selection.
		if (featDef == null) {
			featDef = new FeatureDefinition(featsDis, false);
			// find the indexes of the features for selection
			// here we need to generate a featureDefinition file with the selectionFeatures
			selectionFeatureIndex = new int[selectionFeature.size()];
			PrintWriter pw = new PrintWriter(new FileWriter(new File("./" + locale + "_featureDefinition.txt")));
			System.out.println("\nCreated featureDefinition file:" + "./" + locale + "_featureDefinition.txt");
			pw.write("# This file lists the features to be used for the selection algorithm. \n" + "#\n"
					+ "# Note that the feature definitions must be identical between this file\n"
					+ "# and the feature files used for selection. Use the corresponding file\n"
					+ "# [locale]-targetfeatures.config for feature computation.\n" + "# \n" + "\n"
					+ "ByteValuedFeatureProcessors\n");

			for (int i = 0; i < selectionFeature.size(); i++) {
				selectionFeatureIndex[i] = featDef.getFeatureIndex(selectionFeature.elementAt(i));

				String[] feas = featDef.getPossibleValues(selectionFeatureIndex[i]);
				pw.print(selectionFeature.elementAt(i) + " ");
				for (int j = 0; j < feas.length; j++)
					pw.print(feas[j] + " ");
				pw.println();
			}
			pw.println("ShortValuedFeatureProcessors\nContinuousFeatureProcessors\n");
			pw.close();

		} else { // once the featDef has been load just skip the first part of the featDis
			// read until an empty line occurs
			while ((line = featsDis.readLine()) != null) {
				if (line.equals(""))
					break;
			}
		}

		// loop over the feature vectors
		List<String> featureLines = new ArrayList<String>();
		while ((line = featsDis.readLine()) != null) {
			if (line.equals(""))
				break;
			featureLines.add(line);
		}
		int numLines = featureLines.size();
		// System.out.println("num vectors = "+numLines);
		byte[][] featVects = new byte[numLines][];
		for (int i = 0; i < numLines; i++) {
			line = (String) featureLines.get(i);
			String[] fv = line.split(" ");
			byte[] nextVector = new byte[4];
			for (int j = 0; j < selectionFeature.size(); j++)
				nextVector[j] = featDef.getFeatureValueAsByte(selectionFeature.elementAt(j), fv[selectionFeatureIndex[j]]);
			featVects[i] = nextVector;

		} // end of while-loop over the feature vectors

		// create a byte vector with the reults
		// System.out.println("number of lines=" + numLines);
		byte feasVector[] = new byte[(numLines * 4)];

		for (int n = 0, i = 0; i < featVects.length; i++) {
			byte[] nextFeatVects = featVects[i];
			if (nextFeatVects == null) {
				System.out.println("nextFeatVects are null at index " + i);
			}
			for (int m = 0; m < selectionFeature.size(); m++)
				feasVector[n++] = nextFeatVects[m];
		}

		return feasVector;
	}

	/**
	 * Split the text into separate sentences
	 * 
	 * @param text
	 *            the file
	 * @param id
	 *            id
	 * @param test
	 *            test
	 * @return true, if successful
	 * @throws Exception
	 *             Exception
	 */
	protected static Vector<String> splitIntoSentences(String text, int id, boolean test) throws Exception {

		Vector<String> sentenceList = null;
		StringBuffer sentence;
		// index2sentences = new TreeMap<Integer,String>();

		Document doc = phonemiseText(text, id);
		// if (doc == null) return false;

		if (doc != null) {
			sentenceList = new Vector<String>();
			NodeList sentences = doc.getElementsByTagName("s");

			int sentenceIndex = 1;
			int unrelSentences = 0;
			for (int j = 0; j < sentences.getLength(); j++) {
				Node nextSentence = sentences.item(j);
				// ignore all non-element children
				if (!(nextSentence instanceof Element))
					continue;
				sentence = null;
				// get the tokens
				NodeList tokens = nextSentence.getChildNodes();
				usefulSentence = true;
				unknownWords = false;
				strangeSymbols = false;
				for (int k = 0; k < tokens.getLength(); k++) {
					Node nextToken = tokens.item(k);
					// ignore all non-element children
					if ((nextToken instanceof Element))
						sentence = collectTokens(nextToken, sentence);
				}
				// System.out.println(sentence);
				if (sentence != null) {
					if (usefulSentence) {
						// store sentence in sentence map
						// index2sentences.put(new Integer(sentenceIndex),sentence.toString());
						// check if the sentence is not .
						if (!sentence.toString().contentEquals(".")) {
							sentenceList.add(sentence.toString());
							// System.out.println("reliable sentence=" + sentence.toString() + "\n");
						}
					} else {
						// just print useless sentence to log file
						// System.out.println(filename+"; "+sentenceIndex+": "+sentence
						// +" : is unreliable");

						unrelSentences++;
						/*
						 * if(unknownWords) System.out.println("unknownWords: " + sentence.toString()); if(strangeSymbols)
						 * System.out.println("strangeSymbols: " + sentence.toString());
						 */

						// Here the reason why is unreliable can be added to the DB.
						// for the moment there is just one field reliable=false in this case.
						if (!test)
							wikiToDB.insertSentence(sentence.toString(), null, usefulSentence, unknownWords, strangeSymbols, id);
						else {
							wikiToDB.setSentenceRecord(id, "reliable", false);
							if (unknownWords)
								wikiToDB.setSentenceRecord(id, "unknownWords", true);
							if (strangeSymbols)
								wikiToDB.setSentenceRecord(id, "strangeSymbols", true);

							// System.out.println("unreliable sentence: " + sentence.toString());
						}
					}
					sentenceIndex++;

				} else {
					// ignore
					// System.out.println("NULL SENTENCE!!!");
				}
			}
			numUnreliableSentences += unrelSentences;
			System.out.println("Inserted " + unrelSentences + " sentences from text id=" + id + " (Total unreliable = "
					+ numUnreliableSentences + ")");

		}

		sentence = null;
		return sentenceList;
	}

	/**
	 * Collect the tokens of a sentence
	 * 
	 * @param nextToken
	 *            the Node to start from checkCredibility returns 0 if the sentence is useful 1 if the sentence contains
	 *            unknownWords (so the sentence is not useful) 2 if the sentence contains strangeSymbols (so the sentence is not
	 *            useful)
	 * @param sentence
	 *            sentence
	 * @return sentence
	 */
	protected static StringBuffer collectTokens(Node nextToken, StringBuffer sentence) {
		int credibility = 0;
		String tokenText, word;
		String name = nextToken.getLocalName();
		if (name.equals("t")) {
			if ((credibility = checkReliability((Element) nextToken)) > 0) {
				// memorize that we found unreliable sentence
				usefulSentence = false;
				if (credibility == 1)
					unknownWords = true;
				else if (credibility == 2)
					strangeSymbols = true;
			}
			if (sentence == null) {
				sentence = new StringBuffer();
				// first word of the sentence
				word = MaryDomUtils.tokenText((Element) nextToken);
				sentence.append(word);

			} else {
				String pos = ((Element) nextToken).getAttribute("pos");
				// if (pos.startsWith("$")){
				if (".,'`:#$".indexOf(pos.substring(0, 1)) != -1) {
					// punctuation
					tokenText = MaryDomUtils.tokenText((Element) nextToken);
					// just append without whitespace
					sentence.append(tokenText);
					// System.out.println(sentence);
				} else {
					// normal word, append a whitespace before it
					word = MaryDomUtils.tokenText((Element) nextToken);
					// System.out.println("word=" + word);
					sentence.append(" " + word);
					// System.out.println(sentence);
				}
			}
		} else {
			if (name.equals("mtu")) {
				// get the tokens
				NodeList mtuTokens = nextToken.getChildNodes();
				for (int l = 0; l < mtuTokens.getLength(); l++) {
					Node nextMTUToken = mtuTokens.item(l);
					// ignore all non-element children
					if (!(nextMTUToken instanceof Element))
						continue;
					collectTokens(nextMTUToken, sentence);
				}
			}

		}
		return sentence;
	}

	/**
	 * Phonemise the given document with the help of JPhonemiser
	 * 
	 * g2p_method "contains-unknown-words" or "contains-strange-symbols",
	 * 
	 * @param t
	 *            t
	 * @return 0 if the sentence is useful 1 if the sentence contains unknownWords 2 if the sentence contains strangeSymbols
	 */
	protected static int checkReliability(Element t) {

		// boolean newUsefulSentence = true;
		int newUsefulSentence = 0;

		if (t.hasAttribute("ph")) {
			// we have a transcription
			if (t.hasAttribute("g2p_method")) {
				// check method of transcription
				String method = t.getAttribute("g2p_method");
				if (!method.equals("lexicon") && !method.equals("userdict")) {
					if (strictReliability) {
						// method other than lexicon or userdict -> unreliable
						newUsefulSentence = 1;
						// System.out.println("  unknownwords: method other than lexicon or userdict -> unreliable");
					} else {
						// lax credibility criterion
						if (!method.equals("phonemiseDenglish") && !method.equals("compound") && !method.equals("rules")) { // NEW:
																															// method
																															// is
																															// rules
							// method other than lexicon, userdict, phonemiseDenglish
							// or compound -> unreliable
							newUsefulSentence = 1;
							// System.out.println("   unknownwords: method other than lexicon, userdict, phonemiseDenglish or compound -> unreliable");
						} // else method is phonemiseDenglish or compound -> credible
					}
				}// else method is lexicon or userdict -> credible
			} // else no method -> preprocessed -> credible
		} else {
			// we dont have a transcription
			// if (t.hasAttribute("pos") && !t.getAttribute("pos").startsWith("$")){
			String pos = t.getAttribute("pos");
			if (".,'`:#$".indexOf(pos.substring(0, 1)) == -1) {
				// no transcription given -> unreliable
				newUsefulSentence = 2;
				// System.out.println("  strangeSymbols: no transcription given -> unreliable");
			} // else punctuation -> credible
		}
		return newUsefulSentence;
	}

	/**
	 * Convert the given xml-node and its subnodes to Strings and collect them in the given StringBuilder
	 * 
	 * @param motherNode
	 *            the xml-node
	 * @param ppText
	 *            the StringBuilder
	 */
	protected static void getXMLAsString(Node motherNode, StringBuilder ppText) {
		NodeList children = motherNode.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node nextChild = children.item(i);
			String name = nextChild.getLocalName();
			if (name == null) {
				continue;
			}

			ppText.append("<" + name);
			if (nextChild instanceof Element) {
				if (nextChild.hasAttributes()) {
					NamedNodeMap atts = nextChild.getAttributes();
					for (int j = 0; j < atts.getLength(); j++) {
						String nextAtt = atts.item(j).getNodeName();
						ppText.append(" " + nextAtt + "=\"" + ((Element) nextChild).getAttribute(nextAtt) + "\"");
					}
				}

			}
			if (name.equals("boundary")) {
				ppText.append("/>\n");
				continue;
			}
			ppText.append(">\n");
			if (name.equals("t")) {
				ppText.append(MaryDomUtils.tokenText((Element) nextChild) + "\n</t>\n");
			} else {
				if (nextChild.hasChildNodes()) {
					getXMLAsString(nextChild, ppText);
				}
				ppText.append("</" + name + ">\n");
			}
		}
	}

}
