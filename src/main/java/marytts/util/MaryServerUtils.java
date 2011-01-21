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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import marytts.server.MaryProperties;

/**
 * @author marc
 *
 */
public class MaryServerUtils {

    /**
     * Instantiate an object by calling one of its constructors.
     * @param objectInitInfo a string description of the object to instantiate.
     * The objectInitInfo is expected to have one of the following forms:
     * <ol>
     *   <li> my.cool.Stuff</li>
     *   <li> my.cool.Stuff(any,string,args,without,spaces)</li>
     *   <li>my.cool.Stuff(arguments,$my.special.property,other,args)</li>
     * </ol>
     * where 'my.special.property' is a property in one of the MARY config files.
     * @return the newly instantiated object.
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Object instantiateObject(String objectInitInfo)
    throws ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException
    {
        Object obj = null;
        String[] args = null;
        String className = null;
        if (objectInitInfo.contains("(")) { // arguments
            int firstOpenBracket = objectInitInfo.indexOf('(');
            className = objectInitInfo.substring(0, firstOpenBracket);
            int lastCloseBracket = objectInitInfo.lastIndexOf(')');
            args = objectInitInfo.substring(firstOpenBracket+1, lastCloseBracket).split(",");
            for (int i=0; i<args.length; i++) {
                if (args[i].startsWith("$")) {
                    // replace value with content of property named after the $
                    args[i] = MaryProperties.getProperty(args[i].substring(1));
                }
                args[i] = args[i].trim();
            }
        } else { // no arguments
            className = objectInitInfo;
        }
        Class<? extends Object> theClass = Class.forName(className).asSubclass(Object.class);
        // Now invoke Constructor with args.length String arguments
        if (args != null) {
            Class<String>[] constructorArgTypes = new Class[args.length];
            Object[] constructorArgs = new Object[args.length];
            for (int i=0; i<args.length; i++) {
                constructorArgTypes[i] = String.class;
                constructorArgs[i] = args[i];
            }
            Constructor<? extends Object> constructor = (Constructor<? extends Object>) theClass.getConstructor(constructorArgTypes);
            obj = constructor.newInstance(constructorArgs);
        } else {
            obj = theClass.newInstance();
        }
        return obj;
    }

    /**
     * Verify if the java virtual machine is in a low memory condition.
     * The memory is considered low if less than a specified value is
     * still available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code>.The threshold value can be specified
     * as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     * @return a boolean indicating whether or not the system is in low memory condition.
     */
    public static boolean lowMemoryCondition()
    {
        return MaryUtils.availableMemory() < lowMemoryThreshold();
    }

    /**
     * Verify if the java virtual machine is in a very low memory condition.
     * The memory is considered very low if less than half a specified value is
     * still available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code>.The threshold value can be specified
     * as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     * @return a boolean indicating whether or not the system is in very low memory condition.
     */
    public static boolean veryLowMemoryCondition()
    {
        return MaryUtils.availableMemory() < lowMemoryThreshold()/2;
    }

    private static long lowMemoryThreshold()
    {
        if (lowMemoryThreshold < 0) // not yet initialised
            lowMemoryThreshold = (long) MaryProperties.getInteger("mary.lowmemory", 10000000);
        return lowMemoryThreshold;
    }

    private static long lowMemoryThreshold = -1;

}
