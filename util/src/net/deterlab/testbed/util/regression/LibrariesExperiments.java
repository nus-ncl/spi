package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests on the interactions between Libraries and
 * Experiments.  The XML exchanges for all of these are logged to the
 * trace file.
 * @author DETER team
 * @version 1.0
 */
public class LibrariesExperiments extends RegressionTest {
    /**
     * Create a new MakeExperiments regression test
     */
    public LibrariesExperiments() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "LibrariesExperiments"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"This test confirms that when experiments and circles are\n"+
		"deleted from the system they are deleted from libraries\n"+
		"that contain them.  The test starts by creating 3\n"+
		"experiments and circles.  The experiments are checked to\n"+
		"be sure they have the specified properties.  Then a\n"+
		"library is created with the new circles and experiments\n"+
		"and checked.  At this point a series of viewExperiments\n"+
		"calls are made to make sure that the offset, count, and\n"+
		"regexp parameters to work properly when scoped by the\n"+
		"library parameter .  Then a circle and an experiment are\n"+
		"removed from the system and the test confirms that they\n"+
		"are no longer listed in the library.  Once that is\n"+
		"complete, the remaining extra circles, experiments and\n"+
		"library are removed.\n"+
	    "-->\n");
    }

    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	CirclesStub cStub = null;
	ExperimentsStub eStub = null;
	LibrariesStub lStub = null;
	UsersStub uStub = null;
	ExperimentsStub.ExperimentAspect[] expectedAspects = null;
	List<MemberDesc> expectedMembers = new ArrayList<MemberDesc>();
	List<LibrariesStub.AccessMember> circles =
	    new ArrayList<LibrariesStub.AccessMember>();
	List<String> expectedExperiments = new ArrayList<String>();
	String[] expectedExpPerms = new String[] {
	    "MODIFY_EXPERIMENT", "MODIFY_EXPERIMENT_ACCESS", "READ_EXPERIMENT"};
	String[] expectedLibPerms = new String[] {
	    "REMOVE_EXPERIMENT", "ADD_EXPERIMENT", "READ_LIBRARY",
	    "MODIFY_LIBRARY_ACCESS" };


	try {
	    cStub = new CirclesStub(getServiceUrl() + "Circles");
	    eStub = new ExperimentsStub(getServiceUrl() + "Experiments");
	    lStub = new LibrariesStub(getServiceUrl() + "Libraries");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(cStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	// We copy the specific profile's values into the array of attributes
	// returned from GetProfileDescription.  These maps let us easliy
	// put the values in the right places.
	Map<String, CirclesStub.Attribute> cNameToAttr = 
	    new HashMap<String, CirclesStub.Attribute>();
	Map<String, ExperimentsStub.Attribute> eNameToAttr = 
	    new HashMap<String, ExperimentsStub.Attribute>();
	Map<String, LibrariesStub.Attribute> lNameToAttr = 
	    new HashMap<String, LibrariesStub.Attribute>();
	logSOAP(eStub, p);
	logSOAP(lStub, p);
	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test0", "test", p);

	String[] allPerms = getValidPermissions(lStub, "for later expansion",
		p, true, DeterFault.none);

	CirclesStub.Attribute[] cProfile = getProfileDescription(
		cStub, "good", p, true, DeterFault.none);
	for (CirclesStub.Attribute a: cProfile) 
	    cNameToAttr.put(a.getName(), a);
	cNameToAttr.get("description").setValue("Hi - I'm a Circle");
	cNameToAttr.get("test-required").setValue("here");

	ExperimentsStub.Attribute[] eProfile = getProfileDescription(
		eStub, "good", p, true, DeterFault.none);
	for (ExperimentsStub.Attribute a: eProfile) 
	    eNameToAttr.put(a.getName(), a);
	eNameToAttr.get("description").setValue("Hi - I'm an Experiment");
	eNameToAttr.get("test-required").setValue("here");

	LibrariesStub.Attribute[] lProfile = getProfileDescription(
		lStub, "good", p, true, DeterFault.none);
	for (LibrariesStub.Attribute a: lProfile) 
	    lNameToAttr.put(a.getName(), a);
	lNameToAttr.get("description").setValue("Hi - I'm a Library");
	lNameToAttr.get("test-required").setValue("here");

	for (int i = 0; i < 3; i++) {
	    LibrariesStub.AccessMember m = new LibrariesStub.AccessMember();
	    String eName = "test0:exp"+i;
	    String cName = "test0:circle"+i;

	    createExperiment(eStub, eName, "test0",
		    null, null, eProfile, "temp experiment", p,
		    true, DeterFault.request);

	    checkExperiment("test0", eName, eStub, "test0",
		    expectedAspects, null, expectedExpPerms,
		    p, "Check new experiment");

	    expectedExperiments.add(eName);

	    createCircle(cStub, cName, "test0", cProfile,
		    "good", p, true, DeterFault.none);

	    m.setCircleId(cName);
	    m.setPermissions(new String[] { "ALL_PERMS" } );
	    circles.add(m);
	    expectedMembers.add(new MemberDesc(m.getCircleId(), allPerms));
	}
	createLibrary(lStub, "test0:lib", "test0",
		expectedExperiments.toArray(new String[0]),
		circles.toArray(new LibrariesStub.AccessMember[0]),
		lProfile, "good", p, true, DeterFault.none);

	checkLibrary("test0", "test0:lib", lStub, "test0",
		expectedExperiments.toArray(new String[0]), expectedMembers,
		expectedLibPerms, p, "Check new library");

	viewExperiments(eStub, "test0", null, "test0:lib", null, true, -1, -1,
		"view no rexgexp, lib",
		p, true, DeterFault.none,
		expectedExperiments.toArray(new String[0]));

	viewExperiments(eStub, "test0", null, "test0:lib", null, true, 1, 1,
		"view no rexgexp, lib, offset, count",
		p, true, DeterFault.none,
		new String[] {
		    expectedExperiments.get(1)});

	viewExperiments(eStub, "test0", "test0:exp1", "test0:lib", null,
		true, -1, -1,
		"view rexgexp, lib, no offset, no count",
		p, true, DeterFault.none,
		new String[] {
		    expectedExperiments.get(1)});

	removeExperiment(eStub, "test0:exp0", "good", p, true,
		DeterFault.none);
	expectedExperiments.remove(0);

	checkLibrary("test0", "test0:lib", lStub, "test0",
		expectedExperiments.toArray(new String[0]), expectedMembers,
		expectedLibPerms, p, "One experiment gone");

	removeCircle(cStub, "test0:circle0", "good", p, true,
		DeterFault.none);
	expectedMembers.remove(0);

	checkLibrary("test0", "test0:lib", lStub, "test0",
		expectedExperiments.toArray(new String[0]), expectedMembers,
		expectedLibPerms, p, "One experiment one circle gone");

	for (int i = 1; i < 3; i++) {
	    removeExperiment(eStub, "test0:exp"+i, "good", p, true,
		    DeterFault.none);
	    removeCircle(cStub, "test0:circle"+i, "good", p, true,
		    DeterFault.none);
	}

	removeLibrary(lStub, "test0:lib", "good", p, true, DeterFault.none);
	regressionLogout(uStub, p);
    }
}
