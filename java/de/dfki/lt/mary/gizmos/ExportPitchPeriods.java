package de.dfki.lt.mary.gizmos;

import java.io.IOException;

import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.voiceimport.DatabaseLayout;

public class ExportPitchPeriods {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        DatabaseLayout dbl = new DatabaseLayout();
        TimelineReader tlr = new TimelineReader( dbl.waveTimelineFileName() );
        WavWriter ww = new WavWriter();
        // for ( int i = 0; i < tlr.getNumDatagrams(); i++ ) {
        for ( int i = 0; i < 10000; i++ ) {
            /*String fName = ( dbl.rootDirName() + "/tests/pitchperiods/"
                    + (i<1000?"0":"") + (i<100?"0":"") + (i<10?"0":"") + i + ".wav" );*/
            String fName = ( dbl.rootDirName() + "/tests/pitchperiods/" + i + ".wav" );
            System.out.println( fName );
            ww.export( fName, 16000, tlr.getDatagram( tlr.getTimePointer() ).getData() );
        }
    }

}
