package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.text.*;

public class HMMVoiceMakeVoice extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceMakeVoice";
    
    /** Tree files and TreeSet object */
    public final String PERLCOMMAND = name+".perlCommand";
    public final String step1_$MKEMV = name+".runStep1_$MKEMV";
    public final String step2_$HCMPV = name+".runStep2_$HCMPV";
    public final String step3_$IN_RE = name+".runStep3_$IN_RE";
    public final String step4_$MMMMF = name+".runStep4_$MMMMF";
    public final String step5_$ERST0 = name+".runStep5_$ERST0";
    public final String step6_$MN2FL = name+".runStep6_$MN2FL";
    public final String step7_$ERST1 = name+".runStep7_$ERST1";
    public final String step8_$CXCL1 = name+".runStep8_$CXCL1";
    public final String step9_$ERST2 = name+".runStep9_$ERST2";
    public final String step10_$UNTIE = name+".runStep10_$UNTIE";
    public final String step11_$ERST3 = name+".runStep11_$ERST3";
    public final String step12_$CXCL2 = name+".runStep12_$CXCL2";
    public final String step13_$ERST4 = name+".runStep13_$ERST4";
    public final String step14_$CXCL3 = name+".runStep14_$CXCL3";
    public final String step15_$MKUN1 = name+".runStep15_$MKUN1";
    public final String step16_$PGEN1 = name+".runStep16_$PGEN1";
    public final String step17_$WGEN1 = name+".runStep17_$WGEN1";
    public final String step18_$CONVM = name+".runStep18_$CONVM";
    public final String step19_$ENGIN = name+".runStep19_$ENGIN";
    public final String step20_$UPMIX = name+".runStep20_$UPMIX";
    public final String step21_$ERST5 = name+".runStep21_$ERST5";
    public final String step22_$CXCL4 = name+".runStep22_$CXCL4";
    public final String step23_$MKUN2 = name+".runStep23_$MKUN2";
    public final String step24_$PGEN2 = name+".runStep24_$PGEN2";
    public final String step25_$WGEN2 = name+".runStep25_$WGEN2";

    
    public String getName(){
        return name;
    }
    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           props.put(PERLCOMMAND, "/usr/bin/perl");
           props.put(step1_$MKEMV, "1"); 
           props.put(step2_$HCMPV, "1");
           props.put(step3_$IN_RE, "1");
           props.put(step4_$MMMMF, "1");
           props.put(step5_$ERST0, "1");
           props.put(step6_$MN2FL, "1"); 
           props.put(step7_$ERST1, "1");
           props.put(step8_$CXCL1, "1");
           props.put(step9_$ERST2, "1"); 
           props.put(step10_$UNTIE, "1"); 
           props.put(step11_$ERST3, "1"); 
           props.put(step12_$CXCL2, "1"); 
           props.put(step13_$ERST4, "1"); 
           props.put(step14_$CXCL3, "1"); 
           props.put(step15_$MKUN1, "1"); 
           props.put(step16_$PGEN1, "1"); 
           props.put(step17_$WGEN1, "1"); 
           props.put(step18_$CONVM, "1"); 
           props.put(step19_$ENGIN, "1"); 
           props.put(step20_$UPMIX, "1"); 
           props.put(step21_$ERST5, "1"); 
           props.put(step22_$CXCL4, "1"); 
           props.put(step23_$MKUN2, "1"); 
           props.put(step24_$PGEN2, "1"); 
           props.put(step25_$WGEN2, "1");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(PERLCOMMAND, "perl command used for executing scripts/Training.pl scripts.");
        props2Help.put(step1_$MKEMV, "preparing environments"); 
        props2Help.put(step2_$HCMPV, "computing a global variance");
        props2Help.put(step3_$IN_RE, "initialization & reestimation");
        props2Help.put(step4_$MMMMF, "making a monophone mmf");
        props2Help.put(step5_$ERST0, "embedded reestimation (monophone)");
        props2Help.put(step6_$MN2FL, "copying monophone mmf to fullcontext one");
        props2Help.put(step7_$ERST1, "embedded reestimation (fullcontext)");
        props2Help.put(step8_$CXCL1, "tree-based context clustering (cmp)");
        props2Help.put(step9_$ERST2, "embedded reestimation (clustered)");
        props2Help.put(step10_$UNTIE, "untying the parameter sharing structure");
        props2Help.put(step11_$ERST3, "embedded reestimation (untied)");
        props2Help.put(step12_$CXCL2, "tree-based context clustering (cmp)");
        props2Help.put(step13_$ERST4, "embedded reestimation (re-clustered)");
        props2Help.put(step14_$CXCL3, "tree-based context clustering (dur)");
        props2Help.put(step15_$MKUN1, "making unseen models (1mix)");
        props2Help.put(step16_$PGEN1, "generating speech parameter sequences (1mix)");
        props2Help.put(step17_$WGEN1, "synthesizing waveforms (1mix)");
        props2Help.put(step18_$CONVM, "converting mmfs to the hts_engine file format");
        props2Help.put(step19_$ENGIN, "synthesizing waveforms using hts_engine");
        props2Help.put(step20_$UPMIX, "increasing the number of mixture components (1mix -> 2mix)");
        props2Help.put(step21_$ERST5, "embedded reestimation (2mix)");
        props2Help.put(step22_$CXCL4, "tree-based context clustering (dur, 2mix)");
        props2Help.put(step23_$MKUN2, "making unseen models (2mix)");
        props2Help.put(step24_$PGEN2, "generating speech parameter sequences (2mix)");
        props2Help.put(step25_$WGEN2, "synthesizing waveforms (2mix)");


    }
 
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
      
      String cmdLine;
      String filedir = db.getProp(db.ROOTDIR);
      
      /* First i need to change the Config.pm file according to the steps marked as 1 */
      /* Read the first part of Config, that part does not change */
      BufferedReader inputStream = null;
      BufferedWriter outputStream = null;
      String line;
      StringBuffer config = new StringBuffer();
      try{
        inputStream = new BufferedReader(new FileReader("scripts/Config.pm"));     
        while ((line = inputStream.readLine()) != null) {   
         if(line.contains("# Switch ")){
           config.append(line+"\n");
           break;
         }
         else             
           config.append(line+"\n");
        }
        inputStream.close();
        
               
        /* The Switch lines should be copied according to the settings */
        config.append("$MKEMV = " + getProp(step1_$MKEMV) + ";  # " + props2Help.get(step1_$MKEMV) + "\n");
        config.append("$HCMPV = " + getProp(step2_$HCMPV) + ";  # " + props2Help.get(step2_$HCMPV) + "\n");       
        config.append("$IN_RE = " + getProp(step3_$IN_RE) + ";  # " + props2Help.get(step3_$IN_RE) + "\n");
        config.append("$MMMMF = " + getProp(step4_$MMMMF) + ";  # " + props2Help.get(step4_$MMMMF) + "\n");
        config.append("$ERST0 = " + getProp(step5_$ERST0) + ";  # " + props2Help.get(step5_$ERST0) + "\n");
        config.append("$MN2FL = " + getProp(step6_$MN2FL) + ";  # " + props2Help.get(step6_$MN2FL) + "\n");
        config.append("$ERST1 = " + getProp(step7_$ERST1) + ";  # " + props2Help.get(step7_$ERST1) + "\n");
        config.append("$CXCL1 = " + getProp(step8_$CXCL1) + ";  # " + props2Help.get(step8_$CXCL1) + "\n");
        config.append("$ERST2 = " + getProp(step9_$ERST2) + ";  # " + props2Help.get(step9_$ERST2) + "\n");
        config.append("$UNTIE = " + getProp(step10_$UNTIE) + ";  # " + props2Help.get(step10_$UNTIE) + "\n");
        config.append("$ERST3 = " + getProp(step11_$ERST3) + ";  # " + props2Help.get(step11_$ERST3) + "\n");
        config.append("$CXCL2 = " + getProp(step12_$CXCL2) + ";  # " + props2Help.get(step12_$CXCL2) + "\n");
        config.append("$ERST4 = " + getProp(step13_$ERST4) + ";  # " + props2Help.get(step13_$ERST4) + "\n");
        config.append("$CXCL3 = " + getProp(step14_$CXCL3) + ";  # " + props2Help.get(step14_$CXCL3) + "\n");
        config.append("$MKUN1 = " + getProp(step15_$MKUN1) + ";  # " + props2Help.get(step15_$MKUN1) + "\n");
        config.append("$PGEN1 = " + getProp(step16_$PGEN1) + ";  # " + props2Help.get(step16_$PGEN1) + "\n");
        config.append("$WGEN1 = " + getProp(step17_$WGEN1) + ";  # " + props2Help.get(step17_$WGEN1) + "\n");
        config.append("$CONVM = " + getProp(step18_$CONVM) + ";  # " + props2Help.get(step18_$CONVM) + "\n");
        config.append("$ENGIN = " + getProp(step19_$ENGIN) + ";  # " + props2Help.get(step19_$ENGIN) + "\n");
        config.append("$UPMIX = " + getProp(step20_$UPMIX) + ";  # " + props2Help.get(step20_$UPMIX) + "\n");
        config.append("$ERST5 = " + getProp(step21_$ERST5) + ";  # " + props2Help.get(step21_$ERST5) + "\n");
        config.append("$CXCL4 = " + getProp(step22_$CXCL4) + ";  # " + props2Help.get(step22_$CXCL4) + "\n");       
        config.append("$MKUN2 = " + getProp(step23_$MKUN2) + ";  # " + props2Help.get(step23_$MKUN2) + "\n");
        config.append("$PGEN2 = " + getProp(step24_$PGEN2) + ";  # " + props2Help.get(step24_$PGEN2) + "\n");
        config.append("$WGEN2 = " + getProp(step25_$WGEN2) + ";  # " + props2Help.get(step25_$WGEN2) + "\n");
        
        config.append("\n" + "1;" + "\n");
        
        //System.out.println("CONFIG:" + config);
        
        outputStream = new BufferedWriter(new FileWriter("scripts/Config.pm"));
        outputStream.write(config.toString());
        outputStream.close();
        
      } catch (Exception e) {
          System.err.println("Exception: " + e.getMessage());
      }  
      
      /* Run: perl scripts/Training.pl scripts/Config.pm (It can take several hours...)*/     
      cmdLine = getProp(PERLCOMMAND) + " scripts/Training.pl scripts/Config.pm";      
      launchProc(cmdLine, "", filedir);
        
      return true;
    }
    
    
  
    /**
     * A general process launcher for the various tasks
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine, String task, String baseName ) {
        
        String filedir = db.getProp(db.ROOTDIR);
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        
        Date today;
        String output;
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyy.MM.dd-H:mm:ss", new Locale("en","US"));
        today = new Date();
        output = formatter.format(today);           
        String logFile = filedir+"log-"+output;
        
        System.out.println("\nRunning: "+ cmdLine);
        System.out.println("\nThe training procedure can take several hours...");
        System.out.println("Detailed information about the training status can be found in the logfile:\n  " + logFile);
        System.out.println("\nTraining voice: " + db.getProp(db.VOICENAME));
        System.out.println("The following is general information about execution of training steps:");
        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter log = new FileWriter(logFile);
            int numSteps = 1;
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( cmdLine );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                if(line.contains("Start ")){
                  System.out.println( "\nStep (" + numSteps + "): " + line );
                  numSteps++;
                }
                log.write(line+"\n");
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
            log.close();
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
        }
        
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
