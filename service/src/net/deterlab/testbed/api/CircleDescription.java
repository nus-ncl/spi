package net.deterlab.testbed.api;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * The Description of a Circle, returned from viewCircles.  This provides a
 * summary of the circle membership and owner.
 * @author the DETER Team
 * @version 1.1
 */
public class CircleDescription extends ApiObject {
    /** The target of the request */
    protected String circleid;
    /** Owner of the Circle */
    protected String owner;
    /** The members of the circle*/
    protected ArrayList<Member> members;

    /**
     * Create an empty description
     */
    public CircleDescription() { 
	circleid = null;
	owner = null;
	members = new ArrayList<Member>();
    }
    /**
     * Create and initialize a result.
     * @param n the name of the attribute
     * @param o the name of the owner
     */
    public CircleDescription(String n, String o) {
	circleid = n;
	owner = o;
	members = new ArrayList<Member>();
    }

    /**
     * Return the attribute name
     * @return the attribute name
     */
    public String getCircleId() { return circleid; }
    /**
     * Set the attribute name
     * @param n the new attribute name
     */
    public void setCircleId(String n) { circleid = n; }
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
