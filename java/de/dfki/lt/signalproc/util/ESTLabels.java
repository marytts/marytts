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

package de.dfki.lt.signalproc.util;

import java.io.File;
import java.io.IOException;

import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtils;

/**
 * @author oytun.turk
 *
 */
public class ESTLabels {
    public ESTLabel[] items;
    
    public ESTLabels(int count)
    {
        items = null;
        
        if (count>0)
        {
            items = new ESTLabel[count];
            for (int i=0; i<count; i++)
                items[i] = new ESTLabel();
        }
    }
    
    //Create ESTLabels from existing ones
    public ESTLabels(ESTLabels e)
    {
        this(e, 0);
    }
    
  //Create ESTLabels from existing ones
    public ESTLabels(ESTLabels e, int startPos)
    {
        this(e, startPos, e.items.length-1);
    }
    
    //Create ESTLabels using labels between [startPos,endPos] 
    public ESTLabels(ESTLabels e, int startPos, int endPos)
    {
        items = null;
        if (e!=null && e.items!=null)
        {
            if (startPos<0)
                startPos = 0;
            if (startPos>e.items.length-1)
                startPos=e.items.length-1;
            if (endPos<startPos)
                endPos=startPos;
            if (endPos>e.items.length-1)
                endPos=e.items.length-1;
            
            items = new ESTLabel[endPos-startPos+1];
            for (int i=startPos; i<=endPos; i++)
                items[i-startPos] = new ESTLabel(e.items[i]);
        }
    }
    
    public ESTLabels(String labelFile)
    {
        this(readESTLabelFile(labelFile));
    }
    
    public static ESTLabels readESTLabelFile(String labelFile)
    {
        ESTLabels labelsRet = null;
        String allText = null;
        try {
            allText = FileUtils.getFileAsString(new File(labelFile), "ASCII");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (allText!=null)
        {
            String[] lines = allText.split("\n");

            labelsRet = parseFromLines(lines, 0, lines.length-1);
        }
        
        return labelsRet;
    }
    
    public static ESTLabels parseFromLines(String[] lines, int startLine, int endLine)
    {
        return parseFromLines(lines, startLine, endLine, 3);
    }
    
    public static ESTLabels parseFromLines(String[] lines, int startLine, int endLine, int minimumItemsInOneLine)
    {
        ESTLabels labels = null;
        ESTLabels labelsRet = null;

        if (startLine<=endLine)
        {
            int i;
            int count = 0;
            for (i=startLine; i<=endLine; i++)
            {
                String[] labelInfos = lines[i].split(" ");
                if (labelInfos.length>=minimumItemsInOneLine)
                    count++;
            }

            int tmpCount = 0;
            if (count>0)
            {
                labels = new ESTLabels(count);
                for (i=startLine; i<=endLine; i++)
                {
                    if (tmpCount>count-1)
                        break;

                    String[] labelInfos = lines[i].split(" ");
                    if (labelInfos.length>=minimumItemsInOneLine && 
                            StringUtils.isNumeric(labelInfos[0]) &&
                            StringUtils.isNumeric(labelInfos[1]))
                    {
                        if (labelInfos.length>0)
                            labels.items[tmpCount].time = (float)Float.valueOf(labelInfos[0]);
                        
                        if (labelInfos.length>1)
                            labels.items[tmpCount].status = (int)Integer.valueOf(labelInfos[1]);
                        
                        if (labelInfos.length>2)
                            labels.items[tmpCount].phn = labelInfos[2].trim();

                        int restStartMin = 4;
                        if (labelInfos.length>3 && StringUtils.isNumeric(labelInfos[3]))
                            labels.items[tmpCount].ll = (float)Float.valueOf(labelInfos[3]);
                        else
                        {
                            restStartMin = 3;
                            labels.items[tmpCount].ll = (float)Float.NEGATIVE_INFINITY; 
                        }
                        
                        //Read additional fields if any in String format
                        //also convert these to double values if they are numeric
                        if (labelInfos.length>restStartMin)
                        {
                            int numericCount = 0;
                            labels.items[tmpCount].rest = new String[labelInfos.length-restStartMin];
                            labels.items[tmpCount].valuesRest = new double[labelInfos.length-restStartMin];
                            for (int j=0; j<labels.items[tmpCount].rest.length; j++)
                            {
                                labels.items[tmpCount].rest[j] = labelInfos[j+restStartMin];
                                if (StringUtils.isNumeric(labels.items[tmpCount].rest[j]))
                                    labels.items[tmpCount].valuesRest[j] = Double.valueOf(labels.items[tmpCount].rest[j]);
                                else
                                    labels.items[tmpCount].valuesRest[j] = Double.NEGATIVE_INFINITY;
                            }
                        }

                        tmpCount++;
                    }
                }

                labelsRet = new ESTLabels(labels, 0, tmpCount-1);
            }
        }
        
        return labelsRet;
    }
    
    public void print()
    {
        for (int i=0; i<items.length; i++)
            items[i].print();
    }
    
    public static void main(String[] args)
    {
        ESTLabels lab = null;
        
        lab = new ESTLabels("d:\\m0001_poppyPhoneLab.lab");
        lab.print();
        
        lab = new ESTLabels("d:\\m0001.lab");
        lab.print();
    }
}
