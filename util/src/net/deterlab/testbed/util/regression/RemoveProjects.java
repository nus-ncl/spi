package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls removeProject from the Projects service.  First error
 * conditions are tested: missing projectid, badly formatted projectid, and then
 * projectid that is not present.  Finally the project(s) created by MakeProject
 * are (successfully) removed.
 * @author DETER team
 * @version 1.0
 */
public class RemoveProjects extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveProjects() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls removeProject from the Projects service.  First\n"+
	    "error conditions are tested: missing projectid, badly formatted\n"+
	    "projectid, and then projectid that is not present.  Finally the\n"+
	    "project(s) created by MakeProject are (successfully) removed.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveProjects"; }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ProjectsStub pStub = null;
	UsersStub uStub = null;
	try {
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(pStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	logSOAP(uStub, p);
	summary(p);

	regressionLogin(uStub, "testadmin", "test", p);
	removeProject(pStub, null, "missing projectid", p, 
		false, DeterFault.request);
	removeProject(pStub, "cisforcookie:goodenough", 
		"bad projectid format", p, false, DeterFault.request);
	removeProject(pStub, "cisforcookie:goodenough", 
		"bad projectid non-existent", p, false, DeterFault.request);

	regressionLogin(uStub, "test", "test", p);
	removeProject(pStub, "regression0", "bad permissions", p, false,
		DeterFault.access);

	regressionLogout(uStub, p);
	removeProject(pStub, "regression0", "bad permissions", p, false,
		DeterFault.login);
	regressionLogin(uStub, "test0", "test", p);
	for (String pid: new String[] {"regression0"} ) {
	    removeProject(pStub, pid, "good", p, true, DeterFault.none);
	}
	regressionLogout(uStub, p);
    }
}
