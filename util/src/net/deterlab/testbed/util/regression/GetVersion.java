package net.deterlab.testbed.util.regression;

import net.deterlab.testbed.client.ApiInfoStub;

import java.io.File;
import java.io.PrintStream;

import org.apache.axis2.AxisFault;

/**
 * Try several ApiInfo tests - call getVersion and echo (with and without a
 * parameter.  Trace all the XML to trace.
 * @author DETER Team
 * @version 1.0
 */
public class GetVersion extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public GetVersion() { super(); }
    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "GetVersion"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"This test calls getVersion, echo, getServerCertificate,\n"+
		"and getClientCertificate  from the ApiInfo  service.\n"+
		"echo is called twice, with and without a  parameter, as\n"+
		"is getClientCertificate.  All calls succeed.\n"+
		"-->\n");
    }
    /**
     *  Run the test
     * @param trace the XML trace file
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	ApiInfoStub stub = null;
	try {
	    stub = new ApiInfoStub(getServiceUrl() + "ApiInfo");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	SerializeEnvelope s = logSOAP(stub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	summary(p);
	getVersion(stub, "good", p, true);
	echo(stub, "test", "good", p, true);
	echo(stub, null, "good (null param) ", p, true);
	getServerCertificate(stub, "good", p, true);
	getClientCertificate(stub, null, "good (null param) ", p, true);
	getClientCertificate(stub, "test", "good ", p, true);
	p.flush();
	p.close();
    }
}

