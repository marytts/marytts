package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class UnitLabelComputer implements VoiceImportComponent
{
    protected File phonelabelDir;
    protected File unitlabelDir;
    protected String pauseSymbol;
    
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    
    /**/
    public UnitLabelComputer( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;

        phonelabelDir = new File( db.labDirName() );
        if (!phonelabelDir.exists()) throw new IOException("No such directory: "+ phonelabelDir);
        unitlabelDir = new File( db.unitLabDirName() );
        if (!unitlabelDir.exists()) unitlabelDir.mkdir();
        pauseSymbol = System.getProperty( "pause.symbol", "pau" );

    }
    
    /**/
    public boolean compute() throws IOException
    {
        System.out.println( "Computing unit labels for " + bnl.getLength() + " files." );
        System.out.println( "From phonetic label files: " + db.labDirName() + "*" + db.labExt() );
        System.out.println( "To       unit label files: " + db.unitLabDirName() + "*" + db.unitLabExt() );
        for (int i=0; i<bnl.getLength(); i++) {
            File labFile = new File( db.labDirName() + bnl.getName(i) + db.labExt() );
            if ( !labFile.exists() ) {
                System.out.println( "Utterance [" + bnl.getName(i) + "] does not have a phonetic label file." );
                System.out.println( "Removing this utterance from the base utterance list." );
                bnl.remove( bnl.getName(i) );
            }
            else {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream( labFile ), "UTF-8"));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File( db.unitLabDirName() + bnl.getName(i) + db.unitLabExt() )), "UTF-8"));
                // Merge adjacent pauses into one: In a sequence of pauses,
                // only remember the last one.
                String pauseLine = null;
                String line;
                while ((line = in.readLine())!= null) {
                    // Verify if this is a pause unit
                    String unit = getUnit(line);
                    if (pauseSymbol.equals(unit)) {
                        pauseLine = line; // remember only the latest pause line
                    } else { // a non-pause symbol
                        if (pauseLine != null) {
                            out.println(pauseLine);
                            pauseLine = null;
                        }
                        out.println(line);
                    }
                }
                if (pauseLine != null) {
                    out.println(pauseLine);
                }
                out.flush();
                out.close();
                System.out.println( "    " + bnl.getName(i) );
            }
        }
        System.out.println("Finished computing unit labels");
        return true;
    }
    
    /**/
    protected String getUnit(String line)
    {
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) st.nextToken();
        if (st.hasMoreTokens()) return st.nextToken();
        return null;
    }
    
    public static void main(String[] args) throws IOException
    {
        new UnitLabelComputer( null, null ).compute();
    }

}
