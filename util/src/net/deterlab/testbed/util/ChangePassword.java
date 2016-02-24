package net.deterlab.testbed.util;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.gui.LoginDialog;

import net.deterlab.testbed.util.option.NumberOption;
import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * @author DETER team
 * @version 1.0
 */
public class ChangePassword extends Utility {
    /**
     * Exit with a usage message
     */
    static public void usage() {
	fatal("Usage: ChangePassword --uid|--challenge param ");
    }

    /**
     * Login as the userid in the first argument.
     * @param args the command line arguments
     */
    static public void main(String[] args) {
	ParamOption uid = new ParamOption("uid");
	NumberOption chall = new NumberOption("challenge");
	LoginDialog ld = new LoginDialog();
	String param = null;

	try {
	    Option.parseArgs(args, new Option[] { uid, chall }, null);
	}
	catch (Option.OptionException e) {
	    System.err.println(e.getMessage());
	    usage();
	}

	if ( (uid.getValue() == null && chall.getValue() == null) ||
	    (uid.getValue() != null && chall.getValue() != null) ) 
	    usage();

	loadTrust();
	loadID();
	ld.setVisible(true);
	char[] pwd = ld.getPassword();
	ld.dispose();

	if (pwd == null ) 
	    fatal("No password entered");

	try {
	    UsersStub stub = new UsersStub(getServiceUrl() + "Users");

	    if ( chall.getValue() != null ) {
		UsersStub.ChangePasswordChallenge req = 
		    new UsersStub.ChangePasswordChallenge();
		req.setChallengeID(chall.getValue().longValue());
		req.setNewPass(new String(pwd));

		stub.changePasswordChallenge(req);
	    }
	    else {
		UsersStub.ChangePassword req = new UsersStub.ChangePassword();
		req.setUid(uid.getValue());
		req.setNewPass(new String(pwd));

		stub.changePassword(req);
	    }
	} catch (NumberFormatException e) {
	    fatal(param + " is not a valid long: " + e.getMessage());
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
