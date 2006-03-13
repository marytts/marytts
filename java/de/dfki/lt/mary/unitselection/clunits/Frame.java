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
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * A single short term sample containing Residual Excited Linear Predictive
 * (RELP) frame and residual voice data.  
 */
public class Frame {
	//the audiodata
    private short[] frameData;
    private byte[]  residualData;
    //size of residualData
    private int residualSize;
    //size of frameData
    private int frameDataSize;
    //number of channels
    private int numChannels;
    //start position of frame data in buffer
    private int start;
    //start position of residual data in buffer
    private int startRes;
    //buffer containing the audiodata
    private ByteBuffer byteBuffer = null;
    
    private RandomAccessFile raf = null;

    private boolean haveFrameData = true;
    private boolean haveResData = true;

    
    /**
     * Loads the frame from the byte bufer
     *
     * @param bb the byte buffer to read the data from.
     * @param start start position of frame data in buffer
     * @param startRes start position of residual data in buffer
     * @param resSize maximum residual size of all samples in set
     * @param frameSize maximum frame size of all samples 
     *
     * @throws IOException if IO error occurs
     */
    public Frame (ByteBuffer bb, int start, int startRes, 
    			   int resSize, int frameSize) throws IOException {
    	this.byteBuffer = bb;
    	this.start = start;
    	this.startRes = startRes;
    	this.residualSize = resSize;
    	this.frameDataSize = frameSize;
    	haveFrameData = false;
    	haveResData = false;		  
    }    
    
    /**
     * Loads the frame from the given file
     *
     * @param raf the random access file to read the data from.
     * @param start start position of frame data in buffer
     * @param startRes start position of residual data in buffer
     * @param resSize maximum residual size of all samples in set
     * @param frameSize maximum frame size of all samples 
     *
     * @throws IOException if IO error occurs
     */
    public Frame (RandomAccessFile raf, int start, int startRes, 
    			   int resSize, int frameSize) throws IOException {
    	this.raf = raf;
    	this.start = start;
    	this.startRes = startRes;
    	this.residualSize = resSize;
    	this.frameDataSize = frameSize;
    	haveFrameData = false;
    	haveResData = false;		  
    }    
    
    /**
     * Reads a sample from the input reader. 
     * This constructor is only used for the conversion from FreeTTS
     * text format to Mary binary format
     * @param reader the input reader to read the data from
     * @param numChannels the number of channels per frame
     */
    public Frame(BufferedReader reader, int numChannels) {
    	try {   		
    		//read the FRAME data
    	    String line = reader.readLine();
    	    StringTokenizer tok = new StringTokenizer(line);
    	    //first token has to be "FRAME"
    	    if (!tok.nextToken().equals("FRAME")) {
    	    	throw new Error("frame Parsing sample error");
    	    }
    	    //the other tokens are frame data
    	    frameData = new short[numChannels];
    	    for (int i = 0; i < numChannels; i++) {
    	    	int svalue = Integer.parseInt(tok.nextToken()) - 32768;
    	
    	    	if ( svalue <  -32768 || svalue > 32767) {
    	    		throw new Error("data out of short range");
    	    	}
    	    	frameData[i] = (short) svalue;
    	    }

    	    //read the residual data
    	    line = reader.readLine();
    	    tok = new StringTokenizer(line);
    	    //first token has to be "RESIDUAL"
    	    if (!tok.nextToken().equals("RESIDUAL")) {
    	    	throw new Error("residual Parsing sample error");
    	    }
    	    //second token is the number of residuals
    	    residualSize = Integer.parseInt(tok.nextToken());
    	    //the other tokens are the residuals
    	    residualData = new byte[residualSize];
    	    for (int i = 0; i < residualSize; i++) {
    	    	int bvalue = Integer.parseInt(tok.nextToken()) - 128;
    	    	if ( bvalue < -128 || bvalue > 127) {
    	    		throw new Error("data out of byte range");
    	    	}
    	    	residualData[i] = (byte) bvalue;
    	    }
    	} catch (NoSuchElementException nse) {
    	    throw new Error("Parsing sample error " + nse.getMessage());
    	} catch (IOException ioe) {
    	    throw new Error("IO error while parsing sample" + ioe.getMessage());
    	} 
        }
    
    
    /**
     * Gets the frame data associated with this frame
     *
     * @return the frame data associated with this frame
     */
    public short[] getFrameData() {
    	
    	//if the data is not read in yet, read it in
    	if (!haveFrameData){
    		if (raf==null){
    			short[] frames = new short[frameDataSize];
    			byteBuffer.position(start);
    			for (int i = 0; i < frames.length; i++) {
    				frames[i] = byteBuffer.getShort();
    			}
    			return frames;}
    		else{
    			frameData = new short[frameDataSize];
    			try{
    				raf.seek(start);
    				for (int i = 0; i < frameData.length; i++) {
        				frameData[i] = raf.readShort();
        			}
    			haveFrameData = true;
    			return frameData;}
    			catch(Exception e){
    				e.printStackTrace();
    				throw new Error("Error reading frame data");}
    		}
      	}else{
    		return frameData;}
    }
    
    
    
    /**
     * Gets the residual data associated with this frame
     *
     * @return the residual data associated with this frame
     */
    public byte[] getResidualData() {
    	
    	//if the data is not read in yet, read it in
    	if (!haveResData){
    		if (raf==null){
    			byte[] res = new byte[residualSize];
    			byteBuffer.position(startRes);
    			for (int i = 0; i < res.length; i++) {
    				res[i] = byteBuffer.get();
    			}
    			return res;
    		}else{
    			try{
    			residualData = new byte[residualSize];
    			raf.seek(startRes);
    			for (int i = 0; i < residualData.length; i++) {
    				residualData[i] = raf.readByte();
    			}
    			haveResData = true;
    			return residualData;}
    			catch(Exception e){
    				e.printStackTrace();
    				throw new Error("Error reading residual data");}
    		}
    	}else{
    		return residualData;}
    }
    
    /**
     * Returns the number of residuals in this frame.
     *
     * @return the number of residuals in this frame
     */
    public int getResidualSize() {
    	return residualSize;
    }
    
    /**
     * Returns the number of LPC Coefficients in this frame.
     *
     * @return the number of residuals in this frame
     */
    public int getFrameSize() {
    	return frameData.length;
    }


    /**
     * Returns the normalized residual data. You may not want to 
     * call this function because of the overhead involved.
     *
     * @param which the index of the data of interest
     *
     * @return the normalized data.
     */
    public int getResidualData(int which) {
    	if (!haveResData){
    		byteBuffer.position(startRes+which);
    		return ((int) byteBuffer.get()) + 128;
    	}
    	else{
    	return ((int)residualData[which]) + 128;}
    }

    /**
     * Returns the normalized frame data. You may not want to 
     * call this function because of the overhead involved.
     *
     * @param which the index of the data of interest
     *
     * @return the normalized data.
     */
    public int getFrameData(int which) {
    	if (!haveFrameData){
    		byteBuffer.position(start+which);
    		return ((int) byteBuffer.getShort()) + 128;
    	}
    	return ((int)frameData[which]) + 32768;
    }

    /**
     * Dumps the frame to the given stream
     * writes the new .bin file format
     *
     * @param os the DataOutputStream to write the data to.
     * @param resSize the standard residual size
     * @param frameSize the standard frame size
     *      
     * @throws IOException if IO error occurs
     */
    public void dumpBinary(DataOutputStream os, 
    						int resSize, 
							int frameSize) 
    					throws IOException {
    	os.writeInt(frameSize);
    	for (int i = 0; i < frameData.length; i++) {
    		os.writeShort(frameData[i]);
    	}
    	os.writeInt(residualSize);
    	for (int i = 0; i < residualSize; i++) {
    		
    		os.writeByte(residualData[i]);
    	}
    	byte dummyByte = new Integer(0).byteValue();
    	for(int i = residualSize; i<resSize;i++){
    			os.writeByte(dummyByte);
    	}
    	//clean up 
    	frameData = null;
    	residualData = null;
    }
    

    /**
     * Compares two frames. Note that this is not the same as
     * "equals"
     *
     * @param other the other frame to compare this one to
     *
     * @return <code>true</code> if they compare; otherwise
     *     <code>false</code> 
     */
    public boolean compare(Frame other) {
    	if (!haveResData){
    		residualData = getResidualData();
    	}
    	if (!haveFrameData){
    		frameData = getFrameData();
    	}
    	if (frameData.length != other.getFrameData().length) {
    		return false;
    	}

    	for (int i = 0; i < frameData.length; i++) {
    		if (frameData[i]  != other.frameData[i]) {
    			return false;
    		}
    	}

    	if (residualData.length != other.residualData.length) {
    		return false;
    	}

    	for (int i = 0; i < residualData.length; i++) {
    		if (residualData[i]  != other.residualData[i]) {
    			return false;
    		}
    	}
    	return true;
    }
}
    


