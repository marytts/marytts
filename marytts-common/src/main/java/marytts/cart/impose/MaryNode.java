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
package marytts.cart.impose;

import marytts.features.FeatureVector;

/**
 * A generic node class for the tree structures.
 * 
 * @author sacha
 * 
 */
public class MaryNode {

	protected int featureIndex = -1;
	protected int from = 0;
	protected int to = 0;
	private MaryNode[] kids = null;

	/**
	 * @param setFrom
	 *            setFrom
	 * @param setTo
	 *            setTo
	 */
	/* Constructor */
	public MaryNode(int setFrom, int setTo) {
		from = setFrom;
		to = setTo;
		kids = null;
	}

	/**
	 * @return from
	 */

	/* Getters for various fields */
	public int getFrom() {
		return (from);
	}

	public int getTo() {
		return (to);
	}

	public int getNumChildren() {
		return (kids.length);
	}

	public MaryNode[] getChildren() {
		return (kids);
	}

	/**
	 * @param i
	 *            i
	 */
	/* Feature index management */
	public void setFeatureIndex(int i) {
		featureIndex = i;
	}

	public int getFeatureIndex() {
		return (featureIndex);
	}

	/**
	 * @param numKids
	 *            numKids
	 */
	/* Node splitting */
	public void split(int numKids) {
		kids = new MaryNode[numKids];
	}

	public void setChild(int i, MaryNode n) {
		kids[i] = n;
	}

	public MaryNode getChild(int i) {
		return (kids[i]);
	}

	/**
	 * @return kids different from null
	 */
	/* Check if this is a node or a leaf */
	public boolean isNode() {
		return (kids != null);
	}

	public boolean isLeaf() {
		return (kids == null);
	}

	// debug output
	public void toStandardOut(FeatureArrayIndexer ffi, int level) {

		String blanks = "";
		for (int i = 0; i < level; i++)
			blanks += "   ";

		if (kids != null) {
			String featureName = ffi.getFeatureDefinition().getFeatureName(featureIndex);
			System.out.println("Node " + featureName + " has " + (to - from) + " units divided into " + kids.length
					+ " branches.");
			for (int i = 0; i < kids.length; i++) {
				if (kids[i] != null) {
					System.out.print(blanks + "Branch " + i + "/" + kids.length + " ( "
							+ ffi.getFeatureDefinition().getFeatureName(featureIndex) + " is "
							+ ffi.getFeatureDefinition().getFeatureValueAsString(featureIndex, i) + " )" + " -> ");
					kids[i].toStandardOut(ffi, level + 1);
				} else {
					System.out.println(blanks + "Branch " + i + "/" + kids.length + " ( "
							+ ffi.getFeatureDefinition().getFeatureName(featureIndex) + " is "
							+ ffi.getFeatureDefinition().getFeatureValueAsString(featureIndex, i) + " )"
							+ " -> DEAD BRANCH (0 units)");
				}
			}
		} else {
			// get the unit indices
			FeatureVector[] fv = ffi.getFeatureVectors(from, to);
			System.out.print("LEAF has " + (to - from) + " units : ");
			for (int i = 0; i < fv.length; i++) {
				System.out.print(fv[i].getUnitIndex() + " ");
			}
			System.out.print("\n");
		}

	}

}
