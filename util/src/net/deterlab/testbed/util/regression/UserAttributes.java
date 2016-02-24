package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls createUserAttribute, and modifyUserAttribute, and
 * removeUserAttribute from the Users service.  It checks parameter for  that
 * createUserAttribute and uses createUserAttribute to create attributes used
 * by later tests.  Then it confirms that modifyUserAttribute checks parameters
 * and functions.  At the end of the test 4 new user attributes exist:
 * test-required, test-readonly, test-format, and test-open.  These are removed
 * by RemoveUserAttributes later.
 * @author DETER team
 * @version 1.0
 */
public class UserAttributes extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public UserAttributes() { 
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "UserAttributes"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls createUserAttribute, and\n"+
	    "modifyUserAttribute, and removeUserAttribute from the Users\n"+
	    "service.  It checks parameter for  that createUserAttribute\n"+
	    "and uses createUserAttribute to create attributes used by\n"+
	    "later tests.  Then it confirms that modifyUserAttribute\n"+
	    "checks parameters and functions.  At the end of the test 4\n"+
	    "new user attributes exist: test-required, test-readonly,\n"+
	    "test-format, and test-open.  These are removed by\n"+
	    "RemoveUserAttributes later.\n"+
		"-->\n");
    }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	UsersStub uStub = null;
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(uStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();
	summary(p);
	UsersStub.CreateUserAttribute req = 
	    new UsersStub.CreateUserAttribute();

	regressionLogin(uStub, "testadmin", "test", p);

	createUserAttribute(uStub, null, "string", true, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "without name", p, 
		false, DeterFault.request);
	createUserAttribute(uStub, "Testattr", "string", true, 
		"1024", "description", null, 
		"formatdescription", 100, 0, null, "bad access", p, 
		false, DeterFault.request);

	createUserAttribute(uStub, "testattr", "string", false, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "bad no default", p, 
		false, DeterFault.request);

	// Fail on permissions

	regressionLogin(uStub, "testnotadmin", "test", p);
	createUserAttribute(uStub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "bad permissions", 
		p, false, DeterFault.access);

	regressionLogout(uStub, p);
	createUserAttribute(uStub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "not logged in", 
		p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Create the array of test attributes 
	createUserAttribute(uStub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create a read-only test attribute for later tests-->");
	createUserAttribute(uStub, "test-readonly", "string", false, 
		Attribute.READ_ONLY, 
		"required non-writable test attr", null, 
		"formatdescription", 11000, 0, "4", "good", 
		p, true, DeterFault.none);
	p.println("<!-- create a read-write test attribute with a format " +
		"for later tests-->");

	createUserAttribute(uStub, "test-format", "string", true, 
		Attribute.READ_WRITE, 
		"non-required writable test attr", "^[0-9]+$", 
		"Numbers Only", 12000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create an unconstrained test attribute " +
		"for later tests-->");
	createUserAttribute(uStub, "test-open", "string", true, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"None Only", 13000, 0, "4", "good", 
		p, true, DeterFault.none);

	// Modify existing attribute - bad permissions

	regressionLogin(uStub, "testnotadmin", "test", p);

	UsersStub.ModifyUserAttribute mReq = 
	    new UsersStub.ModifyUserAttribute();
	p.println("<!-- modify a test attribute bad permissions -->");

	modifyUserAttribute(uStub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	modifyUserAttribute(uStub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Modify existing attribute
	modifyUserAttribute(uStub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, true, DeterFault.none);

	p.println("<!-- modify a test attribute that doesn't exist-->");
	modifyUserAttribute(uStub, "testattr2", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, false, DeterFault.request);
	regressionLogout(uStub, p);
	p.flush();
	p.close();
    }
}

