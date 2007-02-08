package de.dfki.lt.mary.gizmos;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.mary.unitselection.voiceimport.BasenameList;

public class TxtToUnitfeats_HTS_BITS {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        BasenameList basenames = new BasenameList( "gen_text", ".txt" );
        String text;
        if (System.getProperty("server.host") == null) {
            System.setProperty("server.host", "localhost");
        }
        if (System.getProperty("server.port") == null) {
            System.setProperty("server.port", "59125");
        }
        MaryClient maryClient = new MaryClient();
        for (int i=0; i<basenames.getLength(); i++) {
            text = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + "en" + "\">\n" +
            "<boundary duration=\"100\"/>\n"
            + FileUtils.getFileAsString(new File( "gen_text/" + basenames.getName(i) + ".txt" ), "UTF-8")
            + "</maryxml>";
            String inputFormat = "RAWMARYXML";
            String outputFormat = "TARGETFEATURES";
            OutputStream os = new BufferedOutputStream(new FileOutputStream(new File( "unitfeats/gen/" + basenames.getName(i) + ".feats" )));
            maryClient.process(text, inputFormat, outputFormat, null, null, os);
            os.flush();
            os.close();
            System.out.println( "    " + basenames.getName(i) );
        }
        System.out.println("Finished computing the unit features.");
    }

}
