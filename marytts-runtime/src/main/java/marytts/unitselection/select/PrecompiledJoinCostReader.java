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
package marytts.unitselection.select;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import marytts.exceptions.MaryConfigurationException;
import marytts.server.MaryProperties;
import marytts.unitselection.data.Unit;
import marytts.util.data.MaryHeader;

/**
 * Loads a precompiled join cost file and provides access to the join cost.
 * 
 */
public class PrecompiledJoinCostReader implements JoinCostFunction {

	private MaryHeader hdr = null;

	// keys = Integers representing left unit index;
	// values = maps containing
	// - keys = Integers representing right unit index;
	// - values = Floats representing the jost of joining them.
	protected Map left;

	/**
	 * Empty constructor; need to call load() separately.
	 * 
	 * @see #load(String fileName, InputStream dummy, String dummy2, float dummy3)
	 */
	public PrecompiledJoinCostReader() {
	}

	/**
	 * Create a precompiled join cost file reader from the given file
	 * 
	 * @param fileName
	 *            the file to read
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public PrecompiledJoinCostReader(String fileName) throws IOException, MaryConfigurationException {
		load(fileName, null, null, 0);
	}

	/**
	 * Initialise this join cost function by reading the appropriate settings from the MaryProperties using the given
	 * configPrefix.
	 * 
	 * @param configPrefix
	 *            the prefix for the (voice-specific) config entries to use when looking up files to load.
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void init(String configPrefix) throws MaryConfigurationException {
		String precomputedJoinCostFileName = MaryProperties.getFilename(configPrefix + ".precomputedJoinCostFile");
		try {
			load(precomputedJoinCostFileName, null, null, 0);
		} catch (IOException ioe) {
			throw new MaryConfigurationException("Problem loading join file " + precomputedJoinCostFileName, ioe);
		}
	}

	/**
	 * Load the given precompiled join cost file
	 * 
	 * @param fileName
	 *            the file to read
	 * @param dummy
	 *            not used, just used to fulfill the join cost function interface
	 * @param dummy2
	 *            not used, just used to fulfill the join cost function interface
	 * @param dummy3
	 *            not used, just used to fulfill the join cost function interface
	 * @throws IOException
	 *             if a problem occurs while reading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	@Override
	public void load(String fileName, InputStream dummy, String dummy2, float dummy3) throws IOException,
			MaryConfigurationException {
		/* Open the file */
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		hdr = new MaryHeader(dis);
		if (hdr.getType() != MaryHeader.PRECOMPUTED_JOINCOSTS) {
			throw new MaryConfigurationException("File [" + fileName + "] is not a valid Mary precompiled join costs file.");
		}
		/* Read the number of units */
		int numberOfLeftUnits = dis.readInt();
		if (numberOfLeftUnits < 0) {
			throw new MaryConfigurationException("File [" + fileName + "] has a negative number of units. Aborting.");
		}

		left = new HashMap();
		/* Read the start times and durations */
		for (int i = 0; i < numberOfLeftUnits; i++) {
			int leftIndex = dis.readInt();
			int numberOfRightUnits = dis.readInt();
			Map right = new HashMap();
			left.put(new Integer(leftIndex), right);
			for (int j = 0; j < numberOfRightUnits; j++) {
				int rightIndex = dis.readInt();
				float cost = dis.readFloat();
				right.put(new Integer(rightIndex), new Float(cost));
			}
		}

	}

	/**
	 * Return the (precomputed) cost of joining the two given units; if there is no precomputed cost, return
	 * Double.POSITIVE_INFINITY.
	 * 
	 * @param t1
	 *            t1
	 * @param uleft
	 *            uleft
	 * @param t2
	 *            t2
	 * @param uright
	 *            uright
	 */
	public double cost(Target t1, Unit uleft, Target t2, Unit uright) {
		Integer leftIndex = new Integer(uleft.index);
		Map rightUnitsMap = (Map) left.get(leftIndex);
		if (rightUnitsMap == null)
			return Double.POSITIVE_INFINITY;
		Integer rightIndex = new Integer(uright.index);
		Float cost = (Float) rightUnitsMap.get(rightIndex);
		if (cost == null)
			return Double.POSITIVE_INFINITY;
		return cost.doubleValue();
	}

}
