/**
 * Copyright 2007 DFKI GmbH.
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

package de.dfki.lt.signalproc.process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class Chorus implements InlineDataProcessor {
    private double [] buffIn;
    private double [] buffOut;
    private boolean [] bFirstInBuff;
    private int [] delays;
    private double [] amps;
    
    private int buffInStart;
    private int numChannels;
    private double sumAmps;
    
    public Chorus(int samplingRate)
    {
        this(null, null, samplingRate);
    }
    
    public Chorus(int [] delaysInMiliseconds, double [] ampsIn, int samplingRate)
    {
        //If null parameters, use a default chorus
        boolean bDelete = false;
        if (delaysInMiliseconds==null)
        {
            bDelete = true;
            
            delaysInMiliseconds = new int[2];
            delaysInMiliseconds[0] = 466;
            delaysInMiliseconds[1] = 600;
            
            ampsIn = new double[2];
            ampsIn[0] = 0.54;
            ampsIn[1] = -0.10;
        }

        numChannels = Math.min(delaysInMiliseconds.length, ampsIn.length);
        
        if (numChannels>0)
        {
            delays = new int[numChannels];
            amps = new double[numChannels];
            bFirstInBuff = new boolean[numChannels];

            int i;
            for (i=0; i<numChannels; i++)
                delays[i] = (int)(Math.floor(delaysInMiliseconds[i]/1000.0*samplingRate + 0.5));

            int buffInLen = delays[0];
            for (i=0; i<numChannels; i++)
            {
                if (buffInLen<delays[i])
                    buffInLen = delays[i];
            }
            
            if (buffInLen<1)
                buffInLen=1;

            buffIn = new double[buffInLen];
            for (i=0; i<buffInLen; i++)
                buffIn[i] = 0.0;

            buffOut = null;

            buffInStart = 1;
            for (i=0; i<numChannels; i++)
                amps[i] = ampsIn[i];

            sumAmps = 1.0;
            for (i=0; i<numChannels; i++)
                sumAmps += amps[i];
            
            for (i=0; i<numChannels; i++)
                bFirstInBuff[i] = true;
        }
        else
        {
            buffIn = null;
            buffOut = null;
            bFirstInBuff = null;
            delays = null;
            amps = null;
            
            buffInStart = 1;
            numChannels = 0;
            sumAmps = 1.0;
        }
        
        if (bDelete)
        {
            delaysInMiliseconds = null;
            ampsIn = null;
        }
    }
    
    public void applyInline(double[] data, int pos, int buffOutLen)
    {
        if (buffOutLen != data.length)
            buffOutLen = data.length;
        
        if (buffOutLen>0)
        {
            //Perform processing on each channel
            if (buffOut == null || buffOut.length != buffOutLen)
                buffOut = new double[buffOutLen];
            
            int i, j, ind;
            for (i=0; i<buffOutLen; i++)
                buffOut[i] = 0.0;
    
            for (j=1; j<=buffOutLen; j++)
            {
                buffIn[buffInStart-1] = data[j-1];
                
                for (i=1; i<=numChannels; i++)
                {
                    if (i==1)
                        buffOut[j-1] = 1.0/sumAmps*buffIn[buffInStart-1]; //Delay-less channel 
                    
                    ind = buffInStart-delays[i-1];
                    
                    if (!(bFirstInBuff[i-1]) && ind<1)
                        ind += buffIn.length;
                    
                    if (buffInStart+1>buffIn.length)
                        bFirstInBuff[i-1] = false;
                       
                    if (ind>=1)
                        buffOut[j-1] += amps[i-1]/sumAmps*buffIn[ind-1];
                }
                
                buffInStart++;
                
                if (buffInStart>buffIn.length)
                    buffInStart = 1;
            }
            
            for (i=0; i<buffOutLen; i++)
                data[i] = buffOut[i];
        }  
    }
    
    public static void main(String[] args) throws Exception
    {
        //Simple stadium effect
        int [] delaysInMiliseconds = {366, 500};
        double [] amps = {0.54, -0.10};
        //
        
        for (int i=0; i<args.length; i++) 
        {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, 1024, samplingRate,
                    new Chorus(delaysInMiliseconds, amps, samplingRate));
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_chorusAdded.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
    }

}
