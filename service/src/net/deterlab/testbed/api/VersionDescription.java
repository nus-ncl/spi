package net.deterlab.testbed.api;

/**
 * The description of the current system's version, returned from getVersion.
 * This gives a version and a patchlevel.  If the getVersion call was made with
 * a client certificate, the key identifier for the certificate's public key is
 * also given.
 *
 * @version 1.1
 * @author DETER Team
 */
public class VersionDescription {
    /** The version */
    protected String version;
    /** The patch level */
    protected String patchLevel;
    /** The caller's key identifier */
    protected String keyid;

    /**
     * Construct an empty description
     */
    public VersionDescription() {
	version = null;
	patchLevel = null;
	keyid = null;
    }

    /**
     * Construct the response.
     * @param v the version number
     * @param pl the patchlevel
     * @param k the key identifier, may be null
     */
    public VersionDescription(String v, String pl, String k) {
	version = v;
	patchLevel = pl;
	keyid = k;
    }

    /**
     * Return the version number.
     * @return the version number
     */
    public String getVersion() { return version;}

    /**
     * Set the version number.
     * @param v the new version number
     */
    public void setVersion(String v) { version = v; }

    /**
     * Return the patch level.
     * @return the patch level
     */
    public String getPatchLevel() { return patchLevel;}

    /**
     * Set the patch level.
     * @param p the new patch level
     */
    public void  setPatchLevel(String p) { patchLevel = p;}

    /**
     * Return the key identitfier.
     * @return the key identitfier
     */
    public String getKeyID() { return keyid;}

    /**
     * Set the key identitfier.
     * @param k the new key identitfier
     */
    public void getKeyID(String k) { keyid = k;}

};
