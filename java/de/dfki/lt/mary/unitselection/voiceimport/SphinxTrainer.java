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

import java.io.*;
import java.util.*;

/**
 * Class to train sphinx labeler
 * 
 * @author Anna Hunecke
 *
 */
public class SphinxTrainer extends VoiceImportComponent {
    
    private DatabaseLayout db;
    
    public final String STDIR = "SphinxTrainer.stDir";
    
     public final String getName(){
        return "SphinxTrainer";
    }
    
   public SortedMap getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(STDIR,db.getProp(db.ROOTDIR)
           				+"st"
           				+System.getProperty("file.separator"));
       }
       return props;
   }
   
   protected void setupHelp(){
       props2Help = new TreeMap();
       props2Help.put(STDIR,"directory containing all files used for training and labeling");
   }
    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        System.out.println("Training HMMs for Sphinx labeling ...");
       
        //Run the sphinxtrain scripts
        Runtime rtime = Runtime.getRuntime();
        
        //get a shell
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to directory where the scripts are
        pw.print("cd "+getProp(STDIR)+"\n");
        pw.flush();
        //call the scripts and exit
        pw.print("(scripts_pl/00.verify/verify_all.pl "
                +"; scripts_pl/01.vector_quantize/slave.VQ.pl "
                +"; scripts_pl/02.ci_schmm/slave_convg.pl "
                +"; scripts_pl/03.makeuntiedmdef/make_untied_mdef.pl "
                +"; scripts_pl/04.cd_schmm_untied/slave_convg.pl "
                +"; scripts_pl/05.buildtrees/make_questions.pl "
                +"; scripts_pl/05.buildtrees/slave.treebuilder.pl "
                +"; scripts_pl/06.prunetree/slave.state-tie-er.pl "
                +"; scripts_pl/07.cd-schmm/slave_convg.pl "
                +"; scripts_pl/08.deleted-interpolation/deleted_interpolation.pl "
                +"; scripts_pl/09.make_s2_models/make_s2_models.pl "        
                +"; exit)\n");
        pw.flush();
        pw.close();
        
        //collect the output
        //read from error stream
        StreamGobbler errorGobbler = new 
            StreamGobbler(process.getErrorStream(), "err");            
        
        //read from output stream
        StreamGobbler outputGobbler = new 
            StreamGobbler(process.getInputStream(), "out");        
        //start reading from the streams
        errorGobbler.start();
        outputGobbler.start();
        
        //close everything down
        process.waitFor();
        process.exitValue();
        System.out.println("... done.");
        return true;
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return -1;
    }

    
    static class StreamGobbler extends Thread
    {
        InputStream is;
        String type;
        
        StreamGobbler(InputStream is, String type)
        {
            this.is = is;
            this.type = type;
        }
        
        public void run()
        {
            try
            {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line=null;
                while ( (line = br.readLine()) != null)
                    System.out.println(type + ">" + line);    
                } catch (IOException ioe)
                  {
                    ioe.printStackTrace();  
                  }
        }
    }
    
    
}