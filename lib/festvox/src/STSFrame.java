import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.StringTokenizer;

public class STSFrame extends Frame {
    String paramsLine;
    String residualsLine;

    public STSFrame(int numChannels, BufferedReader reader)
        throws IOException {
        String line = reader.readLine();
        pitchmarkTime = Float.parseFloat(line);

        // If we wanted to have an explicit representation of the data,
        // we could use the following:
        /*
        line = reader.readLine();
        StringTokenizer tokenizer = new StringTokenizer(line);
        for (int i = 0; i < numChannels; i++) {
            parameters[i] = Integer.parseInt(tokenizer.nextToken());
        }

        line = reader.readLine();
        tokenizer = new StringTokenizer(line);
        residuals = new int[Integer.parseInt(tokenizer.nextToken())];
        for (int i = 0; i < residuals.length; i++) {
            residuals[i] = Integer.parseInt(tokenizer.nextToken());
        }
        */
        // Instead, we just blindly copy the entire line:
        paramsLine = reader.readLine();
        residualsLine = reader.readLine();
    }

    /**
     * Dumps the ASCII form of this Frame.
     */
    public void dumpData(PrintStream out) {
        out.println("FRAME "+paramsLine);
        out.println("RESIDUAL " + residualsLine);
    }    
}
