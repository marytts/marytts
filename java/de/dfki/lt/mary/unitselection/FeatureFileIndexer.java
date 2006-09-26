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
    
    private Node tree = null;
    private int[] featureSequence = null;
    private FeatureComparator c = new FeatureComparator( -1 );
    private UnitIndexComparator cui = new UnitIndexComparator();

    /**
     * Constructor which loads the feature file and launches an indexing
     * operation.
     * 
     * @param fileName The name of the file to load.
     * @param setFeatureSequence An array of indexes indicating the hierarchical order
     * (or, equivalently, the sequence) of the features to use for the indexing.
     * 
     * @throws IOException
     * @see FeaturefileReader
     */
    public FeatureFileIndexer( String fileName, int[] setFeatureSequence ) throws IOException {
        super( fileName );
        deepSort( setFeatureSequence );
    }
    
    /**
     * A local sort at a particular node along the deep sorting operation.
     * This is a recursive function.
     * 
     * @param currentFeatureIdx The currently tested feature.
     * @param currentNode The current node, holding the currently processed
     * zone in the array of feature vectors.
     */
    private void sortNode( int currentFeatureIdx, Node currentNode ) {
        /* If we have reached a leaf, do a final sort according to the unit index and return: */
        if ( currentFeatureIdx == featureSequence.length ) {
            Arrays.sort( featureVectors, cui );
            return;
        }
        /* Else: */
        /* Perform the sorting according to the currently considered feature: */
        /* 1) position the comparator onto the right feature */
        c.setFeatureIdx( featureSequence[currentFeatureIdx] );
        /* 2) do the sorting */
        Arrays.sort( featureVectors, currentNode.from, currentNode.to, c );
        
        /* Then, seek for the zones where the feature value is the same,
         * and launch the next sort level on these. */
        int nVal = featureDefinition.getNumberOfValues( currentFeatureIdx );
        currentNode.split( nVal );
        int nextFrom = currentNode.from;
        int nextTo = currentNode.from;
        for ( int i = 0; i < nVal; i++ ) {
            nextFrom = nextTo;
            while ( featureVectors[nextTo].getFeatureAsInt( currentFeatureIdx ) == i ) nextTo++;
            Node nod = new Node( nextFrom, nextTo );
            currentNode.setChild( i, nod );
            sortNode( currentFeatureIdx+1, nod );
        }
    }
    
    /**
     * Launches a deep sort on the array of feature vectors. This is public because
     * it can be used to re-index the previously read feature file.
     * 
      * @param featureIdx An array of feature indexes, indicating the sequence of
     * features according to which the sorting should be performed.
     */
    public void deepSort( int[] setFeatureSequence ) {
        featureSequence = setFeatureSequence;
        tree = new Node( 0, featureVectors.length );
        sortNode( 0, tree );
    }
    
    /**
     * Retrieve an array of unit features which complies with a specific target specification.
     * For this to work, a preliminary indexing should be performed with the deepSort( int f[] )
     * method.
     * 
     * @param v A feature vector for which to send back an array of complying unit indexes. 
     * @return An array of feature vectors.
     * 
     * @see FeatureFileIndexer#deepSort(int[])
     */
    public FeatureVector[] retrieve( FeatureVector v ) {
        /* Walk down the tree */
        Node n = tree;
        for ( int i = 0; i < featureSequence.length; i++ ) {
            n = n.getChild( v.getFeatureAsInt( featureSequence[i] ) );
        }
        /* Check if we actually reached a leaf (this is more a debug check) */
        if ( !n.isLeaf() ) {
            throw new RuntimeException( "Something went wrong:"
                    + " the retrieve operation did not lead to a leaf node." );
        }
        /* Dereference the leaf */
        int retFrom = n.from;
        int retTo = n.to;
        FeatureVector[] ret = new FeatureVector[retTo - retFrom];
        for ( int i = retFrom; i < retTo; i++ ) {
            ret[i-retFrom] = featureVectors[i];
        }
        return( ret );
    }
    
}

/**
 * A local helper class to make a node of a tree.
 * */
class Node {
   protected int from = 0;
   protected int to = 0;
   private Node[] kids = null;
   
   /***************************/
   /* Constructor             */
   public Node( int setFrom, int setTo ) {
       from = setFrom;
       to = setTo;
       kids = null;
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
   
   /*************************************/
   /* Check if this is a node or a leaf */
   public boolean isNode() {
       return( kids != null );
   }
   public boolean isLeaf() {
       return( kids == null );
   }
   
}
