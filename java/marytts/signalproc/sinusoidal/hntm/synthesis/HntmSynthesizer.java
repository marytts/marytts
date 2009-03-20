/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.sinusoidal.hntm.synthesis;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.SinusoidalTrack;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartLpc;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartPseudoHarmonic;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartRegularizedCeps;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.hntm.modification.HntmDurationModifier;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.HanningWindow;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * Synthesis using harmonics plus noise (and possibly plus transients) model
 * 
 * @author Oytun T&uumlrk
 *
 */
public class HntmSynthesizer {

    public static final double ENERGY_TRIANGLE_LOWER_VALUE = 1.0;
    public static final double ENERGY_TRIANGLE_UPPER_VALUE = 0.5;
    public static final double NUM_PERIODS_NOISE = 2.0;
    public static final float NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS = 0.050f;
    public static final float NOISE_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.010f;
    public static final float HARMONIC_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.002f;
    public static final float UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS = 0.005f;

    public HntmSynthesizer()
    {

    }

    public HntmSynthesizedSignal synthesize(HntmSpeechSignal hntmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        //Handle time scaling by adjusting synthesis times
        HntmSpeechSignal hntmSignalMod = HntmDurationModifier.modify(hntmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        HntmSynthesizedSignal s = new HntmSynthesizedSignal();
        s.harmonicPart = HarmonicPartLinearPhaseInterpolatorSynthesizer.synthesize(hntmSignalMod, pScales, pScalesTimes);
        
        float[] times = hntmSignalMod.getAnalysisTimes();
     
        //Harmonic part energy contour normalization
        float[] targetEnergyContour = hntmSignalMod.getTargetEnergyContour();
        float[] currentContour = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        s.harmonicPart = SignalProcUtils.normalizeAverageSampleEnergyContour(s.harmonicPart, times, currentContour, targetEnergyContour, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        float[] currentContour2 = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        //

        //Synthesize noise part
        if (hntmSignalMod.frames[0].n instanceof FrameNoisePartLpc)
            s.noisePart = NoisePartLpcSynthesizer.synthesize(hntmSignalMod);
        else if (hntmSignalMod.frames[0].n instanceof FrameNoisePartRegularizedCeps)
            s.noisePart = NoisePartRegularizedCepsSynthesizer.synthesize(hntmSignalMod);
        else if (hntmSignalMod.frames[0].n instanceof FrameNoisePartPseudoHarmonic)
            s.noisePart = NoisePartPseudoHarmonicSynthesizer.synthesize(hntmSignalMod, pScales, pScalesTimes);
        //
        
        //Synthesize transients
        if (hntmSignalMod instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients!=null)
            s.transientPart = TransientPartSynthesizer.synthesize((HntmPlusTransientsSpeechSignal)hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        float[] averageSampleEnergyContourHarmonic = null;
        float[] averageSampleEnergyContourNoise = null;
        float[] averageSampleEnergyContourTransient = null;
        
        if (s.harmonicPart!=null)
           averageSampleEnergyContourHarmonic = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        if (s.noisePart!=null)
            averageSampleEnergyContourNoise = SignalProcUtils.getAverageSampleEnergyContour(s.noisePart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        if (s.transientPart!=null)
            averageSampleEnergyContourTransient = SignalProcUtils.getAverageSampleEnergyContour(s.transientPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);

        return s;
    }

    public static void mainSingleFile(String wavFile) throws UnsupportedAudioFileException, IOException
    {
        //File input
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        x = MathUtils.multiply(x, 32768.0);
        //

        //Analysis
        HntmAnalyzer ha = new HntmAnalyzer();
        
        int model = HntmAnalyzer.HARMONICS_PLUS_NOISE;
        //int model = HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE;
        
        //int noisePartRepresentation = HntmAnalyzer.LPC;
        int noisePartRepresentation = HntmAnalyzer.REGULARIZED_CEPS;
        //int noisePartRepresentation = HntmAnalyzer.PSEUDO_HARMONIC;

        F0ReaderWriter f0 = null;
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        if (FileUtils.exists(strPitchFile))
        {
            f0 = new F0ReaderWriter(strPitchFile);
            //Arrays.fill(f0.contour, 100.0);
        }
        
        Labels labels = null;
        if (model == HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
        {
            String strLabFile = StringUtils.modifyExtension(wavFile, ".lab"); 
            if (FileUtils.exists(strLabFile)!=true) //Labels required for transients analysis (unless we design an automatic algorithm)
            {
                System.out.println("Error! Labels required for transient analysis...");
                System.exit(1);
            }
            labels = new Labels(strLabFile);
        }
         
        int fftSize = 4096;
        int harmonicPartAnalysisMethod = HntmAnalyzer.TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS;
        //int harmonicPartAnalysisMethod = HntmAnalyzer.FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS;
        
        HntmSpeechSignal hnmSignal = ha.analyze(x, samplingRate, f0, labels, fftSize, model, noisePartRepresentation, harmonicPartAnalysisMethod);
        //

        if (hnmSignal!=null)
        {
            //Synthesis
            //float[] tScales = {0.3f, 0.5f, 1.0f, 1.5f, 2.5f};
            float[] tScales = {1.0f};
            //float[] tScalesTimes = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f};
            float[] tScalesTimes = null;
            
            float[] pScales = {1.0f};
            float[] pScalesTimes = null;

            HntmSynthesizer hs = new HntmSynthesizer();
            HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);

            double hGain = 1.0;
            double nGain = 1.0;
            double tGain = 1.0;
            if (xhat.harmonicPart!=null)
            {
                xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, hGain);
                //xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, MathUtils.absMax(x)/MathUtils.absMax(xhat.harmonicPart));
                //MaryUtils.plot(xhat.harmonicPart);
            }
            
            if (xhat.noisePart!=null)
            {
                xhat.noisePart = MathUtils.multiply(xhat.noisePart, nGain);
              //MaryUtils.plot(xhat.noisePart);
            }
            
            if (xhat.transientPart!=null)
            {
                xhat.transientPart = MathUtils.multiply(xhat.transientPart, tGain);
                //MaryUtils.plot(xhat.transientPart);
            }

            double[] y = SignalProcUtils.addSignals(xhat.harmonicPart, xhat.noisePart);
            y = SignalProcUtils.addSignals(y, xhat.transientPart);
            //y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));
            //MaryUtils.plot(x);
            //MaryUtils.plot(xhat.harmonicPart);
            //MaryUtils.plot(xhat.noisePart);
            //MaryUtils.plot(xhat.transientPart);
            //MaryUtils.plot(y);
            
            //double[] d = SignalProcUtils.addSignals(x, 1.0f, xhat.harmonicPart, -1.0f);
            
            /*
            for (int i=0; i<300; i+=100)
            {
                int startIndex = i;
                int len = 100;
                double[] xPart = ArrayUtils.subarray(x, startIndex, len);
                double[] hPart = ArrayUtils.subarray(xhat.harmonicPart, startIndex, len);
                double[] dPart = SignalProcUtils.addSignals(xPart, 1.0f, hPart, -1.0f);
                MaryUtils.plot(xPart);
                MaryUtils.plot(hPart);
                MaryUtils.plot(dPart);
            }
            */

            //xhat.noisePart = ArrayUtils.subarray(hpf, 0, hpf.length);
            //

            //File output
            DDSAudioInputStream outputAudio = null;
            String outFileName = null;
            String strExt = "";
            String modelName = "";
            if (model==HntmAnalyzer.HARMONICS_PLUS_NOISE)
                modelName = "hnml";
            else if (model==HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
                modelName = "hntml";

            //y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));
            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(y,32768.0)), inputAudio.getFormat());
            outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Resynth" + strExt + ".wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

            if (xhat.harmonicPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.harmonicPart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Harmonic" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }
            
            if (xhat.noisePart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.noisePart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Noise" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }

            if (xhat.transientPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(xhat.transientPart, 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Transient" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }
            
            if (xhat.harmonicPart!=null)
            {
                outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(MathUtils.divide(SignalProcUtils.addSignals(x, 1.0, xhat.harmonicPart, -1.0), 32768.0)), inputAudio.getFormat());
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "OrigMinusHarmonic" + strExt + ".wav";
                AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            }

            //MaryUtils.plot(xhat.harmonicPart);
            //MaryUtils.plot(xhat.noisePart);
            //MaryUtils.plot(y);

            //if (nEstimate!=null)
            //{
            //    outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(nEstimate), inputAudio.getFormat());
            //    outFileName = args[0].substring(0, args[0].length()-4) + "_" + modelName + "Diff.wav";
            //    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            //}
            //
        }
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        if (FileUtils.isDirectory(args[0])) //Process folder
        {
            String[] fileList = FileUtils.getFileList(args[0], "wav");
            if (fileList!=null)
            {
                for (int i=0; i<fileList.length; i++)
                {
                    mainSingleFile(fileList[i]);
                    System.out.println("HNM processing completed for file " + String.valueOf(i+1) + " of " + String.valueOf(fileList.length));
                }
            }
            else
                System.out.println("No wav files found!");
        }
        else //Process file
            mainSingleFile(args[0]);
    }
}
