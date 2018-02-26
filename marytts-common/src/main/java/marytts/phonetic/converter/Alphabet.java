package marytts.phonetic.converter;


import java.util.Map;
import java.util.List;

import marytts.MaryException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public abstract class Alphabet
{

    protected Map<String, List<String>> alpha2ipa;
    protected Map<List<String>, String> ipa2alpha;

    protected void addIpaCorrespondance(String label, List<String> ipa) {
	alpha2ipa.put(label, ipa);
	ipa2alpha.put(ipa, label);
    }

    public List<String> getCorrespondingIPA(String label) throws MaryException {
	if (!alpha2ipa.containsKey(label))
	    throw new MaryException(label + " is not part of the current alphabet (\"" +
				    this.getClass().getName() + "\")");

	return alpha2ipa.get(label);
    }

    public String getLabelFromIPA(List<String> ipa) throws MaryException {
	if (!ipa2alpha.containsKey(ipa))
	    throw new MaryException(ipa.toString() + " doesn't match any element of the current alphabet (\"" +
				    this.getClass().getName() + "\")");

	return ipa2alpha.get(ipa);
    }
}


/* Alphabet.java ends here */
