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
package marytts.tools.voiceimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.unitselection.data.LPCDatagram;
import marytts.util.data.ESTTrackReader;
import marytts.util.io.General;

/**
 * The LPCTimelineMaker class takes a database root directory and a list of basenames, and converts the related wav files into a
 * LPC timeline in Mary format.
 * 
 * @author sacha
 */
public class LPCTimelineMaker extends VoiceImportComponent {

	protected DatabaseLayout db = null;
	protected int percent = 0;
	protected String lpcExt = ".lpc";

	public final String LPCDIR = "LPCTimelineMaker.lpcDir";
	public final String LPCTIMELINE = "LPCTimelineMaker.lpcTimeline";

	public String getName() {
		return "LPCTimelineMaker";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(LPCDIR, db.getProp(db.ROOTDIR) + "lpc" + System.getProperty("file.separator"));
			props.put(LPCTIMELINE, db.getProp(db.FILEDIR) + "timeline_lpc" + db.getProp(db.MARYEXT));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(LPCDIR, "directory containing the lpc files");
		props2Help.put(LPCTIMELINE, "file containing the lpc files. Will be created by this module");
	}

	/**
	 * Reads and concatenates a list of LPC EST tracks into one single timeline file.
	 * 
	 */
	public boolean compute() {
		System.out.println("---- Importing LPC coefficients\n\n");

		/* Export the basename list into an array of strings */
		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Processing [" + baseNameArray.length + "] utterances.\n");

		/* Prepare the output directory for the timelines if it does not exist */
		File timelineDir = new File(db.getProp(db.FILEDIR));

		try {
			/*
			 * 1) Determine the reference sampling rate as being the sample rate of the first encountered wav file
			 */
			WavReader wav = new WavReader(db.getProp(db.WAVDIR) + baseNameArray[0] + db.getProp(db.WAVEXT));
			int globSampleRate = wav.getSampleRate();
			System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz.");

			/* 2) Scan all the EST LPC Track files for LPC min, LPC max and total LPC-timeline duration */

			System.out.println("---- Scanning for LPC min and LPC max...");

			ESTTrackReader lpcFile; // Structure that holds the LPC track data
			float[] current; // local [min,max] vector for the current LPC track file
			float lpcMin, lpcMax, lpcRange; // Global min/max/range values for the LPC coefficients
			float totalDuration = 0.0f; // Accumulator for the total timeline duration
			long numDatagrams = 0l; // Total number of LPC datagrams in the timeline file
			int numLPC = 0; // Number of LPC channels, assumed from the first LPC file

			/* Initialize with the first file: */
			/* - open and load */
			// System.out.println( baseNameArray[0] );
			lpcFile = new ESTTrackReader(getProp(LPCDIR) + baseNameArray[0] + lpcExt);
			/* - get the min and the max */
			current = lpcFile.getMinMaxNo1st();
			lpcMin = current[0];
			lpcMax = current[1];
			/* - accumulate the file duration */
			totalDuration += lpcFile.getTimeSpan();
			/* - accumulate the number of datagrams: */
			numDatagrams += lpcFile.getNumFrames();
			/* - get the number of LPC channels: */
			numLPC = lpcFile.getNumChannels() - 1; // -1 => ignore the energy channel.
			System.out.println("Assuming that the number of LPC coefficients is: [" + numLPC + "] coefficients.");

			/* Then, browse the remaining files: */
			for (int i = 1; i < baseNameArray.length; i++) {
				percent = 100 * i / baseNameArray.length;
				/* - open+load */
				// System.out.println( baseNameArray[i] );
				lpcFile = new ESTTrackReader(getProp(LPCDIR) + baseNameArray[i] + lpcExt);
				/* - get min and max */
				current = lpcFile.getMinMaxNo1st();
				if (current[0] < lpcMin) {
					lpcMin = current[0];
				}
				if (current[1] > lpcMax) {
					lpcMax = current[1];
				}
				/* - accumulate and approximate of the total speech duration (to build the index) */
				totalDuration += lpcFile.getTimeSpan();
				/* - accumulate the number of datagrams: */
				numDatagrams += lpcFile.getNumFrames();
			}
			lpcRange = lpcMax - lpcMin;
			/*
			 * NOTE: accumulating the total LPC timeline duration (which is necessary for dimensioning the index) from the LPC
			 * track times is slightly more imprecise than accumulating durations from the residuals, but it avoids another loop
			 * through on-disk files.
			 */

			System.out.println("LPCMin   = " + lpcMin);
			System.out.println("LPCMax   = " + lpcMax);
			System.out.println("LPCRange = " + lpcRange);

			System.out.println("---- Done.");

			System.out.println("---- Filtering the EST LPC tracks...");

			/* 3) Open the destination timeline file */

			/* Make the file name */
			String lpcTimelineName = getProp(LPCTIMELINE);
			System.out.println("Will create the LPC timeline in file [" + lpcTimelineName + "].");

			/* Processing header: */
			Properties props = new Properties();
			String hdrCmdLine = "$ESTDIR/bin/sig2fv "
					+ "-window_type hamming -factor 3 -otype est_binary -preemph 0.95 -coefs lpc -lpc_order 16 "
					+ "-pm PITCHMARKFILE.pm -o LPCDIR/LPCFILE.lpc WAVDIR/WAVFILE.wav\n";
			props.setProperty("command", hdrCmdLine);
			props.setProperty("lpc.order", String.valueOf(numLPC));
			props.setProperty("lpc.min", String.valueOf(lpcMin));
			props.setProperty("lpc.range", String.valueOf(lpcRange));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			props.store(baos, null);
			String processingHeader = baos.toString("latin1");

			/* Instantiate the TimelineWriter: */
			TimelineWriter lpcTimeline = new TimelineWriter(lpcTimelineName, processingHeader, globSampleRate, 0.1);

			/* 4) Write the datagrams and feed the index */

			long totalTime = 0l;

			/* For each EST track file: */
			for (int i = 0; i < baseNameArray.length; i++) {
				/* - open+load */
				System.out.println(baseNameArray[i]);
				lpcFile = new ESTTrackReader(getProp(LPCDIR) + baseNameArray[i] + lpcExt);
				wav = new WavReader(db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT));
				short[] wave = wav.getSamples();
				/* - Reset the frame locations in the local file */
				int frameStart = 0;
				int frameEnd = 0;
				int duration = 0;
				long localTime = 0l;
				/* - For each frame in the LPC file: */
				for (int f = 0; f < lpcFile.getNumFrames(); f++) {

					/* Locate the corresponding segment in the wave file */
					frameStart = frameEnd;
					frameEnd = (int) ((double) lpcFile.getTime(f) * (double) (globSampleRate));
					duration = frameEnd - frameStart;

					/* Quantize the LPC coeffs: */
					short[] quantizedFrame = General.quantize(lpcFile.getFrame(f), lpcMin, lpcRange);
					float[] unQuantizedFrame = General.unQuantize(quantizedFrame, lpcMin, lpcRange);
					/*
					 * Note: for inverse filtering (below), we will use the un-quantized values of the LPC coefficients, so that
					 * the quantization noise is registered into the residual (for better reconstruction of the waveform from
					 * quantized coeffs). Warning: in the EST format, the first LPC coefficient is the filter gain, which should
					 * not be used for the inverse filtering.
					 */

					/* PERFORM THE INVERSE FILTERING with the quantized LPCs, and write the residual to the datagram: */
					double r;
					int numRes = duration - numLPC;
					byte[] residual = new byte[numRes];
					for (int k = 0; k < numRes; k++) {
						// try {
						r = (double) (wave[frameStart + k]);
						/*
						 * } catch ( ArrayIndexOutOfBoundsException e ) { System.out.println( "ARGH: " + (frameStart + numLPC + k)
						 * ); System.out.println( "Wlen: " + wave.length ); System.out.println( "FrameEnd: " + frameEnd );
						 * System.out.println( "FrameSize: " + frameSize ); return; }
						 */
						for (int j = 0; j < numLPC; j++) {
							// try {
							r -= unQuantizedFrame[j] * ((double) wave[frameStart + (numLPC - 1) + (k - j)]);
							/*
							 * } catch ( ArrayIndexOutOfBoundsException e ) { System.out.println( "ARGH: " + (frameStart + numLPC
							 * + k) ); System.out.println( "Wlen: " + wave.length ); return; }
							 */
						}
						residual[k] = General.shortToUlaw((short) r);
					}

					/* Feed the datagram to the timeline */
					lpcTimeline.feed(new LPCDatagram(duration, quantizedFrame, residual), globSampleRate);
					totalTime += duration;
					localTime += duration;
				}
				// System.out.println( baseNameArray[i] + " -> lpc file says [" + localTime + "] samples, wav file says ["+
				// wav.getNumSamples() + "] samples." );
			}
			lpcTimeline.close();

			System.out.println("---- Done.");

			/* 7) Print some stats and close the file */
			System.out.println("---- LPC timeline result:");
			System.out.println("Number of files scanned: " + baseNameArray.length);
			System.out.println("Total speech duration: [" + totalTime + "] samples / ["
					+ ((float) (totalTime) / (float) (globSampleRate)) + "] seconds.");
			System.out.println("(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)");
			System.out.println("Number of frames: [" + numDatagrams + "].");
			System.out.println("Size of the index: [" + lpcTimeline.getIndex().getNumIdx() + "] ("
					+ (lpcTimeline.getIndex().getNumIdx() * 16) + " bytes, i.e. "
					+ new DecimalFormat("#.##").format((double) (lpcTimeline.getIndex().getNumIdx()) * 16.0 / 1048576.0)
					+ " megs).");
			System.out.println("---- LPC timeline done.");

		} catch (SecurityException e) {
			System.err.println("Error: you don't have write access to the target database directory.");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
		}

		return (true);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

}
