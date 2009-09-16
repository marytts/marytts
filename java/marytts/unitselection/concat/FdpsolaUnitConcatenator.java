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
package marytts.unitselection.concat;

import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.modules.phonemiser.Allophone;
import marytts.signalproc.process.FDPSOLAProcessor;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.Unit;
import marytts.unitselection.select.SelectedUnit;


/**
 * A unit concatenator that supports FD-PSOLA based prosody modifications during speech synthesis
 * 
 * @author Oytun T&uumlrk
 *
 */
public class FdpsolaUnitConcatenator extends OverlapUnitConcatenator {
    private boolean [][] voicings;
    private double [][] pscales;
    private double [][] tscales;
    private Datagram[][] datagrams;
    private Datagram[] rightContexts;
    
    /**
     * 
     */
    public FdpsolaUnitConcatenator() 
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
            int unitSize = unitToTimeline(unit.getUnit().duration); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().startTime); // convert to timeline samples
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

            Allophone allophone = unit.getTarget().getAllophone();

            voicings[i] = new boolean[datagrams[i].length];
            
            for (j=0; j<datagrams[i].length; j++)
            {
                if (allophone != null && (allophone.isVowel() || allophone.isVoiced()))
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
            
            Allophone allophone = unit.getTarget().getAllophone();

            int totalDatagrams = 0;
            averageUnitF0InHz = 0.0;
            averageTargetF0InHz = 0.0;
            totalTargetUnits = 0;
            
            if (i>0)
            {
                for (j=0; j<datagrams[i-1].length; j++)
                {
                    if (allophone != null && (allophone.isVowel() || allophone.isVoiced()))
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
                if (allophone != null && (allophone.isVowel() || allophone.isVoiced()))
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
                    if (allophone != null && (allophone.isVowel() || allophone.isVoiced()))
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
                if (allophone != null && (allophone.isVowel() || allophone.isVoiced()))
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
    //2) Duration modification factors can be limited or even set to 1.0 for different phone classes
    //3) Duration modification factors can be limited depending on the previous/next phone class
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

