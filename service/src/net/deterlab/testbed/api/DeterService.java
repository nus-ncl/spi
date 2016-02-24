package net.deterlab.testbed.api;

import org.apache.axis2.context.MessageContext;
import org.apache.catalina.connector.Request;

import java.io.Reader;
import java.io.OutputStreamWriter;

import java.lang.reflect.Constructor;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;

import net.deterlab.abac.ABACException;
import net.deterlab.abac.BadSignatureException;
import net.deterlab.abac.CertInvalidException;
import net.deterlab.abac.Context;
import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;

import net.deterlab.testbed.db.SharedConnection;

import net.deterlab.testbed.embedding.Embedder;

import net.deterlab.testbed.policy.Credentials;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.policy.CredentialStoreDB;

import net.deterlab.testbed.project.ProjectDB;

import net.deterlab.testbed.topology.TopologyDescription;

import net.deterlab.testbed.user.UserDB;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Base class for API services; subclasses of this export web services to
 * applications.  It provides internal shared routines, and no externally
 * accessible operations.  Applications need not and cannot access it directly.
 *
 * @author DETER team
 * @version 1.0
 */
public class DeterService {
    /** True if logging has been configured */
    private boolean loggingStarted = false;

    /**
     * Simple constructor.
     */
    // If the logging system has not been started, start it.
    public DeterService() { 
	if ( loggingStarted) return;
	loggingStarted = true;
	try {
	    Config config = new Config();
	    String logConfig = config.getLogConfiguration();

	    // If the service configuration a configuration specified, use it.
	    // Otherwise, log to the console.
	    if (logConfig != null ) 
		PropertyConfigurator.configure(logConfig);
	    else
		BasicConfigurator.configure();
	}
	catch (Exception e) {
	    Logger.getRootLogger().error("Error configuring logs: " + 
		    e.getMessage());
	}
    }

    /**
     * Set the logger for this class.  A NOP, but some subclasses need it and
     * the interface is cleaner this way.
     * @param l the new logger
     */
    protected void setLogger(Logger l) { }

    /**
     * Get an ABAC identity derived from the caller's client certificate. If
     * the identity cannot be foud or converted, throw a DeterFault with an
     * apropriate code and detail.
     * @return the RPC caller's ABAC.Identity
     * @throws DeterFault with code and detail set
     */
    protected Identity getCallerIdentity() throws DeterFault {
	MessageContext ctxt = MessageContext.getCurrentMessageContext();
	ServletRequest req = null;
	X509Certificate[] certs = null;

	/* This is basically voodoo to find the client certificate from the
	 * axis servlet system.
	 */
	if ( ctxt == null ) 
	    throw new DeterFault(DeterFault.internal, "null Message context");
	req = (ServletRequest)ctxt.getProperty("transport.http.servletRequest");
	if ( req == null ) 
	    throw new DeterFault(DeterFault.request, 
		    "Null SSL request where SSL required");
	certs = (X509Certificate[]) req.getAttribute(
		"javax.servlet.request.X509Certificate");

	if (certs == null ) 
	    throw new DeterFault(DeterFault.request, 
		    "No client certificate and one is required");
	try {
	    return new Identity(certs[0]);
	} catch (ABACException e) {
	    throw new DeterFault(DeterFault.access,
		    "Error creating ABAC identity from X.509: " + 
		    e.getMessage());
	}
    }

    /**
     * Get the caller identity if there is one, but don't throw any exceptions
     * if one cannot be found.
     * @return the caller ABAC Identity, if there is one.
     */
    protected Identity getOptionalCallerIdentity() {
	try {
	    return getCallerIdentity();
	}
	catch (DeterFault df) {
	    return null;
	}
    }

    // XXX This is very ad-hoc right now and a better implementation will be
    // needed
    /**
     * Send an e-mail to address with the given subject and conntent.
     * @param address destination address
     * @param subject the subject line of the e-mail
     * @param content the contents of the e-mail 
     * @throws DeterFault if the mail cannot be submitted
     */
    protected void sendEmail(String address, String subject, 
	    Reader content) throws DeterFault {
	Config config = new Config();
	// /usr/bin/mail expects the destination last
	ProcessBuilder mailer = new ProcessBuilder(
		config.getMailer(), "-s", subject, address);
	Process m = null;
	OutputStreamWriter stdin = null;
	char[] buf= new char[10 * 1024];
	int r = 0;	    // Scratch

	try { 
	    m = mailer.start();
	    stdin = new OutputStreamWriter(m.getOutputStream());
	    while ( (r = content.read(buf)) != -1)  
		stdin.write(buf, 0, r);
	    stdin.close();
	    r = m.waitFor();

	    if ( r != 0 ) 
		throw new DeterFault(DeterFault.internal,
		    "Cannot send mail: mailer returned " + r);

	}
	catch (DeterFault df) {
	    // Throw any inner DeterFaults out unmodified
	    throw df;
	}
	catch (Exception e ) {

	    throw new DeterFault(DeterFault.internal, 
		    "Cannot send mail: " + e.getMessage());
	}
    }
    /**
     * Check the user's access based on the requested info.  If a uid is given,
     * that id is used, otherwise the caller's ID is looked up, and that user's
     * CredentialSet is loaded in addition to the set passed in from outside,
     * and then the caller is checked for having the requested attribute.  If
     * all is well, nothing happens.  If the shared connetion is not null, use
     * it to look up relevant credentials. If there are errors, a DeterFault is
     * thrown.
     * @param attr the traget attribute
     * @param csets the object credential sets to load
     * @param u the uid to check access for
     * @param sc a shared database connection
     * @throws DeterFault if access is denied or if an error occurs determining
     * access.
     */
    protected void checkAccess(String attr, Collection<CredentialSet> csets,
	    String u, SharedConnection sc) throws DeterFault {
	Credentials cr = new Credentials();
	CredentialStoreDB cdb = null;
	List<CredentialSet> sets = new ArrayList<CredentialSet>();
	Context ctxt = new Context();
	Logger log = Logger.getLogger(getClass());
	Identity caller = null;
	String target = null;
	boolean loadedUser = false;

	if ( attr == null || csets == null )
	    throw new DeterFault(DeterFault.internal,
		    "Bad parameters to checkAccess");

	try {
	    String uid = u;
	    cdb = new CredentialStoreDB(sc);

	    if ( uid == null ) {
		caller = getCallerIdentity();
		uid = cdb.keyToUid(caller);
	    }

	    if ( uid == null )
		throw new DeterFault(DeterFault.login, "Not logged in");

	    for (CredentialSet cs: csets) {
		if (log.isDebugEnabled())
		    log.debug("Loading " + cs.getType() + " " + cs.getName());
		sets.add(cs);
		if ( cs.getType().equals("user") && cs.getName().equals(uid))
		    loadedUser = true;
	    }
	    // If the set passed in is the same as the one we're generating to
	    // add user credentials, don't load it twice.
	    if ( !loadedUser) {
		if (log.isDebugEnabled())
		    log.debug("Loading user " + uid);
		sets.add(new CredentialSet("user", uid));
	    }
	    cdb.loadContext(ctxt, sets);
	    cdb.close();

	    if ( caller != null ) target = caller.getKeyID();
	    else target = cr.scopeRoleString("user_" + uid);
	}
	catch (DeterFault df) {
	    if (cdb != null ) cdb.forceClose();
	    throw df;
	}

	Context.QueryResult qr = ctxt.query(cr.scopeRoleString(attr), 
		target);

	if ( qr.getSuccess() ) {

	    if ( log.isDebugEnabled()) {
		for (Credential c: qr.getCredentials())
		    log.debug(c.head().simpleString(ctxt) + "<-" + 
			c.tail().simpleString(ctxt));
	    }
	}
	else {
	    if ( log.isDebugEnabled()) {
		log.debug("Access denied (debug)");
		log.debug("looked for  " + attr + " " + target);
		for (Credential c: ctxt.credentials())
		    log.debug(c.head().simpleString(ctxt) + "<-" + 
			c.tail().simpleString(ctxt));
	    }
	    throw new DeterFault(DeterFault.access, "Access denied");
	}
    }
    /**
     * Check the user's access based on the requested info.  If a uid is given,
     * that id is used, otherwise the caller's ID is looked up, and that user's
     * CredentialSet is loaded in addition to the set passed in from outside,
     * and then the caller is checked for having the requested attribute.  If
     * all is well, nothing happens.  If the shared connetion is not null, use
     * it to look up relevant credentials. If there are errors, a DeterFault is
     * thrown.
     * @param attr the traget attribute
     * @param set the object credential set to load
     * @param u the uid to check access for
     * @param sc a shared database connection
     * @throws DeterFault if access is denied or if an error occurs determining
     * access.
     */
    protected void checkAccess(String attr, CredentialSet set, String u,
	    SharedConnection sc) throws DeterFault {
	checkAccess(attr, Arrays.asList(new CredentialSet[] { set }), u, sc);
    }
    /**
     * Check the user's access based on the requested info.  The caller's ID is
     * looked up, and that user's CredentialSet is loaded in addition to the
     * set passed in from outside, and then the caller is checked for having
     * the requested attribute.  If all is well, nothing happens.  If the
     * shared connetion is not null, use it to look up relevant credentials. If
     * there are errors, a DeterFault is thrown.
     * @param attr the traget attribute
     * @param set the object credential set to load
     * @param sc a shared database connection
     * @throws DeterFault if access is denied or if an error occurs determining
     * access.
     */
    protected void checkAccess(String attr, CredentialSet set,
	    SharedConnection sc) throws DeterFault {
	checkAccess(attr, Arrays.asList(new CredentialSet[] { set }), null, sc);
    }

    /**
     * Check to see if the user can create an object with a scoped name
     * ({project|user}:name).
     * @param name the name to check
     * @param type the type of object to create (first letter caps)
     * @param sc a shared DB connection
     * @throws DeterFault if access is denied or there is a DB problem
     */
    protected void checkScopedName(String name, String type,
	    SharedConnection sc) throws DeterFault {
	String[] parts = name.split(":", 2);
	if ( parts.length != 2 )
	    throw new DeterFault(DeterFault.request,
		    "Bad " + type + " name");

	if ( sc == null ) sc = new SharedConnection();

	ProjectDB p = null;
	try {
	    // Check creation rights based on the namespace (project/user)
	    p = new ProjectDB(parts[0], sc);
	    if ( p.isValid() && p.isApproved())
		checkAccess("project_" + parts[0] + "_create" + type,
			new CredentialSet("project", parts[0]), sc);
	    else
		checkAccess("user_" + parts[0] + "_create" + type,
			new CredentialSet("user", parts[0]), sc);
	    p.close();
	}
	catch (DeterFault df) {
	    if ( p != null) p.forceClose();
	    throw df;
	}
    }

    /**
     * Return a list of users with a given attribute.
     * @param attr the attribute to search for
     * @param csets the credential sets to load
     * @param sc a shared connection.
     * @throws DeterFault if access is denied or if an error occurs determining
     * access.
     */
    protected List<String> getUsersWithAccess(String attr,
	    Collection<CredentialSet> csets, SharedConnection sc)
	throws DeterFault {

	Credentials cr = new Credentials();
	CredentialStoreDB cdb = null;
	Context ctxt = new Context();
	Logger log = Logger.getLogger(getClass());
	List<CredentialSet> sets = new ArrayList<>();
	List<String> rv = new ArrayList<>();

	if ( attr == null || csets == null )
	    throw new DeterFault(DeterFault.internal,
		    "Bad parameters to getUSersWithAccess");

	try {
	    Context.QueryResult qr = null;
	    Map<String, String> principalToUid = new HashMap<>();
	    List<String> allUsers = new ArrayList<>();
	    int i = 0;

	    cdb = new CredentialStoreDB(sc);
	    for (CredentialSet cs: csets)
		sets.add(cs);

	    for (UserDB u : UserDB.getUsers(null, sc)) {
		String uid = u.getUid();

		sets.add(new CredentialSet("user", uid));
		allUsers.add(uid);
	    }
	    cdb.loadContext(ctxt, sets);
	    cdb.close();

	    // The query that finds all principals with an attribut requires
	    // users to be bound to prinicpals, but dummy principals are fine.
	    // Bind each user in the context ro a dummy principal and keep a
	    // map of them.
	    for (String u : allUsers) {
		String p = String.format("%040x", i++);
		Credential cred = cr.makeCredentialKeyID("user_" + u, p);

		principalToUid.put(p, u);
		ctxt.load_attribute_chunk(cred);
	    }
	    qr = ctxt.query(cr.scopeRoleString(attr), "");
	    if ( !qr.getSuccess() ) return rv;

	    // If there are principals who have the attribute, back-map them to
	    // uids.
	    for (Credential c: qr.getCredentials())
		if ( c.tail().is_principal()) {
		    String u = principalToUid.get(c.tail().toString());

		    if ( u != null ) rv.add(u);
		}
	    return rv;
	}
	catch (DeterFault df) {
	    if (cdb != null ) cdb.forceClose();
	    throw df;
	}
    }
    /**
     * Return a list of users with a given attribute.
     * @param attr the attribute to search for
     * @param set a single credential set to load
     * @param sc a shared connection.
     * @throws DeterFault if access is denied or if an error occurs determining
     * access.
     */
    protected List<String> getUsersWithAccess(String attr, CredentialSet set,
	    SharedConnection sc) throws DeterFault {
	return getUsersWithAccess(attr,
		Arrays.asList(new CredentialSet[] { set }), sc);
    }

    /**
     * Get an embedder from a TopologyDescription.  Specifically, if there is
     * an embedder attribute in the description, try to instantiate that class,
     * and if that's possible, return it.  Otherwise return the default.
     * @param td the description
     * @param defaultEmbedder the default embedder
     * @return the selected embedder
     * @throws DeterFault on a complete failure
     */
    protected Embedder getEmbedder(TopologyDescription td,
	    Embedder defaultEmbedder) throws DeterFault {
	Embedder rv = defaultEmbedder;
	String embedder = null;

	if ( (embedder = td.getAttribute("embedder")) != null )
	    rv = getEmbedder(embedder, defaultEmbedder);
	return rv;
    }
    /**
     * Get an embedder from a String.  Try to instantiate that class,
     * and if that's possible, return it.  Otherwise return the default.
     * @param embedderSTring
     * @param defaultEmbedderString the default embedder
     * @return the selected embedder
     * @throws DeterFault on a complete failure
     */
    protected Embedder getEmbedder(String embedderString,
	    Embedder defaultEmbedder) throws DeterFault {
	Embedder rv = defaultEmbedder;

	try {
	    if ( embedderString  == null ) return rv;

	    Class<?> cl = Class.forName(embedderString);
	    Constructor c = cl.getConstructor(new Class[0]);

	    if ( c == null ) return rv;
	    rv = (Embedder) c.newInstance(new Object[0]);
	}
	catch (LinkageError le) {
	    throw new DeterFault(DeterFault.request,
		    "Error linking on class " + embedderString + ": " + le);
	}
	catch (ReflectiveOperationException re ) {
	    throw new DeterFault(DeterFault.request,
		    "Reflection Error on class " + embedderString + ": " + re);
	}
	catch (IllegalArgumentException ie) {
	    throw new DeterFault(DeterFault.request,
		    "Illegal argument on class " + embedderString + ": " + ie);
	}
	return rv;
    }
}
