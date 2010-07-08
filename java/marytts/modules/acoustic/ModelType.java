package marytts.modules.acoustic;

/**
 * List of known model types as constants; can be extended but needs to mesh with Classes extending {@link Model} and switch statement in
 * {@linkplain marytts.modules.synthesis.Voice#Voice(String[], java.util.Locale, javax.sound.sampled.AudioFormat, marytts.modules.synthesis.WaveformSynthesizer, marytts.modules.synthesis.Voice.Gender)
 * <code>Voice()</code>}:
 * 
 * @author steiner
 * 
 */
public enum ModelType {
    // enumerate model types here:
    CART;

    // get the appropriate model type from a string (which can be lower or mixed case):
    // adapted from http://www.xefer.com/2006/12/switchonstring
    public static ModelType fromString(String string) {
        try {
            ModelType modelString = valueOf(string.toUpperCase());
            return modelString;
        } catch (Exception e) {
            return null;
        }
    }
}
