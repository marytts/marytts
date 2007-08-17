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
    FDPSOLAProcessor fdpsola;
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
    protected void getDatagramsFromTimeline(List units) throws IOException
    {
        for (int i=0, len=units.size(); i<len; i++) {
            SelectedUnit unit = (SelectedUnit) units.get(i);
            assert !unit.getUnit().isEdgeUnit() : "We should never have selected any edge units!";
            OverlapUnitData unitData = new OverlapUnitData();
            unit.setConcatenationData(unitData);
            int nSamples = 0;
            int unitSize = unitToTimeline(unit.getUnit().getDuration()); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().getStart()); // convert to timeline samples
            //System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
            Datagram[] datagrams = timeline.getDatagrams(unitStart,(long)unitSize);
            unitData.setFrames(datagrams);
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
     * Generate audio to match the target pitchmarks as closely as possible.
     * @param units
     * @return
     */
    protected AudioInputStream generateAudioStream(List units)
    {
        fdpsola = new FDPSOLAProcessor();
        
        int len = units.size();
        Datagram[][] datagrams = new Datagram[len][];
        Datagram[] rightContexts = new Datagram[len];
        boolean[][] voicings = new boolean[len][];
        
        for (int i=0; i<len; i++) {
            SelectedUnit unit = (SelectedUnit) units.get(i);
            
            OverlapUnitData unitData = (OverlapUnitData)unit.getConcatenationData();
            assert unitData != null : "Should not have null unitdata here";
            Datagram[] frames = unitData.getFrames();
            assert frames != null : "Cannot generate audio from null frames";
            // Generate audio from frames
            datagrams[i] = frames;
            voicings[i] = new boolean[datagrams[i].length];
            Phoneme sampaPhoneme = unit.getTarget().getSampaPhoneme();
            
            for (int j=0; j<voicings[i].length; j++)
            {
                if (sampaPhoneme != null && (sampaPhoneme.isVowel() || sampaPhoneme.isVoiced()))
                    voicings[i][j] = true;
                else
                    voicings[i][j] = false;
            }
            
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
        
        return fdpsola.process(datagrams, rightContexts, audioformat, voicings);
    }
    
    protected void determineTargetPitchmarks(List units)
    {

    }
}
