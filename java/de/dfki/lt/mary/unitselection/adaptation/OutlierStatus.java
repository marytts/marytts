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

package de.dfki.lt.mary.unitselection.adaptation;

/**
 * @author oytun.turk
 *
 */
public class OutlierStatus {
    public static final int NON_OUTLIER      = Integer.parseInt("00000000", 2);
    public static final int LSF_OUTLIER      = Integer.parseInt("00000001", 2);
    public static final int F0_OUTLIER       = Integer.parseInt("00000010", 2);
    public static final int DURATION_OUTLIER = Integer.parseInt("00000100", 2);
    public static final int ENERGY_OUTLIER   = Integer.parseInt("00001000", 2);
    public static final int GENERAL_OUTLIER  = Integer.parseInt("00010000", 2);
    
    public int totalNonOutliers;
    public int totalLsfOutliers;
    public int totalF0Outliers;
    public int totalDurationOutliers;
    public int totalEnergyOutliers;
    public int totalGeneralOutliers;
    
    public OutlierStatus()
    {
       init();
    }
    
    public void init()
    {
        totalNonOutliers = 0;
        totalLsfOutliers = 0;
        totalF0Outliers = 0;
        totalDurationOutliers = 0;
        totalEnergyOutliers = 0;
        totalGeneralOutliers = 0;
    }
}
