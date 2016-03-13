package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.abac.Identity;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.embedding.Embedder;
import net.deterlab.testbed.experiment.ExperimentDB;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.policy.CredentialStoreDB;
import net.deterlab.testbed.realization.RealizationDB;

/**
 * This service manages realizations used in realizing experiment that
 * represent how users make use of DETER in their research.  
 * <p>
 * A resource consists of mappings between resources and between topology
 * elements and resources.  Containment hierarchies are represented
 * <a href="RealizationContainment.java">RealizationContaiment</a> objects.
 * Mappings are represented by <a href="RealizationMap.java">RealizationMap</a>
 * objects.
 * <p>
 * The right to inspect and control a resource is controlled by the
 * resource's access control list (ACL).  An ACL maps
 * <a href="Circles.html">circles</a> to permissions.  These are represented as
 * collections of <a href="AccessMember.html">AccessMember</a> objects.
 * <p>
 *
 * @author ISI DETER team
 * @version 1.0
 * @see AccessMember
 * @see RealizationContainment
 * @see RealizationMap
 */
public class Realizations extends DeterService {
    /** Resources log */
    private Logger log;
    // XXX
    /** Embedder */
    private Embedder defaultEmbedder;

    /**
     * Construct a Resources service.
     */
    public Realizations() { 
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
     * Return the strings that encode valid permissions for resources.
     * @return the strings that encode valid permissions for resources.
     * @throws DeterFault on system errors
     */
    public String[] getValidPermissions() throws DeterFault {
	RealizationDB rdb = null;

	log.info("getValidPermissions");
	try {
	    rdb = new RealizationDB();
	    Set<String> perms = rdb.getValidPerms();
	    log.info("getValidPermissions succeeded");
	    return perms.toArray(new String[0]);
	}
	catch (DeterFault df) {
	    if  (rdb != null ) rdb.forceClose();
	    log.info("getValidPermissions failed: " + df);
	    throw df;
	}
    }

    /**
     * Change elements in an existing realization's ACL.  The new
     * <a href="AccessMember.html">AccessMember</a>s
     * overwrite old ones if present and are added if not.  If no permissions
     * are given, the entry is removed.  An array of
     * <a href="ChangeResult.html">ChangeResult</a> objects is returned.
     *
     * @param name the realization to modify
     * @param acl the ACL changes
     * @return a list of ACL change results successfully added
     * @throws DeterFault on error
     * @see AccessMember
     * @see ChangeResult
     */
    public ChangeResult[] changeRealizationACL(String name, AccessMember[] acl)
	throws DeterFault {
	SharedConnection sc = null;
	RealizationDB res = null;
	List<ChangeResult> rv = new ArrayList<>();

	log.info("changeRealizationACL name " + name );

	try {
	    if ( name == null)
		throw new DeterFault(DeterFault.request, "name is required");
	    if ( acl == null || acl.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty ACL change list");

	    sc = new SharedConnection();
	    sc.open();

	    res = new RealizationDB(sc);
	    res.setName(name);

	    checkAccess("realization_" + name + "_changeRealizationACL",
		    new CredentialSet("realization", name), sc);

	    for (AccessMember m : acl) {
		try {
		    res.assignPermissions(m);
		    rv.add(new ChangeResult(m.getCircleId(), null, true));
		    log.info("changeRealizationACL succeeded for " +
			    m.getCircleId());
		}
		catch (DeterFault df) {
		    rv.add(new ChangeResult(m.getCircleId(),
				df.getDetailMessage(), false));
		    log.info("changeRealizationACL failed for " +
			    m.getCircleId() + ": " + df.getDetailMessage());
		}
	    }
	    log.info("changeRealizationACL " + name + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changeRealizationACL " + name + " failed: " + df);
	    if ( res != null ) res.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Release resources from  a realization.
     * @param name the realization to release
     * @return A description of the realization
     * @throws DeterFault on failure
     */
    public RealizationDescription releaseRealization(String name)
	throws DeterFault {
	log.info("releaseRalization name " + name);
	SharedConnection sc = null;
	RealizationDB rem = null;
	ExperimentDB edb = null;
	CredentialStoreDB cdb = null;

	try {
	    if ( name == null )
		throw new DeterFault(DeterFault.request, "name is required");

	    sc = new SharedConnection();
	    sc.open();
	    
	    cdb = new CredentialStoreDB(sc);

	    rem = new RealizationDB(sc);
	    rem.setName(name);

	    Identity caller = getCallerIdentity();
	    String uid = cdb.keyToUid(caller);
	    cdb.close();
	    cdb = null;

	    checkAccess("realization_" + name + "_releaseRealization",
		    new CredentialSet("realization", name), sc);

	    rem.load();
	    String embedderName = rem.getEmbedderName();
	    Embedder embedder = getEmbedder(embedderName, defaultEmbedder);

	    edb = new ExperimentDB(rem.getExperimentID(), sc);
	    edb.releaseAspects();
	    edb.close();
	    edb = null;

	    if (embedder == null )
		throw new DeterFault(DeterFault.internal,
			"Cannot find embedder");

	    RealizationDescription rv =
		embedder.terminateRealization(uid, name);
	    log.info("releaseRealization eid " + name + " succeeded");
	    return rv;
	}
	catch (DeterFault df) {
	    log.error("releaseRealization " + name + " failed: " + df);
	    if ( edb != null ) edb.forceClose();
	    if ( rem != null ) rem.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a realization from the testbed.  This releases resources from the
     * testbed as a side effect.
     * @param name the realization to remove
     * @return true on success
     * @throws DeterFault on failure
     */
    public boolean removeRealization(String name) throws DeterFault {
	log.info("removeRalization name " + name);
	SharedConnection sc = null;
	RealizationDB rem = null;
	ExperimentDB edb = null;
	CredentialStoreDB cdb = null;

	try {
	    if ( name == null )
		throw new DeterFault(DeterFault.request, "name is required");

	    sc = new SharedConnection();
	    sc.open();

	    cdb = new CredentialStoreDB(sc);

	    Identity caller = getCallerIdentity();
	    String uid = cdb.keyToUid(caller);
	    cdb.close();
	    cdb = null;

	    rem = new RealizationDB(sc);
	    rem.setName(name);

	    checkAccess("realization_" + name + "_removeRealization",
		    new CredentialSet("realization", name), sc);

	    rem.load();
	    String embedderName = rem.getEmbedderName();
	    Embedder embedder = getEmbedder(embedderName, defaultEmbedder);

	    // Call releaseAspect on each aspect of the experiments
	    edb = new ExperimentDB(rem.getExperimentID(), sc);
	    edb.releaseAspects();
	    edb.close();
	    edb = null;
	    rem.close();
	    rem = null;

	    if (embedder == null )
		throw new DeterFault(DeterFault.internal,
			"Cannot find embedder");

	    embedder.terminateRealization(uid, name);
	    log.info("removeRealization eid " + name + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeRealization " + name + " failed: " + df);
	    if ( cdb != null) cdb.forceClose();
	    if ( edb != null ) edb.forceClose();
	    if ( rem != null ) rem.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Return information about Realizations.  Realizations that the given user
     * can read, that the regexp matches the name are returned.
     * <p>
     * The views returned are
     * <a href="RealizationDescription.html">RealizationDescription</a>
     * objects.
     *
     * @param uid the users resources to read
     * @param regex further matching regexp (optional)
     * @param offset the first resource to return (1-based)
     * @param count the number of resource to return
     * @return an array of ResourceDescriptions
     * @throws DeterFault on failure
     */
    public RealizationDescription[] viewRealizations(String uid, String regex,
	    Integer offset, Integer count) throws DeterFault {
	SharedConnection sc = null;
	List<RealizationDB> res = null;
	Set<String> vp = null;
	int off = -1;
	int cnt = -1;
	log.info("viewRealizations for " + uid + " " + " " + regex );
	try {

	    if ( uid == null )
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    checkAccess("user_" + uid + "_viewResources", 
	    		new CredentialSet("user", uid), sc); 

	    List<RealizationDescription> rv = new ArrayList<>();
	    if ( offset != null && count == null )
		    throw new DeterFault(DeterFault.request,
			    "Offset without count");
	    if ( count != null && count != 0 )
		cnt = count;

	    if  ( offset != null )
		off = offset;

	    res = RealizationDB.getRealizations(uid, regex, off, cnt, sc);

	    for (RealizationDB r : res ){
		String name = r.getName();
		CredentialSet es = new CredentialSet("realization", name);
		Set<String> up = new TreeSet<String>();

		if (vp == null)
		    vp = r.getValidPerms();
		RealizationDescription rd = new RealizationDescription();
		rd.setName(name);
		rd.setStatus(r.getStatus());
		rd.setCircle(r.getCircleID());
		rd.setExperiment(r.getExperimentID());
		rd.setACL(r.getACL());
		for (String p: vp) {
		    // XXX: was disabled as it was very slow
		    try {
		    	checkAccess("resource_" + name + "_" + p, es, uid, sc);
		    	up.add(p);
		    } catch (DeterFault ignored) { }
		}
		rd.setPerms(up);
		rd.setMapping(r.getMapping());
		rd.setContainment(r.getContainment());
		rv.add(rd);
		r.close();
	    }
	    res = null;
	    sc.close();
	    log.info("viewRealizations for " + uid + " " + " " +
		    regex + " succeeded");
	    return rv.toArray(new RealizationDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewRealizations for " + uid + " " + " " + regex +
		    " failed: " + df);
	    if ( res != null)
		for ( RealizationDB r : res)
		    if ( r != null ) r.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
