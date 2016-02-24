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
public class ModifyAttribute extends Utility {

    /**
     * Print a usage message and exit.
     */
    static public void usage() {
	fatal("Usage: ModifyAttribute " +
		"(user|circle|project|experiment|library) " +
		"name [--type type] " +
		"[--optional|--no-optional] [--access access] " +
		"[--description description] [--format format] " +
		"[--formatdescription formatdescription] [--order order] "+
		"[--length length]");
    }

    static boolean checkType(String t) {
	return t.equals("STRING") || t.equals("INT") || t.equals("FLOAT") ||
	    t.equals("OPAQUE");
    }

    static boolean checkScope(String scope) {
	return scope.equals("user") || scope.equals("circle") ||
	    scope.equals("project") || scope.equals("experiment") ||
	    scope.equals("library");
    }

    static public class ModifyException extends Exception {
	public ModifyException(String msg) { super(msg); }
    }

    static void modifyCircleAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length)
	throws CirclesDeterFault, AxisFault, RemoteException, ModifyException {

	CirclesStub stub = new CirclesStub(getServiceUrl() + "Circles");
	CirclesStub.GetProfileDescription dreq =
	    new CirclesStub.GetProfileDescription();
	CirclesStub.GetProfileDescriptionResponse dresp =
	    stub.getProfileDescription(dreq);
	CirclesStub.Profile cp = dresp.get_return();
	CirclesStub.Attribute[] profile = cp.getAttributes();
	CirclesStub.Attribute thisOne = null;

	for (CirclesStub.Attribute a: profile)
	    if ( a.getName().equals(name)) {
		thisOne = a;
		break;
	    }

	if ( thisOne == null )
	    throw new ModifyException("No such attribute: " + name);

	CirclesStub.ModifyCircleAttribute req =
	    new CirclesStub.ModifyCircleAttribute();

	req.setName(name);
	req.setType(type.getValue() != null ?
		type.getValue() : thisOne.getDataType());
	req.setOptional(optional.getValue() != null ?
		optional.getValue().booleanValue() : thisOne.getOptional());
	req.setAccess(access.getValue() != null ?
		access.getValue() : thisOne.getAccess());
	req.setDescription(description.getValue() != null ?
		description.getValue() : thisOne.getDescription());
	req.setFormat(format.getValue() != null ?
		format.getValue() : thisOne.getFormat());
	req.setFormatdescription(formatdescription.getValue() != null ?
		formatdescription.getValue() : thisOne.getFormatDescription());
	req.setOrder(order.getValue() != null ?
		order.getValue().intValue() : thisOne.getOrderingHint());
	req.setLength(length.getValue() != null ?
		length.getValue().intValue() : thisOne.getLengthHint());
	stub.modifyCircleAttribute(req);
    }

    static void modifyExperimentAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length)
	throws ExperimentsDeterFault, AxisFault, RemoteException,
			  ModifyException {

	ExperimentsStub stub =
	    new ExperimentsStub(getServiceUrl() + "Experiments");
	ExperimentsStub.GetProfileDescription dreq =
	    new ExperimentsStub.GetProfileDescription();
	ExperimentsStub.GetProfileDescriptionResponse dresp =
	    stub.getProfileDescription(dreq);
	ExperimentsStub.Profile cp = dresp.get_return();
	ExperimentsStub.Attribute[] profile = cp.getAttributes();
	ExperimentsStub.Attribute thisOne = null;

	for (ExperimentsStub.Attribute a: profile)
	    if ( a.getName().equals(name)) {
		thisOne = a;
		break;
	    }

	if ( thisOne == null )
	    throw new ModifyException("No such attribute: " + name);

	ExperimentsStub.ModifyExperimentAttribute req =
	    new ExperimentsStub.ModifyExperimentAttribute();

	req.setName(name);
	req.setType(type.getValue() != null ?
		type.getValue() : thisOne.getDataType());
	req.setOptional(optional.getValue() != null ?
		optional.getValue().booleanValue() : thisOne.getOptional());
	req.setAccess(access.getValue() != null ?
		access.getValue() : thisOne.getAccess());
	req.setDescription(description.getValue() != null ?
		description.getValue() : thisOne.getDescription());
	req.setFormat(format.getValue() != null ?
		format.getValue() : thisOne.getFormat());
	req.setFormatdescription(formatdescription.getValue() != null ?
		formatdescription.getValue() : thisOne.getFormatDescription());
	req.setOrder(order.getValue() != null ?
		order.getValue().intValue() : thisOne.getOrderingHint());
	req.setLength(length.getValue() != null ?
		length.getValue().intValue() : thisOne.getLengthHint());
	stub.modifyExperimentAttribute(req);
    }

    static void modifyLibraryAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length)
	throws LibrariesDeterFault, AxisFault, RemoteException,
			  ModifyException {

	LibrariesStub stub = new LibrariesStub(getServiceUrl() + "Libraries");
	LibrariesStub.GetProfileDescription dreq =
	    new LibrariesStub.GetProfileDescription();
	LibrariesStub.GetProfileDescriptionResponse dresp =
	    stub.getProfileDescription(dreq);
	LibrariesStub.Profile cp = dresp.get_return();
	LibrariesStub.Attribute[] profile = cp.getAttributes();
	LibrariesStub.Attribute thisOne = null;

	for (LibrariesStub.Attribute a: profile)
	    if ( a.getName().equals(name)) {
		thisOne = a;
		break;
	    }

	if ( thisOne == null )
	    throw new ModifyException("No such attribute: " + name);

	LibrariesStub.ModifyLibraryAttribute req =
	    new LibrariesStub.ModifyLibraryAttribute();

	req.setName(name);
	req.setType(type.getValue() != null ?
		type.getValue() : thisOne.getDataType());
	req.setOptional(optional.getValue() != null ?
		optional.getValue().booleanValue() : thisOne.getOptional());
	req.setAccess(access.getValue() != null ?
		access.getValue() : thisOne.getAccess());
	req.setDescription(description.getValue() != null ?
		description.getValue() : thisOne.getDescription());
	req.setFormat(format.getValue() != null ?
		format.getValue() : thisOne.getFormat());
	req.setFormatdescription(formatdescription.getValue() != null ?
		formatdescription.getValue() : thisOne.getFormatDescription());
	req.setOrder(order.getValue() != null ?
		order.getValue().intValue() : thisOne.getOrderingHint());
	req.setLength(length.getValue() != null ?
		length.getValue().intValue() : thisOne.getLengthHint());
	stub.modifyLibraryAttribute(req);
    }

    static void modifyProjectAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length)
	throws ProjectsDeterFault, AxisFault, RemoteException, ModifyException {

	ProjectsStub stub = new ProjectsStub(getServiceUrl() + "Projects");
	ProjectsStub.GetProfileDescription dreq =
	    new ProjectsStub.GetProfileDescription();
	ProjectsStub.GetProfileDescriptionResponse dresp =
	    stub.getProfileDescription(dreq);
	ProjectsStub.Profile cp = dresp.get_return();
	ProjectsStub.Attribute[] profile = cp.getAttributes();
	ProjectsStub.Attribute thisOne = null;

	for (ProjectsStub.Attribute a: profile)
	    if ( a.getName().equals(name)) {
		thisOne = a;
		break;
	    }

	if ( thisOne == null )
	    throw new ModifyException("No such attribute: " + name);

	ProjectsStub.ModifyProjectAttribute req =
	    new ProjectsStub.ModifyProjectAttribute();

	req.setName(name);
	req.setType(type.getValue() != null ?
		type.getValue() : thisOne.getDataType());
	req.setOptional(optional.getValue() != null ?
		optional.getValue().booleanValue() : thisOne.getOptional());
	req.setAccess(access.getValue() != null ?
		access.getValue() : thisOne.getAccess());
	req.setDescription(description.getValue() != null ?
		description.getValue() : thisOne.getDescription());
	req.setFormat(format.getValue() != null ?
		format.getValue() : thisOne.getFormat());
	req.setFormatdescription(formatdescription.getValue() != null ?
		formatdescription.getValue() : thisOne.getFormatDescription());
	req.setOrder(order.getValue() != null ?
		order.getValue().intValue() : thisOne.getOrderingHint());
	req.setLength(length.getValue() != null ?
		length.getValue().intValue() : thisOne.getLengthHint());
	stub.modifyProjectAttribute(req);
    }


    static void modifyUserAttribute(String name, ParamOption type,
	    BooleanOption optional, ParamOption access,
	    ParamOption description, ParamOption format,
	    ParamOption formatdescription, NumberOption order,
	    NumberOption length)
	throws UsersDeterFault, AxisFault, RemoteException, ModifyException {

	UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	UsersStub.GetProfileDescription dreq =
	    new UsersStub.GetProfileDescription();
	UsersStub.GetProfileDescriptionResponse dresp =
	    stub.getProfileDescription(dreq);
	UsersStub.Profile up = dresp.get_return();
	UsersStub.Attribute[] profile = up.getAttributes();
	UsersStub.Attribute thisOne = null;

	for (UsersStub.Attribute a: profile)
	    if ( a.getName().equals(name)) {
		thisOne = a;
		break;
	    }

	if ( thisOne == null )
	    throw new ModifyException("No such attribute: " + name);

	UsersStub.ModifyUserAttribute req =
	    new UsersStub.ModifyUserAttribute();

	req.setName(name);
	req.setType(type.getValue() != null ?
		type.getValue() : thisOne.getDataType());
	req.setOptional(optional.getValue() != null ?
		optional.getValue().booleanValue() : thisOne.getOptional());
	req.setAccess(access.getValue() != null ?
		access.getValue() : thisOne.getAccess());
	req.setDescription(description.getValue() != null ?
		description.getValue() : thisOne.getDescription());
	req.setFormat(format.getValue() != null ?
		format.getValue() : thisOne.getFormat());
	req.setFormatdescription(formatdescription.getValue() != null ?
		formatdescription.getValue() : thisOne.getFormatDescription());
	req.setOrder(order.getValue() != null ?
		order.getValue().intValue() : thisOne.getOrderingHint());
	req.setLength(length.getValue() != null ?
		length.getValue().intValue() : thisOne.getLengthHint());
	System.err.println(req.getLength());
	stub.modifyUserAttribute(req);
    }

    /**
     * For each key/value pair on the command line, call setProperty
     * UserAttributes
     * @param args the key/value pairs and an optional --file fn parameter
     */
    static public void main(String[] args) {
	ParamOption type = new ParamOption("type");
	BooleanOption optional = new BooleanOption("optional", true);
	ParamOption access = new ParamOption("access");
	ParamOption description = new ParamOption("description");
	ParamOption format = new ParamOption("format");
	ParamOption formatdescription = new ParamOption("formatdescription");
	NumberOption order = new NumberOption("order");
	NumberOption length = new NumberOption("length");
	List<String> argv = new ArrayList<String>();
	String scope = null;
	String name = null;

	try {
	    Option.parseArgs(args, new Option[] {
		type, optional, access, description,
		format, formatdescription, order, length },
		argv);

	    if (argv.size() < 2)
		usage();
	    scope = argv.get(0);
	    name = argv.get(1);

	    if ( !checkScope(scope) ) {
		System.err.println("Bad scope (user, circle)");
		usage();
	    }
	    if (type.getValue() != null && !checkType(type.getValue()) ) {
		System.err.println("Bad type");
		usage();
	    }
	    if (access.getValue() != null &&
		    !Attribute.validateAccess(access.getValue())) {
		System.err.println("Bad access");
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
		modifyUserAttribute(name, type, optional, access, description,
			format, formatdescription, order, length);
	    else if ( scope.equals("circle"))
		modifyCircleAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length);
	    else if ( scope.equals("experiment"))
		modifyExperimentAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length);
	    else if ( scope.equals("library"))
		modifyLibraryAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length);
	    else if ( scope.equals("project"))
		modifyProjectAttribute(name, type, optional, access,
			description, format, formatdescription, order,
			length);
	    else fatal("Unknown scope: " + scope);
	} catch (ModifyException e) {
	    fatal(e.getMessage());
	} catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
