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

package de.dfki.lt.machinelearning;

import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 * Single Gaussian component
 */
public class GaussianComponent {
    public double[] meanVector;
    public double[][] covMatrix;
    
    //These are used in pdf computation
    private double[][] invCovMatrix;
    private double detCovMatrix;
    private double constantTerm;
    private double constantTermLog;
    //
    
    public GaussianComponent()
    {
        this(0, true);
    }
    
    public GaussianComponent(int featureDimensionIn, boolean isDiagonal)
    {
        init(featureDimensionIn, isDiagonal);
    }
    
    public GaussianComponent(GaussianComponent existing)
    {        
        init(existing.meanVector, existing.covMatrix);
    }
    
    public GaussianComponent(Cluster c)
    {
        init(c.meanVector, c.covMatrix);
    }
    
    public void init(int featureDimensionIn, boolean isDiagonal)
    {
        if (featureDimensionIn>0)
        {
            meanVector = new double[featureDimensionIn];
            if (isDiagonal)
                covMatrix = new double[1][featureDimensionIn];
            else
                covMatrix = new double[featureDimensionIn][featureDimensionIn];
        }
        else
        {
            meanVector = null;
            covMatrix = null;
        }
    }
    
    public void init(double[] meanVectorIn, double[][] covMatrixIn)
    {
        setMeanVector(meanVectorIn);
        setCovMatrix(covMatrixIn);
        
        for (int i=0; i<covMatrix.length; i++)
            assert meanVector.length == covMatrix[i].length;
    }
    
    public void setMeanVector(double[] meanVectorIn)
    {
        if (meanVectorIn!=null)
        {
            meanVector = new double[meanVectorIn.length];
            System.arraycopy(meanVectorIn, 0, meanVector, 0, meanVectorIn.length);
        }
        else
            meanVector = null;
    }
    
    public void setCovMatrix(double[][] covMatrixIn)
    {
        if (covMatrixIn!=null)
        {
            covMatrix = new double[covMatrixIn.length][];

            for (int i=0; i<covMatrixIn.length; i++)
            {
                covMatrix[i] = new double[covMatrixIn[i].length];
                System.arraycopy(covMatrixIn[i], 0, covMatrix[i], 0, covMatrixIn[i].length);
            }
        }
        else
            covMatrix = null;

        setDerivedValues();
    }
    
    //Computes the inverse covariance, determinant, constant term to be used in pdf evalutaion
    public void setDerivedValues()
    {
        if (covMatrix!=null)
        {
            invCovMatrix = MathUtils.inverse(covMatrix);
            detCovMatrix = MathUtils.determinant(covMatrix);
            constantTerm = MathUtils.getGaussianPdfValueConstantTerm(covMatrix[0].length, detCovMatrix);
            constantTermLog = MathUtils.getGaussianPdfValueConstantTermLog(covMatrix[0].length, detCovMatrix);
        }
        else
        {
            invCovMatrix = null;
            detCovMatrix = 0.0;
            constantTerm = 0.0;
            constantTermLog = 0.0;
        }  
    }
    
    public boolean isDiagonalCovariance()
    {
        if (meanVector!=null && covMatrix!=null)
        {
            if (covMatrix.length==1 && meanVector.length>1 && covMatrix[0].length==meanVector.length)
                return true;
        }
        
        return false;
    }
    
    public double[] getCovMatrixDiagonal()
    {
        if (covMatrix!=null)
            return covMatrix[0];
        else
            return null;
    }
    
    public double[][] getInvCovMatrix()
    {
        return invCovMatrix;
    }
    
    public double getDetCovMatrix()
    {
        return detCovMatrix;
    }
    
    public double getConstantTerm()
    {
        return constantTerm;
    }
    
    public double getConstantTermLog()
    {
        return constantTermLog;
    }
}
