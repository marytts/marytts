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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.FeatureFileIndexingResult;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.unitselection.voiceimport.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport.HalfPhoneFeatureFileWriter;
import de.dfki.lt.mary.unitselection.voiceimport.WaveTimelineMaker;
import de.dfki.lt.mary.unitselection.voiceimport.VoiceImportComponent;

public class ListenToPreselection {
    
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {

        WaveTimelineMaker wtlm = new WaveTimelineMaker();
        HalfPhoneFeatureFileWriter ffw = new HalfPhoneFeatureFileWriter();
        VoiceImportComponent[] comps = new VoiceImportComponent[2];
        comps[0] = wtlm;
        comps[1] = ffw;
        DatabaseLayout dbl = new DatabaseLayout(comps);
        UnitFileReader ufr = new UnitFileReader( ffw.getProp(ffw.UNITFILE));
        TimelineReader tlr = new TimelineReader( wtlm.getProp(wtlm.WAVETIMELINE));
        //TimelineReader tlr = new TimelineReader( dbl.lpcTimelineFileName() );
        FeatureFileIndexer ffi = new FeatureFileIndexer( ffw.getProp(ffw.FEATUREFILE));
        FeatureDefinition feaDef = ffi.getFeatureDefinition();
        WavWriter ww = new WavWriter();
        
        System.out.println( "Indexing the phonemes..." );
        String[] feaSeq = { "mary_phoneme" }; // Sort by phoneme name
        ffi.deepSort( feaSeq );
        
        /* Loop across possible phonemes */
        long tic = System.currentTimeMillis();
        int mary_phonemeIndex = feaDef.getFeatureIndex("mary_phoneme");
        int nbPhonVal = feaDef.getNumberOfValues( feaDef.getFeatureIndex( "mary_phoneme" ) );
        for ( int phon = 1; phon < nbPhonVal; phon++ ) {
        // for ( int phon = 14; phon < nbPhonVal; phon++ ) {
            String phonID = feaDef.getFeatureValueAsString( 0, phon );
            /* Loop across all instances */
            byte[] phonFeature = new byte[mary_phonemeIndex+1];
            phonFeature[mary_phonemeIndex] = (byte)( phon );
            FeatureVector target = new FeatureVector( phonFeature, new short[0], new float[0], 0 );
            FeatureFileIndexingResult instances = ffi.retrieve( target );
            int[] ui = instances.getUnitIndexes();
            System.out.println( "Concatenating the phoneme [" + phonID + "] which has [" + ui.length + "] instances..." );
            ByteArrayOutputStream bbis = new ByteArrayOutputStream();
            /* Concatenate the instances */
            for ( int i = 0; i < ui.length; i++ ) {
                /* Concatenate the datagrams from the instances */
                Datagram[] dat = tlr.getDatagrams( ufr.getUnit(ui[i]), ufr.getSampleRate() );
                for ( int k = 0; k < dat.length; k++ ) {
                    bbis.write( dat[k].getData() );
                }
            }
            /* Get the bytes as an array */
            byte[] buf = bbis.toByteArray();
            /* Output the header of the wav file */
            String fName = ( dbl.getProp(dbl.ROOTDIR) + "/tests/" + phonID + ".wav" );
            System.out.println( "Outputting file [" + fName + "]..." );
            ww.export( fName, 16000, buf );
            /* Sanity check */
            /* WavReader wr = new WavReader( dbl.rootDirName() + "/tests/" + phonID + ".wav" );
            System.out.println( "File [" + ( dbl.rootDirName() + "/tests/" + phonID + ".wav" )
                    + "] has [" + wr.getNumSamples() + "] samples." ); */
        }
        long toc = System.currentTimeMillis();
        System.out.println( "Copying the phonemes took [" + (toc-tic) + "] milliseconds." );
    }

}
