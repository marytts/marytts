/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.tests;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

/**
 * Test case for the FeatureFileIndexer.
 * 
 * @author sacha
 *
 */
public class FeatureFileIndexerTest extends TestCase {

    /**
     * MAIN!
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        /* Load a target feature file */
        String feaFile = "/home/cl-home/sacha/disk/cmu_us_bdl_arctic/mary/targetFeatures.bin";
        FeatureFileIndexer ffi = new FeatureFileIndexer( feaFile );
        
        /* Determine the number of discrete features */
        FeatureDefinition feaDef = ffi.getFeatureDefinition();
        int numberOfDiscreteFeatures = feaDef.getNumberOfByteFeatures() + ffi.getFeatureDefinition().getNumberOfShortFeatures();
        System.out.println( "Found [" + numberOfDiscreteFeatures + "] discrete features in the feature definition." );
        int check = 1;
        int xorWidth = 0;
        for ( int i = 0; i < numberOfDiscreteFeatures; i++ ) {
            int nVal = feaDef.getNumberOfValues( i );
            check *= nVal;
            xorWidth += nVal;
            System.out.println( "Feature [" + feaDef.getFeatureName(i)
                    + " - " + i + "]\thas [" + feaDef.getNumberOfValues( i ) + "] values. (Number of leaves = ["
                    + check + "] - Total bits [" + xorWidth + "].)" );
            
        }
        System.out.println("");
        
        //BitSet bs = new BitSet();
        
        /* Make a random sequence of NUMFEATS discrete features */
        int[] feaSeq = new int[numberOfDiscreteFeatures];
        for ( int i = 0; i < numberOfDiscreteFeatures; i++ ) {
            feaSeq[i] = i;
        }
        Random rand = new Random(); // New random number generator
        final int NUMSHUFFLES = 1000;
        int i1 = 0;
        int i2 = 0;
        int dummy;
        for ( int i = 0; i < NUMSHUFFLES; i++ ) {
            i1 = rand.nextInt( numberOfDiscreteFeatures );
            i2 = rand.nextInt( numberOfDiscreteFeatures );
            dummy = feaSeq[i1];
            feaSeq[i1] = feaSeq[i2];
            feaSeq[i2] = dummy;
        }
        final int NUMFEATS = 42;
        int[] feaFinalSeq = new int[NUMFEATS];
        System.arraycopy( feaSeq, 0, feaFinalSeq, 0, NUMFEATS );
        check = 1;
        xorWidth = 0;
        
        // CHEATS
        
        // feaFinalSeq[0] = 0; // Cheat this value to force phoneme ID
        
        // ALL features
//      feaFinalSeq = new int[42];
//      for ( int i = 0; i < 42; i++ ) {
//      feaFinalSeq[i] = i;
//      }
        
        // CART top level
//      feaFinalSeq = new int[3];
//      feaFinalSeq[0] = 0; // Cheat this value to force phoneme ID
//      feaFinalSeq[1] = 3; // ph_cplace
//      feaFinalSeq[2] = 28;
        
//      feaFinalSeq = new int[2];
//      feaFinalSeq[0] = 0; // Cheat this value to force phoneme ID
//      feaFinalSeq[1] = 3; // c_place
        
        //String[] feaList = { "mary_phoneme", "mary_ph_cplace" };
        String[] feaList = { "mary_ph_cplace", "mary_phoneme" };
        feaFinalSeq = ffi.getFeatureDefinition().getFeatureIndexArray( feaList );
        
        // Phonemes in context
//      feaFinalSeq = new int[3];
//      feaFinalSeq[0] = 0; // Cheat this value to force phoneme ID
//      feaFinalSeq[1] = 10; // c_place
//      feaFinalSeq[2] = 19;
        
        // END CHEATS
        
        for ( int i = 0; i < feaFinalSeq.length; i++ ) {
            int nVal = feaDef.getNumberOfValues( feaFinalSeq[i] );
            check *= nVal;
            xorWidth += nVal;
            System.out.println( "Feature [" + feaDef.getFeatureName(feaFinalSeq[i])
                    + " - " + feaFinalSeq[i] + "]\thas [" + feaDef.getNumberOfValues( feaFinalSeq[i] )
                    + "] values. (Number of leaves = [" + check + "] - Total bits ["
                    + xorWidth + "].)" );
        }
        
        /**** CHEAT THE ABOVE VALUE ****/
        // feaFinalSeq = new int[1];
        // feaFinalSeq[0] = 0;

        
        /* Measure the indexing time */
        System.out.println( "Starting to index across [" + ffi.getNumberOfUnits() + "] units..." );
        System.out.flush();
        long tic = System.currentTimeMillis();
        final int NUMITER = 10;
        long subtic = tic;
        long subtoc = tic;
        for ( int i = 0; i < (NUMITER+1); i++ ) {
        //for ( int i = 0; i < 1; i++ ) {
            subtic = subtoc;
            //ffi.deepSort( feaFinalSeq );
            ffi.deepSort( feaList );
            subtoc = System.currentTimeMillis();
            System.out.println( "Iteration [" + i + "] took [" + (subtoc-subtic) + "] milliseconds." );
            System.out.flush();
            if ( i == 0 ) tic = subtoc; // Discard the first iteration
            System.out.flush();
        }
        long toc = System.currentTimeMillis();
        System.out.println( "Average indexing time over [" + NUMITER + "] iterations : [" + ((toc-tic)/NUMITER) + "] milliseconds." );
        System.out.flush();
        
        System.out.println( "TREE DEBUG OUT" );
        ffi.getTree().toStandardOut( ffi, 0 );
        System.out.flush();
        
        /* Measure the retrieval time */
        
    }

}
