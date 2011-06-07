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

import java.io.File;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.string.StringUtils;


/**
 * A collection of EST formatted labels with ascii text file input/output functionality
 * 
 * @author Oytun T&uumlrk
 */
public class Labels extends AlignmentData {
    public Label[] items;
    
    public Labels(int count)
    {
        items = null;
        
        if (count>0)
        {
            items = new Label[count];
            for (int i=0; i<count; i++)
                items[i] = new Label();
        }
    }
    
    //Create ESTLabels from existing ones
    public Labels(Labels e)
    {
        initFromLabels(e);
    }
    
  //Create ESTLabels from existing ones
    public Labels(Labels e, int startPos)
    {
        initFromLabels(e, startPos);
    }
    
    //Create ESTLabels using labels between [startPos,endPos] 
    public Labels(Labels e, int startPos, int endPos)
    {
        initFromLabels(e, startPos, endPos);
    }
    
    public Labels(String labelFile)
    {
        initFromFile(labelFile, false);
    }
    
    public Labels(String labelFile, boolean isRealisedDurationsFile)
    {
        initFromFile(labelFile, isRealisedDurationsFile);
    }
    
    public void initFromLabels(Labels e)
    {
        initFromLabels(e, 0);
    }
    
    public void initFromLabels(Labels e, int startPos)
    {
        initFromLabels(e, startPos, (e==null) ? 0 : e.items.length-1);
    }
    
    public void initFromLabels(Labels e, int startPos, int endPos)
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
            
            items = new Label[endPos-startPos+1];
            for (int i=startPos; i<=endPos; i++)
                items[i-startPos] = new Label(e.items[i]);
        }
    }
    
    public void initFromFile(String file, boolean isRealisedDurationsFile)
    {
        if (!isRealisedDurationsFile)
            initFromLabels(readESTLabelFile(file));
        else
            initFromLabels(readRealisedDurationsFile(file));
    }
    
    public static Labels readESTLabelFile(String labelFile)
    {
        Labels labelsRet = null;
        String allText = null;
        
        if (FileUtils.exists(labelFile))
        {
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
        }
        
        return labelsRet;
    }
    
    //Realised durations file contain duration of each label instead of its end time
    public static Labels readRealisedDurationsFile(String labelFile)
    {
        Labels labelsRet = readESTLabelFile(labelFile);
        
        /*
        //Conversion only required if label file contains phone durations instead of end times
        for (int i=1; i<labelsRet.items.length; i++)
            labelsRet.items[i].time += labelsRet.items[i-1].time;
            */
        
        return labelsRet;
    }
    
    public static Labels parseFromLines(String[] lines, int startLine, int endLine)
    {
        return parseFromLines(lines, startLine, endLine, 3);
    }
    
    public static Labels parseFromLines(String[] lines, int startLine, int endLine, int minimumItemsInOneLine)
    {
        Labels labels = null;
        Labels labelsRet = null;

        if (startLine<=endLine)
        {
            int i;
            int count = 0;
            for (i=startLine; i<=endLine; i++)
            {
                String[] labelInfos = lines[i].split("\\s+");
                if (labelInfos.length>=minimumItemsInOneLine)
                    count++;
            }

            int tmpCount = 0;
            if (count>0)
            {
                labels = new Labels(count);
                for (i=startLine; i<=endLine; i++)
                {
                    if (tmpCount>count-1)
                        break;

                    String[] labelInfos = lines[i].trim().split("\\s+");
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

                labelsRet = new Labels(labels, 0, tmpCount-1);
            }
        }
        
        return labelsRet;
    }
    
    public void print()
    {
        for (int i=0; i<items.length; i++)
            items[i].print();
    }


    /**
     * For the given time, return the index of the label at that time, if any.
     * @param time time in seconds
     * @return the index of the label at that time, or -1 if there isn't any
     */
    public int getLabelIndexAtTime(double time) {
        if (items == null) {
            return -1;
        }
        // We return the first label whose end time is >= time:
        for (int i=0; i<items.length; i++) {
            if (items[i].time >= time) {
                return i;
            }
        }
        return -1;
    }
    
    public static void main(String[] args)
    {
        Labels lab = null;
        
        lab = new Labels("d:/m0001_poppyPhoneLab.lab");
        lab.print();
        
        lab = new Labels("d:/m0001.lab");
        lab.print();
    }
}

