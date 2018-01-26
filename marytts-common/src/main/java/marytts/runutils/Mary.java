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
package marytts.runutils;

// General Java Classes
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

// Reflection
import org.reflections.Reflections;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

// Configuration
import marytts.config.MaryConfigLoader;

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.Pair;
import marytts.util.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main program for the mary TtS system. It can run as a socket server or as
 * a stand-alone program.
 *
 * @author Marc Schr&ouml;der
 */

public class Mary {
    public static final int STATE_OFF = 0;
    public static final int STATE_STARTING = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_SHUTTING_DOWN = 3;

    private static Logger logger =  LogManager.getLogger(Mary.class);

    private static int currentState = STATE_OFF;
    private static boolean jarsAdded = false;

    /**
     * Inform about system state.
     *
     * @return an integer representing the current system state.
     * @see #STATE_OFF
     * @see #STATE_STARTING
     * @see #STATE_RUNNING
     * @see #STATE_SHUTTING_DOWN
     */
    public synchronized static int currentState() {
        return currentState;
    }


    private synchronized static void startModules() throws ClassNotFoundException, InstantiationException,
        Exception {

	// Load configurations
	for (MaryConfigLoader mc: MaryConfigLoader.getConfigLoaders()) {
	    mc.load();
	}

	// Instantiate available modules
	Reflections reflections = new Reflections("marytts");
        for (Class<? extends MaryModule> moduleClass : reflections.getSubTypesOf(MaryModule.class)) {
	    if (! Modifier.isAbstract(moduleClass.getModifiers())) {
		MaryModule m = ModuleRegistry.instantiateModule(moduleClass.getName());
		ModuleRegistry.registerModule(m);
	    }
        }

	// Start the modules
        List<Pair<MaryModule, Long>> startupTimes = new ArrayList<Pair<MaryModule, Long>>();
        for (MaryModule m : ModuleRegistry.listRegisteredModules()) {
            // Only start the modules here if in server mode:
            if (m.getState() == MaryModule.MODULE_OFFLINE) {
                long before = System.currentTimeMillis();
                try {
                    m.startup();
                } catch (Throwable t) {
                    throw new Exception("Problem starting module " + m.getClass().getName(), t);
                }
                long after = System.currentTimeMillis();
                startupTimes.add(new Pair<MaryModule, Long>(m, after - before));
            }
        }

        if (startupTimes.size() > 0) {
            Collections.sort(startupTimes, new Comparator<Pair<MaryModule, Long>>() {
                public int compare(Pair<MaryModule, Long> o1, Pair<MaryModule, Long> o2) {
                    return -o1.getSecond().compareTo(o2.getSecond());
                }
            });
            logger.debug("Startup times:");
            for (Pair<MaryModule, Long> p : startupTimes) {
                logger.debug(p.getFirst().getClass().getName() + ": " + p.getSecond() + " ms");
            }
        }
    }


    /**
     * Start the MARY system and all modules. This method must be called once
     * before any calls to
     * {@link #process(String configuration, String input_data, OutputStream output)}
     * are possible.
     *
     * @throws IllegalStateException
     *             if the system is not offline.
     * @throws Exception
     *             Exception
     */
    public synchronized static void startup() throws Exception {
        if (currentState != STATE_OFF) {
            throw new IllegalStateException("Cannot start system: it is not offline");
        }
        currentState = STATE_STARTING;
        logger.info("Mary starting up...");
        logger.info("Running on a Java " + System.getProperty("java.version") +
		    " implementation by "  + System.getProperty("java.vendor") +
		    ", on a " + System.getProperty("os.name") +
		    " platform ("  + System.getProperty("os.arch") + ", " +
		    System.getProperty("os.version") + ")");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });

	//
        // Instantiate module classes and startup modules:
        startModules();

        logger.info("Startup complete.");
        currentState = STATE_RUNNING;
    }

    /**
     * Orderly shut down the MARY system.
     *
     * @throws IllegalStateException
     *             if the MARY system is not running.
     */
    public static void shutdown() {
        if (currentState != STATE_RUNNING) {
            throw new IllegalStateException("MARY system is not running");
        }
        currentState = STATE_SHUTTING_DOWN;
        logger.info("Shutting down modules...");
        // Shut down modules:
        for (MaryModule m : ModuleRegistry.listRegisteredModules()) {
            if (m.getState() == MaryModule.MODULE_RUNNING) {
                m.shutdown();
            }
        }

        logger.info("Shutdown complete.");
        currentState = STATE_OFF;
    }

}
