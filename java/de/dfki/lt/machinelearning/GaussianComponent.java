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

import java.io.IOException;

import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
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
        setMeanVector(meanVectorIn, 0, meanVectorIn.length);
    }
    
    public void setMeanVector(double[] bigVector, int startIndex, int meanLength)
    {
        if (bigVector!=null && meanLength>0)
        {
            if (startIndex+meanLength>bigVector.length)
                meanLength = bigVector.length-startIndex;

            if (meanVector==null || meanLength!=meanVector.length)
                meanVector = new double[meanLength];

            for (int i=0; i<meanLength; i++)
                meanVector[i] = bigVector[startIndex+i];
        }
        else
            meanVector = null;
    }
    
    public void setCovMatrix(double[][] covMatrixIn)
    {
        if (covMatrixIn.length==1)
            setCovMatrix(covMatrixIn, 0, 0, covMatrixIn[0].length);
        else
            setCovMatrix(covMatrixIn, 0, 0, covMatrixIn.length);
    }
    
    public void setCovMatrix(double[][] bigCovMatrix, int rowStartIndex, int colStartIndex, int covLength)
    {
        if (bigCovMatrix!=null && covLength>0)
        {
            if (bigCovMatrix.length==1) //Diagonal
            {
                int startIndex = Math.max(rowStartIndex, colStartIndex);
                if (startIndex+covLength>bigCovMatrix[0].length)
                    covLength = bigCovMatrix[0].length-startIndex;
                
                if (covMatrix==null || covMatrix.length>1 || covMatrix[0].length!=covLength)
                    covMatrix = new double[1][covLength];
                
                System.arraycopy(bigCovMatrix[0], startIndex, covMatrix[0], 0, covLength);
            }
            else //Full
            {
                int i, j;
                for (i=0; i<bigCovMatrix.length; i++)
                {
                    if (colStartIndex+covLength>bigCovMatrix[i].length)
                        covLength = bigCovMatrix[i].length-colStartIndex;
                }
                
                if (rowStartIndex+covLength>bigCovMatrix.length)
                    covLength = bigCovMatrix.length-rowStartIndex;
                
                if (covMatrix==null)
                    covMatrix = new double[covLength][];
                    
                for (i=rowStartIndex; i<rowStartIndex+covLength; i++)
                {
                    if (covMatrix[i-rowStartIndex]==null || covMatrix[i-rowStartIndex].length!=covLength)
                        covMatrix[i-rowStartIndex] = new double[covLength];
                    
                    for (j=colStartIndex; j<colStartIndex+covLength; j++)
                        covMatrix[i-rowStartIndex][j-colStartIndex] = bigCovMatrix[i][j];
                }
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
    
    public void write(MaryRandomAccessFile stream) throws IOException
    {
        if (meanVector!=null)
        {
            stream.writeInt(meanVector.length);
            stream.writeDouble(meanVector);
        }
        else
            stream.writeInt(0);
         
        int i;
        
        if (covMatrix!=null)
            stream.writeInt(covMatrix.length);
        else
            stream.writeInt(0);
            
        if (covMatrix!=null)
        { 
            for (i=0; i<covMatrix.length; i++)
            {
                if (covMatrix[i]!=null)
                {
                    stream.writeInt(covMatrix[i].length);
                    stream.writeDouble(covMatrix[i]);
                }
                else
                    stream.writeInt(0);
            }
        }
        
        if (invCovMatrix!=null)
            stream.writeInt(invCovMatrix.length);
        else
            stream.writeInt(0);
            
        if (invCovMatrix!=null)
        { 
            for (i=0; i<invCovMatrix.length; i++)
            {
                if (invCovMatrix[i]!=null)
                {
                    stream.writeInt(invCovMatrix[i].length);
                    stream.writeDouble(invCovMatrix[i]);
                }
                else
                    stream.writeInt(0);
            }
        }
        
        stream.writeDouble(detCovMatrix);
        stream.writeDouble(constantTerm);
        stream.writeDouble(constantTermLog);
    }
    
    public void read(MaryRandomAccessFile stream) throws IOException
    {
        int tmpLen, tmpLen2;
        tmpLen = stream.readInt();
        
        if (tmpLen>0)
            meanVector = stream.readDouble(tmpLen);
        else
            meanVector = null;
         
        int i;
        
        tmpLen = stream.readInt();
         
        if (tmpLen>0)
        { 
            covMatrix = new double[tmpLen][];
            
            for (i=0; i<tmpLen; i++)
            {
                tmpLen2 = stream.readInt();
                if (tmpLen2>0)
                    covMatrix[i] = stream.readDouble(tmpLen2);
                else
                    covMatrix[i] = null;
            }
        }
        else
            covMatrix = null;
        
        tmpLen = stream.readInt();
        
        if (tmpLen>0)
        { 
            invCovMatrix = new double[tmpLen][];

            for (i=0; i<tmpLen; i++)
            {
                tmpLen2 = stream.readInt();
                if (tmpLen2>0)
                    invCovMatrix[i] = stream.readDouble(tmpLen2);
                else
                    invCovMatrix[i] = null;
            }
        }
        else
            invCovMatrix = null;
        
        detCovMatrix = stream.readDouble();
        constantTerm = stream.readDouble();
        constantTermLog = stream.readDouble();
    }
    
    public double probability(double[] x)
    {
        double P;
        if (covMatrix.length==1) //Diagonal
            P = MathUtils.getGaussianPdfValue(x, meanVector, covMatrix[0], getConstantTerm());
        else //Full-covariance
            P = MathUtils.getGaussianPdfValue(x, meanVector, getDetCovMatrix(), getInvCovMatrix());
        
        return P;
    }
}
