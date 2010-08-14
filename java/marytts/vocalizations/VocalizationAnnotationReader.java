package marytts.vocalizations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import marytts.tools.voiceimport.BasenameList;

/**
 * Reader class for manual annotation of vocalizations
 * 
 * The format of the file should be as following examples:  
 * (first line indicates list of feature names)
 * 
 * categories|name|voicequality|angry|sadness|amusement|happiness|contempt|solidarity|antagonism|certain|agreeing|interested|anticipation
 * Obadiah_049|right|modal|2|3|1|1|2|2|1|0|0|-1|0
 * Poppy2_078|yeah|modal|1|1|1|2|1|2|1|-1|0|-1|0
 * Prudence_058|(laughter)|breathy|1|1|3|2|1|2|1|0|0|0|-1
 * Poppy2_085|yeah|breathy|1|2|1|1|1|2|1|0|0|-1|0
 *  
 *  
 * @author sathish
 *
 */
public class VocalizationAnnotationReader {

    private ArrayList<String> featureCategories; // feature categories  
    private Map<String,Map<String, String>> annotationData; // basename --> (feature category, feature value)
    private BasenameList bnl = null;
    
    public VocalizationAnnotationReader(String featureFile) throws IOException {
        this(featureFile, null);
    }
    
    public VocalizationAnnotationReader(String featureFile, BasenameList bnl) throws IOException {
        this.bnl = bnl;
        formatChecker(featureFile);
        featureCategories = new ArrayList<String>();
        annotationData    = new HashMap<String,Map<String, String>>();
        getAnnotations(featureFile); 
    }
    
    /**
     * 
     * @param featureFile
     * @throws IOException 
     */
    private void formatChecker(String featureFile) throws IOException {
        BufferedReader bfrMean = new BufferedReader(new FileReader(new File(featureFile)));
        String lineMeaning = bfrMean.readLine();
        String[] mlines = lineMeaning.split("\\|");
        int noOfStrings = mlines.length;
        
        while ( (lineMeaning = bfrMean.readLine()) != null ) {
            mlines = lineMeaning.split("\\|");
            if(noOfStrings != mlines.length) {
                throw new RuntimeException("the format of the file is not good.");
            }
        }
        
        System.out.println("The format of the file is good");
        bfrMean.close();
    }

    private void getAnnotations( String featureFile ) throws IOException {
        
        BufferedReader bfrMean = new BufferedReader(new FileReader(new File(featureFile)));
        String lineMeaning = bfrMean.readLine();
        String[] mlines = lineMeaning.split("\\|");
        
        // read feature categories
        for ( int i=1; i < mlines.length; i++ ) {
            featureCategories.add(mlines[i].trim());
        }
        
        while ( (lineMeaning = bfrMean.readLine()) != null ) {
            mlines = lineMeaning.split("\\|");
            if ( bnl != null ) {
                if ( !bnl.contains(mlines[0].trim()) ) {
                    continue;
                }
            }
            Map<String, String> featureMap = new HashMap<String, String>();  
            // read feature categories
            for ( int i=1; i < mlines.length; i++ ) {
                featureMap.put( featureCategories.get(i-1), mlines[i].trim() );
            }
            annotationData.put(mlines[0].trim(), featureMap);
        }
        bfrMean.close();
    }
    
    public Map<String,Map<String, String>> getVocalizationsAnnotation() {
        return this.annotationData;
    }
    
    public ArrayList<String> getFeatureList() {
        return this.featureCategories;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        

    }

}
