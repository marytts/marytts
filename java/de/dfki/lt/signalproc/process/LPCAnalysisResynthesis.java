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

package de.dfki.lt.signalproc.process;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.filter.RecursiveFilter;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.util.ArrayUtils;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * A base class for LPC-based analysis and resynthesis, which does nothing.
 * @author Marc Schr&ouml;der
 *
 */
public class LPCAnalysisResynthesis implements InlineDataProcessor
{
    protected int p;
    
    /**
     * Apply LPC analysis-resynthesis.
     * @param p prediction order, i.e. number of LPC coefficients to compute.
     */
    public LPCAnalysisResynthesis(int p) 
    {
        this.p = p;
    }
    
    public void applyInline(double[] data, int off, int len)
    {
        assert off==0;
        assert len==data.length;
        // Compute LPC coefficients and residual
        LPCoeffs coeffs = LPCAnalyser.calcLPC(data, p);
        //double gain = coeffs.getGain();
        double[] residual = ArrayUtils.subarray(new FIRFilter(coeffs.getOneMinusA()).apply(data),0,len);
        // Do something fancy with the lpc coefficients and/or the residual
        processLPC(coeffs, residual);
        // Resynthesise audio from residual and LPC coefficients
        double[] newData = new RecursiveFilter(coeffs.getA()).apply(residual);
        //System.err.println("Sum squared error:"+MathUtils.sumSquaredError(data, newData));
        System.arraycopy(newData, 0, data, 0, len);
    }
    
    /**
     * Process the LPC coefficients and/or the residual in place.
     * This method does nothing; subclasses may want to override it
     * to do something meaningful.
     * @param coeffs the LPC coefficients
     * @param residual the residual, of length framelength
     */
    protected void processLPC(LPCoeffs coeffs, double[] residual) {}


    public static void main(String[] args) throws Exception
    {
        for (int i=0; i<args.length; i++) {
            AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[i]));
            int samplingRate = (int)inputAudio.getFormat().getSampleRate();
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
            int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
            int predictionOrder = Integer.getInteger("signalproc.lpcanalysissynthesis.predictionorder", 20).intValue();
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, samplingRate, new LPCAnalysisResynthesis(predictionOrder));
            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
            String outFileName = args[i].substring(0, args[i].length()-4) + "_lpc_ar.wav";
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        }

    }

}
