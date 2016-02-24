package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;
import net.deterlab.testbed.client.ProjectsStub;

import org.apache.axis2.AxisFault;

import org.apache.log4j.Logger;

/**
 * Regression test for creating users through the no confirmation
 * (system) interfaces.
 * @author DETER team
 * @version 1.0
 */
public class MakeUsersNoConfirm extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public MakeUsersNoConfirm() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "MakeUsersNoConfirm"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls getProfileDescription and createUserNoConfirm\n"+
	    "from the Users service.  getProfileDescription acquires the\n"+
	    "profile and subsequent createUserNoConfirm calls test that\n"+
	    "call.  The test confirms that a profile must be present,\n"+
	    "contain all required fields, and that those fields are properly\n"+
	    "formatted.  Then a createUserNoConfirm request is made for a\n"+
	    "new uid that will return that uid.  The same call is made\n"+
	    "again, and succeeds returning a different uid.  Then a\n"+
	    "createUserNoConfirm request is made with no uid specified and\n"+
	    "one is derived from the e-mail address in the profile.  Then\n"+
	    "calls are made to test the password setting parameters.  A\n"+
	    "createUserNoCOnfirm call is made that successfully creates a\n"+
	    "user with a hashed crypt type password hash (earlier users used\n"+
	    "cleartext passwords).  createUserNoConfirm calls are nade\n"+
	    "to show that bas hashtypes fail, and that passing both\n"+
	    "cleartext and hashed passwords fail, as does passing\n"+
	    "neither.\n"+
	    "When this test is complete 4 new users exist: test, test0, \n"+
	    "faber0, faber1\n"+
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
	ProjectsStub pStub = null;
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
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
	UsersStub.GetProfileDescription descReq = 
	    new UsersStub.GetProfileDescription();
	summary(p);
	regressionLogin(uStub, "testadmin", "test", p);
	UsersStub.Attribute[] profile = getProfileDescription(uStub, 
		"good", p, true, DeterFault.none);

	// Initialize the nameToAttr Map
	for (UsersStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("name").setValue("Test User");
	nameToAttr.get("phone").setValue("123");
	nameToAttr.get("email").setValue("faber@isi.edu");
	nameToAttr.get("test-readonly").setValue("here");

	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"missing field in profile", p, false, DeterFault.request);

	p.println("<!-- createUserNoConfirm (formatting error in profile) -->");
	nameToAttr.get("test-required").setValue("here");
	nameToAttr.get("test-format").setValue("bob");

	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"format error in profile", p, false, DeterFault.request);

	regressionLogin(uStub, "testnotadmin", "test", p);
	nameToAttr.get("test-format").setValue("123");

	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"get uid asked for", p, true, DeterFault.none);
	createUserNoConfirm(uStub, "test", profile, "test", null, null,
		"dont' get uid asked for", p, true, DeterFault.none);
	createUserNoConfirm(uStub, null, profile, "test", null, null,
		"uid from e-mail", p, true, DeterFault.none);
	createUserNoConfirm(uStub, null, profile, null, 
		"$1$t9ASYPHn$gXzGM7bMAw.sRVjY1qlix0);", "crypt",
		"hashed password", p, true, DeterFault.none);
	createUserNoConfirm(uStub, null, profile, null, 
		"$1$t9ASYPHn$gXzGM7bMAw.sRVjY1qlix0);", "fred",
		"bad hash type ", p, false, DeterFault.request);
	createUserNoConfirm(uStub, null, profile, "test", 
		"$1$t9ASYPHn$gXzGM7bMAw.sRVjY1qlix0);", "crypt",
		"both clear and hashed", p, false, DeterFault.request);

	createUserNoConfirm(uStub, null, profile, null, null, null,
		"neither clear nor hashed", p, false, DeterFault.request);

	createUserNoConfirm(uStub, "admin", profile, "test", null, null,
		"projectid conflict (resolved)", p, true, DeterFault.none);
	regressionLogout(uStub, p);
    }
}
