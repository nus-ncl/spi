package net.deterlab.testbed.util.regression;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests adding removing aspects from Libraries.
 * The XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class ManipulateLibraries extends RegressionTest {
    /**
     * Create a new MakeLibraries regression test
     */
    public ManipulateLibraries() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ManipulateLibraries"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls addLibraryAspects and\n"+
	    "removeLibraryExperiments from the Libraries service.  It\n"+
	    "must be called after MakeUsers, Make Experiments, and\n"+
	    "MakeLibraries as it assumes those users and experiments are\n"+
	    "present.  For each routine we first confirm argument\n"+
	    "checking bo omitting various parameters, giving bad values\n"+
	    "and then bad permissions.  ACLs and experiment contesnt are\n"+
	    "manipulates, as well as ownership.\n"+
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
	List<MemberDesc> expectedMembers = null;
	LibrariesStub.AccessMember t0Access =
	    new LibrariesStub.AccessMember();
	LibrariesStub.AccessMember t1Access =
	    new LibrariesStub.AccessMember();
	LibrariesStub.AccessMember t2Access =
	    new LibrariesStub.AccessMember();
	String[] newExperiments = null;
	String[] expectedPerms = new String[] {
	    "REMOVE_EXPERIMENT",
	    "ADD_EXPERIMENT",
	    "READ_LIBRARY",
	    "MODIFY_LIBRARY_ACCESS" };
	String[] expectedT2Perms = {
	    "MODIFY_LIBRARY_ACCESS",
	    "READ_LIBRARY"};


	t0Access.setCircleId("test0:test0");
	t0Access.setPermissions(expectedPerms);

	t1Access.setCircleId("test0:regression0");
	t1Access.setPermissions(expectedPerms);

	t2Access.setCircleId("test:test");
	t2Access.setPermissions(expectedT2Perms);

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

	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	String[] allPerms = getValidPermissions(lStub, "for later expansion",
		p, true, DeterFault.none);
	newExperiments = new String[] { "test0:regression0" };

	addLibraryExperiments(lStub, null, 
		newExperiments,
		"no experiment name", p, false, DeterFault.request);

	addLibraryExperiments(lStub, "test0:regression0", null,
		"no aspects", p, false, DeterFault.request);

	addLibraryExperiments(lStub, "bob", 
		newExperiments,
		"bad experiment name", p, false, DeterFault.request);

	addLibraryExperiments(lStub, "test0:regression10", 
		newExperiments,
		"no such experiment", p, false, DeterFault.request);

	addLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	expectedExperiments = new String[] { "test0:noacl" };
	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms, p,
		"Confirm no changes from failed attempts");



	addLibraryExperiments(lStub, "test0:regression0",
		newExperiments, "good", p, true, DeterFault.request);
	expectedExperiments = new String[] { "test0:noacl", "test0:regression0",};


	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms,
		p, "Check new aspect");

	addLibraryExperiments(lStub, "test0:regression0", 
		newExperiments, "add same name (call succeeds, change fails)",
		p, true, DeterFault.request);

	newExperiments = new String[] {
	    "test0:regression1", "regression0:circle" };
	addLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"add multiple", p, true, DeterFault.request);

	expectedExperiments = new String[] {
	    "test0:noacl", "test0:regression1", "test0:regression0",
	    "regression0:circle" };

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms,
		p, "Check new aspects");


	newExperiments = new String[] { "test0:regression0" };
	regressionLogin(uStub, "test", "test", p);
	removeLibraryExperiments(lStub, null, 
		newExperiments,
		"no experiment name", p, false, DeterFault.request);

	removeLibraryExperiments(lStub, "test0:regression0", null,
		"no experiments", p, false, DeterFault.request);

	removeLibraryExperiments(lStub, "bob", 
		newExperiments,
		"bad library name", p, false, DeterFault.request);

	removeLibraryExperiments(lStub, "test0:regression10", 
		newExperiments,
		"no such library", p, false, DeterFault.request);

	removeLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	newExperiments = new String[] { "test0:fred" };
	removeLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"bad experiment name (call succeeds, delete fails)",
		p, true, DeterFault.request);

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms,
		p, "Confirm no changes");

	newExperiments = new String[] { "test0:regression0" };
	removeLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"good", p, true, DeterFault.request);

	expectedExperiments = new String[] {
	    "test0:noacl", "test0:regression1", "regression0:circle" };

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms,
		p, "Confirm removal");

	newExperiments = new String[] {
	    "test0:regression1", "regression0:circle" };
	removeLibraryExperiments(lStub, "test0:regression0", 
		newExperiments,
		"good multiple removal", p, true, DeterFault.request);

	expectedExperiments = new String[] { "test0:noacl"};

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, null, expectedPerms,
		p, "Confirm multiple removal");

	regressionLogin(uStub, "test", "test", p);
	changeLibraryACL(lStub, null,
		new LibrariesStub.AccessMember[] { t1Access },
		"no experiment name", p, false, DeterFault.request);

	changeLibraryACL(lStub, "test0:regression0", null,
		"no ACL", p, false, DeterFault.request);

	changeLibraryACL(lStub, "bob",
		new LibrariesStub.AccessMember[] { t1Access },
		"bad experiment name", p, false, DeterFault.request);

	changeLibraryACL(lStub, "test0:regression10",
		new LibrariesStub.AccessMember[] { t1Access },
		"no such experiment", p, false, DeterFault.request);

	changeLibraryACL(lStub, "test0:regression0",
		new LibrariesStub.AccessMember[] { t1Access },
		"no permissions", p, false, DeterFault.access);

	regressionLogin(uStub, "test0", "test", p);

	expectedMembers = new ArrayList<MemberDesc>();
	expectedMembers.add(new MemberDesc(t0Access.getCircleId(),
		    t0Access.getPermissions()));

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms,
		p,
		"Confirm no ACL changes");

	changeLibraryACL(lStub, "test0:regression0",
		new LibrariesStub.AccessMember[] { t1Access },
		"change ACL good", p, true, DeterFault.none);

	expectedMembers.add(new MemberDesc(t1Access.getCircleId(),
		    t1Access.getPermissions()));

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms,
		p, "Confirm ACL addition");

	changeLibraryACL(lStub, "test0:regression0",
		new LibrariesStub.AccessMember[] { t2Access },
		"change ACL good", p, true, DeterFault.none);

	expectedMembers.add(new MemberDesc(t2Access.getCircleId(),
		    t2Access.getPermissions()));

	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms, p,
		"Confirm ACL addition");

	regressionLogin(uStub, "testadmin", "test", p);

	checkLibrary("test", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedT2Perms, p,
		"Confirm user ACL");

	regressionLogin(uStub, "test", "test", p);

	newExperiments = new String[] { "test0:noacl"};

	removeLibraryExperiments(lStub, "test0:regression0",
		newExperiments,
		"permissions", p, false, DeterFault.access);

	t2Access.setPermissions(new String[0]);
	changeLibraryACL(lStub, "test0:regression0",
		new LibrariesStub.AccessMember[] { t2Access },
		"remove ACL good", p, true, DeterFault.none);

	expectedMembers.remove(2);

	// Log back in as test0 - test cannot read the experiment.
	regressionLogin(uStub, "test0", "test", p);
	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers, expectedPerms,
		p, "Confirm ACL removal");
	// change the owner

	setOwner(lStub, null, "test", "no eid", p, false, DeterFault.request);
	setOwner(lStub, "cisforcookie", "test",
		"bad eid format", p, false, DeterFault.request);
	setOwner(lStub, "regression0:cisforcookie", "test",
		"bad eid non-existent", p, false, DeterFault.request);


	// Make sure none of those actually worked
	checkLibrary("test0", "test0:regression0", lStub, "test0",
		expectedExperiments, expectedMembers,
		expectedPerms, p, "Confirm no changes");
	// change the owner
	setOwner(lStub, "test0:regression0", "test",
		"setOwner good", p, true, DeterFault.none);

	// Make sure the correct change worked
	checkLibrary("test0", "test0:regression0", lStub, "test",
		expectedExperiments, expectedMembers,
		expectedPerms, p, "Confirm change");

	// not the owner anymore...
	setOwner(lStub, "test0:regression0", "test",
		"bad permissions - not the owner anymore", p, false,
		DeterFault.access);


	regressionLogout(uStub, p);
    }
}
