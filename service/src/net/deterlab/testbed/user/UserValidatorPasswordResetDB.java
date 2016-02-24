package net.deterlab.testbed.user;

import java.util.Date;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.UserChallenge;

import net.deterlab.testbed.db.SharedConnection;

import net.deterlab.testbed.user.UserChallengeDB;

/**
 * A validator used only to issue and remove PasswordReset challenges.  These
 * challenges are valid just because they exist, so the validateChallenge
 * method is mostly for cleanup.
 * @author the DETER Team
 * @version 1.0
 */
public class UserValidatorPasswordResetDB extends UserValidatorDB {
    /** Maximum number of outstanding requests */
    private static final int maxOut = 5;

    /**
     * Make a UserValidatorClearDB
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorPasswordResetDB() throws DeterFault {
	super();
    }

    /**
     * Make a UserValidatorClearDB that shares a DB connection
     * @param sc the shared connection
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorPasswordResetDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Return the name of this class (the type in a request)
     * @return the name of this class (the type in a request)
     */
    public String getName() { return "PasswordReset"; }

    /**
     * Return the challenge to issue this user.  As a side effect, put the
     * challenge into the database.
     * @param u the loaded user of the user to validate
     * @return the challenge to issue
     * @throws DeterFault if the challenge cannot be issued (DB problems or too
     * many outstanding challenges)
     */
    public UserChallenge issueChallenge(UserDB u)
	    throws DeterFault {
	UserChallengeDB uc = null;
	UserChallenge rv = null;

	clearExpired();
	if ( countOpenChallenges(u.getUid()) >= maxOut) 
	    throw new DeterFault(DeterFault.request, 
		    "Too many open challenges for " + u.getUid());

	try {
	    uc = new UserChallengeDB(u.getUid(), getName(),
		    (byte[]) null,
		    new Date(System.currentTimeMillis() + 2L * 3600L * 1000L),
		    getSharedConnection());
	    uc.save();
	    rv = uc.export();
	    uc.close();
	}
	catch (DeterFault df) {
	    if ( uc != null ) uc.forceClose();
	    throw df;
	}
	return rv;
    }

    /**
     * Returns true if the challenge exists and returns the UID.  Removes the
     * challenge from the DB.
     * @param response the user-submitted response (null)
     * @param uc the challenge being responded to
     * @return the uid validated 
     * @throws DeterFault if validation fails.
     */
    public String validateChallenge(byte[] response, UserChallengeDB uc) 
	    throws DeterFault {

	String uid = uc.getUid();
	// Now we have all the information we need from the challenge.  
	// Remove it so no one else can guess at it.
	clearExpired();
	clearID(uc.getChallengeID());

	return uid;
    }
}
