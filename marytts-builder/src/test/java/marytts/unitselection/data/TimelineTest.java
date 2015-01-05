/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.unitselection.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import marytts.exceptions.MaryConfigurationException;
import marytts.tools.voiceimport.TimelineWriter;
import marytts.util.Pair;
import marytts.util.data.Datagram;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Provides the actual timeline test case for the timeline reading/writing symmetry.
 */
public class TimelineTest {
	private static TestableTimelineReader tlr;
	private static String hdrContents;
	private static int NUMDATAGRAMS;
	private static int MAXDATAGRAMBYTESIZE;
	private static int MAXDATAGRAMDURATION;
	private static int sampleRate;
	private static Datagram[] origDatagrams;
	private static final String tlFileName = "timelineTest.bin";

	@BeforeClass
	public static void setUp() throws Exception {
		Random rand = new Random(); // New random number generator

		NUMDATAGRAMS = rand.nextInt(87) + 6; // Number of datagrams to test with, between 6 and 100.
		System.out.println("Testing with [" + NUMDATAGRAMS + "] random datagrams.");
		MAXDATAGRAMBYTESIZE = 64; // Maximum datagram length in bytes
		MAXDATAGRAMDURATION = 20; // Maximum datagram duration (in samples)
		hdrContents = "Blah This is the procHeader Blah";
		sampleRate = 1000;

		origDatagrams = new Datagram[NUMDATAGRAMS]; // An array of datagrams with random length
		int len = 0;
		long dur = 0l;

		/* Fill the array of random datagrams */
		long lenCumul = 74;
		long durCumul = 0l;
		for (int i = 0; i < NUMDATAGRAMS; i++) {
			/* Make the first datagram very long, for special tests */
			if (i == 0 || i == 2) {
				len = 1234567;
			} else {
				/* Make a random length */
				len = rand.nextInt(MAXDATAGRAMBYTESIZE) + 1;
			}
			lenCumul += (len + 12);
			/* Allocate the corresponding byte array */
			byte[] buff = new byte[len];
			/* Fill all the bytes with the datagram index */
			for (int l = 0; l < len; l++) {
				buff[l] = (byte) i;
			}
			/* Make a random datagram duration */
			dur = (long) (rand.nextInt(MAXDATAGRAMDURATION) + 2);
			durCumul += dur;
			/* Store the datagram */
			origDatagrams[i] = new Datagram(dur, buff);
			/* Check */
			System.out.println("[ " + len + " , " + dur + " ]\t( " + lenCumul + " , " + durCumul + " )");
		}

		/* Write the datagram array in a timeline */
		System.out.println("Opening new timeline file...");
		TimelineWriter tlw = new TimelineWriter(tlFileName, hdrContents, sampleRate, 0.1d);
		System.out.println("Feeding...");
		tlw.feed(origDatagrams, sampleRate);
		System.out.println("Closing...");
		tlw.close();
		System.out.println("Done.");

		System.out.println("Datagram zone pos. = " + tlw.getDatagramsBytePos());
		System.out.println("WRITTEN INDEX:");
		tlw.getIndex().print();
		/* Testing the readonly file opening */
		System.out.println("Testing the TimelineReader construction...");
		/* Re-read the datagrams */
		tlr = new TestableTimelineReader(tlFileName, true);
	}

	@Test
	public void procHeader() throws IOException {
		/* Check the procHeader */
		Assert.assertEquals("The procHeader is out of sync.", tlr.getProcHeaderContents(), hdrContents);
	}

	@Test
	public void numDatagrams() throws IOException {
		/* Check the number of datagrams */
		Assert.assertEquals("numDatagrams is out of sync.", tlr.getNumDatagrams(), NUMDATAGRAMS);
	}

	@Test
	public void testSkip() throws IOException {
		System.out.println("READ INDEX:");
		tlr.getIndex().print();
		/* Testing skip */
		System.out.println("Testing skip...");
		long timeNow = 0;
		long timeBefore = 0;
		Pair<ByteBuffer, Long> p = tlr.getByteBufferAtTime(timeNow);
		ByteBuffer bb = p.getFirst();
		int byteNow = bb.position();
		int byteBefore = 0;
		for (int i = 0; i < NUMDATAGRAMS; i++) {
			timeBefore = timeNow;
			byteBefore = byteNow;
			try {
				long skippedDuration = tlr.skipNextDatagram(bb);
				timeNow += skippedDuration;
			} catch (BufferUnderflowException e) {
				// reached end of byte buffer
				break;
			}
			byteNow = bb.position();
			Assert.assertEquals("Skipping fails on datagram [" + i + "].", (long) (origDatagrams[i].getLength()) + 12l,
					(byteNow - byteBefore));
			Assert.assertEquals("Time is out of sync after skipping datagram [" + i + "].", origDatagrams[i].getDuration(),
					(timeNow - timeBefore));
		}

		/* Testing the EOF trap for skip */
		try {
			tlr.skipNextDatagram(bb);
			Assert.fail("should have thrown BufferUnderflowException to indicate end of byte buffer");
		} catch (BufferUnderflowException e) {
			// OK, expected
		}

	}

	@Test
	public void testGet() throws IOException {

		/* Testing get */
		System.out.println("Testing get...");
		Datagram[] readDatagrams = new Datagram[NUMDATAGRAMS];
		Pair<ByteBuffer, Long> p = tlr.getByteBufferAtTime(0);
		ByteBuffer bb = p.getFirst();
		for (int i = 0; i < NUMDATAGRAMS; i++) {
			readDatagrams[i] = tlr.getNextDatagram(bb);
			if (readDatagrams[i] == null) {
				System.err.println("Could read " + i + " datagrams");
				break;
			}
			Assert.assertTrue("Datagram [" + i + "] is out of sync.",
					areEqual(origDatagrams[i].getData(), readDatagrams[i].getData()));
			Assert.assertEquals("Time for datagram [" + i + "] is out of sync.", origDatagrams[i].getDuration(),
					readDatagrams[i].getDuration());
		}
		/* Testing the EOF trap for get */
		Assert.assertEquals(null, tlr.getNextDatagram(bb));
	}

	@Test
	public void gotoTime1() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		// exercise
		ByteBuffer bb = tlr.getByteBufferAtTime(onTime).getFirst();
		Datagram d = tlr.getNextDatagram(bb);
		// verify
		assertEquals(origDatagrams[testIdx], d);
	}

	@Test
	public void gotoTime2() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		long afterTime = onTime + origDatagrams[testIdx].getDuration();
		long midTime = onTime + ((afterTime - onTime) / 2);
		// exercise
		ByteBuffer bb = tlr.getByteBufferAtTime(midTime).getFirst();
		Datagram d = tlr.getNextDatagram(bb);
		// verify
		assertEquals(origDatagrams[testIdx], d);
	}

	@Test
	public void gotoTime3() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		long afterTime = onTime + origDatagrams[testIdx].getDuration();
		// exercise
		ByteBuffer bb = tlr.getByteBufferAtTime(afterTime).getFirst();
		Datagram d = tlr.getNextDatagram(bb);
		// verify
		assertEquals(origDatagrams[testIdx + 1], d);
	}

	@Test
	public void getDatagrams1() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration();
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(onTime, span, sampleRate, offset);
		// verify
		assertEquals(1, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(0l, offset[0]);
	}

	@Test
	public void getDatagrams2() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration() / 2;
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(onTime, span, sampleRate, offset);
		// verify
		assertEquals(1, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(0l, offset[0]);
	}

	@Test
	public void getDatagrams3() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		long afterTime = onTime + origDatagrams[testIdx].getDuration();
		long midTime = onTime + ((afterTime - onTime) / 2);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration() / 2;
		// exercise
		D = tlr.getDatagrams(midTime, span, sampleRate);
		// verify
		assertEquals(1, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
	}

	@Test
	public void getDatagrams4() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration() + 1;
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(onTime, span, sampleRate, offset);
		// verify
		assertEquals(2, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(origDatagrams[testIdx + 1], D[1]);
		assertEquals(0l, offset[0]);
	}

	@Test
	public void getDatagrams5() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration() + origDatagrams[testIdx + 1].getDuration();
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(onTime, span, sampleRate, offset);
		// verify
		assertEquals(2, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(origDatagrams[testIdx + 1], D[1]);
		assertEquals(0l, offset[0]);
	}

	@Test
	public void getDatagrams6() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long span = origDatagrams[testIdx].getDuration() + origDatagrams[testIdx + 1].getDuration();
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(onTime + 1, span, sampleRate, offset);
		// verify
		assertEquals("textIdx=" + testIdx + ", span=" + span + ", dur[" + testIdx + "]=" + origDatagrams[testIdx].getDuration()
				+ ", dur[" + (testIdx + 1) + "]=" + origDatagrams[testIdx + 1].getDuration() + ", offset=" + offset[0], 3,
				D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(origDatagrams[testIdx + 1], D[1]);
		assertEquals(origDatagrams[testIdx + 2], D[2]);
		assertEquals(1l, offset[0]);
	}

	@Test
	public void getDatagrams7() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		long afterTime = onTime + origDatagrams[testIdx].getDuration();
		long midTime = onTime + ((afterTime - onTime) / 2);
		Datagram[] D = null;
		long dur = origDatagrams[testIdx].getDuration();
		long span = dur - dur / 2 + 1;
		long[] offset = new long[1];
		// exercise
		D = tlr.getDatagrams(midTime, span, sampleRate, offset);
		// verify
		assertEquals(2, D.length);
		assertEquals(origDatagrams[testIdx], D[0]);
		assertEquals(origDatagrams[testIdx + 1], D[1]);
		assertEquals(dur / 2, offset[0]);
	}

	@Test
	public void otherSampleRate() throws IOException {
		// setup
		final int testIdx = NUMDATAGRAMS / 2;
		long onTime = getTimeOfIndex(testIdx);
		Datagram[] D = null;
		long dur = origDatagrams[testIdx].getDuration();
		long span = dur;
		// exercise
		D = tlr.getDatagrams(onTime * 2, span * 2, sampleRate / 2);
		// verify
		assertEquals(1, D.length);
		Assert.assertTrue(areEqual(D[0].getData(), origDatagrams[testIdx].getData()));
		Assert.assertTrue(D[0].getDuration() != origDatagrams[testIdx].getDuration());

	}

	/**
	 * @param testIdx
	 * @return
	 */
	private long getTimeOfIndex(final int testIdx) {
		long onTime = 0l;
		for (int i = 0; i < testIdx; i++) {
			onTime += origDatagrams[i].getDuration();
		}
		return onTime;
	}

	@Test
	public void getLastDatagram() throws MaryConfigurationException, IOException {
		long totalDur = tlr.getTotalDuration();
		Assert.assertTrue(totalDur > 0);

		Datagram d = tlr.getDatagram(totalDur - 1);
		Assert.assertTrue(d != null);

		long dur = d.getDuration();

		d = tlr.getDatagram(totalDur - dur);
		Assert.assertTrue(d != null);
	}

	@Test
	public void getLastDatagrams() throws MaryConfigurationException, IOException {
		long totalDur = tlr.getTotalDuration();
		Assert.assertTrue(totalDur > 0);

		Datagram[] ds = tlr.getDatagrams(totalDur - 1, 1);
		Assert.assertTrue(ds != null);
		Assert.assertTrue(ds.length == 1);

		ds = tlr.getDatagrams(totalDur - 1, 2);
		Assert.assertTrue(ds != null);
		Assert.assertTrue(ds.length == 1);
	}

	@Test
	public void cannotGetAfterLastDatagram() throws MaryConfigurationException, IOException {
		long totalDur = tlr.getTotalDuration();
		Assert.assertTrue(totalDur > 0);

		try {
			Datagram d = tlr.getDatagram(totalDur);
			Assert.fail("Should have thrown a BufferUnderflowException");
		} catch (BufferUnderflowException e) {
			// OK, expected
		}
	}

	@Test
	public void cannotGetAfterLastDatagrams() throws MaryConfigurationException, IOException {
		long totalDur = tlr.getTotalDuration();
		Assert.assertTrue(totalDur > 0);

		try {
			Datagram[] ds = tlr.getDatagrams(totalDur, 1);
			Assert.fail("Should have thrown a BufferUnderflowException");
		} catch (BufferUnderflowException e) {
			// OK, expected
		}
	}

	@Test
	public void canReadLongDatagram() throws MaryConfigurationException, IOException {
		// setup custom fixture for this method
		TimelineReader timeline = new TimelineReader(tlFileName, false); // do not try memory mapping
		// exercise
		Datagram d = timeline.getDatagram(0);
		// verify
		Assert.assertEquals(origDatagrams[0].getLength(), d.getLength());
	}

	@Test
	public void canReadLongDatagrams1() throws MaryConfigurationException, IOException {
		// setup custom fixture for this method
		TimelineReader timeline = new TimelineReader(tlFileName, false); // do not try memory mapping
		// exercise
		Datagram[] ds = timeline.getDatagrams(0, origDatagrams[0].getDuration() + 1);
		// verify
		Assert.assertEquals(2, ds.length);
		Assert.assertEquals(origDatagrams[0].getLength(), ds[0].getLength());
	}

	@Test
	public void canReadLongDatagrams2() throws MaryConfigurationException, IOException {
		// setup custom fixture for this method
		TimelineReader timeline = new TimelineReader(tlFileName, false); // do not try memory mapping
		// exercise
		Datagram[] ds = timeline.getDatagrams(origDatagrams[0].getDuration(), origDatagrams[1].getDuration() + 1);
		// verify
		Assert.assertEquals(2, ds.length);
		Assert.assertEquals(origDatagrams[1].getLength(), ds[0].getLength());
	}

	@AfterClass
	public static void tearDown() throws IOException {
		/* Delete the test file */
		File fid = new File(tlFileName);
		fid.delete();

		/* ----------- */
		System.out.println("End of the timeline symmetry test.");
	}

	/**
	 * Compare two arrays of long.
	 * 
	 * @param a1
	 *            an array of longs
	 * @param a2
	 *            an array of longs
	 * @return true if they are equal, false if not.
	 */
	private static boolean areEqual(long[] a1, long[] a2) {
		/* Check number of longs */
		if (a1.length != a2.length)
			return false;
		/* Check contents */
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i])
				return false;
		}
		/* If all passed, then the arrays are equal */
		return (true);
	}

	/**
	 * Compare two arrays of bytes.
	 * 
	 * @param a1
	 *            an array of bytes
	 * @param a2
	 *            an array of bytes
	 * @return true if they are equal, false if not.
	 */
	private static boolean areEqual(byte[] a1, byte[] a2) {
		/* Check number of longs */
		if (a1.length != a2.length)
			return false;
		/* Check contents */
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i])
				return false;
		}
		/* If all passed, then the arrays are equal */
		return (true);
	}

}
