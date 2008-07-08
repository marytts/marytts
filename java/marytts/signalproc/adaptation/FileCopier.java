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

package marytts.signalproc.adaptation;

import java.io.IOException;

import marytts.util.FileUtils;
import marytts.util.StringUtils;


/**
 * @author oytun.turk
 *
 */
public class FileCopier {
    public FileCopier()
    {
        
    }
    
    //Generate appropriate wav and lab files by copying
    // source and target files from sourceInputBaseDir and targetInputBaseDir.
    // This is required since source and target files might not have identical filenames
    //
    // to sourceTrainingBaseDir and targetTrainingBaseDir with appropriate renaming
    // The output will be wav and lab files for source and target for parallel voice conversion training
    // and when the wav and lab files are sorted according to filenames, they will be
    // identical in content,
    //
    // sourceTargetFile is a text file which has two columns that list the mapping between
    // source and target files under input base directories:
    //
    // sourceFileName1 targetFileName1
    // sourceFileName2 targetFileName2
    // ...etc
    //
    // The genearted source files will have identical filenames with the input source files
    // Target files will be copied with a new name in the following format:
    //
    // sourceFileName1_targetFileName1.wav,
    // sourceFileName1_targetFileName1.lab, etc.
    //
    public void copy(String sourceTargetFile,      //Input
                     String sourceInputBaseDir,    //Input
                     String targetInputBaseDir,    //Input
                     String sourceTrainingBaseDir, //Output
                     String targetTrainingBaseDir) //Output
    {
        String[][] stNameMap = StringUtils.readTextFileInRows(sourceTargetFile, 2);
        int i;
        
        //Determine source and target input sub directories
        sourceInputBaseDir = StringUtils.checkLastSlash(sourceInputBaseDir);
        targetInputBaseDir = StringUtils.checkLastSlash(targetInputBaseDir);
        String sourceInputWavDir = sourceInputBaseDir + "wav\\";
        String targetInputWavDir = targetInputBaseDir + "wav\\";
        String sourceInputLabDir = sourceInputBaseDir + "lab\\";
        String targetInputLabDir = targetInputBaseDir + "lab\\";
        if (!FileUtils.exists(sourceInputWavDir))
        {
            System.out.println("Error! Folder not found: " + sourceInputWavDir); 
            return;
        }
        if (!FileUtils.exists(targetInputWavDir))
        {    
            System.out.println("Error! Folder not found: " + targetInputWavDir);
            return;
        }
        if (!FileUtils.exists(sourceInputLabDir))
        {    
            System.out.println("Error! Folder not found: " + sourceInputLabDir);
            return;
        }
        if (!FileUtils.exists(targetInputLabDir))
        {    
            System.out.println("Error! Folder not found: " + targetInputLabDir);
            return;
        }
        //
        
        //Create training sub-folders for source and target
        sourceTrainingBaseDir = StringUtils.checkLastSlash(sourceTrainingBaseDir);
        targetTrainingBaseDir = StringUtils.checkLastSlash(targetTrainingBaseDir);
        FileUtils.createDirectory(sourceTrainingBaseDir);
        FileUtils.createDirectory(targetTrainingBaseDir);
        //
        
        if (stNameMap!=null)
        {
            System.out.println("Generating - " + sourceTrainingBaseDir + " and " + targetTrainingBaseDir);
            
            String tmpFileIn, tmpFileOut;
            for (i=0; i<stNameMap.length; i++)
            {
                //Source wav
                tmpFileIn = sourceInputWavDir + stNameMap[i][0] + ".wav";
                tmpFileOut = sourceTrainingBaseDir + stNameMap[i][0] + ".wav";
                if (!FileUtils.exists(tmpFileOut))
                {
                    if (FileUtils.exists(tmpFileIn))
                    {
                        try {
                            FileUtils.copy(tmpFileIn, tmpFileOut);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println("Error! Input file not found: " + tmpFileIn);
                        return;
                    }
                }
                //
                
                //Source lab
                tmpFileIn = sourceInputLabDir + stNameMap[i][0] + ".lab";
                tmpFileOut = sourceTrainingBaseDir + stNameMap[i][0] + ".lab";
                if (!FileUtils.exists(tmpFileOut))
                {
                    if (FileUtils.exists(tmpFileIn))
                    {
                        try {
                            FileUtils.copy(tmpFileIn, tmpFileOut);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println("Error! Input file not found: " + tmpFileIn);
                        return;
                    }
                }
                //
                
                //Target wav
                tmpFileIn = targetInputWavDir + stNameMap[i][1] + ".wav";
                tmpFileOut = targetTrainingBaseDir + stNameMap[i][0] + "_" + stNameMap[i][1] + ".wav";
                if (!FileUtils.exists(tmpFileOut))
                {
                    if (FileUtils.exists(tmpFileIn))
                    {
                        try {
                            FileUtils.copy(tmpFileIn, tmpFileOut);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println("Error! Input file not found: " + tmpFileIn);
                        return;
                    }
                }
                //
                
                //Target lab
                tmpFileIn = targetInputLabDir + stNameMap[i][1] + ".lab";
                tmpFileOut = targetTrainingBaseDir + stNameMap[i][0] + "_" + stNameMap[i][1] + ".lab";
                if (!FileUtils.exists(tmpFileOut))
                {
                    if (FileUtils.exists(tmpFileIn))
                    {
                        try {
                            FileUtils.copy(tmpFileIn, tmpFileOut);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        System.out.println("Error! Input file not found: " + tmpFileIn);
                        return;
                    }
                }
                //
                
                System.out.println(String.valueOf(i+1) + " of " + String.valueOf(stNameMap.length));
            }
        }
        
    }
    
    public static void main(String[] args)
    {
        FileCopier f = new FileCopier();
        String sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir;
        
        sourceTargetFile = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\mappings-mini-ea.txt";
        sourceInputBaseDir = "D:\\Oytun\\DFKI\\voices\\DFKI_German_Neutral";
        sourceTrainingBaseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\neutral";
        
        //Obadiah_Sad
        targetInputBaseDir = "D:\\Oytun\\DFKI\\voices\\DFKI_German_Obadiah_Sad";
        targetTrainingBaseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\sad";
        f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);
        
        //Poppy_Happy
        targetInputBaseDir = "D:\\Oytun\\DFKI\\voices\\DFKI_German_Poppy_Happy";
        targetTrainingBaseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\happy";
        f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);

        //Spike_Angry
        targetInputBaseDir = "D:\\Oytun\\DFKI\\voices\\DFKI_German_Spike_Angry";
        targetTrainingBaseDir = "D:\\Oytun\\DFKI\\voices\\Interspeech08\\angry";
        f.copy(sourceTargetFile, sourceInputBaseDir, targetInputBaseDir, sourceTrainingBaseDir, targetTrainingBaseDir);
    }

}
