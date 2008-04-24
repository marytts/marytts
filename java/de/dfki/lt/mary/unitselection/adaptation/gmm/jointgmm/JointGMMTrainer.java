package de.dfki.lt.mary.unitselection.adaptation.gmm.jointgmm;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.machinelearning.ContextualGMMParams;
import de.dfki.lt.mary.unitselection.adaptation.BaselineFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.BaselinePreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.BaselineTrainer;
import de.dfki.lt.mary.unitselection.adaptation.BaselineFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFile;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFileHeader;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookOutlierEliminator;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookParallelTrainer;
import de.dfki.lt.mary.unitselection.adaptation.BaselinePreprocessor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTrainer;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookTrainerParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.KMeansMappingEliminatorParams;
import de.dfki.lt.mary.unitselection.adaptation.outlier.TotalStandardDeviations;
import de.dfki.lt.signalproc.util.distance.DistanceComputer;
import de.dfki.lt.signalproc.window.Window;

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
