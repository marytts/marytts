package de.dfki.lt.signalproc.demo;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;

public class OnlineAudioEffects extends Thread
{
    protected InlineDataProcessor effect;
    protected TargetDataLine microphone;
    protected SourceDataLine loudspeakers;
    private boolean stopRequested;
    
    public OnlineAudioEffects(InlineDataProcessor effect, TargetDataLine microphone, SourceDataLine loudspeakers)
    {
        this.effect = effect;
        this.microphone = microphone;
        this.loudspeakers = loudspeakers;
    }
    
    public void run()
    {
        stopRequested = false;
        microphone.start();
        loudspeakers.start();
        AudioInputStream input = new AudioInputStream(microphone);
        DoubleDataSource inputSource = new AudioDoubleDataSource(input);
        DoubleDataSource outputSource = new FrameOverlapAddSource(inputSource, 1024, (int) input.getFormat().getSampleRate(), effect);
        AudioInputStream result = new DDSAudioInputStream(outputSource, input.getFormat());
        byte[] buf = new byte[1024];
        while (!stopRequested) {
            try {
                int nRead = result.read(buf);
                if (nRead == -1) stopRequested = true;
                else {
                    loudspeakers.write(buf, 0, nRead);                    
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                stopRequested = true;
            }
        }
    }
    
    public void requestStop() 
    {
        stopRequested = true;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // Get a TargetDataLine (microphone signal)
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        // Hard-coded shortcut -- this can be done via a ComboBox in a GUI
        Mixer.Info mixerInfo = mixerInfos[1];
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        Line.Info[] lineInfos = mixer.getTargetLineInfo();
        // Hard-coded shortcut -- this can be done via a ComboBox in a GUI
        Line.Info lineInfo = lineInfos[0];
        DataLine.Info datalineInfo = (DataLine.Info) lineInfo;
        AudioFormat[] formats = datalineInfo.getFormats();
        // Hard-coded -- this can be done via a ComboBox in a GUI
        AudioFormat format = null;
        for (int i=formats.length-1; i >= 0; i--) {
            if (formats[i].getChannels() == 1
                && formats[i].getFrameSize() == 2) {
                format = formats[i];
                break;
            }
        }
        TargetDataLine microphone = null;
        try {
            microphone = (TargetDataLine) mixer.getLine(lineInfo);
            microphone.open(format);
            System.out.println("Microphone format: "+microphone.getFormat());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        
        // In a similar way we can get the output channel from mixer.getSourceLineInfo().
        // Somehow only mixer 0 works for me:
        SourceDataLine loudspeakers = null;
        try {
            DataLine.Info   info = new DataLine.Info(SourceDataLine.class, format);
            loudspeakers = (SourceDataLine) AudioSystem.getLine(info);
            loudspeakers.open(format);
            System.out.println("Loudspeaker format: "+loudspeakers.getFormat());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        
        // Choose an audio effect
        InlineDataProcessor effect = new Robotiser.PhaseRemover(4096);
        //InlineDataProcessor effect = new LPCWhisperiser(20);

        // Create the output thread and make it run in the background:
        OnlineAudioEffects online = new OnlineAudioEffects(effect, microphone, loudspeakers);
        online.start();
        
        // Here we simply wait until it is finished (which will never happen):
        try {
            online.join();
        } catch (InterruptedException ie) {}
    }

}
