package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls createProjectAttribute, and modifyProjectAttribute, and
 * removeProjectAttribute from the Projects service.  It checks parameter for
 * that createProjectAttribute and uses createProjectAttribute to create
 * attributes used by later tests.  Then it confirms that
 * modifyProjectAttribute checks parameters and functions.  At the end of the
 * test 4 new user attributes exist: test-required, test-readonly, test-format,
 * and test-open.  These are removed by RemoveProjectAttributes later.
 * @author DETER team
 * @version 1.0
 */
public class ProjectAttributes extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public ProjectAttributes() { 
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ProjectAttributes"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls createProjectAttribute, and\n"+
	    "modifyProjectAttribute from the Projects\n"+
	    "service.  It checks parameter for  that createProjectAttribute\n"+
	    "and uses createProjectAttribute to create attributes used by\n"+
	    "later tests.  Then it confirms that modifyProjectAttribute\n"+
	    "checks parameters and functions.  At the end of the test 4\n"+
	    "new project attributes exist: test-required, test-readonly,\n"+
	    "test-format, and test-open.  These are removed by\n"+
	    "RemoveProjectAttributes later.\n"+
		"-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ProjectsStub stub = null;
	UsersStub uStub = null;
	try {
	    stub = new ProjectsStub(getServiceUrl() + "Projects");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(stub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();
	logSOAP(uStub, p);
	summary(p);
	ProjectsStub.CreateProjectAttribute req = 
	    new ProjectsStub.CreateProjectAttribute();

	regressionLogin(uStub, "testadmin", "test", p);

	createProjectAttribute(stub, null, "string", true, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "without name", p, false,
		DeterFault.request);
	createProjectAttribute(stub, "Testattr", "string", true, 
		"1024", "description", null, 
		"formatdescription", 100, 0, null, "bad access", p, false,
		DeterFault.request);

	createProjectAttribute(stub, "testattr", "string", false, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "bad no default", p,
		false, DeterFault.request);

	// Fail on permissions

	regressionLogin(uStub, "testnotadmin", "test", p);
	createProjectAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "bad permissions", 
		p, false, DeterFault.access);

	regressionLogout(uStub, p);
	createProjectAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "not logged in", 
		p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Create the array of test attributes 
	createProjectAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create a read-only test attribute for later tests-->");
	createProjectAttribute(stub, "test-readonly", "string", false, 
		Attribute.READ_ONLY, 
		"required non-writable test attr", null, 
		"formatdescription", 11000, 0, "4", "good", 
		p, true, DeterFault.none);
	p.println("<!-- create a read-write test attribute with a format " +
		"for later tests-->");

	createProjectAttribute(stub, "test-format", "string", true, 
		Attribute.READ_WRITE, 
		"non-required writable test attr", "^[0-9]+$", 
		"Numbers Only", 12000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create an unconstrained test attribute " +
		"for later tests-->");
	createProjectAttribute(stub, "test-open", "string", true, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"None Only", 13000, 0, "4", "good", 
		p, true, DeterFault.none);

	// Modify existing attribute - bad permissions

	regressionLogin(uStub, "testnotadmin", "test", p);

	ProjectsStub.ModifyProjectAttribute mReq = 
	    new ProjectsStub.ModifyProjectAttribute();
	p.println("<!-- modify a test attribute bad permissions -->");

	modifyProjectAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	modifyProjectAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Modify existing attribute
	modifyProjectAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, true, DeterFault.none);

	p.println("<!-- modify a test attribute that doesn't exist-->");
	modifyProjectAttribute(stub, "testattr2", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, false, DeterFault.request);
	regressionLogout(uStub, p);
	p.flush();
	p.close();
    }
}

