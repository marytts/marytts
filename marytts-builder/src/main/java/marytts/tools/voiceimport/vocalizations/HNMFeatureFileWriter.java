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

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.exceptions.MaryConfigurationException;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.RegularizedCepstrumEstimator;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.util.data.MaryHeader;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.BasenameList;
import marytts.util.math.MathUtils;
import marytts.vocalizations.HNMFeatureFileReader;
import marytts.vocalizations.VocalizationUnitFileReader;

/**
 * A component to extract and write HNM features of all vocalizations into a single file
 * 
 * @author sathish
 *
 */
public class HNMFeatureFileWriter extends VoiceImportComponent {

	private String waveExt = ".wav";
	private String ptcExt = ".ptc";
	private String hnmAnalysisFileExt = ".ana";
	private int progress = 0;
	protected DatabaseLayout db = null;
	protected BasenameList bnlVocalizations;

	private final String WAVEDIR = getName() + ".vocalizationWaveDir";
	private final String HNMANADIR = getName() + ".hnmAnalysisDir";
	private final String OUTHNMFILE = getName() + ".hnmAnalysisTimelineFile";
	private final String UNITFILE = getName() + ".unitFile";

	private HntmSynthesizerParams synthesisParamsBeforeNoiseAnalysis;
	private HntmAnalyzerParams analysisParams;
	private PitchFileHeader f0Params;

	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(UNITFILE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_units" + db.getProp(db.MARYEXT));
			props.put(HNMANADIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "hna" + File.separator);
			props.put(WAVEDIR, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "wav");
			props.put(OUTHNMFILE, db.getProp(db.VOCALIZATIONSDIR) + File.separator + "files" + File.separator
					+ "vocalization_hnm_analysis" + db.getProp(db.MARYEXT));
		}
		return props;
	}

	@Override
	protected void setupHelp() {
		if (props2Help == null) {
			props2Help = new TreeMap<String, String>();
			props2Help.put(UNITFILE, "unit file representing all vocalizations");
			props2Help.put(HNMANADIR, "HNM features directory");
			props2Help.put(WAVEDIR, "vocalization wave files directory");
			props2Help.put(OUTHNMFILE, "a single file to write all HNM features");
		}
	}

	@Override
	public boolean compute() throws UnsupportedAudioFileException, IOException, MaryConfigurationException {

		VocalizationUnitFileReader listenerUnits = new VocalizationUnitFileReader(getProp(UNITFILE));

		// sanity checker
		if (listenerUnits.getNumberOfUnits() != bnlVocalizations.getLength()) {
			throw new MaryConfigurationException(
					"The number of vocalizations given in basename list and number of units in unit file should be matched");
		}

		F0TrackerAutocorrelationHeuristic pitchDetector = new F0TrackerAutocorrelationHeuristic(f0Params);
		HntmAnalyzer ha = new HntmAnalyzer();
		DataOutputStream outStream = new DataOutputStream(new FileOutputStream(new File(getProp(OUTHNMFILE))));
		writeHeaderTo(outStream);
		outStream.writeInt(listenerUnits.getNumberOfUnits());

		for (int i = 0; i < bnlVocalizations.getLength(); i++) {
			progress = i * 100 / bnlVocalizations.getLength();
			String wavFile = getProp(WAVEDIR) + File.separator + bnlVocalizations.getName(i) + waveExt;
			String pitchFile = getProp(HNMANADIR) + File.separator + bnlVocalizations.getName(i) + ptcExt;
			String analysisResultsFile = getProp(HNMANADIR) + File.separator + bnlVocalizations.getName(i) + hnmAnalysisFileExt;
			PitchReaderWriter f0 = pitchDetector.pitchAnalyzeWavFile(wavFile, pitchFile);
			AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
			int samplingRate = (int) inputAudio.getFormat().getSampleRate();
			AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
			double[] x = signal.getAllData();
			x = MathUtils.multiply(x, 32768.0);
			HntmSpeechSignal hnmSignal = ha.analyze(x, samplingRate, f0, null, analysisParams,
					synthesisParamsBeforeNoiseAnalysis, analysisResultsFile);
			hnmSignal.write(outStream);
		}
		outStream.close();

		HNMFeatureFileReader tester = new HNMFeatureFileReader(getProp(OUTHNMFILE));
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
	 * Initialize this component
	 * 
	 * @throws Exception
	 *             Exception
	 */
	@Override
	protected void initialiseComp() throws Exception {

		createDirectoryifNotExists(getProp(HNMANADIR));

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
			throw new MaryConfigurationException("Problem with basename list " + e);
		}
		f0Params = new PitchFileHeader();
		analysisParams = new HntmAnalyzerParams();

		analysisParams.harmonicModel = HntmAnalyzerParams.HARMONICS_PLUS_NOISE;
		analysisParams.noiseModel = HntmAnalyzerParams.WAVEFORM;
		analysisParams.useHarmonicAmplitudesDirectly = true;
		analysisParams.harmonicSynthesisMethodBeforeNoiseAnalysis = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;
		analysisParams.regularizedCepstrumWarpingMethod = RegularizedCepstrumEstimator.REGULARIZED_CEPSTRUM_WITH_POST_MEL_WARPING;

		synthesisParamsBeforeNoiseAnalysis = new HntmSynthesizerParams();
		synthesisParamsBeforeNoiseAnalysis.harmonicPartSynthesisMethod = HntmSynthesizerParams.LINEAR_PHASE_INTERPOLATION;
	}

	/**
	 * Create new directory if the directory doesn't exist
	 * 
	 * @param dirName
	 *            dirName
	 * @throws Exception
	 *             Exception
	 */
	private void createDirectoryifNotExists(String dirName) throws Exception {
		if (!(new File(dirName)).exists()) {
			System.out.println(dirName + " directory does not exist; ");
			if (!(new File(dirName)).mkdirs()) {
				throw new Exception("Could not create directory " + dirName);
			}
			System.out.println("Created successfully.\n");
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
	}

	/**
	 * Return this voice import component name
	 * 
	 * @return "HNMfeatureFileWriter"
	 */
	@Override
	public String getName() {
		return "HNMFeatureFileWriter";
	}

	/**
	 * Return the progress of this component
	 * 
	 * @return this.progress
	 */
	@Override
	public int getProgress() {
		return this.progress;
	}

	/**
	 * @param args
	 *            args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
