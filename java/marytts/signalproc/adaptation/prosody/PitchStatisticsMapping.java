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

package marytts.signalproc.adaptation.prosody;

import java.util.Arrays;

import marytts.util.math.MathUtils;


/**
 * @author oytun.turk
 *
 */
public class PitchStatisticsMapping {
    public PitchStatistics sourceGlobalStatisticsHz;
    public PitchStatistics targetGlobalStatisticsHz;
    public PitchStatisticsCollection sourceLocalStatisticsHz;
    public PitchStatisticsCollection targetLocalStatisticsHz;
    public PitchStatistics sourceGlobalStatisticsLogHz;
    public PitchStatistics targetGlobalStatisticsLogHz;
    public PitchStatisticsCollection sourceLocalStatisticsLogHz;
    public PitchStatisticsCollection targetLocalStatisticsLogHz;
    public double[] sourceVariancesHz;
    public double[] targetVariancesHz;
    public double[] sourceVariancesLogHz;
    public double[] targetVariancesLogHz;
    
    public PitchStatisticsMapping(PitchStatisticsCollection allFromTraining)
    {
        PitchStatisticsCollection tmpCollection = null;
        //Hertz statistics
        sourceGlobalStatisticsHz = new PitchStatistics(allFromTraining.getGlobalStatisticsSourceHz());
        targetGlobalStatisticsHz = new PitchStatistics(allFromTraining.getGlobalStatisticsTargetHz());   
        sourceLocalStatisticsHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsSourceHz());
        targetLocalStatisticsHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsTargetHz()); 
        sourceVariancesHz = setVariances(sourceLocalStatisticsHz);
        targetVariancesHz = setVariances(targetLocalStatisticsHz);
        
        //Log Hertz statistics
        sourceGlobalStatisticsLogHz = new PitchStatistics(allFromTraining.getGlobalStatisticsSourceLogHz());
        targetGlobalStatisticsLogHz = new PitchStatistics(allFromTraining.getGlobalStatisticsTargetLogHz()); 
        sourceLocalStatisticsLogHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsSourceLogHz());
        targetLocalStatisticsLogHz = new PitchStatisticsCollection(allFromTraining.getLocalStatisticsTargetLogHz()); 
        sourceVariancesLogHz = setVariances(sourceLocalStatisticsLogHz);
        targetVariancesLogHz = setVariances(targetLocalStatisticsLogHz);
    }
    
    private double[] setVariances(PitchStatisticsCollection p)
    {
        double[] variances = new double[5];
        if (p.entries!=null)
        {
            if (p.entries.length<2)
                Arrays.fill(variances, 1.0);
            else
            {
                int i, j;
                double[][] vals = new double[variances.length][];
                for (i=0; i<variances.length; i++)
                    vals[i] = new double[p.entries.length];
                
                for (j=0; j<p.entries.length; j++)
                    vals[0][j] = p.entries[j].mean;
                
                for (j=0; j<p.entries.length; j++)
                    vals[1][j] = p.entries[j].standardDeviation;
                
                for (j=0; j<p.entries.length; j++)
                    vals[2][j] = p.entries[j].range;
                
                for (j=0; j<p.entries.length; j++)
                    vals[3][j] = p.entries[j].intercept;
                
                for (j=0; j<p.entries.length; j++)
                    vals[4][j] = p.entries[j].slope;
                
                variances = MathUtils.getVarianceRows(vals);
            }
        }
        
        return variances;
    }
}
