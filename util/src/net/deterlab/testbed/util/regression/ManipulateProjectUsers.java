package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression that moves users among projects (including ownership
 * changes).
 * @author DETER team
 * @version 1.0
 */
public class ManipulateProjectUsers extends RegressionTest {
    /**
     * Create a new regression test
     * @param l the interactive log
     */
    public ManipulateProjectUsers() {
	super();
    }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "ManipulateProjectUsers"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	"This test calls addUsersNoConfirm, viewProjects, addUsers,\n"+
	"addUserConfirm, oinProject, joinProjectConfirm, changePermissions,\n"+
	"setOwner, and removeUsers from the Projects service.  It\n"+
	"incidentally calls getUserNotifications from the SUers service as\n"+
	"well. This test exercises all the choices for manipulating the\n"+
	"contents of a project.  Each call has its parameter checking worked\n"+
	"out and then is executed successfully.  First the various adding\n"+
	"mechanisms.  For AddUsers and joinProject which require the user to\n"+
	"check notifications, we do that as well.  Additionally we check the\n"+
	"permission changing and ownership setting.  Finally we remove some\n"+
	"users from projects as well.\n"+
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
	List<MemberDesc> expectedCircleMembers = new ArrayList<MemberDesc>();
	CirclesStub cStub = null;
	ProjectsStub pStub = null;
	UsersStub uStub = null;
	try {
	    cStub = new CirclesStub(getServiceUrl() + "Circles");
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(cStub, trace);
	logSOAP(pStub, s);
	logSOAP(uStub, s);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();

	summary(p);

	regressionLogin(uStub, "test", "test", p);
	String[] allPerms = getValidPermissions(pStub, "for later expansion",
		p, true, DeterFault.none);
	String[] allCirclePerms = getValidPermissions(cStub,
		"for later expansion", p, true, DeterFault.none);

	addUsersNoConfirm(pStub, null, new String[] { "test0" }, 
		new String[] { "ALL_PERMS" },
		"no project", p, false, 
		DeterFault.request,
		null);
	
	addUsersNoConfirm(pStub, "project:cisforcookie", 
		new String[] { "test0" },
		new String[] { "ALL_PERMS" },
		"bad projectid format", p, false, DeterFault.request, null);
	addUsersNoConfirm(pStub, "cisforcookie", 
		new String[] { "test0" },
		new String[] { "ALL_PERMS" },
		"bad projectid nonxistent", p, false, DeterFault.request,
		null);

	addUsersNoConfirm(pStub, "regression0", 
		null, new String[] { "ALL_PERMS" },
		"no uids", p, false, DeterFault.request, null);

	addUsersNoConfirm(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ALL_PERMS" },
		"bad permissions", p, false, DeterFault.access, 
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogout(uStub, p);
	addUsersNoConfirm(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ALL_PERMS" },
		"bad permissions", p, false, DeterFault.login, 
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogin(uStub, "testadmin", "test", p);
	addUsersNoConfirm(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "badstring" }, 
		"bad permissions string", p, false, 
		DeterFault.request, new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	addUsersNoConfirm(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ALL_PERMS" },
		"good", p, true, DeterFault.none, new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogin(uStub, "test", "test", p);
	viewProjects(pStub, cStub, "test0", null, "bad permissions", p, false, 
		DeterFault.access, null);

	regressionLogout(uStub, p);

	viewProjects(pStub, cStub, "test0", null, "not logged in", p, false, 
		DeterFault.login, null);

	regressionLogin(uStub, "test", "test", p);
	viewProjects(pStub, cStub, "test", null, "no regexp", p, true, 
		DeterFault.none,
		new String[] { "regression0"});
	// Check the actual membership of regression0
	expectedMembers.add(new MemberDesc("test", allPerms));
	expectedMembers.add(new MemberDesc("test0", allPerms));

	expectedCircleMembers.add(new MemberDesc("test", allCirclePerms));
	expectedCircleMembers.add(new MemberDesc("test0", allCirclePerms));
	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");



	// Now confirm that an addUsers cycle works.  First parameter checking
	// - exactly the same as above.
	regressionLogin(uStub, "test", "test", p);
	addUsers(pStub, null, new String[] { "test0" }, 
		new String[] { "ADD_USER" }, "ProjectChallenge:",
		"no project", p, false, DeterFault.request, null);
	
	addUsers(pStub, "project:cisforcookie", 
		new String[] { "test0" },
		new String[] { "ADD_USER" },
		"ProjectChallenge:",  "bad projectid format", p,
		false, DeterFault.request, null);
	addUsers(pStub, "cisforcookie", 
		new String[] { "test0" },
		new String[] { "ADD_USER" },
		"ProjectChallenge:",  "bad projectid nonxistent", p, 
		false, DeterFault.request, null);

	addUsers(pStub, "regression0", 
		null, new String[] { "ADD_USER" },
		"ProjectChallenge:",  
		"no uids", p, false, DeterFault.request, null);
	regressionLogin(uStub, "faber0", "test", p);
	addUsers(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" }, 
		new String[] { "ADD_USER" },
		"ProjectChallenge:",  
		"bad permissions", p, false, DeterFault.access,
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("test0", true),
		    new MemberResp("23test23", false),
		});

	regressionLogin(uStub, "test", "test", p);
	addUsers(pStub, "regression0", 
		new String[] { "test", "faber0", "23test23" }, 
		new String[] { "badstring" },
		"ProjectChallenge:",  
		"bad permission string", p, false, DeterFault.request,
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("faber0", true),
		    new MemberResp("23test23", false),
		});
	addUsers(pStub, "regression0", 
		new String[] { "test", "faber0", "23test23" }, 
		new String[] { "ADD_USER" },
		"ProjectChallenge:",  
		"good", p, true, DeterFault.none,
		new MemberResp[] { 
		    new MemberResp("test", false),
		    new MemberResp("faber0", true),
		    new MemberResp("23test23", false),
		});

	// Make sure the user hasn't really been added

	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// OK, now let's really do the addtion.  Get the notifications for
	// faber0 and find the challenge
	regressionLogin(uStub, "faber0", "test", p);
	long chall = getChallengeFromNotifications(uStub, "faber0", 
		"ProjectChallenge:(\\S+)", "get challenge", p);
	// We have the challenge.  Make sure replying to the wrong one fails
	// and then reply to the right one and confirm that it worked.
	
	addUserConfirm(pStub, 101010L, "wrong challenge", p, false,
		DeterFault.request);
	addUserConfirm(pStub, chall, "correct challenge", p, true,
		DeterFault.none);
	addUserConfirm(pStub, chall, "correct challenge - replay", p, false,
		DeterFault.request);

	// Make sure the user has really been added
	expectedMembers.add(new MemberDesc("faber0",
		    new String[] {"ADD_USER"}));
	expectedCircleMembers.add(new MemberDesc("faber0",
		    new String[] {"ADD_USER"}));

	checkProject("faber0", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// Now confirm that an JoinProject cycle works.  First parameter
	// checking -  similar.
	

	joinProject(pStub, null, null, null, "no project", p, 
		false, DeterFault.request);
	joinProject(pStub, "project:cisforcookie", null, null, 
		"bad projectid format", p, false, DeterFault.request);
	joinProject(pStub, "cisforcookie", null, null, 
		"bad projectid non-existent", p, false, DeterFault.request);

	joinProject(pStub, "regression0", null, null, "no uid", p,
		false, DeterFault.request);

	joinProject(pStub, "regression0", "23test23", null, 
		"no such uid", p, false, DeterFault.access);

	regressionLogin(uStub, "test", "test", p);
	joinProject(pStub, "regression0", "test", null, 
		"already a member", p, false, DeterFault.request);

	joinProject(pStub, "regression0", "admin0", null, 
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	joinProject(pStub, "regression0", "admin0", null, 
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "admin0", "test", p);
	joinProject(pStub, "regression0", "admin0", "ProjectChallenge:", 
		"good", p, true, DeterFault.none);

	// Make sure the user hasn't really been added

	regressionLogin(uStub, "test", "test", p);
	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// OK, now let's really do the addtion.  Get the notifications for
	// test (who has add rights) and find the challenge
	chall = getChallengeFromNotifications(uStub, "test",
		"ProjectChallenge:(\\S+)", "get challenge", p);

	// We have the challenge.  Make sure replying to the wrong one fails
	// and then reply to the right one and confirm that it worked.

	joinProjectConfirm(pStub, 101010L,
		new String[] { "REMOVE_USER" },
		"wrong challenge", p, false, DeterFault.request);

	joinProjectConfirm(pStub, chall,
		new String[] { "REMOVE_USER" },
		"correct challenge", p, true, DeterFault.none);

	joinProjectConfirm(pStub, chall,
		new String[] { "REMOVE_USER" },
		"correct challenge - replay ", p, false, DeterFault.request);
	// Make sure the user has really been added
	expectedMembers.add(new MemberDesc("admin0",
		    new String[] { "REMOVE_USER" }));
	expectedCircleMembers.add(new MemberDesc("admin0",
		    new String[] { "REMOVE_USER" }));


	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// Confirm that changePermissions works - first check parameter
	// checking then make a change and confirm it.  (note still logged in
	// as test)
	

	changePermissions(pStub, null, new String[] { "test0", "23test23" },
		new String[] { "CREATE_CIRCLE" },
		"no projectid", p, false, 
		DeterFault.request, null);

	changePermissions(pStub, "project:cisforcookie",
		new String[] { "test0", "23test23" },
		new String[] { "CREATE_CIRCLE" },
		"bad projectid format",
		p, false, 
		DeterFault.request, null);

	changePermissions(pStub, "cisforcookie",
		new String[] { "test0", "23test23" },
		new String[] { "CREATE_CIRCLE" },
		"bad projectid nonexistent",
		p, false, 
		DeterFault.request, null);

	changePermissions(pStub, "cisforcookie",
		null,
		new String[] { "CREATE_CIRCLE" },
		"no uids", p, false, 
		DeterFault.request, null);

	changePermissions(pStub, "regression0",
		new String[] { "test0", "23test23" },
		new String[] { "badstring" }, "bad string",
		p, false, DeterFault.request,
		new MemberResp[] {
		    new MemberResp("test0", true), 
		    new MemberResp("23test23", false), 
		});

	changePermissions(pStub, "regression0",
		new String[] { "test0", "23test23" },
		new String[] {
		    "CREATE_CIRCLE",
		    "CREATE_EXPERIMENT",
		    "CREATE_LIBRARY"
		}, "good",
		p, true, DeterFault.none,
		new MemberResp[] {
		    new MemberResp("test0", true), 
		    new MemberResp("23test23", false), 
		});

	for (MemberDesc m: expectedMembers)
	    if ( m.name.equals("test0"))
		m.setPermissions(new String[] {
		    "CREATE_CIRCLE",
		    "CREATE_EXPERIMENT",
		    "CREATE_LIBRARY"
		});

	for (MemberDesc m: expectedCircleMembers)
	    if ( m.name.equals("test0"))
		m.setPermissions(new String[0]);

	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	regressionLogin(uStub, "admin0", "test", p);
	changePermissions(pStub, "regression0",
		new String[] { "test0", "23test23" },
		new String[] { "CREATE_CIRCLE" },
		"bad permissions",
		p, false, DeterFault.access, null);
	regressionLogout(uStub, p);
	changePermissions(pStub, "regression0",
		new String[] { "test0", "23test23" },
		new String[] { "CREATE_CIRCLE" },
		"not logged in",
		p, false, DeterFault.login, null);

	// change the owner
	regressionLogin(uStub, "test", "test", p);

	setOwner(pStub, null, "test0", "no projectid", p, 
		false, DeterFault.request);
	setOwner(pStub, "project:cisforcookie", "test0", 
		"bad projectid format", p, false, DeterFault.request);
	setOwner(pStub, "cisforcookie", "test0", 
		"bad projectid non-existent", p, false, DeterFault.request);

	setOwner(pStub, "cisforcookie", "tst0", 
		"owner not in project", p, false, DeterFault.request);


	// Make sure none of those actually worked
	checkProject("test", "regression0", pStub, cStub, "test", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");
	// change the owner
	setOwner(pStub, "regression0", "test0", 
		"good", p, true, DeterFault.none);

	// Make sure the correct change worked 
	checkProject("test", "regression0", pStub, cStub, "test0", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// not the owner anymore...
	setOwner(pStub, "regression0", "test0", 
		"bad permissions - not the owner anymore", p,
		false, DeterFault.access);

	regressionLogout(uStub, p);
	setOwner(pStub, "regression0", "test0", 
		"not logged in", p,
		false, DeterFault.login);
	regressionLogin(uStub, "test", "test", p);


	removeUsers(pStub, null, new String[] { "test", "test0", "23test23" },
		"no projectid", p, false, DeterFault.request, null);
	removeUsers(pStub, "project:cisforcookie", 
		new String[] { "test", "test0", "23test23" },
		"bad projectid format", p, false, DeterFault.request, null);
	removeUsers(pStub, "cisforcookie", 
		new String[] { "test", "test0", "23test23" },
		"bad projectid nonexistent", p, 
		false, DeterFault.request, null);

	removeUsers(pStub, "regression0", null,
		"no users", p, false, DeterFault.request, null);

	regressionLogin(uStub, "faber0", "test", p);
	removeUsers(pStub, "regression0", 
		new String[] { "test", "test0", "23test23" },
		"bad permissions", p, false, DeterFault.access, null);

	regressionLogin(uStub, "test", "test", p);
	removeUsers(pStub, "regression0",
		new String[] { "test", "test0", "23test23" },
		"good", p, true, DeterFault.none,
		new MemberResp[] {
		    new MemberResp("test0", false),
		    new MemberResp("23test23", false),
		    new MemberResp("test", true),
		});
	List<MemberDesc> tmp = new ArrayList<MemberDesc>();

	for (MemberDesc m: expectedMembers)
	    if (!m.name.equals("test")) tmp.add(m);
	expectedMembers = tmp;

	tmp = new ArrayList<MemberDesc>();
	for (MemberDesc m: expectedCircleMembers)
	    if (!m.name.equals("test")) tmp.add(m);
	expectedCircleMembers = tmp;

	regressionLogin(uStub, "test0", "test", p);
	checkProject("test0", "regression0", pStub, cStub, "test0", 
		expectedMembers, expectedCircleMembers, p,
		"check regression0 membership");

	// Put test back into regression0 so that it is an approved user
	regressionLogin(uStub, "testadmin", "test", p);
	addUsersNoConfirm(pStub, "regression0", 
		new String[] { "test" }, 
		new String[] { "ALL_PERMS" },
		"restore test", p, true, DeterFault.none, new MemberResp[] { 
		    new MemberResp("test", true),
		});
	regressionLogout(uStub, p);
    }
}
