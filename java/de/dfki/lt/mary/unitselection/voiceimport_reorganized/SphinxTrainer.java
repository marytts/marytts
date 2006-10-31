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

import java.io.*;

/**
 * Class to train sphinx labeler
 * 
 * @author Anna Hunecke
 *
 */
public class SphinxTrainer implements VoiceImportComponent {
    
    private DatabaseLayout dbLayout;
    
    /**
     * Get a new Sphinx trainer
     * 
     * @param dbLayout the database layout
     */
    public SphinxTrainer(DatabaseLayout dbLayout){
        this.dbLayout = dbLayout;
    }
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        //Run the sphinxtrain scripts
        Runtime rtime = Runtime.getRuntime();
        String rootDirName = new File(dbLayout.rootDirName()).getCanonicalPath();
        
        //get a shell
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to directory where the scripts are
        pw.print("cd "+rootDirName+"/st/scripts_pl/\n");
        pw.flush();
        //call the scripts
        pw.print("(00.verify/verify_all.pl "
                +"; 01.vector_quantize/slave.VQ.pl "
                +"; 02.ci_schmm/slave_convg.pl "
                +"; 03.makeuntiedmdef/make_untied_mdef.pl "
                +"; 04.cd_schmm_untied/slave_convg.pl "
                +"; 05.buildtrees/make_questions.pl "
                +"; 05.buildtrees/slave.treebuilder.pl "
                +"; 06.prunetree/slave.state-tie-er.pl "
                +"; 07.cd-schmm/slave_convg.pl "
                +"; 08.deleted-interpolation/deleted_interpolation.pl "
                +"; 09.make_s2_models/make_s2_models.pl "        
                +")\n");
        pw.flush();
        //collect the output
        String line;
        BufferedReader inReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        
        while((line = inReader.readLine()) != null){
            System.out.println(line);
        }
        BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
        while((line = errReader.readLine()) != null){
            System.out.println(line);
        }
        //close everything down
        errReader.close();
        inReader.close();
        pw.print("exit\n");
        pw.flush();
        pw.close();
        process.waitFor();
        process.exitValue();
        
        return true;
    }
    
    
}