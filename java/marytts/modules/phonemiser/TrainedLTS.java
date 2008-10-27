package marytts.modules.phonemiser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import marytts.cart.StringCART;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;

import org.xml.sax.SAXException;



/**
 * 
 * This predicts pronunciation from a model trained with LTSTrainer.
 * 
 * @author benjaminroth
 *
 */
public class TrainedLTS {
    
    private StringCART tree;
    private int context = 2;
    private AllophoneSet allophoneSet;
    Locale locale;
    
    /**
     * 
     * Initializes letter to sound system with a phoneSet. To actually be able 
     * to predict something you have to load the decision trees.
     * 
     * @param aPhonSet phoneset used in syllabification
     * @param aLocale locale used for lowercasing
     * @throws IOException 
     * 
     */
    public TrainedLTS(AllophoneSet aPhonSet, Locale aLocale, String treeDirName) throws IOException {
        this.allophoneSet = aPhonSet;
        this.locale = aLocale;
        this.loadTree(treeDirName);
    }
    
    public TrainedLTS(AllophoneSet aPhonSet, Locale aLocale, StringCART predictionTree) {
        this.allophoneSet = aPhonSet;
        this.locale = aLocale;
        this.tree = predictionTree;
    }
    
    /**
     * 
     * Convenience method to load tree from graph2phon.wagon and graph2phon.pfeats
     * in a specified directory with UTF-8 encoding.
     * 
     * @param tree
     * @param saveTreePath
     * @throws IOException
     */
    public void loadTree(String treeDirName) throws IOException {
        
        File treeDir = new File(treeDirName);
        if (!treeDir.exists()) {
            throw new FileNotFoundException("Configuration directory not found: "+ treeDir.getPath());
        }
        
        String fdFileName = treeDir.getPath() + File.separator + "graph2phon.pfeats";
        
        
        BufferedReader featureReader = new BufferedReader(
                new InputStreamReader(
                new FileInputStream(fdFileName),"UTF-8"));
        FeatureDefinition fd = new FeatureDefinition(featureReader,false);
                
        String bigTreeName = treeDir.getPath() + File.separator + "graph2phon.wagon";
        BufferedReader treeReader = new BufferedReader(
                new InputStreamReader(
                new FileInputStream(bigTreeName),"UTF-8"));
        this.tree = new StringCART(treeReader,fd, fd.getFeatureIndex("target"));
        
    }
    
    public String predictPronunciation(String graphemes){

        graphemes = graphemes.toLowerCase(locale);
        
        String returnStr = "";
        
        for (int i = 0 ; i < graphemes.length() ; i++){
            
            byte[] byteFeatures = new byte[2*this.context + 1];
            
            for (int fnr = 0; fnr < 2*this.context + 1; fnr++){
                int pos = i - context + fnr;
                
                String grAtPos = (pos < 0 || pos >= graphemes.length())? 
                        "null":graphemes.substring(pos, pos+1);
                
        try {
            byteFeatures[fnr] = this.tree.getFeatureDefinition().getFeatureValueAsByte(fnr, grAtPos);
            // ... can also try to call explicit:
            //features[fnr] = this.fd.getFeatureValueAsByte("att"+fnr, cg.substr(pos)
        } catch (IllegalArgumentException iae) {
            // Silently ignore unknown characters
            byteFeatures[fnr] = this.tree.getFeatureDefinition().getFeatureValueAsByte(fnr, "null");
        }
            }

            FeatureVector fv = new FeatureVector(byteFeatures, new short[]{}, new float[]{},0);
            
                String prediction = this.tree.maxString(fv);
                returnStr += prediction.substring(1, prediction.length() - 1);
        }
        
        return returnStr;        

    }
     
    /**
     * Phoneme chain is syllabified. After that, no white spaces are
     * included, stress is on syllable of first stress bearing vowal,
     * or assigned rule-based if there is no stress predicted by the tree.
     * 
     * @param phonemes input phoneme chain, unsyllabified, stress marking attached to vowals
     * @return phoneme chain, with syllable sepeators "-" and stress symbols "'"
     */
    public String syllabify(String phonemes){
        
        Syllabifier sfr = new Syllabifier(this.allophoneSet);
        
        return sfr.syllabify(phonemes);
    }
    
    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {

        //String  phFileLoc = "/home/sathish/Work/blizzard2008/lts/phoneme-list-en_gb.xml";
        String  phFileLoc = "/Users/benjaminroth/Desktop/mary/english/phoneme-list-engba.xml";
        
        TrainedLTS lts = new TrainedLTS(AllophoneSet.getAllophoneSet(phFileLoc), Locale.ENGLISH, "/Users/benjaminroth/Desktop/mary/english/trees/");
        
        System.out.println(lts.predictPronunciation("tuition"));

        System.out.println(lts.syllabify(lts.predictPronunciation("tuition")));

        System.out.println(lts.predictPronunciation("synthesis"));
        
        System.out.println(lts.syllabify(lts.predictPronunciation("synthesis")));
        
        System.out.println(lts.predictPronunciation("autobahn"));
        
        System.out.println(lts.syllabify(lts.predictPronunciation("autobahn")));

        System.out.println(lts.predictPronunciation("kite"));
        
        System.out.println(lts.syllabify(lts.predictPronunciation("kite")));
    }


}
