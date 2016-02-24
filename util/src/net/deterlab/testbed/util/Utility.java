package net.deterlab.testbed.util;

import java.util.Properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.GeneralSecurityException;

import java.security.cert.X509Certificate;

import javax.activation.DataHandler;
import javax.net.ssl.SSLException;

import net.deterlab.abac.ABACException;
import net.deterlab.abac.Identity;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.AdminDeterFault;
import net.deterlab.testbed.client.AdminStub;
import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;
import net.deterlab.testbed.client.RealizationsDeterFault;
import net.deterlab.testbed.client.RealizationsStub;
import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

import org.apache.axis2.client.Stub;

import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;

import org.apache.axis2.description.MessageContextListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

/**
 * Base class that gives utility programs access to common functionality.
 * @author DETER Team
 * @version 1.0
 */
public class Utility {
    /** Utility properties */
    static protected Properties props = null;
    /** Property file name */
    static protected File propName = 
	new File(System.getProperty("user.home"), ".deterutils.properties");


    /**
     * This class listens to the messages created by an AxisService and prints
     * the envelopes (messages) to a PrintStream when they go out.  The helper
     * functions below can be used to attach and remove these from a stub.
     * @author DETER team
     * @version 1.0
     */
    static public class SerializeEnvelope implements MessageContextListener {
	/** The output stream */
	private PrintStream writer;

	/**
	 * Create a new serializer. 
	 * @param w the output stream.  If null, output to System.err
	 */
	public SerializeEnvelope(PrintStream w) {
	    writer = (w != null ) ? w : System.err;
	}

	/**
	 * Create a new serializer
	 */
	public SerializeEnvelope() { this(null); }

	/**
	 * Handle attachServiceContext events: if an envelope is present in teh
	 * message context, print it.
	 * @param sc - ignored
	 * @param mc MessageContext to check for an envelope.
	 */
	public void attachServiceContextEvent(ServiceContext sc, 
		MessageContext mc) { 
	    if ( mc.getEnvelope() == null || writer == null ) return;
	    writer.println(mc.getEnvelope().toString());
	}
	/**
	 * This is called when an incoming message arrives - always output that
	 * message's envelope.
	 * @param mc the MessageContext to check
	 */
	public void attachEnvelopeEvent(MessageContext mc) {
	    if ( writer == null ) return;
	    writer.println(mc.getEnvelope().toString());
	}
	/**
	 * Return the underlying PrintStream.  NB, closing this without
	 * disconnecting the class from the AxisService (via stopLoggingSOAP)
	 * is contraindicated.
	 * @return the underlying PrintStream
	 */
	public PrintStream getStream() { 
	    return writer;
	}
    }
    /**
     * Start serializing events on stub to s.  S must be an existing
     * SerializeEnvelope object that can be used to stop tracing.
     * @param stub the client stub to trace
     * @param s the SerializeEnvelope object to use.
     * @return s
     */
    static public SerializeEnvelope logSOAP(Stub stub, SerializeEnvelope s) {
	stub._getServiceClient().getAxisService().addMessageContextListener(s);
	return s;
    }


    /**
     * Start serializing events on stub to p.  Creates and returns a
     * SerializeEnvelope object that can be used to stop tracing.
     * @param stub the client stub to trace
     * @param p the stream to trace to - if null, trace to System.err
     * @return a SerializeEnvelope that refers to this trace.
     */
    static public SerializeEnvelope logSOAP(Stub stub, PrintStream p) {
	SerializeEnvelope s = new SerializeEnvelope(p);

	stub._getServiceClient().getAxisService().addMessageContextListener(s);
	return s;
    }

    /**
     * Start serializing events on stub to f.  Creates and returns a
     * SerializeEnvelope object that can be used to stop tracing.
     * @param stub the client stub to trace
     * @param f the file to trace to
     * @return a SerializeEnvelope that refers to this trace.  If the file
     * cannot be used, return null;
     */
    static public SerializeEnvelope logSOAP(Stub stub, File f) {
	try { 
	    return logSOAP(stub, new PrintStream(new FileOutputStream(f)));
	}
	catch (IOException e) { 
	    return null;
	}
    }

    /**
     * Start serializing events on stub to fn.  Creates and returns a
     * SerializeEnvelope object that can be used to stop tracing.
     * @param stub the client stub to trace
     * @param fn the filename to trace to
     * @return a SerializeEnvelope that refers to this trace.  If the file
     * cannot be used, return null;
     */
    static public SerializeEnvelope logSOAP(Stub stub, String fn) {
	return logSOAP(stub, new File(fn));
    }

    /**
     * Stop tracing using this serializer on this stub.  Also flushes the
     * serializer.
     * @param stub the stub
     * @param s the serializer
     */
    static public void stopLoggingSOAP(Stub stub, SerializeEnvelope s) {
	stub._getServiceClient().getAxisService().removeMessageContextListener(
		s);
	if ( s.getStream() != null ) 
	    s.getStream().flush();
    }


    /*
     * Axis2 logs a lot of data using log4j.  This stops warnings and greatly
     * reduces the logged data flow.  Also initialize the utility property
     * object.
     */
    static {
	BasicConfigurator.configure();

	Logger.getRootLogger().setLevel(Level.ERROR);
	setPropertyFile(propName, true);
    };

    /**
     * Set the axis2 logging level
     * @param level the new logging level
     */
    static public void setAxis2LoggingLevel(Level level) {
	Logger.getRootLogger().setLevel(level);
    }

    /**
     * Set the file in which to store the utility properties, and reload it if
     * requested.  If reload is set then the underlying properties object will
     * be cleared and relaoded from the given filename.  If that filename is
     * not properly formatted or not accessible, the properties object will be
     * empty.  If reload is false this function resets the save target, future
     * calls to saveProperty will save the entire properties file in the new
     * target.
     * @param f the new file underlying utility properties
     * @param reload if true, load the utility properties from f
     * @return true if the properties were successfully loaded or reload is
     * false
     */
    static public boolean setPropertyFile(File f, boolean reload) {
	if ( f != null ) propName = f;

	if (reload) {
	    // If the properties object fails to load, utilities can still
	    // (potentially) write to it.
	    props = new Properties();
	    try { 
		props.load(new FileReader(propName));
		return true;
	    }
	    catch (IOException ignored) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Get the generic property with the given kjey from teh default utility
     * property store.
     * @param key the property key
     * @return the property value
     */
    static public String getProperty(String key) {
	return (props != null ) ? props.getProperty(key) : null;
    }

    /**
     * Set the property with teh given key to value in the utility store, and
     * save the store.
     * @param key the property key
     * @param value the property value
     * @throws IOException if there is an error saving the property file.
     */
    static public void setProperty(String key, String value) 
	    throws IOException {
	if ( props == null ) 
	    throw new IOException("Failed to init property store");
	props.setProperty(key, value);
	props.store(new FileWriter(propName), "DETER utilities properties");
    }

    /**
     * Return the default service url
     * @return the default service url
     */
    static public String getServiceUrl() { return getProperty("serviceurl"); }

    /**
     * Return the default service url with the service appended
     * @param service the service to append
     * @return the default service url with the service appended
     */
    static public String getServiceUrl(String service) { 
	return getServiceUrl() + service; 
    }

    /**
     * Return the default trust store filename
     * @return the default trust store filename
     */
    static public String getTrustStoreFilename() { 
	return getProperty("trustfilename");
    }

    /**
     * Return the default trust store password
     * @return the default trust store password
     */
    static public String getTrustStorePassword() { 
	return getProperty("trustpw");
    }

    /**
     * Return the default userID filename
     * @return the default userID filename
     */
    static public String getUserIDFilename() { 
	return getProperty("useridfilename");
    }

    /**
     * Return the default userID password
     * @return the default userID password
     */
    static public String getUserIDPassword() { 
	return getProperty("useridpw"); 
    }

    /**
     * Load trusted certificates from keystore.
     * @param store the filename of the store to load from
     * @param the password of the store, if any
     * @return true if the store was present and assigned
     */
    static public boolean loadTrust(String store, String pwd) {
	if ( store == null ) return false;
	File sf = new File(store);

	if (!sf.exists()) return false;
	System.setProperty("javax.net.ssl.trustStore", store);
	if ( pwd != null )
	    System.setProperty("javax.net.ssl.trustStorePassword", pwd);
	return true;
    }

    /**
     * Load the trusted store from the deadult file with the default password.
     */
    static public boolean loadTrust() {
	return loadTrust(getTrustStoreFilename(), getTrustStorePassword());
    }

    /**
     * Load user ID from keystore.  If the store is not in the filesystem,
     * return without changing anything.
     * @param store the filename of the store to load from
     * @param pwd the password of the store, if any
     * @return true if the store was present and assigned
     */
    static public boolean loadID(String store, String pwd) {
	if ( store == null ) return false;
	File sf = new File(store);

	if (!sf.exists()) return false;
	System.setProperty("javax.net.ssl.keyStore", store);
	if ( pwd != null ) 
	    System.setProperty("javax.net.ssl.keyStorePassword", pwd);
	return true;
    }

    /**
     * Load the user ID from the deadult file with the default password.
     * @return true if the store was present and assigned
     */
    static public boolean loadID() {
	return loadID(getUserIDFilename(), getUserIDPassword());
    }

    /**
     * Print msg to stderr and exit with a high exit code.
     * @param msg the message to print
     */
    static public void fatal(String msg) {
	System.err.println(msg);
	System.exit(20);
    }

    /**
     * Exit with an error code of 0
     */
    static public void exit() {
	System.exit(0);
    }

    /**
     * Print a warning message and continue
     * @param msg the message
     */
    static public void warn(String msg) {
	System.err.println(msg);
    }

    /**
     * Put a byte array into a DataHandler so that they can be made part of a
     * request.  The result is a DataHandler with MIME type
     * "application/octet-string containing the byte array.
     * @param b the bytes to store
     * @return a properly initialized DataHandler
     */
    static public DataHandler putBytes(byte[] b) {
	return new DataHandler(b, "application/octet-string");
    }

    /**
     * Get a byte array from a DataHandler.  This is the usual way to retrieve
     * opaque values from an RPC call.
     * @param dh the DataHandler
     * @return a byte array with the data handler's contents
     * @throws IOException if there are I/O problems.
     */
    static public byte[] getBytes(DataHandler dh) throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	dh.writeTo(bos);
	return bos.toByteArray();
    }

    /**
     * Print a hex dump of a byte array.  Mostly for debugging.
     * @param bytes the array to dump
     * @param lim max number of bytes to print.  0 is print all
     * @param p the PrintStream to output the data on.
     */
    static public void dumpBytes(byte[] bytes, int lim, PrintStream p) {
	int w = 0;
	String chars = "";

	if (lim == 0) lim = Integer.MAX_VALUE;

	for (byte b : bytes) {
	    if ( w % 16 == 0 )
		p.format("%04x - ", w);
	    p.format("%02x ", b);

	    w ++;
	    if ( w >= lim ) {
		p.println();
		return;
	    }
	    if ( w %16 == 0 ) p.println();
	}
    }

    /**
     * Print a hex dump of a byte array to System.out.  Mostly for debugging.
     * @param bytes the array to dump
     * @param lim max number of bytes to print.  0 is print all
     */
    static public void dumpBytes(byte[] bytes, int lim) {
	dumpBytes(bytes, lim, System.out);
    }

    /**
     * Print a hex dump of a byte array to System.out.
     * @param bytes the array to dump
     */
    static public void dumpBytes(byte[] bytes) {
	dumpBytes(bytes, 0, System.out);
    }

    /**
     * Read bytes of f into a byte[] and return them.
     * @param f the File to read
     * @return a byte array
     * @throws IOException on read error
     */
    static protected byte[] readFile(File f) throws IOException {
	FileInputStream is = new FileInputStream(f);
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	final int bufSize = 1024 * 1024;
	byte[] buf = new byte[bufSize];
	int r = 0;

	while ( ( r = is.read(buf)) != -1)
	    bout.write(buf, 0, r);
	return bout.toByteArray();
    }

    /**
     * Do the format translations to get a DeterFault from a AdminDeterFault.
     * @param uf the AdminDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(AdminDeterFault uf) {
	AdminStub.AdminDeterFault uudf = uf.getFaultMessage();
	AdminStub.DeterFault udf = (uudf != null) ? uudf.getDeterFault() : null;

	return (udf != null )? new DeterFault(udf.getErrorCode(),
		udf.getDetailMessage()) : null;
    }
    /**
     * Do the format translations to get a DeterFault from a UsersDeterFault.
     * @param uf the UsersDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(UsersDeterFault uf) {
	UsersStub.UsersDeterFault uudf = uf.getFaultMessage();
	UsersStub.DeterFault udf = (uudf != null) ? uudf.getDeterFault() : null;

	return (udf != null )? new DeterFault(udf.getErrorCode(), 
		udf.getDetailMessage()) : null;
    }
    /**
     * Do the format translations to get a DeterFault from a CirclesDeterFault.
     * @param uf the CirclesDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(CirclesDeterFault cf) {
	CirclesStub.CirclesDeterFault ccdf = cf.getFaultMessage();
	CirclesStub.DeterFault cdf = (ccdf != null) ? 
	    ccdf.getDeterFault() : null;

	return (cdf != null )? new DeterFault(cdf.getErrorCode(), 
		cdf.getDetailMessage()) : null;
    }

    /**
     * Do the format translations to get a DeterFault from a
     * ExperimentsDeterFault.
     * @param pf the ProjectsDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(ExperimentsDeterFault ef) {
	ExperimentsStub.ExperimentsDeterFault eedf = ef.getFaultMessage();
	ExperimentsStub.DeterFault edf = (eedf != null) ? 
	    eedf.getDeterFault() : null;

	return (edf != null )? new DeterFault(edf.getErrorCode(), 
		edf.getDetailMessage()) : null;
    }

    /**
     * Do the format translations to get a DeterFault from a
     * LibrariesDeterFault.
     * @param pf the ProjectsDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(LibrariesDeterFault lf) {
	LibrariesStub.LibrariesDeterFault lldf = lf.getFaultMessage();
	LibrariesStub.DeterFault ldf = (lldf != null) ?
	    lldf.getDeterFault() : null;

	return (ldf != null )? new DeterFault(ldf.getErrorCode(),
		ldf.getDetailMessage()) : null;
    }

    /**
     * Do the format translations to get a DeterFault from a ProjectsDeterFault.
     * @param pf the ProjectsDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(ProjectsDeterFault pf) {
	ProjectsStub.ProjectsDeterFault ppdf = pf.getFaultMessage();
	ProjectsStub.DeterFault pdf = (ppdf != null) ? 
	    ppdf.getDeterFault() : null;

	return (pdf != null )? new DeterFault(pdf.getErrorCode(), 
		pdf.getDetailMessage()) : null;
    }

    /**
     * Do the format translations to get a DeterFault from a
     * ResourcesDeterFault.
     * @param pf the ResourcesDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(ResourcesDeterFault rf) {
	ResourcesStub.ResourcesDeterFault rrdf = rf.getFaultMessage();
	ResourcesStub.DeterFault rdf = (rrdf != null) ? 
	    rrdf.getDeterFault() : null;

	return (rdf != null )? new DeterFault(rdf.getErrorCode(), 
		rdf.getDetailMessage()) : null;
    }

    /**
     * Do the format translations to get a DeterFault from a
     * RealizationsDeterFault.
     * @param pf the RealizationsDeterFault from the connection
     * @return a DeterFault that can be accessed simply.
     */
    static public DeterFault getDeterFault(RealizationsDeterFault rf) {
	RealizationsStub.RealizationsDeterFault rrdf = rf.getFaultMessage();
	RealizationsStub.DeterFault rdf = (rrdf != null) ?
	    rrdf.getDeterFault() : null;

	return (rdf != null )? new DeterFault(rdf.getErrorCode(),
		rdf.getDetailMessage()) : null;
    }

    /**
     * Trace through the cause chain of an AxisFault to see if it was an SSL
     * failure.  If so terminate with a message to that effect, therwise
     * terminate with teh root AxisFault's message.
     * @param e the AxisFault
     */
    static public void  handleAxisFault(AxisFault e) {
	for ( Throwable t = e.getCause(); t != null; t=t.getCause() ) {
	    if (t instanceof SSLException  ) 
		fatal("SSL error: has your login expired?");
	}
	fatal(e.getMessage());
    }

    /**
     * Join an array of strings into single string separated by d
     * @param s the array
     * @param d the delimiter
     * @return the connected string
     */
    static String joinStrings(String[] s, String d) {
	if ( s == null || s.length < 1) return "";
	StringBuilder sb = new StringBuilder(s[0]);
	for (int i = 1; i < s.length; i++ ) {
	    if (d != null ) sb.append(d);
	    if (s[i] != null) sb.append(s[i]);
	}
	return sb.toString();
    }

    /**
     * Create an ABAC Identity from a byte array containing a PEM file.
     * @param b the PEM file contents
     * @return an ABAC ID
     * @throws ABACException if there are problems with the ID
     */
    static public Identity loadIdentity(byte[] b) throws ABACException {
	ByteArrayInputStream bi = new ByteArrayInputStream(b);
	return new Identity(bi);
    }

    /**
     * Execute the login exchange and return the returned ID, if any
     * @param uid user to log in as
     * @param password the password
     * @return the ID generated by Deter, if any
     * @throws DETERFault if the login fails.
     */
    static public Identity login(String uid, String password) 
	throws DeterFault {
	Identity i = null;
	try {
	    UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	    return login(stub, uid, password);
	}
	catch (AxisFault e) {
	    handleAxisFault(e);
	    return null;
	}
    }

    /**
     * Execute the login exchange and return the returned ID, if any
     * @stub the stib to make the callout on
     * @param uid user to log in as
     * @param password the password
     * @return the ID generated by Deter, if any
     * @throws DETERFault if the login fails.
     */
    static public Identity login(UsersStub stub, String uid, String password) 
	throws DeterFault {
	Identity i = null;
	try {
	    UsersStub.RequestChallenge req = new UsersStub.RequestChallenge();

	    req.setUid(uid);
	    req.setTypes(new String[] { "clear" });

	    UsersStub.RequestChallengeResponse resp = stub.requestChallenge(req);
	    UsersStub.UserChallenge uc = resp.get_return();
	    UsersStub.ChallengeResponse chR = new UsersStub.ChallengeResponse();

	    chR.setResponseData(putBytes(password.getBytes()));
	    chR.setChallengeID(uc.getChallengeID());

	    UsersStub.ChallengeResponseResponse chRR = 
		stub.challengeResponse(chR);

	    DataHandler dh = chRR.get_return();
	    if ( dh != null ) {
		i = loadIdentity(getBytes(dh));
	    }
	}
	catch (UsersDeterFault e) {
	    throw getDeterFault(e);
	}
	catch (AxisFault e) {
	    handleAxisFault(e);
	}
	catch (ABACException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot parse returned Cert");
	}
	catch (Exception e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Unexpected Exception " + e);
	}

	return i;
    }

    /**
     * Logout.
     * @throws DETERFault if the login fails.
     */
    static public void logout() throws DeterFault {
	try {
	    UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	    logout(stub);
	}
	catch (AxisFault e) {
	    handleAxisFault(e);
	}
    }

    /**
     * Logout.
     * @stub the stib to make the callout on
     * @throws DETERFault if the login fails.
     */
    static public void logout(UsersStub stub) throws DeterFault {
	try {
	    UsersStub.Logout req = new UsersStub.Logout();

	    stub.logout(req);
	}
	catch (UsersDeterFault e) {
	    throw getDeterFault(e);
	}
	catch (AxisFault e) {
	    handleAxisFault(e);
	}
	catch (Exception e) {
	    throw new DeterFault(DeterFault.internal,
		    "Unexpected Exception " + e);
	}
    }
    /**
     * Store the ABAC Identity into a PEM file.  The key will be unencrypted.
     * @param i the ABAC Identity
     * @param f the keystore file
     * @throws ABACException if the Identity is incomplete
     * @throws IOException if there are filesystem problems manipulating the
     * store.
     */
    static public void identityToPem(Identity i, File f) 
	    throws ABACException, GeneralSecurityException, IOException {
	KeyPair kp = i.getKeyPair();
	if (kp == null ) 
	    throw new ABACException("No private keys in " +i);

	FileOutputStream fo = new FileOutputStream(f);
	i.write(fo);
	i.writePrivateKey(fo);
	fo.close();
    }

    /**
     * Translate the ABAC identity into JKS PrivateKey Entry and store it in
     * the given file.
     * @param i the ABAC Identity
     * @param f the keystore file
     * @param passwd the password used to protect the store and the key
     * @throws ABACException if the Identity is incomplete
     * @throws GeneralSecurityException if there are crypto problems with the
     * keystore
     * @throws IOException if there are filesystem problems manipulating the
     * store.
     */
    static public void identityToKeyStore(Identity i, File f, char[] passwd) 
	    throws ABACException, GeneralSecurityException, IOException {
	KeyPair kp = i.getKeyPair();
	if (kp == null ) 
	    throw new ABACException("No private keys in " +i);

	KeyStore ks = KeyStore.getInstance("JKS");
	KeyStore.PrivateKeyEntry pke = 
	    new KeyStore.PrivateKeyEntry(kp.getPrivate(), 
		    new X509Certificate[] { i.getCertificate() });

	ks.load( f.exists() ? new FileInputStream(f) : null, passwd);
	if (ks.containsAlias(i.getName()))
	    ks.deleteEntry(i.getName());

	ks.setEntry(i.getName(), pke, new KeyStore.PasswordProtection(passwd));
	ks.store(new FileOutputStream(f), passwd);
    }

    /**
     * Print a formatted copy of the given RealizationDescription on the given
     * stream.  Used by RealizeExperiment and ViewRealizations.
     * @param rd the RealizationDescription to print
     * @param p the PrintStream
     */
    static public void dumpRealizationDescription(
	    ExperimentsStub.RealizationDescription rd, PrintStream p) {
	ExperimentsStub.AccessMember[] acl = (rd.getACL() != null) ?
	    rd.getACL() : new ExperimentsStub.AccessMember[0];
	ExperimentsStub.RealizationContainment[] cont =
	    (rd.getContainment() != null) ?
		rd.getContainment() :
		new ExperimentsStub.RealizationContainment[0];
	ExperimentsStub.RealizationMap[] map =
	    (rd.getMapping() != null) ?
		rd.getMapping() : new ExperimentsStub.RealizationMap[0];

	p.println("Name: " + rd.getName());
	p.println("\tStatus: " + rd.getStatus());
	p.println("\tExperiment: " + rd.getExperiment());
	p.println("\tCircle: " + rd.getCircle());
	p.println("\tACL: ");
	for (ExperimentsStub.AccessMember m : acl)
	    p.println("\t\t"+m.getCircleId() + " " +
		    joinStrings(m.getPermissions(), ", "));

	p.println("\tContainment: ");
	for (ExperimentsStub.RealizationContainment m : cont)
	    p.println("\t\t" + m.getOuter() + " <- " + m.getInner());
	p.println("\tMapping: ");
	for (ExperimentsStub.RealizationMap m : map)
	    p.println("\t\t" + m.getResource() + " -> " + m.getTopologyName());
    }

    /**
     * Print a formatted copy of the given RealizationDescription on the given
     * stream.  An adapter to the dumpRealizationDescription with the
     * ExperimentsStub prefix.
     * @param rrd the RealizationDescription to print
     * @param p the PrintStream
     */
    static public void dumpRealizationDescription(
	    RealizationsStub.RealizationDescription rrd, PrintStream p) {
	ExperimentsStub.RealizationDescription rd =
	    new ExperimentsStub.RealizationDescription();
	int i = 0;

	rd.setName(rrd.getName());

	RealizationsStub.AccessMember[] oldAcl =
	    (rrd.getACL() != null) ? rrd.getACL() :
		new RealizationsStub.AccessMember[0];
	ExperimentsStub.AccessMember[] newAcl =
	    new ExperimentsStub.AccessMember[oldAcl.length];

	i = 0;
	for (RealizationsStub.AccessMember m: oldAcl) {
	    newAcl[i] = new ExperimentsStub.AccessMember();
	    newAcl[i].setCircleId(m.getCircleId());
	    newAcl[i].setPermissions(m.getPermissions());
	    i++;
	}

	rd.setACL(newAcl);
	rd.setCircle(rrd.getCircle());
	RealizationsStub.RealizationContainment[] oldCont =
	    (rrd.getContainment() != null) ? rrd.getContainment() :
		new RealizationsStub.RealizationContainment[0];
	ExperimentsStub.RealizationContainment[] newCont =
	    new ExperimentsStub.RealizationContainment[oldCont.length];

	i = 0;
	for (RealizationsStub.RealizationContainment m: oldCont) {
	    newCont[i] = new ExperimentsStub.RealizationContainment();
	    newCont[i].setOuter(m.getOuter());
	    newCont[i].setInner(m.getInner());
	    i++;
	}
	rd.setContainment(newCont);
	rd.setExperiment(rrd.getExperiment());

	RealizationsStub.RealizationMap[] oldMap =
	    (rrd.getMapping() != null) ? rrd.getMapping() :
		new RealizationsStub.RealizationMap[0];
	ExperimentsStub.RealizationMap[] newMap =
	    new ExperimentsStub.RealizationMap[oldMap.length];

	i = 0;
	for (RealizationsStub.RealizationMap m: oldMap) {
	    newMap[i] = new ExperimentsStub.RealizationMap();
	    newMap[i].setResource(m.getResource());
	    newMap[i].setTopologyName(m.getTopologyName());
	    i++;
	}
	rd.setMapping(newMap);
	rd.setPerms(rrd.getPerms());
	rd.setStatus(rrd.getStatus());
	dumpRealizationDescription(rd, p);
    }
}
