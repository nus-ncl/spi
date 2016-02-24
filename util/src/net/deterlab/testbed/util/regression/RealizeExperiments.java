package net.deterlab.testbed.util.regression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.RealizationsStub;
import net.deterlab.testbed.client.ResourcesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests adding removing aspects from Experiments.
 * The XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class RealizeExperiments extends RegressionTest {
    /**
     * Create a new MakeExperiments regression test
     */
    public RealizeExperiments() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "RealizeExperiments"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test demonstrates the Realization interface.  It is not \n" +
	    "yet full regression test, but more of a demonstration of the \n" +
	    "interface. It will be replaced with a more complete \n" +
	    "regression.\n" +
	    "-->\n");
    }
    /**
     * Simple pair to simiplify init
     */
    private class Pair<F, S> {
	public F first;
	public S second;

	public Pair(F f, S s) {
	    first = f; second = s;
	}
    };

    /**
     * Slight of hand so we can declare an array of these pairs for a for loop.
     */
    private class FileAspectPair extends
	Pair<File, ExperimentsStub.ExperimentAspect> {
	    public FileAspectPair(File f, ExperimentsStub.ExperimentAspect s) {
		super(f, s);
	    }
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	/*
	ExperimentsStub eStub = null;
	RealizationsStub rStub = null;
	ResourcesStub resStub = null;
	UsersStub uStub = null;
	ExperimentsStub.ExperimentAspect r2 =
	    new ExperimentsStub.ExperimentAspect();
	File r2Xml = new File(dataDir, "r2.xml");

	try {
	    r2.setType("layout");
	    r2.setData(putBytes(readFile(r2Xml)));
	}
	catch (IOException e) {
	    failed("Could not read from " + r2Xml+ ": " +e.getMessage());
	}

	try {
	    eStub = new ExperimentsStub(getServiceUrl() + "Experiments");
	    rStub = new RealizationsStub(getServiceUrl() + "Realizations");
	    resStub = new ResourcesStub(getServiceUrl() + "Resources");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(eStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	logSOAP(uStub, s);
	logSOAP(rStub, s);
	logSOAP(resStub, s);
	summary(p);
	ExperimentsStub.Attribute[] profile = getProfileDescription(
		eStub, "good", p, true, DeterFault.none);

	for (ExperimentsStub.Attribute a : profile )
	    a.setValue("23");

	regressionLogin(uStub, "testadmin", "test", p);
	createExperiment(eStub, "test:realizable", "test", 
		new ExperimentsStub.ExperimentAspect[] { r2 },
		new ExperimentsStub.AccessMember[0], profile,
		"Create experiment to realize", p, true, DeterFault.none);
	viewResources(resStub, "testadmin", null, null, null, null,
		null, -1, -1,
		"View all resources before allocation (empty testbed)",
		p, true, DeterFault.none);
	realizeExperiment(eStub, "test:realizable", "test:test", "test",
		new ExperimentsStub.AccessMember[0], null,
		"Realize it", p, true, DeterFault.none);
	viewRealizations(rStub, "testadmin", "", -1, -1,
		"Look at the realization we just made", p,
		true, DeterFault.none);
	try { Thread.sleep(15000); } catch (InterruptedException ignored) { }
	viewRealizations(rStub, "testadmin", "test:realizable-test:test",
		-1, -1,
		"Status will be Initializing (note regexp parameter)", p,
		true, DeterFault.none);
	try { Thread.sleep(25000); } catch (InterruptedException ignored) { }
	viewRealizations(rStub, "testadmin", "test:realizable-test:test",
		-1, -1,
		"Status will be Active", p,
		true, DeterFault.none);
	viewResources(resStub, "testadmin", null, null,
		"test:realizable-test:test", null, null, -1, -1,
		"View realization resources", p, true, DeterFault.none);
	viewResources(resStub, "testadmin", null, null,
		"test:realizable-test:test",
		false, null, -1, -1,
		"View realization virtual resources", p, true, DeterFault.none);
	releaseRealization(rStub, "test:realizable-test:test",
		"Release resources from the realization",
		p, true, DeterFault.none);
	viewRealizations(rStub, "testadmin", "test:realizable-test:test",
		-1, -1,
		"Status will be Empty", p,
		true, DeterFault.none);
	// XXX eventually this moves to a RemoveRealizations test
	removeRealization(rStub, "test:realizable-test:test",
		"Remove the realization", p, true, DeterFault.none);


	regressionLogout(uStub, p);
	*/
    }
}
