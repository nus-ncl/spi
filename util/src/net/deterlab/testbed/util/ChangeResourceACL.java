package net.deterlab.testbed.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;

import net.deterlab.testbed.util.option.Option;

/**
 * Change access control for an experiment.
 * @author DETER Team
 * @version 1.0
 */
public class ChangeResourceACL extends Utility {

    /** a circle specification from the command line: circlename(PERMS).  The
     * circle name is further parsed by validCircle below */
    static private Pattern splitCircle = Pattern.compile("(.*)\\((.*)\\)");

    /** valid cirle names are owner:name */
    static private Pattern validCircle = Pattern.compile("^[^:]+:[^:]+$");

    /**
     * Parse a string of the form given by the splitCircle regexp into parts
     * and build an AccessMember object from it.  If there are problems, throw
     * an OptionException.
     * @param s the circle specification string
     * @return the constructed ResourcesStub.AccessMember
     * @throws OptionException on errors
     */
    static ResourcesStub.AccessMember parseCircle(String s)
	    throws Option.OptionException {
	Matcher m = splitCircle.matcher(s);
	ResourcesStub.AccessMember rv = new ResourcesStub.AccessMember();

	if (!m.matches()) throw new Option.OptionException("Bad circle " + s);
	String circleId = m.group(1);
	Matcher valid = validCircle.matcher(circleId);

	if ( !valid.matches())
	    throw new Option.OptionException("Bad circleId " + s);

	String[] perms = m.group(2).split("\\s*,\\s*");

	if ( perms.length == 1 && perms[0].isEmpty())
	    perms = new String[0];

	rv.setCircleId(circleId);
	rv.setPermissions(perms);
	return rv;
    }

    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("ChangeResourceACL name circleid(perms) [... ]");
    }
	
    /**
     * Parse the circles and  then the permissions.  Call changeResourceACL.
     * @param args the experiment id and circles
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    if ( args.length < 2) usage(null);

	    String name = args[0];
	    ResourcesStub.AccessMember[] am =
		new ResourcesStub.AccessMember[args.length-1];

	    for ( int i = 1; i < args.length; i++) 
		am[i-1] = parseCircle(args[i]);

	    ResourcesStub stub =
		new ResourcesStub(getServiceUrl("Resources"));
	    ResourcesStub.ChangeResourceACL req = 
		new ResourcesStub.ChangeResourceACL();

	    req.setName(name);
	    req.setAcl(am);

	    ResourcesStub.ChangeResourceACLResponse resp = 
		stub.changeResourceACL(req);
	    ResourcesStub.ChangeResult[] res = resp.get_return();

	    for ( ResourcesStub.ChangeResult r : res) {
		System.out.print("Change for " + r.getName());

		if ( r.getSuccess()) System.out.println(" succeeded");
		else System.out.println(" failed: " + r.getReason());
	    }

	}
	catch (Option.OptionException e) {
	    usage("Option parsing exception: " + e);
	}
	catch (ResourcesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
