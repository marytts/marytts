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
package marytts.util.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * A collection of public static utility methods, doing
 * file operations.
 * @author schroed
 *
 */
public class FileUtils
{

    /*
     * Close a socket and closeables.
     * Use this in a finally clause.
     * Exists because Sockets are only closeable in jdk 1.7.
     * @param socket to close.
     * @param closeables to close.
     */
    public static void close(Socket socket, Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    Logger.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Closeable.", ex);
                }
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ex) {
                Logger.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Socket.", ex);
            }
        }
    }

    /**
     * Close closeables. Use this in a finally clause.
     * @param Closeables closeables to close.
     */
    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ex) {
                    Logger.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Closeable.", ex);
                }
            }
        }
    }

    /**
     * List the basenames of all files in directory that end in suffix, without that suffix.
     * For example, if suffix is ".wav", return the names of all .wav files in the
     * directory, but without the .wav extension. The file names
     * are sorted in alphabetical order, according to java's string search.
     * @param directory
     * @param suffix
     * @return
     */
    public static String[] listBasenames(File directory, String suffix)
    {
        final String theSuffix = suffix;
        String[] filenames = directory.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(theSuffix);
            }
        });
        
        /* Sort the file names alphabetically */
        Arrays.sort(filenames);
        
        for (int i = 0; i < filenames.length; i++) {
            filenames[i] = filenames[i].substring(0, filenames[i].length()-suffix.length());
        }
        return filenames;
    }

    /**
     * Read a file into a string, using the given encoding, and return that string.
     * @param file
     * @param encoding
     * @return
     */
    public static String getFileAsString(File file, String encoding) throws IOException
    {
        return getStreamAsString(new FileInputStream(file), encoding);
    }

    public static String getStreamAsString(InputStream inputStream, String encoding) throws IOException
    {
        return getReaderAsString(new InputStreamReader(inputStream, encoding));
    }
    
    public static String getReaderAsString(Reader reader) throws IOException
    {
        StringWriter sw = new StringWriter();
        BufferedReader in = new BufferedReader(reader);
        char[] buf = new char[8192];
        int n;
        while ((n=in.read(buf))> 0) {
            sw.write(buf, 0, n);
        }
        return sw.toString();
        
    }
    
    public static byte[] getFileAsBytes(String filename) throws IOException 
    {
        return getFileAsBytes(new File(filename));
    }
    
    public static byte[] getFileAsBytes(File file) throws IOException 
    {
        InputStream is = new FileInputStream(file);

        long length = file.length();

        if (length > Integer.MAX_VALUE) 
            return null;

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while ( (offset < bytes.length) &&
                ( (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) ) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not read file " + file.getName());
        }

        is.close();
        
        return bytes;
    }

    
    
    public static void writeToTextFile(double[] array, String textFile)
    {
        FileWriter outFile = null;
        try {
            outFile = new FileWriter(textFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (outFile!=null)
        {
            PrintWriter out = new PrintWriter(outFile);

            for (int i=0; i<array.length; i++)
                out.println(String.valueOf(array[i]));
            out.close();
        }
        else
            System.out.println("Error! Cannot create file: " + textFile);
    }

    public static void writeToBinaryFile(int[] pitchMarks, String filename) throws IOException 
    {
        DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(filename))); 
        
        d.writeInt(pitchMarks.length);
        
        for (int i=0; i<pitchMarks.length; i++)
            d.writeInt(pitchMarks[i]);
        
        d.close();
    }
    
    public static int[] readFromBinaryFile(String filename) throws IOException 
    {
        DataInputStream d = new DataInputStream(new FileInputStream(new File(filename))); 
        
        int[] x = null;
        int len = d.readInt();
        
        if (len>0)
        {
            x = new int[len];
            
            for (int i=0; i<len; i++)
                x[i] = d.readInt();
        }
        
        d.close();
        
        return x;
    }
    
    public static boolean exists(String file)
    {
        boolean bRet = false;

        if (file!=null)
        {
            File f = new File(file);
            if (f.exists())
                bRet =  true;
        }

        return bRet;
    }
    
    //Checks for the existence of file and deletes if existing
    public static void delete(String file, boolean bDisplayInfo)
    {
        boolean bRet = false;
        File f = new File(file);
        if (f.exists())
            bRet = f.delete();
        
        if (!bRet)
            System.out.println("Unable to delete file: " + file);
        else
        {
            if (bDisplayInfo)
                System.out.println("Deleted: " + file);
        }
    }
    
    //Silent version
    public static void delete(String file)
    {
        if (exists(file))
            delete(file, false);
    }
    
    public static void delete(String[] files, boolean bDisplayInfo)
    {
        for (int i=0; i<files.length; i++)
            delete(files[i], bDisplayInfo);
    }
    
    //Silnet version
    public static void delete(String[] files)
    {
        delete(files, false);
    }
    
    public static void copy(String sourceFile, String destinationFile) throws IOException {
        File fromFile = new File(sourceFile);
        File toFile = new File(destinationFile);

        if (!fromFile.exists())
            throw new IOException("FileCopy: " + "no such source file: " + sourceFile);
        if (!fromFile.isFile())
            throw new IOException("FileCopy: " + "can't copy directory: " + sourceFile);
        if (!fromFile.canRead())
            throw new IOException("FileCopy: " + "source file is unreadable: " + sourceFile);

        if (toFile.isDirectory())
            toFile = new File(toFile, fromFile.getName());

        if (toFile.exists()) 
        {
            if (!toFile.canWrite())
                throw new IOException("FileCopy: " + "destination file cannot be written: " + destinationFile);
        }

        String parent = toFile.getParent();
        if (parent == null)
            parent = System.getProperty("user.dir");
        File dir = new File(parent);
        if (!dir.exists())
            throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
        if (dir.isFile())
            throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
        if (!dir.canWrite())
            throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);

        FileInputStream from = null;
        FileOutputStream to = null;
        try 
        {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = from.read(buffer)) != -1)
                to.write(buffer, 0, bytesRead); // write
        } 
        finally 
        {
            if (from != null)
            {
                try {
                    from.close();
                } catch (IOException e) {
                    ;
                }
            }
            
            if (to != null)
            {
                try {
                    to.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
    }

    public static void createDirectory(String trainingBaseFolder) {
        File f = new File(trainingBaseFolder);
        if (!f.exists())
            f.mkdirs();
    }

    public static boolean isDirectory(String dirName) {
        File f = new File(dirName);
        
        if (f.isDirectory())
            return true;
        else
            return false;
    }
    
    public static void rename(String existingFile, String newFilename)
    {
        if (exists(existingFile))
        {
            File oldFile = new File(existingFile); 
            oldFile.renameTo(new File(newFilename));
        }
    }
    
    //Gets filenames only without paths!
    public static String[] getFileNameList(String directory, String extension)
    {
        return  getFileNameList(directory, extension, true);
    }
    
    //Gets filenames only without paths!
    public static String[] getFileNameList(String directory, String extension, boolean recurse)
    {
        File[] files = listFilesAsArray(new File(directory), new FileFilter(extension), recurse);
        String[] fileList = null;
        if (files!=null && files.length>0)
        {
            fileList = new String[files.length];
            for (int i=0; i<files.length; i++)
                fileList[i] = files[i].getName();
        }
        
        return fileList;
    }
    
    //Gets filenames with full path
    public static String[] getFileList(String directory, String extension)
    {
        return  getFileList(directory, extension, true);
    }
    
    //Gets filenames with full path
    public static String[] getFileList(String directory, String extension, boolean recurse)
    {
        File[] files = listFilesAsArray(new File(directory), new FileFilter(extension), recurse);
        String[] fileList = null;
        if (files!=null && files.length>0)
        {
            fileList = new String[files.length];
            for (int i=0; i<files.length; i++)
                fileList[i] = files[i].getAbsolutePath();
        }
        
        return fileList;
    }
    
    public static File[] listFilesAsArray(File directory, FilenameFilter filter, boolean recurse)
    {
        Collection<File> files = listFiles(directory, filter, recurse);
  
        File[] arr = new File[files.size()];
        return files.toArray(arr);
    }

    public static Collection<File> listFiles(File directory, FilenameFilter filter, boolean recurse)
    {
        // List of files / directories
        Vector<File> files = new Vector<File>();
        
        // Get files / directories in the directory
        File[] entries = directory.listFiles();
        
        // Go over entries
        for (File entry : entries)
        {
            // If there is no filter or the filter accepts the 
            // file / directory, add it to the list
            if (filter == null || filter.accept(directory, entry.getName()))
            {
                files.add(entry);
            }
            
            // If the file is a directory and the recurse flag
            // is set, recurse into the directory
            if (recurse && entry.isDirectory())
            {
                files.addAll(listFiles(entry, filter, recurse));
            }
        }
        
        // Return collection of files
        return files;       
    }
}

