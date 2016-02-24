package net.deterlab.testbed.util.regression;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.UsersStub;

import java.io.File;
import java.io.PrintStream;

import org.apache.axis2.AxisFault;

/** 
 * This test calls createLibraryAttribute, and modifyLibraryAttribute,
 * and removeLibraryAttribute from the Libraries service.  It checks
 * parameter for  that createLibraryAttribute and uses
 * createLibraryAttribute to create attributes used by later tests.  Then it
 * confirms that modifyLibraryAttribute checks parameters and functions.  At
 * the end of the test 4 new circle attributes exist: test-required,
 * test-readonly, test-format, and test-open.  These are removed by
 * RemoveLibraryAttributes later.
 * @author DETER team
 * @version 1.0
 */
public class LibraryAttributes extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public LibraryAttributes() { 
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "LibraryAttributes"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls createLibraryAttribute, and\n"+
	    "modifyLibraryAttribute, and removeLibraryAttribute from\n" +
	    "the Libraries service.  It checks parameters for\n" +
	    "createLibraryAttribute and uses createLibraryAttribute\n" +
	    "to create attributes used by later tests.  Then it confirms\n" +
	    "that modifyLibraryAttribute checks parameters and\n" +
	    "functions.  At the end of the test 4 new experiment \n" +
	    "attributes exist: test-required, test-readonly,\n"+
	    "test-format, and test-open.  These are removed by\n"+
	    "RemoveLibraryAttributes later.\n"+
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
	LibrariesStub stub = null;
	try {
	    stub = new LibrariesStub(getServiceUrl() + "Libraries");
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
	LibrariesStub.CreateLibraryAttribute req = 
	    new LibrariesStub.CreateLibraryAttribute();

	regressionLogin(uStub, "testadmin", "test", p);

	createLibraryAttribute(stub, null, "string", true, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "without name", p,
		false, DeterFault.request);
	createLibraryAttribute(stub, "Testattr", "string", true, 
		"1024", "description", null, 
		"formatdescription", 100, 0, null, "bad access", p,
		false, DeterFault.request);

	createLibraryAttribute(stub, "testattr", "string", false, 
		Attribute.READ_ONLY, "description", null, 
		"formatdescription", 100, 0, null, "bad no default", p,
		false, DeterFault.request);

	// Fail on permissions

	regressionLogin(uStub, "testnotadmin", "test", p);
	createLibraryAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "bad permissions", 
		p, false, DeterFault.access);

	regressionLogout(uStub, p);
	createLibraryAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "not logged in", 
		p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Create the array of test attributes 
	createLibraryAttribute(stub, "test-required", "string", false, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"formatdescription", 10000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create a read-only test attribute for later tests-->");
	createLibraryAttribute(stub, "test-readonly", "string", false, 
		Attribute.READ_ONLY, 
		"required non-writable test attr", null, 
		"formatdescription", 11000, 0, "4", "good", 
		p, true, DeterFault.none);
	p.println("<!-- create a read-write test attribute with a format " +
		"for later tests-->");

	createLibraryAttribute(stub, "test-format", "string", true, 
		Attribute.READ_WRITE, 
		"non-required writable test attr", "^[0-9]+$", 
		"Numbers Only", 12000, 0, "4", "good", 
		p, true, DeterFault.none);

	p.println("<!-- create an unconstrained test attribute " +
		"for later tests-->");
	createLibraryAttribute(stub, "test-open", "string", true, 
		Attribute.READ_WRITE, 
		"required writable test attr", null, 
		"None Only", 13000, 0, "4", "good", 
		p, true, DeterFault.none);

	// Modify existing attribute - bad permissions

	regressionLogin(uStub, "testnotadmin", "test", p);

	LibrariesStub.ModifyLibraryAttribute mReq = 
	    new LibrariesStub.ModifyLibraryAttribute();
	p.println("<!-- modify a test attribute bad permissions -->");

	modifyLibraryAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	modifyLibraryAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	// Modify existing attribute
	modifyLibraryAttribute(stub, "test-readonly", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, true, DeterFault.none);

	p.println("<!-- modify a test attribute that doesn't exist-->");
	modifyLibraryAttribute(stub, "testattr2", "string", true, 
		Attribute.READ_ONLY, "description",
		null, "format description", 100, 0, 
		"good", p, false, DeterFault.request);
	regressionLogout(uStub, p);
	p.flush();
	p.close();
    }
}

