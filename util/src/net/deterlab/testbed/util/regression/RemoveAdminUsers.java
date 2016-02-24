package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test removes the temporary admin users from the testbed.  It is less a
 * test than part of the setup and teardown.
 * @author DETER team
 * @version 1.0
 */
public class RemoveAdminUsers extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveAdminUsers() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test removes the temporary admin users from the testbed.\n"+
	    "It is less a test than part of the setup and teardown.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveAdminUsers"; }

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
	regressionLogin(uStub, "testadmin", "test", p);
	for (String uid: new String[] {
	    "testnotadmin",
	    "testadmin" } ) {
	    removeUser(uStub, uid, "good", p, true, DeterFault.none);
	}
	regressionLogout(uStub, p);
    }
}
