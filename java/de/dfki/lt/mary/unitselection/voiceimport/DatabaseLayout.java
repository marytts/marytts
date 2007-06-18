/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.voiceimport;


import java.util.*;
import java.io.*;

/**
 * The DatabaseLayout class registers the base directory of a voice database,
 * as well as the various subdirectories where the various voice database
 * components should be stored or read from.
 * 
 * @author Anna Hunecke
 *
 */
public class DatabaseLayout 
{   
    private String configFileName;
    private SortedMap props;
    private BasenameList bnl;
    private SortedMap localProps;
    private String fileSeparator;
    private VoiceImportComponent[] components;
    private String[] compNames;
    private Map compnames2comps;
    private SortedMap missingProps;
    private boolean initialized;
    private List uneditableProps;
    //marybase
    public final String MARYBASE = "db.marybase";
    //voicename
    public final String VOICENAME = "db.voicename";
    //gender
    public final String GENDER = "db.gender";
    //domain
    public final String DOMAIN  = "db.domain";
    //locale
    public final String LOCALE = "db.locale";
    //the sampling rate
    public final String SAMPLINGRATE = "db.samplingrate";
    //root directory for the database
    public final String ROOTDIR = "db.rootDir";        
    //directory for Mary config files
    public final String CONFIGDIR = "db.configDir";
    //directory for Mary voice files
    public final String FILEDIR = "db.fileDir";
    //mary file extension
    public final String MARYEXT = "db.maryExtension";  
    //basename list file
    public final String BASENAMEFILE = "db.basenameFile";
    //text file dir
    public final String TEXTDIR = "db.textDir";
    //text file extension
    public final String TEXTEXT = "db.textExtension";  
    //wav file dir
    public final String WAVDIR = "db.wavDir";
    //wav file extension
    public final String WAVEXT = "db.wavExtension";
    //phonetic label files
    public final String LABDIR = "db.labDir";
    //phonetic label file extension
    public final String LABEXT = "db.labExtension";
    //directory for temporary files
    public final String TEMPDIR = "db.tempDir";
    //maryxml dir
    public final String MARYXMLDIR = "db.maryxmlDir";
    //maryxml extentsion
    public final String MARYXMLEXT = "db.maryxmlExtension";
    //the help file for import main
    public final String MAINHELPFILE = "db.mainHelpFile";
    //the help file for the settings dialogue
    public final String SETTINGSHELPFILE = "db.settingsHelpFile";
    
    public DatabaseLayout(){
        initialized = false;
        initialize(new VoiceImportComponent[0]);
    }
    
    public DatabaseLayout(VoiceImportComponent[] comps){        
        initialized = false;
        initialize(comps);
    }
    
    public DatabaseLayout(VoiceImportComponent comp){        
        initialized = false;
        VoiceImportComponent[] comps = new VoiceImportComponent[1];
        comps[0] = comp;
        initialize(comps);        
    }
    
    private void initialize(VoiceImportComponent[] components){
        System.out.println("Loading database layout:");
        this.components = components;
        fileSeparator = System.getProperty("file.separator");
        uneditableProps = new ArrayList();
        uneditableProps.add(MAINHELPFILE);
        uneditableProps.add(SETTINGSHELPFILE);
        uneditableProps.add(MARYEXT);
        uneditableProps.add(CONFIGDIR);
        uneditableProps.add(FILEDIR);
        uneditableProps.add(TEMPDIR);
        /* check if there is a config file */
        //TODO: config file name as property or command line arg?
        configFileName = "./database.config";
        File configFile = new File(configFileName);
        getCompNames();
        if (configFile.exists()){
            
            System.out.println("Reading config file "+configFileName);
            readConfigFile(configFile);  
            SortedMap defaultGlobalProps = new TreeMap();
            //get the default values for the global props
            defaultGlobalProps = initDefaultProps(defaultGlobalProps,true);
            //get the local default props from the components
            SortedMap defaultLocalProps = getDefaultPropsFromComps();
            //try to get all props and values from config file
            if (!checkProps(defaultGlobalProps,defaultLocalProps)){
                //some props are missing                
                //prompt the user for the missing props
                //(user input updates the props via the GUI)
                promptUserForMissingProps();      
                //check if all dirs have a file separator at the end
                checkForFileSeparators();
                //save the props
                saveProps(configFile);
            } 
            //check if all dirs have a file separator at the end
            checkForFileSeparators();
        } else {
            //we have no values for our props
            props = new TreeMap();
            //prompt the user for some props
            promptUserForBasicProps(props);
            //fill in the other props with default values
            props = initDefaultProps(props,false);
            //get the local default props from the components
            localProps = getDefaultPropsFromComps();
            //check if all dirs have a file separator at the end
            checkForFileSeparators();
            //save the props
            saveProps(configFile);
        }
        assureFileIntegrity();
        loadBasenameList();
        initializeComps();
        initialized = true;
    }
    
    
     /**
     * Get the names of the components
     * and store them in array
     */
    private void getCompNames(){
        compnames2comps = new HashMap();
        compNames = new String[components.length];
        for (int i=0;i<components.length;i++){
            compNames[i] = components[i].getName();
            compnames2comps.put(compNames[i],components[i]);
        }        
    }
    
    /**
     * Read the props in the config file
     * @param configFile the config file
     */
    private void readConfigFile(File configFile){
        props = new TreeMap();
        localProps = new TreeMap();
        try{
            //open the file
            BufferedReader in =
                new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(configFile),"UTF-8"));
            String line;
            while ((line=in.readLine())!= null){
                if (line.startsWith("#")
                        || line.equals(""))
                    continue;
                //System.out.println(line);
                //line looks like "<propName> <value>"
                //<propname> looks like "<compName>.<prop>"
                String[] lineSplit = line.split(" ");
                if (lineSplit[0].startsWith("db.")){
                   //global prop
                    props.put(lineSplit[0],lineSplit[1]);                    
                } else {
                    //local prop
                    String compName = 
                        lineSplit[0].substring(0,lineSplit[0].indexOf('.'));
                    if (localProps.containsKey(compName)){
                        SortedMap localPropMap = (SortedMap) localProps.get(compName);
                        localPropMap.put(lineSplit[0],lineSplit[1]);
                    } else {
                        SortedMap localPropMap = new TreeMap();
                        localPropMap.put(lineSplit[0],lineSplit[1]);
                        localProps.put(compName,localPropMap);
                    }                    
                }
            }
            in.close(); 
            //add the props that are not editable
            SortedMap defaultGlobalProps = new TreeMap();
            //get the default values for the global props
            defaultGlobalProps = initDefaultProps(defaultGlobalProps,false);
            for (Iterator it=uneditableProps.iterator();it.hasNext();){
                String key = (String) it.next();
                if (defaultGlobalProps.containsKey(key)){
                    props.put(key, defaultGlobalProps.get(key));
                } else {
                    //this case should never happen -> Error
                    throw new Error("Not editable global prop "+key
                            +" not defined in default props.");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new Error("Error reading config file");
        }   
    }
    
   
    
    /**
     * Check if all props are set
     * @param defaultGlobalProps default global props
     * @param defaultLocalProps default local props
     * @return true if all props are set, false otherwise
     */
    private boolean checkProps(SortedMap defaultGlobalProps,SortedMap defaultLocalProps){
        boolean allFine = true;
        /* check the global props */
        missingProps = new TreeMap();
        Set defaultProps = defaultGlobalProps.keySet();
        for (Iterator it = defaultProps.iterator();it.hasNext();){
            String key = (String) it.next();
            if (!props.containsKey(key)){
                missingProps.put(key,defaultGlobalProps.get(key));
                allFine = false;
            } else {
                //make sure all dir names have a / at the end
                if (key.endsWith("Dir")){
                    String prop = (String)props.get(key);
                    if (!prop.endsWith(fileSeparator)){
                        prop = prop+fileSeparator;
                        props.put(key,prop);
                    }
                }
            }
            
        }
        /* check the local props */
        defaultProps = defaultLocalProps.keySet();
        for (Iterator it = defaultProps.iterator();it.hasNext();){
            String key = (String) it.next();
            if (!localProps.containsKey(key)){
                missingProps.put(key,defaultLocalProps.get(key));
                allFine = false;
            } else {
                SortedMap nextLocalPropMap = 
                    (SortedMap) localProps.get(key);
                SortedMap nextDefaultLocalPropMap = 
                    (SortedMap) defaultLocalProps.get(key);
                Set nextDefaultLocalProps = nextDefaultLocalPropMap.keySet();
                boolean haveAllLocalProps = true;
                SortedMap missingLocalPropMap = new TreeMap();
                for (Iterator it2 = nextDefaultLocalProps.iterator();it2.hasNext();){
                    String nextKey = (String) it2.next();
                    if (!nextLocalPropMap.containsKey(nextKey)){
                        missingLocalPropMap.put(nextKey,nextDefaultLocalPropMap.get(nextKey));
                        haveAllLocalProps = false;
                    }else {
                        //make sure all dir names have a / at the end
                        if (nextKey.endsWith("Dir")){
                            String prop = (String)nextLocalPropMap.get(nextKey);
                            if (!prop.endsWith(fileSeparator)){
                                prop = prop+fileSeparator;
                                nextLocalPropMap.put(key,prop);
                            }
                        }
                    }
                }
                if (!haveAllLocalProps){
                    missingProps.put(key,missingLocalPropMap);
                    allFine = false;
                }
            }
        }        
        return allFine;
    }
    
    /**
     * Prompt the user for the props that are missing
     */
    private void promptUserForMissingProps(){
        displayProps(missingProps,"The following properties are missing:");
    }
    
    
    
    private void checkForFileSeparators(){
        
        /* check the global props */       
        Set propKeys = props.keySet();
        for (Iterator it = propKeys.iterator();it.hasNext();){
            String key = (String) it.next();
            //make sure all dir names have a / at the end
            if (key.endsWith("Dir")){
                String prop = (String)props.get(key);
                if (!prop.endsWith(fileSeparator)){
                    prop = prop+fileSeparator;
                    props.put(key,prop);
                }
            }            
        }
        /* check the local props */
        Set localPropKeys = localProps.keySet();
        for (Iterator it = localPropKeys.iterator();it.hasNext();){
            SortedMap nextLocalPropMap = 
                (SortedMap) localProps.get(it.next());
            for (Iterator it2 = nextLocalPropMap.keySet().iterator();it2.hasNext();){
                String nextKey = (String) it2.next();
                //make sure all dir names have a / at the end
                if (nextKey.endsWith("Dir")){
                    String prop = (String)nextLocalPropMap.get(nextKey);
                    if (!prop.endsWith(fileSeparator)){
                        prop = prop+fileSeparator;
                        nextLocalPropMap.put(nextKey,prop);
                    }
                }                
            }            
        }
    }
        
    
    
    /**
     * Save the props and their values in config file
     * @param configFile the config file
     */
    private void saveProps(File configFile){
        try{
            PrintWriter out = 
                new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(configFile),"UTF-8"),true);
           
            out.println("# GlobalProperties:");
            Set globalPropSet = props.keySet();
            for (Iterator it=globalPropSet.iterator();it.hasNext();){
                String key = (String) it.next();
                if (isEditable(key)){
                    out.println(key+" "+props.get(key));      
                }
            }
            out.println();     
             StringBuffer outBuf = new StringBuffer();
            for (int i=0;i<compNames.length;i++){
                String key = compNames[i];
                SortedMap nextProps = (SortedMap) localProps.get(key);                
                outBuf.append("# Properties for module "+key+":\n");
                Set propSet = nextProps.keySet();
                for (Iterator it2=propSet.iterator();it2.hasNext();){
                    String localKey = (String) it2.next();
                    outBuf.append(localKey+" "+nextProps.get(localKey)+"\n");                
                }
                outBuf.append("\n");
            }
            out.print(outBuf.toString());
            out.close();
        } catch(Exception e){
            e.printStackTrace();
            throw new Error("Error writing config file");
        }
    
    
    }
    
    /**
     * Prompt the user for the basic props
     * (This is called if we don't have any props)
     * @param props the map of props to be filled
     */
    private void promptUserForBasicProps(SortedMap basicprops){
        //fill in the map with the prop names and value templates
        basicprops.put(MARYBASE,"/path/to/marybase");
        basicprops.put(VOICENAME,"<name of your voice>");
        basicprops.put(GENDER,"<female or male>");
        basicprops.put(DOMAIN,"<general or limited>");
        basicprops.put(LOCALE,"<de or en>");
        basicprops.put(SAMPLINGRATE,"<sampling rate of wave files>");
        basicprops.put(ROOTDIR,"/path/to/voicedirectory");
        basicprops.put(WAVDIR,"/path/to/wavefiles");
        basicprops.put(WAVEXT,"<extension of your wav files, e.g., .wav>");
        basicprops.put(LABDIR,"/path/to/labelfiles");
        basicprops.put(LABEXT,"<extension of your lab files, e.g., .lab>");        
        basicprops.put(TEXTDIR,"/path/to/transcriptfiles");
        basicprops.put(TEXTEXT,"<extension of your transcript files, e.g., .txt>");
        displayProps(basicprops,"Enter the basic properties of your voice:");
    }
    
    /**
     * Init the default props of the database layout
     * (the props that are not set during promptUserForBasicProps)
     * @param props the map of props to be filled
     * @return the map of default props
     */
    private SortedMap initDefaultProps(SortedMap props,boolean withBasicProps){
        if (withBasicProps){
            props.put(MARYBASE,"/path/to/marybase/");
            props.put(VOICENAME,"<name of your voice>");
            props.put(GENDER,"<female or male>");
            props.put(DOMAIN,"<general or limited>");
            props.put(LOCALE,"<de or en>");
            props.put(SAMPLINGRATE,"<sampling rate of wave files>");
            props.put(ROOTDIR,"/path/to/voicedirectory/");
            props.put(WAVDIR,"/path/to/wavefiles");
            props.put(WAVEXT,"<extension of your wav files, e.g., .wav>");
            props.put(LABDIR,"/path/to/labelfiles");
            props.put(LABEXT,"<extension of your lab files, e.g., .lab>");
            props.put(TEXTDIR,"/path/to/transcriptfiles");
            props.put(TEXTEXT,"<extension of your transcript files, e.g., .txt>");
        }        
        String rootDir = getProp(ROOTDIR);        
        props.put(CONFIGDIR,rootDir+"mary_configs"+fileSeparator);
        props.put(FILEDIR,rootDir+"mary_files"+fileSeparator);
        props.put(MARYEXT,".mry");
        props.put(BASENAMEFILE,rootDir+"basenames.lst");        
        props.put(TEMPDIR,rootDir+"temp"+fileSeparator);
        props.put(MARYXMLDIR,rootDir+"rawmaryxml"+fileSeparator);
        props.put(MARYXMLEXT,".xml");  
        props.put(MAINHELPFILE,getProp(MARYBASE)+"lib/modules/import/help_import_main.txt");
        props.put(SETTINGSHELPFILE,getProp(MARYBASE)+"lib/modules/import/help_settings.txt");
        return props;
    }
    
    
    
    /**
     * Get the default props+values from the components
     * @return the default props of the components
     */
    private SortedMap getDefaultPropsFromComps(){
        SortedMap localProps = new TreeMap();
        for (int i=0;i<components.length;i++){
            VoiceImportComponent nextComp = components[i];
            SortedMap nextProps = nextComp.getDefaultProps(this);
            //get the name of the component
            String name = nextComp.getName();
            localProps.put(name,nextProps);
        }
        return localProps;
    }
    
    /**
     * Make sure that we have all files and dirs
     * that we will need
     */
    private void assureFileIntegrity(){
        /* check root dir */
        checkDir(ROOTDIR);
        /* check file dir */
        checkDir(FILEDIR);
        /* check config dir */
        checkDir(CONFIGDIR);
        /* check temp dir */
        checkDir(TEMPDIR);
        /* check maryxml dir */
        checkDir(MARYXMLDIR);
        /* check text dir */  
        checkDir(TEXTDIR);        
        /* check wav dir */
        File dir = new File(getProp(WAVDIR));
        if (!dir.exists()){
            throw new Error("WAVDIR "+getProp(WAVDIR)+" does not exist!");
        }
        if (!dir.isDirectory()){
            throw new Error("WAVDIR "+getProp(WAVDIR)+" is not a directory!");
        }
        /* check lab dir */
        checkDir(LABDIR);
        dir = new File(getProp(LABDIR));
        
    }
    
    /**
     * Test if a directory exists
     * and try to create it if not;
     * throws an error if the dir
     * can not be created
     * @param propname the prop containing the name of the dir
     */
    private void checkDir(String propname){
        File dir = new File(getProp(propname));
        if (!dir.exists()){
            System.out.print(propname+" "+getProp(propname)
                    +" does not exist; ");
            if (!dir.mkdir()){
                throw new Error("Could not create "+propname);
            }
            System.out.print("Created successfully.\n");
        }  
        if (!dir.isDirectory()){
            throw new Error(propname+" "+getProp(LABDIR)+" is not a directory!");
        }        
    }
    
    /**
     * Load the basenamelist
     */
    private void loadBasenameList(){
        //test if basenamelist file exists
        File basenameFile = new File(getProp(BASENAMEFILE));
        if (!basenameFile.exists()){
            //make basename list from wav files 
            System.out.println("Loading basename list from wav files");
            bnl = new BasenameList(getProp(WAVDIR),getProp(WAVEXT));
        } else {
            //load basename list from file
            try{
                System.out.println("Loading basename list from file "
                        +getProp(BASENAMEFILE));
                bnl = new BasenameList(getProp(BASENAMEFILE));
            }catch (IOException ioe){
                throw new Error("Error loading basenames from file "
                        +getProp(BASENAMEFILE)+": "+ioe.getMessage());            
            }
        }
        System.out.println("Found "+bnl.getLength()+" files in basename list");
    }
    
    /**
     * Initialize the components
     */
    private void initializeComps(){
        for (int i=0;i<components.length;i++){
            SortedMap nextProps =
                (SortedMap) localProps.get(compNames[i]);
            components[i].initialise(bnl,nextProps);
        }
    }
    
    public String getProp(String prop){
        return (String)props.get(prop);
    }
    
    public void setProp(String prop, String val){
        props.put(prop,val);
    }
    
    public boolean isEditable(String propname){
        return !uneditableProps.contains(propname);
    }
    
    /**
     * Get all props of all components
     * as an Array representation
     * for displaying with the SettingsGUI.
     * Does not include uneditable props.
     */
    public String[][] getAllPropsForDisplay(){
        List keys = new ArrayList();
        List values = new ArrayList();
        for (Iterator it = props.keySet().iterator();it.hasNext();){
            String key = (String) it.next();
            if (isEditable(key)){
                keys.add(key);
                values.add(props.get(key));
            }
        }
        for (int i=0;i<compNames.length;i++){
            SortedMap nextProps = (SortedMap)localProps.get(compNames[i]);
            for (Iterator it = nextProps.keySet().iterator();it.hasNext();){
                String key = (String) it.next();
                keys.add(key);
                values.add(nextProps.get(key));
            }
        }
        String[][] result = new String[keys.size()][];
        for (int i=0;i<result.length;i++){
            String[] keyAndValue = new String[2];
            keyAndValue[0]=(String)keys.get(i);
            keyAndValue[1]=(String)values.get(i);
            result[i] = keyAndValue;
        }
        return result;
    }
    
    /**
     * Update the old props with the given props
     * @param newprops the new props 
     */
    public void updateProps(String[][] newprops){
        for (int i=0;i<newprops.length;i++){
            String[] keyAndValue = newprops[i];
            String key = keyAndValue[0];
            String value = keyAndValue[1];
            //find out if this is a global or a local prop
            if (key.startsWith("db.")){
                //global prop
                if (isEditable(key))
                    setProp(key,value);
            } else {
                //local prop: get the name of the component
                String compName = key.substring(0,key.indexOf('.'));
                //update our representation of local props for this component
                if (localProps.containsKey(compName)){
                    ((SortedMap) localProps.get(compName)).put(key,value);
                } else {
                    SortedMap keys2values = new TreeMap();
                    keys2values.put(key,value);
                    localProps.put(compName,keys2values);
                    
                }
                //update the representation of props in the component
                ((VoiceImportComponent) 
                        compnames2comps.get(compName)).setProp(key,value);
            }           
        }       
        //finally, save everything in config file
        if (initialized){
            saveProps(new File(configFileName));
        }
    }
    
    public void initialiseComponent(VoiceImportComponent vic){
        String name = vic.getName();
        SortedMap defaultProps = vic.getDefaultProps(this);
        if (!compnames2comps.containsKey(name)){
            System.out.println("comp "+name+" not in db");
            displayProps(defaultProps,"The following properties are missing :");
            saveProps(new File(configFileName));
        }
        vic.initialise(bnl,(SortedMap) localProps.get(name));
     }
    
    public BasenameList getBasenames(){
        return bnl;
    }
    
    private void displayProps(SortedMap props, String text){
        try{
           new SettingsGUI().display(this, props,text);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Can not display props");
        }
        
    }
        
}


