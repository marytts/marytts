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
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.JoinCostFunction;
import de.dfki.lt.mary.unitselection.Unit;

/**
 * A join cost function for evaluating the goodness-of-fit of 
 * a given pair of left and right unit.
 * @author Marc Schr&ouml;der
 *
 */
public class ClusterJoinCostFunction implements JoinCostFunction
{
    private float[] weights;
    private String[] weightFuncts;
    
    private float[][] leftValues;
    private float[][] rightValues;
    
    private Logger logger;
    
    public ClusterJoinCostFunction()
    {
        this.logger = Logger.getLogger("ClusterJoinCostFunction");
    }
    
    public void load(RandomAccessFile raf) throws IOException {
        int numFeats = raf.readInt();
        weights = new float[numFeats];
        weightFuncts = new String[numFeats];
        for (int i=0;i<numFeats;i++){
            weights[i] = raf.readFloat();
            weightFuncts[i] = raf.readUTF();
        }
        int numUnits = raf.readInt();
        leftValues = new float[numUnits][numFeats];
        rightValues = new float[numUnits][numFeats];
        for (int i=0;i<numUnits;i++){
            for(int j=0;j<numFeats;j++){
               leftValues[i][j] = raf.readFloat();
            }
            for(int j=0;j<numFeats;j++){
               rightValues[i][j] = raf.readFloat();
           } 
            
        }
        if (raf.getFilePointer() != raf.length()){
            throw new Error("Read only "+raf.getFilePointer()
                    +" bytes of data, but there are "+raf.length()
                    +" bytes in file.");
        }
        
    }
    
    public void overwriteWeights(BufferedReader reader) throws IOException{
        String line=reader.readLine();
        int i=0;
        while (line!=null && (!line.startsWith("***"))){
            StringTokenizer tok = new StringTokenizer(line);
            weights[i] = Float.parseFloat(tok.nextToken());
            weightFuncts[i] = tok.nextToken();            
            line = reader.readLine();
            i++;
        }
        
    }
    
    /**
     * Compute the goodness-of-fit of joining two units. 
     * @param left the proposed left unit
     * @param right the proposed right unit
     * @return a non-negative integer; smaller values mean better fit, i.e. smaller cost.
     **/
    public int cost(Unit left, Unit right){
        int leftIndex = left.getIndex();
        int rightIndex = right.getIndex();
        
        int cost = 0;
        //for all features, calculate costs
        for (int i=0;i<weights.length;i++){
            float weight = weights[i];
            String func = weightFuncts[i];           
            float leftVal = leftValues[leftIndex][i];
            float rightVal = rightValues[rightIndex][i];
            if (func.equals("0")){
                //do simple comparison
                cost = (int) (Math.abs(leftVal-rightVal)*weight);
            } else {
                //TODO: implement step20 comparison
                cost = (int) (Math.abs(leftVal-rightVal)*weight);
            }
        }
        return cost;
    }
    
}
