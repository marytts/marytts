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

import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.exceptions.MaryConfigurationException;

import java.io.StringReader;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import marytts.data.Utterance;
import marytts.util.MaryUtils;
import marytts.MaryException;

// Logging
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

/**
 *
 */

public abstract class MaryModule {
    public static final int MODULE_OFFLINE = 0;
    public static final int MODULE_RUNNING = 1;

    private MaryConfiguration default_configuration = null;
    protected int state;

    /**
     * The logger instance to be used by this module. It will identify the
     * origin of the log message in the log file.
     */
    protected Logger logger;


    /**
     *  Default constructor but available only for subclass. It just sets the default configuration
     *
     */
    protected MaryModule() {
	this(MaryConfigurationFactory.getDefaultConfiguration());
    }


    /**
     *  Constructor but available only for subclass. It just sets a given configuration
     *
     *  @param default_configuration the given configuration
     */
    protected MaryModule(MaryConfiguration default_configuration) {
	this.default_configuration = default_configuration;
        logger = LogManager.getLogger(this);
        this.state = MODULE_OFFLINE;
    }

    /**
     *  Get the state of the module (MODULE_RUNNING or MODULE_OFFLINE).
     *
     *  @return the state of the module
     */
    public int getState() {
        return state;
    }

    /**
     *  Get the default configuration of the module.
     *
     *  @return the default configuration object
     */
    public MaryConfiguration getDefaultConfiguration() {
	return default_configuration;
    }

    /**
     *  Apply the default configuration (could be seen as a kind of reset of parameters)
     *
     *  @throws MaryConfigurationException if anything is going wrong
     */
    public void applyDefaultConfiguration() throws MaryConfigurationException {
	if (default_configuration != null) {
	    default_configuration.applyConfiguration(this);
	}
    }

    /**
     *  Method to start the modules. This consists of applying the configuration and change the
     *  state by default.
     *
     *  @throws MaryException if the startup is failing
     */
    public void startup() throws MaryException {
        assert state == MODULE_OFFLINE;
	applyDefaultConfiguration();
        state = MODULE_RUNNING;

	logger.debug("\n" + MaryConfigurationFactory.dump());

        logger.info("Module " + this.getClass().toGenericString() + " started.");
    }

    /**
     *  Abstract method to validate that the startup succeeded. Only the actual module is
     *  responsible of checking that needed parameters are sets.
     *
     *  @throws MaryConfigurationException if anything is going wrong
     */
    public abstract void checkStartup() throws MaryConfigurationException;

    /**
     *  Method do shutdown the module. By default just the state is changed
     *
     */
    public void shutdown() {
        logger.info("Module shut down.");
        state = MODULE_OFFLINE;
    }

    /**
     *  Method to add an appender to the module logger
     *
     *  @param app the appender to add to the logger
     */
    protected void addAppender(Appender app) {
	((org.apache.logging.log4j.core.Logger) this.logger).addAppender(app);
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public abstract void checkInput(Utterance utt) throws MaryException;

    /**
     *  Method to call the module without any logging and the default configuration
     *
     *  @param utt the input utterance
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt) throws MaryException {
        return process(utt, new MaryConfiguration());
    }


    /**
     *  Method to call the module with a given appender and the default configuration
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt, Appender app) throws MaryException {
	((org.apache.logging.log4j.core.Logger) this.logger).addAppender(app);
        return process(utt, new MaryConfiguration());
    }


    /**
     *  Method to call the module with a given appender and a given configuration
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @param runtime_configuration the user configuration
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt, MaryConfiguration runtime_configuration, Appender app) throws MaryException {
	addAppender(app);
        return process(utt, runtime_configuration);
    }


    /**
     *  Method to call the module with a given configuration. This method should be implemented by
     *  the subclass module. All the other ones are referring to this one at the end.
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @param runtime_configuration the user configuration
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public abstract Utterance process(Utterance utt, MaryConfiguration runtime_configuration) throws MaryException;
}
