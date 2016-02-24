package net.deterlab.testbed.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.NumberOption;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * View a list of experiments
 * @author DETER Team
 * @version 1.0
 */
public class ViewExperiments extends Utility {

    /**
     * Formats for printing data of aspects
     */
    static public enum DataFormats { NONE, HEX, CHARS};

    /**
     * Print a usage message and exit
     */
    static public void usage() {
	fatal("Usage: ViewExperiments [--aspect name,type,subtype...] " +
		"[--alldata]\n[--format (hex|chars|none)] " +
		"[--datalim int] [--offset offset] [--count count] "+
		"user [regex]");
    }

    /**
     * Parse an aspect query spec into ExperimentAspects.  The spec is 1-3
     * comma separated names: a name, type, and subtype.  The name may be empty
     * and type or subtype may be omitted.  Subtype cannot be present without
     * type. Subtype may be specified as * to match any subtype.
     * @param q a collection of query specs
     * @return the array of ExperimentAspects
     * @throws an OptionException if a spec is malformed.
     */
    static public ExperimentsStub.ExperimentAspect[]
	parseQueryAspects(Collection<String> q) throws Option.OptionException {
	ArrayList<ExperimentsStub.ExperimentAspect> alist =
	    new ArrayList<ExperimentsStub.ExperimentAspect>();

	for ( String as : q) {
	    ExperimentsStub.ExperimentAspect ea =
		new ExperimentsStub.ExperimentAspect();
	    String[] parts = as.split("\\s*,\\s*");

	    if ( parts.length > 3)
		throw new Option.OptionException("Bad aspect spec " +
			"(1-3 parts required) for " + as);
	    if (!parts[0].isEmpty()) ea.setName(parts[0]);
	    if (parts.length > 1 && !parts[1].isEmpty() )
		ea.setType(parts[1]);
	    if (parts.length > 2) {
		if ( ea.getType() != null)
		    ea.setSubType(parts[2]);
		else
		    throw new Option.OptionException(
			    "Subtype without type for " + as);
	    }
	    alist.add(ea);
	}
	return alist.toArray(new ExperimentsStub.ExperimentAspect[0]);
    }

    /**
     * General(ish) command line interface to viewExperiments.  Parameters are
     * the user to query, regexp to match experiments against.  By default only
     * a list of aspects is returned.  To limit the query to different aspects
     * specify them using --aspect with 1-3 comma-separated aspect identifiers.
     * The identifiers are name, type, and subtype of the aspect.  Subtype is
     * only valid if there is a type, anme or subtype may be omitted.  To
     * output the data of an aspect, specify a format with --format (hex or
     * chars).  --datalim limits the number of bytes printed (default 64),
     * --alldata prints it all (as does datalim 0).
     * @param args the user regexp and params
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();

	    ListOption aspectOption = new ListOption("aspect");
	    ParamOption fmtOption = new ParamOption("format", "NONE");
	    ParamOption libOption = new ParamOption("library");
	    NumberOption limOption = new NumberOption("datalim", 64);
	    NumberOption countOption = new NumberOption("count");
	    NumberOption offsetOption = new NumberOption("offset");
	    BooleanOption allDataOption = new BooleanOption("alldata", false);
	    Option[] opts = new Option[] {
		aspectOption, fmtOption, libOption, limOption, countOption,
		    offsetOption, allDataOption
	    };
	    ArrayList<String> pos = new ArrayList<String>();
	    DataFormats fmt = DataFormats.NONE;
	    int dataLim = 64;

	    Option.parseArgs(args, opts, pos, 0);


	    if ( pos.size() < 1)
		throw new Option.OptionException(
			"Not enough positional parameters");

	    String uid = pos.get(0);
	    String regex = (pos.size() > 1) ? pos.get(1) : null;
	    Number count = countOption.getValue();
	    Number offset = offsetOption.getValue();
	    ExperimentsStub.ExperimentAspect[] aspects =
		parseQueryAspects(aspectOption.getValue());
	    switch (fmtOption.getValue().toUpperCase()) {
		case "HEX":
		    fmt = DataFormats.HEX;
		    break;
		case "CHARS":
		    fmt = DataFormats.CHARS;
		    break;
		case "NONE":
		    fmt = DataFormats.NONE;
		    break;
		default:
		    throw new Option.OptionException("Unknown format: " +
			    fmtOption.getValue());
	    }
	    if ( allDataOption.getValue()) dataLim = 0;
	    else dataLim = limOption.getValue().intValue();

	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.ViewExperiments req = 
		new ExperimentsStub.ViewExperiments();

	    req.setUid(uid);
	    if (libOption.getValue() != null)
		req.setLib(libOption.getValue());
	    req.setRegex(regex);
	    req.setQueryAspects(aspects);
	    req.setListOnly(fmt == DataFormats.NONE);
	    if ( count != null )
		req.setCount(new Integer(count.intValue()));
	    if ( offset != null )
		req.setOffset(new Integer(offset.intValue()));

	    ExperimentsStub.ViewExperimentsResponse resp = 
		stub.viewExperiments(req);

	    ExperimentsStub.ExperimentDescription[] exps = resp.get_return();

	    if (exps  == null ) {
		String errStr = "No experiments found for " + uid;

		if (regex != null) errStr += " regex " + regex;
		fatal(errStr);
	    }

	    for (ExperimentsStub.ExperimentDescription e : exps) {
		ExperimentsStub.ExperimentAspect[] rAspects = e.getAspects();
		ExperimentsStub.AccessMember[] acl =  e.getACL();

		if (rAspects == null )
		    rAspects = new ExperimentsStub.ExperimentAspect[0];
		if (acl == null )
		    acl = new ExperimentsStub.AccessMember[0];

		System.out.println("Experiment- " + e.getExperimentId() +
			"  Owner: " + e.getOwner());
		System.out.print("\tUser permissions: ");
		boolean first = true;
		String [] perms = e.getPerms();

		if (perms == null ) perms = new String[0];
		for (String p : perms)
		    if ( first ) {
			System.out.print(p);
			first = false;
		    }
		    else {
			System.out.print(", " + p );
		    }
		System.out.println();

		System.out.println("\tACL-");
		for (ExperimentsStub.AccessMember m : acl)
		    System.out.println("\t\t"+m.getCircleId() + " " +
			    joinStrings(m.getPermissions(), ", "));

		System.out.println("\tAspects-");
		for (ExperimentsStub.ExperimentAspect m : rAspects) {
		    if ( m.getSubType() != null )
			System.out.println("\t\t"+m.getName() + ", " +
				m.getType() + ", " + m.getSubType());
		    else
			System.out.println("\t\t"+m.getName() + ", " +
				m.getType());
		    if (m.getData() != null ) {
			switch (fmt) {
			    case HEX:
				dumpBytes(getBytes(m.getData()), dataLim);
				break;
			    case CHARS:
				String s = new String(getBytes(m.getData()));
				if ( dataLim != 0 )
				    System.out.println(s.substring(0,dataLim));
				else
				    System.out.println(s);
				break;
			    default:
			    case NONE:
				break;
			}
		    }
		}
	    }
	} catch (Option.OptionException oe) {
	    System.err.println(oe.getMessage());
	    usage();
	} catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
