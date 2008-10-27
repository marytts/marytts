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

package marytts.signalproc.adaptation.prosody;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;


/**
 * @author oytun.turk
 *
 */
public class PitchMappingFile {
    int status;
    public static int NOT_OPENED = -1;
    public static int OPEN_FOR_READ = 0;
    public static int OPEN_FOR_WRITE = 1;
    
    public MaryRandomAccessFile stream;
    public String currentFile;
    
    public static String DEFAULT_EXTENSION = ".pmf";
    
    public PitchMappingFile()
    {
        this("");
    }
    
    public PitchMappingFile(String pitchMappingFile)
    {
        this(pitchMappingFile, NOT_OPENED);
    }
    
    public PitchMappingFile(String pitchMappingFile, int desiredStatus)
    {
        init(pitchMappingFile, desiredStatus);
    }
    
    private void init(String pitchMappingFile, int desiredStatus)
    {
        status = NOT_OPENED;
        stream = null;
        currentFile = "";
        
        if (desiredStatus==OPEN_FOR_READ)
        {
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(pitchMappingFile, "r");
                currentFile = pitchMappingFile;
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        else if (desiredStatus==OPEN_FOR_WRITE)
        {
            FileUtils.delete(pitchMappingFile);
            
            status = desiredStatus;
            try {
                stream = new MaryRandomAccessFile(pitchMappingFile, "rw");
                currentFile = pitchMappingFile;
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
    
    public PitchMappingFileHeader readPitchMappingHeader(String pitchMappingFile, boolean bCloseAfterReading)
    {
        init(pitchMappingFile, OPEN_FOR_READ);
        return readPitchMappingHeader();
    }

    public PitchMappingFileHeader readPitchMappingHeader()
    {
        try {
            return readPitchMappingHeader(stream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static PitchMappingFileHeader readPitchMappingHeader(MaryRandomAccessFile ler) throws IOException
    {
        PitchMappingFileHeader header = new PitchMappingFileHeader();
        
        header.read(ler);
        
        return header;
    }
    
    public MaryRandomAccessFile writePitchMappingHeader(String pitchMappingFile, PitchMappingFileHeader header)
    {   
        init(pitchMappingFile, OPEN_FOR_WRITE);
        return writePitchMappingHeader(header);
    }
    
    public MaryRandomAccessFile writePitchMappingHeader(PitchMappingFileHeader header)
    {
        try {
            writePitchMappingHeader(stream, header);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return stream;
    }
    
    public void writePitchMappingHeader(MaryRandomAccessFile ler, PitchMappingFileHeader header) throws IOException
    {
        header.write(ler);
    }
    
    public PitchMapping readPitchMappingFile() throws IOException
    {
        return readPitchMappingFile(currentFile);
    }
    
    //Read whole codebook from a file into memory
    public PitchMapping readPitchMappingFile(String pitchMappingFile) throws IOException
    {
        PitchMapping pitchMapping = null;
        
        if (FileUtils.exists(pitchMappingFile))
        {
            if (status!=OPEN_FOR_READ)
            {
                if (status!=NOT_OPENED)
                    close();

                init(pitchMappingFile, OPEN_FOR_READ);
            }

            if (status==OPEN_FOR_READ)
            {
                pitchMapping = new PitchMapping();

                pitchMapping.header = readPitchMappingHeader();
                
                readPitchMappingFileExcludingHeader(pitchMapping);
            }
        }

        return pitchMapping;
    }
    
    public void readPitchMappingFileExcludingHeader(PitchMapping pitchMapping)
    {
        pitchMapping.allocate();

        System.out.println("Reading pitch mapping file: "+ currentFile + "...");
        
        int i;

        pitchMapping.f0StatisticsCollection = new PitchStatisticsCollection(pitchMapping.header.totalF0StatisticsEntries);
        for (i=0; i<pitchMapping.header.totalF0StatisticsEntries; i++)
            pitchMapping.f0StatisticsCollection.entries[i] = readF0StatisticsEntry();
        
        pitchMapping.setF0StatisticsMapping();
        
        close();
        
        System.out.println("Reading completed...");
    }
    
    public void WriteCodebookFile(String pitchMappingFile, PitchMapping pitchMapping)
    {
        if (status!=OPEN_FOR_WRITE)
        {
            if (status!=NOT_OPENED)
                close();

            init(pitchMappingFile, OPEN_FOR_WRITE);
        }

        pitchMapping.header.totalF0StatisticsEntries = pitchMapping.f0StatisticsCollection.entries.length;
        writePitchMappingHeader(pitchMappingFile, pitchMapping.header);

        int i;
        
        for (i=0; i<pitchMapping.header.totalF0StatisticsEntries; i++)
            writeF0StatisticsEntry(pitchMapping.f0StatisticsCollection.entries[i]);
        
        close();
    }
    
    //Append a new lsf entry to a codebook file opened with write permission
    public void writeF0StatisticsEntry(PitchStatistics p)
    {        
        if (status==OPEN_FOR_WRITE)
        {
            p.write(stream);
            incrementTotalF0StatisticsEntries();
        }
    }
    
    //Read an F0StatisticsEntry entry from a pitchMappingFile file opened with read permission
    public PitchStatistics readF0StatisticsEntry()
    {
        PitchStatistics p = new PitchStatistics();
        
        if (status==OPEN_FOR_READ)
            p.read(stream);
        
        return p;
    }
    
    public void incrementTotalF0StatisticsEntries()
    {
        if (status==OPEN_FOR_WRITE)
        {
            try {
                long currentPos = stream.getFilePointer();
                stream.seek(0);
                int totalF0StatisticsEntries = stream.readInt();
                totalF0StatisticsEntries++;
                stream.seek(0);
                stream.writeInt(totalF0StatisticsEntries);
                System.out.println("Wrote f0 statistics entry " + String.valueOf(totalF0StatisticsEntries));
                stream.seek(currentPos);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
