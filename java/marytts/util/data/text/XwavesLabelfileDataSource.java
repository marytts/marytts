package marytts.util.data.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A class to read and parse labels in a text file. The file format should
 * conform to that used by ESPS Xwaves and the many other labeling programs
 * which support that format.
 * 
 * @author Ingmar Steiner
 */
public class XwavesLabelfileDataSource {
    protected BufferedReader reader;

    /**
     * Read data from a Label file.
     * 
     * @param filename
     *            Label filename as a String
     */
    public XwavesLabelfileDataSource(String filename) throws FileNotFoundException {
        this(new FileReader(filename));
    }

    /**
     * Read data from a Label file.
     * 
     * @param reader
     *            Label file as a Reader
     */
    public XwavesLabelfileDataSource(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    /**
     * Read lines from the label file and parse them. As each line is parsed,
     * the label in that line and its start time are appended to the appropriate
     * vectors.
     * 
     * @param times
     *            Vector of type Double, into which the label start times are
     *            read
     * @param labels
     *            Vector of type String, into which the label text strings are
     *            read
     * @param header
     *            Vector of type String, into which lines are read that cannot
     *            be parsed and presumably belong to the header
     * @return true if everything went well, otherwise false
     * @throws IOException
     */
    public boolean parseLabels(Vector<Double> times, Vector<String> labels)
            throws IOException {
        // if no header is requested, create a dummy vector, which is
        // subsequently discarded
        Vector<String> header = new Vector<String>();
        return parseLabels(times, labels, header);
    }

    /**
     * Read lines from the label file and parse them. As each line is parsed,
     * the label in that line and its start time are appended to the appropriate
     * vectors, and the initial header lines are stored in a third vector.
     * 
     * @param times
     *            Vector of type Double, into which the label start times are
     *            read
     * @param labels
     *            Vector of type String, into which the label text strings are
     *            read
     * @param header
     *            Vector of type String, into which lines are read that cannot
     *            be parsed and presumably belong to the header
     * @return true if everything went well, otherwise false
     * @throws IOException
     */
    public boolean parseLabels(Vector<Double> times, Vector<String> labels,
            Vector<String> header) throws IOException {
        // initialize some variables
        String line;
        boolean headerComplete = false;

        /* Legend for regular expression:
         * 
         * ^      start of line 
         * \\s*   leading whitespace 
         * (      start of first captured group (time) 
         * \\d+   one or more digits 
         * (?:    followed by a non-capturing group containing 
         * \\.    a period and 
         * \\d+   one or more digits 
         * )?     this group is optional 
         * )      end of first captured group 
         * \\s+   whitespace 
         * .+?    second column, which is ignored (not captured)
         * \\s+?  whitespace 
         * (.*)   second captured group (label)
         * $      end of line
         */
        Pattern linePattern = Pattern
                .compile("^\\s*(\\d+(?:\\.\\d+)?)\\s+.+?\\s+?(.*)$");
        boolean matches = false;

        // initialize some more variables for each line's captured groups
        String timeStr = null;
        String label = null;
        double time;

        // read the file line by line
        while ((line = reader.readLine()) != null) {
            // apply the regex Pattern to the current line...
            Matcher lineMatcher = linePattern.matcher(line);
            // ...and see if it matches
            matches = lineMatcher.matches();
            // some label files might be headerless;
            // in that case, a well-formed line indicates that we are already
            // seeing label data
            if (matches)
                headerComplete = true;

            // parse the line by accessing the groups captured by the regex
            // Matcher
            try {
                // the first group is the label's start time
                timeStr = lineMatcher.group(1);
                try {
                    // which should be parsed into a Double
                    time = Double.parseDouble(timeStr);
                    // append time to the times Vector
                    times.add(time);
                } catch (NumberFormatException nfe) {
                    // number could not be parsed; this should never actually
                    // happen!
                    throw nfe;
                }

                // the second group is the label itself
                label = lineMatcher.group(2);
                // which is appended to the labels Vector
                labels.add(label);
            }
            catch (IllegalStateException ise)
            {
                continue;
            }

            catch (IndexOutOfBoundsException ioobe) {
                // line could not be parsed by regex; are we still in the
                // header?
                if (!headerComplete) {
                    if (line.trim().startsWith("#"))
                        // hash line signals end of header (but is not itself
                        // part of the header)
                        headerComplete = true;
                    else
                        // no hash line seen so far, line seems to be part of
                        // header
                        header.add(line);
                } else {
                    // header was already complete, or we are dealing with a
                    // headerless label file,
                    // but we found a line that could not be parsed!
                    System.err
                            .println("Malformed line found outside of header:\n"
                                    + line);
                    throw ioobe;
                }
            }

        }

        // it should never happen that times and labels do not have the same
        // number of elements!
        assert times.size() == labels.size();

        return true;
    }

}
