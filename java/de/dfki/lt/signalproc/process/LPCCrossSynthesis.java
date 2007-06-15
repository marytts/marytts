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
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.filter.FIRFilter;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.util.ArrayUtils;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LPCCrossSynthesis extends LPCAnalysisResynthesis
{
    protected FrameProvider newResidualAudioFrames;
    
    public LPCCrossSynthesis(FrameProvider newResidualAudioFrames, int p)
    {
        super(p);
        this.newResidualAudioFrames = newResidualAudioFrames;
    }
    
    /**
     * Replace residual with new residual from audio signal,
     * adapting the gain in order to maintain overall volume.
     */
    protected void processLPC(LPCoeffs coeffs, double[] residual)
    {
        double gain = coeffs.getGain();
        double[] frame = newResidualAudioFrames.getNextFrame();
        assert frame.length == residual.length;
        int excP = 3;
        LPCoeffs newCoeffs = LPCAnalyser.calcLPC(frame, excP);
        double newResidualGain = newCoeffs.getGain();
        //double[] newResidual = ArrayUtils.subarray(new FIRFilter(oneMinusA).apply(frame),0,frame.length);
        //System.arraycopy(newResidual, 0, residual, 0, residual.length);
        double gainFactor = gain/newResidualGain;
        Arrays.fill(residual, 0);
        for (int n=0; n<residual.length; n++) {
            for (int i=0; i<=excP && i<=n; i++) {
                residual[n] += newCoeffs.getOneMinusA(i) * frame[n-i];
            }
            residual[n] *= gainFactor;
        }
//      System.out.println("Gain:" + coeffs.getGain() + ", otherGain:"+newCoeffs.getGain()+", factor="+gainFactor);

    }

    public static void main(String[] args) throws Exception
    {
        long startTime = System.currentTimeMillis();
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        AudioInputStream newResidualAudio = AudioSystem.getAudioInputStream(new File(args[1]));
        DoubleDataSource newResidual = new AudioDoubleDataSource(newResidualAudio);
        int frameLength = Integer.getInteger("signalproc.lpcanalysisresynthesis.framelength", 512).intValue();
        int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
        DoubleDataSource padding1 = new BufferedDoubleDataSource(new double[3*frameLength/4]);
        DoubleDataSource paddedExcitation = new SequenceDoubleDataSource(new DoubleDataSource[]{padding1, newResidual});
        FrameProvider newResidualAudioFrames = new FrameProvider(paddedExcitation, Window.get(Window.HANN, frameLength, 0.5), frameLength, frameLength/4, samplingRate, true);
        FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, false, frameLength, samplingRate,
                new LPCCrossSynthesis(newResidualAudioFrames, predictionOrder));
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
        String outFileName = args[0].substring(0, args[0].length()-4) + "_" + args[1].substring(args[1].lastIndexOf("\\")+1, args[1].length()-4)+"_lpcCrossSynth.wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        long endTime = System.currentTimeMillis();
        System.out.println("LPC cross synthesis took "+ (endTime-startTime) + " ms");
        
    }

}
