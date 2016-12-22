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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.util.data.ESTTrackReader;

public class MCEPMaker extends VoiceImportComponent {

	protected DatabaseLayout db = null;

	protected String mcepExt = ".mcep";
	protected String lpcExt = ".lpc";

	private final String name = "MCEPMaker";
	public final String LPCDIR = name + ".lpcDir";
	public final String MCEPDIR = name + ".mcepDir";

	public String getName() {
		return name;
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			String rootDir = db.getProp(DatabaseLayout.ROOTDIR);

			/**
			 * Uncomment if you want to use this class for making lpcs props.put(LPCDIR, rootDir +"lpc"
			 * +System.getProperty("file.separator"));
			 **/
			props.put(MCEPDIR, rootDir + "mcep" + System.getProperty("file.separator"));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(MCEPDIR, "directory containing the mcep files");
	}

	/**
	 * Shift the pitchmarks to the closest peak.
	 * 
	 * @param pmIn
	 *            pmIn
	 * @param w
	 *            w
	 * @param sampleRate
	 *            sampleRate
	 */
	private Float[] shiftToClosestPeak(Float[] pmIn, short[] w, int sampleRate) {

		final int HORIZON = 32; // <= number of samples to seek before and after the pitchmark
		Float[] pmOut = new Float[pmIn.length];

		/* Browse the pitchmarks */
		int pm = 0;
		int pmwmax = w.length - 1;
		int TO = 0;
		int max = 0;
		for (int pi = 0; pi < pmIn.length; pi++) {
			pm = (int) (pmIn[pi].floatValue() * sampleRate);
			// If the pitchmark goes out of the waveform (this sometimes
			// happens with the last one due to rounding errors), just clip it.
			if (pm > pmwmax) {
				// If this was not the last pitchmark, there is a problem
				if (pi < (pmIn.length - 1)) {
					throw new RuntimeException("Some pitchmarks are located above the location of the last waveform sample !");
				}
				// Else, if it was the last pitchmark, clip it:
				pmOut[pi] = new Float((double) (pmwmax) / (double) (sampleRate));
			}
			// Else, if the pitchmark is in the waveform:
			else {
				/* Seek the max of the wav samples around the pitchmark */
				max = pm;
				// - Back:
				TO = (pm - HORIZON) < 0 ? 0 : (pm - HORIZON);
				for (int i = pm - 1; i >= TO; i--) {
					if (w[i] > w[max])
						max = i;
				}
				// - Forth:
				TO = (pm + HORIZON + 1) > w.length ? w.length : (pm + HORIZON + 1);
				for (int i = pm + 1; i < TO; i++) {
					if (w[i] >= w[max])
						max = i;
				}
				/* Translate the pitchmark */
				pmOut[pi] = new Float((double) (max) / (double) (sampleRate));
			}
		}

		return (pmOut);
	}

	/**
	 * Shift the pitchmarks to the previous zero crossing.
	 * 
	 * @param pmIn
	 *            pmIn
	 * @param w
	 *            w
	 * @param sampleRate
	 *            sampleRate
	 */
	private Float[] shiftToPreviousZero(Float[] pmIn, short[] w, int sampleRate) {

		final int HORIZON = 32; // <= number of samples to seek before the pitchmark
		Float[] pmOut = new Float[pmIn.length];

		/* Browse the pitchmarks */
		int pm = 0;
		int TO = 0;
		int zero = 0;
		for (int pi = 0; pi < pmIn.length; pi++) {
			pm = (int) (pmIn[pi].floatValue() * sampleRate);
			/* If the initial pitchmark is on a zero, don't shift the pitchmark. */
			if (w[pm] == 0) {
				pmOut[pi] = pmIn[pi];
				continue;
			}
			/* Else: */
			/* Seek the zero crossing preceding the pitchmark */
			TO = (pm - HORIZON) < 0 ? 0 : (pm - HORIZON);
			for (zero = (pm - 1); (zero > TO) && ((w[zero] * w[zero + 1]) > 0); zero--)
				;
			/* If no zero crossing was found, don't move the pitchmark */
			if ((zero == TO) && ((w[zero] * w[zero + 1]) > 0)) {
				pmOut[pi] = pmIn[pi];
			}
			/* If a zero crossing was found, translate the pitchmark */
			else {
				pmOut[pi] = new Float((double) ((-w[zero]) < w[zero + 1] ? zero : (zero + 1)) / (double) (sampleRate));
			}
		}

		return (pmOut);
	}

	/**
	 * Rectification of the pitchmarks.
	 * 
	 * @param baseNameArray
	 *            baseNameArray
	 * @throws IOException
	 *             IOException
	 */
	private void tweakThePitchmarks(String[] baseNameArray) throws IOException {

		System.out.println("---- Correcting the pitchmarks...");

		/* Ensure the existence of the target directory */
		File dir = new File(db.getProp(DatabaseLayout.PMDIR));
		if (!dir.exists()) {
			System.out.println("Creating the directory [" + db.getProp(DatabaseLayout.PMDIR) + "].");
			dir.mkdir();
		}

		// For each file
		for (int f = 0; f < baseNameArray.length; f++) {
			// for ( int f = 0; f < 1; f++ ) {
			/* Load the pitchmark file */
			// System.out.println( baseNameArray[f] );
			String fName = db.getProp(DatabaseLayout.PMDIR) + baseNameArray[f] + db.getProp(DatabaseLayout.PMEXT);
			ESTTrackReader pmFileIn = new ESTTrackReader(fName);
			/* Wrap the primitive floats so that we can use vectors thereafter */
			float[] pmInPrimitive = pmFileIn.getTimes();
			Float[] pmIn = new Float[pmInPrimitive.length];
			for (int i = 0; i < pmInPrimitive.length; i++) {
				pmIn[i] = (Float) Array.get(pmInPrimitive, i);
			}
			/* Load the wav file */
			fName = db.getProp(DatabaseLayout.WAVDIR) + baseNameArray[f] + db.getProp(DatabaseLayout.WAVEXT);
			WavReader wf = new WavReader(fName);
			short[] w = wf.getSamples();
			Float[] pmOut = null;
			try {
				/* Shift to the closest peak */
				pmOut = shiftToClosestPeak(pmIn, w, wf.getSampleRate());
				/* Shift to the zero immediately preceding the closest peak */
				pmOut = shiftToPreviousZero(pmOut, w, wf.getSampleRate());
			} catch (RuntimeException e) {
				throw new RuntimeException("For utterance [" + baseNameArray[f] + "]:", e);
			}
			/* Export the corrected pitchmarks as an EST file */
			fName = db.getProp(DatabaseLayout.PMDIR) + baseNameArray[f] + db.getProp(DatabaseLayout.PMEXT);
			DataOutputStream dos = null;
			try {
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fName)));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Can't open file [" + fName + "] for writing.", e);
			}
			dos.writeBytes("EST_File Track\n" + "DataType ascii\n" + "NumFrames " + Integer.toString(pmOut.length) + "\n"
					+ "NumChannels 0\n" + "NumAuxChannels 0\n" + "EqualSpace 0\n" + "BreaksPresent true\n" + "EST_Header_End\n");
			for (int i = 0; i < pmOut.length; i++) {
				dos.writeBytes(pmOut[i].toString());
				dos.writeBytes("\t1\n");
			}
			dos.flush();
			dos.close();
		}

	}

	/**
	 * The standard compute() method of the VoiceImportComponent interface.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	public boolean compute() throws IOException {

		// do not attempt to launch ESTCaller if bnl is empty
		if (bnl.getLength() < 1) {
			logger.error("BaseNameList is empty, cannot proceed!");
			return false;
		}

		String[] baseNameArray = bnl.getListAsArray();
		System.out.println("Computing Mel cepstra for [" + baseNameArray.length + "] utterances.");
		ESTCaller caller = new ESTCaller(db);
		caller.make_mcep(baseNameArray, db.getProp(DatabaseLayout.PMDIR), db.getProp(DatabaseLayout.PMEXT), getProp(MCEPDIR),
				mcepExt);

		return (true);
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return -1;
	}

}
