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
import java.lang.reflect.Method;
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

import marytts.Version;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryCache;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.Pair;
import marytts.util.io.FileUtils;

import marytts.config.MaryProperties;


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
    public static int currentState() {
        return currentState;
    }

    /**
     * Add jars to classpath. Normally this is called from startup().
     *
     * @throws Exception
     *             Exception
     */
    protected static void addJarsToClasspath() throws Exception {
        if (true) {
            return;
        }
        // TODO: clean this up when the new modularity mechanism is in place
        if (jarsAdded) {
            return;    // have done this already
        }
        File jarDir = new File(MaryProperties.maryBase() + "/java");
        File[] jarFiles = jarDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        assert jarFiles != null;
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {URL.class});
        method.setAccessible(true);
        for (int i = 0; i < jarFiles.length; i++) {
            URL jarURL = new URL("file:" + jarFiles[i].getPath());
            method.invoke(sysloader, new Object[] {jarURL});
        }
        jarsAdded = true;
    }

    private static void startModules() throws ClassNotFoundException, InstantiationException,
        Exception {
        for (String moduleClassName : MaryProperties.moduleInitInfo()) {
            MaryModule m = ModuleRegistry.instantiateModule(moduleClassName);
            // Partially fill module repository here;
            // TODO: voice-specific entries will be added when each voice is
            // loaded.
            ModuleRegistry.registerModule(m, m.getLocale());
        }
        ModuleRegistry.setRegistrationComplete();

        List<Pair<MaryModule, Long>> startupTimes = new ArrayList<Pair<MaryModule, Long>>();

        // Separate loop for startup allows modules to cross-reference to each
        // other via Mary.getModule(Class) even if some have not yet been
        // started.
        for (MaryModule m : ModuleRegistry.getAllModules()) {
            // Only start the modules here if in server mode:
            if ((!MaryProperties.getProperty("server").equals("commandline"))
                    && m.getState() == MaryModule.MODULE_OFFLINE) {
                long before = System.currentTimeMillis();
                try {
                    m.startup();
                } catch (Throwable t) {
                    throw new Exception("Problem starting module " + m.name(), t);
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
                logger.debug(p.getFirst().name() + ": " + p.getSecond() + " ms");
            }
        }
    }

    /**
     * Start the MARY system and all modules. This method must be called once
     * before any calls to
     * {@link #process(String configuration, String input_data, OutputStream output)}
     * are possible. The method will dynamically extend the classpath to all jar
     * files in MARY_BASE/java/*.jar. Use <code>startup(false)</code> if you do
     * not want to automatically extend the classpath in this way.
     *
     * @throws IllegalStateException
     *             if the system is not offline.
     * @throws Exception
     *             Exception
     */
    public static void startup() throws Exception {
        startup(true);
    }

    /**
     * Start the MARY system and all modules. This method must be called once
     * before any calls to
     * {@link #process(String configuration, String input_data, OutputStream output)}
     * are possible.
     *
     * @param addJarsToClasspath
     *            if true, the method will dynamically extend the classpath to
     *            all jar files in MARY_BASE/java/*.jar; if false, the classpath
     *            will remain unchanged.
     * @throws IllegalStateException
     *             if the system is not offline.
     * @throws Exception
     *             Exception
     */
    public static void startup(boolean addJarsToClasspath) throws Exception {
        if (currentState != STATE_OFF) {
            throw new IllegalStateException("Cannot start system: it is not offline");
        }
        currentState = STATE_STARTING;

        if (addJarsToClasspath) {
            addJarsToClasspath();
        }

        logger.info("Mary starting up...");
        logger.info("Specification version " + Version.specificationVersion());
        logger.info("Implementation version " + Version.implementationVersion());
        logger.info("Running on a Java " + System.getProperty("java.version") + " implementation by "
                    + System.getProperty("java.vendor") + ", on a " + System.getProperty("os.name") + " platform ("
                    + System.getProperty("os.arch") + ", " + System.getProperty("os.version") + ")");
        logger.debug("MARY_BASE: " + MaryProperties.maryBase());
        String[] installedFilenames = new File(MaryProperties.maryBase() + "/installed").list();
        if (installedFilenames == null) {
            logger.debug("The installed/ folder does not exist.");
        } else {
            StringBuilder installedMsg = new StringBuilder();
            for (String filename : installedFilenames) {
                if (installedMsg.length() > 0) {
                    installedMsg.append(", ");
                }
                installedMsg.append(filename);
            }
            logger.debug("Content of installed/ folder: " + installedMsg);
        }
        String[] confFilenames = new File(MaryProperties.maryBase() + "/conf").list();
        if (confFilenames == null) {
            logger.debug("The conf/ folder does not exist.");
        } else {
            StringBuilder confMsg = new StringBuilder();
            for (String filename : confFilenames) {
                if (confMsg.length() > 0) {
                    confMsg.append(", ");
                }
                confMsg.append(filename);
            }
            logger.debug("Content of conf/ folder: " + confMsg);
        }
        logger.debug("Full dump of system properties:");
        for (Object key : new TreeSet<Object>(System.getProperties().keySet())) {
            logger.debug(key + " = " + System.getProperties().get(key));
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });

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
        for (MaryModule m : ModuleRegistry.getAllModules()) {
            if (m.getState() == MaryModule.MODULE_RUNNING) {
                m.shutdown();
            }
        }

        if (MaryCache.haveCache()) {
            MaryCache cache = MaryCache.getCache();
            try {
                cache.shutdown();
            } catch (SQLException e) {
                logger.warn("Cannot shutdown cache: ", e);
            }
        }
        logger.info("Shutdown complete.");
        currentState = STATE_OFF;
    }

}
