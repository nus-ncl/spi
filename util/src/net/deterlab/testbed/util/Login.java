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

import net.deterlab.testbed.util.gui.LoginDialog;
import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * Log the user in to the DETER API.  Such a login results in an X.509
 * certificate being stored in the default user ID file that other utilities
 * can use.  The main() method pops a dialog up to get the user's password and
 * then completes the login.
 * @author DETER team
 * @version 1.0
 */
public class Login extends Utility {
    /**
     * Exit with a usage message
     */
    static public void usage() {
	fatal("Usage: Login [--pem file] [--keystore store] " +
		"[--keypass pass] [--password pwd] user");
    }

    /**
     * Login as the userid in the first argument.
     * @param args the command line arguments
     */
    static public void main(String[] args) {
	LoginDialog ld = null;
	Identity i = null;
	List<String> users = new ArrayList<String>();
	String uid = null;
	ParamOption pem = new ParamOption("pem");
	ParamOption store = new ParamOption("keystore", getUserIDFilename());
	ParamOption pass = new ParamOption("keystore", getUserIDPassword());
	ParamOption loginOption = new ParamOption("password");

	try {
	    Option.parseArgs(args, new Option[] {
		pem, store, pass, loginOption }, users);
	}
	catch (Option.OptionException e) {
	    usage();
	}

	if ( users.size() != 1 ) 
	    usage();
	uid = users.get(0);

	String pemfile = pem.getValue();
	String keystore = store.getValue();
	String keypass = pass.getValue();
	String passwd = loginOption.getValue();
	char[] pwd = null;

	// Get the user's password
	loadTrust();
	loadID();

	if ( passwd == null ) {
	    ld = new LoginDialog();
	    ld.setVisible(true);
	    pwd = ld.getPassword();

	    if (ld.isCancelled() || pwd.length == 0) {
		ld.dispose();
		return;
	    }
	    ld.dispose();
	    passwd = new String(pwd);
	}

	// Carry out the challenge
	try {
	    i = login(uid, passwd);
	} catch (DeterFault df) {
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (Exception e) {
	    e.printStackTrace();
	    fatal("unexpected exception");
	}

	if (i == null) {
	    if ( pemfile != null )
		warn("No identity returned and --pem specified. " +
			"Try again w/o an identity");
	    return;
	}
	// If an identity came back, store it.
	try {

	    /* Store to pem if we have a pem filename */
	    if ( pemfile != null ) 
		identityToPem(i, new File(pemfile));

	    /* Store to keystore if we have a keystore filename */
	    if (keystore != null ) {
		char[] p = (keypass != null) ? keypass.toCharArray() : null;
		identityToKeyStore(i, new File(keystore), p);
	    }
	}
	catch (IOException e) {
	    fatal("File system error: " +e.getMessage());
	}
	catch (ABACException e ) {
	    fatal("Identity error: " +e.getMessage());
	}
	catch (GeneralSecurityException e) {
	    fatal("Keystore error: " +e.getMessage());
	}
    }
}
