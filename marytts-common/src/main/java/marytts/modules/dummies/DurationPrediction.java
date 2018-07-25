package marytts.modules.dummies;

import marytts.MaryException;
// Configuration
import marytts.config.MaryConfiguration;
// Data
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.phonology.Phone;
import marytts.data.item.phonology.Phoneme;
import marytts.exceptions.MaryConfigurationException;
// Module
import marytts.modules.MaryModule;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class DurationPrediction extends MaryModule
{
    private static final double DEFAULT_DUR = 10.0;

    public DurationPrediction() throws Exception
    {
	super("duration");
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
        if (!utt.hasSequence(SupportedSequenceType.PHONE)) {
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

	double start = 0.0;
	Sequence<Phoneme> ph_seq = (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);
	for (int i=0; i<ph_seq.size(); i++) {
	    Phone tmp = new Phone(ph_seq.get(i), start, DEFAULT_DUR);
	    ph_seq.set(i, tmp);
	    start += DEFAULT_DUR;
	}

	return utt;
    }

    public void setDescription() {
	this.description = "Dummy duration prediction which sets each phone at 1s.";
    }
}
