/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.*;
import java.util.*;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.JoinCostFeatures;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.util.MaryUtils;

public class JoinCostFileMaker extends VoiceImportComponent {
    
    private DatabaseLayout db = null;
    private int percent = 0;
    private String mcepExt = ".mcep";    
    private int numberOfFeatures = 0;
    private float[] fw = null;
    private String[] wfun = null;
    
    public final String JOINCOSTFILE = "JoinCostFileMaker.joinCostFile";
    public final String MCEPTIMELINE = "JoinCostFileMaker.mcepTimeline";
    public final String UNITFILE = "JoinCostFileMaker.unitFile";
    public final String WEIGHTSFILE = "JoinCostFileMaker.weightsFile";
    public final String MCEPDIR = "JoinCostFileMaker.mcepDir";
    
    public String getName(){
        return "JoinCostFileMaker";
    }
    
     public void initialiseComp()
    {
        //make sure that we have a weights file
        File weightsFile = new File(getProp(WEIGHTSFILE));
        if (!weightsFile.exists()){
            try{
                PrintWriter weightsOut =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(weightsFile),"UTF-8"));
                printWeightsFile(weightsOut);
            } catch (Exception e){
                System.out.println("Warning: no join cost weights file "
                        +getProp(WEIGHTSFILE)
                        +"; JoinCostFileMaker will not run.");
            }            
        }
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           String filedir = db.getProp(db.FILEDIR);
           props.put(JOINCOSTFILE, filedir
                        +"joinCostFeatures"+db.getProp(db.MARYEXT));
           props.put(MCEPTIMELINE, filedir
                        +"timeline_mcep"+db.getProp(db.MARYEXT));
           props.put(UNITFILE, filedir
                        +"halfphoneUnits"+db.getProp(db.MARYEXT));
           props.put(WEIGHTSFILE, db.getProp(db.CONFIGDIR)
                        +"joinCostWeights.txt");
           props.put(MCEPDIR, db.getProp(db.ROOTDIR)
                        +"mcep"
                        +System.getProperty("file.separator"));
                  
       }
       return props;
    }
    
    protected void setupHelp(){         
        props2Help = new TreeMap();
        props2Help.put(JOINCOSTFILE, "file containing all halfphone units and their join cost features."
                +" Will be created by this module");
        props2Help.put(MCEPTIMELINE, "file containing all mcep files");
        props2Help.put(UNITFILE, "file containing all halfphone units");
        props2Help.put(WEIGHTSFILE, "file containing the list of join cost weights and their weights");
        props2Help.put(MCEPDIR, "directory containing the mcep files");
        
    }
    
    public boolean compute() throws IOException
    {
        System.out.print("---- Making the join cost file\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        
        /* Read the number of mel cepstra from the first melcep file */
        ESTTrackReader firstMcepFile = new ESTTrackReader(getProp(MCEPDIR) 
                + baseNameArray[0] + mcepExt);
        int numberOfMelcep = firstMcepFile.getNumChannels();
        firstMcepFile = null; // Free the memory taken by the file
        
        /* Make a new join cost file to write to */
        DataOutputStream jcf = null;
        try {
            jcf = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(getProp(JOINCOSTFILE)) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "Can't create the join cost file [" 
                    + getProp(JOINCOSTFILE)
                    + "]. The path is probably wrong.", e );
        }
        
        /**********/
        /* HEADER */
        /**********/
        /* Make a new mary header and ouput it */
        MaryHeader hdr = new MaryHeader( MaryHeader.JOINFEATS );
        try {
            hdr.writeTo( jcf );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException happened when writing the Mary header to the Join Cost file.", e );
        }
        hdr = null;
        
        /****************************/
        /* WEIGHTING FUNCTION SPECS */
        /****************************/
        /* Load the weight vectors */
        Object[] weightData = 
            JoinCostFeatures.readJoinCostWeightsFile(getProp(WEIGHTSFILE));
        fw = (float[]) weightData[0];
        wfun = (String[]) weightData[1];
        numberOfFeatures = fw.length;
        /* Output those vectors */
        try {
            jcf.writeInt( fw.length );
            for ( int i = 0; i < fw.length; i++ ) {
                jcf.writeFloat( fw[i] );
                jcf.writeUTF( wfun[i] );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException happened when writing the weighting specifications to the Join Cost file.", e );
        }
        /* Clean the house */
        fw = null;
        wfun = null;
        
        /************/
        /* FEATURES */
        /************/
        
        /* Open the melcep timeline */
        TimelineReader mcep = new TimelineReader(getProp(MCEPTIMELINE));
        
        /* Open the unit file */
        UnitFileReader ufr = new UnitFileReader(getProp(UNITFILE));
        
        /* Start writing the features: */
        try {
            /* - write the number of features: */
            jcf.writeInt( ufr.getNumberOfUnits() );
            /* - for each unit, write the left and right features: */
            Vector buff = new Vector( 0, 5 );
            // final int F0_HORIZON = 5;
            final int F0_HORIZON = 1;
            boolean averageF0AcrossUnitBoundary = true;
            long median = 0;
            double leftF0 = 0.0d;
            double prevRightF0 = 0.0d;
            double F0 = 0.0d;
            int unitSampleFreq = ufr.getSampleRate();
            long unitPosition = 0l;
            int unitDuration = 0;
            long endPoint = 0l;
            long targetEndPoint = 0l;
            Datagram dat = null;
            
            /* Check the consistency between the number of join cost features
             * and the number of Mel-cepstrum coefficients */
            dat = mcep.getDatagram( 0, unitSampleFreq );
            if ( dat.getData().length != (4*(numberOfFeatures-1)) ) {
                throw new RuntimeException( "The number of join cost features [" + numberOfFeatures
                        + "] read from the join cost weight config file [" + getProp(WEIGHTSFILE)
                        + "] does not match the number of Mel Cepstra [" + (dat.getData().length / 4)
                        + "] found in the Mel-Cepstrum timeline file [" + getProp(MCEPTIMELINE)
                        + "], plus [1] for the F0 feature." );
            }
            
            /* Loop through the units */

            for ( int i = 0; i < ufr.getNumberOfUnits(); i++ ) {
                percent = 100*i/ufr.getNumberOfUnits();

                /* Read the unit */
                unitPosition = ufr.getUnit(i).getStart();
                unitDuration = ufr.getUnit(i).getDuration();
                
                /* If the unit is not a START or END marker
                 * and has length > 0: */
                if ( unitDuration != -1 && unitDuration > 0 ) {
                    
                    /* Reset the datagram buffer */
                    buff.removeAllElements();
                    
                    /* -- COMPUTE the LEFT join cost features: */
                    /* Grow the datagram vector to F0_HORIZON datagram, but stop if it trespasses the unit boundary: */
                    targetEndPoint = unitPosition + unitDuration;
                    endPoint = unitPosition;
                    for ( int j = 0; j < F0_HORIZON; j++ ) {
                        if ( endPoint >= targetEndPoint ) break;
                        dat = mcep.getDatagram( endPoint, unitSampleFreq );
                        buff.add( dat );
                        endPoint += dat.getDuration();
                    }
                    /* Compute the left F0 from the datagram durations: */
                    assert buff.size() > 0 : "Unit seems to be shorter than one pitch period?!";
                    // number of periods is <= F0_HORIZON --
                    // usually ==, but < if unit is too short
                    long[] periods = new long[buff.size()];
                    for ( int j = 0; j < buff.size(); j++ ) {
                        dat = (Datagram) buff.elementAt( j );
                        periods[j] = dat.getDuration();
                    }
                    median = MaryUtils.median( periods );
                    leftF0 = (double)(unitSampleFreq) / (double)(median);
                    /* Compute the F0 joining this unit to the preceding one: */
                    F0 = (prevRightF0 + leftF0) / 2.0d;
                    
                    
                    /* -- WRITE: */
                    /* Complete the unfinished preceding unit by writing the join F0: */
                    if (averageF0AcrossUnitBoundary)
                        jcf.writeFloat( (float)( F0 ) );
                    else 
                        jcf.writeFloat( (float)( prevRightF0 ) );
                    // System.out.println( " and Right F0 is [" + F0 + "]Hz." );
                    /* Get the datagram corresponding to the left mel cepstra and pipe it out: */
                    dat = (Datagram) buff.elementAt( 0 );
                    jcf.write( dat.getData(), 0, dat.getData().length );
                    /* Write the left join F0, which is the same than at the end of the preceding unit: */
                    if (averageF0AcrossUnitBoundary)
                        jcf.writeFloat( (float)( F0 ) );
                    else
                        jcf.writeFloat( (float)( leftF0 ) );
                    // System.out.print( "At unit [" + i + "] :  (Buffsize " + buff.size() + ") Left F0 is [" + F0 + "]Hz" );
                    
                    
                    /* -- COMPUTE the RIGHT JCFs: */
                    /* Crawl along the datagrams until we trespass the end of the unit: */
                    if ( buff.size() == F0_HORIZON ) { /* => If the buffer is F0_HORIZON frames long,
                                                        *    it means we have not trespassed the unit yet,
                                                        *    so we can crawl further. */
                        dat = mcep.getDatagram( endPoint, unitSampleFreq );
                        while ( dat != null && (endPoint+dat.getDuration()) < targetEndPoint ) {
                            buff.removeElementAt( 0 );
                            buff.add( dat );
                            endPoint += dat.getDuration();
                            dat = mcep.getDatagram( endPoint, unitSampleFreq );
                        }
                        /* Compute the right F0 from the datagram durations: */
                        for ( int j = 0; j < buff.size(); j++ ) {
                            dat = (Datagram) buff.elementAt( j );
                            assert dat != null;
                            periods[j] = dat.getDuration();
                        }
                        median = MaryUtils.median( periods );
                        prevRightF0 = (double)(unitSampleFreq) / (double)(median);
                    }
                    /* Else, if we can't crawl any further, keep the same value for the left F0: */
                    else prevRightF0 = leftF0;
                    
                    
                    /* -- WRITE: */
                    /* Get the datagram corresponding to the right join cost feature and pipe it out: */
                    dat = (Datagram) buff.lastElement();
                    assert dat != null;
                    jcf.write( dat.getData(), 0, dat.getData().length );
                    /* But DO NOT WRITE the trailing join F0, because we don't know it yet. */
                }
                
                /* If the unit is a START or END marker, output dummy zeros
                 * for the left and right Join Cost Features, and assume F0= 0.0 across the unit: */
                else {
                    /* Compute the F0 joining this unit to the preceding one: */
                    F0 = prevRightF0 / 2.0d; // (Assuming that leftF0 is 0 for a null unit.)
                    
                    /* Write the preceding right F0 join, except if
                     * this is the very first unit in the file: */
                    if ( i != 0 ) {
                        if (averageF0AcrossUnitBoundary)
                            jcf.writeFloat( (float)(F0) );
                        else
                            jcf.writeFloat( (float)( prevRightF0 ) );
                        // System.out.println( " and Right F0 is [" + F0 + "]Hz." );
                    }
                    /* Write the left mel cepstra for the current unit: */
                    for ( int j = 0; j < numberOfMelcep; j++ ) {
                        jcf.writeFloat( 0.0f );
                    }
                    /* Write the left F0 join: */
                    if (averageF0AcrossUnitBoundary)
                        jcf.writeFloat( (float)(F0) ); // (Assuming that leftF0 is 0.0 here.)
                    else
                        jcf.writeFloat( 0.0f );
                    // System.out.print( "At unit [" + i + "] : START/END unit. (Buffsize 0) Left F0 is [" + F0 + "]Hz" );
                    /* Write the right mel cepstra for the current unit: */
                    for ( int j = 0; j < numberOfMelcep; j++ ) {
                        jcf.writeFloat( 0.0f );
                    }
                    /* DO NOT write the right F0 join, but do register the right F0 value: */
                    prevRightF0 = 0.0d;
                }
                
            }
            
            /* Complete the very last unit by flushing the right join F0: */
            F0 = prevRightF0 / 2.0d; // (Assuming that leftF0 is 0 for a null unit.)
            if (averageF0AcrossUnitBoundary)
                jcf.writeFloat( (float)( F0 ) );
            else
                jcf.writeFloat( (float)( prevRightF0 ) );
            // System.out.println( " and Right F0 is [" + F0 + "]Hz." );
            jcf.close();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException happened when writing the features to the Join Cost file.", e );
        }
        
        System.out.println("---- Join Cost file done.\n\n");
        System.out.println("Number of processed units: " + ufr.getNumberOfUnits() );
        
        JoinCostFeatures tester = new JoinCostFeatures(getProp(JOINCOSTFILE));
        int unitsOnDisk = tester.getNumberOfUnits();
        if (unitsOnDisk == ufr.getNumberOfUnits()) {
            System.out.println("Can read right number of units");
            return true;
        } else {
            System.out.println("Read wrong number of units: "+unitsOnDisk);
            return false;
        }
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

    private void printWeightsFile(PrintWriter weightsOut)throws Exception{
        weightsOut.println("# This file lists the weights and weighting functions to be used for\n"
                +"# creating the MARY join cost file, joinCostFeature.mry .\n"
                +"#\n"
                +"# Lines starting with '#' are ignored; they can be used for comments\n"
                +"# anywhere in the file. Empty lines are also ignored.\n"
                +"# Entries must have the following form:\n"
                +"# \n"
                +"# <feature index> : <weight value> <weighting function> <optional weighting function parameter>\n"
                +"# \n"
                +"# The <feature index> is an integer value from 0 to the number of join cost features\n"
                +"# minus one. It is used for readability, but is ignored when parsing the file.\n"
                +"# The database import process will nevertheless check that the number of valid\n"
                +"# lines corresponds to the number of join cost features specified from external\n"
                +"# constraints (such as the order of the Mel-Cepstra).\n"
                +"#\n"
                +"# The <weight value> is a float value in text format.\n"
                +"#\n"
                +"# The <weighting function> is a string, for the moment one of \"linear\" or \"step\".\n"
                +"#\n"
                +"# The <optional weighting function parameter> is a string giving additional optional\n"
                +"# info about the weighting function:\n"
                +"# - \"linear\" does not take an optional argument;\n"
                +"# - \"step\" takes a threshold position argument, e.g. \"step 20%\" means a step function\n"
                +"#   with weighs 0 when the join feature difference is less than 20%, and applies\n"
                +"#   the weight value when the join feature difference is 20% or more.\n"
                +"#\n"
                +"# THIS FILE WAS GENERATED AUTOMATICALLY\n"
                +"\n"
                +"# Weights applied to the Mel-cepstra:\n"
                +"0  : 1.0 linear\n"
                +"1  : 1.0 linear\n"
                +"2  : 1.0 linear\n"
                +"3  : 1.0 linear\n"
                +"4  : 1.0 linear\n"
                +"5  : 1.0 linear\n"
                +"6  : 1.0 linear\n"
                +"7  : 1.0 linear\n"
                +"8  : 1.0 linear\n"
                +"9  : 1.0 linear\n"
                +"10 : 1.0 linear\n"
                +"11 : 1.0 linear\n"
                +"\n"
                +"# Weight applied to the F0 parameter:\n"
                +"12 : 1.0 linear");
        weightsOut.flush();
        weightsOut.close();
    }

}
