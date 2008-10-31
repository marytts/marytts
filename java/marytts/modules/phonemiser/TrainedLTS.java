package marytts.modules.phonemiser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import marytts.cart.CART;
import marytts.cart.LeafNode;
import marytts.cart.LeafNode.StringAndFloatLeafNode;
import marytts.cart.io.MaryCARTReader;
import marytts.cart.io.WagonCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.newlanguage.LTSTrainer;

import org.xml.sax.SAXException;



/**
 * 
 * This predicts pronunciation from a model trained with LTSTrainer.
 * 
 * @author benjaminroth
 *
 */
public class TrainedLTS {
    
    private CART tree;
    private FeatureDefinition featureDefinition;
    private int indexPredictedFeature;
    private int context;
    private AllophoneSet allophoneSet;
    private boolean convertToLowercase;
    
    /**
     * 
     * Initializes letter to sound system with a phoneSet, and load the decision
     * tree from the given file.
     * 
     * @param aPhonSet phoneset used in syllabification
     * @param treeFilename
     * @throws IOException 
     * 
     */
    public TrainedLTS(AllophoneSet aPhonSet, String treeFilename) throws IOException {
        this.allophoneSet = aPhonSet;
        this.loadTree(treeFilename);
    }
    
    public TrainedLTS(AllophoneSet aPhonSet, CART predictionTree) {
        this.allophoneSet = aPhonSet;
        this.tree = predictionTree;
        this.featureDefinition = tree.getFeatureDefinition();
        this.indexPredictedFeature = featureDefinition.getFeatureIndex(LTSTrainer.PREDICTED_STRING_FEATURENAME);
        Properties props = tree.getProperties();
        if (props == null) throw new IllegalArgumentException("Prediction tree does not contain properties");
        convertToLowercase = Boolean.parseBoolean(props.getProperty("lowercase"));
        context = Integer.parseInt(props.getProperty("context"));
    }
    
    /**
     * 
     * Convenience method to load tree from file
     * 
     * @param treeFilename
     * @throws IOException
     */
    public void loadTree(String treeFilename) throws IOException
    {
        MaryCARTReader cartReader = new MaryCARTReader();
        this.tree = cartReader.load(treeFilename);
        this.featureDefinition = tree.getFeatureDefinition();
        this.indexPredictedFeature = featureDefinition.getFeatureIndex(LTSTrainer.PREDICTED_STRING_FEATURENAME);
        this.convertToLowercase = false;
        Properties props = tree.getProperties();
        if (props == null) throw new IllegalArgumentException("Prediction tree does not contain properties");
        convertToLowercase = Boolean.parseBoolean(props.getProperty("lowercase"));
        context = Integer.parseInt(props.getProperty("context"));
    }
    
    public String predictPronunciation(String graphemes)
    {
        if (convertToLowercase)
            graphemes = graphemes.toLowerCase(allophoneSet.getLocale());

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

            StringAndFloatLeafNode leaf = (StringAndFloatLeafNode) tree.interpretToNode(fv, 0);
            String prediction = leaf.mostProbableString(featureDefinition, indexPredictedFeature);
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
        
        TrainedLTS lts = new TrainedLTS(AllophoneSet.getAllophoneSet(phFileLoc), "/Users/benjaminroth/Desktop/mary/english/cmudict.lts");
        
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
