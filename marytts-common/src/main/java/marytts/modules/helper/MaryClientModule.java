package marytts.modules.helper;


// Mary elementary packages
import marytts.modules.MaryModule;
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.data.Utterance;

// Exceptions
import marytts.exceptions.MaryConfigurationException;
import marytts.MaryException;

// Networking / Streams
import java.net.Socket;
import java.io.IOException;

// Logging
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

/**
 *  The base class to subclass to implement a module based on network communication to be used in
 *  MaryTTS
 *
 *  The key element is that the data is normally encoded/decoded using serializers. The
 *  communication protocol should be implemented in the process method when extending this class.
 *
 *  @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public abstract class MaryClientModule extends MaryModule {
    /** The internal socket */
    protected Socket socket;

    /** The host of the server */
    protected String hostname;

    /** The port of the server */
    protected int port;

    /** The classname of the serializer used to export the utterance to be transmitted to the server */
    protected String export_serializer_classpath;

    /** The classname of the serialiser used to import into the utterance the data received by the server */
    protected String import_serializer_classpath;

    /*************************************************************************************************
     ** Constructors
     *************************************************************************************************/

    /**
     *  Constructor but available only for subclass. It just sets a given configuration
     *
     *  @param default_configuration the given configuration
     *  @param category the category of the module
     */
    protected MaryClientModule(MaryConfiguration default_configuration, String category) {
        super(default_configuration, category);
        setHostname("");
        setPort(-1);
        socket = null;
    }

    /*************************************************************************************************
     ** Configuration
     *************************************************************************************************/

    /**
     *  Method to get the socket used to communicate with the server
     *
     *  @return the socket
     */
    protected Socket getSocket() {
        return socket;
    }

    /**
     *  Method to get the port of the server
     *
     *  @return the port of the server
     */
    public int getPort() {
        return port;
    }

    /**
     *  Method to get the host of the server
     *
     *  @return the host of the server
     */
    public String getHostname() {
        return hostname;
    }

    /**
     *  Method to get the name of the export serializer class
     *
     *  @return the export serializer class
     */
    public String getExportSerializerClasspath() {
        return export_serializer_classpath;
    }

    /**
     *  Method to get the name of the import serializer class
     *
     *  @return the import serializer class
     */
    public String getImportSerializerClasspath() {
        return import_serializer_classpath;
    }


    /**
     *  Method to set the socket. If a socket was already existing, it is closed.
     *
     *  @param socket the new socket to set.
     */
    protected void setSocket(Socket socket) {
        if (getSocket() != null) {
            try {
                getSocket().close();
            } catch (IOException ex) {
                logger.warn("The socket was already closed!");
            }
        }

        this.socket = socket;
    }

    /**
     *  Method to set the port
     *
     *  @param port the new port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     *  Method to set the hostname
     *
     *  @param hostname the new hostname
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     *  Method to set the export serializer classname
     *
     *  @param export_serializer_classpath the new export serializer classname
     */
    public void setExportSerializerClasspath(String export_serializer_classpath) {
        this.export_serializer_classpath = export_serializer_classpath;
    }

    /**
     *  Method to set the import serializer classname
     *
     *  @param import_serializer_classpath the new import serializer classname
     */
    public void setImportSerializerClasspath(String import_serializer_classpath) {
        this.import_serializer_classpath = import_serializer_classpath;
    }


    /*************************************************************************************************
     ** Operations
     *************************************************************************************************/
    /**
     *  Method to start the modules. This consists of applying the configuration and change the
     *  state by default.
     *
     *  @throws MaryException if the startup is failing
     */
    public void startup() throws MaryException {
        assert state == MODULE_OFFLINE;
	applyDefaultConfiguration();

        try {
            if ((getPort() > 0) && (!getHostname().isEmpty())) {
                Socket s = new Socket(this.getHostname(), this.getPort());
                setSocket(s);
            }
        } catch (Exception ex) {
            throw new MaryException("Couldn't start MaryClientModule based module", ex);
        }
        state = MODULE_RUNNING;

	logger.debug("Configuration for module \"\"" +
		     this.getClass().toGenericString() + "\"\n" +
		     MaryConfigurationFactory.dump());

        logger.info("Module " + this.getClass().toGenericString() + " started.");
    }
}
