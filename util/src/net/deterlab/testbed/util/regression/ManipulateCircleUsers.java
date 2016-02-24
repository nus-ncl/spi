package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression that exercises all the choices for manipulating the
 * contents of a circle.
 * @author DETER team
 * @version 1.0
 */
public class ManipulateCircleUsers extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public ManipulateCircleUsers() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ManipulateCircleUsers"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	"This test calls addUsersNoConfirm, viewCircles, addUsers,\n"+
	"addUserConfirm, oinCircle, joinCircleConfirm, changePermissions,\n"+
	"setOwner, and removeUsers from the Circles service.  It\n"+
	"incidentally calls getUserNotifications from the Users service as\n"+
	"well. This test exercises all the choices for manipulating the\n"+
	"contents of a circle.  Each call has its parameter checking worked\n"+
	"out and then is executed successfully.  First the various adding\n"+
	"mechanisms.  For AddUsers and joinCircle which require the user to\n"+
	"check notifications, we do that as well.  Additionally we check the\n"+
	"permission changing and ownership setting.  Finally we remove some\n"+
	"users from circles (not the system) as well.\n"+
	    "-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	List<MemberDesc> expectedMembers = new ArrayList<MemberDesc>();
	CirclesStub cStub = null;
	UsersStub uStub = null;
	try {
	    cStub = new CirclesStub(getServiceUrl() + "Circles");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(cStub, trace);
	logSOAP(uStub, s);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	summary(p);

	regressionLogin(uStub, "test0", "test", p);
	String[] allPerms = getValidPermissions(cStub, "for later expansion",
		p, true, DeterFault.none);
	addUsersNoConfirm(cStub, null, new String[] { "test" }, 
		new String[] { "ALL_PERMS" }, "no circle", p,
		false, DeterFault.request, null);
	
	addUsersNoConfirm(cStub, "cisforcookie", 
		new String[] { "test" }, 
		new String[] { "ALL_PERMS" },
		"bad circleid format", p, false, DeterFault.request, null);
	addUsersNoConfirm(cStub, "regression0:cisforcookie", 
		new String[] { "test" },
		new String[] { "ALL_PERMS" },
		"bad circleid nonexistent", p, false, 
		DeterFault.request, null);

	addUsersNoConfirm(cStub, "regression0:circle", 
		null, new String[] { "ALL_PERMS" },
		"no uids", p, false, DeterFault.request, null);
	addUsersNoConfirm(cStub, "regression0:circle", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] {"ALL_PERMS" },
		"bad permissions", p, false, DeterFault.access, 
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	addUsersNoConfirm(cStub, "regression0:circle", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "Badpermission" }, 
		"bad permission string", p, false, DeterFault.request, 
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogin(uStub, "testadmin", "test", p);
	addUsersNoConfirm(cStub, "regression0:circle", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ALL_PERMS" },
		"good", p, true, DeterFault.none,
		new MemberResp[] { 
		    new MemberResp("test", true),
		    new MemberResp("test0", false),
		    new MemberResp("23test23", false),
		});
	regressionLogin(uStub, "test0", "test", p);
	viewCircles(cStub, "test", null, "bad permissions", p, false, 
		DeterFault.access, null);

	regressionLogout(uStub, p);

	viewCircles(cStub, "test0", null, "not logged in", p, false, 
		DeterFault.login, null);

	regressionLogin(uStub, "test0", "test", p);

	expectedMembers.add(new MemberDesc("test", allPerms));
	expectedMembers.add(new MemberDesc("test0", allPerms));

	viewCircles(cStub, "test0", null, "no regexp", p, true, 
		DeterFault.none,
		new String[] {
		    "test0:test0",
		    "test0:regression0",
		    "regression0:regression0",
		    "regression0:circle"});
	// Check the actual membership of regression0
	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");



	// Now confirm that an addUsers cycle works.  First parameter checking
	// - exactly the same as above.
	regressionLogin(uStub, "test0", "test", p);
	addUsers(cStub, null, new String[] { "test" }, 
		new String[] { "ADD_USER" }, "CircleChallenge:",
		"no circle", p, false, DeterFault.request, null);
	
	addUsers(cStub, "cisforcookie", 
		new String[] { "test" },
		new String[] { "ADD_USER" },
		"CircleChallenge:",  "bad circleid format", p, 
		false, DeterFault.request, null);
	addUsers(cStub, "regression0:cisforcookie", 
		new String[] { "test" },
		new String[] { "ADD_USER" },
		"CircleChallenge:",  "bad circleid nonxistent", p, 
		false, DeterFault.request, null);

	addUsers(cStub, "regression0:circle", 
		null, new String[] { "ADD_USER" },
		"CircleChallenge:",  
		"no uids", p, false, DeterFault.request, null);
	regressionLogin(uStub, "faber0", "test", p);
	addUsers(cStub, "regression0:circle", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ADD_USER" }, "CircleChallenge:",
		"bad permissions", p, false, DeterFault.access, 
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});
	addUsers(cStub, "regression0:circle", 
		new String[] { "test0", "faber0", "23test23" }, 
		new String[] {  "bad string" }, "CircleChallenge:",  
		"bad permissions string", p, false, DeterFault.request,
		new MemberResp[] { 
		    new MemberResp("test0", false),
		    new MemberResp("faber0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogin(uStub, "test0", "test", p);
	addUsers(cStub, "regression0:circle", 
		new String[] { "test0", "faber0", "23test23" }, 
		new String[] { "ADD_USER" }, "CircleChallenge:",
		"good", p, true, DeterFault.none,
		new MemberResp[] { 
		    new MemberResp("test0", false),
		    new MemberResp("faber0", true),
		    new MemberResp("23test23", false),
		});

	// Make sure the user hasn't really been added

	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");

	// OK, now let's really do the addtion.  Get the notifications for
	// faber0 and find the challenge
	regressionLogin(uStub, "faber0", "test", p);
	long chall = getChallengeFromNotifications(uStub, "faber0", 
		"CircleChallenge:(\\S+)", "get challenge", p);
	// We have the challenge.  Make sure replying to the wrong one fails
	// and then reply to the right one and confirm that it worked.
	
	addUserConfirm(cStub, 101010L, "wrong challenge", p, false, 
		DeterFault.request);
	addUserConfirm(cStub, chall, "correct challenge", p, true,
		DeterFault.none);
	addUserConfirm(cStub, chall, "correct challenge - replay", p,
		false, DeterFault.request);

	// Make sure the user has really been added
	expectedMembers.add(new MemberDesc("faber0",
		    new String[] { "ADD_USER"}));

	checkCircle("faber0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");

	// Now confirm that an JoinCircle cycle works.  First parameter
	// checking -  similar.
	

	joinCircle(cStub, null, null, null, "no circle", p, 
		false, DeterFault.request);
	joinCircle(cStub, "cisforcookie", null, null, 
		"bad circleid format", p, false, DeterFault.request);
	joinCircle(cStub, "regression0:cisforcookie", null, null, 
		"bad circleid non-existent", p, false, DeterFault.request);

	joinCircle(cStub, "regression0:circle", null, null, "no uid", 
		p, false, DeterFault.request);
	joinCircle(cStub, "regression0:circle", "23test23", null, 
		"no such uid", p, false, DeterFault.access);

	joinCircle(cStub, "regression0:circle", "test", null, 
		"already a member", p, false, DeterFault.request);

	joinCircle(cStub, "regression0:circle", "admin0", null, 
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	joinCircle(cStub, "regression0:circle", "admin0", null, 
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "admin0", "test", p);
	joinCircle(cStub, "regression0:circle", "admin0", "CircleChallenge:", 
		"good", p, true, DeterFault.none);

	// Make sure the user hasn't really been added

	regressionLogin(uStub, "test0", "test", p);
	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");

	// OK, now let's really do the addtion.  Get the notifications for
	// test0 (who has add rights) and find the challenge
	chall = getChallengeFromNotifications(uStub, "test0",
		"CircleChallenge:(\\S+)", "get challenge", p);

	// We have the challenge.  Make sure replying to the wrong one fails
	// and then reply to the right one and confirm that it worked.

	joinCircleConfirm(cStub, 101010L,
		new String[] { "REMOVE_USER" },
		"wrong challenge", p, false, DeterFault.request);

	joinCircleConfirm(cStub, chall,
		new String[] { "REMOVE_USER" },
		"correct challenge", p, true, DeterFault.none);

	joinCircleConfirm(cStub, chall,
		new String[] { "REMOVE_USER" },
		"correct challenge - replay ", p, false, DeterFault.request);
	// Make sure the user has really been added
	expectedMembers.add(new MemberDesc("admin0",
		    new String[] { "REMOVE_USER" }));

	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");

	// Confirm that changePermissions works - first check parameter
	// checking then make a change and confirm it.  (note still logged in
	// as test)
	

	changePermissions(cStub, null, new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" },
		"no circleid", p, 
		false, DeterFault.request, null);

	changePermissions(cStub, "cisforcookie",
		new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" },
		"bad circleid format",
		p, false, DeterFault.request, null);

	changePermissions(cStub, "regression0:cisforcookie",
		new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" },
		"bad circleid nonexistent",
		p, false, DeterFault.request, null);

	changePermissions(cStub, "regression0:circle",
		null,
		new String[] { "REALIZE_EXPERIMENT" },
		"no uids", p, 
		false, DeterFault.request, null);

	changePermissions(cStub, "regression0:circle",
		new String[] { "test", "23test23" },
		new String[] {  "badstring" }, "bad permission string",
		p, false, DeterFault.request,
		new MemberResp[] {
		    new MemberResp("test", true), 
		    new MemberResp("23test23", false), 
		});

	changePermissions(cStub, "regression0:circle",
		new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" }, "good",
		p, true, DeterFault.none,
		new MemberResp[] {
		    new MemberResp("test", true), 
		    new MemberResp("23test23", false), 
		});

	for (MemberDesc m: expectedMembers)
	    if ( m.name.equals("test"))
		m.setPermissions(new String[] { "REALIZE_EXPERIMENT" });

	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");

	regressionLogin(uStub, "admin0", "test", p);
	changePermissions(cStub, "regression0:circle",
		new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" },
		"bad permissions",
		p, false, DeterFault.access, null);
	regressionLogout(uStub, p);
	changePermissions(cStub, "regression0:circle",
		new String[] { "test", "23test23" },
		new String[] { "REALIZE_EXPERIMENT" },
		"not logged in",
		p, false, DeterFault.login, null);

	// change the owner
	regressionLogin(uStub, "test0", "test", p);

	setOwner(cStub, null, "test", "no circleid", p, false, 
		DeterFault.request);
	setOwner(cStub, "cisforcookie", "test", 
		"bad circleid format", p, false, DeterFault.request);
	setOwner(cStub, "regression0:cisforcookie", "test", 
		"bad circleid non-existent", p, false, DeterFault.request);

	setOwner(cStub, "regression0:cisforcookie", "tst0", 
		"owner not in circle", p, false, DeterFault.request);


	// Make sure none of those actually worked
	checkCircle("test0", "regression0:circle", cStub, "test0", 
		expectedMembers, p, "check regression0:circle membership");
	// change the owner
	setOwner(cStub, "regression0:circle", "test", 
		"good", p, true, DeterFault.none);

	// Make sure the correct change worked 
	checkCircle("test0", "regression0:circle", cStub, "test", 
		expectedMembers, p, "check regression0:circle membership");

	// not the owner anymore...
	setOwner(cStub, "regression0:circle", "test", 
		"bad permissions - not the owner anymore", p, false,
		DeterFault.access);


	// Remove users
	removeUsers(cStub, null, new String[] { "test", "test0", "23test23" },
		"no circleid", p, false, DeterFault.request, null);
	removeUsers(cStub, "cisforcookie", 
		new String[] { "test", "test0", "23test23" },
		"bad circleid format", p, false, DeterFault.request, null);
	removeUsers(cStub, "regression0:cisforcookie", 
		new String[] { "test", "test0", "23test23" },
		"bad circleid nonexistent", p, false, DeterFault.request, null);

	removeUsers(cStub, "regression0:circle", null,
		"no users", p, false, DeterFault.request, null);

	regressionLogin(uStub, "faber0", "test", p);
	removeUsers(cStub, "regression0:circle", 
		new String[] { "test", "test0", "23test23" },
		"bad permissions", p, false, DeterFault.access, null);

	regressionLogin(uStub, "test0", "test", p);
	removeUsers(cStub, "regression0:circle",
		new String[] { "test", "test0", "23test23" },
		"good", p, true, DeterFault.none,
		new MemberResp[] {
		    new MemberResp("test", false),
		    new MemberResp("23test23", false),
		    new MemberResp("test0", true),
		});
	List<MemberDesc> tmp = new ArrayList<MemberDesc>();

	for (MemberDesc m: expectedMembers)
	    if (!m.getUid().equals("test0")) tmp.add(m);
	expectedMembers = tmp;

	regressionLogin(uStub, "test", "test", p);
	checkCircle("test", "regression0:circle", cStub, "test", 
		expectedMembers, p, "check regression0:circle membership");
	regressionLogout(uStub, p);

    }
}
