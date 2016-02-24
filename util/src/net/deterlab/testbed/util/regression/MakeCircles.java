package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Perform a series of tests on CircleAttributes (profile schema
 * manipulation for circle profiles).  The tests are: create a schema
 * (succeeds), create the same schema again (fails), create a new schema
 * that is required, but has no default (fails), modify the initial
 * attribute (succeeds), modify a non-existant attribute (fails), remove an
 * attribute that exists (succeeds), remove one that does not (fails).  The
 * XML exchanges for all of these are logged to the trace file.
 * @author DETER team
 * @version 1.0
 */
public class MakeCircles extends RegressionTest {
    /**
     * Create a new GetVersion regression test
     * @param l the interactive log
     */
    public MakeCircles() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeCircles"; }


    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls getProfileDescription and createCircle from\n"+
	    "the Circles service.  It must be called after MakeUsers as it\n"+
	    "assumes those users are present to act as owners.  The circle\n"+
	    "profile is gathered from getProfileDescription and\n"+
	    "createCircle is called several times with invalid paremeters.\n"+
	    "In order, it is called with a required profile field missing,\n"+
	    "with no profileid to create, with now owner, with an owner name\n"+
	    "that does not exist, and with no profile at all.  Finally a\n"+
	    "successful call is made to create the regression circle.  One\n"+
	    "more call is made to confirm that a circle cannot be created\n"+
	    "with the same name as an existing user (which would confuse the\n"+
	    "circle naming system).  At the end of the test the regression\n"+
	    "circle exists owned by the test user.\n"+
	    "-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	CirclesStub pStub = null;
	UsersStub uStub = null;
	try {
	    pStub = new CirclesStub(getServiceUrl() + "Circles");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(pStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	// We copy the specific circle's values into the array of attributes
	// returned from GetProfileDescription.  This map lets us easliy
	// put the values in the right places.
	Map<String, CirclesStub.Attribute> nameToAttr = 
	    new HashMap<String, CirclesStub.Attribute>();
	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	
	CirclesStub.Attribute[] profile = getProfileDescription(pStub, "good",
		p, true, DeterFault.none);

	CirclesStub.Attribute[] extra_field =
	    new CirclesStub.Attribute[profile.length+1];

	for ( int i = 0; i < profile.length; i++)
	    extra_field[i] = profile[i];

	extra_field[profile.length] = new CirclesStub.Attribute();
	extra_field[profile.length].setName("unknown_attribute");
	extra_field[profile.length].setAccess("READ_WRITE");
	extra_field[profile.length].setDataType("STRING");
	extra_field[profile.length].setDescription("STRING");
	extra_field[profile.length].setOptional(true);


	// Initialize the nameToAttr Map
	for (CirclesStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("description").setValue("Hi - I'm a circle");

	createCircle(pStub, "test:regression0", "test", profile,
		"missing field in profile", p, false, DeterFault.request);
	nameToAttr.get("test-required").setValue("here");
	createCircle(pStub, null, "test", profile,
		"no profile id", p, false, DeterFault.request);
	createCircle(pStub, "regression0", "test", profile,
		"badly constructed name", p, false, DeterFault.request);
	createCircle(pStub, "test:regression0", null, profile,
		"no owner", p, false, DeterFault.request);
	createCircle(pStub, "test:regression0", "23test23", profile,
		"bad owner", p, false, DeterFault.request);
	createCircle(pStub, "test:regression0", "23test23", null,
		"no profile", p, false, DeterFault.request);

	nameToAttr.get("test-format").setValue("here");
	createCircle(pStub, "test:regression0", "test", profile,
		"bad format in profile", p, false, DeterFault.request);
	nameToAttr.get("test-format").setValue("123");
	createCircle(pStub, "test:regression0", "test", extra_field,
		"unknown attribute in profile", p, false, DeterFault.request);
	regressionLogout(uStub, p);
	createCircle(pStub, "test:regression0", "test", profile,
		"not logged in", p, false, DeterFault.login);
	regressionLogin(uStub, "testnotadmin", "test", p);
	createCircle(pStub, "test:regression0", "test", profile,
		"bad permissions", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createCircle(pStub, "test0:regression0", "test0", profile,
		"good", p, true, DeterFault.none);
	regressionLogin(uStub, "admin0", "test", p);
	createCircle(pStub, "regression0:circle", "admin0", profile,
		"bad namespace perms", p, false, DeterFault.access);
	regressionLogin(uStub, "test0", "test", p);
	createCircle(pStub, "regression0:circle", "test0", profile,
		"good", p, true, DeterFault.none);
	regressionLogout(uStub, p);
    }
}
