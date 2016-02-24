package net.deterlab.testbed.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

/**
 * A utility to insert the default user profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class SetAllUserEmail extends Utility {
    /**
     * Fail with a usage message
     */
    static public void usage() {
	fatal("SetAllUUserEmail address");
    }
    /**
     * Build a deter database from an exisyting database.
     * @param args are the users to add to the admin project
     */
    static public void main(String[] args) {
	Connection c =null;

	if ( args.length != 1) 
	    usage();
	try {
	    Config config = new Config();
	    // Just change everyone's e-mail in one fell swoop.
	    c = DriverManager.getConnection(config.getDeterDbUrl());
	    PreparedStatement p = c.prepareStatement(
		    "UPDATE userattributevalue SET value=? WHERE aidx="+
		    "(SELECT idx FROM userattribute WHERE name='email')");
	    p.setString(1, args[0]);
	    p.executeUpdate();

	}
	catch (DeterFault df) {
	    fatal(df.getDetailMessage());
	} 
	catch (SQLException e) {
	    fatal(e.getMessage());
	}
	finally {
	    try {
		if ( c != null ) c.close();
	    } catch (SQLException ignored) { }
	}
    }
}
