package de.dfki.lt.signalproc.effects;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.unitselection.AudioEffect;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.window.Window;

public class LPCWhisperiserEffect implements AudioEffect {

    public AudioInputStream apply(AudioInputStream inputAudio)
    {
        AudioFormat audioformat = inputAudio.getFormat();
        int fs = (int)audioformat.getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
        int predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
        FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, fs,
                new LPCWhisperiser(predictionOrder));
        return new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audioformat);
    }
}
