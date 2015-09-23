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

package marytts.tools.voiceimport.vocalizations;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.exceptions.MaryConfigurationException;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.SPTKPitchReaderWriter;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.MaryHeader;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.BasenameList;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;
import marytts.vocalizations.VocalizationIntonationReader;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * Vocalization intonation writer into a time-line file This class can create a timeline file with intonation contours and thier
 * polynomial coeffs
 * 
 * @author sathish pammi
 *
 */
public class VocalizationIntonationWriter extends VoiceImportComponent {

	protected String vocalizationsDir;
	protected BasenameList bnlVocalizations;
	protected VocalizationUnitFileReader listenerUnits;

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public final String PITCHDIR = getName() + ".pitchDir";
	public final String WAVEDIR = getName() + ".inputWaveDir";
	public final String POLYORDER = getName() + ".polynomialOrder";
	public final String ISEXTERNALF0 = getName() + ".isExternalF0Usage";
	public final String EXTERNALF0FORMAT = getName() + ".externalF0Format";
	public final String EXTERNALEXT = getName() + ".externalF0Extention";
	public final String UNITFILE = getName() + ".unitFile";
	public final String SKIPSIZE = getName() + ".skipSize";
	public final String WINDOWSIZE = getName() + ".windowSize";
	public final String F0TIMELINE = getName() + ".intonationTimeLineFile";
	public final String F0FEATDEF = getName() + ".intonationFeatureDefinition";

	public String getName() {
		return "VocalizationIntonationWriter";
	}

	@Override
	protected void initialiseComp() {

		String timelineDir = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files";
		if (!(new File(timelineDir)).exists()) {
			System.out.println("vocalizations/files directory does not exist; ");
			if (!(new File(timelineDir)).mkdirs()) {
				throw new Error("Could not create vocalizations/files");
			}
			System.out.println("Created successfully.\n");
		}

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

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(UNITFILE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_units" + db.getProp(db.MARYEXT));
			props.put(POLYORDER, "3");
			props.put(ISEXTERNALF0, "true");
			props.put(EXTERNALF0FORMAT, "sptk");
			props.put(EXTERNALEXT, ".lf0");
			props.put(PITCHDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "lf0");
			props.put(SKIPSIZE, "0.005");
			props.put(WINDOWSIZE, "0.005");
			props.put(F0TIMELINE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_intonation" + db.getProp(db.MARYEXT));
			props.put(F0FEATDEF, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "features" + File.separator
					+ "vocalization_f0_feature_definition.txt");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();

	}

	/**
	 * Reads and concatenates a list of waveforms into one single timeline file.
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	@Override
	public boolean compute() throws IOException, MaryConfigurationException {

		listenerUnits = new VocalizationUnitFileReader(getProp(UNITFILE));

		// write features into timeline file
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(getProp(F0TIMELINE)))));
		writeHeaderTo(out);
		writeUnitFeaturesTo(out);
		out.close();

		VocalizationIntonationReader tester = new VocalizationIntonationReader(getProp(F0TIMELINE));
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
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 */
	protected void writeUnitFeaturesTo(DataOutput out) throws IOException {

		int numUnits = listenerUnits.getNumberOfUnits();
		float windowSize = new Float(getProp(WINDOWSIZE)).floatValue();
		float skipSize = new Float(getProp(SKIPSIZE)).floatValue();

		out.writeFloat(windowSize);
		out.writeFloat(skipSize);
		out.writeInt(numUnits);

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {

			double[] f0Array = null;

			try {
				f0Array = getVocalizationF0(bnlVocalizations.getName(i), false);
			} catch (UnsupportedAudioFileException e) {
				e.printStackTrace();
			}

			// write coeffs followed by its order
			double[] coeffs = getPolynomialCoeffs(f0Array);
			if (coeffs == null) {
				out.writeInt(0);
			} else {
				out.writeInt(coeffs.length);
				for (int j = 0; j < coeffs.length; j++) {
					out.writeFloat((float) coeffs[j]);
				}
			}

			// write f0 Array followed by f0 contour array size
			if (f0Array == null) {
				out.writeInt(0);
			} else {
				out.writeInt(f0Array.length);
				for (int j = 0; j < f0Array.length; j++) {
					out.writeFloat((float) f0Array[j]);
				}
			}
		}
	}

	/**
	 * get f0 contour of vocalization f0
	 * 
	 * @param baseName
	 *            baseName
	 * @param doInterpolate
	 *            doInterpolate
	 * @return interpolateF0Array(f0Array) if doInterpolate, f0Array otherwise
	 * @throws UnsupportedAudioFileException
	 *             UnsupportedAudioFileException
	 * @throws IOException
	 *             IOException
	 */
	private double[] getVocalizationF0(String baseName, boolean doInterpolate) throws UnsupportedAudioFileException, IOException {

		double[] f0Array = null;

		if ("true".equals(getProp(ISEXTERNALF0))) {

			String externalFormat = getProp(EXTERNALF0FORMAT);
			String externalExt = getProp(EXTERNALEXT);
			System.out.println("Loading f0 contour from file : " + getProp(PITCHDIR) + File.separator + baseName + externalExt);
			if ("sptk".equals(externalFormat)) {
				String fileName = getProp(PITCHDIR) + File.separator + baseName + externalExt;
				SPTKPitchReaderWriter sprw = new SPTKPitchReaderWriter(fileName);
				f0Array = sprw.getF0Contour();
			} else if ("ptc".equals(externalFormat)) {
				String fileName = getProp(PITCHDIR) + File.separator + baseName + externalExt;
				PitchReaderWriter sprw = new PitchReaderWriter(fileName);
				f0Array = sprw.contour;
			}
		} else {
			PitchFileHeader params = new PitchFileHeader();
			F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
			String waveFile = db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav" + baseName + db.getProp(db.WAVEXT);
			System.out.println("Computing f0 contour from wave file: " + waveFile);
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(waveFile));

			// Enforce PCM_SIGNED encoding
			if (!inputAudio.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
				inputAudio = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, inputAudio);
			}

			int audioSampleRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			double[] sentenceAudio = signal.getAllData();
			tracker.pitchAnalyze(new BufferedDoubleDataSource(sentenceAudio));
			// double frameShiftTime = tracker.getSkipSizeInSeconds();
			f0Array = tracker.getF0Contour();
		}

		if (doInterpolate) {
			return interpolateF0Array(f0Array);
		}

		return f0Array;
	}

	/**
	 * to get polynomial coeffs of f0 contour
	 * 
	 * @param f0Array
	 *            f0Array
	 * @return null if f0Array == null, coeffs otherwise
	 */
	private double[] getPolynomialCoeffs(double[] f0Array) {

		if (f0Array == null) {
			return null;
		}

		f0Array = cutStartEndUnvoicedSegments(f0Array);
		double[] f0AndInterpolate = interpolateF0Array(f0Array);
		int polynomialOrder = (new Integer(getProp(POLYORDER))).intValue();
		double[] coeffs = Polynomial.fitPolynomial(f0AndInterpolate, polynomialOrder);
		return coeffs;
	}

	/**
	 * to interpolate F0 contour values
	 * 
	 * @param f0Array
	 *            f0Array
	 * @return null if f0Array == null, f0AndInterpolate
	 */
	private double[] interpolateF0Array(double[] f0Array) {

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

		double[] f0AndInterpolate = combineF0andInterpolate(f0Array, interpol);
		return f0AndInterpolate;
	}

	/**
	 * cut begin-end unvoiced segments
	 * 
	 * @param array
	 *            array
	 * @return null if array == null, newArray
	 */
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

		/*
		 * for ( int i=0; i<newArray.length; i++ ) { System.out.println(newArray[i]); }
		 * System.out.println("Resized from "+array.length+" to "+newArraySize);
		 */
		return newArray;
	}

	/**
	 * interpolate f0
	 * 
	 * @param f0Array
	 *            f0Array
	 * @param interpol
	 *            interpol
	 * @return f0AndInterpolate
	 */
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
	 * Write the header of this feature file to the given DataOutput
	 * 
	 * @param out
	 *            out
	 * @throws IOException
	 *             IOException
	 */
	protected void writeHeaderTo(DataOutput out) throws IOException {
		new MaryHeader(MaryHeader.LISTENERFEATS).writeTo(out);
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
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
