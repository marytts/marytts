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

import java.io.*;
import java.nio.ByteBuffer;

/**
 * 
 * Represents the frame and residual data
 * used by the diphone database
 * used Residual Excited Linear Predictive synthesizer
 * currently only reading .bin file from RandomAccessFile
 * 
 * @author Anna Hunecke, DFKI Saarbruecken
 * 
 */
public class FiledFrameSet extends FrameSet{
	
    private int samplesStart;
    private int sampleSize;
    private int frameSize;
    private int residualSize;
    private RandomAccessFile raf;
    private int[] residualSizes;
    
    /**
     * Creates a FrameSet by reading it from the given random access file
     *
     * @param raf source of the Unit data
     * 
     * @throws IOException if an IO error occurs
     */
    public FiledFrameSet(RandomAccessFile raf) throws IOException
    {
    	super();
    	this.raf = raf;
    	byte[] ba = new byte[24];
    	raf.read(ba);
    	frameSetInfo = new FrameSetInfo(ByteBuffer.wrap(ba));
    	int numSamples = raf.readInt();
    	frameSize = raf.readInt();
    	residualSize = raf.readInt();
    	frames = new Frame[numSamples];
    	residualSizes = new int[numSamples];
    	samplesStart = (int) raf.getFilePointer();
    	sampleSize = 8+frameSize*2+residualSize;
    	int lastPosition = 4+frameSize*2;
    	raf.skipBytes(lastPosition);
    	//if max residual Size of all samples is 0,
    	//we are reading in MCEP samples
    	//and do not need residuals
    	if(residualSize == 0){
			raf.skipBytes((numSamples-1)*sampleSize+4);
			for (int j=1;j<numSamples;j++){
				residualSizes[j]=0;}
		}
    	//else read in the residual sizes of the samples
    	else{for (int i=0;i<numSamples;i++){
    		residualSizes[i]=raf.readInt();
    		if(i+1<numSamples){
    		raf.skipBytes(sampleSize-4);}
    		else{raf.skipBytes(residualSize);}
    	}}
    } 
    
    /**
     * return the frame associated with the index
     *
     * @param index the index of the frame
     *
     * @return the frame.
     */
    public Frame getFrame(int index) {
        if (frames[index] == null) {
            try {
    			int start = samplesStart + sampleSize*index+4;
    			int startRes = start+frameSize*2+4;
    			frames[index] =
    				new Frame(raf, start,startRes,residualSizes[index],frameSize);
            }catch(Exception e){
                e.printStackTrace();
                throw new Error ("Error building sample "+index);
            }
        }
        return frames[index];
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
    public int getUnitSize(int start, int end) {
    	
    	int size = 0;

    	for (int i = start; i < end; i++) {
    	    size += residualSizes[i];
    	}
    	return size;
    }


    /**
     * Gets the size of the given frame
     *
     * @param frame the frame of interest
     *
     * @return the size of the frame
     */
    public int getFrameSize(int frame) {
	return  residualSizes[frame];
    }
    
}
    
