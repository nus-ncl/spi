package net.deterlab.testbed.user;

import java.lang.reflect.Constructor;

import java.util.Map;
import java.util.HashMap;

/**
 * Abstract base class for a password hash.  This is a hash value stored as a
 * string for use as a password.  The string representation is for easier
 * debugging nad human comprehension. Includes a name and a current value, as
 * well as methods to generate and compare.
 * @author DETER team
 * @version 1.1
 */
public abstract class PasswordHash {
    /** map for generating password hashes */
    static protected 
	Map<String, Constructor<? extends PasswordHash>> generator = 
	    new HashMap<String, Constructor<? extends PasswordHash> >();
    /** The hashed value */
    protected String hash;
    /**
     * Base initializer
     * @param h the hashed value
     */
    public PasswordHash(String h) {
	hash = h;
    }

    /**
     * Return the hashed value
     * @return the hashed value
     */
    public String getValue() { return hash; }
    /**
     * Set the hashed value
     * @param h the new value
     */
    public void setValue(String h) { hash = h; }

    /**
     * Return the type of this hash
     * @return the type of this hash
     */
    public abstract String getType(); 

    /**
     * Hash the input and set the value to the hash
     * @param h the new unhashed string
     */
    public abstract void hashAndSet(String h);

    /**
     * Hash the input and set the value to the hash
     * @param h the new unhashed string(as a byte array)
     */
    public abstract void hashAndSet(byte[] h);
    /**
     * Hash the input and compare to the current hash.  
     * @param h the new unhashed string
     * @return true if the input hashes to the current value of this object
     */
    public abstract boolean hashAndCompare(String h);
    /**
     * Hash the input and compare to the current hash.  
     * @param h the new unhashed string
     * @return true if the input hashes to the current value of this object
     */
    public abstract boolean hashAndCompare(byte[] h);

    /**
     * Register a type of PasswordHash for use by getInstance()
     * @param t the type of hash being registered
     * @param c the class being registered
     * @throws NoSuchMethodException if the class does not have a constructor
     * that takes one String.
     */
    static public void registerHash(String t, Class<? extends PasswordHash> c) 
	    throws NoSuchMethodException {
	Constructor<? extends PasswordHash> con = 
	    c.getConstructor( new Class<?>[] { String.class });
	generator.put(t, con);
    }

    /**
     * If type is known, return a PasswordHash of that type, initialized with
     * hv.
     * @param type the type of PasswordHash
     * @param hv the initial value
     * @return a PasswordHash of type type initialized with hv
     */
    static public PasswordHash getInstance(String type, String hv) {
	if (!generator.containsKey(type) ) return null;

	try {
	    Constructor<? extends PasswordHash> con = generator.get(type);
	    return con.newInstance(hv);
	}
	catch (Exception ignored) {
	    System.err.println("Error " + ignored);
	    return null;
	}
    }
}
