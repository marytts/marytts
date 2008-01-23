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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import de.dfki.lt.mary.unitselection.adaptation.AdaptationUtils;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationItem;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.IndexMap;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;
import de.dfki.lt.signalproc.analysis.Lsfs;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author oytun.turk
 *
 * This class implements training for weighted codebook mapping based voice conversion
 * using parallel training data (i.e. source and target data in pairs of audio recordings which have identical content)
 */
public class WeightedCodebookParallelTrainer extends WeightedCodebookTrainer {
    
    public WeightedCodebookParallelTrainer(WeightedCodebookPreprocessor pp,
                                           WeightedCodebookFeatureExtractor fe,
                                           WeightedCodebookTrainerParams pa) 
    {
        super(pp, fe, pa);
    }
      
    public void run() throws IOException
    {
        checkParams();
        
        BaselineAdaptationSet sourceTrainingSet = getTrainingSet(params.sourceTrainingFolder);
        BaselineAdaptationSet targetTrainingSet = getTrainingSet(params.targetTrainingFolder);
        
        train(sourceTrainingSet, targetTrainingSet);
    }
    
    public void checkParams()
    {
        params.trainingBaseFolder = StringUtil.checkLastSlash(params.trainingBaseFolder);
        params.sourceTrainingFolder = StringUtil.checkLastSlash(params.sourceTrainingFolder);
        params.targetTrainingFolder = StringUtil.checkLastSlash(params.targetTrainingFolder);
    }
    
    //Create list of files
    public BaselineAdaptationSet getTrainingSet(String trainingFolder)
    {   
        BasenameList b = new BasenameList(trainingFolder, wavExt);
        
        BaselineAdaptationSet trainingSet = new BaselineAdaptationSet(b.getListAsVector().size());
        
        for (int i=0; i<trainingSet.items.length; i++)
            trainingSet.items[i].setFromWavFilename(trainingFolder + b.getName(i) + wavExt);
        
        return trainingSet;
    }
    //
    
    public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet) throws IOException
    {
        int numItems = Math.min(sourceTrainingSet.items.length, targetTrainingSet.items.length);
        if (numItems>0)
        {
            int [] map = new int[numItems]; 
            int i;
            
            for (i=0; i<numItems; i++)
                map[i] = i;
            
            train(sourceTrainingSet, targetTrainingSet, map);
        }
    }
    
    //<map> is a vector of same length as sourceItems showing the index of the corresponding target item 
    //  for each source item. This allows to specify the target files in any order, i.e. file names are not required to be in alphabetical order
    public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map) throws IOException
    {
        if (sourceTrainingSet.items!=null && targetTrainingSet.items!=null && map!=null)
        {
            int numItems = Math.min(sourceTrainingSet.items.length, sourceTrainingSet.items.length);
            numItems = Math.min(numItems, map.length);
            
            if (numItems>0)
            {
                preprocessor.run(sourceTrainingSet);
                preprocessor.run(targetTrainingSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.LSF_ANALYSIS +
                                      WeightedCodebookFeatureExtractor.F0_ANALYSIS;
                
                featureExtractor.run(sourceTrainingSet, params, desiredFeatures);
                featureExtractor.run(targetTrainingSet, params, desiredFeatures);
            }
            
            WeightedCodebookFeatureCollection fcol = collectFeatures(sourceTrainingSet, targetTrainingSet, map);

            learnMapping(fcol, sourceTrainingSet, targetTrainingSet, map);
            
            deleteTemporaryFiles(fcol, sourceTrainingSet, targetTrainingSet);
        }
    }

    //Parallel training, sourceItems and targetItems should have at least map.length elements (ensured if this function is called through train)
    public WeightedCodebookFeatureCollection collectFeatures(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        WeightedCodebookFeatureCollection fcol = new WeightedCodebookFeatureCollection(params, map.length);
        int i;
        IndexMap imap = null;
          
        if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfFramesMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                        sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfFrameGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             params.codebookHeader.numNeighboursInFrameGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfLabelsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                        sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfLabelGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             params.codebookHeader.numNeighboursInLabelGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
        {
            imap = AdaptationUtils.lsfSpeechMapping();
            try {
                imap.writeToFile(fcol.indexMapFiles[0]);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return fcol;
    }
    
    //This function generates the codebooks from training pairs
    public void learnMapping(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
            learnMappingFrames(fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
            learnMappingFrameGroups(fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
            learnMappingLabels(fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
            learnMappingLabelGroups(fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
            learnMappingSpeech(fcol, sourceTrainingSet, targetTrainingSet, map);
    }
    
    public void learnMappingFrames(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES;

        IndexMap imap = new IndexMap();
        int i, j;

        WeightedCodebookEntry codebookEntry = null;
        WeightedCodebookFile codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        codebookFile.writeCodebookHeader(params.codebookHeader);

        //Take directly the corresponding source-target frame LSF vectors and write them as a new entry
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("Training pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            try {
                imap.readFromFile(fcol.indexMapFiles[i]); //imap keeps information about a single source-target pair only
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (imap.files!=null && sourceTrainingSet.items.length>i && targetTrainingSet.items.length>i)
            {
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);
                
                if (srcLsfs.lsfs!=null && tgtLsfs.lsfs!=null)
                {
                    for (j=0; j<imap.files[0].indicesMap.length; j++) //j is the index for labels
                    {
                        if (srcLsfs.lsfs.length>imap.files[0].indicesMap[j][0] && tgtLsfs.lsfs.length>imap.files[0].indicesMap[j][1])
                        {
                            //Write to codebook file
                            codebookEntry = new WeightedCodebookEntry(srcLsfs.params.lpOrder);
                            codebookEntry.setLsfs(srcLsfs.lsfs[imap.files[0].indicesMap[j][0]],tgtLsfs.lsfs[imap.files[0].indicesMap[j][1]]);
                            codebookFile.writeEntry(codebookEntry);
                            //
                        }
                    }
                    
                    System.out.println("Frame pairs processed in file " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length));
                }
            } 
        }

        codebookFile.close();
    }
     
    public void learnMappingFrameGroups(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS;

        IndexMap imap = new IndexMap();
        int i, j, k, n, totalFrames;
        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;
        
        boolean bSourceOK = false;
        boolean bTargetOK = false;

        WeightedCodebookEntry codebookEntry = null;
        WeightedCodebookFile codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        codebookFile.writeCodebookHeader(params.codebookHeader);

        //Average neighbouring frame lsfs to obtain a smoother estimate of the source and target LSF vectors and write the averaged versions as a new entry
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("Training pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            try {
                imap.readFromFile(fcol.indexMapFiles[i]); //imap keeps information about a single source-target pair only
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (imap.files!=null && sourceTrainingSet.items.length>i && targetTrainingSet.items.length>i)
            {
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);
                
                if (i==0)
                {
                    meanSourceLsfs = new double[srcLsfs.params.lpOrder];
                    meanTargetLsfs = new double[tgtLsfs.params.lpOrder];
                }
                else
                {
                    if (meanSourceLsfs.length!=srcLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in source lsf file " + sourceTrainingSet.items[i].lsfFile);
                        return;
                    }

                    if (meanTargetLsfs.length!=tgtLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in target lsf file " + targetTrainingSet.items[map[i]].lsfFile);
                        return;
                    }
                }  
                
                if (srcLsfs.lsfs!=null && tgtLsfs.lsfs!=null)
                {
                    for (j=0; j<imap.files[0].indicesMap.length; j++) //j is the index for labels
                    {
                        Arrays.fill(meanSourceLsfs, 0.0);
                        Arrays.fill(meanTargetLsfs, 0.0);
                        
                        totalFrames = 0;
                        bSourceOK = false;
                        for (k=imap.files[0].indicesMap[j][0]; k<=imap.files[0].indicesMap[j][1]; k++)
                        {
                            if (k>=0 && k<srcLsfs.lsfs.length)
                            {
                                totalFrames++;
                                bSourceOK = true;
                                
                                for (n=0; n<srcLsfs.params.lpOrder; n++)
                                    meanSourceLsfs[n] += srcLsfs.lsfs[k][n];
                            }
                        }
                        
                        if (bSourceOK)
                        {
                            for (n=0; n<srcLsfs.params.lpOrder; n++)
                                meanSourceLsfs[n] /= totalFrames;
                            
                            totalFrames = 0;
                            bTargetOK = false;
                            for (k=imap.files[0].indicesMap[j][2]; k<=imap.files[0].indicesMap[j][3]; k++)
                            {
                                if (k>=0 && k<tgtLsfs.lsfs.length)
                                {
                                    totalFrames++;
                                    bTargetOK = true;
                                    
                                    for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                        meanTargetLsfs[n] += tgtLsfs.lsfs[k][n];
                                }
                            }
                            
                            if (bTargetOK)
                            {
                                for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                    meanTargetLsfs[n] /= totalFrames;

                                //Write to codebook file
                                codebookEntry = new WeightedCodebookEntry(meanSourceLsfs.length);
                                codebookEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);
                                codebookFile.writeEntry(codebookEntry);
                                //
                            }
                        }
                    }
                    
                    System.out.println("Frame pairs processed in file " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length));
                }
            } 
        }

        codebookFile.close();  
    }
    
    public void learnMappingLabels(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS;

        IndexMap imap = new IndexMap();
        int i, j, k, n, totalFrames;
        boolean bSourceOK = false;
        boolean bTargetOK = false;

        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;

        WeightedCodebookEntry codebookEntry = null;
        WeightedCodebookFile codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        codebookFile.writeCodebookHeader(params.codebookHeader);

        //Take an average of LSF vectors within each label pair and write the resulting vector as the state
        // average for source and target
        // To do: Weighting of vectors within each label according to some criteria
        //        on how typical they represent the current phoneme.
        //        This can be implemented by looking at some distance measure (eucledian, mahalonoibis, LSF, etc) 
        //        to the cluster mean (i.e. mean of all LSF vectors for this phoneme), for example.
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("Training pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            try {
                imap.readFromFile(fcol.indexMapFiles[i]); //imap keeps information about a single source-target pair only
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (imap.files!=null && sourceTrainingSet.items.length>i && targetTrainingSet.items.length>i)
            {
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);

                if (i==0)
                {
                    meanSourceLsfs = new double[srcLsfs.params.lpOrder];
                    meanTargetLsfs = new double[tgtLsfs.params.lpOrder];
                }
                else
                {
                    if (meanSourceLsfs.length!=srcLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in source lsf file " + sourceTrainingSet.items[i].lsfFile);
                        return;
                    }

                    if (meanTargetLsfs.length!=tgtLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in target lsf file " + targetTrainingSet.items[map[i]].lsfFile);
                        return;
                    }
                }  

                if (srcLsfs.lsfs!=null && tgtLsfs.lsfs!=null)
                {
                    for (j=0; j<imap.files[0].indicesMap.length; j++) //j is the index for labels
                    {
                        Arrays.fill(meanSourceLsfs, 0.0);
                        Arrays.fill(meanTargetLsfs, 0.0);

                        totalFrames = 0;
                        bSourceOK = false;
                        for (k=imap.files[0].indicesMap[j][0]; k<=imap.files[0].indicesMap[j][1]; k++)
                        {
                            if (k>=0 && k<srcLsfs.lsfs.length)
                            {
                                totalFrames++;
                                bSourceOK = true;

                                for (n=0; n<srcLsfs.params.lpOrder; n++)
                                    meanSourceLsfs[n] += srcLsfs.lsfs[k][n];
                            }
                        }

                        if (bSourceOK)
                        {
                            for (n=0; n<srcLsfs.params.lpOrder; n++)
                                meanSourceLsfs[n] /= totalFrames;

                            totalFrames = 0;
                            bTargetOK = false;
                            for (k=imap.files[0].indicesMap[j][2]; k<=imap.files[0].indicesMap[j][3]; k++)
                            {
                                if (k>=0 && k<tgtLsfs.lsfs.length)
                                {
                                    totalFrames++;
                                    bTargetOK = true;

                                    for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                        meanTargetLsfs[n] += tgtLsfs.lsfs[k][n];
                                }
                            }

                            if (bTargetOK)
                            {
                                for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                    meanTargetLsfs[n] /= totalFrames;

                                //Write to codebook file
                                codebookEntry = new WeightedCodebookEntry(meanSourceLsfs.length);
                                codebookEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);
                                codebookFile.writeEntry(codebookEntry);
                                //

                                System.out.println("Label pair " + String.valueOf(j+1) + " of " + String.valueOf(imap.files[0].indicesMap.length));
                            }
                        }
                    }
                }
            } 
        }

        codebookFile.close();
    }
    
    //This function is identical to learnMappingLabels since the mapping is performed accordingly in previous steps
    public void learnMappingLabelGroups(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        learnMappingLabels(fcol, sourceTrainingSet, targetTrainingSet, map);
    }
    
    public void learnMappingSpeech(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH;

        int i, j, n;
        
        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;

        WeightedCodebookEntry codebookEntry = null;
        WeightedCodebookFile codebookFile = new WeightedCodebookFile(params.codebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        codebookFile.writeCodebookHeader(params.codebookHeader);

        //Take an average of LSF vectors within each label pair and write the resulting vector as the state
        // average for source and target
        // To do: Weighting of vectors within each label according to some criteria
        //        on how typical they represent the current phoneme.
        //        This can be implemented by looking at some distance measure (eucledian, mahalonoibis, LSF, etc) 
        //        to the cluster mean (i.e. mean of all LSF vectors for this phoneme), for example.
        int totalFramesSrc = 0;
        boolean bSourceOK = false;
        int totalFramesTgt = 0;
        boolean bTargetOK = false;
        int lpOrderSrc = 0;
        int lpOrderTgt = 0;
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("Training pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            if (sourceTrainingSet.items.length>i)
            {
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);
                
                if (i==0)
                {
                    meanSourceLsfs = new double[srcLsfs.params.lpOrder];
                    meanTargetLsfs = new double[tgtLsfs.params.lpOrder];
                    Arrays.fill(meanSourceLsfs, 0.0);
                    Arrays.fill(meanTargetLsfs, 0.0);
                    lpOrderSrc = srcLsfs.params.lpOrder; 
                    lpOrderTgt = srcLsfs.params.lpOrder;
                }
                else
                {
                    if (meanSourceLsfs.length!=srcLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in source lsf file " + sourceTrainingSet.items[i].lsfFile);
                        return;
                    }
                    
                    if (meanTargetLsfs.length!=tgtLsfs.params.lpOrder)
                    {
                        System.out.println("Error! LSF vector size mismatch in target lsf file " + targetTrainingSet.items[map[i]].lsfFile);
                        return;
                    }
                }  

                if (srcLsfs.lsfs!=null)
                {
                    for (j=0; j<srcLsfs.params.numfrm; j++)
                    {
                        totalFramesSrc++;
                        bSourceOK = true;
                        for (n=0; n<lpOrderSrc; n++)
                            meanSourceLsfs[n] += srcLsfs.lsfs[j][n];
                    }
                }
                
                if (tgtLsfs.lsfs!=null)
                {
                    for (j=0; j<tgtLsfs.params.numfrm; j++)
                    {
                        totalFramesTgt++;
                        bTargetOK = true;
                        for (n=0; n<lpOrderTgt; n++)
                            meanTargetLsfs[n] += tgtLsfs.lsfs[j][n];
                    }
                }
            }
        } 
        
        if (bSourceOK)
        {
            for (n=0; n<lpOrderSrc; n++)
                meanSourceLsfs[n] /= totalFramesSrc;
        }
        
        if (bTargetOK)
        {
            for (n=0; n<lpOrderTgt; n++)
                meanTargetLsfs[n] /= totalFramesTgt;
        }  
        
        if (bSourceOK && bTargetOK)
        {
            //Write to codebook file
            codebookEntry = new WeightedCodebookEntry(meanSourceLsfs.length);
            codebookEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);
            codebookFile.writeEntry(codebookEntry);
            //
        }     
        
        codebookFile.close();
    }
    
    public void deleteTemporaryFiles(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet)
    {
        FileUtils.delete(fcol.indexMapFiles, true);
        FileUtils.delete(sourceTrainingSet.getLsfFiles(), true);
        FileUtils.delete(targetTrainingSet.getLsfFiles(), true);
    }
    
    public static void main(String[] args)
    {
        WeightedCodebookPreprocessor pp = new WeightedCodebookPreprocessor();
        WeightedCodebookFeatureExtractor fe = new WeightedCodebookFeatureExtractor();
        
        WeightedCodebookTrainerParams pa = new WeightedCodebookTrainerParams();
        
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAMES; //Frame-by-frame mapping of features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.FRAME_GROUPS; pa.codebookHeader.numNeighboursInFrameGroups = 2; //Mapping of frame average features (no label information but fixed amount of neighbouring frames is used)
        pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABELS; //Mapping of label average features
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.LABEL_GROUPS; pa.codebookHeader.numNeighboursInLabelGroups = 1; //Mapping of average features collected across label groups (i.e. vowels, consonants, etc)
        //pa.codebookHeader.codebookType = WeightedCodebookFileHeader.SPEECH; //Mapping of average features collected across all speech parts (i.e. like spectral equalization)

        pa.codebookHeader.sourceTag = "sourceL"; //Source name tag (i.e. style or speaker identity)
        pa.codebookHeader.targetTag = "targetL"; //Target name tag (i.e. style or speaker identity)

        pa.trainingBaseFolder = "d:\\1"; //Training base directory
        pa.sourceTrainingFolder = "d:\\1\\src"; //Source training folder
        pa.targetTrainingFolder = "d:\\1\\tgt"; //Target training folder

        pa.indexMapFileExtension = ".imf"; //Index map file extensions
        
        pa.codebookHeader.lsfParams.lpOrder = 0; //Auto set
        pa.codebookHeader.lsfParams.preCoef = 0.97f;
        pa.codebookHeader.lsfParams.skipsize = 0.010f;
        pa.codebookHeader.lsfParams.winsize = 0.020f;
        pa.codebookHeader.lsfParams.windowType = Window.HAMMING;
        
        pa.codebookFile = StringUtil.checkLastSlash(pa.trainingBaseFolder) + pa.codebookHeader.sourceTag + "_X_" + pa.codebookHeader.targetTag + WeightedCodebookFile.defaultExtension;

        WeightedCodebookParallelTrainer t = new WeightedCodebookParallelTrainer(pp, fe, pa);
        try {
            t.run();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("Training completed...");
    }
}
