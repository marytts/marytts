/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.tools.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.unitselection.data.TimelineReader;
import marytts.util.data.Datagram;

public abstract class AbstractTimelineMaker extends VoiceImportComponent {

	protected final String DATADIR = getName() + ".dataDir";

	protected final String DATATIMELINE = getName() + ".dataTimeline";

	protected final String BASENAMETIMELINE = getName() + ".basenameTimeline";

	protected final String DATAEXT = getName() + ".dataExt";

	protected int percent;

	protected DatabaseLayout db;

	protected TimelineWriter dataTimeline;

	protected TimelineReader basenameTimeline;

	/**
	 * get the type of this {@link VoiceImportComponent}
	 * 
	 * @return the type
	 */
	public abstract String getType();

	/**
	 * {@inheritDoc}
	 * 
	 * @param theDB
	 *            theDB
	 */
	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout theDB) {
		db = theDB;
		if (props == null) {
			props = new TreeMap<String, String>();
			props.put(DATADIR, db.getProp(db.ROOTDIR) + getType() + System.getProperty("file.separator"));
			props.put(DATAEXT, "." + getType());
			props.put(DATATIMELINE, db.getProp(db.FILEDIR) + "timeline_" + getType() + db.getProp(db.MARYEXT));
			props.put(BASENAMETIMELINE, db.getProp(db.FILEDIR) + "timeline_basenames" + db.getProp(db.MARYEXT));
		}
		return props;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(DATADIR, "Directory containing the " + getType() + " files");
		props2Help.put(DATAEXT, "File extension of " + getType() + " files");
		props2Help.put(DATATIMELINE, "File containing all " + getType() + ". Will be created by this module");
		props2Help.put(BASENAMETIMELINE, "File containing all basenames.");
	}

	/**
	 * Read and concatenate a list of data files into a single timeline file.
	 * 
	 * @throws IOException
	 *             IOException
	 */
	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		System.out.println("---- Importing data files");
		System.out.println("Base directory: " + db.getProp(db.ROOTDIR));

		// read Files into List and check for readability early on:
		List<File> files = new ArrayList<File>(bnl.getLength());
		for (int b = 0; b < bnl.getLength(); b++) {
			String basename = bnl.getName(b);
			File file = new File(getProp(DATADIR) + basename + getProp(DATAEXT));
			if (file.canRead()) {
				files.add(file);
			} else {
				logger.warn("Ignoring unreadable file " + file);
			}
		}
		// if List is still empty at this point, something is wrong; break:
		if (files.size() == 0) {
			return false;
		}

		// load basename timeline:
		String basenameTimelineFilename = getProp(BASENAMETIMELINE);
		TimelineReader basenameTimelineReader = null;
		basenameTimelineReader = new TimelineReader(basenameTimelineFilename);

		// process each file:
		dataTimeline = null;
		int p = 0; // progress counter
		long basenameStart = 0;
		for (File file : files) {
			AbstractDataFile dataFile = loadDataFile(file);

			// first, make sure dataTimeLine is initialized (if not, init from dataFile):
			if (dataTimeline == null) {
				initializeDataTimeline(dataFile);
			}
			assert dataFile.sampleRate == dataTimeline.sampleRate;

			// get required duration from basename timeline:
			Datagram basenameDatagram = basenameTimelineReader.getDatagram(basenameStart);
			long basenameEnd = basenameStart + basenameDatagram.getDuration();
			int requiredDuration = (int) (basenameEnd - basenameStart);

			// get Datagrams for dataFile, enforcing requiredDuration, and feed them into the dataTimeline:
			Datagram[] datagrams = dataFile.getDatagrams(requiredDuration);
			dataTimeline.feed(datagrams, dataTimeline.getSampleRate());

			// set basenameStart to presumed start of next basename datagram:
			basenameStart = basenameEnd + 1;

			// progress info:
			percent = 100 * p / files.size();
			p++;
			System.out.println(file.getName());
		}

		// finally, close the data timeline...
		dataTimeline.close();

		// ...and print some statistics:
		System.out.println("---- data timeline result:");
		System.out.println("Number of data files scanned: " + bnl.getLength());
		System.out.println("Number of frames: [" + dataTimeline.getNumDatagrams() + "].");
		int numIndex = dataTimeline.getIndex().getNumIdx();
		System.out.println("Size of the index: [" + numIndex + "] (" + (dataTimeline.getIndex().getNumIdx() * 16)
				+ " bytes, i.e. " + String.format("%.2f", numIndex * 16.0 / 1048576.0) + " megs).");
		System.out.println("---- data timeline done.");

		return true;
	}

	/**
	 * load an {@link AbstractDataFile}
	 * 
	 * @param file
	 *            to load
	 * @return the AbstractDataFile
	 */
	protected abstract AbstractDataFile loadDataFile(File file);

	/**
	 * initialize the data timeline, using one {@link AbstractDataFile} to provide the parameters
	 * 
	 * @param dataFile
	 *            to provide parameters
	 * @throws IOException
	 *             IOException
	 */
	protected void initializeDataTimeline(AbstractDataFile dataFile) throws IOException {
		String processingHeader = getProcessingHeader();
		int sampleRate = dataFile.getSampleRate();
		float frameSkip = dataFile.getFrameSkip();
		dataTimeline = new TimelineWriter(getProp(DATATIMELINE), processingHeader, sampleRate, frameSkip);
	}

	/**
	 * generate a processing header for the {@link TimelineWriter}
	 * 
	 * @throws IOException
	 *             IOException
	 * @return processing header
	 */
	protected abstract String getProcessingHeader() throws IOException;

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	@Override
	public int getProgress() {
		return percent;
	}

}
