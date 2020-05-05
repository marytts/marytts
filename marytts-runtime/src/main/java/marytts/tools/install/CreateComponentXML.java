/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.install;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twmacinta.util.MD5;

/**
 * @author marc
 * 
 */
public class CreateComponentXML {
	public static final String PROPERTY_XML_FOLDER = "mary.componentXML.folder";
	public static final String PROPERTY_XML_OUTPUTFILE = "mary.componentXML.outputfile";

	/**
	 * For the list of zip files given on the command line, create component XML descriptor files for the mary component
	 * installer.
	 * <p>
	 * The zip file names are expected to have the following structure: <code>mary-</code>(component name)<code>-</code>(version
	 * number)<code>.zip</code>
	 * <p>
	 * If the component name is a locale, a language component xml is created; otherwise, a voice component xml is created.
	 * <p>
	 * The program will look in the following folders for existing xml:
	 * <ul>
	 * <li>if a folder is passed as the system property <code>mary.componentXML.folder</code>, that folder and no other folder;</li>
	 * <li>otherwise:</li>
	 * </ul>
	 * <ul>
	 * <li>the folders in which the zip files are located;</li>
	 * <li>the current folder;</li>
	 * <li>the folders <code>download/</code> and <code>installed/</code> below the current folder;</li>
	 * </ul>
	 * If existing xml files contain a component with the same name, any information that can not be automatically computed (such
	 * as description, voice gender, synthesis technology etc.) will be copied from that component.
	 * <p>
	 * The resulting files will by default be written into individual xml files located in the same folder as the respective zip
	 * file, and named <code>mary-</code>(component name)<code>-</code>(version number)<code>-component.xml</code>. Alternatively,
	 * if the system property <code>mary.componentXML.outputfile</code> is set, this is used as the filename of a single XML file
	 * combining the component names.
	 * 
	 * @param args
	 *            zip files for which component xml is to be generated
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		// Write some documentation if appropriate:
		if (System.getProperty(PROPERTY_XML_OUTPUTFILE) == null) {
			System.out.println("You can indicate a single output file for the generated XML with -D" + PROPERTY_XML_OUTPUTFILE
					+ "=(filename)");
			System.out.println();
		}
		if (System.getProperty(PROPERTY_XML_FOLDER) == null) {
			System.out.println("You can indicate a folder containing existing component XML files with -D" + PROPERTY_XML_FOLDER
					+ "=(foldername)");
			System.out.println();
		}

		// Where to write output
		boolean writeIndividualXML = true;
		File outputFile = null;
		Document allXML = null;
		String outputFilename = System.getProperty(PROPERTY_XML_OUTPUTFILE);
		if (outputFilename != null) {
			outputFile = new File(outputFilename);
			writeIndividualXML = false;

		}
		// Where to look for known records:
		Set<File> xmlFolders = new HashSet<File>();
		boolean haveCustomXMLFolder = false;
		String customXMLFolder = System.getProperty(PROPERTY_XML_FOLDER);
		if (customXMLFolder != null) {
			haveCustomXMLFolder = true;
			File custom = new File(customXMLFolder);
			if (!custom.isDirectory()) {
				throw new FileNotFoundException("Custom XML folder '" + customXMLFolder
						+ "' was specified in system properties but does not exist!");
			}
			xmlFolders.add(custom);
		} else {
			xmlFolders.add(new File("."));
			File downloadFolder = new File("./download");
			if (downloadFolder.isDirectory()) {
				xmlFolders.add(downloadFolder);
			}
			File installedFolder = new File("./installed");
			if (installedFolder.isDirectory()) {
				xmlFolders.add(installedFolder);
			}
		}
		// Jobs
		List<File> zips = new ArrayList<File>(args.length);
		for (String a : args) {
			File f = new File(a);
			if (!f.canRead()) {
				throw new FileNotFoundException("Cannot read file: " + a);
			}
			if (!f.getName().startsWith("mary-") || !f.getName().endsWith(".zip")) {
				throw new IllegalArgumentException("File '" + f.getName()
						+ " doesn't follow convention 'mary-(name)-(version).zip'");
			}
			zips.add(f);
			if (!haveCustomXMLFolder) {
				xmlFolders.add(f.getAbsoluteFile().getParentFile());
			}
		}
		// Read known records
		Map<String, ComponentDescription> knownComponents = new TreeMap<String, ComponentDescription>();
		for (File folder : xmlFolders) {
			File[] xmlFiles = folder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});
			for (File xmlFile : xmlFiles) {
				try {
					InstallFileParser ifp = new InstallFileParser(xmlFile.toURI().toURL());
					for (ComponentDescription cd : ifp.getLanguageDescriptions()) {
						knownComponents.put(cd.getName(), cd);
					}
					for (ComponentDescription cd : ifp.getVoiceDescriptions()) {
						knownComponents.put(cd.getName(), cd);
					}
				} catch (Exception e) {
					/*
					 * System.err.println("Cannot load existing xml file "+xmlFile.getAbsolutePath()+" -- ignoring.");
					 * e.printStackTrace();
					 */
				}
			}
		}
		System.out.print("Loaded known component descriptions: ");
		for (String componentName : knownComponents.keySet()) {
			System.out.print(componentName + "(" + knownComponents.get(componentName).getVersion() + ") ");
		}
		System.out.println();
		System.out.println();

		// Now go through the zip files and try to create suitable component descriptions
		Set<ComponentDescription> newDescriptions = new TreeSet<ComponentDescription>();
		Set<Locale> existingLocales = new HashSet<Locale>(Arrays.asList(Locale.getAvailableLocales()));
		existingLocales.add(new Locale("te")); // Telugu
		for (File zip : zips) {
			String name;
			String version;
			// Filename convention: mary-(name)-(version).zip
			String filename = zip.getName();
			assert filename.startsWith("mary-");
			int nameStart = "mary-".length();
			int versionEnd = filename.length() - ".zip".length();
			int lastDash = filename.lastIndexOf('-', versionEnd);
			if (lastDash != -1) { // have a dash, normal
				name = filename.substring(nameStart, lastDash);
				version = filename.substring(lastDash + 1, versionEnd);
			} else { // no dash, treat as version "unknown"
				name = filename.substring(nameStart, versionEnd);
				version = "unknown";
			}
			Locale l = MaryUtils.string2locale(name); // this will work even if it's nonsense
			boolean isLanguageComponent = existingLocales.contains(l);
			boolean haveKnownComponent = knownComponents.containsKey(name);
			System.out.println((isLanguageComponent ? "Language" : "Voice")
					+ " component "
					+ name
					+ ", version "
					+ version
					+ (haveKnownComponent ? " (have component description for version " + knownComponents.get(name).getVersion()
							+ ")" : ""));
			// Create a component description that describes this zip file as appropriately as possible
			ComponentDescription cd;
			if (isLanguageComponent) {
				cd = new LanguageComponentDescription(name, version, filename);
				cd.setLocale(MaryUtils.string2locale(name));
			} else {
				VoiceComponentDescription vcd = new VoiceComponentDescription(name, version, filename);
				if (haveKnownComponent) {
					VoiceComponentDescription old = (VoiceComponentDescription) knownComponents.get(name);
					vcd.setGender(old.getGender());
					vcd.setType(old.getType());
					vcd.setDependsLanguage(old.getDependsLanguage());
					vcd.setDependsVersion(old.getDependsVersion());
					vcd.setLocale(old.getLocale());
				} else {
					vcd.setGender("unknown");
					vcd.setType("unknown");
					vcd.setDependsLanguage("unknown");
					vcd.setDependsVersion(version);
					vcd.setLocale(new Locale("unknown"));
				}
				cd = vcd;
			}
			// Hard facts:
			cd.setPackageSize((int) zip.length());
			cd.setPackageMD5Sum(MD5.asHex(MD5.getHash(zip)));
			// Further description elements:
			if (haveKnownComponent) {
				ComponentDescription old = knownComponents.get(name);
				cd.setDescription(old.getDescription());
				cd.setLicenseURL(old.getLicenseURL());
				for (URL loc : old.getLocations()) {
					cd.addLocation(loc);
				}
			} else { // need to guess
				cd.setDescription(" ");
				if (isLanguageComponent) { // assume LGPL
					cd.setLicenseURL(new URL("http://www.gnu.org/licenses/lgpl-3.0-standalone.html"));
				} else { // voice, assume by-nd
					cd.setLicenseURL(new URL("http://mary.dfki.de/download/by-nd-3.0.html"));
				}
				cd.addLocation(new URL("http://mary.dfki.de/download/" + version + "/" + filename));
			}
			// Now get the XML description to the right place:
			Document oneXML = cd.createComponentXML();
			if (writeIndividualXML) {
				File oneXMLFile = new File(zip.getParentFile(), "mary-" + name + "-" + version + "-component.xml");
				DomUtils.document2File(oneXML, oneXMLFile);
				System.out.println("Wrote " + oneXMLFile.getPath());
			} else { // combine them all into allXML, then write at the end
				if (allXML == null) {
					allXML = oneXML;
				} else {
					Node compDesc = oneXML.getDocumentElement().getElementsByTagName(cd.getComponentTypeString()).item(0);
					allXML.getDocumentElement().appendChild(allXML.adoptNode(compDesc));
				}
			}
		}
		if (!writeIndividualXML && allXML != null) {
			DomUtils.document2File(allXML, outputFile);
			System.out.println("Wrote " + outputFile.getPath());
		}
	}
}
