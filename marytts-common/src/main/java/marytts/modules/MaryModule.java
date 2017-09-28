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

import marytts.config.MaryProperties;

// Log4j Logging classes
import java.io.StringReader;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import marytts.data.Utterance;
import marytts.util.MaryUtils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */

public abstract class MaryModule {
    public static final int MODULE_OFFLINE = 0;
    public static final int MODULE_RUNNING = 1;

    private MaryProperties default_configuration = null;
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
        logger = LogManager.getLogger(this);
        this.state = MODULE_OFFLINE;
    }

    protected MaryModule(String name, Locale locale) {
        this.name = name;
        this.locale = locale;
        logger = LogManager.getLogger(this);
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
        logger.info("Module shut down.");
        state = MODULE_OFFLINE;
    }

    public Utterance process(Utterance utt) throws Exception {
        return process(utt, default_configuration);
    }

    public abstract Utterance process(Utterance utt, MaryProperties configuration) throws Exception;
}
