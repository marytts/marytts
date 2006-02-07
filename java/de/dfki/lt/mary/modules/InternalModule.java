/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.modules;

// Log4j Logging classes
import java.io.StringReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.synthesis.Voice;


/**
 * A stub implementation of the MaryModule interface
 *          as a basis for internal modules.
 * <p>
 * Any internal module extending this class will need to implement
 * a constructor calling this class's constructor, and override
 * <code>process()</code> in a meaningful way. Care must be taken
 * to make sure the <code>process()</code> method is thread-seafe.
 * <p>
 * Example for a subclass:
 * <pre>
 * public class Postlex extends InternalModule
 * {
 *    public Postlex()
 *    {
 *        super("Postlex",
 *              MaryDataType.PHONEMISED,
 *              MaryDataType.POSTPROCESSED);
 *    }
 *
 *    public MaryData process(MaryData d)
 *    throws Exception
 *    {
 *        Document doc = d.getDocument();
 *        mtuPostlex(doc);
 *        phonologicalRules(doc);
 *        MaryData result = new MaryData(outputType());
 *        result.setDocument(doc);
 *        return result;
 *    }
 *
 *    private void mtuPostlex(Document doc) {...}
 *    private void phonologicalRules(Document doc) {...}
 * }
 * </pre>
 *
 * @author Marc Schr&ouml;der
 */

public class InternalModule implements MaryModule
{
    private String name = null;
    private MaryDataType inputType = null;
    private MaryDataType outputType = null;
    protected int state;
    /** The logger instance to be used by this module.
     * It will identify the origin of the log message in the log file.
     */
    protected Logger logger;

    protected InternalModule(String name, MaryDataType inputType, MaryDataType outputType)
    {
        this.name = name;
        this.inputType = inputType;
        this.outputType = outputType;
        this.state = MODULE_OFFLINE;
    }

    // Interface MaryModule implementation:
    public String name() { return name; }
    public MaryDataType inputType() { return inputType; }
    public MaryDataType outputType() { return outputType; }
    public int getState() { return state; }
    public void startup() throws Exception {
        assert state == MODULE_OFFLINE;
        logger = Logger.getLogger(name());
        logger.info("Module started.");
        state = MODULE_RUNNING;
    }

    /**
     * Perform a power-on self test by processing some example input data.
     * @throws Error if the module does not work properly.
     */
    public void powerOnSelfTest() throws Error
    {
        assert state == MODULE_RUNNING;
        logger.info("Starting power-on self test.");
        try {
            if (inputType.exampleText() == null) {
                return; // cannot test
            }
            MaryData in = new MaryData(inputType);
            in.readFrom(new StringReader(inputType.exampleText()));
            if (outputType.equals(MaryDataType.get("AUDIO")))
                in.setAudioFileFormat(new AudioFileFormat(
                    AudioFileFormat.Type.WAVE, Voice.AF22050, AudioSystem.NOT_SPECIFIED)
                );
            process(in);
        } catch (Throwable t) {
            throw new Error("Module " + name + ": Power-on self test failed.", t);
        }
        logger.info("Power-on self test complete.");
    }


    public void shutdown()
    {
        state = MODULE_OFFLINE;
    }


    /**
     * Perform this module's processing on abstract "MaryData" input
     * <code>d</code>.
     * Subclasses need to make sure that the <code>process()</code>
     * method is thread-safe, because in server-mode,
     * it will be called from different threads at the same time.
     * A sensible way to do this seems to be not to use any
     * global or static variables, or to use them read-only.
     * <p>
     * @return A MaryData object of type
     * <code>outputType()</code> encapsulating the processing result.
     * <p>
     * This method just returns its input. Subclasses should override this.
     */
    public MaryData process(MaryData d) throws Exception
    {
        return d; // just return input.
    }

}
