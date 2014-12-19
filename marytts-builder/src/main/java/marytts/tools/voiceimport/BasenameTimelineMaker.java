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

import java.io.File;
import java.text.DecimalFormat;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.data.Datagram;
import marytts.util.data.ESTTrackReader;

/**
 * The BasenameTimelineMaker class takes a database root directory and a list of basenames, and associates the basenames with
 * absolute times in a timeline in Mary format.
 * 
 * @author sacha
 */
public class BasenameTimelineMaker extends VoiceImportComponent {

	protected DatabaseLayout db = null;
	protected int percent = 0;

	public final String TIMELINEFILE = "BasenameTimelineMaker.timelineFile";

	public final String PMDIR = "db.pmDir";
	public final String PMEXT = "db.pmExtension";

	public String getName() {
		return "BasenameTimelineMaker";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap();
			props.put(TIMELINEFILE, db.getProp(db.FILEDIR) + "timeline_basenames" + db.getProp(db.MARYEXT));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(TIMELINEFILE, "directory containing the pitchmarks");
	}

	/**
	 * Reads and concatenates a list of LPC EST tracks into one single timeline file.
	 * 
	 */
	public boolean compute() {
		System.out.println("---- Making a timeline for the base names\n\n");
		System.out.println("Base directory: " + db.getProp(db.ROOTDIR) + "\n");

		/* Export the basename list into an array of strings */
		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Processing [" + baseNameArray.length + "] utterances.\n");

		/* Prepare the output directory for the timelines if it does not exist */
		File timelineDir = new File(db.getProp(db.FILEDIR));
		if (!timelineDir.exists()) {
			timelineDir.mkdir();
			System.out.println("Created output directory [" + db.getProp(db.FILEDIR) + "] to store the timelines.");
		}

		try {
			/*
			 * 1) Determine the reference sampling rate as being the sample rate of the first encountered wav file
			 */
			WavReader wav = new WavReader(db.getProp(db.WAVDIR) + baseNameArray[0] + db.getProp(db.WAVEXT));
			int globSampleRate = wav.getSampleRate();
			System.out.println("---- Detected a global sample rate of: [" + globSampleRate + "] Hz.");

			System.out.println("---- Folding the basenames according to the pitchmarks tics...");

			/* 2) Open the destination timeline file */

			/* Make the file name */
			String bnTimelineName = getProp(TIMELINEFILE);
			System.out.println("Will create the basename timeline in file [" + bnTimelineName + "].");

			/* Processing header: */
			String processingHeader = "\n";

			/* Instantiate the TimelineWriter: */
			TimelineWriter bnTimeline = new TimelineWriter(bnTimelineName, processingHeader, globSampleRate, 2.0);

			/* 3) Write the datagrams and feed the index */

			float totalDuration = 0.0f; // Accumulator for the total timeline duration
			long totalTime = 0l;
			int numDatagrams = 0;

			/* For each EST pitchmarks track file: */
			ESTTrackReader pmFile = null;
			int duration = 0;
			for (int i = 0; i < baseNameArray.length; i++) {
				percent = 100 * i / baseNameArray.length;
				/* - open+load */
				pmFile = new ESTTrackReader(db.getProp(PMDIR) + baseNameArray[i] + db.getProp(PMEXT));
				wav = new WavReader(db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT));
				totalDuration += pmFile.getTimeSpan();
				duration = (int) ((double) pmFile.getTimeSpan() * (double) (globSampleRate));
				// System.out.println( baseNameArray[i] + " -> [" + duration + "] samples." );
				System.out.println(baseNameArray[i] + " -> pm file says [" + duration + "] samples, wav file says ["
						+ wav.getNumSamples() + "] samples.");
				bnTimeline.feed(new Datagram(duration, baseNameArray[i].getBytes("UTF-8")), globSampleRate);
				totalTime += duration;
				numDatagrams++;
			}
			bnTimeline.close();

			System.out.println("---- Done.");

			/* 7) Print some stats and close the file */
			System.out.println("---- Basename timeline result:");
			System.out.println("Number of files scanned: " + baseNameArray.length);
			System.out.println("Total speech duration: [" + totalTime + "] samples / ["
					+ ((float) (totalTime) / (float) (globSampleRate)) + "] seconds.");
			System.out.println("(Speech duration approximated from EST Track float times: [" + totalDuration + "] seconds.)");
			System.out.println("Number of frames: [" + numDatagrams + "].");
			System.out.println("Size of the index: [" + bnTimeline.getIndex().getNumIdx() + "] ("
					+ (bnTimeline.getIndex().getNumIdx() * 16) + " bytes, i.e. "
					+ new DecimalFormat("#.##").format((double) (bnTimeline.getIndex().getNumIdx()) * 16.0 / 1048576.0)
					+ " megs).");
			System.out.println("---- Basename timeline done.");

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
