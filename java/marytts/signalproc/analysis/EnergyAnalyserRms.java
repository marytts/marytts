/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.AudioDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
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
