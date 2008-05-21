/**
 * Copyright 2000-2006 DFKI GmbH.
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

package de.dfki.lt.mary;

// General Java Classes
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import de.dfki.lt.mary.modules.MaryModule;
import de.dfki.lt.mary.modules.Synthesis;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.MaryAudioUtils;

/**
 * The main program for the mary TtS system.
 *          It can run as a socket server or as a stand-alone program.
 * @author Marc Schr&ouml;der
 */

public class Mary {
    public static final int STATE_OFF = 0;
    public static final int STATE_STARTING = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_SHUTTING_DOWN = 3;

    private static Category logger;

    private static Vector<MaryModule> allModules;
    private static HashMap<MaryDataType,Vector<MaryModule>> type2Modules;
    private static int currentState = STATE_OFF;

    /**
     * Inform about system state.
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
     * Provide vector containing all MaryModules instances.
     * The order is not important.
     */
    public static Vector allModules() {
        return allModules;
    }

    /**
     * Provide a vector containing all modules accepting the given data type
     * as input.
     */
    public static Vector<MaryModule> getModulesAcceptingType(MaryDataType type) {
        if (type == null)
            throw new NullPointerException("received null type");
        assert type2Modules != null;
        return type2Modules.get(type);
    }

    /**
     * Find an active module by its class.
     * @return the module instance if found, or null if not found.
     */
    public static MaryModule getModule(Class moduleClass) {
        for (Iterator it = allModules.iterator(); it.hasNext();) {
            Object m = it.next();
            if (m.getClass().equals(moduleClass)) {
                return (MaryModule) m;
            }
        }
        // Not found:
        return null;
    }

    /**
     * A method for determining the list of modules required to transform
     * the given source data type into the requested target data type.
     * @return the (ordered) list of modules required, or null if no such
     * list could be found.
     */
    public static LinkedList<MaryModule> modulesRequiredForProcessing(
        MaryDataType sourceType,
        MaryDataType targetType,
        Locale locale)
    {
        return modulesRequiredForProcessing(sourceType, targetType, locale, null);
    }
    /**
     * A method for determining the list of modules required to transform
     * the given source data type into the requested target data type. If the
     * voice given is not null, any preferred modules it may have are taken into account.
     * @return the (ordered) list of modules required, or null if no such
     * list could be found.
     */
    public static LinkedList<MaryModule> modulesRequiredForProcessing(
        MaryDataType sourceType,
        MaryDataType targetType,
        Locale locale,
        Voice voice) {
        if (sourceType == null)
            throw new NullPointerException("Received null source type");
        if (targetType == null)
            throw new NullPointerException("Received null target type");
        if (locale == null)
            throw new NullPointerException("Received null locale");
        LinkedList<MaryDataType> seenTypes = new LinkedList<MaryDataType>();
        seenTypes.add(sourceType);
        return modulesRequiredForProcessing(
            sourceType,
            targetType,
            locale,
            voice,
            seenTypes);
    }
    
    protected static void addJarsToClasspath() throws Exception
    {
        File jarDir = new File(MaryProperties.maryBase()+"/java");
        File[] jarFiles = jarDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        assert jarFiles != null;
        URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        for (int i=0; i<jarFiles.length; i++) {
            URL jarURL = new URL("file:"+jarFiles[i].getPath());
            method.invoke(sysloader, new Object[] {jarURL});
        }
        
    }

    private static LinkedList<MaryModule> modulesRequiredForProcessing(
        MaryDataType sourceType,
        MaryDataType targetType,
        Locale locale,
        Voice voice,
        LinkedList<MaryDataType> seenTypes) {
        // This method recursively calls itself.
        // It forward-constructs a list of seen types (upon test),
        // and backward-constructs a list of required modules (upon success).

        // Terminating condition:
        if (sourceType == targetType) {
                logger.debug("found path through modules");
            return new LinkedList<MaryModule>();
        }
        // Recursion step:
        // Any voice-specific modules?
        Vector candidates = null;
        if (voice != null) candidates = voice.getPreferredModulesAcceptingType(sourceType);
        if (candidates == null || candidates.isEmpty()) { // default: use all available modules
            candidates = getModulesAcceptingType(sourceType);
        }
        if (candidates == null || candidates.isEmpty()) {
            // Verify if it is a language-independent type for which
            // a language-dependent data type exists:
            if (sourceType.getLocale() == null) { // language-independent
                MaryDataType langSpecType =
                    MaryDataType.getLanguageSpecificVersion(sourceType, locale);
                if (langSpecType != null) {
                    candidates = getModulesAcceptingType(langSpecType);
                }
            }
        }
        if (candidates == null || candidates.isEmpty()) {
            // no module can handle this type
            return null;
        }
        for (Iterator it = candidates.iterator(); it.hasNext();) {
            MaryModule candidate = (MaryModule) it.next();
            MaryDataType outputType = candidate.outputType();
            // Ignore candidates that would bring us to a data type that we
            // have already seen (i.e., that would lead to a loop):
            if (!seenTypes.contains(outputType)) {
                seenTypes.add(outputType);
                    logger.debug(
                        "Module "
                            + candidate.name()
                            + " converts "
                            + sourceType.name()
                            + " into "
                            + outputType);
                // recursive call:
                LinkedList<MaryModule> path =
                    modulesRequiredForProcessing(
                        outputType,
                        targetType,
                        locale,
                        voice,
                        seenTypes);
                if (path != null) {
                    // success, found a path of which candidate is the first
                    // step
                    path.addFirst(candidate);
                    return path;
                } // else, try next candidate
                seenTypes.removeLast();
            }
        }
        // We get here only if none of the candidates lead to a valid path
        return null; // failure
    }

    private static void startModules()
        throws ClassNotFoundException, InstantiationException, Exception {
        for (String moduleClassName : MaryProperties.moduleClasses()) {
            MaryModule m =
                (MaryModule) Class.forName(moduleClassName).newInstance();
            allModules.add(m);
        }
        // Separate loop for startup allows modules to cross-reference to each
        // other via Mary.getModule(Class) even if some have not yet been
        // started.
        for (MaryModule m : allModules) {
            // Only start the modules here if in server mode: 
            if ((MaryProperties.getBoolean("server") || m instanceof Synthesis) 
                    && m.getState() == MaryModule.MODULE_OFFLINE) {
                try {
                    m.startup();
                } catch (Throwable t) {
                    throw new Exception("Problem starting module "+ m.name(), t);
                }
                
            }
            if (MaryProperties.getAutoBoolean("modules.poweronselftest", false)) {
                m.powerOnSelfTest();
            }

        }
    }

    public static void startup() throws Exception {
        currentState = STATE_STARTING;

        allModules = new Vector<MaryModule>();

        // Configure Logging:
        logger = Logger.getLogger("main");
        Logger.getRootLogger().setLevel(Level.toLevel(MaryProperties.needProperty("log.level")));
        PatternLayout layout = new PatternLayout("%d [%t] %-5p %-10c %m\n");
        if (MaryProperties.needAutoBoolean("log.tofile")) {
            String filename = MaryProperties.getFilename("log.filename", "mary.log");
            File logFile = new File(filename);
            if (logFile.exists()) logFile.delete();
            BasicConfigurator.configure(
                new FileAppender(layout, filename));
        } else {
            BasicConfigurator.configure(new WriterAppender(layout, System.err));
        }
        logger.info("Mary starting up...");
        logger.info(
            "Specification version " + Version.specificationVersion());
        logger.info(
            "Implementation version " + Version.implementationVersion());
        logger.info(
            "Running on a Java "
                + System.getProperty("java.version")
                + " implementation by "
                + System.getProperty("java.vendor")
                + ", on a "
                + System.getProperty("os.name")
                + " platform ("
                + System.getProperty("os.arch")
                + ", "
                + System.getProperty("os.version")
                + ")");
            logger.debug("Full dump of system properties:");
            logger.debug(
                "java.version = " + System.getProperty("java.version"));
            logger.debug("java.vendor = " + System.getProperty("java.vendor"));
            logger.debug(
                "java.vendor.url = " + System.getProperty("java.vendor.url"));
            logger.debug("java.home = " + System.getProperty("java.home"));
            logger.debug(
                "java.vm.specification.version = "
                    + System.getProperty("java.vm.specification.version"));
            logger.debug(
                "java.vm.specification.vendor = "
                    + System.getProperty("java.vm.specification.vendor"));
            logger.debug(
                "java.vm.specification.name = "
                    + System.getProperty("java.vm.specification.name"));
            logger.debug(
                "java.vm.version = " + System.getProperty("java.vm.version"));
            logger.debug(
                "java.vm.vendor = " + System.getProperty("java.vm.vendor"));
            logger.debug(
                "java.vm.name = " + System.getProperty("java.vm.name"));
            logger.debug(
                "java.specification.version = "
                    + System.getProperty("java.specification.version"));
            logger.debug(
                "java.specification.vendor = "
                    + System.getProperty("java.specification.vendor"));
            logger.debug(
                "java.specification.name = "
                    + System.getProperty("java.specification.name"));
            logger.debug(
                "java.class.version = "
                    + System.getProperty("java.class.version"));
            logger.debug(
                "java.class.path = " + System.getProperty("java.class.path"));
            logger.debug(
                "java.library.path = "
                    + System.getProperty("java.library.path"));
            logger.debug(
                "java.io.tmpdir = " + System.getProperty("java.io.tmpdir"));
            logger.debug(
                "java.compiler = " + System.getProperty("java.compiler"));
            logger.debug(
                "java.ext.dirs = " + System.getProperty("java.ext.dirs"));
            logger.debug("os.name = " + System.getProperty("os.name"));
            logger.debug("os.arch = " + System.getProperty("os.arch"));
            logger.debug("os.version = " + System.getProperty("os.version"));
            logger.debug("file.encoding = " + System.getProperty("file.encoding"));
            logger.debug(
                "file.separator = " + System.getProperty("file.separator"));
            logger.debug(
                "path.separator = " + System.getProperty("path.separator"));
            logger.debug(
                "line.separator = " + System.getProperty("line.separator"));
            logger.debug("user.name = " + System.getProperty("user.name"));
            logger.debug("user.home = " + System.getProperty("user.home"));
            logger.debug("user.dir = " + System.getProperty("user.dir"));
            logger.debug("Mary-specific system properties:");
            logger.debug("mary.base = " + System.getProperty("mary.base"));
            logger.debug("shprot.base = " + System.getProperty("shprot.base"));
            logger.debug("server = " + System.getProperty("server"));
            logger.debug("XML libraries used:");
            try {
                Class xercesVersion = Class.forName("org.apache.xerces.impl.Version");
                logger.debug(xercesVersion.getMethod("getVersion").invoke(null));
            } catch (Exception e) {
                logger.debug("XML parser is not Xerces: " + DocumentBuilderFactory.newInstance().getClass());
            }
            try {
                Class xalanVersion = Class.forName("org.apache.xalan.Version");
                logger.debug(xalanVersion.getMethod("getVersion").invoke(null));
            } catch (Exception e) {
                logger.debug("XML transformer is not Xalan: " + TransformerFactory.newInstance().getClass());
            }

        // Essential environment checks:
        EnvironmentChecks.check();

        // Instantiate module classes and startup modules:
        startModules();

        // Fill datatype-to-module translation table:
        // (for each data type, provide a list of modules accepting this
        // type as input)
        type2Modules = new HashMap<MaryDataType,Vector<MaryModule>>();
        for (Iterator it = allModules.iterator(); it.hasNext();) {
            MaryModule m = (MaryModule) it.next();
            Vector<MaryModule> list = null;
            if (type2Modules.containsKey(m.inputType())) {
                list = type2Modules.get(m.inputType());
            } else {
                list = new Vector<MaryModule>();
                type2Modules.put(m.inputType(), list);
            }
            list.add(m);
        }

        logger.info("Startup complete.");
        currentState = STATE_RUNNING;
    }

    public static void shutdown() {
        currentState = STATE_SHUTTING_DOWN;
        logger.info("Shutting down modules...");
        // Shut down modules:
        for (Iterator it = allModules.iterator(); it.hasNext();) {
            MaryModule m = (MaryModule) it.next();
            if (m.getState() == MaryModule.MODULE_RUNNING)
                m.shutdown();
        }
        logger.info("Shutdown complete.");
        currentState = STATE_OFF;
    }

    /**
     * The starting point of the Mary program.
     * If server mode is requested by property settings, starts
     * the <code>MaryServer</code>; otherwise, a <code>Request</code>
     * is created reading from the file given as first argument and writing
     * to System.out.
     *
     * <p>Usage:<p>
     * As a socket server:
     * <pre>
     * java -Dmary.base=$MARY_BASE -Dserver=true de.dfki.lt.mary.Mary
     * </pre><p>
     * As a stand-alone program:
     * <pre>
     * java -Dmary.base=$MARY_BASE de.dfki.lt.mary.Mary myfile.txt
     * </pre>
     * @see MaryProperties
     * @see MaryServer
     * @see RequestHandler
     * @see Request
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        addJarsToClasspath();
        // Read properties:
        // (Will throw exceptions if problems are found)
        MaryProperties.readProperties();

        if (MaryProperties.needBoolean("server")) {
            System.err.print("MARY server " + Version.specificationVersion() + " starting...");
            startup();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    shutdown();
                }
            });
            System.err.println(" started in " + (System.currentTimeMillis()-startTime)/1000. + " s");
            new MaryServer().run();
        } else { // command-line mode
            startup();
            String inputTypeName = MaryProperties.getProperty("input.type");
            if (inputTypeName == null) {
                inputTypeName = "TEXT_EN";
                logger.warn("-Dinput.type not set! Assuming default -Dinput.type="+inputTypeName);
            }
            String outputTypeName = MaryProperties.getProperty("output.type");
            if (outputTypeName == null) {
                outputTypeName = "AUDIO";
                logger.warn("-Doutput.type not set! Assuming default -Doutput.type="+outputTypeName);
            }
            MaryDataType inputType = MaryDataType.get(inputTypeName);
            MaryDataType outputType = MaryDataType.get(outputTypeName);
            Voice voice = null;
            String voiceName = MaryProperties.getProperty("voice");
            if (voiceName != null)
                voice = Voice.getVoice(voiceName);
            else if (inputType.getLocale() != null)
                voice = Voice.getDefaultVoice(inputType.getLocale());
            AudioFileFormat audioFileFormat = null;
            if (outputType.equals(MaryDataType.get("AUDIO"))) {
                String audioTypeName = MaryProperties.getProperty("audio.type");
                if (audioTypeName == null) {
                    audioTypeName = "WAVE";
                    logger.warn("-Daudio.type not set! Assuming default -Daudio.type="+audioTypeName);
                }
                AudioFileFormat.Type audioType = MaryAudioUtils.getAudioFileFormatType(audioTypeName);
                AudioFormat audioFormat = null;
                if (audioType.toString().equals("MP3")) {
                    if (!MaryAudioUtils.canCreateMP3())
                        throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
                    audioFormat = MaryAudioUtils.getMP3AudioFormat();
                } else {
                    Voice ref = (voice != null) ? voice : Voice.getDefaultVoice(Locale.GERMAN);
                    audioFormat = ref.dbAudioFormat();
                }
                audioFileFormat = new AudioFileFormat(audioType, audioFormat, AudioSystem.NOT_SPECIFIED);
            }
            Request request = new Request(inputType, outputType, voice, "", "", 1, audioFileFormat);
     
            InputStream is;
            if (args.length == 0 || args[0].equals("-"))
                is = System.in;
            else
                is = new FileInputStream(args[0]);
            request.readInputData(
                new InputStreamReader(is, "UTF-8"));
            request.process();
            request.writeOutputData(System.out);
        }
        shutdown();
    }
}
