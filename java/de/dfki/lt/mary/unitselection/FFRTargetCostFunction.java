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
package de.dfki.lt.mary.unitselection;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunctionManager;

public class FFRTargetCostFunction extends FeatureFileReader implements TargetCostFunction 
{
    protected WeightFunc[] weightFunction;
    protected TargetFeatureComputer targetFeatureComputer;
    
    
    public FFRTargetCostFunction()
    {
    }

    /**
     * Compute the goodness-of-fit of a given unit for a given target.
     * @param target 
     * @param unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public double cost(Target target, Unit unit)
    {
        FeatureVector targetFeatures = target.getFeatureVector(); 
        assert targetFeatures != null: "Target "+target+" does not have pre-computed feature vector";
        FeatureVector unitFeatures = featureVectors[unit.getIndex()];
        int nBytes = targetFeatures.getNumberOfByteFeatures();
        int nShorts = targetFeatures.getNumberOfShortFeatures();
        int nFloats = targetFeatures.getNumberOfContinuousFeatures();
        assert nBytes == unitFeatures.getNumberOfByteFeatures();
        assert nShorts == unitFeatures.getNumberOfShortFeatures();
        assert nFloats == unitFeatures.getNumberOfContinuousFeatures();
        // Now the actual computation
        double cost = 0;
        // byte-valued features:
        for (int i=0; i<nBytes; i++) {
            float weight = featureDefinition.getWeight(i);
            if (targetFeatures.getByteFeature(i) != unitFeatures.getByteFeature(i))
                cost += weight;
        }
        // short-valued features:
        for (int i=nBytes, n=nBytes+nShorts; i<n; i++) {
            float weight = featureDefinition.getWeight(i);
            if (targetFeatures.getShortFeature(i) != unitFeatures.getShortFeature(i))
                cost += weight;
        }
        // continuous features:
        for (int i=nBytes+nShorts, n=nBytes+nShorts+nFloats; i<n; i++) {
            float weight = featureDefinition.getWeight(i);
            float a = targetFeatures.getContinuousFeature(i);
            float b = unitFeatures.getContinuousFeature(i);
            cost += weight * weightFunction[i-nBytes-nShorts].cost(a, b);
        }
        return cost;
    }
    
    /**
     * Initialise the data needed to do a target cost computation.
     * @param featureFileName name of a file containing the unit features
     * @param weightsFile an optional weights file -- if non-null, contains
     * feature weights that override the ones present in the feature file.
     * @param featProc a feature processor manager which can provide feature processors
     * to compute the features for a target at run time
     * @throws IOException
     */
    public void load(String featureFileName, String weightsFile,
            FeatureProcessorManager featProc)
    throws IOException
    {
        super.load(featureFileName);
        if (weightsFile != null) {
            // overwrite weights from file
            FeatureDefinition newWeights = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(weightsFile), "UTF-8")), true);
            if (!newWeights.featureEquals(featureDefinition)) {
                throw new IOException("Weights file '"+weightsFile+"': feature definition incompatible with feature file '"+featureFileName+"'");
            }
            featureDefinition = newWeights;
        }
        weightFunction = new WeightFunc[featureDefinition.getNumberOfContinuousFeatures()];
        WeightFunctionManager wfm = new WeightFunctionManager();
        int nDiscreteFeatures = featureDefinition.getNumberOfByteFeatures()+featureDefinition.getNumberOfShortFeatures();
        for ( int i = 0; i < weightFunction.length; i++ ) {
            String weightFunctionName = featureDefinition.getWeightFunctionName(nDiscreteFeatures+i);
            if ( "".equals( weightFunctionName ) )
                weightFunction[i] = wfm.getWeightFunction( "linear" );
            else
                weightFunction[i] = wfm.getWeightFunction(weightFunctionName);
        }
        // TODO: If the target feature computer had direct access to the feature definition, it could do some consistency checking
        this.targetFeatureComputer = new TargetFeatureComputer(featProc, featureDefinition.getFeatureNames());
    }

    /**
     * Compute the features for a given target. A typical use case is to
     * call this method once for every target, and store the resulting
     * feature vector in the target using target.setFeatureVector().
     * @param target the target for which to compute the features
     * @return a feature vector
     * @see Target#setFeatureVector(FeatureVector)
     */
    public FeatureVector computeTargetFeatures(Target target)
    {
        return targetFeatureComputer.computeFeatureVector(target);
    }
    
    
    /**
     * Look up the features for a given unit.
     * @param unit a unit in the database
     * @return the FeatureVector for target cost computation associated to this unit
     */
    public FeatureVector getUnitFeatures(Unit unit)
    {
        return featureVectors[unit.getIndex()];
    }
    
    /**
     * Get the string representation of the feature value associated with
     * the given unit 
     * @param unit the unit whose feature value is requested
     * @param featureName name of the feature requested
     * @return a string representation of the feature value
     * @throws IllegalArgumentException if featureName is not a known feature
     */
    public String getFeature(Unit unit, String featureName)
    {
        int featureIndex = featureDefinition.getFeatureIndex(featureName);
        if (featureDefinition.isByteFeature(featureIndex)) {
            byte value = featureVectors[unit.getIndex()].getByteFeature(featureIndex);
            return featureDefinition.getFeatureValueAsString(featureIndex, value);
        } else if (featureDefinition.isShortFeature(featureIndex)) {
            short value = featureVectors[unit.getIndex()].getShortFeature(featureIndex);
            return featureDefinition.getFeatureValueAsString(featureIndex, value);
        } else { // continuous -- return float as string
            float value = featureVectors[unit.getIndex()].getContinuousFeature(featureIndex);
            return String.valueOf(value);
        }
    }
}
