/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import marytts.client.MaryClient;
import marytts.client.http.Address;
import marytts.client.http.MaryHttpClient;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;



/**
 * For the given texts, compute allophones, especially boundary tags.
 * @author Benjamin Roth, adapted from Sathish Chandra Pammi
 *
 */
public class AllophonesExtractor extends VoiceImportComponent
{
    protected File textDir;
    protected File unitfeatureDir;
    protected String featsExt = ".xml";
    protected String locale;
    protected MaryHttpClient mary;
    protected String maryInputType;
    protected String maryOutputType;
    
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public String ALLOPHONES = "AllophonesExtractor.promptallophonesDir";
    public String MARYSERVERHOST = "AllophonesExtractor.maryServerHost";
    public String MARYSERVERPORT = "AllophonesExtractor.maryServerPort";
       
   
    
   public String getName(){
        return "AllophonesExtractor";
    }
     
   public void initialiseComp()
    {      
        locale = db.getProp(db.LOCALE);   
        mary = null; // initialised only if needed   
        unitfeatureDir = new File(getProp(ALLOPHONES));
        if (!unitfeatureDir.exists()){
            System.out.print(ALLOPHONES+" "+getProp(ALLOPHONES)
                    +" does not exist; ");
            if (!unitfeatureDir.mkdir()){
                throw new Error("Could not create ALLOPHONES");
            }
            System.out.print("Created successfully.\n");
        }    
        
        maryInputType = "RAWMARYXML";
        maryOutputType = "ALLOPHONES";
    }
     
     public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb)
     {
         this.db = theDb;
         if (props == null) {
             props = new TreeMap<String, String>();
             props.put(ALLOPHONES, db.getProp(db.ROOTDIR)
                     +"prompt_allophones"
                     +System.getProperty("file.separator"));
             props.put(MARYSERVERHOST,"localhost");
             props.put(MARYSERVERPORT,"59125");
         } 
         return props;
     }
     
     protected void setupHelp()
     {
         props2Help = new TreeMap<String, String>();
         props2Help.put(ALLOPHONES, "directory to store allophonesXML files." 
                 +"Will be created if it does not exist");
         props2Help.put(MARYSERVERHOST,"the host were the Mary server is running, default: \"localhost\"");
         props2Help.put(MARYSERVERPORT,"the port were the Mary server is listening, default: \"59125\"");
     }
     
     public MaryHttpClient getMaryClient() throws IOException
     {
        if (mary == null) {
            try{
                Address server = new Address(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT)));
                mary = new MaryHttpClient(server);
            } catch (IOException e){
                throw new IOException("Could not connect to Maryserver at "
                        +getProp(MARYSERVERHOST)+" "+getProp(MARYSERVERPORT));
            }
        }
        return mary;
    }

    public boolean compute() throws IOException
    {
        String inputDir = db.getProp(db.TEXTDIR);
        textDir = new File(db.getProp(db.TEXTDIR));
        System.out.println( "Computing IntonisedXML files for "+ bnl.getLength() + " files" );
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            computeFeaturesFor( bnl.getName(i), inputDir, unitfeatureDir.getAbsolutePath());
            System.out.println( "    " + bnl.getName(i) );
        }
        System.out.println("...Done.");
        return true;
    }

    public void computeFeaturesFor(String basename, String inputDir, String outputDir)
    throws IOException
    {
        String text;
        Locale localVoice;
        localVoice = MaryClient.string2locale(locale);
        String xmlLocale = MaryUtils.locale2xmllang(localVoice);
        
        String fullFileName;
        
        if (db.getProp(db.TEXTEXT).startsWith(System.getProperty("file.separator"))){
            // absolute path
            fullFileName = inputDir + File.separator + basename + db.getProp(db.TEXTEXT);
        } else {
            // relative path
            fullFileName = db.getProp(db.ROOTDIR) + System.getProperty("file.separator") + inputDir
            +File.separator+ basename + db.getProp(db.TEXTEXT);
        }
        
        File textFile = new File(fullFileName);
        text = FileUtils.getFileAsString(textFile, "UTF-8");
        
        
        // First, test if there is a corresponding .rawmaryxml file in textdir:
        File rawmaryxmlFile = new File(db.getProp(db.MARYXMLDIR) + File.separator
                                + basename + db.getProp(db.MARYXMLEXT));
        if (rawmaryxmlFile.exists()) {
            text = FileUtils.getFileAsString(rawmaryxmlFile, "UTF-8");
        } else {
            text = getMaryXMLHeaderWithInitialBoundary(xmlLocale)
                + FileUtils.getFileAsString(new File(inputDir 
                                + basename + db.getProp(db.TEXTEXT)), "UTF-8")
                + "</maryxml>";
        }
        
        OutputStream os = new BufferedOutputStream(new FileOutputStream(new File( outputDir, basename + featsExt )));
        MaryHttpClient maryClient = getMaryClient();
        
        maryClient.process(text, maryInputType, maryOutputType, db.getProp(db.LOCALE), null, null, os);
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

