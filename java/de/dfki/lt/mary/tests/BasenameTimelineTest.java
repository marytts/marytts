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
package de.dfki.lt.mary.tests;

import java.io.IOException;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.voiceimport.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport.HalfPhoneFeatureFileWriter;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameTimelineMaker;
import de.dfki.lt.mary.unitselection.voiceimport.VoiceImportComponent;

public class BasenameTimelineTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        
        BasenameTimelineMaker btlm = new BasenameTimelineMaker();
        HalfPhoneFeatureFileWriter ffw = new HalfPhoneFeatureFileWriter();
        VoiceImportComponent[] comps = new VoiceImportComponent[2];
        comps[0] = btlm;
        comps[1] = ffw;
        DatabaseLayout dbl = new DatabaseLayout(comps);
        UnitFileReader ufr = new UnitFileReader(ffw.getProp(ffw.UNITFILE));
        TimelineReader tlr = new TimelineReader(btlm.getProp(btlm.TIMELINEFILE));
        //TimelineReader tlr = new TimelineReader( dbl.lpcTimelineFileName() );
        FeatureFileReader ffr = new FeatureFileReader(ffw.getProp(ffw.FEATUREFILE));
        FeatureDefinition feaDef = ffr.getFeatureDefinition();
        
        System.out.println( "Sample rates:\nunit file -> [" + ufr.getSampleRate()
                + "]Hz\ntimeline -> [" + tlr.getSampleRate() + "]Hz\n" );
        
        int[] testUnits = { 0, 1, 2, 41, 42, 43, 44, 45,
                60, 61, 62, 63, 64, 103, 104,
                22942, 22959,
                22960, 22980, 22986 };
        
        Unit[] unit = ufr.getUnit( testUnits );
//        for ( int i = 0; i < unit.length; i++ ) {
//            System.out.println( "Unit [" + unit[i].getIndex() + "] starts at sample [" + unit[i].getStart()
//                    + "] and has a duration of [" + unit[i].getDuration() + "] samples." );
//        }
        
        String basename = null;
        for ( int i = 0; i < unit.length; i++ ) {
            long[] offset = new long[1];
            Datagram[] dat = tlr.getDatagrams( unit[i], ufr.getSampleRate(), offset );
            /* Assert that there is only one filename: */
            if ( dat.length == 0 ) {
                String str = "For unit [" + unit[i].getIndex() + "], the returned basename datagram has [0] elements!";
                System.out.println( str );
            }
            else if ( dat.length != 1 ) {
                String str = "For unit [" + unit[i].getIndex() + "], the returned basename datagram has [" + dat.length + "] elements:\n";
                for ( int k = 0; k < dat.length; k++ ) {
                    str += (new String( dat[k].getData(), "UTF-8" ) ) + "\n";
                }
                throw new RuntimeException( str );
            }
            /* Output the results */
            basename = new String( dat[0].getData(), "UTF-8" );
            System.out.println( "Unit [" + unit[i].getIndex() + "] starts at sample [" + unit[i].getStart()
                    + "] and has a duration of [" + unit[i].getDuration() + "] samples." );
            System.out.println( "Unit [" + testUnits[i] + "] is found to come from file [" + basename + "] "
                    + "(with offset[" + offset[0] + "])." );
            String ph_name = feaDef.getFeatureValueAsString( 0, ffr.getFeatureVector( unit[i] ).getFeatureAsInt( 0 ) );
            System.out.println( "Unit [" + testUnits[i] + "] is found to be a [" + ph_name + "].\n" );
            System.out.println( "Basename [" + basename + "] was ending at time [" + tlr.getTimePointer() + "].\n" );
            System.out.flush();
        }
        
        System.out.println( "Pre-final unit starts at: " + unit[unit.length-2].getStart() + " and has duration " + unit[unit.length-2].getDuration() );
        System.out.println( "Pre-final unit ends at: " + (unit[unit.length-2].getStart() + unit[unit.length-2].getDuration()) );
        System.out.println( "" );
        System.out.println( "Final unit starts at: " + unit[unit.length-1].getStart() + " and has duration " + unit[unit.length-1].getDuration() );
        System.out.println( "Final unit ends at: " + unit[unit.length-1].getStart() );
        System.out.println( "" );
        
        final int OVERFLOWTIME = 27079952;
        try {
            Datagram[] dat = tlr.getDatagrams( OVERFLOWTIME, 1, ufr.getSampleRate() );
            basename = new String( dat[0].getData(), "UTF-8" );
            System.out.println( "Voluntary time overflow [" + OVERFLOWTIME + "] is found to come from file [" + basename + "]), NO exception was produced." );
        }
        catch ( IndexOutOfBoundsException e ) {
            System.out.println( "The voluntary overflow DID produce an IndexOutOfBoundsException." );
        }
    }

}
