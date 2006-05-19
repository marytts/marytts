/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection;

import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.unitselection.clunits.Frame;

/**
 * A unit selected from Viterbi
 * 
 * @author Marc Schr&ouml;der
 *
 */
public class SelectedUnit
{
    protected Unit unit;
    protected Target target;
    protected int unitStartShift = 0;
    protected int unitEndShift = 0;
    protected Object concatenationData;
    protected AudioInputStream audio;
    
    public SelectedUnit(Unit unit, Target target)
    {
        this.unit = unit;
        this.target = target;
        this.audio = null;
    }

    public Unit getUnit()
    {
        return unit;
    }
    
    public Target getTarget()
    {
        return target;
    }

    public void setUnitStartShift(int unitStart)
    {
        this.unitStartShift = unitStart;
    }
    
    public void setUnitEndShift(int unitEnd)
    {
        this.unitEndShift = unitEnd;
    }
    
    public int getUnitStartShift()
    {
        return unitStartShift;
    }
    
    public int getUnitEndShift()
    {
        return unitEndShift;
    }
    
    public int getNumberOfFrames()
    {
        return unit.getNumberOfFrames()-unitStartShift+unitEndShift;
    }
    
    public int targetDurationInSamples()
    {
        return (int) (target.getTargetDurationInSeconds()*unit.getDatabase().getSamplingRate());
    }
    
    public int unitDurationInSamples()
    {
        return unit.durationInSamples();
    }
    
    /**
     * Remember data about this selected unit which is relevant for unit concatenation.
     * What type of data is saved here depends on the UnitConcatenator used.
     * @param concatenationData
     */
    public void setConcatenationData(Object concatenationData)
    {
        this.concatenationData = concatenationData;
    }
    
    public Object getConcatenationData()
    {
        return concatenationData;
    }
    
    public void setAudio(AudioInputStream audio)
    {
        this.audio = audio;
    }
    
    public AudioInputStream getAudio()
    {
        return audio;
    }
    
    
    /**
     * Retrieves the nearest frame.
     *
     * @param index the ideal sample position for the frame start, relative to (possibly shifted) unit start, in samples
     * @param start first frame belonging to the relevant unit
     * @param end frame after the last frame belonging to the relevant unit
     *
     * @return the nearest frame
     */
    public Frame getNearestAudioFrame(float index) {
        int frameIndex = 0, nextFrameIndex;
        // frame index positions are in samples relative to unit start

        // loop through all the Frames in this unit
        for (int i = 0, iMax = getNumberOfFrames(); i < iMax; i++) {
            nextFrameIndex = frameIndex + getAudioFrameSize(i);
            if (Math.abs(index - frameIndex) < Math.abs(index - nextFrameIndex)) {
                return getAudioFrame(i);
            }
            frameIndex = nextFrameIndex;
        }
        return getAudioFrame(getNumberOfFrames() - 1);
    }

    
    /**
     * For this selected unit, get the audio frame with the given index number,
     * taking into account any start or end shifts.
     * @param i the index number, between 0 and getNumberOfFrames()-1.
     * @return the corresponding frame.
     */
    public Frame getAudioFrame(int i)
    {
        if (i<0 || i>=getNumberOfFrames()) throw new IllegalArgumentException("Have "+getNumberOfFrames()+" frames, requested number "+i);
        int translatedI = unitStartShift+i; // remember, unitStartShift <= 0
        if (translatedI<0) { // we must look to the left of this unit
            Unit prev = unit.getPrevious();
            return prev.getAudioFrame(prev.getNumberOfFrames()+translatedI);
        }
        return unit.getAudioFrame(translatedI);
    }
    
    /**
     * For this selected unit, get the size of the audio frame with the given index number,
     * taking into account any start or end shifts.
     * @param i the index number, between 0 and getNumberOfFrames()-1.
     * @return the corresponding frame size, in samples.
     */
    public int getAudioFrameSize(int i)
    {
        if (i<0 || i>=getNumberOfFrames()) throw new IllegalArgumentException("Have "+getNumberOfFrames()+" frames, requested number "+i);
        int translatedI = unitStartShift+i; // remember, unitStartShift <= 0
        if (translatedI<0) { // we must look to the left of this unit
            Unit prev = unit.getPrevious();
            return prev.getAudioFrameSize(prev.getNumberOfFrames()+translatedI);
        }
        return unit.getAudioFrameSize(translatedI);
    }
    
    
    
    public String toString()
    {
        return "Target: "+target.toString() + " Unit: " + unit.toString()
        + (unitStartShift != 0 ? " start shifted by "+unitStartShift : "")
        + (unitEndShift != 0 ? " end shifted by "+unitEndShift : "")
        + " target duration " + targetDurationInSamples()
        + " prev unit "+ unit.getPrevInstance() + ", next unit " + unit.getNextInstance();
    }
}
