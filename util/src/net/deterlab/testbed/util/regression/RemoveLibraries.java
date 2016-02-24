package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls removeLibrary from the Libraries service.  First error
 * conditions are tested: missing eid, badly formatted eid, and then eid that
 * doesn't exist.  Finally the experiment(s) created by MakeLibraries are
 * (successfully) removed.
 * @author DETER team
 * @version 1.0
 */
public class RemoveLibraries extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveLibraries() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls removeLibrary from the Libraries service. \n"+
	    "First error conditions are tested: missing eid, badly formatted\n"+
	    "eid, and then eid that doesn't exist.  Finally the\n"+
	    "experiment(s) created by MakeLibraries are (successfully) \n"+
	    "removed.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveLibraries"; }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	LibrariesStub eStub = null;
	UsersStub uStub = null;
	try {
	    eStub = new LibrariesStub(getServiceUrl() + "Libraries");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(eStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	logSOAP(uStub, p);
	summary(p);

	regressionLogin(uStub, "test", "test", p);
	removeLibrary(eStub, null, "missing eid", p, false, 
		DeterFault.request);
	removeLibrary(eStub, "cisforcookie", "bad eid format", p, false,
		DeterFault.request);
	removeLibrary(eStub, "cisforcookie:goodenough", 
		"bad eid non-existent", p, false, DeterFault.request);

	regressionLogin(uStub, "test", "test", p);
	removeLibrary(eStub, "regression0:circle", "bad permissions", p,
		false,
		DeterFault.access);
	regressionLogout(uStub, p);
	removeLibrary(eStub, "regression0:circle", "bad permissions", p,
		false, DeterFault.login);

	regressionLogin(uStub, "test0", "test", p);
	removeLibrary(eStub, "regression0:circle", "good", p, true,
		DeterFault.none);
	regressionLogin(uStub, "testadmin", "test", p);
	removeLibrary(eStub, "test0:regression0", "good", p, true,
		DeterFault.none);
	removeLibrary(eStub, "test0:noexps", "good", p, true,
		DeterFault.none);
	removeLibrary(eStub, "test0:noacl", "good", p, true,
		DeterFault.none);
	removeLibrary(eStub, "test0:empty", "good", p, true,
		DeterFault.none);
	regressionLogout(uStub, p);
    }
}
