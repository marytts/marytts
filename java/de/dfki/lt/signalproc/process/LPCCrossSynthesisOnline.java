package de.dfki.lt.signalproc.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.analysis.LPCAnalyser;
import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.demo.ChangeMyVoiceUI;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class LPCCrossSynthesisOnline extends LPCAnalysisResynthesis {
    protected int frameLength;
    protected AudioInputStream residualStream;
    protected DoubleDataSource newResidual;
    protected DoubleDataSource padding1;
    protected DoubleDataSource paddedExcitation;
    protected FrameProvider newResidualAudioFrames;
    protected int samplingRate;
    protected InputStream resStream;
    protected String resFile;
    
    public LPCCrossSynthesisOnline(int p, int frmLen, String inResFile, int fs)
    {
        super(p);
        
        this.resFile = inResFile;
        this.frameLength = frmLen;
        this.samplingRate = fs;
        
        this.resStream = null;
        this.residualStream = null;
        this.newResidual = null;
        this.padding1 = null;
        this.paddedExcitation = null;
        this.newResidualAudioFrames = null;  
    }
    
    /**
     * Replace residual with new residual from audio signal,
     * adapting the gain in order to maintain overall volume.
     */
    protected void processLPC(LPCoeffs coeffs, double[] residual)
    {   
        if (newResidualAudioFrames==null || !newResidualAudioFrames.hasMoreData())
        {
            resStream = ChangeMyVoiceUI.class.getResourceAsStream(resFile);
            
            try {
                residualStream = AudioSystem.getAudioInputStream(resStream);
            } catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            newResidual = new AudioDoubleDataSource(residualStream);
            padding1 = new BufferedDoubleDataSource(new double[3*frameLength/4]);
            paddedExcitation = new SequenceDoubleDataSource(new DoubleDataSource[]{padding1, newResidual});
            newResidualAudioFrames = new FrameProvider(paddedExcitation, Window.get(Window.HANN, frameLength, 0.5), frameLength, frameLength/4, samplingRate, false);
        }
        
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

}
