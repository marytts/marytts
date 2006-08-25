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

import java.io.File;
import java.io.FilenameFilter;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.LPCTimelineMaker;

public class DatabaseImportMain 
{ 
    /**
     *  Imports a database from a set of wav files:
     *  - launches the EST tools to compute the LPCs
     *  - reads and concatenates the LPC EST tracks into one single timeline file.
     *
     * <p>
     * <b> Usage </b>
     * <p>
     *  <code> java de.dfki.lt.mary.unitselection.voiceimport.databaseImportMain [-r|--recompute] <databaseDir></code> 
     * <p>
     * <b> Options </b>
     * <p>
     *    <ul>
     *          <li> <code> [ -r | --recompute ] </code> Re-compute the LPC parameters from the wav files,
     *          using the Festvox/EST shell scripts.
     *          
     *          <li> <code> <databaseBaseName> </code> The location of the base directory
     *          holding the database. <dataBaseDir>/wav/ should hold the corresponding initial
     *          .wav files. If not given, defaults to ./ .
     *    </ul>
     * 
     */
    public static void main( String[] args ) 
    {
        
        /* Read in the args */
        String databaseBaseName = ".";
        boolean recompute = false;
        
        if ( args.length > 0 ){
            if ( args[0].equals("-r") || args[0].equals("--recompute") ) {
                recompute = true;
            }
            else {
                databaseBaseName = args[0];
            }
        }
        if ( args.length > 1 ){
            databaseBaseName = args[1];
        }
        if ( args.length > 2 ){
            System.out.println("Usage:\n java ImportDatabase [-r|--recompute] <databaseDir>.\n" +
            "Ignoring additional arguments after <databaseDir>." );
        }
        
        /* Invoke a new database layout, starting in the database directory */
        DatabaseLayout db = new DatabaseLayout( databaseBaseName, "wav", "lpc", "timelines", "pm", "mcep" );
        
        /* Sum up the argument parsing results */
        System.out.println("Importing Voice in base directory [" + db.baseName() + "]." );
        if ( recompute ) {
            System.out.println("EST files WILL be recomputed." );
        }
        else {
            System.out.println("EST files WILL NOT be recomputed." );
        }
        
        /* Prepare the output directory for the timelines */
        if ( !db.timelineDir().exists() ) {
            db.timelineDir().mkdir();
            System.out.println("Created output directory [" + db.timelineDirName() + "] to store the timelines." );
        }
        
        /* List the wav files: */
        String[] baseNameArray = new BasenameList( db.wavDirName() ).getList();
        System.out.println("Found [" + baseNameArray.length + "] wav files to convert." );
        
        /* If recomputation is asked for, launch the EST utilities */
        if ( recompute ) {
            ESTCaller caller = new ESTCaller( db, "/home/cl-home/sacha/temp/speech_tools/" );
            caller.make_pm_wave( baseNameArray );
            caller.make_lpc( baseNameArray );
            caller.make_mcep( baseNameArray );
        }
        
        /* Invoke the LPC timeline maker */
        LPCTimelineMaker.run( db, baseNameArray, recompute );
        
        /* Read in the units into a catalogue */
        //Get the catalog file
        File catalogDir = new File(databaseBaseName + "/festival/clunits");
        File catalogFile = catalogDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
               return name.endsWith(".catalogue");
        }
        })[0];
        //Read in the catalogue 
        System.out.println("Reading Catalog : " + catalogFile.getPath());
        UnitCatalog unitCatalog = new UnitCatalog(catalogFile.getPath());
        
        /* Read and dump the CARTs */
        //TODO: Get destination path in here:
        String destPath = marybase + "/lib/voices/" + voiceName + "/";
        CARTImporter cp = new CARTImporter();
        cp.importCARTS(databaseBaseName, destPath, unitCatalog);

        /* Close the shop */
        System.out.println( "----\n" + "---- Rock'n Roll!" );
    }
    
    
}