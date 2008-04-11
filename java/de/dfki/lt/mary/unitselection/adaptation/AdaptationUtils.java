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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.LsfFileHeader;
import de.dfki.lt.signalproc.util.ESTLabels;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class AdaptationUtils {    
    
    public static int ALL_AVAILABLE_TRAINING_FRAMES = -1;

    //An optimal alignment is found by dynamic programming if the labels are not identical
    public static IndexMap lsfFramesMapping(String sourceLabelFile, String targetLabelFile, 
                                            String sourceLsfFile, String targetLsfFile)
    {
        IndexMap im = null;

        //Read label files
        ESTLabels sourceLabels = new ESTLabels(sourceLabelFile);
        ESTLabels targetLabels = new ESTLabels(targetLabelFile);
        //
        
        //Read LSF file headers
        LsfFileHeader hdr1 = new LsfFileHeader(sourceLsfFile);
        LsfFileHeader hdr2 = new LsfFileHeader(targetLsfFile);
        //

        if (sourceLabels.items!=null && targetLabels.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[] labelMap = StringUtil.alignLabels(sourceLabels.items, targetLabels.items);
            //

            if (labelMap!=null)
            {
                int j, srcLabInd, tgtLabInd, tgtFrmInd;
                double time1, time2;
                double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

                srcLabInd = 0;

                //Find the corresponding target frame index for each source frame index
                int count = 0;
                im = new IndexMap(1);
                im.files[0] = new FileMap(hdr1.numfrm,2);

                for (j=0; j<hdr1.numfrm; j++)
                {
                    time1 = SignalProcUtils.frameIndex2Time(j, hdr1.winsize, hdr1.skipsize);

                    while (time1>sourceLabels.items[srcLabInd].time)
                    {
                        srcLabInd++;
                        if (srcLabInd>sourceLabels.items.length-1)
                        {
                            srcLabInd = sourceLabels.items.length-1;
                            break;
                        }
                    }

                    tgtLabInd = labelMap[srcLabInd];

                    if (tgtLabInd>=0 && sourceLabels.items[srcLabInd].phn.compareTo(targetLabels.items[tgtLabInd].phn)==0)
                    {
                        if (srcLabInd>0)   
                            srcStartTime = sourceLabels.items[srcLabInd-1].time;
                        else
                            srcStartTime = 0.0;

                        if (tgtLabInd>0) 
                            tgtStartTime = targetLabels.items[tgtLabInd-1].time;
                        else
                            tgtStartTime = 0.0;

                        srcEndTime = sourceLabels.items[srcLabInd].time;
                        tgtEndTime = targetLabels.items[tgtLabInd].time;

                        time2 = MathUtils.linearMap(time1, srcStartTime, srcEndTime, tgtStartTime, tgtEndTime);

                        tgtFrmInd = SignalProcUtils.time2frameIndex(time2, hdr1.winsize, hdr1.skipsize);

                        im.files[0].indicesMap[count][0] = j;
                        im.files[0].indicesMap[count][1] = tgtFrmInd; 
                        count++;

                        if (count>hdr1.numfrm-1)
                            break;
                    } 
                }
            }
        }

        return im;  
    }

    //Each frame is mapped as a group of frames, i.e. with frames on the left and right context
    public static IndexMap lsfFrameGroupsMapping(String sourceLabelFile, String targetLabelFile, 
                                                 String sourceLsfFile, String targetLsfFile,
                                                 int numNeighbours)
    {
        IndexMap im = null;

        //Read label files
        ESTLabels sourceLabels = new ESTLabels(sourceLabelFile);
        ESTLabels targetLabels = new ESTLabels(targetLabelFile);
        //
        
        //Read LSF file headers
        LsfFileHeader hdr1 = new LsfFileHeader(sourceLsfFile);
        LsfFileHeader hdr2 = new LsfFileHeader(targetLsfFile);
        //

        if (sourceLabels.items!=null && targetLabels.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[] labelMap = StringUtil.alignLabels(sourceLabels.items, targetLabels.items);
            //

            if (labelMap!=null)
            {
                int j, srcLabInd, tgtLabInd, tgtFrmInd;
                double time1, time2;
                double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

                srcLabInd = 0;

                //Find the corresponding target frame index for each source frame index
                int count = 0;
                im = new IndexMap(1);
                im.files[0] = new FileMap(hdr1.numfrm,4);

                for (j=0; j<hdr1.numfrm; j++)
                {
                    time1 = SignalProcUtils.frameIndex2Time(j, hdr1.winsize, hdr1.skipsize);

                    while (time1>sourceLabels.items[srcLabInd].time)
                    {
                        srcLabInd++;
                        if (srcLabInd>sourceLabels.items.length-1)
                        {
                            srcLabInd = sourceLabels.items.length-1;
                            break;
                        }
                    }

                    tgtLabInd = labelMap[srcLabInd];

                    if (tgtLabInd>=0 && sourceLabels.items[srcLabInd].phn.compareTo(targetLabels.items[tgtLabInd].phn)==0)
                    {
                        if (srcLabInd>0)   
                            srcStartTime = sourceLabels.items[srcLabInd-1].time;
                        else
                            srcStartTime = 0.0;

                        if (tgtLabInd>0) 
                            tgtStartTime = targetLabels.items[tgtLabInd-1].time;
                        else
                            tgtStartTime = 0.0;

                        srcEndTime = sourceLabels.items[srcLabInd].time;
                        tgtEndTime = targetLabels.items[tgtLabInd].time;

                        time2 = MathUtils.linearMap(time1, srcStartTime, srcEndTime, tgtStartTime, tgtEndTime);

                        tgtFrmInd = SignalProcUtils.time2frameIndex(time2, hdr1.winsize, hdr1.skipsize);

                        im.files[0].indicesMap[count][0] = Math.max(0, j-numNeighbours);
                        im.files[0].indicesMap[count][1] = Math.min(j+numNeighbours, hdr1.numfrm-1);
                        im.files[0].indicesMap[count][2] = Math.max(0, tgtFrmInd-numNeighbours); 
                        im.files[0].indicesMap[count][3] = Math.min(tgtFrmInd+numNeighbours, hdr2.numfrm-1); 
                        count++;

                        if (count>hdr1.numfrm-1)
                            break;
                    } 
                }
            }
        }

        return im; 
    }

    public static IndexMap lsfLabelsMapping(String sourceLabelFile, String targetLabelFile, 
                                            String sourceLsfFile, String targetLsfFile)
    {
        IndexMap im = null;

        //Read label files
        ESTLabels sourceLabels = new ESTLabels(sourceLabelFile);
        ESTLabels targetLabels = new ESTLabels(targetLabelFile);
        //
        
        //Read LSF file headers
        LsfFileHeader hdr1 = new LsfFileHeader(sourceLsfFile);
        LsfFileHeader hdr2 = new LsfFileHeader(targetLsfFile);
        //

        if (sourceLabels.items!=null && targetLabels.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[] labelMap = StringUtil.alignLabels(sourceLabels.items, targetLabels.items);
            //

            if (labelMap!=null)
            {
                int j, tgtLabInd;
                double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

                //Find the corresponding target frame index for each source frame index
                int count = 0;
                im = new IndexMap(1);
                im.files[0] = new FileMap(sourceLabels.items.length,4);

                for (j=0; j<sourceLabels.items.length; j++)
                {
                    if (j>0)
                        srcStartTime = sourceLabels.items[j-1].time;
                    else
                        srcStartTime = 0.0;

                    tgtLabInd = labelMap[j];

                    if (tgtLabInd>=0 && sourceLabels.items[j].phn.compareTo(targetLabels.items[tgtLabInd].phn)==0)
                    {
                        if (tgtLabInd>0) 
                            tgtStartTime = targetLabels.items[tgtLabInd-1].time;
                        else
                            tgtStartTime = 0.0;

                        srcEndTime = sourceLabels.items[j].time;
                        tgtEndTime = targetLabels.items[tgtLabInd].time;

                        im.files[0].indicesMap[count][0] = SignalProcUtils.time2frameIndex(srcStartTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][1] = SignalProcUtils.time2frameIndex(srcEndTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][2] = SignalProcUtils.time2frameIndex(tgtStartTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][3] = SignalProcUtils.time2frameIndex(tgtEndTime, hdr1.winsize, hdr1.skipsize);
                        
                        count++;

                        if (count>sourceLabels.items.length-1)
                            break;
                    } 
                }
            }
        }

        return im; 
    }

    public static IndexMap lsfLabelGroupsMapping(String sourceLabelFile, String targetLabelFile, 
                                                 String sourceLsfFile, String targetLsfFile,
                                                 int numNeighbours)
    {
        IndexMap im = null;

        //Read label files
        ESTLabels sourceLabels = new ESTLabels(sourceLabelFile);
        ESTLabels targetLabels = new ESTLabels(targetLabelFile);
        //
        
        //Read LSF file headers
        LsfFileHeader hdr1 = new LsfFileHeader(sourceLsfFile);
        LsfFileHeader hdr2 = new LsfFileHeader(targetLsfFile);
        //

        if (sourceLabels.items!=null && targetLabels.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[] labelMap = StringUtil.alignLabels(sourceLabels.items, targetLabels.items);
            //

            if (labelMap!=null)
            {
                int j, tgtLabInd;
                double srcStartTime, srcEndTime, tgtStartTime, tgtEndTime;

                //Find the corresponding target frame index for each source frame index
                int count = 0;
                im = new IndexMap(1);
                im.files[0] = new FileMap(sourceLabels.items.length,4);

                for (j=0; j<sourceLabels.items.length; j++)
                {
                    if (j-numNeighbours-1>=0)
                        srcStartTime = sourceLabels.items[j-numNeighbours-1].time;
                    else
                        srcStartTime = 0.0;

                    tgtLabInd = labelMap[j];

                    if (tgtLabInd>=0 && sourceLabels.items[j].phn.compareTo(targetLabels.items[tgtLabInd].phn)==0)
                    {
                        if (tgtLabInd-numNeighbours-1>=0) 
                            tgtStartTime = targetLabels.items[tgtLabInd-numNeighbours-1].time;
                        else
                            tgtStartTime = 0.0;

                        srcEndTime = sourceLabels.items[Math.min(j+numNeighbours, sourceLabels.items.length-1)].time;
                        tgtEndTime = targetLabels.items[Math.min(tgtLabInd+numNeighbours, targetLabels.items.length-1)].time;

                        im.files[0].indicesMap[count][0] = SignalProcUtils.time2frameIndex(srcStartTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][1] = SignalProcUtils.time2frameIndex(srcEndTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][2] = SignalProcUtils.time2frameIndex(tgtStartTime, hdr1.winsize, hdr1.skipsize);
                        im.files[0].indicesMap[count][3] = SignalProcUtils.time2frameIndex(tgtEndTime, hdr1.winsize, hdr1.skipsize);
                        
                        count++;

                        if (count>sourceLabels.items.length-1)
                            break;
                    } 
                }
            }
        }

        return im;  
    }
    
    public static IndexMap lsfSpeechMapping()
    {
        IndexMap im = new IndexMap(1);
        im.files[0] = new FileMap(1,1);
        
        im.files[0].indicesMap[0][0] = ALL_AVAILABLE_TRAINING_FRAMES;
        
        return im;   
    }
}
