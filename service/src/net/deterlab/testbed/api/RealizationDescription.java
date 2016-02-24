package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A realization description as passed through the SPI calls. A description
 * identifies the physical or virtual realizations that the testbed assigns to
 * experiments.  This includes computers, switches, VMs, accounts, keys, and
 * other virtual and physical realizations.
 * @author the DETER Team
 * @version 1.0
 */
public class RealizationDescription extends ApiObject {
    /** The name of the realization */
    private String name;
    /** The circle */
    private String circle;
    /** The experiment */
    private String experiment;
    /** State of the realization */
    private String status;
    /** Facets of this realization */
    private List<RealizationContainment> containment;
    /** Tags on this facet */
    private List<RealizationMap> mapping;
    /** Access control information */
    private ArrayList<AccessMember> acl;
    /** User permissions */
    private Set<String> perms;

    /**
     * Create an empty description
     */
    public RealizationDescription() { 
	name = null;
	circle = null;
	experiment = null;
	status = "Empty";
	containment = new ArrayList<>();
	mapping = new ArrayList<>();
	acl = new ArrayList<>();
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
     * Return the circle
     * @return the circle
     */
    public String getCircle() { return circle; }
    /**
     * Set the circle
     * @param c the new circle
     */
    public void setCircle(String c) { circle = c; }
    /**
     * Return the experiment
     * @return the experiment
     */
    public String getExperiment() { return experiment; }
    /**
     * Set the experiment
     * @param e the new experiment
     */
    public void setExperiment(String e) { experiment = e; }
    
    /**
     * Return the status
     * @return the status
     */
    public String getStatus() { return status; }
    /**
     * Set the status
     * @param s the new status
     */
    public void setStatus(String s) { status = s; }
    
    /**
     * Return the containment as an array
     * @return the containment as an array
     */
    public RealizationContainment[] getContainment() {
	return containment.toArray(new RealizationContainment[0]);
    }

    /**
     * Set containment from a containment array
     * @param con the new containment
     */
    public void setContainment(RealizationContainment[] con) {
	containment.clear();
	for (RealizationContainment c : con)
	    containment.add(c);
    }

    /**
     * Set containment from a containment collection
     * @param con the new containment
     */
    public void setContainment(Collection<RealizationContainment> con) {
	containment.clear();
	containment.addAll(con);
    }

    /**
     * Set containment from a Map 
     * @param m the Map
     */
    public void setContainment(Map<String, Set<String>> m) {
	containment.clear();
	for (Map.Entry<String, Set<String>> e : m.entrySet())
	    for (String inner : e.getValue())
		containment.add(new RealizationContainment(e.getKey(), inner));
    }

    
    /**
     * Return the mapping as an array
     * @return the mapping as an array
     */
    public RealizationMap[] getMapping() {
	return mapping.toArray(new RealizationMap[0]);
    }

    /**
     * Set mapping from a mapping array
     * @param con the new mapping
     */
    public void setMapping(RealizationMap[] con) {
	mapping.clear();
	for (RealizationMap c : con)
	    mapping.add(c);
    }

    /**
     * Set mapping from a mapping collection
     * @param con the new mapping
     */
    public void setMapping(Collection<RealizationMap> con) {
	mapping.clear();
	mapping.addAll(con);
    }

    /**
     * Set mapping from a Map 
     * @param m the Map
     */
    public void setMapping(Map<String, Set<String>> m) {
	mapping.clear();
	for (Map.Entry<String, Set<String>> e : m.entrySet())
	    for (String resName : e.getValue())
		mapping.add(new RealizationMap(e.getKey(), resName));
    }

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
