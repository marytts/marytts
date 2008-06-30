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

package marytts.machinelearning;

import java.util.Arrays;

/**
 * @author oytun.turk
 *
 */
public class Cluster {
    public double[] meanVector;
    public double[][] covMatrix;
    public double[][] invCovMatrix; //This is not supported yet (requires matrix inversion)
    public boolean isDiagonalCovariance;
    
    public Cluster()
    {
        this(0, true);
    }
    
    public Cluster(int dimension, boolean isDiagonalCovariance)
    {
        allocate(dimension, isDiagonalCovariance);
    }
    
    public void allocate(int dimension, boolean isDiagonalCovarianceIn)
    {
        if (dimension>0)
        {
            isDiagonalCovariance = isDiagonalCovarianceIn;
            meanVector = new double[dimension];
            Arrays.fill(meanVector, 0.0);
            
            if (isDiagonalCovariance)
            {
                covMatrix = new double[1][];
                covMatrix[0] = new double[dimension];
                Arrays.fill(covMatrix[0], 0.0);
                
                invCovMatrix = new double[1][];
                invCovMatrix[0] = new double[dimension];
                Arrays.fill(invCovMatrix[0], 0.0);
            }
            else
            {
                covMatrix = new double[dimension][];
                for (int i=0; i<dimension; i++)
                {
                    covMatrix[i] = new double[dimension];
                    Arrays.fill(covMatrix[i], 0.0);
                }
                
                invCovMatrix = new double[dimension][];
                for (int i=0; i<dimension; i++)
                {
                    invCovMatrix[i] = new double[dimension];
                    Arrays.fill(invCovMatrix[i], 0.0);
                }
            }
        }
        else
        {
            meanVector = null;
            covMatrix = null;
            invCovMatrix = null;
        }
    }
    
    public double[] getCovarianceDiagonal()
    {
        double[] diagonal = null;
        
        if (covMatrix!=null && covMatrix[0]!=null && covMatrix[0].length>0)
        {
            diagonal = new double[covMatrix[0].length];
            if (isDiagonalCovariance)
                System.arraycopy(covMatrix[0], 0, diagonal, 0, covMatrix[0].length);
            else
            {
                for (int i=0; i<covMatrix.length; i++)
                    diagonal[i] = covMatrix[i][i];
            }
        }
        
        return diagonal;
    }
}
