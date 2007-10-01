package de.dfki.lt.mary.unitselection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

//An interface for wrapping all audio effects in unit selection synthesis
public interface AudioEffect {
    
    public AudioInputStream apply(AudioInputStream inputAudio);
}
