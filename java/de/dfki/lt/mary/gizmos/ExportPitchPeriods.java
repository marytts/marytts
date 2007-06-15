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
package de.dfki.lt.mary.gizmos;

import java.io.IOException;

import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.voiceimport.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport.WaveTimelineMaker;

public class ExportPitchPeriods {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        WaveTimelineMaker wtlm = new WaveTimelineMaker();
        DatabaseLayout dbl = new DatabaseLayout(wtlm);
        TimelineReader tlr = new TimelineReader(wtlm.getProp(wtlm.WAVETIMELINE));
        WavWriter ww = new WavWriter();
        // for ( int i = 0; i < tlr.getNumDatagrams(); i++ ) {
        for ( int i = 0; i < 10000; i++ ) {
            /*String fName = ( dbl.rootDirName() + "/tests/pitchperiods/"
                    + (i<1000?"0":"") + (i<100?"0":"") + (i<10?"0":"") + i + ".wav" );*/
            String fName = ( dbl.getProp(dbl.ROOTDIR) + "tests/pitchperiods/" + i + ".wav" );
            System.out.println( fName );
            ww.export( fName, 16000, tlr.getDatagram( tlr.getTimePointer() ).getData() );
        }
    }

}
