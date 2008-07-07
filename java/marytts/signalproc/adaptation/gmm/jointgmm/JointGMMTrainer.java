package marytts.signalproc.adaptation.gmm.jointgmm;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.machinelearning.ContextualGMMParams;
import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.adaptation.BaselinePreprocessor;
import marytts.signalproc.adaptation.BaselineTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFile;
import marytts.signalproc.adaptation.codebook.WeightedCodebookFileHeader;
import marytts.signalproc.adaptation.codebook.WeightedCodebookOutlierEliminator;
import marytts.signalproc.adaptation.codebook.WeightedCodebookParallelTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainer;
import marytts.signalproc.adaptation.codebook.WeightedCodebookTrainerParams;
import marytts.signalproc.adaptation.outlier.KMeansMappingEliminatorParams;
import marytts.signalproc.adaptation.outlier.TotalStandardDeviations;
import marytts.signalproc.distance.DistanceComputer;
import marytts.signalproc.window.Window;


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
