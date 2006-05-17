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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.DataOutputStream;
import java.util.StringTokenizer;

/**
 * Represents the frame and residual data
 * used by the diphone database
 * used Residual Excited Linear Predictive synthesizer
 */
public class FrameSet {
	//an Array containing all samples
    protected Frame[] frames;
    //general info about all samples
    protected FrameSetInfo frameSetInfo;
    
    //for converting from .txt to .bin

    /** Empty Constructor for inheritance**/
    public FrameSet(){}
    
    /**
     * Reads a SampleSet from the input reader. 
     *
     * @param tok tokenizer that holds parameters for this SampleSet
     * @param reader the input reader to read the data from
     */
    public FrameSet(StringTokenizer tok, BufferedReader reader) {
    	try {
    		
    	    int numSamples = Integer.parseInt(tok.nextToken());
    	    int numChannels = Integer.parseInt(tok.nextToken());
    	    int sampleRate = Integer.parseInt(tok.nextToken());
    	    float coeffMin = Float.parseFloat(tok.nextToken());
    	    float coeffRange = Float.parseFloat(tok.nextToken());
    	    float postEmphasis = Float.parseFloat(tok.nextToken());
    	    int residualFold = Integer.parseInt(tok.nextToken());
    	    
    	    frames = new Frame[numSamples];
	    	frameSetInfo = new FrameSetInfo(sampleRate, numChannels,
	    			residualFold, coeffMin, coeffRange, postEmphasis);
    	  
    	    for (int i = 0; i < numSamples; i++) {
    	        try{
    		    frames[i] = new Frame(reader, numChannels,i);
    		    
    	        } catch (Exception e){
    	            e.printStackTrace();
    	            throw new Error("Error parsing frame "+i);
    	        }
    	    }
    	    
    	    } catch (Exception nse) {
    	    	throw new Error("Parsing frame error " + nse.getMessage());
    	    }
        }
    
    
    /**
     * Dumps this frame set to the given stream
     * writes the Mary .bin file format
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs.
     */
    public void dumpBinary(DataOutputStream os) throws IOException {
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
	frames = null;
	System.gc();
    }

   

    /**
     * return the frame associated with the index
     *
     * @param index the index of the frame
     *
     * @return the frame.
     */
    public Frame getFrame(int index) {
        return frames[index];}

    /**
     * Retrieves the info on this FrameSet
     *
     * @return the frame set info
     */
    public FrameSetInfo getFrameSetInfo() {
    	return frameSetInfo;
    }

    public int getSamplingRate(){
        return frameSetInfo.getSampleRate();
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
    public int getUnitSize(int start, int end) 
    {
        return -1;
    }


    /**
     * Gets the size of the given frame
     *
     * @param frame the frame of interest
     *
     * @return the size of the frame
     */
    public int getFrameSize(int frame)
    {
        return -1;
    }
    
    /**
     * Retrieves the nearest frame.
     *
     * @param index the ideal sample position for the frame start, relative to unit start, in samples
     * @param start first frame belonging to the relevant unit
     * @param end frame after the last frame belonging to the relevant unit
     *
     * @return the nearest frame
     */
    public Frame getNearestFrame(float index, int start, int end) {
    	int frameIndex = 0, nextFrameIndex;
        // frame index positions are in samples relative to unit start

    	// loop through all the Frames in this unit
    	for (int i = start; i < end; i++) {
    		nextFrameIndex = frameIndex + getFrameSize(i);
    		if (Math.abs(index - frameIndex) < Math.abs(index - nextFrameIndex)) {
    			return getFrame(i);
    		}
    		frameIndex = nextFrameIndex;
    	}
    	return getFrame(end - 1);
    }
    
}
    
