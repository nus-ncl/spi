package net.deterlab.testbed.api;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Properties;

import net.deterlab.testbed.api.DeterFault;

/**
 * This class encapsulates global API parameters, used internally by service
 * implementations.   External services and applications will not need to
 * access it.
 * <p>
 * This reads from a static instance of a properties object that contains the
 * configuration.  The constructor primarily gives services a way to tell that
 * the configuration is missing.
 *
 * @author DETER Team
 * @version 1.0
 */
public class Config {
    /** shared properties */
    static protected Properties props = null;
    /** filename from which to draw properties */
    static protected File filename = 
	new File("/usr/local/etc/deter/service.properties");

    /**
     * Create a new configuration.
     * @throws DeterFault if the configuration is unreadable
     */
    public Config() throws DeterFault {
	if (props != null) return;
	try {
	    props = new Properties();
	    props.load(new FileReader(filename));
	}
	catch (IOException e) {
	    props = null;
	    throw new DeterFault(DeterFault.internal, 
		    "Cannot load configuration: " + e.getMessage());
	}
    }

    /**
     * The URL for the DETER database
     * @return the URL for the DETER database
     */
    public String getDeterDbUrl() { 
	if ( props == null ) return null;
	return props.getProperty("deterDbUrl");
    }

    /**
     * The URL for the legacy Emulab database
     * @return the URL for the DETER database
     */
    public String getEmulabDbUrl() {
	if ( props == null ) return null;
	return props.getProperty("emulabDbUrl");
    }

    /**
     * The keystore holding the service's ID
     * @return The keystore holding the service's ID
     */
    public String getKeystore() {
	if ( props == null ) return null;
	return props.getProperty("keystorefilename");
    }
     
    // XXX clearly must be made more secure
    /**
     * The keystore password
     * @return The keystore password
     */
    public char[] getKeystorePassword() {
	if ( props == null ) return null;
	String pwProp = props.getProperty("keystorepw");

	return (pwProp != null) ? pwProp.toCharArray() : null;
    }

    /**
     * Return the filename containing the log4j configuration properties.
     * @return the filename containing the log4j configuration properties.
     */
    public String getLogConfiguration() {
	if ( props == null ) return null;
	return props.getProperty("logconfig");
    }

    /**
     * Return the mailer
     * @return the mailer
     */
    public String getMailer() {
	if (props == null) return null;
	String rv = props.getProperty("mailer");
	return (rv != null) ? rv : "/usr/bin/mail";
    }

    /**
     * Return the experiment files root directory
     * @return the experiment files root directory
     */
    public String getExperimentRoot() {
	if (props == null) return null;
	return props.getProperty("experiment_root");
    }

    /**
     * Return the realization files root directory
     * @return the realization files root directory
     */
    public String getRealizationRoot() {
	if (props == null) return null;
	return props.getProperty("realization_root");
    }

    /**
     * Return the file containing the Aspect classnames
     * @return the file containing the Aspect classnames
     */
    public String getAspectClassFile() {
	return getProperty("aspectClassFile");
    }

    /**
     * Return the file containing the server certificate for export
     * @return the file containing the server certificate for export
     */
    public String getServerCertificateFile() {
	return getProperty("ServerCertFile");
    }

    /**
     * Return the world circle ID, if any.
     * @return the world circle ID, if any.
     */
    public String getWorldCircle() {
	return getProperty("WorldCircle");
    }
    /**
     * Return a comma separated list of system projects to be created by
     * bootstrap.
     * @return a comma separated list of system projects to be created by
     *	    bootstrap.
     */
    public String getSystemProjects() {
	return getProperty("SystemProjects");
    }

    /**
     * Return the e-mail address for support, if any
     * @return the e-mail address for support, if any
     */
    public String getSupportEmail() {
	return getProperty("supportEmail");
    }


    /**
     * Return a named property
     * @param pname the property name
     * @return a named property
     */
    public String getProperty(String pname) {
	return (props != null ) ? props.getProperty(pname) : null;
    }
}
