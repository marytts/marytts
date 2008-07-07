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

package marytts.signalproc.analysis.distance;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.FileMap;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.analysis.ESTLabels;
import marytts.signalproc.analysis.LineSpectralFrequencies;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.Lsfs;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;


/**
 * @author oytun.turk
 *
 */
public class RmsLsfDistortionComputer extends BaselineDistortionComputer {
    public RmsLsfDistortionComputer()
    {
        super();
    }
    
    public double[] getDistances(String folder1, String folder2, double upperFreqInHz)
    {
        folder1 = StringUtils.checkLastSlash(folder1);
        folder2 = StringUtils.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return getDistances(set1, set2, false, upperFreqInHz);
    }
    
    public double[] getDistances(String folder1, String folder2, boolean isBark, double upperFreqInHz)
    {
        folder1 = StringUtils.checkLastSlash(folder1);
        folder2 = StringUtils.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return getDistances(set1, set2, isBark, upperFreqInHz);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark, double upperFreqInHz)
    {
        int[] map = new int[Math.min(set1.items.length, set2.items.length)];
        for (int i=0; i<map.length; i++)
            map[i] = i;
        
        return getDistances(set1, set2, isBark, upperFreqInHz, map);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, boolean isBark, double upperFreqInHz, int[] map)
    {
        double[] distances = null;
        double[] tmpDistances = null;
        
        for (int i=0; i<map.length; i++)
        {
            double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], isBark, upperFreqInHz);
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
    
    public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, boolean isBark, double upperFreqInHz)
    {
        if (!FileUtils.exists(item1.lsfFile)) //Extract lsfs if necessary
        {
            LsfFileHeader lsfParams = new LsfFileHeader();
            try {
                BaselineFeatureExtractor.lsfAnalysis(item1, lsfParams, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        if (!FileUtils.exists(item2.lsfFile)) //Extract lsfs if necessary
        {
            LsfFileHeader lsfParams = new LsfFileHeader();
            try {
                BaselineFeatureExtractor.lsfAnalysis(item2, lsfParams, true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        Lsfs lsfs1 = new Lsfs(item1.lsfFile);
        Lsfs lsfs2 = new Lsfs(item2.lsfFile);
        
        ESTLabels labs1 = new ESTLabels(item1.labelFile);
        ESTLabels labs2 = new ESTLabels(item2.labelFile);
 
        double[] frameDistances = null;
        int count = 0;
        
        if (labs1.items!=null && labs2.items!=null)
        {
            //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
            int[][] labelMap = StringUtils.alignLabels(labs1.items, labs2.items);
            //

            if (labelMap!=null)
            {
                int j, labInd1, labInd2, frmInd1, frmInd2;
                double time1, time2;
                double startTime1, endTime1, startTime2, endTime2;
                double[] tmpLsfs1 = null;
                double[] tmpLsfs2 = null;
                int maxInd1, maxInd2, maxInd;

                labInd1 = 0;

                frameDistances = new double[lsfs1.params.numfrm];

                //Find the corresponding target frame index for each source frame index
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
                    
                    if (labInd1>0 && labInd1<labs1.items.length-1) //Exclude first and last label)
                    {
                        labInd2 = StringUtils.findInMap(labelMap, labInd1);

                        if (labInd2>=0 && labs1.items[labInd1].phn.compareTo(labs2.items[labInd2].phn)==0)
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
                            if (frmInd2<0)
                                frmInd2=0;
                            if (frmInd2>lsfs2.params.numfrm-1)
                                frmInd2=lsfs2.params.numfrm-1;
                            
                            maxInd1 = MathUtils.getLargestIndexSmallerThan(lsfs1.lsfs[j], upperFreqInHz);
                            maxInd2 = MathUtils.getLargestIndexSmallerThan(lsfs2.lsfs[frmInd2], upperFreqInHz);
                            maxInd = Math.min(maxInd1, maxInd2);
                            
                            tmpLsfs1 = new double[maxInd+1];
                            tmpLsfs2 = new double[maxInd+1];
                            System.arraycopy(lsfs1.lsfs[j], 0, tmpLsfs1, 0, maxInd+1);
                            System.arraycopy(lsfs2.lsfs[frmInd2], 0, tmpLsfs2, 0, maxInd+1);

                            if (!isBark)
                                frameDistances[count++] = SignalProcUtils.getRmsDistance(tmpLsfs1, tmpLsfs2);
                            else
                                frameDistances[count++] = SignalProcUtils.getRmsDistance(SignalProcUtils.freq2bark(tmpLsfs1), SignalProcUtils.freq2bark(tmpLsfs2));
                        } 
                    }
                    
                    if (count>=frameDistances.length)
                        break;
                }
            }
        }
        
        if (count>0)
        {
            double[] tmpFrameDistances = new double[count];
            System.arraycopy(frameDistances, 0, tmpFrameDistances, 0, count);
            frameDistances = new double[count];
            System.arraycopy(tmpFrameDistances, 0, frameDistances, 0, count);
        }
        
        return frameDistances;
    }
    
    public static void mainParametric(String method, String emotion, boolean isBark)
    {  
        String baseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08_out\\objective_test\\";

        String tgtFolder = baseDir + "target\\" + emotion;
        String srcFolder = baseDir + "source\\" + emotion;
        String tfmFolder = baseDir + method + "\\" + emotion;
        
        String outputFile = baseDir + method + "_" + emotion + "_rmsLsf.txt";
        
        RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();
  
        double[] distances1 = r.getDistances(tgtFolder, srcFolder, isBark, 8000);
        double[] distances2 = r.getDistances(tgtFolder, tfmFolder, isBark, 8000);
        
        double m1 = MathUtils.mean(distances1);
        double s1 = MathUtils.standardDeviation(distances1, m1);
        double m2 = MathUtils.mean(distances2);
        double s2 = MathUtils.standardDeviation(distances2, m2);
        
        double[] tmpOut = new double[distances1.length+distances2.length + 6];
        tmpOut[0] = m1; //tgt-src mean
        tmpOut[1] = s1; //tgt-src std
        tmpOut[2] = m2; //tgt-tfm mean
        tmpOut[3] = s2; //tgt-tfm std
        tmpOut[4] = m1-m2; //decrease in tgt-src distance by tfm
        System.arraycopy(distances1, 0, tmpOut, 5, distances1.length);
        System.arraycopy(distances2, 0, tmpOut, distances1.length+5, distances2.length);
        FileUtils.writeToTextFile(tmpOut, outputFile);
        
        System.out.println(method + " " + emotion + " tgt-src: MeanDist=" + String.valueOf(m1) + " " + "StdDist=" + String.valueOf(s1));
        System.out.println(method + " " + emotion + " tgt-tfm: MeanDist=" + String.valueOf(m2) + " " + "StdDist=" + String.valueOf(s2));
        System.out.println(method + " " + emotion + " distance reduction=" + String.valueOf(m1-m2));
    }
    
  //Put source and target wav and lab files into two folders and call this function
    public static void main(String[] args)
    {
        boolean isBark = true;
        String method; //"1_codebook"; "2_frame"; "3_gmm";
        String emotion; //"angry"; "happy"; "sad"; "all";
        
        method = "1_codebook";
        emotion = "angry";  mainParametric(method, emotion, isBark);
        emotion = "happy";  mainParametric(method, emotion, isBark);
        emotion = "sad";    mainParametric(method, emotion, isBark);
        emotion = "all";    mainParametric(method, emotion, isBark);
        
        method = "2_frame";
        emotion = "angry";  mainParametric(method, emotion, isBark);
        emotion = "happy";  mainParametric(method, emotion, isBark);
        emotion = "sad";    mainParametric(method, emotion, isBark);
        emotion = "all";    mainParametric(method, emotion, isBark);

        method = "3_gmm";
        emotion = "angry";  mainParametric(method, emotion, isBark);
        emotion = "happy";  mainParametric(method, emotion, isBark);
        emotion = "sad";    mainParametric(method, emotion, isBark);
        emotion = "all";    mainParametric(method, emotion, isBark);
        
        System.out.println("Objective test completed...");
    }
}
