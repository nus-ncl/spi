package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

import org.apache.log4j.Logger;

/**
 * This test calls getProfileDescription, getCircleProfile, and changeCircleProfile
 * from the Circles service.  The test reads the profile description and makes
 * sure that getCircleProfile checks its arguments and succeeds.  Then various
 * attempts are made to modify attributes confirming that READ_ONLY, optional,
 * and formats are respected.
 * @author DETER team
 * @version 1.0
 */
public class CircleProfile extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public CircleProfile() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "CircleProfile"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
		"This test calls getProfileDescription, getCircleProfile,\n"+
		"and changeCircleProfile from the Circles service.  The \n"+
		"test reads the profile description and makes sure that\n"+
		"getCircleProfile checks its arguments and succeeds.  Then\n"+
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
	CirclesStub pStub = null;
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
	CirclesStub.ChangeAttribute ca = new CirclesStub.ChangeAttribute();

	logSOAP(uStub, p);
	summary(p);
	regressionLogin(uStub, "test", "test", p);
	    
	getProfileDescription(pStub, "good", p, true, DeterFault.none);

	getCircleProfile(pStub, null, "no id", p, false, DeterFault.request);
	getCircleProfile(pStub, "testjkadfhlkh", "bad id format", p, false,
		DeterFault.request);
	getCircleProfile(pStub, "regression0:cisforcookie", 
		"bad id nonexistent", p, false, DeterFault.request);
	getCircleProfile(pStub, "regression0:circle", "good", p, true,
		DeterFault.request);

	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeCircleProfile(pStub, null, 
		new CirclesStub.ChangeAttribute[] { ca },
		"no circleid", p, false, DeterFault.request, null);

	changeCircleProfile(pStub, "testjkadfhlkh", 
		new CirclesStub.ChangeAttribute[] { ca },
		"bad circleid format", p, false, DeterFault.request, null);

	changeCircleProfile(pStub, "regression0:cisforcookie", 
		new CirclesStub.ChangeAttribute[] { ca },
		"bad circleid non-existent", p, false, 
		DeterFault.request, null);


	regressionLogout(uStub, p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"not logged in", p, false, DeterFault.login, null);

	regressionLogin(uStub, "test0", "test", p);
	ca.setName("test-open");
	ca.setValue("fred");
	ca.setDelete(true);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"bad permissions", p, false, DeterFault.access, null);

	regressionLogin(uStub, "test", "test", p);
	ca.setName("test-required");
	ca.setValue(null);
	ca.setDelete(true);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"delete required", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-required", false)
		});
	ca.setName("test-readonly");
	ca.setValue("bob");
	ca.setDelete(false);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"not writable", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-readonly", false)
		});

	ca.setName("test-format");
	ca.setValue("bob");
	ca.setDelete(false);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"bad format", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", false)
		});
	ca.setName("test-format");
	ca.setValue("4321");
	ca.setDelete(false);
	changeCircleProfile(pStub, "regression0:circle", 
		new CirclesStub.ChangeAttribute[] { ca },
		"good format", p, true, DeterFault.none,
		new AttrResp[] {
		    new AttrResp("test-format", true)
		});
	regressionLogout(uStub, p);
    }
}
