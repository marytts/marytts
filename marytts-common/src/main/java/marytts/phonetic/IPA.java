package marytts.phonetic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class IPA
{
    public static final Map<String, Set<String>> ipa_cat_map;
    public static final Map<String, Set<String>> cat_ipa_map;


    static {
	Map<String, Set<String>> tmp_map = new HashMap<String, Set<String>>();
	Set<String> tmp = new HashSet<String>();
	tmp.add("syllable_break");
	tmp_map.put(".", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("a", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("b", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("palatal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("c", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("d", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("e", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("f", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("h", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("i", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("glide");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put("j", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("velar");
	tmp_map.put("k", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("l", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("m", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("n", tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("o", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("p", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("uvular");
	tmp_map.put("q", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("voiced");
	tmp_map.put("r", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("s", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("t", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("u", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("v", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put("w", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("velar");
	tmp_map.put("x", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("y", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("z", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ã", tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ä", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("æ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ç", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("low_or_fourth_tone");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("è", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("high_or_second_tone");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("é", tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp.add("close");
	tmp.add("front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ï", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ð", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ñ", tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("near-back");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("õ", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ø", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("pharyngeal");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put("ħ", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ĩ", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("short");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ĭ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put("ŋ", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open-mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("œ", tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ǀ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ǁ", tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("palatal");
	tmp.add("unvoiced");
	tmp_map.put("ǂ", tmp);

	tmp = new HashSet<String>();
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put("ǃ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɐ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("near-open");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɑ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɒ", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("non-pulmonic");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put("ɓ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("open-mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɔ", tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ɕ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put("ɖ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("non-pulmonic");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put("ɗ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɘ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ə", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close-mid");
	tmp.add("rhoticity");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɚ", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɛ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɜ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("open-mid");
	tmp.add("rhoticity");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɝ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɞ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("palatal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɟ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put("ɠ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put("ɡ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put("ɢ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put("ɣ", tmp);

	tmp = new HashSet<String>();
	tmp.add("mid");
	tmp.add("near-back");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɤ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("voiced");
	tmp_map.put("ɥ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɦ", tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ɧ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɨ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-close");
	tmp.add("near-front");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɪ", tmp);

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
	tmp_map.put("ɫ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ɬ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put("ɭ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɮ", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("near-back");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɯ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put("ɰ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɱ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɲ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put("ɳ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("nasal");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put("ɴ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("mid");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɵ", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("near-open");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ɶ", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ɸ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ɹ", tmp);

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
	tmp_map.put("ɺ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put("ɻ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("flap");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put("ɽ", tmp);

	tmp = new HashSet<String>();
	tmp.add("alveolar");
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("flap");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put("ɾ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put("ʀ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp_map.put("ʁ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put("ʂ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palato-alveolar");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ʃ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("palatal");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put("ʄ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("unvoiced");
	tmp_map.put("ʈ", tmp);

	tmp = new HashSet<String>();
	tmp.add("central");
	tmp.add("close");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ʉ", tmp);

	tmp = new HashSet<String>();
	tmp.add("back");
	tmp.add("near-close");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ʊ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ʋ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-back");
	tmp.add("open-mid");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ʌ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("glide");
	tmp.add("pulmonic");
	tmp.add("semi-vowel");
	tmp.add("unvoiced");
	tmp_map.put("ʍ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ʎ", tmp);

	tmp = new HashSet<String>();
	tmp.add("near-close");
	tmp.add("near-front");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ʏ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("retroflex");
	tmp.add("voiced");
	tmp_map.put("ʐ", tmp);

	tmp = new HashSet<String>();
	tmp.add("co-articulated");
	tmp.add("consonant");
	tmp.add("double");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ʑ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palato-alveolar");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ʒ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("glottal");
	tmp.add("laryngeal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ʔ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("pharyngeal");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("voiced");
	tmp_map.put("ʕ", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("click");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("non-pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("ʘ", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("labial");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("trill");
	tmp.add("voiced");
	tmp_map.put("ʙ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("non-pulmonic");
	tmp.add("uvular");
	tmp.add("voiced");
	tmp.add("voiced-implosive");
	tmp_map.put("ʛ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put("ʜ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("palatal");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("ʝ", tmp);

	tmp = new HashSet<String>();
	tmp.add("approximant");
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("lateral");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("velar");
	tmp.add("voiced");
	tmp_map.put("ʟ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("plosive");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("unvoiced");
	tmp_map.put("ʡ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("epiglottal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("radical");
	tmp.add("voiced");
	tmp_map.put("ʢ", tmp);

	tmp = new HashSet<String>();
	tmp.add("aspirated");
	tmp_map.put("ʰ", tmp);

	tmp = new HashSet<String>();
	tmp.add("palatalized");
	tmp_map.put("ʲ", tmp);

	tmp = new HashSet<String>();
	tmp.add("labialized");
	tmp_map.put("ʷ", tmp);

	tmp = new HashSet<String>();
	tmp.add("primary_stress");
	tmp_map.put("ˈ", tmp);

	tmp = new HashSet<String>();
	tmp.add("secondary_stress");
	tmp_map.put("ˌ", tmp);

	tmp = new HashSet<String>();
	tmp.add("long");
	tmp_map.put("ː", tmp);

	tmp = new HashSet<String>();
	tmp.add("rhoticity");
	tmp_map.put("˞", tmp);

	tmp = new HashSet<String>();
	tmp.add("velarized");
	tmp_map.put("ˠ", tmp);

	tmp = new HashSet<String>();
	tmp.add("lateral_release");
	tmp_map.put("ˡ", tmp);

	tmp = new HashSet<String>();
	tmp.add("pharyngealized");
	tmp_map.put("ˤ", tmp);

	tmp = new HashSet<String>();
	tmp.add("low_or_fourth_tone");
	tmp_map.put("̀", tmp);

	tmp = new HashSet<String>();
	tmp.add("high_or_second_tone");
	tmp_map.put("́", tmp);

	tmp = new HashSet<String>();
	tmp.add("nasalized");
	tmp_map.put("̃", tmp);

	tmp = new HashSet<String>();
	tmp.add("mid_or_first_tone");
	tmp_map.put("̄", tmp);

	tmp = new HashSet<String>();
	tmp.add("short");
	tmp_map.put("̆", tmp);

	tmp = new HashSet<String>();
	tmp.add("centralized");
	tmp_map.put("̈", tmp);

	tmp = new HashSet<String>();
	tmp.add("rising_or_third_tone");
	tmp_map.put("̌", tmp);

	tmp = new HashSet<String>();
	tmp.add("advanced_tongue_root");
	tmp_map.put("̘", tmp);

	tmp = new HashSet<String>();
	tmp.add("retracted_tongue_root");
	tmp_map.put("̙", tmp);

	tmp = new HashSet<String>();
	tmp.add("no_audible_release");
	tmp_map.put("̚", tmp);

	tmp = new HashSet<String>();
	tmp.add("less_rounded");
	tmp_map.put("̜", tmp);

	tmp = new HashSet<String>();
	tmp.add("raised");
	tmp_map.put("̝", tmp);

	tmp = new HashSet<String>();
	tmp.add("lowered");
	tmp_map.put("̞", tmp);

	tmp = new HashSet<String>();
	tmp.add("advanced");
	tmp_map.put("̟", tmp);

	tmp = new HashSet<String>();
	tmp.add("retracted");
	tmp_map.put("̠", tmp);

	tmp = new HashSet<String>();
	tmp.add("breathy_voiced");
	tmp_map.put("̤", tmp);

	tmp = new HashSet<String>();
	tmp.add("voiceless");
	tmp_map.put("̥", tmp);

	tmp = new HashSet<String>();
	tmp.add("syllabic");
	tmp_map.put("̩", tmp);

	tmp = new HashSet<String>();
	tmp.add("dental");
	tmp_map.put("̪", tmp);

	tmp = new HashSet<String>();
	tmp.add("voiced");
	tmp_map.put("̬", tmp);

	tmp = new HashSet<String>();
	tmp.add("non_syllabic");
	tmp_map.put("̯", tmp);

	tmp = new HashSet<String>();
	tmp.add("creaky_voiced");
	tmp_map.put("̰", tmp);

	tmp = new HashSet<String>();
	tmp.add("velarized_or_pharyngealized");
	tmp_map.put("̴", tmp);

	tmp = new HashSet<String>();
	tmp.add("more_rounded");
	tmp_map.put("̹", tmp);

	tmp = new HashSet<String>();
	tmp.add("apical");
	tmp_map.put("̺", tmp);

	tmp = new HashSet<String>();
	tmp.add("laminal");
	tmp_map.put("̻", tmp);

	tmp = new HashSet<String>();
	tmp.add("linguolabial");
	tmp_map.put("̼", tmp);

	tmp = new HashSet<String>();
	tmp.add("mid_centralized");
	tmp_map.put("̽", tmp);

	tmp = new HashSet<String>();
	tmp.add("bilabial");
	tmp.add("consonant");
	tmp.add("fricative");
	tmp.add("labial");
	tmp.add("pulmonic");
	tmp.add("voiced");
	tmp_map.put("β", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("coronal");
	tmp.add("dental");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp_map.put("θ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("dorsal");
	tmp.add("fricative");
	tmp.add("pulmonic");
	tmp.add("unvoiced");
	tmp.add("uvular");
	tmp_map.put("χ", tmp);

	tmp = new HashSet<String>();
	tmp.add("front");
	tmp.add("mid");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ẽ", tmp);

	tmp = new HashSet<String>();
	tmp.add("close");
	tmp.add("front");
	tmp.add("nasal");
	tmp.add("nasalized");
	tmp.add("rounded");
	tmp.add("voiced");
	tmp.add("vowel");
	tmp_map.put("ỹ", tmp);

	tmp = new HashSet<String>();
	tmp.add("syllable_linking");
	tmp_map.put("‿", tmp);

	tmp = new HashSet<String>();
	tmp.add("nasal_release");
	tmp_map.put("ⁿ", tmp);

	tmp = new HashSet<String>();
	tmp.add("consonant");
	tmp.add("flap");
	tmp.add("labial");
	tmp.add("labio-dental");
	tmp.add("liquid");
	tmp.add("pulmonic");
	tmp.add("tap");
	tmp.add("voiced");
	tmp_map.put("ⱱ", tmp);
	ipa_cat_map = Collections.unmodifiableMap(tmp_map);


	tmp_map = new HashMap<String, Set<String>>();
	tmp = new HashSet<String>();
	tmp.add(".");
	tmp_map.put("syllable_break", tmp);

	tmp = new HashSet<String>();
	tmp.add("a");
	tmp.add("e");
	tmp.add("i");
	tmp.add("y");
	tmp.add("ã");
	tmp.add("ä");
	tmp.add("æ");
	tmp.add("è");
	tmp.add("é");
	tmp.add("ï");
	tmp.add("ø");
	tmp.add("ĩ");
	tmp.add("ĭ");
	tmp.add("œ");
	tmp.add("ɛ");
	tmp.add("ɶ");
	tmp.add("ẽ");
	tmp.add("ỹ");
	tmp_map.put("front", tmp);

	tmp = new HashSet<String>();
	tmp.add("a");
	tmp.add("ã");
	tmp.add("ä");
	tmp.add("ɑ");
	tmp.add("ɒ");
	tmp.add("ɞ");
	tmp.add("ɶ");
	tmp_map.put("near-open", tmp);

	tmp = new HashSet<String>();
	tmp.add("a");
	tmp.add("b");
	tmp.add("d");
	tmp.add("e");
	tmp.add("i");
	tmp.add("j");
	tmp.add("l");
	tmp.add("m");
	tmp.add("n");
	tmp.add("o");
	tmp.add("r");
	tmp.add("u");
	tmp.add("v");
	tmp.add("w");
	tmp.add("y");
	tmp.add("z");
	tmp.add("ã");
	tmp.add("ä");
	tmp.add("æ");
	tmp.add("è");
	tmp.add("é");
	tmp.add("ï");
	tmp.add("ð");
	tmp.add("ñ");
	tmp.add("õ");
	tmp.add("ø");
	tmp.add("ĩ");
	tmp.add("ĭ");
	tmp.add("ŋ");
	tmp.add("œ");
	tmp.add("ɐ");
	tmp.add("ɑ");
	tmp.add("ɒ");
	tmp.add("ɓ");
	tmp.add("ɔ");
	tmp.add("ɖ");
	tmp.add("ɗ");
	tmp.add("ɘ");
	tmp.add("ə");
	tmp.add("ɚ");
	tmp.add("ɛ");
	tmp.add("ɜ");
	tmp.add("ɝ");
	tmp.add("ɞ");
	tmp.add("ɟ");
	tmp.add("ɠ");
	tmp.add("ɡ");
	tmp.add("ɢ");
	tmp.add("ɣ");
	tmp.add("ɤ");
	tmp.add("ɥ");
	tmp.add("ɦ");
	tmp.add("ɨ");
	tmp.add("ɪ");
	tmp.add("ɫ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɯ");
	tmp.add("ɰ");
	tmp.add("ɱ");
	tmp.add("ɲ");
	tmp.add("ɳ");
	tmp.add("ɴ");
	tmp.add("ɵ");
	tmp.add("ɶ");
	tmp.add("ɹ");
	tmp.add("ɺ");
	tmp.add("ɻ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ʀ");
	tmp.add("ʁ");
	tmp.add("ʄ");
	tmp.add("ʉ");
	tmp.add("ʊ");
	tmp.add("ʋ");
	tmp.add("ʌ");
	tmp.add("ʎ");
	tmp.add("ʏ");
	tmp.add("ʐ");
	tmp.add("ʑ");
	tmp.add("ʒ");
	tmp.add("ʕ");
	tmp.add("ʙ");
	tmp.add("ʛ");
	tmp.add("ʝ");
	tmp.add("ʟ");
	tmp.add("ʢ");
	tmp.add("̬");
	tmp.add("β");
	tmp.add("ẽ");
	tmp.add("ỹ");
	tmp.add("ⱱ");
	tmp_map.put("voiced", tmp);

	tmp = new HashSet<String>();
	tmp.add("a");
	tmp.add("e");
	tmp.add("i");
	tmp.add("o");
	tmp.add("u");
	tmp.add("y");
	tmp.add("ã");
	tmp.add("ä");
	tmp.add("æ");
	tmp.add("è");
	tmp.add("é");
	tmp.add("ï");
	tmp.add("õ");
	tmp.add("ø");
	tmp.add("ĩ");
	tmp.add("ĭ");
	tmp.add("œ");
	tmp.add("ɐ");
	tmp.add("ɑ");
	tmp.add("ɒ");
	tmp.add("ɔ");
	tmp.add("ɘ");
	tmp.add("ə");
	tmp.add("ɚ");
	tmp.add("ɛ");
	tmp.add("ɜ");
	tmp.add("ɝ");
	tmp.add("ɞ");
	tmp.add("ɤ");
	tmp.add("ɨ");
	tmp.add("ɪ");
	tmp.add("ɯ");
	tmp.add("ɵ");
	tmp.add("ɶ");
	tmp.add("ʉ");
	tmp.add("ʊ");
	tmp.add("ʌ");
	tmp.add("ʏ");
	tmp.add("ẽ");
	tmp.add("ỹ");
	tmp_map.put("vowel", tmp);

	tmp = new HashSet<String>();
	tmp.add("b");
	tmp.add("m");
	tmp.add("p");
	tmp.add("ɓ");
	tmp.add("ɸ");
	tmp.add("ʘ");
	tmp.add("ʙ");
	tmp.add("β");
	tmp_map.put("bilabial", tmp);

	tmp = new HashSet<String>();
	tmp.add("b");
	tmp.add("c");
	tmp.add("d");
	tmp.add("f");
	tmp.add("h");
	tmp.add("j");
	tmp.add("k");
	tmp.add("l");
	tmp.add("m");
	tmp.add("n");
	tmp.add("p");
	tmp.add("q");
	tmp.add("r");
	tmp.add("s");
	tmp.add("t");
	tmp.add("v");
	tmp.add("w");
	tmp.add("x");
	tmp.add("z");
	tmp.add("ç");
	tmp.add("ð");
	tmp.add("ñ");
	tmp.add("ħ");
	tmp.add("ŋ");
	tmp.add("ǀ");
	tmp.add("ǁ");
	tmp.add("ǂ");
	tmp.add("ǃ");
	tmp.add("ɓ");
	tmp.add("ɕ");
	tmp.add("ɖ");
	tmp.add("ɗ");
	tmp.add("ɟ");
	tmp.add("ɠ");
	tmp.add("ɡ");
	tmp.add("ɢ");
	tmp.add("ɣ");
	tmp.add("ɥ");
	tmp.add("ɦ");
	tmp.add("ɧ");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɰ");
	tmp.add("ɱ");
	tmp.add("ɲ");
	tmp.add("ɳ");
	tmp.add("ɴ");
	tmp.add("ɸ");
	tmp.add("ɹ");
	tmp.add("ɺ");
	tmp.add("ɻ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ʀ");
	tmp.add("ʁ");
	tmp.add("ʂ");
	tmp.add("ʃ");
	tmp.add("ʄ");
	tmp.add("ʈ");
	tmp.add("ʋ");
	tmp.add("ʍ");
	tmp.add("ʎ");
	tmp.add("ʐ");
	tmp.add("ʑ");
	tmp.add("ʒ");
	tmp.add("ʔ");
	tmp.add("ʕ");
	tmp.add("ʘ");
	tmp.add("ʙ");
	tmp.add("ʛ");
	tmp.add("ʜ");
	tmp.add("ʝ");
	tmp.add("ʟ");
	tmp.add("ʡ");
	tmp.add("ʢ");
	tmp.add("β");
	tmp.add("θ");
	tmp.add("χ");
	tmp.add("ⱱ");
	tmp_map.put("consonant", tmp);

	tmp = new HashSet<String>();
	tmp.add("b");
	tmp.add("f");
	tmp.add("m");
	tmp.add("p");
	tmp.add("v");
	tmp.add("ɓ");
	tmp.add("ɱ");
	tmp.add("ɸ");
	tmp.add("ʋ");
	tmp.add("ʘ");
	tmp.add("ʙ");
	tmp.add("β");
	tmp.add("ⱱ");
	tmp_map.put("labial", tmp);

	tmp = new HashSet<String>();
	tmp.add("b");
	tmp.add("c");
	tmp.add("d");
	tmp.add("k");
	tmp.add("p");
	tmp.add("q");
	tmp.add("t");
	tmp.add("ɖ");
	tmp.add("ɟ");
	tmp.add("ɡ");
	tmp.add("ɢ");
	tmp.add("ʈ");
	tmp.add("ʔ");
	tmp.add("ʡ");
	tmp_map.put("plosive", tmp);

	tmp = new HashSet<String>();
	tmp.add("b");
	tmp.add("c");
	tmp.add("d");
	tmp.add("f");
	tmp.add("h");
	tmp.add("j");
	tmp.add("k");
	tmp.add("l");
	tmp.add("m");
	tmp.add("n");
	tmp.add("p");
	tmp.add("q");
	tmp.add("r");
	tmp.add("s");
	tmp.add("t");
	tmp.add("v");
	tmp.add("w");
	tmp.add("x");
	tmp.add("z");
	tmp.add("ç");
	tmp.add("ð");
	tmp.add("ñ");
	tmp.add("ħ");
	tmp.add("ŋ");
	tmp.add("ɕ");
	tmp.add("ɖ");
	tmp.add("ɟ");
	tmp.add("ɡ");
	tmp.add("ɢ");
	tmp.add("ɣ");
	tmp.add("ɥ");
	tmp.add("ɦ");
	tmp.add("ɧ");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɰ");
	tmp.add("ɱ");
	tmp.add("ɲ");
	tmp.add("ɳ");
	tmp.add("ɴ");
	tmp.add("ɸ");
	tmp.add("ɹ");
	tmp.add("ɺ");
	tmp.add("ɻ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ʀ");
	tmp.add("ʁ");
	tmp.add("ʂ");
	tmp.add("ʃ");
	tmp.add("ʈ");
	tmp.add("ʋ");
	tmp.add("ʍ");
	tmp.add("ʎ");
	tmp.add("ʐ");
	tmp.add("ʑ");
	tmp.add("ʒ");
	tmp.add("ʔ");
	tmp.add("ʕ");
	tmp.add("ʙ");
	tmp.add("ʜ");
	tmp.add("ʝ");
	tmp.add("ʟ");
	tmp.add("ʡ");
	tmp.add("ʢ");
	tmp.add("β");
	tmp.add("θ");
	tmp.add("χ");
	tmp.add("ⱱ");
	tmp_map.put("pulmonic", tmp);

	tmp = new HashSet<String>();
	tmp.add("c");
	tmp.add("j");
	tmp.add("k");
	tmp.add("q");
	tmp.add("x");
	tmp.add("ç");
	tmp.add("ŋ");
	tmp.add("ǂ");
	tmp.add("ɟ");
	tmp.add("ɠ");
	tmp.add("ɡ");
	tmp.add("ɢ");
	tmp.add("ɣ");
	tmp.add("ɰ");
	tmp.add("ɲ");
	tmp.add("ɴ");
	tmp.add("ʀ");
	tmp.add("ʁ");
	tmp.add("ʃ");
	tmp.add("ʄ");
	tmp.add("ʎ");
	tmp.add("ʒ");
	tmp.add("ʛ");
	tmp.add("ʝ");
	tmp.add("ʟ");
	tmp.add("χ");
	tmp_map.put("dorsal", tmp);

	tmp = new HashSet<String>();
	tmp.add("c");
	tmp.add("j");
	tmp.add("ç");
	tmp.add("ǂ");
	tmp.add("ɟ");
	tmp.add("ɲ");
	tmp.add("ʄ");
	tmp.add("ʎ");
	tmp.add("ʝ");
	tmp_map.put("palatal", tmp);

	tmp = new HashSet<String>();
	tmp.add("c");
	tmp.add("f");
	tmp.add("h");
	tmp.add("k");
	tmp.add("p");
	tmp.add("q");
	tmp.add("s");
	tmp.add("t");
	tmp.add("x");
	tmp.add("ç");
	tmp.add("ħ");
	tmp.add("ǀ");
	tmp.add("ǁ");
	tmp.add("ǂ");
	tmp.add("ǃ");
	tmp.add("ɕ");
	tmp.add("ɧ");
	tmp.add("ɬ");
	tmp.add("ɸ");
	tmp.add("ʂ");
	tmp.add("ʃ");
	tmp.add("ʈ");
	tmp.add("ʍ");
	tmp.add("ʔ");
	tmp.add("ʘ");
	tmp.add("ʜ");
	tmp.add("ʡ");
	tmp.add("θ");
	tmp.add("χ");
	tmp_map.put("unvoiced", tmp);

	tmp = new HashSet<String>();
	tmp.add("d");
	tmp.add("l");
	tmp.add("n");
	tmp.add("r");
	tmp.add("s");
	tmp.add("t");
	tmp.add("z");
	tmp.add("ñ");
	tmp.add("ǁ");
	tmp.add("ɗ");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɮ");
	tmp.add("ɹ");
	tmp.add("ɺ");
	tmp.add("ɾ");
	tmp_map.put("alveolar", tmp);

	tmp = new HashSet<String>();
	tmp.add("d");
	tmp.add("l");
	tmp.add("n");
	tmp.add("r");
	tmp.add("s");
	tmp.add("t");
	tmp.add("z");
	tmp.add("ð");
	tmp.add("ñ");
	tmp.add("ǀ");
	tmp.add("ǁ");
	tmp.add("ǃ");
	tmp.add("ɖ");
	tmp.add("ɗ");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɳ");
	tmp.add("ɹ");
	tmp.add("ɺ");
	tmp.add("ɻ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ʂ");
	tmp.add("ʃ");
	tmp.add("ʈ");
	tmp.add("ʐ");
	tmp.add("ʒ");
	tmp.add("θ");
	tmp_map.put("coronal", tmp);

	tmp = new HashSet<String>();
	tmp.add("e");
	tmp.add("o");
	tmp.add("è");
	tmp.add("é");
	tmp.add("õ");
	tmp.add("ø");
	tmp.add("ɘ");
	tmp.add("ɤ");
	tmp.add("ɵ");
	tmp.add("ẽ");
	tmp_map.put("mid", tmp);

	tmp = new HashSet<String>();
	tmp.add("f");
	tmp.add("h");
	tmp.add("s");
	tmp.add("v");
	tmp.add("x");
	tmp.add("z");
	tmp.add("ç");
	tmp.add("ð");
	tmp.add("ħ");
	tmp.add("ɕ");
	tmp.add("ɣ");
	tmp.add("ɦ");
	tmp.add("ɧ");
	tmp.add("ɬ");
	tmp.add("ɮ");
	tmp.add("ɸ");
	tmp.add("ʁ");
	tmp.add("ʂ");
	tmp.add("ʃ");
	tmp.add("ʐ");
	tmp.add("ʑ");
	tmp.add("ʒ");
	tmp.add("ʕ");
	tmp.add("ʜ");
	tmp.add("ʝ");
	tmp.add("ʢ");
	tmp.add("β");
	tmp.add("θ");
	tmp.add("χ");
	tmp_map.put("fricative", tmp);

	tmp = new HashSet<String>();
	tmp.add("f");
	tmp.add("v");
	tmp.add("ɱ");
	tmp.add("ʋ");
	tmp.add("ⱱ");
	tmp_map.put("labio-dental", tmp);

	tmp = new HashSet<String>();
	tmp.add("h");
	tmp.add("ɦ");
	tmp.add("ʔ");
	tmp_map.put("glottal", tmp);

	tmp = new HashSet<String>();
	tmp.add("h");
	tmp.add("ɦ");
	tmp.add("ʔ");
	tmp_map.put("laryngeal", tmp);

	tmp = new HashSet<String>();
	tmp.add("i");
	tmp.add("u");
	tmp.add("y");
	tmp.add("ï");
	tmp.add("ĩ");
	tmp.add("ĭ");
	tmp.add("ɨ");
	tmp.add("ɯ");
	tmp.add("ʉ");
	tmp.add("ỹ");
	tmp_map.put("close", tmp);

	tmp = new HashSet<String>();
	tmp.add("j");
	tmp.add("l");
	tmp.add("w");
	tmp.add("ɥ");
	tmp.add("ɫ");
	tmp.add("ɭ");
	tmp.add("ɰ");
	tmp.add("ɹ");
	tmp.add("ɻ");
	tmp.add("ʋ");
	tmp.add("ʍ");
	tmp.add("ʎ");
	tmp.add("ʟ");
	tmp_map.put("approximant", tmp);

	tmp = new HashSet<String>();
	tmp.add("j");
	tmp.add("w");
	tmp.add("ɥ");
	tmp.add("ɰ");
	tmp.add("ʍ");
	tmp_map.put("glide", tmp);

	tmp = new HashSet<String>();
	tmp.add("j");
	tmp.add("w");
	tmp.add("ɥ");
	tmp.add("ɰ");
	tmp.add("ʍ");
	tmp_map.put("semi-vowel", tmp);

	tmp = new HashSet<String>();
	tmp.add("k");
	tmp.add("x");
	tmp.add("ŋ");
	tmp.add("ɠ");
	tmp.add("ɡ");
	tmp.add("ɣ");
	tmp.add("ɰ");
	tmp.add("ʟ");
	tmp_map.put("velar", tmp);

	tmp = new HashSet<String>();
	tmp.add("l");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɺ");
	tmp.add("ʎ");
	tmp.add("ʟ");
	tmp_map.put("lateral", tmp);

	tmp = new HashSet<String>();
	tmp.add("l");
	tmp.add("r");
	tmp.add("ɫ");
	tmp.add("ɬ");
	tmp.add("ɭ");
	tmp.add("ɮ");
	tmp.add("ɺ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ʀ");
	tmp.add("ʎ");
	tmp.add("ʙ");
	tmp.add("ʟ");
	tmp.add("ⱱ");
	tmp_map.put("liquid", tmp);

	tmp = new HashSet<String>();
	tmp.add("m");
	tmp.add("n");
	tmp.add("ã");
	tmp.add("ñ");
	tmp.add("õ");
	tmp.add("ĩ");
	tmp.add("ŋ");
	tmp.add("ɱ");
	tmp.add("ɲ");
	tmp.add("ɳ");
	tmp.add("ɴ");
	tmp.add("ẽ");
	tmp.add("ỹ");
	tmp_map.put("nasal", tmp);

	tmp = new HashSet<String>();
	tmp.add("o");
	tmp.add("u");
	tmp.add("õ");
	tmp.add("ɑ");
	tmp.add("ɒ");
	tmp.add("ɔ");
	tmp.add("ɤ");
	tmp.add("ɯ");
	tmp.add("ʌ");
	tmp_map.put("near-back", tmp);

	tmp = new HashSet<String>();
	tmp.add("o");
	tmp.add("u");
	tmp.add("y");
	tmp.add("õ");
	tmp.add("ø");
	tmp.add("œ");
	tmp.add("ɒ");
	tmp.add("ɔ");
	tmp.add("ɞ");
	tmp.add("ɵ");
	tmp.add("ɶ");
	tmp.add("ʉ");
	tmp.add("ʊ");
	tmp.add("ʏ");
	tmp.add("ỹ");
	tmp_map.put("rounded", tmp);

	tmp = new HashSet<String>();
	tmp.add("q");
	tmp.add("ɢ");
	tmp.add("ɴ");
	tmp.add("ʀ");
	tmp.add("ʁ");
	tmp.add("ʛ");
	tmp.add("χ");
	tmp_map.put("uvular", tmp);

	tmp = new HashSet<String>();
	tmp.add("r");
	tmp.add("ʀ");
	tmp.add("ʙ");
	tmp_map.put("trill", tmp);

	tmp = new HashSet<String>();
	tmp.add("w");
	tmp.add("ɕ");
	tmp.add("ɥ");
	tmp.add("ɧ");
	tmp.add("ʍ");
	tmp.add("ʑ");
	tmp_map.put("co-articulated", tmp);

	tmp = new HashSet<String>();
	tmp.add("w");
	tmp.add("ɕ");
	tmp.add("ɥ");
	tmp.add("ɧ");
	tmp.add("ʍ");
	tmp.add("ʑ");
	tmp_map.put("double", tmp);

	tmp = new HashSet<String>();
	tmp.add("ã");
	tmp.add("ñ");
	tmp.add("õ");
	tmp.add("ĩ");
	tmp.add("̃");
	tmp.add("ẽ");
	tmp.add("ỹ");
	tmp_map.put("nasalized", tmp);

	tmp = new HashSet<String>();
	tmp.add("ä");
	tmp.add("ï");
	tmp.add("̈");
	tmp_map.put("centralized", tmp);

	tmp = new HashSet<String>();
	tmp.add("æ");
	tmp.add("ɐ");
	tmp_map.put("open", tmp);

	tmp = new HashSet<String>();
	tmp.add("è");
	tmp.add("̀");
	tmp_map.put("low_or_fourth_tone", tmp);

	tmp = new HashSet<String>();
	tmp.add("é");
	tmp.add("́");
	tmp_map.put("high_or_second_tone", tmp);

	tmp = new HashSet<String>();
	tmp.add("ð");
	tmp.add("ǀ");
	tmp.add("̪");
	tmp.add("θ");
	tmp_map.put("dental", tmp);

	tmp = new HashSet<String>();
	tmp.add("ħ");
	tmp.add("ʕ");
	tmp_map.put("pharyngeal", tmp);

	tmp = new HashSet<String>();
	tmp.add("ħ");
	tmp.add("ʕ");
	tmp.add("ʜ");
	tmp.add("ʡ");
	tmp.add("ʢ");
	tmp_map.put("radical", tmp);

	tmp = new HashSet<String>();
	tmp.add("ĭ");
	tmp.add("̆");
	tmp_map.put("short", tmp);

	tmp = new HashSet<String>();
	tmp.add("œ");
	tmp.add("ɔ");
	tmp.add("ɛ");
	tmp.add("ɜ");
	tmp.add("ɝ");
	tmp.add("ʌ");
	tmp_map.put("open-mid", tmp);

	tmp = new HashSet<String>();
	tmp.add("ǀ");
	tmp.add("ǁ");
	tmp.add("ǂ");
	tmp.add("ǃ");
	tmp.add("ʘ");
	tmp_map.put("click", tmp);

	tmp = new HashSet<String>();
	tmp.add("ǀ");
	tmp.add("ǁ");
	tmp.add("ǂ");
	tmp.add("ǃ");
	tmp.add("ɓ");
	tmp.add("ɗ");
	tmp.add("ɠ");
	tmp.add("ʄ");
	tmp.add("ʘ");
	tmp.add("ʛ");
	tmp_map.put("non-pulmonic", tmp);

	tmp = new HashSet<String>();
	tmp.add("ǃ");
	tmp.add("ɖ");
	tmp.add("ɭ");
	tmp.add("ɳ");
	tmp.add("ɻ");
	tmp.add("ɽ");
	tmp.add("ʂ");
	tmp.add("ʈ");
	tmp.add("ʐ");
	tmp_map.put("retroflex", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɐ");
	tmp.add("ɘ");
	tmp.add("ə");
	tmp.add("ɚ");
	tmp.add("ɜ");
	tmp.add("ɝ");
	tmp.add("ɞ");
	tmp.add("ɨ");
	tmp.add("ɵ");
	tmp.add("ʉ");
	tmp_map.put("central", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɓ");
	tmp.add("ɗ");
	tmp.add("ɠ");
	tmp.add("ʄ");
	tmp.add("ʛ");
	tmp_map.put("voiced-implosive", tmp);

	tmp = new HashSet<String>();
	tmp.add("ə");
	tmp.add("ɚ");
	tmp_map.put("close-mid", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɚ");
	tmp.add("ɝ");
	tmp.add("˞");
	tmp_map.put("rhoticity", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɪ");
	tmp.add("ʊ");
	tmp.add("ʏ");
	tmp_map.put("near-close", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɪ");
	tmp.add("ʏ");
	tmp_map.put("near-front", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɫ");
	tmp.add("̴");
	tmp_map.put("velarized_or_pharyngealized", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɺ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ⱱ");
	tmp_map.put("flap", tmp);

	tmp = new HashSet<String>();
	tmp.add("ɺ");
	tmp.add("ɽ");
	tmp.add("ɾ");
	tmp.add("ⱱ");
	tmp_map.put("tap", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʃ");
	tmp.add("ʒ");
	tmp_map.put("palato-alveolar", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʊ");
	tmp_map.put("back", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʜ");
	tmp.add("ʡ");
	tmp.add("ʢ");
	tmp_map.put("epiglottal", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʰ");
	tmp_map.put("aspirated", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʲ");
	tmp_map.put("palatalized", tmp);

	tmp = new HashSet<String>();
	tmp.add("ʷ");
	tmp_map.put("labialized", tmp);

	tmp = new HashSet<String>();
	tmp.add("ˈ");
	tmp_map.put("primary_stress", tmp);

	tmp = new HashSet<String>();
	tmp.add("ˌ");
	tmp_map.put("secondary_stress", tmp);

	tmp = new HashSet<String>();
	tmp.add("ː");
	tmp_map.put("long", tmp);

	tmp = new HashSet<String>();
	tmp.add("ˠ");
	tmp_map.put("velarized", tmp);

	tmp = new HashSet<String>();
	tmp.add("ˡ");
	tmp_map.put("lateral_release", tmp);

	tmp = new HashSet<String>();
	tmp.add("ˤ");
	tmp_map.put("pharyngealized", tmp);

	tmp = new HashSet<String>();
	tmp.add("̄");
	tmp_map.put("mid_or_first_tone", tmp);

	tmp = new HashSet<String>();
	tmp.add("̌");
	tmp_map.put("rising_or_third_tone", tmp);

	tmp = new HashSet<String>();
	tmp.add("̘");
	tmp_map.put("advanced_tongue_root", tmp);

	tmp = new HashSet<String>();
	tmp.add("̙");
	tmp_map.put("retracted_tongue_root", tmp);

	tmp = new HashSet<String>();
	tmp.add("̚");
	tmp_map.put("no_audible_release", tmp);

	tmp = new HashSet<String>();
	tmp.add("̜");
	tmp_map.put("less_rounded", tmp);

	tmp = new HashSet<String>();
	tmp.add("̝");
	tmp_map.put("raised", tmp);

	tmp = new HashSet<String>();
	tmp.add("̞");
	tmp_map.put("lowered", tmp);

	tmp = new HashSet<String>();
	tmp.add("̟");
	tmp_map.put("advanced", tmp);

	tmp = new HashSet<String>();
	tmp.add("̠");
	tmp_map.put("retracted", tmp);

	tmp = new HashSet<String>();
	tmp.add("̤");
	tmp_map.put("breathy_voiced", tmp);

	tmp = new HashSet<String>();
	tmp.add("̥");
	tmp_map.put("voiceless", tmp);

	tmp = new HashSet<String>();
	tmp.add("̩");
	tmp_map.put("syllabic", tmp);

	tmp = new HashSet<String>();
	tmp.add("̯");
	tmp_map.put("non_syllabic", tmp);

	tmp = new HashSet<String>();
	tmp.add("̰");
	tmp_map.put("creaky_voiced", tmp);

	tmp = new HashSet<String>();
	tmp.add("̹");
	tmp_map.put("more_rounded", tmp);

	tmp = new HashSet<String>();
	tmp.add("̺");
	tmp_map.put("apical", tmp);

	tmp = new HashSet<String>();
	tmp.add("̻");
	tmp_map.put("laminal", tmp);

	tmp = new HashSet<String>();
	tmp.add("̼");
	tmp_map.put("linguolabial", tmp);

	tmp = new HashSet<String>();
	tmp.add("̽");
	tmp_map.put("mid_centralized", tmp);

	tmp = new HashSet<String>();
	tmp.add("‿");
	tmp_map.put("syllable_linking", tmp);

	tmp = new HashSet<String>();
	tmp.add("ⁿ");
	tmp_map.put("nasal_release", tmp);

	cat_ipa_map = Collections.unmodifiableMap(tmp_map);
    }
}


/* IPA.java ends here */
