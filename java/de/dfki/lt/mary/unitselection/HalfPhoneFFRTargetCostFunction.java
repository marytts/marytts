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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.featureprocessors.TargetFeatureComputer;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunctionManager;
import de.dfki.lt.signalproc.display.Histogram;

public class HalfPhoneFFRTargetCostFunction extends FFRTargetCostFunction
{
    protected FeatureDefinition leftWeights;
    protected FeatureDefinition rightWeights;
    protected WeightFunc[] leftWeightFunction;
    protected WeightFunc[] rightWeightFunction;
    
    public HalfPhoneFFRTargetCostFunction()
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
        if (!(target instanceof HalfPhoneTarget))
            throw new IllegalArgumentException("This target cost function can only be called for half-phone targets!");
        HalfPhoneTarget hpTarget = (HalfPhoneTarget) target;
        boolean isLeftHalf = hpTarget.isLeftHalf();
        FeatureDefinition weights = isLeftHalf ? leftWeights : rightWeights;
        WeightFunc[] weightFunctions = isLeftHalf ? leftWeightFunction : rightWeightFunction;
        return cost(target, unit, weights, weightFunctions);
    }

    /**
     * Initialise the data needed to do a target cost computation.
     * @param featureFileName name of a file containing the unit features
     * @param weightsFile an optional string containing weights file names 
     * -- if non-null, contains two file names separated by the character '|',
     * pointing to feature weights files for left and right units, respectively,
     * that override the ones present in the feature file.
     * @param featProc a feature processor manager which can provide feature processors
     * to compute the features for a target at run time
     * @throws IOException
     */
    public void load(String featureFileName, String weightsFile,
            FeatureProcessorManager featProc)
    throws IOException
    {
        HalfPhoneFeatureFileReader ffr = new HalfPhoneFeatureFileReader(featureFileName);
        load(ffr, weightsFile, featProc);
    }
    
    public void load(FeatureFileReader featureFileReader, String weightsFile, FeatureProcessorManager featProc)
    throws IOException
    {
        if (!(featureFileReader instanceof HalfPhoneFeatureFileReader))
            throw new IllegalArgumentException("Featurefilereader must be a HalfPhoneFeatureFileReader");
        HalfPhoneFeatureFileReader ffr = (HalfPhoneFeatureFileReader) featureFileReader;
        this.leftWeights = ffr.getLeftWeights();
        this.featureDefinition = this.leftWeights;
        this.rightWeights = ffr.getRightWeights();
        this.featureVectors = ffr.getFeatureVectors();

        if (weightsFile != null) {
            String[] weightsFiles = weightsFile.split("\\|");
            if (weightsFiles.length != 2)
                throw new IllegalArgumentException("Parameter weightsFile should contain exactly two fields separated by a '|' character -- instead, it is: '"+weightsFile+"'");
            File leftF = new File(weightsFiles[0].trim());
            File rightF;
            // If the second weights file has no path, it is in the same directory as the first
            if (weightsFiles[1].indexOf("/") == -1
                && weightsFiles[1].indexOf("\\") == -1) {
                File dir = leftF.getParentFile();
                rightF = new File(dir, weightsFiles[1].trim());
            } else {
                rightF = new File(weightsFiles[1].trim());
            }
            
            // overwrite weights from files
            FeatureDefinition newLeftWeights = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(leftF), "UTF-8")), true);
            if (!newLeftWeights.featureEquals(leftWeights)) {
                throw new IOException("Weights file '"+leftF+"': feature definition incompatible with feature file");
            }
            leftWeights = newLeftWeights;
            FeatureDefinition newRightWeights = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(rightF), "UTF-8")), true);
            if (!newRightWeights.featureEquals(rightWeights)) {
                throw new IOException("Weights file '"+rightF+"': feature definition incompatible with feature file");
            }
            rightWeights = newRightWeights;
        }
        WeightFunctionManager wfm = new WeightFunctionManager();
        WeightFunc linear = wfm.getWeightFunction( "linear" );
        int nDiscreteFeatures = leftWeights.getNumberOfByteFeatures()+leftWeights.getNumberOfShortFeatures();
        leftWeightFunction = new WeightFunc[leftWeights.getNumberOfContinuousFeatures()];
        rightWeightFunction = new WeightFunc[leftWeightFunction.length];
        for ( int i = 0; i < leftWeightFunction.length; i++ ) {
            String weightFunctionName = leftWeights.getWeightFunctionName(nDiscreteFeatures+i);
            if ( "".equals( weightFunctionName ) )
                leftWeightFunction[i] = linear;
            else
                leftWeightFunction[i] = wfm.getWeightFunction(weightFunctionName);
            weightFunctionName = rightWeights.getWeightFunctionName(nDiscreteFeatures+i);
            if ( "".equals( weightFunctionName ) )
                rightWeightFunction[i] = linear;
            else
                rightWeightFunction[i] = wfm.getWeightFunction(weightFunctionName);
        }
        // TODO: If the target feature computer had direct access to the feature definition, it could do some consistency checking
        this.targetFeatureComputer = new TargetFeatureComputer(featProc, leftWeights.getFeatureNames());
        
        rememberWhichWeightsAreNonZero();

        if (MaryProperties.getBoolean("debug.show.cost.graph")) {
            debugShowCostGraph = true;
            cumulWeightedCosts = new double[featureDefinition.getNumberOfFeatures()];
            TargetCostReporter tcr2 = new TargetCostReporter(cumulWeightedCosts);
            tcr2.showInJFrame("Average weighted target costs", false, false);
            tcr2.start();
        }
    }

    /**
     * Compute the features for a given target, and store them in the target.
     * @param target the target for which to compute the features
     * @see Target#getFeatureVector()
     */
    public void computeTargetFeatures(Target target)
    {
        FeatureVector fv = targetFeatureComputer.computeFeatureVector(target);
        target.setFeatureVector(fv);
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
    
    public class TargetCostReporter extends Histogram
    {
        private double[] data;
        private int lastN = 0;
        public TargetCostReporter(double[] data)
        {
            super(0, 1, data);
            this.data = data;
        }
        
        public void start()
        {
            new Thread() {
                public void run() {
                    while (isVisible()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ie) {}
                        updateGraph();
                    }
                }
            }.start();
        }
        
        protected void updateGraph()
        {
            if (nCostComputations == lastN) return;
            lastN = nCostComputations;
            double[] newCosts = new double[data.length];
            for (int i=0; i<newCosts.length; i++) {
                newCosts[i] = data[i] / nCostComputations;
            }
            updateData(0, 1, newCosts);
            repaint();
        }
    }
}
