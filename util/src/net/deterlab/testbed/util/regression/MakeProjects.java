package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression tests for Project Creation.
 * @author DETER team
 * @version 1.0
 */
public class MakeProjects extends RegressionTest {
    /**
     * Create a new GetVersion regression test
     * @param l the interactive log
     */
    public MakeProjects() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeProjects"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls getProfileDescription, createProject and\n"+
	    "approveProject from the Projects service.  It must be called\n"+
	    "after MakeUsers as it assumes those users are present to act as\n"+
	    "owners.  The project profile is gathered from\n"+
	    "getProfileDescription and createProject is called several times\n"+
	    "with invalid paremeters.  In order, it is called with a\n"+
	    "required profile field missing, with no profileid to create,\n"+
	    "with now owner, with an owner name that does not exist, and\n"+
	    "with no profile at all.  Finally a successful call is made to\n"+
	    "create the regression0 project.  One more call is made to\n"+
	    "confirm that a project cannot be created with the same name as\n"+
	    "an existing user (which would confuse the circle naming\n"+
	    "system).  Finally it tests the parameters to approve\n"+
	    "project and approves the regression0 project  At the end of the\n"+
	    "test the regression0 project exists owned by the test user.\n"+
	    "-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ProjectsStub pStub = null;
	UsersStub uStub = null;
	try {
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(pStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	// We copy the specific project's values into the array of attributes
	// returned from GetProfileDescription.  This map lets us easliy
	// put the values in the right places.
	Map<String, ProjectsStub.Attribute> nameToAttr = 
	    new HashMap<String, ProjectsStub.Attribute>();
	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	
	ProjectsStub.Attribute[] profile = getProfileDescription(pStub, "good",
		p, true, DeterFault.none);
	ProjectsStub.Attribute[] extra_field =
	    new ProjectsStub.Attribute[profile.length+1];

	for ( int i = 0; i < profile.length; i++)
	    extra_field[i] = profile[i];

	extra_field[profile.length] = new ProjectsStub.Attribute();
	extra_field[profile.length].setName("unknown_attribute");
	extra_field[profile.length].setAccess("READ_WRITE");
	extra_field[profile.length].setDataType("STRING");
	extra_field[profile.length].setDescription("STRING");
	extra_field[profile.length].setOptional(true);



	// Initialize the nameToAttr Map
	for (ProjectsStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("description").setValue("Hi - I'm a project");
	nameToAttr.get("URL").setValue("http://www.deterlab.net");

	createProject(pStub, "regression0", "test", profile,
		"missing field in profile", p, false, DeterFault.request);
	nameToAttr.get("test-required").setValue("here");
	createProject(pStub, null, "test", profile,
		"no profile id", p, false, DeterFault.request);
	createProject(pStub, "regression:regression0", "test", profile,
		"badly constructed name", p, false, DeterFault.request);
	createProject(pStub, "regression0", null, profile,
		"no owner", p, false, DeterFault.request);
	createProject(pStub, "regression0", "23test23", profile,
		"bad owner", p, false, DeterFault.access);
	createProject(pStub, "regression0", "test", null,
		"no profile", p, false, DeterFault.request);

	nameToAttr.get("test-format").setValue("here");
	createProject(pStub, "regression0", "test", profile,
		"bad format in profile", p, false, DeterFault.request);
	nameToAttr.get("test-format").setValue("123");
	createProject(pStub, "regression0", "test", extra_field,
		"unknown attribute in profile", p, false, DeterFault.request);
	regressionLogout(uStub, p);
	createProject(pStub, "regression0", "test", profile,
		"not logged in", p, false, DeterFault.login);
	regressionLogin(uStub, "testnotadmin", "test", p);
	createProject(pStub, "regression0", "test", profile,
		"bad permissions", p, false, DeterFault.access);
	regressionLogin(uStub, "test", "test", p);
	createProject(pStub, "regression0", "test", profile,
		"good", p, true, DeterFault.none);
	createProject(pStub, "test", "test", profile,
		"userid conflict", p, false, DeterFault.request);
	// The regression project is not approved yet, so let's approve it.
	approveProject(pStub, null, true, "no project", p, 
		false, DeterFault.request);
	approveProject(pStub, "regression:cisforcookie", 
		true, "bad project id format", p, false, DeterFault.request);
	approveProject(pStub, "cisforcookie", 
		true, "bad project id nonexistent", p, false,
		DeterFault.request);
	approveProject(pStub, "regression0", 
		true, "bad permissions", p, false, 
		DeterFault.access);
	regressionLogin(uStub, "testadmin", "test", p);
	approveProject(pStub, "regression0", 
		true, "good", p, true, DeterFault.none);
	regressionLogin(uStub, "test", "test", p);

	regressionLogout(uStub, p);
    }
}
