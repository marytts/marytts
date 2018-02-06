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

import org.apache.commons.lang.StringUtils;

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
import marytts.data.Utterance;
import marytts.io.serializer.Serializer;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryRuntimeUtils;
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

    protected String input_data;

    protected String outputTypeParams;
    protected Locale defaultLocale;

    protected int id;
    protected Logger logger;
    protected Utterance inputData;
    protected Utterance outputData;
    protected Serializer output_serializer = null;
    protected Serializer input_serializer = null;
    protected List<MaryModule> module_sequence = null;
    protected boolean abortRequested = false;
    protected Appender appender;
    protected ByteArrayOutputStream baos_logger;

    // Keep track of timing info for each module (map MaryModule onto Long)
    protected Map<MaryModule, Long> timingInfo;

    public Request(MaryConfiguration configuration, String input_data) throws MaryConfigurationException
    {
	id = counter.getAndIncrement();

	// Deal with logger
        this.logger = LogManager.getLogger("R " + id);

	// Set the configuration
	configuration.applyConfiguration(this);

	// Set the input data
        this.input_data = input_data;

        timingInfo = new HashMap<MaryModule, Long>();
    }


    public void setLoggerLevel(String level) throws Exception {
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

    public void setOutputSerializer(String output_serializer_classname) throws Exception {

        // Output serializer reflection
        Class<?> clazz;
        Constructor<?> ctor;
	clazz = Class.forName(output_serializer_classname);
	ctor = clazz.getConstructor();
	this.output_serializer = (Serializer) ctor.newInstance(new Object[] {});
    }

    public void setInputSerializer(String input_serializer_classname) throws Exception {

        // Input serializer reflection
        Class<?> clazz;
        Constructor<?> ctor;
	clazz = Class.forName(input_serializer_classname);
	ctor = clazz.getConstructor();
	this.input_serializer = (Serializer) ctor.newInstance(new Object[] {});
    }

    public void setModuleSequence(ArrayList<String> list_module_names) throws Exception {
	// Module sequence reflexion (FIXME: check if module is existing !)
        module_sequence = new ArrayList<MaryModule>();
	for (String module_class_name : list_module_names) {
	    logger.debug("trying to load the following class " + module_class_name);

	    MaryModule cur_module = null;
	    cur_module = ModuleRegistry.getModule(Class.forName(module_class_name));
	    if (cur_module == null) {
		throw new MaryException("Cannot load module \"" + module_class_name +
					"\" as it is not existing");
	    }

	    module_sequence.add(cur_module);
	}
    }

    public ByteArrayOutputStream getBaosLogger() {
	return baos_logger;
    }

    public void process() throws Exception {

        assert Mary.getCurrentState() == Mary.STATE_RUNNING;
	assert input_serializer != null;
	assert output_serializer != null;

        // Define the data
        Utterance input_mary_data = input_serializer.load(this.input_data);

        // Start to achieve the process
        long startTime = System.currentTimeMillis();

        logger.info("Handling request using the following modules:");
        for (MaryModule m : module_sequence) {
            logger.info("- " + m.getClass().getName());
        }
        outputData = input_mary_data;
        for (MaryModule m : module_sequence) {
            if (abortRequested) {
                break;
            }

            logger.info("Next module: " + m.getClass().getName());

            // Start module if needed
            logger.debug("Starting the module");
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
		// FIXME: what about the configuration and the logger
                outData = m.process(outputData, this.appender);
            } catch (Exception e) {
                throw new MaryException("Module " + m.getClass().getName() + ": Problem processing the data.", e);
            }

            if (outData == null) {
                throw new NullPointerException("Module " + m.getClass().getName() + " returned null. This should not happen.");
            }

            outputData = outData;

            long moduleStopTime = System.currentTimeMillis();
            long delta = moduleStopTime - moduleStartTime;
            Long soFar = timingInfo.get(m);
            if (soFar != null) {
                timingInfo.put(m, new Long(soFar.longValue() + delta));
            } else {
                timingInfo.put(m, new Long(delta));
            }

	    // // FIXME: fix memory part
            // if (MaryRuntimeUtils.veryLowMemoryCondition()) {
            //     logger.info("Very low memory condition detected (only " + MaryUtils.availableMemory()
            //                 + " bytes left). Triggering garbage collection.");
            //     Runtime.getRuntime().gc();
            //     logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
            // }
        }

        long stopTime = System.currentTimeMillis();
        logger.info("Request processed in " + (stopTime - startTime) + " ms.");
        for (MaryModule m : module_sequence) {
            logger.info("   " + m.getClass().getName() + " took " + timingInfo.get(m) + " ms");
        }
    }

    public int getId() {
        return id;
    }

    /**
     * Inform this request that any further processing does not make sense.
     */
    public void abort() {
        logger.info("Requesting abort.");
        abortRequested = true;
    }

    /**
     * Set the input data directly, in case it is already in the form of a
     * Utterance object.
     *
     * @param input_data
     *            inputData
     */
    public void setInputData(String input_data) {
        this.input_data = input_data;
    }

    /**
     * Read the input data from a Reader.
     *
     * @param inputReader
     *            inputReader
     * @throws Exception
     *             Exception
     */
    public void readInputData(Reader inputReader) throws Exception {
        String inputText = FileUtils.getReaderAsString(inputReader);
        setInputData(inputText);
    }

    /**
     * Direct access to the output data.
     *
     * @return outputdata
     */
    public Utterance getOutputData() {
        return outputData;
    }

    public Object serializeFinaleUtterance() throws MaryIOException {
        return output_serializer.export(this.outputData);
    }
}
