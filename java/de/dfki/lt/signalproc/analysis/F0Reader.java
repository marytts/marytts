package de.dfki.lt.signalproc.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.dfki.lt.signalproc.analysis.F0Tracker.F0Contour;
import de.dfki.lt.signalproc.util.LittleEndianBinaryReader;
import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.MathUtils;

public class F0Reader {
    public double ws; //Window size in seconds
    public double ss; //Skip size in seconds
    public int fs; //Rate in Hz
    protected double [] contour; //f0 values in Hz (0.0 for unvoiced)
    
    public F0Reader(String ptcFile) {
        contour = null;
        ws = 0.0;
        ss = 0.0;
        fs = 0;
        
        try {
            read_pitch_file(ptcFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    //Create f0 contour from pitch marks
    //Note that, as we do not have voicing information, an all-voiced pitch contour is generated
    // using whatever pitch period is assigned to unvoiced segments in the pitch marks
    public F0Reader(int [] pitchMarks, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds) 
    {
        contour = null;
        ws = windowSizeInSeconds;
        ss = skipSizeInSeconds;
        fs = samplingRate;
        float currentTime;
        int currentInd;
        
        if (pitchMarks != null && pitchMarks.length>1)
        {
            int numfrm = (int)Math.floor(((float)pitchMarks[pitchMarks.length-2])/fs/ss+0.5);
            
            if (numfrm>0)
            {
                float [] onsets = SignalProcUtils.samples2times(pitchMarks, fs);
                
                contour = new double[numfrm];
                for (int i=0; i<numfrm; i++)
                {
                    currentTime = (float) (i*ss+0.5*ws);
                    currentInd = MathUtils.findClosest(onsets, currentTime);
                    
                    if (currentInd<onsets.length-1)
                        contour[i] = fs/(pitchMarks[currentInd+1]-pitchMarks[currentInd]);
                    else
                        contour[i] = fs/(pitchMarks[currentInd]-pitchMarks[currentInd-1]);
                }
            }
        }
    }
    
    public double [] getContour()
    {
        return contour;
    }
    
    public void read_pitch_file(String ptcFile) throws IOException
    {
        //LittleEndianBinaryReader lr = new LittleEndianBinaryReader(new DataInputStream(new FileInputStream(ptcFile)));
        LEDataInputStream lr = new LEDataInputStream(new DataInputStream(new FileInputStream(ptcFile)));
        
        if (lr!=null)
        {
            int winsize = (int)lr.readFloat();
            int skipsize = (int)lr.readFloat();
            fs = (int)lr.readFloat();
            int numfrm = (int)lr.readFloat();

            ws = ((double)winsize)/fs;
            ss = ((double)skipsize)/fs;
            contour = new double[numfrm];
            
            for (int i=0; i<numfrm; i++)
                contour[i] = (double)lr.readFloat();

            lr.close();
        }
    } 
}
