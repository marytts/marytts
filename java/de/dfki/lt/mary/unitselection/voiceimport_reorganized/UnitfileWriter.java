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
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;

import de.dfki.lt.mary.util.FileUtils;

public class UnitfileWriter implements VoiceImportComponent
{
    protected File maryDir;
    protected File unitFile;
    protected int samplingRate;
    protected String pauseSymbol;

    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    
    public UnitfileWriter( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;

        maryDir = new File( db.maryDirName() );
        if (!maryDir.exists()) {
            maryDir.mkdir();
            System.out.println("Created the output directory [" + db.maryDirName() + "] to store the unit file." );
        }
        unitFile = new File( db.unitFileName() );
        samplingRate = Integer.getInteger("unit.file.samplingrate", 16000).intValue(); // TODO: make a better passing of the sampling rate
        pauseSymbol = System.getProperty("pause.symbol", "pau");

    }
    
    public boolean compute() throws IOException
    {
        System.out.println("Unitfile writer started.");
        System.out.println("Verifying that unit feature and label files are perfectly aligned...");
        LabelFeatureAligner aligner = new LabelFeatureAligner( db, bnl );
        if (!aligner.compute()) throw new IllegalStateException("Database is NOT perfectly aligned. Cannot create unit file.");
        System.out.println("OK, alignment verified.");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(unitFile)));
        long posNumUnits = new MaryHeader(MaryHeader.UNITS).write(out);
        out.writeInt(-1); // number of units; needs to be corrected later.
        out.writeInt(samplingRate);
        int index = 0; // the unique index number of units in the unit file
        long start = 0; // time, given as sample position with samplingRate
        
        // Loop over all utterances
        for (int i=0; i<bnl.getLength(); i++) {
            // Utterance start marker: "null" unit
            out.writeLong(start); out.writeInt(-1);
            index++;
            BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitLabDirName() + bnl.getName(i) + db.unitLabExt() )), "UTF-8"));
            String line;
            // Skip label file header
            while ((line = labels.readLine()) != null) {
                if (line.startsWith("#")) break; // line starting with "#" marks end of header
            }
            // Now read the actual units
            while ((line = labels.readLine()) != null) {
                line = line.trim();
                if (line.equals("")) continue; // ignore empty lines
                StringTokenizer st = new StringTokenizer(line);
                String startTime = st.nextToken();
                double endTime = Double.valueOf(startTime).doubleValue();
                long end = (long) Math.round(endTime * samplingRate);
                int duration = (int) (end - start);
                out.writeLong(start); out.writeInt(duration);
                start += duration; index++;
            }
            // Utterance end marker: "null" unit
            out.writeLong(start); out.writeInt(-1);
            index++;
            labels.close();
            System.out.println( "    " + bnl.getName(i) + " (" + index + ")" );
        }
        out.close();
        // Now index is the number of units. Set this in the file:
        RandomAccessFile raf = new RandomAccessFile(unitFile, "rw");
        raf.seek(posNumUnits);
        raf.writeInt(index);
        raf.close();
        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        new UnitfileWriter( null, null ).compute();
    }

}
