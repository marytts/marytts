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
package de.dfki.lt.mary.unitselection.clunits;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.jsresources.SequenceAudioInputStream;

import com.sun.speech.freetts.util.WaveUtils;

import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.UnitConcatenator;
import de.dfki.lt.mary.unitselection.UnitDatabase;
import de.dfki.lt.mary.util.FloatList;

/**
 * Concatenates ClusterUnits and returns
 * an audio stream
 * 
 * @author Anna Hunecke
 *
 */
public class ClusterUnitConcatenator extends UnitConcatenator
{
    protected ClusterUnitDatabase database;
    protected AudioFormat audioformat;
    
    ////////////// LPC helpers //////////////////////
    /**
     * Given a residual, maps it using WaveUtils.ulawToShort() to a float.
     */
    protected final static float[] residualToFloatMap = new float[256];

    static {
        for (short i = 0; i < residualToFloatMap.length; i++) {
            residualToFloatMap[i] = (float) WaveUtils.ulawToShort(i);
        }
        residualToFloatMap[128] = (float) WaveUtils.ulawToShort((short) 255);
    }
    
    /**
     * Given a 16 bit value (represented as an int), extract the high eight bits
     * and return them
     * 
     * @param val
     *            the 16 bit value
     * 
     * @return the high eight bits
     */
    protected final static byte hibyte(int val)
    {
        return (byte) (val >>> 8);
    }

    /**
     * Given a 16 bit value (represented as an int), extract the low eight bits
     * and return them
     * 
     * @param val
     *            the 16 bit value
     * 
     * @return the low eight bits
     */
    protected final static byte lobyte(int val)
    {
        return (byte) (val & 0x000000FF);
    }
    ////////////////////// LPC helpers end /////////////////////////

    /**
     * Constructor
     * @param database the database of the voice
     * @param audioformat the audioformat of the audio stream
     */
    public ClusterUnitConcatenator(UnitDatabase database, AudioFormat audioformat){
        super();
        this.database = (ClusterUnitDatabase) database;
        this.audioformat = audioformat;
    }
    
    /**
     * Build the audio stream from the units
     * 
     * @param units the units
     * @return the resulting audio stream
     */
    protected AudioInputStream getAudio(List units){
        logger.debug("Getting audio for "+units.size()+" units");
        List audioStreams = new ArrayList(units.size());
        FrameSet sts = database.getAudioFrames();
        // Information for LPC resynthesis:
        FrameSetInfo frameSetInfo = sts.getFrameSetInfo();
        if (frameSetInfo == null) {
            throw new IllegalStateException("UnitConcatenator: FrameSetInfo does not exist");
        }
        int lpcOrder = frameSetInfo.getNumberOfChannels();
        FloatList globalLPCHistory = FloatList.createList(lpcOrder + 1);
        double lpcRangeFactor = (double) frameSetInfo.getCoeffRange() / 65535.0;
        float lpcMinimum = frameSetInfo.getCoeffMin();
        
        // First loop through all units: collect information and build up preparatory structures
    	for (Iterator it = units.iterator();it.hasNext();) {
    	    SelectedUnit unit = (SelectedUnit) it.next();
            UnitLPCData lpcData = new UnitLPCData();
            unit.setConcatenationData(lpcData);
            int unitStart = unit.getUnitStart();
            int unitEnd = unit.getUnitEnd();
            assert unitEnd >= unitStart;
            int pitchmarksInUnit = unitEnd - unitStart;
            int nSamples = 0;
            // First of all: Set target pitchmarks,
            // either by copying from units (data-driven)
            // or by computing from target (model-driven)
            int[] pitchmarks;
            if (unit.getTarget().isSilence()) {
                int targetLength = unit.targetDurationInSamples();
                int unitLength = unit.unitDurationInSamples();
                int avgPeriodLength = unitLength / pitchmarksInUnit; // there will be rounding errors here
                int nTargetPitchmarks = Math.round((float)targetLength / avgPeriodLength); // round to the nearest integer
                pitchmarks = new int[nTargetPitchmarks];
                lpcData.setPitchmarks(pitchmarks);
                for (int i=0; i<nTargetPitchmarks-1; i++) {
                    nSamples += avgPeriodLength;
                    pitchmarks[i] = nSamples;
                }
                // last pitchmark compensates for rounding errors
                nSamples += targetLength - (nTargetPitchmarks-1)*avgPeriodLength;
                pitchmarks[nTargetPitchmarks-1] = nSamples;
                assert pitchmarks[nTargetPitchmarks-1] == unit.targetDurationInSamples();
            } else {
                pitchmarks = new int[pitchmarksInUnit];
                lpcData.setPitchmarks(pitchmarks);
                for (int i = 0; i < pitchmarks.length; i++) {
                    nSamples += sts.getFrameSize(unitStart + i);
                    pitchmarks[i] = nSamples;
                }
                assert pitchmarks[pitchmarks.length-1] == unit.unitDurationInSamples(); 
            }
            int nPitchmarks = pitchmarks.length;
            short[][] frames = new short[nPitchmarks][];
            lpcData.setFrameCoefficients(frames);
            byte[] residuals = new byte[nSamples];
            lpcData.setResiduals(residuals);
            int unitSize = unit.unitDurationInSamples();
            float uIndex = 0;
            float m = (float)unitSize/(float)(nSamples);
            int targetResidualPosition = 0;
            // for each pitchmark, get frame coefficients and residual
            for (int i=0; i < nPitchmarks && pitchmarks[i] <= nSamples; i++) {
                Frame nextFrame = sts.getNearestFrame(uIndex, unit.getUnit().getStart(), unit.getUnit().getEnd());
                frames[i] = nextFrame.getCoefficients();
                // Get residual by copying, adapting residual length if necessary
                byte[] residual = nextFrame.getResidualData();
                int targetResidualSize = lpcData.getPeriodLength(i);
                if (residual.length < targetResidualSize) {
                    int targetResidualStart = (targetResidualSize - residual.length) / 2;
                    System.arraycopy(residual, 0, residuals,
                            targetResidualPosition + targetResidualStart, residual.length);
                } else {
                    int sourcePosition = (residual.length - targetResidualSize) / 2;
                    System.arraycopy(residual, sourcePosition, residuals, targetResidualPosition,
                            targetResidualSize);
                }
                targetResidualPosition += targetResidualSize;
                assert targetResidualPosition == pitchmarks[i];
                uIndex += ((float) targetResidualSize * m);
            }
            assert targetResidualPosition == nSamples;
        }
        
        // Second loop through all units: Generate audio
        for (Iterator it = units.iterator();it.hasNext();) {
            // TODO: verify if this should be inserted into the above loop
            SelectedUnit unit = (SelectedUnit) it.next();
            UnitLPCData lpcData = (UnitLPCData) unit.getConcatenationData();
            int nPitchmarks = lpcData.getPitchmarks().length;
            int nSamples = lpcData.getResiduals().length;
            byte[] residuals = lpcData.getResiduals();
            
            FloatList outBuffer = globalLPCHistory;
            byte[] samples = new byte[2*nSamples];
            float[] lpcCoefficients = new float[lpcOrder];
            int s = 0;
            // For each frame:
            for (int r = 0, i = 0; i < nPitchmarks; i++) {
                // unpack the LPC coefficients
                short[] frame = lpcData.getFrameCoefficients(i);
                for (int k = 0; k < lpcOrder; k++) {
                    lpcCoefficients[k] = (float) ((frame[k] + 32768.0) * lpcRangeFactor) + lpcMinimum;
                }
                int nSamplesInFrame = lpcData.getPeriodLength(i);
                // For each sample:
                for (int j = 0; j < nSamplesInFrame; j++, r++) {
                    FloatList backBuffer = outBuffer.prev;
                    float ob = residualToFloatMap[residuals[r] + 128];
                    for (int k=0; k<lpcOrder; k++) {
                        ob += lpcCoefficients[k] * backBuffer.value;
                        backBuffer = backBuffer.prev;
                    }
                    int sample = (int) ob;
                    samples[s++] = (byte) hibyte(sample);
                    samples[s++] = (byte) lobyte(sample);
                    outBuffer.value = ob;
                    outBuffer = outBuffer.next;
                }
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(samples);
            int samplesize = audioformat.getSampleSizeInBits();
            if (samplesize == AudioSystem.NOT_SPECIFIED)
                samplesize = 16; // usually 16 bit data
            long lengthInSamples = samples.length / (samplesize/8);
            AudioInputStream ais = new AudioInputStream(bais, audioformat, lengthInSamples);
            
            unit.setAudio(ais);
            audioStreams.add(ais);
        }
        
        return new SequenceAudioInputStream(audioformat, audioStreams);
    }

    protected static class UnitLPCData
    {
        int[] pitchmarks;
        short[][] frameCoefficients;
        byte[] residuals;

        public UnitLPCData()
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

        public void setFrameCoefficients(short[][] coefficients)
        {
            this.frameCoefficients = coefficients; 
        }
        
        public short[] getFrameCoefficients(int frameIndex)
        {
            return frameCoefficients[frameIndex];
        }
        
        public void setResiduals(byte[] residuals)
        {
            this.residuals = residuals;
        }
        
        public byte[] getResiduals()
        {
            return residuals;
        }

    }
}

