package marytts.signalproc.sinusoidal.hnm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.RegularizedCepstralEnvelopeEstimator;
import marytts.signalproc.window.HanningWindow;
import marytts.signalproc.window.Window;
import marytts.util.MaryUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

public class Temporary {
    public double[] synthesizeHarmonicPartLinearPhaseInterpolation(HnmSpeechSignal hnmSignal, 
            float[] tScales,
            float[] tScalesTimes,
            float[] pScales,
            float[] pScalesTimes)
    {
        double[] harmonicPart = null;
        double[][] harmonicTracks = null;
        double[][] trackAmps = null;
        double[][] trackPhases = null;
        double[] currentFrameTrack = null;
        int trackNoToExamine = 1;
        
        double overlapAmountInSeconds = 0.005;
        int overlapSize = SignalProcUtils.time2sample(overlapAmountInSeconds, hnmSignal.samplingRateInHz);
        Window w = new HanningWindow(2*overlapSize);
        double[] wgtLeft = w.getCoeffsLeftHalf();
        double[] wgtRight = w.getCoeffsRightHalf();
        
        int i, k, n;
        double t, tsi, tsiPlusOne; //Time in seconds
        double frameStartInSeconds, frameEndInSeconds;
        int frameStartIndex, frameEndIndex;
        double akt;
        int numHarmonicsCurrentFrame;
        int maxNumHarmonics = 0;
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            if (hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f && hnmSignal.frames[i].h.phases!=null)
            {
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
                if (numHarmonicsCurrentFrame>maxNumHarmonics)
                    maxNumHarmonics = numHarmonicsCurrentFrame;
            }  
        }

        double[] aksis = null;
        double[] aksiPlusOnes = null;
        float[] phasekis = null;
        float[] phasekiPlusOnes = null;
        if (maxNumHarmonics>0)
        {
            harmonicTracks = new double[maxNumHarmonics][];
            trackAmps = new double[maxNumHarmonics][];
            trackPhases = new double[maxNumHarmonics][];
            
            aksis = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            aksiPlusOnes = new double[maxNumHarmonics];
            Arrays.fill(aksis, 0.0);
            phasekis = new float[maxNumHarmonics];
            Arrays.fill(phasekis, 0.0f);
            phasekiPlusOnes = new float[maxNumHarmonics];
            Arrays.fill(phasekiPlusOnes, 0.0f);
        }

        float f0InHz, f0InHzNext, f0InHzPrev, f0av;
        f0InHzPrev = 0.0f;
        double lastPeriodInSeconds = 0.0;
        double ht;
        float phasekt = 0.0f;

        float phasekiPlusOneEstimate = 0.0f;
        int Mk;
        boolean isVoiced, isNextVoiced, isPrevVoiced;
        int origLen = SignalProcUtils.time2sample(hnmSignal.originalDurationInSeconds, hnmSignal.samplingRateInHz);
        harmonicPart = new double[origLen]; //In fact, this should be prosody scaled length when you implement prosody modifications
        Arrays.fill(harmonicPart, 0.0);
        double currentWgt;

        for (k=0; k<maxNumHarmonics; k++)
        {
            harmonicTracks[k] = new double[origLen];
            Arrays.fill(harmonicTracks[k], 0.0);
            trackAmps[k] = new double[origLen];
            Arrays.fill(trackAmps[k], 0.0);
            trackPhases[k] = new double[origLen];
            Arrays.fill(trackPhases[k], 0.0);
        }
        
        for (i=0; i<hnmSignal.frames.length; i++)
        {
            isVoiced = ((hnmSignal.frames[i].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            if (i<hnmSignal.frames.length-1)
                isNextVoiced = ((hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            else
                isNextVoiced = false;

            if (i>0)
                isPrevVoiced = ((hnmSignal.frames[i-1].maximumFrequencyOfVoicingInHz>0.0f) ? true : false);
            else
                isPrevVoiced = false;

            if (i==0)
                tsi = 0.0f;
            else
                tsi = hnmSignal.frames[i].tAnalysisInSeconds;
            frameStartInSeconds = tsi;
            if (isNextVoiced)
            {
                if (i==hnmSignal.frames.length-1)
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                else
                    tsiPlusOne = hnmSignal.frames[i+1].tAnalysisInSeconds;
                frameEndInSeconds = tsiPlusOne;
            }
            else
            {
                if (i==hnmSignal.frames.length-1)
                {
                    tsiPlusOne = hnmSignal.originalDurationInSeconds;
                    frameEndInSeconds = tsiPlusOne;
                }
                else
                {
                    frameEndInSeconds = frameStartInSeconds + lastPeriodInSeconds;
                    tsiPlusOne = frameEndInSeconds;
                }
            }

            if (hnmSignal.frames[i].h.phases!=null)
                numHarmonicsCurrentFrame = hnmSignal.frames[i].h.phases.length;
            else
                numHarmonicsCurrentFrame = 0;

            frameStartIndex = SignalProcUtils.time2sample(frameStartInSeconds, hnmSignal.samplingRateInHz);
            frameEndIndex = SignalProcUtils.time2sample(frameEndInSeconds, hnmSignal.samplingRateInHz);
            
            int tempStartIndex;
            if (!isPrevVoiced && isVoiced)
                tempStartIndex = Math.max(0,frameStartIndex-overlapSize);
            else
                tempStartIndex = frameStartIndex;
            
            int tempEndIndex;
            if (isVoiced && !isNextVoiced)
                tempEndIndex = Math.max(0,frameEndIndex-overlapSize);
            else
                tempEndIndex = frameEndIndex;
            
            currentFrameTrack = new double[tempEndIndex-tempStartIndex+1];
            
            for (n=tempStartIndex; n<=Math.min(tempEndIndex, origLen-1); n++)
            {
                if (!isPrevVoiced && isVoiced && n<frameStartIndex)
                    currentWgt = wgtLeft[n-(frameStartIndex-overlapSize)];
                else if (isVoiced && !isNextVoiced && n>Math.min(frameEndIndex,origLen-1))
                    currentWgt = wgtRight[n-(frameStartIndex-overlapSize)];
                else
                    currentWgt = 1.0;
                    
                t = SignalProcUtils.sample2time(n, hnmSignal.samplingRateInHz);

                f0InHz = hnmSignal.frames[i].h.f0InHz;
                if (isNextVoiced &&  hnmSignal.frames[i+1].h.phases!=null)
                    f0InHzNext = hnmSignal.frames[i+1].h.f0InHz;
                else
                    f0InHzNext = f0InHz;
                
                if (i>0)
                    f0InHzPrev = hnmSignal.frames[i-1].h.f0InHz;

                f0av = 0.5f*(f0InHz+f0InHzNext);
                
                /*
                TO DO: Synthesis times has to change with prosody modifications, also check amplitude and phase envelope resampling
                double pscale = 1.5;
                f0InHz *= pscale;
                f0InHzNext *= pscale;
                f0av *= pscale;
                */
                
                harmonicPart[n] = 0.0;
                for (k=0; k<Math.min(numHarmonicsCurrentFrame,maxNumHarmonics); k++)
                {
                    //Estimate amplitude
                    aksis[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i].h.ceps, (k+1)*f0InHz, hnmSignal.samplingRateInHz);
                    //aksis[k] = hnmSignal.frames[i].h.ceps[k]; //Use amplitudes directly without cepstrum method

                    aksiPlusOnes[k] = aksis[k];
                    if (isNextVoiced && hnmSignal.frames[i+1].h.ceps!=null)
                    {
                        if ((k+1)*f0InHzNext<hnmSignal.frames[i+1].maximumFrequencyOfVoicingInHz)
                        {     
                            aksiPlusOnes[k] = RegularizedCepstralEnvelopeEstimator.cepstrum2linearSpectrumValue(hnmSignal.frames[i+1].h.ceps , (k+1)*f0InHzNext, hnmSignal.samplingRateInHz);
                            //aksiPlusOnes[k] = hnmSignal.frames[i+1].h.ceps[k]; //Use amplitudes directly without cepstrum method
                        }
                    }

                    akt = aksis[k] + (aksiPlusOnes[k]-aksis[k])*(t-tsi)/(tsiPlusOne-tsi);
                    //

                    //Estimate phase                     
                    if (isVoiced)
                        phasekis[k] = hnmSignal.frames[i].h.phases[k];

                    if (isNextVoiced && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length)
                        phasekiPlusOnes[k] = hnmSignal.frames[i+1].h.phases[k];
                    else
                        phasekiPlusOnes[k] = (float)(MathUtils.TWOPI*(Math.random()-0.5));

                    if (!isVoiced)
                        phasekis[k] = (float)( phasekiPlusOnes[k] - (k+1)*MathUtils.TWOPI*f0InHzNext*(tsiPlusOne-tsi) ); //Equation (3.54)

                    if (!(isNextVoiced && hnmSignal.frames[i+1].h.phases!=null && k<hnmSignal.frames[i+1].h.phases.length))
                        phasekiPlusOnes[k] = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0InHz*(tsiPlusOne-tsi) ); //Equation (3.55)

                    phasekiPlusOneEstimate = (float)( phasekis[k] + (k+1)*MathUtils.TWOPI*f0av*(tsiPlusOne-tsi));
                    Mk = (int)Math.floor((phasekiPlusOneEstimate-phasekiPlusOnes[k])/MathUtils.TWOPI + 0.5);
                    phasekt = (float)( phasekis[k] + (phasekiPlusOnes[k]+MathUtils.TWOPI*Mk-phasekis[k])*(t-tsi)/(tsiPlusOne-tsi) );
                    //

                    harmonicPart[n] += currentWgt*akt*Math.cos(phasekt);
                    harmonicTracks[k][n] = harmonicPart[n];
                    trackAmps[k][n] = akt;
                    trackPhases[k][n] = phasekt;
                    
                    if (trackNoToExamine==k+1)
                        currentFrameTrack[n-tempStartIndex] = harmonicPart[n];
                }
            }

            /*
            if (tsi>0.40 && tsi<0.45)
                MaryUtils.plot(currentFrameTrack);
                */
            
            lastPeriodInSeconds= tsiPlusOne - tsi;
        }

        harmonicPart = MathUtils.divide(harmonicPart, 32768.0);
        
        //Write individual tracks to file for examination
        AudioInputStream inputAudio = null;
        try {
            inputAudio = AudioSystem.getAudioInputStream(new File("d:\\i.wav"));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (inputAudio!=null)
        {
            //for (k=0; k<harmonicTracks.length; k++)
            k=1;
            {
                MaryUtils.plot(trackAmps[k]);
                MaryUtils.plot(trackPhases[k]);

                harmonicTracks[k] = MathUtils.divide(harmonicTracks[k], 32768.0);
                MaryUtils.plot(harmonicTracks[k]);
                
                DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(harmonicTracks[k]), inputAudio.getFormat());
                String outFileName = "d:\\harmonicTrack" + String.valueOf(k+1) + ".wav";
                try {
                    AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        //
        
        return harmonicPart;
    }

}
