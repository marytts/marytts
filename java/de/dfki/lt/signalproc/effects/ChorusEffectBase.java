package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.unitselection.AudioEffect;
import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class ChorusEffectBase implements AudioEffect {
    
    int [] delaysInMiliseconds;
    double [] amps;
    
    public ChorusEffectBase()
    {
        delaysInMiliseconds = new int[3];
        delaysInMiliseconds[0] = 700;
        delaysInMiliseconds[1] = 350;
        delaysInMiliseconds[2] = 150;
        
        amps = new double[3];
        amps[0] = 0.70;
        amps[1] = -0.45;
        amps[2] = 0.80;
    }
    
    public AudioInputStream apply(AudioInputStream inputAudio)
    {
        AudioFormat audioformat = inputAudio.getFormat();
        int fs = (int)audioformat.getSampleRate();
        
        int maxDelayInMiliseconds = MathUtils.getMax(delaysInMiliseconds);
        int maxDelayInSamples = (int)(maxDelayInMiliseconds/1000.0*fs);
        
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
        if (frameLength<maxDelayInSamples)
            frameLength *= 2;
        
        int predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
        FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, fs,
                new Chorus(delaysInMiliseconds, amps, fs));
        return new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audioformat);
    }

}
