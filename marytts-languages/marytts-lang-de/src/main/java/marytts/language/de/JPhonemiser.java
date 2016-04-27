/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.de;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.language.de.phonemiser.Inflection;
import marytts.language.de.phonemiser.PhonemiseDenglish;
import marytts.language.de.phonemiser.Result;
import marytts.modules.synthesis.PAConverter;
import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

/**
 * The phonemiser module -- java implementation.
 *
 * @author Marc Schr&ouml;der
 */

public class JPhonemiser extends marytts.modules.JPhonemiser {
	private Inflection inflection;
	private FSTLookup usEnglishLexicon = null;
	private String logUnknownFileName = null;
	private Map<String, Integer> unknown2Frequency = null;
	private String logEnglishFileName = null;
	private Map<String, Integer> english2Frequency = null;
	private PhonemiseDenglish phonemiseDenglish;

	public JPhonemiser() throws IOException, MaryConfigurationException {
		super("JPhonemiser_de", MaryDataType.PARTSOFSPEECH, MaryDataType.PHONEMES, "de.allophoneset", "de.userdict",
				"de.lexicon", "de.lettertosound");
	}

	public void startup() throws Exception {
		super.startup();
		phonemiseDenglish = new PhonemiseDenglish(this);
		inflection = new Inflection();

		if (MaryProperties.getBoolean("de.phonemiser.logunknown")) {
			String logBasepath = MaryProperties.maryBase() + File.separator + "log" + File.separator;
			File logDir = new File(logBasepath);
			try {
				if (!logDir.isDirectory()) {
					logger.info("Creating log directory " + logDir.getCanonicalPath());
					FileUtils.forceMkdir(logDir);
				}
				logUnknownFileName = MaryProperties.getFilename("de.phonemiser.logunknown.filename", logBasepath
						+ "de_unknown.txt");
				unknown2Frequency = new HashMap<String, Integer>();
				logEnglishFileName = MaryProperties.getFilename("de.phonemiser.logenglish.filename", logBasepath
						+ "de_english-words.txt");
				english2Frequency = new HashMap<String, Integer>();
			} catch (IOException e) {
				logger.info("Could not create log directory " + logDir.getCanonicalPath() + " Logging disabled!", e);
			}
		}
		if (MaryProperties.getBoolean("de.phonemiser.useenglish")) {
			InputStream usLexStream = MaryProperties.getStream("en_US.lexicon");
			if (usLexStream != null) {
				try {
					usEnglishLexicon = new FSTLookup(usLexStream, MaryProperties.getProperty("en_US.lexicon"));
				} catch (Exception e) {
					logger.info("Cannot load English lexicon '" + MaryProperties.getProperty("en_US.lexicon") + "'", e);
				}
			}
		}
	}

	public void shutdown() {
		if (logUnknownFileName != null || logEnglishFileName != null) {
			try {
				/* print unknown words */

				// open file
				PrintWriter logUnknown = new PrintWriter(
						new OutputStreamWriter(new FileOutputStream(logUnknownFileName), "UTF-8"));
				// sort the words
				Set<String> unknownWords = unknown2Frequency.keySet();
				SortedMap<Integer, List<String>> freq2Unknown = new TreeMap<Integer, List<String>>();

				for (String nextUnknown : unknownWords) {
					int nextFreq = unknown2Frequency.get(nextUnknown);
					// logUnknown.println(nextFreq+" "+nextUnknown);
					if (freq2Unknown.containsKey(nextFreq)) {
						List<String> unknowns = freq2Unknown.get(nextFreq);
						unknowns.add(nextUnknown);
					} else {
						List<String> unknowns = new ArrayList<String>();
						unknowns.add(nextUnknown);
						freq2Unknown.put(nextFreq, unknowns);
					}
				}
				// print the words
				for (int nextFreq : freq2Unknown.keySet()) {
					List<String> unknowns = freq2Unknown.get(nextFreq);
					for (int i = 0; i < unknowns.size(); i++) {
						String unknownWord = (String) unknowns.get(i);
						logUnknown.println(nextFreq + " " + unknownWord);
					}

				}
				// close file
				logUnknown.flush();
				logUnknown.close();

				/* print english words */
				// open the file
				PrintWriter logEnglish = new PrintWriter(
						new OutputStreamWriter(new FileOutputStream(logEnglishFileName), "UTF-8"));
				// sort the words
				SortedMap<Integer, List<String>> freq2English = new TreeMap<Integer, List<String>>();
				for (String nextEnglish : english2Frequency.keySet()) {
					int nextFreq = english2Frequency.get(nextEnglish);
					if (freq2English.containsKey(nextFreq)) {
						List<String> englishWords = freq2English.get(nextFreq);
						englishWords.add(nextEnglish);
					} else {
						List<String> englishWords = new ArrayList<String>();
						englishWords.add(nextEnglish);
						freq2English.put(nextFreq, englishWords);
					}

				}
				// print the words
				for (int nextFreq : freq2English.keySet()) {
					List<String> englishWords = freq2English.get(nextFreq);
					for (int i = 0; i < englishWords.size(); i++) {
						logEnglish.println(nextFreq + " " + englishWords.get(i));
					}
				}
				// close file
				logEnglish.flush();
				logEnglish.close();

			} catch (Exception e) {
				logger.info("Error printing log files for english and unknown words", e);
			}
		}
	}

	@Override
	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		inflection.determineEndings(doc);

		NodeIterator it = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.TOKEN);
		Element t = null;
		while ((t = (Element) it.nextNode()) != null) {
			String text;

			// Do not touch tokens for which a transcription is already
			// given (exception: transcription contains a '*' character:
			if (t.hasAttribute("ph") && t.getAttribute("ph").indexOf('*') == -1) {
				continue;
			}
			if (t.hasAttribute("sounds_like"))
				text = t.getAttribute("sounds_like");
			else
				text = MaryDomUtils.tokenText(t);

			// use part-of-speech if available
			String pos = null;
			if (t.hasAttribute("pos")) {
				pos = t.getAttribute("pos");
			}

			boolean isEnglish = false;
			if (t.hasAttribute("xml:lang")
					&& MaryUtils.subsumes(Locale.ENGLISH, MaryUtils.string2locale(t.getAttribute("xml:lang")))) {
				isEnglish = true;
			}

			if (maybePronounceable(text, pos)) {
				// If text consists of several parts (e.g., because that was
				// inserted into the sounds_like attribute), each part
				// is transcribed separately.
				StringBuilder ph = new StringBuilder();
				String g2pMethod = null;
				StringTokenizer st = new StringTokenizer(text, " -");
				while (st.hasMoreTokens()) {
					String graph = st.nextToken();
					StringBuilder helper = new StringBuilder();
					String phon = null;
					if (isEnglish && usEnglishLexicon != null) {
						phon = phonemiseEn(graph);
						if (phon != null)
							helper.append("foreign:en");
					}
					if (phon == null) {
						phon = phonemise(graph, pos, helper);
					}
					// null result should not be processed
					if (phon == null) {
						continue;
					}
					if (ph.length() == 0) { // first part
						// The g2pMethod of the combined beast is
						// the g2pMethod of the first constituant.
						g2pMethod = helper.toString();
						ph.append(phon);
					} else { // following parts
						ph.append(" - ");
						// Reduce primary to secondary stress:
						ph.append(phon.replace('\'', ','));
					}
				}

				if (ph != null && ph.length() > 0) {
					setPh(t, ph.toString());
					t.setAttribute("g2p_method", g2pMethod);
				}
			}
		}
		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setDocument(doc);
		return result;
	}

	/**
	 * Phonemise the word text. This starts with a simple lexicon lookup, followed by some heuristics, and finally applies
	 * letter-to-sound rules if nothing else was successful.
	 * 
	 * @param text
	 *            the textual (graphemic) form of a word.
	 * @param pos
	 *            pos
	 * @param g2pMethod
	 *            This is an awkward way to return a second String parameter via a StringBuilder. If a phonemisation of the text
	 *            is found, this parameter will be filled with the method of phonemisation ("lexicon", ... "rules").
	 * @return a phonemisation of the text if one can be generated, or null if no phonemisation method was successful.
	 */
	@Override
	public String phonemise(String text, String pos, StringBuilder g2pMethod) {
		// First, try a simple userdict and lexicon lookup:
		String result = userdictLookup(text, pos);
		if (result != null) {
			g2pMethod.append("userdict");
			return result;
		}
		result = lexiconLookup(text, pos);
		if (result != null) {
			g2pMethod.append("lexicon");
			return result;
		}
		/**
		 * // Not found? Try a compound "analysis": result = compoundSearch(text);
		 * //logger.debug("Compound here: "+compoundSearch(text)); if (result != null) { g2pMethod.append("compound"); return
		 * result; }
		 **/

		// Lookup attempts failed. Try normalising exotic letters
		// (diacritics on vowels, etc.), look up again:
		String normalised = MaryUtils.normaliseUnicodeLetters(text, Locale.GERMAN);
		if (!normalised.equals(text)) {
			// First, try a simple userdict and lexicon lookup:
			result = userdictLookup(normalised, pos);
			if (result != null) {
				g2pMethod.append("userdict");
				return result;
			}
			result = lexiconLookup(normalised, pos);
			if (result != null) {
				g2pMethod.append("lexicon");
				return result;
			}
			/**
			 * // Not found? Try a compound "analysis": result = compoundSearch(normalised); if (result != null) {
			 * g2pMethod.append("compound"); return result; }
			 **/
		}

		// plain English word must be looked up in English lexicon before phonemiseDenglish starts
		if (usEnglishLexicon != null) {
			String englishTranscription = phonemiseEn(text);
			if (englishTranscription != null) {
				g2pMethod.append("foreign:en");
				logger.debug(text + " is English");
				if (logEnglishFileName != null) {
					String englishText = text.trim();
					if (english2Frequency.containsKey(englishText)) {
						int textFreq = english2Frequency.get(englishText);
						textFreq++;
						english2Frequency.put(englishText, textFreq);
					} else {
						english2Frequency.put(englishText, 1);
					}
				}
				return englishTranscription;
			}
		}
		Result resultingWord = null;
		boolean usedOtherLanguageToPhonemise = false;
		try {
			resultingWord = phonemiseDenglish.processWord(text, usEnglishLexicon != null);
			result = resultingWord.getTranscription();
			usedOtherLanguageToPhonemise = resultingWord.isUsedOtherLanguageToPhonemise();
		} catch (NullPointerException e) {
			logger.debug(String.format("Word is Null: ", e.getMessage()));
		}
		// logger.debug("input for PD: "+text);
		if (result != null) {
			result = allophoneSet.splitAllophoneString(result);
			if (usedOtherLanguageToPhonemise) {
				g2pMethod.append("phonemiseDenglish");
				return result;
			} else {
				g2pMethod.append("compound");
				return result;
			}
		}

		// Cannot find it in the lexicon -- apply letter-to-sound rules
		// to the normalised form

		String phones = ""; // added
		try {
			phones = lts.predictPronunciation(normalised); // added
			result = lts.syllabify(phones);
		} catch (IllegalArgumentException e) {
			logger.error(String.format("Problem with token <%s> [%s]: %s", normalised, phones, e.getMessage()));
		} catch (ClassCastException e) {
			logger.error(String.format("Problem with token <%s> : %s", normalised, e.getMessage())); // added
		}
		if (result != null) {
			if (logUnknownFileName != null) {
				String unknownText = text.trim();
				if (unknown2Frequency.containsKey(unknownText)) {
					int textFreq = unknown2Frequency.get(unknownText);
					textFreq++;
					unknown2Frequency.put(unknownText, textFreq);
				} else {
					unknown2Frequency.put(unknownText, new Integer(1));
				}
			}
			g2pMethod.append("rules");
			return result;
		}
		return null;
	}

	/**
	 * Try to determine an English transcription of the text according to English rules, but using German Sampa.
	 * 
	 * @param text
	 *            Word to transcribe
	 * @return the transcription, or null if none could be determined.
	 */
	public String phonemiseEn(String text) {
		assert usEnglishLexicon != null;
		// We get here only if there is an English lexicon
		String normalisedEn = MaryUtils.normaliseUnicodeLetters(text, Locale.US);
		normalisedEn = normalisedEn.toLowerCase();
		String[] transcriptions = usEnglishLexicon.lookup(normalisedEn);
		assert transcriptions != null; // if nothing is found, an array of length 0 is returned.
		if (transcriptions.length == 0) {
			return null;
		}
		String usSampa = transcriptions[0];

		String deSampa = PAConverter.sampaEnString2sampaDeString(usSampa);
		// logger.debug("converted "+usSampa+" to "+deSampa);
		return deSampa;
	}

	/**
	 * This method tries to decompose a compound. It calls itself recursively.
	 * 
	 * @param text
	 *            the word to be transcribed.
	 * @return the SAMPA transcription of text, or null if none was found.
	 */
	/*
	 * private String compoundSearch(String text) { // Chop off longest possible prefixes and try to look them up // in the
	 * lexicon. Any part must have a minimum length of 3 characters.
	 * //System.out.println("Compound Search is starting with: "+text);
	 * 
	 * for (int i=text.length() - 3; i >= 3; i--) { //-3!!! >= 3!!!
	 * 
	 * String firstPhon = null; String fugePhon = null; String restPhon = null; String prefix = text.substring(0, i);
	 * 
	 * 
	 * firstPhon = userdictLookup(prefix);
	 * 
	 * if (firstPhon == null) firstPhon = lexiconLookup(prefix);
	 * 
	 * if (firstPhon != null) { // found a valid prefix String rest = text.substring(i); logger.debug("Rest is: "+rest);
	 * 
	 * // Is the rest a simple lexical entry? restPhon = userdictLookup(rest);
	 * 
	 * if (restPhon == null) restPhon = lexiconLookup(rest);
	 * 
	 * // Or can the rest be analysed as a compound? if (restPhon == null) restPhon = compoundSearch(rest);
	 * 
	 * // Or does it help if we cut off a Fuge? if (restPhon == null) { String [] helper = fugeSearch(rest); //hier scheint er
	 * nicht mehr reinzugehen //logger.debug("fugeSearch(rest) is: " + fugeSearch(rest)); if (helper != null && helper.length ==
	 * 2) { fugePhon = helper[0]; String rest2 = helper[1]; assert fugePhon != null; assert rest2 != null; restPhon =
	 * userdictLookup(rest2); if (restPhon == null) restPhon = lexiconLookup(rest2); if (restPhon == null) restPhon =
	 * compoundSearch(rest2); } } if (restPhon != null) // success! return firstPhon + (fugePhon != null ? fugePhon : "") + "-" +
	 * restPhon; } } return null; }
	 */

	/**
	 * Try to cut off a Fuge morpheme at the beginning of suffix.
	 * 
	 * @param suffix
	 *            a part of a word with a prefix already removed.
	 * @return a two-item String array. First string is the transcription of the Fuge found, second is the suffix after the Fuge
	 *         was removed. Returns null if no Fuge was found.
	 */
	/*
	 * private String[] fugeSearch(String suffix) { String fugePhon = null; int fugeLength = 0; if (suffix.startsWith("es")) {
	 * fugePhon = "@s"; fugeLength = 2; } else if (suffix.startsWith("en")) { fugePhon = "@n"; fugeLength = 2; } else if
	 * (suffix.startsWith("n")) { fugePhon = "n"; fugeLength = 1; } else if (suffix.startsWith("s")) { fugePhon = "s"; fugeLength
	 * = 1; } else if (suffix.startsWith("e")) { fugePhon = "@"; fugeLength = 1; } if (fugePhon != null) { // found a Fuge
	 * String[] returnValue = new String[2]; returnValue[0] = fugePhon; returnValue[1] = suffix.substring(fugeLength); return
	 * returnValue; } else { return null; } }
	 */

}
