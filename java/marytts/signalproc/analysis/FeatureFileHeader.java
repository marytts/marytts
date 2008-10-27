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

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * @author oytun.turk
 * 
 * Generic file header for binary acoustic feature files
 * 
 */
public class FeatureFileHeader {
    public int numfrm; //Total number of frames
    public int dimension; //Feature vector dimension (total)
    public float winsize; //Analysis window size in seconds
    public float skipsize; //Analysis skip size in seconds
    public int samplingRate; //Sampling rate in Hz
    
    public FeatureFileHeader()
    {
        numfrm = 0;
        dimension = 0;
        winsize = 0.020f;
        skipsize = 0.010f;
        samplingRate = 0;
    }
    
    public FeatureFileHeader(FeatureFileHeader existingHeader)
    {
        numfrm = existingHeader.numfrm;
        dimension = existingHeader.dimension;
        winsize = existingHeader.winsize;
        skipsize = existingHeader.skipsize;
        samplingRate = existingHeader.samplingRate;
    }
    
    public FeatureFileHeader(String featureFile)
    {
        try {
            readHeader(featureFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean isIdenticalAnalysisParams(FeatureFileHeader hdr)
    {   
        if (this.numfrm!=hdr.numfrm)
            return false;
        if (this.dimension!=hdr.dimension)
            return false;
        if (this.winsize!=hdr.winsize)
            return false;
        if (this.skipsize!=hdr.skipsize)
            return false;
        if (this.samplingRate!=hdr.samplingRate)
            return false;
        else
            return true;
    }
    
    public void readHeader(String file) throws IOException
    {
        readHeader(file, false);
    }
    
    public MaryRandomAccessFile readHeader(String file, boolean bLeaveStreamOpen) throws IOException
    {
        MaryRandomAccessFile stream = new MaryRandomAccessFile(file, "rw");

        if (stream!=null)
            readHeader(stream, bLeaveStreamOpen);
        
        return stream;
    }
    
    public void readHeader(MaryRandomAccessFile stream) throws IOException
    {
        readHeader(stream, true);
    }
    
    //Baseline version does nothing!
    //It is the derived class´ responsibility to do the reading and closing the file handle
    public void readHeader(MaryRandomAccessFile stream, boolean bLeaveStreamOpen) throws IOException
    {
        numfrm = stream.readInt();
        dimension = stream.readInt();
        winsize = stream.readFloat();
        skipsize = stream.readFloat();
        samplingRate = stream.readInt();
        
        if (!bLeaveStreamOpen)
        {
            stream.close();
            stream = null;
        }
    }
    
    public void writeHeader(String file) throws IOException
    {
        writeHeader(file, false);
    }
    
    //This version returns the file output stream for further use, i.e. if you want to write additional information
    // in the file use this version
    public MaryRandomAccessFile writeHeader(String file, boolean bLeaveStreamOpen) throws IOException
    {
        MaryRandomAccessFile stream = new MaryRandomAccessFile(file, "rw");

        if (stream!=null)
        {
            writeHeader(stream);
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
        
        return stream;
    }
    
    //Baseline version does nothing!
    //It is the derived class´ responsibility to do the writing and closing the file handle
    public void writeHeader(MaryRandomAccessFile ler) throws IOException
    {   
        ler.writeInt(numfrm);
        ler.writeInt(dimension);
        ler.writeFloat(winsize);
        ler.writeFloat(skipsize);
        ler.writeInt(samplingRate);
    }
}
