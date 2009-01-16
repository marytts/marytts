/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * A class which converts a text file in festvox format
 * into a one-file-per-utterance format in a given directory.
 * @author schroed
 *
 */
public class Festvox2MaryTranscripts extends VoiceImportComponent
{
    protected File textFile;
    protected File textDir;
    
    protected DatabaseLayout db = null;
    public final String TRANSCRIPTFILE = "Festvox2MaryTranscripts.transcriptFile";
 
    
    public String getName(){
        return "Festvox2MaryTranscripts";
    }
    
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
       if (props == null){
           props = new TreeMap();
           props.put(TRANSCRIPTFILE,db.getProp(db.ROOTDIR)
           				+"txt.done.data");
       }
       return props;
   }
    
    protected void setupHelp(){         
        props2Help = new TreeMap();
        props2Help.put(TRANSCRIPTFILE,"file containing the transcripts in festvox format");
    }
    
    /**/
    public boolean compute() throws IOException
    {
        //check if transcription file exists
        textFile = new File( getProp(TRANSCRIPTFILE) );
        if (!textFile.exists()) throw new IOException( "No such file: " + textFile );
        textDir = new File( db.getProp(db.TEXTDIR) );
        
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), "UTF-8"));
        String line;
        BasenameList checkList = new BasenameList();
        while ((line = in.readLine()) != null) {
            line = line.substring(line.indexOf("(")+1, line.lastIndexOf(")"));
            StringTokenizer st = new StringTokenizer(line);
            String basename = st.nextToken();
            /* If the basename list asks to process this file, then write the text file */
            if ( bnl.contains( basename ) ) {
                checkList.add( basename );
                PrintWriter out = 
                    new PrintWriter( 
                            new OutputStreamWriter(
                                    new FileOutputStream(
                                            new File( textDir, basename + db.getProp(db.TEXTEXT) )), "UTF-8" ));
                String text = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
                out.print(text);
                out.flush();
                out.close();
            }
            
        }
        /* Check if all the basenames requested in the basename list were present in the text file */
        BasenameList diffList = bnl.duplicate();
        diffList.remove( checkList );
        if ( diffList.getLength() != 0 ) {
            System.out.println( "WARNING: the following utterances have not been found in the file [" 
                    				+ getProp(TRANSCRIPTFILE) + "]:" );
            for ( int i = 0; i < diffList.getLength(); i++ ) {
                System.out.println( diffList.getName(i) );
            }
            System.out.println( "They will be removed from the base utterance list." );
            bnl.remove( diffList );
            return( false );
        }
        else return (true );
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return -1;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception 
    {
        Festvox2MaryTranscripts f2mt = 
            new Festvox2MaryTranscripts();
        DatabaseLayout db = new DatabaseLayout(f2mt);
        f2mt.compute();
    }

}
