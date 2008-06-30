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

package marytts.signalproc.adaptation.smoothing;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.FileUtils;
import marytts.util.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 */
public class SmoothingFile {
    public static final int NOT_OPENED = 0;
    public static final int OPEN_FOR_WRITE = 1;
    public static final int OPEN_FOR_READ = 2;
    private MaryRandomAccessFile stream;
    public int status;
    private int totalEntries;
    public int smoothingMethod;
    
    public SmoothingFile(String filename, int desiredStatus)
    {
        this(filename, desiredStatus, SmoothingDefinitions.NO_SMOOTHING);
    }
    
    public SmoothingFile(String filename, int desiredStatus, int smoothingMethodIn) //smoothingMethod only effective when writing to file
    {
        status = NOT_OPENED;
        stream = null;
        totalEntries = 0;
        smoothingMethod = smoothingMethodIn;
        
        if (desiredStatus==OPEN_FOR_READ)
        {
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(filename, "r");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        else if (desiredStatus==OPEN_FOR_WRITE)
        {
            FileUtils.delete(filename);
            
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(filename, "rw");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void writeHeader()
    {
        if (status==OPEN_FOR_WRITE)
        {
            try {
                stream.seek(0);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                stream.writeInt(totalEntries);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                stream.writeInt(smoothingMethod);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void readHeader()
    {
        if (status==OPEN_FOR_READ)
        {
            try {
                stream.seek(0);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                totalEntries = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            try {
                smoothingMethod = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void writeSingle(double[] x)
    {
        writeSingle(x, x.length);
    }
    
    public void writeSingle(double[] x, int len)
    {
        try {
            stream.writeInt(Math.min(x.length, len));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            stream.writeDouble(x, 0, Math.min(x.length, len));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        incrementTotalEntries();
    }
    
    public void writeAll(double[][] x)
    {
        totalEntries = 0;
        writeHeader();
        
        for (int i=0; i<x.length; i++)
            writeSingle(x[i]);
        
        close();
    }
    
    public void incrementTotalEntries()
    {
        if (status==OPEN_FOR_WRITE)
        {
            try {
                long currentPos = stream.getFilePointer();
                stream.seek(0);
                totalEntries = stream.readInt();
                totalEntries++;
                //System.out.println(String.valueOf(totalEntries));
                stream.seek(0);
                stream.writeInt(totalEntries);
                stream.seek(currentPos);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public double[] readSingle()
    {
        double[] x = null;
        
        if (status==OPEN_FOR_READ)
        {
            int len = 0;
            try {
                len = stream.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            if (len>0)
            {
                try {
                    x = stream.readDouble(len);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        return x;
    }
    
    public double[][] readAll()
    {
        double[][] x = null;
        if (status==OPEN_FOR_READ)
        {
            readHeader();
            
            if (totalEntries>0)
            {
                x = new double[totalEntries][];
                for (int i=0; i<totalEntries; i++)
                    x[i] = readSingle();
            }
            
            close();
        }
        
        return x;
    }
    
    public void close()
    {
        if (status!=NOT_OPENED)
        {
            if (stream!=null)
            {       
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            stream = null;
            status = NOT_OPENED;
        }
    }
}
