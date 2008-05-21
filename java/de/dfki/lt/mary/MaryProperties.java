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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JOptionPane;

import de.dfki.lt.mary.installvoices.VoiceInstaller;
import de.dfki.lt.mary.util.InstallationUtils;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.effects.BaseAudioEffect;

/**
 * A static class reading once, at program start, properties
 * from a number of external property files,
 * and providing them via static
 * getter methods to anyone wishing to read them.
 * At program start, this will read all the config files in the MARY_BASE/conf
 * directory that end in *.config. See the config files for more information.
 *
 * @author Marc Schr&ouml;der
 */

public class MaryProperties
{
    // The underlying Properties object
    private static Properties p = null;
    // Global configuration settings independent of any particular request:
    private static String maryBase = null;
    private static boolean logToFile = false;
    private static Vector<String> moduleClasses = new Vector<String>();
    private static Vector<String> synthClasses = new Vector<String>();
    private static Vector<String> audioEffectClasses = new Vector<String>();
    private static Vector<String> audioEffectNames = new Vector<String>();
    private static Vector<String> audioEffectParams = new Vector<String>();
    private static Vector<String> audioEffectSampleParams = new Vector<String>();
    private static Vector<String> audioEffectHelpTexts = new Vector<String>();
    
    private static Map<Locale,String> locale2prefix = new HashMap<Locale,String>();
    
    private static Object[] localSchemas;

    /** The mary base directory, e.g. /usr/local/mary */
    public static String maryBase()
    {
        if (maryBase == null) {
            maryBase = System.getProperty("mary.base");
            if (maryBase == null)
                throw new RuntimeException("System property mary.base not defined");
            maryBase = expandPath(maryBase);
        }
        return maryBase; 
    }
    /** Whether to log to the log file or to the screen */
    public static boolean logToFile() { return logToFile; }
    /** Names of the classes to use as modules. */
    public static Vector<String> moduleClasses() { return moduleClasses; }
    /** Names of the classes to use as waveform synthesizers. */
    public static Vector<String> synthesizerClasses() { return synthClasses; }
    /** Names of the classes to use as audio effects. */
    public static Vector<String> effectClasses() { return audioEffectClasses; }
    /** Names of audio effects. */
    public static Vector<String> effectNames() { return audioEffectNames; }
    /** Parameters of audio effects. */
    public static Vector<String> effectParams() { return audioEffectParams; }
    /** Sample Parameters of audio effects. */
    public static Vector<String> effectSampleParams() { return audioEffectSampleParams; }
    /** Help text of audio effects. */
    public static Vector<String> effectHelpTexts() { return audioEffectHelpTexts; }
    /** An Object[] containing File objects referencing local Schema files */
    public static Object[] localSchemas() { return localSchemas; }
    
    /**
     * Read the properties from property files and command line.
     * Properties are read from
     * <ul>
     * <li> the System properties, as specified on the command line; </li>
     * <li> MARY_BASE/conf/*.config </li>
     * </ul>
     * The system properties settings have precedence,
     * i.e. it is possible to override single config settings on the
     * command line.
     * <br>
     * This method will also do dependency checking, and propose to download
     * missing packages from the MARY installer.
     */
    public static void readProperties()
        throws Exception {
        // Throws all sorts of exceptions, each of them should lead to
        // a program halt: We cannot start up properly.
        
        File confDir = new File(maryBase()+"/conf");
        if (!confDir.exists()) {
            throw new FileNotFoundException("Configuration directory not found: "+ confDir.getPath());
        }
        File[] configFiles = confDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".config");
            }
        });
        assert configFiles != null;
        if (configFiles.length == 0) {
            throw new FileNotFoundException("No configuration files found in directory: "+ confDir.getPath()); 
        }
        List<Properties> allConfigs = readConfigFiles(configFiles);
        boolean allThere = checkDependencies(allConfigs);
        if (!allThere) System.exit(1);

        // Add properties from individual config files to global properties:
        // Global mary properties
        p = new Properties();
        for (Properties oneP : allConfigs) {
            for (Iterator keyIt=oneP.keySet().iterator(); keyIt.hasNext(); ) {
                String key = (String) keyIt.next();
                // Ignore dependency-related properties:
                if (key.equals("name") || key.equals("provides") || key.startsWith("requires")) {
                    continue;
                }
                String value = oneP.getProperty(key);
                // Properties with keys ending in ".list" are to be appended in global properties.
                if (key.endsWith(".list")) {
                    String prevValue = p.getProperty(key);
                    if (prevValue != null) {
                        value = prevValue + " " + value;
                    }
                }
                p.setProperty(key, value);
            }
        }
        // Overwrite settings from config files with those set on the
        // command line (System properties):
        p.putAll(System.getProperties());

        // Reciprocally, put all settings in p into the system properties
        // (to allow for non-Mary config settings, e.g. for tritonus).
        // If one day we find this to be too unflexible, we can also
        // identify a subset of properties to put into the System properties,
        // e.g. by prepending them with a "system." prefix. For now, this seems 
        // unnecessary.
        System.getProperties().putAll(p);

        // OK, now the individual settings
        // If necessary, derive shprot.base from mary.base:
        if (getProperty("shprot.base") == null) {
            p.setProperty("shprot.base", maryBase + File.separator + "lib" +
                File.separator + "modules" + File.separator + "shprot");
            System.setProperty("shprot.base", getProperty("shprot.base"));
        }
        String helperString;
        StringTokenizer st;

        Set<String> ignoreModuleClasses = new HashSet<String>();
        helperString = getProperty("ignore.modules.classes.list");
        if (helperString != null) {
            // allow whitespace and comma as list delimiters
            st = new StringTokenizer(helperString, ", \t\n\r\f");
            while (st.hasMoreTokens()) {
                ignoreModuleClasses.add(st.nextToken());
            }
        }
        
        helperString = needProperty("modules.classes.list");
        // allow whitespace and comma as list delimiters
        st = new StringTokenizer(helperString, ", \t\n\r\f");
        while (st.hasMoreTokens()) {
            String className = st.nextToken();
            if (!ignoreModuleClasses.contains(className))
                moduleClasses.add(className);
        }

        helperString = needProperty("synthesizers.classes.list");
        // allow whitespace and comma as list delimiters
        st = new StringTokenizer(helperString, ", \t\n\r\f");
        while (st.hasMoreTokens()) {
            synthClasses.add(st.nextToken());
        }

        String audioEffectClassName, audioEffectName, audioEffectParam, audioEffectSampleParam, audioEffectHelpText;
        helperString = getProperty("audioeffects.classes.list");
        if (helperString!=null)
        {
            // allow whitespace and comma as list delimiters
            st = new StringTokenizer(helperString, ", \t\n\r\f");
            while (st.hasMoreTokens()) {
                audioEffectClassName = st.nextToken();
                audioEffectClasses.add(audioEffectClassName);
                
                BaseAudioEffect ae = null;
                try {
                    ae = (BaseAudioEffect)Class.forName(audioEffectClassName).newInstance();
                } catch (InstantiationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                ae.setParams(ae.getExampleParameters());
                
                audioEffectName = ae.getName();
                audioEffectNames.add(audioEffectName);
                audioEffectParam = ae.getParamsAsString(false);
                audioEffectParams.add(audioEffectParam);
                audioEffectSampleParam = ae.getExampleParameters();
                audioEffectSampleParams.add(audioEffectParam);
                audioEffectHelpText = ae.getHelpText();
                audioEffectHelpTexts.add(audioEffectHelpText);
            }
        }

        helperString = getProperty("schema.local");
        if (helperString!=null)
        {
            st = new StringTokenizer(helperString);
            Vector<File> v = new Vector<File>();
            while (st.hasMoreTokens()) {
                String schemaFilename = expandPath(st.nextToken());
                File schemaFile = new File(schemaFilename);
                if (!schemaFile.canRead())
                    throw new Exception("Cannot read Schema file: " + schemaFilename);
                v.add(schemaFile);
            }
            localSchemas = v.toArray();
        }

        // Setup locale translation table:
        locale2prefix.put(Locale.ENGLISH, "english");
        locale2prefix.put(Locale.GERMAN, "german");
        locale2prefix.put(new Locale("tib"), "tibetan");
    }

    /**
     * Read the config files from the config directory and return them
     * as a list of Property objects
     * @return a list of property objects, each representing a config file.
     * Each property object has one key/value pair added: "configfile", which
     * points to the path on the filesystem from where the config file was loaded.
     * @throws Exception if problem occurred that impairs a proper system startup 
     */
    private static List<Properties> readConfigFiles(File[] configFiles) throws Exception
    {
        
        /////////////////// Read config files ////////////////////
        List<Properties> allConfigs = new LinkedList<Properties>();
        for (int i=0; i<configFiles.length; i++) {
            // Ignore config file?
            if (System.getProperty("ignore."+configFiles[i].getName()) != null) 
                continue;
            String path = configFiles[i].getPath();
            // Properties for one config file
            Properties oneP = new Properties();
            oneP.setProperty("configfile", path);
            oneP.load(new FileInputStream(configFiles[i]));
            allConfigs.add(oneP);
        }
        return allConfigs;
    }

    /**
     * Check dependencies between components, and try to download and install components
     * that are missing.
     * @param allConfigs a list of Properties objects as loaded by readConfigFiles().
     * @return true if all dependencies are OK
     * @throws Exception if a problem occurs
     */
    private static boolean checkDependencies(List<Properties> allConfigs)
    throws Exception
    {
        // Keep track of "requires" and "provides" settings, in order to check if
        // all requirements are satisfied.
        Map<String,List<Properties>> requiresMap = new HashMap<String,List<Properties>>(); // map a requirement to a list of components that have it
        Map<String,List<Properties>> providesMap = new HashMap<String,List<Properties>>(); // map a provided item to a list of components providing it
        for (Properties oneP : allConfigs) {
            // Build up dependency lists
            String name = oneP.getProperty("name");
            if (name == null) {
                throw new NoSuchPropertyException("In config file `"+oneP.getProperty("configfile")+"': property `name' missing.");
            }
            addToMapList(providesMap, name, oneP);
            String provides = oneP.getProperty("provides");
            if (provides != null) {
                StringTokenizer st = new StringTokenizer(provides);
                while (st.hasMoreTokens()) {
                    addToMapList(providesMap, st.nextToken(), oneP);
                }
            }
            String requires = oneP.getProperty("requires");
            if (requires != null) {
                StringTokenizer st = new StringTokenizer(requires);
                while (st.hasMoreTokens()) {
                    addToMapList(requiresMap, st.nextToken(), oneP);
                }
            }
        }

        // Resolve dependencies
        boolean allThere = true;
        for (String requirement : requiresMap.keySet()) {
            List<Properties> requirers = requiresMap.get(requirement);
            for (Properties requirer : requirers) {
                if (!providesMap.containsKey(requirement)) {
                    // Missing dependency
                    String component = tryToSolveDependencyProblem(requirement, requirer, "component is missing");
                    if (component != null) { // could solve one
                        // update classpath, in case a new jar file was installed
                        Mary.addJarsToClasspath();
                        // add new config file, re-check
                        File configFile = new File(maryBase+"/conf/"+component+".config");
                        assert configFile.exists();
                        allConfigs.addAll(readConfigFiles(new File[] {configFile}));
                        // recursive call
                        return checkDependencies(allConfigs);
                    } else { // failed, cannot solve
                        allThere = false;
                    }
                } else { // Component is there
                    // Check version
                    String reqVersion = requirer.getProperty("requires."+requirement+".version");
                    List<Properties> providers = providesMap.get(requirement);
                    for (Properties provider : providers) {
                        if (reqVersion != null) {
                            String version =  provider.getProperty(requirement+".version");
                            String problem = null;
                            if (version == null) { // bad configuration
                                problem = "no version number";
                            } else if (version.compareTo(reqVersion) < 0) { // version too small
                                problem = "version `"+version+"'"; 
                            } // else version OK
                            if (problem != null) {
                                String component = tryToSolveDependencyProblem(requirement, requirer,
                                        "version number "+reqVersion+" is required, and component `"+provider.getProperty("name")+"' provides "+problem);
                                if (component != null) { // could solve one
                                    // update classpath, in case a new jar file was installed
                                    Mary.addJarsToClasspath();
                                    // add new config file, re-check
                                    File configFile = new File(maryBase+"/conf/"+component+".config");
                                    assert configFile.exists();
                                    allConfigs.addAll(readConfigFiles(new File[] {configFile}));
                                    // recursive call
                                    allThere = checkDependencies(allConfigs);
                                } else { // failed, cannot solve
                                    allThere = false;
                                }
                            }
                        }
                        // Try to make sure that each provider is listed *before*
                        // the requirer in allConfigs -- this will affect the order
                        // in which modules are loaded.
                        int iProvider = allConfigs.indexOf(provider);
                        int iRequirer = allConfigs.indexOf(requirer);
                        assert iProvider >= 0;
                        assert iRequirer >= 0;
                        if (iProvider > iRequirer) {
                            allConfigs.remove(provider);
                            allConfigs.add(iRequirer, provider);
                        }
                    }
                }
            }
        }
        return allThere;
    }

    
    /**
     * Helper to map one key to a list of values.
     * @param map
     * @param key
     * @param listValue
     */
    private static final void addToMapList(Map<String,List<Properties>> map, String key, Properties listValue)
    {
        List<Properties> list;
        if (map.containsKey(key)) {
            list = map.get(key);
        } else {
            list = new ArrayList<Properties>();
            map.put(key, list);
        }
        list.add(listValue);
    }
    
    /**
     * For a missing component, try to solve the dependency problem.
     * @param missing name of the missing component
     * @param reqProps properties of the requirer
     * @param message description of the problem
     * @return a String representing the new component name if the problem could be fixed, e.g. by
     * installing the missing component; null on failure.
     */
    private static final String tryToSolveDependencyProblem(String missing, Properties reqProps, String message)
    {
        String requirer = reqProps.getProperty("name");
        String problem = "Component `"+missing+"' is required by `"+requirer+"',\n"+
        "but "+message+".";
        
        String component = reqProps.getProperty("requires."+missing+".download.package-name", missing).trim();
        String download = reqProps.getProperty("requires."+missing+".download");
        if (missing.contains("voice")) {
            int answer = JOptionPane.showConfirmDialog(null,
                    problem+"\n"+
                    "Would you like to download `"+ component +"'\nusing the MARY Voice Installer?",
                    "Dependency problem",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                // Try to install it via the voice installer
                // run(), not start(), so that the code is executed in the current thread
                new VoiceInstaller(component, false).run(); 
            }
        } else if (download != null) {
            int answer = JOptionPane.showConfirmDialog(null,
                    problem+"\n"+
                    "Would you like to download `"+ component +"' from\n" + download + "?",
                    "Dependency problem",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                System.getProperties().setProperty("licensepanel.title", "Installing "+component);
                try {
                    // TODO: The AutomatedInstaller in IzPack calls System.exit(),
                    // so this does not return -- replace with own AutomatedInstaller?
                    //InstallationUtils.directRunInstaller(new URL(download), component);
                    InstallationUtils.downloadAndRunInstaller(new URL(download), component);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
                if (new File(maryBase()+"/conf/"+component+".config").exists()) {
                    // apparently, the component has now been installed
                    return component;
                }
            }
            
        } else {
            // no download option, just inform.
            JOptionPane.showMessageDialog(null,
                    problem,
                    "Dependency problem",
                    JOptionPane.ERROR_MESSAGE);            
        }
        return null;
    }
    
    
	/**
	 * From a path entry in the properties, create an expanded form.
	 * Replace the string MARY_BASE with the value of property "mary.base";
	 * replace all "/" and "\\" with the platform-specific file separator.
	 */
	private static String expandPath(String path)
	{
		final String MARY_BASE = "MARY_BASE";
		StringBuffer buf = null;
		if (path.startsWith(MARY_BASE)) {
			buf = new StringBuffer(maryBase);
			buf.append(path.substring(MARY_BASE.length()));
		} else {
			buf = new StringBuffer(path);
		}
		if (File.separator.equals("/")) {
			int i = -1;
			while ((i = buf.indexOf("\\")) != -1)
				buf.replace(i, i+1, "/");
		} else if (File.separator.equals("\\")) {
			int i = -1;
			while ((i = buf.indexOf("/")) != -1)
				buf.replace(i, i+1, "\\");
		} else {
			throw new Error("Unexpected File.separator: `" + File.separator + "'");
		}
		return buf.toString();
	}

    /**
     * Get a property from the underlying properties.  
     * @param property the property requested
     * @return the property value if found, null otherwise.
     */
    public static String getProperty(String property)
    {
        return getProperty(property, null);
    }

    /**
     * Get a boolean property from the underlying properties.  
     * @param property the property requested
     * @return the boolean property value if found, false otherwise.
     */
    public static boolean getBoolean(String property)
    {
        return getBoolean(property, false);
    }

    /**
     * Get or infer a boolean property from the underlying properties.
     * Apart from the values "true"and "false", a value "auto" is permitted;
     * it will resolve to "true" in server mode and to "false" in non-server mode.  
     * @param property the property requested
     * @return the boolean property value if found, false otherwise.
     */
    public static boolean getAutoBoolean(String property)
    {
        return getAutoBoolean(property, false);
    }

    /**
     * Get an integer property from the underlying properties.  
     * @param property the property requested
     * @return the integer property value if found, -1 otherwise.
     */
    public static int getInteger(String property)
    {
        return getInteger(property, -1);
    }

    /**
     * Get a filename property from the underlying properties. The string MARY_BASE is
     * replaced with the value of the property mary.base, and path separators are adapted
     * to the current platform.
     * @param property the property requested
     * @return the filename corresponding to the property value if found, null otherwise.
     */
    public static String getFilename(String property)
    {
        return getFilename(property, null);
    }

	/**
	 * Get a Class property from the underlying properties.  
	 * @param property the property requested
	 * @return the Class property value if found and valid, null otherwise.
	 */
	public static Class getClass(String property)
	{
		return getClass(property, null);
	}

    /**
     * Get a property from the underlying properties.  
     * @param property the property requested
     * @param defaultValue the value to return if the property is not defined
     * @return the property value if found, defaultValue otherwise.
     */
    public static String getProperty(String property, String defaultValue)
    {
        if (p == null) return defaultValue;
        return p.getProperty(property, defaultValue);
    }

    /**
     * Get a boolean property from the underlying properties.  
     * @param property the property requested
     * @param defaultValue the value to return if the property is not defined
     * @return the boolean property value if found and valid, defaultValue otherwise.
     */
    public static boolean getBoolean(String property, boolean defaultValue)
    {
        if (p == null) return defaultValue;
        String value = p.getProperty(property);
        if (value == null)
            return defaultValue;
        try {
            boolean b = Boolean.valueOf(value).booleanValue();
            return b;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get or infer a boolean property from the underlying properties.
     * Apart from the values "true"and "false", a value "auto" is permitted;
     * it will resolve to "true" in server mode and to "false" in non-server mode.  
     * @param property the property requested
     * @param defaultValue the value to return if the property is not defined
     * @return the boolean property value if found and valid, false otherwise.
     */
    public static boolean getAutoBoolean(String property, boolean defaultValue)
    {
      if (p == null) return defaultValue;
      String value = p.getProperty(property);
        if (value == null)
            return defaultValue;
        if (value.equals("auto")) {
            return getBoolean("server");
        } else {
            return getBoolean(property, defaultValue);
        }
    }

    /**
     * Get a property from the underlying properties.  
     * @param property the property requested
     * @param defaultValue the value to return if the property is not defined
     * @return the integer property value if found and valid, defaultValue otherwise.
     */
    public static int getInteger(String property, int defaultValue)
    {
        if (p == null) return defaultValue;
        String value = p.getProperty(property);
        if (value == null)
            return defaultValue;
        try {
            int i = Integer.decode(value).intValue();
            return i;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get a filename property from the underlying properties. The string MARY_BASE is
     * replaced with the value of the property mary.base, and path separators are adapted
     * to the current platform.
     * @param property the property requested
     * @param defaultValue the value to return if the property is not defined
     * @return the filename corresponding to the property value if found, defaultValue otherwise.
     */
    public static String getFilename(String property, String defaultValue)
    {
        if (p == null) return defaultValue;
        String filename = p.getProperty(property);
        if (filename == null)
            return defaultValue;
        return expandPath(filename);
    }

	/**
	 * Get a Class property from the underlying properties.  
	 * @param property the property requested
	 * @param defaultValue the value to return if the property is not defined
	 * @return the property value if found, defaultValue otherwise.
	 */
	public static Class getClass(String property, Class defaultValue)
	{
        if (p == null) return defaultValue;
		String value = p.getProperty(property);
		if (value == null)
			return defaultValue;
		Class c = null;
		try {
			c = Class.forName(value);
		} catch (ClassNotFoundException e) {
			return defaultValue;
		}
		return c;
	}

    /**
     * Get a property from the underlying properties, throwing an exception if
     * it is not defined.
     * @param property the property required
     * @return the property value
     * @throws NoSuchPropertyException if the property is not defined.
     */
    public static String needProperty(String property) throws NoSuchPropertyException
    {
        String value = p.getProperty(property);
        if (value == null)
            throw new NoSuchPropertyException("Missing value `" + property + "' in configuration files");
        return value;
    }

    /**
     * Get a boolean property from the underlying properties, throwing an exception if
     * it is not defined.  
     * @param property the property requested
     * @return the boolean property value
     * @throws NoSuchPropertyException if the property is not defined.
     */
    public static boolean needBoolean(String property) throws NoSuchPropertyException
    {
        String value = p.getProperty(property);
        if (value == null)
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        try {
            boolean b = Boolean.valueOf(value).booleanValue();
            return b;
        } catch (NumberFormatException e) {
            throw new NoSuchPropertyException("Boolean property `" + property + "' in configuration files has wrong value `" + value + "'");
        }
    }
    
    /**
     * Get or infer a boolean property from the underlying properties, throwing an exception if
     * it is not defined.
     * Apart from the values "true"and "false", a value "auto" is permitted;
     * it will resolve to "true" in server mode and to "false" in non-server mode.  
     * @param property the property requested
     * @return the boolean property value
     * @throws NoSuchPropertyException if the property is not defined.
     */
    public static boolean needAutoBoolean(String property)
    throws NoSuchPropertyException
    {
        String value = p.getProperty(property);
        if (value == null)
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        if (value.equals("auto")) {
            return needBoolean("server");
        } else {
            return needBoolean(property);
        }
    }
    

    /**
     * Get an integer property from the underlying properties, throwing an exception if
     * it is not defined.  
     * @param property the property requested
     * @return the integer property value
     * @throws NoSuchPropertyException if the property is not defined.
     */
    public static int needInteger(String property) throws NoSuchPropertyException
    {
        String value = p.getProperty(property);
        if (value == null)
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        try {
            int i = Integer.decode(value).intValue();
            return i;
        } catch (NumberFormatException e) {
            throw new NoSuchPropertyException("Integer property `" + property + "' in configuration files has wrong value `" + value + "'");
        }
    }

    /**
     * Get a filename property from the underlying properties, throwing an exception if
     * it is not defined. The string MARY_BASE is
     * replaced with the value of the property mary.base, and path separators are adapted
     * to the current platform.
     * @param property the property requested
     * @return the filename corresponding to the property value
     * @throws NoSuchPropertyException if the property is not defined or the value is not a valid filename
     */
    public static String needFilename(String property) throws NoSuchPropertyException
    {
        String filename = expandPath(needProperty(property));
        if (!new File(filename).canRead()) {
            throw new NoSuchPropertyException("Cannot read file `" + filename +
                                "'. Check property `" + property +
								"' in configuration files");
        }
        return filename;
    }

	/**
	 * Get a Class property from the underlying properties, throwing an exception if
     * it is not defined.  
	 * @param property the property requested
	 * @return the Class corresponding to the property value
     * @throws NoSuchPropertyException if the property is not defined or the value is not a valid class
	 */
	public static Class needClass(String property)
	throws NoSuchPropertyException
	{
		String value = needProperty(property);
		Class c = null;
		try {
			c = Class.forName(value);
		} catch (ClassNotFoundException e) {
			throw new NoSuchPropertyException("Cannot find class `" + value +
				"'. Check property `" + property +
				"' in configuration files");
		}
		return c;
	}
    
    /**
     * Provide the config file prefix used for different locales in the
     * config files. Will return "german" for Locale.GERMAN, "english" for
     * Locale.US and Locale.ENGLISH, and "tibetan" for Locale("tib").
     * @param locale
     * @return
     */
    public static String localePrefix(Locale locale)
    {
        if (locale2prefix.containsKey(locale)) {
            return locale2prefix.get(locale);
        }
        for (Locale l : locale2prefix.keySet()) {
            if (MaryUtils.subsumes(l, locale)) {
                return locale2prefix.get(l);
            }
        }
        return null;
    }

}
