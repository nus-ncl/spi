package net.deterlab.testbed.policy;

/**
 * Callers of CredentialStoreDB functions specify credential sets using
 * this class.  The class includes the type - a set name, and name the
 * identifier that defines the set in question.  To get the parameters
 * associated with user faber, theParameterizedCredSet would have type
 * "usercreds" and name "faber".
 * @author DETER Team
 * @version 1.1
 */
public class CredentialSet {
    /** The set type */
    private String type;
    /** The parameter */
    private String name;

    /**
     * Create a ParameterizedCredSet
     * @param t the type
     * @param n the name
     */
    public CredentialSet (String t, String n) {
	type = t; name = n;
    }

    /**
     * Return the type
     * @return the type
     */
    public String getType() { return type; }
    /**
     * Set the type.
     * @param t the new type
     */
    public void setType(String t) { type = t; }
    /**
     * Return the name
     * @return the name
     */
    public String getName() { return name; }
    /**
     * Set the name.
     * @param n the new name
     */
    public void setName(String n) { name = n; }

    /**
     * Return a string representation of the set's type/name
     * @return a string representation of the set's type/name
     */
    public String toString() { return type + "/" + name; }
}

