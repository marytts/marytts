package de.dfki.lt.signalproc.process;

import java.util.Arrays;

import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.window.Window;

public class PSOLAFrameProvider {
    protected double [] buffer;
    protected DoubleDataSource input;
    protected int pitchMarkIndex;
    protected int numPeriods;
    protected PitchMarker pitchMarker;
    protected int frmSize;
    protected int prevFrmSize;
    protected int remain;
    protected int fromBuffer;
    
    public PSOLAFrameProvider(DoubleDataSource inputSource, PitchMarker pm, int fs, int psPeriods)
    {
        this.input = inputSource;
        this.numPeriods = psPeriods;
        this.pitchMarker = pm;
        
        int maxFrmSize = (int)(numPeriods*fs/40.0);
        if ((maxFrmSize % 2) != 0)
            maxFrmSize++;
            
        this.buffer = new double[maxFrmSize];
        Arrays.fill(buffer, 0.0);
        
        pitchMarkIndex = -1;
    }

    protected double[] getNextFrame()
    {
        double [] y = null;
        pitchMarkIndex++;

        if (pitchMarkIndex+numPeriods<pitchMarker.pitchMarks.length)
        {
            frmSize = pitchMarker.pitchMarks[pitchMarkIndex+numPeriods]-pitchMarker.pitchMarks[pitchMarkIndex]+1;
                
            if ((frmSize % 2) !=0) 
                frmSize++;

            if (frmSize<4)
                frmSize = 4;

            y = new double[frmSize];
            
            
            if (pitchMarkIndex==0) //Read all from the source
                input.getData(y, 0, frmSize);
            else //Read numPeriods-1 pitch synchronous frames from the buffer and one period from the source
            {
                fromBuffer = prevFrmSize-(pitchMarker.pitchMarks[pitchMarkIndex]-pitchMarker.pitchMarks[pitchMarkIndex-1]);
                System.arraycopy(buffer, pitchMarker.pitchMarks[pitchMarkIndex]-pitchMarker.pitchMarks[pitchMarkIndex-1], y, 0, fromBuffer);
                
                remain = frmSize - fromBuffer;
                input.getData(y, fromBuffer, remain); 
            }
            
            System.arraycopy(y, 0, buffer, 0, frmSize);
            prevFrmSize = frmSize;
        } 

        return y;
    }
}
