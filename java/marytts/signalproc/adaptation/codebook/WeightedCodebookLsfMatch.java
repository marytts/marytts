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

package marytts.signalproc.adaptation.codebook;

/**
 * 
 * @author oytun.turk
 *
 * Wrapper class for a single weighted codebook entry LSF match
 *
 */
public class WeightedCodebookLsfMatch {
    public WeightedCodebookEntry entry;
    public int[] indices;
    public double[] weights;
    public int totalMatches; //This is less than or equal to the length of the above items (i.e. number of
                             // best codebook matches). Can be lower when the codebook has less entries
    
    public WeightedCodebookLsfMatch(int numMaxMatches, int lpOrder)
    {
        init(numMaxMatches, lpOrder);
    }
    
    //Create a dummy match to use original input lsfs 
    // (Should not be used in real codebook matching but only for control purposes)
    public WeightedCodebookLsfMatch(double[] sourceLsfs, double[] targetLsfs)
    {
        init(1, sourceLsfs.length);
        entry = new WeightedCodebookEntry(sourceLsfs, targetLsfs, null, null);
    }
    
    public void init(int numMaxMatches, int lpOrder)
    {
        if (numMaxMatches>0 && lpOrder>0)
        {
            entry = new WeightedCodebookEntry(lpOrder, 0);
            indices = new int[numMaxMatches];
            weights = new double[numMaxMatches];
            totalMatches = numMaxMatches;
        }
        else
        {
            entry = null;
            indices = null;
            weights = null;
            totalMatches = 0;
        }
    }
}
