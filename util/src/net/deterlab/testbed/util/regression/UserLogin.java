package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Test the Login facility.
 * @author DETER team
 * @version 1.0
 */
public class UserLogin extends RegressionTest {
    /**
     * Create a new UserLogin regression test
     */
    public UserLogin() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "UserLogin"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls requestChallenge and challengeResponse from\n"+
	    "the Users service.  It mimics a user logging in (or attempting\n"+
	    "to do so).  First several failing requestChallenge calls\n"+
	    "are made - with no uid and with no valid challenge types.  Then\n"+
	    "a call with no types at all is made, which succeeds because\n"+
	    "missing types is taken as the UI having no preference.  That\n"+
	    "challenge is responded to (using challengeResponse) once with\n"+
	    "the wrong challengeID (which is really not responding to it at\n"+
	    "all, and once with no data.  The second call fails, but\n"+
	    "also removes the  challenge.  A replay of the same\n"+
	    "challengeResponse call is made that fails with an\n"+
	    "indication that there is no such challenge.  Another\n"+
	    "requestChallenge call is made with a vaild type (and some\n"+
	    "invalid ones).  That challenge is correctly responded\n"+
	    "to and then a the test tries a replay, which fails.\n"+
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

	UsersStub.RequestChallenge req = new UsersStub.RequestChallenge();
	UsersStub.RequestChallengeResponse resp = null;
	UsersStub.UserChallenge uc = null;
	UsersStub.ChallengeResponse chR = new UsersStub.ChallengeResponse();
	String xlate = "test";
	long chall = 0;
	summary(p);

	requestChallenge(uStub, null, new String[] {"clear"}, "no id", 
		p, false, DeterFault.request);
	requestChallenge(uStub, "test", new String[] {"foo", "bar"}, 
		"bad types", p, false, DeterFault.request);

	chall = requestChallenge(uStub, "test", null, "no types", p,
		true, DeterFault.none);

	challengeResponse(uStub, xlate.getBytes(), 10101L, "wrong id", 
		p, false, DeterFault.request);
	challengeResponse(uStub, null, chall, "no data", p, false,
		DeterFault.request);
	challengeResponse(uStub, null, chall, "no data retry", p, false,
		DeterFault.request);
	

	chall = requestChallenge(uStub, "test", 
		new String[] { "foo", "bar", "clear"}, "good types", p, true,
		DeterFault.none);

	challengeResponse(uStub, xlate.getBytes(), chall, "good", p, true,
		DeterFault.none);
	challengeResponse(uStub, xlate.getBytes(), chall, "replay", p, false,
		DeterFault.request);
	regressionLogout(uStub, p);
    }
}
