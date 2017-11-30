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
package marytts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import marytts.io.MaryIOException;
import marytts.io.serializer.ROOTSJSONSerializer;
import marytts.io.serializer.XMLSerializer;
import marytts.data.Utterance;
import marytts.modules.MaryModule;
import marytts.runutils.Mary;
import marytts.util.MaryUtils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


import org.testng.Assert;
import org.testng.annotations.*;
import marytts.config.MaryConfigurationFactory;

// Log
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class MaryIT{

    @Test
    public void testStartingShutting() throws Exception
    {
    	LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    	Configuration config = ctx.getConfiguration();
    	LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    	loggerConfig.setLevel(Level.DEBUG);
    	ctx.updateLoggers();

    	Mary.startup();

    	if (Mary.currentState() != Mary.STATE_RUNNING)
    	    throw new Exception("Mary is not started!");

    	Assert.assertNotNull(MaryConfigurationFactory.getDefaultConfiguration());

    	Mary.shutdown();
    }
}
