package marytts.phonetic.converter;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import marytts.MaryException;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public abstract class Alphabet
{

    protected Map<String, String> alpha2ipa;
    protected Map<String, String> ipa2alpha;

    protected Alphabet() {
	alpha2ipa = new HashMap<String, String>();
	ipa2alpha = new HashMap<String, String>();
    }

    protected void addIpaCorrespondance(String label, String ipa) {
	alpha2ipa.put(label, ipa);
	ipa2alpha.put(ipa, label);
    }

    public String getCorrespondingIPA(String label) throws MaryException {
	if (!alpha2ipa.containsKey(label))
	    throw new MaryException(label + " is not part of the current alphabet (\"" +
				    this.getClass().getName() + "\")");

	return alpha2ipa.get(label);
    }

    public String getLabelFromIPA(String ipa) throws MaryException {
	if (!ipa2alpha.containsKey(ipa))
	    throw new MaryException(ipa.toString() + " doesn't match any element of the current alphabet (\"" +
				    this.getClass().getName() + "\")");

	return ipa2alpha.get(ipa);
    }

    public Set<String> listLabels() {
	return alpha2ipa.keySet();
    }
}


/* Alphabet.java ends here */
