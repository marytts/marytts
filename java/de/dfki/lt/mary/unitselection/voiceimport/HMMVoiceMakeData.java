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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.SortedMap;
import java.util.TreeMap;
import de.dfki.lt.mary.unitselection.voiceimport.HMMVoiceConfigure;

public class HMMVoiceMakeData extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceMakeData";
    
    /** Tree files and TreeSet object */
    public final String MGC           = name+".makeMGC";
    public final String LF0           = name+".makeLF0";
    public final String STR           = name+".makeSTR";
    public final String CMPMARY       = name+".makeCMPMARY";
    public final String GVMARY        = name+".makeGV";
    public final String LABELMARY     = name+".makeLABELMARY";
    public final String COUNTMARY     = name+".makeCOUNTMARY";
    public final String QUESTIONSMARY = name+".makeQUESTIONSMARY";
    public final String MLF           = name+".makeMLF";
    public final String LIST          = name+".makeLIST";
    public final String SCP           = name+".makeSCP";
    
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
           
           props.put(MGC, "1");
           props.put(LF0, "1");
           props.put(STR, "1");
           props.put(CMPMARY, "1");
           props.put(GVMARY, "1");
           props.put(LABELMARY, "1");
           props.put(COUNTMARY, "1");
           props.put(QUESTIONSMARY, "1");
           props.put(MLF, "1");
           props.put(LIST, "1");
           props.put(SCP, "1");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap<String,String>();
        
        props2Help.put(MGC, "Extracting MGC or MGC-LSP coefficients from raw audio.");
        props2Help.put(LF0, "Extracting log f0 sequence from raw audio.");
        props2Help.put(STR, "Extracting strengths from 5 filtered bands from raw audio.");
        props2Help.put(CMPMARY, "Composing training data files from mgc, lf0 and str files.");
        props2Help.put(GVMARY, "Calculating GV and saving in Mary format.");
        props2Help.put(LABELMARY, "Extracting monophone and fullcontext labels from phonelab and phonefeature files.");
        props2Help.put(COUNTMARY, "Counting the phone occurrences and extracting the phonesets.");
        props2Help.put(QUESTIONSMARY, "Creating questions .hed file.");
        props2Help.put(MLF, "Generating monophone and fullcontext Master Label Files (MLF).");
        props2Help.put(LIST, "Generating a fullcntext model list occurred in the training data.");
        props2Help.put(SCP, "Generating a trainig data script.");

    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{

      
      String cmdLine;
      String filedir = db.getProp(db.ROOTDIR);
        
      if( Integer.parseInt(getProp(MGC)) == 1 ){
        cmdLine = "cd data\nmake mgc\n";
        launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LF0)) == 1 ){
          cmdLine = "cd data\nmake lf0\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(STR)) == 1 ){
          cmdLine = "cd data\nmake str\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(CMPMARY)) == 1 ){
          cmdLine = "cd data\nmake cmp-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(GVMARY)) == 1 ){
          cmdLine = "cd data\nmake gv-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LABELMARY)) == 1 ){
          cmdLine = "cd data\nmake label-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(COUNTMARY)) == 1 ){
          cmdLine = "cd data\nmake count-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(QUESTIONSMARY)) == 1 ){
          cmdLine = "cd data\nmake questions-mary\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(MLF)) == 1 ){
          cmdLine = "cd data\nmake mlf\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(LIST)) == 1 ){
          cmdLine = "cd data\nmake list\n";
          launchBatchProc(cmdLine, "", filedir);
      }
      if( Integer.parseInt(getProp(SCP)) == 1 ){
          cmdLine = "cd data\nmake scp\n";
          launchBatchProc(cmdLine, "", filedir);
      }

    
      /* delete the temporary file*/
      File tmpBatch = new File(filedir+"tmp.bat");
      tmpBatch.delete();
 
      return true;
        
    }
    
 
    /**
     * A general process launcher for the various tasks but using an intermediate batch file
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchBatchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.ROOTDIR);
        String tmpFile = filedir+"tmp.bat";

        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            proctmp.waitFor();
            
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( tmpFile );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
            
            
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
