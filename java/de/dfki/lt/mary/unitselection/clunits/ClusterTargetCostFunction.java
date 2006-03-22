/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.clunits;

import de.dfki.lt.mary.unitselection.TargetCostFunction;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.Target;
import de.dfki.lt.mary.unitselection.cart.PathExtractorImpl;
import de.dfki.lt.mary.unitselection.featureprocessors.*;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * A target cost function for evaluating the goodness-of-fit of 
 * a given unit for a given target. 
 * @author Marc Schr&ouml;der
 *
 */
public class ClusterTargetCostFunction implements TargetCostFunction
{
    private Map features2Extractor;
    private List features;
    private List types;
    private List weights;
    private int targetIndex;
    
    private Logger logger;
    
    public ClusterTargetCostFunction() {
        this.logger = Logger.getLogger("ClusterTargetCostFunction");
        targetIndex = 0;
    }
    
    /**
     * Compute the goodness-of-fit of a given unit for a given target.
     * @param target 
     * @param unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public float cost(Target target, Unit unit)
    {
        //if you have no features, you can not calculate
        if (features == null){
            return 0;
        } else {
            //if the unit has no features, you can not calculate
            if (!unit.hasValues()){
                logger.debug("Could not calculate cost for unit "
                        +unit.getName()+" since it has no features, returning 0");
                return 0;
            } else {
                // else go through the features and compare
                //logger.debug("Now calculating cost for "+unitType+" "
                //        +unitInstance+" "+target.getIndex());               
                float cost = 0;
                for (int i = 0;i < features.size(); i++){
                    float weight = ((Float) weights.get(i)).floatValue();
                    if (weight != 0){
                        String nextFeature = (String) features.get(i);
                        //extract the targets value
                        String targetValue;
                        
                            targetValue = target.getValueForFeature(nextFeature);
                        if (targetValue == null){
                            targetValue = (String)
                            ((PathExtractorImpl)features2Extractor.get(nextFeature)).findFeature(target.getItem());
                            target.setFeatureAndValue(nextFeature,targetValue);
                            /**
                            if (nextFeature.endsWith("seg_pitch")){
                                System.out.println("TargetValue : "+targetValue+", target : "+
                                        target.toString());
                            }**/
                        }
                        //extract the units value
                        String unitValue = unit.getValueForFeature(i);
                        if (unitValue != null || targetValue != null){
                            //extract the weight and compare
                            //logger.debug("Comparing feature "+features.get(i)
                              //      +" with weight "+weights.get(i));
                            cost += compare((String) types.get(i),targetValue, unitValue, weight);
                        } else {
                            cost += weight;
                        }                        
                    }
                }
                //logger.debug("Succesfully calculated cost: "+cost);
                return cost;
            }
        }
    }
    
    /**
     * Compare two values considering a weight
     * @param targetValue the target Value
     * @param unitValue the unit Value
     * @param weight the weight
     * @return the resulting cost
     */
    private float compare(String featureType,
            			String targetValue, 
            			String unitValue, 
            			float weight)
    {
        if (featureType.equals("String")){
           float result = 1;
           if (targetValue.equals(unitValue)){
                result = 0;
           }
           return (weight*result);
        } else { //featureType is Float
            float targetFloat = Float.parseFloat(targetValue);
            float unitFloat = Float.parseFloat(unitValue);
            float result = Math.abs(targetFloat-unitFloat)*weight;
            //logger.debug("Multiplying "+targetFloat+" minus "+unitFloat
              //      +" with "+weight+" equals "+result);
            return result;
        }
    }
    
    /**
     * Set the features of the cost function
     * @param features the features
     */
    public void setFeatsAndWeights(List featsNWeights,
            						UnitSelectionFeatProcManager featProc)
    {
        //if you did not get any features, do nothing
        if (featsNWeights == null){
            this.features2Extractor = null;
            this.features = null;
            logger.warn("Did not get any features, " +
            			"can not calculate target costs"); 
        } else {
            //split features and feature types
            types = new ArrayList();
            features = new ArrayList();
            weights = new ArrayList();
            for (int i=0; i<featsNWeights.size();i++){
                StringTokenizer tok = 
                    new StringTokenizer((String)featsNWeights.get(i)," ");
                features.add(tok.nextToken());
                types.add(tok.nextToken());
                weights.add(new Float(tok.nextToken()));
            }
            //build a PathExtractor for each feature
            features2Extractor = new HashMap();
            for (Iterator it = features.iterator();it.hasNext();){
                String nextFeature = (String)it.next();
                if (!nextFeature.equals("occurid")){
                    PathExtractorImpl pathEx = new PathExtractorImpl(nextFeature,true);
                    pathEx.setFeatureProcessors((UtteranceFeatProcManager)featProc);
                    features2Extractor.put(nextFeature,pathEx);
                }
            }
        }
    }
        
}
