package de.dfki.lt.mary.unitselection.adaptation.gmm.jointgmm;

import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookFeatureExtractor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookPostprocessor;
import de.dfki.lt.mary.unitselection.adaptation.codebook.WeightedCodebookPreprocessor;

public class JointGMMTransformer {

    public WeightedCodebookPreprocessor preprocessor;
    public WeightedCodebookFeatureExtractor featureExtractor;
    public WeightedCodebookPostprocessor postprocessor;
    public JointGMMTransformerParams params;
    public static String wavExt = ".wav";
    
    public JointGMMTransformer(WeightedCodebookPreprocessor pp,
            WeightedCodebookFeatureExtractor fe, 
            WeightedCodebookPostprocessor po,
            JointGMMTransformerParams pa)
    {
        preprocessor = pp;
        featureExtractor = fe;
        postprocessor = po;
        params = pa;
    }
}
