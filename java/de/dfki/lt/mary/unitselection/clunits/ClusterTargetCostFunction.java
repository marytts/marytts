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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * A target cost function for evaluating the goodness-of-fit of 
 * a given unit for a given target. 
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */
public class ClusterTargetCostFunction implements TargetCostFunction
{
    private Map features2Extractor;
    private List features;
    private List types;
    private List weights;
    
    private Logger logger;
    
    public ClusterTargetCostFunction() {
        this.logger = Logger.getLogger("ClusterTargetCostFunction");
    }
    
    /**
     * Compute the goodness-of-fit of a given unit for a given target.
     * @param target 
     * @param unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public int cost(Target target, Unit unit)
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
                int cost = 0;
                int[] costEnum = new int[features.size()];
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
                        }
                        //extract the units value
                        String unitValue = unit.getValueForFeature(i);
                        if (unitValue != null || targetValue != null){
                            //extract the weight and compare
                            //logger.debug("Comparing feature "+features.get(i)
                              //      +" with weight "+weights.get(i));
                            int result = compare((String) types.get(i),targetValue, unitValue, weight);
                            cost+= result;
                            //if (logger.getEffectiveLevel().equals(Level.DEBUG)){
                            //    costEnum[i] = result;
                            //}
                        } else {
                            cost += weight;
                        }                        
                    }
                }
                /*if (logger.getEffectiveLevel().equals(Level.DEBUG)){
                    StringBuffer sb = new StringBuffer();
                    sb.append("Succesfully calculated cost: "+cost
                        +" for target "+target.getName()
                        +" and unit "+unit.toString());                    
                    sb.append("Values for the features:\n ");
                    for (int i = 0; i<costEnum.length;i++){
                        if (costEnum[i] != 0){
                            sb.append(i+": "+costEnum[i]+" ");
                        }
                    }
                    logger.debug(sb.toString());
                }*/
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
    private int compare(String valueType,
            			String targetValue, 
            			String unitValue, 
            			float weight)
    {
        //if the value is a String, just check for String equality 
        if (valueType.equals("String") || valueType.equals("string")){
           int result = 1;
           if (targetValue.equals(unitValue)){
                result = 0;
           }
           return (int) weight*result;
        } else { //featureType is Float, take abs of the two values 
            float targetFloat = Float.parseFloat(targetValue);
            float unitFloat = Float.parseFloat(unitValue);
            // rounding errors are negligible for costs usually above 100.000
            int result = (int) (Math.abs(targetFloat-unitFloat)*weight);
            //logger.debug("Multiplying "+targetFloat+" minus "+unitFloat
            //      +" with "+weight+" equals "+result);
            return result;
        }
    }
    
    /**
     * Set the features of the cost function
     * @param featusNWeights a List of Strings containing
     * 		  a feature, weight and type
     * @param featProc a feature processors manager
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
                try{
                StringTokenizer tok = 
                    new StringTokenizer((String)featsNWeights.get(i)," ");
                features.add(tok.nextToken());
                String type = tok.nextToken();
                //check, if value type is String of Float
                if (!(type.equals("String") || type.equals("string")
                        || type.equals("Float") || type.equals("float"))){
                    throw new Error("Wrong Value Type \""+type+
                            "\" in line \""+(String)featsNWeights.get(i)
                            +"\" of feature-weights file. Value type has"+
                            " to be Float or String");
                }
                types.add(type);
                weights.add(new Float(tok.nextToken()));
                }catch (NumberFormatException nfe){
                    throw new Error("Wrong Weight Type in line \""+(String)featsNWeights.get(i)
                            +"\" in your feature-weights file. Weight has to be a float number.");
                }catch (Exception e){
                    throw new Error("There is something wrong in line \""+(String)featsNWeights.get(i)
                            +"\" in your feature-weights file");
                }
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
