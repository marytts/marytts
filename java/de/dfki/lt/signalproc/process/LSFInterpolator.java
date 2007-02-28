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
import java.io.FileReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.LabelfileDoubleDataSource;
import de.dfki.lt.util.PrintfFormat;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class LSFInterpolator extends LPCAnalysisResynthesis implements InlineFrameMerger
{
    protected double[] otherFrame1;
    protected double[] otherFrame2;
    protected double relativeWeightOther1;
    protected double r;
    
    /**
     * Create an LSF-based interpolator.
     * @param p the order of LPC analysis
     * @param r the interpolation ratio, between 0 and 1: <code>new = r * this + (1-r) * other</code>
     */
    public LSFInterpolator(int p, double r)
    {
        super(p);
        if (r < 0 || r > 1) throw new IllegalArgumentException("Mixing ratio r must be between 0 and 1");
        this.r = r;
    }
    
    /**
     * Set the frame of data to merge into the next call of applyInline().
     * This is the data towards which LSF-based interpolation will be done.
     * @param frameToMerge
     */
    public void setFrameToMerge(double[] frameToMerge)
    {
        this.otherFrame1 = frameToMerge;
        this.otherFrame2 = null;
        this.relativeWeightOther1 = 1;
    }

    /**
     * Set the frame of data to merge into the next call of applyInline().
     * This method allows for an interpolation of two frames to be merged into the data set;
     * for example, in order to correct for time misalignment between signal and other frames.
     * @param frame1 
     * @param frame2
     * @param relativeWeightFrame1, a number between 0 and 1 indicating the relative weight of frame1^
     * with respect to frame2. Consequently, the relative weight of frame 2 will be (1 - relativeWeightFrame1).
     */
    public void setFrameToMerge(double[] frame1, double[] frame2, double relativeWeightFrame1)
    {
        this.otherFrame1 = frame1;
        this.otherFrame2 = frame2;
        this.relativeWeightOther1 = relativeWeightFrame1;
    }

    
    /**
     * Process the LPC coefficients in place. This implementation converts
     * the LPC coefficients into line spectral frequencies, and interpolates
     * between these and the corresponding frame in the "other" signal.
     * @param a the LPC coefficients
     */
    protected void processLPC(LPCoeffs coeffs, double[] residual) 
    {
        if (otherFrame1 == null) return; // no more other audio -- leave signal as is
        LPCoeffs otherCoeffs = LPCAnalyser.calcLPC(otherFrame1, p);
        double[] otherlsf = otherCoeffs.getLSF();
        double[] lsf = coeffs.getLSF();
        assert lsf.length == otherlsf.length;
        if (otherFrame2 != null && relativeWeightOther1 < 1) { // optionally, interpolate between two "other" frames before merging into the signal
            assert 0 <= relativeWeightOther1;
            LPCoeffs other2Coeffs = LPCAnalyser.calcLPC(otherFrame2, p);
            double[] other2lsf = other2Coeffs.getLSF();
            /*PrintfFormat f = new PrintfFormat("%      .1f ");
            System.out.print("LSF     ");
            for (int i=0; i<lsf.length; i++) {
                System.out.print(f.sprintf(lsf[i]*16000));
            }
            System.out.println();
            
            System.out.print("Other1  ");
            for (int i=0; i<lsf.length; i++) {
                System.out.print(f.sprintf(otherlsf[i]*16000));
            }
            System.out.println();

            System.out.print("Other2  ");
            for (int i=0; i<lsf.length; i++) {
                System.out.print(f.sprintf(other2lsf[i]*16000));
            }
            System.out.println();

            System.out.println();
            */
            for (int i=0; i<otherlsf.length; i++) {
                otherlsf[i] = relativeWeightOther1*otherlsf[i] + (1-relativeWeightOther1)*other2lsf[i];
            }
        }
        // now interpolate between the two:
        for (int i=0; i<lsf.length; i++)
            lsf[i] = (1-r)*lsf[i] + r*otherlsf[i];
        coeffs.setLSF(lsf);
        // Adapt residual gain to also interpolate average energy:
        double gainFactor = Math.sqrt((1-r)*coeffs.getGain()*coeffs.getGain() + r*otherCoeffs.getGain()*otherCoeffs.getGain())/coeffs.getGain();
//        System.out.println("Gain:" + coeffs.getGain() + ", otherGain:"+otherCoeffs.getGain()+", factor="+gainFactor);
        for (int i=0; i<residual.length; i++)
            residual[i] *= gainFactor;
        
    }


    public static void main(String[] args) throws Exception
    {
        long startTime = System.currentTimeMillis();
        double r = Double.parseDouble(System.getProperty("r", "0.5"));
        String file1 = null;
        String file2 = null;
        DoubleDataSource label1 = null;
        DoubleDataSource label2 = null;
        if (args.length == 2) {
            file1 = args[0];
            file2 = args[1];
        } else if (args.length == 4) {
            file1 = args[0];
            label1 = new LabelfileDoubleDataSource(new FileReader(args[1]));
            file2 = args[2];
            label2 = new LabelfileDoubleDataSource(new FileReader(args[3]));
            // Safety check: verify that we have the same number of labels in both files
            double[] labelData1 = label1.getAllData();
            double[] labelData2 = label2.getAllData();
            if (labelData1.length != labelData2.length) {
                System.err.println("Warning: Number of labels is different!");
                System.err.println(args[1]+":");
                System.err.println(FileUtils.getFileAsString(new File(args[1]), "ASCII"));
                System.err.println(args[3]+":");
                System.err.println(FileUtils.getFileAsString(new File(args[3]), "ASCII"));
            } // but continue
            label1 = new BufferedDoubleDataSource(labelData1);
            label2 = new BufferedDoubleDataSource(labelData2);
        } else {
            System.out.println("Usage: java [-Dr=<mixing ratio> de.dfki.lt.signalproc.process.LSFInterpolator signal.wav [signal.lab] other.wav [other.lab]");
            System.out.println("where");
            System.out.println("    <mixing ratio> is a value between 0.0 and 1.0 indicating how much of \"other\" is supposed to be mixed into \"signal\"");
            System.exit(1);
        }
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(file1));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        AudioInputStream otherAudio = AudioSystem.getAudioInputStream(new File(file2));
        DoubleDataSource otherSource = new AudioDoubleDataSource(otherAudio);
        int frameLength = Integer.getInteger("signalproc.lpcanalysisresynthesis.framelength", 512).intValue();
        int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
        FramewiseMerger foas = new FramewiseMerger(signal, frameLength, samplingRate, label1,
                otherSource, samplingRate, label2, 
                new LSFInterpolator(predictionOrder, r));
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), inputAudio.getFormat());
        String outFileName = file1.substring(0, file1.length()-4) + "_" + file2.substring(file2.lastIndexOf("\\")+1, file2.length()-4)+"_"+r+".wav";
        AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
        long endTime = System.currentTimeMillis();
        int audioDuration = (int) (AudioSystem.getAudioFileFormat(new File(file1)).getFrameLength() / (double)samplingRate * 1000);
        System.out.println("LSF-based interpolatin took "+ (endTime-startTime) + " ms for "+ audioDuration + " ms of audio");
        
    }

}
