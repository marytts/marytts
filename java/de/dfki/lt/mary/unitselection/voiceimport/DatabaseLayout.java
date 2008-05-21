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
    private String missingPropsHelp;
    private boolean initialized;
    private List uneditableProps;
    private Map props2Help;
    //marybase
    public final String MARYBASE = "db.marybase";
    //marybase version
    public final String MARYBASEVERSION = "db.marybaseversion";
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
    
    private void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(BASENAMEFILE,"file containing the list of files that are used to build the voice");
        props2Help.put(DOMAIN,"general or limited");
        props2Help.put(GENDER,"female or male");
        props2Help.put(LABDIR,"directory containing the label files. Will be created if it does not exist.");
        props2Help.put(LABEXT,"extension of the label files, default: \".lab\"");
        props2Help.put(LOCALE,"de, en or en_US");
        props2Help.put(MARYBASE,"directory containing the local Mary installation");
        props2Help.put(MARYBASEVERSION,"local Mary installation version");
        props2Help.put(MARYXMLDIR,"directory containing maryxml representations of the transcripts. Will be created if it does not exist.");
        props2Help.put(MARYXMLEXT,"extension of the maryxml files, default: \".xml\"");
        props2Help.put(ROOTDIR,"directory in which all the files created during installation will be stored. Will be created if it does not exist.");
        props2Help.put(SAMPLINGRATE,"the sampling rate of the wave files, default: \"16000\"");
        props2Help.put(TEXTDIR,"directory containing the transcript files. Will be created if it does not exist.");
        props2Help.put(TEXTEXT,"extension of the transcript files, default: \".txt\"");
        props2Help.put(VOICENAME,"the name of the voice, one word, for example: \"my_voice\"");
        props2Help.put(WAVDIR,"directory containing the wave files. If it does not exist, an Error is thrown.");
        for (int i=0;i<components.length;i++){
            components[i].setupHelp();
        }
    }
    
    private void initialize(VoiceImportComponent[] components){
        System.out.println("Loading database layout:");
        
        /* first, handle the components */     
        this.components = components;      
        getCompNames();  
        /* initialize the help texts */
        setupHelp();
        
        fileSeparator = System.getProperty("file.separator");
        /* define the uneditable props */
        uneditableProps = new ArrayList();
        uneditableProps.add(MARYEXT);
        uneditableProps.add(CONFIGDIR);
        uneditableProps.add(FILEDIR);
        uneditableProps.add(TEMPDIR);
        uneditableProps.add(WAVEXT);
        
        /* check if there is a config file */
        configFileName = System.getProperty("user.dir")+System.getProperty("file.separator")+"database.config";
        File configFile = new File(configFileName);
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
                if (!displayProps(missingProps,missingPropsHelp,"The following properties are missing:"))
                    return;
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
            if (!promptUserForBasicProps(props))
                return;
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
                String[] lineSplit = line.split(" ",2);
                if (lineSplit[0].startsWith("db.")){
                   //global prop
                    props.put(lineSplit[0],lineSplit[1]);                    
                } else {
                    //local prop
                    String compName = lineSplit[0].substring(0,lineSplit[0].indexOf('.'));
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
        missingProps = new TreeMap();
        StringBuffer helpTextBuf = new StringBuffer();
        helpTextBuf.append("<html>\n<head>\n<title>SETTINGS HELP</title>\n"
                +"</head>\n<body>\n<dl>\n");
        /* check the local props */
        Set defaultProps = defaultLocalProps.keySet();
        for (Iterator it = defaultProps.iterator();it.hasNext();){
            String key = (String) it.next();
            if (!localProps.containsKey(key)){
                VoiceImportComponent comp = (VoiceImportComponent) compnames2comps.get(key);
                SortedMap nextLocalPropMap = 
                    (SortedMap) defaultLocalProps.get(key);
                missingProps.put(key,nextLocalPropMap);
                for (Iterator it2=nextLocalPropMap.keySet().iterator();it2.hasNext();){
                    String nextKey = (String) it2.next();                    
                    helpTextBuf.append("<dt><strong>"+nextKey+"</strong></dt>\n"
                            +"<dd>"+comp.getHelpTextForProp(nextKey)+"</dd>\n");
                }
                allFine = false;
            } else {
                VoiceImportComponent comp = 
                    (VoiceImportComponent) compnames2comps.get(key);
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
                        helpTextBuf.append("<dt><strong>"+nextKey+"</strong></dt>\n"
                                +"<dd>"+comp.getHelpTextForProp(nextKey)+"</dd>\n");
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
        /* check the global props */        
        defaultProps = defaultGlobalProps.keySet();
        for (Iterator it = defaultProps.iterator();it.hasNext();){
            String key = (String) it.next();
            if (!props.containsKey(key)){
                missingProps.put(key,defaultGlobalProps.get(key));
                 helpTextBuf.append("<dt><strong>"+key+"</strong></dt>\n"
                    +"<dd>"+props2Help.get(key)+"</dd>\n");
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
        helpTextBuf.append("</dl>\n</body>\n</html>");
        missingPropsHelp = helpTextBuf.toString();
        return allFine;
    }    
    
    
    private void checkForFileSeparators(){
        
        /* check the global props */       
        Set propKeys = props.keySet();
        for (Iterator it = propKeys.iterator();it.hasNext();){
            String key = (String) it.next();
            //make sure all dir names have a / at the end
            if (key.endsWith("Dir")){
                String prop = (String)props.get(key);
                char lastChar = prop.charAt(prop.length()-1);
                if (Character.isLetterOrDigit(lastChar)){
                    props.put(key,prop+fileSeparator);
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
                    char lastChar = prop.charAt(prop.length()-1);
                    if (Character.isLetterOrDigit(lastChar)){
                        nextLocalPropMap.put(nextKey,prop+fileSeparator);
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
    private boolean promptUserForBasicProps(SortedMap basicprops){
        //fill in the map with the prop names and value templates
        String marybase = System.getProperty("MARYBASE");
        if ( marybase == null ) {
            marybase = "/path/to/marybase/";
        }
        basicprops.put(MARYBASE,marybase);
        basicprops.put(MARYBASEVERSION, "3.6.0");
        basicprops.put(VOICENAME,"my_voice");
        basicprops.put(GENDER,"female");
        basicprops.put(DOMAIN,"general");
        basicprops.put(LOCALE,"de");
        basicprops.put(SAMPLINGRATE,"16000");
        String rootDir = new File(".").getAbsolutePath();
        basicprops.put(ROOTDIR,rootDir.substring(0,rootDir.length()-1));
        basicprops.put(WAVDIR,"wav/");
        basicprops.put(LABDIR,"lab/");
        basicprops.put(LABEXT,".lab");        
        basicprops.put(TEXTDIR,"text/");
        basicprops.put(TEXTEXT,".txt");
        
        StringBuffer helpText = new StringBuffer();
        helpText.append("<html>\n<head>\n<title>SETTINGS HELP</title>\n"
                +"</head>\n<body>\n<dl>\n");
        for (Iterator it=props2Help.keySet().iterator();it.hasNext();){
            String key = (String) it.next();
            String value = (String) props2Help.get(key);
            helpText.append("<dt><strong>"+key+"</strong></dt>\n"
                    +"<dd>"+value+"</dd>\n");
        }
        
        helpText.append("</dl>\n</body>\n</html>");
        
        if (!displayProps(basicprops,
                helpText.toString(),
                "Please adjust the following settings:")){
        	return false;
        } else {
            return true;
        }
    }
    
    /**
     * Init the default props of the database layout
     * (the props that are not set during promptUserForBasicProps)
     * @param props the map of props to be filled
     * @return the map of default props
     */
    private SortedMap initDefaultProps(SortedMap props,boolean withBasicProps){
        if (withBasicProps){
            String marybase = System.getProperty("MARYBASE");
            if ( marybase == null ) {
                marybase = "/path/to/marybase/";
            }
            props.put(MARYBASE,marybase);
            props.put(VOICENAME,"my_voice");
            props.put(GENDER,"female");
            props.put(DOMAIN,"general");
            props.put(LOCALE,"de");
            props.put(SAMPLINGRATE,"16000");
            String rootDir = new File(".").getAbsolutePath();
            props.put(ROOTDIR,rootDir.substring(0,rootDir.length()-1));
            props.put(WAVDIR,"wav/");
            props.put(LABDIR,"lab/");
            props.put(LABEXT,".lab");        
            props.put(TEXTDIR,"text/");
            props.put(TEXTEXT,".txt");
        }        
        String rootDir = getProp(ROOTDIR);
        char lastChar = rootDir.charAt(rootDir.length()-1);
        if (Character.isLetterOrDigit(lastChar)){
            props.put(ROOTDIR,rootDir+fileSeparator);
        }
        
        props.put(CONFIGDIR,rootDir+"mary_configs"+fileSeparator);
        props.put(FILEDIR,rootDir+"mary_files"+fileSeparator);
        props.put(MARYEXT,".mry");
        props.put(BASENAMEFILE,rootDir+"basenames.lst");        
        props.put(TEMPDIR,rootDir+"temp"+fileSeparator);
        props.put(MARYXMLDIR,rootDir+"rawmaryxml"+fileSeparator);
        props.put(MARYXMLEXT,".xml"); 
        props.put(WAVEXT,".wav");
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
        //checkDir(TEXTDIR);
        checkDirinCurrentDir(TEXTDIR);
        /* check wav dir */
        File dir = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+getProp(WAVDIR));
        //System.out.println(System.getProperty("user.dir")+System.getProperty("file.separator")+getProp(WAVDIR));
        if (!dir.exists()){
            throw new Error("WAVDIR "+getProp(WAVDIR)+" does not exist!");
        }
        if (!dir.isDirectory()){
            throw new Error("WAVDIR "+getProp(WAVDIR)+" is not a directory!");
        }
        /* check lab dir */
        //checkDir(LABDIR);
        checkDirinCurrentDir(LABDIR);
        //dir = new File(getProp(LABDIR));
        
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
            throw new Error(propname+" "+getProp(propname)+" is not a directory!");
        }        
    }
    
    /**
     * Test if a directory exists
     * and try to create it if not;
     * throws an error if the dir
     * can not be created
     * @param propname the prop containing the name of the dir
     */
    private void checkDirinCurrentDir(String propname){
        File dir = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+getProp(propname));
        if (!dir.exists()){
            System.out.print(propname+" "+getProp(propname)
                    +" does not exist; ");
            if (!dir.mkdir()){
                throw new Error("Could not create "+propname);
            }
            System.out.print("Created successfully.\n");
        }  
        if (!dir.isDirectory()){
            throw new Error(propname+" "+getProp(propname)+" is not a directory!");
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
            bnl = new BasenameList(System.getProperty("user.dir")+System.getProperty("file.separator")+getProp(WAVDIR),getProp(WAVEXT));
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
    
    public boolean isInitialized(){
        return initialized;
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
            vic.setupHelp();
            if (!displayProps(defaultProps,vic.getHelpText(),"The following properties are missing:"))
                return;
            saveProps(new File(configFileName));
        }
        vic.initialise(bnl,(SortedMap) localProps.get(name));
     }
    
    public BasenameList getBasenames(){
        return bnl;
    }
    
    public String[] getCompNamesForDisplay(){
        String[] names = new String[compNames.length+1];
        names[0] = "Global properties";
        String[] sortedCompNames = new String[compNames.length];
        System.arraycopy(compNames,0,sortedCompNames,0,compNames.length);
        Arrays.sort(sortedCompNames);
        for (int i=1;i<names.length;i++){
             names[i] = sortedCompNames[i-1];
        }
        return names;
    }
    
    private boolean displayProps(SortedMap props, String helpText, String guiText){
        try{
            SettingsGUI gui = 
                new SettingsGUI(this, props,helpText,guiText);
            return gui.wasSaved();
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Can not display props");
            return false;
        }
        
    }
    
    public Map getComps2HelpText(){
        Map comps2HelpText = new HashMap();
        StringBuffer helpText = new StringBuffer();
        helpText.append("<html>\n<head>\n<title>SETTINGS HELP</title>\n"
                +"</head>\n<body>\n<dl>\n");
        
        for (Iterator it=props2Help.keySet().iterator();it.hasNext();){
            String key = (String) it.next();
            String value = (String) props2Help.get(key);
            helpText.append("<dt><strong>"+key+"</strong></dt>\n"
                    +"<dd>"+value+"</dd>\n");
        }
        
        helpText.append("</dl>\n</body>\n</html>");
        comps2HelpText.put("Global properties",helpText.toString());
        for (int i=0;i<components.length;i++){
            comps2HelpText.put(compNames[i],components[i].getHelpText());
        }
        return comps2HelpText;
    }
        
}


