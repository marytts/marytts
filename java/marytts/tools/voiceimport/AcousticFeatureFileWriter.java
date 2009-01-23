/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;


public class AcousticFeatureFileWriter extends VoiceImportComponent
{
    protected File maryDir;
    protected FeatureFileReader feats;
    protected FeatureDefinition inFeatureDefinition;
    protected File outFeatureFile;
    protected FeatureDefinition outFeatureDefinition;
    protected UnitFileReader unitFileReader;
    protected TimelineReader timeline;
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public final String UNITFILE = "AcousticFeatureFileWriter.unitFile";
    public final String WAVETIMELINE = "AcousticFeatureFileWriter.waveTimeLine";
    public final String FEATUREFILE = "AcousticFeatureFileWriter.featureFile";
    public final String ACFEATUREFILE = "AcousticFeatureFileWriter.acFeatureFile";  
    public final String ACFEATDEF = "AcousticFeatureFileWriter.acFeatDef";
    
    
    public String getName(){
        return "AcousticFeatureFileWriter";
    }
   
    
   public SortedMap getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap();
           String fileDir = db.getProp(db.FILEDIR);
           String maryExt = db.getProp(db.MARYEXT);
           props.put(UNITFILE,fileDir+"halfphoneUnits"+maryExt);
           props.put(WAVETIMELINE,fileDir+"timeline_waveforms"+maryExt);
           props.put(FEATUREFILE,fileDir+"halfphoneFeatures"+maryExt);
           props.put(ACFEATUREFILE,fileDir+"halfphoneFeatures_ac"+maryExt);
           props.put(ACFEATDEF,db.getProp(db.CONFIGDIR)+"halfphoneUnitFeatureDefinition_ac.txt");
       }
       return props;
   }
    
   protected void setupHelp(){
       if (props2Help ==null){
           props2Help = new TreeMap();
           props2Help.put(UNITFILE,"file containing all halfphone units");
           props2Help.put(WAVETIMELINE,"file containing all wave files");
           props2Help.put(FEATUREFILE,"file containing all halfphone units and their target cost features");
           props2Help.put(ACFEATUREFILE,"file containing all halfphone units and their target cost features"
								 +" plus the acoustic target cost features. Will be created by this module.");
           props2Help.put(ACFEATDEF,"file containing the list of phone target cost features, their values and weights");
       }
   }
  
   
    public boolean compute() throws IOException
    {
        System.out.println("Acoustic feature file writer started.");

        maryDir = new File( db.getProp(db.FILEDIR));
        if (!maryDir.exists()) {
            maryDir.mkdir();
            System.out.println("Created the output directory [" + ( db.getProp(db.FILEDIR)) + "] to store the feature file." );
        }
        //System.out.println("A");
        unitFileReader = new UnitFileReader(getProp(UNITFILE));
        //System.out.println("B");
        timeline = new TimelineReader(getProp(WAVETIMELINE));
        //System.out.println("C");
        
        feats = new FeatureFileReader(getProp(FEATUREFILE));
        inFeatureDefinition = feats.getFeatureDefinition();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        inFeatureDefinition.writeTo(pw, true);
        // And now, append the two float features for duration and f0:
        pw.println("0 linear | unit_duration");
        pw.println("0 linear | unit_logf0");
        pw.close();
        String fd = sw.toString();
        System.out.println("Generated the following feature definition:");
        System.out.println(fd);
        StringReader sr = new StringReader(fd);
        BufferedReader br = new BufferedReader(sr);
        outFeatureDefinition = new FeatureDefinition(br, true);

        outFeatureFile = new File(getProp(ACFEATUREFILE));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFeatureFile)));
        writeHeaderTo(out);
        writeUnitFeaturesTo(out);
        out.close();
        System.out.println("Number of processed units: " + unitFileReader.getNumberOfUnits() );

        //make sure we have a feature definition with acoustic features
        File featWeights = new File(getProp(ACFEATDEF));
        if (!featWeights.exists()){
            try{
                PrintWriter featWeightsOut =
                    new PrintWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(featWeights),"UTF-8"),true);
                
                
                outFeatureDefinition.generateFeatureWeightsFile(featWeightsOut);                
            } catch (Exception e){
                System.out.println("No phone feature weights ac file "
                        +getProp(ACFEATDEF));
                return false;
            }
        }
        FeatureFileReader tester = FeatureFileReader.getFeatureFileReader(getProp(ACFEATUREFILE));
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
        int unitSampleRate = unitFileReader.getSampleRate();

        out.writeInt( numUnits );
        System.out.println("Number of units : "+numUnits);
        for (int i=0; i<numUnits; i++) {
            percent = 100*i/numUnits;
            FeatureVector inFV = feats.getFeatureVector(i);
            Unit u = unitFileReader.getUnit(i);
            float dur = u.getDuration() / (float) unitSampleRate;
            Datagram[] unitAudio = timeline.getDatagrams(u, unitSampleRate);
            float logF0;
            if (unitAudio.length == 0 || dur < 0.005) {
                logF0 = 0;
            } else {
                double avgPeriodLength = dur / unitAudio.length;
                logF0 = (float) Math.log(1 / avgPeriodLength);
                assert !Float.isNaN(logF0);
                assert !Float.isInfinite(logF0);
            }
            String line = inFV.toString() + " " + dur + " " + logF0;
            //System.out.println("fv: "+line);
            FeatureVector outFV = outFeatureDefinition.toFeatureVector(0, line);
            outFV.writeTo(out);
        }
    }

    /**
     * Write the header of this feature file to the given DataOutput
     * @param out
     * @throws IOException
     */
    protected void writeHeaderTo(DataOutput out) throws IOException {
        new MaryHeader(MaryHeader.UNITFEATS).writeTo(out);
        outFeatureDefinition.writeBinaryTo(out);
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
    public static void main(String[] args) throws Exception
    {
        AcousticFeatureFileWriter acfeatsWriter = 
            new AcousticFeatureFileWriter();
        DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
        acfeatsWriter.compute();
    }


}

