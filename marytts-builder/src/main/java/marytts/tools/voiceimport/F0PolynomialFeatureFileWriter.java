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
package marytts.tools.voiceimport;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.swing.JFrame;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.display.FunctionGraph;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.Sentence;
import marytts.unitselection.data.SentenceIterator;
import marytts.unitselection.data.Syllable;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.Pair;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DatagramDoubleDataSource;
import marytts.util.data.MaryHeader;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;

public class F0PolynomialFeatureFileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected FeatureFileReader features;
	protected File outFeatureFile;
	protected FeatureDefinition outFeatureDefinition;
	protected UnitFileReader units;
	protected TimelineReader audio;
	protected DatabaseLayout db = null;
	protected int percent = 0;

	private final String name = "F0PolynomialFeatureFileWriter";
	public final String UNITFILE = name + ".unitFile";
	public final String WAVETIMELINE = name + ".waveTimeLine";
	public final String ISHNMTIMELINE = name + ".isHnmTimeline";
	public final String FEATUREFILE = name + ".featureFile";
	public final String F0FEATUREFILE = name + ".f0FeatureFile";
	public final String POLYNOMORDER = name + ".polynomOrder";
	public final String SHOWGRAPH = name + ".showGraph";
	public final String INTERPOLATE = name + ".interpolate";
	public final String MINPITCH = name + ".minPitch";
	public final String MAXPITCH = name + ".maxPitch";

	// a battery of constants intended to make the interpretation of
	// feature vectors faster. Filled in initialiseFeatureConstants().
	private int fiPhoneme;
	private byte fvPhoneme_0;
	private byte fvPhoneme_Silence;
	private int fiLR;
	private byte fvLR_L;
	private byte fvLR_R;
	private int fiSylStart;
	private int fiSylEnd;
	private int fiSentenceStart;
	private int fiSentenceEnd;
	private int fiWordStart;
	private int fiWordEnd;
	private int fiVowel;
	private byte fvVowel_Plus;
	private boolean haveUnitLogF0;
	private int fiUnitLogF0;
	private int fiUnitLogF0delta;

	private FunctionGraph f0Graph = null;
	private JFrame jf = null;

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
		this.db = theDb;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileDir = theDb.getProp(theDb.FILEDIR);
			String maryExt = theDb.getProp(theDb.MARYEXT);
			props.put(UNITFILE, fileDir + "halfphoneUnits" + maryExt);
			props.put(WAVETIMELINE, fileDir + "timeline_waveforms" + maryExt);
			props.put(ISHNMTIMELINE, "false");
			props.put(FEATUREFILE, fileDir + "halfphoneFeatures" + maryExt);
			props.put(F0FEATUREFILE, fileDir + "syllableF0Polynomials" + maryExt);
			props.put(POLYNOMORDER, "3");
			props.put(SHOWGRAPH, "false");
			props.put(INTERPOLATE, "true");
			if (theDb.getProp(theDb.GENDER).equals("female")) {
				props.put(MINPITCH, "100");
				props.put(MAXPITCH, "500");
			} else {
				props.put(MINPITCH, "75");
				props.put(MAXPITCH, "300");
			}
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(UNITFILE, "file containing all halfphone units");
			props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them");
			props2Help.put(ISHNMTIMELINE, "file containing all wave files");
			props2Help.put(FEATUREFILE, "file containing all halfphone units and their target cost features");
			props2Help.put(F0FEATUREFILE, "file containing syllable-based polynom coefficients on vowels");
			props2Help.put(POLYNOMORDER, "order of the polynoms used to approximate syllable F0 curves");
			props2Help.put(SHOWGRAPH, "whether to show a graph with f0 aproximations for each sentence");
			props2Help.put(INTERPOLATE, "whether to interpolate F0 across unvoiced regions");
			props2Help.put(MINPITCH, "minimum value for the pitch (in Hz). Default: female 100, male 75");
			props2Help.put(MAXPITCH, "maximum value for the pitch (in Hz). Default: female 500, male 300");
		}
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		logger.info("F0 polynomial feature file writer started.");

		maryDir = new File(db.getProp(db.FILEDIR));
		if (!maryDir.exists()) {
			maryDir.mkdir();
			System.out.println("Created the output directory [" + (db.getProp(db.FILEDIR)) + "] to store the feature file.");
		}
		units = new UnitFileReader(getProp(UNITFILE));
		audio = null;
		if (getProp(ISHNMTIMELINE).compareToIgnoreCase("true") == 0) {
			audio = new HnmTimelineReader(getProp(WAVETIMELINE));
		} else {
			audio = new TimelineReader(getProp(WAVETIMELINE));
		}

		features = new FeatureFileReader(getProp(FEATUREFILE));
		// Initialise the constants that will be used later for fast interpretation of the feature vectors:
		initialiseFeatureConstants();
		// Create the feature definition for the polynomial feature file:
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(FeatureDefinition.BYTEFEATURES); // no byte features
		pw.println(FeatureDefinition.SHORTFEATURES); // no short features
		pw.println(FeatureDefinition.CONTINUOUSFEATURES);
		int polynomOrder = Integer.parseInt(getProp(POLYNOMORDER));
		for (int i = polynomOrder; i >= 0; i--) {
			pw.println("0 linear | f0contour_a" + i);
		}
		pw.close();
		String fd = sw.toString();
		logger.debug("Generated the following feature definition:");
		logger.debug(fd);
		StringReader sr = new StringReader(fd);
		BufferedReader br = new BufferedReader(sr);
		outFeatureDefinition = new FeatureDefinition(br, true);

		outFeatureFile = new File(getProp(F0FEATUREFILE));
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFeatureFile)));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();
		logger.debug("Number of processed units: " + units.getNumberOfUnits());

		FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(getProp(F0FEATUREFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == units.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	/**
	 * From the given feature definition, set up a battery of instance variables intended to speed up the analysis of the feature
	 * vectors.
	 */
	private void initialiseFeatureConstants() {
		assert features != null : "This shouldn't be called without features";
		FeatureDefinition featureDefinition = features.getFeatureDefinition();
		fiPhoneme = featureDefinition.getFeatureIndex("phone");
		fvPhoneme_0 = featureDefinition.getFeatureValueAsByte(fiPhoneme, "0");
		fvPhoneme_Silence = featureDefinition.getFeatureValueAsByte(fiPhoneme, "_");
		fiLR = featureDefinition.getFeatureIndex("halfphone_lr");
		fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L");
		fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R");
		fiSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
		fiSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
		fiSentenceStart = featureDefinition.getFeatureIndex("words_from_sentence_start");
		fiSentenceEnd = featureDefinition.getFeatureIndex("words_from_sentence_end");
		fiWordStart = featureDefinition.getFeatureIndex("segs_from_word_start");
		fiWordEnd = featureDefinition.getFeatureIndex("segs_from_word_end");
		fiVowel = featureDefinition.getFeatureIndex("ph_vc");
		fvVowel_Plus = featureDefinition.getFeatureValueAsByte(fiVowel, "+");

		haveUnitLogF0 = false;
		fiUnitLogF0 = -1;
		fiUnitLogF0delta = -1;
		if (featureDefinition.hasFeature("unit_logf0") && featureDefinition.hasFeature("unit_logf0delta")) {
			haveUnitLogF0 = true;
			fiUnitLogF0 = featureDefinition.getFeatureIndex("unit_logf0");
			fiUnitLogF0delta = featureDefinition.getFeatureIndex("unit_logf0delta");
		}
	}

	/**
	 * Get the audio data for the given sentence
	 * 
	 * @param s
	 *            s
	 * @return the audio data for the sentence, in double representation
	 * @throws IOException
	 *             if there is a problem reading the audio data
	 */
	private double[] getAudio(Sentence s) throws IOException {
		long tsSentenceStart = units.getUnit(s.getFirstUnitIndex()).startTime;
		long tsSentenceEnd = units.getUnit(s.getLastUnitIndex()).startTime + units.getUnit(s.getLastUnitIndex()).duration;
		long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
		Datagram[] sentenceData = audio.getDatagrams(tsSentenceStart, tsSentenceDuration);
		DatagramDoubleDataSource ddds = new DatagramDoubleDataSource(sentenceData);
		double[] sentenceAudio = ddds.getAllData();
		return sentenceAudio;
	}

	/**
	 * Play the audio for the given sentence in blocking mode (returns when finished playing)
	 * 
	 * @param s
	 *            s
	 * @throws IOException
	 *             if there is a problem reading the audio data
	 */
	private void playAudio(Sentence s) throws IOException {
		double[] sentenceAudio = getAudio(s);
		AudioPlayer ap = new AudioPlayer(new DDSAudioInputStream(new BufferedDoubleDataSource(sentenceAudio), new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED, audio.getSampleRate(), // samples per second
				16, // bits per sample
				1, // mono
				2, // nr. of bytes per frame
				audio.getSampleRate(), // nr. of frames per second
				true))); // big-endian;))
		ap.run(); // play in same thread, i.e. blocking mode
	}

	/**
	 * For the given sentence, obtain a log f0 contour. This implementation computes the f0 contour on the fly, using a built-in
	 * F0 tracker. If config setting {@see #INTERPOLATE} is true, interpolate the curve across unvoiced sections, else unvoiced
	 * sections will be NaN.
	 * 
	 * @param s
	 *            the current sentence
	 * @param skipSizeInSeconds
	 *            the sampling frequency of the F0 contour to return (e.g., 0.005 to get a value every 5 ms)
	 * @return a double array representing the F0 contour, sampled at skipSizeInSeconds, or null if no f0 contour could be
	 *         computed
	 * @throws IOException
	 *             if there is a problem reading the audio data
	 */
	private double[] getLogF0Contour(Sentence s, double skipSizeInSeconds) throws IOException {
		PitchFileHeader params = new PitchFileHeader();
		params.fs = audio.getSampleRate();
		params.minimumF0 = Double.parseDouble(getProp(MINPITCH));
		params.maximumF0 = Double.parseDouble(getProp(MAXPITCH));
		params.skipSizeInSeconds = skipSizeInSeconds;
		F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
		double[] sentenceAudio = getAudio(s);
		tracker.pitchAnalyze(new BufferedDoubleDataSource(sentenceAudio));
		double frameShiftTime = skipSizeInSeconds;
		double[] f0Array = tracker.getF0Contour();
		if (f0Array == null) {
			return null;
		}
		for (int j = 0; j < f0Array.length; j++) {
			if (f0Array[j] == 0) {
				f0Array[j] = Double.NaN;
			}
		}
		if (f0Array.length >= 3) {
			f0Array = SignalProcUtils.medianFilter(f0Array, 5);
		}

		for (int j = 0; j < f0Array.length; j++) {
			if (f0Array[j] == 0)
				f0Array[j] = Double.NaN;
			else
				f0Array[j] = Math.log(f0Array[j]);
		}
		return f0Array;
	}

	/**
	 * For the given log f0 contour, compute an interpolation across NaN sections
	 * 
	 * @param rawLogF0Contour
	 *            rawLogF0Contour
	 * @return a version of the LogF0 contour for which values are interpolated across NaN regions
	 */
	private double[] getInterpolatedLogF0Contour(double[] rawLogF0Contour) {
		double[] interpol = new double[rawLogF0Contour.length];
		int iLastValid = -1;
		for (int j = 0; j < rawLogF0Contour.length; j++) {
			if (!Double.isNaN(rawLogF0Contour[j])) { // a valid value
				interpol[j] = rawLogF0Contour[j];
				if (iLastValid != j - 1) { // need to interpolate
					double prevLogF0;
					if (iLastValid < 0) { // we don't have a previous value, use current one
						prevLogF0 = rawLogF0Contour[j];
					} else {
						prevLogF0 = rawLogF0Contour[iLastValid];
					}
					double delta = (rawLogF0Contour[j] - prevLogF0) / (j - iLastValid);
					double logF0 = prevLogF0;
					for (int k = iLastValid + 1; k < j; k++) {
						logF0 += delta;
						interpol[k] = logF0;
					}
				}
				iLastValid = j;
			}
		}
		return interpol;
	}

	/**
	 * For a syllable that is part of a sentence, determine the position of the syllable in an array representing the full
	 * sentence.
	 * 
	 * @param s
	 *            the sentence
	 * @param syl
	 *            the syllable which must be inside the sentence
	 * @param arrayLength
	 *            the length of an array representing the temporal extent of the sentence
	 * @return a pair of ints representing start (inclusive) and end position (exclusive) of the syllable in the array
	 */
	private Pair<Integer, Integer> getSyllableIndicesInSentenceArray(Sentence s, Syllable syl, int arrayLength) {
		long tsSentenceStart = units.getUnit(s.getFirstUnitIndex()).startTime;
		long tsSentenceEnd = units.getUnit(s.getLastUnitIndex()).startTime + units.getUnit(s.getLastUnitIndex()).duration;
		long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
		long tsSylStart = units.getUnit(syl.getFirstUnitIndex()).startTime;
		assert tsSylStart >= tsSentenceStart;
		long tsSylEnd = units.getUnit(syl.getLastUnitIndex()).startTime + units.getUnit(syl.getLastUnitIndex()).duration;
		assert tsSylEnd >= tsSylStart;
		assert tsSylEnd <= tsSentenceEnd;
		long tsSylDuration = tsSylEnd - tsSylStart;
		// Now map time to position in logF0 array:
		double factor = (double) arrayLength / (double) tsSentenceDuration;
		int iSylStart = (int) (factor * (tsSylStart - tsSentenceStart));
		int iSylEnd = (int) (factor * (tsSylEnd - tsSentenceStart));
		return new Pair<Integer, Integer>(iSylStart, iSylEnd);

	}

	private List<Polynomial> fitPolynomialsToSyllables(Sentence s, double[] logF0, int polynomOrder) {
		List<Polynomial> poly = new ArrayList<Polynomial>();
		for (Syllable syl : s) {
			Pair<Integer, Integer> syllableIndices = getSyllableIndicesInSentenceArray(s, syl, logF0.length);
			// System.out.println(" -- from "+iSylStart+" to "+iSylEnd + " out of " + logF0.length);
			double[] sylLogF0 = new double[syllableIndices.getSecond() - syllableIndices.getFirst()];
			System.arraycopy(logF0, syllableIndices.getFirst(), sylLogF0, 0, sylLogF0.length);
			// Now that we have the log F0 curve corresponding to the syllable, fit polynomial:
			double[] coeffs = Polynomial.fitPolynomial(sylLogF0, polynomOrder);
			if (coeffs != null) {
				poly.add(new Polynomial(coeffs));
			} else {
				poly.add(null);
			}
		}
		return poly;
	}

	private double[] fillApproxFromPolynomials(Sentence s, List<Polynomial> polynomials, int len) {
		double[] approx = new double[len];
		Arrays.fill(approx, Double.NaN);
		Iterator<Polynomial> polyIt = polynomials.iterator();
		for (Syllable syl : s) {
			Polynomial poly = polyIt.next();
			if (poly == null) {
				continue;
			}
			System.out.print(" (" + syl.getFirstUnitIndex() + "-" + syl.getSyllableNucleusIndex() + "-" + syl.getLastUnitIndex()
					+ ")");
			Pair<Integer, Integer> syllableIndices = getSyllableIndicesInSentenceArray(s, syl, len);
			double[] sylPred = poly.generatePolynomialValues(syllableIndices.getSecond() - syllableIndices.getFirst(), 0, 1);
			System.arraycopy(sylPred, 0, approx, syllableIndices.getFirst(), sylPred.length);
		}
		System.out.println();
		return approx;
	}

	/**
	 * Draw or update a graph showing the different F0 curves
	 * 
	 * @param logF0
	 *            the raw log F0 curve (mandatory)
	 * @param interpolatedLogF0
	 *            the interpolated log F0 curve (ignored if equal to logF0 or null)
	 * @param approximatedLogF0
	 *            the approximated log F0 curve (ignored if null)
	 * @param f0FrameSkip
	 *            f0FrameSkip
	 */
	private void drawGraph(double[] logF0, double[] interpolatedLogF0, double[] approximatedLogF0, double f0FrameSkip) {
		if (f0Graph == null) {
			f0Graph = new FunctionGraph(0, 1, new double[1]);
			f0Graph.setYMinMax(50, 300);
			f0Graph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
			jf = f0Graph.showInJFrame("Sentence", false, true);
		}

		double[] f0 = MathUtils.exp(logF0);
		double[] interpol = null;
		if (interpolatedLogF0 != null && interpolatedLogF0 != logF0 /* not the same object */) {
			assert interpolatedLogF0.length == logF0.length : "interpol not same length";
			interpol = MathUtils.exp(interpolatedLogF0);
			// Only leave those values in interpol for which f0 doesn't have a value!
			for (int j = 0; j < f0.length; j++) {
				if (!Double.isNaN(f0[j])) {
					interpol[j] = Double.NaN;
				}
			}
		}
		double[] approx = null;
		if (approximatedLogF0 != null) {
			assert approximatedLogF0.length == logF0.length : "approx not same length";
			approx = MathUtils.exp(approximatedLogF0);
		}
		f0Graph.updateData(0, f0FrameSkip, f0);
		if (interpol != null) {
			f0Graph.addDataSeries(interpol, Color.GREEN, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_EMPTYCIRCLE);
		}
		if (approx != null) {
			f0Graph.addDataSeries(approx, Color.RED, FunctionGraph.DRAW_LINE, -1);
		}
		jf.repaint();

	}

	/**
	 * Compute the polynomial unit features and write them to the given data output.
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 * @throws UnsupportedEncodingException
	 *             UnsupportedEncodingException
	 * @throws FileNotFoundException
	 *             FileNotFoundException
	 */
	protected void writeUnitFeaturesTo(DataOutput out) throws IOException, UnsupportedEncodingException, FileNotFoundException {
		int numUnits = units.getNumberOfUnits();
		boolean showGraph = Boolean.parseBoolean(getProp(SHOWGRAPH));
		boolean interpolate = Boolean.parseBoolean(getProp(INTERPOLATE));
		int polynomOrder = Integer.parseInt(getProp(POLYNOMORDER));
		float[] zeros = new float[polynomOrder + 1];
		int unitIndex = 0;

		out.writeInt(numUnits);
		logger.debug("Number of units : " + numUnits);

		long startTime = System.currentTimeMillis();

		// Overall strategy:
		// 1. Go through the units sentence by sentence
		// 2. For every sentence, get the f0 contour
		// 3. For every syllable in the sentence, fit a polynomial to the f0 contour

		// 1. Go through the units sentence by sentence
		Iterator<Sentence> sit = new SentenceIterator(features);
		while (sit.hasNext()) {
			Sentence s = sit.next();
			percent = 100 * s.getFirstUnitIndex() / numUnits;

			// 2. For every sentence, get the f0 contour
			double f0FrameSkip = 0.005; // 5 ms
			double[] rawLogF0 = getLogF0Contour(s, f0FrameSkip);
			// TODO: act appropriately if rawLogF0 is null
			double[] logF0;
			if (interpolate) {
				logF0 = getInterpolatedLogF0Contour(rawLogF0);
			} else {
				logF0 = rawLogF0;
			}

			// 3. For every syllable in the sentence, fit a polynomial to the f0 contour
			List<Polynomial> polynomials = fitPolynomialsToSyllables(s, logF0, polynomOrder);
			// Now save the coefficients as features of the respective syllable nucleus
			Iterator<Polynomial> polyIt = polynomials.iterator();
			for (Syllable syl : s) {
				assert polyIt.hasNext();
				Polynomial poly = polyIt.next();
				int iSylNucleus = syl.getSyllableNucleusIndex();
				// We write the polynomial coefficients as features of the nucleus, and zeros for the other units.
				// First the zeros:
				while (unitIndex < iSylNucleus) {
					FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
					outFV.writeTo(out);
					unitIndex++;
				}
				// And now the nucleus:
				float[] fcoeffs;
				if (poly == null) {
					fcoeffs = zeros;
				} else {
					fcoeffs = ArrayUtils.copyDouble2Float(poly.coeffs);
				}
				FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, fcoeffs);
				outFV.writeTo(out);
				unitIndex++;
			}
			assert !polyIt.hasNext(); // as many polynomials as syllables

			// Now if user wants to see the graph, show it and play the audio:
			if (showGraph) {
				System.out.println("Sentence from " + s.getFirstUnitIndex() + " to " + s.getLastUnitIndex());
				double[] approx = fillApproxFromPolynomials(s, polynomials, logF0.length);
				drawGraph(rawLogF0, logF0, approx, f0FrameSkip);
				playAudio(s);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ie) {
				}
			}

		}
		// Write any trailing zeros after last syllable nucleus of last sentence:
		while (unitIndex < numUnits) {
			FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
			outFV.writeTo(out);
			unitIndex++;
		}

		long endTime = System.currentTimeMillis();
		System.out.println("Processed " + numUnits + " units in " + (endTime - startTime) + " ms");

	}

	/**
	 * Write the header of this feature file to the given DataOutput
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 */
	protected void writeHeaderTo(DataOutput out) throws IOException {
		new MaryHeader(MaryHeader.UNITFEATS).writeTo(out);
		outFeatureDefinition.writeBinaryTo(out);
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
		F0PolynomialFeatureFileWriter acfeatsWriter = new F0PolynomialFeatureFileWriter();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
