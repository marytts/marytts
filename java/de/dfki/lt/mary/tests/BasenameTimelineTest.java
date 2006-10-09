package de.dfki.lt.mary.tests;

import de.dfki.lt.mary.unitselection.*;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.*;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

import java.io.IOException;

public class BasenameTimelineTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        
        DatabaseLayout dbl = new DatabaseLayout();
        UnitFileReader ufr = new UnitFileReader( dbl.unitFileName() );
        TimelineReader tlr = new TimelineReader( dbl.basenameTimelineFileName() );
        //TimelineReader tlr = new TimelineReader( dbl.lpcTimelineFileName() );
        FeatureFileReader ffr = new FeatureFileReader( dbl.targetFeaturesFileName() );
        FeatureDefinition feaDef = ffr.getFeatureDefinition();
        
        System.out.println( "Sample rates:\nunit file -> [" + ufr.getSampleRate()
                + "]Hz\ntimeline -> [" + tlr.getSampleRate() + "]Hz\n" );
        
        int[] testUnits = { 0, 1, 2, 41, 42, 43, 44, 45,
                60, 61, 62, 63, 64, 103, 104,
                22942, 22959,
                22960, 22986 };
        
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
        }
        
    }

}
