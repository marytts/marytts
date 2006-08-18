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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.lang.ProcessBuilder;
import java.lang.Process;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.DatabaseLayout;

/**
 * This class emulates the behaviour of the EST-calling shell scripts
 * for the calculation of pitchmarks, lpc, mel-cepstrum etc.
 * 
 * @author sacha
 *
 */
public class ESTCaller
{ 
    /* Fields */
    private String ESTDIR = "/home/cl-home/sacha/temp/speech_tools/";
    private ProcessBuilder pb;
    private DatabaseLayout db;
 
    /****************/
    /* CONSTRUCTORS */
    /****************/
    
    public ESTCaller( String newESTDir, DatabaseLayout newDb ) {
        
        ESTDIR = newESTDir;
        db = newDb;
        
        pb = new ProcessBuilder( new String[]{" "} );
        Map env = pb.environment();
        env.put("ESTDIR", ESTDIR );
        pb.directory( db.baseDir() );
        pb.redirectErrorStream( true );
        
    }
    
    /*****************/
    /* OTHER METHODS */
    /*****************/
    
    /**
     * A general process launcher for the various tasks
     * 
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        BufferedReader procStdout = null;
        String line = null;
        String[] cmd = null;
        
        try {
            /* Hook the command line to the process builder: */
            cmd = cmdLine.split( " " );
            pb.command( cmd );
            /* Launch the process: */
            proc = pb.start();
            
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
                new Error( task + " computation failed on file [" + baseName + "]!" );
            }
        }
        catch ( IOException e ) {
            new Error( task + " computation provoked an IOException on file [" + baseName + "]." );
        }
        catch ( InterruptedException e ) {
            new Error( task + " computation interrupted on file [" + baseName + "]." );
        }
        
    }
    
    /**
     * An equivalent to the make_pm_wave shell script
     * 
     * @param db The layout of the processed database
     * @param baseNameArray The array of basenames of the .wav files to process
     * 
     */
    public void make_pm_wave( String[] baseNameArray ) {
        
        System.out.println("---- Calculating the pitchmarks..." );
        
        String cmdLine = null;
        
        /* For each file (or each basename): */
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            
            /* Make the command lines and launch them */
            /* - Scaling + resampling: */
            cmdLine = ESTDIR + "/bin/ch_wave -scaleN 0.9 -F 16000 "
            + "-o tmp" + baseNameArray[i] + ".wav "
            + db.wavDirName() + baseNameArray[i] + ".wav";
            launchProc( cmdLine, "Pitchmarks ", baseNameArray[i] );
            
            /* - Pitchmarks extraction: */
            cmdLine = ESTDIR + "/bin/pitchmark -min 0.0057 -max 0.012 -def 0.01 -wave_end -lx_lf 140 -lx_lo 111 -lx_hf 80 -lx_ho 51 -med_o 0 -fill -otype est "
            + "-o " + db.pitchmarksDirName() + baseNameArray[i] + ".pm "
            + "tmp" + baseNameArray[i] + ".wav";
            launchProc( cmdLine, "Pitchmarks ", baseNameArray[i] );
            
            /* - Cleanup of temporary file: */
            cmdLine = "rm -f tmp" + baseNameArray[i] + ".wav";
            launchProc( cmdLine, "Pitchmarks ", baseNameArray[i] );
            
        }
        System.out.println("---- Pitchmarks done." );
    }

    /**
     * An equivalent to the make_lpc shell script
     * 
     * @param db The layout of the processed database
     * @param baseNameArray The array of basenames of the .wav files to process
     * 
     */
    public void make_lpc( String[] baseNameArray ) {
        
        System.out.println("---- Calculating the LPC coefficents..." );
        
        String cmdLine = null;
        
        /* For each file (or each basename): */
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            
            /* Make the command line */
            cmdLine = ESTDIR + "/bin/sig2fv "
                + "-window_type hamming -factor 3 -otype est_binary -preemph 0.95 -coefs lpc -lpc_order 16 "
                + "-pm " + db.pitchmarksDirName() + baseNameArray[i] + ".pm "
                + "-o " + db.lpcDirName() + baseNameArray[i] + ".lpc "
                + db.wavDirName() + baseNameArray[i] + ".wav ";
            // System.out.println( cmdLine );
            
            /* Launch the relevant process */
            launchProc( cmdLine, "LPC ", baseNameArray[i] );
        }
        System.out.println("---- LPC coefficients done." );
    }

    /**
     * An equivalent to the make_mcep shell script
     * 
     * @param db The layout of the processed database
     * @param baseNameArray The array of basenames of the .wav files to process
     * 
     */
    public void make_mcep( String[] baseNameArray ) {
        
        System.out.println("---- Calculating the Mel-Cepstrum coefficents..." );
        
        String cmdLine = null;
        
        /* For each file (or each basename): */
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            
            /* Make the command line */
           cmdLine = ESTDIR + "/bin/sig2fv "
                + "-window_type hamming -factor 2.5 -otype est_binary -coefs melcep -melcep_order 12 -fbank_order 24 -shift 0.01 -preemph 0.97 "
                + "-pm " + db.pitchmarksDirName() + baseNameArray[i] + ".pm "
                + "-o " + db.melcepDirName() + baseNameArray[i] + ".mcep "
                + db.wavDirName() + baseNameArray[i] + ".wav ";
            // System.out.println( cmdLine );
           /* Note: parameter "-delta melcep" has been commented out in the original script.
            * Refer to the EST docs on http://www.cstr.ed.ac.uk/projects/speech_tools/manual-1.2.0/
            * for the meaning of the command line parameters. */
            
            /* Launch the relevant process */
            launchProc( cmdLine, "Mel-Cepstrum ", baseNameArray[i] );
        }
        System.out.println("---- Mel-Cepstrum coefficients done." );
    }

    
    
 }