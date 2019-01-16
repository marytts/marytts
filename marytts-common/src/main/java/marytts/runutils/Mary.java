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

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryRuntimeUtils;
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
    public synchronized static int getCurrentState() {
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
		ModuleRegistry.registerModule(moduleClass.getName());
	    }
        }
    }

    private static void overrideLogLevel() throws Exception {
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
	    throw new Exception("\"" + level + "\" is an unknown level");

	// Set the level
	Configurator.setRootLevel(current_level);
	System.out.println("new level = " + level);
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

	if (System.getProperty("log4j.level") != null) {
	    overrideLogLevel();
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
        long before = System.currentTimeMillis();
        startModules();
        long after = System.currentTimeMillis();

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
        currentState = STATE_SHUTTING_DOWN;
        logger.info("Shutting down modules...");

        long before = System.currentTimeMillis();
        ModuleRegistry.clear();
        long after = System.currentTimeMillis();

        currentState = STATE_OFF;
        logger.info("Shutdown complete in " + (after - before) + " ms");
    }

}
