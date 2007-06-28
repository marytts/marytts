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
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class VocalTractScalingSimpleProcessor extends FrequencyDomainProcessor {

    private double [] vscales;
    private int maxFreq;
    private double [] realOut;
    private double [] imagOut;
    
    //Call this function whenever you want to change the scaling ratios
    //If they are fixed for the whole signal, it is sufficient to specify them only once in the constructor below
    public void SetVScales(double [] vscalesIn)
    {
        if (vscalesIn.length>0)
        {
            vscales = InterpolationUtils.modifySize(vscalesIn, maxFreq); //Modify length to match current length of spectrum
            
            for (int i=0; i<maxFreq; i++)
            {
                if (vscales[i]<0.05)
                    vscales[i]=0.05; //Put a floor to avoid divide by zero
            }
        }
        else
            vscales = null;
    }
    
    /**
     * @param fftSize
     */
    public VocalTractScalingSimpleProcessor(int fftSize, double [] vscalesIn) {
        super(fftSize);
        
        maxFreq = fftSize/2+1;
        
        SetVScales(vscalesIn);
        
        realOut = new double[maxFreq];
        imagOut = new double[maxFreq];
    }
    
    
    //Perform linear/non-linear vocal tract scaling
    protected void process(double[] real, double[] imag)
    {
        if (vscales!=null)
        {
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
             
                realOut[i-1] = real[wInd-1];
                imagOut[i-1] = imag[wInd-1];
            }
            //
            
            //Copy the modified DFT to input
            System.arraycopy(realOut, 0, real, 0, maxFreq);
            System.arraycopy(imagOut, 0, imag, 0, maxFreq);
            //
            
            //Generate the complex conjugate part to make the output the DFT of a real-valued signal
            for (i=maxFreq+1; i<=real.length; i++)
            {
                real[i-1] = real[2*maxFreq-i-1];
                imag[i-1] = imag[2*maxFreq-i-1];
            }
            //
        }
    }

    public static void main(String[] args) throws Exception
    {
        //Joint vocal tract and pitch scaling since there is no LP based vocal tract estimation yet
        double [] vscales = {2.0};
        //
        
        for (int i=0; i<args.length; i++) 
        {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, 1024, samplingRate,
                    new VocalTractScalingSimpleProcessor(1024, vscales));
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_vocalTractSimpleScaled.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }
    }
}
