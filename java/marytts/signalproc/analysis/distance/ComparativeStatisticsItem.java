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

package marytts.signalproc.analysis.distance;

import marytts.util.io.FileUtils;

/**
 * @author Oytun T&uumlrk
 */
public class ComparativeStatisticsItem {
    public StatisticsItem referenceVsMethod1;
    public StatisticsItem referenceVsMethod2;
    
    public ComparativeStatisticsItem(double[] x1, double[] x2)
    {
        referenceVsMethod1 = new StatisticsItem(x1);
        referenceVsMethod2 = new StatisticsItem(x2);
    }
    
    public void writeToTextFile(String textFile)
    {
        double[] tmpOut = new double[5];
        tmpOut[0] = referenceVsMethod1.mean;
        tmpOut[1] = referenceVsMethod1.std;
        tmpOut[2] = referenceVsMethod2.mean;
        tmpOut[3] = referenceVsMethod1.std; 
        tmpOut[4] = referenceVsMethod1.mean-referenceVsMethod2.mean;

        FileUtils.writeToTextFile(tmpOut, textFile);
    }
}
