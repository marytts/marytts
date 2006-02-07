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

import java.io.IOException;
import java.io.DataOutputStream;

/**
 * Represents the frame and residual data
 * used by the diphone database
 * used Residual Excited Linear Predictive synthesizer
 */
public abstract class FrameSet {
	//an Array containing all samples
    protected Frame[] frames;
    //general info about all samples
    protected FrameSetInfo frameSetInfo;

    /** Empty Constructor for inheritance**/
    public FrameSet(){}
    
    /**
     * Dumps this frame set to the given stream
     * writes the new .bin file format
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs.
     */
    public void dumpBinary_new(DataOutputStream os) throws IOException {
	frameSetInfo.dumpBinary(os);
	int samplesLength = frames.length;
	os.writeInt(samplesLength);
	int residualSize = 0;
	int frameSize = 0;
	for (int i = 0; i<samplesLength; i++){
		 if (frames[i].getResidualSize()>residualSize){
	    	residualSize = frames[i].getResidualSize();}
		 if (frames[i].getFrameSize()>frameSize){
	    	frameSize = frames[i].getFrameSize();}
	}
	os.writeInt(frameSize);
	os.writeInt(residualSize);
	for (int i = 0; i < samplesLength; i++) {
	    frames[i].dumpBinary(os, residualSize, frameSize);
	}
    }

   

    /**
     * return the frame associated with the index
     *
     * @param index the index of the frame
     *
     * @return the frame.
     */
    public abstract Frame getFrame(int index) ;

    /**
     * Retrieves the info on this FrameSet
     *
     * @return the frame set info
     */
    public FrameSetInfo getFrameSetInfo() {
    	return frameSetInfo;
    }


    /**
     * Returns the size of the unit represented
     * by the given start and end points
     *
     * @param start the start of the unit
     * @param end the end of the unit
     *
     * @return the size of the unit
     */
    public abstract int getUnitSize(int start, int end) ;


    /**
     * Gets the size of the given frame
     *
     * @param frame the frame of interest
     *
     * @return the size of the frame
     */
    public abstract int getFrameSize(int frame);
    
    /**
     * Retrieves the nearest frame.
     *
     * @param index the ideal index
     *
     * @return the nearest frame
     */
    public Frame getNearestFrame(float index, int start, int end) {
    	int i, iSize = 0, nSize;

    	// loop through all the Samples in this unit
    	for (i = start; i < end; i++) {
    		Frame frame = getFrame(i);
    		nSize = iSize + frame.getResidualSize();

    		if (Math.abs(index - (float) iSize) <
    				Math.abs(index - (float) nSize)) {
    			return frame;
    		}
    		iSize = nSize;
    	}
    	return getFrame(end - 1);
    }
    
}
    
