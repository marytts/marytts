package marytts;

import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.SynthesisException;

import org.w3c.dom.Document;

/**
 * A simple access API for using MARY TTS. The same API can be used to interact with a local TTS runtime, in the same JVM, or with
 * a remote TTS server via a client-server protocol.
 * 
 * The basic idea is to use reasonable defaults when instantiating a MaryInterface, but to let the user adapt all parts of it.
 * 
 * Examples of use:
 * 
 * <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * AudioInputStream audio = marytts.generateAudio("This is my text.");
 * </code>
 * 
 * Default voice in a different language: <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setLocale(Locale.SWEDISH);
 * AudioInputStream audio = marytts.generateAudio("Välkommen till talsyntesens värld!");
 * </code>
 * 
 * Custom voice: <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setVoice("dfki-pavoque-neutral"); // a German voice
 * AudioInputStream audio = marytts.generateAudio("Hallo und willkommen!");
 * </code>
 * 
 * Other input and output types: <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setInputType("SSML");
 * marytts.setOutputType("TARGETFEATURES");
 * marytts.setLocale(Locale.SWEDISH);
 * Document ssmlDoc = DomUtils.parseDocument("myfile.ssml");
 * String targetfeatures = marytts.generateText(ssmlDoc);
 * </code>
 * 
 * The exact same syntax should work with the RemoteMaryInterface included in the marytts-client package: <code>
 * MaryInterface marytts = new RemoteMaryInterface("localhost", 59125);
 * AudioInputStream audio = marytts.generateAudio("This is my text.");
 * </code>
 * 
 * Some introspection: <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * System.out.println("I currently have " + marytts.getAvailableVoices() + " voices in " + marytts.getAvailableLocales() + " languages available.");
 * System.out.println("Out of these, " + marytts.getAvailableVoices(Locale.US) + " are for US English.");
 * </code>
 * 
 * @author marc
 *
 */
public interface MaryInterface {

	/**
	 * Set the input type for processing to the new input type.
	 * 
	 * @param newInputType
	 *            a string representation of a MaryDataType.
	 * @throws IllegalArgumentException
	 *             if newInputType is not a valid and known input data type.
	 */
	public void setInputType(String newInputType) throws IllegalArgumentException;

	/**
	 * Get the current input type, either the default ("TEXT") or the value most recently set through
	 * {@link #setInputType(String)}.
	 * 
	 * @return the currently set input type.
	 */
	public String getInputType();

	/**
	 * Set the output type for processing to the new output type.
	 * 
	 * @param newOutputType
	 *            a string representation of a MaryDataType.
	 * @throws IllegalArgumentException
	 *             if newOutputType is not a valid and known output data type.
	 */
	public void setOutputType(String newOutputType) throws IllegalArgumentException;

	/**
	 * Get the current output type, either the default ("AUDIO") or the value most recently set through
	 * {@link #setInputType(String)}.
	 * 
	 * @return the currently set input type.
	 */
	public String getOutputType();

	/**
	 * Set the locale for processing. Set the voice to the default voice for this locale.
	 * 
	 * @param newLocale
	 *            a supported locale.
	 * @throws IllegalArgumentException
	 *             if newLocale is not among the {@link #getAvailableLocales()}.
	 */
	public void setLocale(Locale newLocale) throws IllegalArgumentException;

	/**
	 * Get the current locale used for processing. Either the default (US English) or the value most recently set through
	 * {@link #setLocale(Locale)} or indirectly through {@link #setVoice(String)}.
	 * 
	 * @return the locale
	 */
	public Locale getLocale();

	/**
	 * Set the voice to be used for processing. If the current locale differs from the voice's locale, the locale is updated
	 * accordingly.
	 * 
	 * @param voiceName
	 *            the name of a valid voice.
	 * @throws IllegalArgumentException
	 *             if voiceName is not among the {@link #getAvailableVoices()}.
	 */
	public void setVoice(String voiceName) throws IllegalArgumentException;

	/**
	 * The name of the current voice, if any.
	 * 
	 * @return the voice name, or null if no voice is currently set.
	 */
	public String getVoice();

	/**
	 * Set the audio effects. For advanced use only.
	 * 
	 * @param audioEffects
	 *            audioEffects
	 */
	public void setAudioEffects(String audioEffects);

	/**
	 * Get the currently set audio effects. For advanced use only.
	 * 
	 * @return audio effects
	 */
	public String getAudioEffects();

	/**
	 * Set the speaking style. For advanced use only.
	 * 
	 * @param newStyle
	 *            newStyle
	 */
	public void setStyle(String newStyle);

	/**
	 * Get the currently speaking style. For advanced use only.
	 * 
	 * @return style
	 */
	public String getStyle();

	/**
	 * Set the output type parameters. For advanced use only.
	 * 
	 * @param params
	 *            params
	 */
	public void setOutputTypeParams(String params);

	/**
	 * Get the currently set output type parameters. For advanced use only.
	 * 
	 * @return output type params
	 * 
	 */
	public String getOutputTypeParams();

	/**
	 * Set whether to stream audio. For advanced use only.
	 * 
	 * @param newIsStreaming
	 *            newIsStreaming
	 */
	public void setStreamingAudio(boolean newIsStreaming);

	/**
	 * Whether to stream audio. For advanced use only.
	 * 
	 * @return stream audio
	 */
	public boolean isStreamingAudio();

	/**
	 * Partial processing command, converting an input text format such as TEXT into an output text format such as TARGETFEATURES.
	 * 
	 * @param text
	 *            text
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return text
	 */
	public String generateText(String text) throws SynthesisException;

	/**
	 * Partial processing command, converting an input XML format such as SSML into an output text format such as TARGETFEATURES.
	 * 
	 * @param doc
	 *            doc
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return text
	 */
	public String generateText(Document doc) throws SynthesisException;

	/**
	 * Partial processing command, converting an input text format such as TEXT into an XML format such as ALLOPHONES.
	 * 
	 * @param text
	 *            text
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return xml
	 */
	public Document generateXML(String text) throws SynthesisException;

	/**
	 * Partial processing command, converting one XML format such as RAWMARYXML into another XML format such as TOKENS.
	 * 
	 * @param doc
	 *            doc
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return xml
	 */
	public Document generateXML(Document doc) throws SynthesisException;

	/**
	 * Synthesis from a text format to audio. This is the method you want to call for text-to-speech conversion.
	 * 
	 * @param text
	 *            text
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return audio
	 */
	public AudioInputStream generateAudio(String text) throws SynthesisException;

	/**
	 * Synthesis from an XML format, such as SSML, to audio.
	 * 
	 * @param doc
	 *            doc
	 * @throws SynthesisException
	 *             SynthesisException
	 * @return audio
	 */
	public AudioInputStream generateAudio(Document doc) throws SynthesisException;

	/**
	 * List the names of all the voices that can be used in {@link #setVoice(String)}.
	 * 
	 * @return voices
	 * 
	 */
	public Set<String> getAvailableVoices();

	/**
	 * List the names of all the voices for the given locale that can be used in {@link #setVoice(String)}.
	 * 
	 * @param locale
	 *            locale
	 * @return voices
	 */
	public Set<String> getAvailableVoices(Locale locale);

	/**
	 * List the locales that can be used in {@link #setLocale(Locale)}.
	 * 
	 * @return locales
	 * 
	 */
	public Set<Locale> getAvailableLocales();

	/**
	 * List the names of the input types that can be used in {@link #setInputType(String)}.
	 * 
	 * @return inputtypes
	 * 
	 */
	public Set<String> getAvailableInputTypes();

	/**
	 * List the names of the input types that can be used in {@link #setInputType(String)}.
	 * 
	 * @return output types
	 * 
	 */
	public Set<String> getAvailableOutputTypes();

	/**
	 * Check whether the given data type is a text type. For input types (i.e. types contained in
	 * {@link #getAvailableInputTypes()}) that are text types, the synthesis methods {@link #generateText(String)},
	 * {@link #generateXML(String)} and {@link #generateAudio(String)} can be used; for output types that are text types, the
	 * synthesis methods {@link #generateText(String)} and {@link #generateText(Document)} can be used.
	 * 
	 * @param dataType
	 *            an input or output data type.
	 * @return type
	 */
	public boolean isTextType(String dataType);

	/**
	 * Check whether the given data type is an XML type. For input types (i.e. types contained in
	 * {@link #getAvailableInputTypes()}) that are XML types, the synthesis methods {@link #generateText(Document)},
	 * {@link #generateXML(Document)} and {@link #generateAudio(Document)} can be used; for output types that are XML types, the
	 * synthesis methods {@link #generateXML(String)} and {@link #generateXML(Document)} can be used.
	 * 
	 * @param dataType
	 *            an input or output data type.
	 * @return type
	 */
	public boolean isXMLType(String dataType);

	/**
	 * Check whether the given data type is an audio type. There are no input audio types; for output audio types, the methods
	 * {@link #generateAudio(String)} and {@link #generateAudio(Document)} can be used.
	 * 
	 * @param dataType
	 *            an input or output data type
	 * @return type
	 */
	public boolean isAudioType(String dataType);

}