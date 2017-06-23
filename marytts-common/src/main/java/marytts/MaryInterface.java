package marytts;

import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.exceptions.SynthesisException;

import org.w3c.dom.Document;

/**
 * A simple access API for using MARY TTS. The same API can be used to interact
 * with a local TTS runtime, in the same JVM, or with a remote TTS server via a
 * client-server protocol.
 *
 * The basic idea is to use reasonable defaults when instantiating a
 * MaryInterface, but to let the user adapt all parts of it.
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
 * The exact same syntax should work with the RemoteMaryInterface included in
 * the marytts-client package: <code>
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
     * Set the locale for processing. Set the voice to the default voice for
     * this locale.
     *
     * @param newLocale
     *            a supported locale.
     * @throws IllegalArgumentException
     *             if newLocale is not among the {@link #getAvailableLocales()}.
     */
    public void setLocale(Locale newLocale) throws IllegalArgumentException;

    /**
     * Get the current locale used for processing. Either the default (US
     * English) or the value most recently set through
     * {@link #setLocale(Locale)}
     *
     * @return the locale
     */
    public Locale getLocale();

    /**
     * List the locales that can be used in {@link #setLocale(Locale)}.
     *
     * @return locales
     *
     */
    public Set<Locale> getAvailableLocales();
}
