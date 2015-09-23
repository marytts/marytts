/**
 * Copyright 2010 DFKI GmbH.
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
import java.io.FileReader;
import java.io.StringReader;
import java.util.Locale;
import java.util.Scanner;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.machinelearning.SoP;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

public class SoPF0Modeller extends InternalModule {
	protected SoP leftSop;
	protected SoP midSop;
	protected SoP rightSop;
	private String sopFileName;
	protected TargetFeatureComputer featureComputer;
	private FeatureProcessorManager featureProcessorManager;
	private AllophoneSet allophoneSet;
	private FeatureDefinition voiceFeatDef;
	private boolean logF0 = false;

	/**
	 * Constructor which can be directly called from init info in the config file. This constructor will use the registered
	 * feature processor manager for the given locale.
	 * 
	 * @param locale
	 *            a locale string, e.g. "en"
	 * @param sopFile
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @throws Exception
	 *             Exception
	 */
	public SoPF0Modeller(String locale, String sopFile) throws Exception {
		this(MaryUtils.string2locale(locale), sopFile, FeatureRegistry
				.getFeatureProcessorManager(MaryUtils.string2locale(locale)));
	}

	/**
	 * Constructor which can be directly called from init info in the config file. Different languages can call this code with
	 * different settings.
	 * 
	 * @param locale
	 *            a locale string, e.g. "en"
	 * @param sopFile
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
	 * @param featprocClassInfo
	 *            a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
	 * @throws Exception
	 *             Exception
	 */
	public SoPF0Modeller(String locale, String sopFile, String featprocClassInfo) throws Exception {
		this(MaryUtils.string2locale(locale), sopFile, (FeatureProcessorManager) MaryRuntimeUtils
				.instantiateObject(featprocClassInfo));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 * @param sopFile
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.f0"
	 * @param featureProcessorManager
	 *            the manager to use when looking up feature processors.
	 */
	protected SoPF0Modeller(Locale locale, String sopFile, FeatureProcessorManager featureProcessorManager) {
		super("SoPF0Modeller", MaryDataType.DURATIONS, MaryDataType.ACOUSTPARAMS, locale);

		this.sopFileName = sopFile;
		this.featureProcessorManager = featureProcessorManager;
	}

	public void startup() throws Exception {
		super.startup();

		// Read dur.sop file to load linear equations
		// The first section contains the feature definition, after one empty line,
		// the first line corresponds to left, the next line to mid and next line to right F0
		String sopFile = MaryProperties.getFilename(sopFileName);
		// System.out.println("sopFileName: " + sopFile);
		String nextLine;
		String strContext = "";
		Scanner s = null;
		try {
			s = new Scanner(new BufferedReader(new FileReader(sopFile)));

			// The first part contains the feature definition
			while (s.hasNext()) {
				nextLine = s.nextLine();
				if (nextLine.trim().equals(""))
					break;
				else
					strContext += nextLine + "\n";
			}
			// the featureDefinition is the same for vowel, consonant and Pause
			voiceFeatDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

			// vowel line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line vowel = " + nextLine);
				leftSop = new SoP(nextLine, voiceFeatDef);
				// leftSop.printCoefficients();
			}
			// consonant line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line consonants = " + nextLine);
				midSop = new SoP(nextLine, voiceFeatDef);
				// midSop.printCoefficients();
			}
			// pause line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line pause = " + nextLine);
				rightSop = new SoP(nextLine, voiceFeatDef);
				// rightSop.printCoefficients();
			}
		} finally {
			if (s != null)
				s.close();
		}

		// get a feature computer
		featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());

	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
		Element sentence = null;
		// AllophoneSet allophoneSet = null;
		while ((sentence = (Element) sentenceIt.nextNode()) != null) {
			// Make sure we have the correct voice:
			Element voice = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.VOICE);
			Voice maryVoice = Voice.getVoice(voice);
			if (maryVoice == null) {
				maryVoice = d.getDefaultVoice();
			}
			if (maryVoice == null) {
				// Determine Locale in order to use default voice
				Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
				maryVoice = Voice.getDefaultVoice(locale);
			}
			SoP currentLeftSop = leftSop;
			SoP currentMidSop = midSop;
			SoP currentRightSop = rightSop;
			TargetFeatureComputer currentFeatureComputer = featureComputer;
			if (maryVoice instanceof UnitSelectionVoice) {
				if (voiceFeatDef != null) {
					currentFeatureComputer = new TargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());
					logger.debug("Using voice feature definition");
				}
			}

			if (currentLeftSop == null) {
				throw new NullPointerException("Do not have f0 prediction Sop model");
			}

			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.SYLLABLE);
			Element syllable;
			Element previous = null;
			while ((syllable = (Element) tw.nextNode()) != null) {
				Element firstVoiced = null;
				Element vowel = null;
				Element lastVoiced = null;
				for (Element s = MaryDomUtils.getFirstChildElement(syllable); s != null; s = MaryDomUtils
						.getNextSiblingElement(s)) {
					assert s.getTagName().equals(MaryXML.PHONE) : "expected phone element, found " + s.getTagName();
					String phone = s.getAttribute("p");
					if (allophoneSet == null) {
						allophoneSet = MaryRuntimeUtils.determineAllophoneSet(s);
					}
					assert allophoneSet != null;
					Allophone allophone = allophoneSet.getAllophone(phone);
					if (allophone.isVowel()) {
						// found a vowel
						if (firstVoiced == null)
							firstVoiced = s;
						if (vowel == null)
							vowel = s;
						lastVoiced = s; // last so far, at least
					} else if (allophone.isVoiced()) {
						// voiced consonant
						if (firstVoiced == null)
							firstVoiced = s;
						lastVoiced = s;
					}
				}
				// only predict F0 values if we have a vowel:
				if (vowel != null) {
					assert firstVoiced != null : "First voiced should not be null";
					assert lastVoiced != null : "Last voiced should not be null";
					// Now predict the f0 values using the SoPs:ssh
					String phone = vowel.getAttribute("p");
					System.out.print("PHONE: " + phone + "  ");

					Target t = new Target(phone, vowel);
					t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));

					// float[] left = (float[])currentLeftSoP.interpret(t, 0);
					float left = (float) currentLeftSop.solve(t, voiceFeatDef, logF0, false);
					// assert left != null : "Null frequency";
					// assert left.length == 2 : "Unexpected frequency length: "+left.length;
					float leftF0InHz = left;
					// float leftStddevInHz = left[0];

					// float[] mid = (float[])currentMidSoP.interpret(t, 0);
					float mid = (float) currentMidSop.solve(t, voiceFeatDef, logF0, false);
					// assert mid != null : "Null frequency";
					// assert mid.length == 2 : "Unexpected frequency length: "+mid.length;
					float midF0InHz = mid;
					// float midStddevInHz = mid[0];

					// float[] right = (float[])currentRightSoP.interpret(t, 0);
					float right = (float) currentRightSop.solve(t, voiceFeatDef, logF0, false);
					// assert right != null : "Null frequency";
					// assert right.length == 2 : "Unexpected frequency length: "+right.length;
					float rightF0InHz = right;
					// float rightStddevInHz = right[0];

					System.out.format("leftf0=%.3f midf0=%.3f  rightf0=%.3f \n", left, mid, right);

					// Now set targets:
					String leftTargetString = "(0," + ((int) leftF0InHz) + ")";
					String currentVal = firstVoiced.getAttribute("f0");
					String newVal;
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + leftTargetString;
					} else {
						newVal = leftTargetString;
					}
					firstVoiced.setAttribute("f0", newVal);

					String midTargetString = "(50," + ((int) midF0InHz) + ")";
					currentVal = vowel.getAttribute("f0");
					// for example, if firstVoiced == vowel, then we have just set a first f0 value
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + midTargetString;
					} else {
						newVal = midTargetString;
					}
					vowel.setAttribute("f0", newVal);

					String rightTargetString = "(100," + ((int) rightF0InHz) + ")";
					currentVal = lastVoiced.getAttribute("f0");
					// for example, if lastVoiced == vowel, then we have just set a first f0 value
					if (!currentVal.equals("")) {
						newVal = currentVal + " " + rightTargetString;
					} else {
						newVal = rightTargetString;
					}
					lastVoiced.setAttribute("f0", newVal);

				}
			}
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;
	}
}
