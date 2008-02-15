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

package de.dfki.lt.signalproc.analysis;

import java.io.IOException;

import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 */
public class PitchFileHeader {
    public double ws; //Window size in seconds
    public double ss; //Skip size in seconds
    public int fs; //Rate in Hz
    public int numfrm; //Number of frames
    public double voicingThreshold; //Voicing threshold
    public static double DEFAULT_VOICING_THRESHOLD = 0.35; //Default voicing threshold
    
    public double minimumF0; //Min f0 in Hz
    public double maximumF0; //Max f0 in Hz
    public static double DEFAULT_MINIMUM_F0 = 50.0;
    public static double DEFAULT_MAXIMUM_F0 = 500.0;
    
    public boolean isDoublingCheck;
    public boolean isHalvingCheck;
    public static boolean DEFAULT_DOUBLING_CHECK = true;
    public static boolean DEFAULT_HALVING_CHECK = true;
    
    public double centerClippingRatio;
    public static double DEFAULT_CENTER_CLIPPING_RATIO = 0.5;
    
    public double cutOff1; //Lower cut-off freq for bandpass filter in Hz (or cut-off freq for lowpass filter if cutOff2<=0.0)
    public double cutOff2; //Upper cut-off freq for bandpass filter in Hz (Set to <=0.0 if you want a lowpass filter with cut-off at cutOff1 Hz.)
    public static double DEFAULT_CUTOFF1 = DEFAULT_MINIMUM_F0-20.0;
    public static double DEFAULT_CUTOFF2 = DEFAULT_MAXIMUM_F0+200.0;
        
    public PitchFileHeader()
    {
        ws = 0.040;
        ss = 0.005;
        fs = 0;
        numfrm = 0;
        voicingThreshold = DEFAULT_VOICING_THRESHOLD;
        minimumF0 = DEFAULT_MINIMUM_F0;
        maximumF0 = DEFAULT_MAXIMUM_F0;
        isDoublingCheck = DEFAULT_DOUBLING_CHECK;
        isHalvingCheck = DEFAULT_HALVING_CHECK;
        centerClippingRatio = DEFAULT_CENTER_CLIPPING_RATIO;
        cutOff1 = DEFAULT_CUTOFF1;
        cutOff2 = DEFAULT_CUTOFF2;
    }
    
    public PitchFileHeader(PitchFileHeader existingHeader)
    {
        ws = existingHeader.ws;
        ss = existingHeader.ss;
        fs = existingHeader.fs;
        numfrm = existingHeader.numfrm;
        voicingThreshold = existingHeader.voicingThreshold;
        minimumF0 = existingHeader.minimumF0;
        maximumF0 = existingHeader.maximumF0;
        isDoublingCheck = existingHeader.isDoublingCheck;
        isHalvingCheck = existingHeader.isHalvingCheck;
        centerClippingRatio = existingHeader.centerClippingRatio;
        cutOff1 = existingHeader.cutOff1;
        cutOff2 = existingHeader.cutOff2;
    }
    
    public void readPitchHeader(MaryRandomAccessFile stream) throws IOException
    {
        readPitchHeader(stream, true);
    }
    
    public void readPitchHeader(MaryRandomAccessFile stream, boolean bLeaveStreamOpen) throws IOException
    {
        if (stream!=null)
        {
            ws = stream.readDouble();
            ss = stream.readDouble();
            fs = stream.readInt();
            numfrm = stream.readInt();
            voicingThreshold = stream.readDouble();
            minimumF0 = stream.readDouble();
            maximumF0 = stream.readDouble();
            isDoublingCheck = stream.readBoolean();
            isHalvingCheck = stream.readBoolean();
            centerClippingRatio = stream.readDouble();
            cutOff1 = stream.readDouble();
            cutOff2 = stream.readDouble();
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
    }
    
    public void readPitchHeaderOld(LEDataInputStream stream) throws IOException
    {
        readPitchHeaderOld(stream, true);
    }
    
    //The old version kept window size and skip size in samples so perform conversion to seconds after reading them from file
    public void readPitchHeaderOld(LEDataInputStream stream, boolean bLeaveStreamOpen) throws IOException
    {
        if (stream!=null)
        {
            ws = stream.readFloat();
            ss = stream.readFloat();
            fs = stream.readInt();
            numfrm = stream.readInt();
            voicingThreshold = DEFAULT_VOICING_THRESHOLD;
            minimumF0 = DEFAULT_MINIMUM_F0;
            maximumF0 = DEFAULT_MAXIMUM_F0;
            isDoublingCheck = DEFAULT_DOUBLING_CHECK;
            isHalvingCheck = DEFAULT_HALVING_CHECK;
            centerClippingRatio = DEFAULT_CENTER_CLIPPING_RATIO;
            cutOff1 = DEFAULT_CUTOFF1;
            cutOff2 = DEFAULT_CUTOFF2;
            
            ws = ws/fs;
            ss = ss/fs;
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
    }
    
    public void writePitchHeader(String pitchFile) throws IOException
    {
        writePitchHeader(pitchFile, false);
    }
    
    //This version returns the file output stream for further use, i.e. if you want to write additional information
    // in the file use this version
    public MaryRandomAccessFile writePitchHeader(String pitchFile, boolean bLeaveStreamOpen) throws IOException
    {
        MaryRandomAccessFile stream = new MaryRandomAccessFile(pitchFile, "rw");

        if (stream!=null)
        {
            writePitchHeader(stream);
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
        
        return stream;
    }
    
    public void writePitchHeader(MaryRandomAccessFile ler) throws IOException
    {   
        ler.writeDouble(ws);
        ler.writeDouble(ss);
        ler.writeInt(fs);
        ler.writeInt(numfrm);
        ler.writeDouble(voicingThreshold);
        ler.writeDouble(minimumF0);
        ler.writeDouble(maximumF0);
        ler.writeBoolean(isDoublingCheck);
        ler.writeBoolean(isHalvingCheck);
        ler.writeDouble(centerClippingRatio);
        ler.writeDouble(cutOff1);
        ler.writeDouble(cutOff2);
    }
    
    public void writePitchHeaderOld(String pitchFile) throws IOException
    {
        writePitchHeaderOld(pitchFile, false);
    }
    
    //This version returns the file output stream for further use, i.e. if you want to write additional information
    // in the file use this version
    public LEDataOutputStream writePitchHeaderOld(String pitchFile, boolean bLeaveStreamOpen) throws IOException
    {
        LEDataOutputStream stream = new LEDataOutputStream(pitchFile);

        if (stream!=null)
        {
            writePitchHeaderOld(stream);
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
        
        return stream;
    }
    
    public void writePitchHeaderOld(LEDataOutputStream ler) throws IOException
    {   
        ler.writeDouble(ws);
        ler.writeDouble(ss);
        ler.writeInt(fs);
        ler.writeInt(numfrm);
    }
    
}
