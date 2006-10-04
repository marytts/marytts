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
import de.dfki.lt.mary.unitselection.MaryNode;

import java.util.Comparator;
import java.util.Arrays;
import java.io.IOException;

public class FeatureFileIndexer extends FeatureFileReader {
    
    private MaryNode tree = null;
    private int[] featureSequence = null;
    private FeatureComparator c = new FeatureComparator( -1 );
    private UnitIndexComparator cui = new UnitIndexComparator();
    
//    long tic = 0;
//    long toc = 0;
//    long nol = 0;
//    long nextNol = 0;
    
    long totnol = 0;

    /**
     * Constructor which loads the feature file and launches an indexing
     * operation.
     * 
     * @param fileName The name of the file to load.
     * @param setFeatureSequence An array of indexes indicating the hierarchical order
     * (or, equivalently, the sequence) of the features to use for the indexing.
     * 
     * @throws IOException
     * @see FeatureFileReader
     */
    public FeatureFileIndexer( String fileName, int[] setFeatureSequence ) throws IOException {
        super( fileName );
        deepSort( setFeatureSequence );
    }
    
    /**
     * Constructor which loads the feature file and but does not launch an indexing
     * operation.
     * 
     * @param fileName The name of the file to load.
     * 
     * @throws IOException
     * @see FeatureFileReader
     */
    public FeatureFileIndexer( String fileName ) throws IOException {
        super( fileName );
    }
    
    /**
     * A local sort at a particular node along the deep sorting operation.
     * This is a recursive function.
     * 
     * @param currentFeatureIdx The currently tested feature.
     * @param currentNode The current node, holding the currently processed
     * zone in the array of feature vectors.
     */
    private void sortNode( int currentFeatureIdx, MaryNode currentNode ) {
        /* If we have reached a leaf, do a final sort according to the unit index and return: */
        if ( currentFeatureIdx == featureSequence.length ) {
            Arrays.sort( featureVectors, currentNode.from, currentNode.to, cui );
//            nol++;
//            if ( nol == nextNol ) {
//                long localtic = toc;
//                toc = System.currentTimeMillis();
//                System.out.println( "Reached leaf [" + nol + "/" + totnol + "] in [" + (toc-localtic)
//                        + " milliseconds (Elapsed time : [" + (toc-tic) + "] milliseconds.)" );
//                System.out.flush();
//                nextNol += 1000;
//            }
            
            /*System.out.print( "LEAF ! " );
            for ( int i = currentNode.from; i < currentNode.to; i++ ) {
                System.out.print( " (" + featureVectors[i].getUnitIndex() + " 0)" );
            }
            System.out.println( "" ); */
            
            return;
        }
        /* Else: */
        int currentFeature = featureSequence[currentFeatureIdx];
        /* Register the feature currently used for the splitting */
        currentNode.setFeatureIndex( currentFeature );
        /* Perform the sorting according to the currently considered feature: */
        /* 1) position the comparator onto the right feature */
        c.setFeatureIdx( currentFeature );
        /* 2) do the sorting */
        Arrays.sort( featureVectors, currentNode.from, currentNode.to, c );
        
        /* Then, seek for the zones where the feature value is the same,
         * and launch the next sort level on these. */
        int nVal = featureDefinition.getNumberOfValues( currentFeature );
        currentNode.split( nVal );
        int nextFrom = currentNode.from;
        int nextTo = currentNode.from;
        for ( int i = 0; i < nVal; i++ ) {
            nextFrom = nextTo;
            // System.out.println( "BEGIN ZONE at " + nextFrom );
            while ( (nextTo < featureVectors.length) && (featureVectors[nextTo].getFeatureAsInt( currentFeature ) == i)  ) {
                // System.out.print( " " + featureVectors[nextTo].getFeatureAsInt( currentFeature ) );
                nextTo++;
            }
            // System.out.println( "END ZONE at " + nextTo );
            MaryNode nod = new MaryNode( nextFrom, nextTo );
            currentNode.setChild( i, nod );
            // System.out.print("(" + i + " isByteOf " + currentFeature + ")" );
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
        totnol = getNumberOfLeaves();
        System.out.println( "Building a tree with [" + totnol + "] leaves..." ); System.out.flush();
        if ( totnol == -1 ) {
            throw new RuntimeException( "The number of leaves blows the capacity of the long type!"
                    + " -> The given feature sequence is too big to build a tree." );
        }
//        nol = 0;
//        nextNol = 1000;
//        tic = System.currentTimeMillis();
        tree = new MaryNode( 0, featureVectors.length );
        sortNode( 0, tree );
    }
    
    /**
     * Retrieve an array of unit features which complies with a specific target specification,
     * according to an underlying tree.
     * 
     * @param v A feature vector for which to send back an array of complying unit indexes. 
     * @return An array of feature vectors.
     * 
     * @see FeatureFileIndexer#deepSort(int[])
     */
    public FeatureVector[] retrieve( FeatureVector v ) {
        if ( featureSequence == null ) {
            throw new RuntimeException( "Can't retrieve candidate units if a tree has not been built. (Run this.deepSort(int[]) first.)" );
        }
        /* Walk down the tree */
        MaryNode n = tree;
        while ( !n.isLeaf() ) {
            n = n.getChild( v.getFeatureAsInt( n.getFeatureIndex() ) );
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
    
    /**
     * Retrieve an array of unit features which complies with a specific target specification,
     * given an underlying tree, but only down to a certain number of levels.
     * 
     * @param v A feature vector for which to send back an array of complying unit indexes.
     * @param numLevels A limit on the number of levels to cross.
     * 
     * @return An array of feature vectors.
     * 
     * @see FeatureFileIndexer#deepSort(int[])
     */
    public FeatureVector[] retrieve( FeatureVector v, int numLevels ) {
        if ( featureSequence == null ) {
            throw new RuntimeException( "Can't retrieve candidate units if a tree has not been built. (Run this.deepSort(int[]) first.)" );
        }
        if ( numLevels > featureSequence.length ) {
            throw new RuntimeException( "Can't walk down to a tree further than the length of the underlying feature sequence." );
        }
        /* Walk down the tree */
        MaryNode n = tree;
        for ( int i = 0; i < numLevels; i++ ) {
            n = n.getChild( v.getFeatureAsInt( n.getFeatureIndex() ) );
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
    
    /**
     * Get the feature sequence, as an information about the underlying tree structure.
     * 
     * @return the feature sequence
     */
    public int[] getFeatureSequence(){
        return featureSequence;
    }
    
    /**
     * Get the tree
     * @return the tree
     */
    public MaryNode getTree(){
        return tree;
    }
    
    /**
     * Get the feature vectors from the big array 
     * according to the given indices
     * @param from the start index
     * @param to the end index
     * @return the feature vectors
     */
    public FeatureVector[] getFeatureVectors(int from, int to){
        FeatureVector[] vectors = new FeatureVector[to-from];
        for ( int i = from; i < to; i++ ) {
            vectors[i-from] = featureVectors[i];
        }
        return vectors;
    }
    
    /**
     * Get the number of leaves.
     * 
     * @return The number of leaves.
     */
    public long getNumberOfLeaves() {
        long ret = 1;
        for ( int i = 0; i < featureSequence.length; i++ ) {
//            System.out.println( "Feature [" + i + "] has [" + featureDefinition.getNumberOfValues( featureSequence[i] ) + "] values."
//                    + "(Number of leaves = [" + ret + "].)" );
            ret *= featureDefinition.getNumberOfValues( featureSequence[i] );
            if ( ret < 0 ) return( -1 );
        }
        return( ret );
    }
}
