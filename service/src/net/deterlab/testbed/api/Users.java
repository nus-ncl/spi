package net.deterlab.testbed.api;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.abac.Identity;
import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.experiment.ExperimentDB;
import net.deterlab.testbed.library.LibraryDB;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.Credentials;
import net.deterlab.testbed.policy.PolicyFile;
import net.deterlab.testbed.project.ProjectDB;
import net.deterlab.testbed.user.CryptPasswordHash;
import net.deterlab.testbed.user.NotificationStoreDB;
import net.deterlab.testbed.user.PasswordHash;
import net.deterlab.testbed.user.UserChallengeDB;
import net.deterlab.testbed.user.UserDB;
import net.deterlab.testbed.user.UserProfileDB;
import net.deterlab.testbed.user.UserValidatorDB;
import net.deterlab.testbed.user.UserValidatorPasswordResetDB;

/**
 * This service manages users &ndash; the system representation of researchers
 * using DETER. This service allows a user to log in and out, manage their
 * password, manage their profile, and manage system notifications.
 * <p>
 * Logging in is accomplished through a challenge response protocol.  The user
 * requests a challenge from the system and responds to it to get access.  The
 * user is identified by their X.509 certificate and as long as the session
 * remains active, subsequent calls with that client certificate are ascribed
 * to the logged in user.
 * <p>
 * A user can also reset and change their password theough this interface.  The
 * system can expire passwords, so users may need to do this periodically.
 * <p>
 * System <a href="UserNotification.html">notifications</a> are system messages
 * that users may need to respond to.  This service allows users to collect and
 * respond to such messages.
 * <p>
 * Users have profiles like  <a href="Circles.html">circles</a> and
 * <a href="Projects.html">projects</a> and this service allows users to
 * manipulate them.
 *
 * @author ISI DETER team
 * @version 1.0
 */
public class Users extends ProfileService {

    /**
     * Class to contain a type of validator and a method for instantiating
     * them.
     */
    static private class TypeConstructor {
	/** Validator type */
	private String name;
	/** Constructor to get the validator */
	private Constructor<? extends UserValidatorDB> cons;
	/** Constructor to get the validator with a shared Connection */
	private Constructor<? extends UserValidatorDB> sharedCons;

	/**
	 * Make a TypeConstructor from a type value and a class name.
	 * @param typeName the type name
	 * @param className the class name
	 */
	public TypeConstructor(String typeName, String className) {
	    name = typeName;
	    cons = null;
	    try {
		// Reflection voodoo to get constructors.  Nothe that
		// asSubclass will throw a ClassCastException if this asks to
		// create something that's not a UserValidatorDB.
		Class <? extends UserValidatorDB> cl =
		    Class.forName(className).asSubclass(UserValidatorDB.class);
		cons = cl.getConstructor(new Class<?>[0]);
		sharedCons = cl.getConstructor(
			new Class<?>[] { SharedConnection.class} );
	    }
	    catch (Exception ignored) { }
	}
	/**
	 * Return the type name
	 * @return the type name
	 */
	public String getName() { return name; }
	/**
	 * Instantiate a validator.
	 * @throws DeterFault if there is any problem creating the Validator
	 */
	public UserValidatorDB getInstance() throws DeterFault {
	    try {
		return cons.newInstance(new Object[0]);
	    }
	    catch (Exception e) {
		throw new DeterFault(DeterFault.internal,
			"Cannot construct validator: " + e);
	    }
	}
	/**
	 * Instantiate a validator that shares the connection
	 * @throws DeterFault if there is any problem creating the Validator
	 */
	public UserValidatorDB getInstance(SharedConnection sc)
		throws DeterFault {
	    try {
		return sharedCons.newInstance(new Object[] { sc });
	    }
	    catch (Exception e) {
		throw new DeterFault(DeterFault.internal,
			"Cannot construct validator: " + e);
	    }
	}
    }
    /**
     * This is a list of constructors for the validators that this system
     * supports in order of preference by the system.
     */
    static final TypeConstructor[] validators = new TypeConstructor[] {
	new TypeConstructor("clear",
		"net.deterlab.testbed.user.UserValidatorClearDB")
    };
    /** Users log */
    private Logger log;
    /**
     * Construct a Users service.
     */
    public Users() { 
	setLogger(Logger.getLogger(this.getClass()));
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
     * Return a challenge to the client.  The server has a list of challenges
     * that it will accept and the client makes a set of requested challenges.
     * If the client's list is null, any challenge type is acceptable.
     * The servers favorite is picked.
     * <p>
     * Currently only "clear" is supported, which requires the client to
     * respond with their password across an encrypted channel.
     * <p>
     * Even in the "clear" case, the server does not store the user's clear
     * text password.
     *
     * @param uid the userid for whom authentication is requested
     * @param types a list of challenges that the caller supports.  May be null.
     * @return the challenge
     * @throws DeterFault if there is a problem issuing the challenge
     */
    public UserChallenge requestChallenge(String uid, String[] types) 
	    throws DeterFault {
	SharedConnection sc = null;
	UserDB u = null;
	log.info("requestChallenge for " + uid);
	try {
	    //CryptPasswordHash cr = new CryptPasswordHash(null);
	    if (uid == null ) 
		throw new DeterFault(DeterFault.request, 
			"No uid given for challenge");

	    sc = new SharedConnection();
	    sc.open();

	    Set<String> requested = new TreeSet<String>();
	    u = new UserDB(uid, sc);

	    u.load();

	    // Gather up user requests, if any
	    if (types != null && types.length > 0  ) 
		for (String t: types)
		    requested.add(t);

	    TypeConstructor chosen = null;
	    for (TypeConstructor v: validators ) {
		// If the user doesn't want this, skip it
		if ( requested.size() > 0 && !requested.contains(v.getName()))
		    continue;
		// Pick the first one the user likes
		chosen = v;
		break;
	    }

	    if ( chosen == null ) 
		throw new DeterFault(DeterFault.request,
			"Requested challenges not supported");
	    UserValidatorDB uv = null;
	    UserChallenge uc = null;
	    try {
		uv = chosen.getInstance(sc);
		uc = uv.issueChallenge(u);
		uv.close();
	    }
	    catch (DeterFault df) {
		if ( uv != null ) uv.forceClose();
		throw df;
	    }
	    u.close();
	    sc.close();

	    log.info("requestChallenge for " + uid + " succeeded");
	    return uc;
	}
	catch (DeterFault df) {
	    log.error("requestChallenge failed: " + df);
	    if ( u != null) u.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Evaluate a user response to a requested challenge.  On success, the user
     * is logged in.  On failure the challenge cannot be successfully responded
     * to.
     * <p>
     * This can be callled with or without a client certificate &ndash; note
     * that the connection is encrypted either way.  If the user supplied a
     * certificate signed by this server, that certificate is returned.  If no
     * such certificate is supplied, a new one, signed by this server is
     * returned.  A user can get an initial certificate that way.
     *
     * @param responseData the users response
     * @param challengeID the identifier of the challenge being responsed to
     * @return A signed certificate to use in future communications, in PEM
     * format.
     * @throws DeterFault if the challenge fails
     */
    public byte[] challengeResponse(byte[] responseData, long challengeID ) 
	    throws DeterFault {
	SharedConnection sc = null;
	UserChallengeDB uc = null;
	CredentialStoreDB cdb = null;

	log.info("challengeResponse id " + challengeID);
	try{ 
	    String uid = null;
	    String oldUid = null;
	    sc = new SharedConnection();
	    sc.open();

	    uc = new UserChallengeDB(sc);

	    uc.load(challengeID);
	    for (TypeConstructor v: validators ) {
		if ( v.getName().equals(uc.getType())) {
		    Identity id = getOptionalCallerIdentity();
		    Config config = new Config();
		    Credentials cr = new Credentials();
		    boolean existingId = id != null;
		    String files = null;
		    UserValidatorDB uv = null;

		    try {
			uv = v.getInstance(sc);
			uid = uv.validateChallenge(responseData, uc);
			uv.close();
		    }
		    catch (DeterFault df) {
			if ( uv != null ) uv.forceClose();
			throw df;
		    }
		    if ( uid == null ) 
			throw new DeterFault(DeterFault.access, 
				"Invalid repsonse");

		    cdb = new CredentialStoreDB(sc);
		    // If this key was bound to a user, unbind it.
		    if ( id != null && (oldUid = cdb.keyToUid(id)) != null ) {
			cdb.unbindKey(id);
			files = config.getProperty("userLogoutPolicy");

			for ( String fn : files.split(",")) {
			    PolicyFile uPolicy = new PolicyFile(new File(fn));
			    uPolicy.removeCredentials(cdb, null, oldUid);
			}
		    }
		    // If this is a call made without an X.509 ID, create a new
		    // ID.
		    if ( id == null ) id = cr.generateIdentity(uid);
		    files = config.getProperty("userLoginPolicy");

		    for ( String fn : files.split(",")) {
			PolicyFile uPolicy = new PolicyFile(new File(fn));
			uPolicy.addCredentials(cdb, null, uid, null, id);
		    }
		    cdb.bindKey(id, uid, 24 * 3600);
		    cdb.close();
		    uc.close();
		    sc.close();
		    log.info("challengeResponse id " + challengeID + 
			    " succeeded for " + uid + " (" + id + ")");
		    return existingId ? null : cr.identityToBytes(id);
		}
	    }
	    uc.close();
	    sc.close();
	    throw new DeterFault(DeterFault.request, "Invalid Response");
	}
	catch (DeterFault df) {
	    log.error("challengeResponse id " + challengeID + " failed: " + df);
	    if ( uc != null ) uc.forceClose();
	    if ( cdb != null ) cdb.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Terminate the binding between this key and the login.
     * @return true if logout was successful.
     * @throws DeterFault if not logged in or difficulties with the session
     */
    public boolean logout() throws DeterFault {
	CredentialStoreDB cdb = null;
	log.info("logout");
	try {
	    Identity id = getOptionalCallerIdentity();
	    cdb = new CredentialStoreDB();

	    if ( id == null ) 
		throw new DeterFault(DeterFault.request, 
			"You are not logged in (no certificate presented)");
	    String uid = cdb.keyToUid(id);
	    if ( uid != null) {
		Config config = new Config();
		String files = config.getProperty("userLogoutPolicy");

		for ( String fn : files.split(",")) {
		    PolicyFile uPolicy = new PolicyFile(new File(fn));
		    uPolicy.removeCredentials(cdb, null, uid);
		}
	    }
	    log.info("Logout " + (uid != null ? uid : "no uid mapped to cert"));
	    cdb.unbindKey(id);
	    cdb.close();
	    log.info("logout succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("logout failed: " + df);
	    if (cdb != null ) cdb.forceClose();
	    throw df;
	}
    }

    /**
     * Return an empty user profile  - the user schema (the ID field of
     * the profile returned will be empty).  Applications will call this to get
     * the names, formats, and other requirements of the user schema.  The
     * caller does not to be logged in or a valid user to successfully call
     * this operation.
     * @return an empty user profile
     * @throws DeterFault on error
     */
    public Profile getProfileDescription() throws DeterFault {
	UserProfileDB up = null;

	try {
	    Profile rv = getProfileDescription(up = new UserProfileDB());
	    up.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( up != null ) up.forceClose();
	    throw df;
	}
    }

    /**
     * Return the completed profile associated with uid.  The schema
     * information is all returned as well as the values for populated fields
     * in this user's profile.
     * @param uid the user whose profile is being retrieved.
     * @return a completed user profile for this user
     * @throws DeterFault on perimission errors or no such user.
     */
    public Profile getUserProfile(String uid) throws DeterFault {
	UserProfileDB up = null;

	try {
	    Profile rv = getProfile(uid, up = new UserProfileDB());
	    up.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( up != null ) up.forceClose();
	    throw df;
	}
    }

    /**
     * Process a list of change requests for attributes in uid's profile.
     * Each request is encoded in a
     * <a href="ChangeAttribute.html">ChangeAttribute</a>.
     * For each request, a <a href="ChangeResult.html">ChangeResult</a>
     * is returned, either indicating that the change has gone through or
     * that it failed.  Failed changes are annotated with a reason.
     * @param uid the user whose profile is being modified
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     * @see ChangeAttribute
     * @see ChangeResult
     */
    public ChangeResult[] changeUserProfile(String uid,
	    ChangeAttribute[] changes) throws DeterFault {

	UserProfileDB all = null;
	UserProfileDB up = null;
	try {
	    all = new UserProfileDB();
	    up = new UserProfileDB(all.getSharedConnection());
	    ChangeResult[] rv = changeProfile(uid,
		    up, all, changes);
	    all.close();
	    up.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( up != null ) up.forceClose();
	    if ( all != null ) all.forceClose();
	    throw df;
	}
    }

    /**
     * Request a password reset challenge.  These challenges requested when a
     * user cannot login - forgotten password, expired password, etc.  They are
     * mailed to the user from their required e-mail address in their profile.
     * Because this kind of request is expected from a web interface, the call
     * can request a URL to prepend to the challenge ID.  This means that the
     * e-mail redirects the user back into the web interface for the actual
     * password resetting process.  This is called using an unauthenticated
     * connection.
     * @param uid the user to request a reset
     * @param urlPrefix optional string to prepend to the challenge in e-mail
     * @return true (errors cause faults)
     * @throws DeterFault if an error occurs
     */
    public boolean requestPasswordReset(String uid, String urlPrefix) 
	    throws DeterFault {
	UserDB u = null;
	UserProfileDB up = null;
	UserChallenge chall = null;
	SharedConnection sc = null;
	Attribute addrAttr = null;
	String addr = null;
	StringWriter bodyString = null;
	PrintWriter body = null;
	ArrayList<String> profileElements = new ArrayList<String>();
	Config config = new Config();
	String supportEmail = config.getSupportEmail();

	if (supportEmail == null )
	    supportEmail = "testbed-ops@deterlab.net";

	log.info("requestPasswordReset for " + uid);
	try {

	    if ( uid == null) 
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    // Get the profile
	    up = new UserProfileDB(uid, sc);
	    profileElements.add("email");
	    up.load(profileElements);

	    if ( (addrAttr  = up.lookupAttribute("email")) == null ) 
		throw new DeterFault(DeterFault.internal, 
			"No email attribute for " + uid + " !?");
	    if ( (addr = addrAttr.getValue()) == null )
		throw new DeterFault(DeterFault.internal, 
			"No email attribute value for " + uid + " !?");
	    up.close();

	    u = new UserDB(uid, sc);
	    // Get the user.
	    u.load();

	    UserValidatorPasswordResetDB validator = null;
	    // Create the challenge and get an ID.
	    try {
		validator = new UserValidatorPasswordResetDB(sc);
		chall = validator.issueChallenge(u);
		validator.close();
	    }
	    catch (DeterFault df) {
		if (validator != null ) validator.forceClose();
		throw df;
	    }

	    // Challenge is committed, compose some e-mail
	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println("Someone (maybe you) has requested a password " + 
		    " change on your DETER account (uid " + uid+ ")");
	    body.println();
	    body.println("To make that change, go to " + urlPrefix + 
		    chall.getChallengeID());
	    body.println();
	    body.println("If you have other concerns, contact " +
		    supportEmail);
	    body.close();
	    sendEmail(addr, "DETER password reset", 
		    new StringReader(bodyString.toString()));
	    u.close();
	    sc.close();
	    log.info("requestPasswordReset for " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("requestPasswordReset for " + uid + " failed: " + df);
	    if (u != null) u.forceClose();
	    if (up != null) up.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}

    }

    /**
     * Change the password of a logged in user.  The user must be authenticated
     * and have the proper privileges.
     * @param uid the users password to change
     * @param newPass the new password
     * @return true (errors cause faults)
     * @throws DeterFault on access control or database errors
     */
    public boolean changePassword(String uid, String newPass) 
	    throws DeterFault {
	SharedConnection sc = null;
	UserDB u = null;
	PasswordHash ph = null;

	log.info("changePassword for " + uid);
	try {
	    if ( uid == null )
		throw new DeterFault(DeterFault.request, "missing uid");
	    if ( newPass == null )
		throw new DeterFault(DeterFault.request, "missing password");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_changePassword", 
		    new CredentialSet("user", uid), sc);

	    u = new UserDB(uid, sc);
	    u.load();
	    if ( (ph = u.getPasswordHash()) == null ) 
		throw new DeterFault(DeterFault.internal, 
			"No password hash for " + uid + "?!?");
	    ph.hashAndSet(newPass);
	    u.setExpiration(new Date(System.currentTimeMillis() + 
			365L * 24L * 3600L * 1000L));
	    u.save();
	    u.close();
	    sc.close();
	    log.info("changePassword for " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("changePassword for " + uid + " failed: " + df);
	    if ( u != null ) u.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change the password of a user based on a valid challenge.  If there is a
     * valid passwordReset challenge with the given ID, make the change.
     * @param challengeID the challenge being responded to
     * @param newPass the new password
     * @return true (errors cause faults)
     * @throws DeterFault on access control or database errors
     */
    public boolean changePasswordChallenge(long challengeID, String newPass) 
	    throws DeterFault {
	SharedConnection sc = null;
	UserDB u = null;
	String uid = null;
	PasswordHash ph = null;
	UserChallengeDB chall = null;
	UserValidatorPasswordResetDB validator = null;

	log.info("changePasswordChallenge for " + challengeID);
	try { 
	    sc = new SharedConnection();
	    sc.open();

	    chall = new UserChallengeDB(sc);
	    validator = new UserValidatorPasswordResetDB(sc);
	    chall.load(challengeID);

	    // This removes the challenge from the DB.
	    uid = validator.validateChallenge(null, chall);

	    chall.close();
	    validator.close();
	    if (newPass == null) 
		throw new DeterFault(DeterFault.request, "No password?");
	    u = new UserDB(uid, sc);
	    u.load();

	    if ( (ph = u.getPasswordHash()) == null ) 
		throw new DeterFault(DeterFault.internal, 
			"No password hash for " + u.getUid() + "?!?");
	    ph.hashAndSet(newPass);
	    u.setExpiration(new Date(System.currentTimeMillis() + 
			365L * 24L * 3600L * 1000L));
	    u.save();
	    u.close();
	    sc.close();
	    log.info("changePasswordChallenge for " + challengeID + 
		    " succeeded (uid " + uid + ")");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("changePasswordChallenge for " + challengeID 
		    + " failed: " + df);
	    if (u != null ) u.forceClose();
	    if (chall != null ) chall.forceClose();
	    if (validator != null ) validator.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Create a new user with the given profile.  The password is either taken
     * from the clear password or from the hash and type.
     * If no uid is given one is derived from the e-mail address in the
     * profile.
     *
     * @param uid The user to create
     * @param profile the filled in user profile
     * @param clearpassword a password to hash
     * @param hash a password already hashed
     * @param hashtype the hash type
     * @return the uid allocated
     * @throws DeterFault on access control or database errors
     */
    public String createUserNoConfirm(String uid, Attribute[] profile, 
	    String clearpassword, String hash, String hashtype ) 
	throws DeterFault {
	SharedConnection sc = null;
	UserDB user = null;
	UserProfileDB newUserProfile = null;
	Attribute emailAttr = null;
	PasswordHash pwd = null;
	String email = null;

	if ( uid != null ) log.info("createUserNoConfirm for " + uid);
	else log.info("createUserNoConfirm");

	try {

	    if ( clearpassword != null && (hash != null || hashtype != null))
		throw new DeterFault(DeterFault.request, 
			"Both clear password and hash supplied");
	    if ( clearpassword == null && hash == null) 
		throw new DeterFault(DeterFault.request, 
			"Neither clear password nor hash supplied");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("createUserNoConfirm", 
		    new CredentialSet("system", null), sc);

	    newUserProfile = new UserProfileDB(sc);
	    UserProfileDB empty = null;
	    try {
		empty = new UserProfileDB(sc);
		checkProfile(profile, newUserProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if (empty != null ) empty.forceClose();
		throw df;
	    }

	    // Get the profile's e-mail address
	    emailAttr = newUserProfile.lookupAttribute("email");
	    email = emailAttr.getValue();

	    // If the user did not request a uid, use the beginning of the
	    // e-mail address.
	    if ( uid == null ) 
		uid = email.substring(0, email.indexOf("@"));

	    // backstop
	    if (uid == null ) uid = "user";

	    // OK, now things get easy.  Fill in the empty fields and push.
	    user = new UserDB(uid, sc);
	    if ( clearpassword != null ) {
		pwd = new CryptPasswordHash(null);
		pwd.hashAndSet(clearpassword);
	    }
	    else {
		pwd = PasswordHash.getInstance(hashtype, hash);
		if (pwd == null ) 
		    throw new DeterFault(DeterFault.request,
			    "Unknown hash type " + hashtype);
	    }
	    user.setPasswordHash(pwd);
	    user.setExpiration(new Date(System.currentTimeMillis() + 
			365L * 24L * 3600L * 1000L));

	    // Try to create the user with the given id.  If it fails, get a
	    // unique ID and try again.  On the off chance someone stole our
	    // ID, keep trying.
	    while (true) {
		try {
		    user.create();
		    break;
		}
		catch (DeterFault df) {
		    if ( df.getErrorCode() != DeterFault.request) throw df;
		}
		user.getUniqueId(uid);
	    }
	    newUserProfile.setId(user.getUid());
	    newUserProfile.saveAll();
	    newUserProfile.close();
	    String rv = user.getUid();
	    user.close();
	    sc.close();

	    log.info("createUserNoConfirm succeeded for " + uid );
	    return rv;
	}
	catch (DeterFault df) {
	    log.error("createUserNoConfirm failed: " + df);
	    if ( user != null ) user.forceClose();
	    if ( newUserProfile != null ) newUserProfile.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Create a new user with the given profile.  No password is created, so a
     * password challenge is also issued to the user.  If no uid is supplied,
     * one is derived from the e-mail address in the profile.
     * <p>
     * An X.509 ID is returned, logged in as the new user for 30 minutes.  The
     * password is expired for that user, so once the login times out, the user
     * will have to reset the password using the password challenge above.
     * This 30 minute grace period is to allow new users to attempt to create
     * or join projects immediately and provide a more seemless user
     * experience.
     * @param uid A suggestion for the new user ID
     * @param profile the filled in user profile
     * @param urlPrefix The prefix to prepend to the new user challenge
     * @return the uid allocated
     * @throws DeterFault on access control or database errors
     */
    public CreateUserResult createUser(String uid, Attribute[] profile,
	    String urlPrefix) 
	throws DeterFault {
	SharedConnection sc = null;
	UserDB user = null;
	UserProfileDB newUserProfile = null;
	UserValidatorPasswordResetDB validator = null;
	CredentialStoreDB cdb = null;
	UserChallenge chall = null;
	StringWriter bodyString = null;
	PrintWriter body = null;
	Attribute emailAttr = null;
	String email = null;
	Config config = new Config();
	String supportEmail = config.getSupportEmail();

	if (supportEmail == null )
	    supportEmail = "testbed-ops@deterlab.net";

	if ( uid != null ) log.info("createUser for " + uid);
	else log.info("createUser");

	try {
	    sc = new SharedConnection();
	    sc.open();

	    newUserProfile = new UserProfileDB(sc);
	    UserProfileDB empty = null;
	    try {
		empty = new UserProfileDB(sc);
		checkProfile(profile, newUserProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if ( empty != null ) empty.forceClose();
		throw df;
	    }

	    // Get the profile's e-mail address
	    emailAttr = newUserProfile.lookupAttribute("email");
	    email = emailAttr.getValue();

	    // If the user did not request a uid, use the beginning of the
	    // e-mail address.
	    if ( uid == null ) 
		uid = email.substring(0, email.indexOf("@"));

	    // backstop
	    if (uid == null ) uid = "user";

	    // OK, now things get easy.  Fill in the empty fields and push.
	    user = new UserDB(uid, sc);
	    user.setPasswordHash(new CryptPasswordHash(null));
	    user.setExpiration(new Date());
	    // Try to create the user with the given id.  If it fails, get a
	    // unique ID and try again.  On the off chance someone stole our
	    // ID, keep trying.
	    while (true) {
		try {
		    user.create();
		    break;
		}
		catch (DeterFault df) {
		    if ( df.getErrorCode() != DeterFault.request) throw df;
		}
		user.getUniqueId(uid);
	    }

	    newUserProfile.setId(user.getUid());
	    newUserProfile.saveAll();
	    newUserProfile.close();

	    // The user now exists, but no password.  Generate a password
	    // challenge and an e-mail explaining the next step.
	    validator = new UserValidatorPasswordResetDB(sc);
	    chall = validator.issueChallenge(user);

	    // Challenge is committed, compose some e-mail
	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println("Someone (maybe you) has created a DETERlab account "
		    + "tied to this e-mail address");
	    body.println();
	    body.println("That account has uid " + user.getUid() +
		    ".  IF YOU REQUESTED A UID THAT WAS IN USE THIS UID IS " +
		    "DIFFERENT THAN THE ONE  YOU REQUESTED.");
	    body.println();
	    body.println("To make that account active, go to " + urlPrefix + 
		    chall.getChallengeID());
	    body.println();
	    body.println("If you have other concerns, contact " +
		    supportEmail);
	    body.close();
	    sendEmail(email, "Confirm your DETERlab account", 
		    new StringReader(bodyString.toString()));

	    // create a new X.509 certificate and log it in.
	    String finalUid = user.getUid();
	    Credentials cr = new Credentials();
	    Identity id = cr.generateIdentity(finalUid);
	    String files = config.getProperty("userLoginPolicy");
	    cdb = new CredentialStoreDB(sc);

	    for ( String fn : files.split(",")) {
		PolicyFile uPolicy = new PolicyFile(new File(fn));
		uPolicy.addCredentials(cdb, null, finalUid, null, id);
	    }
	    cdb.bindKey(id, finalUid, 30 * 60);
	    cdb.close();
	    user.close();
	    validator.close();
	    sc.close();

	    log.info("createUser succeeded for " + finalUid);
	    return new CreateUserResult(finalUid, cr.identityToBytes(id));
	}
	catch (DeterFault df) {
	    log.error("createUser failed: " + df);
	    if (user != null) user.forceClose();
	    if (validator != null) validator.forceClose();
	    if (newUserProfile != null) newUserProfile.forceClose();
	    if (cdb != null) cdb.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a user from the testbed.  A user can remove themselves and an
     * administrator can remove a user.  This will fail if the user owns
     * circles, experiments, libraries or projects (other than the per-user
     * ones).
     *
     * @param uid the user to remove
     * @return true on success
     * @throws DeterFault on failure
     */
    public boolean removeUser(String uid) throws DeterFault {
	SharedConnection sc = null;
	UserDB user = null;
	List<ProjectDB> plist = null;
	List<CircleDB> clist = null;
	List<ExperimentDB> elist = null;
	List<LibraryDB> llist = null;

	log.info("removeUser uid " + uid);
	try {
	    if ( uid == null ) 
		throw new DeterFault(DeterFault.request, 
			"Must give a user to remove");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_removeUser", 
		    new CredentialSet("user", uid), sc);
	    plist = ProjectDB.getProjects(uid, null, sc);
	    clist = CircleDB.getCircles(uid, null, sc);
	    elist = ExperimentDB.getExperiments(uid, null, null, -1, -1, sc);
	    llist = LibraryDB.getLibraries(uid, null, -1, -1, sc);

	    // Make sure the user doesn't own anything
	    for (ProjectDB pdb : plist) {
		if (uid.equals(pdb.getOwner()))
		    throw new DeterFault(DeterFault.request,
			    uid + " owns project " + pdb.getName() +
			    " cannot remove");
	    }

	    for (CircleDB cdb : clist) {
		// Ignore linked and per-user circles, they are handled by
		// projects or users objects.
		if ( cdb.getName().equals(uid + ":" + uid) || cdb.isLinked())
		    continue;
		if (uid.equals(cdb.getOwner()))
		    throw new DeterFault(DeterFault.request,
			    uid + " owns circle " + cdb.getName() +
			    " cannot remove");
	    }

	    for (ExperimentDB edb: elist) {
		if (uid.equals(edb.getOwner()))
		    throw new DeterFault(DeterFault.request,
			    uid + " owns experiment " + edb.getEid() +
			    " cannot remove");
	    }

	    for (LibraryDB ldb: llist) {
		if (uid.equals(ldb.getOwner()))
		    throw new DeterFault(DeterFault.request, 
			    uid + " owns library " + ldb.getLibid() +
			    " cannot remove");
	    }

	    // Safe to remove the user from all these circles and projects, so
	    // do it.
	    for (ProjectDB pdb : plist) {
		pdb.removeUser(uid);
		pdb.close();
	    }
	    plist = null;

	    for (CircleDB cdb : clist) {
		// Ignore linked and per-user circles, they are handled by
		// projects or users objects, but do close the CircleDB objects.
		if ( cdb.getName().equals(uid + ":" + uid) || cdb.isLinked()) {
		    cdb.close();
		    continue;
		}
		cdb.removeUser(uid);
		cdb.close();
	    }
	    clist = null;

	    UserProfileDB profile = null;
	    try {
		profile = new UserProfileDB(uid, sc);

		profile.loadAll();
		profile.removeAll();
		profile.close();
	    }
	    catch (DeterFault df) {
		log.warn("Could not remove profile: " + df.getDetailMessage());
		if ( profile != null ) profile.forceClose();
	    }
	    user = new UserDB(uid, sc);
	    user.remove();
	    user.close();
	    sc.close();
	    log.info("removeUser uid " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    if ( user != null) user.forceClose();
	    if ( plist != null ) 
		for (ProjectDB pdb : plist)
		    pdb.forceClose();
	    if ( clist != null )
		for (CircleDB cdb : clist)
		    cdb.forceClose();
	    if (sc != null ) sc.forceClose();
	    log.error("removeUser failed: " + df);
	    throw df;
	}
    }

    /**
     * Create a notification and deliver it to the given users or projects.
     * This is an administrative action.
     * @param users an array of receiver userids 
     * @param flags flags set on the notification
     * @param text the text of the notification
     * @return a boolean, true on success
     * @throws DeterFault on failure
     */
    public boolean sendNotification(String[] users, 
	    NotificationFlag[] flags, String text) throws DeterFault {
	SharedConnection sc = null;
	NotificationStoreDB store = null;

	log.info("sendNotification to " + 
		((users != null && users.length > 0) ? 
		 users[0] : " (no users)"));
	try {
	    int iFlags = NotificationStoreDB.validateFlags(flags);
	    if (users == null || users.length == 0)
		throw new DeterFault(DeterFault.request,
			"No users specified for notification");

	    if ( text == null ) 
		throw new DeterFault(DeterFault.request,
			"No notification text");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("sendNotification", 
		    new CredentialSet("system", null), sc);

	    store = new NotificationStoreDB(sc);

	    long id = store.create(text);
	    store.deliver(Arrays.asList(users), iFlags, id);
	    store.close();
	    sc.close();
	    log.info("sendNotification succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("sendNotification failed: " + df);
	    if (store != null) store.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Return a filtered array of user notifications for the given uid. Users
     * can filter on sending time and notification flags.  Calling this without
     * the <a href="NotificationFlag.html">READ_TAG NotificationFlag</a> in
     * flags will return unread notifications. The notifications
     * are returned as <a href="UserNotification.html">UserNotification</a>
     * objects.
     * @param uid return this user's notifications (required)
     * @param from starting date (may be null)
     * @param to ending date (may be null)
     * @param flags return messages with flags in this state
     * @return an array of UserNotifications meeting these criteria
     * @throws DeterFault on failure
     * @see NotificationFlag
     * @see UserNotification
     */
    public UserNotification[] getNotifications(String uid, String from,
	    String to, NotificationFlag[] flags) throws DeterFault {
	SharedConnection sc = null;
	NotificationStoreDB store = null;

	log.info("getNotifications for " + uid);
	try {
	    int iFlags = NotificationStoreDB.validateFlags(flags);
	    int mask = NotificationStoreDB.makeMask(flags);

	    if ( uid == null ) 
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_getNotifications", 
		    new CredentialSet("user", uid), sc);
	    store = new NotificationStoreDB(sc);
	    List<UserNotification> rv = store.get(uid, from, to, iFlags, mask);
	    store.close();
	    sc.close();
	    log.info("getNotifications for " + uid + " succeeded");
	    return rv.toArray(new UserNotification[0]);
	}
	catch (DeterFault df) {
	    log.error("getNotifications failed: " + df);
	    if (store != null ) store.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Set the flags on a set of notifications to this user; this is commonly
     * used to mark notifications as read.  Calling this operation on a
     * notification with the
     * <a href="NotificationFlag.html">READ_TAG  NotificationFlag</a>
     * will mark a notification as read.
     *
     * @param uid the recipient
     * @param ids notifications to flag
     * @param flags the flags to modify
     * @return true on success
     * @throws DeterFault on error
     * @see NotificationFlag
     */
    public boolean markNotifications(String uid, long[] ids, 
	    NotificationFlag[] flags) throws DeterFault {
	SharedConnection sc = null;
	NotificationStoreDB store = null;

	log.info("markNotifications for " + uid);
	try {
	    int iFlags = NotificationStoreDB.validateFlags(flags);
	    int mask = NotificationStoreDB.makeMask(flags);

	    if ( uid == null ) 
		throw new DeterFault(DeterFault.request, "Missing uid");

	    if ( ids == null || ids.length == 0 ) 
		throw new DeterFault(DeterFault.request, "No notification ids");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_markNotifications", 
		    new CredentialSet("user", uid), sc);
	    store = new NotificationStoreDB(sc);
	    store.mark(uid, ids, iFlags, mask);
	    store.close();
	    sc.close();
	    log.info("markNotifications for " + uid + " succeeded");
	    return true; 
	}
	catch (DeterFault df) {
	    log.error("markNotifications failed: " + df);
	    if (store != null ) store.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Add a new user attribute to the profile schema.  This operation is an
     * administrative action that modifies the profiles of all users to
     * include a new field.  If the new attribute is not optional, existing
     * profiles will have the new attribute set to the def parameter.
     *
     * @param name the attribute name
     * @param type the type (STRING, INT, FLOAT, OPAQUE)
     * @param optional true if this attribute is optional
     * @param access the user's ability to modify (READ_WRITE, READ_ONLY,
     *	    WRITE_ONLY, NO_ACCESS)
     * @param description natural language description of the field
     * @param format a regular expression describing the format (optional)
     * @param formatdescription a natural language explanation of the format
     * @param order the ordering of this field relative to others (0 means put
     * it last)
     * @param length a suggestion of the field's length for UI presentation
     * @param def default value of the attribute (will be assigned to all users)
     * @return true on success
     * @throws DeterFault on errors
     */
    public boolean createUserAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def) 
	throws DeterFault {
	UserProfileDB profile = null;

	try {
	    boolean rv = createAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    def, profile = new UserProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing user attribute.  This operation is
     * administrative. All fields overwrite existing ones.  An application will
     * probably need to read the attribute's schema using GetProfileDescription
     * before making changes.  Only attributes already in the schema can be
     * modified.
     *
     * @param name the attribute name
     * @param type the type (STRING, INT, FLOAT, OPAQUE)
     * @param optional true if this attribute is optional
     * @param access the user's ability to modify (READ_WRITE, READ_ONLY,
     *	    WRITE_ONLY, NO_ACCESS)
     * @param description natural language description of the field
     * @param format a regular expression describing the format (optional)
     * @param formatdescription a natural language explanation of the format
     * @param order the ordering of this field relative to others (0 means put
     * it last)
     * @param length a suggestion of the field's length for UI presentation
     * @return true on success
     * @throws DeterFault on errors
     */
    public boolean modifyUserAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length) 
	throws DeterFault {
	UserProfileDB profile = null;

	try {
	    boolean rv = modifyAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    profile = new UserProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if (profile != null) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a user attribute from the schema of all user profiles.  Any
     * values are also deleted. This operation is administrative.
     * @param name the attribute to remove
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean removeUserAttribute(String name) throws DeterFault {
	UserProfileDB profile = null;

	try {
	    boolean rv = removeAttribute(name,
		    profile = new UserProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }
}
