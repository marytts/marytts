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

package marytts.signalproc.adaptation;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;


/**
 * @author oytun.turk
 *
 * A class for handling source-target frame indices mapping
 * It can be used in various ways:
 * 
 */
public class IndexMap {
    public FileMap [] files; //A frame map for individual file pairs
    
    public IndexMap()
    {
        files = null;   
    }

    public IndexMap(int numItems)
    {
        allocate(numItems);
    }
    
    public IndexMap(IndexMap im)
    {
        copyFrom(im);
    }
    
    public void allocate(int numItems)
    {
        if (numItems>0)
            files = new FileMap[numItems];
        else
            files = null;
    }
    
    public void copyFrom(IndexMap im)
    {
        if (im.files.length>0)
        {
            files = new FileMap[im.files.length];
            for (int i=0; i<im.files.length; i++)
                files[i] = new FileMap(im.files[i]);
        }
        else
            files = null;
    }
    
    //Write the object into a binary file
    public void writeToFile(String binaryFileName) throws IOException
    {
        LEDataOutputStream out = null;
        
        try {
            out = new LEDataOutputStream(binaryFileName);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (out!=null)
        {
            int i, j;
            
            out.writeInt(files.length);
            for (i=0; i<files.length; i++)
            {
                out.writeInt(files[i].indicesMap.length);
                for (j=0; j<files[i].indicesMap.length; j++)
                {
                    out.writeInt(files[i].indicesMap[j].length);
                    out.writeInt(files[i].indicesMap[j]);        
                }
            }
            
            out.close();
        }
    }
    
    //Read the object from a binary file
    public void readFromFile(String binaryFileName) throws IOException
    {
        LEDataInputStream in = null;
        
        try {
            in = new LEDataInputStream(binaryFileName);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (in!=null)
        {
            int i, j;
            
            int numItems = in.readInt();
            allocate(numItems);
            int tmpNumGroups;
            int tmpNumItems;
            int [] tmpInts;
            
            for (i=0; i<numItems; i++)
            {
                tmpNumGroups = in.readInt();
                
                files[i] = new FileMap(tmpNumGroups,0);
                
                for (j=0; j<tmpNumGroups; j++)
                {
                    tmpNumItems = in.readInt();
                    if (tmpNumItems>0)
                    {
                        files[i].indicesMap[j] = new int[tmpNumItems];
                        tmpInts = in.readInt(tmpNumItems);  
                        System.arraycopy(tmpInts, 0, files[i].indicesMap[j], 0, files[i].indicesMap[j].length);
                    }
                }
            }
            
            in.close();
        }
        
    }
}
