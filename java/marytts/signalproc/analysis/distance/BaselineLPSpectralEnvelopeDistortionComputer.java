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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.signalproc.adaptation.BaselineAdaptationItem;
import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.util.ESTLabels;
import marytts.signalproc.util.SignalProcUtils;
import marytts.util.FileUtils;
import marytts.util.MathUtils;
import marytts.util.StringUtils;
import marytts.util.audio.AudioDoubleDataSource;


/**
 * @author oytun.turk
 *
 */
public class BaselineLPSpectralEnvelopeDistortionComputer extends BaselineDistortionComputer {
    public static final double DEFAULT_WINDOWSIZE = 0.020;
    public static final double DEFAULT_SKIPSIZE = 0.010;
    public static final int DEFAULT_FFTSIZE = -1;
    public static final int DEFAULT_LPORDER = -1;
    
    public BaselineLPSpectralEnvelopeDistortionComputer()
    {
        super();
    }
    
    public double[] getDistances(String folder1, String folder2)
    {
        return getDistances(folder1, folder2, DEFAULT_WINDOWSIZE);
    }
    
    public double[] getDistances(String folder1, String folder2,  double winSizeInSeconds)
    {   
        return getDistances(folder1, folder2, winSizeInSeconds, DEFAULT_SKIPSIZE);
    }
    
    public double[] getDistances(String folder1, String folder2,  double winSizeInSeconds, double skipSizeInSeconds)
    {  
        return getDistances(folder1, folder2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
    }
    public double[] getDistances(String folder1, String folder2,  double winSizeInSeconds, double skipSizeInSeconds, int fftSize)
    {  
        return getDistances(folder1, folder2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
    }
    
    public double[] getDistances(String folder1, String folder2,  double winSizeInSeconds, double skipSizeInSeconds, int fftSize, int lpOrder)
    {   
        folder1 = StringUtils.checkLastSlash(folder1);
        folder2 = StringUtils.checkLastSlash(folder2);
        
        BaselineAdaptationSet set1 = new BaselineAdaptationSet(folder1, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        BaselineAdaptationSet set2 = new BaselineAdaptationSet(folder2, BaselineAdaptationSet.DEFAULT_WAV_EXTENSION);
        
        return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, lpOrder);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2)
    {
        return getDistances(set1, set2, DEFAULT_WINDOWSIZE);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, 
                                 double winSizeInSeconds)
    {
           return getDistances(set1, set2, winSizeInSeconds, DEFAULT_SKIPSIZE);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, 
                                 double winSizeInSeconds, double skipSizeInSeconds)
    {
        return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
    }
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, 
                                 double winSizeInSeconds, double skipSizeInSeconds, int fftSize)
    {
        return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, 
                                 double winSizeInSeconds, double skipSizeInSeconds, 
                                 int fftSize, int lpOrder)
    {
        int[] map = new int[Math.min(set1.items.length, set2.items.length)];
        for (int i=0; i<map.length; i++)
            map[i] = i;
        
        return getDistances(set1, set2, winSizeInSeconds, skipSizeInSeconds, fftSize, lpOrder, map);
    }
    
    public double[] getDistances(BaselineAdaptationSet set1, BaselineAdaptationSet set2, 
                                 double winSizeInSeconds, double skipSizeInSeconds, 
                                 int fftSize, int lpOrder, 
                                 int[] map)
    {
        double[] distances = null;
        double[] tmpDistances = null;
        
        for (int i=0; i<map.length; i++)
        {
            double[] itemDistances = getItemDistances(set1.items[i], set2.items[map[i]], winSizeInSeconds, skipSizeInSeconds, fftSize, lpOrder);
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
    
    public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, double winSizeInSeconds, double skipSizeInSeconds)
    {
        return getItemDistances(item1, item2, winSizeInSeconds, skipSizeInSeconds, DEFAULT_FFTSIZE);
    }
    
    public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, double winSizeInSeconds, double skipSizeInSeconds, int fftSize)
    {
        return getItemDistances(item1, item2, winSizeInSeconds, skipSizeInSeconds, fftSize, DEFAULT_LPORDER);
    }
    
    public double[] getItemDistances(BaselineAdaptationItem item1, BaselineAdaptationItem item2, 
                                     double winSizeInSeconds, double skipSizeInSeconds, 
                                     int fftSize, int lpOrder)
    {
        double[] frameDistances = null;
        
        //Read wav files & determine avaliable number of frames
        AudioInputStream inputAudio1 = null;
        try {
            inputAudio1 = AudioSystem.getAudioInputStream(new File(item1.audioFile));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        AudioInputStream inputAudio2 = null;
        try {
            inputAudio2 = AudioSystem.getAudioInputStream(new File(item2.audioFile));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (inputAudio1!=null && inputAudio2!=null)
        {
            int samplingRate1 = (int)inputAudio1.getFormat().getSampleRate();
            int ws1 =  (int)Math.floor(winSizeInSeconds*samplingRate1+0.5);
            int ss1 = (int)Math.floor(skipSizeInSeconds*samplingRate1+0.5);
            AudioDoubleDataSource signal1 = new AudioDoubleDataSource(inputAudio1);
            double[] x1 = signal1.getAllData();
            double[] frm1 = new double[ws1];
            int numfrm1 = (int)Math.floor((x1.length-ws1)/((double)ss1)+0.5);

            int samplingRate2 = (int)inputAudio2.getFormat().getSampleRate();
            int ws2 =  (int)Math.floor(winSizeInSeconds*samplingRate2+0.5);
            int ss2 = (int)Math.floor(skipSizeInSeconds*samplingRate2+0.5);
            AudioDoubleDataSource signal2 = new AudioDoubleDataSource(inputAudio2);
            double[] x2 = signal2.getAllData();
            double[] frm2 = new double[ws2];
            int numfrm2 = (int)Math.floor((x2.length-ws2)/((double)ss2)+0.5);

            if (fftSize<0)
            {
                fftSize = Math.max(SignalProcUtils.getDFTSize(samplingRate1), SignalProcUtils.getDFTSize(samplingRate2));
                while (fftSize<ws1)
                    fftSize*=2;
                while (fftSize<ws2)
                    fftSize*=2;
            }
            
            if (lpOrder<0)
                lpOrder = Math.max(SignalProcUtils.getLPOrder(samplingRate1), SignalProcUtils.getLPOrder(samplingRate2));
            //

            ESTLabels labs1 = new ESTLabels(item1.labelFile);
            ESTLabels labs2 = new ESTLabels(item2.labelFile);

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
                    int x1Start, x2Start;

                    labInd1 = 0;

                    frameDistances = new double[numfrm1];

                    //Find the corresponding target frame index for each source frame index
                    for (j=0; j<numfrm1; j++)
                    {
                        time1 = SignalProcUtils.frameIndex2Time(j, winSizeInSeconds, skipSizeInSeconds);

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

                                frmInd2 = SignalProcUtils.time2frameIndex(time2, winSizeInSeconds, skipSizeInSeconds);
                                if (frmInd2<0)
                                    frmInd2=0;
                                if (frmInd2>numfrm2-1)
                                    frmInd2=numfrm2-1;

                                x1Start = (int)Math.floor(j*ss1+0.5*ws1+0.5);
                                x2Start = (int)Math.floor(frmInd2*ss2+0.5*ws2+0.5);
                                
                                if (x1Start+ws1<x1.length)
                                    System.arraycopy(x1, x1Start, frm1, 0, ws1);
                                else
                                {
                                    Arrays.fill(frm1, 0.0);
                                    System.arraycopy(x1, x1Start, frm1, 0, x1.length-x1Start);
                                }

                                if (x2Start+ws2<x2.length)
                                    System.arraycopy(x2, x2Start, frm2, 0, ws2);
                                else
                                {
                                    Arrays.fill(frm2, 0.0);
                                    System.arraycopy(x2, x2Start, frm2, 0, x2.length-x2Start);
                                }

                                frameDistances[count] = frameDistance(frm1, frm2, fftSize, lpOrder);
                                
                                count++;
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
        }
        else
        {
            if (inputAudio1==null)
                System.out.println("Error! Cannot open file: " + item1.audioFile);
            
            if (inputAudio2==null)
                System.out.println("Error! Cannot open file: " + item2.audioFile);
        }

        return frameDistances;
    }
    
    //Implement functionality in dervied classes
    public double frameDistance(double[] frm1, double[] frm2, int fftSize, int lpOrder)
    {
        return 1.0;
    }
    
    public void mainParametric(String method, String emotion, String outputTextFileExtension)
    {  
        String baseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08_out\\objective_test\\";

        String tgtFolder = baseDir + "target\\" + emotion;
        String srcFolder = baseDir + "source\\" + emotion;
        String tfmFolder = baseDir + method + "\\" + emotion;
        
        String outputFile = baseDir + method + "_" + emotion + "_" + outputTextFileExtension;
  
        double[] distances1 = getDistances(tgtFolder, srcFolder);
        double[] distances2 = getDistances(tgtFolder, tfmFolder);
        
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
    
    public void mainBase(String outputTextFileExtension)
    {   
        String method; //"1_codebook"; "2_frame"; "3_gmm";
        String emotion; //"angry"; "happy"; "sad"; "all";
        
        method = "1_codebook";
        emotion = "angry";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "happy";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "sad";    mainParametric(method, emotion, outputTextFileExtension);
        emotion = "all";    mainParametric(method, emotion, outputTextFileExtension);
        
        method = "2_frame";
        emotion = "angry";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "happy";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "sad";    mainParametric(method, emotion, outputTextFileExtension);
        emotion = "all";    mainParametric(method, emotion, outputTextFileExtension);

        method = "3_gmm";
        emotion = "angry";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "happy";  mainParametric(method, emotion, outputTextFileExtension);
        emotion = "sad";    mainParametric(method, emotion, outputTextFileExtension);
        emotion = "all";    mainParametric(method, emotion, outputTextFileExtension);

        System.out.println("Objective test completed...");
    }
}
