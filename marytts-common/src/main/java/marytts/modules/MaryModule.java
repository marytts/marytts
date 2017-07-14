/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.modules;

// Log4j Logging classes
import java.io.StringReader;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import marytts.data.Utterance;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * A stub implementation of the MaryModule interface as a basis for internal
 * modules.
 * <p>
 * Any internal module extending this class will need to implement a constructor
 * calling this class's constructor, and override <code>process()</code> in a
 * meaningful way. Care must be taken to make sure the <code>process()</code>
 * method is thread-seafe.
 * <p>
 * Example for a subclass:
 *
 * <pre>
 * public class Postlex extends MaryModule {
 *  public Postlex() {
 *      super(&quot;Postlex&quot;, UtteranceType.PHONEMISED, UtteranceType.POSTPROCESSED);
 *  }
 *
 *  public Utterance process(Utterance d) throws Exception {
 *      Document doc = d.getDocument();
 *      mtuPostlex(doc);
 *      phonologicalRules(doc);
 *      Utterance result = new Utterance(outputType());
 *      result.setDocument(doc);
 *      return result;
 *  }
 *
 *  private void mtuPostlex(Document doc) {...}
 *
 *  private void phonologicalRules(Document doc) {...}
 * }
 * </pre>
 *
 * @author Marc Schr&ouml;der
 */

public abstract class MaryModule {
    public static final int MODULE_OFFLINE = 0;
    public static final int MODULE_RUNNING = 1;

    private String name = null;
    private Locale locale = null;
    protected int state;
    /**
     * The logger instance to be used by this module. It will identify the
     * origin of the log message in the log file.
     */
    protected Logger logger;


    protected MaryModule(String name) {
	this.name = name;
	this.locale = Locale.getDefault();
    }

    protected MaryModule(String name, Locale locale) {
        this.name = name;
        this.locale = locale;
        logger = MaryUtils.getLogger(name());
        this.state = MODULE_OFFLINE;
    }

    // Interface MaryModule implementation:
    public String name() {
        return name;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getState() {
        return state;
    }

    public void startup() throws Exception {
        assert state == MODULE_OFFLINE;
        logger.info("Module " + this.getClass().toGenericString() + "started, locale " + getLocale() +
                    ").");

        state = MODULE_RUNNING;
    }

    public void shutdown() {
        logger = MaryUtils.getLogger(name());
        logger.info("Module shut down.");
        state = MODULE_OFFLINE;
    }

    /**
     * Perform this module's processing on abstract "Utterance" input
     * <code>d</code>. Subclasses need to make sure that the
     * <code>process()</code> method is thread-safe, because in server-mode, it
     * will be called from different threads at the same time. A sensible way to
     * do this seems to be not to use any global or static variables, or to use
     * them read-only.
     * <p>
     *
     * @return A Utterance object of type <code>outputType()</code> encapsulating
     *         the processing result.
     *         <p>
     *         This method just returns its input. Subclasses should override
     *         this.
     */
    public abstract Utterance process(Utterance d) throws Exception;

}
