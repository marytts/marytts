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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import de.dfki.lt.mary.unitselection.adaptation.GaussianOutlierEliminator;
import de.dfki.lt.mary.unitselection.adaptation.KMeansOutlierEliminator;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookOutlierEliminator {
    public static final int GAUSSIAN = 1;
    public static final int KMEANS = 2;
    
    private GaussianOutlierEliminator gaussian;
    private KMeansOutlierEliminator kmeans;
    
    public void run(WeightedCodebookTrainerParams params)
    {
        run(params.totalStandardDeviationsLsf,
            params.totalStandardDeviationsF0,
            params.totalStandardDeviationsDuration,
            params.totalStandardDeviationsEnergy, 
            params.outlierEliminatorType, 
            params.temporaryCodebookFile, 
            params.codebookFile);
    }
    public void run(double totalStandardDeviationsLsf,
                    double totalStandardDeviationsF0,
                    double totalStandardDeviationsDuration,
                    double totalStandardDeviationsEnergy,
                    int outlierEliminatorType, 
                    String codebookFileIn, 
                    String codebookFileOut)
    {
        if (outlierEliminatorType == WeightedCodebookOutlierEliminator.GAUSSIAN)
        {
            gaussian = new GaussianOutlierEliminator(totalStandardDeviationsLsf,
                                                     totalStandardDeviationsF0,
                                                     totalStandardDeviationsDuration,
                                                     totalStandardDeviationsEnergy);
            
            gaussian.eliminate(codebookFileIn, codebookFileOut);
        }
        else if (outlierEliminatorType == WeightedCodebookOutlierEliminator.KMEANS)
        {
            /*
            kmeans = new KMeansOutlierEliminator(params.totalStandardDeviations);
            kmeans.run();
            */
        }
    }

}
