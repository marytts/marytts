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


import java.nio.ByteBuffer;
import java.io.*;


/**
 * Describes global frame set parameters. A frame set info is
 * generally added
 * to an utterance to describe the type of unit data that has been
 * generated.
 *
 */
public class FrameSetInfo {
    public final static String UTT_NAME = "FrameSetInfo";

    private final int sampleRate;
    private final int numberOfChannels;
    private final int residualFold;
    private final float coeffMin;
    private final float coeffRange;
    private final float postEmphasis;
    
    
    
    /**
     * Constructs a sample info from the given byte buffer.
     *
     * @param bb the byte buffer
     *
     * @throws IOException if an input error occurs
     */
    public FrameSetInfo(ByteBuffer bb) throws IOException {
	numberOfChannels = bb.getInt();
	sampleRate = bb.getInt();
	coeffMin = bb.getFloat();
	coeffRange = bb.getFloat();
	postEmphasis = bb.getFloat();
	residualFold = bb.getInt();
    }

    /**
     * Creates a new FrameSetInfo from the given parameters
     * This constructor is only used for the conversion from FreeTTS
     * text format to Mary binary format
     */
    public FrameSetInfo(int sampleRate, int numberOfChannels,
	    int residualFold, float coeffMin, 
	    float coeffRange, float postEmphasis) {
	this.sampleRate = sampleRate;
	this.numberOfChannels = numberOfChannels;
	this.residualFold = residualFold;
	this.coeffMin = coeffMin;
	this.coeffRange = coeffRange;
	this.postEmphasis = postEmphasis;
    }
    
    /**
     * Returns the sample rate.
     *
     * @return the sample rate
     */
    public final int getSampleRate() {
	return sampleRate;
    }
    
    /**
     * Returns the number of channels.
     *
     * @return the number of channels.
     */
    public final int getNumberOfChannels() {
	return numberOfChannels;
    }

    /**
     * Returns the residual fold.
     *
     * @return the residual fold
     */
    public final int getResidualFold() {
	return residualFold;
    }
    
    /**
     * Returns the minimum for linear predictive coding.
     *
     * @return the minimum for linear predictive coding.
     */
    public final float getCoeffMin() {
	return coeffMin;
    }

    /**
     * Returns the range for linear predictive coding.
     *
     * @return the range for linear predictive coding.
     */
    public final float getCoeffRange() {
	return coeffRange;
    }

    /**
     * Returns the post emphasis
     *
     * @return the post emphasis
     */
    public final float getPostEmphasis() {
	return postEmphasis;
    }

    
    /**
     * Dump a binary form of the sample rate
     * to the given output stream
     *
     * @param os the output stream
     * 
     * @throws IOException if an error occurs
     */
    public void dumpBinary(DataOutputStream os) throws IOException {
	os.writeInt(numberOfChannels);
	os.writeInt(sampleRate);
	os.writeFloat(coeffMin);
	os.writeFloat(coeffRange);
	os.writeFloat(postEmphasis);
	os.writeInt(residualFold);
    }
}

    
