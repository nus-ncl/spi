package net.deterlab.testbed.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;

import net.deterlab.testbed.util.gui.EditProfileDialog;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Create a new experiment.  Pop up a dialog that displays an empty profile
 * and allow editing of fields that can be edited.  When OK is pressed,
 * submit the request.  Aspects, the owner, and access rights are passed in on
 * the command line.
 * @author DETER Team
 * @version 1.0
 */
public class CreateResource extends Utility {

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

	rv.setCircleId(circleId);
	rv.setPermissions(perms);
	return rv;
    }

    /**
     * Parse a facet specification string into a ResourceFacet. The parsing is
     * primarily splitting on commas.  Throw an OptionException if the string
     * is confusing.
     * @param s the specification string
     * @return the constructed ResourcesSub.ResourceFacet
     * @throws OptionException if the specification is badly formed.
     */
    static private ResourcesStub.ResourceFacet parseFacet(String s)
	    throws Option.OptionException, IOException {
	String[] parts = s.split(",");
	String id = null;
	String type = null;
	String value = null;
	String units = null;
	ResourcesStub.ResourceTag[] tags = null;
	String p = null;
	ResourcesStub.ResourceFacet rv =
	    new ResourcesStub.ResourceFacet();

	try { 
	    if ( parts.length < 3 )
		throw new Option.OptionException("Bad facet: " + s);

	    tags = (parts.length > 3) ? 
		new ResourcesStub.ResourceTag[parts.length-3] : null;

	    rv.setType(parts[0]);
	    rv.setValue(Double.parseDouble(parts[1]));
	    rv.setUnits(parts[2]);

	    for (int i = 3; i < parts.length; i++) {
		ResourcesStub.ResourceTag t = new ResourcesStub.ResourceTag();
		String[] v = parts[i].split("=", 2);

		if ( v.length != 2 ) continue;
		t.setName(v[0]);
		t.setValue(v[1]);
		tags[i-3] = t;
	    }
	    if ( tags != null ) rv.setTags(tags);
	}
	catch (NumberFormatException e) {
	    throw new Option.OptionException("Badly formatted value " +
		    parts[1]);
	}
	return rv;
    }

    /**
     * Parse a tag specification string into a ResourceTag. The parsing is
     * primarily splitting on the equals sign.  Throw an OptionException if the
     * string is confusing.
     * @param s the specification string
     * @return the constructed ResourcesSub.ResourceTag
     * @throws OptionException if the specification is badly formed.
     */
    static private ResourcesStub.ResourceTag parseTag(String s)
	    throws Option.OptionException {
	String[] parts = s.split("=");
	ResourcesStub.ResourceTag rv =
	    new ResourcesStub.ResourceTag();

	if ( parts.length < 2 )
	    throw new Option.OptionException("Bad tag: " + s);

	rv.setName(parts[0]);
	rv.setValue(parts[1]);
	return rv;
    }
    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("CreateResource name type " +
		"[--persist] [--no-persist] " +
		"[--description desc] " +
		"[--tag name=value] " +
		"[--facet type,value,units,[name=value[,...]]...] "+
		"[--circle circleid(perms) ... ]");
    }
	
    /**
     * Create the new resource.  Parse parameters, construct the request and
     * call it.
     * @param args the required resource name and optional facets and circles.
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    ListOption circles = new ListOption("circle");
	    ListOption facetParam = new ListOption("facet");
	    ListOption tagParam = new ListOption("tag");
	    ParamOption descParam = new ParamOption("description");
	    BooleanOption persistParam = new BooleanOption("persist", true);
	    List<String> argv = new ArrayList<>();
	    List<ResourcesStub.AccessMember> acl = new ArrayList<>();
	    List<ResourcesStub.ResourceFacet> facets = new ArrayList<>();
	    List<ResourcesStub.ResourceTag> tags = new ArrayList<>();

	    Option[] options = new Option[] {
		descParam, facetParam, tagParam, persistParam, circles };
	    Option.parseArgs(args, options, argv);

	    if ( argv.size()!= 2) usage(null);

	    String name = argv.get(0);
	    String type = argv.get(1);
	    String desc = descParam.getValue();

	    ResourcesStub stub =
		new ResourcesStub(getServiceUrl("Resources"));

	    ResourcesStub.CreateResource createReq = 
		new ResourcesStub.CreateResource();

	    createReq.setName(name);
	    createReq.setType(type);

	    if ( desc != null )
		createReq.setDescription(desc);

	    for ( String s : facetParam.getValue()) 
		facets.add(parseFacet(s));

	    for (String s : tagParam.getValue())
		tags.add(parseTag(s));

	    createReq.setFacets(facets.toArray(
			new ResourcesStub.ResourceFacet[0]));

	    createReq.setTags(tags.toArray(
			new ResourcesStub.ResourceTag[0]));

	    createReq.setPersist(persistParam.getValue());

	    for ( String s : circles.getValue()) {
		ResourcesStub.AccessMember a = parseCircle(s);
		if ( a == null )
		    throw new Option.OptionException("Bad circle: " +s);
		acl.add(a);
	    }

	    createReq.setAccessLists(
		    acl.toArray(new ResourcesStub.AccessMember[0]));

	    ResourcesStub.CreateResourceResponse createResp = 
		stub.createResource(createReq);
	    System.out.println("CreateResource returned " + 
		    createResp.get_return());

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
