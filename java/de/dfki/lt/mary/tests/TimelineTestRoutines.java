/**
 * Copyright 2000-2006 DFKI GmbH.
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

import java.io.File;
import java.io.IOException;
import java.util.Random;

import junit.framework.Assert;
import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.voiceimport.TimelineWriter;

/**
 * Provides the actual timeline test case for the timeline reading/writing symmetry.
 */
public class TimelineTestRoutines extends TimelineReader {
    
    public TimelineTestRoutines( String fileName ) throws IOException 
    {
        super( fileName );
    }
    
    public static void testSymmetry() throws IOException {
        
        Random rand = new Random(); // New random number generator
        
        final int NUMDATAGRAMS = rand.nextInt( 87 ) + 4; // Number of datagrams to test with, between 4 and 100.
        System.out.println( "Testing with [" + NUMDATAGRAMS + "] random datagrams." );
        final int MAXDATAGRAMBYTESIZE = 64; // Maximum datagram length in bytes
        final int MAXDATAGRAMDURATION = 20; // Maximum datagram duration (in samples)
        final String hdrContents = "Blah This is the procHeader Blah";
        final int sampleRate = 1000;
        
        Datagram[] origDatagrams = new Datagram[NUMDATAGRAMS]; // An array of datagrams with random length
        int len = 0;
        long dur = 0l;
        
        final String tlFileName = "timelineTest.bin";
        Datagram[] readDatagrams = null;
        
        /* Fill the array of random datagrams */
        long lenCumul = 74;
        long durCumul = 0l;
        for ( int i = 0; i < NUMDATAGRAMS; i++ ) {
            /* Make a random length */
            len = rand.nextInt(MAXDATAGRAMBYTESIZE) + 1;
            lenCumul += (len + 12);
            /* Allocate the corresponding byte array */
            byte[] buff = new byte[len];
            /* Fill all the bytes with the datagram index */
            for ( int l = 0; l < len; l++ ) {
                buff[l] = new Integer(i).byteValue();
            }
            /* Make a random datagram duration */
            dur = (long)( rand.nextInt(MAXDATAGRAMDURATION) + 1 );
            durCumul += dur;
            /* Store the datagram */
            origDatagrams[i] = new Datagram( dur, buff );
            /* Check */
            System.out.println( "[ " + len + " , " + dur + " ]\t( " + lenCumul + " , " + durCumul + " )" );
        }
        
        /* Write the datagram array in a timeline */
        System.out.println( "Opening new timeline file..." );
        TimelineWriter tlw = new TimelineWriter( tlFileName , hdrContents, sampleRate, 0.1d );
        System.out.println( "Feeding..." );
        tlw.feed( origDatagrams, sampleRate );
        System.out.println( "Closing..." );
        tlw.close();
        System.out.println( "Done." );
        
        System.out.println( "Datagram zone pos. = " + tlw.getDatagramsBytePos() );
        System.out.println( "WRITTEN INDEX:" );
        tlw.printIdx();
        
        /* Testing the readonly file opening */
        System.out.println( "Testing the TimelineReader construction..." );
        /* Re-read the datagrams */
        //TimelineReader tlr = new TimelineReader( tlFileName );
        TimelineTestRoutines tlr = new TimelineTestRoutines( tlFileName );
        /* Check the procHeader */
        Assert.assertEquals( "The procHeader is out of sync.", tlr.getProcHeaderContents(), hdrContents );
        /* Check the number of datagrams */
        Assert.assertEquals( "numDatagrams is out of sync.", tlr.getNumDatagrams(), NUMDATAGRAMS );
        /* Check if the time pointer is zero just after opening the timeline */
        Assert.assertEquals( "The time pointer is out of sync after opening.", tlr.getTimePointer(), 0l );
        /* Check if the byte pointer is at the beginning of the datagram zone */
        Assert.assertEquals( "The byte pointer is out of sync after opening.", tlr.getBytePointer(), tlr.getDatagramsBytePos() );
        System.out.println( "READ INDEX:" );
        tlr.printIdx();
        
        /* Testing skip */
        System.out.println( "Testing skip..." );
        readDatagrams = new Datagram[NUMDATAGRAMS];
        long timeNow = tlr.getTimePointer();
        long timeBefore = 0l;
        long byteNow = tlr.getBytePointer();
        long byteBefore = 0l;
        for ( int i = 0; i < NUMDATAGRAMS; i++ ) {
            timeBefore = timeNow;
            byteBefore = byteNow;
            tlr.skipNextDatagram();
            timeNow = tlr.getTimePointer();
            byteNow = tlr.getBytePointer();
            Assert.assertEquals( "Skipping fails on datagram [" + i + "].", (long)(origDatagrams[i].getLength()) + 12l, (byteNow - byteBefore) );
            Assert.assertEquals( "Time is out of sync after skipping datagram [" + i + "].", origDatagrams[i].getDuration(), (timeNow - timeBefore) );
        }
        /* Testing the EOF trap for skip */
        Assert.assertTrue( tlr.skipNextDatagram() );

        /* Testing rewind() */
        System.out.println( "Testing rewind..." );
        tlr.rewind();
        Assert.assertEquals( "Rewind fails.", tlr.getBytePointer(), tlr.getDatagramsBytePos() );
        
        /* Testing get */
        System.out.println( "Testing get..." );
        readDatagrams = new Datagram[NUMDATAGRAMS];
        timeNow = tlr.getTimePointer();
        timeBefore = 0l;
        for ( int i = 0; i < NUMDATAGRAMS; i++ ) {
            timeBefore = timeNow;
            readDatagrams[i] = tlr.getNextDatagram();
            timeNow = tlr.getTimePointer();
            Assert.assertTrue( "Datagram [" + i + "] is out of sync.", areEqual( origDatagrams[i].getData(), readDatagrams[i].getData() ) );
            Assert.assertEquals( "Time for datagram [" + i + "] is out of sync.", readDatagrams[i].getDuration(),(timeNow - timeBefore)  );
        }
        /* Testing the EOF trap for get */
        Assert.assertEquals( null, tlr.getNextDatagram() );
        
        /* Testing the time-driven access */
        final int testIdx = NUMDATAGRAMS / 2;
        long onTime = 0l;
        for( int i = 0; i < testIdx; i++ ) {
            onTime += origDatagrams[i].getDuration();
        }
        long afterTime = onTime + origDatagrams[testIdx].getDuration();
        long midTime = onTime + ((afterTime - onTime) / 2);
        
        System.out.println( "Testing gotoTime 1 ..." );
        tlr.rewind();
        tlr.gotoTime( onTime, sampleRate );
        // System.out.println( "Position after gotoTime 1 : ( " + tlr.getBytePointer() + " , " + tlr.getTimePointer() + " )" );
        Datagram d = tlr.getNextDatagram();
        Assert.assertTrue( d.equals( origDatagrams[testIdx] ) );
        
        System.out.println( "Testing gotoTime 2 ..." );
        tlr.rewind();
        tlr.gotoTime( midTime, sampleRate );
        // System.out.println( "Position after gotoTime 2 : ( " + tlr.getBytePointer() + " , " + tlr.getTimePointer() + " )" );
        d = tlr.getNextDatagram();
        Assert.assertTrue( d.equals( origDatagrams[testIdx] ) );
        
        System.out.println( "Testing gotoTime 3 ..." );
        tlr.rewind();
        tlr.gotoTime( afterTime, sampleRate );
        // System.out.println( "Position after gotoTime 3 : ( " + tlr.getBytePointer() + " , " + tlr.getTimePointer() + " )" );
         d = tlr.getNextDatagram();
        Assert.assertTrue( d.equals( origDatagrams[testIdx+1] ) );
        
        
        /* Testing time-spanned access */
        System.out.println( "Testing getDatagrams  1 ..." );
        Datagram[] D = null;
        long span = origDatagrams[testIdx].getDuration();
        long[] offset = new long[1];
        tlr.rewind();
        D = tlr.getDatagrams( onTime, span, sampleRate, offset );
        Assert.assertEquals( 1, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        Assert.assertEquals( 0l, offset[0] );
        
        System.out.println( "Testing getDatagrams  2 ..." );
        span = origDatagrams[testIdx].getDuration() / 2;
        tlr.rewind();
        D = tlr.getDatagrams( onTime, span, sampleRate );
        Assert.assertEquals( 1, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        
        System.out.println( "Testing getDatagrams  3 ..." );
        span = origDatagrams[testIdx].getDuration() / 2;
        tlr.rewind();
        D = tlr.getDatagrams( midTime, span, sampleRate );
        Assert.assertEquals( 1, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        
        System.out.println( "Testing getDatagrams  4 ..." );
        span = origDatagrams[testIdx].getDuration() + 1;
        tlr.rewind();
        D = tlr.getDatagrams( onTime, span, sampleRate );
        Assert.assertEquals( 2, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        Assert.assertTrue( D[1].equals( origDatagrams[testIdx+1] ) );
        
        System.out.println( "Testing getDatagrams  5 ..." );
        span = origDatagrams[testIdx].getDuration() + origDatagrams[testIdx+1].getDuration();
        tlr.rewind();
        D = tlr.getDatagrams( onTime, span, sampleRate );
        Assert.assertEquals( 2, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        Assert.assertTrue( D[1].equals( origDatagrams[testIdx+1] ) );
        
        System.out.println( "Testing getDatagrams  6 ..." );
        span = origDatagrams[testIdx].getDuration() + origDatagrams[testIdx+1].getDuration();
        tlr.rewind();
        D = tlr.getDatagrams( onTime+1, span, sampleRate, offset );
        Assert.assertEquals( 3, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        Assert.assertTrue( D[1].equals( origDatagrams[testIdx+1] ) );
        Assert.assertTrue( D[2].equals( origDatagrams[testIdx+2] ) );
        Assert.assertEquals( 1l, offset[0] );
        
        System.out.println( "Testing getDatagrams  7 ..." );
        span = origDatagrams[testIdx].getDuration() / 2;
        tlr.rewind();
        D = tlr.getDatagrams( midTime, 2, sampleRate, offset );
        Assert.assertEquals( 2, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        Assert.assertTrue( D[1].equals( origDatagrams[testIdx+1] ) );
        Assert.assertEquals( ((afterTime - onTime) / 2), offset[0] );
        
        
        /* Testing time-spanned access with alternate sample rate */
        System.out.println( "Testing getDatagrams with alternate sample rate ..." );
        span = origDatagrams[testIdx].getDuration();
        tlr.rewind();
        D = tlr.getDatagrams( onTime*2, span*2, sampleRate/2 );
        Assert.assertEquals( 1, D.length );
        Assert.assertTrue( D[0].equals( origDatagrams[testIdx] ) );
        
        
        /* Delete the test file */
        File fid = new File( tlFileName );
        fid.delete();
        
        /* ----------- */
        System.out.println("End of the timeline symmetry test.");
   }
    
    /**
     * Compare two arrays of long.
     * 
     * @param a1 an array of longs
     * @param a2 an array of longs
     * @return true if they are equal, false if not.
     */
    private static boolean areEqual( long[] a1, long[] a2 ) {
        /* Check number of longs */
        if ( a1.length != a2.length ) return false;
        /* Check contents */
        for ( int i = 0; i < a1.length; i++ ) {
            if ( a1[i] != a2[i] ) return false;
        }
        /* If all passed, then the arrays are equal */
        return( true );
    }
    
    /**
     * Compare two arrays of bytes.
     * 
     * @param a1 an array of bytes
     * @param a2 an array of bytes
     * @return true if they are equal, false if not.
     */
    private static boolean areEqual( byte[] a1, byte[] a2 ) {
        /* Check number of longs */
        if ( a1.length != a2.length ) return false;
        /* Check contents */
        for ( int i = 0; i < a1.length; i++ ) {
            if ( a1[i] != a2[i] ) return false;
        }
        /* If all passed, then the arrays are equal */
        return( true );
    }
    
}
