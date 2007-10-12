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

package de.dfki.lt.mary.unitselection.concat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.concat.BaseUnitConcatenator.UnitData;
import de.dfki.lt.mary.unitselection.concat.OverlapUnitConcatenator.OverlapUnitData;
import de.dfki.lt.signalproc.process.FDPSOLAProcessor;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;

/**
 * @author oytun.turk
 *
 */
public class FDPSOLAUnitConcatenator extends OverlapUnitConcatenator {
    private boolean [][] voicings;
    private double [][] pscales;
    private double [][] tscales;
    private Datagram[][] datagrams;
    private Datagram[] rightContexts;
    
    /**
     * 
     */
    public FDPSOLAUnitConcatenator() 
    {
        super();
    }
    
    /**
     * Get the raw audio material for each unit from the timeline.
     * @param units
     */
    protected void getDatagramsFromTimeline(List<SelectedUnit> units) throws IOException
    {
        for (SelectedUnit unit : units) 
        {
            assert !unit.getUnit().isEdgeUnit() : "We should never have selected any edge units!";
            OverlapUnitData unitData = new OverlapUnitData();
            unit.setConcatenationData(unitData);
            int nSamples = 0;
            int unitSize = unitToTimeline(unit.getUnit().getDuration()); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().getStart()); // convert to timeline samples
            //System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
            Datagram [] tmpDatagrams = timeline.getDatagrams(unitStart,(long)unitSize);
            unitData.setFrames(tmpDatagrams);
            // one right context period for windowing:
            Datagram rightContextFrame = null;
            Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
            if (nextInDB != null && !nextInDB.isEdgeUnit()) {
                rightContextFrame = timeline.getDatagram(unitStart+unitSize);
                unitData.setRightContextFrame(rightContextFrame);
            }
        }
    }
    
    /**
     * Determine target pitchmarks (= duration and f0) for each unit.
     * @param units
     */
    protected void determineTargetPitchmarks(List<SelectedUnit> units)
    {
        // First, determine the target pitchmarks as usual by the parent
        // implementation:
        super.determineTargetPitchmarks(units);
        
        int len = units.size();
        datagrams = new Datagram[len][];
        rightContexts = new Datagram[len];
    
        int i, j;
        SelectedUnit unit = null;

        //Preprocessing and allocation
        for (i=0; i<len; i++) 
        {
            unit = (SelectedUnit) units.get(i);
            
            OverlapUnitData unitData = (OverlapUnitData)unit.getConcatenationData();
            assert unitData != null : "Should not have null unitdata here";
            Datagram[] frames = unitData.getFrames();
            assert frames != null : "Cannot generate audio from null frames";
            // Generate audio from frames
            datagrams[i] = frames;
            
            Unit nextInDB = database.getUnitFileReader().getNextUnit(unit.getUnit());
            Unit nextSelected;
            if (i+1==len) nextSelected = null;
            else nextSelected = ((SelectedUnit)units.get(i+1)).getUnit();
            if (nextInDB != null && !nextInDB.equals(nextSelected)) {
                // Only use right context if we have a next unit in the DB is not the
                // same as the next selected unit.
                rightContexts[i] = unitData.getRightContextFrame(); // may be null
            }
        }
        //
        getVoicings(units);

        getPitchScales(units);
        
        getDurationScales(units);
    }
    
    private void getVoicings(List<SelectedUnit> units)
    {
        int len = units.size();
        int i, j;
        
        voicings = new boolean[len][];

        SelectedUnit unit = null;
        
        //Estimation of pitch scale modification amounts
        for (i=0; i<len; i++) 
        {
            unit = (SelectedUnit) units.get(i);

            Phoneme sampaPhoneme = unit.getTarget().getSampaPhoneme();

            voicings[i] = new boolean[datagrams[i].length];
            
            for (j=0; j<datagrams[i].length; j++)
            {
                if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                    voicings[i][j] = true;
                else
                    voicings[i][j] = false;
            }
        }
        //
    }
    
    //We can try different things in this function
    //1) Pitch of the selected units can  be smoothed without using the target pitch values at all. 
    //   This will involve creating the target f0 values for each frame by ensuing small adjustments and yet reduce pitch discontinuity
    //2) Pitch of the selected units can be modified to match the specified target where those target values are smoothed
    //3) A mixture of (1) and (2) can be deviced, i.e. to minimize the amount of pitch modification one of the two methods can be selected for a given unit
    //4) Pitch segments of selected units can be shifted 
    //5) Pitch segments of target units can be shifted
    //6) Pitch slopes can be modified for better matching in concatenation boundaries
    private void getPitchScales(List<SelectedUnit> units)
    {
        int len = units.size();
        int i, j;
        double averageUnitF0InHz;
        double averageTargetF0InHz;
        int totalTargetUnits;
        voicings = new boolean[len][];
        pscales = new double[len][];
        SelectedUnit prevUnit = null;
        SelectedUnit unit = null;
        SelectedUnit nextUnit = null;
        
        //Estimation of pitch scale modification amounts
        for (i=0; i<len; i++) 
        {
            if (i>0)
                prevUnit = (SelectedUnit) units.get(i-1);
            else
                prevUnit = null;
            
            unit = (SelectedUnit) units.get(i);
            
            if (i<len-1)
                nextUnit = (SelectedUnit) units.get(i+1);
            else
                nextUnit = null;
            
            Phoneme sampaPhoneme = unit.getTarget().getSampaPhoneme();

            int totalDatagrams = 0;
            averageUnitF0InHz = 0.0;
            averageTargetF0InHz = 0.0;
            totalTargetUnits = 0;
            
            if (i>0)
            {
                for (j=0; j<datagrams[i-1].length; j++)
                {
                    if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                    {
                        averageUnitF0InHz += ((double)timeline.getSampleRate())/((double)datagrams[i-1][j].getDuration());
                        totalDatagrams++;
                    }
                }
                
                averageTargetF0InHz += prevUnit.getTarget().getTargetF0InHz();
                totalTargetUnits++;
            }
            
            for (j=0; j<datagrams[i].length; j++)
            {
                if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                {
                    averageUnitF0InHz += ((double)timeline.getSampleRate())/((double)datagrams[i][j].getDuration());
                    totalDatagrams++;
                }
                
                averageTargetF0InHz += unit.getTarget().getTargetF0InHz();
                totalTargetUnits++;
            }
            
            if (i<len-1)
            {
                for (j=0; j<datagrams[i+1].length; j++)
                {
                    if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                    {
                        averageUnitF0InHz += ((double)timeline.getSampleRate())/((double)datagrams[i+1][j].getDuration());
                        totalDatagrams++;
                    }
                }
                
                averageTargetF0InHz += nextUnit.getTarget().getTargetF0InHz();
                totalTargetUnits++;
            }
            
            averageTargetF0InHz /= totalTargetUnits;
            averageUnitF0InHz /= totalDatagrams;

            voicings[i] = new boolean[datagrams[i].length];
            pscales[i] = new double[datagrams[i].length];
            
            for (j=0; j<datagrams[i].length; j++)
            {
                if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                {
                    voicings[i][j] = true;
                    
                    /*
                    pscales[i][j] = averageTargetF0InHz/averageUnitF0InHz;
                    if (pscales[i][j]>1.2)
                        pscales[i][j]=1.2;
                    if (pscales[i][j]<0.8)
                        pscales[i][j]=0.8;
                        */
                    pscales[i][j] = 1.0;
                }
                else
                {
                    voicings[i][j] = false;
                    pscales[i][j] = 1.0;
                }
            }
        }
        //
    }
    
    //We can try different things in this function
    //1) Duration modification factors can be estimated using neighbouring selected and target unit durations
    //2) Duration modification factors can be limited or even set to 1.0 for different phoneme classes
    //3) Duration modification factors can be limited depending on the previous/next phoneme class
    private void getDurationScales(List<SelectedUnit> units)
    {
        int len = units.size();
        
        int i, j;
        tscales = new double[len][];
        int unitDuration;
        
        double [] unitDurationsInSeconds = new double[datagrams.length];
        
        SelectedUnit prevUnit = null;
        SelectedUnit unit = null;
        SelectedUnit nextUnit = null;
        
        for (i=0; i<len; i++)
        {
            unitDuration = 0;
            for (j=0; j<datagrams[i].length; j++)
            {
                if (j==datagrams[i].length-1)
                {
                    if (rightContexts!=null && rightContexts[i]!=null)
                        unitDuration += datagrams[i][j].getDuration()+rightContexts[i].getDuration();
                    else
                        unitDuration += datagrams[i][j].getDuration();
                }
                else
                    unitDuration += datagrams[i][j].getDuration();
            }
            unitDurationsInSeconds[i] = ((double)unitDuration)/timeline.getSampleRate();
        }
        
        double targetDur, unitDur;
        for (i=0; i<len; i++)
        {
            targetDur = 0.0;
            unitDur = 0.0;
            if (false && i>0)
            {
                prevUnit = (SelectedUnit) units.get(i-1);
                targetDur += prevUnit.getTarget().getTargetDurationInSeconds();
                unitDur += unitDurationsInSeconds[i-1];
            }
            
            unit = (SelectedUnit) units.get(i);
            targetDur += unit.getTarget().getTargetDurationInSeconds();
            unitDur += unitDurationsInSeconds[i];
            
            if (false && i<len-1)
            {
                nextUnit = (SelectedUnit) units.get(i+1);
                targetDur += nextUnit.getTarget().getTargetDurationInSeconds();
                unitDur += unitDurationsInSeconds[i+1];
            }
            
            tscales[i] = new double[datagrams[i].length];
            
            for (j=0; j<datagrams[i].length; j++)
            {
                /*
                tscales[i][j] = targetDur/unitDur;
                if (tscales[i][j]>1.2)
                    tscales[i][j]=1.2;
                if (tscales[i][j]<0.8)
                    tscales[i][j]=0.8;
                    */
                
                tscales[i][j] = 1.0;
            }
        }
    }
    
    /**
     * Generate audio to match the target pitchmarks as closely as possible.
     * @param units
     * @return
     */
    protected AudioInputStream generateAudioStream(List<SelectedUnit> units)
    {
        // TODO: this does not seem thread-safe -- what happens if several threads call FDPSOLAUnitConcatenator? Store all data in units.
        return (new FDPSOLAProcessor()).process(datagrams, rightContexts, audioformat, voicings, pscales, tscales);
    }
}
