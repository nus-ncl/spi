package net.deterlab.testbed.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.GeneralSecurityException;

import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import net.deterlab.abac.Identity;
import net.deterlab.abac.ABACException;

import net.deterlab.testbed.api.UserChallenge;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Request a password change from DETER.  
 * @author DETER team
 * @version 1.0
 */
public class PasswordReset extends Utility {
    /**
     * Exit with a usage message
     */
    static public void usage() {
	fatal("Usage: PasswordReset user url");
    }

    /**
     * Login as the userid in the first argument.
     * @param args the command line arguments
     */
    static public void main(String[] args) {

	if ( args.length < 1) usage();
	loadTrust();
	String user = args[0];
	String url = args.length > 1 ? args[1] : "http://www.isi.edu?cookie=";
	// Carry out the challenge
	try {
	    UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	    UsersStub.RequestPasswordReset req = 
		new UsersStub.RequestPasswordReset();

	    req.setUid(user);
	    req.setUrlPrefix(url);
	    stub.requestPasswordReset(req);
	} catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
