/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.sinusoidal;

/**
 * @author oytun.turk
 *
 */

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class ProsodyModifier extends SinusoidalSynthesizer {
    
    public ProsodyModifier(int samplingRate) {
        super(samplingRate);
    }
    
    public double [] process(double [] x, 
                             double [] f0s, 
                             float f0_ws, float f0_ss,
                             boolean isVoicingAdaptiveTimeScaling,
                             float timeScalingVoicingThreshold, 
                             boolean isVoicingAdaptivePitchScaling,
                             float timeScale,
                             float pitchScale,
                             float skipSizeInSeconds,
                             float deltaInHz,
                             float numPeriods,
                             boolean bRefinePeakEstimatesParabola, 
                             boolean bRefinePeakEstimatesBias, 
                             boolean bSpectralReassignment,
                             boolean bAdjustNeighFreqDependent,
                             boolean isSilentSynthesis,
                             double absMaxDesired,
                             int spectralEnvelopeType)
    {    
        float [] tScales = new float[1];
        float [] tScalesTimes = new float[1];
        tScales[0] = timeScale;
        tScalesTimes[0] = skipSizeInSeconds;
        
        float [] pScales = new float[1];
        float [] pScalesTimes = new float[1];
        pScales[0] = pitchScale;
        pScalesTimes[0] = skipSizeInSeconds;
        
        return process(x, 
                       f0s, 
                       f0_ws,  f0_ss,
                       isVoicingAdaptiveTimeScaling, 
                       timeScalingVoicingThreshold,
                       isVoicingAdaptivePitchScaling,
                       tScales,
                       tScalesTimes,
                       pScales,
                       pScalesTimes,
                       skipSizeInSeconds,
                       deltaInHz,
                       numPeriods,
                       bRefinePeakEstimatesParabola, 
                       bRefinePeakEstimatesBias,  
                       bSpectralReassignment,
                       bAdjustNeighFreqDependent,
                       isSilentSynthesis,
                       absMaxDesired,
                       spectralEnvelopeType);
    }
    
    public double [] process(double [] x, 
                             double [] f0s, 
                             float f0_ws, float f0_ss,
                             boolean isVoicingAdaptiveTimeScaling,
                             float timeScalingVoicingThreshold,
                             boolean isVoicingAdaptivePitchScaling,
                             float [] timeScales,
                             float [] timeScalesTimes,
                             float [] pitchScales,
                             float [] pitchScalesTimes,
                             float skipSizeInSeconds,
                             float deltaInHz,
                             float numPeriods,
                             boolean bRefinePeakEstimatesParabola, 
                             boolean bRefinePeakEstimatesBias,  
                             boolean bSpectralReassignment,
                             boolean bAdjustNeighFreqDependent,
                             boolean isSilentSynthesis,
                             double absMaxDesired,
                             int spectralEnvelopeType)
    {    
        //Analysis
        PitchSynchronousSinusoidalAnalyzer pa = new PitchSynchronousSinusoidalAnalyzer(fs, Window.HANN, 
                                                                                       bRefinePeakEstimatesParabola, 
                                                                                       bRefinePeakEstimatesBias, 
                                                                                       bSpectralReassignment,
                                                                                       bAdjustNeighFreqDependent);
        
        PitchMarker pm = SignalProcUtils.pitchContour2pitchMarks(f0s, fs, x.length, f0_ws, f0_ss, false);

        SinusoidalTracks st = pa.analyzePitchSynchronous(x, pm.pitchMarks, numPeriods, skipSizeInSeconds, deltaInHz, spectralEnvelopeType);
        
        /*
        try {
            st.writeToTextFile("d:\\log_ts1.txt");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
        
        //To do: Estimation of voicing probabilities...
        
        //Modification
        SinusoidalTracks stMod = TrackModifier.modify(st, f0s, f0_ss, f0_ws, pm.pitchMarks, st.voicings, numPeriods, 
                                                      isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold,
                                                      isVoicingAdaptivePitchScaling,  
                                                      timeScales, timeScalesTimes, pitchScales, pitchScalesTimes);

        /*
        try {
            stMod.writeToTextFile("d:\\log_ts2.txt");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        */
        
        //Synthesis
        return synthesize(stMod, isSilentSynthesis);
    }
    
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException
    {
        if (true)
        {
            //File input
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            double [] x = signal.getAllData();

            //Read pitch contour (real speech or create it from pm file
            F0ReaderWriter f0 = null;
            String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
            f0 = new F0ReaderWriter(strPitchFile);
            //

            //Analysis
            float deltaInHz = SinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ;
            float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;
            boolean isSilentSynthesis = false;

            boolean bRefinePeakEstimatesParabola = true;
            boolean bRefinePeakEstimatesBias = true;
            boolean bSpectralReassignment = true;
            boolean bAdjustNeighFreqDependent = false;

            double absMaxOriginal = MathUtils.getAbsMax(x);

            float skipSizeInSeconds = TrackModifier.DEFAULT_MODIFICATION_SKIP_SIZE;
            //skipSizeInSeconds = -1.0f;
            //skipSizeInSeconds = 0.002f;

            boolean isVoicingAdaptiveTimeScaling = true;
            float timeScalingVoicingThreshold = 0.5f;
            boolean isVoicingAdaptivePitchScaling = true;

            int spectralEnvelopeType = SinusoidalAnalyzer.LP_SPEC;

            ProsodyModifier pm = new ProsodyModifier(samplingRate);
            double [] y = null;

            if (true)
            {
                float timeScale = 1.0f;
                float pitchScale = 1.5f;
                y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                        isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                        timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                        bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                        absMaxOriginal, spectralEnvelopeType);
            }
            else
            {
                float [] timeScales = {0.5f, 0.75f, 1.25f, 1.75f};
                float [] timeScalesTimes = {0.5f, 1.25f, 2.0f, 2.5f};
                float [] pitchScales = {2.0f, 1.5f, 0.8f, 0.6f};
                float [] pitchScalesTimes = {0.5f, 1.25f, 2.0f, 2.5f};

                y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss,
                        isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                        timeScales, timeScalesTimes, pitchScales, pitchScalesTimes, skipSizeInSeconds, deltaInHz, numPeriods,
                        bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                        absMaxOriginal, spectralEnvelopeType);
            }

            //File output
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
            String outFileName = args[0].substring(0, args[0].length()-4) + "_sinTScaled.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
            //
        }
        else
            main2(args);
    }
    
    public static void main2(String[] args) throws UnsupportedAudioFileException, IOException
    {
        //File input
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double [] x = signal.getAllData();

        //Read pitch contour (real speech or create it from pm file
        F0ReaderWriter f0 = null;
        String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        f0 = new F0ReaderWriter(strPitchFile);
        //

        //Analysis
        float deltaInHz = SinusoidalAnalyzer.DEFAULT_DELTA_IN_HZ;
        float numPeriods = PitchSynchronousSinusoidalAnalyzer.DEFAULT_ANALYSIS_PERIODS;
        boolean isSilentSynthesis = false;

        boolean bRefinePeakEstimatesParabola = true;
        boolean bRefinePeakEstimatesBias = true;
        boolean bSpectralReassignment = true;
        boolean bAdjustNeighFreqDependent = true;

        double absMaxOriginal = MathUtils.getAbsMax(x);

        float skipSizeInSeconds = TrackModifier.DEFAULT_MODIFICATION_SKIP_SIZE;
        //skipSizeInSeconds = -1.0f;
        //skipSizeInSeconds = 0.002f;

        boolean isVoicingAdaptiveTimeScaling = true;
        float timeScalingVoicingThreshold = 0.5f;
        boolean isVoicingAdaptivePitchScaling = true;

        int spectralEnvelopeType = SinusoidalAnalyzer.LP_SPEC;

        ProsodyModifier pm = new ProsodyModifier(samplingRate);
        double [] y = null;

        float pitchScale;
        float timeScale;
        DDSAudioInputStream outputAudio;
        String outFileName;
        
        pitchScale = 1.0f;
        timeScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_none" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

        pitchScale = 0.55f;
        timeScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p055" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 0.80f;
        timeScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p080" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 1.50f;
        timeScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p150" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 2.50f;
        timeScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p250" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        timeScale = 0.55f;
        pitchScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_d055" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        timeScale = 0.80f;
        pitchScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_d080" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        timeScale = 1.50f;
        pitchScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_d150" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        timeScale = 2.50f;
        pitchScale = 1.0f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_d250" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 0.55f;
        timeScale = 0.80f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p055_d080" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 0.80f;
        timeScale = 2.50f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p080_d250" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 1.50f;
        timeScale = 0.55f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p150_d055" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        pitchScale = 2.50f;
        timeScale = 1.50f;
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss, 
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScale, pitchScale, skipSizeInSeconds, deltaInHz, numPeriods, 
                bRefinePeakEstimatesParabola,  bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_p250_d150" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));

        float [] pitchScales = {0.60f, 0.65f, 0.70f, 0.75f, 0.80f, 0.85f, 0.90f, 0.95f, 1.05f, 1.10f, 1.15f, 1.20f, 1.25f, 1.30f, 1.35f, 1.40f, 1.45f, 1.50f};
        float [] timeScales = {1.50f, 1.45f, 1.40f, 1.35f, 1.30f, 1.25f, 1.20f, 1.15f, 1.10f, 1.05f, 0.95f, 0.90f, 0.85f, 0.80f, 0.75f, 0.70f, 0.65f, 0.60f};
        y = pm.process(x, f0.contour, (float)f0.header.ws, (float)f0.header.ss,
                isVoicingAdaptiveTimeScaling, timeScalingVoicingThreshold, isVoicingAdaptivePitchScaling,
                timeScales, null, pitchScales, null, skipSizeInSeconds, deltaInHz, numPeriods,
                bRefinePeakEstimatesParabola, bRefinePeakEstimatesBias, bSpectralReassignment, bAdjustNeighFreqDependent, isSilentSynthesis, 
                absMaxOriginal, spectralEnvelopeType);
        outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), inputAudio.getFormat());
        outFileName = args[0].substring(0, args[0].length()-4) + "_sin3_pvar_dvar" + ".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        
        System.out.println("Sinusoidal prosody modifications completed...");
    }
}
