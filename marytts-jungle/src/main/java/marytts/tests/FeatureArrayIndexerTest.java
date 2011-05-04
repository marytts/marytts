/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.tests;

import java.util.Random;

import marytts.cart.impose.FeatureArrayIndexer;
import marytts.features.FeatureDefinition;
import marytts.unitselection.data.FeatureFileReader;

/**
 * Test case for the FeatureFileIndexer.
 * 
 * @author sacha
 *
 */
public class FeatureArrayIndexerTest {

    /**
     * MAIN!
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        /* Load a target feature file */
        String feaFile = "/home/cl-home/sacha/disk/cmu_us_bdl_arctic/mary/targetFeatures.bin";
        FeatureFileReader ffr = new FeatureFileReader(feaFile);
        FeatureArrayIndexer ffi = new FeatureArrayIndexer( ffr.getFeatureVectors(), ffr.getFeatureDefinition() );
        
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
        
        // feaFinalSeq[0] = 0; // Cheat this value to force phone ID
        
        // ALL features
//      feaFinalSeq = new int[42];
//      for ( int i = 0; i < 42; i++ ) {
//      feaFinalSeq[i] = i;
//      }
        
        // CART top level
//      feaFinalSeq = new int[3];
//      feaFinalSeq[0] = 0; // Cheat this value to force phone ID
//      feaFinalSeq[1] = 3; // ph_cplace
//      feaFinalSeq[2] = 28;
        
//      feaFinalSeq = new int[2];
//      feaFinalSeq[0] = 0; // Cheat this value to force phone ID
//      feaFinalSeq[1] = 3; // c_place
        
        //String[] feaList = { "phone", "ph_cplace" };
        String[] feaList = { "ph_cplace", "phone" };
        feaFinalSeq = ffi.getFeatureDefinition().getFeatureIndexArray( feaList );
        
        // Phonemes in context
//      feaFinalSeq = new int[3];
//      feaFinalSeq[0] = 0; // Cheat this value to force phone ID
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
        System.out.println( "Starting to index across [" + ffr.getNumberOfUnits() + "] units..." );
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

