/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitConcatenator;
import de.dfki.lt.mary.unitselection.UnitDatabase;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.window.HannWindow;
import de.dfki.lt.signalproc.window.Window;

/**
 * Concatenates Units and returns
 * an audio stream
 * 
 *
 */
public class BaseUnitConcatenator implements UnitConcatenator
{
    protected Logger logger;
    protected UnitDatabase database;
    protected TimelineReader timeline;
    protected AudioFormat audioformat;
    protected double unitToTimelineSampleRateFactor;
    
    

    /**
     * Empty Constructor; need to call load(UnitDatabase) separately
     * @see #load(UnitDatabase)
     */
    public BaseUnitConcatenator()
    {
        logger = Logger.getLogger(this.getClass());
    }

    public void load(UnitDatabase unitDatabase)
    {
        this.database = unitDatabase;
        this.timeline = database.getAudioTimeline();
        int sampleRate = timeline.getSampleRate();
        this.audioformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, // samples per second
                16, // bits per sample
                1, // mono
                2, // nr. of bytes per frame
                sampleRate, // nr. of frames per second
                true); // big-endian;
        this.unitToTimelineSampleRateFactor =  sampleRate / (double) database.getUnitFileReader().getSampleRate();
    }
    
    /**
     * Provide the audio format which will be produced by this
     * unit concatenator.
     * @return the audio format
     */
    public AudioFormat getAudioFormat()
    {
        return audioformat;
    }
    

    
    /**
     * Build the audio stream from the units
     * 
     * @param units the units
     * @return the resulting audio stream
     */
    public AudioInputStream getAudio(List<SelectedUnit> units) throws IOException
    {
        logger.debug("Getting audio for "+units.size()+" units");

        // 1. Get the raw audio material for each unit from the timeline
        getDatagramsFromTimeline(units);
        
        // 2. Determine target pitchmarks (= duration and f0) for each unit
        determineTargetPitchmarks(units);
        
        // 3. Generate audio to match the target pitchmarks as closely as possible
        return generateAudioStream(units);
    }

    /**
     * Get the raw audio material for each unit from the timeline.
     * @param units
     */
    protected void getDatagramsFromTimeline(List<SelectedUnit> units) throws IOException
    {
        for (SelectedUnit unit : units) {
            UnitData unitData = new UnitData();
            unit.setConcatenationData(unitData);
            int nSamples = 0;
            int unitSize = unitToTimeline(unit.getUnit().getDuration()); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().getStart()); // convert to timeline samples
            //System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
            Datagram[] datagrams = timeline.getDatagrams(unitStart,(long)unitSize);
            unitData.setFrames(datagrams);
        }
    }
    
    /**
     * Determine target pitchmarks (= duration and f0) for each unit.
     * @param units
     */
    protected void determineTargetPitchmarks(List<SelectedUnit> units)
    {
        for (SelectedUnit unit : units) {
            UnitData unitData = (UnitData)unit.getConcatenationData();
            assert unitData != null : "Should not have null unitdata here";
            Datagram[] datagrams = unitData.getFrames();
            Datagram[] frames = null; // frames to realise
            // The number and duration of the frames to realise
            // must be the result of the target pitchmark computation.
            if (datagrams != null && datagrams.length > 0) {
                frames = datagrams;
            } else { // no datagrams -- set as silence
                int targetLength = (int) (unit.getTarget().getTargetDurationInSeconds() * timeline.getSampleRate());
                frames = new Datagram[] { createZeroDatagram(targetLength) };
            }
            int unitDuration = 0;
            for (int i=0; i<frames.length; i++) {
                int dur = (int) frames[i].getDuration();
                unitDuration += frames[i].getDuration();
            }

            unitData.setUnitDuration(unitDuration);
            unitData.setFrames(frames);
        }
    }
    
    /**
     * Generate audio to match the target pitchmarks as closely as possible.
     * @param units
     * @return
     */
    protected AudioInputStream generateAudioStream(List<SelectedUnit> units)
    {
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        for (SelectedUnit unit : units) {
            UnitData unitData = (UnitData)unit.getConcatenationData();
            assert unitData != null : "Should not have null unitdata here";
            Datagram[] frames = unitData.getFrames();
            assert frames != null : "Cannot generate audio from null frames";
            // Generate audio from frames
            datagrams.addAll(Arrays.asList(frames));
        }
        
        DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
        return new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), audioformat);
    }
    
    
    /**
     * Create a datagram appropriate for this unit concatenator
     * which contains only zero values as samples.
     * @param length the number of zeros that the datagram should contain
     * @return
     */
    protected Datagram createZeroDatagram(int length) {
        return new Datagram(length, new byte[2*length]);
    }
    
    protected int unitToTimeline(int duration)
    {
        return (int) (duration*unitToTimelineSampleRateFactor);
    }

    protected long unitToTimeline(long time)
    {
        return (long) (time*unitToTimelineSampleRateFactor);
    }

    public static class UnitData
    {
        protected int[] pitchmarks;
        protected Datagram[] frames;
        protected int unitDuration = -1;

        public UnitData()
        {
        }
        /**
         * Set the array of to-be-realised pitchmarks for the realisation of the selected unit.
         * @param pitchmarks
         */
        public void setPitchmarks(int[] pitchmarks)
        {
            this.pitchmarks = pitchmarks;
        }
        
        public int[] getPitchmarks()
        {
            return pitchmarks;
        }
        
        /**
         * Get the pitchmark marking the end of the period with the index number periodIndex.
         * @param periodIndex
         * @return the pitchmark position, in samples
         */
        public int getPitchmark(int periodIndex)
        {
            return pitchmarks[periodIndex];
        }
        
        /**
         * Get the length of the pitch period ending with pitchmark with the index number periodIndex.
         * @param periodIndex
         * @return the period length, in samples
         */
        public int getPeriodLength(int periodIndex)
        {
            if (0 <= periodIndex && periodIndex < pitchmarks.length) {
                if (periodIndex > 0) {
                    return pitchmarks[periodIndex] - pitchmarks[periodIndex - 1];
                } else {
                    return pitchmarks[periodIndex];
                }
            } else {
                return 0;
            }
        }
        
        public int getNumberOfPitchmarks()
        {
            return pitchmarks.length;
        }

        public void setFrames(Datagram[] frames)
        {
            this.frames = frames; 
        }
        
        public Datagram[] getFrames()
        {
            return frames;
        }
        
        public void setFrame(int frameIndex, Datagram frame)
        {
            this.frames[frameIndex] = frame;
        }
        
        public Datagram getFrame(int frameIndex)
        {
            return frames[frameIndex];
        }
        
        /**
         * Set the realised duration of this unit,
         * in samples.
         * @param duration
         */
        public void setUnitDuration(int duration)
        {
            this.unitDuration = duration;
        }
        
        /**
         * Get the realised duration of this unit,
         * in samples
         * @return
         */
        public int getUnitDuration()
        {
            return unitDuration;
        }
        
    }
}

