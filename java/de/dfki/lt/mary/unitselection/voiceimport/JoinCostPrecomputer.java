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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.JoinCostFeatures;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.util.MaryUtils;

public class JoinCostPrecomputer implements VoiceImportComponent
{
    
    private DatabaseLayout db = null;
    private BasenameList bnl = null;
    private int percent = 0;
    
    private int numberOfFeatures = 0;
    private float[] fw = null;
    private String[] wfun = null;
    
    /** Constructor */
    public JoinCostPrecomputer( DatabaseLayout setdb, BasenameList setbnl ) {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    
    public boolean compute() throws IOException
    {
        System.out.println("---- Precomputing join costs");
        System.out.println("Base directory: " + db.rootDirName());
        System.out.println("Reading join cost features file from: " + db.joinCostFeaturesFileName());
        System.out.println("Reading unit feature file from: " + db.targetFeaturesFileName());
        System.out.println("Reading unit file from: " + db.unitFileName());
        System.out.println("Writing precomputed join costs file to: " + db.precomputedJoinCostsFileName());
        
        /* Make a new join cost file to write to */
        DataOutputStream jc = null;
        try {
            jc = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( db.precomputedJoinCostsFileName() ) ) );
        }
        catch ( FileNotFoundException e ) {
            throw new RuntimeException( "Can't create the join cost file [" + db.precomputedJoinCostsFileName() + "]. The path is probably wrong.", e );
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
            throw new RuntimeException( "An IOException happened when writing the Mary header to the Join Cost file.", e );
        }
        hdr = null;
        
        FeatureFileReader unitFeatures = FeatureFileReader.getFeatureFileReader(db.targetFeaturesFileName());
        JoinCostFeatures joinFeatures = new JoinCostFeatures(db.joinCostFeaturesFileName());
        UnitFileReader units = new UnitFileReader(db.unitFileName());
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
        // TODO: write to jc
        for (int i=0; i<nPhonemes; i++) {
            int nLeftPhoneme = left[i].size();
            int nRightPhoneme = right[i].size();
            for (int j=0; j<nLeftPhoneme; j++) {
                Unit uleft = (Unit) left[i].get(j);
                int ileft = uleft.getIndex();
                // TODO: write to jc
                // Now for this left halfphone, compute the cost of joining to each
                // right halfphones of the same phoneme, and remember only the best.
                for (int k=0; k<nRightPhoneme; k++) {
                    Unit uright = (Unit) right[i].get(k);
                    int iright = uright.getIndex();
                    double cost = joinFeatures.cost(ileft, iright);
                    // TODO: Now save the cost somewhere.
                }
                // TODO: for this left phoneme, save acceptable right units with their join costs 
            }
            percent += 100*nLeftPhoneme/totalLeftUnits;
        }
        jc.close();
        return false;
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
