
package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.resource.ResourceDB;

/**
 * This service manages resources used in realizing experiment that represent how users make use of DETER
 * in their research.  
 * <p>
 * A resource consists of zero or more
 * <a href="ResourceFacet.html">facets</a>s that define its salient
 * components. 
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
 * @see ResourceFacet
 */
public class Resources extends DeterService {
    /** Resources log */
    private Logger log;

    /**
     * Construct a Resources service.
     */
    public Resources() { 
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
     * Return the strings that encode valid permissions for resources.
     * @return the strings that encode valid permissions for resources.
     * @throws DeterFault on system errors
     */
    public String[] getValidPermissions() throws DeterFault {
	ResourceDB edb = null;

	log.info("getValidPermissions");
	try {
	    edb = new ResourceDB();
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
     * Create an resources named name from the given components.
     * <p>
     * The name is a URI that identifies the resource.  It is also given a type
     * and optional description.
     * <p>
     * The facets are given as an array of
     * <a href="ResourceFacet.html">ResourceFacet</a>s.
     * <p>
     * The access lists is an array of
     * <a href="AccessMember.html">AccessMember</a>s that map circles to
     * experiment pernmissions.
     * <p>
     *
     * @param name the resource URI
     * @param type the type
     * @param persist true if the resource lasts beyond the realizations it is
     * bound to.
     * @param description the description (optional)
     * @param data the data (optional)
     * @param facets the facets that make up the resource
     * @param tags the name/value tags to attach to the resource
     * @param accessLists lists of circles and permissions to this resource
     * @return true if the resource is created
     * @throws DeterFault on error
     * @see AccessMember
     * @see ResourceFacet
     */
    public boolean createResource(String name, String type, Boolean persist,
	    String description, byte[] data, ResourceFacet[] facets,
	    ResourceTag[] tags, AccessMember[] accessLists) throws DeterFault {

	SharedConnection sc = null;
	ResourceDB nres = null;

	log.info("createResource name " + name);

	if ( facets == null )
	    throw new DeterFault(DeterFault.request, "Null facet list?");

	if ( accessLists == null )
	    throw new DeterFault(DeterFault.request, "Null access list?");

	if (tags == null ) tags = new ResourceTag[0];

	try {
	    if ( name == null)
		throw new DeterFault(DeterFault.request, "name is required");

	    if ( type == null)
		throw new DeterFault(DeterFault.request, "type is required");

	    sc = new SharedConnection();
	    sc.open();
	    checkScopedName(name, "Resource", sc);

	    nres = new ResourceDB(name, sc);
	    nres.setType(type);
	    nres.setPersist((persist != null) ? persist : false);
	    if ( description != null ) nres.setDescription(description);
	    if ( data != null ) nres.setData(data);
	    nres.create(facets, Arrays.asList(accessLists), tags);
	    nres.close();
	    sc.close();
	    log.info("createResource name " + name  + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("CreateResource " + name + " failed: " + df);
	    if (nres != null) nres.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Change elements in an existing resource's ACL.  The new
     * <a href="AccessMember.html">AccessMember</a>s
     * overwrite old ones if present and are added if not.  If no permissions
     * are given, the entry is removed.  An array of
     * <a href="ChangeResult.html">ChangeResult</a> objects is returned.
     *
     * @param name the resource to modify
     * @param acl the ACL changes
     * @return a list of ACL change results successfully added
     * @throws DeterFault on error
     * @see AccessMember
     * @see ChangeResult
     */
    public ChangeResult[] changeResourceACL(String name, AccessMember[] acl)
	throws DeterFault {
	SharedConnection sc = null;
	ResourceDB res = null;
	List<ChangeResult> rv = new ArrayList<>();

	log.info("changeResourceACL name " + name );

	try {
	    if ( name == null)
		throw new DeterFault(DeterFault.request, "name is required");
	    if ( acl == null || acl.length == 0)
		throw new DeterFault(DeterFault.request,
			"Null or empty ACL change list");

	    sc = new SharedConnection();
	    sc.open();

	    res = new ResourceDB(name, sc);

	    checkAccess("resource_" + name + "_changeResourceACL",
		    new CredentialSet("resource", name), sc);

	    for (AccessMember m : acl) {
		try {
		    res.assignPermissions(m);
		    rv.add(new ChangeResult(m.getCircleId(), null, true));
		    log.info("changeResourceACL succeeded for " +
			    m.getCircleId());
		}
		catch (DeterFault df) {
		    rv.add(new ChangeResult(m.getCircleId(),
				df.getDetailMessage(), false));
		    log.info("changeResourceACL failed for " +
			    m.getCircleId() + ": " + df.getDetailMessage());
		}
	    }
	    log.info("changeResourceACL " + name + " succeeded.");
	    return rv.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("changeResourceACL " + name + " failed: " + df);
	    if ( res != null ) res.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }

    /**
     * Remove a resource from the testbed.
     * @param name the resource to remove
     * @return true on success
     * @throws DeterFault on failure
     */
    public boolean removeResource(String name) throws DeterFault {
	log.info("removeResource name " + name);
	SharedConnection sc = null;
	ResourceDB rem = null;

	try {
	    if ( name == null )
		throw new DeterFault(DeterFault.request, "name is required");

	    sc = new SharedConnection();
	    sc.open();

	    rem = new ResourceDB(name, sc);

	    checkAccess("resource_" + name + "_removeResource",
		    new CredentialSet("resource", name), sc);
	    rem.remove();
	    log.info("removeResource eid " + name + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("removeResource " + name + " failed: " + df);
	    if ( rem != null ) rem.forceClose();
	    if (sc != null) sc.forceClose();
	    throw df;
	}
    }
    /**
     * Return information about Resources.  Resources that the given user
     * can read, that the regexp matches the name are returned.
     * <p>
     * The views returned are
     * <a href="ResourceDescription.html">ResourceDescription</a> objects.
     *
     * @param uid the users resources to read
     * @param type the type of resources to read
     * @param regex further matching regexp (optional)
     * @param realization get resources bound to this realization (optional)
     * @param persist true if only persistent resources are requested (optional)
     * @param tags list of tags than must be present in returned resources
     * @param offset the first resource to return (1-based)
     * @param count the number of resource to return
     * @return an array of ResourceDescriptions
     * @throws DeterFault on failure
     */
    public ResourceDescription[] viewResources(String uid, String type,
	    String regex, String realization, Boolean persist,
	    ResourceTag[] tags, Integer offset, Integer count)
	throws DeterFault {
	SharedConnection sc = null;
	List<ResourceDB> res = null;
	List<ResourceTag> tagList = (tags != null) ?
	    Arrays.asList(tags) : new ArrayList<ResourceTag>();
	Set<String> vp = null;
	int off = -1;
	int cnt = -1;
	log.info("viewResources for " + uid + " " + type + " " + regex );
	try {

	    if ( uid == null )
		throw new DeterFault(DeterFault.request, "Missing uid");

	    sc = new SharedConnection();
	    sc.open();

	    /* checkAccess("user_" + uid + "_viewResources",
		    new CredentialSet("user", uid), sc); */

	    List<ResourceDescription> rv = new ArrayList<>();
	    if ( offset != null && count == null )
		    throw new DeterFault(DeterFault.request,
			    "Offset without count");
	    if ( count != null )
		cnt = count;

	    if  ( offset != null )
		off = offset;

	    res = ResourceDB.getResources(uid, type, regex, realization,
		    persist, tagList, off, cnt, sc);

	    for (ResourceDB r : res ){
		String name = r.getName();
		//CredentialSet es = new CredentialSet("resource", name);
		Set<String> up = new TreeSet<String>();

		if (vp == null)
		    vp = r.getValidPerms();
		ResourceDescription rd = new ResourceDescription();
		rd.setName(name);
		rd.setType(r.getType());
		if ( r.getDescription() != null)
		    rd.setDescription(r.getDescription());
		rd.setACL(r.getACL());
		rd.setTags(r.exportTags());
		for (String p: vp) {
		    // XXX: diable for testing - it's very slow
		    // try {
			// checkAccess("resource_" + name + "_" + p, es, uid, sc);
			up.add(p);
		    // }
		    // catch (DeterFault ignored) { }
		}
		rd.setPerms(up);
		List<ResourceFacet> ef = new ArrayList<>();
		for (ResourceDB.ResourceFacetDB f : r.getFacets())
		    ef.add(f.export());
		rd.setFacets(ef);
		if ( r.getData() != null )
		    rd.setData(r.getData());
		rv.add(rd);
		r.close();
	    }
	    res = null;
	    sc.close();
	    log.info("viewResources for " + uid + " " + type + " " +
		    regex + " succeeded");
	    return rv.toArray(new ResourceDescription[0]);
	}
	catch (DeterFault df) {
	    log.error("viewResources for " + uid + " " + type + " " + regex +
		    " failed: " + df);
	    if ( res != null)
		for ( ResourceDB r : res)
		    if ( r != null ) r.forceClose();
	    if (sc != null ) sc.forceClose();
	    throw df;
	}
    }
}
