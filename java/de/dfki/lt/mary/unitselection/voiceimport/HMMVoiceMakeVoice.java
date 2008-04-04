package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.SortedMap;
import java.util.TreeMap;

public class HMMVoiceMakeVoice extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceMakeVoice";
    
    /** Tree files and TreeSet object */
    public final String treeDurFile = name+".Ftd";
    public final String treeLf0File = name+".Ftf";
    
    public String getName(){
        return name;
    }
    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(treeDurFile, "hts/voices/qst001/ver1/tree-dur.inf"); 
           props.put(treeLf0File, "hts/voices/qst001/ver1/tree-lf0.inf");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        
        props2Help.put(treeDurFile, "durations tree file"); 
        props2Help.put(treeLf0File, "log F0 tree file");

    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        System.out.println("Installing hmm voice: ");
        /* make a new directory for the voice */
        System.out.println("Making voice directory ... ");
        String fileSeparator = System.getProperty("file.separator");
        String filedir = db.getProp(db.FILEDIR);
        String configdir = db.getProp(db.CONFIGDIR);
        String newVoiceDir = db.getProp(db.MARYBASE)
                            +"lib"+fileSeparator
                            +"voices"+fileSeparator
                            +db.getProp(db.VOICENAME).toLowerCase()
                            +fileSeparator;
        
        System.out.println(" newVoiceDir = " +  newVoiceDir);
        File newVoiceDirFile = new File(newVoiceDir);
        if (!newVoiceDirFile.exists()) newVoiceDirFile.mkdir();
        
        /* copy the files */
        System.out.println("Copying files ... ");
        try{
            File in, out;
            in = new File(getProp(treeDurFile));
            out = new File(newVoiceDir + getFileName(getProp(treeDurFile)));
            copy(in,out);   
            in = new File(getProp(treeLf0File));
            out = new File(newVoiceDir + getFileName(getProp(treeLf0File)));
            copy(in,out);   

               
        }catch (IOException ioe){
            return false;
        }
        

        return true;
        }
    
    private void copy(File source, File dest)throws IOException{
        try { 
            System.out.println("copying: " + source + "\n    --> " + dest);
            FileChannel in = new FileInputStream(source).getChannel();
            FileChannel out = new FileOutputStream(dest).getChannel();   
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());            
            out.write(buf);
            in.close();
            out.close();
        } catch (Exception e){
            System.out.println("Error copying file "
                    +source.getAbsolutePath()+" to "+dest.getAbsolutePath()
                    +" : "+e.getMessage());
            throw new IOException();
        }
    }
    
    
 
    
  
    
    /**
     * Given a file name with path it return the file name
     * @param fileNameWithPath
     * @return
     */
    private String getFileName(String fileNameWithPath) {
       String str;
       int i;
       
       i = fileNameWithPath.lastIndexOf("/");
       str = fileNameWithPath.substring(i+1); 
       
       return str;
        
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
}
