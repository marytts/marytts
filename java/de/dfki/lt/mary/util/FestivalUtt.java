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

package de.dfki.lt.mary.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import de.dfki.lt.signalproc.util.ESTLabels;

/**
 * @author oytun.turk
 *
 */
public class FestivalUtt {
    public ESTLabels[] labels;
    public String[] keys;
    
    public FestivalUtt()
    {
        this("");
    }
    
    public FestivalUtt(String festivalUttFile)
    {
        keys = new String[6];
        keys[0] = "==Segment=="; 
        keys[1] = "==Target==";
        keys[2] = "==Syllable=="; 
        keys[3] = "==Word==";
        keys[4] = "==IntEvent==";
        keys[5] = "==Phrase==";
        
        labels = new ESTLabels[keys.length];
        
        if (FileUtils.exists(festivalUttFile))
        {
            read(festivalUttFile);
        }
    }
    
    public void read(String festivalUttFile)
    {
        String allText = null;
        try {
            allText = FileUtils.getFileAsString(new File(festivalUttFile), "ASCII");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (allText!=null)
        {
            String[] lines = allText.split("\n");

            int i, j;
            int[] boundInds = new int[keys.length];
            Arrays.fill(boundInds, -1);
            int boundCount = 0;
            for (i=0; i<keys.length; i++)
            {
                for (j=0; j<lines.length; j++)
                {
                    if (lines[j].compareTo(keys[i])==0)
                    {
                        boundInds[i]=j;
                        break;
                    }
                }
            }
            
            for (i=0; i<keys.length; i++)
            {
                if (boundInds[i]>-1)
                {
                    if (i<keys.length-1)
                        labels[i] = ESTLabels.parseFromLines(lines, boundInds[i]+1, boundInds[i+1]-1, 2);
                    else
                        labels[i] = ESTLabels.parseFromLines(lines, boundInds[i]+1, lines.length-1, 2);
                    
                    //Shift all valuesRest by one, and put the f0 into valuesRest[0]
                    if (keys[i].compareTo("==Target==")==0)
                    {
                        for (j=0; j<labels[i].items.length; j++)
                        {
                            int numTotalValuesRest = 0;
                            if (labels[i].items[j].valuesRest!=null)
                            {
                                double[] tmpValues = new double[labels[i].items[j].valuesRest.length];
                                System.arraycopy(labels[i].items[j].valuesRest, 0, tmpValues, 0, labels[i].items[j].valuesRest.length);
                                labels[i].items[j].valuesRest = new double[tmpValues.length+1];
                                labels[i].items[j].valuesRest[0] = Double.valueOf(labels[i].items[j].phn); 
                                System.arraycopy(labels[i].items[j].valuesRest, 0, tmpValues, 1, labels[i].items[j].valuesRest.length);
                            }
                            else
                            {
                                labels[i].items[j].valuesRest = new double[1];
                                labels[i].items[j].valuesRest[0] = Double.valueOf(labels[i].items[j].phn); 
                            }
                        }
                    }
                    //
                }
                else
                    labels[i] = null;
            }
        }
    }
    
    public static FestivalUtt readFestivalUttFile(String festivalUttFile)
    {
        FestivalUtt f = new FestivalUtt(festivalUttFile);
        
        return f;
    }
    
    public static void main(String[] args)
    {
        FestivalUtt f = new FestivalUtt("d:\\a.utt");
        
        System.out.println("Test completed...");
    }
}
