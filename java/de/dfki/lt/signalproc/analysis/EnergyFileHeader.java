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
public class EnergyFileHeader {
    public int samplingRate;
    public double windowSizeInSeconds;
    public double skipSizeInSeconds;
    public int totalFrames;
    
    public static final double DEFAULT_WINDOW_SIZE = 0.020;
    public static final double DEFAULT_SKIP_SIZE = 0.010;
    
    public EnergyFileHeader(EnergyFileHeader existing)
    {
        samplingRate = existing.samplingRate;
        windowSizeInSeconds = existing.windowSizeInSeconds;
        skipSizeInSeconds = existing.skipSizeInSeconds;
        totalFrames = existing.totalFrames;
    }
    
    public EnergyFileHeader()
    {
        this(0);
    }
    
    public EnergyFileHeader(int samplingRateIn)
    {
        this(samplingRateIn, DEFAULT_WINDOW_SIZE);
    }
    
    public EnergyFileHeader(int samplingRateIn, double windowSizeInSecondsIn)
    {
        this(samplingRateIn, windowSizeInSecondsIn, DEFAULT_SKIP_SIZE);
    }
    
    public EnergyFileHeader(int samplingRateIn, double windowSizeInSecondsIn, double skipSizeInSecondsIn)
    {
        this(samplingRateIn, windowSizeInSecondsIn, skipSizeInSecondsIn, 0);
    }
    
    public EnergyFileHeader(int samplingRateIn, double windowSizeInSecondsIn, double skipSizeInSecondsIn, int totalFramesIn)
    {
        samplingRate = samplingRateIn;
        windowSizeInSeconds = windowSizeInSecondsIn;
        skipSizeInSeconds = skipSizeInSecondsIn;
        totalFrames = totalFramesIn;
    }
    
    public void write(MaryRandomAccessFile ler)
    {
        try {
            ler.writeInt(samplingRate);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(windowSizeInSeconds);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeDouble(skipSizeInSeconds);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            ler.writeInt(totalFrames);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void read(MaryRandomAccessFile ler)
    {
        read(ler, false);
    }
    
    public void read(MaryRandomAccessFile ler, boolean bLeaveOpen)
    {
        try {
            samplingRate = ler.readInt();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            windowSizeInSeconds = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            skipSizeInSeconds = ler.readDouble();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            totalFrames = ler.readInt();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (!bLeaveOpen)
        {
            try {
                ler.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
