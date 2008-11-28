/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.analysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * File I/O for binary pitch contour files
 * 
 * @author Oytun T&uumlrk
 */
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
        if (FileUtils.exists(ptcFile))
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
        else
            System.out.println("Pitch file not found: " + ptcFile);
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
