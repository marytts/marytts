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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class VocalTractScalingProcessor extends VocalTractModifier {
    private double [] vscales;
    private double [] PxOut;
    /**
     * @param p
     * @param fs
     * @param fftSize
     */
    public VocalTractScalingProcessor(int p, int fs, int fftSize, double [] vscalesIn) {
        super(p, fs, fftSize);
        
        PxOut = new double[this.maxFreq];
        
        if (vscalesIn.length>0)
        {
            vscales = InterpolationUtils.modifySize(vscalesIn, this.maxFreq); //Modify length to match current length of spectrum
            
            for (int i=0; i<this.maxFreq; i++)
            {
                if (vscales[i]<0.05)
                    vscales[i]=0.05; //Put a floor to avoid divide by zero
            }
        }
        else
            vscales = null;
    }
    
    protected void processSpectrum(double [] Px) 
    {
        if (vscales!=null)
        {
            /*
            //Scale the vocal tract
            int i;
            int wInd;
            for (i=1; i<=maxFreq; i++)
            {
                wInd = (int)(Math.floor(((double)i)/vscales[i-1]+0.5)); //Find new index
                if (wInd<1)
                    wInd=1;
                if (wInd>maxFreq)
                    wInd=maxFreq;
             
                PxOut[i-1] = Px[wInd-1];
            }
            //
            
            //Copy the modified vocal tract spectrum to input
            System.arraycopy(PxOut, 0, Px, 0, maxFreq);
            //
             */

            int newLen = (int)Math.floor(Px.length*vscales[0] + 0.5);
            
            double [] Px2 = MathUtils.interpolate(Px, newLen);
            
            int i;
            
            if (newLen>maxFreq)
            {
                for (i=0; i<maxFreq; i++)
                    Px[i] = Px2[i];
            }
            else
            {
                for (i=0; i<newLen; i++)
                    Px[i] = Px2[i];
                for (i=newLen; i<maxFreq; i++)
                    Px[i] = 0.0;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        double [] vscales = {1.0};
        
        for (int i=0; i<args.length; i++) 
        {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            int p = SignalProcUtils.getLPOrder(samplingRate);
            int fftSize = Math.max(SignalProcUtils.getDFTSize(samplingRate), 1024);
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, fftSize, samplingRate,
                    new VocalTractScalingProcessor(p, samplingRate, fftSize, vscales));
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_vocalTractScaled.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }

    }

}
