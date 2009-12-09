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

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;
import javax.swing.JFrame;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.nonverbal.BackchannelFeatureFileReader;
import marytts.nonverbal.BackchannelUnit;
import marytts.nonverbal.BackchannelUnitFileReader;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.display.FunctionGraph;
import marytts.unitselection.concat.DatagramDoubleDataSource;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.MaryHeader;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.ArrayUtils;
import marytts.util.math.MathUtils;
import marytts.util.math.Polynomial;
import marytts.util.signal.SignalProcUtils;

/**
 * Backchannel feature file writer
 * @author sathish
 *
 */
public class BackchannelFeatureFileWriter extends VoiceImportComponent
{
    protected File maryDir;
    protected File outFeatureFile;
    protected FeatureDefinition featureDefinition;
    protected BackchannelUnitFileReader listenerUnits;
    //protected TimelineReader audio;
    protected DatabaseLayout db = null;
    protected int percent = 0;
    protected BasenameList bachChannelList;
    protected Map<String, String> nameMap;
    protected Map<String, String> meaningMap;
    protected Map<String, String> voiceQualityMap;
    protected Map<String, String> intonationMap;
    
    private final String name = "BackchannelFeatureFileWriter";
    public final String UNITFILE = name + ".unitFile";
    public final String WAVETIMELINE = name + ".waveTimeLine";
    public final String FEATUREFILE = name + ".featureFile";
    public final String MANUALFEATURES = name + ".manualFeatureFile";  
    public final String POLYNOMORDER = name + ".polynomOrder";
    public final String SHOWGRAPH = name + ".showGraph";
    public final String INTERPOLATE = name + ".interpolate";
    public final String MINPITCH = name + ".minPitch";
    public final String MAXPITCH = name + ".maxPitch";
    public String BASELIST =  name + ".backchannelBNL";
 
    public String getName(){
        return name;
    }
   
    
   public SortedMap<String, String> getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap<String, String>();
           String fileDir = db.getProp(db.FILEDIR);
           String maryExt = db.getProp(db.MARYEXT);
           props.put(UNITFILE,fileDir+"backchannelUnits"+maryExt);
           props.put(WAVETIMELINE,fileDir+"timeline_backchannels"+maryExt);
           props.put(FEATUREFILE,fileDir+"backChannelFeatures"+maryExt);
           props.put(MANUALFEATURES,db.getProp(db.ROOTDIR)+File.separator+"manualFeatures.txt");
           props.put(BASELIST, db.getProp(db.ROOTDIR)+File.separator+"backchannel.lst");
       }
       return props;
   }
    
   protected void setupHelp()
   {
       if (props2Help ==null) {
           props2Help = new TreeMap<String, String>();
           props2Help.put(UNITFILE,"file containing all halfphone units");
           props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them"); 
           props2Help.put(FEATUREFILE,"file containing all halfphone units and their target cost features");
       }
   }
   
   public void initialiseComp()
   {
       maryDir = new File(db.getProp(db.FILEDIR));
       try {
           System.out.println("NAME: "+getProp(BASELIST));
           bachChannelList = new BasenameList( getProp(BASELIST));
       } catch (IOException e) {
           e.printStackTrace();
       }
       
       nameMap = new HashMap<String, String>();
       meaningMap = new HashMap<String, String>();
       voiceQualityMap = new HashMap<String, String>();
       intonationMap = new HashMap<String, String>();
   }
  
   
    public boolean compute() throws IOException
    {
        String featureDefinitionFile = db.getProp(db.FILEDIR) + File.separator + "vocalizationFeatureDefinition.txt";
        System.out.println("Feat : "+featureDefinitionFile);
        BufferedReader fDBfr = new BufferedReader(new FileReader(new File(featureDefinitionFile)));
        featureDefinition = new FeatureDefinition(fDBfr, true);
        
        maryDir = new File( db.getProp(db.FILEDIR));
        if (!maryDir.exists()) {
            maryDir.mkdir();
            System.out.println("Created the output directory [" + ( db.getProp(db.FILEDIR)) + "] to store the feature file." );
        }
        
        listenerUnits = new BackchannelUnitFileReader(getProp(UNITFILE));
        //audio = new TimelineReader(getProp(WAVETIMELINE));
        loadManulaFeatures(getProp(MANUALFEATURES));
        System.out.println(getProp(FEATUREFILE));
        
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(getProp(FEATUREFILE)))));
        writeHeaderTo(out);
        writeUnitFeaturesTo(out);
        out.close();
        logger.debug("Number of processed units: " + listenerUnits.getNumberOfUnits() );

        BackchannelFeatureFileReader tester = BackchannelFeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
        int unitsOnDisk = tester.getNumberOfUnits();
        if (unitsOnDisk == listenerUnits.getNumberOfUnits()) {
            System.out.println("Can read right number of units");
            return true;
        } else {
            System.out.println("Read wrong number of units: "+unitsOnDisk);
            return false;
        }
    }

    private void loadManulaFeatures(String manualFeatFile) throws IOException {
        BufferedReader bfr = new BufferedReader(new FileReader(new File(manualFeatFile)));
        String line;
        int numberOfBc = (new Integer(bfr.readLine().trim())).intValue();
        for ( int i=0; i<numberOfBc; i++ ) {
            line = bfr.readLine().trim();
            if( !"".equals(line) ) {
                throw new RuntimeException("File Format ERROR: Backchannel feature file is not in expected format");
            }
            String baseName = bfr.readLine().trim();
            line = bfr.readLine().trim();
            line = line.replaceAll("\\s+", "_");
            nameMap.put(baseName, line);
            line = bfr.readLine().trim();
            line = line.replaceAll("\\s+", "_");
            meaningMap.put(baseName, line);
            line = bfr.readLine().trim();
            line = line.replaceAll("\\s+", "_");
            voiceQualityMap.put(baseName, line);
            line = bfr.readLine().trim();
            line = line.replaceAll("\\s+", "_");
            intonationMap.put(baseName, line);
        }
        
        
        
    }


    protected void writeUnitFeaturesTo(DataOutput out) throws IOException, UnsupportedEncodingException, FileNotFoundException {
        int numUnits = listenerUnits.getNumberOfUnits();
        
        out.writeInt( numUnits );
        logger.debug("Number of units : "+numUnits);

        if(bachChannelList.getLength() != listenerUnits.getNumberOfUnits()) {
            throw new RuntimeException("Number of units in Bachchannel units files are not equal to number of basenames. ");
        }
        /**
         * TODO sanity check for each basename
         */
        
        int nameFeatIndex = featureDefinition.getFeatureIndex("name");
        int vQualityFeatIndex = featureDefinition.getFeatureIndex("voicequality");
        int intonationFeatIndex = featureDefinition.getFeatureIndex("intonation");
        int meaningFeatIndex = featureDefinition.getFeatureIndex("meaning");
        
        System.out.println(nameFeatIndex + " " + vQualityFeatIndex + " " + intonationFeatIndex + " " + meaningFeatIndex);
        
        //featureDefinition.get
        for( int i=0; i < bachChannelList.getLength(); i++ ) {
            //BackchannelUnit oneListenerUnit = listenerUnits.getUnit(i);
            String baseName = bachChannelList.getName(i);
            byte[] bcFeatures = new byte[4];
            bcFeatures[0] = featureDefinition.getFeatureValueAsByte(nameFeatIndex, nameMap.get(baseName));
            bcFeatures[1] = featureDefinition.getFeatureValueAsByte(intonationFeatIndex, intonationMap.get(baseName));
            bcFeatures[2] = featureDefinition.getFeatureValueAsByte(vQualityFeatIndex, voiceQualityMap.get(baseName));
            bcFeatures[3] = featureDefinition.getFeatureValueAsByte(meaningFeatIndex, meaningMap.get(baseName));
            
            FeatureVector outFV = featureDefinition.toFeatureVector(i, bcFeatures, null, null);
            outFV.writeTo(out);
        }
        

    }
    
    /**
     * Write the header of this feature file to the given DataOutput
     * @param out
     * @throws IOException
     */
    protected void writeHeaderTo(DataOutput out) throws IOException {
        new MaryHeader(MaryHeader.LISTENERFEATS).writeTo(out);
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
    public static void main(String[] args) throws Exception
    {
        BackchannelFeatureFileWriter acfeatsWriter = 
            new BackchannelFeatureFileWriter();
        DatabaseLayout db = new DatabaseLayout(acfeatsWriter);
        acfeatsWriter.compute();
    }


}

