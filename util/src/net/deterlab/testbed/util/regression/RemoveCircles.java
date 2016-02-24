package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls removeCircle from the Circles service.  First error
 * conditions are tested: missing circleid, badly formatted circleid, and then
 * circleid that is not present.  Finally the circle(s) created by MakeCircle
 * are (successfully) removed.
 * @author DETER team
 * @version 1.0
 */
public class RemoveCircles extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveCircles() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls removeCircle from the Circles service.  First\n"+
	    "error conditions are tested: missing circleid, badly formatted\n"+
	    "circleid, and then circleid that is not present.  Finally the\n"+
	    "circle(s) created by MakeCircle are (successfully) removed.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveCircles"; }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	CirclesStub cStub = null;
	UsersStub uStub = null;
	try {
	    cStub = new CirclesStub(getServiceUrl() + "Circles");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(cStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	logSOAP(uStub, p);
	summary(p);

	regressionLogin(uStub, "test", "test", p);
	removeCircle(cStub, null, "missing circleid", p, false, 
		DeterFault.request);
	removeCircle(cStub, "cisforcookie", "bad circleid format", p, false,
		DeterFault.request);
	removeCircle(cStub, "cisforcookie:goodenough", 
		"bad circleid non-existent", p, false, DeterFault.request);

	regressionLogin(uStub, "test0", "test", p);
	removeCircle(cStub, "regression0:circle", "bad permissions", p, false,
		DeterFault.access);
	regressionLogout(uStub, p);
	removeCircle(cStub, "regression0:circle", "bad permissions", p, false,
		DeterFault.login);

	regressionLogin(uStub, "test", "test", p);
	removeCircle(cStub, "regression0:circle", "good", p, true,
		DeterFault.none);
	regressionLogin(uStub, "testadmin", "test", p);
	removeCircle(cStub, "test0:regression0", "good", p, true,
		DeterFault.none);
	regressionLogout(uStub, p);
    }
}
