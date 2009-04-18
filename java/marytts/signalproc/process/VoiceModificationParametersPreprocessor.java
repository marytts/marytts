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
package marytts.signalproc.process;

import java.util.Arrays;

import marytts.signalproc.adaptation.BaselineTransformerParams;
import marytts.signalproc.adaptation.prosody.ProsodyTransformerParams;
import marytts.signalproc.analysis.AlignmentData;
import marytts.signalproc.analysis.F0ReaderWriter;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.FestivalUtt;
import marytts.signalproc.analysis.Labels;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;
import marytts.util.MaryUtils;

/**
 * @author Oytun T&uumlrk
 *
 */
public class VoiceModificationParametersPreprocessor extends VoiceModificationParameters 
{    
    public double [] pscalesVar;
    public double [] tscalesVar;
    public double [] escalesVar;
    public double [] vscalesVar;
    
    public double tscaleSingle;
    public int numPeriods;

    public VoiceModificationParametersPreprocessor(int samplingRate, int LPOrder,
                                                   double[] pscalesIn, 
                                                   double[] tscalesIn, 
                                                   double[] escalesIn, 
                                                   double[] vscalesIn, 
                                                   int[] pitchMarksIn, 
                                                   double wsFixedIn, double ssFixedIn, 
                                                   int numfrm, int numfrmFixed, int numPeriodsIn, 
                                                   boolean isFixedRate) 
    {
        super(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
        
        initialise(pitchMarksIn, wsFixedIn, ssFixedIn, numfrm, numfrmFixed, numPeriodsIn, isFixedRate);
    }
    
    //To do: Handle all isPscaleFromFestivalUttFile, isTscaleFromFestivalUttFile, isEscaleFromTargetWavFile,
    //       requests separately. Currently, there is no isEscaleFromTargetWavFile support
    //       and no support for using isPscaleFromFestivalUttFile but not isTscaleFromFestivalUttFile
    //       and vice versa.
    //       This constructor should also be combined with the above constructor
    //       which takes user specified scaling factors.
    //       Therefore, in the final version the user can request all variations,
    //       i.e. pscale as in the utt file with some additional scaling or shifting, 
    //       escale using only scale values provided by the user, etc
    public VoiceModificationParametersPreprocessor(String sourcePitchFile,
                                                   boolean isF0File,
                                                   String sourceLabelFile, 
                                                   String sourceEnergyFile, //only required for escales
                                                   String targetPitchFile, //only required for copy pitch synthesis
                                                   String targetEnergyFile, //only required for escales
                                                   boolean isPitchFromTargetFile, 
                                                   int pitchFromTargetMethod,
                                                   boolean isDurationFromTargetFile,
                                                   boolean isEnergyFromTargetFile,
                                                   int targetAlignmentFileType,
                                                   String targetAlignmentFile, 
                                                   int[] pitchMarks, 
                                                   double wsFixed, double ssFixed, 
                                                   int numfrmIn, int numfrmFixedIn, int numPeriodsIn, 
                                                   boolean isFixedRate)
    {
        super();
        
        numPeriods = numPeriodsIn;
        
        //These are not implemented!!! To do later after Interspeech 2008 paper
        //escalesVar from sourceLabelFile, sourceEnergyFile, targetLabelFile, targetEnergyFile
        //vscalesVar from vscalesIn
        
        //Read from files (only necessary ones, you will need to read more when implementing escales etc)
        AlignmentData ad = null;
        if (isPitchFromTargetFile || isDurationFromTargetFile)
        {
            if (FileUtils.exists(targetAlignmentFile))
            {
                if (targetAlignmentFileType==BaselineTransformerParams.LABELS)
                    ad = new Labels(targetAlignmentFile);
                else if (targetAlignmentFileType==BaselineTransformerParams.FESTIVAL_UTT)
                    ad = new FestivalUtt(targetAlignmentFile);
            }
        }
        
        PitchReaderWriter sourceF0s = null;
        if (isF0File)
            sourceF0s = new F0ReaderWriter(sourcePitchFile);
        else
            sourceF0s = new PitchReaderWriter(sourcePitchFile);
        
        Labels sourceLabels = new Labels(sourceLabelFile);
        
        PitchReaderWriter targetF0s = null;
        if (targetPitchFile!=null && FileUtils.exists(targetPitchFile))
        {
            if (isF0File)
                targetF0s = new F0ReaderWriter(targetPitchFile);
            else
                targetF0s = new PitchReaderWriter(targetPitchFile);
        }
        
        //MaryUtils.plot(sourceF0s.contour);
        //MaryUtils.plot(targetF0s.contour);
        
        //Find pscalesVar and tscalesVar from targetFestivalUttFile, sourcePitchFile, sourceLabelFile
        tscaleSingle=-1;

        //Determine the pitch and time scaling factors corresponding to each pitch synchronous frame
        pscalesVar = MathUtils.ones(numfrmIn);
        double[] sourceMappedF0s = MathUtils.zeros(numfrmIn);
        double[] targetMappedF0s = MathUtils.zeros(numfrmIn);
        tscalesVar = MathUtils.ones(numfrmIn);
        escalesVar = MathUtils.ones(numfrmIn);
        vscalesVar = MathUtils.ones(numfrmIn);
        boolean[] voiceds = new boolean[numfrmIn];
        Arrays.fill(voiceds, false);
        
        int i;
        double tSource, tTarget;
        int sourceLabInd, targetDurationLabInd, targetPitchLabInd, sourcePitchInd, targetPitchInd;
        double sourceDuration, targetDuration, sourcePitch, targetPitch;
        double sourceLocationInLabelPercent;
        
        //Find the optimum alignment between the source and the target labels since the phoneme sequences may not be identical due to silence periods etc.
        int[][] durationMap = null;
        Labels targetDurationLabels = null;
        Labels targetPitchLabels = null;
        
        if (ad!=null)
        {
            if (ad instanceof FestivalUtt)
            {
                for (i=0; i<((FestivalUtt)ad).labels.length; i++)
                {
                    if (((FestivalUtt)ad).keys[i].compareTo("==Segment==")==0 && durationMap==null)
                    {
                        durationMap = StringUtils.alignLabels(sourceLabels.items, ((FestivalUtt)ad).labels[i].items);
                        targetDurationLabels = new Labels(((FestivalUtt)ad).labels[i]);
                    }
                    else if (((FestivalUtt)ad).keys[i].compareTo("==Target==")==0)
                        targetPitchLabels = new Labels(((FestivalUtt)ad).labels[i]);
                }
            }
            else if (ad instanceof Labels)
            {
                durationMap = StringUtils.alignLabels(sourceLabels.items, ((Labels)ad).items);
                targetDurationLabels = new Labels((Labels)ad);
                targetPitchLabels = new Labels((Labels)ad);
            }
        }
        //
        
        double[] modifiedContour = new double[numfrmIn];
        
        if (durationMap!=null && targetDurationLabels!=null && targetPitchLabels!=null)
        {
            for (i=0; i<numfrmIn; i++)
            {
                if (!isFixedRate)
                    tSource = (0.5*(pitchMarks[i+numPeriods]+pitchMarks[i]))/fs;
                else
                    tSource = i*ssFixed+0.5*wsFixed;

                sourceLabInd = SignalProcUtils.time2LabelIndex(tSource, sourceLabels);
                if (sourceLabInd>0)
                {
                    sourceDuration = sourceLabels.items[sourceLabInd].time-sourceLabels.items[sourceLabInd-1].time;
                    sourceLocationInLabelPercent = (tSource-sourceLabels.items[sourceLabInd-1].time)/sourceDuration;
                }
                else
                {
                    sourceDuration = sourceLabels.items[sourceLabInd].time;
                    sourceLocationInLabelPercent = tSource/sourceLabels.items[sourceLabInd].time;
                }

                targetDurationLabInd = StringUtils.findInMap(durationMap, sourceLabInd);
                if (targetDurationLabInd>0)
                    targetDuration = targetDurationLabels.items[targetDurationLabInd].time-targetDurationLabels.items[targetDurationLabInd-1].time;
                else
                    targetDuration = targetDurationLabels.items[targetDurationLabInd].time;
                
                if (isDurationFromTargetFile && targetDurationLabInd>=0)
                    tscalesVar[i] = targetDuration/sourceDuration;
                else
                    tscalesVar[i] = 1.0;

                tTarget = -1.0;
                targetPitch = 0.0;
                sourcePitch = 0.0;
                pscalesVar[i] = 1.0;
                if (isPitchFromTargetFile)
                {
                    sourcePitchInd = SignalProcUtils.time2frameIndex(tSource, sourceF0s.header.ws, sourceF0s.header.ss);
                    if (sourcePitchInd>sourceF0s.header.numfrm-1)
                        sourcePitchInd=sourceF0s.header.numfrm-1;
                    sourcePitch = sourceF0s.contour[sourcePitchInd];
                    if (sourcePitch>10.0)
                        voiceds[i] = true;

                    if (ad instanceof FestivalUtt)
                    {
                        tTarget = tSource;
                        targetPitchLabInd = SignalProcUtils.time2LabelIndex(tTarget, targetPitchLabels);
                        if (targetPitchLabInd>0)
                        {

                            targetPitch = MathUtils.linearMap(tTarget, 
                                    targetPitchLabels.items[targetPitchLabInd-1].time, 
                                    targetPitchLabels.items[targetPitchLabInd].time, 
                                    targetPitchLabels.items[targetPitchLabInd-1].valuesRest[0],
                                    targetPitchLabels.items[targetPitchLabInd].valuesRest[0]);
                        }
                        else
                            targetPitch = targetPitchLabels.items[targetPitchLabInd].valuesRest[0];
                    }
                    else if (ad instanceof Labels) //Pitch comes from a target pitch contour
                    {
                        if (targetF0s!=null)
                        {
                            if (targetDurationLabInd>0)
                                tTarget = targetDurationLabels.items[targetDurationLabInd-1].time+sourceLocationInLabelPercent*targetDuration;
                            else
                                tTarget = sourceLocationInLabelPercent*targetDuration;

                            targetPitchInd = SignalProcUtils.time2frameIndex(tTarget, targetF0s.header.ws, targetF0s.header.ss);
                            targetPitchInd = MathUtils.CheckLimits(targetPitchInd, 0, targetF0s.contour.length-1);
                            targetPitch = targetF0s.contour[targetPitchInd];
                        }
                        else
                            targetPitch = sourcePitch;
                    }
                    
                    sourceMappedF0s[i] = sourcePitch;
                    targetMappedF0s[i] = targetPitch;

                    if (pitchFromTargetMethod==ProsodyTransformerParams.FULL_CONTOUR)
                    {
                        if (targetPitch>10.0 && sourcePitch>10.0)
                            pscalesVar[i] = targetPitch/sourcePitch;
                        else
                            pscalesVar[i] = 1.0;
                    }
                }

                System.out.println("SLab=" + sourceLabels.items[sourceLabInd].phn + " TLab=" + targetDurationLabels.items[targetDurationLabInd].phn + " STime=" + String.valueOf(tSource) + " TTime=" + String.valueOf(tTarget) + " SPtich=" + sourcePitch + " TPitch=" + targetPitch + " ps=" + String.valueOf(pscalesVar[i])+ " ts=" + String.valueOf(tscalesVar[i]));
            }
            
            if (pitchFromTargetMethod==ProsodyTransformerParams.FULL_CONTOUR)
            {
                int smootherLen = 4;
                //pscalesVar = SignalProcUtils.meanFilter(pscalesVar, smootherLen);
                //pscalesVar = SignalProcUtils.shift(pscalesVar, (int)Math.floor(0.5*smootherLen));
                for (i=0; i<numfrmIn; i++)
                {
                    if (!voiceds[i])
                        pscalesVar[i] = 1.0;

                    pscalesVar[i] = Math.max(pscalesVar[i], BaselineTransformerParams.MINIMUM_ALLOWED_PITCH_SCALE);
                    pscalesVar[i] = Math.min(pscalesVar[i], BaselineTransformerParams.MAXIMUM_ALLOWED_PITCH_SCALE);

                }

                //tscalesVar = SignalProcUtils.meanFilter(tscalesVar, smootherLen);
                //tscalesVar = SignalProcUtils.shift(tscalesVar, (int)Math.floor(0.5*smootherLen));
                for (i=0; i<numfrmIn; i++)
                {                
                    tscalesVar[i] = Math.max(tscalesVar[i], BaselineTransformerParams.MINIMUM_ALLOWED_TIME_SCALE);
                    tscalesVar[i] = Math.min(tscalesVar[i], BaselineTransformerParams.MAXIMUM_ALLOWED_TIME_SCALE);
                }
            }
            else if (pitchFromTargetMethod==ProsodyTransformerParams.SENTENCE_MEAN || pitchFromTargetMethod==ProsodyTransformerParams.SENTENCE_MEAN_STDDEV)
            {
                double[] sourceVoicedF0s = MathUtils.findValues(sourceF0s.contour, MathUtils.GREATER_THAN, 10.0);
                double[] targetVoicedF0s = MathUtils.findValues(targetF0s.contour, MathUtils.GREATER_THAN, 10.0);
                
                double sourceF0Mean = MathUtils.mean(sourceVoicedF0s);
                double targetF0Mean = MathUtils.mean(targetVoicedF0s);
                
                if (pitchFromTargetMethod==ProsodyTransformerParams.SENTENCE_MEAN_STDDEV)
                {
                    double sourceF0Std = MathUtils.standardDeviation(sourceVoicedF0s, sourceF0Mean);
                    double targetF0Std = MathUtils.standardDeviation(targetVoicedF0s, targetF0Mean);
                    
                    for (i=0; i<numfrmIn; i++)
                    {
                        pscalesVar[i] = 1.0;
                        if (sourceMappedF0s[i]>10.0 && targetMappedF0s[i]>10.0)
                        {
                            double tF0 = ((sourceMappedF0s[i]-sourceF0Mean)/sourceF0Std)*targetF0Std+targetF0Mean;
                            pscalesVar[i] = tF0/sourceMappedF0s[i];
                        }  
                    }     
                }
                else
                {
                    for (i=0; i<numfrmIn; i++)
                    {
                        pscalesVar[i] = 1.0;
                        if (sourceMappedF0s[i]>10.0 && targetMappedF0s[i]>10.0)
                            pscalesVar[i] = targetF0Mean/sourceF0Mean;
                    }    
                }
            }
            
            //Arrays.fill(pscalesVar, 0.8);

            //MaryUtils.plot(pscalesVar);
            //MaryUtils.plot(tscalesVar);
        }
    }

    private void initialise(int[] pitchMarksIn, double wsFixedIn, double ssFixedIn, int numfrm, int numfrmFixed, int numPeriodsIn, boolean isFixedRate)
    {
        numPeriods = numPeriodsIn;

        if (pitchMarksIn != null)
        {
            getScalesVar(pitchMarksIn, wsFixedIn, ssFixedIn, numfrm, numfrmFixed, isFixedRate);
        }     
    }

    private void getScalesVar(int [] pitchMarks, double wsFixed, double ssFixed, int numfrm, int numfrmFixed, boolean isFixedRate)
    {   
        if (tscales.length==1)
            tscaleSingle=tscales[0]; 
        else
            tscaleSingle=-1;

        //Find pscale, tscale and escale values corresponding to each fixed skip rate frame
        if (pscales.length != numfrmFixed)
            pscales = MathUtils.modifySize(pscales, numfrmFixed);
        
        if (tscales.length !=numfrmFixed)
            tscales = MathUtils.modifySize(tscales, numfrmFixed);
        
        if (escales.length != numfrmFixed)
            escales = MathUtils.modifySize(escales, numfrmFixed);
        
        if (vscales.length != numfrmFixed)
            vscales = MathUtils.modifySize(vscales, numfrmFixed);
        //

        //Determine the pitch, time, and energy scaling factors corresponding to each pitch synchronous frame
        pscalesVar = MathUtils.ones(numfrm);
        tscalesVar = MathUtils.ones(numfrm);
        escalesVar = MathUtils.ones(numfrm);
        vscalesVar = MathUtils.ones(numfrm);
        
        double tVar;
        int ind;
        for (int i=0; i<numfrm; i++)
        {
            if (!isFixedRate)
                tVar = (0.5*(pitchMarks[i+numPeriods]+pitchMarks[i]))/fs;
            else
                tVar = i*ssFixed+0.5*wsFixed;
            
            ind = (int)(Math.floor((tVar-0.5*wsFixed)/ssFixed+0.5));
            if (ind<0)
                ind=0;
            if (ind>numfrmFixed-1)
                ind=numfrmFixed-1;
            
            pscalesVar[i] = pscales[ind];
            tscalesVar[i] = tscales[ind];
            escalesVar[i] = escales[ind];
            vscalesVar[i] = vscales[ind];
        }
        //
    }
}

