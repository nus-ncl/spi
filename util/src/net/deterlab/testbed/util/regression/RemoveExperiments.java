package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls removeExperiment from the Experiments service.  First error
 * conditions are tested: missing eid, badly formatted eid, and then eid that
 * doesn't exist.  Finally the experiment(s) created by MakeExperiments are
 * (successfully) removed.
 * @author DETER team
 * @version 1.0
 */
public class RemoveExperiments extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveExperiments() {
	super();
    }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls removeExperiment from the Experiments service. \n"+
	    "First error conditions are tested: missing eid, badly formatted\n"+
	    "eid, and then eid that doesn't exist.  Finally the\n"+
	    "experiment(s) created by MakeExperiments are (successfully) \n"+
	    "removed.\n"+
	    "-->\n");
    }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveExperiments"; }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ExperimentsStub eStub = null;
	UsersStub uStub = null;
	try {
	    eStub = new ExperimentsStub(getServiceUrl() + "Experiments");
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
	removeExperiment(eStub, null, "missing eid", p, false, 
		DeterFault.request);
	removeExperiment(eStub, "cisforcookie", "bad eid format", p, false,
		DeterFault.request);
	removeExperiment(eStub, "cisforcookie:goodenough", 
		"bad eid non-existent", p, false, DeterFault.request);

	regressionLogin(uStub, "test", "test", p);
	removeExperiment(eStub, "regression0:circle", "bad permissions", p,
		false,
		DeterFault.access);
	regressionLogout(uStub, p);
	removeExperiment(eStub, "regression0:circle", "bad permissions", p,
		false, DeterFault.login);

	regressionLogin(uStub, "test0", "test", p);
	removeExperiment(eStub, "regression0:circle1", "good", p, true,
		DeterFault.none);
	removeExperiment(eStub, "regression0:circle", "good", p, true,
		DeterFault.none);
	regressionLogin(uStub, "testadmin", "test", p);
	removeExperiment(eStub, "test0:regression0", "good test0:regression0", p, true,
		DeterFault.none);
	removeExperiment(eStub, "test0:regression1", "good", p, true,
		DeterFault.none);
	removeExperiment(eStub, "test0:regression2", "good", p, true,
		DeterFault.none);
	removeExperiment(eStub, "test0:noaspects", "good", p, true,
		DeterFault.none);
	removeExperiment(eStub, "test0:noacl", "good", p, true,
		DeterFault.none);
	removeExperiment(eStub, "test0:empty", "good", p, true,
		DeterFault.none);
	/*
	 * removeExperiment(eStub, "test:realizable", "good", p, true,
		DeterFault.none);
	*/
	regressionLogout(uStub, p);
    }
}
