package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.unitselection.AudioEffect;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;

public class RobotiserEffect implements AudioEffect {
    
    public AudioInputStream apply(AudioInputStream inputAudio)
    {
        AudioFormat audioformat = inputAudio.getFormat();
        int fs = (int)audioformat.getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        Robotiser robotiser = new Robotiser(signal, fs);
        return new DDSAudioInputStream(new BufferedDoubleDataSource(robotiser), audioformat);
    }
}
