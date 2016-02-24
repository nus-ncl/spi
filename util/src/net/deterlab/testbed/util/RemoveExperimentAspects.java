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

import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;

import net.deterlab.testbed.util.gui.EditProfileDialog;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Remove aspects from an experiment.  Each aspect is specified on the command
 * line as name,type,subtype where each of these can be omitted.
 * @author DETER Team
 * @version 1.0
 */
public class RemoveExperimentAspects extends Utility {
    /**
     * Print a usage message and exit; is a message is given print that as a
     * warning first.
     * @param s a warning message (may be null)
     */
    static void usage(String s) {
	if ( s != null) warn(s);
	fatal("RemoveExperimentAspect eid [name][,type[,subtype]] ...");
    }
	
    /**
     * Parse an aspect specification string into an ExperimentAspect. The
     * parsing is primarily splitting on commas.  
     * @param s the specification string
     * @return the constructed ExperimentsSub.ExperimentAspect
     * @throws OptionException if the specification is badly formed.
     */
    static private ExperimentsStub.ExperimentAspect parseAspect(String s)
	    throws Option.OptionException, IOException {
	String[] parts = s.split(",");
	String name = (parts.length > 0) ? parts[0] : null;
	String type = (parts.length > 1) ? parts[1] : null;
	String subtype = (parts.length > 2) ? parts[2] : null;
	ExperimentsStub.ExperimentAspect rv =
	    new ExperimentsStub.ExperimentAspect();

	if ( type == null && subtype != null)
	    throw new Option.OptionException("Subtype not valid without type");

	rv.setName(name);
	rv.setType(type);
	rv.setSubType(subtype);

	return rv;
    }

    /**
     * Convert an ExperimentAspect into a comma-separated name,type,sybtype
     * string.
     * @param a the ExperimetAspect to convert
     * @return the string representation
     */
    static protected String aspectToString(ExperimentsStub.ExperimentAspect a) {
	StringBuilder sb = new StringBuilder();

	if ( a == null ) return "(null)";

	String name = a.getName();
	String type = a.getType();
	String subType = a.getSubType();

	if ( name != null ) sb.append(name);
	sb.append(",");
	if ( type != null ) sb.append(type);
	sb.append(",");
	if ( subType != null ) sb.append(subType);

	return sb.toString();
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();

	    List<String> argv = new ArrayList<String>();
	    List<ExperimentsStub.ExperimentAspect> aspects =
		new ArrayList<ExperimentsStub.ExperimentAspect>();

	    Option[] options = new Option[0];
	    Option.parseArgs(args, options, argv);

	    if ( argv.size() < 2) usage(null);

	    String eid = argv.get(0);
	    for ( String as: argv.subList(1, argv.size()))
		aspects.add(parseAspect(as));

	    ExperimentsStub stub =
		new ExperimentsStub(getServiceUrl("Experiments"));
	    ExperimentsStub.RemoveExperimentAspects removeAspectsReq = 
		new ExperimentsStub.RemoveExperimentAspects();

	    removeAspectsReq.setEid(eid);
	    removeAspectsReq.setAspects(aspects.toArray(
			new ExperimentsStub.ExperimentAspect[0]));

	    ExperimentsStub.RemoveExperimentAspectsResponse removeAspectsResp = 
		stub.removeExperimentAspects(removeAspectsReq);
	    ExperimentsStub.ChangeResult[] resp =
		    removeAspectsResp.get_return();

	    if ( resp == null ) {
		System.err.println("Nothing removed");
		System.exit(20);
	    }

	    for (ExperimentsStub.ChangeResult ra : resp) {
		System.out.print(ra.getName());
		if ( ra.getSuccess()) System.out.println(" succeeded");
		else System.out.println(" failed: " + ra.getReason());
	    }


	}
	catch (Option.OptionException e) {
	    usage("Option parsing exception: " + e);
	}
	catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
