package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;

/**
 * For the given texts, compute unit features and align them
 * with the given unit labels.
 * @author schroed
 *
 */
public class PhoneUnitFeatureComputer extends VoiceImportComponent
{
    protected File textDir;
    protected File unitfeatureDir;
    protected String featsExt;
    protected String locale;
    protected MaryClient mary;
    protected String maryInputType;
    protected String maryOutputType;

    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    public String FEATUREDIR = "phoneUnitFeatureComputer.featureDir";
    public String FEATUREEXT = "phoneUnitFeatureComputer.featureExt";
    public String MARYSERVERHOST = "phoneUnitFeatureComputer.maryServerHost";
    public String MARYSERVERPORT = "phoneUnitFeatureComputer.maryServerPort";
    
    public String getName(){
        return "phoneUnitFeatureComputer";
    }
    
    public static String getMaryXMLHeaderWithInitialBoundary(String locale)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + locale + "\">\n" +
            "<boundary duration=\"100\"/>\n";
        
    }
    
     public void initialise( BasenameList setbnl, SortedMap newProps )
    {
        this.bnl = setbnl;
        this.props = newProps;        
        locale = db.getProp(db.LOCALE);        
        mary = null; // initialised only if needed   
        unitfeatureDir = new File(getProp(FEATUREDIR));
        if (!unitfeatureDir.exists()){
            System.out.print(FEATUREDIR+" "+getProp(FEATUREDIR)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create FEATUREDIR");
            }
            System.out.print("Created successfully.\n");
        }    
        featsExt = getProp(FEATUREEXT);
        maryInputType = "RAWMARYXML";
        if (locale.equals("de")) maryOutputType = "TARGETFEATURES_DE";
        else if (locale.equals("en")) maryOutputType = "TARGETFEATURES_EN";
        else throw new IllegalArgumentException("Unsupported locale: "+locale);
    }
    
      public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(FEATUREDIR, db.getProp(db.ROOTDIR)
                        +"phonefeatures"
                        +System.getProperty("file.separator"));
           props.put(FEATUREEXT,".pfeats");
           props.put(MARYSERVERHOST,"localhost");
           props.put(MARYSERVERPORT,"59125");
       } 
       return props;
      }
    
    public MaryClient getMaryClient() throws IOException
    {
        if (mary == null) {
            if (System.getProperty("server.host") == null) {
                System.setProperty("server.host", getProp(MARYSERVERHOST));
            }
            if (System.getProperty("server.port") == null) {
                System.setProperty("server.port", getProp(MARYSERVERPORT));
            }
            mary = new MaryClient();
        }
        return mary;
    }

    public boolean compute() throws IOException
    {
        
        textDir = new File(db.getProp(db.TEXTDIR));
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
        File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR)
                				+ basename + db.getProp(db.MARYXMLEXT));
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
        } else {
            text = getMaryXMLHeaderWithInitialBoundary(locale)
                + FileUtils.getFileAsString(new File(db.getProp(db.TEXTDIR) 
                        		+ basename + db.getProp(db.TEXTEXT)), "UTF-8")
                + "</maryxml>";
        }
        
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File( unitfeatureDir, basename + featsExt )));
        MaryClient maryClient = getMaryClient();
        maryClient.process(text, maryInputType, maryOutputType, null, null, os);
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
