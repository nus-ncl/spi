package net.deterlab.testbed.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

/**
 * A utility to insert the default circle profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class EmptyDeterDatabase extends Utility {

    /** The right order to empty the tables */
    static public String[] tables = new String[] {
	"usernotification",
	"notification",
	"userchallenge",
	"circlechallenge",
	"projectchallenge",
	"userattributevalue",
	"circleattributevalue",
	"libraryattributevalue",
	"experimentattributevalue",
	"projectattributevalue",
	"realizationcreds",
	"resourcecreds",
	"systemcreds",
	"logincreds",
	"circlecreds",
	"circlemembercreds",
	"circleownercreds",
	"experimentcreds",
	"librarycreds",
	"projectcreds",
	"projectmembercreds",
	"usercreds",
	"projectusers",
	"keytouser",
	"credentials",
	"libraryperms",
	"librarymembers",
	"experimentperms",
	"experimentaspects",
	"projectusers",
	"projectperms",
	"realizationcontainment",
	"realizationperms",
	"realizationtopology",
	"resourceperms",
	"realizations",
	"facettags",
	"facets",
	"resourcetags",
	"resources",
	"rawprojects",
	"circleperms",
	"circleusers",
	"circles",
	"libraries",
	"experiments",
	"rawusers",
	"userattribute",
	"scopenames",
	"circleattribute",
	"libraryattribute",
	"experimentattribute",
	"projectattribute",
	"permissions",
    };

    /**
     * Empty out the DETER database, this executes the deletes in the correct
     * order.
     * @param args are ignored
     */
    static public void main(String[] args) {
	Connection c =null;

	try {
	    Config config = new Config();
	    c = DriverManager.getConnection(config.getDeterDbUrl());
	    PreparedStatement p = null;

	    for (String t : tables ) {
		try { 
		    p = c.prepareStatement("DELETE FROM " + t );
		    System.out.println(p);
		    p.executeUpdate();
		}
		catch (SQLException e) {
		    warn(e.getMessage());
		}
	    }
	}
	catch (DeterFault df) {
	    fatal("Really!?! " + df);
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
