/**
 * Copyright 2011 DFKI GmbH.
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
package marytts.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import marytts.exceptions.MaryConfigurationException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * An abstract class to represent a MaryTTS configuration file loader.
 *
 * @author marc
 */
public abstract class MaryConfigLoader {

    /** The logger of the config loader */
    private Logger logger;

    /** The configuration service loader */
    protected static final ServiceLoader<MaryConfigLoader> configLoader = ServiceLoader.load(MaryConfigLoader.class);


    /**
     * Default constructor to deal with default configuration
     *
     *  @throws MaryConfigurationException if something is going wrong while the default
     *  configuration is loaded
     */
    protected MaryConfigLoader() throws MaryConfigurationException {
        logger = LogManager.getLogger(this.getClass());
    }

    /**
     * This method is here to force the loading to happen
     *
     */
    public void load() {
        logger.debug("Configuration " + this.getClass().toString() + " is loading");
    }


    /**
     * Static method to list the configuration loaders available
     *
     * @returns an iterable over the available MaryConfigLoader objects
     */
    public static synchronized Iterable<MaryConfigLoader> getConfigLoaders() {
        return configLoader;
    }


    /**
     * Configuration loading method
     *
     *  @param input_stream the stream containing the configuration
     */
    public abstract MaryConfiguration loadConfiguration(InputStream input_stream) throws MaryConfigurationException;


    /**
     * Configuration loading method directly into the factory
     *
     *  @param set the name of the configuration set for later reference in the configuration hash
     *  @param input_stream the stream containing the configuration
     */
    public void loadConfiguration(String set, InputStream input_stream) throws MaryConfigurationException {
	MaryConfiguration mc = loadConfiguration(input_stream);
	MaryConfigurationFactory.addConfiguration(set, mc);
    }
}
