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
    
    private ClusterUnit[] units;
    private Map unitTypesMap; // Map unit names to unit type objects  
    
    private final static int MAGIC = 0xf0cacc1a;
    private final static int VERSION = 0x2000;
    
    public ClusterUnitDatabase(){
        super();}
    
    /**
     * Load the database from the given file
     *@param databaseFile the file containing the database
     *@param featureProcessors the feature processors for the carts
     *@param voice the voice to which this database belongs
     */
    public void load(String databaseFile, 
            		 UnitSelectionFeatProcManager featureProcessors,
            		 String voice){
        try{
            this.voice = voice;
            this.featureProcessors = (UtteranceFeatProcManager) featureProcessors;
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
        }catch(IOException e1){
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
    	   	
    	if (!(bb.getInt() == VERSION))  {
    		throw new Error("Wrong version of database file");}

    	continuityWeight = bb.getInt();
    	optimalCoupling = bb.getInt();
		extendSelections = bb.getInt();
		joinMethod = bb.getInt();
		joinWeightShift = bb.getInt();

		int weightLength = bb.getInt();
		joinWeights = new int[weightLength];
		for (int i = 0; i < joinWeights.length; i++) {
			joinWeights[i] = bb.getInt();
		}

		int unitsLength = bb.getInt();
        units = new ClusterUnit[unitsLength];
        int unitsBlockStart = bb.position();
        int unitEntryLength = 24; // 6 int = 24 byte
        // Fast forward to unit types block first:
        bb.position(unitsBlockStart+unitsLength*unitEntryLength);

		int unitTypesLength = bb.getInt();
        unitTypesMap = new HashMap(unitTypesLength);
		for (int i = 0; i < unitTypesLength; i++) {
			ClusterUnitType unitType = new ClusterUnitType(bb, this);
            unitTypesMap.put(unitType.getName(), unitType);
            int currentUnitTypePos = bb.position();

            // Read in the units of this type:
            int firstUnitIdx = unitType.getStart();
            int lastUnitIdx = firstUnitIdx + unitType.getCount();
            bb.position(unitsBlockStart+firstUnitIdx*unitEntryLength);
            for (int unitIdx=firstUnitIdx; unitIdx<lastUnitIdx; unitIdx++) {
                units[unitIdx] = new ClusterUnit(bb, this, unitType.getName());
                units[unitIdx].setInstanceNumber(unitIdx-firstUnitIdx);
            }
            // back to position in list of unit types:
            bb.position(currentUnitTypePos);
		}

		audioFrames = new BufferedFrameSet(bb);
		joinCostFeatureVectors = new BufferedFrameSet(bb);

        
		int numCarts = bb.getInt();
		cartMap = new HashMap();
		for (int i = 0; i < numCarts; i++) {
			String name = Utilities.getString(bb);
			CART cart = CARTImpl.loadBinary(bb);
			((CARTImpl) cart).setFeatureProcessors(featureProcessors);
			cartMap.put(name, cart);

			if (defaultCart == null) {
				defaultCart = cart;
			}
		}
        
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
    	
        if (!(raf.readInt() == VERSION))  {
    		throw new Error("Wrong version of database file");}
        
    	continuityWeight = raf.readInt();
    	optimalCoupling = raf.readInt();
		extendSelections = raf.readInt();
		joinMethod = raf.readInt();
		joinWeightShift = raf.readInt();

		int weightLength = raf.readInt();
		joinWeights = new int[weightLength];
		for (int i = 0; i < joinWeights.length; i++) {
			joinWeights[i] = raf.readInt();
		}

		int unitsLength = raf.readInt();
		units = new ClusterUnit[unitsLength];
        long unitsBlockStart = raf.getFilePointer();
        int unitEntryLength = 24; // 6 int = 24 byte
        // Fast forward to unit types block first:
        raf.seek(unitsBlockStart+unitsLength*unitEntryLength);

        int unitTypesLength = raf.readInt();
        unitTypesMap = new HashMap(unitTypesLength);
        for (int i = 0; i < unitTypesLength; i++) {
            ClusterUnitType unitType = new ClusterUnitType(raf, this);
            unitTypesMap.put(unitType.getName(), unitType);
            long currentUnitTypePos = raf.getFilePointer();
            // Read in the units of this type:
            int firstUnitIdx = unitType.getStart();
            int lastUnitIdx = firstUnitIdx + unitType.getCount();
            raf.seek(unitsBlockStart+firstUnitIdx*unitEntryLength);
            for (int unitIdx=firstUnitIdx; unitIdx<lastUnitIdx; unitIdx++) {
                units[unitIdx] = new ClusterUnit(raf, this, unitType.getName());
                units[unitIdx].setInstanceNumber(unitIdx-firstUnitIdx);
            }
            // back to position in list of unit types:
            raf.seek(currentUnitTypePos);
        }

        audioFrames = new FiledFrameSet(raf);
		joinCostFeatureVectors = new FiledFrameSet(raf);
		
		int numCarts = raf.readInt();
		cartMap = new HashMap();
		for (int i = 0; i < numCarts; i++) {
			int size = raf.readShort();
	    	char[] charBuffer = new char[size];
	    	for (int j = 0; j < size; j++) {
	    	    charBuffer[j] = raf.readChar();
	    	}
			String name = new String(charBuffer, 0, size);
			CART cart = CARTImpl.loadBinary(raf);
			((CARTImpl)cart).setFeatureProcessors(featureProcessors);
			cartMap.put(name, cart);

			if (defaultCart == null) {
				defaultCart = cart;
			}
		}
        
    }
    
    /**
     * Load the information about the units origin
     *
     */
    private void loadCatalogue(){
        try{
        String file = 
            MaryProperties.getFilename("voice."+voice+".unitCatalogue");
        BufferedReader reader = 
            new BufferedReader(new FileReader(new File(file)));
        String line = reader.readLine();
        //skip the header
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("EST_Header_End")) break;
        }
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(" ");
            String name = tokens[0];
            int index = getUnitIndexName(name);
            try {
                UnitOriginInfo unitOrigin = new UnitOriginInfo();
                unitOrigin.originFile = tokens[1];
                unitOrigin.originStart = Float.valueOf(tokens[2]).floatValue();
                unitOrigin.originEnd = Float.valueOf(tokens[4]).floatValue();
                getUnit(index).setOriginInfo(unitOrigin);
            } catch (NumberFormatException nfe) {}
        }
        }catch(Exception e){
            e.printStackTrace();
            logger.debug("Error reading catalogue of voice "+voice);
        }
    }
    
    /**
     * Retrieves the index for the name given a name. 
     *
     * @param name the name
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
        return audioFrames.getFrameSetInfo().getSampleRate();
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
    
    
    /**
     * Gets a unit
     * 
     * @param which the index of the unit
     * @return the unit
     */
    public ClusterUnit getUnit(int which){
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
            logger.debug("getUnit(type,instance): can't find unit type " + unitType);
            return null;
        }
        return unitType.getInstance(instance);
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
     */
    public CART getTree(String unitType) {
	CART cart =  (CART) cartMap.get(unitType);

	if (cart == null) {
	    System.err.println("ClusterUnitDatabase: can't find tree for " 
		    + unitType);
	    return defaultCart; 	// "graceful" failrue
	}
	return cart;
    }
    
}
