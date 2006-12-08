package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;

/**
 * For the given texts, compute unit features and align them
 * with the given unit labels.
 * @author schroed
 *
 */
public class UnitFeatureComputer implements VoiceImportComponent
{
    protected File textDir;
    protected File unitfeatureDir;
    protected String locale;
    protected MaryClient mary;

    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    public static String getMaryXMLHeaderWithInitialBoundary(String locale)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + locale + "\">\n" +
            "<boundary duration=\"100\"/>\n";
        
    }
    
    public UnitFeatureComputer( DatabaseLayout setdb, BasenameList setbnl )
    throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;

        
        locale = System.getProperty("locale", "en");

        mary = null; // initialised only if needed
    }
    
    public File getTextDir()
    {
        return textDir;
    }
    
    public File getUnitFeatureDir()
    {
        return unitfeatureDir;
    }
    
    public String getLocale()
    {
        return locale;
    }
    
    public MaryClient getMaryClient() throws IOException
    {
        if (mary == null) {
            if (System.getProperty("server.host") == null) {
                System.setProperty("server.host", "localhost");
            }
            if (System.getProperty("server.port") == null) {
                System.setProperty("server.port", "59125");
            }
            mary = new MaryClient();
        }
        return mary;
    }

    public boolean compute() throws IOException
    {
        textDir = new File( db.txtDirName() );
        if (!textDir.exists()) throw new IOException("No such directory: "+ textDir);
        unitfeatureDir = new File( db.unitFeaDirName() );
        if (!unitfeatureDir.exists()) unitfeatureDir.mkdir();
        
        //String[] basenames = FileUtils.listBasenames(textDir, ".txt");
        //System.out.println("Computing unit features for "+basenames.length+" files");
        System.out.println( "Computing unit features for " + bnl.getLength() + " files" );
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            computeFeaturesFor( bnl.getName(i) );
            System.out.println( "    " + bnl.getName(i) );
        }
        System.out.println("Finished computing the unit features.");
        return true;
    }

    public void computeFeaturesFor(String basename) throws IOException
    {
        String text;
        // First, test if there is a corresponding .rawmaryxml file in textdir:
        File rawmaryxmlFile = new File( db.rmxDirName() + basename + db.rmxExt() );
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
        } else {
            text = getMaryXMLHeaderWithInitialBoundary(locale)
                + FileUtils.getFileAsString(new File( db.txtDirName() + basename + db.txtExt() ), "UTF-8")
                + "</maryxml>";
        }

        //just a hack
        //text = FileUtils.getFileAsString(new File( db.txtDirName() + basename + db.txtExt() ), "UTF-8");
        //text.trim();
        //String inputFormat = "TEXT";
        
        String inputFormat = "RAWMARYXML";
        String outputFormat = "TARGETFEATURES";
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File( db.unitFeaDirName() + basename + db.unitFeaExt() )));
        MaryClient maryClient = getMaryClient();
        maryClient.process(text, inputFormat, outputFormat, null, null, os);
        os.flush();
        os.close();
    }

    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

}
