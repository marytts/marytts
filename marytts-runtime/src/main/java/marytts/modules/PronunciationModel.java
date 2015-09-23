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
package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.cart.StringPredictionTree;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;

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
	protected boolean postlexicalRules(Element token, AllophoneSet allophoneSet) {
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
		logger.debug("Getting xml-data from document finished.");

		TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.TOKEN);
		Element t;
		AllophoneSet allophoneSet = null;
		while ((t = (Element) tw.nextNode()) != null) {
			// First, create the substructure of <t> elements: <syllable> and <ph>.
			if (allophoneSet == null) { // need to determine it once, then assume it is the same for all
				allophoneSet = MaryRuntimeUtils.determineAllophoneSet(t);
			}
			createSubStructure(t, allophoneSet);

			// Modify by rule:
			boolean changedSomething = postlexicalRules(t, allophoneSet);
			if (changedSomething) {
				updatePhAttributesFromPhElements(t);
			}

			if (treeMap == null)
				continue;

			// Modify by trained model:
			assert featureComputer != null;

			// Now, predict modified pronunciations, adapt <ph> elements accordingly,
			// and update ph for syllable and t elements where necessary
			StringBuilder tPh = new StringBuilder();
			TreeWalker sylWalker = MaryDomUtils.createTreeWalker(doc, t, MaryXML.SYLLABLE);
			Element syllable;
			while ((syllable = (Element) sylWalker.nextNode()) != null) {
				StringBuilder sylPh = new StringBuilder();
				String stressed = syllable.getAttribute("stress");
				if (stressed.equals("1")) {
					sylPh.append("'");
				} else if (stressed.equals("2")) {
					sylPh.append(",");
				}
				TreeWalker segWalker = MaryDomUtils.createTreeWalker(doc, syllable, MaryXML.PHONE);
				Element seg;
				// Cannot use tree walker directly, because we concurrently modify the tree:
				List<Element> originalSegments = new ArrayList<Element>();
				while ((seg = (Element) segWalker.nextNode()) != null) {
					originalSegments.add(seg);
				}
				for (Element s : originalSegments) {
					String phoneString = s.getAttribute("p");
					String[] predicted;
					// in case we have a decision tree for phone, predict - otherwise leave unchanged
					if (treeMap.containsKey(phoneString)) {
						Target tgt = new Target(phoneString, s);
						tgt.setFeatureVector(featureComputer.computeFeatureVector(tgt));
						StringPredictionTree tree = (StringPredictionTree) treeMap.get(phoneString);
						String predictStr = tree.getMostProbableString(tgt);
						if (sylPh.length() > 0)
							sylPh.append(" ");
						sylPh.append(predictStr);
						// if phone is deleted:
						if (predictStr.equals("")) {
							predicted = null;
						} else {
							// predictStr contains whitespace between phones
							predicted = predictStr.split(" ");
						}
					} else {
						logger.debug("didn't find decision tree for phone (" + phoneString + "). Just keeping it.");
						predicted = new String[] { phoneString };
					}
					logger.debug("  Predicted phone in sequence of " + predicted.length + " phones.");
					// deletions:
					if (predicted == null || predicted.length == 0) {
						syllable.removeChild(s);
						continue; // skip what follows
					}
					assert predicted != null && predicted.length > 0;
					// insertions: for each but the last predicted phone, make a new element
					for (int lc = 0; lc < predicted.length - 1; lc++) {
						Element newPh = MaryXML.createElement(doc, MaryXML.PHONE);
						newPh.setAttribute("p", predicted[lc]);
						syllable.insertBefore(newPh, s);
					}
					// for the last (or only) predicted segment, just update the phone label
					if (!phoneString.equals(predicted[predicted.length - 1])) {
						s.setAttribute("p", predicted[predicted.length - 1]);
					}
				} // for each segment in syllable
				String newSylPh = sylPh.toString();
				syllable.setAttribute("ph", newSylPh);
				if (tPh.length() > 0)
					tPh.append(" -"); // syllable boundary
				tPh.append(newSylPh);
			} // for each syllable in token
			t.setAttribute("ph", tPh.toString());

		} // for each token in document

		// return new MaryData with changed phonology
		MaryData result = new MaryData(outputType(), d.getLocale());
		result.setDocument(doc);

		logger.debug("Setting the changed xml document finished.");
		return result;
	}

	private void createSubStructure(Element token, AllophoneSet allophoneSet) {
		String phone = token.getAttribute("ph");
		if (phone.equals(""))
			return; // nothing to do

		if (token.getElementsByTagName(MaryXML.SYLLABLE).getLength() > 0) {
			return; // there is already a substructure under this token; nothing to do
		}

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
		while (tok.hasMoreTokens()) {
			String sylString = tok.nextToken();
			if (sylString.trim().isEmpty()) {
				continue;
			}
			Allophone[] allophones = allophoneSet.splitIntoAllophones(sylString);
			Element syllable = MaryXML.createElement(document, MaryXML.SYLLABLE);
			token.appendChild(syllable);
			String syllableText = "";
			for (int i = 0; i < allophones.length; i++) {
				if (allophones[i].isTone()) {
					syllable.setAttribute("tone", allophones[i].name());
					continue;
				}
				if (i == 0) {
					syllableText = allophones[i].name();
				} else {
					syllableText = syllableText + " " + allophones[i].name();
				}
			}
			// Check for stress signs:
			String first = sylString.trim().substring(0, 1);
			if (first.equals("'")) {
				syllable.setAttribute("stress", "1");
				// The primary stressed syllable of a word
				// inherits the accent:
				if (token.hasAttribute("accent")) {
					syllable.setAttribute("accent", token.getAttribute("accent"));
				}
			} else if (first.equals(",")) {
				syllable.setAttribute("stress", "2");
			}
			// Remember transcription in ph attribute:
			syllable.setAttribute("ph", syllableText);
			// Now identify the composing segments:
			for (int i = 0; i < allophones.length; i++) {
				if (allophones[i].isTone()) {
					continue;
				}
				Element segment = MaryXML.createElement(document, MaryXML.PHONE);
				syllable.appendChild(segment);
				segment.setAttribute("p", allophones[i].name());
				if (vq != null && !(allophones[i].name().equals("_") || allophones[i].name().equals("?"))) {
					segment.setAttribute("vq", vq);
				}
			}
		}
	}

	protected void updatePhAttributesFromPhElements(Element token) {
		if (token == null)
			throw new NullPointerException("Got null token");
		if (!token.getTagName().equals(MaryXML.TOKEN)) {
			throw new IllegalArgumentException("Argument should be a <" + MaryXML.TOKEN + ">, not a <" + token.getTagName() + ">");
		}
		StringBuilder tPh = new StringBuilder();
		TreeWalker sylWalker = MaryDomUtils.createTreeWalker(token, MaryXML.SYLLABLE);
		Element syl;
		while ((syl = (Element) sylWalker.nextNode()) != null) {
			StringBuilder sylPh = new StringBuilder();
			String stress = syl.getAttribute("stress");
			if (stress.equals("1"))
				sylPh.append("'");
			else if (stress.equals("2"))
				sylPh.append(",");
			TreeWalker phWalker = MaryDomUtils.createTreeWalker(syl, MaryXML.PHONE);
			Element ph;
			while ((ph = (Element) phWalker.nextNode()) != null) {
				if (sylPh.length() > 0)
					sylPh.append(" ");
				sylPh.append(ph.getAttribute("p"));
			}
			String sylPhString = sylPh.toString();
			syl.setAttribute("ph", sylPhString);
			if (tPh.length() > 0)
				tPh.append(" - ");
			tPh.append(sylPhString);
			if (syl.hasAttribute("tone")) {
				tPh.append(" " + syl.getAttribute("tone"));
			}
		}
		token.setAttribute("ph", tPh.toString());
	}
}
