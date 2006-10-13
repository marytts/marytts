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
package de.dfki.lt.mary.unitselection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;


import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.UnitConcatenator;
import de.dfki.lt.mary.unitselection.UnitDatabase;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.window.HannWindow;
import de.dfki.lt.signalproc.window.Window;

/**
 * Concatenates Units and returns
 * an audio stream
 * 
 *
 */
public class WaveOverlapUnitConcatenator implements UnitConcatenator
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
    public WaveOverlapUnitConcatenator()
    {
        logger = Logger.getLogger(this.getClass());
    }

    public void load(UnitDatabase database)
    {
        this.database = database;
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
    public AudioInputStream getAudio(List units) throws IOException
    {
        logger.debug("Getting audio for "+units.size()+" units");
        
        int totalNSamples = 0;
        // First loop through all units: collect information and build up preparatory structures
    	for (Iterator it = units.iterator();it.hasNext();) {
    	    SelectedUnit unit = (SelectedUnit) it.next();
            UnitData unitData = new UnitData();
            unit.setConcatenationData(unitData);
            int nSamples = 0;
            int unitSize = unitToTimeline(unit.getUnit().getDuration()); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().getStart()); // convert to timeline samples
            //System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
            Datagram[] datagrams = timeline.getDatagrams(unitStart,(long)unitSize);
            // one right context period for windowing:
            Datagram rightContextFrame = null;
            Unit next = database.getUnitFileReader().getNextUnit(unit.getUnit());
            if (next != null && !next.isEdgeUnit()) {
                rightContextFrame = timeline.getDatagram(unitStart+unitSize);
            } else { // no right context: add a zero frame as long as the last frame in the unit
                int length = datagrams[datagrams.length-1].getLength() / 2; // length = number of samples
                rightContextFrame = new Datagram(length, new byte[2*length]);
            }
            int rightContextFrameLength;

            int pitchmarksInUnit = datagrams.length;
            assert pitchmarksInUnit > 0;
            // First of all: Set target pitchmarks,
            // either by copying from units (data-driven)
            // or by computing from target (model-driven)
            int[] pitchmarks;
            if (unit.getTarget().isSilence()) {
                int targetLength = (int) Math.round(unit.getTarget().getTargetDurationInSeconds()*audioformat.getSampleRate());
                
                int avgPeriodLength = unitSize / pitchmarksInUnit; // there will be rounding errors here
                int nTargetPitchmarks = Math.round((float)targetLength / avgPeriodLength); // round to the nearest integer
                pitchmarks = new int[nTargetPitchmarks+1];
                unitData.setPitchmarks(pitchmarks);
                for (int i=0; i<nTargetPitchmarks-1; i++) {
                    nSamples += avgPeriodLength;
                    pitchmarks[i] = nSamples;
                }
                // last regular pitchmark compensates for rounding errors
                nSamples += targetLength - (nTargetPitchmarks-1)*avgPeriodLength;
                pitchmarks[nTargetPitchmarks-1] = nSamples;
                assert pitchmarks[nTargetPitchmarks-1] == targetLength;
                // empty right context
                rightContextFrame = new Datagram(0, new byte[0]);
            } else {
                pitchmarks = new int[pitchmarksInUnit+1];
                unitData.setPitchmarks(pitchmarks);
                for (int i = 0; i < pitchmarksInUnit; i++) {
                    nSamples += datagrams[i].getDuration(); // length in samples
                    pitchmarks[i] = nSamples;
                }
                assert pitchmarks[pitchmarks.length-2] == unitToTimeline(unit.getUnit().getDuration()):
                    "Unexpected difference: for unit "+unit+", expected "+unitToTimeline(unit.getUnit().getDuration())+" samples, found "+pitchmarks[pitchmarks.length-2]; 
            }
            // And the last pitchmark for windowing the right context frame:
            rightContextFrameLength = (int) rightContextFrame.getDuration(); // length in samples
            pitchmarks[pitchmarks.length-1] = nSamples+rightContextFrameLength;
            totalNSamples += nSamples;
            int nPitchmarks = pitchmarks.length;
            //System.out.println("Unit size "+unitSize+", pitchmarks length "
              //      +nPitchmarks);
            Datagram[] frames = new Datagram[nPitchmarks];
            unitData.setFrames(frames);
            
            float timeStretch = (float)unitSize/(float)(nSamples); 
            // if timeStretch == 1, copy unit as it is; 
            // if timeStretch < 1, lengthen by duplicating frames
            // if timeStretch > 1, shorten by skipping frames
            float frameIndex = 0;
            int realisedDuration = 0;
            //float uIndex = 0; // counter of imaginary sample position in the unit 
            // for each pitchmark, get frame coefficients and residual
            for (int i=0; i < nPitchmarks - 1; i++) {
                int datagramIndex = Math.round(frameIndex);
                if (datagramIndex >= datagrams.length) datagramIndex = datagrams.length - 1;
                frames[i] = datagrams[datagramIndex];
                frameIndex += timeStretch; // i.e., increment by less than 1 for stretching, by more than 1 for shrinking
                // FreeTTS did this time stretching on the samples level, and retrieved the frame closest to the resulting sample position:
                // uIndex += ((float) targetResidualSize * m);
                realisedDuration += frames[i].getDuration();
            }
            assert rightContextFrame != null;
            frames[nPitchmarks-1] = rightContextFrame;
            realisedDuration += rightContextFrame.getDuration();
            if (nSamples+rightContextFrameLength != realisedDuration) {
                logger.debug("Expected duration: "+(nSamples+rightContextFrameLength)+"; realised duration: "+realisedDuration+" for unit "+unit);
            }

            // Generate audio: Residual-excited linear prediction
            byte[] audioData = new byte[2*realisedDuration];
            for (int i=0,pos=0; i<nPitchmarks; i++) {
                assert frames[i] != null: "Frame "+i+" of "+nPitchmarks+" is null: Unit "+unit;
                byte[] frameAudio = frames[i].getData();
                if (frameAudio.length + pos > audioData.length) {
                    logger.debug("Attempt to copy "+frameAudio.length+" bytes to pos "+pos+" of "+audioData.length);                    
                }
                System.arraycopy(frameAudio, 0, audioData, pos, frameAudio.length);
                pos += frameAudio.length;
            }
            double[] audio = new AudioDoubleDataSource(new AudioInputStream(new ByteArrayInputStream(audioData), audioformat, audioData.length/2)).getAllData();
            
            // Now audio is the resynthesized audio signal for the unit plus the right context frame,
            // without any post-processing.
            unit.setAudio(audio);
        }
        
        // Second loop through all units: post-process and concatenate audio
        double[] totalAudio = new double[totalNSamples];
        int iTotal = 0; // write position in totalAudio
        for (Iterator it = units.iterator();it.hasNext();) {
            SelectedUnit unit = (SelectedUnit) it.next();
            UnitData unitData = (UnitData) unit.getConcatenationData();
            int nPitchmarks = unitData.getPitchmarks().length;
            int rightContextFrameLength = unitData.getPeriodLength(nPitchmarks-1);
            double[] audio = unit.getAudio();
            int nSamples = audio.length-rightContextFrameLength;
            
            // Now apply the left half of a Hann window to the first frame and 
            // the right half of a Hann window to the right context frame:
            int firstPeriodLength = unitData.getPeriodLength(0);
            Window hannWindow = new HannWindow(2*firstPeriodLength);
            // start overlap at iTotal:
            for (int i=0; i<firstPeriodLength; i++, iTotal++) {
                assert iTotal < totalNSamples: 
                    "Trying to write sample "+iTotal+" of "+totalNSamples+" (i="+i+" of "+firstPeriodLength+", unit"+unit;
                totalAudio[iTotal] += audio[i] * hannWindow.value(i);
            }
            hannWindow = new HannWindow(2*rightContextFrameLength);
            for (int i=0; i<rightContextFrameLength; i++) {
                audio[nSamples+i] *= hannWindow.value(rightContextFrameLength+i);
            }
            int toCopy = Math.min(nSamples+rightContextFrameLength-firstPeriodLength, totalAudio.length-iTotal);
            System.arraycopy(audio, firstPeriodLength, totalAudio, iTotal, toCopy);
            iTotal += nSamples - firstPeriodLength;
            // TODO: or should this be iTotal += nSamples - rightContextFrameLength; ???
        }
        return new DDSAudioInputStream(new BufferedDoubleDataSource(totalAudio), audioformat);

    }
    
    private int unitToTimeline(int duration)
    {
        return (int) (duration*unitToTimelineSampleRateFactor);
    }

    private long unitToTimeline(long time)
    {
        return (long) (time*unitToTimelineSampleRateFactor);
    }

    protected static class UnitData
    {
        int[] pitchmarks;
        Datagram[] frames;

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
        
        public void setFrame(int frameIndex, Datagram frame)
        {
            this.frames[frameIndex] = frame;
        }
        
        public Datagram getFrame(int frameIndex)
        {
            return frames[frameIndex];
        }
        
    }
}

