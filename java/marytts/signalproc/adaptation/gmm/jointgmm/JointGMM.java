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

package marytts.signalproc.adaptation.gmm.jointgmm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import marytts.machinelearning.GMM;
import marytts.signalproc.adaptation.VocalTractTransformationData;
import marytts.signalproc.adaptation.codebook.WeightedCodebook;
import marytts.signalproc.adaptation.codebook.WeightedCodebookMapperParams;
import marytts.signalproc.adaptation.codebook.WeightedCodebookMatch;
import marytts.signalproc.adaptation.gmm.GMMMatch;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.util.MaryRandomAccessFile;
import marytts.util.MathUtils;


/**
 * @author oytun.turk
 *
 * This class is the dual of WeightedCodebook class in codebook mapping
 * 
 */
public class JointGMM { 
   public GMM source; //Full GMM for source
   public GMM targetMeans; //Means for target
   public GMM covarianceTerms; //Cross-covariance terms required for transformation
                                      // Cov(y,x)_i * inverse(Cov(x,x)_i)
   public LsfFileHeader lsfParams;
   
   public JointGMM(JointGMM existing)
   {
       if (existing!=null)
       {
           source = new GMM(existing.source);
           targetMeans = new GMM(existing.targetMeans);
           covarianceTerms = new GMM(existing.covarianceTerms);
           lsfParams = new LsfFileHeader(existing.lsfParams);
       }
       else
       {
           source = null;
           targetMeans = null;
           covarianceTerms = null;
           lsfParams = null;
       }
   }
   
   public JointGMM(GMM gmm, LsfFileHeader lsfParamsIn)
   {
       if (gmm!=null && gmm.featureDimension>0)
       {
           int actualFeatureDimension = (int)Math.floor(gmm.featureDimension*0.5 + 0.5);
           source = new GMM(actualFeatureDimension, gmm.totalComponents, gmm.isDiagonalCovariance);
           targetMeans = new GMM(actualFeatureDimension, gmm.totalComponents, true);
           covarianceTerms = new GMM(actualFeatureDimension, gmm.totalComponents, gmm.isDiagonalCovariance);
           source.info = gmm.info;
           targetMeans.info = gmm.info;
           covarianceTerms.info = gmm.info;
           
           int i, j;
           for (i=0; i<gmm.totalComponents; i++)
           {
               source.components[i].setMeanVector(gmm.components[i].meanVector, 0, actualFeatureDimension);
               source.components[i].setCovMatrix(gmm.components[i].covMatrix, 0, 0, actualFeatureDimension);
               targetMeans.components[i].setMeanVector(gmm.components[i].meanVector, actualFeatureDimension, actualFeatureDimension);
               
               //Set to  Cov(y,x)_i
               covarianceTerms.components[i].setCovMatrix(gmm.components[i].covMatrix, actualFeatureDimension, 0, actualFeatureDimension);
               //Multiply with inverse(Cov(x,x)_i)
               covarianceTerms.components[i].covMatrix = MathUtils.matrixProduct(covarianceTerms.components[i].covMatrix, source.components[i].getInvCovMatrix());
           }
           
           lsfParams = new LsfFileHeader(lsfParamsIn);
       }
       else
       {
           source = null;
           targetMeans = null;
           covarianceTerms = null;
           lsfParams = null;
       }
   }
   
   public JointGMM(MaryRandomAccessFile stream)
   {
       read(stream);
   }
   
   public void write(MaryRandomAccessFile stream)
   {
       if (stream != null)
       {
           try {
               lsfParams.writeLsfHeader(stream);
           } catch (IOException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
           }

           if (stream!=null)
           {
               try {
                   source.write(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
               try {
                   targetMeans.write(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
               try {
                   covarianceTerms.write(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }
           }
       }
   }

   public void read( MaryRandomAccessFile stream)
   {
       if (stream!=null)
       {
           lsfParams = new LsfFileHeader();
           try {
               lsfParams.readLsfHeader(stream);
           } catch (IOException e1) {
               // TODO Auto-generated catch block
               e1.printStackTrace();
           }

           if (stream!=null)
           {
               if (source==null)
                   source = new GMM();

               try {
                   source.read(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }

               if (targetMeans==null)
                   targetMeans = new GMM();

               try {
                   targetMeans.read(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }

               if (covarianceTerms==null)
                   covarianceTerms = new GMM();

               try {
                   covarianceTerms.read(stream);
               } catch (IOException e) {
                   // TODO Auto-generated catch block
                   e.printStackTrace();
               }

           }
       }
   }
}
