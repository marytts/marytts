/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.signalproc.process;

/**
 * @author marc
 *
 */
public interface InlineFrameMerger extends InlineDataProcessor
{
    /**
     * Set the frame of data to merge into the next call of applyInline().
     * @param frameToMerge
     */
    public void setFrameToMerge(double[] frameToMerge);
    
    /**
     * Set the frame of data to merge into the next call of applyInline().
     * This method allows for an interpolation of two frames to be merged into the data set;
     * for example, in order to correct for time misalignment between signal and other frames.
     * @param frame1 
     * @param frame2
     * @param relativeWeightFrame1, a number between 0 and 1 indicating the relative weight of frame1^
     * with respect to frame2. Consequently, the relative weight of frame 2 will be (1 - relativeWeightFrame1).
     */
    public void setFrameToMerge(double[] frame1, double[] frame2, double relativeWeightFrame1);
}
