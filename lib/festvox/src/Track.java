import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.StringTokenizer;

public class Track {
    public String filename;
    public int numFrames;
    public int numChannels;
    public int sampleRate;
    public float min;
    public float range;   
    public Frame[] frames;

    /* Set up later - says where this track is in the big monolithic
     * sts and mcep files.
     */
    public int startIndex;
    
    static public final int STS = 1;
    static public final int MCEP = 2;
    
    public Track(String filename, int type) throws IOException {
        this(filename, type, 0.0f, 0.0f);
    }

    public Track(String filename, int type, float min, float range)
        throws IOException {
        this.min = min;
        this.range = range;
        if (type == STS) {
            readSTS(filename);
        } else if (type == MCEP) {
            readMCEP(filename);
        } else {
            throw new Error("unknown type: " + type);
        }
    }
    
    void readSTS(String filename) throws IOException {
        this.filename = filename;
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(filename)));

        /* Read STS meta info
         */
        String line = reader.readLine();
        StringTokenizer tokenizer = new StringTokenizer(line);
        numFrames = Integer.parseInt(tokenizer.nextToken());
        numChannels = Integer.parseInt(tokenizer.nextToken());
        sampleRate = Integer.parseInt(tokenizer.nextToken());
        min = Float.parseFloat(tokenizer.nextToken());
        range = Float.parseFloat(tokenizer.nextToken());
        
        /* Read in the STS frame data from the file.
         */
        frames = new STSFrame[numFrames];
        for (int i = 0; i < numFrames; i++) {
            frames[i] = new STSFrame(numChannels, reader);
        }
        
        reader.close();
    }

    void readMCEP(String filename) throws IOException {
        this.filename = filename;
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(filename)));

        /* Read MCEP meta info
         */
        String line = reader.readLine();
        while (!line.equals("EST_Header_End")) {
            line = reader.readLine();
            if (line.startsWith("NumFrames")) {
                numFrames = Integer.parseInt(line.substring(10));
            } else if (line.startsWith("NumChannels")) {
                /* With MCEP, the first channel is the energy.  We
                 * drop this because it is not used to select units.
                 */
                numChannels = Integer.parseInt(line.substring(12)) - 1;
            }
        }

        /* Read each of the frames.
         */
        frames = new Frame[numFrames];
        for (int i = 0; i < numFrames; i++) {
            line = reader.readLine();
            StringTokenizer tokenizer = new StringTokenizer(line);
            float pitchmarkTime = Float.parseFloat(tokenizer.nextToken());
            tokenizer.nextToken(); /* no clue what 1 means in the file */
            int mcepParameters[] = new int[numChannels];
            for (int j = 0; j < numChannels + 1; j++) {
                float mcepParameter = Float.parseFloat(tokenizer.nextToken());

                /* With MCEP, the first channel is the energy.  We
                 * drop this because it is not used to select units.
                 */
                if (j == 0) {
                    continue;
                } else {
                    /* Normalize the parameter to 0 - 65535.
                     */
                    mcepParameters[j - 1] = (int)
                        (65535.0f * (mcepParameter - min) / range);
                }
            }
            frames[i] = new Frame(pitchmarkTime, mcepParameters);
        }
        
        reader.close();
    }

    /**
     * Returns all the frames for this file.
     */
    public Frame[] getFrames() {
        return frames;
    }
    
    /**
     * Finds the index of the frame closest to the given time.
     *
     * @param time the time in seconds
     */
    public int findTrackFrameIndex(float time) {
        int frameNum = 0;
        while ((frameNum < (frames.length - 1))
               && (Math.abs(time - frames[frameNum].pitchmarkTime)
                   > Math.abs(time - frames[frameNum + 1].pitchmarkTime))) {
            frameNum++;
        }
        return frameNum;
    }

    /**
     * Dumps the ASCII form of this track.
     */
    public void dumpData(PrintStream out) {
        for (int i = 0; i < frames.length; i++) {
            frames[i].dumpData(out);
        }
    }
    
    /**
     * For testing.
     *
     *  args[0] = filename
     *  args[1] = type (1 = STS, 2 = MCEP)
     */
    static public void main(String[] args) {
        try {
            Track track = new Track(args[0], Integer.parseInt(args[1]));
            System.out.println(track.filename);
            System.out.println(track.numFrames);
            System.out.println(track.numChannels);
            System.out.println(track.min);
            System.out.println(track.range);
            System.out.println(track.frames.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
