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

public class F0Reader {
    public double ws;
    public double ss;
    public int fs;
    protected double [] contour;
    
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
