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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunctionManager;

public class DiphoneFFRTargetCostFunction implements TargetCostFunction 
{
    protected TargetCostFunction tcfForHalfphones;
    
    public DiphoneFFRTargetCostFunction()
    {
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
            FeatureProcessorManager featProc) throws IOException
    {
        FeatureFileReader ffr = FeatureFileReader.getFeatureFileReader(featureFileName);
        load(ffr, weightsFile, featProc);
    }
    
    public void load(FeatureFileReader ffr, String weightsFile, FeatureProcessorManager featProc)
    throws IOException
    {
        if (ffr instanceof HalfPhoneFeatureFileReader) {
            tcfForHalfphones = new HalfPhoneFFRTargetCostFunction();
        } else {
            tcfForHalfphones = new FFRTargetCostFunction();
        }
        tcfForHalfphones.load(ffr, weightsFile, featProc);
    }

    
    /**
     * Provide access to the Feature Definition used.
     * @return the feature definition object.
     */
    public FeatureDefinition getFeatureDefinition()
    {
        return tcfForHalfphones.getFeatureDefinition();
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
        return tcfForHalfphones.getFeature(unit, featureName);
    }

    public FeatureVector getFeatureVector(Unit unit)
    {
        return tcfForHalfphones.getFeatureVector(unit);
    }

    /**
     * Compute the goodness-of-fit of a given unit for a given target.
     * @param target 
     * @param unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public double cost(Target target, Unit unit)
    {
        if (target instanceof HalfPhoneTarget)
            return tcfForHalfphones.cost(target, unit);
        if (!(target instanceof DiphoneTarget))
            throw new IllegalArgumentException("This target cost function can only be called for diphone and half-phone targets!");
        if (!(unit instanceof DiphoneUnit))
            throw new IllegalArgumentException("Diphone targets need diphone units!");
        DiphoneTarget dt = (DiphoneTarget) target;
        DiphoneUnit du = (DiphoneUnit) unit;
        return tcfForHalfphones.cost(dt.getLeft(), du.getLeft()) + tcfForHalfphones.cost(dt.getRight(), du.getRight());
    }


    /**
     * Compute the features for a given target, and store them in the target.
     * @param target the target for which to compute the features
     * @see Target#getFeatureVector()
     */
    public void computeTargetFeatures(Target target)
    {
        if (!(target instanceof DiphoneTarget)) {
            tcfForHalfphones.computeTargetFeatures(target);
        } else {
            DiphoneTarget dt = (DiphoneTarget) target;
            tcfForHalfphones.computeTargetFeatures(dt.getLeft());
            tcfForHalfphones.computeTargetFeatures(dt.getRight());
            
        }
    }
    
    
    
}
