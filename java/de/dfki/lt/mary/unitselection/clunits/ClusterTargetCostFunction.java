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
    private Map features2Weights;
    
    
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
        if (features2Extractor == null){
            return 0;
        } else {
            //if the unit has no features, you can not calculate
            if (!unit.hasFeaturesMap()){
                logger.debug("Could not calculate cost for unit "
                        +unit.getName()+", returning 0");
                return 0;
            } else {
                int cost = 0;
                //go through the features and compare
                Set features = features2Extractor.keySet();
                for (Iterator it= features.iterator(); it.hasNext();){
                    String nextFeature = (String) it.next();
                    //extract the targets value
                    String targetValue = target.getValueForFeature(nextFeature);
                    if (targetValue == null){
                        targetValue = (String)
                    	((PathExtractorImpl)features2Extractor.get(nextFeature)).findFeature(target.getItem());
                        target.setFeatureAndValue(nextFeature,targetValue);
                    }
                    //extract the units value
                    String unitValue = unit.getValueForFeature(nextFeature);
                    if (unitValue != null || targetValue != null){
                        //extract the weight and compare
                        Integer weight = (Integer) features2Weights.get(nextFeature);
                        cost += compare(targetValue, unitValue, weight);
                    }
                }
                logger.debug("Succesfully calculated cost for unit "+unit.getName()
                        +" and target "+target.toString());
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
    private int compare(String targetValue, 
            			String unitValue, 
            			Integer weight)
    {
        int weightInt = weight.intValue();
        int resultInt = 0;
        if (!targetValue.equals(unitValue)){
            resultInt = 1;
        }
        return (weightInt*resultInt);
    }
    
    /**
     * Set the features of the cost function
     * @param features the features
     */
    public void setFeatsAndWeights(List features,Map features2Weights,
            						UnitSelectionFeatProcManager featProc)
    {
        this.features2Weights = features2Weights;
        //if you did not get any features, do nothing
        if (features == null){
            this.features2Extractor = null;
            logger.warn("Did not get any features, " +
            			"can not calculate target costs"); 
        } else {
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
