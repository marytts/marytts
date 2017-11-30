/**
 * Copyright 2011 DFKI GmbH.
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

package marytts.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.config.MaryConfigLoader;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.runutils.Mary;
import marytts.util.string.StringUtils;

import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class MaryRuntimeUtils {

    // FIXME: synchronized mary
    public static void ensureMaryStarted() throws Exception {
	if (Mary.currentState() == Mary.STATE_OFF) {
	    Mary.startup();
	}
    }


    // /**
    //  * Verify if the java virtual machine is in a low memory condition. The
    //  * memory is considered low if less than a specified value is still
    //  * available for processing. "Available" memory is calculated using
    //  * <code>availableMemory()</code>.The threshold value can be specified as
    //  * the Mary property mary.lowmemory (in bytes). It defaults to 20000000
    //  * bytes.
    //  *
    //  * @return a boolean indicating whether or not the system is in low memory
    //  *         condition.
    //  */
    // public static boolean lowMemoryCondition() {
    //     return MaryUtils.availableMemory() < lowMemoryThreshold();
    // }

    // /**
    //  * Verify if the java virtual machine is in a very low memory condition. The
    //  * memory is considered very low if less than half a specified value is
    //  * still available for processing. "Available" memory is calculated using
    //  * <code>availableMemory()</code> .The threshold value can be specified as
    //  * the Mary property mary.lowmemory (in bytes). It defaults to 20000000
    //  * bytes.
    //  *
    //  * @return a boolean indicating whether or not the system is in very low
    //  *         memory condition.
    //  */
    // public static boolean veryLowMemoryCondition() {
    //     return MaryUtils.availableMemory() < lowMemoryThreshold() / 2;
    // }

    // private static long lowMemoryThreshold() {
    //     if (lowMemoryThreshold < 0) { // not yet initialised
    //         lowMemoryThreshold = (long) MaryProperties.getInteger("mary.lowmemory", 10000000);
    //     }
    //     return lowMemoryThreshold;
    // }

    // private static long lowMemoryThreshold = -1;

}
