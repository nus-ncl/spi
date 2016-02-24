package net.deterlab.testbed.user;

import java.security.SecureRandom;

import org.apache.commons.codec.digest.Crypt;

/**
 * A PasswordHash that understands the crypt/unix style of password hashing.
 * @author DETER team
 * @version 1.1
 */
public class CryptPasswordHash extends PasswordHash {
    static private String type = "crypt";
    /* Register with the PasswordHash generator */
    static {
	try {
	    PasswordHash.registerHash(type, CryptPasswordHash.class);
	}
	catch (Exception e) {
	    System.err.println("Trouble registering " + e);
	}
    }

    /**
     * Valid characters for a crypt salt
     */
    protected static final char[] saltChars = new char[] {
	'.','/','0','1','2','3','4','5','6','7','8','9','A','B','C','D','E',
	'F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V',
	'W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m',
	'n','o','p','q','r','s','t','u','v','w','x','y','z',
    };
    /**
     * Constructor
     * @param h the hash value
     */
    public CryptPasswordHash(String h) { super(h); }

    /**
     * Return the type of this hash
     * @return the type of this hash
     */
    public String getType() { return type; }

    /**
     * Hash the input and set the value to the hash.  If there's an existing
     * hash, use it to get the underlying hash type to be the same.  Otherwise
     * completely default everything.
     * @param h the new unhashed string
     */
    public void hashAndSet(String h) {
	hashAndSet(h.getBytes());
    }

    /**
     * Generate a salt string, 8 valid salt characters.
     * @return the string
     */
    protected String genSalt() {
	final int saltLen = 8;
	SecureRandom rng = new SecureRandom();
	char[] sc = new char[saltLen];
	byte[] b = new byte[saltLen];

	rng.nextBytes(b);
	for ( int i =0; i < saltLen; i++ ) {
	    int idx = (int) b[i];
	    sc[i] = saltChars[Math.abs(idx) % saltChars.length];
	}
	return new String(sc);
    }


    /**
     * Hash the input and set the value to the hash.  If there's an existing
     * hash, use it to get the underlying hash type to be the same.  Otherwise
     * completely default everything.
     * @param h the new unhashed byte array
     */
    public void hashAndSet(byte[] h) {
	String v = getValue();
	if ( v != null ) 
	    v = v.substring(0,3) + genSalt() +"$";
	setValue(Crypt.crypt(h, v));
    }

    /**
     * Hash the input and compare to the current hash.   Note that the hashing
     * uses the same salt here.
     * @param h the new unhashed string
     * @return true if the input hashes to the current value of this object
     */
    public boolean hashAndCompare(String h) {
	return hashAndCompare(h.getBytes());
    }

    /**
     * Hash the input and compare to the current hash.   Note that the hashing
     * uses the same salt here.
     * @param h the new unhashed string
     * @return true if the input hashes to the current value of this object
     */
    public boolean hashAndCompare(byte[] h) {
	String s = Crypt.crypt(h, getValue());

	return s.equals(getValue());
    }
}
