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
package marytts.runutils;

import java.lang.reflect.Constructor;
import java.io.StringReader;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import marytts.io.MaryIOException;
import marytts.MaryException;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import marytts.exceptions.MaryConfigurationException;
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.data.Utterance;
import marytts.io.serializer.Serializer;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;



import marytts.data.Utterance;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A request consists of input data, a desired output data type and the means to
 * process the input data into the data of the output type.<br>
 * <br>
 * A request is used as follows. First, its basic properties are set in the
 * constructor, such as its input and output types. Second, the input data is
 * provided to the request either by directly setting it
 * (<code>setInputData()</code>) or by reading it from a Reader
 * (<code>readInputData()</code>). Third, the request is processed
 * (<code>process()</code>). Finally, the output data is either accessed
 * directly (<code>getOutputData()</code>) or written to an output stream
 * (<code>writeOutputData</code>).
 */
public class Request {
    private static final AtomicInteger counter = new AtomicInteger();


    protected int id;
    protected Logger logger;


    protected String input_data;
    protected MaryConfiguration configuration;
    protected Serializer input_serializer = null;


    protected List<MaryModule> module_sequence = null;

    protected String outputTypeParams;
    protected Utterance outputData;
    protected Serializer output_serializer = null;


    protected boolean abortRequested = false;


    protected Appender appender;
    protected ByteArrayOutputStream baos_logger = new ByteArrayOutputStream();

    // Keep track of timing info for each module (map MaryModule onto Long)
    protected Map<MaryModule, Long> timingInfo;


    /**
     *  Constructor.
     *
     *  A request is composed by a configuration and an input data
     *
     *  @param configuration the configuration object
     *  @param input_data the input data in a string format (the serializer is going to take care of it late)
     *  @throws MaryConfigurationException if anything is going wrong
     */
    public Request(MaryConfiguration configuration, String input_data) throws MaryConfigurationException
    {
	id = counter.getAndIncrement();

	// Deal with logger
        this.logger = LogManager.getLogger("R " + id);

	//
	this.module_sequence = new ArrayList<MaryModule>();
        this.input_data = input_data;

        // Resolve configuration
        logger.debug("Configuration to be used");
        logger.debug(configuration.toString());
        configuration.resolve();
        logger.debug("Resolve configuration to be used");
        logger.debug(configuration.toString());

        // Set the configuration
        this.configuration = configuration;
	this.configuration.applyConfiguration(this);

        timingInfo = new HashMap<MaryModule, Long>();
    }


    /**
     *  Set the request logger level
     *
     *  @param level the level in String
     *  @throws Exception if anything is going wrong
     */
    public void setLoggerLevel(String level) throws Exception {
	level = level.toUpperCase();
	Level current_level;
	if (level.equals("ERROR"))
	    current_level = Level.ERROR;
	else if (level.equals("WARN"))
	    current_level = Level.WARN;
	else if (level.equals("INFO"))
	    current_level = Level.INFO;
	else if (level.equals("DEBUG"))
	    current_level = Level.DEBUG;
	else
	    throw new Exception("\"" + level + "\" is an unknown level");

	// Logging configuration
	baos_logger = new ByteArrayOutputStream();
        ThresholdFilter threshold_filter = ThresholdFilter.createFilter(current_level, null, null);
        LoggerContext context = LoggerContext.getContext(false);
        Configuration config = context.getConfiguration();
        PatternLayout layout = PatternLayout.createDefaultLayout(config);
	this.appender = OutputStreamAppender.createAppender(layout, threshold_filter, baos_logger,
							    "client " + (new Integer(id)).toString(),
							    false, true);
        this.appender.start();
	((org.apache.logging.log4j.core.Logger) this.logger).addAppender(this.appender);
    }


    /**
     *  Define the output serializer knowing the class name
     *
     *  @param output_serializer_classname the output serializer class name
     *  @throws Exception if anything is going wrong
     */
    public void setOutputSerializer(String output_serializer_classname) throws Exception {
        Class<?> clazz = Class.forName(output_serializer_classname);
        Constructor<?> ctor = clazz.getConstructor();
	this.output_serializer = (Serializer) ctor.newInstance(new Object[] {});
    }


    /**
     *  Define the input serializer knowing the class name
     *
     *  @param output_serializer_classname the output serializer class name
     *  @throws Exception if anything is going wrong
     */
    public void setInputSerializer(String input_serializer_classname) throws Exception {
        Class<?> clazz = Class.forName(input_serializer_classname);
        Constructor<?> ctor = clazz.getConstructor();
	this.input_serializer = (Serializer) ctor.newInstance(new Object[] {});
    }


    /**
     *  Define module sequence given a list of module names
     *
     *  @param list_module_names the list of module names
     *  @throws Exception if anything is going wrong
     */
    public void setModuleSequence(ArrayList<String> list_module_names) throws Exception {
	// Module sequence reflexion (FIXME: check if module is existing !)
        module_sequence = new ArrayList<MaryModule>();
	for (String module_class_name : list_module_names) {
	    logger.debug("trying to load the following class " + module_class_name);

	    MaryModule cur_module = null;
	    cur_module = ModuleRegistry.getModule(MaryConfigurationFactory.DEFAULT_KEY,
						  Class.forName(module_class_name));
	    if (cur_module == null) {
		throw new MaryException("Cannot load module \"" + module_class_name +
					"\" as it is not existing");
	    }

	    module_sequence.add(cur_module);
	}
    }

    /**
     *  Get the logger output stream
     *
     *  @return the logger output stream
     */
    public ByteArrayOutputStream getBaosLogger() {
	return baos_logger;
    }


    /**
     *  Method to achieve the request (synthesis or generation)
     *
     *  @throws MaryException if something is going wrong during the process
     */
    public void process() throws MaryException {

	// Assert that everything is ready to run
        assert Mary.getCurrentState() == MaryState.RUNNING;
	assert input_serializer != null;
	assert output_serializer != null;

        // Load the input data
        this.configuration.applyConfiguration(input_serializer);
        Utterance input_mary_data = input_serializer.load(this.input_data);
        outputData = input_mary_data;

        // Information about what is the module sequence
	long startTime = System.currentTimeMillis();
	if (logger.getLevel().isLessSpecificThan(Level.INFO)) {
	    logger.info("Handling request using the following modules:");
	    for (MaryModule m : module_sequence) {
		logger.info("- " + m.getClass().getName());
	    }
	}

	// Achieve the process
        for (MaryModule m : module_sequence) {

	    // Abort => exit the loop
            if (abortRequested)
                break;

            // Start module if needed
            logger.info("Starting the module " + m.getClass().getName());
            if (m.getState() == MaryModule.MODULE_OFFLINE) {
                // This should happen only in command line mode:
		logger.info("Starting module " + m.getClass().getName());
                m.startup();
                assert m.getState() == MaryModule.MODULE_RUNNING;
            }
            long moduleStartTime = System.currentTimeMillis();

            // Assess the input is ok
            m.checkInput(outputData);

            // Process the module
            Utterance outData = null;
            try {
                // Define appender
		if (this.appender != null)
                    m.addAppender(this.appender);

                // Apply configuration
                m.applyConfiguration(this.configuration);

                // Process
                outData = m.process(outputData);
            } catch (Exception e) {
                throw new MaryException("Module " + m.getClass().getName() + ": Problem processing the data.", e);
            }

	    // Assess that the output data is correct
            if (outData == null)
                throw new NullPointerException("Module " + m.getClass().getName() +
					       " returned null. This should not happen.");
            outputData = outData;

	    // If some info are requested => compute some log
	    if (logger.getLevel().isLessSpecificThan(Level.INFO)) {
		long moduleStopTime = System.currentTimeMillis();
		long delta = moduleStopTime - moduleStartTime;
		Long soFar = timingInfo.get(m);

		if (soFar != null) {
		    timingInfo.put(m, new Long(soFar.longValue() + delta));
		} else {
		    timingInfo.put(m, new Long(delta));
		}
	    }

	    // FIXME: fix memory part
            if (MemoryUtils.veryLowMemoryCondition()) {
                logger.warn("Very low memory condition detected (only " + MemoryUtils.availableMemory()
                            + " bytes left). Triggering garbage collection.");
                Runtime.getRuntime().gc();
                logger.warn("After garbage collection: " + MemoryUtils.availableMemory() + " bytes available.");
            }
        }


	if (logger.getLevel().isLessSpecificThan(Level.INFO)) {
	    long stopTime = System.currentTimeMillis();
	    logger.info("Request processed in " + (stopTime - startTime) + " ms.");
	    for (MaryModule m : module_sequence) {
		logger.info("   " + m.getClass().getName() + " took " + timingInfo.get(m) + " ms");
	    }
	}
    }

    /**
     *  Return the id of the request
     *
     *  @return the id of the request
     */
    public int getId() {
        return id;
    }

    /**
     * Inform this request that any further processing does not make sense.
     *
     */
    public void abort() {
        logger.info("Requesting abort.");
        abortRequested = true;
    }

    /**
     * Direct access to the output data.
     *
     * @return outputdata
     */
    public Utterance getOutputData() {
        return outputData;
    }

    /**
     *  Serialize utterance
     *
     *  @return the utterance serialized
     *  @throws MaryIOException if the serialization is failing
     *  @throws MaryConfigurationException if the configuration associated to the request is malformed
     */
    public Object serializeFinaleUtterance() throws MaryIOException, MaryConfigurationException {
        this.configuration.applyConfiguration(output_serializer);
        return output_serializer.export(this.outputData);
    }
}
