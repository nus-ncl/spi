package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * The Description of a Experiment as reported by viewExperiments.  The
 * representation includes the experiment ID, owner, access control lists
 * (ACLs), aspects, and a representation of th epermissions held by the user
 * calling viewExperiments.  If viewExperiments is invoked with the listOnly
 * parameter true, the aspects will not contain data members.
 * <p>
 * Experiment ACLs are represented as collections of
 * <a href="AccessMember.html">AccessMember</a>s.
 * <p>
 * Experiment aspects are represented as collections of
 * <a href="ExperimentAspect.html">ExperimentAspect</a>s.
 *
 * @author the DETER Team
 * @version 1.1
 * @see AccessMember
 * @see ExperimentAspect
 */
public class ExperimentDescription extends ApiObject {
    /** The target of the request */
    protected String experimentid;
    /** Owner of the Experiment */
    protected String owner;
    /** Access control information */
    protected ArrayList<AccessMember> acl;
    /** Aspects of the experiment */
    protected ArrayList<ExperimentAspect> aspects;
    /** User permissions */
    protected Set<String> perms;

    /**
     * Create an empty description
     */
    public ExperimentDescription() { 
	experimentid = null;
	owner = null;
	acl = new ArrayList<AccessMember>();
	aspects = new ArrayList<ExperimentAspect>();
	perms = new HashSet<String>();
    }
    /**
     * Create and initialize a result.
     * @param n the name of the experiment
     * @param o the name of the owner
     */
    public ExperimentDescription(String n, String o) {
	this();
	experimentid = n;
	owner = o;
    }

    /**
     * Return the experiment name
     * @return the experiment name
     */
    public String getExperimentId() { return experimentid; }
    /**
     * Set the experiment name
     * @param n the new experiment name
     */
    public void setExperimentId(String n) { experimentid = n; }
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
     * @see AccessMember
     */
    public AccessMember[] getACL() {
	return acl.toArray(new AccessMember[0]);
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
     * Return aspects
     * @return aspects
     * @see ExperimentAspect
     */
    public ExperimentAspect[] getAspects() {
	return aspects.toArray(new ExperimentAspect[0]);
    }
    /**
     * Set the aspects
     * @param m the aspects
     * @see ExperimentAspect
     */
    public void setAspects(List<ExperimentAspect> m) {
	aspects = new ArrayList<ExperimentAspect>(m);
    }
    /**
     * Set the aspects
     * @param m the aspects
     * @see ExperimentAspect
     */
    public void setAspects(ExperimentAspect[] m) {
	aspects = new ArrayList<ExperimentAspect>(Arrays.asList(m));
    }
    /**
     * Add a aspect
     * @param m the aspect to add
     * @see ExperimentAspect
     */
    public void addAspect(ExperimentAspect m) {
	aspects.add(m);
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
