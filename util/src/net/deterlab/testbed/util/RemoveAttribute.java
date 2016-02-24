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

import org.apache.axis2.AxisFault;

/**
 * A utility to add a property to the default utility store.
 * @author the DETER Team
 * @version 1.0
 */
public class RemoveAttribute extends Utility {

    /**
     * Print a usage message and exit.
     */
    static public void usage() {
	fatal("Usage: RemoveUserAttribute " +
		"(user|circle|project|experiment|library)) name ");
    }

    static boolean checkScope(String scope) {
	return scope.equals("user") || scope.equals("circle") ||
	    scope.equals("project") || scope.equals("experiment") ||
	    scope.equals("library");
    }

    static public void removeProjectAttribute(String name) 
	    throws ProjectsDeterFault, RemoteException, AxisFault {
	ProjectsStub stub = new ProjectsStub(getServiceUrl() + "Projects");
	ProjectsStub.RemoveProjectAttribute req = 
	    new ProjectsStub.RemoveProjectAttribute();

	req.setName(name);
	stub.removeProjectAttribute(req);
    }

    static public void removeCircleAttribute(String name) 
	    throws CirclesDeterFault, RemoteException, AxisFault {
	CirclesStub stub = new CirclesStub(getServiceUrl() + "Circles");
	CirclesStub.RemoveCircleAttribute req = 
	    new CirclesStub.RemoveCircleAttribute();

	req.setName(name);
	stub.removeCircleAttribute(req);
    }

    static public void removeExperimentAttribute(String name)
	    throws ExperimentsDeterFault, RemoteException, AxisFault {
	ExperimentsStub stub =
	    new ExperimentsStub(getServiceUrl() + "Experiments");
	ExperimentsStub.RemoveExperimentAttribute req =
	    new ExperimentsStub.RemoveExperimentAttribute();

	req.setName(name);
	stub.removeExperimentAttribute(req);
    }

    static public void removeLibraryAttribute(String name)
	    throws LibrariesDeterFault, RemoteException, AxisFault {
	LibrariesStub stub =
	    new LibrariesStub(getServiceUrl() + "Libraries");
	LibrariesStub.RemoveLibraryAttribute req =
	    new LibrariesStub.RemoveLibraryAttribute();

	req.setName(name);
	stub.removeLibraryAttribute(req);
    }

    static public void removeUserAttribute(String name) 
	    throws UsersDeterFault, RemoteException, AxisFault {
	UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	UsersStub.RemoveUserAttribute req = 
	    new UsersStub.RemoveUserAttribute();

	req.setName(name);
	stub.removeUserAttribute(req);
    }

    /**
     * For each key/value pair on the command line, call setProperty
     * UserAttributes
     * @param args the key/value pairs and an optional --file fn parameter
     */
    static public void main(String[] args) {
	String scope = null;
	String name = null;

	if ( args.length != 2 ) 
	    usage();
	scope = args[0];
	name = args[1];

	if (!checkScope(scope) ) {
	    System.err.println("Bad scope: " + scope);
	    usage();
	}

	loadTrust();
	loadID();
	try {
	    if (scope.equals("user"))
		removeUserAttribute(name);
	    else if (scope.equals("circle"))
		removeCircleAttribute(name);
	    else if (scope.equals("experiment"))
		removeExperimentAttribute(name);
	    else if (scope.equals("library"))
		removeLibraryAttribute(name);
	    else if (scope.equals("project"))
		removeProjectAttribute(name);
	    else
		fatal("Unknown scope: " + scope);
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
	} catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
