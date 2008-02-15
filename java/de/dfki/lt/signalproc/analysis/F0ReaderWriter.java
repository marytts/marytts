package de.dfki.lt.signalproc.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.dfki.lt.signalproc.analysis.F0Tracker.F0Contour;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.LittleEndianBinaryReader;
import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.MathUtils;

public class F0ReaderWriter {
    public PitchFileHeader header;
    public double [] contour; //f0 values in Hz (0.0 for unvoiced)
    
    public F0ReaderWriter(String ptcFile) {
        contour = null;
        
        header = new PitchFileHeader();
        
        header.ws = 0.0;
        header.ss = 0.0;
        header.fs = 0;
        
        try {
            read_pitch_file(ptcFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public F0ReaderWriter() {
        contour = null;
        
        header = new PitchFileHeader();
        
        header.ws = 0.0;
        header.ss = 0.0;
        header.fs = 0;
    }
    
    //Create f0 contour from pitch marks
    //Note that, as we do not have voicing information, an all-voiced pitch contour is generated
    // using whatever pitch period is assigned to unvoiced segments in the pitch marks
    public F0ReaderWriter(int [] pitchMarks, int samplingRate, float windowSizeInSeconds, float skipSizeInSeconds) 
    {
        contour = null;
     
        header = new PitchFileHeader();
        
        header.ws = windowSizeInSeconds;
        header.ss = skipSizeInSeconds;
        header.fs = samplingRate;
        float currentTime;
        int currentInd;
        
        if (pitchMarks != null && pitchMarks.length>1)
        {
            int numfrm = (int)Math.floor(((float)pitchMarks[pitchMarks.length-2])/header.fs/header.ss+0.5);
            
            if (numfrm>0)
            {
                float [] onsets = SignalProcUtils.samples2times(pitchMarks, header.fs);
                
                contour = new double[numfrm];
                for (int i=0; i<numfrm; i++)
                {
                    currentTime = (float) (i*header.ss+0.5*header.ws);
                    currentInd = MathUtils.findClosest(onsets, currentTime);
                    
                    if (currentInd<onsets.length-1)
                        contour[i] = header.fs/(pitchMarks[currentInd+1]-pitchMarks[currentInd]);
                    else
                        contour[i] = header.fs/(pitchMarks[currentInd]-pitchMarks[currentInd-1]);
                }
            }
        }
    }
    
    public double [] getVoiceds()
    {
        return SignalProcUtils.getVoiceds(contour);
    }
    
    public void read_pitch_file(String ptcFile) throws IOException
    {
        LEDataInputStream lr = new LEDataInputStream(new DataInputStream(new FileInputStream(ptcFile)));
        
        if (lr!=null)
        {
            int winsize = (int)lr.readFloat();
            int skipsize = (int)lr.readFloat();
            header.fs = (int)lr.readFloat();
            header.numfrm = (int)lr.readFloat();

            header.ws = ((double)winsize)/header.fs;
            header.ss = ((double)skipsize)/header.fs;
            contour = new double[header.numfrm];
            
            for (int i=0; i<header.numfrm; i++)
                contour[i] = (double)lr.readFloat();

            lr.close();
        }
    } 
    
    public static void write_pitch_file(String ptcFile, double [] f0s, float windowSizeInSeconds, float skipSizeInSeconds, int samplingRate) throws IOException
    {
        float [] f0sFloat = new float[f0s.length];
        for (int i=0; i<f0s.length; i++)
            f0sFloat[i] = (float)f0s[i];
        
        write_pitch_file(ptcFile, f0sFloat, windowSizeInSeconds, skipSizeInSeconds, samplingRate);
    } 
    
    public static void write_pitch_file(String ptcFile, float [] f0s, float windowSizeInSeconds, float skipSizeInSeconds, int samplingRate) throws IOException
    {
        LEDataOutputStream lw = new LEDataOutputStream(new DataOutputStream(new FileOutputStream(ptcFile)));
        
        if (lw!=null)
        {
            int winsize = (int)Math.floor(windowSizeInSeconds*samplingRate+0.5);
            lw.writeFloat(winsize);
            
            int skipsize = (int)Math.floor(skipSizeInSeconds*samplingRate+0.5);
            lw.writeFloat(skipsize);
            
            lw.writeFloat(samplingRate);
            
            lw.writeFloat(f0s.length);
            
            lw.writeFloat(f0s);

            lw.close();
        }
    } 
}
