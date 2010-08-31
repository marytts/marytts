/**
 * Copyright 2010 DFKI GmbH.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.twmacinta.util.MD5;

import marytts.util.MaryUtils;

/**
 * Package all of the data files required for the voice in a zip file and generate a component file with the corresponding
 * metadata. Also generate the voice config file and include it in the package. This package is put in MARYBASE/download.
 * <p>
 * <b>This component does <i>not</i> install the voice!</b> The Mary Component Installer must be run for that purpose.
 * 
 * @author steiner (based on {@link VoiceInstaller})
 * 
 */
public class VoicePackager extends VoiceImportComponent {

    protected String name;

    protected DatabaseLayout db;

    protected String VOICETYPE;

    protected String EXAMPLETEXT;

    protected String LICENSEURL;
    
    protected String VOICEDESCRIPTION;

    // constants to access filenames in database component properties and organize file list:

    protected final String CARTFILE = "CARTBuilder.cartFile";

    protected final String DURTREE = "DurationCARTTrainer.durTree";

    protected final String F0LEFTTREE = "F0CARTTrainer.f0LeftTreeFile";

    protected final String F0MIDTREE = "F0CARTTrainer.f0MidTreeFile";

    protected final String F0RIGHTTREE = "F0CARTTrainer.f0RightTreeFile";

    protected final String HALFPHONEFEATSAC = "AcousticFeatureFileWriter.acFeatureFile";

    protected final String HALFPHONEFEATDEFAC = "AcousticFeatureFileWriter.acFeatDef";

    protected final String HALFPHONEUNITS = "HalfPhoneUnitfileWriter.unitFile";

    protected final String JOINCOSTFEATS = "JoinCostFileMaker.joinCostFile";

    protected final String JOINCOSTFEATDEF = "JoinCostFileMaker.weightsFile";

    protected final String PHONEFEATDEF = "PhoneFeatureFileWriter.weightsFile";

    protected String TIMELINE = null;

    protected final String HNMTIMELINE = "HnmTimelineMaker.hnmTimeline";

    protected final String WAVETIMELINE = "WaveTimelineMaker.waveTimeline";

    protected final String BASETIMELINE = "BasenameTimelineMaker.timelineFile";

    
    public VoicePackager() {
        this("VoicePackager");
    }
    
    protected VoicePackager(String name) {
        super();
        this.name = name;
        VOICETYPE = name + ".voiceType";
        EXAMPLETEXT = name + ".exampleText";
        LICENSEURL = name + ".licenseUrl";
        VOICEDESCRIPTION = name + ".voiceDescription";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setupHelp() {
        props2Help = new TreeMap<String, String>();
        props2Help.put(VOICETYPE, "voice type; one of <b>unit selection</b>, <b>HSMM</b>, <b>FDPSOLA</b>, <b>HNM</b>"
                + " (note that support for FDPSOLA and HNM are experimental!)");
        props2Help.put(EXAMPLETEXT, "file containing example text (for limited domain voices only)");
        props2Help.put(LICENSEURL, "URL of the license agreement for this voice"
                + " (<a href=\"http://creativecommons.org/licenses/by-nd/3.0/\">cc-by-nd</a> by default)");
        props2Help.put(VOICEDESCRIPTION, "short text describing this voice");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SortedMap<String, String> getDefaultProps(DatabaseLayout databaseLayout) {
        this.db = databaseLayout;
        if (props == null) {
            props = new TreeMap<String, String>();
            String voiceType = System.getProperty("VOICETYPE", "unit selection");
            props.put(VOICETYPE, voiceType);
            String exampleText = System.getProperty("EXAMPLETEXT", "examples.text");
            props.put(EXAMPLETEXT, exampleText);
            String licenseUrl = System.getProperty("LICENSEURL", "http://mary.dfki.de/download/by-nd-3.0.html");
            props.put(LICENSEURL, licenseUrl);
            String voiceDescription = System.getProperty("VOICEDESCRIPTION", "");
            props.put(VOICEDESCRIPTION, voiceDescription);
        }
        return props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws Exception
     */
    @Override
    public boolean compute() throws Exception {
        // (0) ensure that properties have valid values:
        validateProperties();

        // (1) gather files required by this voice in a convenient structure:
        HashMap<String, File> files = getVoiceDataFiles();

        // (2) create config file and add it to the files:
        try {
            File configFile = createVoiceConfig(files);
            files.put("CONFIG", configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        // (3) create zip file and component file (directly at their destination, MARYBASE/download):
        try {
            File zipFile = createZipFile(files);
            createComponentFile(zipFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        logger.info("The voice package and component file have been successfully created and placed in " + getMaryBase()
                + "download");
        logger.info("IMPORTANT: You must run the MARY Component Installer to install the voice!");
        return true;
    }

    /**
     * Check various properties for invalid values
     * 
     * @throws Exception
     */
    protected void validateProperties() throws Exception {
        // ensure that voice type is supported:
        if (!getProp(VOICETYPE).toLowerCase().matches("(unit selection|fdpsola|hnm|hsmm)")) {
            throw new Exception("Unsupported voice type: " + getProp(VOICETYPE));
        }
        // check for valid license URL:
        try {
            new URL(getProp(LICENSEURL));
        } catch (MalformedURLException e) {
            throw new MalformedURLException(getProp(LICENSEURL) + " is not a valid URL!");
        }
    }

    /**
     * Get the data files for this voice (which go into <tt>MARYBASE/lib/voices</tt>) and put them in a &lt;property, File&gt; Map
     * so that they can be accessed individually by the corresponding property key, e.g. "WaveTimelineMaker.waveTimeline" &rarr;
     * File("VOICE_DIR/mary/timeline_waves.mry")
     * <p>
     * TODO The way individual components provide access to their properties is a little patchy, so for now, this method is a
     * little fragile!
     * 
     * @return the file Map
     */
    protected HashMap<String, File> getVoiceDataFiles() {
        HashMap<String, File> files = new HashMap<String, File>();

        if (getProp(VOICETYPE).equalsIgnoreCase("HNM")) {
            TIMELINE = HNMTIMELINE;
        } else {
            TIMELINE = WAVETIMELINE;
        }

        String[] properties = { CARTFILE, DURTREE, F0LEFTTREE, F0MIDTREE, F0RIGHTTREE, HALFPHONEFEATSAC, HALFPHONEFEATDEFAC,
                HALFPHONEUNITS, JOINCOSTFEATS, JOINCOSTFEATDEF, PHONEFEATDEF, TIMELINE, BASETIMELINE };

        for (String property : properties) {
            String fileName = getProperty(property);
            File file = new File(fileName);
            files.put(property, file);
        }

        return files;
    }

    /**
     * Create config file for this voice.
     * 
     * @param files
     *            &lt;property, File&gt; Map, e.g. "WaveTimelineMaker.waveTimeline" &rarr;
     *            File("VOICE_DIR/mary/timeline_waves.mry")
     * @return the config File object
     * @throws FileNotFoundException
     */
    protected File createVoiceConfig(HashMap<String, File> files) throws FileNotFoundException {
        // open the config file for writing:
        String configFileName = String.format("%s-%s.config", getVoiceLocale(), getVoiceName());
        logger.info("Creating voice configuration file " + configFileName);
        File configFile = new File(getVoiceFileDir() + configFileName);
        PrintWriter out = new PrintWriter(configFile);

        // generate the config file contents, line by line:
        out.format("# Auto-generated config file for voice %s\n\n", getVoiceName());

        out.format("name = %s\n", getVoiceName());
        out.format("# Declare \"group names\" as component that other components can require.\n");
        out.format("# These correspond to abstract \"groups\" of which this component is an instance.\n");
        out.format("provides = \\\n\t%s-voice\n\n", getVoiceLocale());

        // TODO these seem to be ignored by MaryProperties, are they really needed?
        out.format("%s-voice.version = %s\n\n", getVoiceLocale(), getMaryVersion());
        out.format("voice.version = %s\n\n", getMaryVersion());

        out.format("# List the dependencies, as a whitespace-separated list.\n");
        out.format("# For each required component, an optional minimum version and an optional\n");
        out.format("# download url can be given.\n");
        out.format("# We can require a component by name or by an abstract \"group name\"\n");
        out.format("# as listed under the \"provides\" element.\n");
        out.format("requires = \\\n\t%s \\\n\tmarybase\n\n", getVoiceLocale());

        out.format("requires.marybase.version = %s\n", getMaryVersion());
        out.format("requires.%s.version = %s\n", getVoiceLocale(), getMaryVersion());
        // TODO: this is obviously a placeholder url; do we really need this?
        out.format("requires.%s.download = http://mary.dfki.de/download/mary-install-4.x.x.jar\n", getVoiceLocale());

        out.format("####################################################################\n");
        out.format("####################### Module settings  ###########################\n");
        out.format("####################################################################\n");
        out.format("# For keys ending in \".list\", values will be appended across config files,\n");
        out.format("# so that .list keys can occur in several config files.\n");
        out.format("# For all other keys, values will be copied to the global config, so\n");
        out.format("# keys should be unique across config files.\n");

        out.format("# If this setting is not present, a default value of 0 is assumed.\n");
        out.format("voice.%s.wants.to.be.default = 20\n\n", getVoiceName());

        out.format("# Add your voice to the list of Unit Selection Voices\n");
        out.format("unitselection.voices.list = \\\n\t%s\n\n", getVoiceName());

        out.format("# Set your voice specifications\n");
        out.format("voice.%s.gender = %s\n", getVoiceName(), getVoiceGender());
        out.format("voice.%s.locale = %s\n", getVoiceName(), getVoiceLocale());
        out.format("voice.%s.domain = %s\n", getVoiceName(), getVoiceDomain());
        out.format("voice.%s.samplingRate = %d\n\n", getVoiceName(), getVoiceSamplingRate());

        out.format("# Relative weight of the target cost function vs. the join cost function\n");
        out.format("voice.bits3v2-hnm.viterbi.wTargetCosts = 0.7\n\n");

        out.format("# Beam size in dynamic programming: smaller => faster but worse quality.\n");
        out.format("# (set to -1 to disable beam search; very slow but best available quality)\n");
        out.format("voice.bits3v2-hnm.viterbi.beamsize = 100\n\n");

        // TODO surely this should be dependent on having locale == "de"?
        out.format("# Sampa mapping for German voices \n");
        out.format("voice.%s.sampamap = \\\n", getVoiceName());
        out.format("\t=6->6 \\\n");
        out.format("\t=n->n \\\n");
        out.format("\t=m->m \\\n");
        out.format("\t=N->N \\\n");
        out.format("\t=l->l \\\n");
        out.format("\ti->i: \\\n");
        out.format("\te->e: \\\n");
        out.format("\tu->u: \\\n");
        out.format("\to->o: \n\n");

        out.format("# Java classes to use for the various unit selection components\n");
        out.format("voice.%s.databaseClass            = marytts.unitselection.data.DiphoneUnitDatabase\n", getVoiceName());
        out.format("voice.%s.selectorClass            = marytts.unitselection.select.DiphoneUnitSelector\n", getVoiceName());
        if (getProp(VOICETYPE).equalsIgnoreCase("HNM")) {
            out.format("voice.%s.concatenatorClass        = marytts.unitselection.concat.HnmUnitConcatenator\n", getVoiceName());
        } else if (getProp(VOICETYPE).equalsIgnoreCase("FDPSOLA")) {
            out.format("voice.%s.concatenatorClass        = marytts.unitselection.concat.FdpsolaUnitConcatenator\n",
                    getVoiceName());
        } else {
            out.format("voice.%s.concatenatorClass        = marytts.unitselection.concat.OverlapUnitConcatenator\n",
                    getVoiceName());
        }
        out.format("voice.%s.targetCostClass          = marytts.unitselection.select.DiphoneFFRTargetCostFunction\n",
                getVoiceName());
        out.format("voice.%s.joinCostClass            = marytts.unitselection.select.JoinCostFeatures\n", getVoiceName());
        out.format("voice.%s.unitReaderClass          = marytts.unitselection.data.UnitFileReader\n", getVoiceName());
        out.format("voice.%s.cartReaderClass          = marytts.cart.io.MARYCartReader\n", getVoiceName());
        if (getProp(VOICETYPE).equalsIgnoreCase("HNM")) {
            out.format("voice.%s.audioTimelineReaderClass = marytts.unitselection.data.HnmTimelineReader\n\n", getVoiceName());
        } else {
            out.format("voice.%s.audioTimelineReaderClass = marytts.unitselection.data.TimelineReader\n\n", getVoiceName());
        }

        out.format("# Voice-specific files\n");
        out.format("voice.%s.featureFile       = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(HALFPHONEFEATSAC).getName());
        out.format("voice.%s.targetCostWeights = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(HALFPHONEFEATDEFAC).getName());
        out.format("voice.%s.joinCostFile      = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(JOINCOSTFEATS).getName());
        out.format("voice.%s.joinCostWeights   = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(JOINCOSTFEATDEF).getName());
        out.format("voice.%s.unitsFile         = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(HALFPHONEUNITS).getName());
        out.format("voice.%s.cartFile          = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(CARTFILE).getName());
        out.format("voice.%s.audioTimelineFile = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                files.get(TIMELINE).getName());
        out.format("voice.%s.basenameTimeline  = MARY_BASE/lib/voices/%s/%s\n\n", getVoiceName(), getVoiceName(),
                files.get(BASETIMELINE).getName());

        // TODO is this ever used anymore?
        if (getVoiceDomain().equalsIgnoreCase("limited")) {
            out.format("# Location of example text\n");
            out.format("voice.%s.exampleTextFile = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(),
                    getProp(EXAMPLETEXT));
        }

        out.format("# Modules to use for predicting acoustic target features for this voice:\n\n");

        out.format("voice.%s.acousticModels = duration F0 midF0 rightF0\n\n", getVoiceName());

        out.format("voice.%s.duration.model = cart\n", getVoiceName());
        out.format("voice.%s.duration.data = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(), files.get(DURTREE)
                .getName());
        out.format("voice.%s.duration.attribute = d\n\n", getVoiceName());

        out.format("voice.%s.F0.model = cart\n", getVoiceName());
        out.format("voice.%s.F0.data = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(), files.get(F0LEFTTREE)
                .getName());
        out.format("voice.%s.F0.attribute = f0\n", getVoiceName());
        out.format("voice.%s.F0.attribute.format = (0,%%.0f)\n", getVoiceName());
        out.format("voice.%s.F0.predictFrom = firstVowels\n", getVoiceName());
        out.format("voice.%s.F0.applyTo = firstVoicedSegments\n\n", getVoiceName());

        out.format("voice.%s.midF0.model = cart\n", getVoiceName());
        out.format("voice.%s.midF0.data = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(), files.get(F0MIDTREE)
                .getName());
        out.format("voice.%s.midF0.attribute = f0\n", getVoiceName());
        out.format("voice.%s.midF0.attribute.format = (50,%%.0f)\n", getVoiceName());
        out.format("voice.%s.midF0.predictFrom = firstVowels\n", getVoiceName());
        out.format("voice.%s.midF0.applyTo = firstVowels\n\n", getVoiceName());

        out.format("voice.%s.rightF0.model = cart\n", getVoiceName());
        out.format("voice.%s.rightF0.data = MARY_BASE/lib/voices/%s/%s\n", getVoiceName(), getVoiceName(), files.get(F0RIGHTTREE)
                .getName());
        out.format("voice.%s.rightF0.attribute = f0\n", getVoiceName());
        out.format("voice.%s.rightF0.attribute.format = (100,%%.0f)\n", getVoiceName());
        out.format("voice.%s.rightF0.predictFrom = firstVowels\n", getVoiceName());
        out.format("voice.%s.rightF0.applyTo = lastVoicedSegments\n", getVoiceName());

        out.close();
        return configFile;
    }

    /**
     * Create zip file containing all of the voice files (including the config file, which should be in <b>files</b>).
     * 
     * @param files
     *            &lt;property, File&gt; Map, e.g. "WaveTimelineMaker.waveTimeline" &rarr;
     *            File("VOICE_DIR/mary/timeline_waves.mry")
     * @return the zip File object
     * @throws Exception
     */
    protected File createZipFile(HashMap<String, File> files) throws Exception {
        // TODO this should probably be optimized by using buffered Readers and Writer:
        byte[] buffer = new byte[4096];

        // initialize zip file:
        String zipFileName = String.format("mary-%s-%s.zip", getVoiceName(), getMaryVersion());
        logger.info("Creating voice package " + zipFileName);
        File zipFile = new File(getMaryBase() + "download" + File.separator + zipFileName);
        FileOutputStream outputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFile));

        // TODO this doesn't explicitly create each ancestor of the voicePath as a separate directory entry in the zip file, but
        // that doesn't seem necessary:
        String voicePath = "lib" + File.separator + "voices" + File.separator + getVoiceName() + File.separator;

        // iterate over files:
        for (String key : files.keySet()) {
            File file = files.get(key);

            // make new entry in zip file, with the appropriate target path:
            logger.debug("Deflating file " + file);
            if (key.equals("CONFIG")) {
                zipStream.putNextEntry(new ZipEntry("conf" + File.separator + file.getName()));
            } else {
                zipStream.putNextEntry(new ZipEntry(voicePath + file.getName()));
            }

            // open data file for reading:
            FileInputStream inputStream = new FileInputStream(file);
            int len;
            // and stream its contents into zip file:
            while ((len = inputStream.read(buffer)) > 0) {
                zipStream.write(buffer, 0, len);
            }

            // complete entry and close data file:
            zipStream.closeEntry();
            inputStream.close();
        }

        // close zip file:
        zipStream.close();

        return zipFile;
    }

    /**
     * Create component file for this voice. This includes various metadata, including the zip file name and MD5 hash, and several
     * other attributes.
     * 
     * @param zipFile
     * @throws Exception
     */
    protected void createComponentFile(File zipFile) throws Exception {

        logger.info("Hashing voice package");
        String zipFileMd5Hash = MD5.asHex(MD5.getHash(zipFile));

        String componentFileName = String.format("mary-%s-%s-component.xml", getVoiceName(), getMaryVersion());
        logger.info("Creating component file " + componentFileName);
        File componentFile = new File(getMaryBase() + File.separator + "download" + File.separator + componentFileName);
        PrintWriter out = new PrintWriter(componentFile);

        // avoid overhead of XML handling by generating the XML with raw strings:
        out.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.format("<marytts-install xmlns=\"http://mary.dfki.de/installer\">\n");
        out.format("    <voice gender=\"%s\" locale=\"%s\" name=\"%s\" type=\"%s\" version=\"%s\">\n", getVoiceGender(),
                getXMLCompatibleVoiceLocale(), getVoiceName(), getProp(VOICETYPE), getMaryVersion());
        out.format("        <description>%s</description>\n", getProp(VOICEDESCRIPTION));
        out.format("        <license href=\"%s\"/>\n", getProp(LICENSEURL));
        out.format("        <package filename=\"%s\"\n", zipFile.getName());
        out.format("            md5sum=\"%s\" size=\"%d\">\n", zipFileMd5Hash, zipFile.length());
        out.format("            <location href=\"http://mary.dfki.de/download/%s/\"/>\n", getMaryVersion());
        out.format("        </package>\n");
        out.format("        <depends language=\"%s\" version=\"%s\"/>\n", getXMLCompatibleVoiceLocale(), getMaryVersion());
        out.format("    </voice>\n");
        out.format("</marytts-install>\n");

        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getProgress() {
        // TODO Auto-generated method stub
        return -1;
    }

    // several getters:
    // TODO these really belong in DatabaseLayout!

    public String getVoiceLocale() {
        return MaryUtils.string2locale(db.getProp(db.LOCALE)).toString();
    }

    public String getXMLCompatibleVoiceLocale() {
        return MaryUtils.locale2xmllang(MaryUtils.string2locale(db.getProp(db.LOCALE)));
    }

    public String getVoiceName() {
        return db.getProp(db.VOICENAME).toLowerCase();
    }

    public String getVoiceFileDir() {
        return db.getProp(db.FILEDIR);
    }

    public String getMaryVersion() {
        return db.getProp(db.MARYBASEVERSION);
    }

    public String getVoiceGender() {
        return db.getProp(db.GENDER).toLowerCase();
    }

    public String getVoiceDomain() {
        return db.getProp(db.DOMAIN).toLowerCase();
    }

    public int getVoiceSamplingRate() {
        return Integer.parseInt(db.getProp(db.SAMPLINGRATE));
    }

    /**
     * Get the value of a property from the voice building DatabaseLayout, or from a VoiceImportComponent.
     * 
     * @param propertyName
     *            (e.g. "db.MARYBASE" or "VoicePackager.voiceType")
     * @return the property value
     * @throws NullPointerException
     *             if <b>propertyName</b> cannot be resolved
     */
    public String getProperty(String propertyName) {
        String[] propertyNameParts = propertyName.split("\\.");
        String component = propertyNameParts[0];
        String property = propertyNameParts[1];

        String value;
        if (component.equals("db")) {
            value = db.getProp(property);
        } else {
            VoiceImportComponent voiceImportComponent = db.getComponent(component);
            value = voiceImportComponent.getProp(propertyName);
        }
        if (value == null) {
            throw new NullPointerException(propertyName + " cannot be resolved!");
        }
        return value;
    }

    public String getMaryBase() {
        return db.getProp(db.MARYBASE) + File.separator;
    }

}
