package net.deterlab.testbed.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.NumberOption;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * View a list of libraries
 * @author DETER Team
 * @version 1.0
 */
public class ViewLibraries extends Utility {


    /**
     * Print a usage message and exit
     */
    static public void usage() {
	fatal("Usage: ViewLibraries user [regex] " +
		"[--count count] [--offset offset]");
    }


    /**
     * General(ish) command line interface to viewLibraries.  Parameters are
     * the user to query, regexp to match libraries against.
     * @param args the user regexp and params
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();

	    NumberOption countOption = new NumberOption("count");
	    NumberOption offsetOption = new NumberOption("offset");
	    Option[] opts = new Option[] { countOption, offsetOption };
	    ArrayList<String> pos = new ArrayList<String>();

	    Option.parseArgs(args, opts, pos, 0);

	    if ( pos.size() < 1)
		throw new Option.OptionException(
			"Not enough positional parameters");

	    String uid = pos.get(0);
	    String regex = (pos.size() > 1) ? pos.get(1) : null;
	    Number count = countOption.getValue();
	    Number offset = offsetOption.getValue();

	    LibrariesStub stub =
		new LibrariesStub(getServiceUrl("Libraries"));
	    LibrariesStub.ViewLibraries req = 
		new LibrariesStub.ViewLibraries();

	    req.setUid(uid);
	    req.setRegex(regex);
	    if ( count != null )
		req.setCount(new Integer(count.intValue()));
	    if ( offset != null )
		req.setOffset(new Integer(offset.intValue()));


	    LibrariesStub.ViewLibrariesResponse resp = stub.viewLibraries(req);
	    LibrariesStub.LibraryDescription[] libs = resp.get_return();

	    if (libs  == null ) {
		String errStr = "No libraries found for " + uid;

		if (regex != null) errStr += " regex " + regex;
		fatal(errStr);
	    }

	    for (LibrariesStub.LibraryDescription lib : libs) {
		LibrariesStub.AccessMember[] acl = lib.getACL();
		String exps[] = lib.getExperiments();

		if (acl == null) acl = new LibrariesStub.AccessMember[0];
		if (exps == null ) exps = new String[0];

		System.out.println("Libraries- " + lib.getLibraryId() +
			"  Owner: " + lib.getOwner());

		System.out.print("\tUser permissions: ");
		boolean first = true;
		for (String p : lib.getPerms())
		    if ( first ) {
			System.out.print(p);
			first = false;
		    }
		    else {
			System.out.print(", " + p );
		    }
		System.out.println();
		System.out.println("\tACL-");
		for (LibrariesStub.AccessMember m : acl)
		    System.out.println("\t\t"+m.getCircleId() + ": " +
			    joinStrings(m.getPermissions(), ", "));
		System.out.println("\tExperiments-");
		for (String e :  exps)
		    System.err.println("\t\t" + e);

	    }
	} catch (Option.OptionException oe) {
	    System.err.println(oe.getMessage());
	    usage();
	} catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
