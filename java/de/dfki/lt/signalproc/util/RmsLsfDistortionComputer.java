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

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.FileMap;
import de.dfki.lt.mary.unitselection.adaptation.IndexMap;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.analysis.LineSpectralFrequencies;
import de.dfki.lt.signalproc.analysis.Lsfs;

/**
 * @author oytun.turk
 *
 */
public class RmsLsfDistortionComputer extends BaselineDistortionComputer {
    public RmsLsfDistortionComputer()
    {
        super();
    }
    
    public double[] getDistances(String folder1, String folder2)
    {
        folder1 = StringUtil.checkLastSlash(folder1);
        folder2 = StringUtil.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return getDistances(set1, set2, false);
    }
    
    public double[] getDistances(String folder1, String folder2, boolean isBark)
    {
        folder1 = StringUtil.checkLastSlash(folder1);
        folder2 = StringUtil.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return getDistances(set1, set2, isBark);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark)
    {
        int[] map = new int[Math.min(set1.items.length, set2.items.length)];
        for (int i=0; i<map.length; i++)
            map[i] = i;
        
        return getDistances(set1, set2, isBark, map);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark, int[] map)
    {
        double[] distances = null;
        double[] tmpDistances = null;
        
        for (int i=0; i<map.length; i++)
        {
            double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], isBark);
            if (distances!=null && itemDistances!=null)
            {
                tmpDistances = new double[distances.length];
                System.arraycopy(distances, 0, tmpDistances, 0, distances.length);
                distances = new double[tmpDistances.length + itemDistances.length];
                System.arraycopy(tmpDistances, 0, distances, 0, tmpDistances.length);
                System.arraycopy(itemDistances, 0, distances, tmpDistances.length, itemDistances.length);
            }
            else
            {
                distances = new double[itemDistances.length];
                System.arraycopy(itemDistances, 0, distances, 0, itemDistances.length);
            }
        }
        
        return distances;
    }
    
    public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, boolean isBark)
    {
        Lsfs lsfs1 = new Lsfs(item1.lsfFile);
        Lsfs lsfs2 = new Lsfs(item2.lsfFile);
        
        ESTLabels labs1 = new ESTLabels(item1.labelFile);
        ESTLabels labs2 = new ESTLabels(item2.labelFile);
 
        double[] frameDistances = null;
        
        if (labs1.items!=null && labs2.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[][] labelMap = StringUtil.alignLabels(labs1.items, labs2.items);
            //

            if (labelMap!=null)
            {
                int j, labInd1, labInd2, frmInd1, frmInd2;
                double time1, time2;
                double startTime1, endTime1, startTime2, endTime2;

                labInd1 = 0;

                frameDistances = new double[lsfs1.params.numfrm];
                
                //Find the corresponding target frame index for each source frame index
                int count = 0;
                for (j=0; j<lsfs1.params.numfrm; j++)
                {
                    time1 = SignalProcUtils.frameIndex2Time(j, lsfs1.params.winsize, lsfs1.params.skipsize);

                    while (time1>labs1.items[labInd1].time)
                    {
                        labInd1++;
                        if (labInd1>labs1.items.length-1)
                        {
                            labInd1 = labs1.items.length-1;
                            break;
                        }
                    }

                    labInd2 = StringUtil.findInMap(labelMap, labInd1);

                    if (labInd2>=0)
                    {
                        if (labInd1>0)   
                            startTime1 = labs1.items[labInd1-1].time;
                        else
                            startTime1 = 0.0;

                        if (labInd2>0) 
                            startTime2 = labs2.items[labInd2-1].time;
                        else
                            startTime2 = 0.0;

                        endTime1 = labs1.items[labInd1].time;
                        endTime2 = labs2.items[labInd2].time;

                        time2 = MathUtils.linearMap(time1, startTime1, endTime1, startTime2, endTime2);

                        frmInd2 = SignalProcUtils.time2frameIndex(time2, lsfs2.params.winsize, lsfs2.params.skipsize);

                        if (!isBark)
                            frameDistances[j] = SignalProcUtils.getRmsDistance(lsfs1.lsfs[j], lsfs2.lsfs[frmInd2]);
                        else
                            frameDistances[j] = SignalProcUtils.getRmsDistance(SignalProcUtils.freq2bark(lsfs1.lsfs[j]), SignalProcUtils.freq2bark(lsfs2.lsfs[frmInd2]));
                    } 
                }
            }
        }
        
        return frameDistances;
    }
    
    public static void main(String[] args)
    {
        RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();
        
        boolean isBark = true;
        double[] distances = r.getDistances("d:\\1\\test_ins", "d:\\1\\test_outs", isBark);
        
        double m = MathUtils.mean(distances);
        double v = MathUtils.variance(distances, m);
        
        System.out.println("MeanDist=" + String.valueOf(m) + " " + "VarDist=" + String.valueOf(v));
    }

}
