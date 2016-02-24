package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.library.LibraryDB;
import net.deterlab.testbed.library.LibraryProfileDB;
import net.deterlab.testbed.policy.CredentialSet;

/**
 * Operations on Experiments.
 * This service manages libraries that represent groups of
 * <a href="Experiments.html">Experiments</a> on DETER.
 * A library is identified by a unique identifier that
 * is prefixed by either a user ID or a project ID that scopes its name (the
 * separation character is a colon (:)).  That scoping may be hidden by an
 * application.  The rights to create libraries in a project's namespace is
 * controlled by a users project permissions.
 * <p>
 * An experiment consists of zero or more experiment IDs that are grouped
 * together.
 * <p>
 * The right to inspect and modify an library is controlled by the
 * experiment's access control list (ACL).  An ACL maps
 * <a href="Circles.html">circles</a> to permissions.  These are represented as
 * collections of <a href="AccessMember.html">AccessMember</a> objects.
 * <p>
 * Each library has an owner, initially the creator of the library,
 * though ownership can be transferred by the owner.  The owner can manipulate
 * the library's profile/description.
 *
 * @author ISI DETER team
 * @version 1.0
 * @see AccessMember
 * @author ISI DETER team
 * @version 1.0
 */
public class Libraries extends ProfileService {
    /** Libraries log */
    private Logger log;
    /**
     * Construct a Libraries service.
     */
    public Libraries() { 
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
     * Return an empty library profile  - the profile schema (the ID field
     * of the profile returned will be empty).  Applications will call this to
     * get the names, formats, and other requirements of the library schema.
     * The caller does not to be logged in or a valid user to successfully call
     * this operation.
     * @return an empty library profile
     * @throws DeterFault on error
     */
    public Profile getProfileDescription() throws DeterFault {
	LibraryProfileDB lp = null;

	try {
	    Profile p = getProfileDescription(
		    lp = new LibraryProfileDB());
	    lp.close();
	    return p;
	}
	catch (DeterFault df) {
	    if ( lp != null ) lp.forceClose();
	    throw df;
	}
    }

    /**
     * Return the completed profile associated with libid.  The schema
     * information is all returned as well as the values for populated fields
     * in this library's profile.  Any user with read rights to the
     * library can retrieve the profile.
     * @param libid the experiment ID whose profile is being retrieved.
     * @return the completed experiment profile
     * @throws DeterFault on perimission errors or no such user.
     */
    public Profile getLibraryProfile(String libid) throws DeterFault {
	LibraryProfileDB lp = null;

	try {
	    Profile p = getProfile(libid, lp = new LibraryProfileDB());
	    lp.close();
	    return p;
	}
	catch (DeterFault df) {
	    if ( lp != null ) lp.forceClose();
	    throw df;
	}
    }

    /**
     * Process a list of change requests for attributes in libid's profile.
     * Each request is encoded in a
     * <a href="ChangeAttribute.html">ChangeAttribute</a>.
     * For each request, a <a href="ChangeResult.html">ChangeResult</a>
     * is returned, either indicating that the change has gone through or
     * that it failed.  Failed changes are annotated with a reason.
     * @param libid the experiment whose profile is being modified
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     * @see ChangeAttribute
     * @see ChangeResult
     */
    public ChangeResult[] changeLibraryProfile(String libid,
	    ChangeAttribute[] changes) throws DeterFault {
	LibraryProfileDB all = null;
	LibraryProfileDB lp = null;

	try {
	    lp = new LibraryProfileDB();
	    all = new LibraryProfileDB(lp.getSharedConnection());
	    ChangeResult[] rv = changeProfile(libid, lp, all,
		    changes);
	    lp.close();
	    all.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( lp != null) lp.forceClose();
	    if ( all != null) all.forceClose();
	    throw df;
	}
    }

    /**
     * Add a new experiment attribute to the profile schema.  This operation is
     * an administrative action that modifies the profiles of all libraries
     * to include a new field.  If the new attribute is not optional, existing
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
    public boolean createLibraryAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def) 
	throws DeterFault {
	LibraryProfileDB profile = null;

	try {
	    boolean rv = createAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    def, profile = new LibraryProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing library attribute.  This operation
     * is administrative. All fields overwrite existing ones.  An application
     * will probably need to read the attribute's schema using
     * GetProfileDescription before making changes.  Only attributes already in
     * the schema can be modified.
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
    public boolean modifyLibraryAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length) 
	throws DeterFault {
	LibraryProfileDB profile = null;

	try {
	    boolean rv = modifyAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    profile = new LibraryProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Remove an library attribute from the schema of all circle profiles.
     * Any values are also deleted. This operation is administrative.
     *
     * @param name the attribute to remove
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean removeLibraryAttribute(String name) throws DeterFault {
	LibraryProfileDB profile = null;

	try {
	    boolean rv = removeAttribute(name,
		    profile = new LibraryProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Return the strings that encode valid permissions for libraries.
     * @return the strings that encode valid permissions for libraries.
     * @throws DeterFault on system errors
     */
    public String[] getValidPermissions() throws DeterFault {
	LibraryDB ldb = null;

	log.info("getValidPermissions");
	try {
	    ldb = new LibraryDB();
	    Set<String> perms = ldb.getValidPerms();
	    log.info("getValidPermissions succeeded");
	    return perms.toArray(new String[0]);
	}
	catch (DeterFault df) {
	    if  (ldb != null ) ldb.forceClose();
	    log.info("getValidPermissions failed: " + df);
	    throw df;
	}
    }

    /**
     * Create a library from a list of experiments and ACLs.
     * <p>
     * The owner is a user ID, usually the user making the request, though
     * administrators can create libraries on others behalf.
     * <p>
     * The eids are given as an array of Strings that identify experiments.
     * <p>
     * The access lists is an array of
     * <a href="AccessMember.html">AccessMember</a>s that map circles to
     * library pernmissions.
     * <p>
     * The profile is an array of profile attributes, properly filled in.
     *
     * @param libid the library ID
     * @param owner the owner
     * @param eids the experiments that make up the library, by eid
     * @param accessLists lists of circles and permissions to this experiment
     * @param profile the experiment profile
     * @return true if the experiment is created
     * @throws DeterFault on error
     */
    public boolean createLibrary(String libid, String owner, String[] eids,
	    AccessMember[] accessLists, Attribute[] profile)
	throws DeterFault {

	SharedConnection sc = null;
	LibraryProfileDB newLibraryProfile = null;
	LibraryDB nlib = null;

	log.info("createLibrary libid " + libid );

	if ( eids == null )
	    throw new DeterFault(DeterFault.request, "Null eid list?");

	if ( accessLists == null )
	    throw new DeterFault(DeterFault.request, "Null access list?");

	try {
	    if ( libid == null)
		throw new DeterFault(DeterFault.request, "libid is required");

	    sc = new SharedConnection();
	    sc.open();

	    checkScopedName(libid, "Library", sc);
	    newLibraryProfile = new LibraryProfileDB(sc);
	    LibraryProfileDB empty = null;
	    try {
		empty = new LibraryProfileDB(sc);
		checkProfile(profile, newLibraryProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if ( empty != null ) empty.forceClose();
		throw df;
	    }
	    nlib = new LibraryDB(libid, sc);
	    nlib.create(owner, eids, Arrays.asList(accessLists));
	    newLibraryProfile.setId(libid);
	    newLibraryProfile.saveAll();
	    newLibraryProfile.close();
	    nlib.close();
	    log.info("createLibrary libid " + libid  + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("createLibrary " + libid + " failed: " + df);
	    if (newLibraryProfile != null) newLibraryProfile.forceClose();
	    if (nlib != null) nlib.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
    /**
     * Remove experiments from an existing library.  The eids in this case
     * are experiment IDs.  Exact matches are required.
     *
     * @param libid the library to modify
     * @param eids the eids of the experiments to remove
     * @return an array of ChangeResults indicating the results of each change
     * @throws DeterFault on error
     * @see ChangeResult
     */
    public ChangeResult[] removeLibraryExperiments(String libid, String[] eids)
	throws DeterFault {
	SharedConnection sc = null;
	LibraryDB lib = null;
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	log.info("removeLibraryExperiments libid " + libid );

	try {
	    if ( libid == null)
		throw new DeterFault(DeterFault.request, "libid is required");
	    if ( eids == null || eids.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty eid list");

	    sc = new SharedConnection();
	    sc.open();

	    lib = new LibraryDB(libid, sc);
	    if ( !lib.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid library " + libid);

	    checkAccess("library_" + libid + "_removeLibraryExperiments",
		    new CredentialSet("library", libid), sc);

	    for (String e : eids) {
		try {
		    lib.removeExperiment(e);
		    log.info("Removed " + e + " from " + libid);
		    rv.add(new ChangeResult(e, null, true));
		}
		catch (DeterFault df) {
		    log.error("Cannot remove " + e + " from " + libid +
			    ": " + df);
		    rv.add(new ChangeResult(e, df.getDetailMessage(), false));
		}
	    }
	    log.info("removeLibraryExperiments " + libid + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("removeLibraryExperiments " + libid + " failed: " + df);
	    if ( lib != null ) lib.forceClose();
	    if ( sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Add experiments to an existing library. The list strings is a list of
     * experiment IDs Results are returned as a collection of
     *
     * <a href="ChangeResult.html">ChangeResult</a> objects.
     * @param libid the library to modify
     * @param eids the experiments to add
     * @return an array of ChangeResults summarizing the results
     * @throws DeterFault on error
     * @see ChangeResult
     */
    public ChangeResult[] addLibraryExperiments(String libid, String[] eids)
	throws DeterFault {
	SharedConnection sc = null;
	LibraryDB lib = null;
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	log.info("addLibraryExperiments libid " + libid );

	try {
	    if ( libid == null)
		throw new DeterFault(DeterFault.request, "libid is required");
	    if ( eids == null || eids.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty eid list");

	    sc = new SharedConnection();
	    sc.open();

	    lib = new LibraryDB(libid, sc);
	    if ( !lib.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid library " + libid);

	    checkAccess("library_" + libid + "_addLibraryExperiments",
		    new CredentialSet("library", libid), sc);
	    for ( String eid: eids ) {
		try {
		    // XXX: per-experiment catalog check
		    lib.addExperiment(eid);
		    log.info("Added " + eid + " to " + libid);
		    rv.add(new ChangeResult(eid, null, true));
		}
		catch (DeterFault df) {
		    log.error("Cannot add " + eid + " to " + libid + ": " +
			    df);
		    rv.add(new ChangeResult(eid, df.getDetailMessage(), false));
		}
	    }
	    log.info("addLibraryExperiments " + libid + " succeeded. ");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addLibraryExperiments " + libid + " failed: " + df);
	    if ( lib != null ) lib.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change elements in an existing experiment's ACL.  The new
     * <a href="AccessMember.html">AccessMember</a>s
     * overwrite old ones if present and are added if not.  If no permissions
     * are given, the entry is removed.  An array of
     * <a href="ChangeResult.html">ChangeResult</a> objects is returned.
     *
     * @param libid the library to modify
     * @param acl the ACL changes
     * @return a list of ACL change results successfully added
     * @throws DeterFault on error
     * @see AccessMember
     * @see ChangeResult
     */
    public ChangeResult[] changeLibraryACL(String libid, AccessMember[] acl)
	throws DeterFault {
	SharedConnection sc = null;
	LibraryDB lib = null;
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	log.info("changeLibraryACL libid " + libid );

	try {
	    if ( libid == null)
		throw new DeterFault(DeterFault.request, "libid is required");
	    if ( acl == null || acl.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty ACL change list");

	    sc = new SharedConnection();
	    sc.open();

	    lib = new LibraryDB(libid, sc);
	    if ( !lib.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid library " + libid);

	    checkAccess("library_" + libid + "_changeLibraryACL",
		    new CredentialSet("library", libid), sc);

	    for (AccessMember m : acl) {
		try {
		    lib.assignPermissions(m);
		    rv.add(new ChangeResult(m.getCircleId(), null, true));
		    log.info("changeLibraryACL succeeded for " +
			    m.getCircleId());
		}
		catch (DeterFault df) {
		    rv.add(new ChangeResult(m.getCircleId(),
				df.getDetailMessage(), false));
		    log.info("changeLibraryACL failed for " +
			    m.getCircleId() + ": " + df.getDetailMessage());
		}
	    }
	    log.info("changeLibraryACL " + libid + " succeeded. ");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changeLibraryACL " + libid + " failed: " + df);
	    if ( lib != null ) lib.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Set the owner of this library.  Only owners and admins can do this.
     *
     * @param libid the library to modify
     * @param uid the new owner
     * @return true if the changed worked
     * @throws DeterFault if something goes wrong
     */
    public boolean setOwner(String libid, String uid) throws DeterFault {
	SharedConnection sc = null;
	LibraryDB lib = null;

	log.info("setOwner for " + libid + " " + uid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    lib = new LibraryDB(libid, sc);

	    if ( !lib.isValid())
		throw new DeterFault(DeterFault.request, "invalid libid");

	    checkAccess("library_" + libid + "_setOwner",
		    new CredentialSet("library", libid), sc);

	    lib.setOwner(uid);
	    lib.close();
	    sc.close();
	    log.info("setOwner for " + libid + " " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("setOwner for " + libid + " " + uid + " failed: " + df);
	    if (lib != null ) lib.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a library from the testbed.  Only the library's owner or an
     * administrator can do this.
     *
     * @param libid the library to remove
     * @return true on success
     * @throws DeterFault on failure
     */
    public boolean removeLibrary(String libid) throws DeterFault {
	log.info("removeLibrary libid " + libid);
	SharedConnection sc = null;
	LibraryDB rem = null;
	LibraryProfileDB profile = null;

	try {

	    if ( libid == null )
		throw new DeterFault(DeterFault.request, "libid is required");

	    sc = new SharedConnection();
	    sc.open();

	    rem = new LibraryDB(libid, sc);
	    if ( !rem.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid library " + libid);

	    checkAccess("library_" + libid + "_removeLibrary",
		    new CredentialSet("library", libid), sc);

	    try {
		profile = new LibraryProfileDB(libid, sc);
		profile.loadAll();
		profile.removeAll();
		profile.close();
	    }
	    catch (DeterFault df) {
		log.warn("Could not remove profile: " + df.getDetailMessage());
		if (profile != null ) profile.forceClose();
	    }
	    rem.remove();
	    log.info("removeLibrary libid " + libid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeLibrary libid " + libid + " failed: " + df);
	    if ( rem != null ) rem.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Return information about libraries.  Libraries that the given user
     * can read and that the regexp matches are returned.
     * <p>
     * The descriptions of the libraries are defines as
     * <a href="LibraryDescription.html">LibraryDescription</a> objects.
     * <p>
     * If given, offest and count allow applications to request subsets of a
     * user's libraries, for example to display pages in a table.
     *
     * @param uid the users libraries to read
     * @param regex further matching
     * @param offset start the list at this entry
     * @param count return this many entries
     * @return an array of LibraryDescriptions
     * @throws DeterFault on failure
     * @see LibraryDescription
     */
    public LibraryDescription[] viewLibraries(String uid, String regex,
	    Integer offset, Integer count)
	    throws DeterFault {
	SharedConnection sc = null;
	List<LibraryDB> libs = null;
	Set<String> vp = null;
	int off = -1;
	int cnt = -1;

	log.info("viewLibraries for " + uid + " " + regex );
	try {

	    if ( uid == null )
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_viewLibraries",
		    new CredentialSet("user", uid), sc);

	    List<LibraryDescription> rv =
		new ArrayList<LibraryDescription>();
	    if ( count != null )
		cnt = count;

	    if  ( offset != null )
		off = offset;

	    libs = LibraryDB.getLibraries(uid, regex, off, cnt, sc);

	    for (LibraryDB lib : libs ){
		LibraryDescription ed =
		    new LibraryDescription(lib.getLibid(), lib.getOwner());
		String lid = lib.getLibid();
		CredentialSet ls = new CredentialSet("library", lid);
		Set<String> up = new TreeSet<String>();

		if (vp == null)
		    vp = lib.getValidPerms();
		for (String p: vp) {
		    try {
			checkAccess("library_" + lid + "_" + p, ls, uid, sc);
			up.add(p);
		    }
		    catch (DeterFault ignored) { }
		}
		ed.setExperiments(lib.getExperiments());
		ed.setACL(lib.getACL());
		ed.setPerms(up);

		rv.add(ed);
		lib.close();
	    }
	    libs = null;
	    sc.close();
	    log.info("viewLibraries for " + uid + " " + regex + " succeeded");
	    return rv.toArray(new LibraryDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewLibraries for " + uid + " " + regex +
		    " failed: " + df);
	    if ( libs != null)
		for ( LibraryDB lib : libs)
		    if ( lib != null ) lib.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
