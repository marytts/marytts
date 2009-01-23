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

import java.util.Comparator;

import marytts.features.FeatureVector;


public class FeatureComparator implements Comparator<FeatureVector> {
 
    /** The index of the feature to be compared in the feature vector. */
    private int I = -1;

    /**
     * Constructor which initializes the feature index.
     * @param setI The index of the feature to be compared on the next
     * run of the comparator.
     */
    public FeatureComparator( int setI ) {
        setFeatureIdx( setI );
    }
    
    /**
     * Accessor to set the feature index.
     * @param setI The index of the feature to be compared on the next
     * run of the comparator.
     */
    public void setFeatureIdx( int setI ) {
        I = setI;
    }
    
    /**
     * Access the index of the currently compared feature.
     * @return The index of the feature which the comparator
     * currently deals with.
     */
    public int getFeatureIdx() {
        return( I );
    }
    
    /**
     * Compares two feature vectors according to their values
     * at an internal index previously set by this.setFeatureIdx().
     * 
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return a negative integer, zero, or a positive integer
     * as the feature at index I for v1 is less than, equal to,
     * or greater than the feature at index I for v2.
     * 
     * @see FeatureComparator#setFeatureIdx(int)
     */
    public int compare( FeatureVector a, FeatureVector b ) {
        Comparable n1 = (Comparable)( a.getFeature( I ) );
        Comparable n2 = (Comparable)( b.getFeature( I ) );
        return( n1.compareTo( n2 ) );
    }
    
    /**
     * The equals() method asked for by the Comparable interface.
     * Returns true if the compared object is a FeatureComparator
     * with the same internal index, false otherwise.
     */
    public boolean equals( Object obj ) {
        if ( !( obj instanceof FeatureComparator ) ) return false;
        else if ( ((FeatureComparator)obj).getFeatureIdx() != this.I ) return false;
        return( true );
    }
}

/**
 * An additional comparator for the unit indexes in the feature vectors.
 * 
 */
class UnitIndexComparator implements Comparator<FeatureVector> {
    
    public int compare( FeatureVector a, FeatureVector b ) {
        return( a.getUnitIndex() - b.getUnitIndex() );
    }

}


