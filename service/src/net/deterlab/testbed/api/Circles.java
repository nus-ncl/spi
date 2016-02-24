
package net.deterlab.testbed.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import net.deterlab.testbed.circle.CircleChallengeDB;
import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.circle.CircleProfileDB;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.user.NotificationStoreDB;
import net.deterlab.testbed.user.UserDB;
/**
 * This service manages circles - the user groupings used to control access to
 * DETER.  Applications can use this to create, remove, and modify circles as
 * well as manipulating their profiles.  For each user in a circle, that user's
 * rights to manipulate the circle can be set and changed.  Circle ownership
 * can also be changed by the owner.
 * <p>
 * Adding users to a circle is a two step process that can be initiated either
 * by a user who wants to join an existing circle or by a circle member who is
 * inviting a user into the circle.  In both cases a member of the circle with
 * permission to add users and the user being added must concur.
 * <p>
 * The sequence by which a user joins a circle is by that user calling
 * joinCircle and a user with appropriate permissions calling
 * joinCircleConfirm.  The sequence where an member invites a new user in is
 * addUsers followed by each invited user calling addUserConfirm.  (There is
 * also an addUserNoConfirm operation, but only administrators can do that).
 *
 * @author ISI DETER team
 * @version 1.0
 */
public class Circles extends ProfileService {
    /** Logger for each instance */
    private Logger log;
    /**
     * Construct a Circles service.
     */
    public Circles() { 
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
     * Add a new circle attribute to the profile schema.  This operation is an
     * administrative action that modifies the profiles of all circles to
     * include a new field.  If the new attribute is not optional, existing
     * profiles will have the new attribute set to the def parameter.
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
    public boolean createCircleAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def) 
	throws DeterFault {
	CircleProfileDB profile = null;

	try{ 
	    boolean rv = createAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    def, profile = new CircleProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if (profile != null) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing circle attribute.  This operation is
     * administrative. All fields overwrite existing ones.  An application will
     * probably need to read the attribute's schema using GetProfileDescription
     * before making changes.  Only attributes already in the schema can be
     * modified.
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
    public boolean modifyCircleAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length) 
	throws DeterFault {
	CircleProfileDB profile = null;

	log.info("modifyCircleAttribute " + name );
	try {
	    boolean rv = modifyAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    profile = new CircleProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a circle attribute from the schema of all circle profiles.  Any
     * values are also deleted.This operation is administrative.
     * @param name the attribute to remove
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean removeCircleAttribute(String name) throws DeterFault {
	CircleProfileDB profile = null;

	log.info("removeCircleAttribute " + name );
	try {
	    boolean rv = removeAttribute(name,
		    profile = new CircleProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Return the strings that encode valid permissions for circles.
     * @return the strings that encode valid permissions for circles.
     * @throws DeterFault on system errors
     */
    public String[] getValidPermissions() throws DeterFault {
	CircleDB cdb = null;

	log.info("getValidPermissions");
	try {
	    cdb = new CircleDB();
	    Set<String> perms = cdb.getValidPerms();
	    log.info("getValidPermissions succeeded");
	    return perms.toArray(new String[0]);
	}
	catch (DeterFault df) {
	    if  (cdb != null ) cdb.forceClose();
	    log.info("getValidPermissions failed: " + df);
	    throw df;
	}
    }

    /**
     * Create a new circle with the given profile, owned by uid.    The owner
     * will be member of the circle.
     * <p>
     * If the circleid is prefixed by a project name, the caller must have the
     * CREATE_CIRCLE permission in that project.
     *
     * @param circleid the circle to remove
     * @param owner the new owner
     * @param profile the completed circle profile.  All required fields must
     *	    be present and all fields with sepcified formats must be properly
     *	    formatted.
     * @return true on success
     * @throws DeterFault on access control or database errors
     */
    public boolean createCircle(String circleid, String owner,
	    Attribute[] profile) throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;
	CircleProfileDB newCircleProfile = null;

	log.info("createCircle for " + circleid);
	try {
	    if ( circleid == null) 
		throw new DeterFault(DeterFault.request, 
			"circleid is required");

	    sc = new SharedConnection();
	    sc.open();

	    checkScopedName(circleid, "Circle", sc);
	    circle = new CircleDB(circleid, sc);
	    newCircleProfile = new CircleProfileDB(sc);
	    CircleProfileDB empty = null;
	    try {
		empty = new CircleProfileDB(sc);
		checkProfile(profile, newCircleProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if ( empty != null ) empty.forceClose();
		throw df;
	    }

	    // Make sure the owner doesn't already have a circle by this name
	    if ( circle.isValid() ) 
		throw new DeterFault(DeterFault.request, 
			owner + " already has has a circle named " + circleid);

	    // Create the circle and the profile.
	    circle.create(owner);
	    newCircleProfile.setId(circle.getName());
	    newCircleProfile.saveAll();
	    circle.close();
	    newCircleProfile.close();
	    sc.close();
	    log.info("createCircle succeeded for " + owner + ":" + circleid);
	    return true;
	}
	catch (DeterFault df) {
	    log.error("createCircle failed: " + df);
	    if ( circle != null ) circle.forceClose();
	    if ( newCircleProfile != null) newCircleProfile.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove the circle from the system completely.  This removes any
     * outstanding challenges, the entire profile, and the circle itself.  The
     * caller must be the owner or an administrator for this to succeed.
     * @param circleid the circle ID to remove
     * @return true on success
     * @throws DeterFault on access control or database errors
     */
    public boolean removeCircle(String circleid) throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;
	CircleChallengeDB challenges = null;

	log.info("removeCircle for " + circleid);
	try {
	    if ( circleid == null) 
		throw new DeterFault(DeterFault.request, 
			"circleid is required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);
	    challenges = new CircleChallengeDB(sc);

	    if ( !circle.isValid()) 
		throw new DeterFault(DeterFault.request, 
			"Invalid circle " + circleid);

	    checkAccess("circle_" + circleid + "_removeCircle", 
		    new CredentialSet("circle", circleid), sc);

	    challenges.clearChallenges(circleid);

	    CircleProfileDB profile = null;
	    try {
		profile = new CircleProfileDB(circleid, sc);
		profile.loadAll();
		profile.removeAll();
		profile.close();
	    }
	    catch (DeterFault df) {
		log.warn("Could not remove profile: " + df.getDetailMessage());
		if ( profile != null ) profile.forceClose();
	    }
	    circle.remove();
	    circle.close();
	    challenges.close();
	    sc.close();
	    log.info("removeCircle succeeded for " + circleid);
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeCircle for " + circleid + " failed: " + df);
	    if (circle != null ) circle.forceClose();
	    if (challenges != null) challenges.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Return an empty circle profile  - the profile schema (the ID field of
     * the profile returned will be empty).  Applications will call this to get
     * the names, formats, and other requirements of the circle schema.  The
     * caller does not to be logged in or a valid user to successfully call
     * this operation.
     * @return an empty circle profile
     * @throws DeterFault on error
     */
    public Profile getProfileDescription() throws DeterFault {
	CircleProfileDB cp = null;

	try {
	    Profile p = getProfileDescription(cp = new CircleProfileDB());
	    cp.close();
	    return p;
	}
	catch (DeterFault df) {
	    if (cp != null ) cp.forceClose();
	    throw df;
	}
    }

    /**
     * Return the completed profile associated with circleid.  The schema
     * information is all returned as well as the values for populated fields
     * in this circle's profile.  Any member of the circle can retrieve the
     * profile.
     * @param circleid the circle ID whose profile is being retrieved.
     * @return the completed circle profile
     * @throws DeterFault on perimission errors or no such user.
     */
    public Profile getCircleProfile(String circleid) throws DeterFault {
	CircleProfileDB cp = null;

	try {
	    Profile p = getProfile(circleid, cp = new CircleProfileDB());
	    cp.close();
	    return p;
	}
	catch (DeterFault df) {
	    if (cp != null ) cp.forceClose();
	    throw df;
	}
    }

    /**
     * Process a list of change requests for attributes in circleid's profile.
     * Each request is encoded in a
     * <a href="ChangeAttribute.html">ChangeAttribute</a>.
     * For each request, a <a href="ChangeResult.html">ChangeResult</a>
     * is returned, either indicating that the change has gone through or
     * that it failed.  Failed changes are annotated with a reason.
     * @param circleid the circle whose profile is being modified (user:name)
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     * @see ChangeAttribute
     * @see ChangeResult
     */
    public ChangeResult[] changeCircleProfile(String circleid,
	    ChangeAttribute[] changes) throws DeterFault {
	CircleProfileDB cp = null;
	CircleProfileDB all = null;

	try {
	    cp = new CircleProfileDB();
	    all = new CircleProfileDB(cp.getSharedConnection());
	    ChangeResult[] rv = changeProfile(
		    circleid, cp, all, changes);
	    cp.close();
	    all.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( cp != null ) cp.forceClose();
	    if ( all != null ) all.forceClose();
	    throw df;
	}
    }

    /**
     * Compose a notification to the given user letting them know they have
     * been added (provisionally) to circleid, including a url to confirm that
     * addition.
     * @param id the challenge identifier
     * @param uid the user to notify
     * @param circleid the destination circle
     * @param urlPrefix the UI customization
     * @param sc a shared DB connection to use
     * @throws DeterFault if there is a problem
     */
    protected void notifyUserOfAdd(long id, String uid, String circleid, 
	    String urlPrefix, SharedConnection sc) throws DeterFault {
	NotificationStoreDB notes = null;
	try {
	    StringWriter bodyString = new StringWriter();
	    PrintWriter body = new PrintWriter(bodyString);
	    long nid = -1;

	    if ( sc == null ) sc = new SharedConnection();

	    notes = new NotificationStoreDB(sc);
	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println("Someone has provisionally added you to circle " +
		    circleid + ".");
	    body.println();
	    if ( urlPrefix != null ) {
		body.println(
			"That change will not take effect until you go to " +
			urlPrefix + id);
		body.println(" or respond to the circle challenge " + id +
			" some other way");
	    }
	    else {
		body.println("That change will not take effect until you " +
			"respond to the circle challenge " + id );
	    }
	    body.println();
	    body.close();

	    nid = notes.create(bodyString.toString());
	    notes.deliver(Arrays.asList(new String[] { uid }),  0, nid);
	    notes.close();
	}
	catch (DeterFault df) {
	    if ( notes != null) notes.forceClose();
	    throw df;
	}
    }

    /**
     * Add the users with the given uids to this circle. This will issue
     * challenges to each user that can be added. Each challenge is delivered
     * via a testbed notification. When each user responds to their challenge
     * that user will actually be added.  This prevents a malicious circle
     * owner from adding users to a circle for the purpose of
     * accessing resources shared by them.
     * <p>
     * The success or failure of each attempted user add is returned in a
     * <a href="ChangeResult.html">ChangeResult</a>.  Note that these results
     * indicate whether a challenge was issued to the given users, not whether
     * those users ultimately successfully confirm their addition.
     * <p>
     * The urlPrefix, if given, is prepended to the challenge issued to the
     * user in the notification.  Applications can use this to compose URLs to
     * pass to users when the notification is shown.  A urlPrefix of the form
     * "https://application.page/some/path?challenge=" is a way to include a
     * URL in the system notification that links back into an application on
     * "application.page" with "some/path" and the challenge value assigned to
     * the "challenge" query string.
     *
     * @param circleid the circle to modify
     * @param uids the users to add
     * @param perms the rights each user will be granted
     * @param urlPrefix a prefix added to the challenge identifier to integrate
     * it with a web interface.
     * @return an array results one for each attempted addition.
     * @throws DeterFault if the command as a whole fails
     * @see ChangeResult
     */
    public ChangeResult[] addUsers(String circleid, String[] uids,
	    String[] perms, String urlPrefix) 
	throws DeterFault {

	SharedConnection sc = null;
	CircleDB circle = null;
	CircleChallengeDB chall = null;
	ArrayList<ChangeResult> added = new ArrayList<ChangeResult>();

	log.info("addUsers for " + circleid );
	try {
	    if (circleid == null ) 
		throw new DeterFault(DeterFault.request, "Circleid required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);
	    chall = new CircleChallengeDB(sc);

	    circle.validatePerms(Arrays.asList(perms));

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make membership changes to project");

	    if ( !circle.isValid()) 
		throw new DeterFault(DeterFault.request, 
			"Invalid circle " + circleid);

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Uids missing");

	    checkAccess("circle_" + circleid + "_addUsers", 
		    new CredentialSet("circle", circleid), sc);

	    for (String uid : uids ) {
		UserDB user = null;
		try {
		    user = new UserDB(uid, sc);

		    if ( circle.isMember(uid)) 
			throw new DeterFault(DeterFault.request,
				"Uid " + uid + " already a member of " + 
				circleid);

		    // Confirm user is valid
		    user.load();
		    long id = chall.addChallenge(uid, circleid,
			    Arrays.asList(perms));
		    notifyUserOfAdd(id, uid, circleid, urlPrefix, sc);
		    log.info("addUsers " + circleid + 
			    " issued challenge for " + uid);
		    added.add(new ChangeResult(uid, null, true));
		    user.close();
		}
		catch (DeterFault df) {
		    if ( user != null ) user.forceClose();
		    log.info("addUsers failed for " + circleid + " " +
			    uid + ": " + df);
		    added.add(new ChangeResult(uid, df.getDetailMessage(),
				false));
		}
	    }
	    circle.close();
	    chall.close();
	    sc.close();
	    log.info("addUsers for " + circleid + " succeeded");
	    return added.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addUsers for " + circleid + " failed: " + df);
	    if ( circle != null ) circle.forceClose();
	    if ( chall != null ) chall.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Send a notification to the given members letting them know of the
     * presence of a join request.
     * @param id the challenge id
     * @param u the members of the circle to send to
     * @param uid the user making the request
     * @param circleid the circle to join
     * @param urlPrefix to customize the notification
     * @parm sc a shared DB connection
     * @throws DeterFault on error
     */
    protected void notifyUsersOfJoin(long id, List<String> u, String uid,
	    String circleid, String urlPrefix, SharedConnection sc)
	throws DeterFault {
	NotificationStoreDB notes = null;

	try {
	    StringWriter bodyString = new StringWriter();
	    PrintWriter body = new PrintWriter(bodyString);
	    long nid = -1;

	    if ( sc == null ) sc = new SharedConnection();

	    notes = new NotificationStoreDB(sc);
	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println(uid + " has asked to join your circle " +
		    circleid + ".");
	    body.println();
	    if ( urlPrefix != null ) {
		body.println(
			"That change will not take effect until you go to " +
			urlPrefix + id);
		body.println(" or respond to the circle challenge " + id +
			" some other way");
	    }
	    else {
		body.println("That change will not take effect until you " +
			"respond to the circle challenge " + id );
	    }
	    body.println();
	    body.close();

	    nid = notes.create(bodyString.toString());
	    notes.deliver(u,  0, nid);
	    notes.close();
	}
	catch (DeterFault df) {
	    if (notes != null ) notes.forceClose();
	    throw df;
	}
    }

    /**
     * Request permission to join a circle.  This is called by a user who has
     * heard of a circle through out of band means and wants to join.
     * <p>
     * If approved by one of the users in
     * the circle with rights to do so (via joinCircleConfirm) the user will be
     * added.  To the circle's membership.  The caller of joinCircleConfirm
     * assigns permissions.
     *
     * @param uid the user to add
     * @param circleid the circle to join
     * @param urlPrefix used to customise the notification
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean joinCircle(String uid, String circleid, String urlPrefix) 
	throws DeterFault {
	SharedConnection sc = null;
	UserDB user = null;
	CircleDB circle = null;
	CircleChallengeDB chall = null;

	log.info("joinCircle for " + uid + " " + circleid );
	try {
	    if (circleid == null || uid == null) 
		throw new DeterFault(DeterFault.request, 
			"Uid and circleid required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make membership changes to project");

	    if ( circle.isMember(uid)) 
		throw new DeterFault(DeterFault.request,
			"Uid " + uid + " already a member of " + circleid);


	    checkAccess("user_" + uid + "_joinCircle", 
		    new CredentialSet("user", uid), sc);
	    chall = new CircleChallengeDB(sc);
	    user = new UserDB(uid, sc);
	    user.load();
	    long id = chall.addChallenge(uid, circleid,
		    new ArrayList<String>());
	    notifyUsersOfJoin(id, 
		    getUsersWithAccess(
			"circle_" + circleid + "_joinCircleConfirm",
			new CredentialSet("circle", circleid), sc),
		    uid, circleid, urlPrefix, sc);
	    user.close();
	    circle.close();
	    chall.close();
	    sc.close();
	    log.info("joinCircle for " + uid + "  " + circleid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("joinCircle for " + uid + " " + circleid + 
		    " failed: " + df);
	    if ( user != null) user.forceClose();
	    if ( circle != null) circle.forceClose();
	    if ( chall != null) chall.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Do the actual addition of a user to a circle started by joinCircle. This
     * operation is called by a member of the circle in question with
     * appropriate permessions (ADD_USER).  This is generally in response to a
     * notification generated by joinCircle.
     * <p>
     * The caller of this routine assigns the permissions of the joining user.
     *
     * @param challengeId the challenge being responded to
     * @param perms the permissions to assign the new member
     * @return true if addition succeeds
     * @throws DeterFault if there is an error.
     */
    public boolean joinCircleConfirm(long challengeId, String[] perms) 
	throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;
	CircleChallengeDB chall = null;

	log.info("joinCircleConfirm for " + challengeId );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    chall = new CircleChallengeDB(sc);
	    CircleChallengeDB.Contents contents = 
		chall.getChallenge(challengeId);

	    if (contents == null ) 
		throw new DeterFault(DeterFault.request, 
			"No such challenge outstanding");

	    String circleid = contents.getCircleId();
	    circle = new CircleDB(circleid, sc);
	    Set<String> vPerms = circle.validatePerms(Arrays.asList(perms));

	    checkAccess("circle_" + circleid + "_joinCircleConfirm", 
		    new CredentialSet("circle", circleid), sc);

	    circle.addUser(contents.getUid(), vPerms);
	    circle.close();
	    // Remove other outstanding challenges for this user, if any
	    chall.clearChallenges(contents.getUid(), circleid);
	    chall.close();
	    sc.close();
	    log.info("joinCircleConfirm for " + challengeId +  " " 
		    + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.info("joinCircleConfirm for " + challengeId + 
		    " failed: " + df);
	    if ( circle != null ) circle.forceClose();
	    if ( chall != null ) chall.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Complete the process of adding a user to a circle started by addUsers.
     * This operation is called by the user who was invited to join the circle,
     * in response to a notification.
     * <p>
     * Note that the permissions for the new member were assigned when the
     * challenge was issued.
     *
     * @param challengeId the challenge being responded to
     * @return true if addition succeeds
     * @throws DeterFault if there is an error.
     */
    public boolean addUserConfirm(long challengeId) throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;
	CircleChallengeDB chall = null;

	log.info("addUserConfirm for " + challengeId );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    chall = new CircleChallengeDB(sc);
	    CircleChallengeDB.Contents contents = 
		chall.getChallenge(challengeId);

	    if (contents == null ) 
		throw new DeterFault(DeterFault.request, 
			"No such challenge outstanding");

	    String circleid = contents.getCircleId();

	    checkAccess("circle_" + circleid + "_addUserConfirm", 
		    new CredentialSet("circle", circleid), sc);

	    circle = new CircleDB(circleid, sc);
	    circle.addUser(contents.getUid(),
		    new HashSet<String>(contents.getPermissions()));
	    circle.close();
	    // Remove other outstanding challenges for this user, if any
	    chall.clearChallenges(contents.getUid(), circleid);
	    chall.close();
	    sc.close();
	    log.info("addUserConfirm for " + challengeId + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.info("addUserConfirm for " + challengeId + " failed: " + df);
	    if (circle != null) circle.forceClose();
	    if (chall != null) chall.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Add the users with the given uids to this circle.  Permissions are
     * specified by the caller of this operation.  The users added are not
     * required to confirm this.
     *<p>
     * This is an administrative operation.
     *
     * @param circleid the circle to modify
     * @param uids the users to add
     * @param perms the rights each user will be granted
     * @return an array of the users added
     * @throws DeterFault if something worse than a user add failing happens
     */
    public ChangeResult[] addUsersNoConfirm(String circleid, String[] uids,
	    String[] perms) 
	throws DeterFault {

	SharedConnection sc = null;
	CircleDB circle = null;
	ArrayList<ChangeResult> added = new ArrayList<ChangeResult>();

	log.info("addUsersNoConfirm for " + circleid );
	try {
	    if (circleid == null ) 
		throw new DeterFault(DeterFault.request, "Circleid required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);

	    circle.validatePerms(Arrays.asList(perms));
	    if (!circle.isValid()) 
		throw new DeterFault(DeterFault.request, 
			"No such circle " + circleid);

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make membership changes to project");

	    if (uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "No uids specified");

	    checkAccess("circle_" + circleid + "_addUsersNoConfirm", 
		    new CredentialSet("circle", circleid), sc);

	    Set<String> vPerms = circle.validatePerms(Arrays.asList(perms));
	    for (String uid : uids ) {
		try {
		    circle.addUser(uid, vPerms);
		    log.info("addUsersNoConfirm for " + circleid + 
			    " added " + uid);
		    added.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("addUsersNoConfirm failed for " + circleid + " " +
			    uid + ": " + df);
		    added.add(new ChangeResult(uid, df.getDetailMessage(),
				false));
		}
	    }
	    circle.close();
	    sc.close();
	    log.info("addUsersNoConfirm for " + circleid + " succeeded");
	    return added.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addUsersNoConfirm for " + circleid + " failed: " + df);
	    if (circle != null ) circle.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change the permissions of the users with the given uids to this circle.
     * Each user in the uids parameter has their permissions reset to the
     * permissions in perms.  Return values are generated for each attempted
     * change.  The caller must have both ADD_USER and REMOVE_USER permissions
     * to the circle.
     * @param circleid the circle to modify
     * @param uids the users to modify
     * @param perms the rights each user will be granted
     * @return an array of the change results
     * @throws DeterFault if the command as a whole failed
     */
    public ChangeResult[] changePermissions(String circleid, String[] uids,
	    String[] perms) 
	throws DeterFault {

	SharedConnection sc = null;
	CircleDB circle = null;
	ArrayList<ChangeResult> changed = new ArrayList<ChangeResult>();

	log.info("changePermissions for " + circleid );
	try {
	    if (circleid == null ) 
		throw new DeterFault(DeterFault.request, "Circleid required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);
	    circle.validatePerms(Arrays.asList(perms));

	    if ( !circle.isValid()) 
		throw new DeterFault(DeterFault.request, 
			"No such circle: " + circleid);

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make permission changes to project");

	    if ( uids == null || uids.length == 0 )
		throw new DeterFault(DeterFault.request, "Uids missing");

	    checkAccess("circle_" + circleid + "_changePermissions", 
		    new CredentialSet("circle", circleid), sc);

	    Set<String> vPerms = new HashSet<String>(Arrays.asList(perms));
	    for (String uid : uids ) {
		try {
		    circle.setPermissions(uid, vPerms);
		    log.info("changePermissions for " + circleid + 
			    " changed " + uid);
		    changed.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("changePermissions failed for " + circleid + " " +
			    uid + ": " + df);
		    changed.add(new ChangeResult(uid,
				df.getDetailMessage(), false));
		}
	    }
	    circle.close();
	    sc.close();
	    log.info("changePermissions for " + circleid + " succeeded");
	    return changed.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changePermissions for " + circleid + " failed: " + df);
	    if (circle != null ) circle.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove the given users.  The caller must have the REMOVE_USER permission
     * for the circle.  An array of results is returned as
     * <a href="ChangeResult.html">ChangeResult</a>s.
     *
     * @param circleid the ID of the cricle to modify
     * @param uids the users to remove
     * @return an array showing the result of each removal
     * @throws DeterFault if the command as a whole fails
     * @see ChangeResult
     */
    public ChangeResult[] removeUsers(String circleid, String uids[])
	throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;
	ArrayList<ChangeResult> removed = new ArrayList<ChangeResult>();

	log.info("removeUsers for " + circleid );
	try {
	    if (circleid == null ) 
		throw new DeterFault(DeterFault.request, "Circleid required");

	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make membership changes to project");

	    if ( !circle.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid circleid");

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Missing uids");


	    checkAccess("circle_" + circleid + "_removeUsers", 
		    new CredentialSet("circle", circleid), sc);

	    for (String uid : uids ) {
		try {
		    circle.removeUser(uid);
		    log.info("removeUsers for " + circleid + 
			    " removed " + uid);
		    removed.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("removeUsers failed for " + circleid + " " +
			    uid + ": " + df);
		    removed.add(new ChangeResult(uid,
				df.getDetailMessage(), false));
		}
	    }
	    circle.close();
	    sc.close();
	    log.info("removeUsers for " + circleid + " succeeded");
	    return removed.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("removeUsers for " + circleid + " failed: " + df);
	    if (circle != null ) circle.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Return descriptions of the circles in which the uid is a member. If
     * regex is given, only circles with IDs matching it are returned.
     * <p>
     * Contents of the description are defined by the
     * <a href="CircleDescription.html">CircleDescription</a> class.
     *
     * @param uid the user to request
     * @param regex the matching regex (optional)
     * @return descriptions of the circles
     * @throws DeterFault on errors
     * @see CircleDescription
     */
    public CircleDescription[] viewCircles(String uid, String regex) 
	throws DeterFault {
	SharedConnection sc = null;
	List<CircleDB> circles = null;
	String logUid = (uid != null ) ? uid : "No UID";

	log.info("viewCircles for " + logUid + " " + regex );
	try {
	    Config config = new Config();
	    String worldName = config.getWorldCircle();

	    sc = new SharedConnection();
	    sc.open();

	    if (uid == null )
		checkAccess("user_admin:admin_viewCircles",
			new CredentialSet("system", null), sc);
	    else
		checkAccess("user_" + uid + "_viewCircles",
			new CredentialSet("user", uid), sc);

	    List<CircleDescription> rv = new ArrayList<CircleDescription>();
	    circles = CircleDB.getCircles(uid, regex, sc);

	    for (CircleDB c : circles ){
		String name = c.getName();

		if ( worldName != null && worldName.equals(name)) {
		    c.close();
		    continue;
		}
		CircleDescription cd = new CircleDescription(c.getName(), 
			c.getOwner());
		cd.setMembers(c.getMembers());
		rv.add(cd);
		c.close();
	    }
	    circles = null;
	    sc.close();
	    log.info("viewCircles for " + logUid + " " + regex + " succeeded");
	    return rv.toArray(new CircleDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewCircles for " + logUid + " " + regex +
		    " failed: " + df);
	    if ( circles != null)
		for ( CircleDB c : circles)
		    if ( c != null ) c.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Set the owner of this circle.  Only owners and admins can do this.
     * @param circleid the circle to modify
     * @param uid the new owner
     * @return true if the change worked
     * @throws DeterFault on errors
     */

    public boolean setOwner(String circleid, String uid) throws DeterFault {
	SharedConnection sc = null;
	CircleDB circle = null;

	log.info("setOwner for " + circleid + " " + uid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    circle = new CircleDB(circleid, sc);

	    if ( circle.isLinked()) 
		throw new DeterFault(DeterFault.request, 
			"Linked circle: make ownership changes to project");

	    if ( !circle.isValid()) 
		throw new DeterFault(DeterFault.request, "invalid circleid");

	    checkAccess("circle_" + circleid + "_setOwner", 
		    new CredentialSet("circle", circleid), sc);

	    circle.setOwner(uid);
	    circle.close();
	    sc.close();
	    log.info("setOwner for " + circleid + " " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("setOwner for " + circleid + " " + uid + 
		    " failed: " + df);
	    if (circle != null ) circle.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
