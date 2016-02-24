package net.deterlab.testbed.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.RealizationsDeterFault;
import net.deterlab.testbed.client.RealizationsStub;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.NumberOption;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * View a list of resources
 * @author DETER Team
 * @version 1.0
 */
public class ViewRealizations extends Utility {

    /**
     * Formats for printing data of aspects
     */
    static public enum DataFormats { NONE, HEX, CHARS};

    /**
     * Print a usage message and exit
     */
    static public void usage() {
	fatal("Usage: ViewRealizations " +
		"[--offset offset] [--count count] "+
		"user [regex] ");
    }

    /**
     * General(ish) command line interface to viewRealizations.
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

	    RealizationsStub stub =
		new RealizationsStub(getServiceUrl("Realizations"));
	    RealizationsStub.ViewRealizations req = 
		new RealizationsStub.ViewRealizations();

	    req.setUid(uid);
	    if ( regex != null ) req.setRegex(regex);
	    if ( count != null )
		req.setCount(new Integer(count.intValue()));
	    if ( offset != null )
		req.setOffset(new Integer(offset.intValue()));

	    RealizationsStub.ViewRealizationsResponse resp = 
		stub.viewRealizations(req);

	    RealizationsStub.RealizationDescription[] res = resp.get_return();

	    if (res  == null ) {
		String errStr = "No realizations found for " + uid;

		if (regex != null) errStr += " regex " + regex;
		fatal(errStr);
	    }

	    for (RealizationsStub.RealizationDescription r : res)
		dumpRealizationDescription(r, System.out);
	} catch (Option.OptionException oe) {
	    System.err.println(oe.getMessage());
	    usage();
	} catch (RealizationsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
