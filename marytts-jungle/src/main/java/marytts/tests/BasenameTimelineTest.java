/**
 * Copyright 2006 DFKI GmbH.
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

import java.io.File;

import marytts.features.FeatureDefinition;
import marytts.tools.voiceimport.BasenameTimelineMaker;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.HalfPhoneFeatureFileWriter;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.Datagram;


public class BasenameTimelineTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        
        BasenameTimelineMaker btlm = new BasenameTimelineMaker();
        HalfPhoneFeatureFileWriter ffw = new HalfPhoneFeatureFileWriter();
        VoiceImportComponent[] comps = new VoiceImportComponent[2];
        comps[0] = btlm;
        comps[1] = ffw;
        DatabaseLayout dbl = new DatabaseLayout(new File(System.getProperty("user.dir", "."), "database.config"), comps);
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
                String str = "For unit [" + unit[i].index + "], the returned basename datagram has [0] elements!";
                System.out.println( str );
            }
            else if ( dat.length != 1 ) {
                String str = "For unit [" + unit[i].index + "], the returned basename datagram has [" + dat.length + "] elements:\n";
                for ( int k = 0; k < dat.length; k++ ) {
                    str += (new String( dat[k].getData(), "UTF-8" ) ) + "\n";
                }
                throw new RuntimeException( str );
            }
            /* Output the results */
            basename = new String( dat[0].getData(), "UTF-8" );
            System.out.println( "Unit [" + unit[i].index + "] starts at sample [" + unit[i].startTime
                    + "] and has a duration of [" + unit[i].duration + "] samples." );
            System.out.println( "Unit [" + testUnits[i] + "] is found to come from file [" + basename + "] "
                    + "(with offset[" + offset[0] + "])." );
            String ph_name = feaDef.getFeatureValueAsString( 0, ffr.getFeatureVector( unit[i] ).getFeatureAsInt( 0 ) );
            System.out.println( "Unit [" + testUnits[i] + "] is found to be a [" + ph_name + "].\n" );
            //System.out.println( "Basename [" + basename + "] was ending at time [" + tlr.getTimePointer() + "].\n" );
            System.out.flush();
        }
        
        System.out.println( "Pre-final unit starts at: " + unit[unit.length-2].startTime + " and has duration " + unit[unit.length-2].duration );
        System.out.println( "Pre-final unit ends at: " + (unit[unit.length-2].startTime + unit[unit.length-2].duration) );
        System.out.println( "" );
        System.out.println( "Final unit starts at: " + unit[unit.length-1].startTime + " and has duration " + unit[unit.length-1].duration );
        System.out.println( "Final unit ends at: " + unit[unit.length-1].startTime );
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

