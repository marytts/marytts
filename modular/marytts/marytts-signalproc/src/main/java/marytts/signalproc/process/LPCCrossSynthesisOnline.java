/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.process;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.analysis.LpcAnalyser;
import marytts.signalproc.analysis.LpcAnalyser.LpCoeffs;
import marytts.signalproc.demo.ChangeMyVoiceUI;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.SequenceDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;


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
    protected void processLPC(LpCoeffs coeffs, double[] residual)
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
            newResidualAudioFrames = new FrameProvider(paddedExcitation, Window.get(Window.HANNING, frameLength, 0.5), frameLength, frameLength/4, samplingRate, false);
        }
        
        double gain = coeffs.getGain();
        double[] frame = newResidualAudioFrames.getNextFrame();
        
        assert frame.length == residual.length;
        
        int excP = 3;
        LpCoeffs newCoeffs = LpcAnalyser.calcLPC(frame, excP);
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

