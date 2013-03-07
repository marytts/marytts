/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.redstart;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;



/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
 */
public class PromptSet {
  
    // ______________________________________________________________________
    // Instance fields
    int promptCount = 0;        // Number of prompts in the set
    File promptFolderPath;      // Path containing prompt set structure
    Prompt[] promptArray;       // Array of prompts
    AdminWindow adminWindow;
    
    // ______________________________________________________________________
    // Class fields

    // ______________________________________________________________________
    // Instance methods
    
    public PromptSet(AdminWindow adminWindow) throws IOException
    {
        this.adminWindow = adminWindow;
        this.promptFolderPath = adminWindow.getPromptFolderPath();     // Set object's folder path containing the prompt set
        this.promptCount = countPrompts();      // Determine the number of prompts
        this.promptArray = getPromptData();     // Load the prompt set
                
        // TESTCODE
        for (int index = 0; index < this.promptCount; index++) {
            Test.output("|PromptSet| Basename " + index + ": " + this.promptArray[index].getBasename());
            Test.output("|PromptSet| Prompt Text " + index + ": " + this.promptArray[index].getPromptText());
            Test.output("|PromptSet| Status " + index + ": " + this.promptArray[index].getRecCount() + " recordings");
        }

    }

    /** Gets number of prompts in the set
     *  @return The number of prompts
     */    
    public int getPromptCount() {        
        return promptCount;
    }
    
    /** Gets the folder path for the prompt set
     *  @return The folder path containing the prompt set
     */
    public File getPromptFolderPath() {
        return promptFolderPath;
    }
        

    
    /** Fills an array of Prompts with basename and prompt text data from a list of prompt text files, where each
     *  file contains a single prompt sentence.
     *  @param promptFile The file path to the folder of prompt text files
     *  @param promptCount The number of prompts in the folder
     *  @return An array of Prompt objects
     */
    private Prompt[] getPromptData() throws FileNotFoundException, IOException {
                
        // Create a list of files that are in the prompt text directory
        File[] promptFileList = this.promptFolderPath.listFiles();
        Arrays.sort(promptFileList);
        
        Prompt[] promptArray = new Prompt[this.promptCount];
        String basename = "";
        String promptText = "";
        String promptTranscriptionText = "" ;
        int fileCount = 0;
        
        // TESTCODE
        Test.output("|PromptSet.getPromptData()| Total files = " + promptFileList.length);
        Test.output("|PromptSet.getPromptData()| Length of prompt array = " + promptArray.length);
        
        // Go through each file
        for (int index = 0; index < promptFileList.length; index++) {
                                   
            File promptFile = promptFileList[index];
            
            // Only consider files (no directories) that are not hidden
            if ((promptFile.isFile()) && !promptFile.isHidden()) {
            
                
                // TESTCODE
                Test.output("***|PromptSet.getPromptData| Prompt file with path: " + promptFile.getPath());

                // Extract the basename from the prompt filename
                basename = getBasename(promptFile);
                
                // Fill the array with a prompt having the above basename
                // Somehow creating this temp prompt avoids a null pointer exception
                Prompt tempPrompt = new Prompt(basename, adminWindow.getRecFolderPath(), adminWindow.getSynthFolderPath());
                promptArray[fileCount] = tempPrompt;                
                
                Test.output("***|PromptSet.getPromptData| Basename: " + basename);  // TESTCODE
                
                // Read in prompt text          
                promptText = getContents(promptFile).trim();
                Test.output("|PromptSet.getPromptData| Prompt text: " + fileCount + ": " + promptText); // TESTCODE
                promptArray[fileCount].setPromptText(promptText);
                
                // Read in prompt transcription text   
                File promptTranscriptionFile = new File(adminWindow.getTranscriptionFolderPath() +  File.separator + basename + ".tr");
                if (promptTranscriptionFile.exists())
                {
	                promptTranscriptionText = getContents(promptTranscriptionFile).trim();
	                Test.output("|PromptSet.getPromptData| Prompt Transcription text: " + fileCount + ": " + promptTranscriptionText); // TESTCODE
	                promptArray[fileCount].setPromptTranscriptionText(promptTranscriptionText);
                }
                
                fileCount++;

            }
            
        }
        
        // TESTCODE Defensive coding below. Both counts should be equal since they used the same
        //          approach for determining their numbers.
        if (promptCount != fileCount) {
            Test.output("|PromptSet.getPromptData| Error: Prompt count and file count are not equal.");            
        }
        
        return promptArray;
    }
    
    
    
    
    // ______________________________________________________________________
    // Class methods

    /** Extracts the basename from a prompt filename
     *  @param promptFile The file path for the prompt file
     *  @return The basename for the file (e.g., bundesliga20003)
     */
    static public String getBasename(File promptFile) {
        
        String filename = promptFile.getName();     // Get name of prompt text file (e.g., bundesliga20003.txt)
        int start = 0;
        int end = filename.length() - 4;            // Don't include file extension
        String basename = filename.substring(start, end);
        
        return basename;    // e.g., bundesliga20003
        
    }
    
    /** Reads in the contents of a file as a string. Taken from http://www.javapractices.com/Topic42.cjp.
     *  @param aFile The file path for the input file
     *  @return The file's contents as a String object
     */
    static public String getContents(File aFile) {
        //...checks on aFile are elided
        StringBuilder contents = new StringBuilder();

        //declared here only to make visible to finally clause
        BufferedReader input = null;
        try {
          //use buffering, reading one line at a time
          //FileReader always assumes default encoding is OK!
          input = new BufferedReader( new InputStreamReader(new FileInputStream(aFile), "UTF-8") );
          String line = null; //not declared within while loop
          /*
          * readLine is a bit quirky :
          * it returns the content of a line MINUS the newline.
          * it returns null only for the END of the stream.
          * it returns an empty String if two newlines appear in a row.
          */
          while (( line = input.readLine()) != null){
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
          }
        }
        catch (FileNotFoundException ex) {
          ex.printStackTrace();
        }
        catch (IOException ex){
          ex.printStackTrace();
        }
        finally {
          try {
            if (input!= null) {
              // Flush and close both "input" and its underlying FileReader
              input.close();
            }
          }
          catch (IOException ex) {
            ex.printStackTrace();
          }
        }
        return contents.toString();
    } 
       
    /** Counts the number of prompts in a set
     *  @param promptFolderPath The file path for the prompt file
     *  @return The number of prompts in the prompt file
     */
    private int countPrompts() throws IOException {

        int promptCount = 0;  // Total number of prompts in prompt file        
        
        // TESTCODE
        String path = this.promptFolderPath.getPath(); // + File.separator; // doesn't seem to matter
        Test.output("Counting prompts in the following path: " + path);
        
        // Create a list of files in the prompt text directory
        File[] promptFiles = this.promptFolderPath.listFiles();
        if (promptFiles == null) throw new IOException("No such directory: "+path);
                
        // Go through each file in the folder
        for (int index = 0; index < promptFiles.length; index++) {
                                   
            File promptFile = promptFiles[index];
            
            // Only consider files (no directories) that are not hidden
            // Assumption: All files in this directory contain prompt text
            if ((promptFile.isFile()) && !promptFile.isHidden()) {
                promptCount++;                
            }
            
        }
        
        // TESTCODE
        Test.output("|PromptSet.countPrompts()| Total prompts = " + promptCount);
        
        return promptCount;

    }  // End of countPrompts
    
    
}

