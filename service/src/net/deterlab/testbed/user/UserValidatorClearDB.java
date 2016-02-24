package net.deterlab.testbed.user;

import java.util.Date;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.UserChallenge;
import net.deterlab.testbed.db.SharedConnection;

/**
 * A class that validates users using a clear password.
 * @author the DETER Team
 * @version 1.0
 */
public class UserValidatorClearDB extends UserValidatorDB {
    /** Maximum number of outstanding requests */
    private static final int maxOut = 5;

    /**
     * Make a UserValidatorClearDB
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorClearDB() throws DeterFault {
	super();
    }

    /**
     * Make a UserValidatorClear that shares a DB connection
     * @param sc the shared connection
     * @throws DeterFault if the DB connection fails
     */
    public UserValidatorClearDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Return the name of this class (the type in a request)
     * @return the name of this class (the type in a request)
     */
    public String getName() { return "clear"; }

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
	    uc = new UserChallengeDB(u.getUid(), getName(), (byte[]) null,
		    new Date(System.currentTimeMillis() + 120000),
		    getSharedConnection());
	    uc.save();
	    rv = uc.export();
	    uc.close();
	}
	catch (DeterFault df) {
	    if ( uc != null) uc.forceClose();
	    throw df;
	}

	return rv;
    }

    /**
     * Return the uid validated or null
     * @param response the user-submitted response
     * @param uc the challenge being responded to
     * @return the uid validated 
     * @throws DeterFault if validation fails.
     */
    public String validateChallenge(byte[] response, UserChallengeDB uc) 
	    throws DeterFault {
	UserDB u = null;
	String rv = null;

	try {
	    PasswordHash ph = null;
	    u = new UserDB(uc.getUid(), getSharedConnection());

	    // Now we have all the information we need from the challenge.
	    // Remove it so no one else can guess at it.
	    clearID(uc.getChallengeID());

	    clearExpired();
	    u.load();
	    if ( (ph = u.getPasswordHash()) == null )
		throw new DeterFault(DeterFault.internal,
			"User has no password!? " + u.getUid());


	    if ( ph.hashAndCompare(response)) rv = u.getUid();
	    else rv = null;
	    u.close();
	}
	catch (DeterFault df) {
	    if ( u != null ) u.forceClose();
	    throw df;
	}
	return rv;
    }
}
