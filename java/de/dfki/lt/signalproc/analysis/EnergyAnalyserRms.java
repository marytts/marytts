package de.dfki.lt.signalproc.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.util.SignalProcUtils;

public class EnergyAnalyserRms {
    EnergyFileHeader header;
    public double[] contour;

    public EnergyAnalyserRms()
    {
        this("");
    }
    
    public EnergyAnalyserRms(String wavFile)
    {
        this(wavFile, 0.020, 0.010);
    }
    
    public EnergyAnalyserRms(String wavFileIn, double windowSizeInSecondsIn, double skipSizeInSecondsIn)
    {   
        this(wavFileIn, "", windowSizeInSecondsIn, skipSizeInSecondsIn);
    }
    
    public EnergyAnalyserRms(String wavFileIn, String energyFileOut, double windowSizeInSecondsIn, double skipSizeInSecondsIn)
    {   
        header = new EnergyFileHeader();
        
        header.windowSizeInSeconds = windowSizeInSecondsIn;
        header.skipSizeInSeconds = skipSizeInSecondsIn;
        
        if (wavFileIn!=null && wavFileIn!="")
        {
            AudioInputStream inputAudio = null;
            try {
                inputAudio = AudioSystem.getAudioInputStream(new File(wavFileIn));
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (inputAudio!=null)
            {
                header.samplingRate = (int)inputAudio.getFormat().getSampleRate();

                AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);

                contour = SignalProcUtils.getEnergyContourRms(signal.getAllData(), header.windowSizeInSeconds, header.skipSizeInSeconds, header.samplingRate);

                header.totalFrames = contour.length;

                if (energyFileOut!=null && energyFileOut!="")
                    WriteEnergyFile(this, energyFileOut);
            }
        }
    }
    
    public static void WriteEnergyFile(EnergyAnalyserRms en, String energyFile)
    {
        if (en.contour!=null)
        {
            en.header.totalFrames = en.contour.length;
            
            MaryRandomAccessFile ler = null;
            try {
                ler = new MaryRandomAccessFile(energyFile, "rw");
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            if (ler!=null)
            {
                en.header.write(ler);
            
                try {
                    ler.writeDouble(en.contour);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            
                try {
                    ler.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static EnergyAnalyserRms ReadEnergyFile(String energyFile)
    {
        EnergyAnalyserRms en = null;
        MaryRandomAccessFile ler = null;
        try {
            ler = new MaryRandomAccessFile(energyFile, "r");
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (ler!=null)
        {
            en = new EnergyAnalyserRms();
            en.header.read(ler, true);

            try {
                en.contour = ler.readDouble(en.header.totalFrames);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                ler.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return en;
    }
}
