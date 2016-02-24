package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls RemoveUser in the Users service.  It confirms that
 * RemoveUser checks its parameters then Uses it to remove the attributes added
 * by UserAttributes.
 * @author DETER team
 * @version 1.0
 */
public class RemoveUserAttributes extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveUserAttributes() { 
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveUserAttributes"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls RemoveUser in the Users service.\n"+
	    "It confirms that RemoveUser checks its parameters then\n"+
	    "Uses it to remove the attributes added by \n"+
	    "UserAttributes.\n"+
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
	regressionLogin(uStub, "testadmin", "test", p);
	removeUserAttribute(uStub, null, "no name",
		p, false, DeterFault.request);
	removeUserAttribute(uStub, "testattr2", "doesn't exist",
		p, false, DeterFault.request);

	regressionLogin(uStub, "testnotadmin", "test", p);
	removeUserAttribute(uStub, "test-required", "bad permssions",
		p, false, DeterFault.access);
	regressionLogout(uStub, p);
	removeUserAttribute(uStub, "test-required", "bad permssions", 
		p, false, DeterFault.login);

	// Remove existing attributes (the ones created in UserAttributes)
	regressionLogin(uStub, "testadmin", "test", p);
	for (String attr: new String[] {
	    "test-required", "test-readonly", "test-format", "test-open" } ) {
	    removeUserAttribute(uStub, attr, "good", p, true, DeterFault.none);
	}
	regressionLogout(uStub, p);
	p.flush();
	p.close();
    }
}

