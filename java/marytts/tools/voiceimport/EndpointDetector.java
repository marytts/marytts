package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import marytts.client.MaryClient;
import marytts.datatypes.MaryDataType;
import marytts.util.io.FileUtils;



/**
 * Identify and Remove End-ponints (intitial and final silences) from
 * given set of wave files.
 * @author Sathish and Oytun
 *  
 */
public class EndpointDetector extends VoiceImportComponent
{
    
    protected File textDir;
    protected File inputWavDir;
    protected File outputWavDir;
    protected String waveExt = ".wav";
    private BasenameList bnlist;
    
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public String INPUTWAVDIR = "EndpointDetector.inputWaveDirectory";
    public String OUTPUTWAVDIR = "EndpointDetector.outputWaveDirectory";
     
   public String getName(){
        return "EndpointDetector";
    }
     
   public void initialiseComp()
    {     

    }
     
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
         if (props == null){
             props = new TreeMap();
             props.put(INPUTWAVDIR, db.getProp(db.ROOTDIR)
                     +"inputwav"
                     +System.getProperty("file.separator"));
             props.put(OUTPUTWAVDIR, db.getProp(db.ROOTDIR)
                     +"outputwav"
                     +System.getProperty("file.separator"));
             
         } 
         return props;
     }
     
    protected void setupHelp(){
         props2Help = new TreeMap();
         props2Help.put(INPUTWAVDIR, "input wave files directory."); 
                 
         props2Help.put(OUTPUTWAVDIR, "output directory to store initial-end silences removed wave files." 
                 +"Will be created if it does not exist");
         
     }
     
    
    public boolean compute() throws IOException
    {
        //      Check existance of input directory  
        inputWavDir = new File(getProp(INPUTWAVDIR));
        if (!inputWavDir.exists()){
            throw new Error("Could not find input Directory: "+ getProp(INPUTWAVDIR));
        }   
        
        // Check existance of output directory
        // if not exists, create a new directory
        outputWavDir = new File(getProp(OUTPUTWAVDIR));
        if (!outputWavDir.exists()){
            System.out.print(OUTPUTWAVDIR+" "+getProp(OUTPUTWAVDIR)
                    +" does not exist; ");
            if (!outputWavDir.mkdir()){
                throw new Error("Could not create OUTPUTWAVDIR");
            }
            System.out.print("Created successfully.\n");
        }
        
        // Automatically collect all ".wav" files from given directory
        bnlist = new BasenameList(inputWavDir+File.separator,waveExt);
        
        System.out.println( "Removing endpoints for "+ bnlist.getLength() + " wave files" );
        for (int i=0; i<bnlist.getLength(); i++) {
            percent = 100*i/bnlist.getLength();
            String inputFile = inputWavDir + File.separator + bnlist.getName(i) + waveExt;
            String outputFile = outputWavDir + File.separator + bnlist.getName(i) + waveExt;
            removeEndponits(inputFile,outputFile);
            System.out.println( "    " + bnlist.getName(i) );
        }
        System.out.println("...Done.");
        return true;
    }

    
    /**
     * Removes endpoints from given file
     * @param inputFile 
     * @param outputFile
     * @throws IOException
     */
    public void removeEndponits(String inputFile, String outputFile) throws IOException
    {
        /**TODO
         * Add corresponding module to remove endpoints
         * 1. identify and remove end points
         * 2. store as output wavefile 
         */
        
        
        
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

}
