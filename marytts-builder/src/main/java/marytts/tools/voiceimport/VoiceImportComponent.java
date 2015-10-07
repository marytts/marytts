/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.util.SortedMap;

import marytts.util.MaryUtils;
import marytts.util.io.BasenameList;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

/**
 * A component in the process of importing a voice into MARY format.
 * 
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */
public abstract class VoiceImportComponent {
	protected SortedMap<String, String> props = null;
	protected SortedMap<String, String> props2Help = null;
	protected BasenameList bnl;
	protected DatabaseLayout db;
	protected Logger logger;

	protected VoiceImportComponent() {
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			BasicConfigurator.configure();
		}
		logger = MaryUtils.getLogger(getName());
	}

	protected abstract void setupHelp();

	/**
	 * Initialise a voice import component: update values of local properties; setup help text for properties; call to component
	 * specific intialisation
	 * 
	 * @param db
	 *            the database layout
	 * @param bnl
	 *            the list of basenames
	 * @param props
	 *            the map from properties to values
	 * @throws Exception
	 *             Exception
	 */
	public final void initialise(DatabaseLayout db, BasenameList bnl, SortedMap<String, String> props) throws Exception {
		// setupHelp(); this is now done by DatabaseLayout
		this.db = db;
		this.props = props;
		this.bnl = bnl;
		initialiseComp();
	}

	/**
	 * Initialise a voice import component: component specific initialisation; to be overwritten by subclasses
	 * 
	 * @throws Exception
	 *             Exception
	 */
	protected void initialiseComp() throws Exception {
	}

	/**
	 * Get the map of properties2values containing the default values
	 * 
	 * @param db
	 *            db
	 * @return map of props2values
	 */
	public abstract SortedMap<String, String> getDefaultProps(DatabaseLayout db);

	/**
	 * Get the value for a property
	 * 
	 * @param prop
	 *            the property name
	 * @return the value
	 */
	public String getProp(String prop) {
		return props.get(prop);
	}

	/**
	 * Set a property to a value
	 * 
	 * @param prop
	 *            the property
	 * @param value
	 *            the value
	 */
	public void setProp(String prop, String value) {
		props.put(prop, value);
	}

	/**
	 * Get the name of this component
	 * 
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Do the computations required by this component.
	 * 
	 * @throws Exception
	 *             Exception
	 * @return true on success, false on failure
	 */
	public abstract boolean compute() throws Exception;

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public abstract int getProgress();

	public String getHelpText() {
		StringBuilder helpText = new StringBuilder();
		helpText.append("<html>\n<head>\n<title>SETTINGS HELP</title>\n" + "</head>\n<body>\n"
				+ "<h2>Settings help for component " + getName() + "</h2>\n<dl>\n");
		try {
			for (String key : props2Help.keySet()) {
				String value = (String) props2Help.get(key);
				helpText.append("<dt><strong>" + key + "</strong></dt>\n" + "<dd>" + value + "</dd>\n");
			}

			helpText.append("</dl>\n</body>\n</html>");
			return helpText.toString();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			throw new Error("No help text for component " + getName());
		}
	}

	public String getHelpTextForProp(String propname) {
		return props2Help.get(propname);
	}

}
