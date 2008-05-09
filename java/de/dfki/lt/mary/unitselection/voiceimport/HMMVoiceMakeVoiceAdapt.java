/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/

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

public class HMMVoiceMakeVoiceAdapt extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceMakeVoiceAdapt";
    
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
    
    public final String step18_$REGTR = name+".runStep18_$REGTR";
    public final String step19_$ADPT1 = name+".runStep19_$ADPT1";
    public final String step20_$PGEN2 = name+".runStep20_$PGEN2";
    public final String step21_$WGEN2 = name+".runStep21_$WGEN2";
    public final String step22_$SATX1 = name+".runStep22_$SATX1";
    public final String step23_$ERST5 = name+".runStep23_$ERST5";
    public final String step24_$SATX2 = name+".runStep24_$SATX2";
    public final String step25_$ERST6 = name+".runStep25_$ERST6";
    public final String step26_$SATX3 = name+".runStep26_$SATX3";
    public final String step27_$ERST7 = name+".runStep27_$ERST7";
    public final String step28_$CXCL4 = name+".runStep28_$CXCL4";
    public final String step29_$MKUN2 = name+".runStep29_$MKUN2";
    public final String step30_$PGEN3 = name+".runStep30_$PGEN3";
    public final String step31_$WGEN3 = name+".runStep31_$WGEN3";
    public final String step32_$ADPT2 = name+".runStep32_$ADPT2";
    public final String step33_$PGEN4 = name+".runStep33_$PGEN4";
    public final String step34_$WGEN4 = name+".runStep34_$WGEN4";
    public final String step35_$CONVM = name+".runStep35_$CONVM";
    public final String step36_$ENGIN = name+".runStep36_$ENGIN";
   

    private ArrayList<String> stepsAdapt = new ArrayList<String>();
    
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
           
           props.put(step18_$REGTR, "1");
           props.put(step19_$ADPT1, "1");
           props.put(step20_$PGEN2, "1");
           props.put(step21_$WGEN2, "1");
           props.put(step22_$SATX1, "1");
           props.put(step23_$ERST5, "1");
           props.put(step24_$SATX2, "1");
           props.put(step25_$ERST6, "1");
           props.put(step26_$SATX3, "1");
           props.put(step27_$ERST7, "1");
           props.put(step28_$CXCL4, "1");
           props.put(step29_$MKUN2, "1");
           props.put(step30_$PGEN3, "1");
           props.put(step31_$WGEN3, "1");
           props.put(step32_$ADPT2, "1");
           props.put(step33_$PGEN4, "1");
           props.put(step34_$WGEN4, "1");
           props.put(step35_$CONVM, "1");
           props.put(step36_$ENGIN, "1");
           
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(PERLCOMMAND, "perl command used for executing scripts/Training.pl scripts.");
      
        stepsAdapt.add("preparing environments"); 
        stepsAdapt.add("computing a global variance");
        stepsAdapt.add("initialization & reestimation");
        stepsAdapt.add("making a monophone mmf");
        stepsAdapt.add("embedded reestimation (monophone)");
        stepsAdapt.add("copying monophone mmf to fullcontext one");
        stepsAdapt.add("embedded reestimation (fullcontext)");
        stepsAdapt.add("tree-based context clustering (cmp)");
        stepsAdapt.add("embedded reestimation (clustered)");
        stepsAdapt.add("untying the parameter sharing structure");
        stepsAdapt.add("embedded reestimation (untied)");
        stepsAdapt.add("tree-based context clustering (cmp)");
        stepsAdapt.add("embedded reestimation (re-clustered)");
        stepsAdapt.add("tree-based context clustering (dur)");
        stepsAdapt.add("making unseen models (speaker independent)");
        stepsAdapt.add("generating speech parameter sequences (speaker independent)");
        stepsAdapt.add("synthesizing waveforms (speaker independent)");
        stepsAdapt.add("building regression-class trees for adaptation");
        stepsAdapt.add("speaker adaptation (speaker independent)");
        stepsAdapt.add("generating speech parameter sequences (speaker adapted)");
        stepsAdapt.add("synthesizing waveforms (speaker adapted)");
        stepsAdapt.add("estimating transforms for speaker adaptive training (SAT) (1st iteration)");
        stepsAdapt.add("embedded reestimation (SAT, 1st iteration)");
        stepsAdapt.add("reestimating transforms for SAT (2nd iteration)");
        stepsAdapt.add("embedded reestimation (SAT, 2nd iteration)");
        stepsAdapt.add("reestimating transforms for SAT (3rd iteration)");
        stepsAdapt.add("embedded reestimation (SAT, 3rd iteration)");
        stepsAdapt.add("tree-based context clustering (dur, SAT)");
        stepsAdapt.add("making unseen models (SAT)");
        stepsAdapt.add("generating speech parameter sequences (SAT)");
        stepsAdapt.add("synthesizing waveforms (SAT)");
        stepsAdapt.add("speaker adaptation (SAT)");
        stepsAdapt.add("generate speech parameter sequences (SAT+adaptation)");
        stepsAdapt.add("synthesizing waveforms (SAT+adaptation)");
        stepsAdapt.add("converting mmfs to the hts_engine file format");
        stepsAdapt.add("synthesizing waveforms using hts_engine");
        
        props2Help.put(step1_$MKEMV, stepsAdapt.get(0)); 
        props2Help.put(step2_$HCMPV, stepsAdapt.get(1));
        props2Help.put(step3_$IN_RE, stepsAdapt.get(2));
        props2Help.put(step4_$MMMMF, stepsAdapt.get(3));
        props2Help.put(step5_$ERST0, stepsAdapt.get(4));
        props2Help.put(step6_$MN2FL, stepsAdapt.get(5));
        props2Help.put(step7_$ERST1, stepsAdapt.get(6));
        props2Help.put(step8_$CXCL1, stepsAdapt.get(7));
        props2Help.put(step9_$ERST2, stepsAdapt.get(8));
        props2Help.put(step10_$UNTIE, stepsAdapt.get(9));
        props2Help.put(step11_$ERST3, stepsAdapt.get(10));
        props2Help.put(step12_$CXCL2, stepsAdapt.get(11));
        props2Help.put(step13_$ERST4, stepsAdapt.get(12));
        props2Help.put(step14_$CXCL3, stepsAdapt.get(13));
        props2Help.put(step15_$MKUN1, stepsAdapt.get(14));
        props2Help.put(step16_$PGEN1, stepsAdapt.get(15));
        props2Help.put(step17_$WGEN1, stepsAdapt.get(16));
 
        props2Help.put(step18_$REGTR, stepsAdapt.get(17));
        props2Help.put(step19_$ADPT1, stepsAdapt.get(18));
        props2Help.put(step20_$PGEN2, stepsAdapt.get(19));
        props2Help.put(step21_$WGEN2, stepsAdapt.get(20));
        
        props2Help.put(step22_$SATX1, stepsAdapt.get(21));
        props2Help.put(step23_$ERST5, stepsAdapt.get(22));
        props2Help.put(step24_$SATX2, stepsAdapt.get(23));
        props2Help.put(step25_$ERST6, stepsAdapt.get(24));
        props2Help.put(step26_$SATX3, stepsAdapt.get(25));
        props2Help.put(step27_$ERST7, stepsAdapt.get(26));
        props2Help.put(step28_$CXCL4, stepsAdapt.get(27));
        props2Help.put(step29_$MKUN2, stepsAdapt.get(28));
        props2Help.put(step30_$PGEN3, stepsAdapt.get(29));
        props2Help.put(step31_$WGEN3, stepsAdapt.get(30));
        props2Help.put(step32_$ADPT2, stepsAdapt.get(31));
        props2Help.put(step33_$PGEN4, stepsAdapt.get(32));
        props2Help.put(step34_$WGEN4, stepsAdapt.get(33));
        props2Help.put(step35_$CONVM, stepsAdapt.get(34));
        props2Help.put(step36_$ENGIN, stepsAdapt.get(35));
        
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
        
        config.append("$REGTR = " + getProp(step18_$REGTR) + ";  # " + props2Help.get(step18_$REGTR) + "\n");
        config.append("$ADPT1 = " + getProp(step19_$ADPT1) + ";  # " + props2Help.get(step19_$ADPT1) + "\n");
        config.append("$PGEN2 = " + getProp(step20_$PGEN2) + ";  # " + props2Help.get(step20_$PGEN2) + "\n");
        config.append("$WGEN2 = " + getProp(step21_$WGEN2) + ";  # " + props2Help.get(step21_$WGEN2) + "\n");
        config.append("$SATX1 = " + getProp(step22_$SATX1) + ";  # " + props2Help.get(step22_$SATX1) + "\n");
        config.append("$ERST5 = " + getProp(step23_$ERST5) + ";  # " + props2Help.get(step23_$ERST5) + "\n");
        config.append("$SATX2 = " + getProp(step24_$SATX2) + ";  # " + props2Help.get(step24_$SATX2) + "\n");
        config.append("$ERST6 = " + getProp(step25_$ERST6) + ";  # " + props2Help.get(step25_$ERST6) + "\n");
        config.append("$SATX3 = " + getProp(step26_$SATX3) + ";  # " + props2Help.get(step26_$SATX3) + "\n");
        config.append("$ERST7 = " + getProp(step27_$ERST7) + ";  # " + props2Help.get(step27_$ERST7) + "\n");
        config.append("$CXCL4 = " + getProp(step28_$CXCL4) + ";  # " + props2Help.get(step28_$CXCL4) + "\n");
        config.append("$MKUN2 = " + getProp(step29_$MKUN2) + ";  # " + props2Help.get(step29_$MKUN2) + "\n");
        config.append("$PGEN3 = " + getProp(step30_$PGEN3) + ";  # " + props2Help.get(step30_$PGEN3) + "\n");
        config.append("$WGEN3 = " + getProp(step31_$WGEN3) + ";  # " + props2Help.get(step31_$WGEN3) + "\n");
        config.append("$ADPT2 = " + getProp(step32_$ADPT2) + ";  # " + props2Help.get(step32_$ADPT2) + "\n");
        config.append("$PGEN4 = " + getProp(step33_$PGEN4) + ";  # " + props2Help.get(step33_$PGEN4) + "\n");
        config.append("$WGEN4 = " + getProp(step34_$WGEN4) + ";  # " + props2Help.get(step34_$WGEN4) + "\n");
        config.append("$CONVM = " + getProp(step35_$CONVM) + ";  # " + props2Help.get(step35_$CONVM) + "\n");
        config.append("$ENGIN = " + getProp(step36_$ENGIN) + ";  # " + props2Help.get(step36_$ENGIN) + "\n");
        
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
        int i;
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
                  for( String stepLine : stepsAdapt){
                    if( line.contains(stepLine) ){
                      //System.out.println("STEP: " + stepsAdapt.indexOf(stepLine));
                      numSteps = stepsAdapt.indexOf(stepLine);
                      break;
                    }
                  }  
                  System.out.println( "\nStep (" + (numSteps+1) + "): " + line );
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
