package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A resource description as passed through the SPI calls. A description
 * identifies the physical or virtual resources that the testbed assigns to
 * experiments.  This includes computers, switches, VMs, accounts, keys, and
 * other virtual and physical resources.
 * @author the DETER Team
 * @version 1.0
 */
public class ResourceDescription extends ApiObject {
    /** The name of the resource */
    private String name;
    /** The type of the aspect */
    private String type;
    /** The description */
    private String description;
    /** Is the resource persistent */
    private boolean persist;
    /** The optional data */
    private byte[] data;
    /** Facets of this resource */
    private List<ResourceFacet> facets;
    /** Tags on this facet */
    private List<ResourceTag> tags;
    /** Access control information */
    private ArrayList<AccessMember> acl;
    /** User permissions */
    private Set<String> perms;

    /**
     * Create an empty description
     */
    public ResourceDescription() { 
	name = null;
	type = null;
	description = null;
	persist = false;
	facets = new ArrayList<>();
	tags = new ArrayList<>();
	perms = new TreeSet<>();
    }
    /**
     * Return the name
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Set the name
     * @param n the new name
     */
    public void setName(String n) { name = n; }
    /**
     * Return the type
     * @return the type
     */
    public String getType() { return type; }
    /**
     * Set the type
     * @param t the new type
     */
    public void setType(String t) { type = t; }
    /**
     * Return the description
     * @return the description
     */
    public String getDescription() { return description; }
    /**
     * Set the description
     * @param d the new description
     */
    public void setDescription(String d) { description = d; }
    /**
     * Return the persistence
     * @return the persistence
     */
    public boolean getPersist() { return persist; }
    /**
     * Set the persistence
     * @param p the new persistence value
     */
    public void setPersist(boolean p) { persist = p; }
    /**
     * Return the data
     * @return the data
     */
    public byte[] getData() { return data; }
    /**
     * Set the data
     * @param d the new data
     */
    public void setData(byte[] d) { data = d; }
    /**
     * Return the tags as an array
     * @return the tags as an array
     */
    public ResourceTag[] getTags() {
	ResourceTag[] empty = new ResourceTag[0];

	return (tags != null) ? tags.toArray(empty) : empty; }

    /**
     * Set tags from a tag array
     * @param t the new tags
     */
    public void setTags(ResourceTag[] t) {
	tags.clear();
	for (ResourceTag r : t)
	    tags.add(r);
    }

    /**
     * Set tags from a tag collection
     * @param t the new tags
     */
    public void setTags(Collection<ResourceTag> t) {
	tags.clear();
	tags.addAll(t);
    }
    /**
     * Return the facets as an array
     * @return the facets as an array
     */
    public ResourceFacet[] getFacets() {
	ResourceFacet[] empty = new ResourceFacet[0];

	return (facets != null) ?  facets.toArray(empty) : empty;
    }

    /**
     * Set facets from a facets array
     * @param f the new facets
     */
    public void setFacets(ResourceFacet[] f) {
	facets.clear();
	for (ResourceFacet r : f)
	    facets.add(r);
    }

    /**
     * Set facets from a facets collection
     * @param f the new facets
     */
    public void setFacets(Collection<ResourceFacet> f) {
	facets.clear();
	facets.addAll(f);
    }
    /**
     * Return ACL entries
     * @return ACL entries
     * @see AccessMember
     */
    public AccessMember[] getACL() {
	AccessMember[] empty = new AccessMember[0];
	return ( acl != null ) ?  acl.toArray(empty): empty;
    }
    /**
     * Set the ACL entries
     * @param m the ACL entries
     * @see AccessMember
     */
    public void setACL(List<AccessMember> m) {
	acl = new ArrayList<AccessMember>(m);
    }
    /**
     * Set the ACL entries
     * @param m the ACL entries
     * @see AccessMember
     */
    public void setACL(AccessMember[] m) {
	acl = new ArrayList<AccessMember>(Arrays.asList(m));
    }
    /**
     * Add an ACL entry
     * @param m the ACL entry to add
     * @see AccessMember
     */
    public void addAccess(AccessMember m) {
	acl.add(m);
    }

    /**
     * Return this user's permissions.
     * @return this user's permissions.
     */
    public String[] getPerms() { return perms.toArray(new String[0]); }

    /**
     * Set this user's permissions
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
