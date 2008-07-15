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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 * A wrapper class for frame based mel frequency cepstral coefficient vectors.
 *
 */
public class Mfccs {
    public double[][] mfccs;
    public MfccFileHeader params;
    
    public Mfccs()
    {
        this("");
    }
    
    public Mfccs(String mfccFile)
    {
        readMfccFile(mfccFile);
    }
    
    public Mfccs(int numfrmIn, int dimensionIn)
    {
        params = new MfccFileHeader();
        allocate(numfrmIn, dimensionIn);
    }
    
    public void readMfccFile(String mfccFile)
    {
        mfccs = null;
        params = new MfccFileHeader();
        
        if (mfccFile!="")
        {
            MaryRandomAccessFile stream = null;
            try {
                stream = params.readHeader(mfccFile, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (stream != null)
            {
                try {
                    mfccs = readMfccs(stream, params);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void writeMfccFile(String mfccFile)
    {   
        if (mfccFile!="")
        {
            MaryRandomAccessFile stream = null;
            try {
                stream = params.writeHeader(mfccFile, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (stream != null)
            {
                try {
                    writeMfccs(stream, mfccs);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    public void allocate()
    {
        allocate(params.numfrm, params.dimension);
    }
    
    public void allocate(int numEntries, int dimension)
    {
        mfccs = null;
        params.numfrm = 0;
        params.dimension = 0;
        
        if (numEntries>0)
        {
            mfccs = new double[numEntries][];
            params.numfrm = numEntries;
            
            if (dimension>0)
            {
                params.dimension = dimension;
                
                for (int i=0; i<numEntries; i++)
                    mfccs[i] = new double[dimension];
            }
        }
    }
    
    public static void writeMfccFile(double[][] mfccs, String mfccFileOut, MfccFileHeader params) throws IOException
    {
        params.numfrm = mfccs.length;
        MaryRandomAccessFile stream = params.writeHeader(mfccFileOut, true);
        writeMfccs(stream, mfccs);
    }
    
    public static void writeMfccsFloat(MaryRandomAccessFile stream, double[][] mfccs) throws IOException
    {
        if (stream!=null && mfccs!=null && mfccs.length>0)
        {
            int i, j;
            for (i=0; i<mfccs.length; i++)
            {
                for (j=0; j<mfccs[i].length; j++)
                    stream.writeFloat((float)mfccs[i][j]);
            }
            
            stream.close();
        }
    }
    
    public static void writeMfccs(MaryRandomAccessFile stream, double[][] mfccs) throws IOException
    {
        if (stream!=null && mfccs!=null && mfccs.length>0)
        {
            for (int i=0; i<mfccs.length; i++)
                stream.writeDouble(mfccs[i]);
            
            stream.close();
        }
    }
    
    public static void writeRawMfccFile(double[][] mfccs, String mfccFileOut) throws IOException
    {
        MaryRandomAccessFile stream = new MaryRandomAccessFile(mfccFileOut, "rw");

        if (stream!=null)
        {
            writeMfccsFloat(stream, mfccs);
            stream.close();
        }
    }
    
    public static double[][] readMfccsFromFile(String mfccFile) throws IOException
    {
        MfccFileHeader params = new MfccFileHeader();
        MaryRandomAccessFile stream = params.readHeader(mfccFile, true);
        return readMfccs(stream, params);
    }
    
    public static double[][] readMfccs(MaryRandomAccessFile stream, MfccFileHeader params) throws IOException
    {
        double[][] mfccs = null;
        
        if (stream!=null && params.numfrm>0 && params.dimension>0)
        {
            mfccs = new double[params.numfrm][];
            
            for (int i=0; i<mfccs.length; i++)
                mfccs[i] = stream.readDouble(params.dimension);
            
            stream.close();
        }
        
        return mfccs;
    }
    
    public static double[][] readMfccsFromFloat(MaryRandomAccessFile stream, MfccFileHeader params) throws IOException
    {
        double[][] mfccs = null;
        
        if (stream!=null && params.numfrm>0 && params.dimension>0)
        {
            mfccs = new double[params.numfrm][params.dimension];
            
            int i, j;
            for (i=0; i<mfccs.length; i++)
            {
                for (j=0; j<mfccs[i].length; j++)
                    mfccs[i][j] = (double)(stream.readFloat());
            }
            
            stream.close();
        }
        
        return mfccs;
    }
    
    public static void readMfccsFromFloat(MaryRandomAccessFile stream, MfccFileHeader params, double[][] outputMfccs) throws IOException
    {
        if (stream!=null && params.numfrm>0 && params.dimension>0)
        {   
            int i, j;
            for (i=0; i<params.numfrm; i++)
            {
                for (j=0; j<params.dimension; j++)
                    outputMfccs[i][j] = (double)(stream.readFloat());
            }
            
            stream.close();
        }
    }
    
    public static void main(String[] args)
    {
        Mfccs l1 = new Mfccs();
        l1.params.dimension = 5;
        l1.params.numfrm = 1;
        l1.allocate();
        l1.mfccs[0][0] = 1.5;
        l1.mfccs[0][1] = 2.5;
        l1.mfccs[0][2] = 3.5;
        l1.mfccs[0][3] = 4.5;
        l1.mfccs[0][4] = 5.5;


        String mfccFile = "d:/1.lsf";
        l1.writeMfccFile(mfccFile);
        Lsfs l2 = new Lsfs(mfccFile);

        System.out.println("Test of class Lsfs completed...");
    }
}

