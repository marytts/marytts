/**
 * Copyright 2004-2006 DFKI GmbH.
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

/**
 * 
 * A wrapper class to store pitch marks as integer sample indices
 * 
 * @author Oytun T&uumlrk
 */
public class PitchMarks {

    public int[] pitchMarks;
    public float[] f0s;
    public int totalZerosToPadd;
    
    //count=total pitch marks
    public PitchMarks(int count, int[] pitchMarksIn, float[] f0sIn, int totalZerosToPaddIn)
    {
        if (count>1)
        {
            pitchMarks = new int[count];
            f0s = new float[count-1];
        
            System.arraycopy(pitchMarksIn, 0, pitchMarks, 0, Math.min(pitchMarksIn.length, count));
            System.arraycopy(f0sIn, 0, f0s, 0, Math.min(f0sIn.length, count-1));
        }
        else
        {
            pitchMarks = null;
            f0s = null;
        }
        
        totalZerosToPadd = Math.max(0, totalZerosToPaddIn);
    }
}

