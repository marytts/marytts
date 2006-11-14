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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.sun.speech.freetts.Item;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors;
import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunctionManager;

public class JoinCostFeatures implements JoinCostFunction {

    protected final float wSignal = Float.parseFloat(MaryProperties.getProperty("joincostfunction.wSignal", "1.0"));
    protected final float wPhonetic = 1 - wSignal;
    
    /****************/
    /* DATA FIELDS  */
    /****************/
    private MaryHeader hdr = null;
    
    private float[] featureWeight = null;
    private WeightFunc[] weightFunction = null;    
    
    private float[][] leftJCF = null;
    private float[][] rightJCF = null;
    
    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Empty constructor; when using this, call load() separately to 
     * initialise this class.
     * @see #load(String)
     */
    public JoinCostFeatures()
    {
    }
    
    /**
     * Constructor which read a Mary Join Cost file.
     */
    public JoinCostFeatures( String fileName ) throws IOException
    {
        load(fileName, null);
    }
    
    /**
     * Load weights and values from the given file
     * @param joinFileName the file from which to read default weights and join cost features
     * @param weightsFileName an optional file from which to read weights, taking precedence over
     * the ones given in the join file
     */
    public void load(String joinFileName, String weightsFileName)
    throws IOException
    {
        /* Open the file */
        File fid = new File( joinFileName );
        DataInput raf = new DataInputStream(new BufferedInputStream(new FileInputStream( fid )));
        /* Read the Mary header */
        hdr = new MaryHeader( raf );
        if ( !hdr.isMaryHeader() ) {
            throw new IOException( "File [" + joinFileName + "] is not a valid Mary format file." );
        }
        if ( hdr.getType() != MaryHeader.JOINFEATS ) {
            throw new IOException( "File [" + joinFileName + "] is not a valid Mary join features file." );
        }
        try {
            /* Read the feature weights and feature processors */
            int numberOfFeatures = raf.readInt();
            featureWeight = new float[numberOfFeatures];
            weightFunction = new WeightFunc[numberOfFeatures];
            WeightFunctionManager wfm = new WeightFunctionManager();
            String wfStr = null;
            for ( int i = 0; i < numberOfFeatures; i++ ) {
                featureWeight[i] = raf.readFloat();
                wfStr = raf.readUTF();
                if ( "".equals( wfStr ) ) weightFunction[i] = wfm.getWeightFunction( "linear" );
                else                      weightFunction[i] = wfm.getWeightFunction(  wfStr   );
            }
            // Overwrite weights and weight functions from file?
            if (weightsFileName != null) {
                Logger.getLogger("JoinCostFeatures").debug("Overwriting join cost weights from file "+weightsFileName);
                Object[] weightData = readJoinCostWeightsFile(weightsFileName);
                featureWeight = (float[]) weightData[0];
                String[] wf = (String[])weightData[1];
                if (featureWeight.length != numberOfFeatures)
                    throw new IllegalArgumentException("Join cost file contains "+numberOfFeatures+" features, but weight file contains "+featureWeight.length+" feature weights!");
                for (int i=0; i<numberOfFeatures; i++) {
                    weightFunction[i] = wfm.getWeightFunction(wf[i]);
                }
            }
            
            
            /* Read the left and right Join Cost Features */
            int numberOfUnits = raf.readInt();
            leftJCF = new float[numberOfUnits][];
            rightJCF = new float[numberOfUnits][];
            for ( int i = 0; i < numberOfUnits; i++ ) {
                //System.out.println("Reading join features for unit "+i+" out of "+numberOfUnits);
                leftJCF[i] = new float[numberOfFeatures];
                for ( int j = 0; j < numberOfFeatures; j++ ) {
                    leftJCF[i][j] = raf.readFloat();
                }
                rightJCF[i] = new float[numberOfFeatures];
                for ( int j = 0; j < numberOfFeatures; j++ ) {
                    rightJCF[i][j] = raf.readFloat();
                }
            }
        }
        catch ( EOFException e ) {
            IOException ioe = new IOException( "The currently read Join Cost File has prematurely reached EOF.");
            ioe.initCause(e);
            throw ioe;
            
        }
    }
    
    /**
     * Read the join cost weight specifications from the given file.
     * The weights will be normalized such that they sum to one.
     * @param fileName the text file containing the join weights
     * */
    public static Object[] readJoinCostWeightsFile( String fileName ) throws IOException, FileNotFoundException {
        Vector v = new Vector( 16, 16 );
        Vector vf = new Vector( 16, 16 );
        /* Open the file */
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        /* Loop through the lines */
        String line = null;
        String[] fields = null;
        float sumOfWeights = 0;
        while ((line = in.readLine()) != null) {
            // System.out.println( line );
            line = line.split( "#", 2 )[0];  // Remove possible trailing comments
            line = line.trim();              // Remove leading and trailing blanks
            if ( line.equals("") ) continue; // Empty line: don't parse
            line = line.split( ":", 2 )[1].trim();  // Remove the line number and :
            // System.out.print( "CLEANED: [" + line + "]" );
            fields = line.split( "\\s", 2 ); // Separate the weight value from the function name
            float aWeight = Float.parseFloat(fields[0]);
            sumOfWeights += aWeight;
            v.add( new Float(aWeight) ); // Push the weight
            vf.add( fields[1] );             // Push the function
            // System.out.println( "NBFEA=" + numberOfFeatures );
        }
        // System.out.flush();
        /* Export the vector of weighting function names as a String array: */
        String[] wfun = (String[]) vf.toArray( new String[vf.size()] );
        /* For the weights, create a float array containing the weights,
         * normalized such that they sum to one: */
        float[] fw = new float[v.size()];
        for (int i=0; i<fw.length; i++) {
            Float aWeight = (Float) v.get(i);
            fw[i] = aWeight.floatValue() / sumOfWeights;
        }
        /* Return these as an Object[2].*/
        return new Object[] {fw, wfun};
    }

    /*****************/
    /* ACCESSORS     */
    /*****************/
    
    /**
     * Get the number of feature weights and weighting functions.
     */
    public int getNumberOfFeatures() {
        return( featureWeight.length );
    }
    
    /**
     * Get the number of units.
     */
    public int getNumberOfUnits() {
        return( leftJCF.length );
    }
    
        
    /**
     * Gets the array of left join cost features for a particular unit index.
     * 
     * @param u The index of the considered unit.
     * 
     * @return The array of left join cost features for the given unit.
     */
    public float[] getLeftJCF( int u ) {
        if ( u < 0 ) {
            throw new RuntimeException( "The unit index [" + u +
                    "] is out of range: a unit index can't be negative." );
        }
        if ( u > getNumberOfUnits() ) {
            throw new RuntimeException( "The unit index [" + u +
                    "] is out of range: this file contains [" + getNumberOfUnits() + "] units." );
        }
        return( leftJCF[u] );
    }
    
    /**
     * Gets the array of right join cost features for a particular unit index.
     * 
     * @param u The index of the considered unit.
     * 
     * @return The array of right join cost features for the given unit.
     */
    public float[] getRightJCF( int u ) {
        if ( u < 0 ) {
            throw new RuntimeException( "The unit index [" + u +
                    "] is out of range: a unit index can't be negative." );
        }
        if ( u > getNumberOfUnits() ) {
            throw new RuntimeException( "The unit index [" + u +
                    "] is out of range: this file contains [" + getNumberOfUnits() + "] units." );
        }
        return( rightJCF[u] );
    }
    
    /*****************/
    /* MISC METHODS  */
    /*****************/
    
    /**
     * Deliver the join cost between two units described by their index.
     * 
     * @param u1 the left unit
     * @param u2 the right unit
     * 
     * @return the cost of joining the right Join Cost features
     * of the left unit with the left Join Cost Features
     * of the right unit.
     */
    public double cost( int u1, int u2 ) {
        /* Check the given indexes */
        if ( u1 < 0 ) {
            throw new RuntimeException( "The left unit index [" + u1 +
                    "] is out of range: a unit index can't be negative." );
        }
        if ( u1 > getNumberOfUnits() ) {
            throw new RuntimeException( "The left unit index [" + u1 +
                    "] is out of range: this file contains [" + getNumberOfUnits() + "] units." );
        }
        if ( u2 < 0 ) {
            throw new RuntimeException( "The right unit index [" + u2 +
                    "] is out of range: a unit index can't be negative." );
        }
        if ( u2 > getNumberOfUnits() ) {
            throw new RuntimeException( "The right unit index [" + u2 +
                    "] is out of range: this file contains [" + getNumberOfUnits() + "] units." );
        }
        /* Cumulate the join costs for each feature */
        double res = 0.0;
        float[] v1 = rightJCF[u1];
        float[] v2 = leftJCF[u2];
        for ( int i = 0; i < v1.length; i++ ) {
            res += featureWeight[i] * weightFunction[i].cost( v1[i], v2[i] );
        }
        return( res );
    }
    
    /**
     * A combined cost computation, as a weighted sum
     * of the signal-based cost (computed from the units)
     * and the phonetics-based cost (computed from the targets).
     * 
     * @param t1 The left target.
     * @param u1 The left unit.
     * @param t2 The right target.
     * @param u2 The right unit.
     * 
     * @return the cost of joining the left unit with the right unit, as a non-negative value.
     */
    public double cost( Target t1, Unit u1, Target t2, Unit u2 ) {
        // Units of length 0 cannot be joined:
        if (u1.getDuration() == 0 || u2.getDuration() == 0) return Double.POSITIVE_INFINITY;
        // In the case of diphones, replace them with the relevant part:
        if (u1 instanceof DiphoneUnit) {
            assert t1 instanceof DiphoneTarget;
            t1 = ((DiphoneTarget)t1).getRight();
            u1 = ((DiphoneUnit)u1).getRight();
        }
        if (u2 instanceof DiphoneUnit) {
            assert t2 instanceof DiphoneTarget;
            t2 = ((DiphoneTarget)t2).getLeft();
            u2 = ((DiphoneUnit)u2).getLeft();
        }
        
        if (u1.index+1 == u2.index) return 0;
        if (t1 instanceof HalfPhoneTarget // half phone synthesis
            && ((HalfPhoneTarget)t1).isRightHalf()) { // We are at a phoneme boundary
            // Try to avoid joining at phoneme boundaries:
            return 10;
        }
        // Either not half phone synthesis, or at a diphone boundary
        double cost = 1; // basic penalty for joins of non-contiguous units. 
        if (wSignal > 0)
            cost += wSignal * cost( u1.index, u2.index );
        if (wPhonetic > 0)
            cost += wPhonetic * cost(t1, t2);
        return cost;
    }
    
    /**
     * A phonetic join cost, computed solely from the target.
     * @param t1 the left target
     * @param t2 the right target
     * @return a non-negative join cost, usually between 0 (best) and 1 (worst).
     */
    protected double cost(Target t1, Target t2)
    {
        // TODO: This is really ad hoc for the moment. Redo once we know what we are doing.
        // Add penalties for a number of criteria.
        double cost = 0;
        // Stressed?
        boolean stressed1 = false;
        Item syllable1 = new MaryGenericFeatureProcessors.SyllableNavigator().getItem(t1);
        if (syllable1 != null) {
            String value = syllable1.getFeatures().getString("stress");
            if (value != null && value.equals("1")) stressed1 = true;
        }
        boolean stressed2 = false;
        Item syllable2 = new MaryGenericFeatureProcessors.SyllableNavigator().getItem(t2);
        if (syllable2 != null) {
            String value = syllable2.getFeatures().getString("stress");
            if (value != null && value.equals("1")) stressed2 = true;
        }
        // Try to avoid joining in a stressed syllable:
        if (stressed1 || stressed2) cost += 0.2;
        Phoneme p1 = t1.getSampaPhoneme();
        Phoneme p2 = t2.getSampaPhoneme();
                
        // Discourage joining vowels:
        if (p1.isVowel() || p2.isVowel()) cost += 0.2;
        // Discourage joining glides:
        if (p1.isGlide() || p2.isGlide()) cost += 0.2;
        // Discourage joining voiced segments:
        if (p1.isVoiced() || p2.isVoiced()) cost += 0.1;
        // If both are voiced, it's really bad
        if (p1.isVoiced() && p2.isVoiced()) cost += 0.1;
        // Slightly penalize nasals and liquids
        if (p1.isNasal() || p2.isNasal()) cost += 0.05;
        if (p1.isLiquid() || p2.isLiquid()) cost += 0.05;
        // Fricatives -- nothing?
        // Plosives -- nothing?
        
        if (cost > 1) cost = 1;
        return cost;
    }
    
}
