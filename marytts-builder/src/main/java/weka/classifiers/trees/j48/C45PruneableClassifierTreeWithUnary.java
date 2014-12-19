/**
 * Copyright 2012 DFKI GmbH.
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

package weka.classifiers.trees.j48;

import weka.classifiers.trees.j48.C45PruneableClassifierTree;
import weka.classifiers.trees.j48.ModelSelection;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;

/**
 * This Class is a wrapper for C45PruneableClassifierTree that enables the UNARY_CLASS capability.
 * 
 * @author Fabio Tesser
 * 
 */

public class C45PruneableClassifierTreeWithUnary extends C45PruneableClassifierTree {

	public C45PruneableClassifierTreeWithUnary(ModelSelection toSelectLocModel, boolean pruneTree, float cf, boolean raiseTree,
			boolean cleanup, boolean collapseTree) throws Exception {
		super(toSelectLocModel, pruneTree, cf, raiseTree, cleanup, collapseTree);
	}

	/**
	 * Returns default capabilities of the classifier tree.
	 * 
	 * @return the capabilities of this classifier tree
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.DATE_ATTRIBUTES);
		result.enable(Capability.MISSING_VALUES);
		result.enable(Capability.UNARY_CLASS); // Can be used also with Unary

		// class
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);

		// instances
		result.setMinimumNumberInstances(0);

		return result;
	}

}
