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

package marytts.signalproc.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.tools.voiceimport.BasenameList;
import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.string.StringUtils;

/**
 * @author oytun.turk
 *
 * Converts binary MFCC files in raw SPTK format into Mary MFCC file format
 * with header
 *  
 */
public class MfccRaw2MfccConverter {
    public static void convertFolder(String folder, 
                                     String rawMfccFileExtension, 
                                     String outputMfccFileExtension,
                                     int dimension, int samplingRateInHz, 
                                     float windowSizeInSeconds, float skipSizeInSeconds)
    {
        folder = StringUtils.checkLastSlash(folder);
        
        BasenameList b = new BasenameList(folder, rawMfccFileExtension);
        
        String rawMfccFile;
        String outputMfccFile;
        int numFiles = b.getListAsVector().size();
        for (int i=0; i<numFiles; i++)
        {
            rawMfccFile = folder + b.getName(i) + rawMfccFileExtension;
            outputMfccFile = StringUtils.modifyExtension(rawMfccFile, outputMfccFileExtension);
            rawFile2mfccFile(rawMfccFile, outputMfccFile, 
                             dimension, samplingRateInHz, 
                             windowSizeInSeconds, skipSizeInSeconds);
            
            System.out.println("Converted MFCC file " + String.valueOf(i+1) + " of " + String.valueOf(numFiles));
        }
    }
    public static void rawFile2mfccFile(String rawFile, String mfccFile, 
                                        int dimension, int samplingRateInHz, 
                                        float windowSizeInSeconds, float skipSizeInSeconds)
    {
        Mfccs m = readRawMfccFile(rawFile, dimension);
        
        m.params.samplingRate = samplingRateInHz;
        m.params.skipsize = skipSizeInSeconds;
        m.params.winsize = windowSizeInSeconds;
        
        m.writeMfccFile(mfccFile);
    }
    
    //This version is for reading SPTK files that have no header
    //The header is created from user specified information
    public static Mfccs readRawMfccFile(String rawMfccFile, int dimension)
    {
        
        MfccFileHeader params = new MfccFileHeader();

        File f  = new File(rawMfccFile);
        long fileSize = f.length();
        int numfrm = (int)(fileSize/(4.0*dimension));
        Mfccs m = new Mfccs(numfrm, dimension);
        params.numfrm = numfrm;
        params.dimension = dimension;
        
        if (rawMfccFile!="" && FileUtils.exists(rawMfccFile))
        {
            MaryRandomAccessFile stream = null;
            try {
                stream = new MaryRandomAccessFile(rawMfccFile, "rw");
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            if (stream != null)
            {
                try {
                    Mfccs.readMfccsFromFloat(stream, params, m.mfccs);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        return m;
    }

    /**
     * @param args
     */
    public static void main(String[] args) 
    {
        String folder;
        String rawMfccFileExtension = ".mgc";
        String outputMfccFileExtension = ".mfc";
        int dimension = 25;
        int samplingRateInHz = 16000;
        float windowSizeInSeconds = 0.020f;
        float skipSizeInSeconds = 0.010f;
        
        folder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mgc-littend/hmm_train";
        MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);

        folder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mgc-littend/hmm_test";
        MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);

        folder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mgc-littend/orig_train";
        MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);

        folder = "D:/Oytun/DFKI/voices/hmmVoiceConversionTest/mgc-littend/orig_test";
        MfccRaw2MfccConverter.convertFolder(folder, rawMfccFileExtension, outputMfccFileExtension, dimension, samplingRateInHz, windowSizeInSeconds, skipSizeInSeconds);
    }
}
