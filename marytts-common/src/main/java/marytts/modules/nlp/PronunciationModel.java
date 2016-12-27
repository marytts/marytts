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
package marytts.modules.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.modules.InternalModule;

import marytts.modeling.cart.StringPredictionTree;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modeling.features.FeatureDefinition;
import marytts.modeling.features.FeatureProcessorManager;
import marytts.modeling.features.TargetFeatureComputer;
import marytts.modules.nlp.phonemiser.Allophone;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.server.MaryProperties;
import marytts.modeling.features.Target;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.modules.synthesis.Voice;

import marytts.data.item.phonology.Syllable;
import marytts.data.item.phonology.Accent;
import marytts.data.item.phonology.Phoneme;
import marytts.data.item.linguistic.Sentence;
import marytts.data.item.linguistic.Word;
import marytts.data.item.prosody.Phrase;
import marytts.data.Utterance;
import marytts.data.Sequence;
import marytts.data.Relation;
import marytts.data.SupportedSequenceType;
import marytts.data.utils.IntegerPair;
import marytts.io.XMLSerializer;

import org.w3c.dom.Document;

/**
 *
 * This module serves as a post-lexical pronunciation model. Its appropriate place in the module chain is after intonisation. The
 * target features are taken and fed into decision trees that predict the new pronunciation. A new mary xml is output, with the
 * difference being that the old pronunciation is replaced by the newly predicted one, and a finer grained xml structure.
 *
 * @author ben
 *
 */
public class PronunciationModel extends InternalModule {

	// for prediction, core of the model - maps phones to decision trees
	private Map<String, StringPredictionTree> treeMap;

	// used in startup() and later for convenience
	private FeatureDefinition featDef;

	private TargetFeatureComputer featureComputer;

	/**
	 * Constructor, stating that the input is of type INTONATION, the output of type ALLOPHONES.
	 *
	 */
	public PronunciationModel() {
		this(null);
	}

	public PronunciationModel(Locale locale) {
		super("PronunciationModel", MaryDataType.INTONATION, MaryDataType.ALLOPHONES, locale);
	}

	public void startup() throws Exception {
		super.startup();

		// TODO: pronunciation model tree and feature definition should be voice-specific
		// get featureDefinition used for trees - just to tell the tree that the
		// features are discrete
		String fdFilename = null;
		if (getLocale() != null) {
			fdFilename = MaryProperties
                .getFilename(MaryProperties.localePrefix(getLocale()) + ".pronunciation.featuredefinition");
		}
		if (fdFilename != null) {
			File fdFile = new File(fdFilename);
			// reader for file, readweights = false
			featDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), false);

			// get path where the prediction trees lie
			File treePath = new File(MaryProperties.needFilename(MaryProperties.localePrefix(getLocale())
                                                                 + ".pronunciation.treepath"));

			// valid predicion tree files are named prediction_<phone_symbol>.tree
			Pattern treeFilePattern = Pattern.compile("^prediction_(.*)\\.tree$");

			// initialize the map that contains the trees
			this.treeMap = new HashMap<String, StringPredictionTree>();

			// iterate through potential prediction tree files
			File[] fileArray = treePath.listFiles();
			for (int fileIndex = 0; fileIndex < fileArray.length; fileIndex++) {
				File f = fileArray[fileIndex];

				// is file name valid?
				Matcher filePatternMatcher = treeFilePattern.matcher(f.getName());

				if (filePatternMatcher.matches()) {
					// phone of file name is a group in the regex
					String phoneId = filePatternMatcher.group(1);

					// construct tree from file and map phone to it
					StringPredictionTree predictionTree = new StringPredictionTree(new BufferedReader(new FileReader(f)), featDef);

					// back mapping from short id
					int index = this.featDef.getFeatureIndex("phone");
					this.treeMap.put(this.featDef.getFeatureValueAsString(index, Short.parseShort(phoneId)), predictionTree);
					// logger.debug("Read in tree for " + PhoneNameConverter.normForm2phone(phone));
				}
			}
			logger.debug("Reading in feature definition and decision trees finished.");

			// TODO: change property name to german.pronunciation.featuremanager/features
			String managerClass = MaryProperties.needProperty(MaryProperties.localePrefix(getLocale())
                                                              + ".pronunciation.targetfeaturelister.featuremanager");
			FeatureProcessorManager manager = (FeatureProcessorManager) Class.forName(managerClass).newInstance();
			String features = MaryProperties.needProperty(MaryProperties.localePrefix(getLocale())
                                                          + ".pronunciation.targetfeaturelister.features");
			this.featureComputer = new TargetFeatureComputer(manager, features);
		}
		logger.debug("Building feature computer finished.");
	}

	/**
	 * Optionally, a language-specific subclass can implement any postlexical rules on the document.
	 *
	 * @param token
	 *            a &lt;t&gt; element with a syllable and &lt;ph&gt; substructure.
	 * @param allophoneSet
	 *            allophoneSet
	 * @return true if something was changed, false otherwise
	 */
	protected boolean postlexicalRules(Word token, AllophoneSet allophoneSet) {
		return false;
	}

	/**
	 * This computes a new pronunciation for the elements of some MaryData, that is phonemised.
	 *
	 * @param d
	 *            d
	 * @throws Exception
	 *             Exception
	 */
	public MaryData process(MaryData d) throws Exception {
		// get the xml document
        Document doc = d.getDocument();
        AllophoneSet allophoneSet = null;
        XMLSerializer xml_ser = new XMLSerializer();
        Utterance utt = xml_ser.unpackDocument(doc);
        Sequence<Word> words = (Sequence<Word>) utt.getSequence(SupportedSequenceType.WORD);
        Relation rel_words_sent = utt.getRelation(SupportedSequenceType.SENTENCE, SupportedSequenceType.WORD).getReverse();
        HashSet<IntegerPair> alignment_word_phrase = new HashSet<IntegerPair>();

        for (int i_word=0; i_word<words.size(); i_word++)
        {
            Word w = words.get(i_word);
            alignment_word_phrase.add(new IntegerPair(rel_words_sent.getRelatedIndexes(i_word)[0], i_word));

            // First, create the substructure of <t> elements: <syllable> and <ph>.
            if (allophoneSet == null) { // need to determine it once, then assume it is the same for all
                Voice maryVoice = Voice.getVoice(utt.getVoiceName());
                if (maryVoice == null) {
                    // Determine Locale in order to use default voice
                    Locale locale = utt.getLocale();
                    maryVoice = Voice.getDefaultVoice(locale);
                }
                if (maryVoice != null) {
                    allophoneSet = maryVoice.getAllophoneSet();
                } else {
                    allophoneSet = MaryRuntimeUtils.determineAllophoneSet(utt.getLocale());
                }
            }

            logger.info(allophoneSet);
            createSubStructure(w, allophoneSet);
        }

        // Create the phrase sequence (FIXME: let's be naive for now, 1 sentence = 1 phrase)
        // Create the sentence/phrase relation

        // Create the phrase/word relation
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(xml_ser.generateDocument(utt));
        return result;
    }

    private void createSubStructure(Word w, AllophoneSet allophoneSet)
        throws Exception
    {
        ArrayList<Phoneme> phonemes = w.getPhonemes();
        if (phonemes.size() == 0)
            return;

        if (w.getSyllables().size() > 0)
            return; // FIXME: maybe throw an exception to indicate that technically we should not arrive in a syllabification stage a second time

        /**********************************************************************
         *** FIXME: why that ?!
            StringTokenizer tok = new StringTokenizer(phone, "-");
            Document document = token.getOwnerDocument();
            Element prosody = (Element) MaryDomUtils.getAncestor(token, MaryXML.PROSODY);
            String vq = null; // voice quality
            if (prosody != null) {
            // Ignore any effects of ancestor prosody tags for now:
            String volumeString = prosody.getAttribute("volume");
            int volume = -1;
            try {
            volume = Integer.parseInt(volumeString);
            } catch (NumberFormatException e) {
            }
            if (volume >= 0) {
            if (volume >= 60) {
            vq = "loud";
            } else if (volume <= 40) {
            vq = "soft";
            } else {
            vq = null;
            }
            }
            }
        */

        for (Phoneme p:phonemes)
        {
            String sylString = p.getLabel();
            if (sylString.trim().isEmpty()) {
                continue;
            }
            logger.info("Dealing with \"" + sylString + "\"");
            Allophone[] allophones = allophoneSet.splitIntoAllophones(sylString);
            Phoneme tone = null;
            ArrayList<Phoneme> syl_phonemes = new ArrayList<Phoneme>();
            for (int i = 0; i < allophones.length; i++) {
                if (allophones[i].isTone()) {
                    tone = allophones[i];
                    continue;
                }

                Phoneme cur_ph = new Phoneme(allophones[i].name());
                syl_phonemes.add(cur_ph);
                /*
                  if (vq != null && !(allophones[i].name().equals("_") || allophones[i].name().equals("?"))) {
                  segment.setAttribute("vq", vq);
                  }
                */
            }

            // Check for stress signs:
            String first = sylString.trim().substring(0, 1);
            int stress = 0;
            Accent accent = null;
            if (first.equals("'")) {
                stress = 1;
                // The primary stressed syllable of a word
                // inherits the accent:
                accent =  w.getAccent();
                logger.info("set syllable accent to \"" + accent + "\"");
            } else if (first.equals(",")) {
                stress = 2;
            }

            // Finally create the syllable and add it to the word
            w.addSyllable(new Syllable(syl_phonemes, tone, stress, accent));
        }
    }

// protected void updatePhAttributesFromPhElements(Word w)
// {
//     if (token == null)
//         throw new NullPointerException("Got null token");
//     if (!token.getTagName().equals(MaryXML.TOKEN)) {
//         throw new IllegalArgumentException("Argument should be a <" + MaryXML.TOKEN + ">, not a <" + token.getTagName() + ">");
//     }
//     StringBuilder tPh = new StringBuilder();
//     TreeWalker sylWalker = MaryDomUtils.createTreeWalker(token, MaryXML.SYLLABLE);
//     Element syl;
//     while ((syl = (Element) sylWalker.nextNode()) != null) {
//         StringBuilder sylPh = new StringBuilder();
//         String stress = syl.getAttribute("stress");
//         if (stress.equals("1"))
//             sylPh.append("'");
//         else if (stress.equals("2"))
//             sylPh.append(",");
//         TreeWalker phWalker = MaryDomUtils.createTreeWalker(syl, MaryXML.PHONE);
//         Element ph;
//         while ((ph = (Element) phWalker.nextNode()) != null) {
//             if (sylPh.length() > 0)
//                 sylPh.append(" ");
//             sylPh.append(ph.getAttribute("p"));
//         }
//         String sylPhString = sylPh.toString();
//         syl.setAttribute("ph", sylPhString);
//         if (tPh.length() > 0)
//             tPh.append(" - ");
//         tPh.append(sylPhString);
//         if (syl.hasAttribute("tone")) {
//             tPh.append(" " + syl.getAttribute("tone"));
//         }
//     }
//     token.setAttribute("ph", tPh.toString());
// }
}
