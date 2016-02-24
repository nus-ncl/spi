package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression to test Password resetting.
 * @author DETER team
 * @version 1.0
 */
public class PasswordReset extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public PasswordReset() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This calls requestPasswordReset and changePasswordChallenge\n"+
	    "from the Users service.  Because the challenge is communicated\n"+
	    "via e-mail, we cannot completely test this from the auto test,\n"+
	    "But we make sure each call vets parameters and execute a\n"+
	    "successful reset request\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "PasswordReset"; }
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

	passwordReset(uStub, null, "http://", "no uid", p,
		false, DeterFault.request);
	passwordReset(uStub, "test", "http://", "good", p,
		true, DeterFault.none);
	changePasswordChallenge(uStub, 1L, null, "no password", p,
		false, DeterFault.request);
	changePasswordChallenge(uStub, 1L, "bob", "bad challenge", p,
		false, DeterFault.request);

    }
}
