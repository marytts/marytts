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
package marytts.server;

import java.lang.reflect.Constructor;
import java.io.StringReader;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;

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

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.util.MaryCache;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import marytts.util.io.FileUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import marytts.io.Serializer;
import marytts.io.XMLSerializer;
import marytts.data.Utterance;

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
	protected String configuration;
	protected String input_data;

	protected String outputTypeParams;
	protected Locale defaultLocale;

	protected int id;
	protected Logger logger;
	protected MaryData inputData;
	protected MaryData outputData;
	protected Serializer output_serializer;
	protected boolean abortRequested = false;

	// Keep track of timing info for each module
	// (map MaryModule onto Long)
	protected Map<MaryModule, Long> timingInfo;

	public Request(String configuration, String input_data) {
		this.logger = MaryUtils.getLogger("R " + id);

		this.configuration = configuration;
		this.input_data = input_data;

		timingInfo = new HashMap<MaryModule, Long>();
	}

	public void process() throws Exception {

		assert Mary.currentState() == Mary.STATE_RUNNING;

		// Parser configuration
		final Properties configuration_properties = new Properties();
		configuration_properties.load(new StringReader(this.configuration));

		// Input serializer reflection (FIXME: check if serializer is ok)
		Class<?> clazz = Class.forName(configuration_properties.get("input_serializer").toString());
		Constructor<?> ctor = clazz.getConstructor();
		Serializer serializer = (Serializer) ctor.newInstance(new Object[]{});

		// Input serializer reflection (FIXME: check if serializer is ok)
		clazz = Class.forName(configuration_properties.get("output_serializer").toString());
		ctor = clazz.getConstructor();
		this.output_serializer = (Serializer) ctor.newInstance(new Object[]{});

		// Locale reflection (FIXME: Check if locale is correct)
		Locale cur_locale = MaryUtils.string2locale(configuration_properties.get("locale").toString());

		// Module sequence reflexion (FIXME: check if module is existing !)
		List<MaryModule> usedModules = new ArrayList<MaryModule>();
		String module_names = (String) configuration_properties.get("modules");
		if (module_names != null) {
			List<String> module_name_list = Arrays.asList(StringUtils.split(module_names));
			for (String module_class_name : module_name_list) {
				logger.debug("trying to load the following class " + module_class_name + " for locale " + cur_locale);
				if (ModuleRegistry.getModule(Class.forName(module_class_name), cur_locale) != null)
					usedModules.add(ModuleRegistry.getModule(Class.forName(module_class_name), cur_locale));
				else
					usedModules.add(ModuleRegistry.getModule(Class.forName(module_class_name)));
			}
		}

		// Define the data
		MaryData input_mary_data = new MaryData(cur_locale);
		input_mary_data.setData(serializer.fromString(this.input_data));

		// Start to achieve the process
		long startTime = System.currentTimeMillis();

		//////////////////////////////////////////////////////////////////////////////////////////////////////
		logger.info("Handling request using the following modules:");
		for (MaryModule m : usedModules) {
			logger.info("- " + m.name() + " (" + m.getClass().getName() + ")");
		}
		outputData = input_mary_data;
		for (MaryModule m : usedModules) {
			if (abortRequested)
				break;

			if (m.getState() == MaryModule.MODULE_OFFLINE) {
				// This should happen only in command line mode:
				assert MaryProperties.needProperty("server").compareTo("commandline") == 0;
				logger.info("Starting module " + m.name());
				m.startup();
				assert m.getState() == MaryModule.MODULE_RUNNING;
			}
			long moduleStartTime = System.currentTimeMillis();

			logger.info("Next module: " + m.name());
			MaryData outData = null;
			try {
				outData = m.process(outputData);
			} catch (Exception e) {
				throw new Exception("Module " + m.name() + ": Problem processing the data.", e);
			}

			if (outData == null)
				throw new NullPointerException("Module " + m.name() + " returned null. This should not happen.");

			outputData = outData;

			long moduleStopTime = System.currentTimeMillis();
			long delta = moduleStopTime - moduleStartTime;
			Long soFar = timingInfo.get(m);
			if (soFar != null)
				timingInfo.put(m, new Long(soFar.longValue() + delta));
			else
				timingInfo.put(m, new Long(delta));

			if (MaryRuntimeUtils.veryLowMemoryCondition()) {
				logger.info("Very low memory condition detected (only " + MaryUtils.availableMemory()
						+ " bytes left). Triggering garbage collection.");
				Runtime.getRuntime().gc();
				logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
			}
		}

		long stopTime = System.currentTimeMillis();
		logger.info("Request processed in " + (stopTime - startTime) + " ms.");
		for (MaryModule m : usedModules) {
			logger.info("   " + m.name() + " took " + timingInfo.get(m) + " ms");
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
	 * MaryData object.
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
	public MaryData getOutputData() {
		return outputData;
	}

	/**
	 * Write the output data to the specified OutputStream.
	 *
	 * @param outputStream
	 *            outputStream
	 * @throws Exception
	 *             Exception
	 */
	public void writeOutputData(OutputStream outputStream) throws Exception {
		if (outputData == null) {
			throw new NullPointerException("No output data -- did process() succeed?");
		}
		if (outputStream == null)
			throw new NullPointerException("cannot write to null output stream");
		// Safety net: if the output is not written within a certain amount of
		// time, give up. This prevents our thread from being locked forever if
		// an
		// output deadlock occurs (happened very rarely on Java 1.4.2beta).
		final OutputStream os = outputStream;
		Timer timer = new Timer();
		TimerTask timerTask = new TimerTask() {
			public void run() {
				logger.warn("Timeout occurred while writing output. Forcefully closing output stream.");
				try {
					os.close();
				} catch (IOException ioe) {
					logger.warn(ioe);
				}
			}
		};
		int timeout = MaryProperties.getInteger("modules.timeout", 10000);
		timer.schedule(timerTask, timeout);
		try {
			os.write(output_serializer.toString(this.outputData.getData()).getBytes());
		} catch (Exception e) {
			timer.cancel();
			throw e;
		}
		timer.cancel();
	}
}
