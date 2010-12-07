/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.unitselection.select;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.server.MaryProperties;
import marytts.signalproc.display.Histogram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.weightingfunctions.WeightFunc;
import marytts.unitselection.weightingfunctions.WeightFunctionManager;
import marytts.vocalizations.VocalizationFeatureFileReader;

import org.apache.log4j.Logger;

/**
 * FFRTargetCostFunction for vocalization selection
 * @author sathish pammi
 *
 */
public class VocalizationFFRTargetCostFunction extends FFRTargetCostFunction 
{
        
    public VocalizationFFRTargetCostFunction(VocalizationFeatureFileReader ffr) {
        this(ffr, ffr.getFeatureDefinition());
    }

    public VocalizationFFRTargetCostFunction(VocalizationFeatureFileReader ffr, FeatureDefinition fDef) {
        load(ffr, fDef);
    }
    
    /**
     * load feature file reader and  feature definition for a cost function
     * @param ffr feature file reader
     * @param fDef feature definition
     */
    private void load(VocalizationFeatureFileReader ffr, FeatureDefinition fDef) {
        this.featureVectors = ffr.featureVectorMapping(fDef);
        this.featureDefinition = fDef;
        
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
        
        rememberWhichWeightsAreNonZero();
    }
}

