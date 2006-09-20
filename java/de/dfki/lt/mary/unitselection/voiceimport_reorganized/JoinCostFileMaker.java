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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class JoinCostFileMaker implements VoiceImportComponent {
    
    private DatabaseLayout db = null;
    private BasenameList bnl = null;
    
    public JoinCostFileMaker( DatabaseLayout setdb, BasenameList setbnl ) {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    public boolean compute()
    {
        System.out.println("---- Making the join cost file\n\n");
        System.out.println("Base directory: " + db.baseName() + "\n");
        System.out.println("Mel Cepstrum timeline: " + db.melcepTimelineFileName() + "\n");
        System.out.println("Outputting join cost file to: " + db.joinCostFeaturesFileName() + "\n");
        
        /* Export the basename list into an array of strings */
        String[] baseNameArray = bnl.getListAsArray();
        
        /* Read the number of mel cepstra from the first melcep file */
        /* TODO: this is a horrible hack, the number of mel cepstrum coeffs should be passed
         * in a more intelligent way. */
        ESTTrackReader firstMcepFile = new ESTTrackReader( db.melcepDirName() + baseNameArray[0] + db.melcepExt());
        int numberOfMelcep = firstMcepFile.getNumChannels();
        firstMcepFile = null; // Free the memory taken by the file
        
        /* Make a new join cost file to write to */
        DataOutputStream jcf = null;
        try {
            jcf = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( db.joinCostFeaturesFileName() ) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "Can't create the join cost file [" + db.joinCostFeaturesFileName() + "]. The path is probably wrong.", e );
        }
        
        /**********/
        /* HEADER */
        /**********/
        /* Make a new mary header and ouput it */
        MaryHeader hdr = new MaryHeader( MaryHeader.JOINFEATS );
        try {
            hdr.write( jcf );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException happened when writing the Mary header to the Join Cost file.", e );
        }
        hdr = null;
        
        /****************************/
        /* WEIGHTING FUNCTION SPECS */
        /****************************/
        /* Make a weight vector */
        int numberOfFeatures = numberOfMelcep + 1;
        float[] fw = new float[numberOfFeatures]; // +1 accounts for the addition of F0
        for ( int i = 0; i < fw.length; i++ ) {
            fw[i] = 1.0f;
        }
        /* Make a weighting function vector */
        String[] wfun = new String[numberOfFeatures];
        for ( int i = 0; i < (numberOfMelcep); i++ ) {
            wfun[i] = "linear";
        }
        wfun[numberOfMelcep+1] = "step 20%"; // This one is for F0
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
        TimelineReader mcep = new TimelineReader( db.melcepTimelineFileName() );
        
        /* Open the unit file */
        UnitFileReader ufr = new UnitFileReader( db.unitFileName() );
        
        /* Start writing the features: */
        try {
            /* - write the number of features: */
            jcf.writeInt( ufr.getNumberOfUnits() );
            /* - for each unit, write the left and right features: */
            Datagram[] buff = null;
            long[] periods = new long[5];
            long median = 0;
            float F0 = 0;
            int unitSampleFreq = ufr.getSampleRate();
            long unitPosition = 0l;
            int unitDuration = 0;
            for ( int i = 0; i < ufr.getNumberOfUnits(); i++ ) {
                
                /* LEFT */
                /* Get the left join cost feature datagram and pipe it out */
                buff = mcep.getDatagrams( unitPosition, 5, unitSampleFreq );
                jcf.write( buff[0].getData(), 0, buff[0].getData().length );
                /* Make the left F0 and write it out */
                for ( int j = 0; j < 5; j++ ) {
                    periods[j] = buff[i].duration;
                }
                Arrays.sort( periods );
                median = periods[2];
                F0 = (float)( (double)(unitSampleFreq) / (double)(median) );
                jcf.writeFloat( F0 );
                System.out.print( "At unit [" + i + "] Left F0 is [" + F0 + "]" );
                
                /* RIGHT*/
                /* Get the right join cost feature datagram and pipe it out */
                buff = mcep.getDatagrams( unitPosition + unitDuration - 1, 1, unitSampleFreq );
                /* Note: in the above line, the -1 insures that we are getting the last melcep frame
                 * of the current unit and not the first melcep frame of the next one. */
                jcf.write( buff[0].getData(), 0, buff[0].getData().length );
                /* Make the right F0 and write it out */
                for ( int j = 0; j < 5; j++ ) {
                    periods[j] = buff[i].duration;
                }
                Arrays.sort( periods );
                median = periods[2];
                F0 = (float)( (double)(unitSampleFreq) / (double)(median) );
                jcf.writeFloat( F0 );
                System.out.println( "and Right F0 is [" + F0 + "]" );
                
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException happened when writing the features to the Join Cost file.", e );
        }
        
        System.out.println("---- Join Cost file done.\n\n");
        System.out.println("Number of processed units: " + ufr.getNumberOfUnits() );
        
        return( true );
    }

}
