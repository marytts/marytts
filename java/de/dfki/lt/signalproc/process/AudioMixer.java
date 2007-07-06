package de.dfki.lt.signalproc.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.SignalProcUtils;

//Mixes the given audio source with a pre-recorded audio file
//by handling continuity and energy levels in real-time
public class AudioMixer implements InlineDataProcessor {
    private int silenceStart; //Silence before starting the mix in samples
    private int silenceInBetween; //Total silent samples between two consecutive mixes 
    private int samplingRate;
    AudioDoubleDataSource mixSignalSource;
    double [] mixSignal; //
    int mixStart; //Index to copy & paste rom mixSignal
    double mixAmount;
    double oneMinusMixAmount;
    int bufferSize;
    int quarterBufferSize;
    double dataEn;
    double mixEn;
    double [] frm;
    
    //stSil: silence before starting the mix in seconds
    //stBwn: silence between consecutive mixes in seconds
    //fs: sampling rate in Hz
    public AudioMixer(InputStream inputStream, double stSil, double stBwn, int fs, int buffSize, double amount)
    {
        AudioInputStream inputAudio = null;
        try {
            inputAudio = AudioSystem.getAudioInputStream(inputStream);
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (inputAudio != null)
        {
            int mixFs = (int)inputAudio.getFormat().getSampleRate();
            mixSignalSource = new AudioDoubleDataSource(inputAudio);
            this.samplingRate = fs;
        
            if (mixFs!=fs)
            {
                System.out.println("Error! Sampling rates do not match, will not do any modificaiton on input");
                mixSignalSource = null;
            }
            else
            {
                silenceStart = (int)Math.floor(stSil*samplingRate + 0.5);
                silenceInBetween = (int)Math.floor(stBwn*samplingRate + 0.5);
                silenceStart = Math.max(0, silenceStart);
                silenceInBetween = Math.max(0, silenceInBetween);
                
                mixSignal = new double[(int)mixSignalSource.getDataLength() + silenceInBetween];
                mixSignalSource.getData(mixSignal);
                for (int i=(int)mixSignalSource.getDataLength(); i<(int)mixSignalSource.getDataLength() + silenceInBetween; i++)
                    mixSignal[i] = 0.0;
                
                mixStart = 0;
                mixAmount = amount;
                mixAmount = Math.max(mixAmount, 0.0);
                mixAmount = Math.min(mixAmount, 1.0);
                oneMinusMixAmount = 1.0-mixAmount;
                bufferSize = buffSize;
                quarterBufferSize = (int)Math.floor(bufferSize*0.25+0.5);
                frm = new double[bufferSize];
            }
        }
        else
        {
            mixSignalSource = null;
        }
    }
    
    public void applyInline(double [] data, int pos, int len) 
    { 
        if (data.length==bufferSize)
        {
            for (int i=0; i<bufferSize; i++)
            {
                frm[i] = mixSignal[(mixStart+i)%mixSignal.length];
            }
            dataEn = Math.sqrt(SignalProcUtils.getAverageSampleEnergy(data));
            mixEn = Math.sqrt(SignalProcUtils.getAverageSampleEnergy(frm));
            
            for (int i=0; i<bufferSize; i++)
            {
                data[i] = oneMinusMixAmount*data[i] + mixAmount*dataEn/mixEn*frm[i];
            }
            
            mixStart += quarterBufferSize;
        }
    }
}