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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.math.Polynomial;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/***
 * This modeller uses the HMMs of the provided hmmVoice. This modeller can be set as preferred module in the configuration file,
 * for example:
 * 
 * voice.unitSelection.preferredModules = \ marytts.modules.HMMDurationF0Modeller(local,hmmVoice)
 *
 * @author marcela
 * @deprecated
 */
public class HMMDurationF0Modeller extends InternalModule {

	private String hmmVoiceName;
	private Locale locale;
	private FeatureProcessorManager featureProcessorManager;
	private TargetFeatureLister targetFeatureLister;
	protected TargetFeatureComputer featureComputer;

	public HMMDurationF0Modeller(String locale, String hmmVoiceName) throws Exception {
		this(MaryUtils.string2locale(locale), hmmVoiceName, FeatureRegistry.getFeatureProcessorManager(MaryUtils
				.string2locale(locale)));
	}

	public HMMDurationF0Modeller(Locale locale, String hmmVoiceName, FeatureProcessorManager featureProcessorManager) {
		super("HMMDurationF0Modeller", MaryDataType.ALLOPHONES, MaryDataType.ACOUSTPARAMS, locale);
		this.hmmVoiceName = hmmVoiceName;
		this.locale = locale;
		this.featureProcessorManager = featureProcessorManager;

	}

	public void startup() throws Exception {
		super.startup();

		try {
			targetFeatureLister = (TargetFeatureLister) ModuleRegistry.getModule(TargetFeatureLister.class);
		} catch (NullPointerException npe) {
			targetFeatureLister = null;
		}
		if (targetFeatureLister == null) {
			logger.info("Starting my own TargetFeatureLister");
			targetFeatureLister = new TargetFeatureLister();
			targetFeatureLister.startup();
		} else if (targetFeatureLister.getState() == MaryModule.MODULE_OFFLINE) {
			targetFeatureLister.startup();
		}
	}

	public MaryData process(MaryData d) throws Exception {

		/**
		 * The utterance model, um, is a Vector (or linked list) of Model objects. It will contain the list of models for current
		 * label file.
		 */
		HTSUttModel um = new HTSUttModel();
		double f0[];

		/*
		 * here we need to use a HMM voice that has been trained with the same data as the unit slection, for example, if this
		 * module is going to be used in the unit selection voice: en_US-cmu-slt then we should load the HMMs from the
		 * en_US-cmu-slt-hsmm
		 */
		HMMVoice hmmVoice = (HMMVoice) Voice.getVoice(hmmVoiceName);
		String features = d.getOutputParams();
		if (hmmVoice != null) {
			featureComputer = FeatureRegistry.getTargetFeatureComputer(hmmVoice, features);
		}
		assert featureComputer != null : "Cannot get a feature computer!";
		Document doc = d.getDocument();
		// First, get the list of segments and boundaries in the current document
		TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY);
		List<Element> segmentsAndBoundaries = new ArrayList<Element>();
		Element e;
		while ((e = (Element) tw.nextNode()) != null) {
			segmentsAndBoundaries.add(e);
		}
		TargetFeatureComputer comp = FeatureRegistry.getTargetFeatureComputer(hmmVoice, features);
		String targetFeatureString = targetFeatureLister.listTargetFeatures(comp, segmentsAndBoundaries);

		if (hmmVoice != null) {
			String context = targetFeatureString;
			// System.out.println("TARGETFEATURES:" + context);

			/* Process label file of Mary context features and creates UttModel um */
			Scanner s = null;
			String realisedDurations;
			String realisedDurF0s;
			try {
				s = new Scanner(context);
				// Create the Uttmodel list and get durations
				realisedDurations = processUtt(s, um, hmmVoice.getHMMData(), hmmVoice.getHMMData().getCartTreeSet());
				// setActualDurations(tw, realisedDurations);

				// Given the UttModel list generate the F0 parameters
				realisedDurF0s = HmmF0Generation(um, hmmVoice.getHMMData());
				setActualDurationsAndF0s(tw, realisedDurF0s);

			} finally {
				if (s != null)
					s.close();
			}
		} else {
			logger.debug("No HMM voice called " + hmmVoiceName);
		}

		// processing 'prosody' tags
		ByteArrayOutputStream dummy = new ByteArrayOutputStream();
		d.writeTo(dummy);

		applyProsodySpecifications(doc);

		// the result is already in d
		return d;
	}

	/**
	 * A method to modify prosody modifications
	 * 
	 * @param doc
	 *            doc
	 */
	private void applyProsodySpecifications(Document doc) {

		TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY, MaryXML.PROSODY);
		Element e = null;

		// TODO: read prosody tags recursively
		while ((e = (Element) tw.nextNode()) != null) {

			if ("prosody".equals(e.getNodeName())) {
				NodeList nl = e.getElementsByTagName("ph");
				applyNewContourSpecifications(nl, e);
				applySpeechRateSpecifications(nl, e);
			}
		}
	}

	/**
	 * Apply 'rate' requirements to ACOUSTPARAMS
	 * 
	 * @param nl
	 *            nl
	 * @param prosodyElement
	 *            prosodyElement
	 */
	private void applySpeechRateSpecifications(NodeList nl, Element prosodyElement) {

		String rateAttribute = null;
		if (!prosodyElement.hasAttribute("rate")) {
			return;
		}

		rateAttribute = prosodyElement.getAttribute("rate");
		Pattern p = Pattern.compile("[+|-]\\d+%");

		// Split input with the pattern
		Matcher m = p.matcher(rateAttribute);
		if (m.find()) {
			double percentage = new Integer(rateAttribute.substring(1, rateAttribute.length() - 1)).doubleValue();
			if (rateAttribute.startsWith("+")) {
				setSpeechRateSpecifications(nl, percentage, -1.0);
			} else {
				setSpeechRateSpecifications(nl, percentage, +1.0);
			}
		}
	}

	/**
	 * set duration specifications according to 'rate' requirements
	 * 
	 * @param nl
	 *            nl
	 * @param percentage
	 *            percentage
	 * @param incriment
	 *            incriment
	 */
	private void setSpeechRateSpecifications(NodeList nl, double percentage, double incriment) {

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			if (!e.hasAttribute("d")) {
				continue;
			}
			double durAttribute = new Double(e.getAttribute("d")).doubleValue();
			double newDurAttribute = durAttribute + (incriment * percentage * durAttribute / 100);
			e.setAttribute("d", newDurAttribute + "");
			// System.out.println(durAttribute+" = " +newDurAttribute);
		}

		Element e = (Element) nl.item(0);

		Element rootElement = e.getOwnerDocument().getDocumentElement();
		NodeIterator nit = MaryDomUtils.createNodeIterator(rootElement, MaryXML.PHONE, MaryXML.BOUNDARY);
		Element nd;
		double duration = 0.0;
		for (int i = 0; (nd = (Element) nit.nextNode()) != null; i++) {
			if ("boundary".equals(nd.getNodeName())) {
				if (nd.hasAttribute("duration")) {
					duration += new Double(nd.getAttribute("duration")).doubleValue();
				}
			} else {
				if (nd.hasAttribute("d")) {
					duration += new Double(nd.getAttribute("d")).doubleValue();
				}
			}
			double endTime = 0.001 * duration;
			nd.setAttribute("end", endTime + "");
			// System.out.println(nd.getNodeName()+" = " +nd.getAttribute("end"));
		}

	}

	/**
	 * 
	 * @param nl
	 *            nl
	 * @param prosodyElement
	 *            prosodyElement
	 */
	private void applyNewContourSpecifications(NodeList nl, Element prosodyElement) {

		String contourAttribute = null;
		if (prosodyElement.hasAttribute("contour")) {
			contourAttribute = prosodyElement.getAttribute("contour");
		}

		String pitchAttribute = null;
		if (prosodyElement.hasAttribute("pitch")) {
			pitchAttribute = prosodyElement.getAttribute("pitch");
		}

		if (contourAttribute == null && pitchAttribute == null) {
			return;
		}

		double[] contour = getContiniousContour(nl);
		contour = interpolateNonZeroValues(contour);
		double[] coeffs = Polynomial.fitPolynomial(contour, 1);
		double[] polyValues = Polynomial.generatePolynomialValues(coeffs, 100, 0, 1);
		double[] diffValues = new double[100];

		// Extract base contour from original contour
		for (int i = 0; i < contour.length; i++) {
			diffValues[i] = contour[i] - polyValues[i];
		}

		polyValues = setBaseContourModifications(polyValues, contourAttribute, pitchAttribute);

		// Now, imposing back the diff. contour
		for (int i = 0; i < contour.length; i++) {
			contour[i] = diffValues[i] + polyValues[i];
		}

		setModifiedContour(nl, contour);

		return;
	}

	/**
	 * To set new modified contour into XML
	 * 
	 * @param nl
	 *            nl
	 * @param contour
	 *            contour
	 */
	private void setModifiedContour(NodeList nl, double[] contour) {

		Element firstElement = (Element) nl.item(0);
		Element lastElement = (Element) nl.item(nl.getLength() - 1);

		double fEnd = (new Double(firstElement.getAttribute("end"))).doubleValue();
		double fDuration = 0.001 * (new Double(firstElement.getAttribute("d"))).doubleValue();
		double lEnd = (new Double(lastElement.getAttribute("end"))).doubleValue();
		double fStart = fEnd - fDuration; // 'prosody' tag starting point
		double duration = lEnd - fStart; // duaration of 'prosody' modification request

		Map<Integer, Integer> f0Map;

		for (int i = 0; i < nl.getLength(); i++) {

			Element e = (Element) nl.item(i);
			String f0Attribute = e.getAttribute("f0");

			if (f0Attribute == null || "".equals(f0Attribute)) {
				continue;
			}

			double phoneEndTime = (new Double(e.getAttribute("end"))).doubleValue();
			double phoneDuration = 0.001 * (new Double(e.getAttribute("d"))).doubleValue();

			Pattern p = Pattern.compile("(\\d+,\\d+)");

			// Split input with the pattern
			Matcher m = p.matcher(e.getAttribute("f0"));
			String setF0String = "";
			while (m.find()) {
				String[] f0Values = (m.group().trim()).split(",");
				Integer percent = new Integer(f0Values[0]);
				Integer f0Value = new Integer(f0Values[1]);
				double partPhone = phoneDuration * (percent.doubleValue() / 100.0);

				int placeIndex = (int) Math.floor(((((phoneEndTime - phoneDuration) - fStart) + partPhone) * 100)
						/ (double) duration);
				if (placeIndex >= 100) {
					placeIndex = 99;
				}
				setF0String = setF0String + "(" + percent + "," + (int) contour[placeIndex] + ")";

			}

			e.setAttribute("f0", setF0String);
		}
	}

	/**
	 * Set modifications to base contour (first order polynomial fit contour)
	 * 
	 * @param polyValues
	 *            polyValues
	 * @param contourAttribute
	 *            contourAttribute
	 * @param pitchAttribute
	 *            pitchAttribute
	 * @return polyValues
	 */
	private double[] setBaseContourModifications(double[] polyValues, String contourAttribute, String pitchAttribute) {

		if (pitchAttribute != null && !"".equals(pitchAttribute)) {
			polyValues = setPitchSpecifications(polyValues, pitchAttribute);
		}

		if (contourAttribute != null && !"".equals(contourAttribute)) {
			polyValues = setContourSpecifications(polyValues, contourAttribute);
		}

		return polyValues;
	}

	/**
	 * Set all specifications to original contour
	 * 
	 * @param polyValues
	 *            polyValues
	 * @param contourAttribute
	 *            contourAttribute
	 * @return modifiedF0Values
	 */
	private double[] setContourSpecifications(double[] polyValues, String contourAttribute) {

		Map<String, String> f0Specifications = getContourSpecifications(contourAttribute);
		Iterator<String> it = f0Specifications.keySet().iterator();
		double[] modifiedF0Values = new double[100];
		Arrays.fill(modifiedF0Values, 0.0);

		if (polyValues.length != modifiedF0Values.length) {
			throw new RuntimeException("The lengths of two arrays are not same!");
		}

		modifiedF0Values[0] = polyValues[0];
		modifiedF0Values[modifiedF0Values.length - 1] = polyValues[modifiedF0Values.length - 1];

		while (it.hasNext()) {

			String percent = it.next();
			String f0Value = f0Specifications.get(percent);

			int percentDuration = (new Integer(percent.substring(0, percent.length() - 1))).intValue();

			// System.out.println( percent + " " + f0Value );

			if (f0Value.startsWith("+")) {
				if (f0Value.endsWith("%")) {
					double f0Mod = (new Double(f0Value.substring(1, f0Value.length() - 1))).doubleValue();
					modifiedF0Values[percentDuration] = polyValues[percentDuration]
							+ (polyValues[percentDuration] * (f0Mod / 100.0));
				} else if (f0Value.endsWith("Hz")) {
					int f0Mod = (new Integer(f0Value.substring(1, f0Value.length() - 2))).intValue();
					modifiedF0Values[percentDuration] = polyValues[percentDuration] + f0Mod;
				}
			} else if (f0Value.startsWith("-")) {
				if (f0Value.endsWith("%")) {
					double f0Mod = (new Double(f0Value.substring(1, f0Value.length() - 1))).doubleValue();
					modifiedF0Values[percentDuration] = polyValues[percentDuration]
							- (polyValues[percentDuration] * (f0Mod / 100.0));

				} else if (f0Value.endsWith("Hz")) {
					int f0Mod = (new Integer(f0Value.substring(1, f0Value.length() - 2))).intValue();
					modifiedF0Values[percentDuration] = polyValues[percentDuration] - f0Mod;
				}
			}
		}

		modifiedF0Values = interpolateNonZeroValues(modifiedF0Values);

		return modifiedF0Values;

	}

	/**
	 * set pitch specifications: Ex: pitch="+20%" or pitch="+50Hz"
	 * 
	 * @param polyValues
	 *            polyValues
	 * @param pitchAttribute
	 *            pitchAttribute
	 * @return polyValues
	 */
	private double[] setPitchSpecifications(double[] polyValues, String pitchAttribute) {

		boolean positivePitch = pitchAttribute.startsWith("+");
		double modificationPitch = (new Integer(pitchAttribute.substring(1, pitchAttribute.length() - 1))).doubleValue();

		if (pitchAttribute.startsWith("+")) {
			if (pitchAttribute.endsWith("%")) {
				for (int i = 0; i < polyValues.length; i++) {
					polyValues[i] = polyValues[i] + (polyValues[i] * (modificationPitch / 100.0));
				}
			} else if (pitchAttribute.endsWith("Hz")) {
				for (int i = 0; i < polyValues.length; i++) {
					polyValues[i] = polyValues[i] + modificationPitch;
				}
			}
		} else if (pitchAttribute.startsWith("-")) {
			if (pitchAttribute.endsWith("%")) {
				for (int i = 0; i < polyValues.length; i++) {
					polyValues[i] = polyValues[i] - (polyValues[i] * (modificationPitch / 100.0));
				}
			} else if (pitchAttribute.endsWith("Hz")) {
				for (int i = 0; i < polyValues.length; i++) {
					polyValues[i] = polyValues[i] - modificationPitch;
				}
			}
		}

		return polyValues;

	}

	/**
	 * to get contour specifications into MAP
	 * 
	 * @param attribute
	 *            attribute
	 * @return f0Map
	 */
	private Map<String, String> getContourSpecifications(String attribute) {

		Map<String, String> f0Map = new HashMap<String, String>();
		Pattern p = Pattern.compile("(\\d+%,[+|-]\\d+[%|Hz])");

		// Split input with the pattern
		Matcher m = p.matcher(attribute);
		while (m.find()) {
			// System.out.println(m.group());
			String[] f0Values = (m.group().trim()).split(",");
			f0Map.put(f0Values[0], f0Values[1]);
		}
		return f0Map;
	}

	/**
	 * To interpolate Zero values with respect to NonZero values
	 * 
	 * @param contour
	 *            contour
	 * @return contour
	 */
	private double[] interpolateNonZeroValues(double[] contour) {

		for (int i = 0; i < contour.length; i++) {
			if (contour[i] == 0) {
				int index = findNextIndexNonZero(contour, i);
				// System.out.println("i: "+i+"index: "+index);
				if (index == -1) {
					for (int j = i; j < contour.length; j++) {
						contour[j] = contour[j - 1];
					}
					break;
				} else {
					for (int j = i; j < index; j++) {
						// contour[j] = contour[i-1] * (index - j) + contour[index] * (j - (i-1)) / ( index - i );
						if (i == 0) {
							contour[j] = contour[index];
						} else {
							contour[j] = contour[j - 1] + ((contour[index] - contour[i - 1]) / (index - i));
						}
					}
					i = index - 1;
				}
			}
		}

		return contour;
	}

	/**
	 * To find next NonZero index
	 * 
	 * @param contour
	 *            contour
	 * @param current
	 *            current
	 * @return -1
	 */
	private int findNextIndexNonZero(double[] contour, int current) {
		for (int i = current + 1; i < contour.length; i++) {
			if (contour[i] != 0) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * get Continuous contour from "ph" nodelist
	 * 
	 * @param nl
	 *            nl
	 * @return contour
	 */
	private double[] getContiniousContour(NodeList nl) {

		Element firstElement = (Element) nl.item(0);
		Element lastElement = (Element) nl.item(nl.getLength() - 1);

		double[] contour = new double[100]; // Assume contour has 100 frames
		Arrays.fill(contour, 0.0);

		double fEnd = (new Double(firstElement.getAttribute("end"))).doubleValue();
		double fDuration = 0.001 * (new Double(firstElement.getAttribute("d"))).doubleValue();
		double lEnd = (new Double(lastElement.getAttribute("end"))).doubleValue();
		double fStart = fEnd - fDuration; // 'prosody' tag starting point
		double duration = lEnd - fStart; // duaration of 'prosody' modification request

		Map<Integer, Integer> f0Map;

		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element) nl.item(i);
			String f0Attribute = e.getAttribute("f0");

			if (f0Attribute == null || "".equals(f0Attribute)) {
				continue;
			}

			double phoneEndTime = (new Double(e.getAttribute("end"))).doubleValue();
			double phoneDuration = 0.001 * (new Double(e.getAttribute("d"))).doubleValue();
			// double localStartTime = endTime - phoneDuration;

			f0Map = getPhoneF0Data(e.getAttribute("f0"));

			Iterator<Integer> it = f0Map.keySet().iterator();
			while (it.hasNext()) {
				Integer percent = it.next();
				Integer f0Value = f0Map.get(percent);
				double partPhone = phoneDuration * (percent.doubleValue() / 100.0);
				int placeIndex = (int) Math.floor(((((phoneEndTime - phoneDuration) - fStart) + partPhone) * 100)
						/ (double) duration);
				if (placeIndex >= 100) {
					placeIndex = 99;
				}
				contour[placeIndex] = f0Value.doubleValue();
			}
		}

		return contour;
	}

	/**
	 * Get f0 specifications in HashMap
	 * 
	 * @param attribute
	 *            attribute
	 * @return f0Map
	 */
	private Map<Integer, Integer> getPhoneF0Data(String attribute) {

		Map<Integer, Integer> f0Map = new HashMap<Integer, Integer>();
		Pattern p = Pattern.compile("(\\d+,\\d+)");

		// Split input with the pattern
		Matcher m = p.matcher(attribute);
		while (m.find()) {
			String[] f0Values = (m.group().trim()).split(",");
			f0Map.put(new Integer(f0Values[0]), new Integer(f0Values[1]));
		}

		// attribute.split(regex)
		return f0Map;

	}

	/**
	 * Parse Mary context features. For each triphone model in the file, it creates a Model object in a linked list of Model
	 * objects -> UttModel um It also estimates state duration from state duration model (Gaussian). For each model in the vector,
	 * the mean and variance of the DUR and LF0 are searched in the ModelSet and copied in each triphone model.
	 * 
	 * @param s
	 *            s
	 * @param um
	 *            um
	 * @param htsData
	 *            htsData
	 * @param cart
	 *            cart
	 * @throws Exception
	 *             Exception
	 */
	private String processUtt(Scanner s, HTSUttModel um, HMMData htsData, CartTreeSet cart) throws Exception {
		int i, mstate, frame, k, statesDuration, newStateDuration;
		;
		HTSModel m; /* current model, corresponds to a line in label file */
		String nextLine;
		double diffdurOld = 0.0;
		double diffdurNew = 0.0;
		float fperiodmillisec = ((float) htsData.getFperiod() / (float) htsData.getRate()) * 1000;
		float fperiodsec = ((float) htsData.getFperiod() / (float) htsData.getRate());
		Integer dur;
		boolean firstPh = true;
		boolean lastPh = false;

		Float durSec;
		Integer numLab = 0;
		FeatureVector fv;
		FeatureDefinition feaDef = htsData.getFeatureDefinition();

		/* Skip mary context features definition */
		while (s.hasNext()) {
			nextLine = s.nextLine();
			if (nextLine.trim().equals(""))
				break;
		}
		/* skip until byte values */
		int numLines = 0;
		while (s.hasNext()) {
			nextLine = s.nextLine();
			if (nextLine.trim().equals(""))
				break;
			numLines++;
		}

		/* Parse byte values */
		i = 0;
		while (s.hasNext()) {
			nextLine = s.nextLine();
			// System.out.println("STR: " + nextLine);

			fv = feaDef.toFeatureVector(0, nextLine);
			um.addUttModel(new HTSModel(cart.getNumStates()));
			m = um.getUttModel(i);
			/* this function also sets the phone name, the phone between - and + */
			m.setPhoneName(fv.getFeatureAsString(feaDef.getFeatureIndex("phone"), feaDef));

			if (!(s.hasNext()))
				lastPh = true;

			// Determine state-level duration
			// Estimate state duration from state duration model (Gaussian)
			diffdurNew = cart.searchDurInCartTree(m, fv, htsData, firstPh, lastPh, diffdurOld);
			um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());

			// Set realised durations in model
			m.setTotalDurMillisec((int) (fperiodmillisec * m.getTotalDur()));
			diffdurOld = diffdurNew;
			durSec = um.getTotalFrame() * fperiodsec;

			numLab++;
			dur = m.getTotalDurMillisec();
			um.concatRealisedAcoustParams(m.getPhoneName() + " " + dur.toString() + "\n");
			// System.out.println("phone=" + m.getPhoneName() + " dur=" + m.getTotalDur() +" durTotal=" + um.getTotalFrame() );

			/*
			 * Find pdf for LF0, this function sets the pdf for each state. here the model (phone) is defined as voiced or
			 * unvoiced.
			 */
			cart.searchLf0InCartTree(m, fv, feaDef, htsData.getUV());

			/* increment number of models in utterance model */
			um.setNumModel(um.getNumModel() + 1);
			/* update number of states */
			um.setNumState(um.getNumState() + cart.getNumStates());
			i++;

			if (firstPh)
				firstPh = false;
		}

		for (i = 0; i < um.getNumUttModel(); i++) {
			m = um.getUttModel(i);
			for (mstate = 0; mstate < cart.getNumStates(); mstate++)
				for (frame = 0; frame < m.getDur(mstate); frame++)
					if (m.getVoiced(mstate))
						um.setLf0Frame(um.getLf0Frame() + 1);
			// System.out.println("Vector m[" + i + "]=" + m.getPhoneName() );
		}

		return um.getRealisedAcoustParams();

	} /* method _ProcessUtt */

	/***
	 * Generate F0 values for voiced frames out of HMMs
	 * 
	 * @param um
	 *            HTSUttModel, linked list of model objects
	 * @param htsData
	 *            HMMData
	 * @return f0Values
	 * @throws Exception
	 *             Exception
	 */
	public String HmmF0Generation(HTSUttModel um, HMMData htsData) throws Exception {

		int frame, uttFrame, lf0Frame;
		int hmmState, k, n, i;
		boolean nobound;
		HTSModel m;
		HTSPStream lf0Pst = null;
		boolean voiced[];
		CartTreeSet ms = htsData.getCartTreeSet();

		/* for lf0 count just the number of lf0frames that are voiced or non-zero */
		lf0Pst = new HTSPStream(ms.getLf0Stream(), um.getLf0Frame(), HMMData.FeatureType.LF0, 200);

		uttFrame = lf0Frame = 0;
		voiced = new boolean[um.getTotalFrame()];

		for (i = 0; i < um.getNumUttModel(); i++) {
			m = um.getUttModel(i);
			for (hmmState = 0; hmmState < ms.getNumStates(); hmmState++)
				for (frame = 0; frame < m.getDur(hmmState); frame++) {
					voiced[uttFrame] = m.getVoiced(hmmState);
					uttFrame++;
					if (m.getVoiced(hmmState))
						lf0Frame++;
				}
		}

		uttFrame = 0;
		lf0Frame = 0;
		/* copy pdfs */
		for (i = 0; i < um.getNumUttModel(); i++) {
			m = um.getUttModel(i);
			for (hmmState = 0; hmmState < ms.getNumStates(); hmmState++) {
				for (frame = 0; frame < m.getDur(hmmState); frame++) {

					// System.out.println("uttFrame=" + uttFrame + "  phone frame=" + frame + "  phone hmmState=" + hmmState);
					/* copy pdfs for lf0 */
					for (k = 0; k < ms.getLf0Stream(); k++) {
						int lw = lf0Pst.getDWLeftBoundary(k);
						int rw = lf0Pst.getDWRightBoundary(k);
						nobound = true;
						/* check if current frame is voiced/unvoiced boundary or not */
						for (n = lw; n <= rw; n++)
							if ((uttFrame + n) <= 0 || um.getTotalFrame() <= (uttFrame + n))
								nobound = false;
							else
								nobound = (nobound && voiced[uttFrame + n]);
						/* copy pdfs */
						if (voiced[uttFrame]) {
							lf0Pst.setMseq(lf0Frame, k, m.getLf0Mean(hmmState, k));
							if (nobound || k == 0)
								lf0Pst.setIvseq(lf0Frame, k, HTSParameterGeneration.finv(m.getLf0Variance(hmmState, k)));
							else
								/* the variances for dynamic feature are set to inf on v/uv boundary */
								lf0Pst.setIvseq(lf0Frame, k, 0.0);
						}
					}
					if (voiced[uttFrame])
						lf0Frame++;
					uttFrame++;
				} /* for each frame in this hmmState */
			} /* for each hmmState in this model */
		} /* for each model in this utterance */

		// System.out.println("After copying pdfs to PStreams uttFrame=" + uttFrame + " lf0frame=" + lf0Frame);
		// System.out.println("mseq[" + uttFrame + "][" + k + "]=" + mceppst.get_mseq(uttFrame, k) + "   " +
		// m.get_mcepmean(hmmState, k));

		double f0s[] = new double[voiced.length];
		i = 0;
		if (lf0Frame > 0) {
			logger.info("Parameter generation for LF0: ");
			lf0Pst.mlpg(htsData, htsData.getUseGV());
			for (int t = 0; t < voiced.length; t++) {
				if (voiced[t]) {
					f0s[t] = Math.exp(lf0Pst.getPar(i, 0));
					// f0s[t] = lf0Pst.getPar(i,0);
					i++;
				} else
					f0s[t] = 0.0;
				// System.out.println("GEN f0s[" + t + "]=" + f0s[t]);
			}
		}

		double totalDur;
		int totalFrames;
		String f0Values = "";
		int t = 0; // total number of frames voiced and unvoiced
		for (i = 0; i < um.getNumUttModel(); i++) {
			m = um.getUttModel(i);
			f0Values += m.getPhoneName() + " " + m.getTotalDurMillisec() + " ";
			// System.out.println(m.getPhoneName() + " dur=" + m.getTotalDurMillisec() + " No. frames=" + m.getTotalDur());
			totalDur = m.getTotalDur();
			totalFrames = 0;
			/**
			 * Here I need to check if the phone, or model is voiced or not. A model has five states and each state can be voiced
			 * or unvoiced, normally if the phone is voiced the majority of the states should be voiced
			 */
			if (checkModelVoiced(m, ms.getNumStates())) // if the majority of the model states are voiced
			{
				for (int j = 0; j < ms.getNumStates(); j++) {
					// System.out.print("  state=" + j);

					for (frame = 0; frame < m.getDur(j); frame++) {
						totalFrames++;
						// System.out.format("(%d frame=%d=%.2f ) %.2f ", t, totalFrames, (totalFrames/totalDur)*100, f0s[t]);
						if (f0s[t] > 0.0) // there are some phoneme states that might contain voiced and unvoiced frames, the
											// unvoiced frames have f0=0.0
							f0Values += "(" + Integer.toString((int) ((totalFrames / totalDur) * 100)) + ","
									+ Integer.toString((int) f0s[t]) + ")";
						t++;
					} // for each frame in this hmmState
						// System.out.println();
				} // for each hmmState in this model

			} else { // if the majority of the model states are unvoiced
				t = t + m.getTotalDur();
				f0Values += "0";
			}

			f0Values += "\n";

		} // for each model in this utterance

		// System.out.println(f0Values);
		return (f0Values);

	} /* method HmmF0Generation */

	/***
	 * Set durations
	 * 
	 * @param tw
	 *            tw
	 * @param durations
	 *            durations
	 * @throws SynthesisException
	 *             SynthesisException
	 */
	public void setActualDurations(TreeWalker tw, String durations) throws SynthesisException {
		int i, j, index;
		NodeList no1, no2;
		NamedNodeMap att;
		Scanner s = null;
		Vector<String> ph = new Vector<String>();
		Vector<Integer> dur = new Vector<Integer>(); // individual durations, in millis
		String line, str[];
		float totalDur = 0f; // total duration, in seconds

		s = new Scanner(durations).useDelimiter("\n");
		while (s.hasNext()) {
			line = s.next();
			str = line.split(" ");
			// --- not needed ph.add(PhoneTranslator.replaceBackTrickyPhones(str[0]));
			ph.add(str[0]);
			dur.add(Integer.valueOf(str[1]));
		}
		/* the duration of the first phone includes the duration of the initial pause */
		if (dur.size() > 1 && ph.get(0).contentEquals("_")) {
			dur.set(1, (dur.get(1) + dur.get(0)));
			ph.set(0, "");
			/* remove this element of the vector otherwise next time it will return the same */
			ph.set(0, "");
		}

		Element e;
		tw.setCurrentNode(tw.getRoot());
		while ((e = (Element) tw.nextNode()) != null) {
			// System.out.println("TAG: " + e.getTagName() + " LocalName=" + e.getLocalName() + " NodeName=" + e.getNodeName());
			if (e.getTagName().equals(MaryXML.PHONE)) {
				Element phone = e;
				String p = phone.getAttribute("p");
				index = ph.indexOf(p);
				int currentDur = dur.elementAt(index);
				totalDur += currentDur * 0.001f;
				phone.setAttribute("d", String.valueOf(currentDur));
				phone.setAttribute("end", String.valueOf(totalDur));
				// remove this element of the vector otherwise next time it will return the same
				ph.set(index, "");
			} else if (e.getTagName().contentEquals(MaryXML.BOUNDARY)) {
				int breakindex = 0;
				try {
					breakindex = Integer.parseInt(e.getAttribute("breakindex"));
				} catch (NumberFormatException nfe) {
				}
				if (e.hasAttribute("duration") || breakindex >= 3) {
					index = ph.indexOf("_");
					int currentDur = dur.elementAt(index);
					totalDur += currentDur * 0.001f;
					e.setAttribute("duration", String.valueOf(currentDur));
					// remove this element of the vector otherwise next time it will return the same
					ph.set(index, "");
				}
			} // else ignore whatever other label...

		}
	}

	/***
	 * Set durations and f0 values The meaning of f0="(X,Y)" is: at X% of the phone duration, the F0 value is Y Hz.
	 * 
	 * @param tw
	 *            treewalker
	 * @param durF0s
	 *            String containing in each line one phoneme its duration and its F0 values if it is voiced or 0 if it is unvoiced
	 * @throws SynthesisException
	 *             SynthesisException
	 */
	public void setActualDurationsAndF0s(TreeWalker tw, String durF0s) throws SynthesisException {
		int i, j, index;
		NodeList no1, no2;
		NamedNodeMap att;
		Scanner s = null;
		Vector<String> ph = new Vector<String>();
		Vector<Integer> dur = new Vector<Integer>(); // individual durations, in millis
		Vector<String> f0 = new Vector<String>();
		String line, str[];
		float totalDur = 0f; // total duration, in seconds

		s = new Scanner(durF0s).useDelimiter("\n");
		while (s.hasNext()) {
			line = s.next();
			str = line.split(" ");
			// --- not needed ph.add(PhoneTranslator.replaceBackTrickyPhones(str[0]));
			ph.add(str[0]);
			dur.add(Integer.valueOf(str[1]));
			f0.add(str[2]);
		}
		/* the duration of the first phone includes the duration of the initial pause */
		if (dur.size() > 1 && ph.get(0).contentEquals("_")) {
			dur.set(1, (dur.get(1) + dur.get(0)));
			ph.set(0, "");
			/* remove this element of the vector otherwise next time it will return the same */
			ph.set(0, "");
		}

		String f0IniMidEndStr;
		int numPh = 1; // because the first one is _ (sil)
		Element e;
		tw.setCurrentNode(tw.getRoot());
		while ((e = (Element) tw.nextNode()) != null) {
			// System.out.println("TAG: " + e.getTagName() + " LocalName=" + e.getLocalName() + " NodeName=" + e.getNodeName());
			if (e.getTagName().equals(MaryXML.PHONE)) {
				numPh++;

				Element phone = e;
				String p = phone.getAttribute("p");
				index = ph.indexOf(p);
				int currentDur = dur.elementAt(index);
				String currentF0 = f0.elementAt(index);
				totalDur += currentDur * 0.001f;
				phone.setAttribute("d", String.valueOf(currentDur));
				phone.setAttribute("end", String.valueOf(totalDur));
				if (!currentF0.contentEquals("0"))
					phone.setAttribute("f0", currentF0);
				// remove this element of the vector otherwise next time it will return the same
				ph.set(index, "");
			} else if (e.getTagName().contentEquals(MaryXML.BOUNDARY)) {
				int breakindex = 0;
				try {
					breakindex = Integer.parseInt(e.getAttribute("breakindex"));
				} catch (NumberFormatException nfe) {
				}
				if (e.hasAttribute("duration") || breakindex >= 3) {
					index = ph.indexOf("_");
					int currentDur = dur.elementAt(index);
					totalDur += currentDur * 0.001f;
					e.setAttribute("duration", String.valueOf(currentDur));
					// remove this element of the vector otherwise next time it will return the same
					ph.set(index, "");
				}
			} // else ignore whatever other label...

		}
	}

	private boolean checkModelVoiced(HTSModel m, int numStates) {
		int numVoiced = 0;
		int numUnvoiced = 0;
		for (int i = 0; i < numStates; i++) {
			if (m.getVoiced(i))
				numVoiced++;
			else
				numUnvoiced++;
		}
		if (numVoiced >= numUnvoiced) {
			// System.out.println(m.getPhoneName() + " is voiced" + "(" + numVoiced + ":" + numUnvoiced + ")");
			return true;
		} else {
			// System.out.println(m.getPhoneName() + " is unvoiced" + "(" + numVoiced + ":" + numUnvoiced + ")");
			return false;
		}

	}

}
