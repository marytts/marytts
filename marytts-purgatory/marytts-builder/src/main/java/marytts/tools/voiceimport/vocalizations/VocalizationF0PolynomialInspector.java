/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.tools.voiceimport.vocalizations;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import marytts.features.FeatureDefinition;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.SPTKPitchReaderWriter;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.signalproc.display.FunctionGraph;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.BasenameList;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;
import marytts.vocalizations.KMeansClusterer;

public class VocalizationF0PolynomialInspector extends VoiceImportComponent {
	protected FeatureFileReader features;
	protected FeatureDefinition inFeatureDefinition;
	protected UnitFileReader units;
	protected FeatureFileReader contours;
	protected TimelineReader audio;
	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected FunctionGraph f0Graph = null;
	protected JFrame jf = null;
	protected PrintWriter featurePW;
	protected double costMeasure = 0;
	private HashMap<String, Integer> minF0Values;
	private HashMap<String, Integer> maxF0Values;
	private Set<String> characters;

	protected BasenameList bnlVocalizations;

	private final String name = "VocalizationF0PolynomialInspector";
	public final String WAVEDIR = name + ".waveDir";
	public final String F0POLYFILE = name + ".f0PolynomialFeatureFile";
	public final String F0MIN = name + ".f0Minimum";
	public final String F0MAX = name + ".f0Maximum";
	public final String PARTBASENAME = name + ".partBaseName";
	public final String ONEWORD = name + ".oneWordDescription";
	public final String KCLUSTERS = name + ".numberOfClusters";
	public final String POLYORDER = name + ".polynomialOrder";
	public final String ISEXTERNALF0 = name + ".isExternalF0Usage";
	public final String EXTERNALF0FORMAT = name + ".externalF0Format";
	public final String EXTERNALDIR = name + ".externalF0Directory";
	public final String EXTERNALEXT = name + ".externalF0Extention";

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(F0POLYFILE, "VocalizationF0PolyFeatureFile.txt");
			props.put(PARTBASENAME, "");
			props.put(ONEWORD, "");
			props.put(F0MIN, "50");
			props.put(F0MAX, "500");
			props.put(KCLUSTERS, "15");
			props.put(POLYORDER, "3");
			props.put(ISEXTERNALF0, "true");
			props.put(EXTERNALF0FORMAT, "sptk");
			props.put(EXTERNALDIR, "lf0");
			props.put(EXTERNALEXT, ".lf0");
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(WAVEDIR, "dir containing all waveforms ");
		}
	}

	@Override
	protected void initialiseComp() {
		minF0Values = new HashMap<String, Integer>();
		maxF0Values = new HashMap<String, Integer>();
		minF0Values.put("Spike", 50);
		minF0Values.put("Poppy", 170);
		minF0Values.put("Obadiah", 70);
		minF0Values.put("Prudence", 130);
		maxF0Values.put("Spike", 150);
		maxF0Values.put("Poppy", 380);
		maxF0Values.put("Obadiah", 150);
		maxF0Values.put("Prudence", 280);

		characters = new HashSet<String>();
		characters.add("Spike");
		characters.add("Poppy");
		characters.add("Obadiah");
		characters.add("Prudence");

		try {
			String basenameFile = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "basenames.lst";
			if ((new File(basenameFile)).exists()) {
				System.out.println("Loading basenames of vocalizations from '" + basenameFile + "' list...");
				bnlVocalizations = new BasenameList(basenameFile);
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in basename list");
			} else {
				String vocalWavDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav";
				System.out.println("Loading basenames of vocalizations from '" + vocalWavDir + "' directory...");
				bnlVocalizations = new BasenameList(vocalWavDir, ".wav");
				System.out.println("Found " + bnlVocalizations.getLength() + " vocalizations in " + vocalWavDir + " directory");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean compute() throws IOException, UnsupportedAudioFileException {
		logger.info("F0 polynomial feature file writer started.");
		f0Graph = new FunctionGraph(0, 1, new double[1]);
		f0Graph.setYMinMax(50, 550);
		f0Graph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
		jf = f0Graph.showInJFrame("Sentence", false, true);

		String outPutFile = db.getProp(db.ROOTDIR) + File.separator + getProp(ONEWORD) + getProp(PARTBASENAME)
				+ getProp(F0POLYFILE);
		featurePW = new PrintWriter(new FileWriter(new File(outPutFile)));

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			percent = 100 * i / bnlVocalizations.getLength();
			displaySentences(bnlVocalizations.getName(i));

		}
		featurePW.flush();
		featurePW.close();
		System.out.println("Total Cost : " + costMeasure / (double) bnlVocalizations.getLength());

		// String fileName = "/home/sathish/phd/voices/en-GB-listener/vocal-polynomials/SpiVocalizationF0PolyFeatureFile.txt";
		int kValue = (new Integer(getProp(KCLUSTERS))).intValue();
		KMeansClusterer kmc = new KMeansClusterer();
		kmc.loadF0Polynomials(outPutFile);
		kmc.trainer(kValue);
		// System.exit(0);

		return true;
	}

	/**
	 * @param baseName
	 *            baseName
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 */
	protected void displaySentences(String baseName) throws IOException, UnsupportedAudioFileException {
		/*
		 * int numUnits = units.getNumberOfUnits(); int unitSampleRate = units.getSampleRate(); int audioSampleRate =
		 * audio.getSampleRate(); int unitIndex = 0;
		 * 
		 * logger.debug("Number of units : "+numUnits);
		 */
		String partBaseName = getProp(PARTBASENAME).trim();
		if (!"".equals(partBaseName) && !baseName.contains(partBaseName)) {
			return;
		}

		/*
		 * String targetDescription = getProp(ONEWORD).trim(); String textFileName =
		 * db.getProp(db.TEXTDIR)+File.separator+baseName+db.getProp(db.TEXTEXT); String audioDescriprion =
		 * FileUtils.getFileAsString(new File(textFileName), "UTF-8");
		 * 
		 * if ( !"".equals(targetDescription) && !targetDescription.equals(audioDescriprion.trim())) { return; }
		 */

		// if ( !baseName.equals("") && !baseName.contains(getProp(PARTBASENAME)) ){
		// return;
		// }

		String waveFile = getProp(WAVEDIR) + File.separator + baseName + db.getProp(db.WAVEXT);
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(waveFile));

		// Enforce PCM_SIGNED encoding
		if (!inputAudio.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
			inputAudio = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, inputAudio);
		}

		int audioSampleRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] sentenceAudio = signal.getAllData(); // Copies all samples in wav file into a double buffer
		long tsSentenceDuration = sentenceAudio.length;

		/*
		 * Datagram[] sentenceData = audio.getDatagrams(tsSentenceStart, tsSentenceDuration); DatagramDoubleDataSource ddds = new
		 * DatagramDoubleDataSource(sentenceData); double[] sentenceAudio = ddds.getAllData();
		 */

		AudioPlayer ap = null;
		ap = new AudioPlayer(new DDSAudioInputStream(new BufferedDoubleDataSource(sentenceAudio), new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, // samples per second
				16, // bits per sample
				1, // mono
				2, // nr. of bytes per frame
				audioSampleRate, // nr. of frames per second
				true))); // big-endian;))
		ap.start();

		PitchFileHeader params = new PitchFileHeader();
		// params.minimumF0 = 50;
		// params.maximumF0 = 250;
		// params.minimumF0 = (new Double(getProp(F0MIN))).doubleValue();
		// params.maximumF0 = (new Double(getProp(F0MAX))).doubleValue();
		String character = getCharacterName(baseName);
		params.minimumF0 = (minF0Values.get(character)).doubleValue();
		params.maximumF0 = (maxF0Values.get(character)).doubleValue();
		params.fs = audioSampleRate;

		double[] f0Array = null;

		if ("true".equals(getProp(ISEXTERNALF0))) {

			String externalFormat = getProp(EXTERNALF0FORMAT);
			String externalDir = getProp(EXTERNALDIR);
			String externalExt = getProp(EXTERNALEXT);

			if ("sptk".equals(externalFormat)) {
				String fileName = db.getProp(db.VOCALIZATIONSDIR) + File.separator + externalDir + File.separator + baseName
						+ externalExt;
				SPTKPitchReaderWriter sprw = new SPTKPitchReaderWriter(fileName);
				f0Array = sprw.getF0Contour();
			} else if ("ptc".equals(externalFormat)) {
				String fileName = db.getProp(db.ROOTDIR) + File.separator + externalDir + File.separator + baseName + externalExt;
				PitchReaderWriter sprw = new PitchReaderWriter(fileName);
				f0Array = sprw.contour;
			}
		} else {
			F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
			tracker.pitchAnalyze(new BufferedDoubleDataSource(sentenceAudio));
			// double frameShiftTime = tracker.getSkipSizeInSeconds();
			f0Array = tracker.getF0Contour();
		}

		// f0Array = cutStartEndUnvoicedSegments(f0Array);

		if (f0Array != null) {
			for (int j = 0; j < f0Array.length; j++) {
				if (f0Array[j] == 0) {
					f0Array[j] = Double.NaN;
				}
			}
			if (f0Array.length >= 3) {
				f0Array = SignalProcUtils.medianFilter(f0Array, 5);
			}
			f0Graph.updateData(0, tsSentenceDuration / (double) audioSampleRate / f0Array.length, f0Array);
			jf.repaint();

			double[] f0AndInterpol;
			double[] interpol = new double[f0Array.length];
			Arrays.fill(interpol, Double.NaN);
			f0AndInterpol = new double[f0Array.length];
			int iLastValid = -1;
			for (int j = 0; j < f0Array.length; j++) {
				if (!Double.isNaN(f0Array[j])) { // a valid value
					if (iLastValid == j - 1) {
						// no need to interpolate
						f0AndInterpol[j] = f0Array[j];
					} else {
						// need to interpolate
						double prevF0;
						if (iLastValid < 0) { // we don't have a previous value -- use current one
							prevF0 = f0Array[j];
						} else {
							prevF0 = f0Array[iLastValid];
						}
						double delta = (f0Array[j] - prevF0) / (j - iLastValid);
						double f0 = prevF0;
						for (int k = iLastValid + 1; k < j; k++) {
							f0 += delta;
							interpol[k] = f0;
							f0AndInterpol[k] = f0;
						}
					}
					iLastValid = j;
				}
			}
			f0Graph.addDataSeries(interpol, Color.GREEN, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_EMPTYCIRCLE);
			jf.repaint();

			double[] f0AndInterpolate = combineF0andInterpolate(f0Array, interpol);

			// double[] coeffs = Polynomial.fitPolynomial(sylF0, polynomOrder);
			int polynomialOrder = (new Integer(getProp(POLYORDER))).intValue();
			double[] coeffs = Polynomial.fitPolynomial(f0AndInterpolate, polynomialOrder);

			if (coeffs != null) {
				double[] sylPred = Polynomial.generatePolynomialValues(coeffs, interpol.length, 0, 1);
				f0Graph.addDataSeries(sylPred, Color.RED, FunctionGraph.DRAW_LINE, -1);
				double eqDistance = DistanceComputer.getEuclideanDistance(sylPred, f0AndInterpolate);
				System.out.println(baseName + " - EqDist: " + eqDistance / (double) sylPred.length);
				costMeasure += (eqDistance / (double) sylPred.length);
				featurePW.print(baseName + " ");
				for (double c : coeffs) {
					featurePW.print(c + " ");
				}
				featurePW.println();

			}

			// double[] pred = Polynomial.generatePolynomialValues(coeffs, iUnitDurationInArray, 0, 1);
			// f0Graph.addDataSeries(unitF0, Color.BLACK, FunctionGraph.DRAW_LINE, -1);

			try {
				ap.join();
				Thread.sleep(10);
			} catch (InterruptedException ie) {
			}
			// System.out.println();

		}

	}

	private double[] cutStartEndUnvoicedSegments(double[] array) {

		if (array == null)
			return null;

		int startIndex = 0;
		int endIndex = array.length;

		// find start index
		for (int i = 0; i < array.length; i++) {
			if (array[i] != 0) {
				startIndex = i;
				break;
			}
		}

		// find end index
		for (int i = (array.length - 1); i > 0; i--) {
			if (array[i] != 0) {
				endIndex = i;
				break;
			}
		}

		int newArraySize = endIndex - startIndex;

		double[] newArray = new double[newArraySize];
		System.arraycopy(array, startIndex, newArray, 0, newArraySize);

		for (int i = 0; i < newArray.length; i++) {
			System.out.println(newArray[i]);
		}
		System.out.println("Resized from " + array.length + " to " + newArraySize);

		return newArray;
	}

	private String getCharacterName(String baseName) {
		Iterator<String> it = characters.iterator();
		while (it.hasNext()) {
			String character = it.next().trim();
			if (baseName.startsWith(character)) {
				return character;
			}
		}
		return null;
	}

	private double[] combineF0andInterpolate(double[] f0Array, double[] interpol) {

		double[] f0AndInterpolate = new double[f0Array.length];
		Arrays.fill(f0AndInterpolate, Double.NaN);
		for (int i = 0; i < f0Array.length; i++) {
			if (!Double.isNaN(f0Array[i])) {
				f0AndInterpolate[i] = f0Array[i];
			} else if (!Double.isNaN(interpol[i])) {
				f0AndInterpolate[i] = interpol[i];
			}
			// System.out.println(f0Array[i]+" "+interpol[i]+" "+f0AndInterpolate[i]);
		}

		return f0AndInterpolate;
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		VocalizationF0PolynomialInspector acfeatsWriter = new VocalizationF0PolynomialInspector();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
