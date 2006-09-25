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

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.FeatureComparator;

import java.util.Comparator;
import java.util.Arrays;
import java.io.IOException;

public class FeatureFileIndexer extends FeaturefileReader {
    
    Node tree = null;

    /**
     * Constructor which loads the feature file.
     * 
     * @param fileName The name of the file to load.
     * @throws IOException
     * @see FeaturefileReader
     */
    public FeatureFileIndexer( String fileName ) throws IOException {
        super( fileName );
    }
    
    /**
     * Sort the array of feature vectors according to a particular feature.
     * 
     * @param featureIdx The index of the feature to use for the sorting operation.
     */
    public void sort( int featureIdx ) {
        Comparator c = new FeatureComparator( featureIdx );
        Arrays.sort( featureVectors, c );
    }
    
    /**
     * Sort the array of feature vectors according to the values of a particular feature,
     * between index fromIndex (inclusive) and index toIndex (exclusive).
     * 
     * @param fromIndex The beginning of the zone to sort, inclusive.
     * @param toIndex The end of the zone to sort, exclusive.
     * @param featureIdx The index of the feature to use for the sorting operation.
     */
    public void sort( int fromIndex, int toIndex, int featureIdx ) {
        Comparator c = new FeatureComparator( featureIdx );
        Arrays.sort( featureVectors, fromIndex, toIndex, c );
    }
    
    /**
     * Sort an array of feature vectors according to an externally defined
     * feature comparator.
     * 
     * @param c The feature comparator to use.
     * 
     * @see FeatureComparator.
     */
    public void sort( FeatureComparator c ) {
        Arrays.sort( featureVectors, c );
    }
    
    /**
     * Sort the array of feature vectors according to an externally defined
     * feature comparator, between index fromIndex (inclusive) and index
     * toIndex (exclusive).
     * 
     * @param fromIndex The beginning of the zone to sort, inclusive.
     * @param toIndex The end of the zone to sort, exclusive.
     * @param c The feature comparator to use.
     * 
     * @see FeatureComparator.
     */
    public void sort( int fromIndex, int toIndex,
            FeatureComparator c ) {
        Arrays.sort( featureVectors, fromIndex, toIndex, c );
    }
    
    /**
     * A local sort at a particular node of the deep sorting operation.
     * 
     * @param fromIndex The beginning of the array zone to be sorted (inclusive).
     * @param toIndex The end of the array zone to be sorted (exclusive).
     * @param currentFeatureIdx The currently tested feature.
     * @param featureIdx The sequence of features to test.
     * @param c An externally declared comparator.
     */
    private void sortNode( int fromIndex, int toIndex,
            int currentFeatureIdx, int featureIdx[], FeatureComparator c,
            Node currentNode ) {
        /* If we have reached a leaf, do a final sort according to the unit index
         * and then save the leaf and return: */
        if ( currentFeatureIdx == featureIdx.length ) {
            int[] ui = new int[toIndex-fromIndex];
            for ( int i = fromIndex, j = 0; i < toIndex; i++, j++ ) {
                ui[j] = featureVectors[i].getUnitIndex();
            }
            Arrays.sort( ui );
            currentNode.blossom( ui );
            return;
        }
        /* Else: */
        /* Perform the sorting according to the currently considered feature. */
        c.setFeatureIdx( featureIdx[currentFeatureIdx] );
        sort( fromIndex, toIndex, c );
        /* Seek for the zones where the feature value is the same, and launch the next
         * sort level on these. */
        int nVal = featureDefinition.getNumberOfValues(currentFeatureIdx);
        currentNode.split( nVal );
        int nextFrom = fromIndex;
        int nextTo = fromIndex;
        for ( int i = 0; i < nVal; i++ ) {
            nextFrom = nextTo;
            while ( featureVectors[nextTo].getFeature( currentFeatureIdx ).intValue() == i ) nextTo++;
            Node n = new Node( nextFrom, nextTo );
            currentNode.setChild( i, n );
            sortNode( nextFrom, nextTo, currentFeatureIdx+1, featureIdx, c, n );
        }
    }
    
    /**
     * Launches a deep sort on the array of feature vectors.
     * 
      * @param featureIdx An array of feature indexes, indicating the sequence of
     * features according to which the sorting should be performed.
     */
    public void deepSort( int[] featureIdx ) {
        FeatureComparator c = new FeatureComparator( featureIdx[0] );
        tree = new Node( 0, featureVectors.length );
        sortNode( 0, featureVectors.length, 0, featureIdx, c, tree );
    }
    
    /**
     * Retrieve an array of indexes of units which comply with a specific sequence of
     * feature values. For this to work, a preliminary indexing should be performed
     * with the deepSort( int f[] ) method.
     * 
     * @param v A feature vector for which to send back an array of complying unit indexes. 
     * @param f The sequence of indexes indicating the order along which the values
     * of v should be considered. This sequence should be the same as the one used for
     * the mandatory preliminary call to deepSort( f ).
     * @return An array of unit indexes.
     * 
     * @see FeatureFileIndexer#deepSort(int[])
     */
    public int[] retrieve( FeatureVector v, int[] f ) {
        Node leaf = tree.dive( v, f, 0 );
        return( leaf.getUnitIndexes() );
    }
    
}

/**
 * A local helper class to make a node of a tree.
 * */
class Node {
   private int from = 0;
   private int to = 0;
   private Node[] kids = null;
   private int[] uIndexes = null;
   
   /***************************/
   /* Constructor             */
   public Node( int setFrom, int setTo ) {
       from = setFrom;
       to = setTo;
       kids = null;
       uIndexes = null;
   }
   
   /******************************/
   /* Getters for various fields */
   public int getFrom() {
       return( from );
   }
   
   public int getTo() {
       return( to );
   }
   
   public int getNumChildren() {
       return( kids.length );
   }
   
   public int[] getUnitIndexes() {
       return( uIndexes );
   }
   
   public Node[] getChildren() {
       return( kids );
   }
   
   /***************************/
   /* Node splitting          */
   public void split( int numKids ) {
       kids = new Node[numKids];
   }
   
   public void setChild( int i, Node n ) {
       kids[i] = n;
   }
   
   public Node getChild( int i ) {
       return( kids[i] );
   }
   
   /***************************/
   /* Turn a node into a leaf */
   public void blossom( int numIdx ) {
       uIndexes = new int[numIdx];
   }
   
   public void setUnitIdx( int i, int ui ) {
       uIndexes[i] = ui;
   }

   public int getUnitIdx( int i ) {
       return( uIndexes[i] );
   }
   
   public void blossom( int[] ui ) {
       uIndexes = ui;
   }
   
   /*************************************/
   /* Check if this is a node or a leaf */
   public boolean isNode() {
       return( (uIndexes == null) && (kids != null) );
   }
   public boolean isLeaf() {
       return( (uIndexes != null) && (kids == null) );
   }
   
   /*************************************************/
   /* Mine the tree for a particular feature vector */
   public Node dive( FeatureVector v, int[] f, int i ) {
       if ( this.isLeaf() ) return( this );
       /* If we are not a leaf, continue branching: */
       
       /*                At the current node, branch to the node           */
       /*                corresponding to the value of the current feature */
       /*                      |                                           */
       /*                      v                                           */
       else return( kids[v.getFeature(f[i]).intValue()].dive( v, f, i+1 ) );
       /*                                                            ^     */
       /*                                                            |     */
       /*                 From the next node, explore the next feature     */
   }
   
}
