package marytts.modules;

// Mary elementary packages
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.data.Utterance;

// Exceptions
import marytts.exceptions.MaryConfigurationException;
import marytts.MaryException;

// Logging
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

/**
 * The base class to subclass to implement a module to be used in MaryTTS
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public abstract class MaryModule {
    public static final int MODULE_OFFLINE = 0;
    public static final int MODULE_RUNNING = 1;

    private MaryConfiguration default_configuration = null;
    protected int state;

    /**
     * The logger instance to be used by this module. It will identify the
     * origin of the log message in the log file.
     */
    protected Logger logger;


    /**
     *  Default constructor but available only for subclass. It just sets the default configuration
     *
     */
    protected MaryModule() {
	this(MaryConfigurationFactory.getDefaultConfiguration());
    }


    /**
     *  Constructor but available only for subclass. It just sets a given configuration
     *
     *  @param default_configuration the given configuration
     */
    protected MaryModule(MaryConfiguration default_configuration) {
	this.default_configuration = default_configuration;
        logger = LogManager.getLogger(this);
        this.state = MODULE_OFFLINE;
    }

    /**
     *  Get the state of the module (MODULE_RUNNING or MODULE_OFFLINE).
     *
     *  @return the state of the module
     */
    public int getState() {
        return state;
    }

    /**
     *  Get the default configuration of the module.
     *
     *  @return the default configuration object
     */
    public MaryConfiguration getDefaultConfiguration() {
	return default_configuration;
    }

    /**
     *  Apply the default configuration (could be seen as a kind of reset of parameters)
     *
     *  @throws MaryConfigurationException if anything is going wrong
     */
    public void applyDefaultConfiguration() throws MaryConfigurationException {
	if (default_configuration != null) {
	    default_configuration.applyConfiguration(this);
	}
    }

    /**
     *  Method to start the modules. This consists of applying the configuration and change the
     *  state by default.
     *
     *  @throws MaryException if the startup is failing
     */
    public void startup() throws MaryException {
        assert state == MODULE_OFFLINE;
	applyDefaultConfiguration();
        state = MODULE_RUNNING;

	logger.debug("\n" + MaryConfigurationFactory.dump());

        logger.info("Module " + this.getClass().toGenericString() + " started.");
    }

    /**
     *  Abstract method to validate that the startup succeeded. Only the actual module is
     *  responsible of checking that needed parameters are sets.
     *
     *  @throws MaryConfigurationException if anything is going wrong
     */
    public abstract void checkStartup() throws MaryConfigurationException;

    /**
     *  Method do shutdown the module. By default just the state is changed
     *
     */
    public void shutdown() {
        logger.info("Module shut down.");
        state = MODULE_OFFLINE;
    }

    /**
     *  Method to add an appender to the module logger
     *
     *  @param app the appender to add to the logger
     */
    protected void addAppender(Appender app) {
	((org.apache.logging.log4j.core.Logger) this.logger).addAppender(app);
    }

    /**
     *  Check if the input contains all the information needed to be
     *  processed by the module.
     *
     *  @param utt the input utterance
     *  @throws MaryException which indicates what is missing if something is missing
     */
    public abstract void checkInput(Utterance utt) throws MaryException;

    /**
     *  Method to call the module without any logging and the default configuration
     *
     *  @param utt the input utterance
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt) throws MaryException {
        return process(utt, new MaryConfiguration());
    }


    /**
     *  Method to call the module with a given appender and the default configuration
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt, Appender app) throws MaryException {
	((org.apache.logging.log4j.core.Logger) this.logger).addAppender(app);
        return process(utt, new MaryConfiguration());
    }


    /**
     *  Method to call the module with a given appender and a given configuration
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @param runtime_configuration the user configuration
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public Utterance process(Utterance utt, MaryConfiguration runtime_configuration, Appender app) throws MaryException {
	addAppender(app);
        return process(utt, runtime_configuration);
    }


    /**
     *  Method to call the module with a given configuration. This method should be implemented by
     *  the subclass module. All the other ones are referring to this one at the end.
     *
     *  @param utt the input utterance
     *  @param app the given appender
     *  @param runtime_configuration the user configuration
     *  @return the enriched utterance
     *  @throws MaryException if anything is going wrong
     */
    public abstract Utterance process(Utterance utt, MaryConfiguration runtime_configuration) throws MaryException;
}
