package net.deterlab.testbed.policy;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import net.deterlab.abac.ABACException;
import net.deterlab.abac.Context;
import net.deterlab.abac.Credential;
import net.deterlab.abac.GENICredentialv1_1;
import net.deterlab.abac.Identity;
import net.deterlab.abac.Role;
import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

public class Credentials {
    /** My identity */
    static Identity myId = null;
    /** My X509 cert */
    static X509Certificate myCert = null;
    /** My private key */
    static PrivateKey myKey = null;
    /** My public key */
    static PublicKey myPubkey = null;

    /**
     * Construct a Credentials object.
     * @throws DeterFault if we cannot intialize identities needed to create
     * credentials.
     */
    public Credentials() throws DeterFault { getMyId(); }

    protected void getMyId() throws DeterFault {
	if ( myId != null ) return;

	DeterFault df = null;
	try {
	    KeyStore ks = KeyStore.getInstance("JKS");
	    KeyStore.PrivateKeyEntry pke = null;
	    Config config = new Config();

	    ks.load(new FileInputStream(config.getKeystore()), 
		    config.getKeystorePassword());
	    pke = (KeyStore.PrivateKeyEntry) ks.getEntry("tomcat", 
		new KeyStore.PasswordProtection(config.getKeystorePassword()));
	    if ( pke == null )
		throw new DeterFault(DeterFault.internal,
			"Cannot read private key: null pointer from keystore");
	    myCert = (X509Certificate) pke.getCertificate();
	    myPubkey = myCert.getPublicKey();
	    myKey = pke.getPrivateKey();
	    myId = new Identity(myCert);
	    myId.setKeyPair(new KeyPair(myPubkey, myKey));
	    return;
	}
	catch (ClassCastException e) {
	    df = new DeterFault(DeterFault.internal,
		    "Keystore contains something unexpected: " + 
		    e.getMessage());
	}
	catch (IOException e) {
	    df = new DeterFault(DeterFault.internal,
		    "Cannot read keystore!?: " + e.getMessage());
	}
	catch (GeneralSecurityException e) {
	    df = new DeterFault(DeterFault.internal, 
		    "Security problem loading keys from keystore: " + 
		    e.getMessage());
	}
	catch (ABACException e) {
	    df = new DeterFault(DeterFault.internal, 
		    "Could not generate Identity: " + e.getMessage());
	}

	/*if ( df == null ) 
	    df = new DeterFault(DeterFault.internal, 
		    "GetMyId: how did I get here?");*/
	throw df;
    }

    /**
     * Create a new ABAC Identity (X.509 identity certificate), 
     * signed by the service provider. The Identity is valid for 10 years.
     * @param uid the uid to use as the identity's common name.
     * @return the Identity
     * @throws DeterFault if anything goes wrong.
     */
    public Identity generateIdentity(String uid) 
	    throws DeterFault {

	try {
	    getMyId();
	    return new Identity(uid, 10 * 365 * 24 * 3600, myCert, myKey);
	}
	catch (ABACException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Could not generate Identity: " + e.getMessage());
	}
    }

    /**
     * Convert the given ABAC Identity into a PEM-encoded byte array.
     * @param id the Identity to convert
     * @return the byte array
     * @throws DeterFault if something goes badly wrong with the IO.
     */
    public byte[] identityToBytes(Identity id) throws DeterFault {
	ByteArrayOutputStream bs = new ByteArrayOutputStream();

	try {
	    id.write(bs);
	    if (id.getKeyPair() != null ) 
		id.writePrivateKey(bs);
	}
	catch (IOException e ) {
	    throw new DeterFault(DeterFault.internal, 
		    "Error writing identity to ByteStream?! " + e.getMessage());
	}
	return bs.toByteArray();
    }

    /**
     * Generate a credential assigning attributes from this service
     * @param head the attribute assigned
     * @param tail the prerequisite attributes
     * @return the credential
     * @throws DeterFault on error
     */
    public Credential makeCredential(String head, Collection<String> tail) 
	throws DeterFault {

	if ( head == null || tail==null || tail.size() == 0) return null;

	//try {
	    getMyId();
	    StringBuilder ts = new StringBuilder();

	    for (String t : tail ) {
		if (ts.length() > 0 ) ts.append(" & ");
		ts.append(scopeRoleString(t));
	    }

	    Context c = new Context();
	    Credential cred = c.newCredential(
		    new Role(myId.getKeyID()+ "." + head), 
		    new Role(ts.toString()));

	    if ( cred instanceof GENICredentialv1_1) {
		GENICredentialv1_1 gcred = (GENICredentialv1_1) cred; 
		gcred.getMapping().addNickname(myId.getKeyID(), "DETER");
	    }

	    //cred.make_cert(myId);
	    return cred;
	/*}
	catch (ABACException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Error creating credential: " + e);
	}*/
    }

    /**
     * Generate a credential assigning attributes from this service
     * @param head the attribute assigned
     * @param tail the prerequisite attribute
     * @return the credential
     * @throws DeterFault on error
     */
    public Credential makeCredential(String head, String tail) 
	throws DeterFault {

	return makeCredential(head, Arrays.asList(new String [] { tail }));
    }
    /**
     * Generate a credential assigning an attribute from this service to an
     * Identity
     * @param head the attribute assigned
     * @param tail the principal
     * @return the credential
     * @throws DeterFault on error
     */
    public Credential makeCredential(String head, Identity tail) 
	throws DeterFault {

	if ( head == null || tail==null ) return null;
	return makeCredentialKeyID(head, tail.getKeyID());
    }

    /**
     * Generate a credential assigning an attribute from this service to a
     * keyid (given as a string)
     * @param head the attribute assigned
     * @param tail the principal
     * @return the credential
     * @throws DeterFault on error
     */
    public Credential makeCredentialKeyID(String head, String tail)
	throws DeterFault {

	if ( head == null || tail==null ) return null;

	//try {
	    getMyId();
	    Context c = new Context();
	    Credential cred = c.newCredential(
		    new Role(scopeRoleString(head)), 
		    new Role(tail));

	    //cred.make_cert(myId);
	    return cred;
	/*}
	catch (ABACException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Error creating credential: " + e);
	}*/
    }

    /**
     * Scope a role to this service.
     * @param r the string part of the role
     * @return the role with the service keyid prepended
     * @throws DeterFault on error loading the ID
     */
    public String scopeRoleString(String r) throws DeterFault {
	getMyId();
	StringBuilder rs = new StringBuilder(myId.getKeyID());
	rs.append(".");
	rs.append(r);
	return rs.toString();
    }
}
