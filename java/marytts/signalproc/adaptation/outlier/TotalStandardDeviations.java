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

package marytts.signalproc.adaptation.outlier;

/**
 * 
 * @author oytun.turk
 * 
 * Class for keeping total standard deviations to be used in automatic thresholding in outlier elimation
 *
 */
public class TotalStandardDeviations {
    public double general;
    public double lsf;
    public double f0;
    public double duration;
    public double energy;
    public static final double DEFAULT_TOTAL_STANDARD_DEVIATIONS = 1.5;
    
    public TotalStandardDeviations()
    {
        general = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
        lsf = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
        f0 = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
        duration = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
        energy = DEFAULT_TOTAL_STANDARD_DEVIATIONS;
    }
    
    public TotalStandardDeviations(TotalStandardDeviations existing)
    {
        general = existing.general;
        lsf = existing.lsf;
        f0 = existing.f0;
        duration = existing.duration;
        energy = existing.energy;
    }
}
