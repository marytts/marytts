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

import marytts.machinelearning.ContextualGMMParams;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;

/**
 * Joint-GMM voice conversion training
 * Both parallel and non-parallel training should derive from this class
 * 
 * @author Oytun T&uumlrk
 */
public class JointGMMTrainer extends BaselineTrainer {

    protected WeightedCodebookTrainerParams codebookTrainerParams;
    protected JointGMMTrainerParams gmmTrainerParams;
    protected ContextualGMMParams cgParams;
    
    public JointGMMTrainer(BaselinePreprocessor pp,
                           BaselineFeatureExtractor fe,
                           WeightedCodebookTrainerParams pa,
                           JointGMMTrainerParams gp,
                           ContextualGMMParams cg) 
    {
        super(pp, fe);
        
        codebookTrainerParams = new WeightedCodebookTrainerParams(pa);
        gmmTrainerParams = new JointGMMTrainerParams(gp);
        cgParams = new ContextualGMMParams(cg);
    }
            
}
