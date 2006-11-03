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

import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;
import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunctionManager;

public class JoinCostFeatures implements JoinCostFunction {

    
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
                Float[] w = (Float[]) weightData[0];
                String[] wf = (String[])weightData[1];
                if (w.length != numberOfFeatures)
                    throw new IllegalArgumentException("Join cost file contains "+numberOfFeatures+" features, but weight file contains "+w.length+" feature weights!");
                for (int i=0; i<numberOfFeatures; i++) {
                    featureWeight[i] = w[i].floatValue();
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
                rightJCF[i] = new float[numberOfFeatures];
                for ( int j = 0; j < numberOfFeatures; j++ ) {
                    leftJCF[i][j] = raf.readFloat();
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
     * Read the join cost weight specifications from the relevant file.
     * */
    public static Object[] readJoinCostWeightsFile( String fileName ) throws IOException, FileNotFoundException {
        // TODO: code duplication: merge with code in JoinCostFileMaker
        Vector v = new Vector( 16, 16 );
        Vector vf = new Vector( 16, 16 );
        /* Open the file */
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        /* Loop through the lines */
        String line = null;
        String[] fields = null;
        while ((line = in.readLine()) != null) {
            // System.out.println( line );
            line = line.split( "#", 2 )[0];  // Remove possible trailing comments
            line = line.trim();              // Remove leading and trailing blanks
            if ( line.equals("") ) continue; // Empty line: don't parse
            line = line.split( ":", 2 )[1].trim();  // Remove the line number and :
            // System.out.print( "CLEANED: [" + line + "]" );
            fields = line.split( "\\s", 2 ); // Separate the weight value from the function name
            v.add( new Float( fields[0] ) ); // Push the weight
            vf.add( fields[1] );             // Push the function
            // System.out.println( "NBFEA=" + numberOfFeatures );
        }
        // System.out.flush();
        /* Export the vectors as arrays, and return these as an Object[2].*/
        Float[] fw = (Float[]) v.toArray( new Float[v.size()] );
        String[] wfun = (String[]) vf.toArray( new String[vf.size()] );
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
     * A cast of the cost() method to respect the JoinCostFunction interface.
     * 
     * @param u1 The left unit.
     * @param u2 The right unit.
     * 
     * @return the cost of joining the right Join Cost features
     * of the left unit with the left Join Cost Features
     * of the right unit, as an int value.
     */
    public double cost( Unit u1, Unit u2 ) {
        // If the two are neighbors, joining them is for free:
        if (u1.index+1 == u2.index) return 0;
        // Units of length 0 cannot be joined:
        if (u1.getDuration() == 0 || u2.getDuration() == 0) return Double.POSITIVE_INFINITY;
        return cost( u1.index, u2.index );
    }
    
}
