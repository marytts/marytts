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
package marytts.signalproc.analysis.distance;

import java.io.IOException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.Lsfs;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;


/**
 * Implements root-mean-square line spectral frequency vector distance given two sets of paired files
 * 
 * @author Oytun T&uumlrk
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
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        
        return getDistances(set1, set2, false, upperFreqInHz);
    }
    
    public double[] getDistances(String folder1, String folder2, boolean isBark, double upperFreqInHz)
    {
        folder1 = StringUtils.checkLastSlash(folder1);
        folder2 = StringUtils.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.WAV_EXTENSION_DEFAULT);
        
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
        
        Labels labs1 = new Labels(item1.labelFile);
        Labels labs2 = new Labels(item2.labelFile);
 
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
    
    public static void mainParametricInterspeech2008(String method, String emotion, boolean isBark)
    {  
        String baseDir = "D:/Oytun/DFKI/voices/Interspeech08_out/objective_test/";

        String tgtFolder = baseDir + "target/" + emotion;
        String srcFolder = baseDir + "source/" + emotion;
        String tfmFolder = baseDir + method + "/" + emotion;
        
        String outputFile = baseDir + method + "_" + emotion + "_rmsLsf.txt";
        
        RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();
  
        double[] distances1 = r.getDistances(tgtFolder, srcFolder, isBark, 8000);
        double[] distances2 = r.getDistances(tgtFolder, tfmFolder, isBark, 8000);
        
        double m1 = MathUtils.mean(distances1);
        double s1 = MathUtils.standardDeviation(distances1, m1);
        double m2 = MathUtils.mean(distances2);
        double s2 = MathUtils.standardDeviation(distances2, m2);
        double conf95_1 = MathUtils.getConfidenceInterval95(s1);
        double conf99_1 = MathUtils.getConfidenceInterval99(s1);
        double conf95_2 = MathUtils.getConfidenceInterval95(s2);
        double conf99_2 = MathUtils.getConfidenceInterval99(s2);
        
        double[] tmpOut = new double[distances1.length+distances2.length + 9];
        tmpOut[0] = m1; //tgt-src mean
        tmpOut[1] = s1; //tgt-src std
        tmpOut[2] = m2; //tgt-tfm mean
        tmpOut[3] = s2; //tgt-tfm std
        tmpOut[4] = m1-m2; //decrease in tgt-src distance by tfm
        tmpOut[5] = conf95_1; //95% confidence interval for distance tgt-src distances
        tmpOut[6] = conf99_1; //99% confidence interval for distance tgt-src distances
        tmpOut[7] = conf95_2; //95% confidence interval for distance tgt-tfm distances
        tmpOut[8] = conf99_2; //99% confidence interval for distance tgt-tfm distances
        
        System.arraycopy(distances1, 0, tmpOut, 9, distances1.length);
        System.arraycopy(distances2, 0, tmpOut, distances1.length + 9, distances2.length);
        FileUtils.writeToTextFile(tmpOut, outputFile);
        
        double c1Left95 = m1-conf95_1;
        double c1Left99 = m1-conf99_1;
        double c1Right95 = m1+conf95_1;
        double c1Right99 = m1+conf99_1;
        double c2Left95 = m2-conf95_2;
        double c2Left99 = m2-conf99_2;
        double c2Right95 = m2+conf95_2;
        double c2Right99 = m2+conf99_2;
        
        System.out.println(method + " " + emotion + " tgt-src: MeanDist=" + String.valueOf(m1) + " " + "StdDist=" + String.valueOf(s1));
        System.out.println(method + " " + emotion + " tgt-tfm: MeanDist=" + String.valueOf(m2) + " " + "StdDist=" + String.valueOf(s2));
        System.out.println(method + " " + emotion + " distance reduction=" + String.valueOf(m1-m2));
        System.out.println("Confidence intervals tgt-src %95:  " + String.valueOf(conf95_1) + " --> [" + String.valueOf(c1Left95) + "," + String.valueOf(c1Right95) + "]");
        System.out.println("Confidence intervals tgt-src %99:  " + String.valueOf(conf99_1) + " --> [" + String.valueOf(c1Left99) + "," + String.valueOf(c1Right99) + "]");
        System.out.println("Confidence intervals tgt-tfm %95:  " + String.valueOf(conf95_2) + " --> [" + String.valueOf(c2Left95) + "," + String.valueOf(c2Right95) + "]");
        System.out.println("Confidence intervals tgt-tfm %99:  " + String.valueOf(conf99_2) + " --> [" + String.valueOf(c2Left99) + "," + String.valueOf(c2Right99) + "]");
        System.out.println("---------------------------------");
    }
    
    //Put source and target wav and lab files into two folders and call this function
    public static void mainInterspeech2008()
    {
        boolean isBark = true;
        String method; //"1_codebook"; "2_frame"; "3_gmm";
        String emotion; //"angry"; "happy"; "sad"; "all";
        
        method = "1_codebook";
        emotion = "angry";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "happy";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "sad";    mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "all";    mainParametricInterspeech2008(method, emotion, isBark);
        
        method = "2_frame";
        emotion = "angry";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "happy";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "sad";    mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "all";    mainParametricInterspeech2008(method, emotion, isBark);

        method = "3_gmm";
        emotion = "angry";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "happy";  mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "sad";    mainParametricInterspeech2008(method, emotion, isBark);
        emotion = "all";    mainParametricInterspeech2008(method, emotion, isBark);
        
        System.out.println("Objective test completed...");
    }
    
    public static void mainHmmVoiceConversion(String method1, String method2, String folder1, String folder2, String referenceFolder, String outputFile, boolean isBark)
    {  
        RmsLsfDistortionComputer r = new RmsLsfDistortionComputer();
  
        double[] distances1 = r.getDistances(referenceFolder, folder1, isBark, 8000);
        double[] distances2 = r.getDistances(referenceFolder, folder2, isBark, 8000);
        
        double m1 = MathUtils.mean(distances1);
        double s1 = MathUtils.standardDeviation(distances1, m1);
        double m2 = MathUtils.mean(distances2);
        double s2 = MathUtils.standardDeviation(distances2, m2);
        double conf95_1 = MathUtils.getConfidenceInterval95(s1);
        double conf99_1 = MathUtils.getConfidenceInterval99(s1);
        double conf95_2 = MathUtils.getConfidenceInterval95(s2);
        double conf99_2 = MathUtils.getConfidenceInterval99(s2);
        
        double[] tmpOut = new double[distances1.length+distances2.length + 9];
        tmpOut[0] = m1; //tgt-src mean
        tmpOut[1] = s1; //tgt-src std
        tmpOut[2] = m2; //tgt-tfm mean
        tmpOut[3] = s2; //tgt-tfm std
        tmpOut[4] = m1-m2; //decrease in tgt-src distance by tfm
        tmpOut[5] = conf95_1; //95% confidence interval for distance tgt-src distances
        tmpOut[6] = conf99_1; //99% confidence interval for distance tgt-src distances
        tmpOut[7] = conf95_2; //95% confidence interval for distance tgt-tfm distances
        tmpOut[8] = conf99_2; //99% confidence interval for distance tgt-tfm distances
        
        System.arraycopy(distances1, 0, tmpOut, 9, distances1.length);
        System.arraycopy(distances2, 0, tmpOut, distances1.length + 9, distances2.length);
        FileUtils.writeToTextFile(tmpOut, outputFile);
        
        double c1Left95 = m1-conf95_1;
        double c1Left99 = m1-conf99_1;
        double c1Right95 = m1+conf95_1;
        double c1Right99 = m1+conf99_1;
        double c2Left95 = m2-conf95_2;
        double c2Left99 = m2-conf99_2;
        double c2Right95 = m2+conf95_2;
        double c2Right99 = m2+conf99_2;
        
        System.out.println(method1+ " tgt-src: MeanDist=" + String.valueOf(m1) + " " + "StdDist=" + String.valueOf(s1));
        System.out.println(method2 + " tgt-tfm: MeanDist=" + String.valueOf(m2) + " " + "StdDist=" + String.valueOf(s2));
        System.out.println("Distance reduction=" + String.valueOf(m1-m2));
        System.out.println("Confidence intervals reference-method1 %95:  " + String.valueOf(conf95_1) + " --> [" + String.valueOf(c1Left95) + "," + String.valueOf(c1Right95) + "]");
        System.out.println("Confidence intervals reference-method1 %99:  " + String.valueOf(conf99_1) + " --> [" + String.valueOf(c1Left99) + "," + String.valueOf(c1Right99) + "]");
        System.out.println("Confidence intervals reference-method2 %95:  " + String.valueOf(conf95_2) + " --> [" + String.valueOf(c2Left95) + "," + String.valueOf(c2Right95) + "]");
        System.out.println("Confidence intervals reference-method2 %99:  " + String.valueOf(conf99_2) + " --> [" + String.valueOf(c2Left99) + "," + String.valueOf(c2Right99) + "]");
        System.out.println("---------------------------------");
    }
    
    public static void mainHmmVoiceConversion()
    {
        String baseInputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/";
        String baseOutputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/objective_test/";
        boolean isBark = true;
        String method1, method2, folder1, folder2, referenceFolder, outputFile;
        
        referenceFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/origTarget";
        
        //No-GV vs GV
        method1 = "NOGV";
        method2 = "GV";
        folder1 = baseInputFolder + "hmmSource_nogv";
        folder2 = baseInputFolder + "hmmSource_gv";
        outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
        mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);
        
        //No-GV vs SC 
        method1 = "NOGV";
        method2 = "NOGV+SC";
        folder1 = baseInputFolder + "hmmSource_nogv";
        folder2 = baseInputFolder + "tfm_nogv_1092files_128mixes";
        outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
        mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);
        
        //GV vs SC 
        method1 = "GV";
        method2 = "GV+SC";
        folder1 = baseInputFolder + "hmmSource_gv";
        folder2 = baseInputFolder + "tfm_gv_1092files_128mixes";
        outputFile = baseOutputFolder + "lsf_" + method1 + "_" + method2 + ".txt";
        mainHmmVoiceConversion(method1, method2, folder1, folder2, referenceFolder, outputFile, isBark);
        
        System.out.println("Objective test completed...");
    }
    
    public static void main(String[] args)
    {
        //mainInterspeech2008();
        
        mainHmmVoiceConversion();
    }
}

