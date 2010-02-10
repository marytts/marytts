/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.util.data.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A class to read and parse labels in a text file. The file format should conform to that used by ESPS Xwaves and the many other
 * labeling programs which support that format.
 * 
 * @author Ingmar Steiner
 */
public class XwavesLabelfileDataSource {
    // main class variables (reader, times, labels, header lines)
    protected BufferedReader reader;
    protected ArrayList<Double> times = new ArrayList<Double>();
    protected ArrayList<String> labels = new ArrayList<String>();
    protected ArrayList<String> header = new ArrayList<String>();
    
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
        try {
            parseLabels();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Read lines from the label file and parse them. As each line is parsed, the label in that line and its end time are appended
     * to the appropriate arrays, and the initial header lines are stored in a third vector.
     * 
     * @return true if everything went well
     * @throws IOException
     */
    private boolean parseLabels() throws IOException {
        // initialize some variables
        String line;
        boolean headerComplete = false;

        // Legend for regular expression:
        // 
        // ^      start of line 
        // \\s*   leading whitespace 
        // (      start of first captured group (time) 
        // \\d+   one or more digits 
        // (?:    followed by a non-capturing group containing 
        // \\.    a period and 
        // \\d+   one or more digits 
        // )?     this group is optional 
        // )      end of first captured group 
        // \\s+   whitespace 
        // .+?    second column, which is ignored (not captured)
        // \\s+?  whitespace 
        // (.*)   second captured group (label)
        // $      end of line
        Pattern linePattern = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s+.+?\\s+?(.*)$");
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

            if (matches) {
                // some label files might be headerless;
                // in that case, a well-formed line indicates that we are already seeing label data
                headerComplete = true;

                // parse the line by accessing the groups captured by the regex Matcher
                try {
                    // the first group is the label's end time
                    timeStr = lineMatcher.group(1);
                    // the second group is the label itself
                    label = lineMatcher.group(2);
                } catch (IllegalStateException ise) {
                    // the match was somehow not performed, is the pattern OK?
                    throw ise;
                } catch (IndexOutOfBoundsException ioobe) {
                    // the match was performed, but no groups were found, is the pattern OK?
                    throw ioobe;
                }

                try {
                    // parse the end time into a Double and append it to times
                    time = Double.parseDouble(timeStr);
                    times.add(time);
                } catch (NumberFormatException nfe) {
                    // number could not be parsed; this should never actually happen!
                    throw nfe;
                }

                // append label to labels
                labels.add(label);

            } else {
                // line could not be parsed by regex; are we still in the header?
                if (!headerComplete) {
                    if (line.trim().startsWith("#"))
                        // hash line signals end of header (but is not itself part of the header)
                        headerComplete = true;
                    else
                        // no hash line seen so far, line seems to be part of header
                        header.add(line);
                } else {
                    // header was already complete, or we are dealing with a headerless label file,
                    // but we found a line that could not be parsed!
                    System.err.println("Malformed line found outside of header:\n" + line);
                    throw new IOException();
                }
            }
        }

        // it should never happen that times and labels do not have the same number of elements!
        assert times.size() == labels.size() : "";

        return true;
    }

    /**
     * getter method for times
     * 
     * @return times as ArrayList of Doubles
     */
    public ArrayList<Double> getTimes() {
        return times;
    }

    /**
     * getter method for labels
     * 
     * @return labels as ArrayList of Strings
     */
    public ArrayList<String> getLabels() {
        return labels;
    }

    /**
     * getter method for header
     * 
     * @return header lines as ArrayList of Strings
     */
    public ArrayList<String> getHeader() {
        return header;
    }

    /**
     * Join labels into string
     * 
     * @param glue
     *            String inserted between the elements as they are joined
     * 
     * @return joined String
     */
    public String joinLabelsToString(String glue) {
        StringBuilder sb = new StringBuilder();
        ListIterator<String> li = labels.listIterator();

        sb.append(li.next());
        while (li.hasNext()) {
            sb.append(glue);
            sb.append(li.next());
        }
        return sb.toString();
    }
}
