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

package marytts.signalproc.adaptation.codebook;

import java.io.IOException;
import java.util.Arrays;

import marytts.signalproc.adaptation.BaselineAdaptationSet;
import marytts.signalproc.adaptation.Context;
import marytts.signalproc.adaptation.IndexMap;
import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.analysis.ESTLabels;
import marytts.signalproc.analysis.EnergyAnalyserRms;
import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.Lsfs;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookLsfMapper {
    private WeightedCodebookTrainerParams params;
    public WeightedCodebookLsfMapper(WeightedCodebookTrainerParams pa)
    {
        params = new WeightedCodebookTrainerParams(pa);
    }
    
    public void learnMappingFrames(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAMES;

        IndexMap imap = new IndexMap();
        int i, j, index;

        WeightedCodebookLsfEntry lsfEntry = null;

        boolean bHeaderWritten = false;

        //Take directly the corresponding source-target frame LSF vectors and write them as a new entry
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("LSF mapping for pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            try {
                imap.readFromFile(fcol.indexMapFiles[i]); //imap keeps information about a single source-target pair only
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (imap.files!=null && sourceTrainingSet.items.length>i && targetTrainingSet.items.length>i)
            {
                //Lsfs
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);
                //
                
                //Pitch: for outlier elimination not prosody modeling!
                F0ReaderWriter sourceF0s = new F0ReaderWriter(sourceTrainingSet.items[i].f0File);
                F0ReaderWriter targetF0s = new F0ReaderWriter(targetTrainingSet.items[map[i]].f0File);
                //
                
                //Duration
                ESTLabels sourceLabels = new ESTLabels(sourceTrainingSet.items[i].labelFile);
                ESTLabels targetLabels = new ESTLabels(targetTrainingSet.items[map[i]].labelFile);
                //
                
                //Energy
                EnergyAnalyserRms sourceEnergies = EnergyAnalyserRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
                EnergyAnalyserRms targetEnergies = EnergyAnalyserRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
                //
                
                if (!bHeaderWritten)
                {
                    params.codebookHeader.lsfParams.lpOrder = srcLsfs.params.lpOrder;
                    params.codebookHeader.lsfParams.samplingRate = srcLsfs.params.samplingRate;
                    
                    codebookFile.writeCodebookHeader(params.codebookHeader);
                    bHeaderWritten = true;
                }
                
                if (srcLsfs.lsfs!=null && tgtLsfs.lsfs!=null)
                {
                    for (j=0; j<imap.files[0].indicesMap.length; j++) //j is the index for labels
                    {
                        if (srcLsfs.lsfs.length>imap.files[0].indicesMap[j][0] && tgtLsfs.lsfs.length>imap.files[0].indicesMap[j][1])
                        {
                            //Write to codebook file
                            lsfEntry = new WeightedCodebookLsfEntry(srcLsfs.params.lpOrder);
                            lsfEntry.setLsfs(srcLsfs.lsfs[imap.files[0].indicesMap[j][0]], tgtLsfs.lsfs[imap.files[0].indicesMap[j][1]]);
                            
                            //Pitch
                            index = MathUtils.linearMap(imap.files[0].indicesMap[j][0], 0, srcLsfs.lsfs.length-1, 0, sourceF0s.contour.length-1);
                            lsfEntry.sourceItem.f0 = sourceF0s.contour[index];
                            index = MathUtils.linearMap(imap.files[0].indicesMap[j][1], 0, tgtLsfs.lsfs.length-1, 0, targetF0s.contour.length-1);
                            lsfEntry.targetItem.f0 = targetF0s.contour[index];
                            //
                            
                            //Duration & Phoneme
                            index = SignalProcUtils.frameIndex2LabelIndex(imap.files[0].indicesMap[j][0], sourceLabels, srcLsfs.params.winsize, srcLsfs.params.skipsize);
                            if (index>0)
                                lsfEntry.sourceItem.duration = sourceLabels.items[index].time-sourceLabels.items[index-1].time;
                            else
                                lsfEntry.sourceItem.duration = sourceLabels.items[index].time;
                            lsfEntry.sourceItem.phn = sourceLabels.items[index].phn;                            
                            lsfEntry.sourceItem.context = new Context(sourceLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                            
                            index = SignalProcUtils.frameIndex2LabelIndex(imap.files[0].indicesMap[j][1], targetLabels, tgtLsfs.params.winsize, tgtLsfs.params.skipsize);
                            if (index>0)  
                                lsfEntry.targetItem.duration = targetLabels.items[index].time-targetLabels.items[index-1].time;
                            else
                                lsfEntry.targetItem.duration = targetLabels.items[index].time;
                            lsfEntry.targetItem.phn = targetLabels.items[index].phn;
                            lsfEntry.targetItem.context = new Context(targetLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                            //
                            
                            //Energy
                            index = MathUtils.linearMap(imap.files[0].indicesMap[j][0], 0, srcLsfs.lsfs.length-1, 0, sourceEnergies.contour.length-1);
                            lsfEntry.sourceItem.energy = sourceEnergies.contour[index];
                            index = MathUtils.linearMap(imap.files[0].indicesMap[j][1], 0, tgtLsfs.lsfs.length-1, 0, targetEnergies.contour.length-1);
                            lsfEntry.targetItem.energy = targetEnergies.contour[index];
                            //
                            
                            codebookFile.writeLsfEntry(lsfEntry);
                            //
                        }
                    }
                    
                    System.out.println("Frame pairs processed in file " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length));
                }
            } 
        }
    }
    
    public void learnMappingFrameGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.FRAME_GROUPS;

        IndexMap imap = new IndexMap();
        int i, j, k, n, totalFrames, index;
        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;
        
        double sourceAverageF0;
        double targetAverageF0;
        double sourceAverageDuration;
        double targetAverageDuration;
        double sourceAverageEnergy;
        double targetAverageEnergy;
        int sourceTotalVoiceds;
        int targetTotalVoiceds;
        int sourceTotal;
        int targetTotal;
        String sourcePhn = "";
        String targetPhn = "";
        Context sourceContext = null;
        Context targetContext = null;
        int middle;
        
        boolean bSourceOK = false;
        boolean bTargetOK = false;

        WeightedCodebookLsfEntry lsfEntry = null;
 
        boolean bHeaderWritten = false;

        //Average neighbouring frame lsfs to obtain a smoother estimate of the source and target LSF vectors and write the averaged versions as a new entry
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("LSF mapping for pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

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
                
                //Pitch: for outlier elimination not prosody modeling!
                F0ReaderWriter sourceF0s = new F0ReaderWriter(sourceTrainingSet.items[i].f0File);
                F0ReaderWriter targetF0s = new F0ReaderWriter(targetTrainingSet.items[map[i]].f0File);
                //
                
                //Duration
                ESTLabels sourceLabels = new ESTLabels(sourceTrainingSet.items[i].labelFile);
                ESTLabels targetLabels = new ESTLabels(targetTrainingSet.items[map[i]].labelFile);
                //
                
                //Energy
                EnergyAnalyserRms sourceEnergies = EnergyAnalyserRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
                EnergyAnalyserRms targetEnergies = EnergyAnalyserRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
                //
                
                if (!bHeaderWritten)
                {
                    params.codebookHeader.lsfParams.lpOrder = srcLsfs.params.lpOrder;
                    params.codebookHeader.lsfParams.samplingRate = srcLsfs.params.samplingRate;
                    
                    codebookFile.writeCodebookHeader(params.codebookHeader);
                    bHeaderWritten = true;
                }
                
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
                        
                        sourceAverageF0 = 0.0;
                        targetAverageF0 = 0.0;
                        sourceAverageDuration = 0.0;
                        targetAverageDuration = 0.0;
                        sourceAverageEnergy = 0.0;
                        targetAverageEnergy = 0.0;
                        sourceTotalVoiceds = 0;
                        targetTotalVoiceds = 0;
                        sourceTotal = 0;
                        targetTotal = 0;
                        
                        totalFrames = 0;
                        bSourceOK = false;
                        middle = (int)Math.floor(0.5*(imap.files[0].indicesMap[j][0] + imap.files[0].indicesMap[j][1])+0.5);
                        for (k=imap.files[0].indicesMap[j][0]; k<=imap.files[0].indicesMap[j][1]; k++)
                        {
                            if (k>=0 && k<srcLsfs.lsfs.length)
                            {
                                totalFrames++;
                                bSourceOK = true;
                                
                                for (n=0; n<srcLsfs.params.lpOrder; n++)
                                    meanSourceLsfs[n] += srcLsfs.lsfs[k][n];
                                
                                //Pitch
                                index = MathUtils.linearMap(k, 0, srcLsfs.lsfs.length-1, 0, sourceF0s.contour.length-1);
                                if (sourceF0s.contour[index]>10.0)
                                {
                                    sourceAverageF0 += sourceF0s.contour[index];
                                    sourceTotalVoiceds++;
                                }
                                //
                                
                                //Duration
                                index = SignalProcUtils.frameIndex2LabelIndex(k, sourceLabels, srcLsfs.params.winsize, srcLsfs.params.skipsize);
                                if (index>0)
                                    sourceAverageDuration += sourceLabels.items[index].time-sourceLabels.items[index-1].time;
                                else
                                    sourceAverageDuration += sourceLabels.items[index].time;
                                //
                                
                                //Phoneme: Middle frames phonetic identity
                                if (k==middle)
                                {
                                    sourcePhn = sourceLabels.items[index].phn;
                                    sourceContext = new Context(sourceLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                                }
                                //
                                
                                //Energy
                                index = MathUtils.linearMap(k, 0, srcLsfs.lsfs.length-1, 0, sourceEnergies.contour.length-1);
                                sourceAverageEnergy += sourceEnergies.contour[index];
                                //
                                
                                sourceTotal++;
                            }
                        }
                        
                        if (bSourceOK)
                        {
                            for (n=0; n<srcLsfs.params.lpOrder; n++)
                                meanSourceLsfs[n] /= totalFrames;
                            
                            totalFrames = 0;
                            bTargetOK = false;
                            middle = (int)Math.floor(0.5*(imap.files[0].indicesMap[j][2] + imap.files[0].indicesMap[j][3])+0.5);
                            for (k=imap.files[0].indicesMap[j][2]; k<=imap.files[0].indicesMap[j][3]; k++)
                            {
                                if (k>=0 && k<tgtLsfs.lsfs.length)
                                {
                                    totalFrames++;
                                    bTargetOK = true;
                                    
                                    for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                        meanTargetLsfs[n] += tgtLsfs.lsfs[k][n];
                                    
                                    //Pitch
                                    index = MathUtils.linearMap(k, 0, tgtLsfs.lsfs.length-1, 0, targetF0s.contour.length-1);
                                    if (targetF0s.contour[index]>10.0)
                                    {
                                        targetAverageF0 += targetF0s.contour[index];
                                        targetTotalVoiceds++;
                                    }
                                    //
                                    
                                    //Duration
                                    index = SignalProcUtils.frameIndex2LabelIndex(k, targetLabels, tgtLsfs.params.winsize, tgtLsfs.params.skipsize);
                                    if (index>0)
                                        targetAverageDuration += targetLabels.items[index].time-targetLabels.items[index-1].time;
                                    else
                                        targetAverageDuration += targetLabels.items[index].time;
                                    //
                                    
                                    //Phoneme: Middle frames phonetic identity
                                    if (k==middle)
                                    {
                                        targetPhn = targetLabels.items[index].phn;
                                        targetContext = new Context(targetLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                                    }
                                    //
                                    
                                    //Energy
                                    index = MathUtils.linearMap(k, 0, tgtLsfs.lsfs.length-1, 0, targetEnergies.contour.length-1);
                                    targetAverageEnergy += targetEnergies.contour[index];
                                    //
                                    
                                    targetTotal++;
                                }
                            }
                            
                            if (bTargetOK)
                            {
                                for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                    meanTargetLsfs[n] /= totalFrames;

                                //Write to codebook file
                                lsfEntry = new WeightedCodebookLsfEntry(meanSourceLsfs.length);
                                lsfEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);
                                
                                //Pitch
                                if (sourceTotalVoiceds>0)
                                    sourceAverageF0 /= sourceTotalVoiceds;
                                if (targetTotalVoiceds>0)
                                    targetAverageF0 /= targetTotalVoiceds;
                                lsfEntry.sourceItem.f0 = sourceAverageF0;
                                lsfEntry.targetItem.f0 = targetAverageF0;
                                //
                                
                                //Duration
                                if (sourceTotal>0)
                                    sourceAverageDuration /= sourceTotal;
                                if (targetTotal>0)
                                    sourceAverageDuration /= targetTotal;
                                lsfEntry.sourceItem.duration = sourceAverageDuration;
                                lsfEntry.targetItem.duration = targetAverageDuration;
                                //
                                
                                //Phoneme
                                lsfEntry.sourceItem.phn = sourcePhn;
                                lsfEntry.targetItem.phn = targetPhn;
                                lsfEntry.sourceItem.context = new Context(sourceContext);
                                lsfEntry.targetItem.context = new Context(targetContext);
                                //
                                
                                //Energy
                                if (sourceTotal>0)
                                    sourceAverageEnergy /= sourceTotal;
                                if (targetTotal>0)
                                    targetAverageEnergy /= targetTotal;
                                lsfEntry.sourceItem.energy = sourceAverageEnergy;
                                lsfEntry.targetItem.energy = targetAverageEnergy;
                                //

                                codebookFile.writeLsfEntry(lsfEntry);
                                //
                            }
                        }
                    }
                    
                    System.out.println("Frame pairs processed in file " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length));
                }
            } 
        }
    }
    
    public void learnMappingLabels(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.LABELS;

        IndexMap imap = new IndexMap();
        int i, j, k, n, totalFrames, index;
        boolean bSourceOK = false;
        boolean bTargetOK = false;

        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;
        
        double sourceAverageF0;
        double targetAverageF0;
        double sourceAverageDuration;
        double targetAverageDuration;
        double sourceAverageEnergy;
        double targetAverageEnergy;
        int sourceTotalVoiceds;
        int targetTotalVoiceds;
        int sourceTotal;
        int targetTotal;
        String sourcePhn = "";
        String targetPhn = "";
        Context sourceContext = null;
        Context targetContext = null;
        int middle;

        WeightedCodebookLsfEntry lsfEntry = null;

        boolean bHeaderWritten = false;

        //Take an average of LSF vectors within each label pair and write the resulting vector as the state
        // average for source and target
        // To do: Weighting of vectors within each label according to some criteria
        //        on how typical they represent the current phoneme.
        //        This can be implemented by looking at some distance measure (eucledian, mahalonoibis, LSF, etc) 
        //        to the cluster mean (i.e. mean of all LSF vectors for this phoneme), for example.
        for (i=0; i<fcol.indexMapFiles.length; i++)
        {
            System.out.println("LSF mapping for pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

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
                
                //Pitch: for outlier elimination not prosody modeling!
                F0ReaderWriter sourceF0s = new F0ReaderWriter(sourceTrainingSet.items[i].f0File);
                F0ReaderWriter targetF0s = new F0ReaderWriter(targetTrainingSet.items[map[i]].f0File);
                //
                
                //Duration
                ESTLabels sourceLabels = new ESTLabels(sourceTrainingSet.items[i].labelFile);
                ESTLabels targetLabels = new ESTLabels(targetTrainingSet.items[map[i]].labelFile);
                //
                
                //Energy
                EnergyAnalyserRms sourceEnergies = EnergyAnalyserRms.ReadEnergyFile(sourceTrainingSet.items[i].energyFile);
                EnergyAnalyserRms targetEnergies = EnergyAnalyserRms.ReadEnergyFile(targetTrainingSet.items[map[i]].energyFile);
                //
                
                if (!bHeaderWritten)
                {
                    params.codebookHeader.lsfParams.lpOrder = srcLsfs.params.lpOrder;
                    params.codebookHeader.lsfParams.samplingRate =  srcLsfs.params.samplingRate;
                    
                    codebookFile.writeCodebookHeader(params.codebookHeader);
                    bHeaderWritten = true;
                }

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
                        
                        sourceAverageF0 = 0.0;
                        targetAverageF0 = 0.0;
                        sourceAverageDuration = 0.0;
                        targetAverageDuration = 0.0;
                        sourceAverageEnergy = 0.0;
                        targetAverageEnergy = 0.0;
                        sourceTotalVoiceds = 0;
                        targetTotalVoiceds = 0;
                        sourceTotal = 0;
                        targetTotal = 0;

                        totalFrames = 0;
                        bSourceOK = false;
                        middle = (int)Math.floor(0.5*(imap.files[0].indicesMap[j][0] + imap.files[0].indicesMap[j][1])+0.5);
                        for (k=imap.files[0].indicesMap[j][0]; k<=imap.files[0].indicesMap[j][1]; k++)
                        {
                            if (k>=0 && k<srcLsfs.lsfs.length)
                            {
                                totalFrames++;
                                bSourceOK = true;

                                for (n=0; n<srcLsfs.params.lpOrder; n++)
                                    meanSourceLsfs[n] += srcLsfs.lsfs[k][n];
                                
                                //Pitch
                                index = MathUtils.linearMap(k, 0, srcLsfs.lsfs.length-1, 0, sourceF0s.contour.length-1);
                                if (sourceF0s.contour[index]>10.0)
                                {
                                    sourceAverageF0 += sourceF0s.contour[index];
                                    sourceTotalVoiceds++;
                                }
                                //
                                
                                //Duration
                                index = SignalProcUtils.frameIndex2LabelIndex(k, sourceLabels, srcLsfs.params.winsize, srcLsfs.params.skipsize);
                                if (index>0)
                                    sourceAverageDuration += sourceLabels.items[index].time-sourceLabels.items[index-1].time;
                                else
                                    sourceAverageDuration += sourceLabels.items[index].time;
                                //
                                
                                //Phoneme: Middle frames phonetic identity
                                if (k==middle)
                                {
                                    sourcePhn = sourceLabels.items[index].phn;
                                    sourceContext = new Context(sourceLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                                }
                                //
                                
                                //Energy
                                index = MathUtils.linearMap(k, 0, srcLsfs.lsfs.length-1, 0, sourceEnergies.contour.length-1);
                                sourceAverageEnergy += sourceEnergies.contour[index];
                                //
                                
                                sourceTotal++;
                            }
                        }

                        if (bSourceOK)
                        {
                            for (n=0; n<srcLsfs.params.lpOrder; n++)
                                meanSourceLsfs[n] /= totalFrames;

                            totalFrames = 0;
                            bTargetOK = false;
                            middle = (int)Math.floor(0.5*(imap.files[0].indicesMap[j][2] + imap.files[0].indicesMap[j][3])+0.5);
                            for (k=imap.files[0].indicesMap[j][2]; k<=imap.files[0].indicesMap[j][3]; k++)
                            {
                                if (k>=0 && k<tgtLsfs.lsfs.length)
                                {
                                    totalFrames++;
                                    bTargetOK = true;

                                    for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                        meanTargetLsfs[n] += tgtLsfs.lsfs[k][n];
                                    
                                    //Pitch
                                    index = MathUtils.linearMap(k, 0, tgtLsfs.lsfs.length-1, 0, targetF0s.contour.length-1);
                                    if (targetF0s.contour[index]>10.0)
                                    {
                                        targetAverageF0 += targetF0s.contour[index];
                                        targetTotalVoiceds++;
                                    }
                                    //
                                    
                                    //Duration
                                    index = SignalProcUtils.frameIndex2LabelIndex(k, targetLabels, tgtLsfs.params.winsize, tgtLsfs.params.skipsize);
                                    if (index>0)
                                        targetAverageDuration += targetLabels.items[index].time-targetLabels.items[index-1].time;
                                    else
                                        targetAverageDuration += targetLabels.items[index].time;
                                    //
                                    
                                    //Phoneme: Middle frames phonetic identity
                                    if (k==middle)
                                    {
                                        targetPhn = targetLabels.items[index].phn;
                                        targetContext = new Context(targetLabels, index, WeightedCodebookTrainerParams.MAXIMUM_CONTEXT);
                                    }
                                    //
                                    
                                    //Energy
                                    index = MathUtils.linearMap(k, 0, tgtLsfs.lsfs.length-1, 0, targetEnergies.contour.length-1);
                                    targetAverageEnergy += targetEnergies.contour[index];
                                    //
                                    
                                    targetTotal++;
                                }
                            }

                            if (bTargetOK)
                            {
                                for (n=0; n<tgtLsfs.params.lpOrder; n++)
                                    meanTargetLsfs[n] /= totalFrames;

                                //Write to codebook file
                                lsfEntry = new WeightedCodebookLsfEntry(meanSourceLsfs.length);
                                lsfEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);

                                //Pitch
                                if (sourceTotalVoiceds>0)
                                    sourceAverageF0 /= sourceTotalVoiceds;
                                if (targetTotalVoiceds>0)
                                    targetAverageF0 /= targetTotalVoiceds;
                                lsfEntry.sourceItem.f0 = sourceAverageF0;
                                lsfEntry.targetItem.f0 = targetAverageF0;
                                //

                                //Duration
                                if (sourceTotal>0)
                                    sourceAverageDuration /= sourceTotal;
                                if (targetTotal>0)
                                    sourceAverageDuration /= targetTotal;
                                lsfEntry.sourceItem.duration = sourceAverageDuration;
                                lsfEntry.targetItem.duration = targetAverageDuration;
                                //

                                //Phoneme
                                lsfEntry.sourceItem.phn = sourcePhn;
                                lsfEntry.targetItem.phn = targetPhn;
                                lsfEntry.sourceItem.context = new Context(sourceContext);
                                lsfEntry.targetItem.context = new Context(targetContext);
                                //
                                
                                //Energy
                                if (sourceTotal>0)
                                    sourceAverageEnergy /= sourceTotal;
                                if (targetTotal>0)
                                    targetAverageEnergy /= targetTotal;
                                lsfEntry.sourceItem.energy = sourceAverageEnergy;
                                lsfEntry.targetItem.energy = targetAverageEnergy;
                                //
                                
                                codebookFile.writeLsfEntry(lsfEntry);
                                //

                                System.out.println("Label pair " + String.valueOf(j+1) + " of " + String.valueOf(imap.files[0].indicesMap.length));
                            }
                        }
                    }
                }
            } 
        }
    }
    
    //This function is identical to learnMappingLabels since the mapping is performed accordingly in previous steps
    public void learnMappingLabelGroups(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
         learnMappingLabels(codebookFile, fcol, sourceTrainingSet, targetTrainingSet, map);
    }
    
    public void learnMappingSpeech(WeightedCodebookFile codebookFile, WeightedCodebookFeatureCollection fcol, BaselineAdaptationSet sourceTrainingSet, BaselineAdaptationSet targetTrainingSet, int [] map)
    {
        assert params.codebookHeader.codebookType==WeightedCodebookFileHeader.SPEECH;

        int i, j, n;
        
        double [] meanSourceLsfs = null;
        double [] meanTargetLsfs = null;

        WeightedCodebookLsfEntry lsfEntry = null;
       
        boolean bHeaderWritten = false;

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
            System.out.println("LSF mapping for pair " + String.valueOf(i+1) + " of " + String.valueOf(fcol.indexMapFiles.length) + ":");

            if (sourceTrainingSet.items.length>i)
            {
                Lsfs srcLsfs = new Lsfs(sourceTrainingSet.items[i].lsfFile);
                Lsfs tgtLsfs = new Lsfs(targetTrainingSet.items[map[i]].lsfFile);
                
                if (!bHeaderWritten)
                {
                    params.codebookHeader.lsfParams.lpOrder = srcLsfs.params.lpOrder;
                    params.codebookHeader.lsfParams.samplingRate = srcLsfs.params.samplingRate;
                    
                    codebookFile.writeCodebookHeader(params.codebookHeader);
                    bHeaderWritten = true;
                }
                
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
            lsfEntry = new WeightedCodebookLsfEntry(meanSourceLsfs.length);
            lsfEntry.setLsfs(meanSourceLsfs, meanTargetLsfs);
            codebookFile.writeLsfEntry(lsfEntry);
            //
        }     
    }
}
