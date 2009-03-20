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

package marytts.signalproc.sinusoidal.hntm.synthesis.hybrid;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.sinusoidal.PeakMatchedSinusoidalSynthesizer;
import marytts.signalproc.sinusoidal.SinusoidalTracks;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartLpc;
import marytts.signalproc.sinusoidal.hntm.analysis.FrameNoisePartPseudoHarmonic;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzer;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.modification.HntmDurationModifier;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.NoisePartLpcSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.NoisePartPseudoHarmonicSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.TransientPartSynthesizer;
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
 * @author oytun.turk
 *
 */
public class HntmHybridSynthesizer extends HntmSynthesizer {

    //Overrides synthesize to use pure sinusoidal synthesizer for synthesizing the harmonic part
    public HntmSynthesizedSignal synthesize(HntmSpeechSignal hntmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        //Handle time scaling by adjusting synthesis times
        HntmSpeechSignal hntmSignalMod = HntmDurationModifier.modify(hntmSignal, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
        //Convert to pure sinusoidal tracks
        SinusoidalTracks st = HarmonicsToTrackConverter.convert(hntmSignalMod);
        //
        
        HntmSynthesizedSignal s = new HntmSynthesizedSignal();
        PeakMatchedSinusoidalSynthesizer ss = new PeakMatchedSinusoidalSynthesizer(hntmSignalMod.samplingRateInHz);
        s.harmonicPart = ss.synthesize(st, false);
        
        float[] times = hntmSignalMod.getAnalysisTimes();
        
        //Energy contour normalization
        float[] targetContour = hntmSignalMod.getTargetEnergyContour();
        float[] currentContour = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, st.fs, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        s.harmonicPart = SignalProcUtils.normalizeAverageSampleEnergyContour(s.harmonicPart, times, currentContour, targetContour, st.fs, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        float[] currentContour2 = SignalProcUtils.getAverageSampleEnergyContour(s.harmonicPart, times, st.fs, NOISE_SYNTHESIS_WINDOW_DURATION_IN_SECONDS);
        //

        //Synthesize noise part
        if (hntmSignalMod.frames[0].n instanceof FrameNoisePartLpc)
            s.noisePart = NoisePartLpcSynthesizer.synthesize(hntmSignalMod);
        else if (hntmSignalMod.frames[0].n instanceof FrameNoisePartPseudoHarmonic)
            s.noisePart = NoisePartPseudoHarmonicSynthesizer.synthesize(hntmSignalMod, pScales, pScalesTimes);
        //
        
        //Synthesize transients
        if (hntmSignalMod instanceof HntmPlusTransientsSpeechSignal && ((HntmPlusTransientsSpeechSignal)hntmSignalMod).transients!=null)
            s.transientPart = TransientPartSynthesizer.synthesize((HntmPlusTransientsSpeechSignal)hntmSignalMod, tScales, tScalesTimes, pScales, pScalesTimes);
        //
        
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
        
        int noisePartRepresentation = HntmAnalyzer.LPC;
        //int noisePartRepresentation = HntmAnalyzer.REGULARIZED_CEPS;
        //int noisePartRepresentation = HntmAnalyzer.PSEUDO_HARMONIC;

        F0ReaderWriter f0 = null;
        String strPitchFile = StringUtils.modifyExtension(wavFile, ".ptc");
        if (FileUtils.exists(strPitchFile))
            f0 = new F0ReaderWriter(strPitchFile);
        
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

            HntmHybridSynthesizer hs = new HntmHybridSynthesizer();
            HntmSynthesizedSignal xhat = hs.synthesize(hnmSignal, tScales, tScalesTimes, pScales, pScalesTimes);

            double hGain = 1.0;
            double nGain = 1.0;
            double tGain = 1.0;
            if (xhat.harmonicPart!=null)
            {
                //xhat.harmonicPart = MathUtils.multiply(xhat.harmonicPart, hGain);
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
            MaryUtils.plot(x);
            MaryUtils.plot(xhat.harmonicPart);
            MaryUtils.plot(xhat.noisePart);
            if (xhat.transientPart!=null)
                MaryUtils.plot(xhat.transientPart);
            MaryUtils.plot(y);
            
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
                modelName = "hnmq";
            else if (model==HntmAnalyzer.HARMONICS_PLUS_TRANSIENTS_PLUS_NOISE)
                modelName = "hntmq";

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
