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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.JoinCostFeatures;
import de.dfki.lt.mary.unitselection.PrecompiledJoinCostReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class JoinCostPrecomputer extends VoiceImportComponent
{
    
    private DatabaseLayout db = null;
    private int percent = 0;
    
    private int numberOfFeatures = 0;
    private float[] fw = null;
    private String[] wfun = null;
    
    public final String JOINCOSTFILE = "JoinCostPrecomputer.joinCostFile";
    public final String JOINCOSTFEATURESFILE = "JoinCostPrecomputer.joinCostFeaturesFile";
    public final String UNITFEATURESFILE = "JoinCostPrecomputer.unitFeaturesFile";
    public final String UNITFILE = "JoinCostPrecomputer.unitFile";
    
    public String getName(){
        return "JoinCostPrecomputer";
    }
 
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           String filedir = db.getProp(db.FILEDIR);
           String maryExt = db.getProp(db.MARYEXT);
           props.put(JOINCOSTFILE,filedir+"joinCosts"+maryExt);
           props.put(JOINCOSTFEATURESFILE,filedir+"joinCostFeatures"+maryExt);
           props.put(UNITFEATURESFILE,filedir+"halfphoneFeatures"+maryExt);
           props.put(UNITFILE,filedir+"halfphoneUnits"+maryExt);
       }
       return props;
    }
    
    protected void setupHelp(){         
        props2Help = new TreeMap();
        props2Help.put(JOINCOSTFILE,"file containing the join costs for every halfphone unit pair."
                +" Will be created by this module");
        props2Help.put(JOINCOSTFEATURESFILE,"file containing all halfphone units and their join cost features");
        props2Help.put(UNITFEATURESFILE,"file containing all halfphone units and their target cost features");
        props2Help.put(UNITFILE,"file containing all halfphone units");
    }
    
    public boolean compute() throws IOException
    {
        System.out.println("---- Precomputing join costs");
        int retainPercent = Integer.getInteger("joincostprecomputer.retainpercent", 10).intValue();
        int retainMin = Integer.getInteger("joincostprecomputer.retainmin", 20).intValue();
        System.out.println("Will retain the top "+retainPercent+"% (but at least "+retainMin+") of all joins within a phoneme");
        
        /* Make a new join cost file to write to */
        DataOutputStream jc = null;
        try {
            jc = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(getProp(JOINCOSTFILE))));
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "Can't create the join cost file [" + getProp(JOINCOSTFILE) + "]. The path is probably wrong.", e );
        }
        
        /**********/
        /* HEADER */
        /**********/
        /* Make a new mary header and ouput it */
        MaryHeader hdr = new MaryHeader( MaryHeader.PRECOMPUTED_JOINCOSTS );
        try {
            hdr.writeTo( jc );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "An IOException occurred when writing the Mary header to the Join Cost file.", e );
        }
        hdr = null;
        
        FeatureFileReader unitFeatures = FeatureFileReader.getFeatureFileReader(getProp(UNITFEATURESFILE));
        JoinCostFeatures joinFeatures = new JoinCostFeatures(getProp(JOINCOSTFEATURESFILE));
        UnitFileReader units = new UnitFileReader(getProp(UNITFILE));
        if (unitFeatures.getNumberOfUnits() != joinFeatures.getNumberOfUnits())
            throw new IllegalStateException("Number of units in unit and join feature files does not match!");
        if (unitFeatures.getNumberOfUnits() != units.getNumberOfUnits())
            throw new IllegalStateException("Number of units in unit file and unit feature file does not match!");
        int numUnits = unitFeatures.getNumberOfUnits();
        FeatureDefinition def = unitFeatures.getFeatureDefinition();
        int iPhoneme = def.getFeatureIndex("mary_phoneme");
        int nPhonemes = def.getNumberOfValues(iPhoneme);
        List[] left = new List[nPhonemes]; // left half phones grouped by phoneme
        List[] right = new List[nPhonemes]; // right half phones grouped by phoneme
        for (int i=0; i<nPhonemes; i++) {
            left[i] = new ArrayList();
            right[i] = new ArrayList();
        }
        int iLeftRight = def.getFeatureIndex("mary_halfphone_lr");
        byte vLeft = def.getFeatureValueAsByte(iLeftRight, "L");
        byte vRight = def.getFeatureValueAsByte(iLeftRight, "R");
        for (int i=0; i<numUnits; i++) {
            FeatureVector fv = unitFeatures.getFeatureVector(i); 
            int phoneme = fv.getFeatureAsInt(iPhoneme);
            assert 0 <= phoneme && phoneme < nPhonemes;
            byte lr = fv.getByteFeature(iLeftRight);
            Unit u = units.getUnit(i);
            if (lr == vLeft) {
                left[phoneme].add(u);
            } else if (lr == vRight) {
                right[phoneme].add(u);
            }
        }
        
        System.out.println("Sorted units by phoneme and halfphone. Now computing costs.");
        int totalLeftUnits = 0;
        for (int i=0; i<nPhonemes; i++) {
            totalLeftUnits += left[i].size();
        }
        jc.writeInt(totalLeftUnits);
        for (int i=0; i<nPhonemes; i++) {
            String phoneSymbol = def.getFeatureValueAsString(iPhoneme, i);
            int nLeftPhoneme = left[i].size();
            int nRightPhoneme = right[i].size();
            System.out.println(phoneSymbol+": "+nLeftPhoneme+" left, "+nRightPhoneme+" right half phones");
            for (int j=0; j<nLeftPhoneme; j++) {
                Unit uleft = (Unit) left[i].get(j);
                SortedMap sortedCosts = new TreeMap();
                int ileft = uleft.getIndex();
                //System.out.println("Left unit "+j+" (index "+ileft+")");
                jc.writeInt(ileft);
                // Now for this left halfphone, compute the cost of joining to each
                // right halfphones of the same phoneme, and remember only the best.
                for (int k=0; k<nRightPhoneme; k++) {
                    Unit uright = (Unit) right[i].get(k);
                    int iright = uright.getIndex();
                    //System.out.println("right unit "+k+" (index "+iright+")");
                    double cost = joinFeatures.cost(ileft, iright);
                    Double dCost = new Double(cost);
                    // make sure we don't overwrite any existing entry:
                    if (!sortedCosts.containsKey(dCost)) {
                        sortedCosts.put(dCost, uright);
                    } else {
                        Object value = sortedCosts.get(dCost);
                        List newVal = new ArrayList();
                        if (value instanceof List)
                            newVal.addAll((List)value);
                        else
                            newVal.add(value);
                        newVal.add(uright);
                        sortedCosts.put(dCost, newVal);
                    }
                }
                // Number of joins we will retain:
                int nRetain = nRightPhoneme * retainPercent / 100;
                if (nRetain < retainMin) nRetain = retainMin;
                if (nRetain > nRightPhoneme) nRetain = nRightPhoneme;
                jc.writeInt(nRetain);
                Iterator it=sortedCosts.keySet().iterator();
                for (int k=0; k<nRetain; ) {
                    Double cost = (Double) it.next();
                    float fcost = cost.floatValue();
                    Object ob = sortedCosts.get(cost);
                    if (ob instanceof Unit) {
                        Unit u = (Unit) ob;
                        int iright = u.getIndex();
                        jc.writeInt(iright);
                        jc.writeFloat(fcost);
                        k++;
                    } else {
                        assert ob instanceof List;
                        List l = (List) ob;
                        for (Iterator li = l.iterator(); k<nRetain && li.hasNext(); ) {
                            Unit u = (Unit) li.next();
                            int iright = u.getIndex();
                            jc.writeInt(iright);
                            jc.writeFloat(fcost);
                            k++;
                        }
                    }
                }
            }
            percent += 100*nLeftPhoneme/totalLeftUnits;
        }
        jc.close();
        PrecompiledJoinCostReader tester = new PrecompiledJoinCostReader(getProp(JOINCOSTFILE));
        return true;
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
