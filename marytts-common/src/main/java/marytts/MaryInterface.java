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
}
