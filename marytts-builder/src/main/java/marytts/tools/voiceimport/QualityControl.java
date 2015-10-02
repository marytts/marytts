/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.filter.BandPassFilter;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * Quality Control Component for Voice Import Tool to perform 'Sensibility check' on Data. It identifies some suspicious labels in
 * label files generated from Automatic Labeling
 * 
 * @author Sathish Chandra Pammi
 *
 */

public class QualityControl extends VoiceImportComponent {

	private DatabaseLayout db;
	private int progress;

	protected String featsExt = ".pfeats";
	protected String labExt = ".lab";
	private PrintWriter outFileWriter;
	private PrintWriter priorityFileWriter;
	private Map fricativeThresholds;
	private ArrayList silenceEnergyList;
	private double sileceThreshold;
	private TreeMap allProblems;
	private TreeMap priorityProblems;
	private String unit;
	private String baseN;

	public final String FEATUREDIR = "QualityControl.featureDir";
	public final String LABELDIR = "QualityControl.labelDir";
	public final String OUTFILE = "QualityControl.outputFile";
	public final String PRIORFILE = "QualityControl.outPriorityFile";
	public final String MLONGPHN = "QualityControl.markUnusuallyLongPhone";
	public final String MHSILEGY = "QualityControl.markHighSILEnergy";
	public final String MHFREQEGY = "QualityControl.markFricativeHighFreqEnergy";
	public final String MUNVOICEDVOWEL = "QualityControl.markUnvoicedVowel";

	public final String getName() {
		return "QualityControl";
	}

	@Override
	protected void initialiseComp() {
		File unitfeatureDir = new File(getProp(FEATUREDIR));
		if (!unitfeatureDir.exists()) {
			throw new Error(FEATUREDIR + " " + getProp(FEATUREDIR) + " does not exist; ");
		}
		File unitlabelDir = new File(getProp(LABELDIR));
		if (!unitlabelDir.exists()) {
			throw new Error(LABELDIR + " " + getProp(LABELDIR) + " does not exist; ");
		}

	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + System.getProperty("file.separator"));
			props.put(LABELDIR, db.getProp(db.ROOTDIR) + "phonelab" + System.getProperty("file.separator"));
			props.put(OUTFILE, db.getProp(db.ROOTDIR) + "QualityControl_Problems.txt");
			props.put(PRIORFILE, db.getProp(db.ROOTDIR) + "QualityControl_Priority.txt");
			props.put(MLONGPHN, "true");
			props.put(MHSILEGY, "true");
			props.put(MHFREQEGY, "true");
			props.put(MUNVOICEDVOWEL, "true");

		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(FEATUREDIR, "directory containing the phone features.");
		props2Help.put(LABELDIR, "directory containing the phone labels");
		props2Help.put(OUTFILE, "Output file which shows suspicious alignments");
		props2Help.put(PRIORFILE, "Output file which shows sorted suspicious aligned basenames according to a priority");
		props2Help.put(MLONGPHN, "if true, Mark Unusually long Phone");
		props2Help.put(MHSILEGY, "if true, Mark Higher Silence Energy");
		props2Help.put(MHFREQEGY, "if true, Mark High-Frequency Energy for a Fricative is very low");
		props2Help.put(MUNVOICEDVOWEL, "if true, Unvoiced Vowels");

	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */

	public boolean compute() throws Exception {

		String wavDir = db.getProp(db.WAVDIR);
		String voiceName = db.getProp(db.VOICENAME);
		progress = 0;
		int bnlLengthIn = bnl.getLength();
		System.out.println("Searching for Suspicious Alignments (Labels) in " + bnlLengthIn + " utterances....");
		Map fricativeHash = createHashMaps();
		if (getProp(MHFREQEGY).equals("true")) {
			fricativeThresholds = getFricativeThresholds(fricativeHash);
		}
		if (getProp(MHSILEGY).equals("true")) {
			sileceThreshold = getSilenceThreshold();
		}

		allProblems = new TreeMap();
		priorityProblems = new TreeMap();

		for (int i = 0; i < bnl.getLength(); i++) {
			progress = 50 + (50 * i / bnl.getLength());
			findSuspiciousAlignments(bnl.getName(i));
		}
		writeProblemstoFile();
		writePrioritytoFile();

		System.out.println("Identified Suspicious Alignments (Labels) written into " + getProp(OUTFILE) + " file.");
		System.out.println("A List of base names sorted according to an associated cost value and written into  "
				+ getProp(OUTFILE) + " file, Which will be useful for Manual Correction.");
		System.out.println(".... Done.");
		return true;
	}

	/**
	 * Take Each Base File and identifies any suspicious-alignments
	 * 
	 * @param basename
	 *            basename
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */

	private void findSuspiciousAlignments(String basename) throws IOException, Exception {

		BufferedReader labels;
		BufferedReader features;
		String wavDir = db.getProp(db.WAVDIR);
		String voiceName = db.getProp(db.VOICENAME);
		int cost = 0;

		AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavDir + "/" + basename + ".wav"));

		if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
		}
		float samplingRate = ais.getFormat().getSampleRate();
		double[] signal = new AudioDoubleDataSource(ais).getAllData();

		labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getProp(LABELDIR) + basename + labExt)),
				"UTF-8"));
		features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getProp(FEATUREDIR) + basename
				+ featsExt)), "UTF-8"));

		String line;
		int ph_VC_idx = -1; // Vowel-Consonent Index
		int ph_Ctype_idx = -1;

		// Skip label file header:
		while ((line = labels.readLine()) != null) {
			if (line.startsWith("#"))
				break; // line starting with "#" marks end of header
		}
		// Skip features file header:
		for (int lineCount = 0; (line = features.readLine()) != null; lineCount++) {

			if (line.startsWith("ph_vc")) {
				ph_VC_idx = lineCount - 1;
			}
			if (line.startsWith("ph_ctype")) {
				ph_Ctype_idx = lineCount - 1;
			}

			if (line.trim().equals(""))
				break; // empty line marks end of header
		}

		boolean correct = true;

		double startTimeStamp = 0.0;
		double endTimeStamp = 0.0;
		boolean isFricative = false;
		int unitIndex = 0;
		String labelUnit;

		while (correct) {
			line = labels.readLine();
			labelUnit = null;
			if (line != null) {
				List labelUnitData = getLabelUnitData(line);
				labelUnit = (String) labelUnitData.get(2);
				unitIndex = Integer.parseInt((String) labelUnitData.get(1));
				endTimeStamp = Double.parseDouble((String) labelUnitData.get(0));
			}
			line = features.readLine();
			String featureUnit = getFeatureUnit(line);
			if (featureUnit == null)
				throw new IOException("Incomplete feature file: " + basename);
			// when featureUnit is the empty string, we have found an empty line == end of feature section
			if ("".equals(featureUnit) && labelUnit == null) {
				// we have reached the end in both labels and features
				break;
			}
			if (!featureUnit.equals(labelUnit)) {
				// label and feature unit do not match
				System.err.println("Non-matching units found: feature unit '" + featureUnit + "' vs. label unit '" + labelUnit
						+ "' ( in basename: " + basename + ")");
			}

			baseN = basename;
			unit = labelUnit;
			double phoneDuration = endTimeStamp - startTimeStamp;
			String currentProblem = "";
			if (phoneDuration > 1 && !labelUnit.equals("_") && getProp(MLONGPHN).equals("true")) {
				currentProblem = labelUnit + "\t" + startTimeStamp + "\t" + endTimeStamp + "\tUnusually Long Phone";
				cost += 4;
			}
			if (isFricative(line, ph_Ctype_idx) && phoneDuration > 0 && getProp(MHFREQEGY).equals("true")) {
				boolean isFHEnergy = isFricativeHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp, labelUnit);
				if (!isFHEnergy) {
					currentProblem = labelUnit + "\t" + startTimeStamp + "\t" + endTimeStamp
							+ "\tFricative High-Frequency Energy is very low";
					cost += 3;
				}
			}
			if (labelUnit.equals("_") && phoneDuration > 0 && getProp(MHSILEGY).equals("true")) {
				boolean isSILHEnergy = isSilenceHighEnergy(signal, samplingRate, startTimeStamp, endTimeStamp);
				if (isSILHEnergy) {
					currentProblem = labelUnit + "\t" + startTimeStamp + "\t" + endTimeStamp + "\tHigherEnergy for a Silence";
					cost += 1;
				}
			}
			if (isVowel(line, ph_VC_idx) && phoneDuration > 0 && getProp(MUNVOICEDVOWEL).equals("true")) {
				boolean isVV = isVowelVoiced(signal, samplingRate, startTimeStamp, endTimeStamp);
				if (!isVV) {
					currentProblem = labelUnit + "\t" + startTimeStamp + "\t" + endTimeStamp + "\tUn-Voiced Vowel";
					cost += 2;
				}
			}

			if (!"".equals(currentProblem)) {
				if (allProblems.containsKey(basename)) {
					ArrayList arrList = (ArrayList) allProblems.get(basename);
					arrList.add(currentProblem);
					allProblems.put(basename, arrList);
				} else {
					ArrayList arrList = new ArrayList();
					arrList.add(currentProblem);
					allProblems.put(basename, arrList);
				}
				if (priorityProblems.containsKey(basename)) {
					Integer problemCost = (Integer) priorityProblems.get(basename);
					problemCost = problemCost + cost;
					priorityProblems.put(basename, problemCost);
				} else {
					Integer problemCost = new Integer(cost);
					// problemCost = 0;
					priorityProblems.put(basename, problemCost);
				}
			}

			startTimeStamp = endTimeStamp;
		}
		labels.close();
		features.close();
	}

	/**
	 * It helps to calculate Thresholds by storing all Energy values in to hash.
	 * 
	 * @return fricativeHash
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	private Map createHashMaps() throws IOException, Exception {

		Map fricativeHash = new HashMap();
		silenceEnergyList = new ArrayList();

		for (int baseCnt = 0; baseCnt < bnl.getLength(); baseCnt++) {

			progress = (50 * baseCnt / bnl.getLength());
			String baseName = bnl.getName(baseCnt);
			BufferedReader labels;
			BufferedReader features;
			String wavDir = db.getProp(db.WAVDIR);
			String voiceName = db.getProp(db.VOICENAME);

			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavDir + "/" + baseName + ".wav"));

			if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
				ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
			}

			float samplingRate = ais.getFormat().getSampleRate();
			double[] signal = new AudioDoubleDataSource(ais).getAllData();

			labels = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(getProp(LABELDIR) + baseName + labExt)), "UTF-8"));
			features = new BufferedReader(new InputStreamReader(new FileInputStream(new File(getProp(FEATUREDIR) + baseName
					+ featsExt)), "UTF-8"));

			String line;
			int ph_VC_idx = -1; // Vowel-Consonent Index
			int ph_Ctype_idx = -1;

			// Skip label file header:
			while ((line = labels.readLine()) != null) {
				if (line.startsWith("#"))
					break; // line starting with "#" marks end of header
			}
			// Skip features file header:
			for (int lineCount = 0; (line = features.readLine()) != null; lineCount++) {

				if (line.startsWith("ph_ctype")) {
					ph_Ctype_idx = lineCount - 1;
				}

				if (line.trim().equals(""))
					break; // empty line marks end of header
			}

			boolean correct = true;
			double startTimeStamp = 0.0;
			double endTimeStamp = 0.0;
			boolean isFricative = false;
			int unitIndex = 0;
			String labelUnit;

			while (correct) {

				line = labels.readLine();
				labelUnit = null;
				if (line != null) {
					List labelUnitData = getLabelUnitData(line);
					labelUnit = (String) labelUnitData.get(2);
					unitIndex = Integer.parseInt((String) labelUnitData.get(1));
					endTimeStamp = Double.parseDouble((String) labelUnitData.get(0));
				}

				line = features.readLine();
				String featureUnit = getFeatureUnit(line);
				if (featureUnit == null)
					throw new IOException("Incomplete feature file: " + baseName);
				// when featureUnit is the empty string, we have found an empty line == end of feature section
				if ("".equals(featureUnit) && labelUnit == null) {
					// we have reached the end in both labels and features
					break;
				}
				if (!featureUnit.equals(labelUnit)) {
					// label and feature unit do not match
					System.err.println("Non-matching units found: feature file '" + featureUnit + "' vs. label file '"
							+ labelUnit + "' (in Basename: " + baseName + " Unit: " + labelUnit + " )");
				}

				if (isFricative(line, ph_Ctype_idx) && getProp(MHFREQEGY).equals("true")) {

					double fricEnergy = getFricativeEnergy(signal, samplingRate, startTimeStamp, endTimeStamp, labelUnit);
					if (fricativeHash.containsKey(featureUnit)) {
						ArrayList arrList = (ArrayList) fricativeHash.get(featureUnit);
						arrList.add(new Double(fricEnergy));
						fricativeHash.put(featureUnit, arrList);
					} else {
						ArrayList arrList = new ArrayList();
						arrList.add(new Double(fricEnergy));
						fricativeHash.put(featureUnit, arrList);
					}
				}
				double phoneDuration = endTimeStamp - startTimeStamp;
				if (labelUnit.equals("_") && phoneDuration > 0 && getProp(MHSILEGY).equals("true")) {
					double silEnergy = getSilenceEnergy(signal, samplingRate, startTimeStamp, endTimeStamp);
					silenceEnergyList.add(new Double(silEnergy));
				}
			}

			features.close();
			labels.close();
			ais.close();
		}

		return fricativeHash;
	}

	/**
	 * Create a HashMap which contains indivisual fricative Thresholds
	 * 
	 * @param fricativeHash
	 *            fricativeHash
	 * @return HashMap which contains indivisual fricative Thresholds
	 */
	private Map getFricativeThresholds(Map fricativeHash) {

		Map hashThresholds = new HashMap();

		for (Iterator it = fricativeHash.entrySet().iterator(); it.hasNext();) {
			Map.Entry e = (Map.Entry) it.next();
			ArrayList arr = (ArrayList) e.getValue();
			double[] arrVal = listToArray(arr);
			double meanVal = MathUtils.mean(arrVal);
			double stDev = MathUtils.standardDeviation(arrVal, 1);

			Double threshold = (Double) (meanVal - (1.5 * stDev));
			/*
			 * if(threshold.doubleValue() < 0) threshold = (Double) (meanVal - (1.5 * stDev));
			 */

			hashThresholds.put((String) e.getKey(), (Double) threshold);
		}

		return hashThresholds;
	}

	/**
	 * Calculating Silence Energy Threshold Level
	 * 
	 * @return Silence Threshold
	 */
	private double getSilenceThreshold() {

		double[] arrVal = listToArray(silenceEnergyList);
		double meanVal = MathUtils.mean(arrVal);
		double stDev = MathUtils.standardDeviation(arrVal, 1);

		return (meanVal + (1.5 * stDev));
	}

	/**
	 * Identifies If Silence has more Energy
	 * 
	 * @param signal
	 *            signal
	 * @param samplingRate
	 *            samplingRate
	 * @param startTimeStamp
	 *            startTimeStamp
	 * @param endTimeStamp
	 *            endTimeStamp
	 * @return true if silence segment more energy, else false
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */

	private boolean isSilenceHighEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
			throws IOException, Exception {
		boolean isSILHEnergy = false;
		float duration = signal.length / samplingRate;
		double phoneDur = endTimeStamp - startTimeStamp;
		int segmentStartIndex = (int) (startTimeStamp * samplingRate);
		int segmentEndIndex = (int) (endTimeStamp * samplingRate);

		if (segmentEndIndex > signal.length) {
			segmentEndIndex = signal.length;
		}
		int segmentSize = segmentEndIndex - segmentStartIndex;
		double[] phoneSegment = new double[segmentSize];
		System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
		double silenceEnergy = SignalProcUtils.getEnergy(phoneSegment);
		if (silenceEnergy > sileceThreshold)
			isSILHEnergy = true;

		return isSILHEnergy;
	}

	/**
	 * Calculate Silence Energy
	 * 
	 * @param signal
	 *            signal
	 * @param samplingRate
	 *            samplingRate
	 * @param startTimeStamp
	 *            startTimeStamp
	 * @param endTimeStamp
	 *            endTimeStamp
	 * @return SignalProcUtils.getEnergy(phoneSegment)
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	private double getSilenceEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
			throws IOException, Exception {
		boolean isSILHEnergy = false;
		float duration = signal.length / samplingRate;
		double phoneDur = endTimeStamp - startTimeStamp;
		int segmentStartIndex = (int) (startTimeStamp * samplingRate);
		int segmentEndIndex = (int) (endTimeStamp * samplingRate);
		if (segmentEndIndex > signal.length) {
			segmentEndIndex = signal.length;
		}
		int segmentSize = segmentEndIndex - segmentStartIndex;
		double[] phoneSegment = new double[segmentSize];
		System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);
		return SignalProcUtils.getEnergy(phoneSegment);
	}

	/**
	 * Writing all suspicious labels to a File
	 * 
	 * @throws IOException
	 *             IOException
	 */
	private void writeProblemstoFile() throws IOException {

		outFileWriter = new PrintWriter(new FileWriter(new File(getProp(OUTFILE))));

		for (int i = 0; i < bnl.getLength(); i++) {
			String baseName = bnl.getName(i);
			if (allProblems.containsKey(baseName)) {
				// outFileWriter.println(baseName);
				ArrayList arrList = (ArrayList) allProblems.get(baseName);
				for (Iterator it = arrList.iterator(); it.hasNext();) {
					String eachProblem = (String) it.next();
					outFileWriter.println(baseName + "\t" + eachProblem);

				}
				// outFileWriter.println("");
			}
		}
		outFileWriter.flush();
		outFileWriter.close();
	}

	/**
	 * Writing all priority problems to a file
	 * 
	 * @throws IOException
	 *             IOException
	 */
	private void writePrioritytoFile() throws IOException {

		priorityFileWriter = new PrintWriter(new FileWriter(new File(getProp(PRIORFILE))));

		TreeSet set = new TreeSet(new Comparator() {
			public int compare(Object obj, Object obj1) {
				int vcomp = ((Comparable) ((Map.Entry) obj1).getValue()).compareTo(((Map.Entry) obj).getValue());
				if (vcomp != 0)
					return vcomp;
				else
					return ((Comparable) ((Map.Entry) obj).getKey()).compareTo(((Map.Entry) obj1).getKey());
			}
		});

		set.addAll(priorityProblems.entrySet());
		for (Iterator i = set.iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry) i.next();
			priorityFileWriter.println(entry.getKey() + "\t" + entry.getValue());
			// System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
		priorityFileWriter.flush();
		priorityFileWriter.close();
	}

	/**
	 * Identifies if Fricative has more higher frequency Energy
	 * 
	 * @param signal
	 *            signal
	 * @param samplingRate
	 *            samplingRate
	 * @param startTimeStamp
	 *            startTimeStamp
	 * @param endTimeStamp
	 *            endTimeStamp
	 * @param unitName
	 *            unitName
	 * @return true if the segment more energy in higher freq. region, else false
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	private boolean isFricativeHighEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp,
			String unitName) throws IOException, Exception {

		boolean isFHighEnergy = true;
		float duration = signal.length / samplingRate;
		double phoneDur = endTimeStamp - startTimeStamp;
		int segmentStartIndex = (int) (startTimeStamp * samplingRate);
		int segmentEndIndex = (int) (endTimeStamp * samplingRate);

		if (segmentEndIndex > signal.length) {
			segmentEndIndex = signal.length;
		}

		int segmentSize = segmentEndIndex - segmentStartIndex;

		double[] phoneSegment = new double[segmentSize];

		System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);

		BandPassFilter filter = new BandPassFilter(0.25, 0.49);
		double[] highFreqSamples = filter.apply(phoneSegment);

		double higherFreqEnergy = SignalProcUtils.getEnergy(highFreqSamples);

		Double cutofEnergy = (Double) fricativeThresholds.get(unitName);
		// System.out.println("High Freq. Energy :  "+ unitName + " Energy : "+ higherFreqEnergy + "-- Threshold : "+
		// cutofEnergy.doubleValue());
		if (higherFreqEnergy < cutofEnergy.doubleValue())
			isFHighEnergy = false;

		return isFHighEnergy;
	}

	/**
	 * To get Fricative High-Freq Energy
	 * 
	 * @param signal
	 *            signal
	 * @param samplingRate
	 *            samplingRate
	 * @param startTimeStamp
	 *            startTimeStamp
	 * @param endTimeStamp
	 *            endTimeStamp
	 * @param unitName
	 *            unitName
	 * @return Fricative High-Freq Energy
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */
	private double getFricativeEnergy(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp,
			String unitName) throws IOException, Exception {

		float duration = signal.length / samplingRate;
		double phoneDur = endTimeStamp - startTimeStamp;
		int segmentStartIndex = (int) (startTimeStamp * samplingRate);
		int segmentEndIndex = (int) (endTimeStamp * samplingRate);

		if (segmentEndIndex > signal.length) {
			segmentEndIndex = signal.length;
		}

		int segmentSize = segmentEndIndex - segmentStartIndex;

		double[] phoneSegment = new double[segmentSize];

		System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);

		BandPassFilter filter = new BandPassFilter(0.25, 0.49);
		double[] highFreqSamples = filter.apply(phoneSegment);

		double higherFreqEnergy = SignalProcUtils.getEnergy(highFreqSamples);

		return higherFreqEnergy;
	}

	/**
	 * Identifies The Given Segment is Voiced or Non-Voiced
	 * 
	 * @param signal
	 *            signal
	 * @param samplingRate
	 *            samplingRate
	 * @param startTimeStamp
	 *            startTimeStamp
	 * @param endTimeStamp
	 *            endTimeStamp
	 * @return true if the segment is Voiced, else false
	 * @throws IOException
	 *             IOException
	 * @throws Exception
	 *             Exception
	 */

	private boolean isVowelVoiced(double[] signal, float samplingRate, double startTimeStamp, double endTimeStamp)
			throws IOException, Exception {

		boolean isVoiced = true;
		float duration = signal.length / samplingRate;
		double phoneDur = endTimeStamp - startTimeStamp;
		int segmentStartIndex = (int) (startTimeStamp * samplingRate);
		int segmentEndIndex = (int) (endTimeStamp * samplingRate);

		if (segmentEndIndex > signal.length) {
			segmentEndIndex = signal.length;
		}

		int segmentSize = segmentEndIndex - segmentStartIndex;

		double[] phoneSegment = new double[segmentSize];

		System.arraycopy(signal, segmentStartIndex, phoneSegment, 0, segmentSize);

		// isVoiced = SignalProcUtils.getVoicing(phoneSegment, (int)samplingRate);
		int noFrames = 1, noVoicedFrames = 0;
		int frameSize = (int) (Double.valueOf(db.getProp(db.SAMPLINGRATE).trim()).intValue() * 0.020f); // 20 ms
		for (int i = 0; i < (phoneSegment.length - frameSize); i += frameSize) {
			double[] frameSegment = new double[frameSize];
			System.arraycopy(phoneSegment, i, frameSegment, 0, frameSize);
			boolean isFrameVoiced = SignalProcUtils.getVoicing(phoneSegment, (int) samplingRate, 0.22f);
			if (isFrameVoiced) {
				noVoicedFrames++;
			}
			noFrames++;
		}
		double normalisedVoice = (double) noVoicedFrames / (double) noFrames;
		if (normalisedVoice < 0.22 && noFrames > 3) {
			// System.out.println("Basename: "+baseN+" Unit: "+unit+" "+startTimeStamp+" "+endTimeStamp+" No. of Frames: "+noFrames+" No. of Voiced Frames: "+noVoicedFrames+" Actual: "+isVoiced);
			isVoiced = false;
		}

		return isVoiced;
	}

	/**
	 * To get Label Unit DATA (time stamp, index, phone unit)
	 * 
	 * @param line
	 *            line
	 * @return ArrayList contains time stamp, index and phone unit
	 * @throws IOException
	 *             IOException
	 */
	private ArrayList getLabelUnitData(String line) throws IOException {
		if (line == null)
			return null;
		ArrayList unitData = new ArrayList();
		StringTokenizer st = new StringTokenizer(line.trim());
		// the first token is the time
		unitData.add(st.nextToken());
		// the second token is the unit index
		unitData.add(st.nextToken());
		// the third token is the phone
		unitData.add(st.nextToken());
		return unitData;
	}

	/**
	 * Double ArrayList to double array conversion
	 * 
	 * @param array
	 *            ArrayList
	 * @return double array
	 */
	private double[] listToArray(ArrayList array) {

		double[] doubleArray = new double[array.size()];
		Iterator it = array.iterator();
		for (int i = 0; it.hasNext(); i++) {
			Double tempDouble = (Double) it.next();
			doubleArray[i] = tempDouble.doubleValue();
		}
		return doubleArray;
	}

	/**
	 * To get Phone Unit from Feature Vector
	 * 
	 * @param line
	 *            line
	 * @return String phone unit
	 * @throws IOException
	 *             IOException
	 */

	private String getFeatureUnit(String line) throws IOException {
		if (line == null)
			return null;
		if (line.trim().equals(""))
			return ""; // empty line -- signal end of section
		StringTokenizer st = new StringTokenizer(line.trim());
		// The expect that the first token in each line is the label
		return st.nextToken();
	}

	/**
	 * To know phone unit in Feature Vector is Vowel or not
	 * 
	 * @param line
	 *            line
	 * @param ph_VC_idx
	 *            ph_VC_idx
	 * @return true if phone unit in Feature Vector is Vowel, else false
	 */
	private boolean isVowel(String line, int ph_VC_idx) {

		boolean isVowel = false;
		String[] feats = line.split(" ");

		if (feats[ph_VC_idx].equals("+")) {
			isVowel = true;

		}
		return isVowel;
	}

	/**
	 * To know phone unit in Feature Vector is Fricative or not
	 * 
	 * @param line
	 *            line
	 * @param ph_Ctype_idx
	 *            ph_Ctype_idx
	 * @return true if phone unit in Feature Vector is Fricative, else false
	 */
	private boolean isFricative(String line, int ph_Ctype_idx) {

		boolean isFricative = false;
		String[] feats = line.split(" ");

		if (feats[ph_Ctype_idx].equals("f")) {
			isFricative = true;
		}
		return isFricative;

	}

	public int getProgress() {
		return progress;
	}

}
