/**
 * Copyright 2007 DFKI GmbH.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.http.Address;
import marytts.util.math.MathUtils;
import marytts.util.string.StringUtils;

/**
 *
 * This class nests all the information and functions that a Mary client needs to receive/send data from/to server. To be able to
 * use the functionality provided by this class, all Mary clients should either: (i) extend this class (Example: MaryHttpClient,
 * MaryWebHttpClient) (ii) use an object of type MaryHttpForm or of a derived class (Example: MaryGUIClient)
 * 
 * @author Oytun T&uuml;rk
 */
public class MaryFormData {
	// Default values which can be overridden from the command line.
	private final String DEFAULT_HOST = "localhost";
	private final int DEFAULT_PORT = 59125;

	public Address hostAddress = null;
	public String serverVersionInfo = null;
	public String serverVersionNo = "unknown";
	public boolean serverCanStream = false;

	public Vector<MaryClient.Voice> allVoices;
	public Map<Locale, Vector<MaryClient.Voice>> voicesByLocaleMap;
	public Map<String, Vector<String>> limitedDomainVoices;
	public Set<Locale> locales;
	public Vector<MaryClient.DataType> allDataTypes;
	public Vector<MaryClient.DataType> inputDataTypes;
	public Vector<MaryClient.DataType> outputDataTypes;
	public Map<String, String> serverExampleTexts;
	public String currentExampleText;
	public Map<String, Vector<String>> voiceExampleTextsLimitedDomain;
	public Map<String, String> voiceExampleTextsGeneralDomain;
	public Map<String, String> audioEffectHelpTextsMap;
	public Vector<String> audioFileFormatTypes;
	public Vector<String> audioOutTypes;
	public String inputText;
	public String outputText;
	public String errorMessage;
	public boolean isOutputText;
	public int voiceSelected;
	public int inputTypeSelected;
	public int outputTypeSelected;
	public int audioFormatSelected;
	public int audioOutSelected;
	public int limitedDomainExampleTextSelected;
	public String audioEffects;
	public String audioEffectsHelpTextLineBreak;
	public AudioEffectsBoxData effectsBoxData;
	public Vector<String> limitedDomainExampleTexts;

	public Map<String, String> keyValuePairs; // Key-Value pairs for communication with server

	public String outputAudioResponseID; // output audio file for web browser client
	public String mimeType; // MIME type for output audio (web browser clients)

	public MaryFormData() {
		String serverHost = System.getProperty("server.host", DEFAULT_HOST);
		int serverPort = 0;
		String helperString = System.getProperty("server.port");
		if (helperString != null)
			serverPort = Integer.decode(helperString).intValue();
		else
			serverPort = DEFAULT_PORT;

		Address serverAddress = new Address(serverHost, serverPort);
		init(serverAddress, null, null, null, null, null, null, null, null);
	}

	public MaryFormData(Address serverAddress) {
		init(serverAddress, null, null, null, null, null, null, null, null);
	}

	public MaryFormData(Address serverAddress, String versionIn, String voicesIn, String dataTypesIn,
			String audioFileFormatTypesIn, String audioEffectHelpTextLineBreakIn, String defaultAudioEffects,
			Vector<String> defaultVoiceExampleTexts) {
		this(serverAddress, null, versionIn, voicesIn, dataTypesIn, audioFileFormatTypesIn, audioEffectHelpTextLineBreakIn,
				defaultAudioEffects, defaultVoiceExampleTexts);
	}

	public MaryFormData(Address serverAddress, Map<String, String> keyValuePairsIn, String versionIn, String voicesIn,
			String dataTypesIn, String audioFileFormatTypesIn, String audioEffectHelpTextLineBreakIn, String defaultAudioEffects,
			Vector<String> defaultVoiceExampleTexts) {
		init(serverAddress, keyValuePairsIn, versionIn, voicesIn, dataTypesIn, audioFileFormatTypesIn,
				audioEffectHelpTextLineBreakIn, defaultAudioEffects, defaultVoiceExampleTexts);
	}

	public void init(Address serverAddress, Map<String, String> keyValuePairsIn, String versionIn, String voicesIn,
			String dataTypesIn, String audioFileFormatTypesIn, String audioEffectHelpTextLineBreakIn, String defaultAudioEffects,
			Vector<String> defaultVoiceExampleTexts) {
		outputAudioResponseID = "";
		mimeType = "";
		hostAddress = null;
		serverVersionInfo = null;
		serverVersionNo = "unknown";
		serverCanStream = false;

		allVoices = null;
		voicesByLocaleMap = null;
		limitedDomainVoices = new HashMap<String, Vector<String>>();
		allDataTypes = null;
		inputDataTypes = null;
		outputDataTypes = null;
		serverExampleTexts = new HashMap<String, String>();
		currentExampleText = "";
		voiceExampleTextsLimitedDomain = new HashMap<String, Vector<String>>();
		voiceExampleTextsGeneralDomain = new HashMap<String, String>();
		audioEffectHelpTextsMap = new HashMap<String, String>();
		audioFileFormatTypes = null;
		audioOutTypes = null;
		inputText = "";
		outputText = "";
		isOutputText = false;
		voiceSelected = 0;
		inputTypeSelected = 0;
		outputTypeSelected = 0;
		audioFormatSelected = 0;
		audioOutSelected = 0;
		limitedDomainExampleTextSelected = 0;
		audioEffects = "";
		audioEffectsHelpTextLineBreak = "";
		effectsBoxData = null;
		keyValuePairs = new HashMap<String, String>();
		limitedDomainExampleTexts = null;

		hostAddress = serverAddress;

		toServerVersionInfo(versionIn);
		toVoices(voicesIn);
		toDataTypes(dataTypesIn);
		toAudioFileFormatAndOutTypes(audioFileFormatTypesIn);
		toAudioEffectsHelpTextLineBreak(audioEffectHelpTextLineBreakIn);
		toAudioEffects(defaultAudioEffects);
		if (keyValuePairsIn != null) {
			toSelections(keyValuePairsIn, defaultVoiceExampleTexts);
		}
	}

	public void toServerVersionInfo(String info) {
		serverVersionInfo = info;

		serverVersionNo = "unknown";

		if (serverVersionInfo != null) {
			String[] parts = serverVersionInfo.split(" ");
			if (parts[0].equals("Mary") && parts[1].equals("TTS") && parts[2].equals("server") && parts.length >= 4) {
				// then parts[3] is the version number
				serverVersionNo = parts[3];
			}
		}

		if (serverVersionNo.equals("unknown") || serverVersionNo.compareTo("3.0.1") < 0) {
			serverCanStream = false;
		} else {
			serverCanStream = true;
		}
	}

	public void toVoices(String info) {
		allVoices = null;
		voicesByLocaleMap = null;
		limitedDomainVoices = null;

		if (info != null && info.length() > 0) {
			allVoices = new Vector<MaryClient.Voice>();
			voicesByLocaleMap = new HashMap<Locale, Vector<MaryClient.Voice>>();
			limitedDomainVoices = new HashMap<String, Vector<String>>();
			String[] voiceStrings = info.split("\n");

			for (int i = 0; i < voiceStrings.length; i++) {
				StringTokenizer st = new StringTokenizer(voiceStrings[i]);
				if (!st.hasMoreTokens())
					continue; // ignore entry
				String name = st.nextToken();
				if (!st.hasMoreTokens())
					continue; // ignore entry
				String localeString = st.nextToken();
				Locale locale = string2locale(localeString);
				assert locale != null;
				if (!st.hasMoreTokens())
					continue; // ignore entry
				String gender = st.nextToken();

				MaryClient.Voice voice = null;
				if (isServerVersionAtLeast("3.5.0")) {
					String synthesizerType;
					if (!st.hasMoreTokens())
						synthesizerType = "non-specified";
					else
						synthesizerType = st.nextToken();

					if (!st.hasMoreTokens()) {
						// assume domain is general
						voice = new MaryClient.Voice(name, locale, gender, "general");
					} else {
						// read in the domain
						String domain = st.nextToken();
						voice = new MaryClient.Voice(name, locale, gender, domain);
					}

					voice.setSynthesizerType(synthesizerType);
				} else {
					if (!st.hasMoreTokens()) {
						// assume domain is general
						voice = new MaryClient.Voice(name, locale, gender, "general");
					} else {
						// read in the domain
						String domain = st.nextToken();
						voice = new MaryClient.Voice(name, locale, gender, domain);
					}
				}

				allVoices.add(voice);
				Vector<MaryClient.Voice> localeVoices = null;
				if (voicesByLocaleMap.containsKey(locale)) {
					localeVoices = voicesByLocaleMap.get(locale);
				} else {
					localeVoices = new Vector<MaryClient.Voice>();
					voicesByLocaleMap.put(locale, localeVoices);
				}
				localeVoices.add(voice);
			}
		}
	}

	public void toDataTypes(String info) {
		allDataTypes = null;
		inputDataTypes = null;
		outputDataTypes = null;

		if (info != null && info.length() > 0) {
			allDataTypes = new Vector<MaryClient.DataType>();
			inputDataTypes = new Vector<MaryClient.DataType>();
			outputDataTypes = new Vector<MaryClient.DataType>();

			String[] typeStrings = info.split("\n");

			for (int i = 0; i < typeStrings.length; i++) {
				StringTokenizer st = new StringTokenizer(typeStrings[i]);
				if (!st.hasMoreTokens())
					continue; // ignore this type
				String name = st.nextToken();
				boolean isInputType = false;
				boolean isOutputType = false;
				Locale locale = null;
				while (st.hasMoreTokens()) {
					String t = st.nextToken();
					if (t.equals("INPUT")) {
						isInputType = true;
					} else if (t.equals("OUTPUT")) {
						isOutputType = true;
					}
				}
				MaryClient.DataType dt = new MaryClient.DataType(name, isInputType, isOutputType);
				allDataTypes.add(dt);
				if (dt.isInputType()) {
					inputDataTypes.add(dt);
				}
				if (dt.isOutputType()) {
					outputDataTypes.add(dt);
				}
			}
		}
	}

	public void toLocales(String info) {
		locales = new HashSet<Locale>();
		for (String localeName : StringUtils.toStringArray(info)) {
			locales.add(MaryUtils.string2locale(localeName));
		}
	}

	public void toAudioFileFormatAndOutTypes(String info) {
		// TODO: this method uses code which is meaningful only
		// in the server (MaryAudioUtils.canCreateMP3() etc.).
		// It should be moved into the server code.
		audioFileFormatTypes = null;
		audioOutTypes = null;

		String[] allTypes = null;
		int spaceInd;

		if (info != null && info.length() > 0)
			allTypes = StringUtils.toStringArray(info);

		if (allTypes != null) {
			for (int i = 0; i < allTypes.length; i++) {
				spaceInd = allTypes[i].indexOf(' ');
				String typeName = allTypes[i].substring(spaceInd + 1);
				Type audioType = null;
				boolean isSupported = true;
				if (typeName.equals("MP3"))
					isSupported = false; // MaryServerUtils.canCreateMP3();
				else if (typeName.equals("Vorbis"))
					isSupported = false; // MaryServerUtils.canCreateOgg();
				try {
					audioType = MaryAudioUtils.getAudioFileFormatType(typeName);
				} catch (Exception e) {
					isSupported = false;
				}

				if (isSupported && audioType != null && AudioSystem.isFileTypeSupported(audioType)) {
					if (audioFileFormatTypes == null)
						audioFileFormatTypes = new Vector<String>();

					audioFileFormatTypes.add(allTypes[i]);

					if (audioOutTypes == null)
						audioOutTypes = new Vector<String>();

					audioOutTypes.add(typeName + "_FILE");

					if (typeName.compareTo("MP3") == 0)
						audioOutTypes.add(typeName + "_STREAM");
				}
			}
		}
	}

	public void toAudioEffectsHelpTextLineBreak(String strLineBreak) {
		if (strLineBreak != null && strLineBreak.length() > 0)
			audioEffectsHelpTextLineBreak = strLineBreak;
		else
			audioEffectsHelpTextLineBreak = null;
	}

	public void toAudioEffects(String availableAudioEffects) {
		if (availableAudioEffects != null && availableAudioEffects.length() > 0)
			audioEffects = availableAudioEffects;
		else
			audioEffects = null;

		if (audioEffects != null && audioEffects.length() > 0)
			effectsBoxData = new AudioEffectsBoxData(audioEffects);
		else
			effectsBoxData = null;
	}

	// Parse fullParamaters which is of the form key1=value1&key2=value2...
	private void toSelections(Map<String, String> keyValuePairsIn, Vector<String> defaultVoiceExampleTexts) {
		assert keyValuePairsIn != null;
		keyValuePairs = keyValuePairsIn;

		inputTypeSelected = 0;
		inputText = "";
		if (outputDataTypes != null && outputDataTypes.size() > 0)
			outputTypeSelected = outputDataTypes.size() - 1;
		else
			outputTypeSelected = 0;

		isOutputText = false;
		outputText = "";
		audioFormatSelected = 0;
		voiceSelected = 0;
		limitedDomainExampleTextSelected = 0;

		if (effectsBoxData == null) {
			/*
			 * if (audioEffectsHelpTextLineBreak==null) getAudioEffectHelpTextLineBreak(); if (audioEffects==null)
			 * getAudioEffects();
			 */
			effectsBoxData = new AudioEffectsBoxData(audioEffects);
		}

		int i;
		String selected;

		// Input type selected
		selected = keyValuePairs.get("INPUT_TYPE");
		if (selected != null) {
			for (i = 0; i < inputDataTypes.size(); i++) {
				if (inputDataTypes.get(i).name().compareTo(selected) == 0) {
					inputTypeSelected = i;
					break;
				}
			}
		}

		// Output type selected
		selected = keyValuePairs.get("OUTPUT_TYPE");
		if (selected != null) {
			for (i = 0; i < outputDataTypes.size(); i++) {
				if (outputDataTypes.get(i).name().compareTo(selected) == 0) {
					outputTypeSelected = i;
					break;
				}
			}

			// Check if output type contains AUDIO
			if (outputDataTypes.get(outputTypeSelected).name().contains("AUDIO"))
				isOutputText = false;
			else
				isOutputText = true;
		}
		//

		// Voice selected
		selected = keyValuePairs.get("VOICE");
		if (selected != null) {
			for (i = 0; i < allVoices.size(); i++) {
				if (allVoices.get(i).name().compareTo(selected) == 0) {
					voiceSelected = i;
					break;
				}
			}
		}
		//

		// Limited domain example texts
		if (allVoices != null && allVoices.size() > 0) {
			if (allVoices.elementAt(voiceSelected).isLimitedDomain()) {
				limitedDomainExampleTexts = defaultVoiceExampleTexts;

				selected = keyValuePairs.get("exampletext");
				if (limitedDomainExampleTexts != null && selected != null) {
					for (i = 0; i < limitedDomainExampleTexts.size(); i++) {
						if (limitedDomainExampleTexts.get(i).compareTo(selected) == 0) {
							limitedDomainExampleTextSelected = i;
							break;
						}
					}
				} else {
					limitedDomainExampleTextSelected = 0;
				}
			}
		}

		// Input text
		selected = keyValuePairs.get("INPUT_TEXT");
		if (selected != null)
			inputText = selected;
		else {
			if (allVoices != null && allVoices.size() > 0 && inputDataTypes != null && inputDataTypes.size() > 0) {
				if (allVoices.elementAt(voiceSelected).isLimitedDomain() && limitedDomainExampleTexts != null)
					inputText = limitedDomainExampleTexts.get(limitedDomainExampleTextSelected);
				else if (serverExampleTexts != null)
					inputText = serverExampleTexts.get(inputDataTypes.get(inputTypeSelected).name() + " "
							+ allVoices.elementAt(voiceSelected).getLocale().toString());
			}
		}
		//

		// Output text if non-audio output
		if (isOutputText) {
			selected = keyValuePairs.get("OUTPUT_TEXT");
			if (selected != null)
				outputText = selected;
		}
		//

		// Audio out format selected:
		// The clients can send audio format in two ways:
		selected = keyValuePairs.get("AUDIO_OUT");

		int spaceInd;
		int scoreInd;
		if (selected != null) {
			scoreInd = selected.indexOf('_');
			String selectedTypeName = selected.substring(scoreInd + 1);
			for (i = 0; i < audioFileFormatTypes.size(); i++) {
				spaceInd = audioFileFormatTypes.get(i).indexOf(' ');
				String typeName = audioFileFormatTypes.get(i).substring(spaceInd + 1);
				if (typeName.compareTo(selected) == 0) {
					audioFormatSelected = i;
					break;
				}
			}

			for (i = 0; i < audioOutTypes.size(); i++) {
				String typeName = audioOutTypes.get(i);
				if (typeName.compareTo(selected) == 0) {
					audioOutSelected = i;
					break;
				}
			}
		}
		//

		// Audio effects
		String currentEffectName;
		for (i = 0; i < effectsBoxData.getTotalEffects(); i++) {
			currentEffectName = effectsBoxData.getControlData(i).getEffectName();

			// Check box
			selected = keyValuePairs.get("effect_" + currentEffectName + "_selected");
			if (selected != null && selected.compareTo("on") == 0) // Effect is selected
				effectsBoxData.getControlData(i).setSelected(true);
			else
				// If not found, effect is not selected
				effectsBoxData.getControlData(i).setSelected(false);
			//

			// Parameters
			selected = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
			if (selected != null) // Effect paramaters is there
				effectsBoxData.getControlData(i).setParams(selected);
			else
				// If not found, something is wrong, set parameters to default
				effectsBoxData.getControlData(i).setEffectParamsToExample();
			//
		}

	}

	public boolean isServerVersionAtLeast(String serverVersionToCompare) {
		if (serverVersionNo.equals("unknown"))
			return false;

		int tmp = serverVersionNo.compareToIgnoreCase(serverVersionToCompare);
		return tmp >= 0;
	}

	// Check if all selections are appropriately made, i.e. no array bounds exceeded etc
	public void checkAndCorrectSelections() {
		audioFormatSelected = MathUtils.CheckLimits(audioFormatSelected, 0, audioFileFormatTypes.size() - 1);
		audioOutSelected = MathUtils.CheckLimits(audioOutSelected, 0, audioOutTypes.size() - 1);
		inputTypeSelected = MathUtils.CheckLimits(inputTypeSelected, 0, inputDataTypes.size() - 1);
		outputTypeSelected = MathUtils.CheckLimits(outputTypeSelected, 0, outputDataTypes.size() - 1);
		voiceSelected = MathUtils.CheckLimits(voiceSelected, 0, allVoices.size() - 1);
	}

	/**
	 * This helper method converts a string (e.g., "en_US") into a proper Locale object.
	 * 
	 * @param localeString
	 *            a string representation of the locale
	 * @return a Locale object.
	 */
	public static Locale string2locale(String localeString) {
		Locale locale = null;
		StringTokenizer localeST = new StringTokenizer(localeString, "_");
		String language = localeST.nextToken();
		String country = "";
		String variant = "";
		if (localeST.hasMoreTokens()) {
			country = localeST.nextToken();
			if (localeST.hasMoreTokens()) {
				variant = localeST.nextToken();
			}
		}
		locale = new Locale(language, country, variant);
		return locale;
	}
}
