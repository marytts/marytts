package marytts.modules.acoustic;


// Utils
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;

// Mary default
import marytts.modules.MaryModule;
import marytts.config.MaryConfiguration;
import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;

// Mary data representation
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.Item;
import marytts.data.utils.IntegerPair;

// Features
import marytts.features.FeatureMap;


/**
 * Generate a binary normalised vector corresponding to the feature map
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class FeatureNormalisation
{
    public FeatureNormalisation()
    {

    }

    public void checkStartup() throws MaryConfigurationException {
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public void checkInput(Utterance utt) throws MaryException {
        if (!utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryException("Phone sequence is missing", null);
        }
    }

    /**
     * The process method which take a Utterance object in parameter, compute the
     * features of the utterance referenced in the parameter and return a new
     * Utterance object which contains the reference to the updated utterance.
     *
     * @param d
     *            the input Utterance object
     * @return the Utterance object with the updated reference
     * @throws Exception
     *             [TODO]
     */
    public Utterance process(Utterance utt, MaryConfiguration configuration) throws MaryException {
	return utt;
    }

}


/* FeatureNormalisation.java ends here */
