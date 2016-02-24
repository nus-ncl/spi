package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * This test calls changePassword to modify a user's password and confirms the
 * change via a sequence of requestChallenge and challengeResponse calls.  The
 * first few changePassword calls intentionally fail because of missing and bad
 * aprameters.  At the end the admin0 user's password has been changed to
 * newpass.
 * @author DETER team
 * @version 1.0
 */
public class ChangePassword extends RegressionTest {
    /**
     * Create a new regression test
     */
    public ChangePassword() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ChangePassword"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls changePassword to modify a user's password and\n"+
	    "confirms the change via a sequence of requestChallenge and\n"+
	    "challengeResponse calls.  The first few changePassword calls\n"+
	    "intentionally fail because of missing and bad parameters.  At\n"+
	    "the end the admin0 user's password has been changed to newpass.\n"+
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

	String xlate = "newpass";
	summary(p);

	regressionLogin(uStub, "admin0", "test", p);
	changePassword(uStub, null, xlate, "no id", p, false,
		DeterFault.request);
	changePassword(uStub, "admin0", null, "no password", p, false,
		DeterFault.request);
	changePassword(uStub, "testabdebcel", xlate, "bad user/bad perms", 
		p, false, DeterFault.access);
	changePassword(uStub, "admin0", xlate, "good", p, true,
		DeterFault.none);
	regressionLogin(uStub, "admin0", xlate, p);
	regressionLogout(uStub, p);
    }
}
