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
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import org.apache.log4j.Level;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.cart.CART;

import com.sun.speech.freetts.util.Utilities;

import de.dfki.lt.mary.unitselection.*;
import de.dfki.lt.mary.unitselection.cart.CARTImpl;
import de.dfki.lt.mary.unitselection.featureprocessors.UtteranceFeatProcManager;
import de.dfki.lt.mary.unitselection.featureprocessors.UnitSelectionFeatProcManager;


public class ClusterUnitDatabase extends UnitDatabase
{ 
    public final static int CLUNIT_NONE = 65535;
        
    private UtteranceFeatProcManager featureProcessors;
    private String voice;
    
    private int continuityWeight;
    private int optimalCoupling;
    private int extendSelections;
    private int joinMethod;
    private int[] joinWeights;
    private int joinWeightShift;

    private Map cartMap = new HashMap();
    private CART defaultCart = null;
    
    private float[] unitsPosition;
    private ClusterUnit[] units;
    private Map unitTypesMap; // Map unit names to unit type objects  
    
    private List featsNWeights = null;
    
    private MappedByteBuffer bb = null;
    private RandomAccessFile raf = null;
    
    //defines, if audio data is thrown away after use (low)
    // or not (high)
    private String memoryRequirement = "low";
    
    //needed for converting from .txt to .bin
    private final static int MAGIC = 0xf0cacc1a;
    private final static int VERSION = 0x2000;
    private final static int VERSIONWITHFEATURES = 0x2001;
    private ClusterUnitType[] unitTypesArray;
    private static boolean loadFeatures = false;
    private static String featDef;
    private static String valDir;
    
    
    public ClusterUnitDatabase()
    {
        super();
    }
    
    /**
     * Load the database from the given file
     *@param databaseFile the file containing the database
     *@param featureProcessors the feature processors for the carts
     *@param voice the voice to which this database belongs
     */
    public void load(String databaseFile, 
                     UnitSelectionFeatProcManager featureProcessors,
                     String voice){
        boolean retrySlow = false;
        this.voice = voice;
        this.featureProcessors = (UtteranceFeatProcManager) featureProcessors;
        try{
            //try loading the database with MappedByteBuffer
            RandomAccessFile raf = new RandomAccessFile(databaseFile, "r");
            FileChannel fc = raf.getChannel();
            logger.debug("Loading in quick mode...");
            MappedByteBuffer bb = 
                fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
            bb.load();
            loadMBB(bb);
        //if loading failed,
        //you have to load the file with RandomAccessFile 
        }catch(OutOfMemoryError oome){
            retrySlow = true;
        }catch(IOException ioe){
            retrySlow = true;
        }
        if (retrySlow) {
            try{
                logger.debug("Quick loading failed. Loading in slow mode now...");
                RandomAccessFile raf = new RandomAccessFile(databaseFile, "r");
                loadRAF(raf);
            } catch(Exception e2){
                e2.printStackTrace();
                throw new Error ("Cannot load database "+databaseFile);}
        }
        
        
        //when in debug mode, load information about unit orgins
        if (logger.getEffectiveLevel().equals(Level.DEBUG)){
            loadCatalogue();
        }
        
    }
    
    /**
     * Load database with MappedByteBuffer
     * Fails on some machines
     * @param bb the MappedByteBuffer
     * @throws IOException
     */
    private void loadMBB(MappedByteBuffer bb) throws IOException{
        if (bb.getInt() != MAGIC)  {
            throw new Error("Bad magic in db");
        }
        int version = bb.getInt();    
        if (!(version == VERSION))  {
            if (!(version == VERSIONWITHFEATURES)){
                throw new Error("Wrong version of database file");
            } else { 
                loadFeatures = true;
            }
        }
        
        continuityWeight = bb.getInt();
        optimalCoupling = bb.getInt();
        extendSelections = bb.getInt();
        //System.out.println(continuityWeight+" "+optimalCoupling+" "
        //        +extendSelections);
        //System.out.println("Building Audio Frames");
        audioFrames = new BufferedFrameSet(bb, this);
        //System.out.println("Building join cost Feature Vectors");
        joinCostFeatureVectors = new BufferedFrameSet(bb,this);
        joinMethod = bb.getInt();
        joinWeightShift = bb.getInt();
        int weightLength = bb.getInt();
        //System.out.println(joinMethod+" "+joinWeightShift+" "+weightLength);
        joinWeights = new int[weightLength];
        for (int i = 0; i < joinWeights.length; i++) {
            joinWeights[i] = bb.getInt();
            //System.out.print(joinWeights[i]+" ");
        }
        unitSize = bb.getInt();
        int unitsLength = bb.getInt();
        units = new ClusterUnit[unitsLength];
        unitsPosition = new float[unitsLength];
        
        int unitTypesLength = bb.getInt();
        unitTypesMap = new HashMap(unitTypesLength);
        ClusterUnitType unitType = null;
        int numberInvalidUnits = 0;
        int numberValidUnits = 0;
        for (int i = 0; i < unitTypesLength; i++) {
            unitType = new ClusterUnitType(bb, this);
            unitTypesMap.put(unitType.getName(), unitType);
            int currentUnitTypePos = bb.position();

            // Read in the units of this type:
            int firstUnitIdx = unitType.getStart();
            int lastUnitIdx = firstUnitIdx + unitType.getCount();
            for (int unitIdx=firstUnitIdx; unitIdx<lastUnitIdx; unitIdx++) {
                ClusterUnit nextUnit = new ClusterUnit(bb, this, unitType.getName(),loadFeatures);
                if (nextUnit.isValid()){
                    nextUnit.setInstanceNumber(unitIdx-firstUnitIdx);
                    units[unitIdx] = nextUnit;
                    numberValidUnits++;
                    //logger.debug("Unit "+nextUnit.toString()+" is valid");
                } else {
                    units[unitIdx] = null;
                    numberInvalidUnits++;
                    logger.debug("Unit "+nextUnit.toString()+" is not valid");
                }
            }
        }
        logger.debug("Valid units: "+numberValidUnits+
                "\nInvalid units: "+numberInvalidUnits);
        int numCarts = bb.getInt();
        cartMap = new HashMap();
        for (int i = 0; i < numCarts; i++) {
            String name = Utilities.getString(bb);
            CART cart = CARTImpl.loadBinary(bb, featureProcessors,name);
                cartMap.put(name, cart);
            if (defaultCart == null) {
                defaultCart = cart;
            }
           
        }
        if (loadFeatures){
            //load features and weights
            int numberOfFeats = bb.getInt();
            featsNWeights = new ArrayList();
            for (int i=0;i<numberOfFeats;i++){
                int featsize = bb.getShort();
                char[] charBufferFeat = new char[featsize];
                for (int j = 0; j < featsize; j++) {
                    charBufferFeat[j] = bb.getChar();
                }
                featsNWeights.add(new String(charBufferFeat, 0, featsize));
            }
        }
        this.bb = bb;
        
    }
    
    /**
     * Load database with RandomAccessFile.
     * Is slower than loadMBB, therefore only used
     * if loadMBB fails
     * @param raf the RandomAccessFile
     * @throws IOException
     */
    private void loadRAF(RandomAccessFile raf)throws IOException{
        if (raf.readInt() != MAGIC)  {
            throw new Error("Bad magic in db");
        }
        int version = raf.readInt();
        if (!(version == VERSION))  {
            if (!(version == VERSIONWITHFEATURES)){
                throw new Error("Wrong version of database file");
            } else { 
                loadFeatures = true;
            }
        }
        
        continuityWeight = raf.readInt();
        optimalCoupling = raf.readInt();
        extendSelections = raf.readInt();

        audioFrames = new FiledFrameSet(raf);
        joinCostFeatureVectors = new FiledFrameSet(raf);
        
        joinMethod = raf.readInt();
        joinWeightShift = raf.readInt();

        int weightLength = raf.readInt();
        joinWeights = new int[weightLength];
        for (int i = 0; i < joinWeights.length; i++) {
            joinWeights[i] = raf.readInt();
        }
        unitSize = raf.readInt();
        int unitsLength = raf.readInt();
        units = new ClusterUnit[unitsLength];
        
        int unitTypesLength = raf.readInt();
        unitTypesMap = new HashMap(unitTypesLength);
        int numberInvalidUnits = 0;
        int numberValidUnits = 0;
        for (int i = 0; i < unitTypesLength; i++) {
            ClusterUnitType unitType = new ClusterUnitType(raf, this);
            unitTypesMap.put(unitType.getName(), unitType);
            long currentUnitTypePos = raf.getFilePointer();
            // Read in the units of this type:
            int firstUnitIdx = unitType.getStart();
            int lastUnitIdx = firstUnitIdx + unitType.getCount();
            for (int unitIdx=firstUnitIdx; unitIdx<lastUnitIdx; unitIdx++) {
                ClusterUnit nextUnit = new ClusterUnit(raf, this, unitType.getName(),loadFeatures);
                if (nextUnit.isValid()){
                    nextUnit.setInstanceNumber(unitIdx-firstUnitIdx);
                    units[unitIdx] = nextUnit;
                    numberValidUnits++;
                    //logger.debug("Unit "+nextUnit.toString()+" is valid");
                } else {
                    units[unitIdx] = null;
                    numberInvalidUnits++;
                    logger.debug("Unit "+nextUnit.toString()+" is not valid");
                }
            }
        }
        logger.debug("Valid units: "+numberValidUnits+
                "\nInvalid units: "+numberInvalidUnits);

        int numCarts = raf.readInt();
        cartMap = new HashMap();
        for (int i = 0; i < numCarts; i++) {
            int size = raf.readShort();
            char[] charBuffer = new char[size];
            for (int j = 0; j < size; j++) {
                charBuffer[j] = raf.readChar();
            }
            String name = new String(charBuffer, 0, size);
            CART cart = CARTImpl.loadBinary(raf,featureProcessors,name);
                cartMap.put(name, cart);
            if (defaultCart == null) {
                defaultCart = cart;
            }
        }
        
        if (loadFeatures){
            //read in features and weights
            int numberOfFeats = raf.readInt();
            featsNWeights = new ArrayList();
            for (int i=0;i<numberOfFeats;i++){
                int featsize = raf.readShort();
                char[] charBufferFeat = new char[featsize];
                for (int j = 0; j < featsize; j++) {
                    charBufferFeat[j] = raf.readChar();
                }
                featsNWeights.add(new String(charBufferFeat, 0, featsize));
            }
        }
        this.raf = raf;
        
    }
    
    /**
     * Load the information about the units origin
     *
     */
    private void loadCatalogue()
    {
		try {
			String file = MaryProperties.getFilename("voice." + voice
					+ ".unitCatalogue");
			if (file == null) {
				logger.debug("No catalogue file given for voice " + voice);
				return;
			}
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					file)));
			String line = reader.readLine();
			// skip the header
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("EST_Header_End"))
					break;
			}
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(" ");
				String name = tokens[0];
				int index = getUnitIndexName(name);
				try {
					String originFile = tokens[1];
					float originStart = Float.valueOf(tokens[2])
							.floatValue();
					float originEnd = Float.valueOf(tokens[4])
							.floatValue();
					UnitOriginInfo unitOrigin = 
					    new UnitOriginInfo(originFile,originStart,
					            originEnd);
                    ClusterUnit unit = (ClusterUnit)getUnit(index); 
					if (unit != null) unit.setOriginInfo(unitOrigin);
				} catch (NumberFormatException nfe) {
				}
			}
		} catch (Throwable e) {
			logger.debug("Error reading catalogue of voice " + voice, e);
		}
	}
    
    /**
     * Reads in new weights from the given file
     * 
     *@param file
     */
    public void overwriteWeights(String file){
        try{
            logger.debug("Overwriting weights for features");
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(new 
                    FileInputStream(new 
                            File(file)),"UTF-8"));
        ArrayList newFeatsNWeights = new ArrayList();
        String line = reader.readLine();
        while (line!=null){
            if (!(line.startsWith("***"))){
                newFeatsNWeights.add(line);
            }
            line = reader.readLine();
        }
        // the database does not care if the lines in
        // the file are right, only the number of lines has to be the same 
        // bad lines throw errors in the TargetCostFunction
        if (newFeatsNWeights.size() != featsNWeights.size()){
            logger.warn("Error reading target cost weights, assigning default weights");
        } else {
            featsNWeights = newFeatsNWeights;
        }
        } catch (Exception e){
            e.printStackTrace();
            throw new Error ("Error reading target cost weights");
        }
        
        
    }
    
    
    public void setOptimalCoupling(String method){
        optimalCoupling = Integer.parseInt(method);
        logger.debug("Set optimal coupling to "+optimalCoupling);
    }
    
    public void setMemoryRequirement(String requirement){
        memoryRequirement = requirement;
        logger.debug("Set Memory Policy to "+memoryRequirement);
    }
    
    /**
	 * Retrieves the index for the name given a name.
	 * 
	 * @param name
	 *            the name
	 * 
	 * @return the index for the name
	 */
    private int getUnitIndexName(String name) {
    int lastIndex = name.lastIndexOf('_');
    if (lastIndex == -1) {
        logger.debug("getUnitIndexName: bad unit name " + name);
        return -1;
    }
    int index = Integer.parseInt(name.substring(lastIndex + 1));
    String unitType = name.substring(0, lastIndex);
    return ((ClusterUnitType)unitTypesMap.get(unitType)).getStart() + index;
    }
    
    public int getSamplingRate()
    {
        return audioFrames.getSamplingRate();
    }
    
    /**
     * Preselect a set of candidates that could be used to realise the
     * given target.
     * @param target a Target object representing an optimal unit
     * @return a Set containing the Unit objects
     */
    public Set getCandidates(Target target){
        return new HashSet();}

    
    public int getOptimalCoupling(){
        return optimalCoupling;
    }
    
    public String getMemoryRequirement(){
        return memoryRequirement;
    }
    
    /**
     * Gets a unit
     * 
     * @param which the index of the unit
     * @return the unit
     */
    public Unit getUnit(int which){
        return units[which];
    }
    

    
    /**
     * Retrieves the unit index given a unit type and val.
     *
     * @param unitTypeName the type of the unit
     * @param instance the instance number of the unit
     *
     * @return the unit, or null if it cannot be found.
     * 
     */
    public Unit getUnit(String unitTypeName, int instance) {
        ClusterUnitType unitType = (ClusterUnitType) unitTypesMap.get(unitTypeName);
        if (unitType == null) {
            logger.debug("getUnit("+unitTypeName+","+instance+"): can't find unit type " + unitType);
            return null;
        }
        return (Unit) unitType.getInstance(instance);
    }
    
    public int[] getJoinWeights(){
        return joinWeights;
    }
    
    public int getContinuityWeight(){
        return continuityWeight;
    }

    /**
     * Retrieves the extend selections setting.
     *
     * @return the extend selections setting
     */
    public int getExtendSelections() {
        return extendSelections;
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
            throw new IllegalArgumentException("ClusterUnitDatabase: can't find tree for " + unitType);
        }
        return cart;
    }
    
    public UtteranceFeatProcManager getFeatProcManager()
    {
        return featureProcessors;
    }
    
    public List getFeatsNWeights(){
        return featsNWeights;
    }
    
    // methods for reading in text file of the voice
    // and the features and dumping all in bin format
    
    /**
     * Loads the text file of a voice.
     * This method is used when converting the FreeTTS
     * voice data into Mary binary format
     * @param is the input stream
     */
    private void loadTextAndDump(String file, String desFile) {
        try {
            //Open source file
            System.out.println("Opening source file ...");
            if (file == null) {
                throw new Error("Can't load cluster db file.");
            }
            URL fileURL = new URL("file:" + file);
            FileInputStream fis = new FileInputStream(fileURL.getFile());
            
            BufferedReader reader =
                new BufferedReader(new 
                        InputStreamReader(fis));
            //Open destination file
            System.out.println("Opening destination file ...");
            FileOutputStream fos = new FileOutputStream(desFile);
            DataOutputStream os = new DataOutputStream(new
                BufferedOutputStream(fos));
            System.out.println("Dumping header ...");
            os.writeInt(MAGIC);
            if (loadFeatures){
                os.writeInt(VERSIONWITHFEATURES);
            } else {
                os.writeInt(VERSION);
            }
            
            
            //start to read the source file
            System.out.println("Reading source file ...");
            List unitList = new ArrayList();
            List unitTypes = new ArrayList();
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("STS STS")){
                    //by the time we reach this line, we have
                    //definitely read the 3 ints in the top section
                    //of the FreeTTS file and can dump then before
                    //dumping the audio data
                    os.writeInt(continuityWeight);
                    os.writeInt(optimalCoupling);
                    os.writeInt(extendSelections);
                    System.out.println("Processing audio data ...");
                } 
                if (!line.startsWith("***")) {
                    parseAndAdd(line, reader,unitList, unitTypes, os);
                }
                line = reader.readLine();
            }
            reader.close();
            System.out.println("Done reading source file");
            units = new ClusterUnit[unitList.size()];
            units = (ClusterUnit[]) unitList.toArray(units);
            
            unitList = null;
            unitTypesArray = new ClusterUnitType[unitTypes.size()];
            unitTypesArray = (ClusterUnitType[]) unitTypes.toArray(unitTypesArray);
            if (unitTypesArray[0].getName().endsWith("left") 
                    || unitTypesArray[0].getName().endsWith("right")){
                unitSize = HALFPHONE;
            }
            unitTypes = null;
            dumpBinary(os);
        } catch (IOException e) {
            throw new Error(e.getMessage());
        } 
    }
    
    /**
     * Parses and process the given line.
     *
     * @param line the line to process
     * @param reader the source for the lines
     *
     * @throws IOException if an error occurs while reading
     */
    private void parseAndAdd(String line, 
                            BufferedReader reader, 
                            List units,
                            List unitTypes,
                            DataOutputStream os)
    throws IOException {
    try {
        StringTokenizer tokenizer = new StringTokenizer(line," ");
        String tag = tokenizer.nextToken();
        if (tag.equals("CONTINUITY_WEIGHT")) {
        continuityWeight = Integer.parseInt(tokenizer.nextToken());
        } else if (tag.equals("OPTIMAL_COUPLING")) {
        optimalCoupling = Integer.parseInt(tokenizer.nextToken());
        }  else if (tag.equals("EXTEND_SELECTIONS")) {
        extendSelections = Integer.parseInt(tokenizer.nextToken());
        }  else if (tag.equals("JOIN_METHOD")) {
        joinMethod = Integer.parseInt(tokenizer.nextToken());
        }  else if (tag.equals("JOIN_WEIGHTS")) {
        int numWeights = Integer.parseInt(tokenizer.nextToken());
            joinWeights = new int[numWeights];
        for (int i = 0; i < numWeights; i++) {
            joinWeights[i]  = Integer.parseInt(tokenizer.nextToken());
        }

        joinWeightShift = calcJoinWeightShift(joinWeights);

        } else if (tag.equals("STS")) {
        String name = tokenizer.nextToken();
        if (name.equals("STS")) {
            audioFrames = new FrameSet(tokenizer, reader);
            System.out.println("Dumping STS");
            audioFrames.dumpBinary(os);
            
            audioFrames = null;
        } else {
            joinCostFeatureVectors = new FrameSet(tokenizer, reader);
            System.out.println("Dumping MCEP");
            joinCostFeatureVectors.dumpBinary(os);

            joinCostFeatureVectors = null;
        }
        } else if (tag.equals("UNITS")) {
        int type = Integer.parseInt(tokenizer.nextToken());
        int phone = Integer.parseInt(tokenizer.nextToken());
        int start = Integer.parseInt(tokenizer.nextToken());
        int end = Integer.parseInt(tokenizer.nextToken());
        int prev = Integer.parseInt(tokenizer.nextToken());
        int next = Integer.parseInt(tokenizer.nextToken());
        ClusterUnit unit 
              = new ClusterUnit(type, phone, start, 
                  end, prev, next);
        units.add(unit);
        } else if (tag.equals("CART")) {
        String name = tokenizer.nextToken();
        int nodes = Integer.parseInt(tokenizer.nextToken());
        CART cart = new CARTImpl(reader, nodes);
        cartMap.put(name, cart);
        if (defaultCart == null) {
            defaultCart = cart;
        }
        } else if (tag.equals("UNIT_TYPE")) {
        String name = tokenizer.nextToken();
        int start = Integer.parseInt(tokenizer.nextToken());
        int count = Integer.parseInt(tokenizer.nextToken());
        ClusterUnitType unitType = new ClusterUnitType(name, start, count);
        unitTypes.add(unitType);
        } else {
        throw new Error("Unsupported tag " + tag + " in db line `" + line + "'");
        }
    
    } catch (NoSuchElementException nse) {
        throw new Error("Error parsing db " + nse.getMessage());
    } catch (NumberFormatException nfe) {
        throw new Error("Error parsing numbers in db line `" + line + "':" + nfe.getMessage());
    }
    }
    
    /**
     * Calculates the join weight shift.
     *
     * @param joinWeights the weights to check
     *
     * @return the amount to right shift (or zero if not possible)
     */
    private int calcJoinWeightShift(int[] joinWeights) {
    int first = joinWeights[0];
    for (int i = 1; i < joinWeights.length; i++) {
        if (joinWeights[i] != first) {
        return 0;
        }
    }

    int divisor = 65536 / first;
    if (divisor == 2) {
        return 1;
    } else if (divisor == 4) {
        return 2;
    }
    return 0;
    }
    
    /**
     * Retrieves the type index for the name given a name. 
     *
     * @param name the name
     *
     * @return the index for the name
     */
// [[[TODO: perhaps replace this with java.util.Arrays.binarySearch]]]
    public int getUnitTypeIndex(String name) {
        int start, end, mid, c;

	start = 0;
	end = unitTypesArray.length;

	while (start < end) {
	    mid = (start + end) / 2;
	    c = unitTypesArray[mid].getName().compareTo(name);
	    if (c == 0) {
		return unitTypesArray[mid].getStart();
	    } else if (c > 0) {
		end = mid;
	    } else {
		start = mid + 1;
	    }
	}
	return -1;
    }
    
    /**
     * Dumps the binary form of the database.
     *
     * @param path the path to dump the file to
     */
    private void dumpBinary(DataOutputStream os) {
    try {
        System.out.println("Dumping unit data ...");
        os.writeInt(joinMethod);
        os.writeInt(joinWeightShift);
        os.writeInt(joinWeights.length);
        for (int i = 0; i < joinWeights.length; i++) {
            os.writeInt(joinWeights[i]);
        }
        os.writeInt(unitSize);
        //load features if specified
        if (loadFeatures){
            loadFeatures(featDef,valDir);
            System.out.println("Continue dumping unit data ...");
        }
        
        os.writeInt(units.length);
        os.writeInt(unitTypesArray.length);
        for (int i = 0; i < unitTypesArray.length; i++) {
            unitTypesArray[i].dumpBinary(os);
            int start = unitTypesArray[i].getStart();
            int end = start+unitTypesArray[i].getCount();
            for (int j =start; j<end;j++){
                if (!units[j].dumpBinary(os, loadFeatures)){
                    System.out.println("No features for unit of type "
                            +unitTypesArray[i].getName()+", starting at"+j);
                }
            }
        }
        

        os.writeInt(cartMap.size());
        for (Iterator i = cartMap.keySet().iterator(); i.hasNext();) {
        String name = (String) i.next();
        CART cart =  (CART) cartMap.get(name);

        Utilities.outString(os, name);
        cart.dumpBinary(os);
        }
        if (loadFeatures){
            os.writeInt(featsNWeights.size());
            for (int i = 0; i<featsNWeights.size(); i++){
                String next = (String)featsNWeights.get(i);
	            os.writeShort((short)next.length());
	            os.writeChars(next);
            } 
        }
        
        os.close();
    } catch (FileNotFoundException fe) {
        throw new Error("Can't dump binary database " +
            fe.getMessage());
    } catch (IOException ioe) {
        throw new Error("Can't dump binary database " +
            ioe.getMessage());
    }
    }

    /**
     * Load the features
     * @param featureDefs the file containing feature definitions
     * @param valuesDir the directory containing the files of values
     */
    private void loadFeatures(String featureDefs, String valuesDir){
        FeatureReader featureReader = 
            new FeatureReader(this, featureDefs, valuesDir);
        featureReader.readFeatures();
        featsNWeights = featureReader.getFeatsNWeights();
    }
    
    /**
     *  Manipulates a ClusterUnitDatabase.  
     *  Can be used to build a .bin file from a .txt file.
     *  Called by target clunit_voice_bin in build.xml
     *
     * <p>
     * <b> Usage </b>
     * <p>
     *  <code> java com.sun.speech.freetts.clunits.ClusterUnitDatabase
     *  [options]</code> 
     * <p>
     * <b> Options </b>
     * <p>
     *    <ul>
     *          <li> <code> -src path </code> provides a directory
     *          path to the source text for the database
     *          <li> <code> -dest path </code> provides a directory
     *          for where to place the resulting binaries
     *      <li> <code> -generate_binary [filename]</code> reads
     *      in the text version of the database and generates
     *      the binary version of the database.
     *      <li> <code> -compare </code>  Loads the text and
     *      binary versions of the database and compares them to
     *      see if they are equivalent.
     *      <li> <code> -showTimes </code> shows timings for any
     *      loading, comparing or dumping operation
     *    </ul>
     * 
     */
    public static void main(String[] args) 
    {
        boolean showTimes = false;
        String srcPath = ".";
        String destPath = ".";

        try {
            if (args.length > 0) {
                for (int i = 0 ; i < args.length; i++) {
                    if (args[i].equals("-src")) {
                        srcPath = args[++i];
                    } else if (args[i].equals("-dest")) {
                        destPath = args[++i];
                    } else if (args[i].equals("-generate_binary")) {
                            String name = "clunits.txt";
                            if (i + 1 < args.length) {
                                String nameArg = args[++i];
                                if (!nameArg.startsWith("-")) {
                                    name = nameArg;
                                }
                                int suffixPos = name.lastIndexOf(".txt");

                                String binaryName = "clunits.bin";
                                if (suffixPos != -1) {
                                    binaryName = name.substring(0, suffixPos) + ".bin";
                                }
                                ClusterUnitDatabase db = new ClusterUnitDatabase();
                                
                                if (i+2<args.length){
                                    String featureDefFile = args[++i];
                                    String valueDir = args[++i];
                                    if (!(featureDefFile.equals("${feature_def}"))){
                                        //System.out.println("Loading features from " 
                                          //  + featureDefFile + " and values from dir "
                                            //+ valueDir);
                                        featDef = srcPath + "/" + featureDefFile;
                                        valDir = srcPath + "/" + valueDir;
                                        //db.loadFeatures(srcPath + "/" + featureDefFile, 
                                          //  		srcPath + "/" + valueDir);
                                        loadFeatures = true;
                                    }
                                }
                               
                                db.loadTextAndDump(srcPath + "/" + name, destPath + "/" + binaryName);
                                System.out.println("Successfully dumped "+binaryName);
                            } else {
                                System.out.println("Need a voice name");
                            }
                    }else {System.out.println("Unknown option " + args[i]);}
            }
            } else {
                System.out.println("Options: ");
                System.out.println("    -src path");
                System.out.println("    -dest path");
                System.out.println("    -generate_binary");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }

    
}
