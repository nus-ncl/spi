package net.deterlab.testbed.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;

import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.experiment.ExperimentDB;
import net.deterlab.testbed.library.LibraryDB;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.policy.PolicyFile;
import net.deterlab.testbed.project.ProjectDB;
import net.deterlab.testbed.realization.RealizationDB;
import net.deterlab.testbed.resource.ResourceDB;
import net.deterlab.testbed.system.PermissionDB;
import net.deterlab.testbed.user.CryptPasswordHash;
import net.deterlab.testbed.user.PasswordHash;
import net.deterlab.testbed.user.UserDB;

/**
 * This service provides very basic service initialization and maintenance
 * features.  Most applications will never use it, and few users will either.
 * It allows one to initialize an empty testbed and reset the policy engine.
 * @author ISI DETER team
 * @version 1.0
 */
public class Admin extends DeterService {
    /** Local RNG */
    static Random rng = new Random();
    /** Characters in deterboss's generated password */
    static String pwdChars = "abcdefghijklmnopqrstuvwxyz" + 
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
	"01234567890" + 
	"!@$%^&*()_+";

    /** Admin log */
    private Logger log;
    /**
     * Construct an Admin service.
     */
    public Admin() { 
	super();
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
     * Insert the system policy.  This will need to be made less ad hoc.
     * @param sc a shared DB connection
     * @throws DeterFault if something is wrong internally
     */
    protected void updateSystemPolicy(SharedConnection sc) throws DeterFault {
	Config config = new Config();
	CredentialStoreDB cdb = null;
	String files = config.getProperty("systemPolicy");

	try {
	    cdb = new CredentialStoreDB(sc);
	    for ( String fn : files.split(",")) {
		PolicyFile systemPolicy = new PolicyFile(new File(fn));
		systemPolicy.updateCredentials(cdb, null, null, null, null);
	    }
	    cdb.close();
	}
	catch (DeterFault df) {
	    if ( cdb != null ) cdb.forceClose();
	    throw df;
	}
    }


    /**
     * Very simple class to hold name object pairs for creating permissions
     */
    static protected class PermissionDesc {
	/** Permission name */
	public String name;
	/** Permission object */
	public String obj;

	/**
	 * Build a PermissionDesc
	 * @param n the name
	 * @param o the object
	 */
	public PermissionDesc(String n, String o) {
	    name = n; obj = o;
	}
    }

    /**
     * Standard permissions to be installed by bootstrap
     */
    static protected PermissionDesc[] permDefs = {
	new PermissionDesc("ADD_USER", "circle"),
	new PermissionDesc("REMOVE_USER", "circle"),
	new PermissionDesc("REALIZE_EXPERIMENT", "circle"),
	new PermissionDesc("ADD_USER", "project"),
	new PermissionDesc("REMOVE_USER", "project"),
	new PermissionDesc("CREATE_CIRCLE", "project"),
	new PermissionDesc("CREATE_EXPERIMENT", "project"),
	new PermissionDesc("CREATE_LIBRARY", "project"),
	new PermissionDesc("CREATE_RESOURCE", "project"),
	new PermissionDesc("READ_EXPERIMENT", "experiment"),
	new PermissionDesc("MODIFY_EXPERIMENT", "experiment"),
	new PermissionDesc("MODIFY_EXPERIMENT_ACCESS", "experiment"),
	new PermissionDesc("READ_LIBRARY", "library"),
	new PermissionDesc("ADD_EXPERIMENT", "library"),
	new PermissionDesc("REMOVE_EXPERIMENT", "library"),
	new PermissionDesc("MODIFY_LIBRARY_ACCESS", "library"),
	new PermissionDesc("MODIFY_RESOURCE", "resource"),
	new PermissionDesc("MODIFY_RESOURCE_ACCESS", "resource"),
	new PermissionDesc("READ_REALIZATION", "realization"),
	new PermissionDesc("MODIFY_REALIZATION", "realization"),
	new PermissionDesc("MODIFY_REALIZATION_ACCESS", "realization"),
	new PermissionDesc("REMOVE_REALIZATION", "realization"),
    };

    /**
     * Bootstrap the system.  Create a single admin-enabled user and return the
     * userid and password.  This will only work if neither the admin user
     * (deterboss) nor the admin project (first member of SystemProjects in the
     * properties file or admin if no such entry or file exists) exists.  It
     * also creates and approves any other system projects and a world circle
     * if one is defined in the service properties file.
     * @return the user and password
     * @throws DeterFault on errors.  On a running system, this is the usual
     *	    case.
     */
    public BootstrapUser bootstrap() throws DeterFault {
	log.info("bootstrap");
	UserDB user = null;
	PermissionDB perm = null;
	ProjectDB p = null;
	CircleDB sysWorld = null;
	SharedConnection sc = null;
	try {
	    Config config = new Config();
	    String sp = config.getSystemProjects();
	    String[] systemProjects =
		(sp != null) ? sp.split(",") : new String[] { "admin" };
	    String worldName = config.getWorldCircle();
	    String uid = "deterboss";

	    // Split puts a single empty String into systemProjects if the
	    // string is empty.  Make it a zero length array instead.
	    if ( systemProjects.length == 1 && systemProjects[0].length() == 0)
		systemProjects = new String[] { "admin" };

	    sc = new SharedConnection();
	    sc.open();

	    // The first system project is special - if it exists, the
	    // bootstrap call fails.
	    p = new ProjectDB(systemProjects[0], sc);
	    if ( p.isValid())
		throw new DeterFault(DeterFault.access, "Access Denied");

	    perm = new PermissionDB(sc);
	    for (PermissionDesc pd: permDefs ) {
		perm.setName(pd.name);
		perm.setObject(pd.obj);
		perm.create();
	    }
	    perm.close();

	    user = new UserDB(uid, sc);
	    PasswordHash pwd = new CryptPasswordHash(null);
	    StringBuilder npw = new StringBuilder();
	    for (int i = 0; i <12; i++)
		npw.append(pwdChars.charAt(rng.nextInt(pwdChars.length()-1)));
	    String newPass = npw.toString();

	    pwd.hashAndSet(newPass);
	    user.setPasswordHash(pwd);
	    user.setExpiration(new Date(System.currentTimeMillis() + 
			365L * 24L * 3600L * 1000L));
	    user.create();
	    for (String pname: systemProjects) {
		p = new ProjectDB(pname, sc);
		if ( !p.isValid()) {
		    p.create(uid);
		    p.setApproval(true);
		}
		p.close();
		p = null;
	    }

	    if ( worldName != null)
		sysWorld = new CircleDB(worldName, sc);

	    if ( sysWorld != null) {
		if (!sysWorld.isValid())
		    sysWorld.create(uid);
		sysWorld.close();
		sysWorld = null;
	    }
	    // Set system policy
	    updateSystemPolicy(sc);
	    user.close();
	    sc.close();
	    log.info("bootstrap succeeded");
	    return new BootstrapUser("deterboss", newPass);
	}
	catch (DeterFault df) {
	    df.printStackTrace();
	    log.error("bootstrap failed: " + df);
	    if (sysWorld != null) sysWorld.forceClose();
	    if (user != null) user.forceClose();
	    if (p != null) p.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Reset the access control policies.  This call clears the policy caches
     * and logs all users out. Rewriting the policies can take a while.
     * @return true if all goes well
     * @throws DeterFault on errors
     */
    public boolean resetAccessControl() throws DeterFault {
	SharedConnection sc = null;
	Collection<UserDB> users = null;
	Collection<ProjectDB> projects = null;
	Collection<CircleDB> circles = null;
	Collection<ExperimentDB> experiments = null;
	Collection<LibraryDB> libraries = null;
	Collection<ResourceDB> resources = null;
	Collection<RealizationDB> realizations = null;
	CredentialStoreDB cdb = null;

	log.info("resetAccessControl");
	try {
	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("resetAccessControl",
		    new CredentialSet("system", null), sc);

	    cdb = new CredentialStoreDB(sc);
	    cdb.clearCache();

	    // System
	    log.info("updating system policy");
	    updateSystemPolicy(sc);
	    log.info("system policy updated");
	    // Users
	    log.info("updating users policy");
	    users = UserDB.getUsers(null, sc);
	    for (UserDB u : users) {
		u.updateUserPolicy();
		// Log everyone out, too, their credentials are being replaced.
		cdb.unbindUid(u.getUid());
		// Close the DB connection
		u.close();
	    }
	    users = null;
	    cdb.close();

	    log.info("users policy updated");

	    // Projects
	    log.info("updating projects policy");
	    projects = ProjectDB.getProjects(null, null, sc);
	    for (ProjectDB p : projects ) {
		p.updateProjectPolicy();
		for (Member m: p.getMembers())
		    p.updateUserCredentials(m.getUid());
		p.close();
	    }
	    projects = null;
	    log.info("projects policy updated");

	    // Circles
	    log.info("updating circles policy");
	    circles = CircleDB.getCircles(null, null, sc);
	    for (CircleDB c : circles ) {
		c.updateCirclePolicy();
		for (Member m: c.getMembers())
		    c.updateUserCredentials(m.getUid());
		c.close();
	    }
	    circles = null;
	    log.info("circles policy updated");

	    // Experiments
	    log.info("updating experiments policy");
	    experiments =
		ExperimentDB.getExperiments(null, null, null, -1, -1, sc);
	    for (ExperimentDB e: experiments ) {
		e.updatePolicyCredentials();
		e.updateCircleCredentials();
		e.updateOwnerCredentials(null, e.getOwner());
	    }
	    experiments = null;
	    log.info("experiments policy updated");
	    // Libraries
	    log.info("updating experiments policy");
	    libraries = LibraryDB.getLibraries(null, null, -1, -1, sc);
	    for (LibraryDB lib: libraries ) {
		lib.updatePolicyCredentials();
		lib.updateCircleCredentials();
		lib.updateOwnerCredentials(null, lib.getOwner());
	    }
	    libraries = null;
	    log.info("libraries policy updated");
	    log.info("updating resources policy");
	    resources = ResourceDB.getResources(null, null, null, null, null,
		    new ArrayList<ResourceTag>(), -1, -1, sc);
	    for (ResourceDB res: resources) {
		res.updatePolicyCredentials();
		res.updateCircleCredentials();
	    }
	    log.info("resources policy updated");
	    resources = null;
	    log.info("updating realizations policy");
	    realizations = RealizationDB.getRealizations(null, null,
		    -1, -1, sc);
	    for (RealizationDB r: realizations) {
		r.updatePolicyCredentials(r.getCreator());
		r.updateCircleCredentials();
	    }
	    log.info("realizations policy updated");
	    realizations = null;
	    sc.close();
	    log.info("resetAccessControl succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("resetAccessControl failed: " + df);
	    if (users != null)
		for (UserDB u : users)
		    u.forceClose();
	    if (projects != null)
		for (ProjectDB p : projects)
		    p.forceClose();
	    if (circles != null)
		for (CircleDB c : circles)
		    c.forceClose();
	    if (experiments != null)
		for (ExperimentDB e : experiments)
		    e.forceClose();
	    if (libraries != null)
		for (LibraryDB lib : libraries)
		    lib.forceClose();
	    if (resources != null)
		for (ResourceDB res : resources)
		    res.forceClose();
	    if (realizations != null)
		for (RealizationDB r : realizations)
		    r.forceClose();
	    if ( cdb != null ) cdb.forceClose();
	    if ( sc != null ) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Clear the credential cache - not the underlying policy definitions.
     * @return true if all goes well
     * @throws DeterFault on errors
     */
    public boolean clearCredentialCache() throws DeterFault {
	SharedConnection sc = null;
	CredentialStoreDB cdb = null;

	log.info("clearCredentialCache");
	try {
	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("clearCredentialCache",
		    new CredentialSet("system", null), sc);

	    cdb = new CredentialStoreDB(sc);
	    cdb.clearCache();
	    cdb.close();
	    sc.close();
	    return true;
	}
	catch (DeterFault df) {
	    log.error("clearCredentialCache failed: " + df);
	    if (cdb != null) cdb.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Add a permission to the system, valid for a given kind of object.
     * @param name The name of the new permission
     * @param object the type of object the permission is applied to
     * @return true if the permission was added
     * @throws DeterFault on error
     */
    public boolean addPermission(String name, String object)
	    throws DeterFault {
	SharedConnection sc = null;
	PermissionDB p = null;

	log.info("addPermission " + name);
	try {
	    if ( name == null )
		throw new DeterFault(DeterFault.request,
			"No permission name given");
	    if ( object == null )
		throw new DeterFault(DeterFault.request,
			"No permission object given");
	    sc = new SharedConnection();
	    sc.open();
	    checkAccess("addPermission",
		    new CredentialSet("system", null), sc);
	    p = new PermissionDB(name, object, sc);
	    p.create();
	    p.close();
	    sc.close();
	    log.info("addPermission " + name + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.info("addPermission " + name + " failed: " + df);
	    if ( p != null ) p.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
