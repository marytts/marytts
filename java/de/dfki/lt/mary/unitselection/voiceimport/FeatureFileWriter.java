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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import de.dfki.lt.mary.unitselection.FeatureFileReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

public class FeatureFileWriter implements VoiceImportComponent
{
    protected File maryDir;
    protected String featureFileName;
    protected String unitFileName;
    protected String weightsFileName;
    
    protected File unitfeatureDir;
    protected String featsExt;
    protected FeatureDefinition featureDefinition;
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    protected UnitFileReader unitFileReader;
    
    public FeatureFileWriter( DatabaseLayout setdb, BasenameList setbnl )
    {
        this.db = setdb;
        this.bnl = setbnl;
    }
    
    /**
     * Set some global variables; sub-classes may want to override.
     *
     */
    protected void init()
    {
        unitfeatureDir = new File(db.phoneUnitFeaDirName());
        if (!unitfeatureDir.exists()) throw new IllegalStateException("Unit feature file "+unitfeatureDir.getAbsolutePath()+" does not exist");
        featsExt = db.phoneUnitFeaExt();
        featureFileName = db.phoneFeaturesFileName();
        unitFileName = db.phoneUnitFileName();
        weightsFileName = db.phoneWeightsFileName();
    }
    
    public boolean compute() throws IOException
    {
        init();
        System.out.println("Featurefile writer started.");

        maryDir = new File( db.maryDirName() );
        if (!maryDir.exists()) {
            maryDir.mkdir();
            System.out.println("Created the output directory [" + db.maryDirName() + "] to store the feature file." );
        }
        unitFileReader = new UnitFileReader( unitFileName );
        featureDefinition = new FeatureDefinition(new BufferedReader(new InputStreamReader(new FileInputStream(weightsFileName), "UTF-8")), true); // true: read weights
        assert featureDefinition != null : "Feature definition not set!";

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(featureFileName)));
        writeHeaderTo(out);
        writeUnitFeaturesTo(out);
        out.close();
        System.out.println("Number of processed units: " + unitFileReader.getNumberOfUnits() );

        FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(featureFileName);
        int unitsOnDisk = tester.getNumberOfUnits();
        if (unitsOnDisk == unitFileReader.getNumberOfUnits()) {
            System.out.println("Can read right number of units");
            return true;
        } else {
            System.out.println("Read wrong number of units: "+unitsOnDisk);
            return false;
        }
    }
    
    /**
     * @param out
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     */
    protected void writeUnitFeaturesTo(DataOutput out) throws IOException, UnsupportedEncodingException, FileNotFoundException {
        int numUnits = unitFileReader.getNumberOfUnits();
        out.writeInt( numUnits );
        System.out.println("Number of units : "+numUnits);
        int index = 0; // the unique index number of units in the unit file
        // Dummy feature vector corresponding to an edge unit:
        FeatureVector start = featureDefinition.createEdgeFeatureVector(0, true);
        FeatureVector end = featureDefinition.createEdgeFeatureVector(0, false);
        // Loop over all utterances
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            System.out.print( "    " + bnl.getName(i) + " : Entering at index (" + index + ") -- " );
            BufferedReader uttFeats = new BufferedReader(new InputStreamReader(new FileInputStream(new File( unitfeatureDir,  bnl.getName(i) + featsExt )), "UTF-8"));
            FeatureDefinition uttFeatDefinition = new FeatureDefinition(uttFeats, false); // false: do not read weights
            if (!uttFeatDefinition.featureEquals(featureDefinition)) {
                throw new IllegalArgumentException("Features in file "+bnl.getName(i)+" do not match definition file "+weightsFileName
                        + " because:\n"
                        + uttFeatDefinition.featureEqualsAnalyse(featureDefinition) );
            }
            // skip the clear text section: read until an empty line occurs
            String line;
            while ((line = uttFeats.readLine()) != null) {
                if (line.trim().equals("")) break;
            }
            // Check the index consistency
            if ( index > numUnits ) {
                throw new IOException("Inconsistency between feature files and unit file: " +
                        "the reached index [" + index+"] is bigger than the number of units in the unit file [" + numUnits + "] !");
            }
            // Empty entry corresponding to start of utterance:
            if (!unitFileReader.isEdgeUnit(index)) {
                // System.out.println( "Unit [" + index + "] : StarTime [" + unitFileReader.getStartTime(i) + "] Duration [" + unitFileReader.getDuration(i) + "]." );
                throw new IOException("Inconsistency between feature files and unit file: Unit "
                        +index+" should correspond to start of file "+bnl.getName(i)
                        +", but is not an edge unit!");
            }
            start.writeTo(out);
            index++;
            // Check the index consistency
            if ( index > numUnits ) {
                throw new IOException("Inconsistency between feature files and unit file: " +
                        "the reached index [" + index+"] is bigger than the number of units in the unit file [" + numUnits + "] !");
            }
            // read the binary section, and write it
            while ((line = uttFeats.readLine()) != null) {
                if (line.trim().equals("")) break;
                FeatureVector fv = featureDefinition.toFeatureVector(0, line);
                if (unitFileReader.isEdgeUnit(index)) {
                    throw new IOException("Inconsistency between feature files and unit file: Unit "
                            +index+"("+fv.getFeatureAsString(0,featureDefinition)+") should correspond to feature line '"+line+"' of file "+bnl.getName(i)
                            +", but is an edge unit!");
                }
                fv.writeTo(out);
                index++;
            }
            // Check the index consistency
            if ( index > numUnits ) {
                throw new IOException("Inconsistency between feature files and unit file: " +
                        "the reached index [" + index+"] is bigger than the number of units in the unit file [" + numUnits + "] !");
            }
            if (!unitFileReader.isEdgeUnit(index)) {
                throw new IOException("Inconsistency between feature files and unit file: Unit "
                        +index+" should correspond to end of file "+bnl.getName(i)
                        +", but is not an edge unit!");
            }
            end.writeTo(out);
            index++;
            System.out.println( "Exiting at index (" + index + ")." );
            System.out.flush();
        }
    }

    /**
     * Write the header of this feature file to the given DataOutput
     * @param out
     * @throws IOException
     */
    protected void writeHeaderTo(DataOutput out) throws IOException {
        new MaryHeader(MaryHeader.UNITFEATS).writeTo(out);
        featureDefinition.writeBinaryTo(out);
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

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        DatabaseLayout db = DatabaseImportMain.getDatabaseLayout();
        BasenameList bnl = DatabaseImportMain.getBasenameList(db);
        new FeatureFileWriter(db, bnl).compute();
    }


}
