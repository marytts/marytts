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
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.Lsfs;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author oytun.turk
 *
 * Implements root-mean-square LP spectral envelope distance between two speech frames
 * 
 */
public class RmsLogLPSpectralEnvelopeDistortionComputer extends BaselineLPSpectralEnvelopeDistortionComputer {
    
    public RmsLogLPSpectralEnvelopeDistortionComputer()
    {
        super();
    }
    
    public double frameDistance(double[] frm1, double[] frm2, int fftSize, int lpOrder)
    {
        super.frameDistance(frm1, frm2, fftSize, lpOrder);
        
        return SpectralDistanceMeasures.rmsLogSpectralDist(frm1, frm2, fftSize, lpOrder);
    }
    
    public void mainParametricInterspeech2008(String outputFolder, String method, String emotion, String outputFilePostExtension)
    { 
        String tgtFolder = outputFolder + "target/" + emotion;
        String srcFolder = outputFolder + "source/" + emotion;
        String tfmFolder = outputFolder + method + "/" + emotion;
        String outputFile = outputFolder + method + "_" + emotion + "_" + outputFilePostExtension;
        String infoString = method + " " + emotion;
        
        mainParametric(srcFolder, tgtFolder, tfmFolder, outputFile, infoString);
    }
    
    //Put source and target wav and lab files into two folders and call this function
    public void mainInterspeech2008()
    {
        String method; //"1_codebook"; "2_frame"; "3_gmm";
        String emotion; //"angry"; "happy"; "sad"; "all";
        String outputFolder = "D:/Oytun/DFKI/voices/Interspeech08_out/objective_test/";
        String tgtFolder, srcFolder, tfmFolder, outputFile, infoString;
        String outputFilePostExtension = "itakuraSaitoLPSpectralEnvelope.txt";
        
        //Method 1: Weighted codebook
        method = "1_codebook";
        emotion = "angry"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "happy"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "sad";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "all";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        //
        
        //Method 2: Frame weighting
        method = "2_frame";
        emotion = "angry"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "happy"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "sad";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "all";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        //

        //Method 3: GMM
        method = "3_gmm";
        emotion = "angry"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "happy"; mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "sad";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        emotion = "all";   mainParametricInterspeech2008(outputFolder, method, emotion, outputFilePostExtension);
        //

        System.out.println("Objective test completed...");
    }
    
    //Put source and target wav and lab files into two folders and call this function
    public void mainHmmVoiceConversion()
    {
        RmsLogLPSpectralEnvelopeDistortionComputer sdc = new RmsLogLPSpectralEnvelopeDistortionComputer();
        
        String baseInputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/";
        String baseOutputFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/objective_test/";
        boolean isBark = true;
        String method1, method2, folder1, folder2, referenceFolder, outputFile, infoString;
        
        referenceFolder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest2/output/final/origTarget";
        
        //No-GV vs GV
        method1 = "NOGV";
        method2 = "GV";
        folder1 = baseInputFolder + "hmmSource_nogv";
        folder2 = baseInputFolder + "hmmSource_gv";
        outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
        infoString = method1 + " " + method2;
        mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);
        
        //No-GV vs SC 
        method1 = "NOGV";
        method2 = "NOGV+SC";
        folder1 = baseInputFolder + "hmmSource_nogv";
        folder2 = baseInputFolder + "tfm_nogv_1092files_128mixes";
        outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
        infoString = method1 + " " + method2;
        mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);
        
        //GV vs SC 
        method1 = "GV";
        method2 = "GV+SC";
        folder1 = baseInputFolder + "hmmSource_gv";
        folder2 = baseInputFolder + "tfm_gv_1092files_128mixes";
        outputFile = baseOutputFolder + "IS_" + method1 + "_" + method2 + ".txt";
        infoString = method1 + " " + method2;
        mainParametric(folder1, folder2, referenceFolder, outputFile, infoString);
       
        System.out.println("Objective test completed...");
    }
    
    public static void main(String[] args)
    {
        RmsLogLPSpectralEnvelopeDistortionComputer d = new RmsLogLPSpectralEnvelopeDistortionComputer();
        
        //d.mainInterspeech2008();
        
        d.mainHmmVoiceConversion();
    }
}
