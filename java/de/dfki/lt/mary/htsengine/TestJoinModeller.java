package de.dfki.lt.mary.htsengine;

import java.io.IOException;

public class TestJoinModeller {

    
    
    public static void main(String[] args) throws IOException, InterruptedException{
    try {
        /* configure log info */
        org.apache.log4j.BasicConfigurator.configure();
        
        String joinPdfFile = "/project/mary/marcela/HMM-voices/DFKI_German_Poker/mary_files/joinModeller.pdf";
        String joinTreeFile = "/project/mary/marcela/HMM-voices/DFKI_German_Poker/mary_files/tree-joinModeller.inf";
        
        /* Load PDFs*/
        HTSModelSet joinPdf = new HTSModelSet();        
        joinPdf.loadJoinModelSet(joinPdfFile);
        
        /* Load Trees */
        int numTrees = 1;  /* just JoinModeller will be loaded */
        HTSTreeSet joinTree = new HTSTreeSet(numTrees);
        joinTree.loadJoinModellerTreeSet(joinTreeFile);
        
        /* Given a context feature model name, find its join PDF mean and variance */
        /* first, find an index in the tree and then find the mean and variance that corresponds to that index in joinPdf */
        int indexPdf;
        int vectorSize = joinPdf.getJoinVsize();
        double[] mean = new double[vectorSize];
        double[] variance = new double[vectorSize];
        
        //String modelName = "0^f-t+_=0||str=0|pos_syl=3|pos_type=final|pos=NN|snt_punc=.|snt_numwrds=7|wrds_snt_stt=2|wrds_snt_end=4|wrd_numsyls=3|syls_wrd_stt=2|syls_wrd_end=0|wrd_numsegs=9|segs_wrd_stt=8|segs_wrd_end=0|syl_numsegs=4|segs_syl_stt=3|segs_syl_end=0|syls_p_str=2|syls_n_str=1|p_punc=0|n_punc=.|wrds_p_punc=2|wrds_n_punc=4|wrd_freq=0|";
        String modelName = "0^i:-o:+_=0||str=0|pos_syl=0|pos_type=final|pos=FM|snt_punc=.|snt_numwrds=17|wrds_snt_stt=16|wrds_snt_end=0|wrd_numsyls=4|syls_wrd_stt=3|syls_wrd_end=0|wrd_numsegs=7|segs_wrd_stt=6|segs_wrd_end=0|syl_numsegs=1|segs_syl_stt=0|segs_syl_end=0|syls_p_str=2|syls_n_str=0|p_punc=:|n_punc=.|wrds_p_punc=4|wrds_n_punc=0|wrd_freq=0|";
        //String modelName = "aI^R-a:+t=@||wrds_n_punc=0|syl_numsegs=2|segs_wrd_stt=5|syls_wrd_stt=2|str=0|wrds_snt_end=0|pos_syl=1|p_punc=0|wrd_freq=3|snt_punc=dt|segs_wrd_end=3|wrds_snt_stt=4|wrds_p_punc=4|n_punc=dt|segs_syl_end=0|syls_wrd_end=1|pos_type=mid|syls_n_str=0|POS=VVPP|snt_numwrds=5|wrd_numsyls=4|wrd_numsegs=9|syls_p_str=1|segs_syl_stt=1||";
        indexPdf = joinTree.searchTree(modelName, joinTree.getTreeHead(0).getRoot(), false);
        
        joinPdf.findJoinPdf(indexPdf, mean, variance);
        
        System.out.print("mean: ");
        for(int i=0; i<vectorSize; i++)
           System.out.print(mean[i] + " ");
        System.out.print("\nvariance: ");
        for(int i=0; i<vectorSize; i++)
           System.out.print(variance[i] + " ");
        System.out.println();
        
    } catch (Exception e) {
        System.err.println("Exception: " + e.getMessage());
    }
    
  }
    
}
