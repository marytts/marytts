/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;


/**
 * Implements a structured header with file I/O functionality 
 * for binary energy contour files
 * 
 * @author Oytun T&uumlrk
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

