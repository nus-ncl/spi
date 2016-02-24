
package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.embedding.Embedder;
import net.deterlab.testbed.experiment.ExperimentDB;
import net.deterlab.testbed.experiment.ExperimentProfileDB;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * This service manages experiments that represent how users make use of DETER
 * in their research.  An experiment is identified by a unique identifier that
 * is prefixed by either a user ID or a project ID that scopes its name (the
 * separation character is a colon (:)).  That scoping may be hidden by an
 * application.  The rights to create experiments in a project's namespace is
 * controlled by a users project permissions.
 * <p>
 * An experiment consists of zero or more
 * <a href="ExperimentAspect.html">aspect</a>s that define its salient
 * components.  Currently the only aspect that the system assigns a meaning to
 * is the layout aspect.
 * <p>
 * The right to inspect and control an experiment is controlled by the
 * experiment's access control list (ACL).  An ACL maps
 * <a href="Circles.html">circles</a> to permissions.  These are represented as
 * collections of <a href="AccessMember.html">AccessMember</a> objects.
 * <p>
 * Each experiment has an owner, initially the creator of the experiment,
 * though ownership can be transferred by the owner.  The owner can manipulate
 * the experiment's profile/description.
 *
 * @author ISI DETER team
 * @version 1.0
 * @see AccessMember
 * @see ExperimentAspect
 */
public class Experiments extends ProfileService {
    /** Experiments log */
    private Logger log;
    /** Embedder */
    private Embedder defaultEmbedder;

    /**
     * Construct an Experiments service.
     */
    public Experiments() { 
	setLogger(Logger.getLogger(this.getClass()));
	defaultEmbedder = null;
	try {
	    Config config = new Config();
	    String defaultEmbedderString =
		config.getProperty("defaultEmbedder");
	    defaultEmbedder = getEmbedder(defaultEmbedderString, null);
	}
	catch (DeterFault df) {
	    if ( log != null )
		log.error("Cannot load embedder on experiments");
	}
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
     * Return an empty experiment profile  - the profile schema (the ID field
     * of the profile returned will be empty).  Applications will call this to
     * get the names, formats, and other requirements of the experiment schema.
     * The caller does not to be logged in or a valid user to successfully call
     * this operation.
     * @return an empty experiment profile
     * @throws DeterFault on error
     */
    public Profile getProfileDescription() throws DeterFault {
	ExperimentProfileDB ep = null;

	try {
	    Profile p = getProfileDescription(
		    ep = new ExperimentProfileDB());
	    ep.close();
	    return p;
	}
	catch (DeterFault df) {
	    if ( ep != null ) ep.forceClose();
	    throw df;
	}
    }

    /**
     * Return the completed profile associated with experimentid.  The schema
     * information is all returned as well as the values for populated fields
     * in this experiment's profile.  Any user with read rights to the
     * experiment can retrieve the profile.
     * @param eid the experiment ID whose profile is being retrieved.
     * @return the completed experiment profile
     * @throws DeterFault on perimission errors or no such user.
     */
    public Profile getExperimentProfile(String eid) throws DeterFault {
	ExperimentProfileDB ep = null;

	try {
	    Profile p = getProfile(eid, ep = new ExperimentProfileDB());
	    ep.close();
	    return p;
	}
	catch (DeterFault df) {
	    if ( ep != null ) ep.forceClose();
	    throw df;
	}
    }

    /**
     * Process a list of change requests for attributes in eid's profile.
     * Each request is encoded in a
     * <a href="ChangeAttribute.html">ChangeAttribute</a>.
     * For each request, a <a href="ChangeResult.html">ChangeResult</a>
     * is returned, either indicating that the change has gone through or
     * that it failed.  Failed changes are annotated with a reason.
     * @param eid the experiment whose profile is being modified
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     * @see ChangeAttribute
     * @see ChangeResult
     */
    public ChangeResult[] changeExperimentProfile(String eid,
	    ChangeAttribute[] changes) throws DeterFault {
	ExperimentProfileDB all = null;
	ExperimentProfileDB ep = null;

	try {
	    ep = new ExperimentProfileDB();
	    all = new ExperimentProfileDB(ep.getSharedConnection());
	    ChangeResult[] rv = changeProfile(eid, ep, all,
		    changes);
	    ep.close();
	    all.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( ep != null) ep.forceClose();
	    if ( all != null) all.forceClose();
	    throw df;
	}
    }

    /**
     * Add a new experiment attribute to the profile schema.  This operation is
     * an administrative action that modifies the profiles of all experiments
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
    public boolean createExperimentAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def) 
	throws DeterFault {
	ExperimentProfileDB profile = null;

	try {
	    boolean rv = createAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    def, profile = new ExperimentProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing experiment attribute.  This operation
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
    public boolean modifyExperimentAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length) 
	throws DeterFault {
	ExperimentProfileDB profile = null;

	try {
	    boolean rv = modifyAttribute(name, type, optional, access,
		    description, format, formatdescription, order, length,
		    profile = new ExperimentProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Remove an experiment attribute from the schema of all circle profiles.
     * Any values are also deleted. This operation is administrative.
     *
     * @param name the attribute to remove
     * @return true on success
     * @throws DeterFault on error
     */
    public boolean removeExperimentAttribute(String name) throws DeterFault {
	ExperimentProfileDB profile = null;

	try {
	    boolean rv = removeAttribute(name,
		    profile = new ExperimentProfileDB());
	    profile.close();
	    return rv;
	}
	catch (DeterFault df) {
	    if ( profile != null ) profile.forceClose();
	    throw df;
	}
    }

    /**
     * Return the strings that encode valid permissions for experiments.
     * @return the strings that encode valid permissions for experiments.
     * @throws DeterFault on system errors
     */
    public String[] getValidPermissions() throws DeterFault {
	ExperimentDB edb = null;

	log.info("getValidPermissions");
	try {
	    edb = new ExperimentDB();
	    Set<String> perms = edb.getValidPerms();
	    log.info("getValidPermissions succeeded");
	    return perms.toArray(new String[0]);
	}
	catch (DeterFault df) {
	    if  (edb != null ) edb.forceClose();
	    log.info("getValidPermissions failed: " + df);
	    throw df;
	}
    }

    /**
     * Create an experiment named eid from the given components.
     * <p>
     * The owner is a user ID, usually the user making the request, though
     * administrators can create experiment on others behalf.
     * <p>
     * The aspects are given as an array of
     * <a href="ExperimentAspect.html">ExperimentAspect</a>s.  Only a layout
     * aspect is driectly understood, but most others will be kept verbatim and
     * made accessible to users and applications.
     * <p>
     * The access lists is an array of
     * <a href="AccessMember.html">AccessMember</a>s that map circles to
     * experiment pernmissions.
     * <p>
     * The profile is an array of profile attributes, properly filled in.
     *
     *
     * @param eid the experiment ID
     * @param owner the owner
     * @param aspects the aspects that make up the experiment
     * @param accessLists lists of circles and permissions to this experiment
     * @param profile the experiment profile
     * @return true if the experiment is created
     * @throws DeterFault on error
     * @see AccessMember
     * @see ExperimentAspect
     */
    public boolean createExperiment(String eid, String owner,
	    ExperimentAspect[] aspects, AccessMember[] accessLists,
	    Attribute[] profile)
	throws DeterFault {

	SharedConnection sc = null;
	ExperimentProfileDB newExperimentProfile = null;
	ExperimentDB nexp = null;

	log.info("createExperiment eid " + eid );

	if ( aspects == null )
	    throw new DeterFault(DeterFault.request, "Null aspect list?");

	if ( accessLists == null )
	    throw new DeterFault(DeterFault.request, "Null access list?");

	try {
	    if ( eid == null)
		throw new DeterFault(DeterFault.request, "eid is required");

	    sc = new SharedConnection();
	    sc.open();

	    checkScopedName(eid, "Experiment", sc);
	    newExperimentProfile = new ExperimentProfileDB(sc);
	    ExperimentProfileDB empty = null;
	    try {
		empty = new ExperimentProfileDB(sc);
		checkProfile(profile, newExperimentProfile, empty);
		empty.close();
	    }
	    catch (DeterFault df) {
		if ( empty != null ) empty.forceClose();
		throw df;
	    }
	    nexp = new ExperimentDB(eid, sc);
	    nexp.create(owner, aspects, Arrays.asList(accessLists));
	    newExperimentProfile.setId(eid);
	    newExperimentProfile.saveAll();
	    newExperimentProfile.close();
	    nexp.close();
	    log.info("createExperiment eid " + eid  + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("CreateExperiment " + eid + " failed: " + df);
	    if (newExperimentProfile != null) newExperimentProfile.forceClose();
	    if (nexp != null) nexp.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove aspects from an existing experiment.  The aspects in this case
     * are only used as aspect identifiers; any fields other than name, type
     * and sub-type are ignored.  Exact matches are required.
     *
     * @param eid the experiment to modify
     * @param aspects the name and types of aspects to remove.
     * @return a list of aspects removed
     * @throws DeterFault on error
     * @see ChangeResult
     * @see ExperimentAspect
     */
    public ChangeResult[] removeExperimentAspects(String eid,
	    ExperimentAspect[] aspects) throws DeterFault {
	SharedConnection sc = null;
	ExperimentDB exp = null;
	List<ExperimentAspect> params = new ArrayList<ExperimentAspect>();
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	log.info("removeExperimentAspects eid " + eid );

	try {
	    if ( eid == null)
		throw new DeterFault(DeterFault.request, "eid is required");
	    if ( aspects == null || aspects.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty aspect list");

	    sc = new SharedConnection();
	    sc.open();

	    exp = new ExperimentDB(eid, sc);
	    if ( !exp.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid experiment " + eid);

	    checkAccess("experiment_" + eid + "_removeExperimentAspects",
		    new CredentialSet("experiment", eid), sc);

	    // Trim out any malformed aspect requests - need at least a name
	    // and a type.
	    for (ExperimentAspect a : aspects) {
		String name = a.getName();
		String type = a.getType();

		if ( name == null ) {
		    log.warn("Ignoring request to delete aspect with no name");
		    rv.add(new ChangeResult(name, "No aspect name", false));
		}
		else if ( type == null ) {
		    log.warn("Ignoring request to delete aspect with no type" +
			    " name is " + name);
		    rv.add(new ChangeResult(name, "No aspect type", false));
		}
		else
		    params.add(a);
	    }
	    rv.addAll(exp.removeAspects(params));
	    log.info("removeExperimentAspects " + eid + " succeeded. ");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("removeExperimentAspects " + eid + " failed: " + df);
	    if ( exp != null ) exp.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Add aspects to an existing experiment.  The additions are complete
     * <a href="ExperimentAspect.html">ExperimentAspect</a> objects.  Results
     * are returned as a collection of
     * <a href="ChangeResult.html">ChangeResult</a> objects.
     * <p>
     * Note that inconsistent or badly formed aspects will be rejected.
     *
     * @param eid the experiment to modify
     * @param aspects the aspects to add
     * @return a list of aspects successfully added
     * @throws DeterFault on error
     * @see ChangeResult
     * @see ExperimentAspect
     */
    public ChangeResult[] addExperimentAspects(String eid,
	    ExperimentAspect[] aspects) throws DeterFault {
	SharedConnection sc = null;
	ExperimentDB exp = null;
	List<ChangeResult> rv = null;

	log.info("addExperimentAspects eid " + eid );

	try {
	    if ( eid == null)
		throw new DeterFault(DeterFault.request, "eid is required");
	    if ( aspects == null || aspects.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty aspect list");

	    sc = new SharedConnection();
	    sc.open();

	    exp = new ExperimentDB(eid, sc);
	    if ( !exp.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid experiment " + eid);

	    checkAccess("experiment_" + eid + "_addExperimentAspects",
		    new CredentialSet("experiment", eid), sc);

	    rv = exp.addAspects(Arrays.asList(aspects));
	    log.info("addExperimentAspects " + eid + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("addExperimentAspects " + eid + " failed: " + df);
	    if ( exp != null ) exp.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change aspects of an existing experiment.  The additions are complete
     * <a href="ExperimentAspect.html">ExperimentAspect</a> objects.
     * The Data or DataReference field contains an aspect-specific incremental
     * change description.  Results are returned as a collection of
     * <a href="ChangeResult.html">ChangeResult</a> objects.
     * <p>
     * Note that inconsistent or badly formed aspects will be rejected.
     *
     * @param eid the experiment to modify
     * @param aspects the aspects to add
     * @return a list of aspects successfully added
     * @throws DeterFault on error
     * @see ChangeResult
     * @see ExperimentAspect
     */
    public ChangeResult[] changeExperimentAspects(String eid,
	    ExperimentAspect[] aspects) throws DeterFault {
	SharedConnection sc = null;
	ExperimentDB exp = null;
	List<ChangeResult> rv = null;

	log.info("changeExperimentAspects eid " + eid );

	try {
	    if ( eid == null)
		throw new DeterFault(DeterFault.request, "eid is required");
	    if ( aspects == null || aspects.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty aspect list");

	    sc = new SharedConnection();
	    sc.open();

	    exp = new ExperimentDB(eid, sc);
	    if ( !exp.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid experiment " + eid);

	    checkAccess("experiment_" + eid + "_changeExperimentAspects",
		    new CredentialSet("experiment", eid), sc);

	    rv = exp.changeAspects(Arrays.asList(aspects));
	    log.info("changeExperimentAspects " + eid + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changeExperimentAspects " + eid + " failed: " + df);
	    if ( exp != null ) exp.forceClose();
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
     * @param eid the experiment to modify
     * @param acl the ACL changes
     * @return a list of ACL change results successfully added
     * @throws DeterFault on error
     * @see AccessMember
     * @see ChangeResult
     */
    public ChangeResult[] changeExperimentACL(String eid, AccessMember[] acl)
	throws DeterFault {
	SharedConnection sc = null;
	ExperimentDB exp = null;
	List<ChangeResult> rv = new ArrayList<ChangeResult>();

	log.info("changeExperimentACL eid " + eid );

	try {
	    if ( eid == null)
		throw new DeterFault(DeterFault.request, "eid is required");
	    if ( acl == null || acl.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty ACL change list");

	    sc = new SharedConnection();
	    sc.open();

	    exp = new ExperimentDB(eid, sc);
	    if ( !exp.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid experiment " + eid);

	    checkAccess("experiment_" + eid + "_changeExperimentACL",
		    new CredentialSet("experiment", eid), sc);

	    for (AccessMember m : acl) {
		try {
		    exp.assignPermissions(m);
		    rv.add(new ChangeResult(m.getCircleId(), null, true));
		    log.info("changeExperimentACL succeeded for " +
			    m.getCircleId());
		}
		catch (DeterFault df) {
		    rv.add(new ChangeResult(m.getCircleId(),
				df.getDetailMessage(), false));
		    log.info("changeExperimentACL failed for " +
			    m.getCircleId() + ": " + df.getDetailMessage());
		}
	    }
	    log.info("changeExperimentACL " + eid + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changeExperimentACL " + eid + " failed: " + df);
	    if ( exp != null ) exp.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Set the owner of this experiment.  Only owners and admins can do this.
     * @param eid the experiment to modify
     * @param uid the new owner
     * @return true if the changed worked
     * @throws DeterFault if something goes wrong
     */

    public boolean setOwner(String eid, String uid) throws DeterFault {
	SharedConnection sc = null;
	ExperimentDB exp = null;

	log.info("setOwner for " + eid + " " + uid );
	try {
	    sc = new SharedConnection();
	    sc.open();

	    exp = new ExperimentDB(eid, sc);

	    if ( !exp.isValid())
		throw new DeterFault(DeterFault.request, "invalid eid");

	    checkAccess("experiment_" + eid + "_setOwner",
		    new CredentialSet("experiment", eid), sc);

	    exp.setOwner(uid);
	    exp.close();
	    sc.close();
	    log.info("setOwner for " + eid + " " + uid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("setOwner for " + eid + " " + uid + " failed: " + df);
	    if (exp != null ) exp.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove an experiment from the testbed.  Only the experiment's owner or
     * an administrator can do this.
     * @param eid the experiment to remove
     * @return true on success
     * @throws DeterFault on failure
     */
    public boolean removeExperiment(String eid) throws DeterFault {
	log.info("removeExperiment eid " + eid);
	SharedConnection sc = null;
	ExperimentDB rem = null;
	ExperimentProfileDB profile = null;

	try {

	    if ( eid == null )
		throw new DeterFault(DeterFault.request, "eid is required");

	    sc = new SharedConnection();
	    sc.open();

	    rem = new ExperimentDB(eid, sc);
	    if ( !rem.isValid())
		throw new DeterFault(DeterFault.request,
			"Invalid experiment " + eid);

	    checkAccess("experiment_" + eid + "_removeExperiment",
		    new CredentialSet("experiment", eid), sc);

	    try {
		profile = new ExperimentProfileDB(eid, sc);
		profile.loadAll();
		profile.removeAll();
		profile.close();
	    }
	    catch (DeterFault df) {
		log.warn("Could not remove profile: " + df.getDetailMessage());
		if (profile != null ) profile.forceClose();
	    }
	    rem.remove();
	    log.info("removeExperiment eid " + eid + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeExperiment " + eid + " failed: " + df);
	    if ( rem != null ) rem.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Realize an experiment on the testbed in the given circle.  This actually
     * starts the realization process.  Users will need to poll the realization
     * to determine when the realization is complete.
     * @param uid user id realizing the experiment
     * @param eid experiment ID to realize
     * @param cid the circle in which to realize the experiment
     * @param acl the inital access control list for the realization
     * @param sendNotifications if true send notifications when state changes
     * @return a description of the realization.
     * @throws DeterFault on errors
     */
    public RealizationDescription realizeExperiment(String uid, String eid,
	    String cid, AccessMember[] acl, Boolean sendNotifications)
	throws DeterFault {
	SharedConnection sc = null;
	RealizationDescription rv = null;
	ExperimentDB edb = null;
	TopologyDescription td = null;

	log.info("realizeExperiment uid " + uid + " eid " + eid +
		" cid " + cid + " sendNotifications " + sendNotifications);
	try {
	    boolean notifications = (sendNotifications != null ) ?
		sendNotifications : false;

	    sc = new SharedConnection();
	    sc.open();

	    edb = new ExperimentDB(eid, sc);
	    if ( !edb.isValid())
		throw new DeterFault(DeterFault.request,
			"No such experiment: " + eid);

	    td = edb.realizeAspects();
	    Embedder embedder = getEmbedder(td, defaultEmbedder);

	    if ( embedder == null )
		throw new DeterFault(DeterFault.internal,
			"Cannot find an embedder");
	    rv = embedder.startRealization(uid, eid, cid, acl,
		    td, notifications);
	    log.info("realizeExperiment " + eid + " succeeded");
	    return rv;
	}
	catch (DeterFault df) {
	    log.error("realizeExperiment " + eid + " failed: " + df);
	    if ( edb != null ) edb.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Return information about Experiments.  Experiments that the given user
     * can read, that are in the given library and that the regexp matches are
     * returned.
     * <p>
     * A list of queryAspects can be included that determine which aspects of
     * the experiments are returned.  The sub-fields of the aspects determine
     * the returned aspects.  If the name is present, it much match.  If the
     * type is given it must also match.  If a type is given the subtype field
     * further scopes the search.  A missing subtype field selects aspects
     * without any subtype.  A specific subtype selects only aspects that match
     * both type and subtype, and the distinguished value "*" matches any
     * subtype.  A subtype with a null type is invalid.
     * <p>
     * The views returned are
     * <a href="ExperimentDescription.html">ExperimentDescription</a> objects.
     *
     * @param uid the users experiments to read
     * @param lib the library to search
     * @param regex further matching
     * @param queryAspects the aspects of the experiments to return, may be
     *        empty
     * @param listOnly if true do not return data in the aspects
     * @param offset the first experiment to return (1-based, ordered by
     *	    creation time)
     * @param count the number of experiments to return
     * @return an array of ExperimentDescriptions
     * @throws DeterFault on failure
     */
    public ExperimentDescription[] viewExperiments(String uid, String lib,
	    String regex, ExperimentAspect[] queryAspects, boolean listOnly,
	    Integer offset, Integer count)
	    throws DeterFault {
	SharedConnection sc = null;
	List<ExperimentDB> exps = null;
	Set<String> vp = null;
	int off = -1;
	int cnt = -1;
	log.info("viewExperiments for " + uid + " " + lib + " " + regex );
	try {

	    if ( uid == null )
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_viewExperiments",
		    new CredentialSet("user", uid), sc);
	    if ( lib != null)
		checkAccess("library_" + lib + "_viewExperiments",
			new CredentialSet("library", lib), sc);

	    List<ExperimentDescription> rv =
		new ArrayList<ExperimentDescription>();
	    if ( offset != null && count == null )
		    throw new DeterFault(DeterFault.request,
			    "Offset without count");
	    if ( count != null && count != 0 )
		cnt = count;

	    if  ( offset != null )
		off = offset;

	    exps = ExperimentDB.getExperiments(uid, lib, regex, off, cnt, sc);

	    for (ExperimentDB e : exps ){
		String eid = e.getEid();
		CredentialSet es = new CredentialSet("experiment", eid);
		Set<String> up = new TreeSet<String>();

		if (vp == null)
		    vp = e.getValidPerms();
		ExperimentDescription ed =
		    new ExperimentDescription(eid, e.getOwner());
		ed.setACL(e.getACL());
		for (String p: vp) {
		    try {
			checkAccess("experiment_" + eid + "_" + p, es, uid, sc);
			up.add(p);
		    }
		    catch (DeterFault ignored) { }
		}
		ed.setPerms(up);

		if ( queryAspects != null && queryAspects.length > 0)
		    ed.setAspects(
			    e.getAspects(Arrays.asList(queryAspects),
				!listOnly));
		else
		    ed.setAspects(e.getAspects(!listOnly));
		rv.add(ed);
		e.close();
	    }
	    exps = null;
	    sc.close();
	    log.info("viewExperiments for " + uid + " " + lib + " " +
		    regex + " succeeded");
	    return rv.toArray(new ExperimentDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewExperiments for " + uid + " " + lib + " " + regex +
		    " failed: " + df);
	    if ( exps != null)
		for ( ExperimentDB e : exps)
		    if ( e != null ) e.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
