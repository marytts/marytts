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

package de.dfki.lt.mary.unitselection.adaptation;

/**
 * @author oytun.turk
 * 
 * A class for handling source-target frame indices mapping for a single pair
 *
 */
public class FileMap {
    public int [][] indicesMap;
    
    public FileMap()
    {
        allocate(0,0);
    }
    
    public FileMap(int numGroups)
    {
        allocate(numGroups,0);
    }
    
    public FileMap(int numGroups, int numItems)
    {
        allocate(numGroups,numItems);
    }
    
    public FileMap(FileMap fm)
    {
        if (fm.indicesMap!=null)
        {
            indicesMap = new int[fm.indicesMap.length][];
            for (int i=0; i<fm.indicesMap.length; i++)
            {
                if (fm.indicesMap[i]!=null)
                {
                    indicesMap[i] = new int[fm.indicesMap[i].length];
                    System.arraycopy(fm.indicesMap[i], 0, indicesMap[i], 0, fm.indicesMap[i].length);
                }
                else
                    indicesMap[i] = null;
            }
        }
        else
            indicesMap = null;
    }
    
    public void allocate(int numGroups, int numItems)
    {
        if (numGroups>0)
        {    
            if (numItems>0)
                indicesMap = new int[numGroups][numItems];
            else
                indicesMap = new int[numGroups][];
        }
        else
            indicesMap = null;
    }
}
