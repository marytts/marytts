/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.adaptation.codebook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.signalproc.adaptation.prosody.PitchStatistics;
import marytts.signalproc.adaptation.prosody.PitchStatisticsCollection;
import marytts.util.io.FileUtils;
import marytts.util.io.LEDataInputStream;
import marytts.util.io.LEDataOutputStream;
import marytts.util.io.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 */
public class WeightedCodebookFile {
    int status;
    public static int NOT_OPENED = -1;
    public static int OPEN_FOR_READ = 0;
    public static int OPEN_FOR_WRITE = 1;
    
    public MaryRandomAccessFile stream;
    public String currentFile;
    
    public static final String DEFAULT_EXTENSION = ".wcf";
    
    public WeightedCodebookFile()
    {
        this("");
    }
    
    public WeightedCodebookFile(String codebookFile)
    {
        this(codebookFile, NOT_OPENED);
    }
    
    public WeightedCodebookFile(String codebookFile, int desiredStatus)
    {
        init(codebookFile, desiredStatus);
    }
    
    private void init(String codebookFile, int desiredStatus)
    {
        status = NOT_OPENED;
        stream = null;
        currentFile = "";
        
        if (desiredStatus==OPEN_FOR_READ)
        {
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(codebookFile, "r");
                currentFile = codebookFile;
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        else if (desiredStatus==OPEN_FOR_WRITE)
        {
            FileUtils.delete(codebookFile);
            
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(codebookFile, "rw");
                currentFile = codebookFile;
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void close()
    {
        if (status!=NOT_OPENED)
        {
            if (stream!=null)
            {       
                try {
                    stream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            stream = null;
            status = NOT_OPENED;
        }
    }
    
    public WeightedCodebookFileHeader readCodebookHeader(String codebookFile, boolean bCloseAfterReading)
    {
        init(codebookFile, OPEN_FOR_READ);
        return readCodebookHeader();
    }

    public WeightedCodebookFileHeader readCodebookHeader()
    {
        try {
            return readCodebookHeader(stream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static WeightedCodebookFileHeader readCodebookHeader(MaryRandomAccessFile ler) throws IOException
    {
        WeightedCodebookFileHeader header = new WeightedCodebookFileHeader();
        
        header.read(ler);
        
        return header;
    }
    
    public MaryRandomAccessFile writeCodebookHeader(String codebookFile, WeightedCodebookFileHeader header)
    {   
        init(codebookFile, OPEN_FOR_WRITE);
        return writeCodebookHeader(header);
    }
    
    public MaryRandomAccessFile writeCodebookHeader(WeightedCodebookFileHeader header)
    {
        try {
             writeCodebookHeader(stream, header);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return stream;
    }
    
    public void writeCodebookHeader(MaryRandomAccessFile ler, WeightedCodebookFileHeader header) throws IOException
    {
        header.write(ler);
    }
    
    public WeightedCodebook readCodebookFile() throws IOException
    {
        return readCodebookFile(currentFile);
    }
    
    //Read whole codebook from a file into memory
    public WeightedCodebook readCodebookFile(String codebookFile) throws IOException
    {
        WeightedCodebook codebook = null;
        
        if (FileUtils.exists(codebookFile))
        {
            if (status!=OPEN_FOR_READ)
            {
                if (status!=NOT_OPENED)
                    close();

                init(codebookFile, OPEN_FOR_READ);
            }

            if (status==OPEN_FOR_READ)
            {
                codebook = new WeightedCodebook();

                codebook.header = readCodebookHeader();
                
                readCodebookFileExcludingHeader(codebook);
            }
        }

        return codebook;
    }
    
    public void readCodebookFileExcludingHeader(WeightedCodebook codebook)
    {
        codebook.allocate();

        System.out.println("Reading codebook file: "+ currentFile + "...");
        
        int i;
        for (i=0; i<codebook.header.totalLsfEntries; i++)
            codebook.lsfEntries[i] = readLsfEntry(codebook.header.lsfParams.lpOrder);
        
        close();
        
        System.out.println("Reading completed...");
    }
    
    public void WriteCodebookFile(String codebookFile, WeightedCodebook codebook)
    {
        if (status!=OPEN_FOR_WRITE)
        {
            if (status!=NOT_OPENED)
                close();

            init(codebookFile, OPEN_FOR_WRITE);
        }

        codebook.header.totalLsfEntries = codebook.lsfEntries.length;
        
        writeCodebookHeader(codebookFile, codebook.header);

        int i;
        for (i=0; i<codebook.header.totalLsfEntries; i++)
            writeLsfEntry(codebook.lsfEntries[i]);
        
        close();
    }
    
    //Append a new lsf entry to a codebook file opened with write permission
    public void writeLsfEntry(WeightedCodebookLsfEntry w)
    {
        if (status!=OPEN_FOR_WRITE)
        {
            if (status!=NOT_OPENED)
                close();
               
            init(currentFile, OPEN_FOR_WRITE);
        }
        
        if (status==OPEN_FOR_WRITE)
        {
            w.write(stream);
            incrementTotalLsfEntries();
        }
    }
    
    //Read an lsf entry from a codebook file opened with read permission
    public WeightedCodebookLsfEntry readLsfEntry(int lpOrder)
    {
        WeightedCodebookLsfEntry w = new WeightedCodebookLsfEntry();
        
        if (status!=OPEN_FOR_READ)
        {
            if (status!=NOT_OPENED)
                close();
               
            init(currentFile, OPEN_FOR_READ);
        }
        
        if (status==OPEN_FOR_READ)
            w.read(stream, lpOrder);
        
        return w;
    }
    //
    
    public void incrementTotalLsfEntries()
    {
        if (status==OPEN_FOR_WRITE)
        {
            try {
                long currentPos = stream.getFilePointer();
                stream.seek(0);
                int totalLsfEntries = stream.readInt();
                totalLsfEntries++;
                stream.seek(0);
                stream.writeInt(totalLsfEntries);
                System.out.println("Wrote LSF entry " + String.valueOf(totalLsfEntries));
                stream.seek(currentPos);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
