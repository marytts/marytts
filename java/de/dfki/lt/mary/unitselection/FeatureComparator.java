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

import java.util.Comparator;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

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

