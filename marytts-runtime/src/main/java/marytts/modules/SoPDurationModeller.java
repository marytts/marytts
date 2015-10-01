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
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

public class SoPDurationModeller extends InternalModule {

	private String sopFileName;
	private SoP vowelSop;
	private SoP consonantSop;
	private SoP pauseSop;
	private boolean logDuration = true;

	protected TargetFeatureComputer featureComputer;
	private FeatureProcessorManager featureProcessorManager;
	private AllophoneSet allophoneSet;
	private FeatureDefinition voiceFeatDef;

	/**
	 * Constructor which can be directly called from init info in the config file. This constructor will use the registered
	 * feature processor manager for the given locale.
	 * 
	 * @param locale
	 *            a locale string, e.g. "en"
	 * @param sopFile
	 *            sopFile
	 * @throws Exception
	 *             Exception
	 */
	public SoPDurationModeller(String locale, String sopFile) throws Exception {
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
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @param featprocClassInfo
	 *            a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
	 * @throws Exception
	 *             Exception
	 */
	public SoPDurationModeller(String locale, String sopFile, String featprocClassInfo) throws Exception {
		this(MaryUtils.string2locale(locale), sopFile, (FeatureProcessorManager) MaryRuntimeUtils
				.instantiateObject(featprocClassInfo));
	}

	/**
	 * Constructor to be called with instantiated objects.
	 * 
	 * @param locale
	 *            locale
	 * @param sopFile
	 *            the prefix to be used when looking up entries in the config files, e.g. "english.duration"
	 * @param featureProcessorManager
	 *            the manager to use when looking up feature processors.
	 */
	protected SoPDurationModeller(Locale locale, String sopFile, FeatureProcessorManager featureProcessorManager) {
		super("SoPDurationModeller", MaryDataType.ALLOPHONES, MaryDataType.DURATIONS, locale);
		this.sopFileName = sopFile;
		this.featureProcessorManager = featureProcessorManager;

	}

	public void startup() throws Exception {
		super.startup();

		// Read dur.sop file to load linear equations
		// The first section contains the feature definition, after one empty line,
		// the first line corresponds to vowels, next line to consonants and next line to pause
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
				vowelSop = new SoP(nextLine, voiceFeatDef);
				// vowelSop.printCoefficients();
			}
			// consonant line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line consonants = " + nextLine);
				consonantSop = new SoP(nextLine, voiceFeatDef);
				// consonantSop.printCoefficients();
			}
			// pause line
			if (s.hasNext()) {
				nextLine = s.nextLine();
				// System.out.println("line pause = " + nextLine);
				pauseSop = new SoP(nextLine, voiceFeatDef);
				// pauseSop.printCoefficients();
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

			allophoneSet = maryVoice.getAllophoneSet();
			TargetFeatureComputer currentFeatureComputer = featureComputer;

			// cumulative duration from beginning of sentence, in seconds:
			float end = 0;
			float durInSeconds;
			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
			Element segmentOrBoundary;
			Element previous = null;
			while ((segmentOrBoundary = (Element) tw.nextNode()) != null) {
				String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);

				Target t = new Target(phone, segmentOrBoundary);
				t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));

				if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause
					System.out.print("Pause PHONE: " + phone);
					durInSeconds = (float) pauseSop.solve(t, voiceFeatDef, logDuration, false);
					if (durInSeconds < 0.0) {
						System.out.println("\nWARNING: duration < 0.0");
						durInSeconds = (float) pauseSop.solve(t, voiceFeatDef, logDuration, true);
					}
				} else {
					if (allophoneSet.getAllophone(phone).isVowel()) {
						// calculate duration with vowelSop
						System.out.print("Vowel PHONE: " + phone);
						durInSeconds = (float) vowelSop.solve(t, voiceFeatDef, logDuration, false);
						if (durInSeconds < 0.0) {
							System.out.println("\nWARNING: duration < 0.0");
							durInSeconds = (float) vowelSop.solve(t, voiceFeatDef, logDuration, true);
						}
					} else {
						// calculate duration with consonantSop
						System.out.print("Cons. PHONE: " + phone);
						durInSeconds = (float) consonantSop.solve(t, voiceFeatDef, logDuration, false);
						if (durInSeconds < 0.0) {
							System.out.println("\nWARNING: duration < 0.0");
							durInSeconds = (float) consonantSop.solve(t, voiceFeatDef, logDuration, true);
						}
					}
				}
				// TODO: where do we check that the solution is log(duration) or duration???
				System.out.format(" = %.3f\n", durInSeconds);
				// TODO: this problem is not solved, it seems it has to do with punctuation (?)
				if (durInSeconds < 0) {
					throw new Exception("Error generating SoP Duration: durInSeconds < 0.0 ");
				}
				end += durInSeconds;
				int durInMillis = (int) (1000 * durInSeconds);
				if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
					segmentOrBoundary.setAttribute("duration", String.valueOf(durInMillis));
				} else { // phone
					segmentOrBoundary.setAttribute("d", String.valueOf(durInMillis));
					segmentOrBoundary.setAttribute("end", String.valueOf(end));
				}
				previous = segmentOrBoundary;
			}
		}
		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;

	}

}
