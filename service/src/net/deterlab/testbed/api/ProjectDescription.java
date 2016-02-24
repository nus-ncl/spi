package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * The Description of a Project, returned from viewProjects.  This provides a
 * summary of the project membership, approval status, and owner.
 * @author the DETER Team
 * @version 1.1
 */
public class ProjectDescription extends ApiObject {
    /** The target of the request */
    protected String projectid;
    /** Onwer of the Project*/
    protected String owner;
    /** true if the project is approved */
    protected boolean approved;
    /** The members of the project*/
    protected ArrayList<Member> members;

    /**
     * Create an empty description
     */
    public ProjectDescription() { 
	projectid = null;
	owner = null;
	approved = false;
	members = new ArrayList<Member>();
    }
    /**
     * Create and initialize a result.
     * @param n the name of the attribute
     * @param o the name of the owner
     * @param a true if approved
     */
    public ProjectDescription(String n, String o, boolean a) {
	projectid = n;
	owner = o;
	approved = a;
	members = new ArrayList<Member>();
    }

    /**
     * Return the attribute name
     * @return the attribute name
     */
    public String getProjectId() { return projectid; }
    /**
     * Set the attribute name
     * @param n the new attribute name
     */
    public void setProjectId(String n) { projectid = n; }
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
     * Return true if approved
     * @return true if approved
     */
    public boolean getApproved() { return approved; }
    /**
     * Set the approval status
     * @param a the new approval status
     */
    public void setApproved(boolean a) { approved = a; }
    /**
     * Return members
     * @return members
     * @see Member
     */
    public Member[] getMembers() { return members.toArray(new Member[0]); }
    /**
     * Set the members
     * @param m the members
     * @see Member
     */
    public void setMembers(List<Member> m) { 
	members = new ArrayList<Member>(m);
    }
    /**
     * Set the members
     * @param m the members
     * @see Member
     */
    public void setMembers(Member[] m) { 
	members = new ArrayList<Member>(Arrays.asList(m));
    }
    /**
     * Add a member
     * @param m the member to add
     * @see Member
     */
    public void addMember(Member m) {
	members.add(m);
    }
}
