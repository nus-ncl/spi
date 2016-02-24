package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;
import net.deterlab.testbed.client.ProjectsStub;

import org.apache.axis2.AxisFault;

/**
 * Create an admin user (testadmin) and a non admin user (testnotadmin) used in
 * later tests.  They are adding to existing admin and regression projects that
 * are already approved.  This is less a test than a setup requirement.
 * @author DETER team
 * @version 1.0
 */
public class AdminUser extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public AdminUser() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "AdminUser"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"Create an admin user (testadmin) and a non admin user\n"+
		"(testnotadmin) used in later tests.  They are adding to\n"+
		"existing admin and regression projects that are already\n"+
		"approved.  This is less a test than a setup requirement.\n"+
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
	logSOAP(pStub, p);
	summary(p);
	UsersStub.Attribute[] profile = getProfileDescription(uStub, 
		"good", p, true, DeterFault.none);

	// Initialize the nameToAttr Map
	for (UsersStub.Attribute a: profile) 
	    nameToAttr.put(a.getName(), a);

	nameToAttr.get("name").setValue("Test User");
	nameToAttr.get("phone").setValue("123");
	nameToAttr.get("email").setValue("faber@isi.edu");

	createUserNoConfirm(uStub, "testadmin", profile, "test", null, null,
		"create admin user", p, true, DeterFault.none);

	createUserNoConfirm(uStub, "testnotadmin", profile, "test", null, null,
		"create notadmin user", p, true, DeterFault.none);

	addUsersNoConfirm(pStub, "admin", new String[] { "testadmin" },
		new String[] { "ALL_PERMS" },
		"add admin user", p, true,
		DeterFault.none,
		new MemberResp[] {
		    new MemberResp("testadmin", true),
		});
	addUsersNoConfirm(pStub, "regression", new String[] { "testnotadmin" },
		new String[] { "ALL_PERMS" },
		    "add admin user", p, true,
		DeterFault.none,
		new MemberResp[] {
		    new MemberResp("testnotadmin", true),
		});
	// Login as the admin user
	regressionLogin(uStub, "testadmin", "test", p);
	regressionLogout(uStub, p);
    }
}
