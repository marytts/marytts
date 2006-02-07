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
        ClusterUnitDatabase db = database;
        FrameSet sts = db.getAudioFrames();
        LPCResult lpcResult;
    	int pitchmarks = 0;
    	int uttSize = 0;
    	int unitStart;
    	int unitEnd;

    	lpcResult = new LPCResult();
    	
    	for (Iterator it = units.iterator();it.hasNext();){
    	    SelectedUnit unit = (SelectedUnit) it.next();
    	    unitStart = unit.getUnitStart();
    	    unitEnd = unit.getUnitEnd();
    	    uttSize += sts.getUnitSize(unitStart, unitEnd);
    	    pitchmarks += unitEnd - unitStart;
    	    //unit.setTargetEnd(uttSize);
    	}
    	
    	lpcResult.resizeFrames(pitchmarks);

    	pitchmarks = 0;
    	uttSize = 0;

    	int[] targetTimes = lpcResult.getTimes();

    	for (Iterator it = units.iterator();it.hasNext();){
    	    SelectedUnit unit = (SelectedUnit) it.next();
    	    unitStart = unit.getUnitStart();
    	    unitEnd = unit.getUnitEnd();
    	    for (int i = unitStart; i < unitEnd; i++,pitchmarks++) {
    	        uttSize += sts.getFrame(i).getResidualSize();
    	        targetTimes[pitchmarks] = uttSize;
    	    }
    	}
    	
    	float uIndex = 0, m;
    	int pmI = 0, targetResidualPosition = 0, targetStart = 0, targetEnd, residualSize, numberFrames;
    	
    	FrameSetInfo frameSetInfo;
    	

    	int addResidualMethod = ADD_RESIDUAL;
    	/**
    	String residualType = utterance.getString("residual_type");
    	if (residualType != null) {
    	    if (residualType.equals("pulse")) {
    		addResidualMethod = ADD_RESIDUAL_PULSE;
    	    } else if (residualType.equals("windowed")) {
    		addResidualMethod = ADD_RESIDUAL_WINDOWED;
    	    }
    	}
    	**/
    	frameSetInfo = (FrameSetInfo) sts.getFrameSetInfo();
    	if (frameSetInfo == null) {
    	    throw new IllegalStateException
    		("UnitConcatenator: FrameSetInfo does not exist");
    	}

    	lpcResult.setValues(frameSetInfo.getNumberOfChannels(),
    	        frameSetInfo.getSampleRate(),
    	        frameSetInfo.getResidualFold(),
    	        frameSetInfo.getCoeffMin(),
    	        frameSetInfo.getCoeffRange());

    	// create the array of final residual sizes
    	int[] residualSizes = lpcResult.getResidualSizes();

    	int samplesSize = 0;
    	if (lpcResult.getNumberOfFrames() > 0) {
    		samplesSize = targetTimes[lpcResult.getNumberOfFrames() - 1];
    	}
    	lpcResult.resizeResiduals(samplesSize);
    	
    	for (Iterator it = units.iterator();it.hasNext();) {
    	    SelectedUnit unit = (SelectedUnit) it.next();
    	    
    	    int unitSize = 
    	        sts.getUnitSize(unit.getUnitStart(), unit.getUnitEnd());
            // If we just force target duration, we basically lose intonation:
            // targetEnd = targetStart + unit.targetDurationInSamples();
    	    // So we use the unit durations from the DB:
            targetEnd = targetStart + unitSize;
            
    	    uIndex = 0;
    	    m = (float)unitSize/(float)(targetEnd - targetStart);
    	    numberFrames = lpcResult.getNumberOfFrames();
    	    
    	    // for all the pitchmarks that are required
    	    for (; (pmI < numberFrames) &&
    	    (targetTimes[pmI] <= targetEnd); pmI++) {
    	    	
    	    	Frame nextFrame = 
    	    	    sts.getNearestFrame(uIndex, 
                            ((ClusterUnit)(unit.getUnit())).start,
                            ((ClusterUnit)(unit.getUnit())).end);
    	    	lpcResult.setFrame(pmI, nextFrame.getFrameData());
    	    	
    	    	// Get residual by copying
    	    	residualSize = lpcResult.getFrameShift(pmI);
    	    	
    	    	residualSizes[pmI] = residualSize;
    	    	//sampleFile.skipBytes((int) 1);
    	    	byte[] residualData2 = nextFrame.getResidualData();
    	    			
    	    	if (addResidualMethod == ADD_RESIDUAL_PULSE) {
    	    		lpcResult.copyResidualsPulse
    				(residualData2, targetResidualPosition, residualSize);
    	    	} else {
    	    		lpcResult.copyResiduals
    				(residualData2, targetResidualPosition, residualSize);
    	    	}	
    		
    	    	targetResidualPosition += residualSize;
    	    	uIndex += ((float) residualSize * m);
    	    	}
    	    	targetStart = targetEnd;
    	    
    	}
    	lpcResult.setNumberOfFrames(pmI);
    	
    	
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
