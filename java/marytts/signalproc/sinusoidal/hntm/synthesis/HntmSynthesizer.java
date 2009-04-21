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

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.sinusoidal.BaseSinusoidalSpeechSignal;
import marytts.signalproc.sinusoidal.PeakMatchedSinusoidalSynthesizer;
import marytts.signalproc.sinusoidal.SinusoidalTrack;
import marytts.signalproc.sinusoidal.SinusoidalTracks;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePart;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartLpc;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartPseudoHarmonic;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartRegularizedCeps;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartWaveform;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.pitch.HnmPitchVoicingAnalyzer;
import marytts.signalproc.sinusoidal.hntm.modification.HntmProsodyModifier;
import marytts.signalproc.sinusoidal.hntm.synthesis.hybrid.HarmonicsToTrackConverter;
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
    //Triangular noise envelope window for voiced segments
    public static final boolean APPLY_TRIANGULAR_NOISE_ENVELOPE_FOR_VOICED_PARTS = false;
    public static final double ENERGY_TRIANGLE_LOWER_VALUE = 1.0;
    public static final double ENERGY_TRIANGLE_UPPER_VALUE = 0.5;
    //
    
    public static final boolean NORMALIZE_HARMONIC_ENERGY_CONTOUR = true;
    
    public static final double NUM_PERIODS_NOISE = 2.0;
    public static final float NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS = 0.050f;
    public static final float NOISE_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.010f;
    public static final float HARMONIC_SYNTHESIS_TRANSITION_OVERLAP_IN_SECONDS = 0.002f;
    public static final float UNVOICED_VOICED_TRACK_TRANSITION_IN_SECONDS = 0.005f;
    
    public static final boolean WRITE_SEPARATE_TRACKS_TO_OUTPUT = true;
    
    public static final int LINEAR_PHASE_INTERPOLATION = 1;
    public static final int QUADRATIC_PHASE_INTERPOLATION = 2;
    
    public static boolean ADJUST_PHASES_AFTER_TIME_SCALING = false;
    public static boolean ADJUST_PHASES_AFTER_PITCH_SCALING = false;
    
    public static boolean APPLY_VOCAL_TRACT_NORMALIZATION_POST_PROCESSOR = true; 
    
    public HntmSynthesizer()
    {

    }

    public HntmSynthesizedSignal synthesize(HntmSpeechSignal hntmSignal, BasicProsodyModifierParams pmodParams, int harmonicPartSynthesisMethod)
    {
        BasicProsodyModifierParams pmodParamsOrig = new BasicProsodyModifierParams(pmodParams);
        
        //Handle time and pitch scaling by adjusting synthesis times
        HntmSpeechSignal hntmSignalMod = HntmProsodyModifier.modify(hntmSignal, pmodParams);
        //
        
        HntmSynthesizedSignal s = new HntmSynthesizedSignal();
        
        if (harmonicPartSynthesisMethod==LINEAR_PHASE_INTERPOLATION)
            s.harmonicPart = HarmonicPartLinearPhaseInterpolatorSynthesizer.synthesize(hntmSignalMod, pmodParams.pScales, pmodParams.pScalesTimes);
        else if (harmonicPartSynthesisMethod==QUADRATIC_PHASE_INTERPOLATION)
        {
            //Convert to pure sinusoidal tracks
            SinusoidalTracks st = HarmonicsToTrackConverter.convert(hntmSignalMod);
            //
            
            PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(hntmSignalMod.samplingRateInHz);
            s.harmonicPart = ss.synthesize(st, false); 
        }

        //Synthesize noise part
        //Search which type of noise part we have
        FrameNoisePart p = null;
        for (int i=0; i<hntmSignalMod.frames.length; i++)
        {
            if (hntmSignalMod.frames[i].n != null)
            {
                p = hntmSignalMod.frames[i].n;
                break;
            }
        }
        //
        
        if (p!=null)
        {
            if (p instanceof FrameNoisePartLpc)
                s.noisePart = NoisePartLpcSynthesizer.synthesize(hntmSignalMod, null); //"d:\\asaisa.wav");
            else if (p instanceof FrameNoisePartRegularizedCeps)
                s.noisePart = NoisePartRegularizedCepsSynthesizer.synthesize(hntmSignalMod);
            else if (p instanceof FrameNoisePartPseudoHarmonic)
                s.noisePart = NoisePartPseudoHarmonicSynthesizer.synthesize(hntmSignalMod, pmodParams.pScales, pmodParams.pScalesTimes);
            else if (p instanceof FrameNoisePartWaveform)
            {
                s.noisePart = NoisePartWaveformSynthesizer.synthesize(hntmSignalMod);

                //Harmonic part energy contour normalization
                if (NORMALIZE_HARMONIC_ENERGY_CONTOUR)
                {
                    float[] times = hntmSignalMod.getAnalysisTimes();
                    float[] originalContour = hntmSignalMod.getOriginalAverageSampleEnergyContour();
                    float[] noiseContour = SignalProcUtils.getAverageSampleEnergyContour(s.noisePart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
                    float[] currentHarmonicContour = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
                    float[] targetHarmonicContour = MathUtils.subtract(originalContour, noiseContour);
                    s.harmonicPart = SignalProcUtils.normalizeAverageSampleEnergyContour(s.harmonicPart, times, currentHarmonicContour, targetHarmonicContour, hntmSignalMod.samplingRateInHz, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
                }
                //
            }
        }
        //
        
        //Synthesize transients
        if (hntmSignalMod instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients!=null)
            s.transientPart = TransientPartSynthesizer.synthesize((HntmPlusTransientsSpeechSignal)hntmSignalMod);
        //
        
        s.output = SignalProcUtils.addSignals(s.harmonicPart, s.noisePart);
        s.output = SignalProcUtils.addSignals(s.output, s.transientPart);
        
        if (APPLY_VOCAL_TRACT_NORMALIZATION_POST_PROCESSOR)
        {
            double[][] mappedTgtLpcs = hntmSignalMod.getLpcsAll();
            s.output = SignalProcUtils.normalizeVocalTract(s.output, hntmSignalMod.getAnalysisTimes(), mappedTgtLpcs, HntmAnalyzer.NOISE_ANALYSIS_WINDOW_TYPE, HntmAnalyzer.NOISE_ANALYSIS_WINDOW_DURATION_IN_SECONDS, mappedTgtLpcs[0].length, hntmSignalMod.samplingRateInHz, HntmAnalyzer.PREEMPHASIS_COEF_NOISE);
        }
        
        return s;
    }

    public static void mainSingleFile(String wavFile) throws UnsupportedAudioFileException, IOException
    {
        /*
        //float[] tScales = {0.3f, 0.5f, 1.0f, 1.5f, 2.5f};
        float[] tScales = {1.0f}; //{0.6f}; {1.0f}; {2.0f};
        //float[] tScalesTimes = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f};
        float[] tScalesTimes = null;
        
        float[] pScales = {2.0f}; //{0.7f}; {1.0f}; {1.2f}; {2.0f};
        //float[] pScalesTimes = {0.05f, 1.0f, 2.0f};
        float[] pScalesTimes = null;
        */
        
        /*
        float[][] pScalesArray = new float[6][1];
        float[][] tScalesArray = new float[6][1];
        pScalesArray[0][0] = 0.7f; tScalesArray[0][0] = 1.0f;
        pScalesArray[1][0] = 1.0f; tScalesArray[1][0] = 1.0f;
        pScalesArray[2][0] = 1.2f; tScalesArray[2][0] = 1.0f;
        pScalesArray[3][0] = 2.0f; tScalesArray[3][0] = 1.0f;
        pScalesArray[4][0] = 1.0f; tScalesArray[4][0] = 0.6f;
        pScalesArray[5][0] = 1.0f; tScalesArray[5][0] = 2.0f;
        */
        
        float[][] pScalesArray = new float[1][1];
        float[][] tScalesArray = new float[1][1];
        pScalesArray[0][0] = 1.5f; tScalesArray[0][0] = 1.0f;
        
        //float[] tScalesTimes = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f};
        float[] tScalesTimes = null;
        //float[] pScalesTimes = {0.05f, 1.0f, 2.0f};
        float[] pScalesTimes = null;
        
        //File input
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        x = MathUtils.multiply(x, 32768.0);
        //

        //Analysis
        HntmAnalyzer ha = new HntmAnalyzer();
        
        //String strComment = null;
        String strComment = "autoMWF";
        
        int model = HntmAnalyzer.HARMONICS_PLUS_NOISE;
        //int model = HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE;
        
        int harmonicPartSynthesisMethod = HntmSynthesizer.LINEAR_PHASE_INTERPOLATION;
        //int harmonicPartSynthesisMethod = HntmSynthesizer.QUADRATIC_PHASE_INTERPOLATION;
        
        //int noisePartRepresentation = HntmAnalyzer.LPC;
        //int noisePartRepresentation = HntmAnalyzer.REGULARIZED_CEPS;
        //int noisePartRepresentation = HntmAnalyzer.PSEUDO_HARMONIC;
        int noisePartRepresentation = HntmAnalyzer.HIGHPASS_WAVEFORM;
        
        PitchReaderWriter f0 = null;
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        if (FileUtils.exists(strPitchFile))
        {
            f0 = new PitchReaderWriter(strPitchFile);
            //Arrays.fill(f0.contour, 100.0);
        }
        
        Labels labels = null;
        String strLabFile = StringUtils.modifyExtension(wavFile, ".lab"); 
        if (model == HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
        {
            if (FileUtils.exists(strLabFile)!=true) //Labels required for transients analysis (unless we design an automatic algorithm)
            {
                System.out.println("Error! Labels required for transient analysis...");
                System.exit(1);
            }
            labels = new Labels(strLabFile);
        }
         
        //boolean isCopyPitch = true;
        //boolean isCopyDuration = true;
        //BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams(strPitchFile, strLabFile, "d:\\m0318_happy.ptc", "d:\\d:\\m0318_happy.lab", isCopyPitch, isCopyDuration); //Prosody from a target file
        //
        
        int fftSize = 4096;
        int harmonicPartAnalysisMethod = HntmAnalyzer.TIME_DOMAIN_CORRELATION_HARMONICS_ANALYSIS;
        //int harmonicPartAnalysisMethod = HntmAnalyzer.FREQUENCY_DOMAIN_PEAK_PICKING_HARMONICS_ANALYSIS;
        
        HntmSpeechSignal hnmSignal = ha.analyze(x, samplingRate, f0, labels, fftSize, model, noisePartRepresentation, harmonicPartAnalysisMethod);
        //
        
        for (int n=0; n<pScalesArray.length; n++)
        {
            if (hnmSignal!=null)
            {
                //BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams(tScalesArray[n], tScalesTimesArray[n], pScalesArray[n], pScalesTimesArray[n]); //Prosody from modification factors above
                BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams(tScalesArray[n], tScalesTimes, pScalesArray[n], pScalesTimes); //Prosody from modification factors above
                
                //Synthesis
                HntmSynthesizer hs = new HntmSynthesizer();
                HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, pmodParams, harmonicPartSynthesisMethod);

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
                
                double absMaxOutput = MathUtils.absMax(xhat.output);
                if (absMaxOutput>32767)
                {
                    System.out.println("Final output clipped re-scaling (abs max=" + String.valueOf(absMaxOutput) + ")");
                    xhat.output = MathUtils.multiply(xhat.output, 32767.0/absMaxOutput);
                }

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
                {
                    if (noisePartRepresentation == HntmAnalyzer.HIGHPASS_WAVEFORM)
                        modelName = "hwm";
                    else
                        modelName = "hnm";
                }
                else if (model==HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
                {
                    if (noisePartRepresentation == HntmAnalyzer.HIGHPASS_WAVEFORM)
                        modelName = "hwtm";
                    else
                        modelName = "hwtm";
                }
                
                if (strComment!=null)
                    modelName += "_" + strComment;
                
                if (pScalesArray[n][0]!=1.0f)
                    modelName += "_ps" + String.valueOf(pScalesArray[n][0]);
                if (tScalesArray[n][0]!=1.0f)
                    modelName += "_ts" + String.valueOf(tScalesArray[n][0]);

                //y = MathUtils.multiply(y, MathUtils.absMax(x)/MathUtils.absMax(y));
                outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + strExt + ".wav";
                FileUtils.writeWavFile(MathUtils.divide(xhat.output,32768.0), outFileName, inputAudio.getFormat());

                if (xhat.harmonicPart!=null)
                {
                    outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Harmonic" + strExt + ".wav";
                    xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, 32768.0/MathUtils.getAbsMax(xhat.harmonicPart));
                    FileUtils.writeWavFile(MathUtils.divide(xhat.harmonicPart, 32768.0), outFileName, inputAudio.getFormat());
                }

                if (xhat.noisePart!=null)
                {
                    outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Noise" + strExt + ".wav";
                    FileUtils.writeWavFile(MathUtils.divide(xhat.noisePart, 32768.0), outFileName, inputAudio.getFormat());
                }

                if (xhat.transientPart!=null)
                {
                    outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "Transient" + strExt + ".wav";
                    FileUtils.writeWavFile(MathUtils.divide(xhat.transientPart, 32768.0), outFileName, inputAudio.getFormat());
                }

                if (xhat.harmonicPart!=null)
                {
                    outFileName = wavFile.substring(0, wavFile.length()-4) + "_" + modelName + "OrigMinusHarmonic" + strExt + ".wav";
                    //FileUtils.writeWavFile(MathUtils.divide(SignalProcUtils.addSignals(x, 1.0, xhat.harmonicPart, -1.0), 32768.0), outFileName, inputAudio.getFormat());
                }

                //MaryUtils.plot(xhat.harmonicPart);
                //MaryUtils.plot(xhat.noisePart);
                //MaryUtils.plot(y);

                //if (nEstimate!=null)
                //{
                //    outFileName = args[0].substring(0, args[0].length()-4) + "_" + modelName + "Diff.wav";
                //    FileUtils.writeWavFile(new BufferedDoubleDataSource(nEstimate), outFileName, inputAudio.getFormat());
                //}
                //
            }
        }
        
        System.out.println("Synthesis...done!");
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
