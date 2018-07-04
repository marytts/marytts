package marytts.phonetic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class IPA
{
    public static final Map<Character, Set<String>> ipa_cat_map;
    public static final Map<String, Set<Character>> cat_ipa_map;


    static {
	Map<Character, Set<String>> tmp_map = new HashMap<Character, Set<String>>();
	Set<String> tmp = new HashSet<String>();
	tmp.add("syllable_break");
	tmp_map.put('.', tmp);

	tmp = new HashSet<String>();
	tmp.add("silence");
	tmp_map.put('_', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('a', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('b', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("palatal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('c', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('d', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('e', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('f', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('h', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('i', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("glide");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put('j', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("velar");
	tmp_map.put('k', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('l', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('m', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('n', tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('o', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('p', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("uvular");
	tmp_map.put('q', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("voiced");
	tmp_map.put('r', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('s', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('t', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('u', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('v', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put('w', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("velar");
	tmp_map.put('x', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('y', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('z', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ã', tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ä', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('æ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ç', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("low_or_fourth_tone");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('è', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("high_or_second_tone");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('é', tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp.add("close");
	tmp.add("front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ï', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ð', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ñ', tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('õ', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ø', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("pharyngeal");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put('ħ', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ĩ', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("short");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ĭ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put('ŋ', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open-mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('œ', tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ǀ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ǁ', tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("palatal");
	tmp.add("unvoiced");
	tmp_map.put('ǂ', tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put('ǃ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɐ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɑ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɒ', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("non-pulmonic");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put('ɓ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("open-mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɔ', tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ɕ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put('ɖ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put('ɗ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɘ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ə', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close-mid");
	tmp.add("rhoticity");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɚ', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɛ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɜ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open-mid");
	tmp.add("rhoticity");
	tmp.add("voiced");
	tmp.add("vowel");

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɞ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("palatal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɟ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put('ɠ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put('ɡ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put('ɢ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put('ɣ', tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("near-back");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɤ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put('ɥ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɦ', tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ɧ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɨ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-close");
	tmp.add("near-front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɪ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("velarized_or_pharyngealized");
	tmp.add("voiced");
	tmp_map.put('ɫ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ɬ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put('ɭ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɮ', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("near-back");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɯ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put('ɰ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɱ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɲ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put('ɳ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put('ɴ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɵ', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ɶ', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ɸ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ɹ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("flap");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put('ɺ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put('ɻ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("flap");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put('ɽ', tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("flap");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put('ɾ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put('ʀ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put('ʁ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put('ʂ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palato-alveolar");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ʃ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("palatal");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put('ʄ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put('ʈ', tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ʉ', tmp);

	tmp = new HashSet<String>();
	tmp.add("back");
	tmp.add("near-close");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ʊ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ʋ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ʌ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("unvoiced");
	tmp_map.put('ʍ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ʎ', tmp);

	tmp = new HashSet<String>();
	tmp.add("near-close");
	tmp.add("near-front");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ʏ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put('ʐ', tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ʑ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palato-alveolar");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ʒ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ʔ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("pharyngeal");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("voiced");
	tmp_map.put('ʕ', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('ʘ', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("voiced");
	tmp_map.put('ʙ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put('ʛ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put('ʜ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('ʝ', tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put('ʟ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put('ʡ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("voiced");
	tmp_map.put('ʢ', tmp);

	tmp = new HashSet<String>();
	tmp.add("aspirated");
	tmp_map.put('ʰ', tmp);

	tmp = new HashSet<String>();
	tmp.add("palatalized");
	tmp_map.put('ʲ', tmp);

	tmp = new HashSet<String>();
	tmp.add("labialized");
	tmp_map.put('ʷ', tmp);

	tmp = new HashSet<String>();
	tmp.add("primary_stress");
	tmp_map.put('ˈ', tmp);

	tmp = new HashSet<String>();
	tmp.add("secondary_stress");
	tmp_map.put('ˌ', tmp);

	tmp = new HashSet<String>();
	tmp.add("long");
	tmp_map.put('ː', tmp);

	tmp = new HashSet<String>();
	tmp.add("rhoticity");
	tmp_map.put('˞', tmp);

	tmp = new HashSet<String>();
	tmp.add("velarized");
	tmp_map.put('ˠ', tmp);

	tmp = new HashSet<String>();
	tmp.add("lateral_release");
	tmp_map.put('ˡ', tmp);

	tmp = new HashSet<String>();
	tmp.add("pharyngealized");
	tmp_map.put('ˤ', tmp);

	tmp = new HashSet<String>();
	tmp.add("low_or_fourth_tone");
	tmp_map.put('̀', tmp);

	tmp = new HashSet<String>();
	tmp.add("high_or_second_tone");
	tmp_map.put('́', tmp);

	tmp = new HashSet<String>();
	tmp.add("nasalized");
	tmp_map.put('̃', tmp);

	tmp = new HashSet<String>();
	tmp.add("mid_or_first_tone");
	tmp_map.put('̄', tmp);

	tmp = new HashSet<String>();
	tmp.add("short");
	tmp_map.put('̆', tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp_map.put('̈', tmp);

	tmp = new HashSet<String>();
	tmp.add("rising_or_third_tone");
	tmp_map.put('̌', tmp);

	tmp = new HashSet<String>();
	tmp.add("advanced_tongue_root");
	tmp_map.put('̘', tmp);

	tmp = new HashSet<String>();
	tmp.add("retracted_tongue_root");
	tmp_map.put('̙', tmp);

	tmp = new HashSet<String>();
	tmp.add("no_audible_release");
	tmp_map.put('̚', tmp);

	tmp = new HashSet<String>();
	tmp.add("less_rounded");
	tmp_map.put('̜', tmp);

	tmp = new HashSet<String>();
	tmp.add("raised");
	tmp_map.put('̝', tmp);

	tmp = new HashSet<String>();
	tmp.add("lowered");
	tmp_map.put('̞', tmp);

	tmp = new HashSet<String>();
	tmp.add("advanced");
	tmp_map.put('̟', tmp);

	tmp = new HashSet<String>();
	tmp.add("retracted");
	tmp_map.put('̠', tmp);

	tmp = new HashSet<String>();
	tmp.add("breathy_voiced");
	tmp_map.put('̤', tmp);

	tmp = new HashSet<String>();
	tmp.add("voiceless");
	tmp_map.put('̥', tmp);

	tmp = new HashSet<String>();
	tmp.add("syllabic");
	tmp_map.put('̩', tmp);

	tmp = new HashSet<String>();
	tmp.add("dental");
	tmp_map.put('̪', tmp);

	tmp = new HashSet<String>();
	tmp.add("voiced");
	tmp_map.put('̬', tmp);

	tmp = new HashSet<String>();
	tmp.add("non_syllabic");
	tmp_map.put('̯', tmp);

	tmp = new HashSet<String>();
	tmp.add("creaky_voiced");
	tmp_map.put('̰', tmp);

	tmp = new HashSet<String>();
	tmp.add("velarized_or_pharyngealized");
	tmp_map.put('̴', tmp);

	tmp = new HashSet<String>();
	tmp.add("more_rounded");
	tmp_map.put('̹', tmp);

	tmp = new HashSet<String>();
	tmp.add("apical");
	tmp_map.put('̺', tmp);

	tmp = new HashSet<String>();
	tmp.add("laminal");
	tmp_map.put('̻', tmp);

	tmp = new HashSet<String>();
	tmp.add("linguolabial");
	tmp_map.put('̼', tmp);

	tmp = new HashSet<String>();
	tmp.add("mid_centralized");
	tmp_map.put('̽', tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put('β', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put('θ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("uvular");
	tmp_map.put('χ', tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ẽ', tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put('ỹ', tmp);

	tmp = new HashSet<String>();
	tmp.add("syllable_linking");
	tmp_map.put('‿', tmp);

	tmp = new HashSet<String>();
	tmp.add("nasal_release");
	tmp_map.put('ⁿ', tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("flap");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put('ⱱ', tmp);
	ipa_cat_map = Collections.unmodifiableMap(tmp_map);


	HashMap<String, Set<Character>> tmp2_map = new HashMap<String, Set<Character>>();
	HashSet<Character> tmp2 = new HashSet<Character>();
	tmp2.add('.');
	tmp2_map.put("syllable_break", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('_');
	tmp2_map.put("silence", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('a');
	tmp2.add('e');
	tmp2.add('i');
	tmp2.add('y');
	tmp2.add('ã');
	tmp2.add('ä');
	tmp2.add('æ');
	tmp2.add('è');
	tmp2.add('é');
	tmp2.add('ï');
	tmp2.add('ø');
	tmp2.add('ĩ');
	tmp2.add('ĭ');
	tmp2.add('œ');
	tmp2.add('ɛ');
	tmp2.add('ɶ');
	tmp2.add('ẽ');
	tmp2.add('ỹ');
	tmp2_map.put("front", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('a');
	tmp2.add('ã');
	tmp2.add('ä');
	tmp2.add('ɑ');
	tmp2.add('ɒ');
	tmp2.add('ɞ');
	tmp2.add('ɶ');
	tmp2_map.put("near-open", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('a');
	tmp2.add('b');
	tmp2.add('d');
	tmp2.add('e');
	tmp2.add('i');
	tmp2.add('j');
	tmp2.add('l');
	tmp2.add('m');
	tmp2.add('n');
	tmp2.add('o');
	tmp2.add('r');
	tmp2.add('u');
	tmp2.add('v');
	tmp2.add('w');
	tmp2.add('y');
	tmp2.add('z');
	tmp2.add('ã');
	tmp2.add('ä');
	tmp2.add('æ');
	tmp2.add('è');
	tmp2.add('é');
	tmp2.add('ï');
	tmp2.add('ð');
	tmp2.add('ñ');
	tmp2.add('õ');
	tmp2.add('ø');
	tmp2.add('ĩ');
	tmp2.add('ĭ');
	tmp2.add('ŋ');
	tmp2.add('œ');
	tmp2.add('ɐ');
	tmp2.add('ɑ');
	tmp2.add('ɒ');
	tmp2.add('ɓ');
	tmp2.add('ɔ');
	tmp2.add('ɖ');
	tmp2.add('ɗ');
	tmp2.add('ɘ');
	tmp2.add('ə');
	tmp2.add('ɚ');
	tmp2.add('ɛ');
	tmp2.add('ɜ');
	tmp2.add('ɞ');
	tmp2.add('ɟ');
	tmp2.add('ɠ');
	tmp2.add('ɡ');
	tmp2.add('ɢ');
	tmp2.add('ɣ');
	tmp2.add('ɤ');
	tmp2.add('ɥ');
	tmp2.add('ɦ');
	tmp2.add('ɨ');
	tmp2.add('ɪ');
	tmp2.add('ɫ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɯ');
	tmp2.add('ɰ');
	tmp2.add('ɱ');
	tmp2.add('ɲ');
	tmp2.add('ɳ');
	tmp2.add('ɴ');
	tmp2.add('ɵ');
	tmp2.add('ɶ');
	tmp2.add('ɹ');
	tmp2.add('ɺ');
	tmp2.add('ɻ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ʀ');
	tmp2.add('ʁ');
	tmp2.add('ʄ');
	tmp2.add('ʉ');
	tmp2.add('ʊ');
	tmp2.add('ʋ');
	tmp2.add('ʌ');
	tmp2.add('ʎ');
	tmp2.add('ʏ');
	tmp2.add('ʐ');
	tmp2.add('ʑ');
	tmp2.add('ʒ');
	tmp2.add('ʕ');
	tmp2.add('ʙ');
	tmp2.add('ʛ');
	tmp2.add('ʝ');
	tmp2.add('ʟ');
	tmp2.add('ʢ');
	tmp2.add('̬');
	tmp2.add('β');
	tmp2.add('ẽ');
	tmp2.add('ỹ');
	tmp2.add('ⱱ');
	tmp2_map.put("voiced", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('a');
	tmp2.add('e');
	tmp2.add('i');
	tmp2.add('o');
	tmp2.add('u');
	tmp2.add('y');
	tmp2.add('ã');
	tmp2.add('ä');
	tmp2.add('æ');
	tmp2.add('è');
	tmp2.add('é');
	tmp2.add('ï');
	tmp2.add('õ');
	tmp2.add('ø');
	tmp2.add('ĩ');
	tmp2.add('ĭ');
	tmp2.add('œ');
	tmp2.add('ɐ');
	tmp2.add('ɑ');
	tmp2.add('ɒ');
	tmp2.add('ɔ');
	tmp2.add('ɘ');
	tmp2.add('ə');
	tmp2.add('ɚ');
	tmp2.add('ɛ');
	tmp2.add('ɜ');
	tmp2.add('ɞ');
	tmp2.add('ɤ');
	tmp2.add('ɨ');
	tmp2.add('ɪ');
	tmp2.add('ɯ');
	tmp2.add('ɵ');
	tmp2.add('ɶ');
	tmp2.add('ʉ');
	tmp2.add('ʊ');
	tmp2.add('ʌ');
	tmp2.add('ʏ');
	tmp2.add('ẽ');
	tmp2.add('ỹ');
	tmp2_map.put("vowel", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('b');
	tmp2.add('m');
	tmp2.add('p');
	tmp2.add('ɓ');
	tmp2.add('ɸ');
	tmp2.add('ʘ');
	tmp2.add('ʙ');
	tmp2.add('β');
	tmp2_map.put("bilabial", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('b');
	tmp2.add('c');
	tmp2.add('d');
	tmp2.add('f');
	tmp2.add('h');
	tmp2.add('j');
	tmp2.add('k');
	tmp2.add('l');
	tmp2.add('m');
	tmp2.add('n');
	tmp2.add('p');
	tmp2.add('q');
	tmp2.add('r');
	tmp2.add('s');
	tmp2.add('t');
	tmp2.add('v');
	tmp2.add('w');
	tmp2.add('x');
	tmp2.add('z');
	tmp2.add('ç');
	tmp2.add('ð');
	tmp2.add('ñ');
	tmp2.add('ħ');
	tmp2.add('ŋ');
	tmp2.add('ǀ');
	tmp2.add('ǁ');
	tmp2.add('ǂ');
	tmp2.add('ǃ');
	tmp2.add('ɓ');
	tmp2.add('ɕ');
	tmp2.add('ɖ');
	tmp2.add('ɗ');
	tmp2.add('ɟ');
	tmp2.add('ɠ');
	tmp2.add('ɡ');
	tmp2.add('ɢ');
	tmp2.add('ɣ');
	tmp2.add('ɥ');
	tmp2.add('ɦ');
	tmp2.add('ɧ');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɰ');
	tmp2.add('ɱ');
	tmp2.add('ɲ');
	tmp2.add('ɳ');
	tmp2.add('ɴ');
	tmp2.add('ɸ');
	tmp2.add('ɹ');
	tmp2.add('ɺ');
	tmp2.add('ɻ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ʀ');
	tmp2.add('ʁ');
	tmp2.add('ʂ');
	tmp2.add('ʃ');
	tmp2.add('ʄ');
	tmp2.add('ʈ');
	tmp2.add('ʋ');
	tmp2.add('ʍ');
	tmp2.add('ʎ');
	tmp2.add('ʐ');
	tmp2.add('ʑ');
	tmp2.add('ʒ');
	tmp2.add('ʔ');
	tmp2.add('ʕ');
	tmp2.add('ʘ');
	tmp2.add('ʙ');
	tmp2.add('ʛ');
	tmp2.add('ʜ');
	tmp2.add('ʝ');
	tmp2.add('ʟ');
	tmp2.add('ʡ');
	tmp2.add('ʢ');
	tmp2.add('β');
	tmp2.add('θ');
	tmp2.add('χ');
	tmp2.add('ⱱ');
	tmp2_map.put("consonant", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('b');
	tmp2.add('f');
	tmp2.add('m');
	tmp2.add('p');
	tmp2.add('v');
	tmp2.add('ɓ');
	tmp2.add('ɱ');
	tmp2.add('ɸ');
	tmp2.add('ʋ');
	tmp2.add('ʘ');
	tmp2.add('ʙ');
	tmp2.add('β');
	tmp2.add('ⱱ');
	tmp2_map.put("labial", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('b');
	tmp2.add('c');
	tmp2.add('d');
	tmp2.add('k');
	tmp2.add('p');
	tmp2.add('q');
	tmp2.add('t');
	tmp2.add('ɖ');
	tmp2.add('ɟ');
	tmp2.add('ɡ');
	tmp2.add('ɢ');
	tmp2.add('ʈ');
	tmp2.add('ʔ');
	tmp2.add('ʡ');
	tmp2_map.put("plosive", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('b');
	tmp2.add('c');
	tmp2.add('d');
	tmp2.add('f');
	tmp2.add('h');
	tmp2.add('j');
	tmp2.add('k');
	tmp2.add('l');
	tmp2.add('m');
	tmp2.add('n');
	tmp2.add('p');
	tmp2.add('q');
	tmp2.add('r');
	tmp2.add('s');
	tmp2.add('t');
	tmp2.add('v');
	tmp2.add('w');
	tmp2.add('x');
	tmp2.add('z');
	tmp2.add('ç');
	tmp2.add('ð');
	tmp2.add('ñ');
	tmp2.add('ħ');
	tmp2.add('ŋ');
	tmp2.add('ɕ');
	tmp2.add('ɖ');
	tmp2.add('ɟ');
	tmp2.add('ɡ');
	tmp2.add('ɢ');
	tmp2.add('ɣ');
	tmp2.add('ɥ');
	tmp2.add('ɦ');
	tmp2.add('ɧ');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɰ');
	tmp2.add('ɱ');
	tmp2.add('ɲ');
	tmp2.add('ɳ');
	tmp2.add('ɴ');
	tmp2.add('ɸ');
	tmp2.add('ɹ');
	tmp2.add('ɺ');
	tmp2.add('ɻ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ʀ');
	tmp2.add('ʁ');
	tmp2.add('ʂ');
	tmp2.add('ʃ');
	tmp2.add('ʈ');
	tmp2.add('ʋ');
	tmp2.add('ʍ');
	tmp2.add('ʎ');
	tmp2.add('ʐ');
	tmp2.add('ʑ');
	tmp2.add('ʒ');
	tmp2.add('ʔ');
	tmp2.add('ʕ');
	tmp2.add('ʙ');
	tmp2.add('ʜ');
	tmp2.add('ʝ');
	tmp2.add('ʟ');
	tmp2.add('ʡ');
	tmp2.add('ʢ');
	tmp2.add('β');
	tmp2.add('θ');
	tmp2.add('χ');
	tmp2.add('ⱱ');
	tmp2_map.put("pulmonic", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('c');
	tmp2.add('j');
	tmp2.add('k');
	tmp2.add('q');
	tmp2.add('x');
	tmp2.add('ç');
	tmp2.add('ŋ');
	tmp2.add('ǂ');
	tmp2.add('ɟ');
	tmp2.add('ɠ');
	tmp2.add('ɡ');
	tmp2.add('ɢ');
	tmp2.add('ɣ');
	tmp2.add('ɰ');
	tmp2.add('ɲ');
	tmp2.add('ɴ');
	tmp2.add('ʀ');
	tmp2.add('ʁ');
	tmp2.add('ʃ');
	tmp2.add('ʄ');
	tmp2.add('ʎ');
	tmp2.add('ʒ');
	tmp2.add('ʛ');
	tmp2.add('ʝ');
	tmp2.add('ʟ');
	tmp2.add('χ');
	tmp2_map.put("dorsal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('c');
	tmp2.add('j');
	tmp2.add('ç');
	tmp2.add('ǂ');
	tmp2.add('ɟ');
	tmp2.add('ɲ');
	tmp2.add('ʄ');
	tmp2.add('ʎ');
	tmp2.add('ʝ');
	tmp2_map.put("palatal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('c');
	tmp2.add('f');
	tmp2.add('h');
	tmp2.add('k');
	tmp2.add('p');
	tmp2.add('q');
	tmp2.add('s');
	tmp2.add('t');
	tmp2.add('x');
	tmp2.add('ç');
	tmp2.add('ħ');
	tmp2.add('ǀ');
	tmp2.add('ǁ');
	tmp2.add('ǂ');
	tmp2.add('ǃ');
	tmp2.add('ɕ');
	tmp2.add('ɧ');
	tmp2.add('ɬ');
	tmp2.add('ɸ');
	tmp2.add('ʂ');
	tmp2.add('ʃ');
	tmp2.add('ʈ');
	tmp2.add('ʍ');
	tmp2.add('ʔ');
	tmp2.add('ʘ');
	tmp2.add('ʜ');
	tmp2.add('ʡ');
	tmp2.add('θ');
	tmp2.add('χ');
	tmp2_map.put("unvoiced", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('d');
	tmp2.add('l');
	tmp2.add('n');
	tmp2.add('r');
	tmp2.add('s');
	tmp2.add('t');
	tmp2.add('z');
	tmp2.add('ñ');
	tmp2.add('ǁ');
	tmp2.add('ɗ');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɮ');
	tmp2.add('ɹ');
	tmp2.add('ɺ');
	tmp2.add('ɾ');
	tmp2_map.put("alveolar", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('d');
	tmp2.add('l');
	tmp2.add('n');
	tmp2.add('r');
	tmp2.add('s');
	tmp2.add('t');
	tmp2.add('z');
	tmp2.add('ð');
	tmp2.add('ñ');
	tmp2.add('ǀ');
	tmp2.add('ǁ');
	tmp2.add('ǃ');
	tmp2.add('ɖ');
	tmp2.add('ɗ');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɳ');
	tmp2.add('ɹ');
	tmp2.add('ɺ');
	tmp2.add('ɻ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ʂ');
	tmp2.add('ʃ');
	tmp2.add('ʈ');
	tmp2.add('ʐ');
	tmp2.add('ʒ');
	tmp2.add('θ');
	tmp2_map.put("coronal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('e');
	tmp2.add('o');
	tmp2.add('è');
	tmp2.add('é');
	tmp2.add('õ');
	tmp2.add('ø');
	tmp2.add('ɘ');
	tmp2.add('ɤ');
	tmp2.add('ɵ');
	tmp2.add('ẽ');
	tmp2_map.put("mid", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('f');
	tmp2.add('h');
	tmp2.add('s');
	tmp2.add('v');
	tmp2.add('x');
	tmp2.add('z');
	tmp2.add('ç');
	tmp2.add('ð');
	tmp2.add('ħ');
	tmp2.add('ɕ');
	tmp2.add('ɣ');
	tmp2.add('ɦ');
	tmp2.add('ɧ');
	tmp2.add('ɬ');
	tmp2.add('ɮ');
	tmp2.add('ɸ');
	tmp2.add('ʁ');
	tmp2.add('ʂ');
	tmp2.add('ʃ');
	tmp2.add('ʐ');
	tmp2.add('ʑ');
	tmp2.add('ʒ');
	tmp2.add('ʕ');
	tmp2.add('ʜ');
	tmp2.add('ʝ');
	tmp2.add('ʢ');
	tmp2.add('β');
	tmp2.add('θ');
	tmp2.add('χ');
	tmp2_map.put("fricative", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('f');
	tmp2.add('v');
	tmp2.add('ɱ');
	tmp2.add('ʋ');
	tmp2.add('ⱱ');
	tmp2_map.put("labio-dental", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('h');
	tmp2.add('ɦ');
	tmp2.add('ʔ');
	tmp2_map.put("glottal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('h');
	tmp2.add('ɦ');
	tmp2.add('ʔ');
	tmp2_map.put("laryngeal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('i');
	tmp2.add('u');
	tmp2.add('y');
	tmp2.add('ï');
	tmp2.add('ĩ');
	tmp2.add('ĭ');
	tmp2.add('ɨ');
	tmp2.add('ɯ');
	tmp2.add('ʉ');
	tmp2.add('ỹ');
	tmp2_map.put("close", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('j');
	tmp2.add('l');
	tmp2.add('w');
	tmp2.add('ɥ');
	tmp2.add('ɫ');
	tmp2.add('ɭ');
	tmp2.add('ɰ');
	tmp2.add('ɹ');
	tmp2.add('ɻ');
	tmp2.add('ʋ');
	tmp2.add('ʍ');
	tmp2.add('ʎ');
	tmp2.add('ʟ');
	tmp2_map.put("approximant", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('j');
	tmp2.add('w');
	tmp2.add('ɥ');
	tmp2.add('ɰ');
	tmp2.add('ʍ');
	tmp2_map.put("glide", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('j');
	tmp2.add('w');
	tmp2.add('ɥ');
	tmp2.add('ɰ');
	tmp2.add('ʍ');
	tmp2_map.put("semi-vowel", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('k');
	tmp2.add('x');
	tmp2.add('ŋ');
	tmp2.add('ɠ');
	tmp2.add('ɡ');
	tmp2.add('ɣ');
	tmp2.add('ɰ');
	tmp2.add('ʟ');
	tmp2_map.put("velar", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('l');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɺ');
	tmp2.add('ʎ');
	tmp2.add('ʟ');
	tmp2_map.put("lateral", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('l');
	tmp2.add('r');
	tmp2.add('ɫ');
	tmp2.add('ɬ');
	tmp2.add('ɭ');
	tmp2.add('ɮ');
	tmp2.add('ɺ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ʀ');
	tmp2.add('ʎ');
	tmp2.add('ʙ');
	tmp2.add('ʟ');
	tmp2.add('ⱱ');
	tmp2_map.put("liquid", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('m');
	tmp2.add('n');
	tmp2.add('ã');
	tmp2.add('ñ');
	tmp2.add('õ');
	tmp2.add('ĩ');
	tmp2.add('ŋ');
	tmp2.add('ɱ');
	tmp2.add('ɲ');
	tmp2.add('ɳ');
	tmp2.add('ɴ');
	tmp2.add('ẽ');
	tmp2.add('ỹ');
	tmp2_map.put("nasal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('o');
	tmp2.add('u');
	tmp2.add('õ');
	tmp2.add('ɑ');
	tmp2.add('ɒ');
	tmp2.add('ɔ');
	tmp2.add('ɤ');
	tmp2.add('ɯ');
	tmp2.add('ʌ');
	tmp2_map.put("near-back", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('o');
	tmp2.add('u');
	tmp2.add('y');
	tmp2.add('õ');
	tmp2.add('ø');
	tmp2.add('œ');
	tmp2.add('ɒ');
	tmp2.add('ɔ');
	tmp2.add('ɞ');
	tmp2.add('ɵ');
	tmp2.add('ɶ');
	tmp2.add('ʉ');
	tmp2.add('ʊ');
	tmp2.add('ʏ');
	tmp2.add('ỹ');
	tmp2_map.put("rounded", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('q');
	tmp2.add('ɢ');
	tmp2.add('ɴ');
	tmp2.add('ʀ');
	tmp2.add('ʁ');
	tmp2.add('ʛ');
	tmp2.add('χ');
	tmp2_map.put("uvular", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('r');
	tmp2.add('ʀ');
	tmp2.add('ʙ');
	tmp2_map.put("trill", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('w');
	tmp2.add('ɕ');
	tmp2.add('ɥ');
	tmp2.add('ɧ');
	tmp2.add('ʍ');
	tmp2.add('ʑ');
	tmp2_map.put("co-articulated", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('w');
	tmp2.add('ɕ');
	tmp2.add('ɥ');
	tmp2.add('ɧ');
	tmp2.add('ʍ');
	tmp2.add('ʑ');
	tmp2_map.put("double", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ã');
	tmp2.add('ñ');
	tmp2.add('õ');
	tmp2.add('ĩ');
	tmp2.add('̃');
	tmp2.add('ẽ');
	tmp2.add('ỹ');
	tmp2_map.put("nasalized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ä');
	tmp2.add('ï');
	tmp2.add('̈');
	tmp2_map.put("centralized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('æ');
	tmp2.add('ɐ');
	tmp2_map.put("open", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('è');
	tmp2.add('̀');
	tmp2_map.put("low_or_fourth_tone", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('é');
	tmp2.add('́');
	tmp2_map.put("high_or_second_tone", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ð');
	tmp2.add('ǀ');
	tmp2.add('̪');
	tmp2.add('θ');
	tmp2_map.put("dental", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ħ');
	tmp2.add('ʕ');
	tmp2_map.put("pharyngeal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ħ');
	tmp2.add('ʕ');
	tmp2.add('ʜ');
	tmp2.add('ʡ');
	tmp2.add('ʢ');
	tmp2_map.put("radical", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ĭ');
	tmp2.add('̆');
	tmp2_map.put("short", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('œ');
	tmp2.add('ɔ');
	tmp2.add('ɛ');
	tmp2.add('ɜ');
	tmp2.add('ʌ');
	tmp2_map.put("open-mid", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ǀ');
	tmp2.add('ǁ');
	tmp2.add('ǂ');
	tmp2.add('ǃ');
	tmp2.add('ʘ');
	tmp2_map.put("click", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ǀ');
	tmp2.add('ǁ');
	tmp2.add('ǂ');
	tmp2.add('ǃ');
	tmp2.add('ɓ');
	tmp2.add('ɗ');
	tmp2.add('ɠ');
	tmp2.add('ʄ');
	tmp2.add('ʘ');
	tmp2.add('ʛ');
	tmp2_map.put("non-pulmonic", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ǃ');
	tmp2.add('ɖ');
	tmp2.add('ɭ');
	tmp2.add('ɳ');
	tmp2.add('ɻ');
	tmp2.add('ɽ');
	tmp2.add('ʂ');
	tmp2.add('ʈ');
	tmp2.add('ʐ');
	tmp2_map.put("retroflex", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɐ');
	tmp2.add('ɘ');
	tmp2.add('ə');
	tmp2.add('ɚ');
	tmp2.add('ɜ');
	tmp2.add('ɞ');
	tmp2.add('ɨ');
	tmp2.add('ɵ');
	tmp2.add('ʉ');
	tmp2_map.put("central", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɓ');
	tmp2.add('ɗ');
	tmp2.add('ɠ');
	tmp2.add('ʄ');
	tmp2.add('ʛ');
	tmp2_map.put("voiced-implosive", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ə');
	tmp2.add('ɚ');
	tmp2_map.put("close-mid", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɚ');
	tmp2.add('˞');
	tmp2_map.put("rhoticity", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɪ');
	tmp2.add('ʊ');
	tmp2.add('ʏ');
	tmp2_map.put("near-close", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɪ');
	tmp2.add('ʏ');
	tmp2_map.put("near-front", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɫ');
	tmp2.add('̴');
	tmp2_map.put("velarized_or_pharyngealized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɺ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ⱱ');
	tmp2_map.put("flap", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ɺ');
	tmp2.add('ɽ');
	tmp2.add('ɾ');
	tmp2.add('ⱱ');
	tmp2_map.put("tap", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʃ');
	tmp2.add('ʒ');
	tmp2_map.put("palato-alveolar", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʊ');
	tmp2_map.put("back", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʜ');
	tmp2.add('ʡ');
	tmp2.add('ʢ');
	tmp2_map.put("epiglottal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʰ');
	tmp2_map.put("aspirated", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʲ');
	tmp2_map.put("palatalized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ʷ');
	tmp2_map.put("labialized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ˈ');
	tmp2_map.put("primary_stress", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ˌ');
	tmp2_map.put("secondary_stress", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ː');
	tmp2_map.put("long", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ˠ');
	tmp2_map.put("velarized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ˡ');
	tmp2_map.put("lateral_release", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ˤ');
	tmp2_map.put("pharyngealized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̄');
	tmp2_map.put("mid_or_first_tone", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̌');
	tmp2_map.put("rising_or_third_tone", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̘');
	tmp2_map.put("advanced_tongue_root", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̙');
	tmp2_map.put("retracted_tongue_root", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̚');
	tmp2_map.put("no_audible_release", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̜');
	tmp2_map.put("less_rounded", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̝');
	tmp2_map.put("raised", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̞');
	tmp2_map.put("lowered", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̟');
	tmp2_map.put("advanced", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̠');
	tmp2_map.put("retracted", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̤');
	tmp2_map.put("breathy_voiced", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̥');
	tmp2_map.put("voiceless", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̩');
	tmp2_map.put("syllabic", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̯');
	tmp2_map.put("non_syllabic", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̰');
	tmp2_map.put("creaky_voiced", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̹');
	tmp2_map.put("more_rounded", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̺');
	tmp2_map.put("apical", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̻');
	tmp2_map.put("laminal", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̼');
	tmp2_map.put("linguolabial", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('̽');
	tmp2_map.put("mid_centralized", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('‿');
	tmp2_map.put("syllable_linking", tmp2);

	tmp2 = new HashSet<Character>();
	tmp2.add('ⁿ');
	tmp2_map.put("nasal_release", tmp2);

	cat_ipa_map = Collections.unmodifiableMap(tmp2_map);
    }
}


/* IPA.java ends here */
