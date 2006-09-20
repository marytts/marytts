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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.dfki.lt.mary.unitselection.voiceimport_reorganized.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport_reorganized.LPCTimelineMaker;

/**
 * The single purpose of the DatabaseImportMain class is to provide a main
 * which executes the sequence of database import and conversion operations.
 * 
 * @author sacha
 *
 */
public class DatabaseImportMain extends JFrame 
{ 
    protected VoiceImportComponent[] components;
    protected JCheckBox[] checkboxes;
    
    public DatabaseImportMain(String title, VoiceImportComponent[] components)
    {
        super(title);
        this.components = components;
        this.checkboxes = new JCheckBox[components.length];
        setupGUI();
    }
    
    protected void setupGUI()
    {
        // A scroll pane containing one labelled checkbox per component,
        // and a "run selected components" button below.
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridC = new GridBagConstraints();
        setLayout( gridBagLayout );

        JPanel checkboxPane = new JPanel();
        checkboxPane.setLayout(new BoxLayout(checkboxPane, BoxLayout.Y_AXIS));
        //checkboxPane.setPreferredSize(new Dimension(250, 300));
        for (int i=0; i<components.length; i++) {
            System.out.println("Adding checkbox for "+components[i].getClass().getName());
            checkboxes[i] = new JCheckBox(components[i].getClass().getSimpleName());
            //checkboxes[i].setPreferredSize(new Dimension(200, 30));
            checkboxPane.add(checkboxes[i]);
        }
        gridC.gridx = 0;
        gridC.gridy = 0;
        gridC.fill = GridBagConstraints.BOTH;
        JScrollPane scrollPane = new JScrollPane(checkboxPane);
        scrollPane.setPreferredSize(new Dimension(300,300));
        gridBagLayout.setConstraints( scrollPane, gridC );
        add(scrollPane);

        JButton runButton = new JButton("Run selected components");
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                runSelectedComponents();
            }
        });
        gridC.gridy = 1;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(runButton);
        gridBagLayout.setConstraints( buttonPanel, gridC );
        add(buttonPanel);

        // End program when closing window:
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                System.exit(0);
            }
        });
    }
    
    protected void runSelectedComponents()
    {
        for (int i=0; i<components.length; i++) {
            if (checkboxes[i].isSelected()) {
                boolean success = false;
                try {
                    success = components[i].compute();
                } catch (Exception exc) {
                    exc.printStackTrace();
                    success = false;
                }
                if (success) {
                    checkboxes[i].setBackground(Color.GREEN);
                } else {
                    checkboxes[i].setBackground(Color.RED);
                }
            }
        }
    }
    
    /**
     *  Imports a database from a set of wav files:
     *  - launches the EST tools to compute the LPCs
     *  - reads and concatenates the LPC EST tracks into one single timeline file.
     *  - reads the unit catalog from the .catalogue file
     *  - reads and dumps the CARTs 
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
     *          <li> <code> <voiceName> </code> The name of the new voice 
     *          
     *          <li> <code> <targetFeatureFile> </code> The file defining the
     *          names, weights and types of the target features
     *          
     *          <li> <code> <joinFeatureFile> </code> The file defining the
     *          weights of the join features
     *          
     *          <li> <code> <databaseBaseName> </code> The location of the base directory
     *          holding the database. <dataBaseDir>/wav/ should hold the corresponding initial
     *          .wav files. If not given, defaults to ./ .
     *    </ul>
     * 
     */
    public static void main( String[] args ) throws IOException
    {
        VoiceImportComponent[] components = new VoiceImportComponent[] {
            new FestvoxTextfileConverter(),
            new UnitLabelComputer(),
            new UnitFeatureComputer(),
            new LabelFeatureAligner(),
            new UnitfileWriter()
        };
        DatabaseImportMain importer = new DatabaseImportMain("Database import", components);
        importer.pack();
        // Center window on screen:
        importer.setLocationRelativeTo(null); 
        importer.setVisible(true);
        
        // The following code is independent of the GUI; both are running in separate threads.
        // TODO: This might be confusing and should be cleaned up.
        
        /* Read in the args */
        boolean recompute = false;
        
        if ( args.length > 0 ){
            if ( args[0].equals("-r") || args[0].equals("--recompute") ) {
                recompute = true;
            }
            else {
                System.setProperty( "db.baseName", args[0] );
            }
        }
        if ( args.length > 3 ){
            System.setProperty( "db.targetFeaturesFileName", args[2] );
            System.setProperty( "db.joinCostFeaturesFileName", args[2] );
        } else {
            System.out.println("Need voicename, targetFeatureFile, joinFeatureFile.\n" 
                            +" Usage:\n java ImportDatabase [-r|--recompute]"
                            +" <voicename> <targetFeaturesFile>" 
                            +" <joinFeaturesFile> <databaseDir>.\n");
            return;
        }
        if ( args.length > 4 ){
            System.setProperty( "db.baseName", args[4] );
        }
        if ( args.length > 5 ){
            System.out.println("Usage:\n java ImportDatabase [-r|--recompute]"
                            +" <voicename> <targetFeaturesFile>" 
                            +" <joinFeaturesFile> <databaseDir>.\n" +
            "Ignoring additional arguments after <databaseDir>." );
        }
        
        /* Invoke a new database layout, starting in the database directory */
        DatabaseLayout db = new DatabaseLayout();
        
        /* Sum up the argument parsing results */
        System.out.println("Importing Voice in base directory [" + db.baseName() + "]." );
        if ( recompute ) {
            System.out.println("EST files WILL be recomputed." );
        }
        else {
            System.out.println("EST files WILL NOT be recomputed." );
        }
        
        /* Prepare the output directory for the timelines */
        File timelineDir = new File( db.timelineDirName() );
        if ( !timelineDir.exists() ) {
            timelineDir.mkdir();
            System.out.println("Created output directory [" + db.timelineDirName() + "] to store the timelines." );
        }
        
        /* List the wav files: */
        String[] baseNameArray = new BasenameList( db.wavDirName() ).getListAsArray();
        System.out.println("Found [" + baseNameArray.length + "] wav files to convert." );
        
        /* If recomputation is asked for, launch the EST utilities */
        if ( recompute ) {
            ESTCaller caller = new ESTCaller( db, "/home/cl-home/sacha/temp/speech_tools/" );
            caller.make_pm_wave( baseNameArray );
            caller.make_lpc( baseNameArray );
            caller.make_mcep( baseNameArray );
        }
        
        /* Invoke the LPC timeline maker */
        LPCTimelineMaker.run( db, baseNameArray );
        
        /* Invoke the MCep timeline maker */
        MCepTimelineMaker.run( db, baseNameArray );
        
        /* Read in the units into a catalogue */
        //Get the catalog file
        File catalogDir = new File( db.baseName() + "/festival/clunits");
        File catalogFile = catalogDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
               return name.endsWith(".catalogue");
        }
        })[0];
        //Read in the catalog 
        System.out.println("Reading Catalog : " + catalogFile.getPath());
        UnitCatalog unitCatalog = new UnitCatalog(catalogFile.getPath());
        
        /* Read and dump the CARTs */
        
        CARTImporter cp = new CARTImporter();
        cp.importCARTS( db.baseName(), db.cartsDirName(), unitCatalog);

        /* Close the shop */
        System.out.println( "----\n" + "---- Rock'n Roll!" );
    }
    
    
}