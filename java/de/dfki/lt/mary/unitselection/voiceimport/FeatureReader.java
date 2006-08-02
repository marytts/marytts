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

package de.dfki.lt.mary.unitselection.voiceimport;

import java.util.*;
import java.io.*;


/**
 * Class for reading in the target features from a file written by the user
 * and the target values of the units from the festival/clunits/features
 * directory in the Festvox directory of a voice;
 * and for dumping the data in MARY-Format.
 * 
 * @author Anna Hunecke
 *
 */
public class FeatureReader
{
    
    private CentralDatabase database;
    private String featsDefsFile;
    private String unitsFeatsDir;
    private List byteFeats;
    private List byteWeights;
    private List shortFeats;
    private List shortWeights;
    private List floatFeats;
    private List floatWeights;
    private List floatWeightFuncts;
    private Map featsNValues;
    private String comments;
       
    public int numByteFeats;
    public int numShortFeats;
    public int numFloatFeats;
    
    /**
     * Build a new FeatureReader
     * @param database the database
     * @param featsDefsFile the file containing 
     *        the feature/weight definitions
     * @param unitsFeatsDir the directory containing 
     *        the features of the units
     */
    public FeatureReader(CentralDatabase database,String featsDefsFile,
			 String unitsFeatsDir){
        this.database = database;
        this.featsDefsFile = featsDefsFile;
        this.unitsFeatsDir = unitsFeatsDir;
    }
    
    /**
     * Read in the features and the feature values of
     * the units. Then read in the values of all units and store 
     * them in the individual units.
     * @return the features/weights
     */
    public void readFeatures()
    {	
        try{
            //Read in the feature definitions
            byteFeats = new ArrayList();
            byteWeights = new ArrayList();
            shortFeats = new ArrayList();
            shortWeights = new ArrayList();
            floatFeats = new ArrayList();
            floatWeights = new ArrayList();
            floatWeightFuncts = new ArrayList();
            System.out.println("Reading features from "+featsDefsFile);
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(new 
                        FileInputStream(new 
                                File(featsDefsFile)),"UTF-8"));
            String line = reader.readLine();

            //analyse and sort feats according to type (String or Float)
            //keep original order of features and store their type
            List types = new ArrayList();
            List names = new ArrayList();
            int numFeats = 0;
            StringBuffer coms = new StringBuffer();
            while (line!=null){
                if (line.startsWith("***")){
                    if (numFeats == 0){
                        //we are still at the beginning of the file
                        //save the comments
                        coms.append(line+"\n");
                    }
                } else {
                    numFeats++;
                    StringTokenizer tok = 
                        new StringTokenizer(line," ");
                    //Store the name of the feature
                    String name = tok.nextToken();
                    names.add(name);
                    String type = tok.nextToken();
                    String weight = tok.nextToken();
                    //check, if value type is String of Float
                    if (type.equals("Byte") || type.equals("byte")){
                        types.add(new Character('b'));
                        byteFeats.add(name);
                        byteWeights.add(Float.valueOf(weight));
                    } else {
                        if (type.equals("Short") || type.equals("short")){
                            types.add(new Character('s'));
                            shortFeats.add(name);
                            shortWeights.add(Float.valueOf(weight));
                        } else {
                            if (type.equals("Float") || type.equals("float")){
                                types.add(new Character('f'));
                                floatFeats.add(name);
                                floatWeights.add(Float.valueOf(weight));
                                floatWeightFuncts.add(tok.nextToken());
                            } else {
                                throw new Error("Wrong Value Type \""+type+
                                "\" in line \""+line
                                +"\" of feature-weights file. Value type has"+
                                " to be Byte, Short or Float");
                            }
                        }
                    }
                }
                line = reader.readLine();
            }
            //store the comments
            comments = coms.toString();
                 
            //store number of values/features
            numByteFeats = byteFeats.size();
            numShortFeats = shortFeats.size();
            numFloatFeats = floatFeats.size();
            
            //Read in the each unit type with all its units and their features
            File featsDir = new File(unitsFeatsDir);
            System.out.println("Reading values from directory "+unitsFeatsDir);
            if (featsDir.isDirectory()){
                File[] entries = featsDir.listFiles();
                int startIndex = (unitsFeatsDir).length();
                featsNValues = new HashMap();
                //go through all files in directory
                for (int i = 0; i<entries.length;i++){
                    //determine the name of the file = unit type
                    String unitType = entries[i].toString();
                    
                    int endIndex = unitType.length()-6;
                   
                    unitType = unitType.substring(startIndex+1, endIndex);
                    //System.out.println("unitType is "+unitType);
                    List units = database.getUnitsForType(unitType);
                    if (units == null){
                        if (!unitType.endsWith("something")){
                            System.out.println("Cannot find unit type : "+unitType);
                        } 
                        continue;
                    } else {
                        int indexLength = units.size() -1;
                        //open the file
                        //System.out.println("Opening file "+entries[i].toString());
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
                            Unit unit;
                            if (unitIndex<0 || unitIndex>indexLength){
                                System.out.println("Index "+unitIndex
                                        +" out of bounds for type "+unitType
                                        +" which has "+units.size()
                                        +" units");
                                unit = null;
                            } else {
                                unit = 
                                    database.getUnit(((Integer)units.get(unitIndex)).intValue());
                            }
                            //if the unit is in the database
                            if (unit!=null){
                                //read in the values and store them in the unit
                                byte[] byteValues = new byte[byteFeats.size()];
                                short[] shortValues = new short[shortFeats.size()];
                                float[] floatValues = new float[floatFeats.size()];
                                byte byteIndex = 0;
                                short shortIndex = 0;
                                int floatIndex = 0;
                                for (int j=0;j<numFeats; j++){
                                    if (tok.hasMoreTokens()){
                                        String nextValue = tok.nextToken();
                                        
                                        //get the feature type
                                        char type = ((Character) types.get(j)).charValue();
                                        if (type == 'b'){ //type is Byte
                                            String featureName = (String)names.get(j);
                                            if (featsNValues.containsKey(featureName)){
                                                List values = (List)featsNValues.get(featureName);
                                                if (values.contains(nextValue)){
                                                    byteValues[byteIndex] = (byte)values.indexOf(nextValue);
                                                } else {
                                                    values.add(nextValue);
                                                    byteValues[byteIndex] = (byte)values.indexOf(nextValue);
                                                }
                                            } else {
                                                List values = new ArrayList();
                                                values.add(nextValue);
                                                byteValues[byteIndex] = 0;
                                                featsNValues.put(featureName, values);
                                            }
                                            byteIndex++;
                                        } else {
                                            if (type == 's'){ //type is short
                                                String featureName = (String)names.get(j);
                                                if (featsNValues.containsKey(featureName)){
                                                    List values = (List)featsNValues.get(featureName);
                                                    if (values.contains(nextValue)){
                                                        shortValues[shortIndex] = (short)values.indexOf(nextValue);
                                                    } else {
                                                        values.add(nextValue);
                                                        shortValues[shortIndex] = (short)values.indexOf(nextValue);
                                                    }
                                                } else {
                                                    List values = new ArrayList();
                                                    values.add(nextValue);
                                                    shortValues[shortIndex] = 0;
                                                    featsNValues.put(featureName, values);
                                                }
                                                shortIndex++;
                                            } else {
                                            if (type == 'f'){
                                                float f = Float.parseFloat(nextValue);
                                                //System.out.print(nextValue+" -> "+f+"; ");
                                                floatValues[floatIndex] = f;
                                                floatIndex++;
                                            }
                                        }
                                        }
                                    } else {
                                        throw new Error ("Mismatch in feature file "
                                                +unitType+", unit "+unitIndex
                                                +"; feature "+names.get(j)
                                                +" is missing");}
                            		}
                                unit.setValues(byteValues, shortValues, floatValues);
                                nextUnit=unitsReader.readLine();
                                
                            } else {
                                nextUnit = null;
                            }    
                        }
                    
                  
                    }
                }
            }  
            
            
        }catch (FileNotFoundException fe){
            System.out.println("Can not read features: "+fe.getMessage());
            throw new Error("Error reading features");
        }catch(Exception e){
            e.printStackTrace();
            throw new Error ("Error reading features");
        }
        
    }
    
    public void rewriteFeatDefs(){
        try{
            //open feature definition file
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
                FileOutputStream(new 
                        File(featsDefsFile)),"UTF-8"));
            //dump initial comments
            writer.write(comments);
            //dump the byte features
            for (int i=0; i<numByteFeats;i++){
                writer.write(byteFeats.get(i)
                		+ " Byte "
                		+ byteWeights.get(i)
                		+"\n");
            }
            //dump the short features
            for (int i=0; i<numShortFeats;i++){
                writer.write(shortFeats.get(i)
                		+ " Short "
                		+ shortWeights.get(i));
            }
            //dump the float features
            for (int i=0; i<numFloatFeats;i++){
                writer.write(floatFeats.get(i)
                		+ " Float "
                		+ floatWeights.get(i));
            }
            //finish
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error in FeatureReader while dumping feature definitions");
        }
    
    }
    
    public void dumpFeaturesAndValues(DataOutputStream out) {
        try{
            //dump the byte Features, their weights and their values
            out.writeInt(numByteFeats);
            for (int i=0; i<numByteFeats;i++){
                String name = (String) byteFeats.get(i);
                float weight = ((Float) byteWeights.get(i)).floatValue();
                out.writeFloat(weight);
                out.writeUTF(name);
                List values = (List)featsNValues.get(name);
                out.write((byte) values.size());
                for (int j=0;j<values.size();j++){
                    out.writeUTF((String) values.get(j));
                }
            }
            
            //dump the short Features, their weights and their values
            out.writeInt(numShortFeats);
            for (int i=0; i<numShortFeats;i++){
                String name = (String) shortFeats.get(i);
                float weight = ((Float) shortWeights.get(i)).floatValue();
                out.writeFloat(weight);
                out.writeUTF(name);
                List values = (List)featsNValues.get(name);
                out.write((short) values.size());
                for (int j=0;j<values.size();j++){
                    out.writeUTF((String) values.get(j));
                }
            }
            
            //dump the float Features and their weights
            out.writeInt(numFloatFeats);
            for (int i=0; i<numFloatFeats;i++){
                String name = (String) floatFeats.get(i);
                float weight = ((Float) floatWeights.get(i)).floatValue();
                String weightFunc = (String) floatWeightFuncts.get(i);
                out.writeFloat(weight);
                out.writeUTF(weightFunc);
                out.writeUTF(name);
            }
            
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error in FeatureReader while dumping target features");
        }
        
        
    }
        
}