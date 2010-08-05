/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.modules.acoustic;

import java.io.File;
import java.util.List;

import javax.swing.text.Document;

import org.w3c.dom.Element;

import marytts.cart.DirectedGraph;
import marytts.cart.io.DirectedGraphReader;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.unitselection.select.Target;

/**
 * Model for applying a CART to a list of Targets
 * 
 * @author steiner
 * 
 */
public class CARTModel extends Model {
    private DirectedGraph cart;

    public CARTModel(String type, String dataFileName, String targetAttributeName, String targetAttributeFormat,
            String targetElementListName, String modelFeatureName) {
        super(type, dataFileName, targetAttributeName, targetAttributeFormat, targetElementListName, modelFeatureName);
    }

    @Override
    public void setFeatureComputer(TargetFeatureComputer featureComputer, FeatureProcessorManager featureProcessorManager)
            throws MaryConfigurationException {
        // ensure that this CART's FeatureDefinition is a subset of the one passed in:
        FeatureDefinition cartFeatureDefinition = cart.getFeatureDefinition();
        FeatureDefinition voiceFeatureDefinition = featureComputer.getFeatureDefinition();
        if (!voiceFeatureDefinition.contains(cartFeatureDefinition)) {
            throw new MaryConfigurationException("CART file " + dataFile + " contains extra features which are not supported!");
        }
        // overwrite featureComputer with one constructed from the cart's FeatureDefinition:
        String cartFeatureNames = cartFeatureDefinition.getFeatureNames();
        featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, cartFeatureNames);
        this.featureComputer = featureComputer;
    }

    @Override
    public void loadDataFile() {
        this.cart = null;
        try {
            File cartFile = new File(dataFile);
            String cartFilePath = cartFile.getAbsolutePath();
            cart = new DirectedGraphReader().load(cartFilePath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Apply the CART to a Target to get its predicted value
     */
    @Override
    protected float evaluate(Target target) {
        float[] result = (float[]) cart.interpret(target);
        float value = result[1]; // assuming result is [stdev, val]
        return value;
    }
    
    @Override
    protected void evaluate(List<Element> applicableElements){ }
    
    @Override
    protected void evaluate(org.w3c.dom.Document doc){ }
    
}
