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

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.HalfPhoneFeatureFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

public class HalfPhoneFeatureFileWriter extends FeatureFileWriter
{
    protected FeatureDefinition leftFeatureDef;
    protected FeatureDefinition rightFeatureDef;

    public HalfPhoneFeatureFileWriter(DatabaseLayout setdb, BasenameList setbnl)
    {
        super(setdb, setbnl);
    }
    
    /**
     * Set some global variables; sub-classes may want to override.
     *
     */
    protected void init()
    {
        unitfeatureDir = new File(db.halfphoneUnitFeaDirName());
        featsExt = db.halfphoneUnitFeaExt();
    }
    
    /* use default feature file format:
    protected void readFeatureDefinition() throws IOException
    {
        String leftWeights = db.halfPhoneLeftWeightsFileName();
        String rightWeights = db.halfPhoneRightWeightsFileName();
        leftFeatureDef = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(leftWeights), "UTF-8")), true); // true: read weights
        rightFeatureDef = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(rightWeights), "UTF-8")), true); // true: read weights
        if (!leftFeatureDef.featureEquals(rightFeatureDef))
            throw new IllegalStateException("left and right feature definitions are not compatible ("+leftWeights+" vs. "+rightWeights+")");
        // set the variable used in super class to one of them:
        featureDefinition = leftFeatureDef;
    }
    */

    /**
     * Write the header of this feature file to the given DataOutput
     * @param out
     * @throws IOException
     */
    /* use default feature file format
    protected void writeHeaderTo(DataOutput out) throws IOException
    {
        new MaryHeader(MaryHeader.HALFPHONE_UNITFEATS).writeTo(out);
        leftFeatureDef.writeBinaryTo(out);
        rightFeatureDef.writeBinaryTo(out);
    }
    */
    
}
