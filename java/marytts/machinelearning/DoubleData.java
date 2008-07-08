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

package marytts.machinelearning;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 */
public class DoubleData {
    public double[][] vectors;
    public int numVectors;
    public int dimension;
    
    public DoubleData()
    {
        vectors = null;
        allocate(0, 0);
    }

    public DoubleData(int numVectorsIn, int dimensionIn)
    {
        vectors = null;
        allocate(numVectorsIn, dimensionIn);
    }

    public DoubleData(String dataFile)
    {
        vectors = null;
        read(dataFile);
    }
    
    public DoubleData(double[][] x)
    {
        setVectors(x);
    }
    
    public void setVectors(double[][] x)
    {
        if (x!=null)
        {
            int i;
            int dimensionIn = x[0].length;
            for (i=1; i<x.length; i++)
                assert x[i].length == dimensionIn;
            
            allocate(x.length, dimensionIn);
            
            for (i=0; i<numVectors; i++)
                System.arraycopy(x[i], 0, vectors[i], 0, dimension);
        }
        else
            allocate(0, 0);
    }

    public void allocate(int numVectorsIn, int dimensionIn)
    {
        if (numVectorsIn>0)
        {
            if (numVectors!=numVectorsIn)
            {
                numVectors = numVectorsIn;
                vectors = new double[numVectors][];
            }
            
            if (dimensionIn>0)
            {
                if (dimension!=dimensionIn)
                {
                    dimension = dimensionIn;
                    for (int i=0; i<numVectors; i++)
                        vectors[i] = new double[dimension];
                }
                    
                dimension = dimensionIn;
            }
            else
                dimension = 0;
        }
        else
        {
            vectors = null;
            numVectors = 0;
            dimension = 0;
        }
    }

    public void write(String dataFile)
    {
        MaryRandomAccessFile fp = null;
        try {
            fp = new MaryRandomAccessFile(dataFile, "rw");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (fp!=null)
        {
            try {
                fp.writeIntEndian(numVectors);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                fp.writeIntEndian(dimension);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (numVectors>0 && dimension>0)
            {
                for (int i=0; i<numVectors; i++)
                {
                    try {
                        fp.writeDoubleEndian(vectors[i]);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            try {
                fp.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void read(String dataFile)
    {
        MaryRandomAccessFile fp = null;
        try {
            fp = new MaryRandomAccessFile(dataFile, "r");
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        if (fp!=null)
        {
            int numVectorsIn = 0;
            int dimensionIn = 0;
            
            try {
                numVectorsIn = fp.readIntEndian();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                dimensionIn = fp.readIntEndian();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (numVectorsIn>0)
            {
                if (numVectors!=numVectorsIn)
                    vectors = new double[numVectorsIn][];

                numVectors = numVectorsIn;
                dimension = dimensionIn;

                if (dimension>0)
                {
                    for (int i=0; i<numVectors; i++)
                    {
                        try {
                            vectors[i] = fp.readDoubleEndian(dimension);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                else
                {
                    for (int i=0; i<numVectors; i++)
                        vectors[i] = null;
                    
                    dimension = 0;
                }
            }
            else
            {
                vectors = null;
                numVectors = 0;
                dimension = 0;
            }

            try {
                fp.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
            allocate(0, 0);
    }
}
