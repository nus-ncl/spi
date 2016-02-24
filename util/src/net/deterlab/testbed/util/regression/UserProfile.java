package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Test the user profile interfaces.
 * @author DETER team
 * @version 1.0
 */
public class UserProfile extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public UserProfile() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "UserProfile"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"This test calls getProfileDescription, getUserProfile,\n"+
		"and changeUserProfile from the Users service.  The \n"+
		"test reads the profile description and makes sure that\n"+
		"getUserProfile checks its arguments and succeeds.  Then\n"+
		"various attempts are made to modify attributes confirming\n"+
		"that READ_ONLY, optional, and formats are respected.\n"+
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
	UsersStub.ChangeAttribute ca = new UsersStub.ChangeAttribute();

	summary(p);
	regressionLogin(uStub, "test", "test", p);
	    
	getProfileDescription(uStub, "good", p, true, DeterFault.none);

	getUserProfile(uStub, null, "no id", p, false,
		DeterFault.request);
	getUserProfile(uStub, "testjkadfhlkh", "bad id", p, false, 
		DeterFault.access);
	getUserProfile(uStub, "test", "good", p, true, DeterFault.none);


	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeUserProfile(uStub, null, 
		new UsersStub.ChangeAttribute[] { ca },
		"no uid", p, false, DeterFault.request, null);

	regressionLogout(uStub, p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"not logged in", p, false, DeterFault.login, null);

	regressionLogin(uStub, "test0", "test", p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"bad permissions", p, false, DeterFault.access, null);

	regressionLogin(uStub, "test", "test", p);
	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"delete required", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-required", false)
		});
	ca.setName("test-readonly");
	ca.setValue("bob");
	ca.setDelete(false);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"not writable", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-readonly", false)
		});

	ca.setName("test-format");
	ca.setValue("bob");
	ca.setDelete(false);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"bad format", p, true,DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", false)
		});
	ca.setName("test-format");
	ca.setValue("4321");
	ca.setDelete(false);
	changeUserProfile(uStub, "test", 
		new UsersStub.ChangeAttribute[] { ca },
		"good format", p, true,DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", true)
		});
	regressionLogout(uStub, p);
    }
}
