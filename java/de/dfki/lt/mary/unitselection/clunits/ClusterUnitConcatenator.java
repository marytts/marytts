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
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.mary.unitselection.LPCResult;
import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.UnitConcatenator;
import de.dfki.lt.mary.unitselection.UnitDatabase;
/**
 * import de.dfki.lt.signalproc.process.PSOLAProcessor;
 * import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
 * import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
 * import de.dfki.lt.signalproc.util.DDSAudioInputStream;
**/
/**
 * Concatenates ClusterUnits and returns
 * an audio stream
 * 
 * @author Anna Hunecke
 *
 */
public class ClusterUnitConcatenator extends UnitConcatenator
{
    private ClusterUnitDatabase database;
    private AudioFormat audioformat;
    static private final int ADD_RESIDUAL_PULSE = 1;
    static private final int ADD_RESIDUAL_WINDOWED = 2;
    static private final int ADD_RESIDUAL = 3;
    
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
        //List audioStreams = new ArrayList(units.size());
        FrameSet sts = database.getAudioFrames();
    	int nPitchmarks = 0;

        // First of all: Set target pitchmarks,
        // either by copying from units (data-driven)
        // or by computing from target (model-driven)
    	for (Iterator it = units.iterator();it.hasNext();) {
    	    SelectedUnit unit = (SelectedUnit) it.next();
            int unitStart = unit.getUnitStart();
            int unitEnd = unit.getUnitEnd();
            assert unitEnd >= unitStart;
            int pitchmarksInUnit = unitEnd - unitStart;
            if (unit.getTarget().isSilence()) {
                int targetLength = unit.targetDurationInSamples();
                int unitLength = unit.unitDurationInSamples();
                int avgPeriodLength = unitLength / pitchmarksInUnit; // there will be rounding errors here
                int nTargetPitchmarks = Math.round((float)targetLength / avgPeriodLength); // round to the nearest integer
                nPitchmarks += nTargetPitchmarks;
            } else {
                nPitchmarks += pitchmarksInUnit;
            }
    	}
        LPCResult lpcResult = new LPCResult(nPitchmarks);

        int nSamples = 0;

    	int[] pitchmarkPositionsInSamples = lpcResult.getTimes();
    	int ipm = 0;
    	for (Iterator it = units.iterator();it.hasNext();) {
            int unitStartSample; // for debugging, remember at which sample unit starts
            if (ipm == 0) unitStartSample = 0;
            else unitStartSample = pitchmarkPositionsInSamples[ipm-1];
    	    SelectedUnit unit = (SelectedUnit) it.next();
            int unitStart = unit.getUnitStart();
            int unitEnd = unit.getUnitEnd();
            int pitchmarksInUnit = unitEnd - unitStart;
            // For silence, roughly maintain average period length of unit
            // but force target pause duration by adjusting number of pitch marks.
            if (unit.getTarget().isSilence()) {
                int targetLength = unit.targetDurationInSamples();
                int unitLength = unit.unitDurationInSamples();
                int avgPeriodLength = unitLength / pitchmarksInUnit; // there will be rounding errors here
                int nTargetPitchmarks = Math.round((float)targetLength / avgPeriodLength); // round to the nearest integer
                for (int i=0; i<nTargetPitchmarks-1; i++, ipm++) {
                    nSamples += avgPeriodLength;
                    pitchmarkPositionsInSamples[ipm] = nSamples;
                }
                // last pitchmark compensates for rounding errors
                nSamples += targetLength - (nTargetPitchmarks-1)*avgPeriodLength;
                pitchmarkPositionsInSamples[ipm] = nSamples;
                ipm++;
                assert pitchmarkPositionsInSamples[ipm-1] - unitStartSample == unit.targetDurationInSamples();
            } else {
                for (int idb = unitStart; idb < unitEnd; idb++,ipm++) {
                    // idb: index in database frames; ipm: index in locally created pitchmarks
                    nSamples += sts.getFrameSize(idb);
                    pitchmarkPositionsInSamples[ipm] = nSamples;
                }
                assert pitchmarkPositionsInSamples[ipm-1] - unitStartSample == unit.unitDurationInSamples(); 
            }
    	}
        lpcResult.resizeResiduals(nSamples);

    	
    	

    	// create the array of final residual sizes
        int targetResidualPosition = 0;
        int targetStart = 0;
        int targetEnd;
    	int[] residualSizes = lpcResult.getResidualSizes();
        ipm = 0;
    	for (Iterator it = units.iterator();it.hasNext();) {
    	    SelectedUnit unit = (SelectedUnit) it.next();
    	    int unitSize = unit.unitDurationInSamples();
            if (unit.getTarget().isSilence()) {
                targetEnd = targetStart + unit.targetDurationInSamples();
            } else { // non-silence units
                // If we just force target duration, we basically lose intonation:
                // So we use the unit durations from the DB:
                targetEnd = targetStart + unitSize;
            }
            
    	    float uIndex = 0;
    	    float m = (float)unitSize/(float)(targetEnd - targetStart);
    	    
    	    // for all the pitchmarks that are required
    	    for (; ipm < nPitchmarks && pitchmarkPositionsInSamples[ipm] <= targetEnd; ipm++) {
    	    	Frame nextFrame = sts.getNearestFrame(uIndex, 
                            ((ClusterUnit)(unit.getUnit())).getStart(),
                            ((ClusterUnit)(unit.getUnit())).getEnd());
    	    	lpcResult.setFrame(ipm, nextFrame.getCoefficients());
    	    	// Get residual by copying
    	    	int targetResidualSize = lpcResult.getFrameShift(ipm);
    	    	residualSizes[ipm] = targetResidualSize;
    	    	byte[] residual = nextFrame.getResidualData();
	    		lpcResult.copyResiduals(residual, targetResidualPosition, targetResidualSize);
    	    	targetResidualPosition += targetResidualSize;
                assert targetResidualPosition == pitchmarkPositionsInSamples[ipm];
    	    	uIndex += ((float) targetResidualSize * m);
    	    }
    	    targetStart = targetEnd;
    	}
        assert targetResidualPosition == nSamples;
    	
        
        // Information for LPC resynthesis:
        FrameSetInfo frameSetInfo = sts.getFrameSetInfo();
        if (frameSetInfo == null) {
            throw new IllegalStateException("UnitConcatenator: FrameSetInfo does not exist");
        }
        lpcResult.setValues(frameSetInfo.getNumberOfChannels(),
                frameSetInfo.getSampleRate(),
                frameSetInfo.getResidualFold(),
                frameSetInfo.getCoeffMin(),
                frameSetInfo.getCoeffRange());
    	
    	byte[] audio = lpcResult.getWaveSamples();
        ByteArrayInputStream bais = new ByteArrayInputStream(audio);
        AudioFormat af = audioformat;
        int samplesize = af.getSampleSizeInBits();
        if (samplesize == AudioSystem.NOT_SPECIFIED)
            samplesize = 16; // usually 16 bit data
        long lengthInSamples = audio.length / (samplesize/8);
        AudioInputStream ais = new AudioInputStream(bais, af, lengthInSamples);
    	
        
    	return ais;
    }
}
