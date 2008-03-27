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

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.adaptation.AdaptationUtils;
import de.dfki.lt.mary.unitselection.adaptation.BaselineAdaptationSet;
import de.dfki.lt.mary.unitselection.adaptation.BaselineTrainer;
import de.dfki.lt.mary.unitselection.adaptation.FeatureCollection;
import de.dfki.lt.mary.unitselection.adaptation.IndexMap;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchTrainer;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.util.StringUtil;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookTrainer extends BaselineTrainer {
    
    public WeightedCodebookPreprocessor preprocessor;
    public WeightedCodebookFeatureExtractor featureExtractor;
    public WeightedCodebookOutlierEliminator outlierEliminator;
    public WeightedCodebookTrainerParams wcParams;
    
    public WeightedCodebookTrainer(WeightedCodebookPreprocessor pp,
            WeightedCodebookFeatureExtractor fe, 
            WeightedCodebookTrainerParams pa) 
    {
        preprocessor = new WeightedCodebookPreprocessor(pp);
        featureExtractor = new WeightedCodebookFeatureExtractor(fe);
        wcParams = new WeightedCodebookTrainerParams(pa);
        outlierEliminator = new WeightedCodebookOutlierEliminator();
    }
    
    //Call this function after initializing the trainer to perform training
    public void run() throws IOException, UnsupportedAudioFileException
    {
        if (checkParams())
        {
            BaselineAdaptationSet sourceTrainingSet = getTrainingSet(wcParams.sourceTrainingFolder);
            BaselineAdaptationSet targetTrainingSet = getTrainingSet(wcParams.targetTrainingFolder);
            
            int[] map = getIndexedMapping(sourceTrainingSet, targetTrainingSet);
            
            train(sourceTrainingSet, targetTrainingSet, map);
        }
    }
    
    //Validate parameters
    public boolean checkParams()
    {
        boolean bContinue = true;
        
        wcParams.trainingBaseFolder = StringUtil.checkLastSlash(wcParams.trainingBaseFolder);
        wcParams.sourceTrainingFolder = StringUtil.checkLastSlash(wcParams.sourceTrainingFolder);
        wcParams.targetTrainingFolder = StringUtil.checkLastSlash(wcParams.targetTrainingFolder);
        
        FileUtils.createDirectory(wcParams.trainingBaseFolder);
        
        if (!FileUtils.exists(wcParams.trainingBaseFolder) || !FileUtils.isDirectory(wcParams.trainingBaseFolder))
        {
            System.out.println("Error! Training base folder " + wcParams.trainingBaseFolder + " not found.");
            bContinue = false;
        }
        
        if (!FileUtils.exists(wcParams.sourceTrainingFolder) || !FileUtils.isDirectory(wcParams.sourceTrainingFolder))
        {
            System.out.println("Error! Source training folder " + wcParams.sourceTrainingFolder + " not found.");
            bContinue = false;
        }
        
        if (!FileUtils.exists(wcParams.targetTrainingFolder) || !FileUtils.isDirectory(wcParams.targetTrainingFolder))
        {
            System.out.println("Error! Target training folder " + wcParams.targetTrainingFolder + " not found.");
            bContinue = false;
        }
        
        wcParams.temporaryCodebookFile = wcParams.codebookFile + ".temp";
        
        return bContinue;
    }
    
    //General purpose training with indexed pairs
    // <map> is a vector of same length as sourceItems showing the index of the corresponding target item 
    //  for each source item. This allows to specify the target files in any order, i.e. file names are not required to be in alphabetical order
    public void train(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map) throws IOException, UnsupportedAudioFileException
    {
        if (sourceTrainingSet.items!=null && targetTrainingSet.items!=null && map!=null)
        {
            int numItems = Math.min(sourceTrainingSet.items.length, sourceTrainingSet.items.length);
            numItems = Math.min(numItems, map.length);
            
            if (numItems>0)
            {
                preprocessor.run(sourceTrainingSet);
                preprocessor.run(targetTrainingSet);
                
                int desiredFeatures = WeightedCodebookFeatureExtractor.LSF_FEATURES +
                                      WeightedCodebookFeatureExtractor.F0_FEATURES + 
                                      WeightedCodebookFeatureExtractor.ENERGY_FEATURES;
                
                featureExtractor.run(sourceTrainingSet, wcParams, desiredFeatures);
                featureExtractor.run(targetTrainingSet, wcParams, desiredFeatures);
            }
            
            WeightedCodebookFeatureCollection fcol = collectFeatures(sourceTrainingSet, targetTrainingSet, map);

            learnMapping(fcol, sourceTrainingSet, targetTrainingSet, map);
            
            outlierEliminator.run(wcParams);
            
            deleteTemporaryFiles(fcol, sourceTrainingSet, targetTrainingSet);
        }
    }

    //For parallel training, sourceItems and targetItems should have at least map.length elements (ensured if this function is called through train)
    public WeightedCodebookFeatureCollection collectFeatures(BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        WeightedCodebookFeatureCollection fcol = new WeightedCodebookFeatureCollection(wcParams, map.length);
        int i;
        IndexMap imap = null;
          
        if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
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
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfFrameGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             wcParams.codebookHeader.numNeighboursInFrameGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
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
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
        {
            for (i=0; i<map.length; i++)
            {
                imap = AdaptationUtils.lsfLabelGroupsMapping(sourceTrainingSet.items[i].labelFile, targetTrainingSet.items[map[i]].labelFile, 
                                                             sourceTrainingSet.items[i].lsfFile, targetTrainingSet.items[map[i]].lsfFile,
                                                             wcParams.codebookHeader.numNeighboursInLabelGroups);
                try {
                    imap.writeToFile(fcol.indexMapFiles[i]);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
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
    
    public void learnMapping(FeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert fcol instanceof WeightedCodebookFeatureCollection;
        
        learnMapping(fcol, sourceTrainingSet, targetTrainingSet, map);
    }
    
    //This function generates the codebooks from training pairs
    public void learnMapping(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        WeightedCodebookLsfMapper lsfMapper = new WeightedCodebookLsfMapper(wcParams);
        WeightedCodebookFile temporaryCodebookFile = new WeightedCodebookFile(wcParams.temporaryCodebookFile, WeightedCodebookFile.OPEN_FOR_WRITE);

        if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES)
            lsfMapper.learnMappingFrames(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS)
            lsfMapper.learnMappingFrameGroups(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS)
            lsfMapper.learnMappingLabels(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.LABEL_GROUPS)
            lsfMapper.learnMappingLabelGroups(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        else if (wcParams.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH)
            lsfMapper.learnMappingSpeech(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        
        PitchTrainer ptcTrainer = new PitchTrainer(wcParams);
        ptcTrainer.learnMapping(temporaryCodebookFile, (WeightedCodebookFeatureCollection)fcol, sourceTrainingSet, targetTrainingSet, map);
        
        temporaryCodebookFile.close();
    }
    
    public void deleteTemporaryFiles(WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet)
    {
        FileUtils.delete(fcol.indexMapFiles, true);
        //FileUtils.delete(sourceTrainingSet.getLsfFiles(), true);
        //FileUtils.delete(targetTrainingSet.getLsfFiles(), true);
        //FileUtils.delete(sourceTrainingSet.getF0Files(), true);
        //FileUtils.delete(targetTrainingSet.getF0Files(), true);
        //FileUtils.delete(sourceTrainingSet.getEnergyFiles(), true);
        //FileUtils.delete(targetTrainingSet.getEnergyFiles(), true);
        
        FileUtils.delete(wcParams.temporaryCodebookFile);
    }
}
