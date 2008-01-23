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

import de.dfki.lt.signalproc.util.MaryRandomAccessFile;

/**
 * @author oytun.turk
 *
 */
public class Lsfs {
    public double[][] lsfs;
    public LsfFileHeader params;
    
    public Lsfs()
    {
        this("");
    }
    
    public Lsfs(String lsfFile)
    {
        readLsfFile(lsfFile);
    }
    
    public void readLsfFile(String lsfFile)
    {
        lsfs = null;
        params = new LsfFileHeader();
        
        if (lsfFile!="")
        {
            MaryRandomAccessFile stream = null;
            try {
                stream = params.readLsfHeader(lsfFile, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (stream != null)
            {
                try {
                    lsfs = LineSpectralFrequencies.readLsfs(stream, params);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void writeLsfFile(String lsfFile)
    {   
        if (lsfFile!="")
        {
            MaryRandomAccessFile stream = null;
            try {
                stream = params.writeLsfHeader(lsfFile, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (stream != null)
            {
                try {
                    LineSpectralFrequencies.writeLsfs(stream, lsfs);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    public void allocate()
    {
        allocate(params.numfrm, params.lpOrder);
    }
    
    public void allocate(int numEntries, int lpOrder)
    {
        lsfs = null;
        params.numfrm = 0;
        params.lpOrder = 0;
        
        if (numEntries>0)
        {
            lsfs = new double[numEntries][];
            params.numfrm = numEntries;
            
            if (lpOrder>0)
            {
                params.lpOrder = lpOrder;
                
                for (int i=0; i<numEntries; i++)
                    lsfs[i] = new double[lpOrder];
            }
        }
    }
    
    public static void main(String[] args)
    {
        Lsfs l1 = new Lsfs();
        l1.params.lpOrder = 5;
        l1.params.numfrm = 1;
        l1.allocate();
        l1.lsfs[0][0] = 1.5;
        l1.lsfs[0][1] = 2.5;
        l1.lsfs[0][2] = 3.5;
        l1.lsfs[0][3] = 4.5;
        l1.lsfs[0][4] = 5.5;


        String lsfFile = "d:\\1.lsf";
        l1.writeLsfFile(lsfFile);
        Lsfs l2 = new Lsfs(lsfFile);

        System.out.println("Test of class Lsfs completed...");
    }
}
