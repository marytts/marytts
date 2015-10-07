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
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.Datagram;
import marytts.util.data.DatagramDoubleDataSource;
import marytts.util.data.MaryHeader;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * NOT COMPLETED (USEFUL FOR FUTURE)
 * 
 * @author sathish
 *
 */
public class VocalizationF0PolyFeatureFileWriter extends VoiceImportComponent {
	protected File maryDir;
	protected FeatureFileReader features;
	protected FeatureDefinition inFeatureDefinition;
	protected File outFeatureFile;
	protected FeatureDefinition outFeatureDefinition;
	protected VocalizationUnitFileReader listenerUnits;
	protected TimelineReader audio;
	protected DatabaseLayout db = null;
	protected int percent = 0;

	private final String name = "F0PolynomialFeatureFileWriter";
	public final String UNITFILE = name + ".unitFile";
	public final String WAVETIMELINE = name + ".waveTimeLine";
	public final String FEATUREFILE = name + ".featureFile";
	public final String F0FEATUREFILE = name + ".f0FeatureFile";
	public final String POLYNOMORDER = name + ".polynomOrder";
	public final String SHOWGRAPH = name + ".showGraph";
	public final String INTERPOLATE = name + ".interpolate";
	public final String MINPITCH = name + ".minPitch";
	public final String MAXPITCH = name + ".maxPitch";

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
			props.put(FEATUREFILE, fileDir + "halfphoneFeatures" + maryExt);
			props.put(F0FEATUREFILE, fileDir + "vocalizationF0Polynomials" + maryExt);
			props.put(POLYNOMORDER, "3");
			props.put(SHOWGRAPH, "false");
			props.put(INTERPOLATE, "true");
			if (db.getProp(db.GENDER).equals("female")) {
				props.put(MINPITCH, "100");
				props.put(MAXPITCH, "600");
			} else {
				props.put(MINPITCH, "60");
				props.put(MAXPITCH, "400");
			}
		}
		return props;
	}

	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(UNITFILE, "file containing all halfphone units");
			props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them");
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
			maryDir.mkdirs();
			System.out.println("Created the output directory [" + (db.getProp(db.FILEDIR)) + "] to store the feature file.");
		}
		listenerUnits = new VocalizationUnitFileReader(getProp(UNITFILE));
		audio = new TimelineReader(getProp(WAVETIMELINE));

		// features = new FeatureFileReader(getProp(FEATUREFILE));
		// inFeatureDefinition = features.getFeatureDefinition();
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
		logger.debug("Number of processed units: " + listenerUnits.getNumberOfUnits());

		FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(getProp(F0FEATUREFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == listenerUnits.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	/**
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
		int numUnits = listenerUnits.getNumberOfUnits();
		int unitSampleRate = listenerUnits.getSampleRate();
		int audioSampleRate = audio.getSampleRate();
		boolean showGraph = Boolean.parseBoolean(getProp(SHOWGRAPH));
		boolean interpolate = Boolean.parseBoolean(getProp(INTERPOLATE));
		int polynomOrder = Integer.parseInt(getProp(POLYNOMORDER));
		float[] zeros = new float[polynomOrder + 1];
		int unitIndex = 0;

		out.writeInt(numUnits);
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
		if (showGraph) {
			f0Graph = new FunctionGraph(0, 1, new double[1]);
			f0Graph.setYMinMax(50, 300);
			f0Graph.setPrimaryDataSeriesStyle(Color.BLUE, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_FULLCIRCLE);
			jf = f0Graph.showInJFrame("Sentence", false, true);
		}

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
				long tsSentenceStart = listenerUnits.getUnit(iSentenceStart).startTime;
				long tsSentenceEnd = listenerUnits.getUnit(iSentenceEnd).startTime + listenerUnits.getUnit(iSentenceEnd).duration;
				long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
				Datagram[] sentenceData = audio.getDatagrams(tsSentenceStart, tsSentenceDuration);
				DatagramDoubleDataSource ddds = new DatagramDoubleDataSource(sentenceData);
				double[] sentenceAudio = ddds.getAllData();
				AudioPlayer ap = null;
				if (showGraph) {
					ap = new AudioPlayer(new DDSAudioInputStream(new BufferedDoubleDataSource(sentenceAudio), new AudioFormat(
							AudioFormat.Encoding.PCM_SIGNED, audioSampleRate, // samples per second
							16, // bits per sample
							1, // mono
							2, // nr. of bytes per frame
							audioSampleRate, // nr. of frames per second
							true))); // big-endian;))
					ap.start();
				}
				PitchFileHeader params = new PitchFileHeader();
				params.fs = audioSampleRate;
				params.minimumF0 = Double.parseDouble(getProp(MINPITCH));
				params.maximumF0 = Double.parseDouble(getProp(MAXPITCH));
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
					if (showGraph) {
						f0Graph.updateData(0, tsSentenceDuration / (double) audioSampleRate / f0Array.length, f0Array);
						jf.repaint();
					}

					double[] f0AndInterpol;
					if (interpolate) {
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
						if (showGraph) {
							f0Graph.addDataSeries(interpol, Color.GREEN, FunctionGraph.DRAW_DOTS, FunctionGraph.DOT_EMPTYCIRCLE);
							jf.repaint();
						}
					} else {
						f0AndInterpol = f0Array.clone();
					}

					for (int j = 0; j < f0AndInterpol.length; j++) {
						if (f0AndInterpol[j] == 0)
							f0AndInterpol[j] = Double.NaN;
						else
							f0AndInterpol[j] = Math.log(f0AndInterpol[j]);
					}
					double[] approx = new double[f0Array.length];
					Arrays.fill(approx, Double.NaN);
					for (int s = 0; s < iSylStarts.size(); s++) {
						long tsSylStart = listenerUnits.getUnit(iSylStarts.get(s)).startTime;
						long tsSylEnd = listenerUnits.getUnit(iSylEnds.get(s)).startTime
								+ listenerUnits.getUnit(iSylEnds.get(s)).duration;
						long tsSylDuration = tsSylEnd - tsSylStart;
						int iSylVowel = iSylVowels.get(s);
						// now map time to position in f0AndInterpol array:
						int iSylStart = (int) (((double) (tsSylStart - tsSentenceStart) / tsSentenceDuration) * f0AndInterpol.length);
						assert iSylStart >= 0;
						int iSylEnd = iSylStart + (int) ((double) tsSylDuration / tsSentenceDuration * f0AndInterpol.length) + 1;
						if (iSylEnd > f0AndInterpol.length)
							iSylEnd = f0AndInterpol.length;
						// System.out.println("Syl "+s+" from "+iSylStart+" to "+iSylEnd+" out of "+f0AndInterpol.length);
						double[] sylF0 = new double[iSylEnd - iSylStart];
						System.arraycopy(f0AndInterpol, iSylStart, sylF0, 0, sylF0.length);
						double[] coeffs = Polynomial.fitPolynomial(sylF0, polynomOrder);
						if (coeffs != null) {
							if (showGraph) {
								double[] sylPred = Polynomial.generatePolynomialValues(coeffs, sylF0.length, 0, 1);
								System.arraycopy(sylPred, 0, approx, iSylStart, sylPred.length);
							}
							// Write coefficients to file
							while (unitIndex < iSylVowel) {
								FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
								outFV.writeTo(out);
								unitIndex++;
							}
							float[] fcoeffs = ArrayUtils.copyDouble2Float(coeffs);
							// System.out.print("Polynomial values (unit "+unitIndex+") ");
							// for (int p=0; p<fcoeffs.length; p++) {
							// System.out.print(", "+fcoeffs[p]);
							// }
							// System.out.println();
							FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, fcoeffs);
							outFV.writeTo(out);
							unitIndex++;
						}
					}
					/*
					 * System.out.print("Approximation values that are zero: "); for (int j=0; j<approx.length; j++) { if
					 * (approx[j] == 0) System.out.print(j+" "); } System.out.println();
					 */
					if (showGraph) {
						for (int j = 0; j < approx.length; j++) {
							approx[j] = Math.exp(approx[j]);
						}
						f0Graph.addDataSeries(approx, Color.RED, FunctionGraph.DRAW_LINE, -1);
					}

				}
				if (showGraph) {
					try {
						ap.join();
						Thread.sleep(2000);
					} catch (InterruptedException ie) {
					}
				}
				iSentenceStart = -1;
				iSentenceEnd = -1;
				iSylStarts.clear();
				iSylEnds.clear();
				iSylVowels.clear();
			}
		}
		while (unitIndex < numUnits) {
			FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
			outFV.writeTo(out);
			unitIndex++;
		}

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
		new MaryHeader(MaryHeader.LISTENERFEATS).writeTo(out);
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
		VocalizationF0PolyFeatureFileWriter acfeatsWriter = new VocalizationF0PolyFeatureFileWriter();
		DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
		acfeatsWriter.compute();
	}

}
