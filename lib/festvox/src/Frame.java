import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.StringTokenizer;

public class Frame {
    public float pitchmarkTime;
    public int[] parameters;

    public Frame() {
    }
    
    public Frame(float pitchmarkTime, int[] parameters) {
        this.pitchmarkTime = pitchmarkTime;
        this.parameters = parameters;
    }

    /**
     * Dumps the ASCII form of this Frame.
     */
    public void dumpData(PrintStream out) {
        out.print("FRAME");
        for (int i = 0; i < parameters.length; i++) {
            out.print(" " + parameters[i]);
        }
        out.println();
        out.println("RESIDUAL 0");
    }    
}
