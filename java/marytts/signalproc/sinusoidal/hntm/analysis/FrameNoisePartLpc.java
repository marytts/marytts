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
package marytts.signalproc.sinusoidal.hntm.analysis;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import marytts.util.math.ArrayUtils;
import marytts.util.math.ComplexNumber;

/**
 * LPC based noise modeling for a given speech frame
 * Full spectrum LP coefficients and LP gain are used
 * Synthesis handles noise generation for upper frequencies(i.e. frequencies larger than maximum voicing freq.)
 * 
 * @author oytun.turk
 *
 */
public class FrameNoisePartLpc implements FrameNoisePart {
    
    public float[] lpCoeffs;
    public float lpGain;
    public float origAverageSampleEnergy;
    public float origNoiseStd;
    
    public FrameNoisePartLpc()
    {
        super();
        
        lpCoeffs = null;
        lpGain = 0.0f;
        origAverageSampleEnergy = 0.0f;
        origNoiseStd = 1.0f;
    }
    
    public FrameNoisePartLpc(FrameNoisePartLpc existing)
    {
        super();
        
        origAverageSampleEnergy = existing.origAverageSampleEnergy;
        origNoiseStd = existing.origNoiseStd;
        
        setLpCoeffs(existing.lpCoeffs, existing.lpGain);
    }
    
    public FrameNoisePartLpc( DataInputStream dis ) throws IOException, EOFException
    {
        lpGain = dis.readFloat();
        origAverageSampleEnergy = dis.readFloat();
        origNoiseStd = dis.readFloat();
        
        int lpLen = dis.readInt();

        if (lpLen>0)
        {
            lpCoeffs = new float[lpLen];
            for (int i=0; i<lpLen; i++) 
                lpCoeffs[i] = dis.readFloat();
        }
        else
            lpCoeffs = null;
    }
    
    public boolean equals(FrameNoisePartLpc other)
    {
        if (lpGain!=other.lpGain) return false;
        if (origAverageSampleEnergy!=other.origAverageSampleEnergy) return false;
        if (origNoiseStd!=other.origNoiseStd) return false;
        
        if (lpCoeffs!=null || other.lpCoeffs!=null)
        {
            if (lpCoeffs!=null && other.lpCoeffs==null) return false;
            if (lpCoeffs==null && other.lpCoeffs!=null) return false;
            if (lpCoeffs.length!=other.lpCoeffs.length) return false;
            for (int i=0; i<lpCoeffs.length; i++)
                if (lpCoeffs[i]!=other.lpCoeffs[i]) return false;
        }
        
        return true;
    }
    
    public int getLength() 
    {
        int lpLen = 0;
        if (lpCoeffs!=null && lpCoeffs.length>0)
            lpLen = lpCoeffs.length;
        
        return 4*(3+lpLen);
    }
    
    public void write(DataOutput out) throws IOException 
    {
        out.writeFloat(lpGain);
        out.writeFloat(origAverageSampleEnergy);
        out.writeFloat(origNoiseStd);
        
        int numLpcs = 0;
        if (lpCoeffs!=null && lpCoeffs.length>0)
            numLpcs = lpCoeffs.length;

        out.writeInt(numLpcs);

        if (numLpcs>0)
        {
            for (int i=0; i<lpCoeffs.length; i++) 
                out.writeFloat(lpCoeffs[i]);
        }
    }
    
    public void setLpCoeffs(float[] lpCoeffsIn, float gainIn)
    {
        lpCoeffs = ArrayUtils.copy(lpCoeffsIn);
        lpGain = gainIn;
    }
    
    public void setLpCoeffs(double[] lpCoeffsIn, float gainIn)
    {
        lpCoeffs = ArrayUtils.copyDouble2Float(lpCoeffsIn);
        lpGain = gainIn;
    }
}

