package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression that teste the interfaces to project profile interfaces.
 * @author DETER team
 * @version 1.0
 */
public class ProjectProfile extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public ProjectProfile() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ProjectProfile"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"This test calls getProfileDescription, getProjectProfile,\n"+
		"and changeProjectProfile from the Projects service.  The \n"+
		"test reads the profile description and makes sure that\n"+
		"getProjectProfile checks its arguments and succeeds.  Then\n"+
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
	ProjectsStub pStub = null;
	UsersStub uStub = null;
	try {
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(pStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();
	ProjectsStub.ChangeAttribute ca = new ProjectsStub.ChangeAttribute();

	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	    
	getProfileDescription(pStub, "good", p, true, DeterFault.none);

	getProjectProfile(pStub, null, "no id", p, false, DeterFault.request);
	getProjectProfile(pStub, "regression0:cisforcookie", 
		"bad id format", p, false, DeterFault.request);
	getProjectProfile(pStub, "testjkadfhlkh",
		"bad id nonexistent", p, false, DeterFault.request);
	getProjectProfile(pStub, "regression0", "good", p, true, 
		DeterFault.none);


	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeProjectProfile(pStub, null, 
		new ProjectsStub.ChangeAttribute[] { ca },
		"no circleid", p, false, DeterFault.request, null);

	changeProjectProfile(pStub, "regression0:cisforcookie", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"bad circleid format", p, false, DeterFault.request, null);

	changeProjectProfile(pStub, "testjkadfhlkh", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"bad circleid non-existent", p, false,
		DeterFault.request, null);


	regressionLogout(uStub, p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"not logged in", p, false, DeterFault.login, null);

	regressionLogin(uStub, "test", "test", p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"bad permissions", p, false, DeterFault.access, null);

	regressionLogin(uStub, "test0", "test", p);
	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"delete required", p, true, DeterFault.none, 
		new AttrResp[] {
		    new AttrResp("test-required", false)
		});
	ca.setName("test-readonly");
	ca.setValue("bob");
	ca.setDelete(false);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"not writable", p, true, DeterFault.none, 
		new AttrResp[] {
		    new AttrResp("test-readonly", false)
		});

	ca.setName("test-format");
	ca.setValue("bob");
	ca.setDelete(false);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"bad format", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", false)
		});
	ca.setName("test-format");
	ca.setValue("4321");
	ca.setDelete(false);
	changeProjectProfile(pStub, "regression0", 
		new ProjectsStub.ChangeAttribute[] { ca },
		"good format", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", true)
		});
	regressionLogout(uStub, p);
    }
}
