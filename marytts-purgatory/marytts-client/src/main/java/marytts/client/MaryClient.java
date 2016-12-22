/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.client;

// General Java Classes
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;

import marytts.Version;
import marytts.client.http.MaryHttpClient;
import marytts.util.http.Address;

/**
 * An HTTP client implementing the MARY protocol. It can be used as a command line client or from within java code.
 * 
 * @author Marc Schr&ouml;der, oytun.turk
 * @see MaryGUIClient A GUI interface to this client
 * @see marytts.server.MaryServer Description of the MARY protocol
 */

public abstract class MaryClient {
	public static MaryClient getMaryClient() throws IOException {
		return getMaryClient(null);
	}

	/**
	 * The typical way to create a mary client. It will connect to the MARY server running at the given host and port, either as a
	 * HTTP or as a Socket server. This constructor reads two system properties:
	 * <ul>
	 * <li><code>mary.client.profile</code> (=true/false) - determines whether profiling (timing) information is calculated;</li>
	 * <li><code>mary.client.quiet</code> (=true/false) - tells the client not to print any of the normal information to stderr.</li>
	 * </ul>
	 * 
	 * @param serverAddress
	 *            address of the server
	 * @throws IOException
	 *             if communication with the server fails
	 * @return getMaryClient(serverAddress, profile, quiet)
	 */
	public static MaryClient getMaryClient(Address serverAddress) throws IOException {
		boolean profile = Boolean.getBoolean("mary.client.profile");
		boolean quiet = Boolean.getBoolean("mary.client.quiet");
		return getMaryClient(serverAddress, profile, quiet);
	}

	public static MaryClient getMaryClient(Address serverAddress, boolean profile, boolean quiet) throws IOException {
		MaryClient m = null;
		try {
			m = new MaryHttpClient(serverAddress, profile, quiet);
		} catch (IOException ioe) {
			try {
				m = new MarySocketClient(serverAddress, profile, quiet);
			} catch (IOException ioe2) {
				IOException ioe3 = new IOException("Cannot connect either to a HTTP nor to a Socket MARY server at "
						+ (serverAddress != null ? serverAddress.getFullAddress() : "default location"));
				ioe3.initCause(ioe2);
				throw ioe3;
			}
		}
		return m;
	}

	protected MaryFormData data;
	protected boolean beQuiet = false;
	protected boolean doProfile = false;

	/**
	 * The simplest way to create a mary client. It will connect to the MARY server running on localhost.
	 * 
	 * @throws IOException
	 *             if communication with the server fails
	 */
	protected MaryClient() throws IOException {
		boolean profile = Boolean.getBoolean("mary.client.profile");
		boolean quiet = Boolean.getBoolean("mary.client.quiet");
		data = new MaryFormData(); // will try to get server address from system properties
		initialise(profile, quiet);
	}

	protected MaryClient(boolean quiet) throws IOException {
		boolean profile = Boolean.getBoolean("mary.client.profile");
		data = new MaryFormData(); // will try to get server address from system properties

		initialise(profile, quiet);
	}

	/**
	 * The typical way to create a mary client. It will connect to the MARY client running at the given host and port. This
	 * constructor reads two system properties:
	 * <ul>
	 * <li><code>mary.client.profile</code> (=true/false) - determines whether profiling (timing) information is calculated;</li>
	 * <li><code>mary.client.quiet</code> (=true/false) - tells the client not to print any of the normal information to stderr.</li>
	 * </ul>
	 * 
	 * @param serverAddress
	 *            the address of the server
	 * @throws IOException
	 *             if communication with the server fails
	 */
	protected MaryClient(Address serverAddress) throws IOException {
		boolean profile = Boolean.getBoolean("mary.client.profile");
		boolean quiet = Boolean.getBoolean("mary.client.quiet");
		if (serverAddress != null) {
			data = new MaryFormData(serverAddress);
		} else {
			data = new MaryFormData(); // will try to get server address from system properties
		}

		initialise(profile, quiet);
	}

	/**
	 * An alternative way to create a mary client, which works with applets. It will connect to the MARY client running at the
	 * given host and port. Note that in applets, the host must be the same as the one from which the applet was loaded;
	 * otherwise, a security exception is thrown.
	 * 
	 * @param serverAddress
	 *            the address of the server
	 * @param profile
	 *            determines whether profiling (timing) information is calculated
	 * @param quiet
	 *            tells the client not to print any of the normal information to stderr
	 * @throws IOException
	 *             if communication with the server fails
	 */
	protected MaryClient(Address serverAddress, boolean profile, boolean quiet) throws IOException {
		if (serverAddress != null) {
			data = new MaryFormData(serverAddress);
		} else {
			data = new MaryFormData(); // will try to get server address from system properties
		}

		initialise(profile, quiet);
	}

	/**
	 * Initialise a connection to the MARY server at the specified host and port.
	 * 
	 * @param profile
	 *            whether to do profiling
	 * @param quiet
	 *            whether to refrain from printing information to stderr
	 * @throws IOException
	 *             if communication with the server fails
	 */
	protected final void initialise(boolean profile, boolean quiet) throws IOException {
		// This must work for applets too, so no system property queries here:
		doProfile = profile;
		beQuiet = quiet;

		String[] info;
		if (!beQuiet)
			System.err.println("Mary TTS client " + Version.specificationVersion() + " (impl. " + Version.implementationVersion()
					+ ")");

		try {
			fillServerVersion();
		} catch (IOException e1) {
			IOException ioe = new IOException("MARY client cannot connect to MARY server at\n"
					+ data.hostAddress.getFullAddress() + "\n" + "Make sure that you have started the mary server\n"
					+ "or specify a different host or port using \n" + "maryclient -Dserver.host=my.host.com -Dserver.port=12345");
			ioe.initCause(e1);
			throw ioe;
		}
		if (data.serverVersionInfo == null || !data.serverVersionInfo.startsWith("Mary")) {
			throw new IOException("This does not seem to be the expected kind of MARY server at "
					+ data.hostAddress.getFullAddress() + "...");
		}

		if (!"unknown".equals(data.serverVersionNo) && !isServerVersionAtLeast("4.0")) {
			throw new IOException("Found old MARY server (version " + data.serverVersionNo
					+ ") -- this client will only work with servers of version 4.0 or newer.");
		}

		if (!beQuiet) {
			System.err.print("Connected to " + data.hostAddress.getFullAddress() + ", ");
			System.err.println(data.serverVersionInfo);

			if (!data.serverCanStream) {
				System.err
						.println("Server version " + data.serverVersionNo + " cannot stream audio, defaulting to non-streaming");
			}
		}

		fillVoices();
		// Limited domain example texts
		if (data.allVoices != null && data.allVoices.size() > 0) {
			if (data.allVoices.elementAt(data.voiceSelected).isLimitedDomain()) {
				data.limitedDomainExampleTexts = getVoiceExampleTextsLimitedDomain(data.allVoices.elementAt(data.voiceSelected)
						.name());
			}
		}

		// Input text
		if (data.allVoices != null && data.allVoices.size() > 0 && data.inputDataTypes != null && data.inputDataTypes.size() > 0) {
			if (data.allVoices.elementAt(data.voiceSelected).isLimitedDomain())
				data.inputText = data.limitedDomainExampleTexts.get(data.limitedDomainExampleTextSelected);
			else
				data.inputText = getServerExampleText(data.inputDataTypes.get(data.inputTypeSelected).name(), data.allVoices
						.elementAt(data.voiceSelected).getLocale().toString());
		}
	}

	// /////////////////////////////////////////////////////////////////////
	// ////////////////////// Information requests /////////////////////////
	// /////////////////////////////////////////////////////////////////////

	public Address getAddress() {
		return data.hostAddress;
	}

	public String getHost() {
		return data.hostAddress.getHost();
	}

	public int getPort() {
		return data.hostAddress.getPort();
	}

	/**
	 * Get the audio file format types known by the server, one per line. Each line has the format: <code>extension name</code>
	 * 
	 * @return data.audioFileFormatTypes if data.audioFileFormatTypes == null || data.audioOutTypes == null
	 * @throws IOException
	 *             IOException
	 */
	public Vector<String> getAudioFileFormatTypes() throws IOException {
		if (data.audioFileFormatTypes == null || data.audioOutTypes == null) {
			fillAudioFileFormatAndOutTypes();
		}
		return data.audioFileFormatTypes;
	}

	public Vector<String> getAudioOutTypes() throws IOException {
		if (data.audioFileFormatTypes == null || data.audioOutTypes == null) {
			fillAudioFileFormatAndOutTypes();
		}
		return data.audioOutTypes;
	}

	protected abstract void fillAudioFileFormatAndOutTypes() throws IOException;

	protected abstract void fillServerVersion() throws IOException;

	public boolean isServerVersionAtLeast(String version) throws IOException {
		if (data.serverVersionNo.equals("unknown"))
			fillServerVersion();
		return data.isServerVersionAtLeast(version);
	}

	/**
	 * Obtain a list of all data types known to the server. If the information is not yet available, the server is queried. This
	 * is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @throws IOException
	 *             if communication with the server fails
	 * @return data.allDataTypes
	 */
	public Vector<MaryClient.DataType> getAllDataTypes() throws IOException {
		if (data.allDataTypes == null)
			fillDataTypes();

		assert data.allDataTypes != null && data.allDataTypes.size() > 0;

		return data.allDataTypes;
	}

	/**
	 * Obtain a list of input data types known to the server. If the information is not yet available, the server is queried. This
	 * is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @return a Vector of MaryHttpClient.DataType objects.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.DataType> getInputDataTypes() throws IOException {
		if (data.inputDataTypes == null)
			fillDataTypes();

		assert data.inputDataTypes != null && data.inputDataTypes.size() > 0;

		return data.inputDataTypes;
	}

	/**
	 * Obtain a list of output data types known to the server. If the information is not yet available, the server is queried.
	 * This is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @return a Vector of MaryHttpClient.DataType objects.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.DataType> getOutputDataTypes() throws IOException {
		if (data.outputDataTypes == null)
			fillDataTypes();

		assert data.outputDataTypes != null && data.outputDataTypes.size() > 0;

		return data.outputDataTypes;
	}

	protected abstract void fillDataTypes() throws IOException;

	/**
	 * Provide a list of voices known to the server. If the information is not yet available, query the server for it. This is
	 * optional information which is not required for the normal operation of the client, but may help to avoid incompatibilities.
	 * 
	 * @return a Vector of MaryHttpClient.Voice objects.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getVoices() throws IOException {
		if (data.allVoices == null)
			fillVoices();

		assert data.allVoices != null && data.allVoices.size() > 0;

		return data.allVoices;
	}

	/**
	 * Provide a list of voices known to the server for the given locale. If the information is not yet available, query the
	 * server for it. This is optional information which is not required for the normal operation of the client, but may help to
	 * avoid incompatibilities.
	 * 
	 * @param locale
	 *            the requested voice locale
	 * @return a Vector of MaryHttpClient.Voice objects, or null if no voices exist for that locale.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getVoices(Locale locale) throws IOException {
		if (data.allVoices == null)
			fillVoices();

		return data.voicesByLocaleMap.get(locale);
	}

	/**
	 * Provide a list of general domain voices known to the server. If the information is not yet available, query the server for
	 * it. This is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getGeneralDomainVoices() throws IOException {
		Vector<MaryClient.Voice> voices = getVoices();
		Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();

		for (MaryClient.Voice v : voices) {
			if (!v.isLimitedDomain())
				requestedVoices.add(v);
		}

		if (!requestedVoices.isEmpty())
			return requestedVoices;
		else
			return null;
	}

	/**
	 * Provide a list of limited domain voices known to the server. If the information is not yet available, query the server for
	 * it. This is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getLimitedDomainVoices() throws IOException {
		Vector<MaryClient.Voice> voices = getVoices();
		Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();

		for (MaryClient.Voice v : voices) {
			if (v.isLimitedDomain())
				requestedVoices.add(v);
		}

		if (!requestedVoices.isEmpty())
			return requestedVoices;
		else
			return null;
	}

	/**
	 * Provide a list of general domain voices known to the server. If the information is not yet available, query the server for
	 * it. This is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @param locale
	 *            the requested voice locale
	 * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getGeneralDomainVoices(Locale locale) throws IOException {
		Vector<MaryClient.Voice> voices = getVoices(locale);
		Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();

		for (MaryClient.Voice v : voices) {
			if (!v.isLimitedDomain())
				requestedVoices.add(v);
		}

		if (!requestedVoices.isEmpty())
			return requestedVoices;
		else
			return null;
	}

	/**
	 * Provide a list of limited domain voices known to the server. If the information is not yet available, query the server for
	 * it. This is optional information which is not required for the normal operation of the client, but may help to avoid
	 * incompatibilities.
	 * 
	 * @param locale
	 *            the requested voice locale
	 * @return a Vector of MaryHttpClient.Voice objects, or null if no such voices exist.
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public Vector<MaryClient.Voice> getLimitedDomainVoices(Locale locale) throws IOException {
		Vector<MaryClient.Voice> voices = getVoices(locale);
		Vector<MaryClient.Voice> requestedVoices = new Vector<MaryClient.Voice>();
		for (MaryClient.Voice v : voices) {
			if (v.isLimitedDomain())
				requestedVoices.add(v);
		}

		if (!requestedVoices.isEmpty())
			return requestedVoices;
		else
			return null;
	}

	protected abstract void fillVoices() throws IOException;

	public Set<Locale> getLocales() throws IOException {
		if (data.locales == null) {
			fillLocales();
		}
		return data.locales;
	}

	protected abstract void fillLocales() throws IOException;

	/**
	 * Request the example texts of a limited domain unit selection voice from the server
	 * 
	 * @param voicename
	 *            the voice
	 * @return the example text
	 * @throws IOException
	 *             IOException
	 */
	public Vector<String> getVoiceExampleTextsLimitedDomain(String voicename) throws IOException {
		if (!data.voiceExampleTextsLimitedDomain.containsKey(voicename)) {
			fillVoiceExampleTexts(voicename);
		}
		return data.voiceExampleTextsLimitedDomain.get(voicename);
	}

	protected abstract void fillVoiceExampleTexts(String voicename) throws IOException;

	/**
	 * Request an example text for a given data type from the server.
	 * 
	 * @param dataType
	 *            the string representation of the data type, e.g. "RAWMARYXML". This is optional information which is not
	 *            required for the normal operation of the client, but may help to avoid incompatibilities.
	 * @return the example text, or null if none could be obtained.
	 * @param locale
	 *            locale
	 * @throws IOException
	 *             if communication with the server fails
	 */
	public String getServerExampleText(String dataType, String locale) throws IOException {
		if (!data.serverExampleTexts.containsKey(dataType + " " + locale)) {
			fillServerExampleText(dataType, locale);
		}

		data.currentExampleText = data.serverExampleTexts.get(dataType + " " + locale);

		return data.currentExampleText;
	}

	protected abstract void fillServerExampleText(String dataType, String locale) throws IOException;

	/**
	 * Request the available audio effects for a voice from the server
	 * 
	 * 
	 * @return A string of available audio effects and default parameters, i.e. "FIRFilter,Robot(amount=50)"
	 * @throws IOException
	 *             IOException
	 */
	protected abstract String getDefaultAudioEffects() throws IOException;

	public String getAudioEffects() throws IOException {
		if (data.audioEffects == null)
			data.audioEffects = getDefaultAudioEffects();

		return data.audioEffects;
	}

	public abstract String requestDefaultEffectParameters(String effectName) throws IOException;

	public abstract String requestFullEffect(String effectName, String currentEffectParameters) throws IOException;

	public abstract boolean isHMMEffect(String effectName) throws IOException;

	public String requestEffectHelpText(String effectName) throws IOException {
		if (!data.audioEffectHelpTextsMap.containsKey(effectName)) {
			fillEffectHelpText(effectName);
		}
		return data.audioEffectHelpTextsMap.get(effectName);
	}

	protected abstract void fillEffectHelpText(String effectName) throws IOException;

	public abstract String getFeatures(String locale) throws IOException;

	public abstract String getFeaturesForVoice(String voice) throws IOException;

	// /////////////////////////////////////////////////////////////////////
	// ////////////////////// Actual synthesis requests ////////////////////
	// /////////////////////////////////////////////////////////////////////

	/**
	 * Call the mary client to stream audio via the given audio player. The server will provide audio data as it is being
	 * generated. If the connection to the server is not too slow, streaming will be attractive because it reduces considerably
	 * the amount of time one needs to wait for the first audio to play.
	 * 
	 * @param input
	 *            a textual representation of the input data
	 * @param inputType
	 *            the name of the input data type, e.g. TEXT or RAWMARYXML.
	 * @param locale
	 *            locale
	 * @param audioType
	 *            the name of the audio format, e.g. "WAVE" or "MP3".
	 * @param defaultVoiceName
	 *            the name of the voice to use, e.g. de7 or us1.
	 * @param defaultStyle
	 *            defaultStyle
	 * @param defaultEffects
	 *            defaultEffects
	 * @param audioPlayer
	 *            the FreeTTS audio player with which to play the synthesized audio data. The given audio player must already be
	 *            instantiated. See the package <code>com.sun.speech.freetts.audio</code> in FreeTTS for implementations of
	 *            AudioPlayer.
	 * @param listener
	 *            a means for letting calling code know that the AudioPlayer has finished.
	 * @throws IOException
	 *             if communication with the server fails
	 * @see #getInputDataTypes()
	 * @see #getVoices()
	 */
	public void streamAudio(String input, String inputType, String locale, String audioType, String defaultVoiceName,
			String defaultStyle, String defaultEffects, marytts.util.data.audio.AudioPlayer audioPlayer,
			AudioPlayerListener listener) throws IOException {
		_process(input, inputType, "AUDIO", locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, audioPlayer, 0,
				true, null, listener);
	}

	/**
	 * The standard way to call the MARY client when the output is to go to an output stream.
	 * 
	 * @param input
	 *            a textual representation of the input data
	 * @param inputType
	 *            the name of the input data type, e.g. TEXT or RAWMARYXML.
	 * @param outputType
	 *            the name of the output data type, e.g. AUDIO or ACOUSTPARAMS.
	 * @param audioType
	 *            the name of the audio format, e.g. "WAVE" or "MP3".
	 * @param defaultVoiceName
	 *            the name of the voice to use, e.g. de7 or us1.
	 * @param locale
	 *            locale
	 * @param defaultStyle
	 *            defaultStyle
	 * @param defaultEffects
	 *            defaultEffects
	 * @param outputTypeParams
	 *            any additional parameters, e.g. for output type TARGETFEATURES, the space-separated list of features to produce.
	 *            Can be null.
	 * @param output
	 *            the output stream into which the data from the server is to be written.
	 * @throws IOException
	 *             if communication with the server fails
	 * @see #getInputDataTypes()
	 * @see #getOutputDataTypes()
	 * @see #getVoices()
	 */
	public void process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, String defaultStyle, String defaultEffects, String outputTypeParams, OutputStream output)
			throws IOException {
		_process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output, 0,
				false, outputTypeParams, null);
	}

	public void process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, OutputStream output) throws IOException {
		process(input, inputType, outputType, locale, audioType, defaultVoiceName, "", null, null, output);
	}

	/**
	 * An alternative way to call the MARY client when the output is to go to an output stream, with a timeout.
	 * 
	 * @param input
	 *            a textual representation of the input data
	 * @param inputType
	 *            the name of the input data type, e.g. TEXT or RAWMARYXML.
	 * @param outputType
	 *            the name of the output data type, e.g. AUDIO or ACOUSTPARAMS.
	 * @param audioType
	 *            the name of the audio format, e.g. "WAVE" or "MP3".
	 * @param defaultVoiceName
	 *            the name of the voice to use, e.g. de7 or us1.
	 * @param locale
	 *            locale
	 * @param defaultStyle
	 *            defaultStyle
	 * @param defaultEffects
	 *            defaultEffects
	 * @param outputTypeParams
	 *            any additional parameters, e.g. for output type TARGETFEATURES, the space-separated list of features to produce.
	 *            Can be null.
	 * @param output
	 *            the output stream into which the data from the server is to be written.
	 * @param timeout
	 *            if &gt;0, sets a timer to as many milliseconds; if processing is not finished by then, the connection with the
	 *            Mary server is forcefully cut, resulting in an IOException.
	 * @throws IOException
	 *             if communication with the server fails
	 * @see #getInputDataTypes()
	 * @see #getOutputDataTypes()
	 * @see #getVoices()
	 */
	public void process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, String defaultStyle, String defaultEffects, String outputTypeParams, OutputStream output,
			long timeout) throws IOException {
		_process(input, inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects, output,
				timeout, false, outputTypeParams, null);
	}

	public void process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, OutputStream output, long timeout) throws IOException {
		process(input, inputType, outputType, locale, audioType, defaultVoiceName, "", null, null, output, timeout);
	}

	protected abstract void _process(String input, String inputType, String outputType, String locale, String audioType,
			String defaultVoiceName, String defaultStyle, String defaultEffects, Object output, long timeout,
			boolean streamingAudio, String outputTypeParams, AudioPlayerListener playerListener) throws IOException;

	/**
	 * Return an audio file format type for the given string. In addition to the built-in types, this can deal with MP3 supported
	 * by tritonus.
	 * 
	 * @param name
	 *            name
	 * @return the audio file format type if it is known, or null.
	 */
	public static AudioFileFormat.Type getAudioFileFormatType(String name) {
		AudioFileFormat.Type at;
		if (name.equals("MP3")) {
			// Supported by tritonus plugin
			at = new AudioFileFormat.Type("MP3", "mp3");
		} else if (name.equals("Vorbis")) {
			// supported by tritonus plugin
			at = new AudioFileFormat.Type("Vorbis", "ogg");
		} else {
			try {
				at = (AudioFileFormat.Type) AudioFileFormat.Type.class.getField(name).get(null);
			} catch (Exception e) {
				return null;
			}
		}

		return at;
	}

	/**
	 * An abstraction of server info about available voices.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 *
	 */
	public static class Voice {
		private String name;
		private Locale locale;
		private String gender;
		private String domain;
		private String synthesizerType;

		private boolean isLimitedDomain;

		public Voice(String name, Locale locale, String gender, String domain) {
			this.name = name;
			this.locale = locale;
			this.gender = gender;
			this.domain = domain;
			if (domain == null || domain.equals("general")) {
				isLimitedDomain = false;
			} else {
				isLimitedDomain = true;
			}

			this.synthesizerType = "not-specified";
		}

		public Locale getLocale() {
			return locale;
		}

		public String name() {
			return name;
		}

		public String gender() {
			return gender;
		}

		public String synthesizerType() {
			return synthesizerType;
		}

		public void setSynthesizerType(String synthesizerTypeIn) {
			synthesizerType = synthesizerTypeIn;
		}

		public String toString() {
			return name + " (" + locale.getDisplayLanguage() + ", " + gender + (isLimitedDomain ? ", " + domain : "") + ")";
		}

		public boolean isLimitedDomain() {
			return isLimitedDomain;
		}

		public boolean isHMMVoice() {
			return synthesizerType.compareToIgnoreCase("hmm") == 0;
		}
	}

	/**
	 * An abstraction of server info about available data types.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 *
	 */
	public static class DataType {
		private String name;
		private boolean isInputType;
		private boolean isOutputType;

		public DataType(String name, boolean isInputType, boolean isOutputType) {
			this.name = name;
			this.isInputType = isInputType;
			this.isOutputType = isOutputType;
		}

		public String name() {
			return name;
		}

		public boolean isInputType() {
			return isInputType;
		}

		public boolean isOutputType() {
			return isOutputType;
		}

		public boolean isTextType() {
			return !name.equals("AUDIO");
		}

		public String toString() {
			return name;
		}
	}

	/**
	 * A means of letting a caller code know that the audioplayer has finished.
	 * 
	 * @author Marc Schr&ouml;der
	 *
	 */
	public static interface AudioPlayerListener {
		/**
		 * Notify the listener that the audio player has finished.
		 *
		 */
		public void playerFinished();

		/**
		 * Inform the listener that the audio player has thrown an exception.
		 * 
		 * @param e
		 *            the exception thrown
		 */
		public void playerException(Exception e);
	}

	public static class WarningReader extends Thread {
		protected BufferedReader in;
		protected StringBuffer warnings;

		public WarningReader(BufferedReader in) {
			this.in = in;
			warnings = new StringBuffer();
		}

		public String getWarnings() {
			return warnings.toString();
		}

		public void run() {
			char[] cbuf = new char[1024];
			int nr;
			try {
				while ((nr = in.read(cbuf)) != -1) {
					// warnings from the server
					warnings.append(cbuf, 0, nr);
				}
			} catch (IOException ioe) {
			}
		}
	}

	public static void usage() {
		System.err.println("usage:");
		System.err.println("java [properties] " + MaryHttpClient.class.getName() + " [inputfile]");
		System.err.println();
		System.err.println("Properties are: -Dinput.type=INPUTTYPE");
		System.err.println("                -Doutput.type=OUTPUTTYPE");
		System.err.println("                -Dlocale=LOCALE");
		System.err.println("                -Daudio.type=AUDIOTYPE");
		System.err.println("                -Dvoice.default=male|female|de1|de2|de3|...");
		System.err.println("                -Dserver.host=HOSTNAME");
		System.err.println("                -Dserver.port=PORTNUMBER");
		System.err.println("where INPUTTYPE is one of TEXT, RAWMARYXML, TOKENS, WORDS, POS,");
		System.err.println("                          PHONEMES, INTONATION, ALLOPHONES, ACOUSTPARAMS or MBROLA,");
		System.err.println("     OUTPUTTYPE is one of TOKENS, WORDS, POS, PHONEMES,");
		System.err.println("                          INTONATION, ALLOPHONES, ACOUSTPARAMS, MBROLA, or AUDIO,");
		System.err.println("     LOCALE is the language and/or the country (e.g., de, en_US);");
		System.err.println("and AUDIOTYPE is one of AIFF, AU, WAVE, MP3, and Vorbis.");
		System.err.println("The default values for input.type and output.type are TEXT and AUDIO,");
		System.err.println("respectively; default locale is en_US; the default audio.type is WAVE.");
		System.err.println();
		System.err.println("inputfile must be of type input.type.");
		System.err.println("If no inputfile is given, the program will read from standard input.");
		System.err.println();
		System.err.println("The output is written to standard output, so redirect or pipe as appropriate.");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length > 0 && args[0].equals("-h")) {
			usage();
			System.exit(1);
		}

		MaryClient mc = getMaryClient();
		BufferedReader inputReader = null;
		// read requested input/output type from properties:
		String inputType = System.getProperty("input.type", "TEXT");
		String outputType = System.getProperty("output.type", "AUDIO");
		String locale = System.getProperty("locale", "en_US");
		String audioType = System.getProperty("audio.type", "WAVE");
		if (!(audioType.equals("AIFC") || audioType.equals("AIFF") || audioType.equals("AU") || audioType.equals("SND")
				|| audioType.equals("WAVE") || audioType.equals("MP3") || audioType.equals("Vorbis"))) {
			System.err.println("Invalid value '" + audioType + "' for property 'audio.type'");
			System.err.println();
			usage();
			System.exit(1);
		}
		String defaultVoiceName = System.getProperty("voice.default");
		String defaultStyle = "";
		String defaultEffects = null;
		String outputTypeParams = System.getProperty("output.type.params"); // null if not present

		if (args.length > 0) {
			File file = new File(args[0]);
			inputReader = new BufferedReader(new FileReader(file));
		} else { // no Filename, read from stdin:
			inputReader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		}

		// Read input into a string:
		StringBuilder sb = new StringBuilder(1024);
		char[] buf = new char[1024];
		int nr;
		while ((nr = inputReader.read(buf)) != -1) {
			sb.append(buf, 0, nr);
		}

		try {
			mc.process(sb.toString(), inputType, outputType, locale, audioType, defaultVoiceName, defaultStyle, defaultEffects,
					outputTypeParams, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
