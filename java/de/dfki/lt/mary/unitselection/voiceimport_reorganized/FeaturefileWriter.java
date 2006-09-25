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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;
import de.dfki.lt.mary.util.FileUtils;

public class FeaturefileWriter implements VoiceImportComponent
{
    protected File maryDir;
    protected File featureFile;
    protected UnitFileReader unitFileReader;
    protected File unitfeatureDir;
    protected FeatureDefinition featureDefinition;
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    
    public FeaturefileWriter( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;

        maryDir = new File( db.maryDirName() );
        if (!maryDir.exists()) {
            maryDir.mkdir();
            System.out.println("Created the output directory [" + db.maryDirName() + "] to store the feature file." );
        }
        featureFile = new File( db.targetFeaturesFileName() );
        unitFileReader = new UnitFileReader( db.unitFileName() );
        unitfeatureDir = new File(db.unitFeaDirName());
        File weightsFile = new File(db.weightsFileName());
        featureDefinition = new FeatureDefinition();
        featureDefinition.readTextWithWeights(new BufferedReader(new InputStreamReader(new FileInputStream(weightsFile), "UTF-8")));

    }
    
    public boolean compute() throws IOException
    {
        System.out.println("Featurefile writer started.");

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(featureFile)));
        new MaryHeader(MaryHeader.TARGETFEATS).write(out);
        featureDefinition.writeBinary(out);
        
        
        out.writeInt(unitFileReader.getNumberOfUnits());
        int index = 0; // the unique index number of units in the unit file
        // Dummy feature vector corresponding to an edge unit:
        FeatureVector edge = featureDefinition.getEmptyFeatureVector();
        // Loop over all utterances
        for (int i=0; i<bnl.getLength(); i++) {
            BufferedReader uttFeats = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitFeaDirName() + bnl.getName(i) + db.unitFeaExt() )), "UTF-8"));
            FeatureDefinition uttFeatDefinition = new FeatureDefinition();
            uttFeatDefinition.readTextWithoutWeights(uttFeats);
            if (!uttFeatDefinition.featureEquals(featureDefinition)) {
                throw new IllegalArgumentException("Features in file "+bnl.getName(i)+" do not match definition file "+db.weightsFileName());
            }
            // skip the clear text section: read until an empty line occurs
            String line;
            while ((line = uttFeats.readLine()) != null) {
                if (line.trim().equals("")) break;
            }
            // Empty entry corresponding to start of utterance:
            if (!unitFileReader.isEdgeUnit(index)) {
                throw new IOException("Inconsistency between feature files and unit file: Unit "
                        +index+" should correspond to start of file "+bnl.getName(i)
                        +", but is not an edge unit!");
            }
            edge.write(out);
            index++;
            // read the binary section, and write it
            while ((line = uttFeats.readLine()) != null) {
                if (line.trim().equals("")) break;
                FeatureVector fv = featureDefinition.toFeatureVector(line);
                if (unitFileReader.isEdgeUnit(index)) {
                    throw new IOException("Inconsistency between feature files and unit file: Unit "
                            +index+" should correspond to feature line '"+line+"' of file "+bnl.getName(i)
                            +", but is an edge unit!");
                }
                fv.write(out);
                index++;
            }
            if (!unitFileReader.isEdgeUnit(index)) {
                throw new IOException("Inconsistency between feature files and unit file: Unit "
                        +index+" should correspond to end of file "+bnl.getName(i)
                        +", but is not an edge unit!");
            }
            edge.write(out);
            index++;
            System.out.println( "    " + bnl.getName(i) + " (" + index + ")" );
        }
        out.close();
        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        new FeaturefileWriter( null, null ).compute();
    }

}
