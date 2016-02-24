package net.deterlab.testbed.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;

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
public class ViewResources extends Utility {

    /**
     * Formats for printing data of aspects
     */
    static public enum DataFormats { NONE, HEX, CHARS};

    /**
     * Print a usage message and exit
     */
    static public void usage() {
	fatal("Usage: ViewResources [--type type]" +
		"[--offset offset] [--count count] "+
		"[--[no-]persist] [--realization realizationID] "+
		"[--save-data] " +
		"user [regex] ");
    }

    /**
     * General(ish) command line interface to viewResources.  Parameters are
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

	    ParamOption typeOption = new ParamOption("type");
	    ParamOption realizationOption = new ParamOption("realization");
	    NumberOption countOption = new NumberOption("count");
	    NumberOption offsetOption = new NumberOption("offset");
	    BooleanOption persistOption = new BooleanOption("persist");
	    BooleanOption saveOption = new BooleanOption("save-data", false);
	    ListOption tagOption = new ListOption("tag");
	    Option[] opts = new Option[] {
		typeOption, countOption, offsetOption, persistOption,
		    realizationOption, tagOption, saveOption
	    };
	    ArrayList<String> pos = new ArrayList<String>();
	    Option.parseArgs(args, opts, pos, 0);


	    if ( pos.size() < 1)
		throw new Option.OptionException(
			"Not enough positional parameters");

	    String uid = pos.get(0);
	    String regex = (pos.size() > 1) ? pos.get(1) : null;
	    String realization = realizationOption.getValue();
	    Number count = countOption.getValue();
	    Number offset = offsetOption.getValue();
	    Boolean persist = persistOption.getValue();
	    boolean saveData = saveOption.getValue();

	    ResourcesStub stub =
		new ResourcesStub(getServiceUrl("Resources"));
	    ResourcesStub.ViewResources req = 
		new ResourcesStub.ViewResources();
	    ArrayList<ResourcesStub.ResourceTag> tags = new ArrayList<>();

	    req.setUid(uid);
	    if ( persist != null) req.setPersist(persist);
	    if ( regex != null ) req.setRegex(regex);
	    if ( realization != null ) req.setRegex(realization);
	    if (typeOption.getValue() != null)
		req.setType(typeOption.getValue());
	    if ( count != null )
		req.setCount(new Integer(count.intValue()));
	    if ( offset != null )
		req.setOffset(new Integer(offset.intValue()));

	    for (String t : tagOption.getValue()) {
		ResourcesStub.ResourceTag nt = new ResourcesStub.ResourceTag();
		String[] s = t.split("=", 2);

		if ( s == null || s.length != 2 ) continue;
		nt.setName(s[0]);
		nt.setValue(s[1]);
		tags.add(nt);
	    }
	    req.setTags(tags.toArray(new ResourcesStub.ResourceTag[0]));

	    ResourcesStub.ViewResourcesResponse resp = 
		stub.viewResources(req);

	    ResourcesStub.ResourceDescription[] res = resp.get_return();

	    if (res  == null ) {
		String errStr = "No resources found for " + uid;

		if (regex != null) errStr += " regex " + regex;
		fatal(errStr);
	    }

	    for (ResourcesStub.ResourceDescription r : res) {
		ResourcesStub.ResourceTag[] rTags = r.getTags();
		ResourcesStub.ResourceFacet[] rFacets = r.getFacets();
		ResourcesStub.AccessMember[] acl =  r.getACL();

		if (rFacets == null )
		    rFacets = new ResourcesStub.ResourceFacet[0];

		if (acl == null )
		    acl = new ResourcesStub.AccessMember[0];

		if ( rTags == null )
		    rTags = new ResourcesStub.ResourceTag[0];

		System.out.println("Resource- " + r.getName() + " " +
			r.getType());
		if ( r.getDescription() != null )
		    System.out.println("\t" + r.getDescription());

		System.out.print("\tUser permissions: ");
		boolean first = true;
		for (String p : r.getPerms()) {
		    if ( first ) {
			System.out.print(p);
			first = false;
		    }
		    else {
			System.out.print(", " + p );
		    }
		}
		System.out.println();
		System.out.println("\tACL-");
		for (ResourcesStub.AccessMember m : acl)
		    System.out.println("\t\t"+m.getCircleId() + " " +
			    joinStrings(m.getPermissions(), ", "));

		System.out.println("\tFacets-");
		for (ResourcesStub.ResourceFacet m : rFacets)
		    System.out.println("\t\t"+m.getName() + " " + m.getValue()+
			    " " + m.getUnits());

		System.out.println("\tTags-");
		for (ResourcesStub.ResourceTag m : rTags)
		    System.out.println("\t\t"+m.getName() + "=\"" +
			    m.getValue() + "\"");

		System.err.println(saveData + " " + r.getData());
		if ( saveData && r.getData() != null) {
		    try {
			File outf = new File(r.getName());
			FileOutputStream outs = new FileOutputStream(outf);

			System.out.println("Saving data to: " + outf);
			outs.write(getBytes(r.getData()));
			outs.close();
		    }
		    catch (IOException ie) {
			System.err.println("Error saving data: " + ie);
		    }
		}
	    }
	} catch (Option.OptionException oe) {
	    System.err.println(oe.getMessage());
	    usage();
	} catch (ResourcesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
