/**
 * Portions Copyright 2003-2004 Sun Microsystems, Inc.
 * Portions Copyright 1999-2003 Language Technologies Institute,
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.AudioInputStream;


/**
 * Performs the generation of STS files in FestVox to FreeTTS
 * conversion.
 *
 * <p>
 * This program is a port from flite/tools/find_sts_main.c
 * </p>
 *
 * <p>
 * Note: 
 * The a/b diff result is slightly different than the C version due to
 * Intel floating-point math.
 * </p>
 */
public strictfp class FindSTS {
    static float lpc_min;
    static float lpc_max;
    static float lpc_range;

    /**
     * Gets the lpc parameters from lpc/lpc.params
     */
    static private void getLpcParams() throws IOException {
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream("lpc/lpc.params")));
        
        String line = reader.readLine();
        while (line != null) {
            if (line.startsWith("LPC_MIN=")) {
                lpc_min = Float.parseFloat(line.substring(8));
            } else if (line.startsWith("LPC_MAX=")) {
                lpc_max = Float.parseFloat(line.substring(8));
            } else if (line.startsWith("LPC_RANGE=")) {
                lpc_range = Float.parseFloat(line.substring(10));
            }
            line = reader.readLine();
        }
        reader.close();
        System.out.println("LPC_MIN=" + lpc_min);
        System.out.println("LPC_MAX=" + lpc_max);
        System.out.println("LPC_RANGE=" + lpc_range);
    }
    
    /**
     * Generate an sts file from lpc and wav files.
     *
     *     args[0..n] = filenames without paths or extensions
     *                  (e.g., "arctic_a0001")
     */
    public static void main(String[] args) {
        try {
            getLpcParams();
            for (int i = 0; i < args.length; i++) {
                System.out.println(args[i] + " STS");
                FileInputStream lpcFile = new FileInputStream(
                    "lpc/" + args[i] + ".lpc");
                FileInputStream waveFile = new FileInputStream(
                    "wav/" + args[i] + ".wav");
                FileOutputStream stsFile = new FileOutputStream(
                    "sts/" + args[i] + ".sts");
                
                // Read input
                LPC lpc = new LPC(new DataInputStream(lpcFile));
                Wave wave = new Wave(new DataInputStream(waveFile));
                lpcFile.close();
                waveFile.close();

                // Generate sts data
                STS[] stsData = findSTS(wave, lpc, lpc_min, lpc_range);

                // Verify STS data for sanity
                if (false) {
                    Wave reconstructedWave = new
                        Wave(wave.getSampleRate(), stsData, lpc,
                             lpc_min, lpc_range);
                    wave.compare(reconstructedWave);
                }
                
                // Save output
                OutputStreamWriter stsWriter = new OutputStreamWriter(stsFile);
                saveSTS(stsData, lpc, wave, stsWriter, lpc_min, lpc_range);

                stsWriter.close();
                stsFile.close();
            }
        } catch (FileNotFoundException ioe) {
            throw new Error("Error while running FindSTS" + ioe.getMessage());
        } catch (IOException ioe) {
            throw new Error("IO error while finding sts" + ioe.getMessage());
        }
    }

    /**
     * Find the sts data.
     *
     * @param wave the data from the wave file
     * @param lpc the data from the lpc file
     * @param lpc_min the minimum lpc value
     * @param lpc_range the range of the lpc values
     *
     * @return an <code>STS</code> array containing the data
     */
    private static STS[] findSTS(Wave wave, LPC lpc, float lpc_min,
            float lpc_range) {
        int size;
        int start = 0;
        int end;

        STS[] stsData = new STS[lpc.getNumFrames()];

        // read wave data into a special array.
        short[] waveData =
            new short[wave.getNumSamples() + lpc.getNumChannels()];
        System.arraycopy(wave.getSamples(), 0, waveData,
                    lpc.getNumChannels(), wave.getNumSamples());

        for (int i = 0; i < lpc.getNumFrames(); i++) {
            double[] resd;
            int[] frame;
            short[] residual;

            end = (int) ((float) wave.getSampleRate() * lpc.getTime(i));
            size = end - start;
            if (size <= 0) {
                System.out.println("frame size at "
                        + Float.toString(lpc.getTime(i)) + " is "
                        + Integer.toString(size) + ".");
            }

            residual = generateResiduals(waveData,
                    start + lpc.getNumChannels(), lpc.getFrame(i),
                    lpc.getNumChannels(), size);

            frame = new int[lpc.getNumChannels() - 1];
            for (int j = 1; j < lpc.getNumChannels(); j++) {
                frame[j - 1] = (int)
                    ((((lpc.getFrameEntry(i, j) - lpc_min) / lpc_range))
                     * (float) 65535.0);
            }

            stsData[i] = new STS(frame, size, residual);
            start = end;
        }

        return stsData;
    }

    /**
     * Generate the residuals for this sts
     *
     * @param wave specially formatted wave data
     * @param start offset into the wave data
     * @param frame frame data from the lpc
     * @param order typically the number of lpc channels
     * @param size size of the residual
     *
     * @return sts residuals
     */
    private static short[] generateResiduals(short[] wave, int start,
            float[] frame, int order, int size) {
        double r;
        short[] residual = new short[size];
        for (int i = 0; i < order; i++) {
            r = wave[start + i];
            for (int j = 1; j < order; j++) {
                r -= frame[j] * ((double) wave[start + (i - j)]);
            }
            residual[i] = Utility.shortToUlaw((short) r);
        }
        for (int i = order; i < size; i++) {
            r = wave[start + i];
            for (int j = 1; j < order; j++) {
                r -= frame[j] * ((double) wave[start + (i - j)]);
            } 
            residual[i] = Utility.shortToUlaw((short) r);
        }
        return residual;
    }

    /**
     * Save the sts data
     *
     * @param stsData generated sts data
     * @param lpc data loaded from the lpc file
     * @param wave data loaded from the wave file
     * @param osw the OutputStreamWriter to write the sts data to
     * @param lpc_min minimum lpc value
     * @param lpc_range range of lpc values
     *
     */
    private static void saveSTS(STS[] stsData, LPC lpc, Wave wave,
            OutputStreamWriter osw, float lpc_min, float lpc_range) {
        try {
            osw.write(Integer.toString(lpc.getNumFrames())
                      + " " + Integer.toString(lpc.getNumChannels() - 1)
                      + " " + Integer.toString(wave.getSampleRate())
                      + " " + Float.toString(lpc_min)
                      + " " + Float.toString(lpc_range) + "\n");
            for (int m=0, i=0; i < lpc.getNumFrames(); i++) {
                /* time lpc lpc lpc lpc ...
                 */
                osw.write(Float.toString(lpc.getTime(i)) + "\n");
                for (int j = 1; j < lpc.getNumChannels(); j++) {
                    osw.write(
                        Integer.toString(stsData[i].getFrameEntry(j - 1))
                        + " ");
                }
                osw.write("\n");

                /* numResid resid resid resid
                 */
                osw.write(Integer.toString(stsData[i].getNumSamples()));
                for (int j = 0; j < stsData[i].getNumSamples(); j++) {
                    osw.write(
                        " "
                        + Integer.toString(stsData[i].getResidual(j)));
                }
                osw.write("\n");
            }
        } catch (IOException ioe) {
            throw new Error("IO error while writing sts." + ioe.getMessage());
        }
    }
}


/**
 * The lpc data
 *
 */
class LPC {
    private int numFrames;
    private int numChannels;
    float[] times;
    float[][] frames;

    /** Create lpc data from an input stream
     *
     * @param dis DataInputStream to read the lpc in from
     *
     */
    public LPC(DataInputStream dis) {
        try {
            if (!Utility.readWord(dis).equals("EST_File") ||
                    !Utility.readWord(dis).equals("Track")) {
                throw new Error("Lpc file not EST Track file");
            }

            boolean isBinary = false;
            boolean isBigEndian = false;

            // Read Header
            String token = Utility.readWord(dis);
            while (!token.equals("EST_Header_End")) {
                if (token.equals("DataType")) {
                    if (Utility.readWord(dis).equals("binary")) {
                        isBinary = true;
                    } else {
                        isBinary = false;
                    }
                } else if (token.equals("ByteOrder")) {
                    if (Utility.readWord(dis).equals("10")) {
                        isBigEndian = true;
                    } else {
                        isBigEndian = false;
                    }
                } else if (token.equals("NumFrames")) {
                    numFrames = Integer.parseInt(Utility.readWord(dis));
                } else if (token.equals("NumChannels")) {
                    numChannels = Integer.parseInt(Utility.readWord(dis));
                }
                // Ignore all other content in header

                token = Utility.readWord(dis);
            }

            times = new float[numFrames];
            frames = new float[numFrames][numChannels];

            // read data section
            if (isBinary) {
                loadBinaryData(dis, isBigEndian);
            } else {
                loadTextData(dis);
            }
        }
        catch (IOException ioe) {
            throw new Error("IO error while parsing lpc" + ioe.getMessage());
        }
    }

    /**
     * load the data section of the lpc file as ascii text
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
    private void loadTextData(DataInputStream dis) throws IOException {
        for (int f=0; f < numFrames; f++) {
            times[f] = Float.parseFloat(Utility.readWord(dis));
            Utility.readWord(dis);  // can be only 1
            for (int c=0; c < numChannels; c++) {
                    frames[f][c] = Float.parseFloat(Utility.readWord(dis));
            }
        }
    }

    /**
     * load the data section of the lpc file as ascii text
     *
     * @param dis DataInputStream to read from
     * @param isBigEndian whether or not the data in the file is in
     *          big endian byte order
     *
     * @throws IOException on ill-formatted input
     */
    private void loadBinaryData(DataInputStream dis, boolean isBigEndian)
            throws IOException {
        for (int f=0; f < numFrames; f++) {
            times[f] = Utility.readFloat(dis, isBigEndian);

            // Ignore the 'breaks' field
            Utility.readFloat(dis, isBigEndian);

            for (int c=0; c < numChannels; c++) {
                frames[f][c] = Utility.readFloat(dis, isBigEndian);
            }
        }
    }

    /**
     * Get the number of frames in this lpc
     *
     * @return number of frames in this lpc
     */
    public int getNumFrames() {
        return numFrames;
    }

    /**
     * Get the number of channels in this lpc
     *
     * @return number of channels in this lpc
     */
    public int getNumChannels() {
        return numChannels;
    }

    /**
     * Get the times associated with this lpc
     *
     * @return an array of times associated with this lpc
     */
    public float[] getTimes() {
        return times;
    }

    /**
     * Get an individual time associated with this lpc
     *
     * @param index index of time to get
     *
     * @return time value at given index
     */
    public float getTime(int index) {
        return times[index];
    }

    /**
     * Get an individual frame
     *
     * @param i index of frame
     *
     * @return the frame
     */
    public float[] getFrame(int i) {
        return frames[i];
    }

    /**
     * Get an individual frame entry
     *
     * @param i index of frame
     * @param j index into frame
     *
     * @return the frame entry in frame <code>i</code> at index
     *          <code>j</code>
     */
    public float getFrameEntry(int i, int j) {
        return frames[i][j];
    }
}


/**
 * The wave (riff) data
 */
class Wave {
    private int numSamples;
    private int sampleRate;
    private short[] samples;

    // Only really used in loading of data.
    private int headerSize;
    private int numBytes;
    private int numChannels = 1;  // Only support mono

    static final short RIFF_FORMAT_PCM = 0x0001;

    /**
     * Read in a wave from a riff format
     *
     * @param dis DataInputStream to read data from
     */
    public Wave (DataInputStream dis) {
        try {
            loadHeader(dis);
            if (dis.skipBytes(headerSize - 16) != (headerSize - 16)) {
                throw new Error("Unexpected error parsing wave file.");
            }

            // Bunch of potential random headers
            while (true) {
                String s = new String(Utility.readChars(dis, 4));

                if (s.equals("data")) {
                    numSamples = Utility.readInt(dis, false) / 2;
                    break;
                } else if (s.equals("fact")) {
                    int i = Utility.readInt(dis, false);
                    if (dis.skipBytes(i) != i) {
                        throw new Error("Unexpected error parsing wave file.");
                    }
                } else {
                    throw new Error("Unsupported wave header chunk type " + s);
                }
            }

            int dataLength = numSamples * numChannels;
            samples = new short[numSamples];

            for (int i = 0; i < dataLength; i++) {
                samples[i] = Utility.readShort(dis, false);
            }

        } catch (IOException ioe) {
            throw new Error("IO error while parsing wave" + ioe.getMessage());
        }
    }

    /**
     * load a RIFF header
     *
     * @param dis DataInputStream to read from
     *
     * @throws IOException on ill-formatted input
     */
    private void loadHeader(DataInputStream dis) throws IOException {
        if (!checkChars(dis, "RIFF")) {
            throw new Error("Invalid wave file format.");
        }
        numBytes = Utility.readInt(dis,false);
        if (!checkChars(dis, "WAVEfmt ")) {
            throw new Error("Invalid wave file format.");
        }

        headerSize = Utility.readInt(dis, false);

        if (Utility.readShort(dis, false) != RIFF_FORMAT_PCM) {
            throw new Error("Invalid wave file format.");
        }

        if (Utility.readShort(dis, false) != 1) {
            throw new Error("Only mono wave files supported.");
        }
        
        sampleRate = Utility.readInt(dis, false);
        Utility.readInt(dis, false);
        Utility.readShort(dis, false);
        Utility.readShort(dis, false);
    }

    /**
     * Reconstruct a wave from a wave, sts, and lpc
     *
     * @param sampleRate the sample rate to use
     * @param lpc lpc
     * @param lpc_min minimum lpc value
     * @param lpc_range range of lpc values
     */
    public Wave(int sampleRate, STS[] stsData, LPC lpc, float lpc_min,
            float lpc_range) {
        // set number of samples and sample rate
        numSamples = 0;
        for (int i = 0; i < lpc.getNumFrames(); i++) {
            numSamples += stsData[i].getNumSamples();
        }
        samples = new short[numSamples];
        this.sampleRate = sampleRate;

        int start = 0;
        int end;
        int[] lpcResTimes = new int[lpc.getNumFrames()];
        int[] lpcResSizes = new int[lpc.getNumFrames()];
        short[] lpcResResidual = new short[numSamples];
        int[][] lpcResFrames = new int[lpc.getNumFrames()][];
        int lpcResNumChannels = lpc.getNumChannels() - 1;

        // load initial data
        for (int i = 0; i < lpc.getNumFrames(); i++) {
            lpcResTimes[i] = (int) (lpc.getTime(i) * sampleRate);
            lpcResFrames[i] = stsData[i].getFrame();
            end = start + stsData[i].getNumSamples();
            lpcResSizes[i] = stsData[i].getNumSamples();
            start = end;
        }

        for (int r = 0, i = 0; i < lpc.getNumFrames(); i++) {
            for (int j = 0; j < stsData[i].getNumSamples(); j++, r++) {
                lpcResResidual[r] = stsData[i].getResidual(j);
            }
        }

        float[] lpcCoefs = new float[lpcResNumChannels];
        float[] outbuf = new float[lpcResNumChannels + 1];
        int ci, cr;
        //float pp = 0;  // the C code uses this unnecessarily (for now)

        for (int r = 0, o = lpcResNumChannels, i = 0; i <
                lpc.getNumFrames(); i++) {
            // residual_fold is hard-coded to 1.
            int pm_size_samps = lpcResSizes[i];//  * residual_fold;

            // Unpack the LPC coefficients
            for (int k = 0; k < lpcResNumChannels; k++) {
                lpcCoefs[k] = (float)
                    ((((double) lpcResFrames[i][k])/65535.0) * lpc_range)
                    + lpc_min;
            }


            // resynthesize the signal
            for (int j = 0; j < pm_size_samps; j++, r++) {
                outbuf[o] = (float)
                    Utility.ulawToShort(lpcResResidual[r/* /residual_fold */]);

                cr = (o == 0 ? lpcResNumChannels : o-1);
                for (ci = 0; ci < lpcResNumChannels; ci++) {
                        outbuf[o] += lpcCoefs[ci] * outbuf[cr];
                        cr = (cr == 0 ? lpcResNumChannels : cr - 1);
                }
                samples[r] = (short) (outbuf[o]
                    /* + pp * lpcres->post_emphasis)*/); // post_emphasis = 0
                // pp = outbuf[o];
                o = (o == lpcResNumChannels ? 0 : o+1);
            }
        }
    }

    /**
     * Compare two waves and output how close the two are.
     * Useful for checking the general accuracy of find sts.
     *
     * <p>
     * Output may not exactly match that of flite find_sts
     * on Intel platforms due to discrepencies in the way that
     * Intel Pentiums perform floating point computations.
     * </p>
     *
     * @param the wave to compare this wave against
     *
     */
    public void compare(Wave wave2) {
        if (numSamples > wave2.numSamples) {
            wave2.compare(this);
        } else {
            double r = 0;
            int i = 0;
            for (i = 0; i < this.numSamples; i++) {
                r += (double)((float)this.samples[i] - (float)wave2.samples[i])
                    *(double)((float)this.samples[i] - (float)wave2.samples[i]);
            }
            r /= this.numSamples;
            System.out.println("a/b diff " + Double.toString(StrictMath.sqrt(r)));
        }
    }

    /**
     * Make sure that a string of characters appear next in the file
     *
     * @param dis DataInputStream to read in
     * @param chars a String containing the ascii characters you
     *          want the <code>dis</code> to contain.
     *
     * @return <code>true</code> if <code>chars</code> appears next
     *          in <code>dis</code>, else <code>false</code>
     * @throws on ill-formatted input (end of file, for example)
     */
    private boolean checkChars(DataInputStream dis, String chars)
            throws IOException {
        char[] carray = chars.toCharArray();
        for (int i = 0; i < carray.length; i++) {
            if ((char) dis.readByte() != carray[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the sample rate for this wave
     *
     * @return sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get the number of samples for this wave
     *
     * @return number of samples
     */
    public int getNumSamples() {
        return numSamples;
    }

    /* Get the sample data of this wave
     *
     * @return samples
     */
    public short[] getSamples() {
        return samples;
    }
}

/**
 * The sts data
 */
class STS {
    private int[] frame;
    private int numSamples;
    private short[] residual;

    /**
     * Create an empty STS
     */
    public STS() {
    }

    /**
     * Create an sts with the given data
     *
     * @param frame frame for this sts
     * @param numSamples number of samples this sts will contain
     * @param residual the residual for this sts
     * 
     */
    public STS(int[] frame, int numSamples, short[] residual) {
        this.frame = new int[frame.length];
        System.arraycopy(frame, 0, this.frame, 0, frame.length);
        this.numSamples = numSamples;
        this.residual = new short[residual.length];
        System.arraycopy(residual, 0, this.residual, 0, residual.length);
    }

    /**
     * Get the number of samples associated with this sts
     *
     * @return the number of samples for this sts
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Get the residual associated with this sts
     *
     * @return residual associated with this sts
     */
    public short getResidual(int i) {
        return residual[i];
    }

    /**
     * Get the frame associated with this sts
     *
     * @return a copy of the frame associated with this sts
     */
    public int[] getFrame() {
        int[] f = new int[frame.length];
        System.arraycopy(frame, 0, f, 0, frame.length);
        return f;
    }

    /**
     * Get an entry out of the frame
     *
     * @param index the index into the frame
     *
     * @return the entry in the frame at offset <code>index</code>
     */
    public int getFrameEntry(int index) {
        return frame[index];
    }
}


/**
 * This class is for general purpose functions such as reading and
 * writing from files, or converting formats of numbers.
 */
class Utility {

    /**
     * Reads the next word (text separated by whitespace) from the
     * given stream
     *
     * @param dis the input stream
     *
     * @return the next word
     *
     * @throws IOException on error
     */
    public static String readWord(DataInputStream dis) throws IOException {
        StringBuffer sb = new StringBuffer();
        char c;

        // skip leading whitespace
        do {
            c = readChar(dis);
        } while(Character.isWhitespace(c));

        // read the word
        do {
            sb.append(c);
            c = readChar(dis);
        } while (!Character.isWhitespace(c));
        return sb.toString();
    }

    /**
     * Reads a single char from the stream
     *
     * @param dis the stream to read
     * @return the next character on the stream
     *
     * @throws IOException if an error occurs
     */
    public static char readChar(DataInputStream dis) throws IOException {
        return (char) dis.readByte();
    }

    /**
     * Reads a given number of chars from the stream
     *
     * @param dis the stream to read
     * @param num the number of chars to read
     * @return a character array containing the next <code>num<code>
     *          in the stream
     *
     * @throws IOException if an error occurs
     */
    public static char[] readChars(DataInputStream dis, int num)
            throws IOException {
        char[] carray = new char[num];
        for (int i = 0; i < num; i++) {
            carray[i] = readChar(dis);
        }
        return carray;
    }

    /**
     * Read a float from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return a floating pint value
     *
     * @throws IOException on error
     */
    public static float readFloat(DataInputStream dis, boolean isBigEndian)
            throws IOException {
        float val;
        if (!isBigEndian) {
            val =  readLittleEndianFloat(dis);
        } else {
            val =  dis.readFloat();
        }
        return val;
    }

    /**
     * Reads the next float from the given DataInputStream,
     * where the data is in little endian.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return a float
     */
    public static float readLittleEndianFloat(DataInputStream dataStream)
            throws IOException {
        return Float.intBitsToFloat(readLittleEndianInt(dataStream));
    }

    /**
     * Read an integer from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return an integer value
     *
     * @throws IOException on error
     */
    public static int readInt(DataInputStream dis, boolean isBigEndian)
            throws IOException {
        if (!isBigEndian) {
            return readLittleEndianInt(dis);
        } else {
            return dis.readInt();
        }
    }

    /**
     * Reads the next little-endian integer from the given DataInputStream.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return an integer
     */
    public static int readLittleEndianInt(DataInputStream dataStream)
            throws IOException {
        int bits = 0x00000000;
        for (int shift = 0; shift < 32; shift += 8) {
            int byteRead = (0x000000ff & dataStream.readByte());
            bits |= (byteRead << shift);
        }
        return bits;
    }

    /**
     * Read a short from the input stream, byte-swapping as
     * necessary
     *
     * @param dis the inputstream
     * @param isBigEndian whether or not the data being read in is in
     *          big endian format.
     *
     * @return an integer value
     *
     * @throws IOException on error
     */
    public static short readShort(DataInputStream dis, boolean isBigEndian)
        throws IOException {
        if (!isBigEndian) {
            return readLittleEndianShort(dis);
        } else {
            return dis.readShort();
        }
    }

    /**
     * Reads the next little-endian short from the given DataInputStream.
     *
     * @param dataStream the DataInputStream to read from
     *
     * @return a short
     */
    public static short readLittleEndianShort(DataInputStream dis)
        throws IOException {
        short bits = (short)(0x0000ff & dis.readByte());
        bits |= (((short)(0x0000ff & dis.readByte())) << 8);
        return bits;
    }

    /**
     * Convert a short to ulaw format
     * 
     * @param sample the short to convert
     *
     * @return a short containing an unsigned 8-bit quantity
     *          representing the ulaw
     */
    public static short shortToUlaw(short sample) {
        final int[] exp_lut = {0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
                                   4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
                                   5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
                                   5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                                   7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7};

        int sign, exponent, mantissa;
        short ulawbyte;

        final short CLIP = 32635;
        final short BIAS = 0x0084;

        /* Get the sample into sign-magnitude. */
        sign = (sample >> 8) & 0x80; /* set aside the sign */
        if ( sign != 0 ) {
            sample = (short) -sample; /* get magnitude */
        }
        if ( sample > CLIP ) sample = CLIP; /* clip the magnitude */

        /* Convert from 16 bit linear to ulaw. */
        sample = (short) (sample + BIAS);
        exponent = exp_lut[( sample >> 7 ) & 0xFF];
        mantissa = ( sample >> ( exponent + 3 ) ) & 0x0F;
        ulawbyte = (short)
            ((~ ( sign | ( exponent << 4 ) | mantissa)) & 0x00FF);
        if ( ulawbyte == 0 ) ulawbyte = 0x02; /* optional CCITT trap */
        return ulawbyte;
    }

    /**
     * Convert a ulaw format to short
     * 
     * @param ulawbyte a short containing an unsigned 8-but quantity
     *          representing a ulaw
     *
     * @return the short equivalent of the ulaw
     */
    public static short ulawToShort(short ulawbyte) {
        final int[] exp_lut = { 0, 132, 396, 924, 1980, 4092, 8316, 16764 };
        int sign, exponent, mantissa;
        short sample;

        ulawbyte = (short) (ulawbyte & 0x00FF);
        ulawbyte = (short) (~ulawbyte);
        sign = ( ulawbyte & ((short) 0x80) );
        exponent = (int) ( (ulawbyte & (short) 0x00FF) >> 4 ) & 0x07;
        mantissa = ulawbyte & (short) 0x0F;
        sample = (short) (exp_lut[exponent] + (mantissa << (exponent + 3)));
        if ( sign != 0 ) sample = (short) (-sample);

        return sample;
    }


    /**
     * Print a float type's internal bit representation in hex
     *
     * @param f the float to print
     *
     * @return a string containing the hex value of <code>f</code>
     */
    public static String hex(float f) {
        return Integer.toHexString(Float.floatToIntBits(f));
    }
}

