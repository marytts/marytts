/**
 * Copyright 2005, 2011 DFKI GmbH.
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

package marytts.language.de.phonemiser;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;

import marytts.language.de.JPhonemiser;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * @author steigner
 */
public class PhonemiseDenglish {
	private static Logger logger = MaryUtils.getLogger("PhonemiseDenglish");

	private String[] vowels = { "a", "e", "i", "o", "u" };
	private String[] dentalPlosives = { "t", "d" };
	private Hashtable<String, String> flectionToPhon = null;
	private Hashtable<String, String> prefixLexicon = null;
	private Hashtable<String, String> endingsAndAffixes = null;
	private Hashtable<String, String> terminalVoicings = null;
	private HashSet<String> endingSet = null;
	private int maxEndingLength = 0;
	private int maxPrefixLength = 0;

	private Locale locale;
	private JPhonemiser jphon = null;// building an instance of JPhonemiser for using
	// lexiconLookup method and transducers from this class

	private MorphologyReader mr = new MorphologyReader();

	public PhonemiseDenglish(JPhonemiser jphon) throws Exception {

		this.jphon = jphon;
		String classpathPrefix = "/marytts/language/de/lexicon/denglish/";
		this.flectionToPhon = mr.loadInputModel(getClass().getResourceAsStream(classpathPrefix + "flectionsPhonLex.xml"));
		this.prefixLexicon = mr.loadInputModel(getClass().getResourceAsStream(classpathPrefix + "PrefixLex.xml"));
		for (Iterator<String> prefixIt = prefixLexicon.keySet().iterator(); prefixIt.hasNext();) {
			String prefix = prefixIt.next();
			if (prefix.length() > this.maxPrefixLength)
				this.maxPrefixLength = prefix.length();
		}
		this.endingsAndAffixes = mr.loadInputModel(getClass().getResourceAsStream(classpathPrefix + "germanEndings.xml"));
		this.terminalVoicings = mr.loadInputModel(getClass().getResourceAsStream(
				classpathPrefix + "terminal_voicing_for_german.xml"));
		String[] endingList = getEndingsAndAffixes("flections");// list of flection endings of a specific language
		if (endingList != null) {
			this.endingSet = new HashSet<String>(50);//
			for (int j = 0; j < endingList.length; j++) {
				if (endingList[j].length() > this.maxEndingLength) {
					this.maxEndingLength = endingList[j].length();
				}
				this.endingSet.add(endingList[j]);
			}
		}
	}

	/**
	 * Method that is called from JPhonemiser - Managing all the processing
	 * 
	 * @param toBePhonemised
	 *            The input word
	 * @param allowOtherLanguage
	 *            allowOtherLanguage
	 * @return Transcription of input word if one can be built - null otherwise
	 */
	public Result processWord(String toBePhonemised, boolean allowOtherLanguage) {
		// cleanAllInstanceVariables();
		Word currentWord = new Word(toBePhonemised);
		// Vector result = new Vector();
		String transcription = null;
		Result currentResult = new Result();
		long time1 = System.currentTimeMillis();
		if (currentWord.getToBePhonemised().equals("")) {
			logger.debug("Empty String!");
			// return null;
			return currentResult;
		}
		// word or item must at least have length 3 to be a denglish item
		if (currentWord.getToBePhonemised().length() <= 2) {
			logger.debug("Input to short to be a deng item");
			logger.debug("Now using letter to sound rules");
			// return null;
			return currentResult;
		}
		// try cutting off inflection ending and/or prefix:
		transcription = processFlection(currentWord, currentResult, allowOtherLanguage);
		if (transcription != null) {
			long time2 = System.currentTimeMillis();
			long end = time2 - time1;
			logger.debug("Processing took: " + end + " ms");
			// return transcription;
			currentResult.setTranscription(transcription);
			// System.out.println("1) var is "+currentResult.isUsedOtherLanguageToPhonemise());
			return currentResult;
		}
		// try compound analysis, first without, then with other language:
		transcription = compoundAnalysis(currentWord, currentResult, false);
		if (transcription != null) {
			long time3 = System.currentTimeMillis();
			long end = time3 - time1;
			logger.debug("Processing took: " + end + " ms");
			// return transcription;
			currentResult.setTranscription(transcription);
			// System.out.println("2) var is "+currentResult.isUsedOtherLanguageToPhonemise());
			return currentResult;
		}
		transcription = compoundAnalysis(currentWord, currentResult, allowOtherLanguage);
		if (transcription != null) {
			long time3 = System.currentTimeMillis();
			long end = time3 - time1;
			logger.debug("Processing took: " + end + " ms");
			// return transcription;
			currentResult.setTranscription(transcription);
			// System.out.println("2) var is "+currentResult.isUsedOtherLanguageToPhonemise());
			return currentResult;
		}
		// return null;
		return currentResult;
	}

	/**
	 * Try to process the input word, as it stands, or by cutting off prefixes or inflectional suffixes.
	 * 
	 * @param toBePhonemised
	 *            the input word
	 * @param allowOtherLanguage
	 *            allowOtherLanguage
	 * @return the transcription of the word, or null if the word could not be transcribed
	 */
	private String processFlection(Word word, Result currentResult, boolean allowOtherLanguage) {
		String toBePhonemised = word.getToBePhonemised();
		logger.debug("processFlection is starting with: " + toBePhonemised);
		// First of all, make sure there is no userdict/lexicon entry:
		String transcription = jphon.userdictLookup(toBePhonemised, null);
		if (transcription != null) {
			return transcription;
		}
		transcription = jphon.lexiconLookup(toBePhonemised, null);
		if (transcription != null) {
			return transcription;
		}
		// Try to process by cutting off endings only, without cutting off prefix:
		if (allowOtherLanguage) {
			transcription = processFlectionEnding(word, currentResult);
		}
		if (transcription != null) {
			return transcription;
		}
		// try removing prefix:
		// Enforce at least 3 characters in the stem (the part of the word that comes after the prefix):
		int maxPrefLen = Math.min(this.maxPrefixLength, word.getToBePhonemised().length() - 3);
		for (int i = maxPrefLen; i > 0; i--) {
			String prefix = word.getToBePhonemised().substring(0, i).toLowerCase();
			String prefixPhon = prefixLexiconLookup(prefix);
			if (prefixPhon != null) {
				logger.debug("Prefix found: " + prefix + " [" + prefixPhon + "]");
				Word partialWord = new Word(word.getToBePhonemised().substring(i));
				// recursively call this method, i.e. allow multiple prefixes:
				String restTranscription = processFlection(partialWord, currentResult, allowOtherLanguage);
				if (restTranscription != null) { // yes, found valid analysis
					if (prefixPhon.indexOf("'") != -1) {
						restTranscription = restTranscription.replaceAll("'", "");
					}
					transcription = prefixPhon + "-" + restTranscription;
					return transcription;
				}
			}
		}
		return null;
	}

	/**
	 * Try to process the input word as a verbal or adjective flection.
	 * 
	 * @param word
	 *            the input word
	 * @param currentResult
	 *            currentResult
	 * @return the transcription of the word
	 */
	private String processFlectionEnding(Word word, Result currentResult) {
		String toBePhonemised = word.getToBePhonemised();
		logger.debug("processFlectionEnding is starting with: " + toBePhonemised);
		String wordMinusFlectionEnding = null;
		String flectionEnding = null;
		String result = null;
		// separateFlectionEndings returns null if no valid flection ending can be found
		// otherwise it returns an array containing the word without flection and the flection
		String[] wordPlusEnding = separateFlectionEndings(toBePhonemised, this.maxEndingLength);
		if (wordPlusEnding != null) {
			wordMinusFlectionEnding = wordPlusEnding[0];
			flectionEnding = wordPlusEnding[1];
			word = transformWordToEnBaseForm(wordMinusFlectionEnding, flectionEnding, word);// language-dependent
			if (word.getOtherLanguageBaseForm() != null) {
				word.setFlectionEnding(flectionEnding);
				result = transcribeFlection(word, currentResult);// language-dependent
			} else {// case of upgedatete
					// start separateFlectionEndings() with a smaller number of ending chars
				int currentEndingLength = flectionEnding.length();
				wordPlusEnding = separateFlectionEndings(toBePhonemised, currentEndingLength - 1);
				if (wordPlusEnding != null) {
					wordMinusFlectionEnding = wordPlusEnding[0];
					flectionEnding = wordPlusEnding[1];
					word = transformWordToEnBaseForm(wordMinusFlectionEnding, flectionEnding, word);// language-dependent
					if (word.getOtherLanguageBaseForm() != null) {
						word.setFlectionEnding(flectionEnding);
						result = transcribeFlection(word, currentResult);// language-dependent
					} else {
						currentEndingLength = flectionEnding.length();
						wordPlusEnding = separateFlectionEndings(toBePhonemised, currentEndingLength - 1);
						if (wordPlusEnding != null) {
							wordMinusFlectionEnding = wordPlusEnding[0];
							flectionEnding = wordPlusEnding[1];
							word = transformWordToEnBaseForm(wordMinusFlectionEnding, flectionEnding, word);// language-dependent
							if (word.getOtherLanguageBaseForm() != null) {
								word.setFlectionEnding(flectionEnding);
								result = transcribeFlection(word, currentResult);// language-dependent
							}
						}
					}
				} else {// array is null
					// we have sth. that is already without flection ending like 'check'
					word = transformWordToEnBaseForm(toBePhonemised, null, word);// language-dependent
					if (word.getOtherLanguageBaseForm() != null) {
						result = transcribeFlection(word, currentResult);// language-dependent
					} else {
						logger.debug("Unable to transcribe flection. Returning null.");
					}
				}
			}
		}
		// System.out.println("var is in processFlection: "+currentResult.isUsedOtherLanguageToPhonemise());
		logger.debug("processFlection: " + result);
		return result;
	}

	/**
	 * Analyses parts of input word for affixes, compounds etc.
	 * 
	 * @param word
	 *            the input word
	 * @param currentResult
	 *            currentResult
	 * @param allowOtherLanguage
	 *            whether to allow component words from other language in compound analysis
	 * @return If a transcription for the input can be found, then it is returned. Otherwise returns null.
	 */
	private String compoundAnalysis(Word word, Result currentResult, boolean allowOtherLanguage) {
		// Chop off longest possible prefixes and try to look them up
		// in the lexicon. Any part must have a minimum length of 3 -> 2!! characters.
		logger.debug("compoundAnalysis is starting with: " + word.getToBePhonemised());

		for (int i = word.getToBePhonemised().length() - 3; i >= 3; i--) { // -3!!! >= 3!!!

			String firstPhon = null;
			String fugePhon = null;
			String restPhon = null;
			String[] genitiveAccusativeAndPluralEndings = getEndingsAndAffixes("noun_genitive_accusative_and_plural_endings");// should
																																// be
																																// 's'
																																// and
																																// 'n'
																																// for
																																// german
			String prefix = word.getToBePhonemised().substring(0, i);
			logger.debug("Pre: " + prefix);

			firstPhon = jphon.userdictLookup(prefix, null);
			if (firstPhon == null) {
				firstPhon = jphon.lexiconLookup(prefix, null);
			}
			if (firstPhon == null && allowOtherLanguage) {
				firstPhon = jphon.phonemiseEn(prefix);
				if (firstPhon != null) {
					currentResult.setUsedOtherLanguageToPhonemise(true);
				}
			}
			if (firstPhon != null) { // found a valid prefix
				// TODO: shouldn't this call processFlection()?
				String rest = word.getToBePhonemised().substring(i);
				logger.debug("Rest is: " + rest);
				// Is the rest a simple lexical entry?
				// restPhon = germanLexiconLookup(rest);
				restPhon = prefixLexiconLookup(rest);
				logger.debug("RestPhon: " + restPhon);
				if (restPhon == null) {
					restPhon = jphon.userdictLookup(rest, null);
				}
				if (restPhon == null) {
					restPhon = jphon.lexiconLookup(rest, null);
				}
				if (restPhon == null && allowOtherLanguage) {
					restPhon = jphon.phonemiseEn(rest);
					if (restPhon != null) {
						currentResult.setUsedOtherLanguageToPhonemise(true);
					}
				}
				if (restPhon == null) {
					for (int j = 0; j < genitiveAccusativeAndPluralEndings.length; j++) {
						if (rest.endsWith(genitiveAccusativeAndPluralEndings[j])) {
							logger.debug("rest ends with: " + genitiveAccusativeAndPluralEndings[j]);
							String restWithoutLast = rest.substring(0, rest.length() - 1);
							String restPhonDe = jphon.userdictLookup(restWithoutLast, null);
							if (restPhonDe == null)
								restPhonDe = jphon.lexiconLookup(restWithoutLast, null);
							String genitiveAndPluralEndingTrans = endingTranscriptionLookup(genitiveAccusativeAndPluralEndings[j]);
							if (restPhonDe != null) {
								restPhon = restPhonDe + genitiveAndPluralEndingTrans;
							} else if (allowOtherLanguage) {
								String restPhonEn = jphon.phonemiseEn(rest.substring(0, rest.length() - 1));
								if (restPhonEn != null) {
									currentResult.setUsedOtherLanguageToPhonemise(true);
									restPhon = restPhonEn + genitiveAndPluralEndingTrans;
								}
							}
						}
						if (restPhon != null)
							break;
					}
				}

				// Or does it help if we cut off a Fuge?
				if (restPhon == null) {
					String[] helper = fugeSearch(rest);
					if (helper != null && helper.length == 2) {
						fugePhon = helper[0];
						String rest2 = helper[1];
						restPhon = jphon.userdictLookup(rest2, null);
						if (restPhon == null) {
							restPhon = jphon.lexiconLookup(rest2, null);
						}
						if (restPhon == null && allowOtherLanguage) {
							restPhon = jphon.phonemiseEn(rest2);
							if (restPhon != null) {
								currentResult.setUsedOtherLanguageToPhonemise(true);
							}
						}
						if (restPhon == null)
							restPhon = compoundAnalysis(new Word(rest2), currentResult, allowOtherLanguage);
					}
				}
				// Maybe rest is a flection
				if (restPhon == null) {
					// System.out.println("1) new word is : "+rest+". processFlection is called from here. var is : "+currentResult.isUsedOtherLanguageToPhonemise());

					restPhon = processFlection(new Word(rest), currentResult, allowOtherLanguage);
					// System.out.println("2) new word was : "+rest+". processFlection is called from here. var is : "+currentResult.isUsedOtherLanguageToPhonemise());
				}
				// Or can the rest be analysed as a compound?
				if (restPhon == null)
					restPhon = compoundAnalysis(new Word(rest), currentResult, allowOtherLanguage);

				if (restPhon != null) {
					// In restPhon, delete stress signs:
					restPhon = restPhon.replaceAll("'", "");
					return firstPhon + (fugePhon != null ? fugePhon : "") + "-" + restPhon;
				}
			}
		}
		return null;
	}

	/**
	 * Try to cut off a Fuge morpheme at the beginning of suffix.
	 * 
	 * @param suffix
	 *            a part of a word with a prefix already removed.
	 * @return a two-item String array. First string is the trannscnscription of the Fuge found, second is the suffix after the
	 *         Fuge was removed. Returns null if no Fuge was found.
	 */
	private String[] fugeSearch(String suffix) {
		String fugePhon = null;
		int fugeLength = 0;
		String[] validFuges = getEndingsAndAffixes("compound_fuge");
		for (int j = 0; j < validFuges.length; j++) {
			if (suffix.startsWith(validFuges[j])) {
				fugePhon = endingTranscriptionLookup(validFuges[j]);
				fugeLength = validFuges[j].length();
				break;
			}
		}
		if (fugePhon != null) { // found a Fuge
			String[] returnValue = new String[2];
			returnValue[0] = fugePhon;
			returnValue[1] = suffix.substring(fugeLength);
			return returnValue;
		} else {
			return null;
		}
	}

	/**
	 * Separates flection ending from input word.
	 * 
	 * @param toBePhonemised
	 *            the input word
	 * @param endingLength
	 *            endings from language specific ending list
	 * @return when valid flection ending is found, returns a string array of two elements (first is stem, second is ending);
	 *         else, returns null.
	 */
	private String[] separateFlectionEndings(String toBePhonemised, int endingLength) {
		String wordMinusFlectionEnding = null;
		String flectionEnding = knowEnding(toBePhonemised, endingLength);
		if (flectionEnding != null) {
			String[] wordPlusEnding = new String[2];
			wordMinusFlectionEnding = toBePhonemised.substring(0, toBePhonemised.length() - flectionEnding.length());
			wordPlusEnding[0] = wordMinusFlectionEnding;
			wordPlusEnding[1] = flectionEnding;
			return wordPlusEnding;
		} else {
			return null;
		}
	}

	/**
	 * Try to find baseform of otherLanguageWord (i.e. english infinitive in denglish word)
	 * 
	 * @param wordMinusFlectionEnding
	 *            wordMinusFlectionEnding
	 * @param flectionEnding
	 *            flectionEnding
	 * @param word
	 *            word
	 * @return word
	 */
	private Word transformWordToEnBaseForm(String wordMinusFlectionEnding, String flectionEnding, Word word) {
		logger.debug("getEnBaseForm is starting with...: " + wordMinusFlectionEnding);
		String[] participleBaseLong = getEndingsAndAffixes("participle_base_long");// 'et' for german
		String[] participleBaseShort = getEndingsAndAffixes("participle_base_short");// 't' for german
		// String[] flectionFuge = getEndingsAndAffixes("flection_fuge");//should be 'e' for german
		String wordMinusFlectionEndingPenultimateChar = null;
		String wordMinusFlectionEndingUltimateChar = null;
		if (wordMinusFlectionEnding.length() > 2) {
			wordMinusFlectionEndingPenultimateChar = wordMinusFlectionEnding.substring(wordMinusFlectionEnding.length() - 2,
					wordMinusFlectionEnding.length() - 1);
		}
		if (wordMinusFlectionEnding.length() > 1) {
			wordMinusFlectionEndingUltimateChar = wordMinusFlectionEnding.substring(wordMinusFlectionEnding.length() - 1,
					wordMinusFlectionEnding.length());
		}
		String wordMinusFlectionEndingLastTwo = wordMinusFlectionEndingPenultimateChar + wordMinusFlectionEndingUltimateChar;
		if (wordMinusFlectionEnding.length() > 3) {
			if (knowEnBaseForm(wordMinusFlectionEnding)) {// item without ending is already en base form like >boot<
				if (flectionEnding != null) {
					if (isLongParticipleBaseEnding(flectionEnding, participleBaseLong)
							|| isShortParticipleBaseEnding(flectionEnding, participleBaseShort)) {// 'boot >et< or 'scroll>t<'
						word.setOtherLanguageBaseForm(wordMinusFlectionEnding);
						word.setCouldBeParticiple(true);
						word.setCouldBeParticipleInBaseForm(true);
						// next is special case for words like dat>e<>te<
					} else if (endsWithVowel(wordMinusFlectionEnding)) { // 'te'
						word.setOtherLanguageBaseForm(wordMinusFlectionEnding);
						word.setWordMinusFlectionEndsWithVowel(true);
						word.setCouldBeParticiple(true);
					} else {// downloaden
						word.setOtherLanguageBaseForm(wordMinusFlectionEnding);
					}
				} else {// scroll
					word.setOtherLanguageBaseForm(wordMinusFlectionEnding);
					logger.debug("wordMinusFlectionEnding is already enBaseForm");
				}
			}

			// (up | ge) | date >t< (em) (j = 1)
			// (ge) | boot >et< (em) (j =2)
			// scroll >t< (en) (j=1)
			// scan >nt< (en) (j=2)
			if (word.getOtherLanguageBaseForm() == null) {
				for (int j = 1; j < 3; j++) {// chop off 1-3 chars from end of word
					logger.debug("new(2a): " + wordMinusFlectionEnding.substring(0, wordMinusFlectionEnding.length() - j));
					if (knowEnBaseForm(wordMinusFlectionEnding.substring(0, wordMinusFlectionEnding.length() - j))) {

						if (isLongParticipleBaseEnding(wordMinusFlectionEndingLastTwo, participleBaseLong) // 'et'
								|| isShortParticipleBaseEnding(wordMinusFlectionEndingUltimateChar, participleBaseShort)
								&& !(isShortParticipleBaseEnding(wordMinusFlectionEndingUltimateChar, participleBaseShort) && // to
																																// force
																																// that
																																// sth
																																// like
																																// 'cha>tt<en'
																																// is
																																// correctly
								isShortParticipleBaseEnding(wordMinusFlectionEndingPenultimateChar, participleBaseShort))) {// processed
																															// in
																															// geminate
																															// clause
							word.setOtherLanguageBaseForm(wordMinusFlectionEnding.substring(0, wordMinusFlectionEnding.length()
									- j));
							word.setCouldBeParticiple(true);
							word.setCutOffCharacter(true);
							logger.debug("new(2a)");
						}
					}
					if (word.getOtherLanguageBaseForm() != null)
						break;
				}
			}

			if (word.getOtherLanguageBaseForm() == null) {
				// check for geminates -> scannen
				logger.debug("in geminate clause: " + wordMinusFlectionEnding);
				if (wordMinusFlectionEndingUltimateChar.equals(wordMinusFlectionEndingPenultimateChar)) {// sca>nn<
					if (knowEnBaseForm(wordMinusFlectionEnding.substring(0, wordMinusFlectionEnding.length() - 1))) {
						word.setOtherLanguageBaseForm(wordMinusFlectionEnding.substring(0, wordMinusFlectionEnding.length() - 1));
						logger.debug("geminate.......");
					}
				}
			}

			if (word.getOtherLanguageBaseForm() == null) {
				// try to test if it is a gerund -> updatend
				word.setIsVerbalGerund(checkIfGerund(wordMinusFlectionEnding));
				if (word.getIsVerbalGerund()) {// we have a gerund
					word.setOtherLanguageBaseForm(transformWordToEnBaseFormGerund(wordMinusFlectionEnding));
				}
			}
		}
		logger.debug("finally enBaseForm: " + word.getOtherLanguageBaseForm());
		return word;
	}

	/**
	 * 
	 * @param word
	 *            word
	 * @return enBaseForm
	 */
	private String transformWordToEnBaseFormGerund(String word) {
		logger.debug("getBaseFormGerund called with: " + word);
		String enBaseForm = null;
		String[] flectionFuge = getEndingsAndAffixes("flection_fuge");//
		logger.debug("found gerund..........");
		for (int j = 0; j < flectionFuge.length; j++) {
			if (knowEnBaseForm(word.substring(0, word.length() - 3) + flectionFuge[j])) {// updat>e<nd
				enBaseForm = word.substring(0, word.length() - 3) + flectionFuge[j];// like updat >e< nd
				logger.debug("gerund case 3");
			}
			if (enBaseForm != null)
				break;
		}
		if (enBaseForm == null) {
			if (knowEnBaseForm(word.substring(0, word.length() - 3))) {// download>end<
				enBaseForm = word.substring(0, word.length() - 3);// item without 'end' is base
				logger.debug("gerund case 1");
			} else if (knowEnBaseForm(word.substring(0, word.length() - 4))
					&& word.charAt(word.length() - 4) == word.charAt(word.length() - 5)) {// scan>n< end
				enBaseForm = word.substring(0, word.length() - 4);
				logger.debug("gerund case 2");
			}
		}
		return enBaseForm;
	}

	/**
	 * Building the transcription and syllabification of a flection
	 * 
	 * @param currentResult
	 *            currentResult
	 * @param word
	 *            : the English infinitive as found in English lexicon
	 * @return transcription of complete input word
	 */
	private String transcribeFlection(Word word, Result currentResult) {

		String result = null;
		String otherLanguageTranscription = null;
		String endingTranscription = null;
		String gerundEndingTrans = null;
		String participleBaseShortEndingTrans = null;
		String flectionFugeTrans = null;
		otherLanguageTranscription = jphon.phonemiseEn(word.getOtherLanguageBaseForm());
		if (otherLanguageTranscription != null) {
			// System.out.println("var should be true");
			currentResult.setUsedOtherLanguageToPhonemise(true);
			for (int j = 0; j < this.dentalPlosives.length; j++) {
				if (otherLanguageTranscription.endsWith(this.dentalPlosives[j])) {
					word.setExtraSyll(true);
					logger.debug("extraSyll true");
				}
			}
			// System.out.println("var is in transcribeFlection: "+currentResult.isUsedOtherLanguageToPhonemise());
			// for cases like 'scrollet' where 'et' is flection ending and NOT ending of
			// participleBaseForm; otherwise 'scrollet' would sound like 'scrollt'
			String[] participleBaseLongEndings = getEndingsAndAffixes("participle_base_long");
			for (int j = 0; j < participleBaseLongEndings.length; j++) {
				if (word.getFlectionEnding() != null && word.getFlectionEnding().equals(participleBaseLongEndings[j])
						&& !(word.getCutOffCharacter())) {// 'et'
					word.setExtraSyll(true);
				}
			}
			String[] gerundEndings = getEndingsAndAffixes("gerund_ending");// should be 'end' -> bootend
			// String gerundEndingTrans = endingTranscriptionLookup(gerundEnding);//should be '@nt'
			for (int j = 0; j < gerundEndings.length; j++) {
				if (endingTranscriptionLookup(gerundEndings[j]) != null) {
					gerundEndingTrans = endingTranscriptionLookup(gerundEndings[j]);
				}
			}
			String[] participleBaseShortEndings = getEndingsAndAffixes("participle_base_short");
			// If the participle ends with 'ed' or 'et' doesn't matter -> you get the same transcription
			// String participleBaseEndingTrans = endingTranscriptionLookup(participleBaseEnding);//gives you 't'
			for (int j = 0; j < participleBaseShortEndings.length; j++) {
				if (endingTranscriptionLookup(participleBaseShortEndings[j]) != null) {
					participleBaseShortEndingTrans = endingTranscriptionLookup(participleBaseShortEndings[j]);// gives you 't'
				}
			}
			String[] flectionFuge = getEndingsAndAffixes("flection_fuge");// gives you 'e'
			for (int j = 0; j < flectionFuge.length; j++) {
				if (endingTranscriptionLookup(flectionFuge[j]) != null) {
					flectionFugeTrans = endingTranscriptionLookup(flectionFuge[j]);
				}
			}

			endingTranscription = endingTranscriptionLookup(word.getFlectionEnding());
			String newEnTranscription = rebuildTrans(otherLanguageTranscription);
			String newGerundEndingTrans = rebuildTrans(gerundEndingTrans);// should then be '@n-t'
			String voicedNewGerundEndingTrans = voiceFinal(newGerundEndingTrans);// should be '@n-d'
			// String voicedGerundEndingTrans = voiceFinal(gerundEndingTrans); //should be '@nd'
			logger.debug("enTrans: " + otherLanguageTranscription);

			if (word.getFlectionEnding() != null) {
				if (endingTranscriptionLookup(word.getFlectionEnding()) != null) {
					// special rule in case of enBaseForm's last char equals valid flection ending i.e. 't'
					// in this case give us back the enBaseForm aka enInfinitive
					// testing for participle because of date>te< enBaseForm ends with found ending
					if (otherLanguageTranscription.endsWith(word.getFlectionEnding()) && !(word.getIsVerbalGerund())
							&& !(word.getCouldBeParticiple())) {
						result = otherLanguageTranscription;
						logger.debug("(0)");
					} else {
						if (word.getCouldBeParticiple() && isShortSuperlative(word.getFlectionEnding()) && word.getExtraSyll()) {// i.e.
																																	// downgeloadetsten
							result = newEnTranscription + flectionFugeTrans + participleBaseShortEndingTrans
									+ endingTranscription;
							logger.debug("(1)");
						} else if (word.getCouldBeParticiple() && word.getCouldBeParticipleInBaseForm() && word.getExtraSyll()) {// scrollet
																																	// or
																																	// downloadet
							result = newEnTranscription + flectionFugeTrans + participleBaseShortEndingTrans;
							logger.debug("(2)");
						} else if (word.getCouldBeParticiple() && word.getExtraSyll() && word.getWordMinusFlectionEndsWithVowel()) {
							result = newEnTranscription + flectionFugeTrans + "-" + endingTranscription;
							logger.debug("(3)");
						} else if (word.getCouldBeParticiple() && word.getExtraSyll()) {// i.e. downgeloadetere
							result = newEnTranscription + flectionFugeTrans + "-" + participleBaseShortEndingTrans
									+ endingTranscription;
							logger.debug("(4)");
						} else if (word.getCouldBeParticiple() && isShortSuperlative(word.getFlectionEnding())) {// i.e.
																													// gescrolltstem
							result = otherLanguageTranscription + participleBaseShortEndingTrans + endingTranscription;
							logger.debug("(5)");
						} else if (word.getCouldBeParticiple() && word.getCouldBeParticipleInBaseForm()) {
							result = otherLanguageTranscription + participleBaseShortEndingTrans;
							logger.debug("(6)");
						} else if (word.getCouldBeParticiple()) {// i.e. gescrolltestem
							result = otherLanguageTranscription + "-" + participleBaseShortEndingTrans + endingTranscription;
							logger.debug("(7)");
						} else {
							if (word.getIsVerbalGerund()) {
								logger.debug("isVerbalGerund");
								if (isShortSuperlative(word.getFlectionEnding())) {
									result = newEnTranscription + gerundEndingTrans + endingTranscription;
								} else {
									result = newEnTranscription + voicedNewGerundEndingTrans + endingTranscription;
								}
							} else {
								if (isShortSuperlative(word.getFlectionEnding())) {
									result = otherLanguageTranscription + endingTranscription;
								} else {// no Gerund, no superlative but maybe something like 'scannst'
									if (word.getExtraSyll()) {// means: word ends on 't' or 'd'
										logger.debug("extraSyll is true here...");
										result = newEnTranscription + endingTranscription;
									} else {// means: word ends on something else
										if (endingContainsVowel(word.getFlectionEnding())
												&& (!(endingBeginsWithVowel(word.getFlectionEnding())))) {
											result = otherLanguageTranscription + "-" + endingTranscription;
										} else {
											if (endingContainsVowel(word.getFlectionEnding())
													&& endingBeginsWithVowel(word.getFlectionEnding())) {
												result = newEnTranscription + endingTranscription;
											} else {
												result = otherLanguageTranscription + endingTranscription;
											}
										}
									}
								}
							}
						}
					}
				}
			} else {// flection ending is null: two possibilities: en-Word like boot or ger gerund like bootend
				if (word.getIsVerbalGerund()) {
					result = newEnTranscription + gerundEndingTrans;
					logger.debug("(((1)))");
				} else {// scann, date
					result = otherLanguageTranscription;
					logger.debug("(((2)))");
				}
			}
		}
		return result;
	}

	/**
	 * Checks if input word has valid ending from ending list.
	 * 
	 * @param toBePhonemised
	 *            The input word
	 * @param endingLength
	 *            endingLength
	 * @return Flection ending if one can be found, null otherwise
	 */
	private String knowEnding(String toBePhonemised, int endingLength) {
		logger.debug("in knowEnding: " + toBePhonemised);
		String currentEnding = null;
		String foundEnding = null;
		int wordLength = toBePhonemised.length();
		for (int j = endingLength; j > 0; j--) {
			if (j < wordLength) {
				currentEnding = toBePhonemised.substring(wordLength - j, wordLength);
				logger.debug("currentEnding: " + currentEnding);
				if (this.endingSet.contains(currentEnding)) {
					foundEnding = currentEnding;
					logger.debug("foundEnding....: " + foundEnding);
				}
			} else {
				continue;
			}

			if (foundEnding != null)
				break;
		}

		return foundEnding;
	}

	/**
	 * Voices the final consonant of an ending.
	 * 
	 * @param ending
	 *            the input ending
	 * @return voiced ending of ending can be voiced, input ending otherwise
	 */
	private String voiceFinal(String ending) {
		String finalPhoneme = null;
		String voicedFinalPhoneme = null;
		String voicedEnding = null;

		if (ending.length() > 0) {
			finalPhoneme = ending.substring(ending.length() - 1, ending.length());
		}
		if (getVoicedFinal(finalPhoneme) != null) {
			voicedFinalPhoneme = getVoicedFinal(finalPhoneme);
			voicedEnding = ending.substring(0, ending.length() - 1) + voicedFinalPhoneme;// return new ending voiced
		} else {
			// if there is no voiced value for the last phone, return ending as it came in
			voicedEnding = ending;
		}
		return voicedEnding;
	}

	/**
	 * If the given string ends with a consonant, insert a syllable boundary before that consonant. Otherwise, append a syllable
	 * boundary.
	 * 
	 * @param s
	 *            input syllable
	 * @return syllable with boundaries reset
	 */
	private String rebuildTrans(String s) {
		AllophoneSet set = jphon.getAllophoneSet();
		if (set != null) {
			Allophone[] allophones = set.splitIntoAllophones(s);
			if (allophones != null && allophones.length > 0) {
				Allophone last = allophones[allophones.length - 1];
				if (last.isConsonant()) { // insert a syllable boundary before final consonant
					String lastPh = last.name();
					return s.substring(0, s.length() - lastPh.length()) + "-" + lastPh;
				}
			}
		}
		return s + "-";
	}

	/**
	 * Checks if input string is a gerund - means: input ends with 'end' Sets global var isVerbalGerund which is important for
	 * building transcription later on
	 * 
	 * @param s
	 *            input string
	 * @return true if s.substring(s.length -3, s.length).equals(gerundEndings[j]), false otherwise
	 */
	private boolean checkIfGerund(String s) {
		String[] gerundEndings = getEndingsAndAffixes("gerund_ending");// should be 'end' for german
		for (int j = 0; j < gerundEndings.length; j++) {
			if (s.length() > gerundEndings[j].length()) {
				if (s.substring(s.length() - 3, s.length()).equals(gerundEndings[j])) {// we have an gerund
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if flection ending is a short superlative ending
	 * 
	 * @param flectionEnding
	 *            flection ending
	 * @return true if ending is short superlative false otherwise
	 */
	private boolean isShortSuperlative(String flectionEnding) {
		String[] shortSuperlativeEndings = getEndingsAndAffixes("superlative_short");
		for (int i = 0; i < shortSuperlativeEndings.length; i++) {
			if (flectionEnding != null) {
				if (flectionEnding.equals(shortSuperlativeEndings[i])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks if flection ending begins with a vowel
	 * 
	 * @param ending
	 *            flection ending
	 * @return true if ending begins with a vowel, false otherwise
	 */
	private boolean endingBeginsWithVowel(String ending) {
		for (int i = 0; i < this.vowels.length; i++) {
			if (this.vowels[i].equals(ending.substring(0, 1))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if flection ending contains a vowel
	 * 
	 * @param ending
	 *            flection ending
	 * @return true if ending contains a vowel, false otherwise
	 */
	private boolean endingContainsVowel(String ending) {
		for (int i = 0; i < this.vowels.length; i++) {
			for (int j = 0; j < ending.length(); j++) {
				if (this.vowels[i].equals(ending.substring(j, j + 1))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean endsWithVowel(String s) {
		for (int i = 0; i < this.vowels.length; i++) {
			for (int j = 0; j < s.length(); j++) {
				if (this.vowels[i].equals(s.substring(s.length() - 1, s.length()))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isLongParticipleBaseEnding(String s, String[] participleBaseLong) {
		for (int j = 0; j < participleBaseLong.length; j++) {
			if (s.equals(participleBaseLong[j])) {// 'et'
				return true;
			}
		}
		return false;
	}

	private boolean isShortParticipleBaseEnding(String s, String[] participleBaseShort) {
		for (int j = 0; j < participleBaseShort.length; j++) {
			if (s.equals(participleBaseShort[j])) {// 't'
				return true;
			}
		}
		return false;
	}

	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Checks if item is in english lexicon.
	 * 
	 * @param s
	 *            english base form
	 * @return true if item is in english lexicon, false if not
	 */
	private boolean knowEnBaseForm(String s) {
		if (jphon.phonemiseEn(s) != null) {
			return true;
		}
		return false;
	}

	// public boolean usedOtherLanguageToPhonemise(boolean usedOtherLanguageToPhonemise) {
	// return usedOtherLanguageToPhonemise;
	// }

	/**
	 ******************************************************************************
	 Hashtable lookup methods for morphology & phonology information
	 ******************************************************************************
	 **/
	/**
	 * Looking for item in german prefix lexicon
	 * 
	 * @param s
	 *            item to be found
	 * @return Transcription of item
	 */
	private String prefixLexiconLookup(String s) {
		String prefixTranscription = null;
		try {
			if (this.prefixLexicon.get(s) != null) {
				prefixTranscription = (String) this.prefixLexicon.get(s);
			}
		} catch (Exception e) {
			logger.debug("prefixLexLookup: " + e.toString());
		}
		return prefixTranscription;
	}

	/**
	 * Looks up in terminal voicing hash if there is a match for a unvoiced consonant
	 * 
	 * @param phon
	 *            The unvoiced consonant
	 * @return Voiced consonant if one can be found, null otherwise
	 */
	private String getVoicedFinal(String phon) {
		String voicedPhon = null;
		try {
			if (this.terminalVoicings.get(phon) != null) {
				voicedPhon = (String) this.terminalVoicings.get(phon);
			}
		} catch (Exception e) {
			logger.debug("getVoicedFinal: " + e.toString());
		}
		return voicedPhon;
	}

	/**
	 * Looks up list of all endings and affixes for specific language
	 * 
	 * @param key
	 *            key of the affixes/endings you want to get, i.e. superlative_short
	 * @return list of endings or affixes if key is valid, null otherwise
	 */
	private String[] getEndingsAndAffixes(String key) {
		String[] endingList = null;
		try {
			if (this.endingsAndAffixes.get(key) != null) {
				String value = (String) this.endingsAndAffixes.get(key);
				endingList = value.split("/");
			}
		} catch (Exception e) {
			logger.debug("getEndingsAndAffixes: " + e.toString());
		}
		return endingList;
	}

	/**
	 * Try to get transcription for ending
	 * 
	 * @param s
	 *            The ending to be phonemised
	 * @return Transcription of ending
	 */
	private String endingTranscriptionLookup(String s) {
		String affixPhon = null;
		try {
			if (this.flectionToPhon.get(s) != null) {
				affixPhon = (String) this.flectionToPhon.get(s);
			}
		} catch (Exception e) {
			logger.debug("endingTranscriptionLookup: " + e.toString());
		}
		return affixPhon;
	}
}
