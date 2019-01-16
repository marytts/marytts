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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryUtils;
import marytts.util.Pair;
import marytts.util.io.FileUtils;

// Logging
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * The main program for the mary TtS system. It can run as a socket server or as
 * a stand-alone program.
 *
 * @author Marc Schr&ouml;der
 */

public class Mary {

    /** Constant to indicate that mary is starting */
    public static final int STATE_STARTING = 1;

    /** Constant to indicate that mary is ready to use */
    public static final int STATE_RUNNING = 2;

    /** Constant to indicate that mary is shutting down */
    public static final int STATE_SHUTTING_DOWN = 3;

    /** Constant to indicate that mary is offline */
    public static final int STATE_OFF = 0;

    /** The logger of the Mary class */
    private static Logger logger =  LogManager.getLogger(Mary.class);

    /** The current state of Mary (see constant and by default indicates that Mary is offline) */
    private static int currentState = STATE_OFF;

    /**
     * Inform about system state.
     *
     * @return an integer representing the current system state.
     * @see #STATE_OFF
     * @see #STATE_STARTING
     * @see #STATE_RUNNING
     * @see #STATE_SHUTTING_DOWN
     */
    public synchronized static int getCurrentState() {
        return currentState;
    }

    /**
     *  Override log level using system property log4j.level
     *
     *  @throws MaryException if the level given is unknown
     */
    private static void overrideLogLevel() throws MaryException {
	String level = System.getProperty("log4j.level");

	// Get the level
	Level current_level;
	if (level.equals("ERROR"))
	    current_level = Level.ERROR;
	else if (level.equals("WARN"))
	    current_level = Level.WARN;
	else if (level.equals("INFO"))
	    current_level = Level.INFO;
	else if (level.equals("DEBUG"))
	    current_level = Level.DEBUG;
	else
	    throw new MaryException("\"" + level + "\" is an unknown level");

	// Set the level
	Configurator.setRootLevel(current_level);
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

        // Start the clock
        long before = System.currentTimeMillis();

        // Adapt global logger level
	if (System.getProperty("log4j.level") != null) {
	    overrideLogLevel();
	}

        // Indicate that mary is starting
        currentState = STATE_STARTING;
        logger.info("Mary starting up...");
        logger.info("Running on a Java " + System.getProperty("java.version") +
		    " implementation by "  + System.getProperty("java.vendor") +
		    ", on a " + System.getProperty("os.name") +
		    " platform ("  + System.getProperty("os.arch") + ", " +
		    System.getProperty("os.version") + ")");

        // Prepare shutdown for later
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    shutdown();
                }
            });

	// Load configurations
	for (MaryConfigLoader mc: MaryConfigLoader.getConfigLoaders()) {
	    mc.load();
	}

	// Instantiate and register available modules
	Reflections reflections = new Reflections("marytts");
        for (Class<? extends MaryModule> moduleClass : reflections.getSubTypesOf(MaryModule.class)) {
	    if (! Modifier.isAbstract(moduleClass.getModifiers())) {
		ModuleRegistry.registerModule(moduleClass.getName());
	    }
        }

        // Stop the clock
        long after = System.currentTimeMillis();

        // Indicate that mary is ready
        currentState = STATE_RUNNING;
        logger.info("Startup complete in " + (after - before) + " ms");
    }

    /**
     * Orderly shut down the MARY system.
     *
     * @throws IllegalStateException
     *             if the MARY system is not running.
     */
    public synchronized static void shutdown() {
        if (currentState != STATE_RUNNING) {
            throw new IllegalStateException("MARY system is not running");
        }

        // Indicate that Mary starts to shut down
        currentState = STATE_SHUTTING_DOWN;
        logger.info("Shutting down modules...");

        // Clear the module registryu (shutting down the modules is part of it)
        long before = System.currentTimeMillis();
        ModuleRegistry.clear();
        long after = System.currentTimeMillis();

        // Indicates that Mary is off
        currentState = STATE_OFF;
        logger.info("Shutdown complete in " + (after - before) + " ms");
    }

    /**
     *  Helper to assess if mary is started and if not force the starting
     *
     *  @throws Exception if anything is going wrong
     */
    public static synchronized void ensureMaryStarted() throws Exception {
	if (Mary.getCurrentState() == Mary.STATE_OFF) {
	    Mary.startup();
	}
    }
}
