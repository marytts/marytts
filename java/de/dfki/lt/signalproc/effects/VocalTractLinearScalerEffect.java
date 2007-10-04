package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class VocalTractLinearScalerEffect extends BaseAudioEffect {
    
    public VocalTractLinearScalerEffect(int samplingRate)
    {
        super(samplingRate);
    }
    
    public AudioInputStream apply(AudioInputStream inputAudio)
    {
        double [] vscales = {1.5};
        AudioFormat audioformat = inputAudio.getFormat();
        int fs = (int)audioformat.getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
        int predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
        FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, fs,
                new VocalTractScalingProcessor(predictionOrder, fs, frameLength, vscales));
        return new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audioformat);
    }
}
