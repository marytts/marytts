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
package de.dfki.lt.mary.unitselection.clunits;

import de.dfki.lt.mary.unitselection.*;
import de.dfki.lt.mary.unitselection.cart.PathExtractorImpl;
import de.dfki.lt.mary.unitselection.featureprocessors.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;

/**
 * A target cost function for evaluating the goodness-of-fit of 
 * a given unit for a given target. 
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */
public class ClusterTargetCostFunction implements TargetCostFunction
{
    private Map features2Extractor;
    private List byteFeats;
    private float[] byteWeights;
    private List byteVals;
    private List shortFeats;
    private float[] shortWeights;
    private List shortVals;
    private List floatFeats;
    private float[] floatWeights;
    private String[] floatWeightFuncts;
    
    private int numByteFeats;
    private int numShortFeats;
    private int numFloatFeats;
    
    private List unitByteVals;
    private List unitShortVals;
    private List unitFloatVals;
       
    private Logger logger;
    
    public ClusterTargetCostFunction() {
        this.logger = Logger.getLogger("ClusterTargetCostFunction");
    }
    
    /**
    
    
    /**
     * Compute the goodness-of-fit of a given unit for a given target.
     * @param target 
     * @param unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public int cost(Target target, Unit unit)
    {
     //go through the features and compare
     //logger.debug("Now calculating cost for "+unitType+" "
     //        +unitInstance+" "+target.getIndex());               
     int cost = 0;
     //go through byte values
     byte[] byteValsOfUnit = (byte[])unitByteVals.get(unit.getIndex());
     for (int i = 0;i < numByteFeats; i++){
          float weight = byteWeights[i];
          if (weight != 0){
              //extract the targets value
              String nextFeature = (String)byteFeats.get(i);
              String targetVal = target.getValueForByteFeature(i);
              if (targetVal == null){
                  	targetVal = (String)
                  	((PathExtractorImpl)features2Extractor.get(nextFeature)).findFeature(target.getItem());
                  	//save the value
                  	target.addByteValue(targetVal,byteFeats.size(),i);
              }                  
              //extract the units value
              String unitValue = ((String[])byteVals.get(i))[byteValsOfUnit[i]];
              //compare the strings
              if (targetVal != null && unitValue != null){
                  int result = compareString(targetVal, unitValue, weight);
                  cost+= result;
              } else {
                  cost += weight;
              }
          }
     }
     //go through short values
     short[] shortValsOfUnit = (short[])unitShortVals.get(unit.getIndex());
     for (int i = 0;i < numShortFeats; i++){
          float weight = shortWeights[i];
          if (weight != 0){
              //extract the targets value
              String nextFeature = (String)shortFeats.get(i);
              String targetVal = target.getValueForShortFeature(i);
              if (targetVal == null){
                  	targetVal = (String)
                  	((PathExtractorImpl)features2Extractor.get(nextFeature)).findFeature(target.getItem());
                  	//save the value
                  	target.addShortValue(targetVal,shortFeats.size(),i);
              }                  
              //extract the units value
              String unitValue = ((String[])shortVals.get(i))[shortValsOfUnit[i]];
              //compare the strings
              if (targetVal != null && unitValue != null){
                  int result = compareString(targetVal, unitValue, weight);
                  cost+= result;
              } else {
                  cost += weight;
              }
          }
     }
     //go through float values
     float[] floatValsOfUnit = (float[])unitFloatVals.get(unit.getIndex());
     for (int i = 0;i < numFloatFeats; i++){
         
          float weight = floatWeights[i];
          String func = floatWeightFuncts[i];
          if (weight != 0){
              //extract the targets value
              String nextFeature = (String)floatFeats.get(i);
              float targetVal = target.getValueForFloatFeature(i);
              if (targetVal == -1){
                    targetVal = Float.parseFloat((String)
                    ((PathExtractorImpl)features2Extractor.get(nextFeature)).findFeature(target.getItem()));
                    //save the value
                    target.addFloatValue(targetVal,floatFeats.size(),i);
              }                  
              //extract the units value
              float unitValue = floatValsOfUnit[i];
              //compare the strings
              if (targetVal != -1 && unitValue != -1){
                  int result = compareFloat(targetVal, unitValue, weight,func);
                  cost+= result;
              } else {
                  cost += weight;
              }
          }
     }
                /**
                if (logger.getEffectiveLevel().equals(Level.DEBUG)){
                    StringBuffer sb = new StringBuffer();
                    sb.append("Succesfully calculated cost: "+cost
                        +" for target "+target.getName()
                        +" and unit "+unit.toString());                    
                    sb.append("Values for the features:\n ");
                    for (int i = 0; i<costEnum.length;i++){
                        if (costEnum[i] != 0){
                            sb.append(i+": "+costEnum[i]+" ");
                        }
                    }
                    logger.debug(sb.toString());
                }
                **/
                return cost;
            
        }
    
    
    
    
    
    /**
     * Compare two values considering a weight
     * @param unitValue the unit Value
     * @param weight the weight
     * @return the resulting cost
     */
    private int compareString(String targetValue, 
            			String unitValue, 
            			float weight)
    {
        //logger.debug("Comparing Strings "+targetValue+" and "+unitValue); 
        int result = 1;
         if (targetValue.equals(unitValue)){
                result = 0;
         }
         return (int) weight*result;
    }
    
    /**
     * Compare two values considering a weight
     * @param unitValue the unit Value
     * @param weight the weight
     * @return the resulting cost
     */
    private int compareFloat(float targetFloat, 
            			float unitFloat, 
            			float weight,
                        String func)
    {
        int result = 0; 
        if (func.equals("0")){
            // rounding errors are negligible for costs usually above 100.000
            result = (int) (Math.abs(targetFloat-unitFloat)*weight);
            //logger.debug("Multiplying "+targetFloat+" minus "+unitFloat
            //      +" with "+weight+" equals "+result);
        } else {
            if (func.equals("step20")){
                //TODO: Write right definition of step20 function here
                result = (int) (Math.abs(targetFloat-unitFloat)*weight);
            }
        }
        return result;
    }
    
    
    /**
     * Set the features of the cost function
     * @param featusNWeights a List of Strings containing
     * 		  a feature, weight and type
     * @param featProc a feature processors manager
     **/
    public void load(RandomAccessFile raf,
            	UnitSelectionFeatProcManager featProc) throws IOException
    {
        //read in the byte features and their possible values
        int numFeats = raf.readInt();
        byteFeats = new ArrayList();
        byteWeights = new float[numFeats];
        byteVals = new ArrayList();
        for (int i=0; i<numFeats;i++){
            float weight = raf.readFloat();
            String name = raf.readUTF();
            int numValues = raf.read();
            String[] values = new String[numValues];
            for (int j=0; j<numValues;j++){
                String value = raf.readUTF();
                values[j] = value;
            }
            byteFeats.add(name);
            byteWeights[i] = weight;
            byteVals.add(values);
        }
        
        //read in the short features and their possible values
        numFeats = raf.readInt();
        shortFeats = new ArrayList();
        shortWeights = new float[numFeats];
        shortVals = new ArrayList();
        for (int i=0; i<numFeats;i++){
            float weight = raf.readFloat();
            String name = raf.readUTF();
            int numValues = raf.read();
            String[] values = new String[numValues];
            for (int j=0; j<numValues;j++){
                String value = raf.readUTF();
                values[j] = value;
            }
            shortFeats.add(name);
            shortWeights[i] = weight;
            shortVals.add(values);
        }
        
        //read in the float features
        numFeats = raf.readInt();
        floatFeats = new ArrayList();
        floatWeights = new float[numFeats];
        floatWeightFuncts = new String[numFeats];
        for (int i=0; i<numFeats;i++){
            float weight = raf.readFloat();
            String weightFunc = raf.readUTF();
            String name = raf.readUTF();
            floatFeats.add(name);
            floatWeights[i] = weight;
            floatWeightFuncts[i] = weightFunc;
        }
        numByteFeats = byteFeats.size();
        numShortFeats = shortFeats.size();
        numFloatFeats = floatFeats.size();
        //read in the values for the units
        int numUnits = raf.readInt();
        unitByteVals = new ArrayList();
        unitShortVals = new ArrayList();
        unitFloatVals = new ArrayList();
        for (int i=0; i<numUnits;i++){
            byte[] bValues = new byte[numByteFeats]; 
            for (int j=0;j<numByteFeats;j++){
                bValues[j] = (byte)raf.read();
            }
            unitByteVals.add(bValues);
            short[] sValues = new short[numShortFeats]; 
            for (int j=0;j<numShortFeats;j++){
                sValues[j] = raf.readShort();
            }
            unitShortVals.add(sValues);
            float[] fValues = new float[numFloatFeats]; 
            for (int j=0;j<numFloatFeats;j++){
                fValues[j] = raf.readFloat();
            }
            unitFloatVals.add(fValues);
        }
        
        //build a PathExtractor for each feature
        features2Extractor = new HashMap();
        for (int i=0;i<numByteFeats;i++){
            String nextFeature = (String) byteFeats.get(i);
            PathExtractorImpl pathEx = new PathExtractorImpl(nextFeature,true);
            pathEx.setFeatureProcessors((UtteranceFeatProcManager)featProc);
            features2Extractor.put(nextFeature,pathEx);
        }
        for (int i=0;i<numShortFeats;i++){
            String nextFeature = (String) shortFeats.get(i);
            PathExtractorImpl pathEx = new PathExtractorImpl(nextFeature,true);
            pathEx.setFeatureProcessors((UtteranceFeatProcManager)featProc);
            features2Extractor.put(nextFeature,pathEx);
        }
        for (int i=0;i<numFloatFeats;i++){
            String nextFeature = (String) floatFeats.get(i);
            PathExtractorImpl pathEx = new PathExtractorImpl(nextFeature,true);
            pathEx.setFeatureProcessors((UtteranceFeatProcManager)featProc);
            features2Extractor.put(nextFeature,pathEx);
        }
        
    } 
    
    public void overwriteWeights(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        while (line!=null){
                if (!line.startsWith("***")){
                    
                    StringTokenizer tok = 
                        new StringTokenizer(line," ");
                   
                    String name = tok.nextToken();
                    String type = tok.nextToken();
                    float weight = Float.parseFloat(tok.nextToken());
                    //check, if value type is String of Float
                    if (type.equals("Byte") || type.equals("byte")){
                        //get index and store new weight
                        int index = byteFeats.indexOf(name);
                        byteWeights[index] = weight;
                    } else {
                        if (type.equals("Short") || type.equals("short")){
                           int index = shortFeats.indexOf(name);
                           shortWeights[index] = weight;
                        } else {
                            if (type.equals("Float") || type.equals("float")){
                                int index = floatFeats.indexOf(name);
                                floatWeights[index] = weight;
                                floatWeightFuncts[index] = tok.nextToken();
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
        
    }
    
   
    
    
        
}
