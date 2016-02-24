package net.deterlab.testbed.util.regression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests on Experiments (profile schema
 * manipulation for experiment profiles and experiment contents). 
 * The XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class MakeExperiments extends RegressionTest {
    /**
     * Create a new MakeExperiments regression test
     */
    public MakeExperiments() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeExperiments"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test creates a series of experiments and tests the\n"+
	    "viewing interface as well.  It must be called after\n"+
	    "MakeUsers as it assumes those users are present to act as\n"+
	    "owners.  The experiment profile is gathered from\n"+
	    "getProfileDescription and createExperiment is called several\n"+
	    "times with invalid paremeters.  Several experiments are\n"+
	    "created with varying aspects to test the various aspect\n"+
	    "implementations.  At conclusion there are 8 new experiments in\n"+
	    "the system: test0:noaspects, test0:empty, test0:noacl,\n"+
	    "test0:regression0, test0:regression1, test0:regression2,\n"+
	    "regression0:circle, and regression0:circle1.  They are removed\n"+
	    "by the RemoveExperiments regression.\n"+
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
	ExperimentsStub.AccessMember t0Access =
	    new ExperimentsStub.AccessMember();
	ExperimentsStub.AccessMember t1Access =
	    new ExperimentsStub.AccessMember();
	File regionXml = new File(dataDir, "region.xml");
	File r2Xml = new File(dataDir, "r2.xml");
	File rbadXml = new File(dataDir, "rbad.xml");
	List<MemberDesc> expectedMembers = new ArrayList<MemberDesc>();
	String[] expectedPerms = new String[] {
	    "MODIFY_EXPERIMENT", "MODIFY_EXPERIMENT_ACCESS", "READ_EXPERIMENT"};

	/* A tricky loop to initialize the layout aspects */
	for (Pair<File, ExperimentsStub.ExperimentAspect> p :
		new FileAspectPair[]  {
		    new FileAspectPair(regionXml, region),
		    new FileAspectPair(r2Xml, r2),
		    new FileAspectPair(r2Xml, r2Dup),
		    new FileAspectPair(rbadXml, rbad),
		}) {
	    try {
		p.second.setType("layout");
		p.second.setData(putBytes(readFile(p.first)));
	    }
	    catch (IOException e) {
		failed("Could not read from " + p.first + ": " +e.getMessage());
	    }
	}

	t0Access.setCircleId("test0:test0");
	t0Access.setPermissions(new String[] { "ALL_PERMS" } );
	t1Access.setCircleId("test0:regression0");
	t1Access.setPermissions(new String[] { "ALL_PERMS" } );
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

	// We copy the specific circle's values into the array of attributes
	// returned from GetProfileDescription.  This map lets us easliy
	// put the values in the right places.
	Map<String, ExperimentsStub.Attribute> nameToAttr = 
	    new HashMap<String, ExperimentsStub.Attribute>();
	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	String[] allPerms = getValidPermissions(eStub, "for later expansion",
		p, true, DeterFault.none);
	
	ExperimentsStub.Attribute[] profile = getProfileDescription(
		eStub, "good", p, true, DeterFault.none);
	ExperimentsStub.Attribute[] extra_field =
	    new ExperimentsStub.Attribute[profile.length+1];

	for ( int i = 0; i < profile.length; i++)
	    extra_field[i] = profile[i];

	extra_field[profile.length] = new ExperimentsStub.Attribute();
	extra_field[profile.length].setName("unknown_attribute");
	extra_field[profile.length].setAccess("READ_WRITE");
	extra_field[profile.length].setDataType("STRING");
	extra_field[profile.length].setDescription("STRING");
	extra_field[profile.length].setOptional(true);


	// Initialize the nameToAttr Map
	for (ExperimentsStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("description").setValue("Hi - I'm an Experiment");

	createExperiment(eStub, "test:regression0", "test", null, null, profile,
		"missing field in profile", p, false, DeterFault.request);
	nameToAttr.get("test-required").setValue("here");
	createExperiment(eStub, null, "test", null, null, profile,
		"no experiment name", p, false, DeterFault.request);
	createExperiment(eStub, "regression0", "test", null, null, profile,
		"badly constructed experiment name no colon (:)", p,
		false, DeterFault.request);
	createExperiment(eStub, "test:regression0", null, null, null, profile,
		"no owner", p, false, DeterFault.request);
	createExperiment(eStub, "test:regression0", "23test23", null, null,
		profile, "bad owner", p, false, DeterFault.request);
	createExperiment(eStub, "test:regression0", "23test23", null, null,
		null, "no profile", p, false, DeterFault.request);

	nameToAttr.get("test-format").setValue("here");
	createExperiment(eStub, "test:regression0", "test", null, null, profile,
		"bad format in profile", p, false, DeterFault.request);
	nameToAttr.get("test-format").setValue("123");
	createExperiment(eStub, "test:regression0", "23test23", null, null,
		extra_field, "unknown attribute in profile", p, false,
		DeterFault.request);
	regressionLogout(uStub, p);
	createExperiment(eStub, "test:regression0", "test", null, null, profile,
		"not logged in", p, false, DeterFault.login);
	regressionLogin(uStub, "testnotadmin", "test", p);
	createExperiment(eStub, "test:regression0", "test", null, null, profile,
		"bad permissions", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createExperiment(eStub, "test0:noaspects", "test0",
		null,
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "no aspects (good)", p, true, DeterFault.request);

	expectedMembers.add(new MemberDesc("test0:test0", allPerms));
	checkExperiment("test0", "test0:noaspects", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check experiment w/o aspects");

	getExperimentProfile(eStub, "test0:noaspects",
		"Confirm read experimentprofile", p, true,
		DeterFault.none);

	expectedMembers.clear();
	expectedAspects = new ExperimentsStub.ExperimentAspect[0];

	createExperiment(eStub, "test0:empty", "test0",
		new ExperimentsStub.ExperimentAspect[0],
		null,
		profile, "empty (good)", p, true, DeterFault.none);

	checkExperiment("test0", "test0:empty", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check empty experiment");

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

	createExperiment(eStub, "test0:noacl", "test0",
		new ExperimentsStub.ExperimentAspect[] { region },
		null,
		profile, "no ACL", p, true, DeterFault.none);

	checkExperiment("test0", "test0:noacl", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check no ACL experiment");

	expectedMembers.add(new MemberDesc("test0:test0", allPerms));
	createExperiment(eStub, "test0:regression0", "test0",
		new ExperimentsStub.ExperimentAspect[] { region, rbad },
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "inconsistent layouts", p, false,
		DeterFault.request);
	createExperiment(eStub, "test0:regression0", "test0",
		new ExperimentsStub.ExperimentAspect[] {
		    initExperimentAspect("unimplemented", "constraints",
			    null, null, "Data".getBytes()),
		},
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "unimplemented aspect", p, false,
		DeterFault.unimplemented);
	createExperiment(eStub, "test0:regression0", "test0",
		new ExperimentsStub.ExperimentAspect[] { region },
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "good", p, true, DeterFault.none);

	checkExperiment("test0", "test0:regression0", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms, p,
		"Check new experiment");

	createExperiment(eStub, "test0:regression1", "test0",
		new ExperimentsStub.ExperimentAspect[] { region, r2, r2Dup },
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "unnamed isomorphic regions", p, true,
		DeterFault.none);
	expectedMembers = new ArrayList<MemberDesc>();
	expectedMembers.add(new MemberDesc("test0:test0", allPerms));

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
	    initExperimentAspect("layout002", "layout", null, null, null),
	    initExperimentAspect("layout002/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout002/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout002/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout002/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout002/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("layout001", "layout", null, null, null),
	    initExperimentAspect("layout001/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout001/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout001/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout001/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout001/minimal_layout", "layout",
		    "minimal_layout", null, null),
	};
	checkExperiment("test0", "test0:regression1", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check unnamed isomorphic");


	r2.setName("R2");
	createExperiment(eStub, "test0:regression2", "test0",
		new ExperimentsStub.ExperimentAspect[] { region, r2, r2Dup },
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "named isomorphic regions", p, true,
		DeterFault.none);


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
	    initExperimentAspect("R2", "layout", null, null, null),
	    initExperimentAspect("R2/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("R2/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("R2/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("R2/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("R2/minimal_layout", "layout",
		    "minimal_layout", null, null),
	    initExperimentAspect("layout001", "layout", null, null, null),
	    initExperimentAspect("layout001/full_layout", "layout",
		    "full_layout", null, null),
	    initExperimentAspect("layout001/fragment/fragment1", "layout",
		    "fragment", null, null),
	    initExperimentAspect("layout001/namemap/R/R-0", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout001/namemap/R", "layout",
		    "namemap", null, null),
	    initExperimentAspect("layout001/minimal_layout", "layout",
		    "minimal_layout", null, null),
	};

	checkExperiment("test0", "test0:regression2", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check named isomorphic");

	viewExperiments(eStub, "test0", null, null, null, true, -1, -1,
		"view no rexgexp, no lib, no count no offset",
		p, true, DeterFault.none, new String[] {
		    "test0:regression0", "test0:regression1",
		    "test0:regression2", "test0:noaspects",
		    "test0:noacl", "test0:empty"
		});
	viewExperiments(eStub, "test0", null, null, null, true, -1, 2,
		"view no rexgexp, no lib, count 2 offset empty",
		p, true, DeterFault.none, new String[] {
		    "test0:noaspects", "test0:empty",
		});
	viewExperiments(eStub, "test0", null, null, null, true, 2, 2,
		"view no rexgexp, no lib, count 2 offset 2",
		p, true, DeterFault.none, new String[] {
		    "test0:noacl", "test0:regression0",
		});
	viewExperiments(eStub, "test0", ".*:regression1", null, null, true,
		-1, -1, "view rexgexp, no lib no count, no offset", p,
		true, DeterFault.none, new String[] { "test0:regression1", });
	regressionLogin(uStub, "admin0", "newpass", p);
	createExperiment(eStub, "regression0:circle", "admin0", null, null,
		profile,
		"bad namespace permissions", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createExperiment(eStub, "regression0:circle", "test0",
		new ExperimentsStub.ExperimentAspect[] { region },
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "good", p, true, DeterFault.none);

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

	checkExperiment("test0", "regression0:circle", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check new experiment");

	createExperiment(eStub, "regression0:circle1", "test0",
		new ExperimentsStub.ExperimentAspect[] {
		    initExperimentAspect("copy", "copy",
			    null, null, "data".getBytes()),
		},
		new ExperimentsStub.AccessMember[] { t0Access },
		profile, "default aspect (copy)", p, true, DeterFault.none);

	expectedAspects = new ExperimentsStub.ExperimentAspect[] {
	    initExperimentAspect("copy", "copy", null, null, null),
	};

	checkExperiment("test0", "regression0:circle1", eStub, "test0",
		expectedAspects, expectedMembers, expectedPerms,
		p, "Check new experiment");

	regressionLogout(uStub, p);
    }
}
