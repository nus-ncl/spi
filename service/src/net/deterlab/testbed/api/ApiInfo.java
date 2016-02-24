package net.deterlab.testbed.api;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import net.deterlab.abac.Identity;
import net.deterlab.testbed.policy.Credentials;
/**
 * This service provides metadata about the running instance of the DETER SPI.
 * Most of these operations can be accessed without logging in.  They are
 * generally used to sanity check an application's connection and interaction
 * with the system.  In addition, there are facilities to read X.509
 * certificates for use as trusted bases for the server and as client
 * certificates for login.
 * @author ISI DETER team
 * @version 1.0
 */
public class ApiInfo extends DeterService {
    /** Current version number */
    static protected String version = "0.3";
    /** Current patch level */
    static protected String patchLevel = "0.0";
    /** Logger for each instance */
    private Logger log;
    /** The server certificate for export */
    String myCert;

    /**
     * Construct an ApiInfo.
     */
    public ApiInfo() {
	setLogger(Logger.getLogger(this.getClass()));
	myCert = null;
    }

    /**
     * Set the logger for this class.  Subclasses set it so that appropriate
     * prefixes show up in the log file.
     * @param l the new logger
     */
    protected void setLogger(Logger l) {
	super.setLogger(l);
	log = l;
    }

    /**
     * Return the version and patchlevel of this instance of DETER; if the
     * caller presented a client certificate, return that certificate's key
     * identifier as well.
     * @return the version info
     */
    public VersionDescription getVersion() {
	Identity id = null;

	if ( (id  = getOptionalCallerIdentity()) != null )
	    log.info("GetVersion call for keyid " + id.getKeyID());
	else
	    log.info("Anonymous GetVersion call");

	return new VersionDescription(version, patchLevel,
		(id != null) ? id.getKeyID(): null);
    }

    /**
     * Return the value passed in.
     * @param param the parameter to echo
     * @return the parameter to echo
     */
    public String echo(String param) {
	log.info("Echo call \""+ param + "\"");
	return param;
    }

    /**
     * Return a pem-encoded server certificate for inclusion as a trusted
     * certificate.
     * @return a pem-encoded server certificate for inclusion as a trusted
     *	    certificate.
     * @throws DeterFault if the server certifcate has not been configured
     */
    public String getServerCertificate() throws DeterFault {
	Identity id = null;

	if ( (id  = getOptionalCallerIdentity()) != null )
	    log.info("getServerCertificate call for keyid " + id.getKeyID());
	else
	    log.info("Anonymous getServerCertificate call");

	if ( myCert == null) {
	    Config config = new Config();
	    String serverFile = config.getServerCertificateFile();

	    if ( serverFile == null )
		throw new DeterFault(DeterFault.unimplemented,
			"This server is not configured to export certificates");
	    try {
		FileReader fr = new FileReader(new File(serverFile));
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[1024];
		int r = 0;

		while ( (r = fr.read(buf, 0, 1024)) != -1)
		    sb.append(buf, 0, r);
		fr.close();
		myCert = sb.toString();
	    }
	    catch (IOException e) {
		throw new DeterFault(DeterFault.internal,
			"Error reading server certificate: " + e);
	    }
	}
	return myCert;
    }

    /**
     * Return a pem-encoded client certificate, signed by the server.
     * @param name the name to attach to the certificate.
     * @return a pem-encoded client certificate, signed by the server.
     * @throws DeterFault if there is an error creating the cert
     */
    public String getClientCertificate(String name) throws DeterFault {
	Identity id = null;
	Credentials cr = new Credentials();
	StringWriter sw = new StringWriter();
	String cn = (name != null) ? name : "generated";

	if ( (id  = getOptionalCallerIdentity()) != null )
	    log.info("getClientCertificate call for keyid " + id.getKeyID());
	else
	    log.info("Anonymous getClientCertificate call");

	id = cr.generateIdentity(cn);
	try {
	    id.write(sw);
	    id.writePrivateKey(sw);
	}
	catch (IOException ie) {
	    throw new DeterFault(DeterFault.internal,
		    "Error writing certificate: " + ie);
	}
	return sw.toString();
    }
}
