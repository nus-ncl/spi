package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests on Libraries (profile schema
 * manipulation for experiment profiles and experiment contents). 
 * The XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class MakeLibraries extends RegressionTest {

    /**
     * Create a new MakeExperiments regression test
     */
    public MakeLibraries() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeLibraries"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test creates a series of libraries and tests the\n"+
	    "viewing interface as well.  It must be called after\n"+
	    "MakeUsers, MakeCircles, and MakeExperiments as it assumes\n"+
	    "those elements are present.  The library profile is gathered\n"+
	    "from getProfileDescription and createLibrart is called\n"+
	    "several times with invalid paremeters.  Several libraries\n"+
	    "are created with varying ACLs and experiments.  At\n"+
	    "conclusion there are 5 new libraries in the system:\n"+
	    "test0:noexps, test0:empty, test0:noacl, test0:regression0,\n"+
	    "and regression0:circle.  They are removed by the\n"+
	    "RemoveLibraries regression.\n"+
	    "-->\n");
    }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	LibrariesStub lStub = null;
	UsersStub uStub = null;
	String[] expectedExperiments = null;
	LibrariesStub.AccessMember t0Access = new LibrariesStub.AccessMember();
	LibrariesStub.AccessMember t1Access = new LibrariesStub.AccessMember();
	List<MemberDesc> expectedMembers = new ArrayList<MemberDesc>();
	String[] expectedPerms = new String[] {
	    "REMOVE_EXPERIMENT", "ADD_EXPERIMENT", "READ_LIBRARY",
	    "MODIFY_LIBRARY_ACCESS" };

	t0Access.setCircleId("test0:test0");
	t0Access.setPermissions(new String[] { "ALL_PERMS" } );
	t1Access.setCircleId("test0:regression0");
	t1Access.setPermissions(new String[] { "ALL_PERMS" } );
	try {
	    lStub = new LibrariesStub(getServiceUrl() + "Libraries");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(lStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	// We copy the specific circle's values into the array of attributes
	// returned from GetProfileDescription.  This map lets us easliy
	// put the values in the right places.
	Map<String, LibrariesStub.Attribute> nameToAttr = 
	    new HashMap<String, LibrariesStub.Attribute>();
	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	String[] allPerms = getValidPermissions(lStub, "for later expansion",
		p, true, DeterFault.none);
	
	LibrariesStub.Attribute[] profile = getProfileDescription(
		lStub, "good", p, true, DeterFault.none);
	LibrariesStub.Attribute[] extra_field =
	    new LibrariesStub.Attribute[profile.length+1];

	for ( int i = 0; i < profile.length; i++)
	    extra_field[i] = profile[i];

	extra_field[profile.length] = new LibrariesStub.Attribute();
	extra_field[profile.length].setName("unknown_attribute");
	extra_field[profile.length].setAccess("READ_WRITE");
	extra_field[profile.length].setDataType("STRING");
	extra_field[profile.length].setDescription("STRING");
	extra_field[profile.length].setOptional(true);



	// Initialize the nameToAttr Map
	for (LibrariesStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("description").setValue("Hi - I'm an Library");

	createLibrary(lStub, "test:regression0", "test", null, null, profile,
		"missing field in profile", p, false, DeterFault.request);
	nameToAttr.get("test-required").setValue("here");
	createLibrary(lStub, null, "test", null, null, profile,
		"no library name", p, false, DeterFault.request);
	createLibrary(lStub, "regression0", "test", null, null, profile,
		"badly constructed name, no colon (:)", p,
		false, DeterFault.request);
	createLibrary(lStub, "test:regression0", null, null, null, profile,
		"no owner", p, false, DeterFault.request);
	createLibrary(lStub, "test:regression0", "23test23", null, null,
		profile, "bad owner", p, false, DeterFault.request);
	createLibrary(lStub, "test:regression0", "23test23", null, null,
		null, "no profile", p, false, DeterFault.request);

	nameToAttr.get("test-format").setValue("here");
	createLibrary(lStub, "test:regression0", "test", null, null, profile,
		"bad format in profile", p, false, DeterFault.request);
	nameToAttr.get("test-format").setValue("123");
	createLibrary(lStub, "test:regression0", "test", null, null,
		extra_field, "unknown attribute in profile",
		p, false, DeterFault.request);
	regressionLogout(uStub, p);
	createLibrary(lStub, "test:regression0", "test", null, null, profile,
		"not logged in", p, false, DeterFault.login);
	regressionLogin(uStub, "testnotadmin", "test", p);
	createLibrary(lStub, "test:regression0", "test", null, null, profile,
		"bad permissions", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createLibrary(lStub, "test0:noexps", "test0",
		null,
		new LibrariesStub.AccessMember[] { t0Access },
		profile, "no experiments (good)", p, true, DeterFault.request);

	createLibrary(lStub, "test0:empty", "test0",
		null, null,
		profile, "no ACL or experiments (good)", p,
		true, DeterFault.none);

	checkLibrary("test0", "test0:empty", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms, p,
		"Check empty experiment");

	getLibraryProfile(lStub, "test0:empty",
		"Check Empty Experiment Profile", p, true, DeterFault.none);

	expectedExperiments = new String[] { "test0:noacl" };
	createLibrary(lStub, "test0:noacl", "test0",
		expectedExperiments, null,
		profile, "no ACL", p, true, DeterFault.none);

	checkLibrary("test0", "test0:noacl", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms, p,
		"Check no ACL experiment");

	createLibrary(lStub, "test0:regression0", "test0",
		expectedExperiments,
		new LibrariesStub.AccessMember[] { t0Access },
		profile, "good with acl", p, true, DeterFault.none);

	expectedMembers.add(new MemberDesc("test0:test0", allPerms));

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms,
		p, "Check new experiment");
	viewLibraries(lStub, "test0", null, -1, -1,
		"view no rexgexp no offset no count",
		p, true, DeterFault.none, new String[] {
		    "test0:empty", "test0:noacl",
		    "test0:regression0", "test0:noexps",
		});
	viewLibraries(lStub, "test0", null, -1, 2,
		"view no rexgexp, no offset,count",
		p, true, DeterFault.none, new String[] {
		    "test0:empty", "test0:noexps",
		});
	viewLibraries(lStub, "test0", null, 2, 2,
		"view no rexgexp, offset, count",
		p, true, DeterFault.none, new String[] {
		    "test0:noacl", "test0:regression0",
		});
	viewLibraries(lStub, "test0", ".*:regression0", -1, -1,
		"view rexgexp, no offset, no count", p, true, DeterFault.none,
		new String[] { "test0:regression0", });

	regressionLogin(uStub, "admin0", "newpass", p);
	createLibrary(lStub, "regression0:circle", "admin0", null, null,
		profile,
		"bad namespace perms", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createLibrary(lStub, "regression0:circle", "test0",
		expectedExperiments,
		new LibrariesStub.AccessMember[] { t0Access },
		profile, "good", p, true, DeterFault.none);

	checkLibrary("test0", "regression0:circle", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms, p,
		"Check new experiment");

	regressionLogout(uStub, p);
    }
}
