package marytts.vocalizations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.data.Unit;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.VocalizationFFRTargetCostFunction;
import marytts.util.MaryUtils;
import marytts.util.math.Polynomial;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

/**
 * Select suitable vocalization for a given target using a cost function 
 * @author sathish
 *
 */
public class VocalizationSelector {

    protected VocalizationFeatureFileReader featureFileReader;
    protected FeatureDefinition featureDefinition;
    protected FeatureDefinition f0FeatureDefinition;
    protected VocalizationIntonationReader vIntonationReader;
    protected VocalizationUnitFileReader unitFileReader;
    protected VocalizationFFRTargetCostFunction vffrtCostFunction = null;
    protected VocalizationFFRTargetCostFunction vffrtIntonationCostFunction = null;
    protected boolean f0ContourImposeSupport;
    protected int noOfSuitableUnits = 1;
    
    protected Logger logger = MaryUtils.getLogger("Vocalization Selector");
    
    public VocalizationSelector(Voice voice) throws MaryConfigurationException{
        
        String unitFileName = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.unitfile");
        String featureFile  = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.featurefile");
        String featureDefinitionFile  = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.featureDefinitionFile");
        f0ContourImposeSupport = MaryProperties.getBoolean("voice."+voice.getName()+".f0ContourImposeSupport", false);
        
        try {
            BufferedReader fDBufferedReader = new BufferedReader( new FileReader( new File(featureDefinitionFile)));
            this.featureDefinition = new FeatureDefinition(fDBufferedReader, true);
            this.featureFileReader = new VocalizationFeatureFileReader(featureFile);
            vffrtCostFunction = new VocalizationFFRTargetCostFunction();
            vffrtCostFunction.load(this.featureFileReader, this.featureDefinition);
            unitFileReader = new VocalizationUnitFileReader(unitFileName);

            if(this.featureFileReader.getNumberOfUnits() != this.unitFileReader.getNumberOfUnits()) {
                throw new MaryConfigurationException("Feature file reader and unit file reader is not aligned properly");
            }

            if ( this.f0ContourImposeSupport ) {
                String intonationFDFile = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.intonation.featureDefinitionFile");
                String intonationFile = MaryProperties.getFilename("voice."+voice.getName()+".vocalization.intonationfile");
                BufferedReader f0FDBufferedReader = new BufferedReader( new FileReader( new File(intonationFDFile)));
                f0FeatureDefinition = new FeatureDefinition(f0FDBufferedReader, true);
                vIntonationReader = new VocalizationIntonationReader(intonationFile);
                noOfSuitableUnits = MaryProperties.getInteger("voice."+voice.getName()+".vocalization.intonation.numberOfSuitableUnits");
                vffrtIntonationCostFunction = new VocalizationFFRTargetCostFunction();
                vffrtIntonationCostFunction.load(this.featureFileReader, this.f0FeatureDefinition);
            }
        }
        catch (IOException e) {
            throw new MaryConfigurationException("Problem loading vocalization files for voice " + e);
        }
    }
    
    /**
     * Get feature definition used to select suitable candidate
     * @return Feature Definition
     */
    public FeatureDefinition getFeatureDefinition() {
        return this.featureDefinition;
    }
    
    
    /**
     * Get best candidate pair to impose F0 contour on other
     * @param domElement xml request for vocalization 
     * @return SourceTargetPair best candidate pair
     */
    public SourceTargetPair getBestCandidatePairtoImposeF0(Element domElement) {
        
        
        //Target targetF0Unit = createIntonationTarget(domElement);
        VocalizationCandidate[] vCosts = getBestMatchingCandidates(domElement);
        VocalizationCandidate[] vIntonationCosts = getBestIntonationCandidates(domElement);
        
        VocalizationCandidate[] suitableCandidates = new VocalizationCandidate[noOfSuitableUnits];
        System.arraycopy(vCosts, 0, suitableCandidates, 0, noOfSuitableUnits);
        VocalizationCandidate[] suitableF0Candidates = new VocalizationCandidate[noOfSuitableUnits];
        System.arraycopy(vIntonationCosts, 0, suitableF0Candidates, 0, noOfSuitableUnits);
        
        Target targetUnit =  createTarget(domElement);
        if (logger.getEffectiveLevel().equals(Level.DEBUG)) {
            debugLogCandidates(targetUnit, suitableCandidates, suitableF0Candidates);
        }
        
        SourceTargetPair[] sortedImposeF0Data = vocalizationF0DistanceComputer(suitableCandidates, suitableF0Candidates);
        
        return sortedImposeF0Data[0];
    }
    
    /**
     * polynomial distance computer between two units
     * @param suitableCandidates vocalization candidates
     * @param suitableF0Candidates intonation candidates
     * @return an array of candidate pairs
     */
    private SourceTargetPair[] vocalizationF0DistanceComputer(VocalizationCandidate[] suitableCandidates,
            VocalizationCandidate[] suitableF0Candidates) {
        
        int noPossibleImpositions = suitableCandidates.length * suitableF0Candidates.length;
        SourceTargetPair[] imposeF0Data = new SourceTargetPair[noPossibleImpositions];
        int count = 0;
        
        for ( int i=0; i < suitableCandidates.length; i++ ) {
            for ( int j=0; j < suitableF0Candidates.length; j++ ) {
                int sourceIndex = suitableCandidates[i].unitIndex;
                int targetIndex = suitableF0Candidates[j].unitIndex;
                double distance;
                if ( targetIndex == sourceIndex ) {
                    distance = 0;
                }
                else {
                    double[] targetCoeffs = vIntonationReader.getIntonationCoeffs(targetIndex);
                    double[] sourceCoeffs = vIntonationReader.getIntonationCoeffs(sourceIndex);
                    if (targetCoeffs != null && sourceCoeffs != null && targetCoeffs.length == sourceCoeffs.length) {
                        distance = Polynomial.polynomialDistance(sourceCoeffs, targetCoeffs);
                    } else {
                        distance = Double.MAX_VALUE;
                    }
                }
                imposeF0Data[count++] = new SourceTargetPair(sourceIndex, targetIndex, distance);
            }
        }
        
        Arrays.sort(imposeF0Data);
        
        return imposeF0Data;
    }

    /**
     * get a best matching candidate for a given target
     * @param domElement xml request for vocalization
     * @return unit index of best matching candidate
     */
    public int getBestMatchingCandidate(Element domElement) {
        
        Target targetUnit = createTarget(domElement);
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        double minCost = Double.MAX_VALUE;
        int index = 0;
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtCostFunction.cost(targetUnit, singleUnit);
            if( cost < minCost ) {
                minCost = cost;
                index = i;
            }
        }
        
        return index;
    }
    
    /**
     * get a array of best candidates sorted according to cost
     * @param domElement xml request for vocalization 
     * @return an array of best vocalization candidates
     */
    public VocalizationCandidate[] getBestMatchingCandidates(Element domElement) {
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        Target targetUnit = createTarget(domElement);
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        VocalizationCandidate[] vocalizationCandidates = new VocalizationCandidate[numberUnits];
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtCostFunction.cost(targetUnit, singleUnit);
            vocalizationCandidates[i] = new VocalizationCandidate(i,cost);
        }
        Arrays.sort(vocalizationCandidates);
        return vocalizationCandidates;
    }


    /**
     * Debug messages for selected candidates 
     * @param targetUnit target unit
     * @param suitableCandidates suitable vocalization candidates
     * @param suitableF0Candidates suitable intonation candidates
     */
    private void debugLogCandidates(Target targetUnit, VocalizationCandidate[] suitableCandidates,
            VocalizationCandidate[] suitableF0Candidates) {
        FeatureVector targetFeatures = targetUnit.getFeatureVector();
        FeatureDefinition fd = featureFileReader.getFeatureDefinition();
        int fiName = fd.getFeatureIndex("name");
        int fiIntonation = fd.getFeatureIndex("intonation");
        int fiVQ = fd.getFeatureIndex("voicequality");
        for (int i=0; i<noOfSuitableUnits; i++) {
            int unitIndex = suitableCandidates[i].unitIndex;
            double unitCost = suitableCandidates[i].cost; 
            FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("Candidate ").append(i).append(": ").append(unitIndex).append(" ( "+unitCost+" ) ").append(" -- "); 
            byte bName = fv.getByteFeature(fiName);
            if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
            }
            if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
            }
            if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
            }
            for (int j=0; j<targetFeatures.getLength(); j++) {
                if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float)targetFeatures.getFeature(j))
                        && !Float.isNaN((Float)fv.getFeature(j))) {
                    String featureName = fd.getFeatureName(j);
                    sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
                }
            }
            logger.debug(sb.toString());
        }
        for (int i=0; i<noOfSuitableUnits; i++) {
            int unitIndex = suitableF0Candidates[i].unitIndex;
            double unitCost = suitableCandidates[i].cost;
            FeatureVector fv = featureFileReader.getFeatureVector(unitIndex);
            StringBuilder sb = new StringBuilder();
            sb.append("F0 Candidate ").append(i).append(": ").append(unitIndex).append(" ( "+unitCost+" ) ").append(" -- ");
            byte bName = fv.getByteFeature(fiName);
            if (fv.getByteFeature(fiName) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiName, fd));
            }
            if (fv.getByteFeature(fiVQ) != 0 && targetFeatures.getByteFeature(fiName) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiVQ, fd));
            }
            if (fv.getByteFeature(fiIntonation) != 0 && targetFeatures.getByteFeature(fiIntonation) != 0) {
                sb.append(" ").append(fv.getFeatureAsString(fiIntonation, fd));
            }
            for (int j=0; j<targetFeatures.getLength(); j++) {
                if (targetFeatures.isContinuousFeature(j) && !Float.isNaN((Float)targetFeatures.getFeature(j))
                        && !Float.isNaN((Float)fv.getFeature(j))) {
                    String featureName = fd.getFeatureName(j);
                    sb.append(" ").append(featureName).append("=").append(fv.getFeature(j));
                }
            }
            logger.debug(sb.toString());
        }
    }

    
    /**
     * get a array of best candidates sorted according to cost (cost computed on f0_feature_definition features only)
     * @param domElement xml request for vocalization
     * @return VocalizationCandidate[] a array of best candidates
     */
    private VocalizationCandidate[] getBestIntonationCandidates(Element domElement) {
        
        Target targetUnit = createIntonationTarget(domElement);
        int numberUnits = this.unitFileReader.getNumberOfUnits();
        VocalizationCandidate[] vocalizationCandidates = new VocalizationCandidate[numberUnits];
        for( int i=0; i<numberUnits; i++ ) {
            Unit singleUnit = this.unitFileReader.getUnit(i);
            double cost = vffrtIntonationCostFunction.cost(targetUnit, singleUnit);
            vocalizationCandidates[i] = new VocalizationCandidate(i,cost);
        }
        Arrays.sort(vocalizationCandidates);
        return vocalizationCandidates;
    }
    
    
    
    /**
     * create target from XML request
     * @param domElement xml request for vocalization 
     * @return Target target represents xml request
     */
    private Target createTarget(Element domElement) {
        
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        FeatureDefinition featDef = this.featureDefinition;
        int numFeatures = featDef.getNumberOfFeatures();
        int numByteFeatures = featDef.getNumberOfByteFeatures();
        int numShortFeatures = featDef.getNumberOfShortFeatures();
        int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
        byte[]  byteFeatures  = new byte[numByteFeatures];
        short[] shortFeatures = new short[numShortFeatures];
        float[] floatFeatures = new float[numContiniousFeatures];
        int byteCount = 0;
        int shortCount = 0;
        int floatCount = 0;
        
        for( int i=0; i<numFeatures; i++ ) {
            
            String featName  = featDef.getFeatureName(i);
            String featValue = "0";
            
            if ( featDef.isByteFeature(featName) || featDef.isShortFeature(featName) ) {
                if( domElement.hasAttribute( featName ) ) {
                    featValue = domElement.getAttribute(featName);
                }
                
                boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
                if( !hasFeature ) featValue = "0";
                
                if ( featDef.isByteFeature(i) ) {
                    byteFeatures[byteCount++]   = featDef.getFeatureValueAsByte(i, featValue);
                }
                else if ( featDef.isShortFeature(i) ) {
                    shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
                }
            }
            else {
                if( domElement.hasAttribute( "meaning" ) ) {
                    featValue = domElement.getAttribute("meaning");
                }
                //float contFeature = getMeaningScaleValue ( featName, featValue );
                floatFeatures[floatCount++] = getMeaningScaleValue ( featName, featValue );
            }
        }
        
        FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);
        
        String name = "0";
        if( domElement.hasAttribute( "name" ) ) {
            name = domElement.getAttribute("name");
        }
        
        Target newTarget = new Target(name, domElement);
        newTarget.setFeatureVector(newFV);
                 
        return newTarget;
    }
    
    
    /**
     * create F0 target from XML request
     * @param domElement xml request for intonation  
     * @return Target target represents xml request
     */
    private Target createIntonationTarget(Element domElement) {
        
        //FeatureDefinition featDef = this.featureFileReader.getFeatureDefinition();
        FeatureDefinition featDef = this.f0FeatureDefinition;
        int numFeatures = featDef.getNumberOfFeatures();
        int numByteFeatures = featDef.getNumberOfByteFeatures();
        int numShortFeatures = featDef.getNumberOfShortFeatures();
        int numContiniousFeatures = featDef.getNumberOfContinuousFeatures();
        byte[]  byteFeatures  = new byte[numByteFeatures];
        short[] shortFeatures = new short[numShortFeatures];
        float[] floatFeatures = new float[numContiniousFeatures];
        int byteCount = 0;
        int shortCount = 0;
        int floatCount = 0;
        
        for( int i=0; i<numFeatures; i++ ) {
            
            String featName  = featDef.getFeatureName(i);
            String featValue = "0";
            
            if ( featDef.isByteFeature(featName) || featDef.isShortFeature(featName) ) {
                if( domElement.hasAttribute( featName ) ) {
                    featValue = domElement.getAttribute(featName);
                }
                
                boolean hasFeature = featDef.hasFeatureValue(featName, featValue);
                if( !hasFeature ) featValue = "0";
                
                if ( featDef.isByteFeature(i) ) {
                    byteFeatures[byteCount++]   = featDef.getFeatureValueAsByte(i, featValue);
                }
                else if ( featDef.isShortFeature(i) ) {
                    shortFeatures[shortCount++] = featDef.getFeatureValueAsShort(i, featValue);
                }
            }
            else {
                if( domElement.hasAttribute( "meaning" ) ) {
                    featValue = domElement.getAttribute("meaning");
                }
                //float contFeature = getMeaningScaleValue ( featName, featValue );
                floatFeatures[floatCount++] = getMeaningScaleValue ( featName, featValue );
            }
        }
        
        FeatureVector newFV = featDef.toFeatureVector(0, byteFeatures, shortFeatures, floatFeatures);
        
        String name = "0";
        if( domElement.hasAttribute( "name" ) ) {
            name = domElement.getAttribute("name");
        }
        
        Target newTarget = new Target(name, domElement);
        newTarget.setFeatureVector(newFV);
                 
        return newTarget;
    }
    
    /**
     * get value on meaning scale as a float value
     * @param featureName feature names
     * @param meaningAttribute meaning attribute
     * @return a float value for a meaning feature 
     */
    private float getMeaningScaleValue(String featureName, String meaningAttribute) {
        
        String[] categories = meaningAttribute.split("\\s+");
        List<String> categoriesList = Arrays.asList(categories);
        
        if( "anger".equals(featureName) && categoriesList.contains("anger") ) {
            return 5;
        }
        else if( "sadness".equals(featureName) && categoriesList.contains("sadness") ) {
            return 5;
        }
        else if( "amusement".equals(featureName) && categoriesList.contains("amusement") ) {
            return 5;
        }
        else if( "happiness".equals(featureName) && categoriesList.contains("happiness") ) {
            return 5;
        }
        else if( "contempt".equals(featureName) && categoriesList.contains("contempt") ) {
            return 5;
        }
        else if( "certain".equals(featureName) && categoriesList.contains("uncertain") ) {
            return -2;
        }
        else if( "certain".equals(featureName) && categoriesList.contains("certain") ) {
            return 2;
        }
        else if( "agreeing".equals(featureName) && categoriesList.contains("disagreeing") ) {
            return -2;
        }
        else if( "agreeing".equals(featureName) && categoriesList.contains("agreeing") ) {
            return 2;
        }
        else if( "interested".equals(featureName) && categoriesList.contains("uninterested") ) {
            return -2;
        }
        else if( "interested".equals(featureName) && categoriesList.contains("interested") ) {
            return 2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("low-anticipation") ) {
            return -2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("anticipation") ) {
            return 2;
        }
        else if( "anticipation".equals(featureName) && categoriesList.contains("high-anticipation") ) {
            return 2;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("solidarity") ) {
            return 5;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("low-solidarity") ) {
            return 1;
        }
        else if( "solidarity".equals(featureName) && categoriesList.contains("high-solidarity") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("antagonism") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("high-antagonism") ) {
            return 5;
        }
        else if( "antagonism".equals(featureName) && categoriesList.contains("low-antagonism") ) {
            return 1;
        }
        
        return Float.NaN;
    }
    
    
}
