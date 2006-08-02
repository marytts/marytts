/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.unitselection.clunits;

import java.io.*;

import java.util.*;

import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Level;

import de.dfki.lt.mary.unitselection.cart.CART;


import de.dfki.lt.mary.unitselection.*;
import de.dfki.lt.mary.unitselection.cart.CARTImpl;
import de.dfki.lt.mary.unitselection.featureprocessors.UtteranceFeatProcManager;
import de.dfki.lt.mary.unitselection.featureprocessors.UnitSelectionFeatProcManager;


public class ClusterUnitDatabase extends UnitDatabase
{ 
    public final static int CLUNIT_NONE = 65535;
        
    private UtteranceFeatProcManager featureProcessors;
    private String voice;
       
    
    private TimeLine audio;
    private Unit[] units;
    private Map cartMap = new HashMap();
    private int sampleRate;
    private TargetCostFunction targetCostFunc;
    private JoinCostFunction joinCostFunc;
    private AudioFormat audioFormat;
    
    //header values
    private final static int MAGIC = 0x4d41525;
    private final static int VERSION = 1;
    private final static int CARTS = 1;
    private final static int UNITS = 2;
    private final static int TARGETFEATS = 3;
    private final static int JOINFEATS = 4;
    private final static int TIMELINE = 5;
    
    //how the audio is encoded
    private final static int LPC = 1;
    
    //filenames of the bin files
    private String unitsFile; 
    private String cartsFile;
    private String targetFeatsFile; 
	private String joinFeatsFile;
	private String audioFile;
    
    public ClusterUnitDatabase(){
        super();
    }
    
    public void loadDatabase(String unitsFile, String cartsFile, 
            					String targetFeatsFile, String joinFeatsFile,
            					String audioFile,
            					UnitSelectionFeatProcManager featureProcessors,
                                String voice,
                                TargetCostFunction targetCostFunc,
                                JoinCostFunction joinCostFunc)
    {
        
        this.unitsFile = unitsFile;
        this.cartsFile = cartsFile;
        this.targetFeatsFile = targetFeatsFile;
        this.joinFeatsFile = joinFeatsFile;
        this.audioFile = audioFile;
        this.voice = voice;
        this.featureProcessors = (UtteranceFeatProcManager) featureProcessors;
        this.targetCostFunc = targetCostFunc;
        this.joinCostFunc = joinCostFunc;
        load();
    }
    
    /**
     * Load the database from the given file
     *@param databaseFile the file containing the database
     *@param featureProcessors the feature processors for the carts
     *@param voice the voice to which this database belongs
     */
    public void load(){
        try{       
        logger.info("Loading database for voice "+voice);
        //load units, CARTs, targetFeatures, joinFeatures
        loadUnits(new RandomAccessFile(unitsFile, "r"));
        loadCARTS(new RandomAccessFile(cartsFile, "r"));
        loadTargetFeats(new RandomAccessFile(targetFeatsFile, "r"));
        loadJoinFeats(new RandomAccessFile(joinFeatsFile, "r"));
        //open RandomAccessFile pointing to audio
        openAudio(new RandomAccessFile(audioFile, "r"));
        }catch(Exception e){
            e.printStackTrace();
            throw new Error("Error loading database");
        } 
    }
    
  

    
    /**
     * Load units into database
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void loadUnits(RandomAccessFile raf) throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("No MARY database file!");
        }
        if (raf.readInt() != VERSION)  {
             throw new Error("Wrong version of database file");
        }
        if (raf.readInt() != UNITS)  {
            throw new Error("No units file");
        }       
        int numUnits = raf.readInt();
        sampleRate = raf.readInt();
        units = new Unit[numUnits];
      
        //TODO: Read all in one byte array and then extract units
        for (int i = 0; i < numUnits; i++){
            long startTime = raf.readLong();
            int duration = raf.readInt();
            units[i] = new Unit(startTime, duration,i);
            //System.out.println("Unit "+i+" from "+numUnits);
        }
        
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, // samples per second
                16, // bits per sample
                1, // mono
                2, // nr. of bytes per frame
                sampleRate, // nr. of frames per second
                true); // big-endian;
    }
    
    /**
     * Load CARTs into database
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void loadCARTS(RandomAccessFile raf)throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("No MARY database file!");
        }
        if (raf.readInt() != VERSION)  {
             throw new Error("Wrong version of database file");
        }
        if (raf.readInt() != CARTS)  {
            throw new Error("No CARTs file");
        } 
        int numCarts = raf.readInt();
        cartMap = new HashMap();
        for (int i=0; i<numCarts;i++){
            String name = raf.readUTF();
            int numNodes = raf.readInt();
            CART cart = new CARTImpl(raf, numNodes, featureProcessors,name);
            cartMap.put(name, cart);
        }
    }
    
    /**
     * Load target features into database
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void loadTargetFeats(RandomAccessFile raf)throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("No MARY database file!");
        }
        if (raf.readInt() != VERSION)  {
             throw new Error("Wrong version of database file");
        }
        if (raf.readInt() != TARGETFEATS)  {
            throw new Error("No target features file");
        }   
        targetCostFunc.load(raf, featureProcessors);
    }
    
    /**
     * Load join features into database
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void loadJoinFeats(RandomAccessFile raf)throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("No MARY database file!");
        }
        if (raf.readInt() != VERSION) {
             throw new Error("Wrong version of database file");
        }
        if (raf.readInt() != JOINFEATS)  {
            throw new Error("No join features file");
        }        
        joinCostFunc.load(raf);
    }
    
    /**
     * Load join features into database
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void openAudio(RandomAccessFile raf)throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("No MARY database file!");
        }
        if (raf.readInt() != VERSION) {
             throw new Error("Wrong version of database file");
        }
        if (raf.readInt() != TIMELINE)  {
            throw new Error("No audio file");
        }        
        //read the header
        String header = raf.readUTF();
        
        StringTokenizer tok = new StringTokenizer(header);
        tok.nextToken();
        String audioType = tok.nextToken().trim();
        
        if (audioType.equals("LPC")){
            //load audio
            audio = new LPCTimeLine(raf,tok);
            
           
        } else {
            throw new Error("Error: Audio type "+audioType+" not supported.");
        }
        
    }
    
    /**
     * Reads in new weights from the given file
     * 
     *@param file
     */
    public void overwriteTargetWeights(String file){
        try{
            logger.debug("Overwriting weights for features");
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new 
                    FileInputStream(new 
                            File(file)),"UTF-8"));
            targetCostFunc.overwriteWeights(reader);
        } catch (Exception e){
            e.printStackTrace();
            throw new Error ("Error reading target cost weights");
        }
    }
    
    public void overwriteJoinWeights(String file){
         try{
            logger.debug("Overwriting weights for features");
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new 
                    FileInputStream(new 
                            File(file)),"UTF-8"));
            joinCostFunc.overwriteWeights(reader);
        } catch (Exception e){
            e.printStackTrace();
            throw new Error ("Error reading target cost weights");
        }
    }
    
    public AudioFormat getAudioFormat(){
        return audioFormat;
    }
    
    public int getSamplingRate()
    {
        return sampleRate;
    }
    
    /**
     * Preselect a set of candidates that could be used to realise the
     * given target.
     * @param target a Target object representing an optimal unit
     * @return a Set containing the Unit objects
     */
    public Set getCandidates(Target target){
        return new HashSet();}

  
  
    
    /**
     * Gets a unit
     * 
     * @param which the index of the unit
     * @return the unit
     */
    public Unit getUnit(int which){
        return units[which];
    }
    
     public int getExtendSelections(){
        return 0;
    }
    
    public TimeLine getAudio(){
        return audio;
    }
   
    /**
     * Returns the cart of the given unit type.
     *
     * @param unitType the type of cart
     *
     * @return the cart
     * @throws IllegalArgumentException if the unit type does not exist 
     */
    public CART getTree(String unitType) {
        CART cart =  (CART) cartMap.get(unitType);
    
        if (cart == null) {
            logger.warn("ClusterUnitDatabase: can't find tree for " + unitType);
            // try to recover as best as we can:
            String fallbackUnitType = null;
            if (unitType.endsWith("coda")) {
                fallbackUnitType = unitType.substring(0, unitType.length()-4) + "onset";
            } else if (unitType.endsWith("onset")) {
                fallbackUnitType = unitType.substring(0, unitType.length()-5) + "coda";
            } else if (unitType.endsWith("1")) {
                fallbackUnitType = unitType.substring(0, unitType.length()-1) + "0";
            } else if (unitType.endsWith("0")) {
                fallbackUnitType = unitType.substring(0, unitType.length()-1) + "1";
            }
            if (fallbackUnitType != null)
                cart = (CART) cartMap.get(fallbackUnitType);
            if (cart != null) {
                logger.warn("Using tree for "+fallbackUnitType+" instead");
            } else {
                cart = (CART) cartMap.get("pau");
                if (cart != null) {
                    logger.warn("Using tree for pau instead");
                } else {
                    throw new IllegalArgumentException("No tree or replacement for "+unitType);
                }
            }
            
        }
        return cart;
    }
    
    public UtteranceFeatProcManager getFeatProcManager()
    {
        return featureProcessors;
    }
    
   
    
   

    
}
