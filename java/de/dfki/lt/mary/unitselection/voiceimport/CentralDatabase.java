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

import java.io.*;
import java.net.URL;
import java.util.*;

import de.dfki.lt.mary.unitselection.cart.CART;

import de.dfki.lt.mary.unitselection.cart.CARTImpl;
import de.dfki.lt.mary.util.MaryUtils;

/**
 * The Central Database is for reading voice data from
 * the Festvox-directory and from FreeTTS files
 * and dumping it in MARY-Format
 * @author Anna
 * @date 31.07.2006
 */
public class CentralDatabase 
{ 
    public final static int CLUNIT_NONE = 65535;
    
    //header values
    private final static int MAGIC = 0x4d41525;
    private final static int VERSION = 1;
    private final static int CARTS = 1;
    private final static int UNITS = 2;
    private final static int TARGETFEATS = 3;
    private final static int JOINFEATS = 4;
    private final static int TIMELINE = 5;
    
    //how the audio is encoded
    private final static int LPC = 1;
   
    private String destPath; 
    private String festvoxDirectory;
    private String[] wavFilenames;
    private Map filenames2Index;
    private UnitCatalog unitCatalog;
    private int audioEncoding;
    private Track[] tracks;
    private int samplingRate;
    private int numFrames;
    
 
    
    public CentralDatabase(String destPath, 
            			String festvoxDirectory, 
            			Map wavFilenames, 
            			UnitCatalog unitCatalog,
            			int audioEncoding)
    {
        super();
        this.destPath = destPath;
        this.festvoxDirectory = festvoxDirectory;
        
        this.unitCatalog = unitCatalog;        
        this.audioEncoding = audioEncoding;
        
        Set wavFiles = new TreeSet(wavFilenames.keySet());
        Iterator keys = wavFiles.iterator();
        this.wavFilenames = new String[wavFiles.size()];
        filenames2Index = new HashMap();
        int i=0;
        while (keys.hasNext()) {
            //build up each STS track
            String nextFile = (String) keys.next();
            this.wavFilenames[i] = nextFile;
            filenames2Index.put(nextFile,new Integer(i));
            i++;
        }
    }
    
    /**
     * Method which opens a source file for reading
     * @param filename the filename
     * @return a BufferedReader of this file
     */
    public BufferedReader openSource(String filename){
        System.out.println("Opening source file "+filename);
        try{
        URL fileURL = new URL("file:" + filename);
        FileInputStream fis = new FileInputStream(fileURL.getFile());
    
        BufferedReader reader =
            new BufferedReader(new 
                InputStreamReader(fis));
        return reader;
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Could not open file");
        }
    }
    
    /**
     * Method which opens a destination file for writing
     * and writes the header
     * @param filename the filename
     * @param content what the file contains (audio, units, etc.)
     * @return DataOutputStream to write on this file
     */
    public DataOutputStream openDestination(String filename, int content){
        try {
        System.out.println("Opening destination file "+filename);
        FileOutputStream fos = new FileOutputStream(filename);
        DataOutputStream os = new DataOutputStream(new
                BufferedOutputStream(fos));
        
        os.writeInt(MAGIC);
        os.writeInt(VERSION);
        os.writeInt(content);
        return os;
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Could not open file");
        }
            
            
    }
    
    /**
     * Gets the units of a type
     * @param type the type
     * @return the units
     */            
    public List getUnitsForType(String type){
        return (List) unitCatalog.get(type);
    }
    
    public Unit getUnit(int index){
        return unitCatalog.getUnit(index);
    }
    
     
    
   
    
    
    public void readAndDumpTargetFeatures(String featDefFile){
     try {
         System.out.println("Loading target features");
    // Load the features
    FeatureReader featureReader = 
            new FeatureReader(this, featDefFile, festvoxDirectory+"/festival/feats");
    featureReader.readFeatures();
       
    //System.out.println("Rewriting feature definition file");
    //Rewrite the feature definition file with the sorted features
    //featureReader.rewriteFeatDefs();
    
    System.out.println("Dumping target features"); 
    //open output stream
    DataOutputStream out = openDestination(destPath +"targetFeatures.bin",TARGETFEATS);
    
    //dump the features and their possible values
    featureReader.dumpFeaturesAndValues(out);
    
    //dump the values for each unit
    List units = unitCatalog.getUnits();
    int length = units.size();
    out.writeInt(length);
    for(int i=0;i<length;i++){
        Unit nextUnit = (Unit) units.get(i);
        boolean success = nextUnit.dumpTargetValues(out);
        if (!success){
            //we have a null-unit or unit has no features
            nextUnit.dumpPseudoTargetValues(out, featureReader.numByteFeats,
                    featureReader.numShortFeats, featureReader.numFloatFeats);
        }
       
    }
    //Finish
    out.close();
    System.out.println("Done\n");
     }catch (Exception e){
         e.printStackTrace();
         throw new Error("Error dumping target features");
     }
    
   } 
    
public void readAndDumpJoinFeatures(String defFile){
    try {
        //open join feats file
        System.out.println("Reading join features");
        BufferedReader reader = openSource(defFile);
        String line = reader.readLine();
        List lines = new ArrayList();
        
        while (line != null){
            if (!(line.startsWith("***"))){
               lines.add(line);
            }
            line = reader.readLine();
        }
        float[] weights = new float[lines.size()];
        String[] functions = new String[lines.size()];
        for (int i = 0; i<lines.size();i++){
            line = (String)lines.get(i);
            StringTokenizer tok = new StringTokenizer(line);
            weights[i] = Float.parseFloat(tok.nextToken());
            if (tok.hasMoreTokens()){
                functions[i] = tok.nextToken();
            } 
        }
        System.out.println("Dumping join features");
        //open the target file
        DataOutputStream out = openDestination(destPath+"joinFeatures.bin",JOINFEATS);
        
        //dump the feature specifications
        out.writeInt(weights.length);
        for (int i=0;i<weights.length;i++){
            out.writeFloat(weights[i]);
            if (functions[i] == null){
                out.writeUTF("0");
            } else {
                out.writeUTF(functions[i]);
            }
        }
        
        //read in the values for the units
        System.out.println("Reading join values");
        
        
        // Read in the MCEPs
        float mcepMin = 0;
        float mcepRange = 0;
        reader = openSource(festvoxDirectory+"/mcep/mcep.params");
        line = reader.readLine();
        while (line != null) {
            if (line.startsWith("MCEP_MIN=")) {
                mcepMin = Float.parseFloat(line.substring(9));
            } else if (line.startsWith("MCEP_RANGE=")) {
                mcepRange = Float.parseFloat(line.substring(11));
            }
            line = reader.readLine();
        }
        reader.close(); 
        
        Frame[] mcepFrames = new Frame[numFrames];
        int numMCEPFrames = 0;
        
        try{
            int i=0;
            while (i<wavFilenames.length) {
                //build up each MCEP track
                String filename = festvoxDirectory+"/mcep/"+wavFilenames[i]+".mcep.txt";
                MCEPTrack track = new MCEPTrack(filename, mcepMin, 
                    						mcepRange, mcepFrames, 
                    						numMCEPFrames);
                numMCEPFrames += track.numFrames; 
               
                i++;
            }
        } catch (ArrayIndexOutOfBoundsException aie){
            aie.printStackTrace();
            throw new Error("Error reading MCEPs");
        }
        
        //go through the units, get the first and last MFCC for each unit
        //and dump it
        System.out.println("Dumping join values");
        
        List units = unitCatalog.getUnits();
        out.writeInt(units.size());
        for (Iterator it = units.iterator();it.hasNext();){
            Unit nextUnit = (Unit) it.next();
            if (nextUnit instanceof NullUnit){
                int bytesNow = out.size();
                ((NullUnit) nextUnit).dumpJoinValues(out,weights.length);
                int bytesThen = out.size();
                if (bytesNow+104 != bytesThen){
                    throw new Error("Did only write "+bytesThen+" bytes");
                }
            } else {
                int bytesNow = out.size();
                int start = nextUnit.getStartFrame();
                int end = nextUnit.getEndFrame();
                mcepFrames[start].dumpBinary(out);
                nextUnit.dumpLeftF0(out);
                mcepFrames[end].dumpBinary(out);
                nextUnit.dumpRightF0(out);
                int bytesThen = out.size();
                if (bytesNow+104 != bytesThen){
                    throw new Error("Did only write "+bytesThen+" bytes");
                }
            }
        }
        
        //finish
        out.close();
        System.out.println("Done\n");
        
    }catch (Exception e){
        e.printStackTrace();
        throw new Error("Error dumping join features");
    }

    
}
    
public void readAndDumpCARTS(){
    try{
        
        //open CART-File  and read in CARTS
        System.out.println("Reading CARTS");
        BufferedReader reader = openSource(festvoxDirectory + "/FreeTTS/trees.txt");
        Map cartMap = new HashMap();
        String line = reader.readLine();
        while (line != null){
            StringTokenizer tokenizer = new StringTokenizer(line);
            tokenizer.nextToken();
            String name = tokenizer.nextToken();
            int nodes = Integer.parseInt(tokenizer.nextToken());
            CART cart = new CARTImpl(reader, nodes);
            cartMap.put(name, cart);
            line = reader.readLine();
        }
        reader.close();
        
        //correct instance numbers to index numbers
        System.out.println("Correcting units indices in CARTS");
        Set carts = cartMap.keySet();
        for (Iterator it = carts.iterator(); it.hasNext();){
            String nextCart = (String) it.next();
            List units = (List)unitCatalog.get(nextCart);
            ((CART)cartMap.get(nextCart)).correctNumbers(units);
        }
        
        //dump CARTS to binary file
        System.out.println("Dumping CARTS");
        DataOutputStream out = openDestination(destPath + "CARTS.bin",CARTS);
        out.writeInt(cartMap.size());
        for (Iterator i = carts.iterator(); i.hasNext();) {
            String name = (String) i.next();
            CART cart =  (CART) cartMap.get(name);
            out.writeUTF(name);
            cart.dumpBinary(out);
        }
        //finish
        out.close();
        System.out.println("Done\n");
        
    } catch (Exception e){
        e.printStackTrace();
        throw new Error("Error reading CARTS");
    }
    
    }     

    /**
     * Dumps the LPC data.
     */
    public void readAndDumpLPCData()
        throws IOException {
        
        float lpcMin = 0.0f;
        float lpcRange = 0.0f;
        long time = 0;
        int numLPCChannels = 0;
        
        System.out.println("Reading LPC data");
        
        /**
         * Gets the LPC metadata
         */
        BufferedReader reader =
            openSource(festvoxDirectory+"/lpc/lpc.params");
        String line = reader.readLine();
        while (line != null) {
            if (line.startsWith("LPC_MIN=")) {
                lpcMin = Float.parseFloat(line.substring(8));
            } else if (line.startsWith("LPC_RANGE=")) {
                lpcRange = Float.parseFloat(line.substring(10));
            }
            line = reader.readLine();
        }
        reader.close();  
        
        
       
        
        long[] trackTimes = new long[wavFilenames.length]; //end times of each track
        tracks = new STSTrack[wavFilenames.length];
        numFrames = 0;
        FindSTS stsFinder = new FindSTS(festvoxDirectory);
        int i=0;
        while (i<wavFilenames.length) {
            //build up each STS track
            String filename = wavFilenames[i];
            
            STSTrack track = stsFinder.getSTSTrack(filename, time, numFrames);
            tracks[i] = track;
            samplingRate = track.sampleRate; //should be the same for all tracks
            numFrames += track.numFrames; //add number of frames in track to total frame count
            time += track.overAllTime;
            trackTimes[i] = time;
            
            if (i==0){
                numLPCChannels = track.numChannels;
                
            }
            i++;
        }
        
        System.out.println("Over-all time of all files (in frames): "+time);
        System.out.println("Dumping LPC Data");
        //open output stream
        DataOutputStream out = openDestination(destPath + "audio.bin",TIMELINE);
        
        
        //write audio info 
        String audioInfo = "AudioType LPC Channels "+numLPCChannels
        			+" LPCMin "+lpcMin
        			+" LPCRange "+lpcRange;
        out.writeUTF(audioInfo);
        
                
        //write data header
        out.writeByte(1); //Variable time spacing
        out.writeLong(numFrames); //Number of frames/datagrams
                
        //write index header
        long numIndices = time/samplingRate;
        int indexIntervall = samplingRate;
        out.writeLong(numIndices);
        out.writeInt(indexIntervall);
        
        long lastBytePos = out.size(); //number of bytes written so far
        lastBytePos += 16*numIndices; //increment up to position of first datagram
        int closestTrack = 0;
        System.out.println("Finding closest frames for "+numIndices+" indices");
        //write the indices
        for (long j=0; j<numIndices; j++){
            //the next index position
            long position = j*indexIntervall;
            //find the closest track
            for (int k=closestTrack;k<trackTimes.length;k++){
                long nextTime = trackTimes[k];
                //System.out.println("Comparison : "+nextTime+" > "+position);
                //System.out.println("Comparison : "+((STSTrack)tracks[closestTrack]).getLastTime()+" >= "+position);
                if (nextTime>=position 
                        && ((STSTrack)tracks[closestTrack]).getLastTime() >= position){ 
                    //break, if the start time of next track is too high and
                    //if last frame start time of current track
                    //is not too low (then index time would be in middle of last frame of
                    //closest track, so we would still have to take the next track)
                    break;
                } else {
                    //accumulate bytes of last closestTrack
                    //(we only accumulate bytes for whole tracks,
                    // never for individual frames)
                    
                        lastBytePos+= 
                            	((STSTrack)tracks[closestTrack]).getByteSize(-1);
                   
                    closestTrack++; //this track is new closest Track                  
                    
                }
            }
            
            //find the closest frame in this track
            STSTrack currentTrack = (STSTrack) tracks[closestTrack];
            int relativeFrameIndex = currentTrack.findClosestFrame(position);
            
            //System.out.println("For index "+j+" at position "+position
            //        +" found closest track at index "+relativeFrameIndex
            //        +" at position "+currentTrack.getTime(relativeFrameIndex));
            //write index info
            long bytes = currentTrack.getByteSize(relativeFrameIndex)+lastBytePos;
            long timeDiff = currentTrack.getTime(relativeFrameIndex)-position;
            out.writeLong(bytes);
            out.writeLong(timeDiff);
            
        }
        
        //dump the audio data
        for (int l=0;l<tracks.length;l++){
            tracks[l].dumpBinary(out);            
        }
        out.close();
        System.out.println("Done\n");
    }

    
    
    
          
         
              

    /**
     * Dumps the unit index.
     */
    public void dumpUnitsAndAudio() throws IOException {
        if (audioEncoding == LPC){
            readAndDumpLPCData();
        } else {
            throw new Error("Unsupported Audio type : "+audioEncoding);
        }

        System.out.println("Dumping units");
        
        //open destination file
        DataOutputStream out = openDestination(destPath+"units.bin", UNITS);
        
        List units = unitCatalog.getUnits();
        int numberOfUnits = units.size();
        out.writeInt(numberOfUnits);
        out.writeInt(samplingRate);
        
        Track track = null;
       
        long samplePosition = 0;
        long trackStartTime = 0;
        for (int i=1; i<numberOfUnits-1; i++) {
            Unit unit = (Unit) units.get(i);
            //System.out.println("Filename "+unit.filename+", unit "+unit.unitType);
            if (track == null){
                //update to first track
                
                track = 
                 tracks[((Integer)filenames2Index.get(unit.filename)).intValue()];
                //insert begin unit
                out.writeLong(samplePosition);
                out.writeInt(0);
            } else {
                
                if (unit.filename == null){
                //we are at null-unit = last unit for track
                //insert begin and end unit
                out.writeLong(samplePosition);
                out.writeInt(0);
                out.writeLong(samplePosition);
                out.writeInt(0);
                //skip the two null units
                i+=2;
                //update to next beginning unit
                unit = (Unit) units.get(i);
                //update track
                track = 
                    tracks[((Integer)filenames2Index.get(unit.filename)).intValue()];
                //update startTime
                trackStartTime = track.getTime(0);
                }
            }
            //System.out.println("Filename "+unit.filename+", unit "+unit.unitType);
            try {
            
            //get the start and end of the unit in the big audio file
            long start = Math.round(unit.start*samplingRate) + trackStartTime;
            long end = Math.round(unit.end*samplingRate) + trackStartTime;
            int closestStartFrame = track.findClosestFrame(start);
            int closestEndFrame = track.findClosestFrame(end);
            
            //if we are at last unit, end of unit might be in the middle of
            //a frame and closestEndFrame returns -1
            //therefore correct closestEndFrame
            if (closestEndFrame == -1){
                closestEndFrame = track.getLastFrame();
                //System.out.println("Correcting end frame to "+closestEndFrame);
            }
            
            //System.out.println("For unit starting at "+start+" and ending at "
              //      		+end+", found startFrame at "+track.getTime(closestStartFrame)
                //    		+" and endFrame at "+track.getTime(closestEndFrame));
            //store start and end frame in unit for the join costs
            unit.setStartEnd(track.getOverAllIndex(closestStartFrame), 
                    	track.getOverAllIndex(closestEndFrame));
            //Compute left and right f0 
            int nPeriods = 5;
            int[] uPeriods = new int[Math.min(nPeriods, (closestEndFrame-closestStartFrame))];
            
            //compute left f0
            for (int j=0; j<uPeriods.length; j++) {
                uPeriods[j] = track.getFrameSize(j+closestStartFrame);
            }
            float leftF0 = MaryUtils.median(uPeriods);
            //compute right f0
            int u0N = closestEndFrame-closestStartFrame-1;
            for (int j=0; j<uPeriods.length; j++) {
                uPeriods[j] = track.getFrameSize(u0N-j+closestStartFrame);
            }
            float rightF0 = MaryUtils.median(uPeriods);
            //store f0 values in units for the join costs
            unit.setF0(leftF0,rightF0);  
            
            //dump the unit
            long startTime = track.getTime(closestStartFrame);
            int duration = track.getNumSamples(closestStartFrame, closestEndFrame);
            
            out.writeLong(startTime);
            out.writeInt(duration);
            System.out.println("Unit "+i+" starts at "+startTime
                    +" and has duration "+duration);
            samplePosition=end;
            } catch (Exception e){
                e.printStackTrace();
                throw new Error("Error at unit "+unit.unitType+" in file "+unit.filename);
            }
        }
        //add final null-unit
        out.writeLong(samplePosition);
        out.writeInt(0);
        
        out.close();
        System.out.println("Done\n");
    }

    
    
    /**
     *  Manipulates a ClusterUnitDatabase.  
     *  Can be used to build a .bin file from a .txt file.
     *  Called by target clunit_voice_bin in build.xml
     *
     * <p>
     * <b> Usage </b>
     * <p>
     *  <code> java com.sun.speech.freetts.clunits.ClusterUnitDatabase
     *  [options]</code> 
     * <p>
     * <b> Options </b>
     * <p>
     *    <ul>
     *          <li> <code> -src path </code> provides a directory
     *          path to the source text for the database
     *          <li> <code> -dest path </code> provides a directory
     *          for where to place the resulting binaries
     *      <li> <code> -generate_binary [filename]</code> reads
     *      in the text version of the database and generates
     *      the binary version of the database.
     *      <li> <code> -compare </code>  Loads the text and
     *      binary versions of the database and compares them to
     *      see if they are equivalent.
     *      <li> <code> -showTimes </code> shows timings for any
     *      loading, comparing or dumping operation
     *    </ul>
     * 
     */
    public static void main(String[] args) 
    {
        System.out.println("Importing Unitselection Voice - Version 1.1mary\n\n");
        System.out.println("Checking arguments and creating target directory");
        
       
        
        String voiceName = "";
        String marybase = ".";
     
        String festvoxDirName;
        String targetDef = "";
        String joinDef = "";
        int audioEncoding = LPC;
        try {
           //read in the args
           voiceName = args[0];
           marybase = args[1];
           festvoxDirName = args[2];
           targetDef = args[3];
           joinDef = args[4];
           //optional arg determines audio encoding (standard LPC)
           if (args.length>5){
               audioEncoding = Integer.parseInt(args[5]);
           }
           
        } catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Usage:\n ant import_unitsel_voice -Dvoice=<voicename>"
                    +" -Dfestvox_dir=<directory of festvox data> -Dtarget_feats=<filename of target feature file>"
                    +" -Djoin_feats=<filename of join feature file>"
                    +" [-Daudio_encod=<audioEncoding (at present, only LPC supported)>]");
            return;
        }
        
        try{
            
            
            //get the filenames
            String binaryName = voiceName + ".bin";
            String targetFeatDefFile = marybase + "/lib/voices/" + voiceName + "/" + targetDef;
            String joinFeatDefFile = marybase + "/lib/voices/" + voiceName + "/" + joinDef;
            String destPath = marybase + "/lib/voices/" + voiceName + "/";
            
            
            //create destination directory
            File destDir = new File(destPath);
            if (!destDir.exists()){
            boolean success = destDir.mkdir();
            if (!success) {
                // Directory creation failed
                System.out.println("Could not create destination directory " 
                        + destPath);
                return;
            }
            }
            
            //get the names of the wav files and store them in the database
            Map filenames = new HashMap();
            File wavDir = new File(festvoxDirName + "/wav");
           
            File[] wavFiles = wavDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                       return name.endsWith(".wav");
            	}
            });
            for (int i = 0; i < wavFiles.length; i++) {
               String basename = wavFiles[i].getName().substring(0, wavFiles[i].getName().length()-4);
               
               filenames.put(basename, null);
            }
            
            
            
            //Get the catalog file
            File catalogDir = new File(festvoxDirName + "/festival/clunits");
            File catalogFile = catalogDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                   return name.endsWith(".catalogue");
            }
            })[0];
            
            //Read in the catalogue 
            System.out.println("Reading Catalog : " + catalogFile.getPath());
            UnitCatalog unitCatalog = new UnitCatalog(catalogFile.getPath());
            
            //build the database
            CentralDatabase database = 
                new CentralDatabase(destPath, festvoxDirName, filenames, unitCatalog, audioEncoding);
          
          /**
            System.out.println(
                    "Creating "+ destPath + "CARTS.bin");               
            database.readAndDumpCARTS();              
               
            System.out.println(
                     "Creating "+ destPath + "targetFeatures.bin");               
            
            database.readAndDumpTargetFeatures(targetFeatDefFile);
            **/
           
            System.out.println(
                    "Creating "+ destPath + "units.bin and"
                    +destPath + "audio.bin");
            database.dumpUnitsAndAudio();
            
            System.out.println(
                    "Creating "+ destPath + "joinFeatures.bin");               
            database.readAndDumpJoinFeatures(joinFeatDefFile); 
             
             System.out.println("Done importing voice "+voiceName);
                    
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }

    
}
