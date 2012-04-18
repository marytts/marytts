package marytts;

import java.util.Locale;

import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.SynthesisException;

import org.w3c.dom.Document;

/**
 * A simple access API for using MARY TTS.
 * The same API can be used to interact with a local TTS runtime, in the same JVM,
 * or with a remote TTS server via a client-server protocol.
 * 
 * The basic idea is to use reasonable defaults when instantiating a MaryInterface,
 * but to let the user adapt all parts of it.
 * 
 * Examples of use:
 * 
 * <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * AudioInputStream audio = marytts.generateAudio("This is my text.");
 * </code>
 * 
 * Default voice in a different language:
 * <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setLocale(Locale.SWEDISH);
 * AudioInputStream audio = marytts.generateAudio("Välkommen till talsyntesens värld!");
 * </code>
 * 
 * Custom voice:
 * <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setVoice("dfki-pavoque-neutral"); // a German voice
 * AudioInputStream audio = marytts.generateAudio("Hallo und willkommen!");
 * </code>
 * 
 * Other input and output types:
 * <code>
 * MaryInterface marytts = new LocalMaryInterface();
 * marytts.setInputType("SSML");
 * marytts.setOutputType("TARGETFEATURES");
 * marytts.setLocale(Locale.SWEDISH);
 * Document ssmlDoc = DomUtils.parseDocument("myfile.ssml");
 * String targetfeatures = marytts.generateText(ssmlDoc);
 * </code>
 * 
 * The exact same syntax should work with the RemoteMaryInterface included in the marytts-client package:
 * <code>
 * MaryInterface marytts = new RemoteMaryInterface("localhost", 59125);
 * AudioInputStream audio = marytts.generateAudio("This is my text.");
 * </code>
 * 
 * 
 * 
 * @author marc
 *
 */
public interface MaryInterface {

	/**
	 * Set the input type for processing to the new input type.
	 * @param newInputType a string representation of a MaryDataType.
	 * @throws SynthesisException if newInputType is not a valid and known input data type.
	 */
	public void setInputType(String newInputType) throws SynthesisException;

	/**
	 * Get the current input type, either the default ("TEXT") or the value most recently set through {@link #setInputType(String)}.
	 * @return the currently set input type.
	 */
	public String getInputType();

	/**
	 * Set the output type for processing to the new output type.
	 * @param newOutputType a string representation of a MaryDataType.
	 * @throws SynthesisException if newOutputType is not a valid and known output data type.
	 */
	public void setOutputType(String newOutputType) throws SynthesisException;

	/**
	 * Get the current output type, either the default ("AUDIO") or the value most recently set through {@link #setInputType(String)}.
	 * @return the currently set input type.
	 */
	public String getOutputType();

	/**
	 * Set the locale for processing. Set the voice to the default voice for this locale.
	 * @param newLocale a supported locale.
	 * @throws SynthesisException if newLocale is not one of the supported locales.
	 */
	public void setLocale(Locale newLocale) throws SynthesisException;

	/**
	 * Get the current locale used for processing. Either the default (US English) or the value most recently set through {@link #setLocale(Locale)} or indirectly through {@link #setVoice(String)}.
	 * @return the locale
	 */
	public Locale getLocale();

	/**
	 * Set the voice to be used for processing. If the current locale differs from the voice's locale, the locale is updated accordingly.
	 * @param voiceName the name of a valid voice. 
	 * @throws SynthesisException
	 */
	public void setVoice(String voiceName) throws SynthesisException;

	/**
	 * The name of the current voice, if any.
	 * @return the voice name, or null if no voice is currently set.
	 */
	public String getVoice();

	/**
	 * Set the audio effects. For advanced use only.
	 * @param audioEffects
	 */
	public void setAudioEffects(String audioEffects);

	/**
	 * Get the currently set audio effects. For advanced use only.
	 * @return
	 */
	public String getAudioEffects();

	/**
	 * Set the speaking style. For advanced use only.
	 * @param params
	 */
	public void setStyle(String newStyle);

	/**
	 * Get the currently speaking style. For advanced use only.
	 * @return
	 */
	public String getStyle();

	/**
	 * Set the output type parameters. For advanced use only.
	 * @param params
	 */
	public void setOutputTypeParams(String params);

	/**
	 * Get the currently set output type parameters. For advanced use only.
	 * @return
	 */
	public String getOutputTypeParams();

	/**
	 * Set whether to stream audio. For advanced use only.
	 * @param isStreaming
	 */
	public void setStreamingAudio(boolean newIsStreaming);

	/**
	 * Whether to stream audio. For advanced use only.
	 * @return
	 */
	public boolean isStreamingAudio();

	public String generateText(String text) throws SynthesisException;

	public String generateText(Document doc) throws SynthesisException;

	public Document generateXML(String text) throws SynthesisException;

	public Document generateXML(Document doc) throws SynthesisException;

	public AudioInputStream generateAudio(String text)
			throws SynthesisException;

	public AudioInputStream generateAudio(Document doc)
			throws SynthesisException;

}