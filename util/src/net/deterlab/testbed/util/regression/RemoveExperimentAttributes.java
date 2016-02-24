package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/** 
 * This test calls RemoveExperiment in the Experiments service.  It
 * confirms that RemoveExperiment checks its parameters then Uses it to
 * remove the attributes added by ExperimentAttributes.
 * @author DETER team
 * @version 1.0
 */
public class RemoveExperimentAttributes extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public RemoveExperimentAttributes() { 
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RemoveExperimentAttributes"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls RemoveExperiment in the Experiments service.\n"+
	    "It confirms that RemoveExperiment checks its parameters then\n"+
	    "Uses it to remove the attributes added by \n"+
	    "ExperimentAttributes.\n"+
		"-->\n");
    }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ExperimentsStub cStub = null;
	UsersStub uStub = null;
	try {
	    cStub = new ExperimentsStub(getServiceUrl() + "Experiments");
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
	regressionLogin(uStub, "testadmin", "test", p);
	removeExperimentAttribute(cStub, null, "no name", p, false,
		DeterFault.request);
	removeExperimentAttribute(cStub, "testattr2", "doesn't exist", p, false,
		DeterFault.request);

	regressionLogin(uStub, "testnotadmin", "test", p);
	removeExperimentAttribute(cStub, "test-required", "bad permssions", 
		p, false,
		DeterFault.access);

	regressionLogout(uStub, p);
	removeExperimentAttribute(cStub, "test-required", "bad permssions", 
		p, false,
		DeterFault.login);

	// Remove existing attributes (the ones created in ExperimentAttributes)
	regressionLogin(uStub, "testadmin", "test", p);
	for (String attr: new String[] {
	    "test-required", "test-readonly", "test-format", "test-open" } ) {

	    removeExperimentAttribute(cStub, attr, "good", p, true,
		    DeterFault.none);
	}
	regressionLogout(uStub, p);
	p.flush();
	p.close();
    }
}

