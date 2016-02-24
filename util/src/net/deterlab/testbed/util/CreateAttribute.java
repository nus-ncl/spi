package net.deterlab.testbed.util;

import java.io.File;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.NumberOption;
import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

/**
 * A utility to add a property to the default utility store.
 * @author the DETER Team
 * @version 1.0
 */
public class CreateAttribute extends Utility {

    /**
     * Print a usage message and exit.
     */
    static public void usage() {
	fatal("Usage: CreateAttribute " +
		"(user|circle|project|experiment|library) " +
		"name [--type type] " +
		"[--optional|--no-optional] [--access access] " + 
		"[--description description] [--format format] " + 
		"[--formatdescription formatdescription] [--order order] "+ 
		"[--length length] [--default default]");
    }

    static boolean checkType(String t) {
	return t.equals("STRING") || t.equals("INT") || t.equals("FLOAT") || 
	    t.equals("OPAQUE");
    }

    static boolean checkScope(String scope) {
	return scope.equals("user") || scope.equals("circle") ||
	    scope.equals("project")||scope.equals("experiment")||
	    scope.equals("library");
    }

    static void createProjectAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access, 
	    ParamOption description, ParamOption format, 
	    ParamOption formatdescription, NumberOption order, 
	    NumberOption length, ParamOption def)
	throws ProjectsDeterFault, AxisFault, RemoteException {

	ProjectsStub stub = new ProjectsStub(getServiceUrl() + "Projects");
	ProjectsStub.CreateProjectAttribute req = 
	    new ProjectsStub.CreateProjectAttribute();

	req.setName(name);
	req.setType(type.getValue());
	req.setOptional(optional.getValue());
	req.setAccess(access.getValue());
	req.setDescription(description.getValue());
	req.setFormat(format.getValue());
	req.setFormatdescription(formatdescription.getValue());
	req.setOrder(order.getValue().intValue());
	req.setLength(length.getValue().intValue());
	req.setDef(def.getValue());
	stub.createProjectAttribute(req);
    }
    static void createCircleAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access, 
	    ParamOption description, ParamOption format, 
	    ParamOption formatdescription, NumberOption order, 
	    NumberOption length, ParamOption def)
	throws CirclesDeterFault, AxisFault, RemoteException {

	CirclesStub stub = new CirclesStub(getServiceUrl() + "Circles");
	CirclesStub.CreateCircleAttribute req = 
	    new CirclesStub.CreateCircleAttribute();

	req.setName(name);
	req.setType(type.getValue());
	req.setOptional(optional.getValue());
	req.setAccess(access.getValue());
	req.setDescription(description.getValue());
	req.setFormat(format.getValue());
	req.setFormatdescription(formatdescription.getValue());
	req.setOrder(order.getValue().intValue());
	req.setLength(length.getValue().intValue());
	req.setDef(def.getValue());
	stub.createCircleAttribute(req);
    }

    static void createUserAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access, 
	    ParamOption description, ParamOption format, 
	    ParamOption formatdescription, NumberOption order, 
	    NumberOption length, ParamOption def)
	throws UsersDeterFault, AxisFault, RemoteException {

	UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	UsersStub.CreateUserAttribute req = 
	    new UsersStub.CreateUserAttribute();

	req.setName(name);
	req.setType(type.getValue());
	req.setOptional(optional.getValue());
	req.setAccess(access.getValue());
	req.setDescription(description.getValue());
	req.setFormat(format.getValue());
	req.setFormatdescription(formatdescription.getValue());
	req.setOrder(order.getValue().intValue());
	req.setLength(length.getValue().intValue());
	req.setDef(def.getValue());
	stub.createUserAttribute(req);
    }

    static void createExperimentAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length, ParamOption def)
	throws ExperimentsDeterFault, AxisFault, RemoteException {

	ExperimentsStub stub =
	    new ExperimentsStub(getServiceUrl() + "Experiments");
	ExperimentsStub.CreateExperimentAttribute req =
	    new ExperimentsStub.CreateExperimentAttribute();

	req.setName(name);
	req.setType(type.getValue());
	req.setOptional(optional.getValue());
	req.setAccess(access.getValue());
	req.setDescription(description.getValue());
	req.setFormat(format.getValue());
	req.setFormatdescription(formatdescription.getValue());
	req.setOrder(order.getValue().intValue());
	req.setLength(length.getValue().intValue());
	req.setDef(def.getValue());
	stub.createExperimentAttribute(req);
    }


    static void createLibraryAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length, ParamOption def)
	throws LibrariesDeterFault, AxisFault, RemoteException {

	LibrariesStub stub =
	    new LibrariesStub(getServiceUrl() + "Libraries");
	LibrariesStub.CreateLibraryAttribute req =
	    new LibrariesStub.CreateLibraryAttribute();

	req.setName(name);
	req.setType(type.getValue());
	req.setOptional(optional.getValue());
	req.setAccess(access.getValue());
	req.setDescription(description.getValue());
	req.setFormat(format.getValue());
	req.setFormatdescription(formatdescription.getValue());
	req.setOrder(order.getValue().intValue());
	req.setLength(length.getValue().intValue());
	req.setDef(def.getValue());
	stub.createLibraryAttribute(req);
    }

    /**
     * For each key/value pair on the command line, call setProperty
     * UserAttributes
     * @param args the key/value pairs and an optional --file fn parameter
     */
    static public void main(String[] args) {
	ParamOption type = new ParamOption("type", "STRING");
	BooleanOption optional = new BooleanOption("optional", true);
	ParamOption access = new ParamOption("access", "READ_WRITE");
	ParamOption description = new ParamOption("description");
	ParamOption format = new ParamOption("format");
	ParamOption formatdescription = new ParamOption("formatdescription");
	NumberOption order = new NumberOption("order", 0);
	NumberOption length = new NumberOption("length", 0);
	ParamOption def = new ParamOption("default");
	List<String> argv = new ArrayList<String>();
	String scope = null;
	String name = null;

	try {
	    Option.parseArgs(args, new Option[] { 
		type, optional, access, description, 
		format, formatdescription, order, length, def },
		argv);

	    if (argv.size() < 2) 
		usage();
	    scope = argv.get(0);
	    name = argv.get(1);
	    if ( !checkScope(scope) ) {
		System.err.println("Bad scope (user, circle, project, "+
			"experiment or library)");
		usage();
	    }
	    if (!checkType(type.getValue()) ) {
		System.err.println("Bad type: " + type.getValue());
		usage();
	    }
	    if (!Attribute.validateAccess(access.getValue())) {
		System.err.println("Bad access: '" + access.getValue()+"'");
		usage();
	    }
	}
	catch (Option.OptionException e) {
	    System.err.println(e.getMessage());
	    usage();
	}
	loadTrust();
	loadID();
	try {
	    if ( scope.equals("user")) 
		createUserAttribute(name, type, optional, access, description, 
			format, formatdescription, order, length, def);
	    else if ( scope.equals("circle")) 
		createCircleAttribute(name, type, optional, access, 
			description, format, formatdescription, order,
			length, def);
	    else if ( scope.equals("project")) 
		createProjectAttribute(name, type, optional, access, 
			description, format, formatdescription, order,
			length, def);
	    else if ( scope.equals("Experiment"))
		createExperimentAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length, def);
	    else if ( scope.equals("library"))
		createLibraryAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length, def);
	    else fatal("Unknown scope: " + scope);
	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
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
