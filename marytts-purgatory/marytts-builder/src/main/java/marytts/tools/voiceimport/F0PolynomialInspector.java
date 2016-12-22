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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
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
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DatagramDoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;

public class F0PolynomialInspector extends VoiceImportComponent {
	protected FeatureFileReader features;
	protected FeatureDefinition inFeatureDefinition;
	protected UnitFileReader units;
	protected FeatureFileReader contours;
	protected TimelineReader audio;
	protected DatabaseLayout db = null;
	protected int percent = 0;

	private final String name = "F0PolynomialInspector";
	public final String UNITFILE = name + ".unitFile";
	public final String WAVETIMELINE = name + ".waveTimeLine";
	public final String ISHNMTIMELINE = name + ".isHnmTimeline";
	public final String FEATUREFILE = name + ".featureFile";
	public final String F0FEATUREFILE = name + ".f0FeatureFile";

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			String fileDir = db.getProp(db.FILEDIR);
			String maryExt = db.getProp(db.MARYEXT);
			props.put(UNITFILE, fileDir + "halfphoneUnits" + maryExt);
			props.put(WAVETIMELINE, fileDir + "timeline_waveforms" + maryExt);
			props.put(ISHNMTIMELINE, "false");
			props.put(FEATUREFILE, fileDir + "halfphoneFeatures_ac" + maryExt);
			props.put(F0FEATUREFILE, fileDir + "syllableF0Polynomials" + maryExt);
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
		}
	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		logger.info("F0 polynomial feature file writer started.");

		units = new UnitFileReader(getProp(UNITFILE));
		audio = null;
		if (getProp(ISHNMTIMELINE).compareToIgnoreCase("true") == 0)
			audio = new HnmTimelineReader(getProp(WAVETIMELINE));
		else
			audio = new TimelineReader(getProp(WAVETIMELINE));

		features = new FeatureFileReader(getProp(FEATUREFILE));
		inFeatureDefinition = features.getFeatureDefinition();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(FeatureDefinition.BYTEFEATURES); // no byte features
		pw.println(FeatureDefinition.SHORTFEATURES); // no short features
		pw.println(FeatureDefinition.CONTINUOUSFEATURES);

		contours = new FeatureFileReader(getProp(F0FEATUREFILE));

		displaySentences();
		return true;
	}

	/**
	 * 
	 * @throws IOException
	 *             IOException
	 */
	protected void displaySentences() throws IOException {
		int numUnits = units.getNumberOfUnits();
		int unitSampleRate = units.getSampleRate();
		int audioSampleRate = audio.getSampleRate();
		int unitIndex = 0;

		logger.debug("Number of units : " + numUnits);

		FeatureDefinition featureDefinition = features.getFeatureDefinition();
		int fiPhoneme = featureDefinition.getFeatureIndex("phone");
		byte fvPhoneme_0 = featureDefinition.getFeatureValueAsByte(fiPhoneme, "0");
		byte fvPhoneme_Silence = featureDefinition.getFeatureValueAsByte(fiPhoneme, "_");
		int fiLR = featureDefinition.getFeatureIndex("halfphone_lr");
		byte fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L");
		byte fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R");
		int fiSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
		int fiSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
		int fiSentenceStart = featureDefinition.getFeatureIndex("words_from_sentence_start");
		int fiSentenceEnd = featureDefinition.getFeatureIndex("words_from_sentence_end");
		int fiWordStart = featureDefinition.getFeatureIndex("segs_from_word_start");
		int fiWordEnd = featureDefinition.getFeatureIndex("segs_from_word_end");
		int fiVowel = featureDefinition.getFeatureIndex("ph_vc");
		byte fvVowel_Plus = featureDefinition.getFeatureValueAsByte(fiVowel, "+");

		boolean haveUnitLogF0 = false;
		int fiUnitLogF0 = -1;
		int fiUnitLogF0delta = -1;
		if (featureDefinition.hasFeature("unit_logf0") && featureDefinition.hasFeature("unit_logf0delta")) {
			haveUnitLogF0 = true;
			fiUnitLogF0 = featureDefinition.getFeatureIndex("unit_logf0");
			fiUnitLogF0delta = featureDefinition.getFeatureIndex("unit_logf0delta");
		}

		FunctionGraph f0Graph = null;
		JFrame jf = null;
		int iSentenceStart = -1;
		int iSentenceEnd = -1;
		List<Integer> iSylStarts = new ArrayList<Integer>();
		List<Integer> iSylEnds = new ArrayList<Integer>();
		List<Integer> iSylVowels = new ArrayList<Integer>();
		f0Graph = new FunctionGraph(0, 1, new double[1]);
		f0Graph.setYMinMax(50, 300);
		f0Graph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
		jf = f0Graph.showInJFrame("Sentence", false, true);

		for (int i = 0; i < numUnits; i++) {
			percent = 100 * i / numUnits;
			FeatureVector fv = features.getFeatureVector(i);
			// System.out.print(featureDefinition.getFeatureValueAsString("phone", fv));
			// if (fv.getByteFeature(fiPhoneme) == fvPhoneme_0
			// || fv.getByteFeature(fiPhoneme) == fvPhoneme_Silence) continue;
			if (iSentenceStart == -1 && fv.getByteFeature(fiSentenceStart) == 0 && fv.getByteFeature(fiWordStart) == 0
					&& fv.getByteFeature(fiLR) == fvLR_L) { // first unit in sentence
				iSentenceStart = i;
				iSylStarts.clear();
				iSylEnds.clear();
				iSylVowels.clear();
				// System.out.print(", is sentence start");
			}
			// Silence and edge units cannot be part of syllables, but they can
			// mark start/end of sentence:
			if (fv.getByteFeature(fiPhoneme) != fvPhoneme_0 && fv.getByteFeature(fiPhoneme) != fvPhoneme_Silence) {
				if (fv.getByteFeature(fiSylStart) == 0 && fv.getByteFeature(fiLR) == fvLR_L) { // first segment in syllable
					if (iSylStarts.size() > iSylEnds.size()) {
						System.err.println("Syllable ends before other syllable starts!");
					}
					iSylStarts.add(i);
					// System.out.print(", is syl start");
				}
				if (fv.getByteFeature(fiVowel) == fvVowel_Plus && iSylVowels.size() < iSylStarts.size()) { // first vowel unit in
																											// syllable
					iSylVowels.add(i);
					// System.out.print(", is vowel");
				}
				if (fv.getByteFeature(fiSylEnd) == 0 && fv.getByteFeature(fiLR) == fvLR_R) { // last segment in syllable
					iSylEnds.add(i);
					// System.out.print(", is syl end");
					assert iSylStarts.size() == iSylEnds.size();
					if (iSylVowels.size() < iSylEnds.size()) {
						// System.err.println("Syllable contains no vowel -- skipping");
						iSylStarts.remove(iSylStarts.size() - 1);
						iSylEnds.remove(iSylEnds.size() - 1);
					}
				}
			}
			if (iSentenceStart != -1 && fv.getByteFeature(fiSentenceEnd) == 0 && fv.getByteFeature(fiWordEnd) == 0
					&& fv.getByteFeature(fiLR) == fvLR_R) { // last unit in sentence
				iSentenceEnd = i;
				// System.out.print(", is sentence end");
				if (iSylEnds.size() < iSylStarts.size()) {
					System.err.println("Last syllable in sentence is not properly closed");
					iSylEnds.add(i);
				}
			}
			// System.out.println();

			if (iSentenceStart >= 0 && iSentenceEnd >= iSentenceStart && iSylVowels.size() > 0) {
				assert iSylStarts.size() == iSylEnds.size() : "Have " + iSylStarts.size() + " syllable starts, but "
						+ iSylEnds.size() + " syllable ends!";
				assert iSylStarts.size() == iSylVowels.size();
				long tsSentenceStart = units.getUnit(iSentenceStart).startTime;
				long tsSentenceEnd = units.getUnit(iSentenceEnd).startTime + units.getUnit(iSentenceEnd).duration;
				long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
				Datagram[] sentenceData = audio.getDatagrams(tsSentenceStart, tsSentenceDuration);
				DatagramDoubleDataSource ddds = new DatagramDoubleDataSource(sentenceData);
				double[] sentenceAudio = ddds.getAllData();
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
				params.fs = audioSampleRate;
				F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
				tracker.pitchAnalyze(new BufferedDoubleDataSource(sentenceAudio));
				double frameShiftTime = tracker.getSkipSizeInSeconds();
				double[] f0Array = tracker.getF0Contour();
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

					double[] approx = new double[f0Array.length];
					Arrays.fill(approx, Double.NaN);
					for (int s = 0; s < iSylStarts.size(); s++) {
						long tsSylStart = units.getUnit(iSylStarts.get(s)).startTime;
						long tsSylEnd = units.getUnit(iSylEnds.get(s)).startTime + units.getUnit(iSylEnds.get(s)).duration;
						long tsSylDuration = tsSylEnd - tsSylStart;
						int iSylVowel = iSylVowels.get(s);
						// now map time to position in f0AndInterpol array:
						int iSylStart = (int) (((double) (tsSylStart - tsSentenceStart) / tsSentenceDuration) * f0AndInterpol.length);
						assert iSylStart >= 0;
						int iSylEnd = iSylStart + (int) ((double) tsSylDuration / tsSentenceDuration * f0AndInterpol.length) + 1;
						if (iSylEnd > approx.length)
							iSylEnd = approx.length;
						// System.out.println("Syl "+s+" from "+iSylStart+" to "+iSylEnd+" out of "+f0AndInterpol.length);
						double[] sylF0 = new double[iSylEnd - iSylStart];
						float[] coeffs = contours.getFeatureVector(iSylVowel).getContinuousFeatures();
						double[] sylPred = Polynomial.generatePolynomialValues(ArrayUtils.copyFloat2Double(coeffs), sylF0.length,
								0, 1);
						System.arraycopy(sylPred, 0, approx, iSylStart, sylPred.length);
					}
					for (int j = 0; j < approx.length; j++) {
						approx[j] = Math.exp(approx[j]);
					}
					f0Graph.addDataSeries(approx, Color.RED, FunctionGraph.DRAW_LINE, -1);
					System.out.println();

					if (haveUnitLogF0) {
						double[] unitF0 = new double[f0Array.length];
						Arrays.fill(unitF0, Double.NaN);
						for (int u = 0; u + iSentenceStart <= iSentenceEnd; u++) {
							FeatureVector localFV = features.getFeatureVector(u + iSentenceStart);
							long tsUnitStart = units.getUnit(u + iSentenceStart).startTime;
							long tsUnitDuration = units.getUnit(u + iSentenceStart).duration;
							int iUnitStartInArray = (int) (unitF0.length * (tsUnitStart - tsSentenceStart) / tsSentenceDuration);
							int iUnitDurationInArray = (int) (unitF0.length * tsUnitDuration / tsSentenceDuration);
							// while (iUnitDurationInArray+iUnitStartInArray>unitF0.length) iUnitDurationInArray--;
							if (iUnitDurationInArray > 0) {
								float logF0 = localFV.getContinuousFeature(fiUnitLogF0);
								float logF0delta = localFV.getContinuousFeature(fiUnitLogF0delta);
								double[] coeffs = new double[2];
								// logF0 is value at 0.5, logF0delta is slope
								// coeffs[0] is slope, coeffs[1] is value at 0 => coeffs[1] + 0.5*slope = logF0
								coeffs[0] = logF0delta;
								coeffs[1] = logF0 - 0.5 * logF0delta;
								double[] pred = Polynomial.generatePolynomialValues(coeffs, iUnitDurationInArray, 0, 1);
								System.arraycopy(pred, 0, unitF0, iUnitStartInArray, iUnitDurationInArray);
								iUnitStartInArray += iUnitDurationInArray;
							}
						}
						for (int j = 0; j < unitF0.length; j++) {
							unitF0[j] = Math.exp(unitF0[j]);
						}
						f0Graph.addDataSeries(unitF0, Color.BLACK, FunctionGraph.DRAW_LINE, -1);

					}
				}
				try {
					ap.join();
					Thread.sleep(4000);
				} catch (InterruptedException ie) {
				}
				iSentenceStart = -1;
				iSentenceEnd = -1;
				iSylStarts.clear();
				iSylEnds.clear();
				iSylVowels.clear();
			}
		}

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
		F0PolynomialInspector acfeatsWriter = new F0PolynomialInspector();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
