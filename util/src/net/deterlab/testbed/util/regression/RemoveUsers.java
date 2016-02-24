package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls removeUser from the Users service.  First error
 * conditions are tested: missing userid, badly formatted userid, and then
 * userid that is not present.  Finally the user(s) created by MakeUser
 * are (successfully) removed.
 * @author DETER team
 * @version 1.0
 */
public class RemoveUsers extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveUsers() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls removeUser from the Users service.  First\n"+
	    "error conditions are tested: missing userid, \n"+
	    "and then userid that is not present.  Finally the\n"+
	    "user(s) created by MakeUser are (successfully) removed.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveUsers"; }

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
	removeUser(uStub, null, "missing uid", p, false, DeterFault.request);
	regressionLogin(uStub, "test", "test", p);
	removeUser(uStub, "test0", "bad permissions", p, 
		false, DeterFault.access);
	regressionLogout(uStub, p);
	removeUser(uStub, "test0", "bad permissions", p, 
		false, DeterFault.login);

	regressionLogin(uStub, "test0", "test", p);
	removeUser(uStub, "test0", "good", p, true, DeterFault.none);
	regressionLogin(uStub, "testadmin", "test", p);
	for (String uid: new String[] {
	    "test",
	    "testtest",
	    "testtest0",
	    "faber0",
	    "faber1",
	    "faber2",
	    "admin0",} ) {
	    removeUser(uStub, uid, "good", p, true, DeterFault.none);
	}
	regressionLogout(uStub, p);
    }
}
