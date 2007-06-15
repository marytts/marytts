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
 * 
 */
package de.dfki.lt.mary.gizmos;

/**
 * @author max
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
// import java.util.Map;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.FeatureFileIndexer;
import de.dfki.lt.mary.unitselection.TimelineReader;
import de.dfki.lt.mary.unitselection.UnitFileReader;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;
import de.dfki.lt.mary.unitselection.voiceimport.DatabaseLayout;
import de.dfki.lt.mary.unitselection.voiceimport.MaryHeader;
import de.dfki.lt.mary.util.MaryAudioUtils;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.mary.unitselection.cart.*;
import de.dfki.lt.mary.unitselection.cart.LeafNode.*;
import de.dfki.lt.mary.unitselection.concat.DatagramDoubleDataSource;
import de.dfki.lt.signalproc.analysis.EnergyAnalyser;
import de.dfki.lt.signalproc.analysis.EnergyAnalyser_dB;
import de.dfki.lt.signalproc.display.EnergyHistogram;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;

public class CARTAnalyzer {

    // GLOBAL VARIABLES

    private float cutAboveValue = 25;

    private float cutBelowValue = 20;

    private byte cutWhere = 1;

    // cutWhere = 1 for above (cutAboveValue), 2 for below (cutBelowValue), 3
    // for both
    private final String[] cutWhereS = { "", "above", "below", "both" };

    // for easy access, first entry "" used so that values can be accessed as
    // [1-3]

    // number of units in CART
    private int numUnits = 0;

    // number of leafs in CART
    private int numLeafs = 0;

    // also, store the current leaf for navigation purposes
    private int currLeafIndex = 1;

    private LeafNode.IntAndFloatArrayLeafNode currLeaf;

    // voice properties

    private TimelineReader tlr;

    private UnitFileReader ufr;

    private WavWriter ww;

    private FeatureDefinition feaDef;

    private FeatureFileIndexer ffi;

    // and the tree, since we want the float probability values, it's an
    // ExtendedClassificationTree
    private ExtendedClassificationTree ctree;
    
    private int percent = 0;

    
    
    public CARTAnalyzer(String unitReaderFile,
            			String timelineReaderFile,
            			String featureFileIndexerFile,
            			String cartFile) throws IOException {
        this.ufr = new UnitFileReader(unitReaderFile);
        this.tlr = new TimelineReader(timelineReaderFile);
        this.ffi = new FeatureFileIndexer(featureFileIndexerFile);
        this.feaDef = ffi.getFeatureDefinition();
        this.ww = new WavWriter();
        // load cart
        ctree = loadTreeFromFile(cartFile, feaDef);
    }

    /**
     * main() method does nothing except create an instance of CA and call its
     * actual main method run()
     * 
     * @param args
     *            command line parameters
     * 
     * @throws Exception
     * 
     
    public static void main(String[] args) throws Exception {
        outln("CART Analyzer initialising...");
        long time1 = System.currentTimeMillis();
        // initialise
        CARTAnalyzer ca = new CARTAnalyzer();
        long time2 = System.currentTimeMillis();
        outln("initialised in " + (time2 - time1) + " ms. now running.");
        outln("see \"help\" for a quick overview of available commands or \"exit\" to quit.");
        outln("");
        // run the main method (command line)
        ca.run();
    }**/

    /**
     * input method for command line of the program, can also be used for any
     * other situation that requires user keyboard input please note that method
     * turns all input to lower case which might cause problems with f.e. the
     * load method but makes if-requests easier, change if necessary
     * 
     * @param prompt
     *            the string of the prompt that should be displayed
     * @return the user input
     * 
     * @throws IOException
     */
    public String input(String prompt) throws IOException {

        BufferedReader line = new BufferedReader(new InputStreamReader(
                System.in));
        String in;
        out(prompt);
        in = line.readLine();
        return in.trim().toLowerCase();
    }

    /**
     * main method, mainly command line parsing of user input and calling the
     * appropriate methods
     * 
     * @throws Exception
     *             (god knows which ones)
     */
    public void run() throws Exception {

        // COMMAND LINE
        String command = "";
        while (true) {
            // call the input method with default ca (cart analyzer) prompt, cL
            // being the -current Leaf-
            StringTokenizer tokenizer = new StringTokenizer(input("ca:cL:"
                    + currLeafIndex + " ?> "));
            if (tokenizer.hasMoreTokens())
                // extract command - the first word of the input
                command = tokenizer.nextToken();
            // empty line => return
            else
                continue;

            // COMMANDS
            try {

                // DEBUG: Print stats in file with format: <leaf> <#units>
                // <mean> <sd>
                if (command.equals("stats")) {
                    String filename = "./stats.txt";
                    if (tokenizer.hasMoreTokens()) {
                        filename = "./" + tokenizer.nextToken();
                    }
                    outln("Writing stats in " + filename);
                    outln("Now computing...");
                    DecimalFormat df = new DecimalFormat("0.00");
                    PrintWriter booyah = new PrintWriter(new FileWriter(
                            new File(filename)));
                    booyah.println("li\t#u\tmean\t\tsd");
                    booyah.println();
                    LeafNode ln = (IntAndFloatArrayLeafNode) ctree
                            .getFirstLeafNode();
                    int counter = 1;
                    while (ln != null) {
                        int nU;
                        outln("leaf " + counter);
                        nU = ln.getNumberOfData();
                        booyah.print(counter + "\t" + nU + "\t");
                        if (nU < 2) {
                            booyah.println("0\t\t0");
                        } else {
                            booyah.print(df.format(MaryUtils.mean(((IntAndFloatArrayLeafNode) ln).getFloatData())) + "\t\t");
                            booyah.println(df.format(MaryUtils.stdDev(((IntAndFloatArrayLeafNode) ln).getFloatData())));
                        }
                        outln("leaf " + counter + " done.");
                        booyah.flush();
                        counter++;
                        ln = (IntAndFloatArrayLeafNode) ln.getNextLeafNode();

                    }
                    booyah.close();
                    outln("finished.");
                }

                /**
                 * Provide a (command-specific) help
                 * 
                 * @param command,
                 *            optional
                 * 
                 */
                if (command.equals("help") || command.equals("h")) {
                    if (tokenizer.hasMoreTokens()) {
                        help(tokenizer.nextToken());
                    } else {
                        help("");
                    }
                    continue;
                }

                /**
                 * Provide help about the program itself
                 * 
                 * @param command,
                 *            optional
                 * 
                 */
                if (command.equals("about")) {
                    help("about");
                    continue;
                }

                /**
                 * This is the actual command for the part of the program that
                 * it was originally invented and written for. Start the
                 * analysis of the CART, either the 1) semi-automatic analysis,
                 * controlled and guided by the user, 2) fully automatic
                 * analysis with or without erasing of outliers
                 * 
                 * @param String
                 *            ("user" or "auto"; respectively for 1) and 2))
                 *            ("resume" is also allowed for resuming last work
                 *            with "user" mode)
                 * @param String
                 *            filename, this file will be used for logging the
                 *            process by a PrintWriter
                 * @param String
                 *            cartfilename, optional, the resulting cart will be
                 *            written to this file when all is done and finished
                 * 
                 * @returns hopefully a clean CART
                 * 
                 */
                if (command.equals("a") || command.equals("analyze")) {
                    if (tokenizer.countTokens() > 3) {
                        outln("unknown usage. type \"help analyze\" or \"help a\" for help on this command.");
                        continue;
                    }
                    // without parameters, ask
                    if (tokenizer.countTokens() == 0) {
                        String modeS;
                        String logfile = "./ca_default.log";
                        String cartfile = null;
                        boolean auto = true;
                        while (true) {
                            modeS = input("Analyze mode? [user,auto,resume]: ")
                                    .toLowerCase();
                            if (modeS.equals("user")) {
                                auto = false;
                                break;
                            }
                            if (modeS.equals("resume")) {
                                auto = false;
                                analyzeInteractive("resume", null);
                                break;
                            }
                            if (modeS.equals("auto")) {
                                break;
                            }
                        }
                        modeS = input("logfile? [" + logfile + "]: ");
                        if (!modeS.equals(""))
                            logfile = "./" + modeS;
                        modeS = input("cartfile? []: ");
                        if (!modeS.equals("")) {
                            cartfile = "./" + modeS;
                        }
                        if (auto) {
                            analyzeAutomatic(logfile, cartfile);
                        } else {
                            analyzeInteractive(logfile, cartfile);
                        }
                        continue;
                    }
                    String mode = tokenizer.nextToken();
                    if (mode.equals("user")) {
                        // launch mode 1 (user-guided), with next token being
                        // the logger filename, then cart or null
                        analyzeInteractive(tokenizer.nextToken(), tokenizer
                                .hasMoreTokens() ? tokenizer.nextToken() : null);
                        // return to command line
                        continue;
                    }
                    if (mode.equals("resume")) {
                        // launch mode 1 (user-guided), but (try to) resume from
                        // last time
                        analyzeInteractive("resume", null);
                        // return to command line
                        continue;
                    }
                    if (mode.equals("auto")) {
                        // launch mode 2 (automatic analysis), with next token
                        // being the logger filename,
                        // then cart or null
                        analyzeAutomatic(tokenizer.nextToken(), tokenizer
                                .hasMoreTokens() ? tokenizer.nextToken() : null);
                        // return to command line
                        continue;
                    }
                    // if mode is invalid, provide help
                    outln("invalid mode, see \"help analyze\" or \"help a\" for details.");
                    continue;
                }

                /**
                 * Dump the current cart in a textfile
                 * 
                 * @param filename
                 *            name of text file that cart should be dumped in
                 */
                if (command.equals("textdump") || command.equals("td")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help textdump\" or \"help td\" for details.");
                        continue;
                    }
                    PrintWriter pw = new PrintWriter(new FileWriter(new File(
                            tokenizer.nextToken())));
                    // call toTextOut method of the cart
                    ctree.toTextOut(pw);
                    continue;
                }

                /**
                 * Dump the current cart in a binary (and thus usable) CA file
                 * (i.e. dumping the ExtendedClassificationTree)
                 * 
                 * @param filename
                 *            name of binary file that cart should be dumped in
                 * 
                 */
                if (command.equals("save") || command.equals("s")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help dump\" or \"help d\" for details.");
                        continue;
                    }
                    String filename = tokenizer.nextToken();
                    // call dumping helper method
                    saveCart(filename);
                    continue;
                }

                /**
                 * Dump the current cart in a binary (and thus usable) MARY file
                 * (i.e. dumping the ECTree as a ClassificationTree; (see note
                 * of command "load" below)
                 * 
                 * @param filename
                 *            name of binary file that cart should be dumped in
                 * 
                 */
                if (command.equals("dump") || command.equals("d")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help dump\" or \"help d\" for details.");
                        continue;
                    }
                    String filename = tokenizer.nextToken();
                    // call dumping helper method
                    dumpCart(filename);
                    continue;
                }

                /**
                 * 
                 * Load another (can of course be a previously dumped) cart from
                 * a given (binary) filename Note: It is possible to load a
                 * "normal" .mry cart, you will still be able to listen to the
                 * leafs and view the untis etc, of course with all means and
                 * sds=0.0, you won't be able to find outliers or cut any units
                 * from the tree.
                 * 
                 * @param string
                 *            of the filename
                 * 
                 * @return the new cart that the program will use and work with
                 * 
                 */
                if (command.equals("load") || command.equals("l")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help load\" or \"help l\" for details.");
                        continue;
                    }
                    // extract filename
                    String cartf = tokenizer.nextToken();
                    // load tree from that file
                    loadTreeFromFile(cartf, feaDef);
                    // show quick stats
                    outln("Number units: " + numUnits + "; number leafs: "
                            + numLeafs);
                    continue;
                }

                /**
                 * Jump to a leaf in the current cart
                 * 
                 * @param index,
                 *            index of the leaf to jump to
                 * 
                 */
                if (command.equals("jump") || command.equals("j")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help jump\" or \"help j\" for details.");
                        continue;
                    }
                    // call helper method jumpToLeaf with next token which
                    // should be index of the new leaf
                    jumpToLeaf(Integer.parseInt(tokenizer.nextToken()));
                    continue;
                }

                /**
                 * Jump forward x leafs in cart
                 * 
                 * no param => just next else => jump forward x leafs
                 */
                if (command.equals("next") || command.equals("n")) {
                    if (tokenizer.countTokens() > 1) {
                        outln("wrong usage. see \"help next\" or \"help n\" for details.");
                        continue;
                    }
                    if (tokenizer.hasMoreTokens()) {
                        jumpToLeaf(currLeafIndex
                                + Integer.parseInt(tokenizer.nextToken()));
                    } else {
                        jumpToLeaf(currLeafIndex + 1);
                    }
                    continue;
                }

                /**
                 * Jump backward x leafs in cart
                 * 
                 * no param => just previous else => jump backward x leafs
                 */
                if (command.equals("prev") || command.equals("p")) {
                    if (tokenizer.countTokens() > 1) {
                        outln("wrong usage. see \"help prev\" or \"help p\" for details.");
                        continue;
                    }
                    if (tokenizer.hasMoreTokens()) {
                        jumpToLeaf(currLeafIndex
                                - Integer.parseInt(tokenizer.nextToken()));
                    } else {
                        jumpToLeaf(currLeafIndex - 1);
                    }
                    continue;
                }

                /**
                 * Get the current cut mode (above cutAboveValue, below
                 * cutBelowValue or both)
                 * 
                 * @returns the item of our final String array to which the
                 *          "pointer" cutWhere is currently pointing to
                 * 
                 */
                if (command.equals("getcutmode") || command.equals("gcm")) {
                    outln("Cut mode is currently \"" + cutWhereS[cutWhere]
                            + "\"");
                    outln("Type setcutmode [above/below/both] to change,see also (get/set)(above/below)value");
                    continue;
                }

                /**
                 * Get the current cut mode (above cutAboveValue, below
                 * cutBelowValue or both)
                 * 
                 * @returns the item of our final String array to which the
                 *          "pointer" cutWhere is currently pointing to
                 * 
                 */
                if (command.equals("setcutmode") || command.equals("scm")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help setcutmode\" or \"help scm\" for details.");
                        continue;
                    }
                    String mode = tokenizer.nextToken();
                    if (mode.equals("above")) {
                        cutWhere = 1;
                        outln("Cut mode successfully set to \"above\".");
                        continue;
                    }
                    if (mode.equals("below")) {
                        cutWhere = 2;
                        outln("Cut mode successfully set to \"below\".");
                        continue;
                    }
                    if (mode.equals("both")) {
                        cutWhere = 3;
                        outln("Cut mode successfully set to \"both\".");
                        continue;
                    }
                    outln("wrong usage. see \"help setcutmode\" or \"help scm\" for details.");
                    continue;
                }

                /**
                 * Get the current cutAboveValue
                 * 
                 * @returns global variable cutAboveValue
                 * 
                 */
                if (command.equals("getabovevalue") || command.equals("gav")) {
                    outln("cutAboveValue currently " + cutAboveValue);
                    continue;
                }

                /**
                 * Set the current cutAboveValue
                 * 
                 * @param new
                 *            float
                 * 
                 */
                if (command.equals("setabovevalue") || command.equals("sav")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help setabovevalue\" or \"help sav\" for details.");
                        continue;
                    }
                    float temp = Float.parseFloat(tokenizer.nextToken());
                    cutAboveValue = temp;
                    outln("cutAboveValue successfully set to " + cutAboveValue);
                    continue;
                }

                /**
                 * Get the current cutBelowValue
                 * 
                 * @returns global variable cutBelowValue
                 * 
                 */
                if (command.equals("getbelowvalue") || command.equals("gbv")) {
                    outln("cutBelowValue currently " + cutBelowValue);
                    continue;
                }

                /**
                 * Set the current cutBelowValue
                 * 
                 * @param new
                 *            float
                 * 
                 */
                if (command.equals("setbelowvalue") || command.equals("sbv")) {
                    if (tokenizer.countTokens() != 1) {
                        outln("wrong usage. see \"help setbelowvalue\" or \"help sbv\" for details.");
                        continue;
                    }
                    float temp = Float.parseFloat(tokenizer.nextToken());
                    cutAboveValue = temp;
                    outln("cutAboveValue successfully set to " + cutAboveValue);
                    continue;
                }

                /**
                 * Look for a unit in the cart with given index
                 * 
                 * @param index,
                 *            the index of the unit to look for
                 * 
                 */
                if (command.equals("lookfor")) {
                    int unit = 0;
                    if (tokenizer.countTokens() == 1) {
                        // parse unit index
                        unit = Integer.parseInt(tokenizer.nextToken());
                    } else {
                        outln("wrong usage. see \"help lookfor\" for details.");
                        continue;
                    }
                    outln("looking for unit index " + unit + " in tree ");
                    // call helper method
                    int leaf = findUnit(unit);

                    // return status of -1 means unit not found
                    if (leaf == -1) {
                        outln("unit " + unit + " not found");
                        continue;
                    }
                    // else "return" leaf index
                    outln("unit found in leaf #" + leaf);
                    continue;
                }

                /**
                 * Erase one or more unit(s) from the cart
                 * 
                 * @param ints
                 *            unit(s) index/indices
                 * 
                 */
                if (command.equals("erase") || command.equals("e")) {
                    if (tokenizer.countTokens() == 0) {
                        outln("wrong usage. see \"help erase\" or \"help e\" for details.");
                        continue;
                    }
                    int unit;
                    // go through all parameters (should be unit indices)
                    while (tokenizer.hasMoreTokens()) {
                        // parse unit index
                        unit = Integer.parseInt(tokenizer.nextToken());

                        if (input("really delete unit " + unit + " [y/N]: ")
                                .equals("y")) {
                            // call erase helper method
                            eraseUnit(unit);
                            continue;
                        } else {
                            outln("unit not deleted.");
                            continue;
                        }
                    }
                }

                /**
                 * Play back audio of all units or outliers of current leaf
                 * 
                 * @param optional,
                 *            int leaf index, if not specified the current leaf
                 *            will be played back
                 * @param String
                 *            "all" or "cut (above/below x)" (if above/below x
                 *            is not given, use default values) allowing
                 *            playback of all units or with normal units and
                 *            outliers seperated
                 */
                if (command.equals("playleaf") || command.equals("pl")) {
                    if (tokenizer.countTokens() > 2) {
                        outln("wrong usage. see \"help playleaf\" or \"help pl\" for details.");
                        continue;
                    }

                    // extract parameters
                    // play all units:
                    String which = tokenizer.nextToken();
                    if (which.equals("all")) {
                        // get all units
                        int[] leafCs = (int[]) currLeaf.getAllData();
                        // if leaf is empty
                        if (leafCs.length == 0) {
                            outln("leaf is empty, nothing to play.");
                            continue;
                        }
                        // else call play helper method with unit indices
                        outln("rendering leaf " + currLeafIndex
                                + " containing " + leafCs.length
                                + " candidates.");
                        play(0, leafCs, true);
                        continue;

                    }// else check for user-given values
                    if (which.equals("cut")) {
                        // if user specified the parameters
                        if (tokenizer.countTokens() == 2) {
                            String where = tokenizer.nextToken();
                            if (where.equals("above")) {
                                cutWhere = 1;
                            }
                            if (where.equals("below")) {
                                cutWhere = 2;
                            }
                            if (where.equals("both")) {
                                outln("Mode \"both\" not (yet) implemented.");
                                continue;
                            }
                            float cutValue = Float.parseFloat(tokenizer
                                    .nextToken());
                            if (cutWhere == 1)
                                cutAboveValue = cutValue;
                            else
                                cutBelowValue = cutValue;
                        } else {
                            if (cutWhere == 3) {
                                outln("Mode \"both\" not (yet) implemented.");
                                continue;
                            }
                            // else do nothing since we will use global
                            // variables
                        }
                        // self-explaining
                        String withBeepS = input("Play with beep [y] or just the outliers [N] ?: ");
                        // now call the appropriate method
                        if (withBeepS.equals("y") || withBeepS.equals("yes")) {
                            playWithBeep();
                            continue;
                        } else {
                            playOutliers();
                            continue;
                        }
                    } // if mode is unknown, continue
                    outln("wrong mode. mode are either \"all\" or \"cut\"(above/below [value]).");
                    continue;
                }

                /**
                 * Display all data of a given leaf, that is: unit indeces and
                 * probability values mean and standard deviation
                 * 
                 * @param (optional)
                 *            int of leaf index, if not specified, display the
                 *            current leaf
                 * @param string:
                 *            "all" for all values, "out" for outliers only,
                 *            again specified by the global variables
                 * 
                 */
                if (command.equals("viewleaf") || command.equals("vl")) {
                    if (tokenizer.countTokens() > 2
                            || tokenizer.countTokens() == 0) {
                        outln("wrong usage. see \"help viewleaf\" or \"help vl\" for details.");
                        continue;
                    }
                    if (tokenizer.countTokens() == 2) {
                        // parse new leaf index and jump
                        int leaf = Integer.parseInt(tokenizer.nextToken());
                        if (leaf < 0 || leaf > numLeafs) {
                            outln("leaf index out of bounds");
                            continue;
                        }
                        jumpToLeaf(leaf);
                    }
                    if (tokenizer.nextToken().equals("out")) {
                        // call helper method without printwriter (null)
                        // and with option "all" false -> cut
                        displayLeaf(currLeaf, currLeafIndex, false);
                    } else {
                        // show all units must be true
                        displayLeaf(currLeaf, currLeafIndex, true);
                    }
                    continue;
                }

                /**
                 * Play back the audio of whatever was rendered last by just
                 * playing back the test.wav
                 * 
                 * no param
                 * 
                 */
                if (command.equals("replay") || command.equals("r")) {
                    if (tokenizer.hasMoreTokens()) {
                        outln("wrong usage. see \"help replay\" or \"help r\" for details.");
                        continue;
                    }
                    // get the wav file
                    String fName = "./test.wav";
                    File file = new File(fName);

                    if (!file.exists()) {
                        outln("test.wav doesn't exist (yet). render anything first.");
                        continue;
                    }
                    outln("Playing last unit(s) again.");
                    // use mary audio utils directly for this one
                    MaryAudioUtils.playWavFile(fName, 0);
                    continue;
                } // end replay

                /**
                 * Provide decision path of current leaf
                 * 
                 */
                if (command.equals("decpath") || command.equals("dp")) {
                    outln("Current decision path: "
                            + currLeaf.getDecisionPath());
                    continue;
                }

                /**
                 * command *(e)x(it)* should be self-explanatory
                 * 
                 * @param doesn't
                 *            even matter, just exit
                 */
                if (command.equals("exit") || command.equals("x"))
                    System.exit(0);

            } catch (Exception e) {
                // if an error occured catch it (do not let command line crash)
                outln("command not found or wrong usage. type \"help\" or \"help [command]\" for help file.");
                outln(e.getMessage());
                e.printStackTrace();
                continue;
            }
        } // "end" while

    } // end run()

    public void analyzeAutomatic(String logfile, String cartfile)
            throws IOException, Exception {
        // provide experimental cut scenarios for the user
        boolean cutAbove1000 = true;
        boolean cutNorm = true;
        boolean cutSilence = true;
        boolean cutLong = true;
        if (input("Cut non-silence leafs with float mean > 1000? [YES/no]: ").equals("no"))
            cutAbove1000 = false;
        if (input("Cut non-silence leafs with normal mean? [YES/no]: ").equals("no"))
            cutNorm = false;
        if (input("Cut silence leafs? [YES/no]: ").equals("no"))
            cutSilence = false;
        if (input("Cut too long units (>250ms)? [YES/no]: ").equals("no"))
            cutLong = false;
        analyzeAutomatic(logfile, cartfile, cutAbove1000, cutNorm, cutSilence, cutLong);
    }
    
    
    public void analyzeAutomatic(String logfile, String cartfile,
            boolean cutAbove1000, boolean cutNorm, boolean cutSilence, boolean cutLong)
    throws IOException, Exception {
        DecimalFormat df = new DecimalFormat("0.000");
        PrintWriter logger = new PrintWriter(new FileWriter(new File(logfile)),
                true);
        outln("Logger for logfile \"" + logfile + "\" activated.");
        logger.println("Logger for logfile \"" + logfile + "\" activated.");
        outln("There are " + numLeafs + " leafs and " + numUnits + " units.");
        logger.println("There are " + numLeafs + " leafs and " + numUnits
                + " units.");

        // take the current time
        long time1 = System.currentTimeMillis();
        outln("Automatic analysis for cart activated...");
        logger.println("Automatic analysis for cart activated...");
        outln("Starting algorithm...");

        // jump to first leaf
        currLeaf = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
        currLeafIndex = 1;
        logger.println("jumped to first leaf.");

        // count how many units in how many leaves are cut during the process
        int totalCutUnits = 0;
        int numCutLeaves = 0;

        while (currLeaf != null) {
            logger.print("Now processing leaf " + currLeafIndex + " of "+numLeafs+"... ");
            percent = (int)((currLeafIndex/(float)numLeafs)*100);
            // now, as always, leave out the unusable leafs
            int numU = currLeaf.getNumberOfData();
            int numCutU = 0;
            if (numU < 2) {
                logger.println("0 or 1 unit(s)... skipping.");
                logger.println("");
                currLeafIndex++;
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf.getNextLeafNode();
                continue;
            }

            logger.print("found " + numU + " units... ");

            float[] floats = currLeaf.getFloatData();
            int[] indices = (int[]) currLeaf.getAllData();
            assert indices.length == numU;
            
            logger.print("retrieved data... ");
            float mean = MaryUtils.mean(floats);
            double sd = MaryUtils.stdDev(floats);
            if (sd == 0) {
                logger.println("sd zero. skipping.");
                logger.println("");
                currLeafIndex++;
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf.getNextLeafNode();
                continue;
            }

            // 1) NO silence
            if (currLeaf.getDecisionPath().indexOf("mary_phoneme==_") == -1) {
                if (mean > 1000 && cutAbove1000
                        || mean <= 1000 && cutNorm) {
                    int cut = eraseUnitsFromLeaf(true,
                            currLeaf, null, 50000, logger, true, true);
                    numCutU += cut;
                    totalCutUnits += cut;
                }
                if (cutLong) {
                    indices = (int[]) currLeaf.getAllData();
                    float sampleRate = ufr.getSampleRate();
                    float[] durations = new float[indices.length];
                    for (int i=0; i<indices.length; i++) {
                        durations[i] = ufr.getUnit(indices[i]).getDuration() / sampleRate;
                    }
                    float meanDur = MaryUtils.mean(durations);
                    double sdDur = MaryUtils.stdDev(durations);
                    //System.out.println("Leaf "+currLeafIndex+ " " + currLeaf.getDecisionPath());
                    //double durationThreshold = meanDur + 2 * sdDur;
                    double durationThreshold = 0.2; // no halfphone should be longer than 200 ms
                    //System.out.println("mean duration = "+ meanDur + " s; sd = "+sdDur+" s; threshold = "+durationThreshold+" s");
                    for (int i=0; i<indices.length; i++) {
                        if (durations[i] > durationThreshold) {
                            //System.out.println(i+" cut: dur="+durations[i]+" s");
                            ((IntAndFloatArrayLeafNode) currLeaf).eraseData(indices[i]);
                            numCutU++;
                            totalCutUnits++;
                        } else {
                            //System.out.println("                   uncut: "+i+", dur="+durations[i]+" s ");
                        }
                    }
                }
            }
            // 2) Silence
            else {
                if (cutSilence) {
                    // Energy mode here
                    logger.println("Silence detected! Computing energy levels...");
                    Datagram[][] data = new Datagram[numU][];
                    int nDatagrams = 0;
                    for (int i = 0; i < numU; i++) {
                        data[i] = tlr.getDatagrams(ufr.getUnit(indices[i]), ufr.getSampleRate());
                        nDatagrams += data[i].length;
                        // outln("data["+i+"].length = "+data[i].length);
                    }
                    Datagram[] allDatagrams = new Datagram[nDatagrams];
                    for (int i = 0, pos = 0; i < numU; pos += data[i].length, i++) {
                        System.arraycopy(data[i], 0, allDatagrams, pos, data[i].length);
                    }
                    double[] audioData = new DatagramDoubleDataSource(allDatagrams).getAllData();
                    EnergyAnalyser_dB allUnitsAnalyser = new EnergyAnalyser_dB(
                            new DatagramDoubleDataSource(allDatagrams), 128, tlr.getSampleRate());
                    allUnitsAnalyser.analyseAllFrames();
                    // EnergyHistogram eh = new EnergyHistogram(audioData,
                    // tlr.getSampleRate());
                    // eh.showInJFrame("Energy histogram for leaf "+currLeafIndex,
                    // false, false);
                    double meanUnitsEnergy = allUnitsAnalyser.getMeanFrameEnergy();
                    double silence = allUnitsAnalyser.getSilenceCutoff();

                    logger.println("Silence cutoff: " + silence
                            + "; meanUnitsEnergy: " + meanUnitsEnergy);

                    int counter1 = 0;

                    for (int i = 0; i < numU; i++) {
                        EnergyAnalyser_dB oneUnitAnalyser = new EnergyAnalyser_dB(
                                new DatagramDoubleDataSource(data[i]), 128, tlr.getSampleRate());
                        oneUnitAnalyser.analyseAllFrames();
                        double meanUnitEnergy = oneUnitAnalyser.getMeanFrameEnergy();
                        // actual cutting happens here
                        if (meanUnitEnergy > silence) {
                            ((IntAndFloatArrayLeafNode) currLeaf).eraseData(indices[i]);
                            numCutU++;
                            totalCutUnits++;
                        }
                    }
                }
            }
            logger.println("Out of the " + numU + " units of the leaf, "
                    + numCutU + " were cut. ("
                    + (float) ((((float) numCutU) / (float) numU) * 100.f) + "%)");
            if (numCutU > 0)
                numCutLeaves++;                        

            // DEBUG
            float[] floatsAfter = currLeaf.getFloatData();
            logger.println("after cutting: new mean = " + MaryUtils.mean(floatsAfter)
                    + "; new sd = " + MaryUtils.stdDev(floatsAfter));
            // END DEBUG
            logger.println("");
            // next leaf
            currLeaf = (IntAndFloatArrayLeafNode) currLeaf.getNextLeafNode();
            currLeafIndex++;
        } // end while loop

        // after all is done, reset cart
        currLeaf = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
        currLeafIndex = 1;

        // show the statistics B07-style
        float percentageUnits = (float) ((float) totalCutUnits / (float) numUnits) * 100.f;
        float percentageLeaves = (float) ((float) numCutLeaves)
                / (float) numLeafs * 100.f;
        outln("Total number of cut units: " + totalCutUnits + " ("
                + percentageUnits + "% of total (" + numUnits
                + ") number of units).");
        logger.println("Total number of cut units: " + totalCutUnits + " ("
                + percentageUnits + "% of total (" + numUnits
                + ") number of units).");
        outln("Total number of leaves pruned: " + numCutLeaves + " ("
                + percentageLeaves + "% of total (" + numLeafs
                + ") number of leaves).");
        logger.println("Total number of leaves pruned: " + numCutLeaves + " ("
                + percentageLeaves + "% of total (" + numLeafs
                + ") number of leaves).");
        outln("Analysis/ search and destroy all done. Tree reset.");
        logger.println("That's it. All sought and destroyed. Tree reset.");
        // measure the time
        long time2 = System.currentTimeMillis();

        outln("Algorithm took " + (time2 - time1)
                + " ms. For details: results have been logged in " + logfile);
        logger.println("Algorithm took " + (time2 - time1) + " ms. Exiting...");
        logger.close();

        // very last step for all sub-methods: save and dump cart if cartfile is
        // given
        if (cartfile != null) {
            saveCart(cartfile + ".ca");
            dumpCart(cartfile + ".mry");
            outln("MARY cart dumped in " + cartfile + ".mry");
            outln("Backup CA cart dumped in " + cartfile + ".ca");
        }
    }

    public void analyzeInteractive(String logfile, String cartfile)
            throws IOException, FileNotFoundException, Exception {
        DecimalFormat df = new DecimalFormat("0.000");
        PrintWriter logger = new PrintWriter(new FileWriter(new File(logfile)),
                true);
        outln("Logger for logfile \"" + logfile + "\" activated.");
        logger.println("Logger for logfile \"" + logfile + "\" activated.");
        outln("There are " + numLeafs + " leafs and " + numUnits + " units.");
        logger.println("There are " + numLeafs + " leafs and " + numUnits
                + " units.");

        outln("Welcome to the interactive cart analysis.");
        logger.println("Started interactive analysis...");
        String filename = "./resume.dat";
        File resFile = new File(filename);

        // this part allows for resuming earlier work
        if (resFile.exists()) {
            String temp = "";
            if (!logfile.equals("resume")) {
                temp = input("Resume from last time [y/N]: ");
            }
            if (temp.equals("y") || (temp.equals("yes"))
                    || logfile.equals("resume")) {
                logger.println("user requested resume from " + filename);
                String newCartFileName = "./resume.cart";
                // load the tree
                ctree = loadTreeFromFile(newCartFileName, feaDef);
                outln("cart successfully loaded, containing in its leaves "
                        + numUnits + " units.");
                logger
                        .println("cart read successfully, containing in its leaves "
                                + numUnits + " units.");
                // read the resume.dat file
                DataInputStream dis = null;
                dis = new DataInputStream(new BufferedInputStream(
                        new FileInputStream(filename)));
                // read logfile and cartfile
                logfile = dis.readUTF();
                if (logfile.equals("null"))
                    logfile = "resume.log";
                cartfile = dis.readUTF();
                if (cartfile.equals("null"))
                    cartfile = null;
                // read new current leaf index
                int leafLoaded = dis.readInt();
                if (leafLoaded < 1 || leafLoaded > numLeafs) {
                    outln("error: leaf index out of bounds, starting from first leaf...");
                    logger.println("error resuming from " + filename
                            + ", starting from first leaf");
                    outln("Jumping to first leaf...");
                    // tree "reset", start from first leaf
                    if (currLeafIndex != 1) {
                        currLeaf = (IntAndFloatArrayLeafNode) ctree
                                .getFirstLeafNode();
                        currLeafIndex = 1;
                    }
                    logger.println("Jumped to first leaf...");
                    dis.close();
                } else {
                    jumpToLeaf(leafLoaded);
                    // get the global variables from the file
                    cutAboveValue = dis.readFloat();
                    outln("file read successfully, set cut above value to "
                            + cutAboveValue);
                    logger
                            .println("file read successfully, set abs cut-off value to "
                                    + cutAboveValue);
                    cutBelowValue = dis.readFloat();
                    outln("file read successfully, set cut above value to "
                            + cutBelowValue);
                    logger
                            .println("file read successfully, set abs cut-off value to "
                                    + cutBelowValue);
                    cutWhere = dis.readByte();
                    outln("set cut mode to " + cutWhereS[cutWhere]);
                    logger.println("set cut mode to " + cutWhereS[cutWhere]
                            + " (" + cutWhere + ")");
                    dis.close();
                }
            } else {
                outln("Jumping to first leaf...");
                // tree "reset", start from first leaf
                if (currLeafIndex != 1) {
                    currLeaf = (LeafNode.IntAndFloatArrayLeafNode) ctree
                            .getFirstLeafNode();
                    currLeafIndex = 1;
                }
                logger.println("Jumped to first leaf...");
            }
        } else {
            outln("Jumping to first leaf...");
            // tree "reset", start from first leaf
            if (currLeafIndex != 1) {
                currLeaf = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
                currLeafIndex = 1;
            }
            logger.println("Jumped to first leaf...");
        }
        // done with resuming/initialising

        // store index of last leaf for navigational purposes
        int lastLeaf = 0;

        // start while loop, going through all the leafs of the cart
        while (currLeaf != null) {

            out("Now processing leaf " + currLeafIndex + "... ");
            logger.print("Now processing leaf " + currLeafIndex + "... ");
            // skip unusable leafs
            int numU = currLeaf.getNumberOfData();
            if (numU < 2) {
                outln("0 or 1 unit(s)... skipping.");
                logger.println("0 or 1 unit(s)... skipping.");
                currLeafIndex++;
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                        .getNextLeafNode();
                continue;
            }
            out("found " + numU + " units... ");
            logger.print("found " + numU + " units... ");

            float[] floats = currLeaf.getFloatData();
            int[] indices = (int[]) currLeaf.getAllData();
            out("retrieved data... ");
            logger.print("retrieved data... ");
            float mean = MaryUtils.mean(floats);
            double sd = MaryUtils.stdDev(floats);
            if (sd == 0) {
                outln("sd zero. skipping.");
                logger.println("sd zero. skipping.");
                currLeafIndex++;
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                        .getNextLeafNode();
                continue;
            } else {
                if (sd == Double.POSITIVE_INFINITY) {
                    outln("sd = Infinity! with mean = " + mean + ".");
                    logger.println("sd = Infinity! with mean = " + mean + ".");
                } else {
                    outln("sd != 0: mean = " + df.format(mean) + "; sd = "
                            + df.format(sd));
                    logger.println("sd != 0: mean = " + mean + "; sd = " + sd);
                }
            }

            // start a command line for the leaf if leaf is usable
            while (true) {
                outln("");
                outln("Current values: cut mode = " + cutWhereS[cutWhere]
                        + "; cutAboveValue = " + cutAboveValue
                        + "; cutBelowValue = " + cutBelowValue);
                outln("Current decision path: " + currLeaf.getDecisionPath());
                outln("Enter \"cut\" to cut outliers, \"view\" to view the leaf and \"play\" to play it back. use \"set\" to change the values.");
                outln("Note that without parameters, the current values will be used, else use command CUTMODE VALUE1 (VALUE2 for mode=both)");
                outln("where VALUE1 can be \"all\" to view or play all. Use \"next\", \"last\" and \"jump\" for navigation,");
                outln("use \"save\" to save your current process and \"exit\" to return to main command line.");
                outln("");
                try {
                    // wrap a tokenizer around command line
                    StringTokenizer tokenizer = new StringTokenizer(input(
                            "ca:aa:cL:" + currLeafIndex + " :> ").toLowerCase());
                    if (tokenizer.hasMoreTokens()) {
                        String inputS = tokenizer.nextToken();

                        // NEW: skip to next silence!
                        if (inputS.equals("ns") || inputS.equals("silence")) {
                            // navigate forward through cart until silence is
                            // found
                            boolean foundS = false;
                            currLeafIndex++;
                            currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                                    .getNextLeafNode();
                            while (currLeaf != null) {

                                if (currLeaf.getDecisionPath().indexOf(
                                        "mary_phoneme==_") != -1
                                        && currLeaf.getNumberOfData() >= 2) {
                                    outln("silence found!");
                                    foundS = true;
                                    break;
                                }

                                currLeafIndex++;
                                currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                                        .getNextLeafNode();
                            }
                            if (!foundS) {
                                currLeafIndex = 1;
                                currLeaf = (IntAndFloatArrayLeafNode) ctree
                                        .getFirstLeafNode();
                            }
                            continue;
                        }

                        if (inputS.equals("energy")) {
                            int len = currLeaf.getNumberOfData();
                            Datagram[][] data = new Datagram[len][];
                            int nDatagrams = 0;
                            for (int i = 0; i < len; i++) {
                                data[i] = tlr.getDatagrams(ufr
                                        .getUnit(indices[i]), ufr
                                        .getSampleRate());
                                nDatagrams += data[i].length;
                                // outln("data["+i+"].length =
                                // "+data[i].length);
                            }
                            Datagram[] allDatagrams = new Datagram[nDatagrams];
                            for (int i = 0, pos = 0; i < len; pos += data[i].length, i++) {
                                System.arraycopy(data[i], 0, allDatagrams, pos,
                                        data[i].length);
                            }
                            double[] audioData = new DatagramDoubleDataSource(
                                    allDatagrams).getAllData();
                            EnergyAnalyser_dB allUnitsAnalyser = new EnergyAnalyser_dB(
                                    new DatagramDoubleDataSource(allDatagrams),
                                    128, tlr.getSampleRate());
                            allUnitsAnalyser.analyseAllFrames();
                            EnergyHistogram eh = new EnergyHistogram(audioData,
                                    tlr.getSampleRate());
                            eh.showInJFrame("Energy histogram for leaf "
                                    + currLeafIndex, false, false);
                            double meanUnitsEnergy = allUnitsAnalyser
                                    .getMeanFrameEnergy();
                            double silence = allUnitsAnalyser
                                    .getSilenceCutoff();

                            outln("Silence cutoff: " + silence
                                    + "; meanUnitsEnergy: " + meanUnitsEnergy);
                            input("Press [Enter] for unit energy analysis ...");

                            int counter1 = 0;

                            for (int i = 0; i < len; i++) {
                                EnergyAnalyser_dB oneUnitAnalyser = new EnergyAnalyser_dB(
                                        new DatagramDoubleDataSource(data[i]),
                                        128, tlr.getSampleRate());
                                oneUnitAnalyser.analyseAllFrames();
                                double meanUnitEnergy = oneUnitAnalyser
                                        .getMeanFrameEnergy();
                                // outln("Unit "+i+" mean energy:
                                // "+meanUnitEnergy);
                                if (meanUnitEnergy > meanUnitsEnergy) {
                                    counter1++;
                                }
                            }

                            input("[Enter] for playback...");

                            int[] above = new int[counter1];
                            int[] below = new int[len - counter1];
                            int aI = 0;
                            int bI = 0;

                            for (int i = 0; i < len; i++) {
                                EnergyAnalyser_dB oneUnitAnalyser = new EnergyAnalyser_dB(
                                        new DatagramDoubleDataSource(data[i]),
                                        128, tlr.getSampleRate());
                                oneUnitAnalyser.analyseAllFrames();
                                double meanUnitEnergy = oneUnitAnalyser
                                        .getMeanFrameEnergy();
                                if (meanUnitEnergy > meanUnitsEnergy) {
                                    above[aI] = indices[i];
                                    aI++;
                                } else {
                                    below[bI] = indices[i];
                                    bI++;
                                }
                            }

                            playWithBeep(below, above);

                            continue;
                        }

                        // first check for the 3 main commands

                        // first off, view
                        if (inputS.equals("view") || inputS.equals("v")) {
                            // adjust the settings
                            if (tokenizer.hasMoreTokens()) {
                                String viewMode = tokenizer.nextToken();
                                if (viewMode.equals("above")) {
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 1;
                                }
                                if (viewMode.equals("below")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 2;
                                }
                                if (viewMode.equals("both")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 3;
                                }
                                // in the special case of viewing all units just
                                // execute the proper command (true)
                                if (viewMode.equals("all")) {
                                    displayLeaf(currLeaf, currLeafIndex, true);
                                    continue;
                                }
                            }
                            // now use current values to actually view the
                            // outliers
                            // (displayLeaf already uses global vars, so no need
                            // for adjusting,
                            // just set boolean all = false)
                            displayLeaf(currLeaf, currLeafIndex, false);
                            continue;
                        }

                        // secondly, play
                        if (inputS.equals("play") || inputS.equals("p")) {
                            // adjust the settings
                            if (tokenizer.hasMoreTokens()) {
                                String playMode = tokenizer.nextToken();
                                if (playMode.equals("above")) {
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 1;
                                }
                                if (playMode.equals("below")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 2;
                                }
                                if (playMode.equals("both")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 3;
                                }
                                // in the special case of playing all units just
                                // execute the proper command
                                // loop = 0, int[] units = intData(), concat =
                                // true
                                if (playMode.equals("all")) {
                                    play(0, (int[]) currLeaf.getAllData(), true);
                                    continue;
                                }
                            }
                            // now use current values to actually play the
                            // outliers
                            // but before that ask whether <normal> <beep>
                            // <outliers> should be used
                            // or only outliers
                            String beep = input(
                                    "Long version with beep [yes] or just outliers [NO]? ")
                                    .toLowerCase();
                            if (beep.equals("yes") || beep.equals("y")) {
                                playWithBeep();
                            } else {
                                playOutliers();
                            }
                            continue;
                        }

                        // finally, cut
                        if (inputS.equals("cut") || inputS.equals("c")) {
                            int erasedUnits = 0;
                            // adjust the settings
                            if (tokenizer.hasMoreTokens()) {
                                String cutMode = tokenizer.nextToken();
                                if (cutMode.equals("above")) {
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 1;
                                }
                                if (cutMode.equals("below")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 2;
                                }
                                if (cutMode.equals("both")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 3;
                                }
                            }
                            // now use current values to actually cut the
                            // outliers
                            switch (cutWhere) {
                            // above: use cutAboveValue and second true as it
                            // says "cut above",
                            // third true means auto mode.
                            case 1: {
                                erasedUnits = eraseUnitsFromLeaf(true,
                                        currLeaf, null, cutAboveValue, logger,
                                        true, true);
                                break;
                            }
                                // below: use cutBelowValue and false for "don't
                                // cut above = cut below"
                            case 2: {
                                erasedUnits = eraseUnitsFromLeaf(true,
                                        currLeaf, null, cutBelowValue, logger,
                                        false, true);
                                break;
                            }
                                // both: the easiest part of it all: just do
                                // both cutting operations
                            case 3: {
                                erasedUnits = eraseUnitsFromLeaf(true,
                                        currLeaf, null, cutAboveValue, logger,
                                        true, true);
                                erasedUnits += eraseUnitsFromLeaf(true,
                                        currLeaf, null, cutBelowValue, logger,
                                        false, true);
                                break;
                            }
                            }
                            outln("Successfully erased " + erasedUnits
                                    + " units from leaf.");
                            continue;
                        }

                        // navigation
                        // last leaf: use lastLeaf to jump to last usable leaf
                        // and store this leaf
                        // as "new" last leaf
                        if (inputS.equals("last") || inputS.equals("l")) {
                            int temp = currLeafIndex;
                            outln("Going to last leaf...");
                            jumpToLeaf(lastLeaf);
                            lastLeaf = temp;
                            break;
                        }

                        // next leaf: just break out of the while(true) loop
                        if (inputS.equals("next") || inputS.equals("n")
                                || inputS.equals("skip") || inputS.equals("s")) {
                            // store leaf as last usable leaf
                            lastLeaf = currLeafIndex;
                            // go to next leaf (if possible)
                            currLeafIndex++;
                            currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                                    .getNextLeafNode();
                            outln("Going to next leaf...");
                            break;
                        }

                        // provide a jump method just like in main command line
                        if (inputS.equals("jump") || inputS.equals("j")) {
                            if (tokenizer.hasMoreTokens()) {
                                int i = Integer.parseInt(tokenizer.nextToken());
                                jumpToLeaf(i);
                            } else {
                                String s = input("Enter leaf index: ");
                                int i = Integer.parseInt(s);
                                jumpToLeaf(i);
                            }
                            break;
                        }

                        // set the global variables
                        if (inputS.equals("set") || inputS.equals("s")) {
                            // if there are more tokens, adjust the settings
                            if (tokenizer.hasMoreTokens()) {
                                String cutMode = tokenizer.nextToken();
                                if (cutMode.equals("above")) {
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 1;
                                }
                                if (cutMode.equals("below")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 2;
                                }
                                if (cutMode.equals("both")) {
                                    cutBelowValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutAboveValue = Float.parseFloat(tokenizer
                                            .nextToken());
                                    cutWhere = 3;
                                }
                            }
                            // else ask interactively
                            else {
                                // just to be sure, use another while(true) loop
                                // here
                                // until user gets input right
                                while (true) {
                                    String setMode = input(
                                            "Choose cut mode to set [above,below,both]: ")
                                            .toLowerCase();
                                    if (setMode.equals("above")) {
                                        String aboveString = input("Enter float value for cutAboveValue: ");
                                        cutAboveValue = Float
                                                .parseFloat(aboveString);
                                        cutWhere = 1;
                                        break;
                                    }
                                    if (setMode.equals("below")) {
                                        String belowString = input("Enter float value for cutBelowValue: ");
                                        cutBelowValue = Float
                                                .parseFloat(belowString);
                                        cutWhere = 2;
                                        break;
                                    }
                                    if (setMode.equals("both")) {
                                        String belowString = input("Enter float value for cutBelowValue: ");
                                        cutBelowValue = Float
                                                .parseFloat(belowString);
                                        String aboveString = input("Enter float value for cutAboveValue: ");
                                        cutAboveValue = Float
                                                .parseFloat(aboveString);
                                        cutWhere = 3;
                                        break;
                                    }
                                    // on wrong user input, keep on asking
                                    continue;
                                }
                            }
                        }

                        // exit and save command, save all work
                        if (inputS.equals("exit") || inputS.equals("x")
                                || inputS.equals("save")) {
                            boolean x = (inputS.equals("exit") || inputS
                                    .equals("x"));
                            if (x) {
                                outln("user exit");
                                logger.println("user aborted process");
                            } else {
                                outln("saving...");
                                logger.println("user requests save");
                            }

                            /*
                             * save work for future resuming DataOutputStream
                             * dos = null; dos = new DataOutputStream(new
                             * BufferedOutputStream(new
                             * FileOutputStream(filename))); // save logfile and
                             * cartfile if (logfile == null){
                             * dos.writeUTF("null"); }else{
                             * dos.writeUTF(logfile); } if (cartfile == null){
                             * dos.writeUTF("null"); }else{
                             * dos.writeUTF(cartfile); } // save leaf index
                             * dos.writeInt(currLeafIndex); // save current cut
                             * above value dos.writeFloat(cutAboveValue); //
                             * save current cut below value
                             * dos.writeFloat(cutBelowValue); // save cut mode
                             * dos.writeByte(cutWhere); // close file
                             * dos.close(); // dump (modified/current) cart
                             * String dumpCartFile = "./resume.cart";
                             * saveCart(dumpCartFile); outln("data saved. next
                             * time you will be asked if you want to resume from
                             * here."); logger.println("data saved for future
                             * resume option");
                             */
                            if (x) {
                                currLeaf = (IntAndFloatArrayLeafNode) ctree
                                        .getFirstLeafNode();
                                currLeafIndex = 1;
                                outln("tree reset, all done.");
                                logger.println("tree reset, all done.");
                                logger.close();
                                return;
                            } else {
                                continue;
                            }
                        }
                    } else {
                        outln("Command empty. Try again.");
                        continue;
                    }
                } catch (Exception e) {
                    outln("command not found. try again.");
                    continue;
                } // end try/catch
            }// end while(true) (command line)

        }// end while (loop through all leafs)

        // another "reset" to finish this, end of (first) sub-method
        currLeaf = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
        currLeafIndex = 1;
        outln("tree reset, all done.");
        logger.println("tree reset, all done.");
        logger.close();

        // very last step for all sub-methods: save and dump cart if cartfile is
        // given
        if (cartfile != null) {
            saveCart(cartfile + ".ca");
            dumpCart(cartfile + ".mry");
            outln("MARY cart dumped in " + cartfile + ".mry");
            outln("Backup CA cart dumped in " + cartfile + ".ca");
        }
    }

    /**
     * Play any given unit(s) in wav format, used mostly as helper method
     * 
     * @param loop
     *            how often the sound should be looped
     * @param units
     *            int array of the unit indices
     * @param concat
     *            boolean of whether or not the units should be concatenated or
     *            played one after another
     * 
     * @throws IOException
     * 
     */
    private void play(int loop, int[] units, boolean concat) throws IOException {

        // create a ByteArrayOutputStream which will then be streamed
        // into a ByteArray that constitutes the wav data
        ByteArrayOutputStream bbis = new ByteArrayOutputStream();
        // get number of units
        int ulength = units.length;

        // default play back method: concatenate all units from the first to the
        // last one
        // in the array
        if (concat) {
            for (int j = 0; j < ulength; j++) {
                // Concatenate the datagrams from the instances, one unit after
                // another
                Datagram[] dat = tlr.getDatagrams(ufr.getUnit(units[j]), ufr
                        .getSampleRate());
                for (int k = 0; k < dat.length; k++) {
                    // write the data in the stream
                    bbis.write(dat[k].getData());
                }
            }
            // Get the bytes as an array
            byte[] buf = bbis.toByteArray();
            // Output the wav file
            String fName = ("./test.wav");
            outln("Outputting file [" + fName + "]...");
            // use the wavwriter to do the output for us
            ww.export(fName, 16000, buf);
            // finally, play back the file, according to parameter loop
            outln("Playing wav looped " + (loop + 1) + " times... [" + fName
                    + "]");
            outln("Playing " + ulength + " units, first unit: " + units[0]
                    + ", last unit: " + units[ulength - 1]);
            for (int i = 0; i <= loop; i++) {
                MaryAudioUtils.playWavFile(fName, 0);
                // allow for user control if loop > 0
                if (i < loop)
                    input("Press Enter for next loop...");
            }
        }

        // not so default mode: play one unit after the other, as you can see,
        // it's just
        // the loops that are exchanged and the code is moved appropriately
        else {
            for (int i = 0; i < ulength; i++) {
                // Concatenate the datagrams from the instances
                Datagram[] dat = tlr.getDatagrams(ufr.getUnit(units[i]), ufr
                        .getSampleRate());
                for (int k = 0; k < dat.length; k++) {
                    bbis.write(dat[k].getData());
                }

                // Get the bytes as an array
                byte[] buf = bbis.toByteArray();
                // Output the wav file
                String fName = ("./test.wav");
                outln("Outputting file [" + fName + "]...");
                ww.export(fName, 16000, buf);
                outln("Playing wav looped " + loop + " times... [" + fName
                        + "]");
                outln("Now playing unit " + units[i]);
                for (int j = 0; j <= loop; j++) {
                    MaryAudioUtils.playWavFile(fName, 0);
                }
                // allow for some user control, else it would be chaos
                if (i < (ulength - 1))
                    input("Press Enter for next unit...");
            }

        }

    }

    /**
     * Render and play units of current leaf, first those with "normal" value,
     * then a beep and then the outliers, using the globally set variables so
     * it's: <normal units> <beep> <possible outliers>
     * 
     * @return hopefully a nice sounding test.wav
     * 
     * @throws IOException
     * 
     */
    private void playWithBeep() throws IOException {

        outln("Now concatenating leaf " + currLeafIndex + " with cutmode \""
                + cutWhereS[cutWhere] + "\"");
        // get data from leaf
        float[] pvalues = currLeaf.getFloatData();
        int[] indices = (int[]) currLeaf.getAllData();

        // divide the indices array according to the probability values array
        // and the given cut mode

        // those arrays contain the unit indices of those units below and above
        // the respective global values
        int[] normal = null;
        int[] outliers = null;

        // use Vectors for dynamic storing
        Vector normalV = new Vector();
        Vector outliersV = new Vector();

        switch (cutWhere) {
        // mode: above
        case 1: {
            for (int i = 0; i < pvalues.length; i++) {
                if (pvalues[i] > cutAboveValue)
                    outliersV.addElement(new Integer(indices[i]));
                else
                    normalV.addElement(new Integer(indices[i]));
            }
            break;
        }
            // mode: below
        case 2: {
            for (int i = 0; i < pvalues.length; i++) {
                if (pvalues[i] < cutBelowValue)
                    outliersV.addElement(new Integer(indices[i]));
                else
                    normalV.addElement(new Integer(indices[i]));
            }
            break;
        }
            // mode: both
        case 3: {
            for (int i = 0; i < pvalues.length; i++) {
                if (pvalues[i] < cutBelowValue || pvalues[i] > cutAboveValue)
                    outliersV.addElement(new Integer(indices[i]));
                else
                    normalV.addElement(new Integer(indices[i]));
            }
            break;
        }
        }
        // convert Vectors back to the arrays
        if (!outliersV.isEmpty()) {
            outliers = new int[outliersV.size()];
            for (int i = 0; i < outliersV.size(); i++) {
                outliers[i] = ((Integer) (outliersV.elementAt(i))).intValue();
            }
        }
        if (!normalV.isEmpty()) {
            normal = new int[normalV.size()];
            for (int i = 0; i < normalV.size(); i++) {
                normal[i] = ((Integer) (normalV.elementAt(i))).intValue();
            }
        }
        // now we have the units seperated, now we need to sort them by p value
        // according to cut mode

        int outlen = outliers.length;
        int normlen = normal.length;

        int[] normalNew;
        int[] outlierNew;
        // only mode 1 and 2 really need sorting (below AND above...?)
        if (cutWhere < 3) {
            // to sort by probability value instead of unit index, use a trick
            // it's basically just like a map
            String[] outlierS = new String[outlen];
            String[] normalS = new String[normlen];

            for (int i = 0; i < outlen; i++) {
                int j;
                for (j = 0; j < indices.length; j++) {
                    if (indices[j] == outliers[i])
                        break;
                }
                outlierS[i] = pvalues[j] + "&" + outliers[i];
            }
            for (int i = 0; i < normlen; i++) {
                int j;
                for (j = 0; j < indices.length; j++) {
                    if (indices[j] == normal[i])
                        break;
                }
                normalS[i] = pvalues[j] + "&" + normal[i];
            }
            // use Arrays sort algorithm, which is enough for mode 1 (ascending)
            Arrays.sort(outlierS);
            Arrays.sort(normalS);
            // for below mode, a descending order would be nice
            // -> revert the arrays
            if (cutWhere == 2) {
                String temp;
                for (int i = 0; i < (outlierS.length / 2); i++) {
                    temp = outlierS[i];
                    outlierS[i] = outlierS[outlierS.length - i - 1];
                    outlierS[outlierS.length - i - 1] = temp;
                }
                for (int i = 0; i < (normalS.length / 2); i++) {
                    temp = normalS[i];
                    normalS[i] = normalS[normalS.length - i - 1];
                    normalS[normalS.length - i - 1] = temp;
                }
                // arrays are now reversed

            }
            // now, after sorting, put units back in the arrays (don't need the
            // values anymore)
            normalNew = new int[normlen];
            outlierNew = new int[outlen];

            for (int i = 0; i < normlen; i++) {
                StringTokenizer st = new StringTokenizer(normalS[i], "&");
                // discard p value
                st.nextToken();
                // extract unit
                normalNew[i] = (Integer.parseInt(st.nextToken()));
            }
            for (int i = 0; i < outlen; i++) {
                StringTokenizer st = new StringTokenizer(outlierS[i], "&");
                st.nextToken();
                outlierNew[i] = (Integer.parseInt(st.nextToken()));
            }
        }// else if mode 3, just copy and don't care about order
        else {
            outlierNew = new int[outliers.length];
            normalNew = new int[normal.length];
            System.arraycopy(outliers, 0, outlierNew, 0, outliers.length);
            System.arraycopy(normal, 0, normalNew, 0, normal.length);
        }

        // create the ByteArray and its OutputStream
        ByteArrayOutputStream bbis = new ByteArrayOutputStream();

        // insert normal values
        for (int i = 0; i < normlen; i++) {
            // Concatenate the normal units
            Datagram[] dat = tlr.getDatagrams(ufr.getUnit(normalNew[i]), ufr
                    .getSampleRate());
            for (int k = 0; k < dat.length; k++) {
                bbis.write(dat[k].getData());
            }
        }

        float sampleRate = 16000.f;
        // one second of sound
        int sampLength = 16000;
        int bytesPerSamp = 2;

        // insert silence (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++)
            bbis.write(0);

        // insert beep (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++) {
            double time = i / sampleRate;
            double freq = 880.0; // frequency
            double sinValue = Math.sin(2 * Math.PI * freq * time);
            sinValue *= 0.001; // lower the amplitude, otherwise it's just too
                                // loud
            bbis.write((short) (16000 * sinValue));
        }// end for loop

        // insert silence (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++)
            bbis.write(0);

        // insert outliers
        for (int j = 0; j < outlen; j++) {
            // Concatenate the datagrams from the instances
            Datagram[] dat = tlr.getDatagrams(ufr.getUnit(outlierNew[j]), ufr
                    .getSampleRate());
            for (int k = 0; k < dat.length; k++) {
                bbis.write(dat[k].getData());
            }
        }

        // Get the bytes as an array
        byte[] buf = bbis.toByteArray();
        // Output the wav file
        String fName = ("./test.wav");
        outln("Outputting file [" + fName + "] with " + normalNew.length
                + " normal values and " + outlierNew.length + " outliers...");
        ww.export(fName, 16000, buf);
        outln("Playing...");
        MaryAudioUtils.playWavFile(fName, 0);

    }

    private void playWithBeep(int[] normalNew, int[] outlierNew)
            throws IOException {

        // create the ByteArray and its OutputStream
        ByteArrayOutputStream bbis = new ByteArrayOutputStream();

        // insert normal values
        for (int i = 0; i < normalNew.length; i++) {
            // Concatenate the normal units
            Datagram[] dat = tlr.getDatagrams(ufr.getUnit(normalNew[i]), ufr
                    .getSampleRate());
            for (int k = 0; k < dat.length; k++) {
                bbis.write(dat[k].getData());
            }
        }

        float sampleRate = 16000.f;
        // one second of sound
        int sampLength = 16000;
        int bytesPerSamp = 2;

        // insert silence (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++)
            bbis.write(0);

        // insert beep (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++) {
            double time = i / sampleRate;
            double freq = 880.0; // frequency
            double sinValue = Math.sin(2 * Math.PI * freq * time);
            sinValue *= 0.001; // lower the amplitude, otherwise it's just too
                                // loud
            bbis.write((short) (16000 * sinValue));
        }// end for loop

        // insert silence (0.2 sec)
        for (int i = 0; i < 0.2 * sampLength; i++)
            bbis.write(0);

        // insert outliers
        for (int j = 0; j < outlierNew.length; j++) {
            // Concatenate the datagrams from the instances
            Datagram[] dat = tlr.getDatagrams(ufr.getUnit(outlierNew[j]), ufr
                    .getSampleRate());
            for (int k = 0; k < dat.length; k++) {
                bbis.write(dat[k].getData());
            }
        }

        // Get the bytes as an array
        byte[] buf = bbis.toByteArray();
        // Output the wav file
        String fName = ("./test.wav");
        outln("Outputting file [" + fName + "] with " + normalNew.length
                + " below values and " + outlierNew.length + " above...");
        ww.export(fName, 16000, buf);
        outln("Playing...");
        MaryAudioUtils.playWavFile(fName, 0);
    }

    /**
     * Render and play outliers of current leaf
     * 
     * @return hopefully a nice sounding test.wav
     * 
     * @throws IOException
     * 
     */
    private void playOutliers() throws IOException {
        outln("Now concatenating outliers of leaf " + currLeafIndex
                + " with cut mode " + cutWhereS[cutWhere]);

        // get data from leaf
        float[] pvalues = currLeaf.getFloatData();
        int[] indices = (int[]) currLeaf.getAllData();

        // divide the indices array according to the pvalues array and the given
        // cut mode and value
        int[] outliers = null;
        Vector outliersV = new Vector();

        switch (cutWhere) {
        // above
        case 1: {
            for (int i = 0; i < pvalues.length; i++)
                if (pvalues[i] > cutAboveValue)
                    outliersV.addElement(new Integer(indices[i]));
            break;
        }
            // below
        case 2: {
            for (int i = 0; i < pvalues.length; i++)
                if (pvalues[i] < cutBelowValue)
                    outliersV.addElement(new Integer(indices[i]));
            break;
        }
            // both
        case 3: {
            for (int i = 0; i < pvalues.length; i++)
                if (pvalues[i] < cutBelowValue || pvalues[i] > cutAboveValue)
                    outliersV.addElement(new Integer(indices[i]));
            break;
        }
        }// end switch

        // if outliers were found, fill the outlier array
        if (!outliersV.isEmpty()) {
            outliers = new int[outliersV.size()];
            for (int i = 0; i < outliersV.size(); i++) {
                outliers[i] = ((Integer) (outliersV.elementAt(i))).intValue();
            }
        }// just in case no outliers were found just return
        else {
            return;
        }
        // finally play them
        play(0, outliers, true);
    }

    /**
     * For navigation purposes: jump to a given leaf
     * 
     * @param index
     *            the index of the leaf to jump to
     * 
     */
    public void jumpToLeaf(int index) {
        if (index < 1 || index > numLeafs) {
            outln("leaf index out of bounds");
            return;
        }
        // if index is above current index, just step forward until destination
        if (index > currLeafIndex) {
            for (int i = (currLeafIndex + 1); i <= index; i++) {
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                        .getNextLeafNode();
            }
            currLeafIndex = index;
        }
        // if index is below current index, start from the first leaf of the
        // cart
        if (index < currLeafIndex) {
            currLeaf = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
            for (int i = 2; i <= index; i++) {
                currLeaf = (IntAndFloatArrayLeafNode) currLeaf
                        .getNextLeafNode();
            }
            currLeafIndex = index;
        }
    }

    /**
     * Display the float values of a given leaf of the cart
     * 
     * @param leaf
     *            the leaf that is to be displayed
     * @param index
     *            its index as given in the program
     * @param all
     *            true: show all units and floats of the leaf, false: use global
     *            variables to limit them
     * 
     * @throws IOException
     *             because of the input lines
     * 
     */
    public void displayLeaf(LeafNode leaf, int index, boolean all)
            throws IOException {
        if (all) {
            outln("Now displaying contents of leaf " + index + ": ");
        } else {
            outln("Now displaying contents of leaf " + index
                    + " with cut mode " + cutWhereS[cutWhere]);
        }

        // get leaf data
        int[] indices = (int[]) ((IntAndFloatArrayLeafNode) leaf).getAllData();
        float[] data = ((IntAndFloatArrayLeafNode) leaf).getFloatData();

        int counter = 0;
        if (indices.length == 0) {
            outln("leaf empty.");
            return;
        }

        // show all data
        if (all) {
            for (int i = 0; i < leaf.getNumberOfData(); i++) {
                outln("		Index " + (i + 1) + ": unit " + indices[i]
                        + " duration " + (ufr.getUnit(indices[i]).getDuration()/(float)ufr.getSampleRate())
                        + ", probability value: " + data[i]);
                counter++;
                if (((i + 1) % 20) == 0)
                    input("[Enter] to continue...");
            }
        }
        // else use the global variables
        else {
            switch (cutWhere) {
            // above
            case 1: {
                for (int i = 0; i < leaf.getNumberOfData(); i++)
                    if (data[i] > cutAboveValue) {
                        outln("		Index " + (i + 1) + ": unit " + indices[i]
                                + " duration " + (ufr.getUnit(indices[i]).getDuration()/(float)ufr.getSampleRate())
                                + ", probability value: " + data[i]);
                        counter++;
                        if (((i + 1) % 20) == 0)
                            input("[Enter] to continue...");
                    }
                break;
            }
                // below
            case 2: {
                for (int i = 0; i < leaf.getNumberOfData(); i++)
                    if (data[i] < cutBelowValue) {
                        outln("		Index " + (i + 1) + ": unit " + indices[i]
                                + " duration " + (ufr.getUnit(indices[i]).getDuration()/(float)ufr.getSampleRate())
                                + ", probability value: " + data[i]);
                        counter++;
                        if (((i + 1) % 20) == 0)
                            input("[Enter] to continue...");
                    }
                break;
            }
                // both
            case 3: {
                for (int i = 0; i < leaf.getNumberOfData(); i++)
                    if (data[i] < cutBelowValue || data[i] > cutAboveValue) {
                        outln("		Index " + (i + 1) + ": unit " + indices[i]
                                + " duration " + (ufr.getUnit(indices[i]).getDuration()/(float)ufr.getSampleRate())
                                + ", probability value: " + data[i]);
                        counter++;
                        if (((i + 1) % 20) == 0)
                            input("[Enter] to continue...");
                    }
                break;
            }
            }// end switch
        } // end if

        // display mean and sd of probability values
        if (indices.length > 1) {
            outln("(mean being " + MaryUtils.mean(data) + "; sd being " + MaryUtils.stdDev(data)
                    + ")");
        }
        if (all) {
            outln("Displayed contents of leaf " + index + ".");
        } else {
            outln("Displayed contents of leaf " + index
                    + " cut off with cut mode " + cutWhereS[cutWhere]);
            outln("With cutAboveValue = " + cutAboveValue
                    + "; cutBelowValue = " + cutBelowValue);
            outln("Leaving " + counter + " of " + data.length
                    + " units as possible outliers");
        }
    }

    /**
     * Find a given unit in the cart and return its leaf
     * 
     * @param index,
     *            index of the unit to look for
     * 
     * @return int of leaf index
     */
    public int findUnit(int index) {

        LeafNode leafN = ctree.getFirstLeafNode();
        int lChecked = 0;
        while (leafN != null) {
            lChecked++;
            int[] cands = (int[]) (((IntAndFloatArrayLeafNode) leafN)
                    .getAllData());
            for (int i = 0; i < cands.length; i++) {
                if (cands[i] == index) {

                    return lChecked;

                }
            }
            leafN = leafN.getNextLeafNode();
        }
        return -1;
    }

    /**
     * Erase a given unit from its leaf, not overly used helper method
     * 
     * @param unit
     *            index of the unit to erase
     * @param leaf
     *            index of the leaf where the unit should be deleted from
     * 
     * @throws IOException
     */
    public void eraseUnit(int unit) throws IOException {
        int leaf = findUnit(unit);
        LeafNode n = (IntAndFloatArrayLeafNode) ctree.getFirstLeafNode();
        for (int i = 1; i < leaf; i++) {
            n = n.getNextLeafNode();
        }
        ((IntAndFloatArrayLeafNode) n).eraseData(unit);
        outln("Unit " + unit + " successfully erased from leaf.");
    }

    /**
     * Erase some units from a leaf, more or less helper method for analyze()
     * 
     * @param reallyCut
     *            set to false if you just want to have the statistics
     * @param lNode
     *            the leaf to erase the units from
     * @param units
     *            an array of the unit indices (null if erasing by cut-off
     *            value)
     * @param cut
     *            the cut-off value (see boolean above below)
     * @param pw
     *            print writer to log it, or null
     * @param above
     *            true if units above cut should be erased, false if below is
     *            the case
     * @param auto
     *            helper boolean for autoAnalyze, if true, don't give
     *            outln-output
     * 
     * @return how many units have been deleted
     */
    public int eraseUnitsFromLeaf(boolean reallyCut, LeafNode lNode,
            int[] units, float cut, PrintWriter pw, boolean above, boolean auto) {
        // counter for how many units have already been deleted.
        int counter = 0;
        // trivial: if units are already given, just erase them and return
        // length of array
        if (!(units == null)) {
            if (reallyCut) {
                for (int i = 0; i < units.length; i++) {
                    ((IntAndFloatArrayLeafNode) lNode).eraseData(units[i]);
                }
            }
            counter = units.length;
        }
        // else: use cut and above to detect the units that should be erased
        else {
            // get data
            float[] allTheData = ((IntAndFloatArrayLeafNode) lNode)
                    .getFloatData();
            int[] allTheIntData = (int[]) (((IntAndFloatArrayLeafNode) lNode)
                    .getAllData());
            Vector uVec = new Vector();
            // if outliers are guessed above the cut value, check for >
            if (above) {
                for (int i = 0; i < lNode.getNumberOfData(); i++) {
                    if (allTheData[i] > cut) {
                        uVec.addElement(new Integer(allTheIntData[i]));
                    }
                }
            }
            // else of course check for probability value below the cut value
            else {
                for (int i = 0; i < lNode.getNumberOfData(); i++) {
                    if (allTheData[i] < cut) {
                        uVec.addElement(new Integer(allTheIntData[i]));
                    }
                }
            }
            // convert the vector to an array
            int[] destroy = new int[uVec.size()];
            for (int i = 0; i < uVec.size(); i++) {
                destroy[i] = ((Integer) (uVec.elementAt(i))).intValue();
            }
            // and do a recursive call of this method, this time it is
            // guaranteed
            // to execute the trivial part of the method
            return eraseUnitsFromLeaf(reallyCut, lNode, destroy, cut, pw,
                    above, auto);

        }
        // only have a System.out if analyze() is not in auto mode
        if (!auto) {
            outln("Successfully cut " + counter + " units, therefore leaving "
                    + lNode.getNumberOfData() + " units in leaf.");
            outln("( => Percentage of cut units: "
                    + (float) ((float) counter / (float) (counter + lNode
                            .getNumberOfData())) * 100.f);
        }
        if (pw != null) {
            pw.println("Successfully cut " + counter
                    + " units, therefore leaving " + lNode.getNumberOfData()
                    + " units in leaf.");
            pw.println("( => Percentage of cut units: "
                    + (float) ((float) counter / (float) (counter + lNode
                            .getNumberOfData())) * 100.f);
        }
        return counter;
    }

    /**
     * 
     * Save current cart in (binary) file for future ca usage
     * 
     * @param filename
     *            file where cart should be dumped in
     */
    public void saveCart(String filename) throws Exception {
        outln("saving new cart in " + filename + "...");
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        // write header
        MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
        hdr.writeTo(dos);
        // write number of nodes
        dos.writeInt(ctree.getNumNodes());
        // write its name (dummy)
        String name = "";
        dos.writeUTF(name);
        // dump the cart
        ctree.dumpBinary(dos);
        dos.close();
        outln("saving finished.");
    }

    /**
     * 
     * Dump current cart in (binary) file for future MARY usage (i.e. final
     * dump; use normal classification tree)
     * 
     * @param filename
     *            file where cart should be dumped in
     */
    public void dumpCart(String filename) throws Exception {

        // first, save the cart in normal ca format in a temporary file
        String tempFile = filename + ".temp";
        saveCart(tempFile);

        // then, load it with a classification tree
        // the ctree will change all floats to 0
        ClassificationTree temp = new ClassificationTree();
        temp.load(tempFile, feaDef, null);

        // delete the temporary file
        File tempF = new File(tempFile);
        tempF.delete();

        // then proceed the dumping process, this time
        // dump the normal classification tree
        outln("dumping new cart in " + filename + "...");
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        // write header
        MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
        hdr.writeTo(dos);
        // write number of nodes
        dos.writeInt(ctree.getNumNodes());
        // write its name (dummy)
        String name = "";
        dos.writeUTF(name);
        // dump the cart
        temp.dumpBinary(dos);
        dos.close();
        outln("dumping finished.");
    }

    /**
     * 
     * Load an (Extended)ClassificationTree from a file, used for command load
     * and initialisation of the program Also, show some statistics about the
     * tree, therefore allowing quick diagnosis should something major have
     * happened
     * 
     * @param filename
     *            filename of tree
     * @param fD
     *            the feature definition
     * @return the loaded ExtendedClassificationTree
     * 
     * @throws IOException
     */
    public ExtendedClassificationTree loadTreeFromFile(String filename,
            FeatureDefinition fD) throws IOException {

        // load the cart
        outln("loading CART from file " + filename + "...");
        ExtendedClassificationTree ectree = new ExtendedClassificationTree();
        ectree.load(filename, fD, null);
        outln("cart loaded.");

        // prepare diagnosis file
        String diag = filename + ".diag";
        PrintWriter pw = new PrintWriter(new FileWriter(new File(diag)));

        // prepare statistics
        currLeaf = (IntAndFloatArrayLeafNode) ectree.getFirstLeafNode();
        currLeafIndex = 1;
        Vector v = new Vector();
        // number of units of leaf
        int units = 0;
        // number of leafs
        int numL = 0;
        // overall number of units
        int numU = 0;
        // number of empty leafs
        int numEmpty = 0;
        // minimal number of units and index of its leaf
        int min = 10000;
        int minLeaf = -1;
        // maximal number of units and index of its leaf
        int max = 0;
        int maxLeaf = -1;

        // now count everything
        // number of units (per leaf and overall), minimal number of units (>0),
        // maximal number of units
        // also average (sum/num) and mean
        pw.println("li\t#units");
        // walk through all the leafs
        while (currLeaf != null) {
            numL++;
            units = currLeaf.getNumberOfData();
            if (units == 0) {
                numEmpty++;
            } else {
                float u = (float) units;
                v.addElement(new Float(u));
                if (units > max) {
                    max = units;
                    maxLeaf = numL;
                }
                if (units < min) {
                    min = units;
                    minLeaf = numL;
                }
            }

            pw.println(numL + "\t" + units);
            pw.flush();
            numU += currLeaf.getNumberOfData();
            currLeaf = (IntAndFloatArrayLeafNode) currLeaf.getNextLeafNode();
        }
        float[] unitsFA = new float[v.size()];
        for (int i = 0; i < v.size(); i++) {
            unitsFA[i] = ((Float) (v.elementAt(i))).floatValue();
        }
        currLeaf = (IntAndFloatArrayLeafNode) ectree.getFirstLeafNode();
        numUnits = numU;
        numLeafs = numL;
        // outln("cart has "+numU+" units, "+numL+" leafs, of which "+numEmpty+"
        // are empty.");
        pw.println("cart has " + numU + " units, " + numL + " leafs, of which "
                + numEmpty + " are empty.");
        // outln("which makes an average of "+(numU/(numL-numEmpty))+" units per
        // non-empty leaf,");
        pw.println("which makes an average of " + (numU / (numL - numEmpty))
                + " units per non-empty leaf,");
        // outln("minimum being "+min+" (@ leaf #"+minLeaf+"), maximum being
        // "+max+" (@ leaf #"+maxLeaf+"), sd being "+getSD(unitsFA));
        pw.println("minimum being " + min + " (1st @ leaf #" + minLeaf
                + "), maximum being " + max + " (1st @ leaf #" + maxLeaf
                + "), sd being " + MaryUtils.stdDev(unitsFA));
        pw.close();
        outln("diag data stored in " + diag + " for diagnosis purposes.");
        return ectree;
    }


    /**
     * Lazy way of System output, again, self-explaining
     * 
     * @param arg0
     *            a string
     */
    public static void outln(String arg0) {
        System.out.println(arg0);
    }

    public static void out(String arg0) {
        System.out.print(arg0);
    }

    /**
     * Show some help in general or for a specific command
     * 
     * @param command,
     *            the command that the user whishes help for, empty string for
     *            overview
     * 
     * @throws IOException
     *             (in case of file not found)
     */
    public void help(String command) throws IOException {
        BufferedReader reader = null;
        String resourceName;
        if (command.equals("")) {
            resourceName = "cahelp.txt";
        } else {
            resourceName = "cahelp_" + command + ".txt";
        }
        reader = new BufferedReader(new InputStreamReader(this.getClass()
                .getResourceAsStream("cahelp.txt")));
        // display the help file
        for (String line = reader.readLine(); line != null; line = reader
                .readLine()) {
            outln(line);
            if (line.equals(""))
                continue;
            if (line.charAt(0) == '-' || line.charAt(0) == '_')
                input("[Enter] for more...");
        }
    }
    
    
    public int percent()
    {
        return percent;
    }

}
