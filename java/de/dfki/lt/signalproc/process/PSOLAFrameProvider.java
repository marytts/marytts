package de.dfki.lt.signalproc.process;

import java.util.Arrays;

import de.dfki.lt.signalproc.analysis.PitchMarker;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.window.Window;

public class PSOLAFrameProvider {
    protected double [] buffer;
    protected DoubleDataSource input;
    protected int index; //Pitch mark index for pitch synchronous processing, and frame index for fixed window size & rate processing
    protected int numPeriods;
    protected PitchMarker pitchMarker;
    protected int frmSize;
    protected int prevFrmSize;
    protected int remain;
    protected int fromBuffer;
    private boolean isFixedRate;
    private int wsFixedLen;
    private int ssFixedLen;
    private int totalFixedFrames;
    private double currentTimeInSeconds;
    private int samplingRate;
    
    //Pitch synchronous frame provider
    public PSOLAFrameProvider(DoubleDataSource inputSource, PitchMarker pm, int fs, int psPeriods)
    {
        this.input = inputSource;
        this.numPeriods = psPeriods;
        this.pitchMarker = pm;
        
        samplingRate = fs;
        int maxFrmSize = (int)(numPeriods*fs/40.0);
        if ((maxFrmSize % 2) != 0)
            maxFrmSize++;
            
        this.buffer = new double[maxFrmSize];
        Arrays.fill(buffer, 0.0);
        
        index = -1;
        isFixedRate = false;
        totalFixedFrames = 0;
        currentTimeInSeconds = -1.0;
    }
    
    //Fixed rate frame provider
    public PSOLAFrameProvider(DoubleDataSource inputSource, double fixedWindowSizeInSeconds, double fixedSkipSizeInSeconds, int fs, int totalFrames)
    {
        this.input = inputSource;
        this.numPeriods = -1;
        this.pitchMarker = null;
        
        samplingRate = fs;
        wsFixedLen = (int)Math.floor(fixedWindowSizeInSeconds*fs + 0.5);
        if ((wsFixedLen % 2) != 0)
            wsFixedLen++;
        
        if (wsFixedLen<4)
            wsFixedLen = 4;
        
        frmSize = wsFixedLen;
        
        ssFixedLen = (int)Math.floor(fixedSkipSizeInSeconds*fs + 0.5);
        
        int maxFrmSize = wsFixedLen;
            
        this.buffer = new double[maxFrmSize];
        Arrays.fill(buffer, 0.0);
        
        index = -1;
        isFixedRate = true;
        totalFixedFrames = totalFrames;
        currentTimeInSeconds = -1.0;
    }

    public double[] getNextFrame()
    {
        double [] y = null;

        if (!isFixedRate) //Return next pitch synchronous speech frame
        {
            index++;
            
            if (index+numPeriods<pitchMarker.pitchMarks.length)
            {
                frmSize = pitchMarker.pitchMarks[index+numPeriods]-pitchMarker.pitchMarks[index]+1;
                currentTimeInSeconds = SignalProcUtils.sample2time((int)(0.5*(pitchMarker.pitchMarks[index+numPeriods]+pitchMarker.pitchMarks[index]+1)), samplingRate);
                if ((frmSize % 2) !=0) 
                    frmSize++;

                if (frmSize<4)
                    frmSize = 4;

                y = new double[frmSize];


                if (index==0) //Read all from the source
                    input.getData(y, 0, frmSize);
                else //Read numPeriods-1 pitch synchronous frames from the buffer and one period from the source
                {
                    fromBuffer = prevFrmSize-(pitchMarker.pitchMarks[index]-pitchMarker.pitchMarks[index-1]);
                    System.arraycopy(buffer, pitchMarker.pitchMarks[index]-pitchMarker.pitchMarks[index-1], y, 0, fromBuffer);

                    remain = frmSize - fromBuffer;
                    input.getData(y, fromBuffer, remain); 
                }

                System.arraycopy(y, 0, buffer, 0, frmSize);
                prevFrmSize = frmSize;
            } 
        }
        else //Return next fixed window size and rate speech frame
        {
            index++;

            if (index<totalFixedFrames)
            {
                y = new double[frmSize];

                if (index==0) //Read all from the source
                    input.getData(y, 0, frmSize);
                else //Read numPeriods-1 pitch synchronous frames from the buffer and one period from the source
                {
                    System.arraycopy(buffer, prevFrmSize-ssFixedLen, y, 0, ssFixedLen);

                    remain = frmSize - ssFixedLen;
                    input.getData(y, ssFixedLen, remain); 
                }

                currentTimeInSeconds = SignalProcUtils.sample2time((int)(index*ssFixedLen+0.5*frmSize), samplingRate);
                
                System.arraycopy(y, 0, buffer, 0, frmSize);
                prevFrmSize = frmSize;
            }
        }

        return y;
    }
    
    public double getCurrentTime()
    {
        return currentTimeInSeconds;
    }
}
