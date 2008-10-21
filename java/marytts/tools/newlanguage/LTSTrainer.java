

package marytts.tools.newlanguage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import marytts.cart.StringCART;
import marytts.features.FeatureDefinition;
import marytts.fst.AlignerTrainer;
import marytts.fst.StringPair;
import marytts.modules.phonemiser.Phoneme;
import marytts.modules.phonemiser.PhonemeSet;

import org.xml.sax.SAXException;

import weka.classifiers.trees.j48.BinC45ModelSelection;
import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.TreeConverter;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * 
 * This class is a generic approach to predict a phoneme sequence from a 
 * grapheme sequence. 
 * 
 * the normal sequence of steps is:
 * 1) initialize the trainer with a phoneme set and a locale
 * 
 * 2) read in the lexicon, preserve stress if you like
 * 
 * 3) make some alignment iterations (usually 5-10)
 * 
 * 4) train the trees and save them in wagon format in a specified directory
 * 
 * see main method for an example.
 * 
 * Apply the model using TrainedLTS
 * 
 * @author benjaminroth
 *
 */

public class LTSTrainer extends AlignerTrainer{

    PhonemeSet phSet;
    Locale loc;

    
    int context = 2;
    
    public LTSTrainer(PhonemeSet aPhSet, Locale aLoc) {
        super();
        this.phSet = aPhSet;
        this.loc = aLoc;

    }
    
    
    public StringCART trainTree(int minLeaveData) throws IOException{
        
        Map<String, List<String[]>> grapheme2align = new HashMap<String, List<String[]>>();
        for (String gr : this.graphemeSet){      
            grapheme2align.put(gr, new ArrayList<String[]>());
        }
        
        Set<String> phChains = new HashSet<String>();
        
        // for every alignment pair collect counts
        for ( int i = 0; i < this.inSplit.size(); i++ ){
            
            StringPair[] alignment = this.getAlignment(i);
            
            for ( int inNr = 0 ; inNr < alignment.length ; inNr ++ ){
                
                //System.err.println(alignment[inNr]);
                
                // quotation signs needed to represent empty string
                String outAlNr = "'" + alignment[inNr].getString2() + "'";
                
                // TODO: don't consider alignments to more than three characters
                if (outAlNr.length() > 5)
                    continue;

                phChains.add(outAlNr);
                
                // storing context and target
                String[] datapoint = new String[2*context + 2];
                
                for (int ct = 0; ct < 2*context+1; ct++){
                    int pos = inNr - context +ct;
                    
                    if (pos >=0  && pos < alignment.length){
                        datapoint[ct] = alignment[pos].getString1();
                    } else {
                        datapoint[ct] = "null";
                    }
                    
                }
                
                // set target
                datapoint[2*context+1] = outAlNr;
                
                // add datapoint
                grapheme2align.get(alignment[inNr].getString1()).add(datapoint);                                
            }
        }
        
        // for conversion need feature definition file        
        FeatureDefinition fd = this.graphemeFeatureDef(phChains);
        
        int centerGrapheme = fd.getFeatureIndex("att"+(context+1));
        
        List<StringCART> stl = new ArrayList(fd.getNumberOfValues(centerGrapheme));
        
        for (String gr : fd.getPossibleValues(centerGrapheme)){
            
            System.out.println("Training decision tree for: " + gr);
            
            FastVector attributeDeclarations = new FastVector();
            
            // attributes with values
            for (int att = 1; att <= context*2 + 1; att++){
                
                // ...collect possible values
                FastVector attVals = new FastVector();
                                
                String featureName = "att"+att;
                
                for (String usableGrapheme:fd.getPossibleValues(fd.getFeatureIndex(featureName))){
                    attVals.addElement(usableGrapheme);
                }
                
                attributeDeclarations.addElement(new Attribute(featureName, attVals) );
            }
            
            List<String[]> datapoints = grapheme2align.get(gr);
            
            // maybe training is faster with targets limited to grapheme
            Set<String> graphSpecPh = new HashSet<String>();
            for (String[] dp : datapoints){
                graphSpecPh.add(dp[dp.length-1]);
            }
            
            // targetattribute
            // ...collect possible values
            FastVector targetVals = new FastVector();
            for (String phc : graphSpecPh){// todo: use either fd of phChains
                targetVals.addElement(phc);
            }
            attributeDeclarations.addElement(new Attribute("target", targetVals) );

            // now, create the dataset adding the datapoints
            Instances data = new Instances(gr, attributeDeclarations, 0);
            
            // datapoints
            for (String[] point : datapoints){
                
                Instance currInst = new Instance( data.numAttributes()  );
                currInst.setDataset(data);
                
                for (int i = 0; i < point.length; i++){
                                       
                    currInst.setValue(i, point[i]);
                }
                
                data.add(currInst);
            }
            
            // Make the last attribute be the class
            data.setClassIndex(data.numAttributes() - 1);
            
            // build the tree without using the J48 wrapper class
            // standard parameters are: 
            //binary split selection with minimum x instances at the leaves, tree is pruned, confidenced value, subtree raising, cleanup
            C45PruneableClassifierTree decisionTree;
            try {
                decisionTree = new C45PruneableClassifierTree(new BinC45ModelSelection(minLeaveData,data),true,0.25f,true,true);
                decisionTree.buildClassifier(data);
            } catch (Exception e) {
                throw new RuntimeException("couldn't train decisiontree using weka: " + e);
            }
            
            StringCART wagonTree = TreeConverter.c45toStringCART(decisionTree, fd,data);
            
            stl.add(wagonTree);
       }
                
        StringCART bigTree = new StringCART(stl,centerGrapheme);
                        
        return bigTree;
    }
    
    /**
     * 
     * Convenience method to save files to graph2phon.wagon and graph2phon.pfeats
     * in a specified directory with UTF-8 encoding.
     * 
     * @param tree
     * @param saveTreePath
     * @throws IOException
     */
    public void save(StringCART tree, String saveTreePath) throws IOException{
        FileOutputStream outFile = new FileOutputStream(saveTreePath + "/graph2phon.wagon");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outFile,"UTF-8"));
        bw.write(tree.toString());
        bw.close();

        // also remember feature definition
        FileOutputStream fdFile = new FileOutputStream(saveTreePath + "/graph2phon.pfeats");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(fdFile,"UTF-8"));
        
        tree.getFeatureDefinition().writeTo(pw, false);
        
        pw.close();
        fdFile.close();
    }
    
    private FeatureDefinition graphemeFeatureDef(Set<String> phChains) throws IOException {
        
        String lineBreak = System.getProperty("line.separator");
        
        String fdString = "ByteValuedFeatureProcessors" + lineBreak;
        
        // add attribute features
        for (int att = 1; att <= context*2 + 1; att++){
            fdString += "att" + att;
            
            for (String gr :  this.graphemeSet){
                fdString += " " + gr;
            }
            
            // the attribute at position "context" is the identity of the grapheme:
            // null is not possible for that
            if (att != (context + 1)){
                fdString +=" null";
            }
            fdString += lineBreak;
            
        }
        
        
        fdString += "ShortValuedFeatureProcessors" + lineBreak;
        
        // add class features
        fdString += "target";
        
        for (String ph :  phChains){
            fdString += " " + ph;
        }
        
        fdString += lineBreak;
        
        fdString += "ContinuousFeatureProcessors" + lineBreak;
        
        BufferedReader featureReader = new BufferedReader(new StringReader(fdString));
        
        return new FeatureDefinition(featureReader,false);        
    }
    
    /**
     * 
     * reads in a lexicon in "sampa" format, lines are of the kind:
     * 
     * graphemechain\phonemechain\otherinformation
     * 
     * Stress is optionally preserved, marking the firsr vowel of a stressed
     * syllable with "1".
     * 
     * @param lexicon reader with lines of lexicon
     * @param considerStress indicator if stress is preserved
     * @throws IOException
     */
    public void readSampaLexicon(BufferedReader lexicon, boolean considerStress) throws IOException{
                
        String line;
        
        while ((line = lexicon.readLine()) != null){
            String[] lineParts = line.trim().split(Pattern.quote(" "));
            // TODO: remove all non-standard symbols from input side, not only ' and -
            String graphStr = lineParts[0].toLowerCase(loc).replaceAll("['-.]", "");

            // remove all secondary stress markers
            String phonStr = lineParts[1].replaceAll(",", "");
            
            String[] syllables = phonStr.split("-");
            
            
            List<String> separatedPhones = new ArrayList<String>();
            List<String> separatedGraphemes = new ArrayList<String>();
                        
            String currPh;
            
            for (String syl : syllables){
            
                boolean stress = false;
                
                if (syl.startsWith("'")){
                    syl = syl.substring(1);
                    stress = true;
                }
                
                for ( Phoneme ph : phSet.splitIntoPhonemes(syl)){
                    currPh = ph.name();
                    
                    if (stress && considerStress && ph.isVowel()){
                        
                        currPh += "1";
                        stress = false;
                    }
                    
                    separatedPhones.add(currPh);
                }// ... for each phoneme
                
            }

            
            for ( int i = 0 ; i < graphStr.length() ; i++ ){
                
                this.graphemeSet.add(graphStr.substring(i, i+1));
                
                separatedGraphemes.add(graphStr.substring(i, i+1));
            }
            
            this.addAlreadySplit(separatedGraphemes, separatedPhones);
            
        }
    }
    
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

        
        String  phFileLoc = "/Users/benjaminroth/Desktop/mary/english/phoneme-list-engba.xml";

        
        // initialize trainer 
        LTSTrainer tp = new LTSTrainer(PhonemeSet.getPhonemeSet(phFileLoc), Locale.ENGLISH);

        BufferedReader lexReader = new BufferedReader(
                new InputStreamReader(
                new FileInputStream(
                        "/Users/benjaminroth/Desktop/mary/english/sampa-lexicon.txt"),"ISO-8859-1"));
        
        // read lexicon for training
        tp.readSampaLexicon(lexReader, true);

        // make some alignment iterations
        for ( int i = 0 ; i < 5 ; i++ ){
            System.out.println("iteration " + i);
            tp.alignIteration();
            
        }
        
        
        StringCART st = tp.trainTree(100);

        tp.save(st, "/Users/benjaminroth/Desktop/mary/english/trees/");
     
    }
    
}
