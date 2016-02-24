package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * The description of a library. 
 * The Description of a Library as reported by viewLibraries.  The
 * representation includes the library ID, owner, access control lists (ACLs),
 * experiment IDs, and a representation of the permissions held by the user
 * calling viewLibraries.
 * <p>
 * Library ACLs are represented as collections of
 * <a href="AccessMember.html">AccessMember</a> objects.
 *
 * @author the DETER Team
 * @version 1.1
 */
public class LibraryDescription extends ApiObject {
    /** The identifier of the library*/
    protected String libid;
    /** Owner of the library */
    protected String owner;
    /** Access control information */
    protected ArrayList<AccessMember> acl;
    /** Experiments in the library */
    protected ArrayList<String> eids;
    /** User permissions */
    protected Set<String> perms;

    /**
     * Create an empty description
     */
    public LibraryDescription() { 
	libid = null;
	owner = null;
	acl = new ArrayList<AccessMember>();
	eids = new ArrayList<String>();
	perms = new HashSet<String>();
    }
    /**
     * Create and initialize a result.
     * @param n the name of the library
     * @param o the name of the owner
     */
    public LibraryDescription(String n, String o) {
	this();
	libid = n;
	owner = o;
    }

    /**
     * Return the library name
     * @return the library name
     */
    public String getLibraryId() { return libid; }
    /**
     * Set the library name
     * @param n the new library name
     */
    public void setLibraryId(String n) { libid = n; }
    /**
     * Return the owner
     * @return the owner
     */
    public String getOwner() { return owner; }
    /**
     * Set the owner
     * @param o the new owner
     */
    public void setOwner(String o) { owner = o; }
    /**
     * Return ACL entries
     * @return ACL entries
     */
    public AccessMember[] getACL() {
	return acl.toArray(new AccessMember[0]);
    }
    /**
     * Set the ACL entries
     * @param m the ACL entries
     */
    public void setACL(List<AccessMember> m) { 
	acl = new ArrayList<AccessMember>(m);
    }
    /**
     * Set the ACL entries
     * @param m the ACL entries
     */
    public void setACL(AccessMember[] m) { 
	acl = new ArrayList<AccessMember>(Arrays.asList(m));
    }
    /**
     * Add an ACL entry
     * @param m the ACL entry to add
     */
    public void addAccess(AccessMember m) {
	acl.add(m);
    }
    /**
     * Return aspects
     * @return aspects
     */
    public String[] getExperiments() {
	return eids.toArray(new String[0]);
    }
    /**
     * Set the experiment IDs
     * @param e the experiment IDs
     */
    public void setExperiments(List<String> e) {
	eids = new ArrayList<String>(e);
    }
    /**
     * Set the experiment IDs
     * @param e the experiment IDs
     */
    public void setExperiments(String[] e) {
	eids = new ArrayList<String>(Arrays.asList(e));
    }
    /**
     * Add an eid
     * @param e the eid to add
     */
    public void addExperiment(String e) {
	eids.add(e);
    }

    /**
     * Return this user's permissions.
     * @return this user's permissions.
     */
    public String[] getPerms() { return perms.toArray(new String[0]); }

    /**
     * Set this users permissions
     * @param p the new permissions
     */
    public void setPerms(String[] p) {
	perms.clear();
	for ( String perm: p)
	    perms.add(perm);
    }

    /**
     * Set this user's permissions.
     * @param p the new permissions
     */
    public void setPerms(Collection<String> p) {
	perms.clear();
	perms.addAll(p);
    }
}
