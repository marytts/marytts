package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.MaryDataType;


/**
 * For the given texts, compute phoneme features.
 * @author Sathish Chandra Pammi
 *
 */
public class PhonemisedXML extends VoiceImportComponent
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
    
    public String PHONEMISED = "PhonemisedXML.phonemisedDir";
    public String MARYSERVERHOST = "PhonemisedXML.maryServerHost";
    public String MARYSERVERPORT = "PhonemisedXML.maryServerPort";
       
   
    
   public String getName(){
        return "PhonemisedXML";
    }
     
   public void initialiseComp()
    {      
        locale = db.getProp(db.LOCALE);   
        
        mary = null; // initialised only if needed   
        unitfeatureDir = new File(getProp(PHONEMISED));
        if (!unitfeatureDir.exists()){
            System.out.print(PHONEMISED+" "+getProp(PHONEMISED)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create PHONEMISED");
            }
            System.out.print("Created successfully.\n");
        }    
        
        //maryInputType = "RAWMARYXML";
        maryInputType = "TEXT_DE";
        maryOutputType = "PHONEMISED_DE";
    }
     
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
         if (props == null){
             props = new TreeMap();
             props.put(PHONEMISED, db.getProp(db.ROOTDIR)
                     +"phonemisedXML"
                     +System.getProperty("file.separator"));
             props.put(MARYSERVERHOST,"localhost");
             props.put(MARYSERVERPORT,"59125");
         } 
         return props;
     }
     
     protected void setupHelp(){
         props2Help = new TreeMap();
         props2Help.put(PHONEMISED, "directory containing the phone features." 
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
        Locale localVoice;
        localVoice = MaryClient.string2locale(locale);
        File textFile = new File(db.getProp(db.TEXTDIR)
                + basename + db.getProp(db.TEXTEXT));
        text = FileUtils.getFileAsString(textFile, "UTF-8");
        
        /*// First, test if there is a corresponding .rawmaryxml file in textdir:
        File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR)
                				+ basename + db.getProp(db.MARYXMLEXT));
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
        } else {
            text = getMaryXMLHeaderWithInitialBoundary(locale)
                + FileUtils.getFileAsString(new File(db.getProp(db.TEXTDIR) 
                        		+ basename + db.getProp(db.TEXTEXT)), "UTF-8")
                + "</maryxml>";
        }*/
        
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File( unitfeatureDir, basename + featsExt )));
        MaryClient maryClient = getMaryClient();
        Vector voices = maryClient.getVoices(localVoice);
        MaryClient.Voice defaultVoice = (MaryClient.Voice) voices.firstElement();
        String voiceName = defaultVoice.name();
        maryClient.process(text, maryInputType, maryOutputType, null, voiceName, os);
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
