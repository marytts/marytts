/**
 * Copyright 2006 DFKI GmbH.
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

package de.dfki.lt.mary.unitselection;

import java.util.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import de.dfki.lt.mary.unitselection.Unit;

/**
 * Reads in the feature of the units 
 * so that a target cost function can be used
 * 
 * @author Anna Hunecke
 *
 */
public class FeatureReader
{
    
    private List definitions;
    private UnitDatabase database;
    private String featsDefsFile;
    private String unitsFeatsDir;
    private Logger logger;
    private boolean debug = false;
    
    public FeatureReader(UnitDatabase database,String featsDefsFile,
            			 String unitsFeatsDir){
        this.database = database;
        this.featsDefsFile = featsDefsFile;
        this.unitsFeatsDir = unitsFeatsDir;
        logger = Logger.getLogger("FeatureReader");
        if (logger.getEffectiveLevel().equals(Level.DEBUG)){
            debug = true;
        }
    }
    
    /**
     * Build the weights map.
     * For the moment, just dummy weights.
     * Maybe features and weights should be read in
     * from the same file.
     * @return the weights
     */
    public Map readWeights()
    {
        Map weights = new HashMap();
        for (Iterator it = definitions.iterator(); it.hasNext();){
            weights.put(it.next(),new Integer(1));
        }
        return weights;
    }
    
    /**
     * Read in the features and the feature values of
     * the units. Then map features to values and store 
     * them in the individual units.
     * @return the features
     */
    public List readFeatures()
    {	
        try{
            //Read in the feature definitions
            definitions = new ArrayList();
            logger.debug("Reading features from "+featsDefsFile);
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new 
                        FileInputStream(new 
                                File(featsDefsFile)),"UTF-8"));
            String line = reader.readLine();
            while (line!=null){
                if (line.startsWith("(")){
                    StringTokenizer tok = 
                        new StringTokenizer(line," ");
                    if (tok.countTokens() >1){
                        //throw away the "("
                        tok.nextToken();
                        //read in the feature name
                        definitions.add(tok.nextToken());
                    }
                }       
                line = reader.readLine();
            }
            if (debug){
                for (int k =0; k<definitions.size();k++){
                    logger.debug("Feature "+k+": "+definitions.get(k));
                }
            }
            //Read in the each unit type with all its units and their features
            File featsDir = new File(unitsFeatsDir);
            logger.debug("Reading values from directory "+unitsFeatsDir);
            if (featsDir.isDirectory()){
                File[] entries = featsDir.listFiles();
                int startIndex = (unitsFeatsDir).length();
                boolean ignoreFirst = true;
                if (!((String)definitions.get(0)).equals("occurid")){
                    ignoreFirst = false;
                }
                for (int i = 0; i<entries.length;i++){
                    //determine the name of the file = unit type
                    String unitType = entries[i].toString();
                    int endIndex = unitType.length()-6;
                    unitType = unitType.substring(startIndex, endIndex);
                    //open the file
                    BufferedReader unitsReader =
                        new BufferedReader(new InputStreamReader(new 
                                FileInputStream(entries[i]),"UTF-8"));
                    //each line refers to a unit
                    String nextUnit = unitsReader.readLine();
                    while (nextUnit!=null){
                    //get the unit and put the features in it
                        StringTokenizer tok = 
                            new StringTokenizer(nextUnit," ");
                        int unitIndex = Integer.parseInt(tok.nextToken());
                        Unit unit = database.getUnit(unitType,unitIndex);
                        if (unit!=null){
                            Map featuresMap = new HashMap();
                            int j = 1;
                            if (!ignoreFirst){
                                j=0;
                            }
                            for (;j<definitions.size(); j++){
                                if (tok.hasMoreTokens()){
                                    featuresMap.put(definitions.get(j),
                                        		tok.nextToken());
                                } else {
                                    throw new Error ("Mismatch in feature file "
                                        	+unitType+", feature "+j
                                        	+" is missing");}
                            	}
                          	unit.setFeaturesMap(featuresMap);
                          	
                          	    
                          	nextUnit=unitsReader.readLine();
                        } else {
                            nextUnit = null;
                        }     
                   }
                }
            }    
            return definitions;
        }catch (FileNotFoundException fe){
            logger.warn("Can not read features: "+fe.getMessage());
            return null;
        }catch(Exception e){
            e.printStackTrace();
            throw new Error ("Mecker mecker...");
        }
    }










}