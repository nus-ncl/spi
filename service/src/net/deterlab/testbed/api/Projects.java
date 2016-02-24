package net.deterlab.testbed.api;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.project.ProjectChallengeDB;
import net.deterlab.testbed.project.ProjectDB;
import net.deterlab.testbed.project.ProjectProfileDB;
import net.deterlab.testbed.user.NotificationStoreDB;
import net.deterlab.testbed.user.UserDB;
/**
 * This service manages projects - the user groupings used to control access to
 * DETER as a whole.  USers cannot access testbed resources unless they are
 * members of an approved project.  Applications can use this to create,
 * remove, and modify projects as well as manipulating their profiles.  For
 * each user in a project, that user's rights to manipulate the project can be
 * set and changed.  Project ownership can also be changed by the owner.
 * <p>
 * Projects are not very useful until they are approved by an administrator via
 * this interface.
 * <p>
 * Adding users to a project is a two step process that can be initiated either
 * by a user who wants to join an existing project or by a project member who
 * is inviting a user into the project.  In both cases a member of the project
 * with permission to add users and the user being added must concur.
 * <p>
 * The sequence by which a user joins a project is by that user calling
 * joinProject and a user with appropriate permissions calling
 * joinProjectConfirm.  The sequence where an member invites a new user in is
 * addUsers followed by each invited user calling addUserConfirm.  (There is
 * also an addUserNoConfirm operation, but only administrators can do that).
 *
 * @author ISI DETER team
 * @version 1.0
 */
public class Projects extends ProfileService {
    /** Logger for each instance */
    private Logger log;
    /**
     * Construct a Projects service.
     */
    public Projects() { 
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
     * Add a new project attribute to the profile schema.  This operation is an
     * administrative action that modifies the profiles of all projects to
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
    public boolean createProjectAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def) 
	throws DeterFault {
	ProjectProfileDB profile = null;

	try{ 
	    boolean rv = createAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    def, profile = new ProjectProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if (profile != null) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing project attribute.  This operation is
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
    public boolean modifyProjectAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length) 
	throws DeterFault {
	ProjectProfileDB profile = null;
	try {
	    boolean rv = modifyAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    profile = new ProjectProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a project attribute from the schema of all project profiles.  Any
     * values are also deleted.This operation is administrative.
     * @param name the attribute to remove
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean removeProjectAttribute(String name) throws DeterFault {
	ProjectProfileDB profile = null;
	try {
	    boolean rv = removeAttribute(name,
		    profile = new ProjectProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Send a notification to the given members letting them know of the
     * that a new project exists and cam be approved.
     * @param u the users to notify
     * @param uid the owner of the new project
     * @param projectid the project to join
     * @param sc a shared DB connection
     * @throws DeterFault on error
     */
    protected void notifyUsersOfCreate(List<String> u, String uid,
	    String projectid, SharedConnection sc)
	throws DeterFault {
	NotificationStoreDB notes = null;

	try {
	    StringWriter bodyString = new StringWriter();
	    PrintWriter body = new PrintWriter(bodyString);
	    long nid = -1;

	    notes = new NotificationStoreDB(sc);

	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println(uid + " has created a new project " +
		    projectid + ".");
	    body.println();
	    body.println("That project will not be active until someone " +
		    "approves it. You have the authority to do so.");
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
     * Return the strings that encode valid permissions for projects.  Note
     * that all circle permissions are valid for calls to createProject and
     * changePermissions and indicate operations on the linked circle.  Those
     * permissions are available from the <a href="Circles.html">Circles</a>
     * service.  They are never returned by viewProjects.
     * @return the strings that encode valid permissions for projects.
     * @throws DeterFault on system errors
     * @see Circles
     */
    public String[] getValidPermissions() throws DeterFault {
	ProjectDB pdb = null;

	log.info("getValidPermissions");
	try {
	    pdb = new ProjectDB(null);
	    Set<String> perms = pdb.getValidPerms();
	    log.info("getValidPermissions succeeded");
	    return perms.toArray(new String[0]);
	}
	catch (DeterFault df) {
	    if  (pdb != null ) pdb.forceClose();
	    log.info("getValidPermissions failed: " + df);
	    throw df;
	}
    }


    /**
     * Create a new project with the given profile, owned by uid.    This
     * creates a new linked circle as well.  The resulting project is not
     * approved.
     * <p>
     * This operation is also used to set initial permissions to the circle
     * linked to this project.  As such, all valid circle permissions are valid
     * to pass to this call, and will be applied to the circle.  The
     * permissions that are valid for both circles and projects (e.g.,
     * ADD_USER, REMOVE_USER) will be applied to both the circle and project.
     * <p>
     * The permissions attached to the linked circle must be queried through
     * the <a href="Circles.html">Circles</a> service.
     *
     * @param projectid the new project
     * @param owner the new project owner
     * @param profile the filled in project profile
     * @return true on success
     * @throws DeterFault on access control or database errors
     * @see Circles
     */
    public boolean createProject(String projectid, String owner, 
	    Attribute[] profile) throws DeterFault {

	SharedConnection sc = null;
	ProjectDB project = null;
	ProjectProfileDB newProjectProfile = null;
	log.info("createProject for " + projectid);

	try {
	    if ( projectid == null) 
		throw new DeterFault(DeterFault.request, 
			"projectid is required");

	    if ( owner == null) 
		throw new DeterFault(DeterFault.request, 
			"owner is required");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + owner + "_createProject", 
		    new CredentialSet("user", owner), sc);

	    project = new ProjectDB(projectid, sc);

	    newProjectProfile = new ProjectProfileDB(sc);
	    ProjectProfileDB empty = null;
	    try {
		empty = new ProjectProfileDB(sc);
		checkProfile(profile, newProjectProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if (empty !=  null) empty.forceClose();
		throw df;
	    }

	    // Create the project and the profile.  Note that create catches
	    // conflicts with other project and user names.
	    project.create(owner);
	    newProjectProfile.setId(project.getName());
	    newProjectProfile.saveAll();
	    newProjectProfile.close();
	    project.close();
	    notifyUsersOfCreate(
		    getUsersWithAccess(
			"project_" + projectid + "_approveProject",
		    new CredentialSet("project", projectid), sc),
		    owner, projectid, sc);
	    sc.close();
	    log.info("createProject succeeded for " + projectid);
	    return true;
	}
	catch (DeterFault df) {
	    log.error("createProject failed: " + df);
	    if ( project != null ) project.forceClose();
	    if ( newProjectProfile != null) newProjectProfile.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Remove the project from the system completely.  This removes any
     * outstanding challenges, the entire profile, the linked circle's
     * challenges, profile and the project and linked circle.  Only the
     * project's owner or an administrator can do this.
     *
     * @param projectid the project to remove
     * @return true on success
     * @throws DeterFault on access control or database errors
     */
    public boolean removeProject(String projectid) throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ProjectChallengeDB challenges = null;

	log.info("removeProject for " + projectid);
	try {
	    if ( projectid == null) 
		throw new DeterFault(DeterFault.request, 
			"projectid is required");

	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    checkAccess("project_" + projectid + "_removeProject", 
		    new CredentialSet("project", projectid), sc);

	    challenges = new ProjectChallengeDB(sc);

	    challenges.clearChallenges(projectid);
	    challenges.close();
	    ProjectProfileDB profile = null;
	    try {
		profile = new ProjectProfileDB(projectid, sc);
		profile.loadAll();
		profile.removeAll();
		profile.close();
	    }
	    catch (DeterFault df) {
		log.warn("Could not remove profile: " + df.getDetailMessage());
		if (profile != null) profile.forceClose();
	    }
	    project.remove();
	    project.close();
	    sc.close();
	    log.info("removeProject succeeded for " + projectid);
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeProject for " + projectid + " failed: " + df);
	    if ( project != null ) project.forceClose();
	    if ( challenges != null ) challenges.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Return an empty project profile  - the profile schema (the ID field of
     * the profile returned will be empty).  Applications will call this to get
     * the names, formats, and other requirements of the project schema.  The
     * caller does not to be logged in or a valid user to successfully call
     * this operation.
     * @return an empty project profile
     * @throws DeterFault on error
     */
    public Profile getProfileDescription() throws DeterFault {
	ProjectProfileDB cp = null;

	try {
	    Profile rv = getProfileDescription(
		    cp = new ProjectProfileDB());
	    cp.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if (cp != null ) cp.forceClose();
	    throw df;
	}
    }
    /**
     * Return the completed profile associated with projectid.  The schema
     * information is all returned as well as the values for populated fields
     * in this project's profile.  Any member of the project can retrieve the
     * profile.
     * @param projectid the project whose profile is being retrieved.
     * @return a completed project profile for project
     * @throws DeterFault on perimission errors or no such user.
     */
    public Profile getProjectProfile(String projectid) throws DeterFault {
	ProjectProfileDB cp = null;

	try {
	    Profile rv = getProfile(projectid,
		    cp = new ProjectProfileDB());
	    cp.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( cp != null ) cp.forceClose();
	    throw df;
	}
    }

    /**
     * Process a list of change requests for attributes in projectid's profile.
     * Each request is encoded in a
     * <a href="ChangeAttribute.html">ChangeAttribute</a>.
     * For each request, a <a href="ChangeResult.html">ChangeResult</a>
     * is returned, either indicating that the change has gone through or
     * that it failed.  Failed changes are annotated with a reason.
     * @param projectid the project whose profile is being modified (user:name)
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     * @see ChangeAttribute
     * @see ChangeResult
     */
    public ChangeResult[] changeProjectProfile(String projectid,
	    ChangeAttribute[] changes) throws DeterFault {
	ProjectProfileDB all = null;
	ProjectProfileDB cp = null;
	try {
	    cp = new ProjectProfileDB();
	    all = new ProjectProfileDB(cp.getSharedConnection());
	    ChangeResult[] rv = changeProfile(projectid,
		    cp, all, changes);
	    all.close();
	    cp.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( cp != null) cp.forceClose();
	    if ( all != null) all.forceClose();
	    throw df;
	}
    }

    /**
     * Compose a notification to the given user letting them know they have
     * been added (provisionally) to projectid, including a url to confirm that
     * addition.
     * @param id the challenge identifier
     * @param uid the user to notify
     * @param projectid the destination project
     * @param urlPrefix the UI customization
     * @param sc a shared DB connection
     * @throws DeterFault if there is a problem
     */
    protected void notifyUserOfAdd(long id, String uid, String projectid, 
	    String urlPrefix, SharedConnection sc) throws DeterFault {
	NotificationStoreDB notes = null;

	try {
	    StringWriter bodyString = new StringWriter();
	    PrintWriter body = new PrintWriter(bodyString);
	    long nid = -1;
	    notes = new NotificationStoreDB(sc);


	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println("Someone has provisionally added you to project " +
		    projectid + ".");
	    body.println();
	    if ( urlPrefix != null ) {
		body.println(
			"That change will not take effect until you go to " +
			urlPrefix + id);
		body.println(" or respond to the project challenge " + id +
			" some other way");
	    }
	    else {
		body.println("That change will not take effect until you " +
			"respond to the project challenge " + id );
	    }
	    body.println();
	    body.close();

	    nid = notes.create(bodyString.toString());
	    notes.deliver(Arrays.asList(new String[] { uid }),  0, nid);
	    notes.close();
	}
	catch (DeterFault df) {
	    if (notes != null) notes.forceClose();
	    throw df;
	}
    }

    /**
     * Add the users with the given uids to this project. This will issue
     * challenges to each user that can be added. Each challenge is delivered
     * via a testbed notification. When each user responds to their challenge
     * that user will actually be added.  This prevents a malicious project
     * owner from adding users to a project for the purpose of
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
     * @param projectid the project to modify
     * @param uids the users to add
     * @param perms the rights each user will be granted
     * @param urlPrefix a prefix added to the challenge identifier to integrate
     * it with a web interface.
     * @return an array of the users added
     * @throws DeterFault if something worse than a user add failing happens
     */
    public ChangeResult[] addUsers(String projectid, String[] uids,
	    String[] perms, String urlPrefix) 
	throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ProjectChallengeDB chall = null;
	ArrayList<ChangeResult> added = new ArrayList<ChangeResult>();

	log.info("addUsers for " + projectid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    Set<String> vPerms = project.validatePerms(Arrays.asList(perms));
	    if (projectid == null )
		throw new DeterFault(DeterFault.request, "Projectid required");

	    project.setName(projectid);
	    chall = new ProjectChallengeDB(sc);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request, 
			"Project is not yet approved");

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Missing uids");

	    checkAccess("project_" + projectid + "_addUsers", 
		    new CredentialSet("project", projectid), sc);

	    for (String uid : uids ) {
		UserDB user = null;
		try {
		    user = new UserDB(uid, sc);

		    if ( project.isMember(uid)) 
			throw new DeterFault(DeterFault.request,
				"Uid " + uid + " already a member of " + 
				projectid);

		    // Confirm user is valid
		    user.load();
		    long id = chall.addChallenge(uid, projectid, vPerms);
		    notifyUserOfAdd(id, uid, projectid, urlPrefix, sc);
		    log.info("addUsers " + projectid + 
			    " issued challenge for " + uid);
		    user.close();
		    added.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    if ( user != null ) user.forceClose();
		    log.info("addUsers failed for " + projectid + " " +
			    uid + ": " + df);
		    added.add(new ChangeResult(uid, df.getDetailMessage(),
				false));
		}
	    }
	    project.close();
	    chall.close();
	    sc.close();
	    log.info("addUsers for " + projectid + " succeeded");
	    return added.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addUsers for " + projectid + " failed: " + df);
	    if (project != null ) project.forceClose();
	    if (chall != null ) chall.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Send a notification to the given members letting them know of the
     * presence of a join request.
     * @param id the challenge id
     * @param u the members of the project to send to
     * @param uid the user making the request
     * @param projectid the project to join
     * @param urlPrefix to customize the notification
     * @param sc a shared DB connection
     * @throws DeterFault on error
     */
    protected void notifyUsersOfJoin(long id, List<String> u, String uid,
	    String projectid, String urlPrefix, SharedConnection sc)
	throws DeterFault {
	NotificationStoreDB notes = null;

	try {
	    StringWriter bodyString = new StringWriter();
	    PrintWriter body = new PrintWriter(bodyString);
	    long nid = -1;

	    notes = new NotificationStoreDB(sc);

	    body = new PrintWriter(bodyString = new StringWriter());

	    body.println(uid + " has asked to join your project " +
		    projectid + ".");
	    body.println();
	    if ( urlPrefix != null ) {
		body.println(
			"That change will not take effect until you go to " +
			urlPrefix + id);
		body.println(" or respond to the project challenge " + id +
			" some other way");
	    }
	    else {
		body.println("That change will not take effect until you " +
			"respond to the project challenge " + id );
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
     * Request permission to join a project.  This is called by a user who has
     * heard of a project through out of band means and wants to join.
     * <p>
     * If approved by one of the users in the project with rights to do so (via
     * joinProjectConfirm) the user will be added.  To the project's membership.
     * The caller of joinProjectConfirm assigns permissions.
     *
     * @param uid the user to add
     * @param projectid the project to join
     * @param urlPrefix used to customise the notification
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean joinProject(String uid, String projectid, String urlPrefix) 
	throws DeterFault {
	SharedConnection sc = null;
	UserDB user = null;
	ProjectDB project = null;
	ProjectChallengeDB chall = null;

	log.info("joinProject for " + uid + " " + projectid );
	try {
	    if (projectid == null || uid == null)
		throw new DeterFault(DeterFault.request, 
			"Uid and projectid required");

	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    checkAccess("user_" + uid + "_joinProject",
		    new CredentialSet("user", uid), sc);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request, 
			"Project is not yet approved");

	    if ( project.isMember(uid)) 
		throw new DeterFault(DeterFault.request,
			"Uid " + uid + " already a member of " + projectid);

	    chall = new ProjectChallengeDB(sc);
	    user = new UserDB(uid, sc);
	    user.load();
	    long id = chall.addChallenge(uid, projectid, new HashSet<String>());
	    notifyUsersOfJoin(id, 
		    getUsersWithAccess(
			"project_" + projectid + "_joinProjectConfirm",
			new CredentialSet("project", projectid), sc),
		    uid, projectid, urlPrefix, sc);
	    log.info("joinProject for " + uid + "  " + projectid +
		    " succeeded");
	    user.close();
	    project.close();
	    chall.close();
	    sc.close();
	    return true;
	}
	catch (DeterFault df) {
	    log.error("joinProject for " + uid + " " + projectid + 
		    " failed: " + df);
	    if (user != null ) user.forceClose();
	    if (project != null ) project.forceClose();
	    if (chall != null ) chall.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Do the actual addition of a user to a project started by joinCircle. This
     * operation is called by a member of the project in question with
     * appropriate permessions (ADD_USER).  This is generally in response to a
     * notification generated by joinProject.
     * <p>
     * The caller of this routine assigns the permissions of the joining user.
     *
     * @param challengeId the challenge being responded to
     * @param perms the permissions to assign the new member
     * @return true if addition succeeds
     * @throws DeterFault if there is an error.
     */
    public boolean joinProjectConfirm(long challengeId, String[] perms) 
	throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ProjectChallengeDB chall = null;

	log.info("joinProjectConfirm for " + challengeId );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(null, sc);
	    Set<String> vPerms = project.validatePerms(Arrays.asList(perms));

	    chall = new ProjectChallengeDB(sc);
	    ProjectChallengeDB.Contents contents = 
		chall.getChallenge(challengeId);

	    if (contents == null ) 
		throw new DeterFault(DeterFault.request, 
			"No such challenge outstanding");

	    String projectid = contents.getProjectId();

	    checkAccess("project_" + projectid + "_joinProjectConfirm", 
		    new CredentialSet("project", projectid), sc);

	    project.setName(projectid);
	    project.addUser(contents.getUid(), vPerms);
	    // Remove other outstanding challenges for this user, if any
	    chall.clearChallenges(contents.getUid(), contents.getProjectId());
	    chall.close();
	    project.close();
	    sc.close();
	    log.info("joinProjectConfirm for " + challengeId +  " " 
		    + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.info("joinProjectConfirm for " + challengeId + 
		    " failed: " + df);
	    if ( project != null) project.forceClose();
	    if ( chall != null) chall.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Complete the process of adding a user to a project started by addUsers.
     * This operation is called by the user who was invited to join the project,
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
	ProjectDB project = null;
	ProjectChallengeDB chall = null;

	log.info("addUserConfirm for " + challengeId );
	try {

	    sc = new SharedConnection();
	    sc.open();

	    chall = new ProjectChallengeDB(sc);
	    ProjectChallengeDB.Contents contents = 
		chall.getChallenge(challengeId);

	    if (contents == null ) 
		throw new DeterFault(DeterFault.request, 
			"No such challenge outstanding");

	    String projectid = contents.getProjectId();


	    checkAccess("project_" + projectid + "_addUserConfirm", 
		    new CredentialSet("project", projectid), sc);

	    project = new ProjectDB(projectid, sc);
	    project.addUser(contents.getUid(), contents.getPermissions());
	    // Remove other outstanding challenges for this user, if any
	    chall.clearChallenges(contents.getUid(), contents.getProjectId());
	    chall.close();
	    project.close();
	    sc.close();
	    log.info("addUserConfirm for " + challengeId + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.info("addUserConfirm for " + challengeId + " failed: " + df);
	    if ( project != null) project.forceClose();
	    if ( chall != null) chall.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Add the users with the given uids to this project.  Permissions are
     * specified by the caller of this operation.  The users added are not
     * required to confirm this.
     *<p>
     * This is an administrative operation.
     *
     * @param projectid the project to modify
     * @param uids the users to add
     * @param perms the rights each user will be granted
     * @return an array of the users added
     * @throws DeterFault if something worse than a user add failing happens
     */
    public ChangeResult[] addUsersNoConfirm(String projectid,
	    String[] uids, String[] perms) 
	throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ArrayList<ChangeResult> added = new ArrayList<ChangeResult>();

	log.info("addUsersNoConfirm for " + projectid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(null, sc);
	    Set<String> vPerms = project.validatePerms(Arrays.asList(perms));

	    if (projectid == null ) 
		throw new DeterFault(DeterFault.request, "Projectid required");

	    project.setName(projectid);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request, 
			"Project is not yet approved");

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Missing uids");

	    checkAccess("project_" + projectid + "_addUsersNoConfirm", 
		    new CredentialSet("project", projectid), sc);

	    for (String uid : uids ) {
		try {
		    project.addUser(uid, vPerms);
		    log.info("addUsersNoConfirm for " + projectid + 
			    " added " + uid);
		    added.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("addUsersNoConfirm failed for " + projectid + " " +
			    uid + ": " + df);
		    added.add(new ChangeResult(uid, df.getDetailMessage(),
				false));
		}
	    }
	    project.close();
	    sc.close();
	    log.info("addUsersNoConfirm for " + projectid + " succeeded");
	    return added.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addUsersNoConfirm for " + projectid + " failed: " + df);
	    if ( project != null ) project.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change the permissions of the users with the given uids to this project.
     * Each user in the uids parameter has their permissions reset to the
     * permissions in perms.  Return values are generated for each attempted
     * change.  The caller must have both ADD_USER and REMOVE_USER permissions
     * to the project.
     * <p>
     * This operation is also used to change permissions to the circle linked
     * to this project.  As such, all valid circle permissions are valid to pass
     * to this call, and will be applied to the circle.  The permissions that
     * are valid for both circles and projects (e.g., ADD_USER, REMOVE_USER)
     * will be applied to both the circle and project.
     * <p>
     * The permissions attached to the linked circle must be queried through
     * the <a href="Circles.html">Circles</a> service.
     *
     * @param projectid the project to modify
     * @param uids the users to modify
     * @param perms the rights each user will be granted
     * @return an array of the users added
     * @throws DeterFault if something worse than a user modifciation failing
     * happens
     * @see Circles
     */
    public ChangeResult[] changePermissions(String projectid,
	    String[] uids, String[] perms) 
	throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ArrayList<ChangeResult> changed = new ArrayList<ChangeResult>();

	log.info("setPermissions for " + projectid );
	try {
	    if (projectid == null ) 
		throw new DeterFault(DeterFault.request, "Projectid required");

	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(null, sc);
	    Set<String> vPerms = project.validatePerms(Arrays.asList(perms));

	    project.setName(projectid);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request,
			"Project is not yet approved");

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Missing uids");

	    checkAccess("project_" + projectid + "_changePermissions", 
		    new CredentialSet("project", projectid), sc);

	    for (String uid : uids ) {
		try {
		    project.setPermissions(uid, vPerms);
		    log.info("setPermissions for " + projectid + 
			    " added " + uid);
		    changed.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("setPermissions failed for " + projectid + " " +
			    uid + ": " + df);
		    changed.add(new ChangeResult(uid,
				df.getDetailMessage(), false));
		}
	    }
	    project.close();
	    sc.close();
	    log.info("setPermissions for " + projectid + " succeeded");
	    return changed.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("setPermissions for " + projectid + " failed: " + df);
	    if ( project != null ) project.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove the given users.  The caller must have the REMOVE_USER permission
     * for the project.  An array of results is returned as
     * <a href="ChangeResult.html">ChangeResult</a>s.
     *
     * @param projectid The project to modify
     * @param uids user IDs to remove
     * @return an array showing the result of each removal
     * @throws DeterFault if something worse than a user removeal failing
     * happens
     * @see ChangeResult
     */
    public ChangeResult[] removeUsers(String projectid, String uids[])
	throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;
	ArrayList<ChangeResult> removed = new ArrayList<ChangeResult>();

	log.info("removeUsers for " + projectid );
	try {
	    if (projectid == null ) 
		throw new DeterFault(DeterFault.request, "Projectid required");

	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request, 
			"Project is not yet approved");

	    if ( uids == null || uids.length == 0) 
		throw new DeterFault(DeterFault.request, "Missing uids");

	    checkAccess("project_" + projectid + "_removeUsers", 
		    new CredentialSet("project", projectid), sc);

	    for (String uid : uids ) {
		try {
		    project.removeUser(uid);
		    log.info("removeUsers for " + projectid + 
			    " added " + uid);
		    removed.add(new ChangeResult(uid, null, true));
		}
		catch (DeterFault df) {
		    log.info("removeUsers failed for " + projectid + " " +
			    uid + ": " + df);
		    removed.add(new ChangeResult(uid, df.getDetailMessage(),
				false));
		}
	    }
	    project.close();
	    sc.close();
	    log.info("removeUsers for " + projectid + " succeeded");
	    return removed.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("removeUsers for " + projectid + " failed: " + df);
	    if (project != null ) project.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Return descriptions of the projects in which the uid is a member. If
     * regex is given, only projects with IDs matching it are returned.
     * <p>
     * Contents of the description are defined by the
     * <a href="ProjectDescription.html">ProjectDescription</a> class.
     *
     * @param uid the user to request
     * @param regex the matching regex (optional)
     * @return descriptions of the projects
     * @throws DeterFault if something goes wrong
     * @see ProjectDescription
     */
    public ProjectDescription[] viewProjects(String uid, String regex) 
	throws DeterFault {
	SharedConnection sc = null;
	Collection<ProjectDB> projects = null;
	String logUid = (uid != null) ? uid : "No UID";

	log.info("viewProjects for " + logUid + " " + regex );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    if (uid == null )
		checkAccess("user_admin:admin_viewProjects",
			new CredentialSet("system", null), sc);
	    else
		checkAccess("user_" + uid + "_viewProjects",
			new CredentialSet("user", uid), sc);
	    List<ProjectDescription> rv = new ArrayList<ProjectDescription>();
	    projects = ProjectDB.getProjects(uid, regex, sc);

	    for (ProjectDB p : projects ){
		ProjectDescription pd = new ProjectDescription(p.getName(), 
			p.getOwner(), p.isApproved());
		pd.setMembers(p.getMembers());
		rv.add(pd);
		p.close();
	    }
	    projects = null;
	    sc.close();
	    log.info("viewProjects for " + logUid + " " + regex + " succeeded");
	    return rv.toArray(new ProjectDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewProjects for " + logUid + " " + regex +
		    " failed: " + df);
	    if ( projects != null)
		for ( ProjectDB p : projects)
		    if (p != null) p.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Set the owner of this project.  Only owners and admins can do this.
     * @param projectid the project to modify
     * @param uid the new owner
     * @return true if the changed worked
     * @throws DeterFault if something goes wrong
     */

    public boolean setOwner(String projectid, String uid) throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;

	log.info("setOwner for " + projectid + " " + uid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    if ( !project.isValid()) 
		throw new DeterFault(DeterFault.request, "Invalid projectid");


	    if (!project.isApproved()) 
		throw new DeterFault(DeterFault.request, 
			"Project is not yet approved");

	    checkAccess("project_" + projectid + "_setOwner", 
		    new CredentialSet("project", projectid), sc);

	    project.setOwner(uid);
	    project.close();
	    sc.close();
	    log.info("setOwner for " + projectid + " " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("setOwner for " + projectid + " " + uid + 
		    " failed: " + df);
	    if ( project != null ) project.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Approve or disapprove project.  The project remains in the system either
     * way.  RemoveProject will remove it completely.  This is an
     * administrative operation.
     * @param projectid the project to approve/disapprove
     * @param approved true if project should be made live.
     * @return true on success
     * @throws DeterFault if there are errors
     */
    public boolean approveProject(String projectid, boolean approved) 
	    throws DeterFault {
	SharedConnection sc = null;
	ProjectDB project = null;

	log.info("setApproval for " + projectid + " " + approved );
	try {
	    if ( projectid == null ) 
		throw new DeterFault(DeterFault.request, "Missing projectid");


	    sc = new SharedConnection();
	    sc.open();

	    project = new ProjectDB(projectid, sc);

	    if ( !project.isValid())
		throw new DeterFault(DeterFault.request, "Invalid projectid");

	    if ( project.isApproved())
		throw new DeterFault(DeterFault.request,
			"Project already approved");

	    checkAccess("project_" + projectid + "_approveProject", 
		    new CredentialSet("project", projectid), sc);

	    project.setApproval(approved);
	    project.close();
	    sc.close();
	    log.info("setApproval for " + projectid + " " + approved + 
		    " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("setApproval for " + projectid + " " + approved + 
		    " failed: " + df);
	    if ( project != null) project.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }
}
