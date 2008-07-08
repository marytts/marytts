package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import marytts.client.MaryClient;
import marytts.datatypes.MaryDataType;
import marytts.util.io.FileUtils;



/**
 * For the given texts, compute intonisation features, especially boundary tags.
 * @author Benjamin Roth, adapted from Sathish Chandra Pammi
 *
 */
public class IntonisedXMLExtractor extends VoiceImportComponent
{
    protected File textDir;
    protected File unitfeatureDir;
    protected String featsExt = ".xml";
    protected String locale;
    protected MaryClient mary;
    protected String maryInputType;
    protected String maryOutputType;
    
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public String INTONISED = "IntonisedXMLExtractor.intonisedDir";
    public String MARYSERVERHOST = "IntonisedXMLExtractor.maryServerHost";
    public String MARYSERVERPORT = "IntonisedXMLExtractor.maryServerPort";
       
   
    
   public String getName(){
        return "IntonisedXMLExtractor";
    }
     
   public void initialiseComp()
    {      
        locale = db.getProp(db.LOCALE);   
        
        mary = null; // initialised only if needed   
        unitfeatureDir = new File(getProp(INTONISED));
        if (!unitfeatureDir.exists()){
            System.out.print(INTONISED+" "+getProp(INTONISED)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create INTONISED");
            }
            System.out.print("Created successfully.\n");
        }    
        
        maryInputType = "RAWMARYXML";
        if(locale.startsWith("en")){
            maryOutputType = "INTONATION_EN";
        }
        else if(locale.startsWith("de")){
            maryOutputType = "INTONISED_DE";
        }
        
    }
     
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
         if (props == null){
             props = new TreeMap();
             props.put(INTONISED, db.getProp(db.ROOTDIR)
                     +"intonisedXML"
                     +System.getProperty("file.separator"));
             props.put(MARYSERVERHOST,"localhost");
             props.put(MARYSERVERPORT,"59125");
         } 
         return props;
     }
     
     protected void setupHelp(){
         props2Help = new TreeMap();
         props2Help.put(INTONISED, "directory to store intonationXML files." 
                 +"Will be created if it does not exist");
         props2Help.put(MARYSERVERHOST,"the host were the Mary server is running, default: \"localhost\"");
         props2Help.put(MARYSERVERPORT,"the port were the Mary server is listening, default: \"59125\"");
     }
     
     public MaryClient getMaryClient() throws IOException
     {
        if (mary == null) {
            try{
                mary = new MaryClient(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT)));        
            } catch (IOException e){
                throw new IOException("Could not connect to Maryserver at "
                        +getProp(MARYSERVERHOST)+" "+getProp(MARYSERVERPORT));
            }
        }
        return mary;
    }

    public boolean compute() throws IOException
    {
        
        textDir = new File(db.getProp(db.TEXTDIR));
        System.out.println( "Computing IntonisedXML files for "+ bnl.getLength() + " files" );
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            computeFeaturesFor( bnl.getName(i) );
            System.out.println( "    " + bnl.getName(i) );
        }
        System.out.println("...Done.");
        return true;
    }

    public void computeFeaturesFor(String basename) throws IOException
    {
        String text;
        Locale localVoice;
        localVoice = MaryClient.string2locale(locale);
        
        String fullFileName;
        
        if (db.getProp(db.TEXTEXT).startsWith(System.getProperty("file.separator"))){
            // absolute path
            fullFileName = db.getProp(db.TEXTDIR) + basename + db.getProp(db.TEXTEXT);
        } else {
            // relative path
            fullFileName = db.getProp(db.ROOTDIR) + System.getProperty("file.separator") + db.getProp(db.TEXTDIR)
            + basename + db.getProp(db.TEXTEXT);
        }
        
        File textFile = new File(fullFileName);
        text = FileUtils.getFileAsString(textFile, "UTF-8");
        
        
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
        
        Vector<MaryClient.Voice> voices = maryClient.getVoices(localVoice);
        if (voices == null) {
            if(locale.equals("en")) {
               locale  =  "en_US";
               localVoice = MaryClient.string2locale(locale);
               voices = maryClient.getVoices(localVoice);
            } 
        }
        // try again:
        if (voices == null) {
            StringBuffer buf = new StringBuffer("Mary server has no voices for locale '"+localVoice+"' -- known voices are:\n");
            Vector<MaryClient.Voice> allVoices = maryClient.getVoices();
            for (MaryClient.Voice v: allVoices) {
                buf.append(v.toString()); buf.append("\n");
            }
            throw new RuntimeException(buf.toString());
        }
        
        MaryClient.Voice defaultVoice = (MaryClient.Voice) voices.firstElement();
        String voiceName = defaultVoice.name();
        maryClient.process(text, maryInputType, maryOutputType, null, voiceName, os);
        os.flush();
        os.close();
    }

    public static String getMaryXMLHeaderWithInitialBoundary(String locale)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + locale + "\">\n" +
            "<boundary  breakindex=\"2\" duration=\"100\"/>\n";
        
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
