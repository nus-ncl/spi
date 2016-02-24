package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression tests for user creation.
 * @author DETER team
 * @version 1.0
 */
public class MakeUsers extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public MakeUsers() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeUsers"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls getProfileDescription and createUser\n"+
	    "from the Users service.  getProfileDescription acquires the\n"+
	    "profile and subsequent createUser calls test that\n"+
	    "call.  The test confirms that a profile must be present,\n"+
	    "contain all required fields, and that those fields are properly\n"+
	    "formatted.  Then a createUser request is made for a\n"+
	    "new uid that will return that uid.  The same call is made\n"+
	    "again, and succeeds returning a different uid.  Then a\n"+
	    "createUser request is made with no uid specified and\n"+
	    "one is derived from the e-mail address in the profile.\n"+
	    "When this test is complete 3 new users exist: testtest,\n"+
	    "testtest0, and faber2\n"+
	    "-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	UsersStub uStub = null;
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(uStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	// We copy the specific user's values into the array of attributes
	// returned from GetProfileDescription.  This map lets us easliy
	// put the values in the right places.
	Map<String, UsersStub.Attribute> nameToAttr = 
	    new HashMap<String, UsersStub.Attribute>();

	summary(p);
	UsersStub.Attribute[] profile = getProfileDescription(uStub,
		"good", p, true, DeterFault.none);
	UsersStub.Attribute[] extra_field =
	    new UsersStub.Attribute[profile.length+1];

	for ( int i = 0; i < profile.length; i++)
	    extra_field[i] = profile[i];

	extra_field[profile.length] = new UsersStub.Attribute();
	extra_field[profile.length].setName("unknown_attribute");
	extra_field[profile.length].setAccess("READ_WRITE");
	extra_field[profile.length].setDataType("STRING");
	extra_field[profile.length].setDescription("STRING");
	extra_field[profile.length].setOptional(true);



	// Initialize the nameToAttr Map
	for (UsersStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("name").setValue("Test User");
	nameToAttr.get("phone").setValue("123");
	nameToAttr.get("email").setValue("faber@isi.edu");
	nameToAttr.get("test-readonly").setValue("here");

	createUser(uStub, "testtest", profile, "http://url/", 
		"missing field in profile", p, false, DeterFault.request);

	nameToAttr.get("test-required").setValue("here");
	nameToAttr.get("test-format").setValue("bob");
	createUser(uStub, "testtest", profile, "http://url/", 
		"misformatted field in profile", p, false, DeterFault.request);
	createUser(uStub, "testtest", extra_field, "http://url/",
		"unknown attribute in profile", p, false, DeterFault.request);

	p.println("<!-- createUser (get uid asked for) -->");
	nameToAttr.get("test-format").setValue("123");
	createUser(uStub, "testtest", profile, "http://url/", 
		"get uid requested", p, true, DeterFault.none);
	createUser(uStub, "testtest", profile, "http://url/", 
		"do not get uid requested", p, true, DeterFault.none);
	createUser(uStub, null, profile, "http://url/", 
		"uid from e-mail", p, true, DeterFault.none);
    }
}
