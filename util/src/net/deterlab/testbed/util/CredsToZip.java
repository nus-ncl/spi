package net.deterlab.testbed.util;

import java.io.File;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.deterlab.abac.Context;
import net.deterlab.abac.Credential;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

/**
 * A utility to insert the default circle profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class CredsToZip extends Utility {

    /**
     * Pull all the credentials in the database into an ABAC context and then
     * save them as a zip for crudge
     * @param args are ignored
     */
    static public void main(String[] args) {
	Connection c =null;
	Context ctxt = new Context();

	try {
	    Config config = new Config();
	    c = DriverManager.getConnection(config.getDeterDbUrl());
	    PreparedStatement p = c.prepareStatement(
		    "SELECT cred FROM credentials");

	    ResultSet r = p.executeQuery();

	    while (r.next())
		if ( ctxt.load_attribute_chunk(r.getString(1)) != 0 ) 
		    fatal("Could not load " + r.getString(1));
	    try {
		ctxt.write_zip(
			new File(args.length > 1 ? args[1] : "./test.zip"),
			false, false);
	    }
	    catch (IOException e) {
		fatal("cannot write zip");
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
