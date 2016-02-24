package net.deterlab.testbed.util.regression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests adding removing aspects from Experiments.
 * The XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class ManipulateExperiments extends RegressionTest {
    /**
     * Create a new MakeExperiments regression test
     */
    public ManipulateExperiments() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ManipulateExperiments"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls addExperimentAspects and\n"+
	    "removeExperimentAspects from the Experiments service.  It must\n"+
	    "be called after MakeUsers and MakeExperiments as it assumes\n"+
	    "those users and experiments are present.  For each routine we\n"+
	    "first confirm argument checking bo omitting various parameters,\n"+
	    "giving bad values and then bad permissions.  Then a single and\n"+
	    "multiple new aspects are added (including an inconsistent\n"+
	    "layout aspect) and the same repeated for removal.  At the\n"+
	    "end of the test, the experiments have the same aspects as they\n"+
	    "did initially.\n"+
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
	ExperimentsStub eStub = null;
	UsersStub uStub = null;
	ExperimentsStub.ExperimentAspect region =
	    new ExperimentsStub.ExperimentAspect();
	ExperimentsStub.ExperimentAspect r2 =
	    new ExperimentsStub.ExperimentAspect();
	ExperimentsStub.ExperimentAspect r2Dup =
	    new ExperimentsStub.ExperimentAspect();
	ExperimentsStub.ExperimentAspect rbad =
	    new ExperimentsStub.ExperimentAspect();
	ExperimentsStub.ExperimentAspect[] expectedAspects = null;
	File regionXml = new File(dataDir, "region.xml");
	File r2Xml = new File(dataDir, "r2.xml");
	File rbadXml = new File(dataDir, "rbad.xml");
	List<MemberDesc> expectedMembers = null;
	ExperimentsStub.AccessMember t0Access =
	    new ExperimentsStub.AccessMember();
	ExperimentsStub.AccessMember t1Access =
	    new ExperimentsStub.AccessMember();
	ExperimentsStub.AccessMember t2Access =
	    new ExperimentsStub.AccessMember();
	String[] expectedPerms = new String[] {
	    "MODIFY_EXPERIMENT",
	    "MODIFY_EXPERIMENT_ACCESS",
	    "READ_EXPERIMENT" };
	String[] expectedT2Perms = new String[] {
	    "MODIFY_EXPERIMENT_ACCESS",
	    "READ_EXPERIMENT" };

	t0Access.setCircleId("test0:test0");
	t0Access.setPermissions(expectedPerms);

	t1Access.setCircleId("test0:regression0");
	t1Access.setPermissions(expectedPerms);

	t2Access.setCircleId("test:test");
	t2Access.setPermissions(expectedT2Perms);

	/* A tricky loop to initialize the layout aspects */
	for (FileAspectPair p: new FileAspectPair[]  {
		new FileAspectPair(regionXml, region),
		new FileAspectPair(r2Xml, r2),
		new FileAspectPair(r2Xml, r2Dup),
		new FileAspectPair(rbadXml, rbad), }) {
	    try {
		p.second.setType("layout");
		p.second.setData(putBytes(readFile(p.first)));
	    }
	    catch (IOException e) {
		failed("Could not read from " + p.first + ": " +e.getMessage());
	    }
	}

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
	String[] allPerms = getValidPermissions(eStub, "for later expansion",
		p, true, DeterFault.none);
	addExperimentAspects(eStub, null, 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no experiment name", p, false, DeterFault.request);

	addExperimentAspects(eStub, "test0:regression0", null,
		"no aspects", p, false, DeterFault.request);

	addExperimentAspects(eStub, "bob", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"bad experiment name", p, false, DeterFault.request);

	addExperimentAspects(eStub, "test0:regression10", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no such experiment", p, false, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	};

	region.setName("newAspect");
	addExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	addExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { rbad },
		"Non isomorphic layout", p, false, DeterFault.request);

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms, p,
		"Confirm no changes from failed attempts");

	addExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] {
		    initExperimentAspect("test", "constraints", null,
			    null, "data".getBytes()),
		},
		"unimplemented aspect", p, false, DeterFault.unimplemented);

	addExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { region },
		"good", p, true, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect", "layout", null, null, null),
	    initExperimentAspect("newAspect/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/minimal_layout", "layout",
		    "minimal_layout", null, null),
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms, p, "Check new aspect");

	addExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"add same name", p, false, DeterFault.request);

	r2.setName("newAspect1");
	r2Dup.setName("newAspect2");
	addExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { r2, r2Dup },
		"add multiple", p, true, DeterFault.request);
	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect", "layout", null, null, null),
	    initExperimentAspect("newAspect/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect1", "layout", null, null, null),
	    initExperimentAspect("newAspect1/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect1/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect1/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect2", "layout", null, null, null),
	    initExperimentAspect("newAspect2/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect2/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect2/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/minimal_layout", "layout",
		    "minimal_layout", null, null),
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms, p, "Check new aspects");

	addExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] {
		    initExperimentAspect("copy", "copy", null, null,
			    "Data".getBytes()),
		    initExperimentAspect("copy1", "copy", null, null,
			    "Data1".getBytes()),
		},
		"add default", p, true, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect", "layout", null, null, null),
	    initExperimentAspect("newAspect/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect1", "layout", null, null, null),
	    initExperimentAspect("newAspect1/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect1/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect1/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect2", "layout", null, null, null),
	    initExperimentAspect("newAspect2/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect2/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect2/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("copy", "copy", null, null, null),
	    initExperimentAspect("copy1", "copy", null, null, null),
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Check default aspect");

	regressionLogin(uStub, "test", "test", p);
	removeExperimentAspects(eStub, null, 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no experiment name", p, false, DeterFault.request);

	removeExperimentAspects(eStub, "test0:regression0", null,
		"no aspects", p, false, DeterFault.request);

	removeExperimentAspects(eStub, "bob", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"bad experiment name", p, false, DeterFault.request);

	removeExperimentAspects(eStub, "test0:regression10", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no such experiment", p, false, DeterFault.request);

	removeExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	region.setName("NoSuchAspect");
	removeExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"bad aspect name", p, false, DeterFault.request);

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm no changes");

	region.setName("newAspect");
	removeExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { region },
		"good", p, true, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect1", "layout", null, null, null),
	    initExperimentAspect("newAspect1/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect1/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect1/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect1/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("newAspect2", "layout", null, null, null),
	    initExperimentAspect("newAspect2/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("newAspect2/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("newAspect2/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("newAspect2/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("copy", "copy", null, null, null),
	    initExperimentAspect("copy1", "copy", null, null, null),
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm removal");

	removeExperimentAspects(eStub, "test0:regression0", 
		new ExperimentsStub.ExperimentAspect[] { r2, r2Dup },
		"good multiple removal", p, true, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("copy", "copy", null, null, null),
	    initExperimentAspect("copy1", "copy", null, null, null),
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm multiple removal");

	regressionLogin(uStub, "test", "test", p);
	changeExperimentAspects(eStub, null,
		new ExperimentsStub.ExperimentAspect[] { region },
		"no experiment name", p, false, DeterFault.request);

	changeExperimentAspects(eStub, "test0:regression0", null,
		"no aspects", p, false, DeterFault.request);

	changeExperimentAspects(eStub, "bob",
		new ExperimentsStub.ExperimentAspect[] { region },
		"bad experiment name", p, false, DeterFault.request);

	changeExperimentAspects(eStub, "test0:regression10",
		new ExperimentsStub.ExperimentAspect[] { region },
		"no such experiment", p, false, DeterFault.request);

	changeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { region },
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	region.setName("layout000");
	changeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { region },
		"layouts don't change", p, false, DeterFault.unimplemented);

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm no changes");

	ExperimentsStub.ExperimentAspect changeAspect = initExperimentAspect(
		"NoSuchAspect", "copy", null, null, "new data".getBytes());

	changeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { changeAspect },
		"bad aspect name", p, false, DeterFault.request);

	changeAspect.setName("copy");
	changeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { changeAspect },
		"good", p, true, DeterFault.none);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    changeAspect,
	    initExperimentAspect("copy1", "copy", null, null, null),
	};
	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm changes");
	changeAspect.setData(putBytes("New Data".getBytes()));
	ExperimentsStub.ExperimentAspect changeAspect1 = initExperimentAspect(
		"copy1", "copy", null, null, "morenew data".getBytes());

	changeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] {
		    changeAspect, changeAspect1 },
		"good multiple changes", p, true, DeterFault.request);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("layout000", "layout", null, null, null),
	    initExperimentAspect("layout000/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout000/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout000/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout000/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    changeAspect,
	    changeAspect1,
	};

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, null, expectedPerms,
		p, "Confirm multiple changes");

	regressionLogin(uStub, "test", "test", p);
	changeExperimentACL(eStub, null,
		new ExperimentsStub.AccessMember[] { t1Access },
		"no experiment name", p, false, DeterFault.request);

	changeExperimentACL(eStub, "test0:regression0", null,
		"no ACL", p, false, DeterFault.request);

	changeExperimentACL(eStub, "bob",
		new ExperimentsStub.AccessMember[] { t1Access },
		"bad experiment name", p, false, DeterFault.request);

	changeExperimentACL(eStub, "test0:regression10",
		new ExperimentsStub.AccessMember[] { t1Access },
		"no such experiment", p, false, DeterFault.request);

	changeExperimentACL(eStub, "test0:regression0",
		new ExperimentsStub.AccessMember[] { t1Access },
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	expectedMembers = new ArrayList<MemberDesc>();
	expectedMembers.add(new MemberDesc(t0Access.getCircleId(),
		    t0Access.getPermissions()));

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm no ACL changes");

	changeExperimentACL(eStub, "test0:regression0",
		new ExperimentsStub.AccessMember[] { t1Access },
		"change ACL good", p, true, DeterFault.none);

	expectedMembers.add(new MemberDesc(t1Access.getCircleId(),
		    t1Access.getPermissions()));

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm ACL addition");

	changeExperimentACL(eStub, "test0:regression0",
		new ExperimentsStub.AccessMember[] { t2Access },
		"change ACL good", p, true, DeterFault.none);

	expectedMembers.add(new MemberDesc(t2Access.getCircleId(),
		    t2Access.getPermissions()));

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm ACL addition");

	regressionLogin(uStub, "testadmin", "test", p);

	checkExperiment("test", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedT2Perms,
		p, "Confirm user ACL");

	regressionLogin(uStub, "test", "test", p);

	region.setName("layout000");
	removeExperimentAspects(eStub, "test0:regression0",
		new ExperimentsStub.ExperimentAspect[] { region },
		"permissions", p, false, DeterFault.access);

	t2Access.setPermissions(new String[0]);
	changeExperimentACL(eStub, "test0:regression0",
		new ExperimentsStub.AccessMember[] { t2Access },
		"remove ACL good", p, true, DeterFault.none);

	expectedMembers.remove(2);

	// Log back in as test0 - test cannot read the experiment.
	regressionLogin(uStub, "test0", "test", p);
	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm ACL removal");
	// change the owner

	setOwner(eStub, null, "test", "no eid", p, false, DeterFault.request);
	setOwner(eStub, "cisforcookie", "test",
		"bad eid format", p, false, DeterFault.request);
	setOwner(eStub, "regression0:cisforcookie", "test",
		"bad eid non-existent", p, false, DeterFault.request);


	// Make sure none of those actually worke
	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm no changes");
	// change the owner
	setOwner(eStub, "test0:regression0", "test",
		"setOwner good", p, true, DeterFault.none);

	// Make sure the correct change worked
	checkExperiment("test0", "test0:regression0", eStub, "test",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Confirm change");

	// not the owner anymore...
	setOwner(eStub, "test0:regression0", "test",
		"bad permissions - not the owner anymore", p, false,
		DeterFault.access);


	regressionLogout(uStub, p);
    }
}
