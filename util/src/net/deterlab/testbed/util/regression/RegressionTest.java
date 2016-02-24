package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.NotificationFlag;

import net.deterlab.testbed.client.ApiInfoStub;
import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.RealizationsDeterFault;
import net.deterlab.testbed.client.RealizationsStub;
import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.ResourcesStub;
import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

import org.apache.log4j.Logger;

import net.deterlab.testbed.util.Utility;

public abstract class RegressionTest extends Utility {
    /** Logger to put output to the screen */
    protected Logger log = null;
    /** The circles we created */
    private List<String> circles;
    /** Circle attributes we created */
    private List<String> circleAttrs;
    /** The experiments we created (for cleanup)*/
    private List<String> experiments;
    /** Experiment attributes we created */
    private List<String> experimentAttrs;
    /** The libraries we created (for cleanup)*/
    private List<String> libraries;
    /** Library attributes we created */
    private List<String> libraryAttrs;
    /** The projects we created */
    private List<String> projects;
    /** Project attributes we created */
    private List<String> projectAttrs;
    /** The realizations we created (for cleanup)*/
    private List<String> realizations;
    /** The users we created */
    private List<String> users;
    /** User attributes we created */
    private List<String> userAttrs;
    /** Set of permissions valid for a circle.  Needed to mask out project


    /**
     * Indicates a test did not go as expected.
     */
    static public class RegressionException extends Exception {
	/**
	 * Create a RegressionException with m as a message.
	 * @param m the message
	 */
	public RegressionException(String m) { super(m); }
    }

    /**
     * Initialize the test to point to the log
     * @param l the log
     */
    public RegressionTest() {
	log = Logger.getLogger(getClass());
	circles = new ArrayList<>();
	circleAttrs = new ArrayList<>();
	experiments = new ArrayList<>();
	experimentAttrs = new ArrayList<>();
	libraries = new ArrayList<>();
	libraryAttrs = new ArrayList<>();
	projects = new ArrayList<>();
	projectAttrs = new ArrayList<>();
	realizations = new ArrayList<>();
	users = new ArrayList<>();
	userAttrs = new ArrayList<>();
    }
    /**
     * Log msg as ERROR and exit
     * @param msg the message to log
     * @throws RegressionException with msg as message
     */
    public void failed(String msg) throws RegressionException {
	log.error(msg);
	throw new RegressionException(msg);
    }

    /**
     * Run the test, logging to log and outputting the XML (SOAP) exchanges to
     * xmlfile.
     * @param xmlFile the file.
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if a test fails
     */
    public abstract void runTest(File xmlFile, File dataDir)
	throws RegressionException;

    /**
     * Get the test name.
     * @return the test name
     */
    public abstract String getName();

    /**
     * Log in as the given user with pass.  Print xml comments to p.  If p is
     * null, print to System.err.
     * @param uid user
     * @param pass password
     * @param p the output destination
     * @throws RegressionException if the logout fails
     */
    public void regressionLogin(UsersStub uStub, String uid, String pass, 
	    PrintStream p)
	throws RegressionException {
	p.println("<!-- login as " + uid + " -->");
	try {
	    login(uStub, uid, pass);
	    log.info("Logged in as " + uid);
	    p.println("<!-- succeeded -->");
	}
	catch (Exception e ) {
	    p.println("<!-- failed -->");
	    failed("Could not log in as " + uid);
	}
    }
    /**
     * Logout, printing XML comments to p.  If p is null, print to System.err.
     * @param p the output destination
     * @throws RegressionException if the logout fails
     */
    public void regressionLogout(UsersStub uStub, PrintStream p)
	throws RegressionException {
	if ( p == null ) p = System.err;

	p.println("<!-- logout -->");
	try {
	    logout(uStub);
	    p.println("<!-- succeeded -->");
	    log.info("logout succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("logout failed");
	}
    }

    /**
     * Represnetation of an expected circle/project member.  Includes the Set
     * of permissions for comparison and the name.
     * @author DETER Team
     * @version 1.0
     */
    protected static class MemberDesc {
	/** The name of the user */
	public String name;
	/** User permissions */
	public Set<String> perms;

	/**
	 * Simple MemberDesc
	 */
	public MemberDesc() {
	    name = null;
	    perms = new TreeSet<String>();
	}

	/**
	 * Memberdesc with only a name
	 * @param n the userid
	 */
	public MemberDesc(String n) {
	    this();
	    setUid(n);
	}

	/**
	 * Construct a MemberDesc from a set of perms
	 * @param n the userid
	 * @param ps the permissions
	 */
	public MemberDesc(String n, Collection<String> ps) {
	    this();
	    setUid(n);
	    setPermissions(ps);
	}

	/**
	 * Construct a MemberDesc from an array of perms
	 * @param n the userid
	 * @param ps the permissions
	 */
	public MemberDesc(String n, String[] ps) {
	    this();
	    setUid(n);
	    setPermissions(ps);
	}

	/**
	 * Return the userid
	 * @return the userid
	 */
	public String getUid() { return name; }

	/**
	 * Set the userid
	 * @param n the new userid
	 */
	public void setUid(String n) { name = n; }

	/**
	 * Return the set of permissions
	 * @return thet set of permissions
	 */
	public Set<String> getPermissions() { return perms; }

	/**
	 * Set permissions for an existing user.
	 * @param ps new permissions
	 */
	public void setPermissions(String[] ps) {
	    perms.clear();
	    if (ps == null ) return;
	    for (String p: ps)
		perms.add(p);
	}
	/**
	 * Set permissions for an existing user.
	 * @param ps new permissions
	 */
	public void setPermissions(Collection<String>ps) {
	    perms.clear();
	    if (ps == null ) return;
	    perms.addAll(ps);
	}

	/**
	 * Add a single permission to the description
	 * @param p the permission to add
	 */
	public void addPermission(String p) {
	    if ( p == null ) return;
	    perms.add(p);
	}
    }

    protected static class MemberResp {
	public String name;
	public boolean success;
	public MemberResp(String n, boolean s) {
	    name = n;
	    success = s;
	}
    }

    protected static String joinPermissions(Set<String> perms) {
	StringBuilder sb = new StringBuilder();
	boolean first = true;

	for (String p : perms)
	    if (  first ) { sb.append(p); first = false; }
	    else { sb.append(", "); sb.append(p); }
	return sb.toString();
    }

    /**
     * Typedef to make MemberResp and AttrResp the same
     */
    protected static class AttrResp extends MemberResp {
	public AttrResp(String n, boolean s) { super(n, s); }
    }

    protected long getChallenge(UsersStub.UserNotification[] un, String pattern)
	    throws RegressionException {
	Pattern pat = Pattern.compile(pattern);
	long chall = 0;

	for (UsersStub.UserNotification not: un) {
	    String body = not.getBody();
	    if ( body == null ) failed("Empty notification!?!?");

	    Matcher m = pat.matcher(body);

	    if (m.find()) {
		try {
		    chall = Long.parseLong(m.group(1));
		    break;
		}
		catch (IndexOutOfBoundsException e) {
		    failed("Bad match!?");
		}
		catch (NumberFormatException e) {
		    failed("Cannot parse challenge: " + m.group());
		}
	    }
	}
	return chall;
    }

    protected void checkMembership(CirclesStub.Member[] mem,
	    Collection<MemberDesc> desc) throws RegressionException {
	members:
	for (CirclesStub.Member m :mem ) {
	    MemberDesc md = new MemberDesc(m.getUid(), m.getPermissions());
	    String name = md.getUid();
	    Set<String> perms = md.getPermissions();

	    if (name == null ) failed("Null name in member?");
	    for ( MemberDesc d : desc) {
		if ( name.equals(d.name)) {
		    if (!perms.equals(d.getPermissions()))
			failed("Unexpected permissions for " + name + " got " +
				joinPermissions(perms) +
				" expected " +
				joinPermissions(d.getPermissions()));
		    continue members;
		}
	    }
	    failed("Unexpected user " + name);
	}
	if ( mem.length != desc.size())
	    failed("Unexpected number of members!  Saw " + mem.length +
		    " expected " + desc.size());
    }

    protected void checkMembership(ProjectsStub.Member[] mem,
	    Collection<MemberDesc> desc) throws RegressionException {
	members:
	for (ProjectsStub.Member m :mem ) {
	    MemberDesc md = new MemberDesc(m.getUid(), m.getPermissions());
	    String name = md.getUid();
	    Set<String> perms = md.getPermissions();

	    if (name == null ) failed("Null name in member?");

	    for ( MemberDesc d : desc) {
		if ( name.equals(d.name)) {
		    if (!perms.equals(d.getPermissions()))
			failed("Unexpected permissions for " + name + " got " +
				joinPermissions(perms) +
				" expected " +
				joinPermissions(d.getPermissions()));
		    continue members;
		}
	    }
	    failed("Unexpected user " + name);
	}
	if ( mem.length != desc.size())
	    failed("Unexpected number of members!  Saw " + mem.length +
		    " expected " + desc.size());
    }

    protected void checkProject(String uid, String project, ProjectsStub pStub,
	    CirclesStub cStub, String owner, Collection<MemberDesc> mdesc,
	    Collection<MemberDesc> cmdesc,
	    PrintStream p, String comment ) throws RegressionException {
	ProjectsStub.ViewProjects vReq = new ProjectsStub.ViewProjects();
	ProjectsStub.ViewProjectsResponse vResp =  null;
	ProjectsStub.ProjectDescription[] desc = null;
	CirclesStub.ViewCircles cvReq = new CirclesStub.ViewCircles();
	CirclesStub.ViewCirclesResponse cvResp =  null;
	CirclesStub.CircleDescription[] cdesc = null;
	String circle = project + ":" + project;

	p.println("<!-- viewProjects " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(project);
	try {
	    vResp = pStub.viewProjects(vReq);
	    p.println("<!-- succeeded -->");
	    log.info("viewProjects " + comment + " succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("viewProjects " + comment + " failed");
	}
	desc = vResp.get_return();
	if (desc == null || desc.length != 1 )
	    failed("Wrong number of projects");

	if ( !project.equals(desc[0].getProjectId()))
	    failed("Did not find " + project);
	log.info("Correct projects in view");

	if (!owner.equals(desc[0].getOwner()))
	    failed("Owner mismatch " + desc[0].getOwner() + " " + owner);

	checkMembership(desc[0].getMembers(), mdesc);
	log.info("Expected members present in project");


	p.println("<!-- viewCircles regexp -->");
	cvReq.setUid(uid);
	cvReq.setRegex(circle);
	try {
	    cvResp = cStub.viewCircles(cvReq);
	    p.println("<!-- succeeded -->");
	    log.info("viewCircles regexp succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("viewCircles regexp failed");
	}
	cdesc = cvResp.get_return();
	if (cdesc == null || cdesc.length != 1 )
	    failed("Wrong number of circles");

	if ( !circle.equals(cdesc[0].getCircleId()))
	    failed("Did not find " + circle);
	log.info("Correct circles in view");

	if (!owner.equals(cdesc[0].getOwner()))
	    failed("Owner mismatch " + cdesc[0].getOwner() + " " + owner);

	checkMembership(cdesc[0].getMembers(), cmdesc);
	log.info("Expected members present in project");
    }

    protected void getVersion(ApiInfoStub stub, String comment, PrintStream p, 
	    boolean shouldSucceed) throws RegressionException {
	ApiInfoStub.GetVersion gReq = new ApiInfoStub.GetVersion();
	
	p.println("<!-- getversion " + comment + "-->");
	try { 
	    stub.getVersion(gReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getversion " + comment + " succeeded");
	    else
		failed("getversion " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getversion " + comment + " failed ");
	    else
		log.info("getversion " + comment + " failed ");
	}
    }

    protected void echo(ApiInfoStub stub, String param, String comment,
	    PrintStream p, boolean shouldSucceed )
	throws RegressionException {
	ApiInfoStub.Echo eReq = new ApiInfoStub.Echo();
	p.println("<!-- echo " + comment + " -->");
	eReq.setParam(param);
	try { 
	    stub.echo(eReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("echo " + comment + "  succeeded");
	    else
		failed("echo " + comment + "  succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("echo " + comment + " failed");
	    else
		log.info("echo " + comment + " failed");
	}
    }

    protected void getServerCertificate(ApiInfoStub stub, String comment,
	    PrintStream p, boolean shouldSucceed )
	throws RegressionException {
	ApiInfoStub.GetServerCertificate eReq =
	    new ApiInfoStub.GetServerCertificate();
	p.println("<!-- getServerCertificate " + comment + " -->");
	try {
	    stub.getServerCertificate(eReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getServerCertificate " + comment + "  succeeded");
	    else
		failed("getServerCertificate " + comment + "  succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getServerCertificate " + comment + " failed");
	    else
		log.info("getServerCertificate " + comment + " failed");
	}
    }

    protected void getClientCertificate(ApiInfoStub stub, String param,
	    String comment, PrintStream p, boolean shouldSucceed )
	throws RegressionException {
	ApiInfoStub.GetClientCertificate eReq =
	    new ApiInfoStub.GetClientCertificate();
	p.println("<!-- getClientCertificate " + comment + " -->");
	eReq.setName(param);
	try {
	    stub.getClientCertificate(eReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getClientCertificate " + comment + "  succeeded");
	    else
		failed("getClientCertificate " + comment + "  succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getClientCertificate " + comment + " failed");
	    else
		log.info("getClientCertificate " + comment + " failed");
	}
    }

    /**
     * Get permission strings for circles
     */
    protected String[] getValidPermissions(CirclesStub cStub,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	CirclesStub.GetValidPermissions cReq =
	    new CirclesStub.GetValidPermissions();
	CirclesStub.GetValidPermissionsResponse cResp = null;
	p.println("<!-- getValidPermissions " + comment + " -->");
	try {
	    cResp = cStub.getValidPermissions(cReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getValidPermissions " + comment + " succeeded");
	    else
		failed("getValidPermissions " + comment + " succeeded");
	    return cResp.get_return();
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("getValidPermissions " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getValidPermissions " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getValidPermissions " + comment +
		    " failed: unexpected exception");
	}
	return null;
    }


    /**
     * Get permission strings for experiments
     */
    protected String[] getValidPermissions(ExperimentsStub eStub,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.GetValidPermissions eReq =
	    new ExperimentsStub.GetValidPermissions();
	ExperimentsStub.GetValidPermissionsResponse eResp = null;
	p.println("<!-- getValidPermissions " + comment + " -->");
	try {
	    eResp = eStub.getValidPermissions(eReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getValidPermissions " + comment + " succeeded");
	    else
		failed("getValidPermissions " + comment + " succeeded");
	    return eResp.get_return();
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("getValidPermissions " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getValidPermissions " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getValidPermissions " + comment +
		    " failed: unexpected exception");
	}
	return null;
    }

    /**
     * Get permission strings for libraries
     */
    protected String[] getValidPermissions(LibrariesStub lStub,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.GetValidPermissions lReq =
	    new LibrariesStub.GetValidPermissions();
	LibrariesStub.GetValidPermissionsResponse lResp = null;
	p.println("<!-- getValidPermissions " + comment + " -->");
	try {
	    lResp = lStub.getValidPermissions(lReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getValidPermissions " + comment + " succeeded");
	    else
		failed("getValidPermissions " + comment + " succeeded");
	    return lResp.get_return();
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("getValidPermissions " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getValidPermissions " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getValidPermissions " + comment +
		    " failed: unexpected exception");
	}
	return null;
    }

    /**
     * Get permission strings for projects
     */
    protected String[] getValidPermissions(ProjectsStub pStub,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ProjectsStub.GetValidPermissions pReq =
	    new ProjectsStub.GetValidPermissions();
	ProjectsStub.GetValidPermissionsResponse pResp = null;
	p.println("<!-- getValidPermissions " + comment + " -->");
	try {
	    pResp = pStub.getValidPermissions(pReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getValidPermissions " + comment + " succeeded");
	    else
		failed("getValidPermissions " + comment + " succeeded");
	    return pResp.get_return();
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("getValidPermissions " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getValidPermissions " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getValidPermissions " + comment +
		    " failed: unexpected exception");
	}
	return null;
    }

    protected void modifyProjectAttribute(ProjectsStub pStub, String name,
	    String type, boolean optional, String access, String desc, 
	    String format, String formatdesc, int order, int len, 
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	ProjectsStub.ModifyProjectAttribute mReq = 
	    new ProjectsStub.ModifyProjectAttribute();
	p.println("<!-- modifyProjectAttribute " + comment + " -->");

	mReq.setName(name);
	mReq.setType(type);
	mReq.setOptional(optional);
	mReq.setAccess(access);
	mReq.setDescription(desc);
	mReq.setFormat(format);
	mReq.setFormatdescription(formatdesc);
	mReq.setOrder(order);
	mReq.setLength(len);
	try {
	    pStub.modifyProjectAttribute(mReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("modifyProjectAttribute " + comment + " succeeded");
	    else
		failed("modifyProjectAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("modifyProjectAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"modifyProjectAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("modifyProjectAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void modifyCircleAttribute(CirclesStub cStub, String name,
	    String type, boolean optional, String access, String desc, 
	    String format, String formatdesc, int order, int len, 
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	CirclesStub.ModifyCircleAttribute mReq = 
	    new CirclesStub.ModifyCircleAttribute();
	p.println("<!-- modifyCircleAttribute " + comment + " -->");

	mReq.setName(name);
	mReq.setType(type);
	mReq.setOptional(optional);
	mReq.setAccess(access);
	mReq.setDescription(desc);
	mReq.setFormat(format);
	mReq.setFormatdescription(formatdesc);
	mReq.setOrder(order);
	mReq.setLength(len);
	try {
	    cStub.modifyCircleAttribute(mReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("modifyCircleAttribute " + comment + " succeeded");
	    else
		failed("modifyCircleAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("modifyCircleAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"modifyCircleAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("modifyCircleAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void modifyExperimentAttribute(ExperimentsStub cStub, String name,
	    String type, boolean optional, String access, String desc, 
	    String format, String formatdesc, int order, int len, 
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	ExperimentsStub.ModifyExperimentAttribute mReq = 
	    new ExperimentsStub.ModifyExperimentAttribute();
	p.println("<!-- modifyExperimentAttribute " + comment + " -->");

	mReq.setName(name);
	mReq.setType(type);
	mReq.setOptional(optional);
	mReq.setAccess(access);
	mReq.setDescription(desc);
	mReq.setFormat(format);
	mReq.setFormatdescription(formatdesc);
	mReq.setOrder(order);
	mReq.setLength(len);
	try {
	    cStub.modifyExperimentAttribute(mReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("modifyExperimentAttribute " + comment + " succeeded");
	    else
		failed("modifyExperimentAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("modifyExperimentAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"modifyExperimentAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("modifyExperimentAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void modifyLibraryAttribute(LibrariesStub cStub, String name,
	    String type, boolean optional, String access, String desc,
	    String format, String formatdesc, int order, int len,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.ModifyLibraryAttribute mReq =
	    new LibrariesStub.ModifyLibraryAttribute();
	p.println("<!-- modifyLibraryAttribute " + comment + " -->");

	mReq.setName(name);
	mReq.setType(type);
	mReq.setOptional(optional);
	mReq.setAccess(access);
	mReq.setDescription(desc);
	mReq.setFormat(format);
	mReq.setFormatdescription(formatdesc);
	mReq.setOrder(order);
	mReq.setLength(len);
	try {
	    cStub.modifyLibraryAttribute(mReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("modifyLibraryAttribute " + comment + " succeeded");
	    else
		failed("modifyLibraryAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("modifyLibraryAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"modifyLibraryAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("modifyLibraryAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void modifyUserAttribute(UsersStub uStub, String name,
	    String type, boolean optional, String access, String desc, 
	    String format, String formatdesc, int order, int len, 
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	UsersStub.ModifyUserAttribute mReq = 
	    new UsersStub.ModifyUserAttribute();
	p.println("<!-- modifyUserAttribute " + comment + " -->");

	mReq.setName(name);
	mReq.setType(type);
	mReq.setOptional(optional);
	mReq.setAccess(access);
	mReq.setDescription(desc);
	mReq.setFormat(format);
	mReq.setFormatdescription(formatdesc);
	mReq.setOrder(order);
	mReq.setLength(len);
	try {
	    uStub.modifyUserAttribute(mReq);
	    p.flush();
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("modifyUserAttribute " + comment + " succeeded");
	    else
		failed("modifyUserAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("modifyUserAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"modifyUserAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("modifyUserAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    public void approveProject(ProjectsStub pStub, String projectid, 
	    boolean value, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	ProjectsStub.ApproveProject appReq = new ProjectsStub.ApproveProject();

	p.println("<!-- approveProject " + comment + "-->");
	appReq.setProjectid(projectid);
	appReq.setApproved(value);
	try {
	    pStub.approveProject(appReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed ) 
		log.info("approveProject " + comment + " succeeded");
	    else
		failed("approveProject " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed) 
		failed("approveProject " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"approveProject " + comment + " failed");
	}
	catch (Exception e) {
	    failed("approveProject " + comment +
		    " failed: unexpecetd exception");
	}
    }


    public void viewProjects(ProjectsStub pStub, CirclesStub cStub, 
	    String uid, String regex,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode, String[] expectedProjects )
	throws RegressionException {
	ProjectsStub.ChangeResult[] results = null;
	ProjectsStub.ViewProjects vReq = new ProjectsStub.ViewProjects();
	ProjectsStub.ViewProjectsResponse vResp =  null;
	p.println("<!-- viewProjects " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(regex);
	try {
	    vResp = pStub.viewProjects(vReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed ) 
		log.info("viewProjects " + comment + " succeeded");
	    else
		failed("viewProjects " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("viewProjects " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"viewProjects " + comment + " failed");
	}
	catch (Exception e) {
	    failed("viewProjects " + comment +
		    " failed: unexpected exception");
	}

	if ( !shouldSucceed ) return;
	if (expectedProjects == null) return;

	ProjectsStub.ProjectDescription[] desc = vResp.get_return();
	if (desc == null || desc.length != expectedProjects.length ) 
	    failed("Wrong number of projects");

	for (ProjectsStub.ProjectDescription d: desc) {
	    String name = d.getProjectId();
	    boolean foundit = false;

	    for ( String pn : expectedProjects)
		if (name.equals(pn)) {
		    foundit = true;
		    break;
		}
	    if ( !foundit) failed("Unexpected project: " + name);
	}
	log.info("Correct projects in view");
    }

    public void addUsersNoConfirm(ProjectsStub pStub, String projectid,
	    String[] uids, String[] perms, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, MemberResp[] resp) 
	throws RegressionException {

	ProjectsStub.AddUsersNoConfirm ancReq = 
	    new ProjectsStub.AddUsersNoConfirm();
	ProjectsStub.AddUsersNoConfirmResponse ancResp =  null;
	p.println("<!-- addUserNoConfirm "+ comment + " -->");
	ancReq.setProjectid(projectid);
	ancReq.setUids(uids);
	ancReq.setPerms(perms);
	try {
	    ancResp = pStub.addUsersNoConfirm(ancReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed) 
		log.info("addUsersNoConfirm "+ comment + " succeeded");
	    else
		failed("addUsersNoConfirm "+ comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("addUsersNoConfirm "+ comment + " failed");
	    else
		checkFault(f, expectedCode, 
			"addUsersNoConfirm "+ comment + " failed");
	}
	catch (Exception e) {
	    failed("addUsersNoConfirm "+ comment + 
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	ProjectsStub.ChangeResult[] results = ancResp.get_return();
	for (ProjectsStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected addition response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Addition responses correct");
    }
    public void addUsers(ProjectsStub pStub, String projectid,
	    String[] uids, String[] perms, String url, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode,
	    MemberResp[] resp)
	throws RegressionException {

	ProjectsStub.AddUsers aReq = new ProjectsStub.AddUsers();
	ProjectsStub.AddUsersResponse aResp =  null;
	p.println("<!-- addUsers "+ comment + " -->");
	aReq.setProjectid(projectid);
	aReq.setUids(uids);
	aReq.setPerms(perms);
	aReq.setUrlPrefix(url);
	try {
	    aResp = pStub.addUsers(aReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed) 
		log.info("addUsers "+ comment + " succeeded");
	    else
		failed("addUsers "+ comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("addUsers "+ comment + " failed");
	    else
		checkFault(f, expectedCode, "addUsers "+ comment + " failed");
	}
	catch (Exception e) {
	    failed("addUsers "+ comment + " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	ProjectsStub.ChangeResult[] results = aResp.get_return();
	for (ProjectsStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected addition response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Addition responses correct");
    }

    protected long getChallengeFromNotifications(UsersStub uStub, 
	    String uid, String pattern, String comment, PrintStream p) 
	throws RegressionException {

	UsersStub.GetNotifications gReq = new UsersStub.GetNotifications();
	UsersStub.GetNotificationsResponse gResp = null;
	long chall;

	p.println("<!-- getNotifications " + comment + "-->");
	gReq.setUid(uid);
	gReq.setFrom(null);
	gReq.setTo(null);
	gReq.setFlags(null);
	try {
	    gResp = uStub.getNotifications(gReq);
	    p.println("<!-- succeeded -->");
	    log.info("getNotifications " + comment + " succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("getNotifications " + comment + " failed");
	}

	chall = getChallenge(gResp.get_return(), pattern);

	if ( chall == 0 ) 
	    failed("Can't find challenge in notifications");
	return chall;
    }

    protected void addUserConfirm(ProjectsStub pStub, long chall, 
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ProjectsStub.AddUserConfirm acReq = new ProjectsStub.AddUserConfirm(); 

	p.println("<!-- addUserConfirm " + comment + " -->");
	acReq.setChallengeId(chall);
	try {
	    pStub.addUserConfirm(acReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("addUserConfirm " +comment +" succeeded");
	    else
		failed("addUserConfirm " +comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed ) 
		failed("addUserConfirm " +comment +" failed");
	    else 
		checkFault(f, expectedCode,
			"addUserConfirm " +comment +" failed");
	}
	catch (Exception e) {
	    failed("addUserConfirm " +comment +
		    " failed: unexpected exception");
	}
    }

    protected void joinProject(ProjectsStub pStub, String projectid, 
	    String uid, String url, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {

	ProjectsStub.JoinProject jReq = new ProjectsStub.JoinProject();

	p.println("<!-- joinProject " + comment + " -->");
	jReq.setProjectid(projectid);
	jReq.setUid(uid);
	jReq.setUrlPrefix(url);
	try {
	    pStub.joinProject(jReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("joinProject " + comment + " succeeded");
	    else
		failed("joinProject " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed )
		failed("joinProject " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"joinProject " + comment + " failed");
	}
	catch (Exception e) {
	    failed("joinProject " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void joinProjectConfirm(ProjectsStub pStub, long chall, 
	    String[] perms, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	ProjectsStub.JoinProjectConfirm jcReq =
	    new ProjectsStub.JoinProjectConfirm();

	p.println("<!-- joinProjectConfirm " + comment + " -->");
	jcReq.setChallengeId(chall);
	jcReq.setPerms(perms);
	try {
	    pStub.joinProjectConfirm(jcReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("joinProjectConfirm " + comment + " succeeded");
	    else
		failed("joinProjectConfirm " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("joinProjectConfirm " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"joinProjectConfirm " + comment + " failed");
	}
	catch (Exception e) {
	    failed("joinProjectConfirm " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changePermissions(ProjectsStub pStub, String projectid,
	    String[] uids, String[] perms, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, MemberResp[] resp)
	throws RegressionException {
	ProjectsStub.ChangePermissions cReq = 
	    new ProjectsStub.ChangePermissions(); 
	ProjectsStub.ChangePermissionsResponse cResp = null;

	p.println("<!-- changePermissions " + comment +" -->");
	cReq.setProjectid(projectid);
	cReq.setUids(uids);
	cReq.setPerms(perms);
	try {
	    cResp = pStub.changePermissions(cReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("changePermissions " + comment +" succeeded");
	    else
		failed("changePermissions " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changePermissions " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"changePermissions " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changePermissions " + comment +
		    " failed: unexpected exception");
	}

	if ( !shouldSucceed) return;

	ProjectsStub.ChangeResult[] results = cResp.get_return();
	for (ProjectsStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected change permission response for " +
				name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Change responses correct");
    }

    protected void setOwner(ProjectsStub pStub, String projectid, String uid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {
	ProjectsStub.SetOwner oReq = new ProjectsStub.SetOwner(); 
	p.println("<!-- setOwner " + comment + " -->");
	oReq.setProjectid(projectid);
	oReq.setUid(uid);
	try {
	    pStub.setOwner(oReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("setOwner " + comment +" succeeded");
	    else
		failed("setOwner " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("setOwner " + comment +" failed");
	    else
		checkFault(f, expectedCode, "setOwner " + comment +" failed");
	}
	catch (Exception e) {
	    failed("setOwner " + comment +" failed: unexpected exception");
	}
    }

    protected void setOwner(ExperimentsStub eStub, String eid, String uid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ExperimentsStub.SetOwner oReq = new ExperimentsStub.SetOwner();
	p.println("<!-- setOwner " + comment + " -->");
	oReq.setEid(eid);
	oReq.setUid(uid);
	try {
	    eStub.setOwner(oReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("setOwner " + comment +" succeeded");
	    else
		failed("setOwner " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("setOwner " + comment +" failed");
	    else
		checkFault(f, expectedCode, "setOwner " + comment +" failed");
	}
	catch (Exception e) {
	    failed("setOwner " + comment +" failed: unexpected exception");
	}
    }

    protected void setOwner(LibrariesStub lStub, String libid, String uid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	LibrariesStub.SetOwner oReq = new LibrariesStub.SetOwner();
	p.println("<!-- setOwner " + comment + " -->");
	oReq.setLibid(libid);
	oReq.setUid(uid);
	try {
	    lStub.setOwner(oReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("setOwner " + comment +" succeeded");
	    else
		failed("setOwner " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("setOwner " + comment +" failed");
	    else
		checkFault(f, expectedCode, "setOwner " + comment +" failed");
	}
	catch (Exception e) {
	    failed("setOwner " + comment +" failed: unexpected exception");
	}
    }

    protected void removeUsers(ProjectsStub pStub, String projectid,
	    String[] uids, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, MemberResp[] resp)
	throws RegressionException {
	ProjectsStub.RemoveUsers rReq = 
	    new ProjectsStub.RemoveUsers(); 
	ProjectsStub.RemoveUsersResponse rResp = null;

	p.println("<!-- removeUsers " + comment +" -->");
	rReq.setProjectid(projectid);
	rReq.setUids(uids);
	try {
	    rResp = pStub.removeUsers(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeUsers " + comment +" succeeded");
	    else
		failed("removeUsers " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeUsers " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"removeUsers " + comment +" failed");
	}
	catch (Exception e) {
	    failed("removeUsers " + comment +" failed: unexpected exception");
	}

	if ( !shouldSucceed) return;

	ProjectsStub.ChangeResult[] results = rResp.get_return();
	for (ProjectsStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected remove response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Remove responses correct");
    }

    protected void checkCircle(String uid, String circle, CirclesStub cStub,
	    String owner, Collection<MemberDesc> mdesc,
	    PrintStream p, String comment ) throws RegressionException {
	CirclesStub.ViewCircles vReq = new CirclesStub.ViewCircles();
	CirclesStub.ViewCirclesResponse vResp =  null;
	CirclesStub.CircleDescription[] desc = null;

	p.println("<!-- viewCircles " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(circle);
	try {
	    vResp = cStub.viewCircles(vReq);
	    p.println("<!-- succeeded -->");
	    log.info("viewCircles " + comment + " succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("viewCircles " + comment + " failed");
	}
	desc = vResp.get_return();
	if (desc == null || desc.length != 1 ) 
	    failed("Wrong number of circles");

	if ( !circle.equals(desc[0].getCircleId()))
	    failed("Did not find " + circle);
	log.info("Correct circles in view");

	if (!owner.equals(desc[0].getOwner()))
	    failed("Owner mismatch " + desc[0].getOwner() + " " + owner);

	checkMembership(desc[0].getMembers(), mdesc);
	log.info("Expected members present in circle");
    }

    public void viewCircles(CirclesStub cStub,
	    String uid, String regex,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode, String[] expectedCircles)
	throws RegressionException {
	CirclesStub.ViewCircles vReq = new CirclesStub.ViewCircles();
	CirclesStub.ViewCirclesResponse vResp =  null;
	p.println("<!-- viewCircles " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(regex);
	try {
	    vResp = cStub.viewCircles(vReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed ) 
		log.info("viewCircles " + comment + " succeeded");
	    else
		failed("viewCircles " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("viewCircles " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"viewCircles " + comment + " failed");
	}
	catch (Exception e) {
	    failed("viewCircles " + comment + " failed: unexpected exception");
	}

	if ( !shouldSucceed ) return;
	if (expectedCircles == null) return;

	CirclesStub.CircleDescription[] desc = vResp.get_return();
	if (desc == null || desc.length != expectedCircles.length ) 
	    failed("Wrong number of circles");

	for (CirclesStub.CircleDescription d: desc) {
	    String name = d.getCircleId();
	    boolean foundit = false;

	    for ( String pn : expectedCircles)
		if (name.equals(pn)) {
		    foundit = true;
		    break;
		}
	    if ( !foundit) failed("Unexpected circle: " + name);
	}
	log.info("Correct circles in view");
    }


    /**
     * Check the access members of an ExperimentDescription against a
     * collection of expected results.  Throw a RegressionException on any
     * discrepency.  (Note that this is selected from the other checkMembership
     * methods by the parameters).
     * @param mem the Experiment's actual access list
     * @param desc the expected members
     * @throws RegressionException if there is an error
     */
    protected void checkMembership(ExperimentsStub.AccessMember[] mem,
	    Collection<MemberDesc> desc) throws RegressionException {
	if ( mem == null ) {
	    if (!desc.isEmpty())
		failed("Expected members, but none present");
	    return;
	}
	members:
	for (ExperimentsStub.AccessMember m : mem ) {
	    MemberDesc md = new MemberDesc(m.getCircleId(),
		    m.getPermissions());
	    String name = md.getUid();
	    Set<String> perms = md.getPermissions();

	    if (name == null ) failed("Null name in member?");

	    for ( MemberDesc d : desc) {
		if ( name.equals(d.name)) {
		    if (!perms.equals(d.getPermissions()))
			failed("Unexpected permissions for " + name + " got " +
				joinPermissions(perms) +
				" expected " +
				joinPermissions(d.getPermissions()));
		    continue members;
		}
	    }
	    failed("Unexpected circle " + name);
	}
	if ( mem.length != desc.size())
	    failed("Unexpected number of circles! Saw " + mem.length +
		    " expected " + desc.size());
    }

    /**
     * Check to make sure all the experiments in aspects are present in mem.
     * If data elements are not presnet, only their presence is required, if
     * the data is given it is checked as well.  This is a simple n x n
     * comparison.
     * @param mem the Experiment's actual aspects
     * @param expected the expected aspects
     * @throws RegressionException if there is an error
     */
    protected void checkAspects(ExperimentsStub.ExperimentAspect[] mem,
	    ExperimentsStub.ExperimentAspect[] expected)
	throws RegressionException {

	if ( mem == null || mem.length == 0) {
	    if ( expected != null && expected.length > 0)
		failed("Expected aspects and none present");
	    return;
	}

	boolean[] sawIt = new boolean[expected.length];
	for ( boolean b: sawIt)
	    b = false;

	if ( mem != null ) {
	    if (mem.length != expected.length)
		failed("Different number of aspects in experiment (" +
			mem.length + ") and expected list (" +
			expected.length + ")");
	    for (ExperimentsStub.ExperimentAspect m : mem) {
		ExperimentsStub.ExperimentAspect e = null;
		String mName = m.getName();
		String mType = m.getType();
		String mSubType = m.getSubType();

		if ( mName == null || mType == null )
		    failed("Bad aspect- no name or type");

		for (int i = 0; i < expected.length; i++) {
		    e = expected[i];

		    if ( !mName.equals(e.getName()) ||
			    !mType.equals(e.getType()))
			continue;
		    if ( mSubType == null && e.getSubType() != null )
			continue;
		    if ( mSubType != null && !mSubType.equals(e.getSubType()))
			continue;
		    // Hey a match!  Check the data if present
		    sawIt[i] = true;
		    if (e.getData() != null ) {
			if (m.getData() == null )
			    failed("No data in " + mName + "/" + mType +
				    ((mSubType != null ) ? "/" + mSubType :
				     "") + " and check requested");
			try {
			    if ( !Arrays.equals(getBytes(m.getData()),
					getBytes(e.getData())))
				failed("Mismatched data in " + mName + "/" +
					mType +
					((mSubType != null ) ? "/" + mSubType :
					 ""));
			}
			catch (IOException ie) {
			    failed("Cannot gather data for aspect!?" +
				    ie.getMessage());
			}
		    }
		    break;
		}
	    }
	}
	for (boolean b: sawIt)
	    if (!b) failed("Did not match all expected aspects ");
    }

    /**
     * Compare two string arrays to make sure they contain the same values.
     * Used to check permissions arrays.  The expected array is the one the
     * caller expeected and the actual was the one found in the data structure.
     * @param expected the expected array
     * @param actual the actual array
     * @throws RegressionException if the arrays don't match
     */
    protected void compareArrays(String[] expected, String[] actual)
	throws RegressionException {
	Set<String> act = new HashSet<String>();

	if ( expected == null || expected.length == 0) {
	    if ( actual != null && actual.length > 0 )
		failed("Expected nothing and got " +
			actual.length + " elements");
	    return;
	}

	if ( actual == null )
	    failed("Expected " + expected.length + " values and got none");

	if ( expected.length != actual.length)
	    failed("Different string arrag lengths.  Expected " +
		    expected.length + " actual " + actual.length);

	for (String a: actual)
	    act.add(a);

	for ( String e: expected)
	    if ( !act.contains(e))
		failed("expected element " + e + " missing");
    }

    /**
     * Confirm that an experiment has been created with the correct owner, ACL,
     * and aspects.  Print regression test results as usual and throw a
     * RegressionException on problems.
     * @param uid user to call viewExperiments with
     * @param eid the experiment to check
     * @param eStub the stub to use for the call
     * @param owner expected owner
     * @param circles the expected ACL
     * @param queryAspects the aspects of the query to check for
     * @param p the print stream for results
     * @param comment the framing comment for the check
     * @throws RegressionException on errors
     */
    protected void checkExperiment(String uid, String eid,
	    ExperimentsStub eStub, String owner,
	    ExperimentsStub.ExperimentAspect[] aspects,
	    Collection<MemberDesc> circles, String[] userPerms,
	    PrintStream p, String comment ) throws RegressionException {
	ExperimentsStub.ViewExperiments vReq =
	    new ExperimentsStub.ViewExperiments();
	ExperimentsStub.ViewExperimentsResponse vResp =  null;
	ExperimentsStub.ExperimentDescription[] desc = null;
	boolean listOnly = true;

	if ( aspects != null )
	    for (ExperimentsStub.ExperimentAspect e : aspects)
		if (e.getData() != null) listOnly = false;

	p.println("<!-- viewExperiments " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(eid);
	vReq.setListOnly(listOnly);
	try {
	    vResp = eStub.viewExperiments(vReq);
	    p.println("<!-- succeeded -->");
	    log.info("viewExperiments " + comment + " succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("viewExperiments " + comment + " failed");
	}
	desc = vResp.get_return();
	if (desc == null || desc.length != 1 )
	    failed("Wrong number of experiments");

	if ( !eid.equals(desc[0].getExperimentId()))
	    failed("Did not find " + eid);
	log.info("Correct experiments in view");

	if (owner != null && !owner.equals(desc[0].getOwner()))
	    failed("Owner mismatch " + desc[0].getOwner() + " " + owner);

	if ( circles != null ) {
	    checkMembership(desc[0].getACL(), circles);
	    log.info("Expected members present in experiment");
	}
	if (aspects != null) {
	    checkAspects(desc[0].getAspects(), aspects);
	    log.info("Expected aspects present in experiment");
	}
	compareArrays(userPerms, desc[0].getPerms());
    }
    /**
     * Call viewExperiments on the given Experiments stub (logging as for all
     * regression tests) and confirm that the expected experiments were found.
     * The view can be scoped by user ID, regular expression applied to name
     * and library name.
     * @param eStub Experiments stub on which to make the call
     * @param uid the uid to get experiments for
     * @param regex regular expression applied to names found
     * @param lib library to search
     * @param queryAspects the aspects of the query to check for
     * @param listOnly if true do not return data in aspects
     * @param offset the first experiment to show (0-referenced) -1 for all
     * @param count the numberof experiments to show  (-1 for all)
     * @param comment comment to insert in the regression test results
     * @param p the stream to print results to
     * @param shouldSucceed true if the call should work
     * @param expectedCode error code expected if shouldSucced is true
     * @param expectedExperiments the names of the experiments returned (may
     *		be null for no checking)
     * @throws RegressionException if an unexpected error (or success) occurs
     */
    public void viewExperiments(ExperimentsStub eStub,
	    String uid, String regex, String lib,
	    ExperimentsStub.ExperimentAspect[] queryAspects, boolean listOnly,
	    int offset, int count,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode, String[] expectedExperiments)
	throws RegressionException {
	ExperimentsStub.ViewExperiments vReq =
	    new ExperimentsStub.ViewExperiments();
	ExperimentsStub.ViewExperimentsResponse vResp =  null;
	p.println("<!-- viewExperiments " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(regex);
	vReq.setLib(lib);
	vReq.setQueryAspects(queryAspects);
	vReq.setListOnly(listOnly);
	if ( offset != -1 )
	    vReq.setOffset(new Integer(offset));
	if ( count != -1 )
	    vReq.setCount(new Integer(count));
	try {
	    vResp = eStub.viewExperiments(vReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("viewExperiments " + comment + " succeeded");
	    else
		failed("viewExperiments " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("viewExperiments " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"viewExperiments " + comment + " failed");
	}
	catch (Exception e) {
	    failed("viewExperiments " + comment +
		    " failed: unexpected exception");
	}

	if ( !shouldSucceed ) return;
	if (expectedExperiments == null) return;

	ExperimentsStub.ExperimentDescription[] desc = vResp.get_return();
	if (desc == null || desc.length != expectedExperiments.length )
	    failed("Wrong number of experiments");

	for (ExperimentsStub.ExperimentDescription d: desc) {
	    String name = d.getExperimentId();
	    boolean foundit = false;

	    for ( String pn : expectedExperiments)
		if (name.equals(pn)) {
		    foundit = true;
		    break;
		}
	    if ( !foundit) failed("Unexpected experiment: " + name);
	}
	log.info("Correct experiments in view");
    }


    /**
     * Check the access members of an LibraryDescription against a
     * collection of expected results.  Throw a RegressionException on any
     * discrepency.  (Note that this is selected from the other checkMembership
     * methods by the parameters).
     * @param mem the Library's actual access list
     * @param desc the expected members
     * @throws RegressionException if there is an error
     */
    protected void checkMembership(LibrariesStub.AccessMember[] mem,
	    Collection<MemberDesc> desc) throws RegressionException {

	if ( mem == null) {
	    if (! desc.isEmpty() )
		failed("Expected " + desc.size() + " members, but had none");
	    return;
	}
	members:
	for (LibrariesStub.AccessMember m : mem ) {
	    MemberDesc md = new MemberDesc(m.getCircleId(),
		    m.getPermissions());
	    String name = md.getUid();
	    Set<String> perms = md.getPermissions();

	    if (name == null ) failed("Null name in member?");

	    for ( MemberDesc d : desc) {
		if ( name.equals(d.name)) {
		    if (!perms.equals(d.getPermissions()))
			failed("Unexpected permissions for " + name + " got " +
				joinPermissions(perms) +
				" expected " +
				joinPermissions(d.getPermissions()));
		    continue members;
		}
	    }
	    failed("Unexpected circle " + name);
	}
	if ( mem.length != desc.size())
	    failed("Unexpected number of circles! Saw " +
		mem.length + " expected " + desc.size());
    }

    /**
     * Check the experiment names of an LibraryDescription against a
     * collection of expected results.  Throw a RegressionException on any
     * discrepency.
     * @param mem the Library's actual experiments list
     * @param desc the expected members
     * @throws RegressionException if there is an error
     */
    protected void checkExperiments(String[] mem,
	    String[] desc) throws RegressionException {

	Set<String> seen = new HashSet<String>();
	for (String e: mem)
	    seen.add(e);

	if ( mem.length != seen.size())
	    failed("Wrong number of experiments! Saw " + mem.length +
		    " expected " + seen.size());

	for (String e : desc )
	    if (!seen.contains(e))
		failed("Expected experiment " + e + " not present");
    }

    /**
     * Confirm that a library has been created with the correct owner, ACL,
     * and experimens.  Print regression test results as usual and throw a
     * RegressionException on problems.
     * @param uid user to call viewExperiments with
     * @param libid the library to check
     * @param lStub the stub to use for the call
     * @param owner expected owner
     * @param experiments the experiments to check for
     * @param circles the expected ACL
     * @param p the print stream for results
     * @param comment the framing comment for the check
     * @throws RegressionException on errors
     */
    protected void checkLibrary(String uid, String libid,
	    LibrariesStub lStub, String owner,
	    String[] experiments,
	    Collection<MemberDesc> circles, String[] userPerms,
	    PrintStream p, String comment ) throws RegressionException {
	LibrariesStub.ViewLibraries vReq =
	    new LibrariesStub.ViewLibraries();
	LibrariesStub.ViewLibrariesResponse vResp =  null;
	LibrariesStub.LibraryDescription[] desc = null;

	p.println("<!-- viewLibraries " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(libid);
	try {
	    vResp = lStub.viewLibraries(vReq);
	    p.println("<!-- succeeded -->");
	    log.info("viewLibraries " + comment + " succeeded");
	}
	catch (Exception e) {
	    p.println("<!-- failed -->");
	    failed("viewLibraries " + comment + " failed");
	}
	desc = vResp.get_return();
	if (desc == null || desc.length != 1 )
	    failed("Wrong number of libraries");

	if ( !libid.equals(desc[0].getLibraryId()))
	    failed("Did not find " + libid);
	log.info("Correct libraries in view");

	if (owner != null && !owner.equals(desc[0].getOwner()))
	    failed("Owner mismatch " + desc[0].getOwner() + " " + owner);

	if ( circles != null ) {
	    checkMembership(desc[0].getACL(), circles);
	    log.info("Expected members present in experiment");
	}
	if (experiments != null) {
	    checkExperiments(desc[0].getExperiments(), experiments);
	    log.info("Expected experiments present in library");
	}
	compareArrays(userPerms, desc[0].getPerms());
    }
    /**
     * Call viewLibraries on the given Libraries stub (logging as for all
     * regression tests) and confirm that the expected experiments were found.
     * The view can be scoped by user ID, regular expression applied to name
     * and library name.
     * @param lStub Libraries stub on which to make the call
     * @param uid the uid to get experiments for
     * @param regex regular expression applied to names found
     * @param comment comment to insert in the regression test results
     * @param p the stream to print results to
     * @param shouldSucceed true if the call should work
     * @param expectedCode error code expected if shouldSucced is true
     * @param expectedLibraries the names of the experiments returned (may
     *		be null for no checking)
     * @throws RegressionException if an unexpected error (or success) occurs
     */
    public void viewLibraries(LibrariesStub lStub,
	    String uid, String regex, int offset, int count,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode, String[] expectedLibraries)
	throws RegressionException {
	LibrariesStub.ViewLibraries vReq =
	    new LibrariesStub.ViewLibraries();
	LibrariesStub.ViewLibrariesResponse vResp =  null;
	p.println("<!-- viewLibraries " + comment + " -->");
	vReq.setUid(uid);
	vReq.setRegex(regex);
	if ( offset != -1 )
	    vReq.setOffset(new Integer(offset));
	if ( count != -1 )
	    vReq.setCount(new Integer(count));
	try {
	    vResp = lStub.viewLibraries(vReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("viewLibraries " + comment + " succeeded");
	    else
		failed("viewLibraries " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("viewLibraries " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"viewLibraries " + comment + " failed");
	}
	catch (Exception e) {
	    failed("viewLibraries " + comment +
		    " failed: unexpected exception");
	}

	if ( !shouldSucceed ) return;
	if (expectedLibraries == null) return;

	LibrariesStub.LibraryDescription[] desc = vResp.get_return();
	if (desc == null || desc.length != expectedLibraries.length )
	    failed("Wrong number of libraries");

	for (LibrariesStub.LibraryDescription d: desc) {
	    String name = d.getLibraryId();
	    boolean foundit = false;

	    for ( String pn : expectedLibraries)
		if (name.equals(pn)) {
		    foundit = true;
		    break;
		}
	    if ( !foundit) failed("Unexpected libraries: " + name);
	}
	log.info("Correct libraries in view");
    }

    public void addUsersNoConfirm(CirclesStub cStub, String circleid,
	    String[] uids, String[] perms, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, MemberResp[] resp)
	throws RegressionException {

	CirclesStub.AddUsersNoConfirm ancReq = 
	    new CirclesStub.AddUsersNoConfirm();
	CirclesStub.AddUsersNoConfirmResponse ancResp =  null;
	p.println("<!-- addUserNoConfirm "+ comment + " -->");
	ancReq.setCircleid(circleid);
	ancReq.setUids(uids);
	ancReq.setPerms(perms);
	try {
	    ancResp = cStub.addUsersNoConfirm(ancReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed) 
		log.info("addUsersNoConfirm "+ comment + " succeeded");
	    else
		failed("addUsersNoConfirm "+ comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("addUsersNoConfirm "+ comment + " failed");
	    else
		checkFault(f, expectedCode,
			"addUsersNoConfirm "+ comment + " failed");
	}
	catch (Exception e) {
	    failed("addUsersNoConfirm "+ comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	CirclesStub.ChangeResult[] results = ancResp.get_return();
	for (CirclesStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected addition response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Addition responses correct");
    }
    public void addUsers(CirclesStub cStub, String circleid,
	    String[] uids, String[] perms, String url, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode,
	    MemberResp[] resp)
	throws RegressionException {

	CirclesStub.AddUsers aReq = new CirclesStub.AddUsers();
	CirclesStub.AddUsersResponse aResp =  null;
	p.println("<!-- addUsers "+ comment + " -->");
	aReq.setCircleid(circleid);
	aReq.setUids(uids);
	aReq.setPerms(perms);
	aReq.setUrlPrefix(url);
	try {
	    aResp = cStub.addUsers(aReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed) 
		log.info("addUsers "+ comment + " succeeded");
	    else
		failed("addUsers "+ comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("addUsers "+ comment + " failed");
	    else
		checkFault(f, expectedCode,
			"addUsers "+ comment + " failed");
	}
	catch (Exception e) {
	    failed("addUsers "+ comment + " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	CirclesStub.ChangeResult[] results = aResp.get_return();
	for (CirclesStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected addition response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Addition responses correct");
    }

    protected void addUserConfirm(CirclesStub cStub, long chall, 
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	CirclesStub.AddUserConfirm acReq = new CirclesStub.AddUserConfirm(); 

	p.println("<!-- addUserConfirm " + comment + " -->");
	acReq.setChallengeId(chall);
	try {
	    cStub.addUserConfirm(acReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("addUserConfirm " +comment +" succeeded");
	    else
		failed("addUserConfirm " +comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed ) 
		failed("addUserConfirm " +comment +" failed");
	    else 
		checkFault(f, expectedCode,
			"addUserConfirm " +comment +" failed");
	}
	catch (Exception e) {
	    failed("addUserConfirm " +comment +" failed: unexpected exception");
	}
    }

    protected void joinCircle(CirclesStub cStub, String circleid, 
	    String uid, String url, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {

	CirclesStub.JoinCircle jReq = new CirclesStub.JoinCircle();

	p.println("<!-- joinCircle " + comment + " -->");
	jReq.setCircleid(circleid);
	jReq.setUid(uid);
	jReq.setUrlPrefix(url);
	try {
	    cStub.joinCircle(jReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed )
		log.info("joinCircle " + comment + " succeeded");
	    else
		failed("joinCircle " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed )
		failed("joinCircle " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"joinCircle " + comment + " failed");
	}
	catch (Exception e) {
	    failed("joinCircle " + comment + " failed: unexpected exception");
	}
    }

    protected void joinCircleConfirm(CirclesStub cStub, long chall, 
	    String[] perms, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	CirclesStub.JoinCircleConfirm jcReq =
	    new CirclesStub.JoinCircleConfirm();

	p.println("<!-- joinCircleConfirm " + comment + " -->");
	jcReq.setChallengeId(chall);
	jcReq.setPerms(perms);
	try {
	    cStub.joinCircleConfirm(jcReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("joinCircleConfirm " + comment + " succeeded");
	    else
		failed("joinCircleConfirm " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("joinCircleConfirm " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"joinCircleConfirm " + comment + " failed");
	}
	catch (Exception e) {
	    failed("joinCircleConfirm " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changePermissions(CirclesStub cStub, String circleid,
	    String[] uids, String[] perms, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, MemberResp[] resp)
	throws RegressionException {
	CirclesStub.ChangePermissions cReq = 
	    new CirclesStub.ChangePermissions(); 
	CirclesStub.ChangePermissionsResponse cResp = null;

	p.println("<!-- changePermissions " + comment +" -->");
	cReq.setCircleid(circleid);
	cReq.setUids(uids);
	cReq.setPerms(perms);
	try {
	    cResp = cStub.changePermissions(cReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("changePermissions " + comment +" succeeded");
	    else
		failed("changePermissions " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changePermissions " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"changePermissions " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changePermissions " + comment +
		    " failed: unexpected exception");
	}

	if ( !shouldSucceed) return;

	CirclesStub.ChangeResult[] results = cResp.get_return();
	for (CirclesStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected change permission response for " +
				name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Change responses correct");
    }

    protected void setOwner(CirclesStub cStub, String circleid, String uid,
	    String comment, PrintStream p, boolean shouldSucceed, 
	    int expectedCode) 
	throws RegressionException {
	CirclesStub.SetOwner oReq = new CirclesStub.SetOwner(); 
	p.println("<!-- setOwner " + comment + " -->");
	oReq.setCircleid(circleid);
	oReq.setUid(uid);
	try {
	    cStub.setOwner(oReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("setOwner " + comment +" succeeded");
	    else
		failed("setOwner " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if ( shouldSucceed)
		failed("setOwner " + comment +" failed");
	    else
		checkFault(f, expectedCode, "setOwner " + comment +" failed");
	}
	catch (Exception e) {
	    failed("setOwner " + comment +" failed: unexpected exception");
	}
    }

    protected void removeUsers(CirclesStub cStub, String circleid,
	    String[] uids, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode, 
	    MemberResp[] resp)
	throws RegressionException {
	CirclesStub.RemoveUsers rReq = 
	    new CirclesStub.RemoveUsers(); 
	CirclesStub.RemoveUsersResponse rResp = null;

	p.println("<!-- removeUsers " + comment +" -->");
	rReq.setCircleid(circleid);
	rReq.setUids(uids);
	try {
	    rResp = cStub.removeUsers(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeUsers " + comment +" succeeded");
	    else
		failed("removeUsers " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeUsers " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"removeUsers " + comment +" failed");
	}
	catch (Exception e) {
	    failed("removeUsers " + comment +" failed: unexpected exception");
	}

	if ( !shouldSucceed) return;

	CirclesStub.ChangeResult[] results = rResp.get_return();
	for (CirclesStub.ChangeResult r : results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (MemberResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected remove response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Remove responses correct");
    }

    protected long requestChallenge(UsersStub uStub,String uid, String[] types,
	    String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException { 
	UsersStub.RequestChallenge req = new UsersStub.RequestChallenge();
	UsersStub.RequestChallengeResponse resp = null;
	UsersStub.UserChallenge uc = null;

	p.println("<!-- requestChallenge " + comment + " -->");
	req.setUid(uid);
	req.setTypes(types);
	try {
	    resp = uStub.requestChallenge(req);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("requestChallenge " + comment + " succeeded");
	    else
		failed("requestChallenge " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("requestChallenge " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"requestChallenge " + comment + " failed");
	}
	catch (Exception e) {
	    failed("requestChallenge " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed ) return -1L;
	uc = resp.get_return();
	return uc.getChallengeID();
    }

    protected void challengeResponse(UsersStub uStub, byte[] data, long id,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {
	UsersStub.ChallengeResponse chR = new UsersStub.ChallengeResponse();

	p.println("<!-- challengeResponse " + comment + " -->");
	chR.setResponseData(data != null ? putBytes(data) : null);
	chR.setChallengeID(id);
	try {
	    uStub.challengeResponse(chR);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("challengeResponse " + comment + " succeeded");
	    else
		failed("challengeResponse " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("challengeResponse " + comment + " failed");
	    else
		log.info("challengeResponse " + comment + " failed");
	}
	catch (Exception e) {
	    failed("challengeResponse " + comment +
		    " failed: unexpected exception" + e);
	}
    }


    protected void passwordReset(UsersStub uStub, String uid, String prefix,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {

	UsersStub.RequestPasswordReset rReq = 
	    new UsersStub.RequestPasswordReset();
	rReq.setUid(uid);
	rReq.setUrlPrefix(prefix);

	p.println("<!-- requestPasswordReset " +comment + "-->");
	try {
	    uStub.requestPasswordReset(rReq);

	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("requestPasswordReset " + comment + " succeeded");
	    else
		failed("requestPasswordReset " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("requestPasswordReset " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"requestPasswordReset " + comment + " failed");
	}
	catch (Exception e) {
	    failed("requestPasswordReset " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changePasswordChallenge(UsersStub uStub, long id, 
	    String newpass, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	UsersStub.ChangePasswordChallenge cReq = 
	    new UsersStub.ChangePasswordChallenge();
	p.println("<!-- changePasswordChallenge " + comment +" -->");
	cReq.setChallengeID(id);
	cReq.setNewPass(newpass);
	try {
	    uStub.changePasswordChallenge(cReq);

	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("changePasswordChallenge " + comment +" succeeded");
	    else
		failed("changePasswordChallenge " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		checkFault(f, expectedCode,
			"changePasswordChallenge " + comment +" failed");
	    else
		log.info("changePasswordChallenge " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changePasswordChallenge " + comment +
		    " failed: unexpected exception");
	}
    }

    protected ExperimentsStub.Attribute[] getProfileDescription(
	    ExperimentsStub eStub, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.GetProfileDescription descReq =
	    new ExperimentsStub.GetProfileDescription();
	ExperimentsStub.GetProfileDescriptionResponse descResp = null;
	p.println("<!-- getProfileDescription " + comment + " -->");
	try {
	    descResp = eStub.getProfileDescription(descReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("getProfileDescription " + comment + " succeeded");
	    else
		failed("getProfileDescription " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getProfileDescription " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getProfileDescription " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getProfileDescription " + comment +
		    "failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;

	ExperimentsStub.Profile up = descResp.get_return();
	ExperimentsStub.Attribute[] profile = up.getAttributes();
	return profile;

    }

    protected LibrariesStub.Attribute[] getProfileDescription(
	    LibrariesStub lStub, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.GetProfileDescription descReq =
	    new LibrariesStub.GetProfileDescription();
	LibrariesStub.GetProfileDescriptionResponse descResp = null;
	p.println("<!-- getProfileDescription " + comment + " -->");
	try {
	    descResp = lStub.getProfileDescription(descReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("getProfileDescription " + comment + " succeeded");
	    else
		failed("getProfileDescription " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getProfileDescription " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getProfileDescription " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getProfileDescription " + comment +
		    "failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;

	LibrariesStub.Profile up = descResp.get_return();
	LibrariesStub.Attribute[] profile = up.getAttributes();
	return profile;

    }


    protected CirclesStub.Attribute[] getProfileDescription(CirclesStub pStub,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	CirclesStub.GetProfileDescription descReq = 
	    new CirclesStub.GetProfileDescription();
	CirclesStub.GetProfileDescriptionResponse descResp = null;
	p.println("<!-- getProfileDescription " + comment + " -->");
	try {
	    descResp = pStub.getProfileDescription(descReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("getProfileDescription " + comment + " succeeded");
	    else
		failed("getProfileDescription " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getProfileDescription " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getProfileDescription " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getProfileDescription " + comment +
		    "failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	CirclesStub.Profile up = descResp.get_return();
	CirclesStub.Attribute[] profile = up.getAttributes();
	return profile;

    }

    protected ProjectsStub.Attribute[] getProfileDescription(ProjectsStub pStub,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ProjectsStub.GetProfileDescription descReq = 
	    new ProjectsStub.GetProfileDescription();
	ProjectsStub.GetProfileDescriptionResponse descResp = null;
	p.println("<!-- getProfileDescription " + comment + " -->");
	try {
	    descResp = pStub.getProfileDescription(descReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("getProfileDescription " + comment + " succeeded");
	    else
		failed("getProfileDescription " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getProfileDescription " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getProfileDescription " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getProfileDescription " + comment +
		    "failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	ProjectsStub.Profile up = descResp.get_return();
	ProjectsStub.Attribute[] profile = up.getAttributes();
	return profile;

    }

    protected UsersStub.Attribute[] getProfileDescription(UsersStub uStub,
	    String comment, PrintStream p, boolean shouldSucceed, 
	    int expectedCode)
	throws RegressionException {
	UsersStub.GetProfileDescription descReq = 
	    new UsersStub.GetProfileDescription();
	UsersStub.GetProfileDescriptionResponse descResp = null;
	p.println("<!-- getProfileDescription " + comment + " -->");
	try {
	    descResp = uStub.getProfileDescription(descReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("getProfileDescription " + comment + " succeeded");
	    else
		failed("getProfileDescription " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		log.info("getProfileDescription " + comment + "failed");
	    else
		checkFault(f, expectedCode,
			"getProfileDescription " + comment + "failed");
	}
	catch (Exception e) {
	    failed("getProfileDescription " + comment + 
		    "failed - unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	UsersStub.Profile up = descResp.get_return();
	UsersStub.Attribute[] profile = up.getAttributes();
	return profile;

    }

    protected UsersStub.Attribute[] getUserProfile(UsersStub uStub,
	    String uid, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	UsersStub.GetUserProfile upReq = new UsersStub.GetUserProfile();
	UsersStub.GetUserProfileResponse upResp = null;

	p.println("<!-- getUserProfile " + comment + " -->");
	upReq.setUid(uid);
	try {
	    upResp = uStub.getUserProfile(upReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getUserProfile " +comment + " succeeded");
	    else
		failed("getUserProfile " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		checkFault(f, expectedCode,
			"getUserProfile " + comment + " failed");
	    else
		log.info("getUserProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getUserProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	UsersStub.Profile up = upResp.get_return();
	UsersStub.Attribute[] profile = up.getAttributes();
	return profile;
    }

    protected void changeUserProfile(UsersStub uStub, String uid,
	    UsersStub.ChangeAttribute[] changes, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode,
	    AttrResp[] resp) 
	throws RegressionException {
	UsersStub.ChangeUserProfile cReq = new UsersStub.ChangeUserProfile();
	UsersStub.ChangeUserProfileResponse cResp = null;

	cReq.setUid(uid);
	cReq.setChanges(changes);
	p.println("<!-- changeUserProfile " + comment + " -->");
	try {
	    cResp = uStub.changeUserProfile(cReq);
	    p.println("<!-- returns succeess (failed change) -->");
	    if (shouldSucceed)
		log.info("changeUserProfile " +comment +" succeeded");
	    else
		failed("changeUserProfile " +comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		checkFault(f, expectedCode,
			"changeUserProfile " + comment + " failed");
	    else
		log.info("changeUserProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("changeUserProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	UsersStub.ChangeResult[] results = cResp.get_return();
	for (UsersStub.ChangeResult r :results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (AttrResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected change response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Change responses correct");
    }


    protected CirclesStub.Attribute[] getCircleProfile(CirclesStub cStub,
	    String circleid, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	CirclesStub.GetCircleProfile cpReq = new CirclesStub.GetCircleProfile();
	CirclesStub.GetCircleProfileResponse cpResp = null;

	p.println("<!-- getCircleProfile " + comment + " -->");
	cpReq.setCircleid(circleid);
	try {
	    cpResp = cStub.getCircleProfile(cpReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getCircleProfile " +comment + " succeeded");
	    else
		failed("getCircleProfile " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getCircleProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getCircleProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getCircleProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	CirclesStub.Profile cp = cpResp.get_return();
	CirclesStub.Attribute[] profile = cp.getAttributes();
	return profile;
    }

    protected void changeCircleProfile(CirclesStub cStub, String circleid,
	    CirclesStub.ChangeAttribute[] changes, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode,
	    AttrResp[] resp) 
	throws RegressionException {
	CirclesStub.ChangeCircleProfile cReq = 
	    new CirclesStub.ChangeCircleProfile();
	CirclesStub.ChangeCircleProfileResponse cResp = null;

	cReq.setCircleid(circleid);
	cReq.setChanges(changes);
	p.println("<!-- changeCircleProfile " + comment + " -->");
	try {
	    cResp = cStub.changeCircleProfile(cReq);
	    p.println("<!-- returns succeess (failed change) -->");
	    if (shouldSucceed)
		log.info("changeCircleProfile " +comment +" succeeded");
	    else
		failed("changeCircleProfile " +comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changeCircleProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"changeCircleProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("changeCircleProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	CirclesStub.ChangeResult[] results = cResp.get_return();
	for (CirclesStub.ChangeResult r :results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (AttrResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected change response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Change responses correct");
    }


    protected ProjectsStub.Attribute[] getProjectProfile(ProjectsStub pStub,
	    String projectid, String comment, PrintStream p, 
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ProjectsStub.GetProjectProfile pReq = 
	    new ProjectsStub.GetProjectProfile();
	ProjectsStub.GetProjectProfileResponse pResp = null;

	p.println("<!-- getProjectProfile " + comment + " -->");
	pReq.setProjectid(projectid);
	try {
	    pResp = pStub.getProjectProfile(pReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getProjectProfile " +comment + " succeeded");
	    else
		failed("getProjectProfile " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getProjectProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getProjectProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getProjectProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;
	    
	ProjectsStub.Profile pr = pResp.get_return();
	ProjectsStub.Attribute[] profile = pr.getAttributes();
	return profile;
    }

    protected void changeProjectProfile(ProjectsStub pStub, String projectid,
	    ProjectsStub.ChangeAttribute[] changes, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode,
	    AttrResp[] resp) 
	throws RegressionException {
	ProjectsStub.ChangeProjectProfile cReq = 
	    new ProjectsStub.ChangeProjectProfile();
	ProjectsStub.ChangeProjectProfileResponse cResp = null;

	cReq.setProjectid(projectid);
	cReq.setChanges(changes);
	p.println("<!-- changeProjectProfile " + comment + " -->");
	try {
	    cResp = pStub.changeProjectProfile(cReq);
	    p.println("<!-- returns succeess (failed change) -->");
	    if (shouldSucceed)
		log.info("changeProjectProfile " +comment +" succeeded");
	    else
		failed("changeProjectProfile " +comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changeProjectProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"changeProjectProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("changeProjectProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return;

	ProjectsStub.ChangeResult[] results = cResp.get_return();
	for (ProjectsStub.ChangeResult r :results) {
	    String name = r.getName();

	    boolean foundit = false;
	    for (AttrResp a: resp) {
		if (name.equals(a.name)) {
		    if ( r.getSuccess() != a.success) 
			failed("Unexpected change response for " + name);
		    foundit = true;
		    break;
		}
	    }
	    if (!foundit) failed("Unexpected name in response " + name);
	}
	log.info("Change responses correct");
    }

    protected ExperimentsStub.Attribute[] getExperimentProfile(
	    ExperimentsStub eStub,
	    String eid, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.GetExperimentProfile eReq =
	    new ExperimentsStub.GetExperimentProfile();
	ExperimentsStub.GetExperimentProfileResponse eResp = null;

	p.println("<!-- getExperimentProfile " + comment + " -->");
	eReq.setEid(eid);
	try {
	    eResp = eStub.getExperimentProfile(eReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getExperimentProfile " +comment + " succeeded");
	    else
		failed("getExperimentProfile " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getExperimentProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getExperimentProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getExperimentProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;

	ExperimentsStub.Profile pr = eResp.get_return();
	ExperimentsStub.Attribute[] profile = pr.getAttributes();
	return profile;
    }

    protected LibrariesStub.Attribute[] getLibraryProfile(
	    LibrariesStub lStub,
	    String libid, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.GetLibraryProfile lReq =
	    new LibrariesStub.GetLibraryProfile();
	LibrariesStub.GetLibraryProfileResponse lResp = null;

	p.println("<!-- getLibraryProfile " + comment + " -->");
	lReq.setLibid(libid);
	try {
	    lResp = lStub.getLibraryProfile(lReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("getLibraryProfile " +comment + " succeeded");
	    else
		failed("getLibraryProfile " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getLibraryProfile " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getLibraryProfile " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getLibraryProfile " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed) return null;

	LibrariesStub.Profile pr = lResp.get_return();
	LibrariesStub.Attribute[] profile = pr.getAttributes();
	return profile;
    }

    protected void sendNotification(UsersStub uStub, String[] users,
	    String text, UsersStub.NotificationFlag[] flags, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode) 
	throws RegressionException {
	UsersStub.SendNotification sReq = new UsersStub.SendNotification();

	sReq.setUsers(users);
	sReq.setText(text);
	sReq.setFlags(flags);
	p.println("<!-- sendNotification " + comment + " -->");
	try {
	    uStub.sendNotification(sReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed) 
		log.info("sendNotification " + comment + " succeeded");
	    else
		failed("sendNotification " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("sendNotification " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"sendNotification " + comment + " failed");
	}
	catch (Exception e) {
	    failed("sendNotification " + comment +
		    " failed: unexpected exception");
	}
    }

    protected UsersStub.UserNotification[] getNotifications(UsersStub uStub,
	    String uid, String from, String to,
	    UsersStub.NotificationFlag[] flags,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	UsersStub.GetNotifications gReq = new UsersStub.GetNotifications();
	UsersStub.GetNotificationsResponse gResp = null;

	p.println("<!-- getNotifications " + comment + " -->");
	gReq.setUid(uid);
	gReq.setFlags(flags);
	gReq.setFrom(from);
	gReq.setTo(to);
	try {
	    gResp = uStub.getNotifications(gReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed ) 
		log.info("getNotifications " + comment + " succeeded");
	    else
		failed("getNotifications " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("getNotifications " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"getNotifications " + comment + " failed");
	}
	catch (Exception e) {
	    failed("getNotifications " + comment +
		    " failed: unexpected exception");
	}
	if ( !shouldSucceed ) return null;
	return gResp.get_return();
    }

    protected void markNotifications(UsersStub uStub, String uid, long[] msgs,
	    UsersStub.NotificationFlag[] flags, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	UsersStub.MarkNotifications mReq = new UsersStub.MarkNotifications();

	p.println("<!-- markNotifications " +comment + " -->");
	mReq.setUid(uid);
	mReq.setIds(msgs);
	mReq.setFlags(flags);
	try {
	    uStub.markNotifications(mReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("markNotifications " +comment + " succeeded");
	    else
		failed("markNotifications " +comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed )
		failed("markNotifications " +comment + " failed");
	    else
		checkFault(f, expectedCode,
			"markNotifications " +comment + " failed");
	}
	catch (Exception e) {
	    failed("markNotifications " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeExperiment(ExperimentsStub eStub, String eid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ExperimentsStub.RemoveExperiment rReq =
	    new ExperimentsStub.RemoveExperiment() ;
	rReq.setEid(eid);
	p.println("<!-- removeExperiment " + comment + " -->");
	try {
	    eStub.removeExperiment(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeExperiment " + comment + " succeeded");
	    else
		failed("removeExperiment " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeExperiment " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeExperiment " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeExperiment " + comment +
		    " failed: unexpected exception");
	}
    }


    protected void removeLibrary(LibrariesStub lStub, String libid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	LibrariesStub.RemoveLibrary rReq =
	    new LibrariesStub.RemoveLibrary() ;
	rReq.setLibid(libid);
	p.println("<!-- removeLibrary " + comment + " -->");
	try {
	    lStub.removeLibrary(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeLibrary " + comment + " succeeded");
	    else
		failed("removeLibrary " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeLibrary " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeLibrary " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeLibrary " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeCircle(CirclesStub cStub, String circleid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {
	CirclesStub.RemoveCircle rReq = new CirclesStub.RemoveCircle() ;
	rReq.setCircleid(circleid);
	p.println("<!-- removeCircle " + comment + " -->");
	try {
	    cStub.removeCircle(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeCircle " + comment + " succeeded");
	    else
		failed("removeCircle " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeCircle " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeCircle " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeCircle " + comment + " failed: unexpected exception");
	}
    }

    protected void removeProject(ProjectsStub cStub, String projectid,
	    String comment, PrintStream p, boolean shouldSucceed, 
	    int expectedCode) 
	throws RegressionException {
	ProjectsStub.RemoveProject rReq = new ProjectsStub.RemoveProject() ;
	rReq.setProjectid(projectid);
	p.println("<!-- removeProject " + comment + " -->");
	try {
	    cStub.removeProject(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeProject " + comment + " succeeded");
	    else
		failed("removeProject " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeProject " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeProject " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeProject " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeUser(UsersStub cStub, String uid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {
	UsersStub.RemoveUser rReq = new UsersStub.RemoveUser() ;
	rReq.setUid(uid);
	p.println("<!-- removeUser " + comment + " -->");
	try {
	    cStub.removeUser(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeUser " + comment + " succeeded");
	    else
		failed("removeUser " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeUser " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeUser " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeUser " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeUserAttribute(UsersStub uStub, String name, 
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	UsersStub.RemoveUserAttribute rReq = 
	    new UsersStub.RemoveUserAttribute();

	// Remove non-existing attribute
	p.println("<!-- removeUserAttribute " + comment + " -->");
	rReq.setName(name);
	try { 
	    uStub.removeUserAttribute(rReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("removeUserAttribute " + comment + " succeeded");
	    else
		failed("removeUserAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) { 
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeUserAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeUserAttribute " + comment + " failed");
	}
	catch (Exception e) { 
	    failed("removeUserAttribute " + comment + 
		    " failed: unexpected exception");
	}
    }

    protected void removeCircleAttribute(CirclesStub cStub, String name, 
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	CirclesStub.RemoveCircleAttribute rReq = 
	    new CirclesStub.RemoveCircleAttribute();

	// Remove non-existing attribute
	p.println("<!-- removeCircleAttribute " + comment + " -->");
	rReq.setName(name);
	try { 
	    cStub.removeCircleAttribute(rReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("removeCircleAttribute " + comment + " succeeded");
	    else
		failed("removeCircleAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) { 
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeCircleAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeCircleAttribute " + comment + " failed");
	}
	catch (Exception e) { 
	    failed("removeCircleAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeExperimentAttribute(ExperimentsStub cStub,
	    String name, String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ExperimentsStub.RemoveExperimentAttribute rReq = 
	    new ExperimentsStub.RemoveExperimentAttribute();

	// Remove non-existing attribute
	p.println("<!-- removeExperimentAttribute " + comment + " -->");
	rReq.setName(name);
	try { 
	    cStub.removeExperimentAttribute(rReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("removeExperimentAttribute " + comment + " succeeded");
	    else
		failed("removeExperimentAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) { 
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeExperimentAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeExperimentAttribute " + comment + " failed");
	}
	catch (Exception e) { 
	    failed("removeExperimentAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeLibraryAttribute(LibrariesStub cStub,
	    String name, String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	LibrariesStub.RemoveLibraryAttribute rReq =
	    new LibrariesStub.RemoveLibraryAttribute();

	// Remove non-existing attribute
	p.println("<!-- removeLibraryAttribute " + comment + " -->");
	rReq.setName(name);
	try {
	    cStub.removeLibraryAttribute(rReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("removeLibraryAttribute " + comment + " succeeded");
	    else
		failed("removeLibraryAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeLibraryAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"removeLibraryAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("removeLibraryAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeProjectAttribute(ProjectsStub pStub, String name, 
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ProjectsStub.RemoveProjectAttribute rReq = 
	    new ProjectsStub.RemoveProjectAttribute();

	// Remove non-existing attribute
	p.println("<!-- removeProjectAttribute " + comment + " -->");
	rReq.setName(name);
	try { 
	    pStub.removeProjectAttribute(rReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("removeProjectAttribute " + comment + " succeeded");
	    else
		failed("removeProjectAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) { 
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeProjectAttribute " + comment + "failed");
	    else
		checkFault(f, expectedCode,
			"removeProjectAttribute " + comment + "failed");
	}
	catch (Exception e) { 
	    failed("removeProjectAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changePassword(UsersStub uStub, String uid, String pass,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode) 
	throws RegressionException {

	UsersStub.ChangePassword cReq = new UsersStub.ChangePassword();
	p.println("<!-- changePassword " + comment + " -->");
	cReq.setUid(uid);
	cReq.setNewPass(pass);
	try {
	    uStub.changePassword(cReq);
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("changePassword " + comment + " succeeded");
	    else
		failed("changePassword " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed) 
		failed("changePassword " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"changePassword " + comment + " failed");
	}
	catch (Exception e) {
	    failed("changePassword " + comment +
		    " failed: unexpected exception");
	}
    }

    protected ExperimentsStub.ExperimentAspect initExperimentAspect(
	    String name, String type, String subType, String dataRef,
	    byte[] data) {
	ExperimentsStub.ExperimentAspect ea =
	    new ExperimentsStub.ExperimentAspect();

	ea.setName(name);
	ea.setType(type);
	ea.setSubType(subType);
	ea.setDataReference(dataRef);
	if (data != null)
	    ea.setData(putBytes(data));

	return ea;
    }

    protected ExperimentsStub.ExperimentAspect initExperimentAspect(
	    ExperimentsStub.ExperimentAspect e, boolean initData) {
	byte[] data = null;

	if (initData) {
	    try {
		data = getBytes(e.getData());
	    }
	    catch (IOException ignored) { }
	}
	return initExperimentAspect(e.getName(), e.getType(), e.getSubType(),
		e.getDataReference(), data);
    }

    protected void addExperimentAspects(ExperimentsStub eStub, String eid,
	    ExperimentsStub.ExperimentAspect[] aspects, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.AddExperimentAspects eReq =
	    new ExperimentsStub.AddExperimentAspects() ;
	ExperimentsStub.AddExperimentAspectsResponse eResp = null;
	eReq.setEid(eid);
	eReq.setAspects(aspects);
	p.println("<!-- addExperimentAspects " + comment +"-->");
	try {
	    eResp = eStub.addExperimentAspects(eReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("addExperimentAspects " + comment +" succeeded");
	    else
		failed("addExperimentAspects " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("addExperimentAspects " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"addExperimentAspects " + comment +" failed");
	}
	catch (Exception e) {
	    failed("addExperimentAspects " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeExperimentAspects(ExperimentsStub eStub, String eid,
	    ExperimentsStub.ExperimentAspect[] aspects, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.RemoveExperimentAspects eReq =
	    new ExperimentsStub.RemoveExperimentAspects() ;
	ExperimentsStub.RemoveExperimentAspectsResponse eResp = null;
	eReq.setEid(eid);
	eReq.setAspects(aspects);
	p.println("<!-- removeExperimentAspects " + comment +"-->");
	try {
	    eResp = eStub.removeExperimentAspects(eReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeExperimentAspects " + comment +" succeeded");
	    else
		failed("removeExperimentAspects " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeExperimentAspects " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"removeExperimentAspects " + comment +" failed");
	}
	catch (Exception e) {
	    failed("removeExperimentAspects " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changeExperimentAspects(ExperimentsStub eStub, String eid,
	    ExperimentsStub.ExperimentAspect[] aspects, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.ChangeExperimentAspects eReq =
	    new ExperimentsStub.ChangeExperimentAspects() ;
	ExperimentsStub.ChangeExperimentAspectsResponse eResp = null;
	eReq.setEid(eid);
	eReq.setAspects(aspects);
	p.println("<!-- changeExperimentAspects " + comment +"-->");
	try {
	    eResp = eStub.changeExperimentAspects(eReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("changeExperimentAspects " + comment +" succeeded");
	    else
		failed("changeExperimentAspects " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changeExperimentAspects " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"changeExperimentAspects " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changeExperimentAspects " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changeExperimentACL(ExperimentsStub eStub, String eid,
	    ExperimentsStub.AccessMember[] acl, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.ChangeExperimentACL eReq =
	    new ExperimentsStub.ChangeExperimentACL() ;
	ExperimentsStub.ChangeExperimentACLResponse eResp = null;
	eReq.setEid(eid);
	eReq.setAcl(acl);

	p.println("<!-- changeExperimentACL " + comment +"-->");
	try {
	    eResp = eStub.changeExperimentACL(eReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("changeExperimentACL " + comment +" succeeded");
	    else
		failed("changeExperimentACL " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changeExperimentACL " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"changeExperimentACL " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changeExperimentACL " + comment +
		    " failed: unexpected exception");
	}
    }


    protected void addLibraryExperiments(LibrariesStub lStub, String libid,
	    String[] eids, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.AddLibraryExperiments lReq =
	    new LibrariesStub.AddLibraryExperiments() ;
	LibrariesStub.AddLibraryExperimentsResponse lResp = null;
	lReq.setLibid(libid);
	lReq.setEids(eids);
	p.println("<!-- addLibraryExperiments " + comment +"-->");
	try {
	    lResp = lStub.addLibraryExperiments(lReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("addLibraryExperiments " + comment +" succeeded");
	    else
		failed("addLibraryExperiments " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("addLibraryExperiments " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"addLibraryExperiments " + comment +" failed");
	}
	catch (Exception e) {
	    failed("addLibraryExperiments " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeLibraryExperiments(LibrariesStub lStub, String libid,
	    String[] eids, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.RemoveLibraryExperiments lReq =
	    new LibrariesStub.RemoveLibraryExperiments() ;
	LibrariesStub.RemoveLibraryExperimentsResponse lResp = null;
	lReq.setLibid(libid);
	lReq.setEids(eids);
	p.println("<!-- removeLibraryExperiments " + comment +"-->");
	try {
	    lResp = lStub.removeLibraryExperiments(lReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeLibraryExperiments " + comment +" succeeded");
	    else
		failed("removeLibraryExperiments " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeLibraryExperiments " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"removeLibraryExperiments " + comment +" failed");
	}
	catch (Exception e) {
	    failed("removeLibraryExperiments " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void changeLibraryACL(LibrariesStub lStub, String libid,
	    LibrariesStub.AccessMember[] acl, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.ChangeLibraryACL lReq =
	    new LibrariesStub.ChangeLibraryACL() ;
	LibrariesStub.ChangeLibraryACLResponse lResp = null;
	lReq.setLibid(libid);
	lReq.setAcl(acl);

	p.println("<!-- changeLibraryACL " + comment +"-->");
	try {
	    lResp = lStub.changeLibraryACL(lReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("changeLibraryACL " + comment +" succeeded");
	    else
		failed("changeLibraryACL " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("changeLibraryACL " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"changeLibraryACL " + comment +" failed");
	}
	catch (Exception e) {
	    failed("changeLibraryACL " + comment +
		    " failed: unexpected exception");
	}
    }
    protected void checkFault(UsersDeterFault f, int expected, String msg)
	throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(ProjectsDeterFault f, int expected, String msg)
	throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(RealizationsDeterFault f, int expected,
	    String msg)
	throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(ResourcesDeterFault f, int expected,
	    String msg)
	throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(CirclesDeterFault f, int expected, String msg)
	throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(ExperimentsDeterFault f,
	    int expected, String msg) throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void checkFault(LibrariesDeterFault f,
	    int expected, String msg) throws RegressionException {
	DeterFault df = getDeterFault(f);
	if ( df.getErrorCode() == expected)
	    log.info(msg);
	else
	    failed(msg + " wrong error code. Expected " + expected + " got " +
		    df.getErrorCode());
    }

    protected void createCircle(CirclesStub pStub, String circleid,
	    String owner, CirclesStub.Attribute[] profile, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	CirclesStub.CreateCircle cpReq = new CirclesStub.CreateCircle() ;
	CirclesStub.CreateCircleResponse cpResp = null;
	cpReq.setCircleid(circleid);
	cpReq.setOwner(owner);
	cpReq.setProfile(profile);
	p.println("<!-- createCircle " + comment +"-->");
	try {
	    cpResp = pStub.createCircle(cpReq);
	    circles.add(cpReq.getCircleid());
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("createCircle " + comment +" succeeded");
	    else
		failed("createCircle " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createCircle " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"createCircle " + comment +" failed");
	}
	catch (Exception e) {
	    failed("createCircle " + comment +" failed: unexpected exception");
	}
    }

    protected void createCircleAttribute(CirclesStub CStub, String name,
	    String type, boolean optional, String access, String desc,
	    String format, String formatdesc, int order, int len,
	    String def, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	CirclesStub.CreateCircleAttribute req =
	    new CirclesStub.CreateCircleAttribute();

	req.setName(name);
	req.setType(type);
	req.setOptional(optional);
	req.setAccess(access);
	req.setDescription(desc);
	req.setFormat(format);
	req.setFormatdescription(formatdesc);
	req.setOrder(order);
	req.setLength(len);
	req.setDef(def);

	// Creation - try a new attribute without a default and not optional
	p.println("<!-- create " + comment + " -->");
	try {
	    CStub.createCircleAttribute(req);
	    p.println("<!-- succeeded -->");
	    circleAttrs.add(name);
	    if (shouldSucceed)
		log.info("createCircleAttribute " + comment + " succeeded");
	    else
		failed("createCircleAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (CirclesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createCircleAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"createCircleAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("createCircleAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createExperiment(ExperimentsStub eStub, String eid,
	    String owner, ExperimentsStub.ExperimentAspect[] aspects,
	    ExperimentsStub.AccessMember[] accessLists,
	    ExperimentsStub.Attribute[] profile, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.CreateExperiment eReq =
	    new ExperimentsStub.CreateExperiment() ;
	ExperimentsStub.CreateExperimentResponse eResp = null;
	eReq.setEid(eid);
	eReq.setOwner(owner);
	eReq.setAspects(aspects);
	eReq.setAccessLists(accessLists);
	eReq.setProfile(profile);
	p.println("<!-- createExperiment " + comment +"-->");
	try {
	    eResp = eStub.createExperiment(eReq);
	    experiments.add(eReq.getEid());
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("createExperiment " + comment +" succeeded");
	    else
		failed("createExperiment " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createExperiment " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"createExperiment " + comment +" failed");
	}
	catch (Exception e) {
	    failed("createExperiment " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void realizeExperiment(ExperimentsStub eStub, String eid,
	    String cid, String uid, ExperimentsStub.AccessMember[] acl,
	    Boolean sendNotifications,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ExperimentsStub.RealizeExperiment eReq =
	    new ExperimentsStub.RealizeExperiment() ;
	ExperimentsStub.RealizeExperimentResponse eResp = null;
	ExperimentsStub.RealizationDescription rd = null;

	eReq.setEid(eid);
	eReq.setCid(cid);
	eReq.setUid(uid);
	eReq.setAcl(acl);
	if ( sendNotifications != null ) 
	    eReq.setSendNotifications(sendNotifications);

	p.println("<!-- realizeExperiment " + comment +"-->");
	try {
	    eResp = eStub.realizeExperiment(eReq);
	    if ( (rd = eResp.get_return()) == null )
		throw new RegressionException(
			"null return from realizeExperiment!?");
	    realizations.add(rd.getName());
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("realizeExperiment " + comment +" succeeded");
	    else
		failed("realizeExperiment " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("realizeExperiment " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"realizeExperiment " + comment +" failed");
	}
	catch (Exception e) {
	    failed("realizeExperiment " + comment +
		    " failed: unexpected exception");
	}
    }

    // XXX: will get folded into some kind of check routine someday
    protected void viewRealizations(RealizationsStub rStub, String uid,
	    String regex, int offset, int count,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	RealizationsStub.ViewRealizations rReq =
	    new RealizationsStub.ViewRealizations() ;
	RealizationsStub.ViewRealizationsResponse rResp = null;

	rReq.setUid(uid);
	if ( regex != null && !regex.isEmpty()) rReq.setRegex(regex);
	if ( offset > -1 ) rReq.setOffset(offset);
	if ( count > -1 ) rReq.setCount(count);
	p.println("<!-- viewRealizations " + comment +"-->");
	try {
	    rResp = rStub.viewRealizations(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("viewRealizations " + comment +" succeeded");
	    else
		failed("viewRealizations " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (RealizationsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("viewRealizations " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"viewRealizations " + comment +" failed");
	}
	catch (Exception e) {
	    failed("viewRealizations " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void releaseRealization(RealizationsStub rStub, String rid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	RealizationsStub.ReleaseRealization rReq =
	    new RealizationsStub.ReleaseRealization() ;
	RealizationsStub.ReleaseRealizationResponse rResp = null;

	rReq.setName(rid);
	p.println("<!-- releaseRealization " + comment +"-->");
	try {
	    rResp = rStub.releaseRealization(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("releaseRealization " + comment +" succeeded");
	    else
		failed("releaseRealization " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (RealizationsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("releaseRealization " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"releaseRealization " + comment +" failed");
	}
	catch (Exception e) {
	    failed("releaseRealization " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void removeRealization(RealizationsStub rStub, String rid,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	RealizationsStub.RemoveRealization rReq =
	    new RealizationsStub.RemoveRealization() ;
	RealizationsStub.RemoveRealizationResponse rResp = null;

	rReq.setName(rid);
	p.println("<!-- removeRealization " + comment +"-->");
	try {
	    rResp = rStub.removeRealization(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("removeRealization " + comment +" succeeded");
	    else
		failed("removeRealization " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (RealizationsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("removeRealization " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"removeRealization " + comment +" failed");
	}
	catch (Exception e) {
	    failed("removeRealization " + comment +
		    " failed: unexpected exception");
	}
    }

    // XXX: will get folded into some kind of check routine someday
    protected void viewResources(ResourcesStub rStub, String uid,
	    String type, String regex, String realization,
	    Boolean persist, ResourcesStub.ResourceTag[] tags,
	    int offset, int count,
	    String comment, PrintStream p, boolean shouldSucceed,
	    int expectedCode)
	throws RegressionException {
	ResourcesStub.ViewResources rReq =
	    new ResourcesStub.ViewResources() ;
	ResourcesStub.ViewResourcesResponse rResp = null;

	rReq.setUid(uid);
	if ( type != null && !type.isEmpty()) rReq.setType(type);
	if ( regex != null && !regex.isEmpty()) rReq.setRegex(regex);
	if ( realization != null && !realization.isEmpty())
	    rReq.setRealization(realization);
	if ( persist != null ) rReq.setPersist(persist);
	if ( tags != null && tags.length > 0 ) rReq.setTags(tags);
	if ( offset > -1 ) rReq.setOffset(offset);
	if ( count > -1 ) rReq.setCount(count);
	p.println("<!-- viewResources " + comment +"-->");
	try {
	    rResp = rStub.viewResources(rReq);
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("viewResources " + comment +" succeeded");
	    else
		failed("viewResources " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ResourcesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("viewResources " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"viewResources " + comment +" failed");
	}
	catch (Exception e) {
	    failed("viewResources " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createExperimentAttribute(ExperimentsStub CStub,
	    String name, String type, boolean optional, String access,
	    String desc, String format, String formatdesc, int order, int len,
	    String def, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ExperimentsStub.CreateExperimentAttribute req =
	    new ExperimentsStub.CreateExperimentAttribute();

	req.setName(name);
	req.setType(type);
	req.setOptional(optional);
	req.setAccess(access);
	req.setDescription(desc);
	req.setFormat(format);
	req.setFormatdescription(formatdesc);
	req.setOrder(order);
	req.setLength(len);
	req.setDef(def);

	// Creation - try a new attribute
	p.println("<!-- create " + comment + " -->");
	try {
	    CStub.createExperimentAttribute(req);
	    p.println("<!-- succeeded -->");
	    experimentAttrs.add(name);
	    if (shouldSucceed)
		log.info("createExperimentAttribute " + comment + " succeeded");
	    else
		failed("createExperimentAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ExperimentsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createExperimentAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"createExperimentAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("createExperimentAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createLibrary(LibrariesStub lStub, String libid,
	    String owner, String[] experiments,
	    LibrariesStub.AccessMember[] accessLists,
	    LibrariesStub.Attribute[] profile, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.CreateLibrary lReq =
	    new LibrariesStub.CreateLibrary() ;
	LibrariesStub.CreateLibraryResponse lResp = null;
	lReq.setLibid(libid);
	lReq.setOwner(owner);
	lReq.setEids(experiments);
	lReq.setAccessLists(accessLists);
	lReq.setProfile(profile);
	p.println("<!-- createLibrary " + comment +"-->");
	try {
	    lResp = lStub.createLibrary(lReq);
	    libraries.add(lReq.getLibid());
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("createLibrary " + comment +" succeeded");
	    else
		failed("createLibrary " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createLibrary " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"createLibrary " + comment +" failed");
	}
	catch (Exception e) {
	    failed("createLibrary " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createLibraryAttribute(LibrariesStub CStub,
	    String name, String type, boolean optional, String access,
	    String desc, String format, String formatdesc, int order, int len,
	    String def, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	LibrariesStub.CreateLibraryAttribute req =
	    new LibrariesStub.CreateLibraryAttribute();

	req.setName(name);
	req.setType(type);
	req.setOptional(optional);
	req.setAccess(access);
	req.setDescription(desc);
	req.setFormat(format);
	req.setFormatdescription(formatdesc);
	req.setOrder(order);
	req.setLength(len);
	req.setDef(def);

	// Creation - try a new attribute
	p.println("<!-- create " + comment + " -->");
	try {
	    CStub.createLibraryAttribute(req);
	    p.println("<!-- succeeded -->");
	    libraryAttrs.add(name);
	    if (shouldSucceed)
		log.info("createLibraryAttribute " + comment + " succeeded");
	    else
		failed("createLibraryAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (LibrariesDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createLibraryAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"createLibraryAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("createLibraryAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createProject(ProjectsStub pStub, String projectid,
	    String owner, ProjectsStub.Attribute[] profile, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ProjectsStub.CreateProject cpReq = new ProjectsStub.CreateProject() ;
	ProjectsStub.CreateProjectResponse cpResp = null;
	cpReq.setProjectid(projectid);
	cpReq.setOwner(owner);
	cpReq.setProfile(profile);
	p.println("<!-- createProject " + comment +"-->");
	try {
	    cpResp = pStub.createProject(cpReq);
	    projects.add(cpReq.getProjectid());
	    p.println("<!-- succeeded -->");
	    if ( shouldSucceed)
		log.info("createProject " + comment +" succeeded");
	    else
		failed("createProject " + comment +" succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createProject " + comment +" failed");
	    else
		checkFault(f, expectedCode,
			"createProject " + comment +" failed");
	}
	catch (Exception e) {
	    failed("createProject " + comment +
		    " failed: unexpected exception");
	}
    }

    protected void createProjectAttribute(ProjectsStub pStub, String name,
	    String type, boolean optional, String access, String desc,
	    String format, String formatdesc, int order, int len,
	    String def, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	ProjectsStub.CreateProjectAttribute req =
	    new ProjectsStub.CreateProjectAttribute();

	req.setName(name);
	req.setType(type);
	req.setOptional(optional);
	req.setAccess(access);
	req.setDescription(desc);
	req.setFormat(format);
	req.setFormatdescription(formatdesc);
	req.setOrder(order);
	req.setLength(len);
	req.setDef(def);

	// Creation - try a new attribute without a default and not optional
	p.println("<!-- create " + comment + " -->");
	try {
	    pStub.createProjectAttribute(req);
	    p.println("<!-- succeeded -->");
	    projectAttrs.add(name);
	    if (shouldSucceed)
		log.info("createProjectAttribute " + comment + " succeeded");
	    else
		failed("createProjectAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (ProjectsDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createProjectAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"createProjectAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("createProjectAttribute " + comment +
		    " failed: unexpected exception");
	}
    }


    protected String createUser(UsersStub uStub, String uid,
	    UsersStub.Attribute[] profile, String url, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	UsersStub.CreateUser cuReq = new UsersStub.CreateUser();
	UsersStub.CreateUserResponse cuResp = null;
	UsersStub.CreateUserResult res = null;
	cuReq.setUid(uid);
	cuReq.setProfile(profile);
	cuReq.setUrlPrefix(url);

	p.println("<!-- createUser " + comment + " -->");
	try {
	    cuResp = uStub.createUser(cuReq);
	    res = cuResp.get_return();
	    users.add(res.getUid());
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("createUser " + comment + "  succeeded");
	    else
		failed("createUser " + comment + "  succeeded");
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createUser " + comment + "  failed");
	    else
		checkFault(f, expectedCode,
			"createUser " + comment + "  failed");
	}
	catch (Exception e) {
	    failed("createUser " + comment +
		    "  failed: unexpected exception");
	}

	if (shouldSucceed) return res.getUid();
	else return null;
    }

    protected String createUserNoConfirm(UsersStub uStub, String uid,
	    UsersStub.Attribute[] profile, String clearPassword,
	    String hash, String hashType, String comment,
	    PrintStream p, boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	UsersStub.CreateUserNoConfirm cuReq =
	    new UsersStub.CreateUserNoConfirm() ;
	UsersStub.CreateUserNoConfirmResponse cuResp = null;
	cuReq.setUid(uid);
	cuReq.setProfile(profile);
	cuReq.setClearpassword(clearPassword);
	cuReq.setHash(hash);
	cuReq.setHashtype(hashType);

	p.println("<!-- createUserNoConfirm " + comment + " -->");
	try {
	    cuResp = uStub.createUserNoConfirm(cuReq);
	    users.add(cuResp.get_return());
	    p.println("<!-- succeeded -->");
	    if (shouldSucceed)
		log.info("createUserNoConfirm " + comment + "  succeeded");
	    else
		failed("createUserNoConfirm " + comment + "  succeeded");
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createUserNoConfirm " + comment + "  failed");
	    else
		checkFault(f, expectedCode,
			"createUserNoConfirm " + comment + "  failed");
	}
	catch (Exception e) {
	    failed("createUserNoConfirm " + comment +
		    "  failed: unexpected exception");
	}

	if (shouldSucceed) return cuResp.get_return();
	else return null;
    }

    protected void createUserAttribute(UsersStub uStub, String name,
	    String type, boolean optional, String access, String desc,
	    String format, String formatdesc, int order, int len,
	    String def, String comment, PrintStream p,
	    boolean shouldSucceed, int expectedCode)
	throws RegressionException {
	UsersStub.CreateUserAttribute req =
	    new UsersStub.CreateUserAttribute();

	req.setName(name);
	req.setType(type);
	req.setOptional(optional);
	req.setAccess(access);
	req.setDescription(desc);
	req.setFormat(format);
	req.setFormatdescription(formatdesc);
	req.setOrder(order);
	req.setLength(len);
	req.setDef(def);

	// Creation - try a new attribute without a default and not optional
	p.println("<!-- create " + comment + " -->");
	try {
	    uStub.createUserAttribute(req);
	    p.println("<!-- succeeded -->");
	    userAttrs.add(name);
	    if (shouldSucceed)
		log.info("createUserAttribute " + comment + " succeeded");
	    else
		failed("createUserAttribute " + comment + " succeeded");
	}
	catch (RegressionException e) {
	    // failed throws a Regression exception that should not be caught
	    // below
	    throw e;
	}
	catch (UsersDeterFault f) {
	    p.println("<!-- failed -->");
	    if (shouldSucceed)
		failed("createUserAttribute " + comment + " failed");
	    else
		checkFault(f, expectedCode,
			"createUserAttribute " + comment + " failed");
	}
	catch (Exception e) {
	    failed("createUserAttribute " + comment +
		    " failed: unexpected exception");
	}
    }

    /**
     * Remove any circles successfully added.
     */
    protected void cleanUpCircles() {
	CirclesStub pStub = null;
	try {
	    pStub = new CirclesStub(getServiceUrl() + "Circles");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored) { }
	CirclesStub.RemoveCircle rpReq = new CirclesStub.RemoveCircle() ;
	for (String uid: circles) {
	    rpReq.setCircleid(uid);
	    try {
		pStub.removeCircle(rpReq);
		log.info("Removed " + uid);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Remove any circle attributes successfully added
     */
    protected void cleanUpCircleAttributes() {
	CirclesStub CStub = null;
	try {
	    CStub = new CirclesStub(getServiceUrl() + "Circles");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// Login as the admin user
	try {
	    login("testadmin", "test");
	}
	catch (Exception ignored ) { }


	CirclesStub.RemoveCircleAttribute raReq =
	    new CirclesStub.RemoveCircleAttribute() ;
	for (String a: circleAttrs) {
	    raReq.setName(a);
	    try {
		CStub.removeCircleAttribute(raReq);
		log.info("Removed CircleAttribute " + a);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Clean up after the run. Remove any experiments successfully added
     */
    protected void cleanUpExperiments() {
	ExperimentsStub eStub = null;
	try {
	    eStub = new ExperimentsStub(getServiceUrl() + "Experiments");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored) { }
	ExperimentsStub.RemoveExperiment eReq =
	    new ExperimentsStub.RemoveExperiment() ;
	for (String eid: experiments) {
	    eReq.setEid(eid);
	    try {
		eStub.removeExperiment(eReq);
		log.info("Removed " + eid);
	    }
	    catch (Exception ignored) { }
	}
    }

    /**
     * Remove any experiment attributes successfully added
     */
    protected void cleanUpExperimentAttributes() {
	ExperimentsStub EStub = null;
	try {
	    EStub = new ExperimentsStub(getServiceUrl() + "Experiments");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// Login as the admin user
	try {
	    login("testadmin", "test");
	}
	catch (Exception ignored ) { }


	ExperimentsStub.RemoveExperimentAttribute raReq =
	    new ExperimentsStub.RemoveExperimentAttribute() ;
	for (String a: experimentAttrs) {
	    raReq.setName(a);
	    try {
		EStub.removeExperimentAttribute(raReq);
		log.info("Removed ExperimentAttribute " + a);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Remove any experiments successfully added
     */
    protected void cleanUpLibraries() {
	LibrariesStub lStub = null;
	try {
	    lStub = new LibrariesStub(getServiceUrl() + "Libraries");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored) { }
	LibrariesStub.RemoveLibrary eReq =
	    new LibrariesStub.RemoveLibrary() ;
	for (String libid: libraries) {
	    eReq.setLibid(libid);
	    try {
		lStub.removeLibrary(eReq);
		log.info("Removed " + libid);
	    }
	    catch (Exception ignored) { }
	}
    }

    /**
     * Remove any library attributes successfully added
     */
    protected void cleanUpLibraryAttributes() {
	LibrariesStub EStub = null;
	try {
	    EStub = new LibrariesStub(getServiceUrl() + "Libraries");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// Login as the admin user
	try {
	    login("testadmin", "test");
	}
	catch (Exception ignored ) { }


	LibrariesStub.RemoveLibraryAttribute raReq =
	    new LibrariesStub.RemoveLibraryAttribute() ;
	for (String a: libraryAttrs) {
	    raReq.setName(a);
	    try {
		EStub.removeLibraryAttribute(raReq);
		log.info("Removed LibraryAttribute " + a);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Remove any projects successfully added
     */
    protected void cleanUpProjects() {
	ProjectsStub pStub = null;
	try {
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored) { }
	ProjectsStub.RemoveProject rpReq = new ProjectsStub.RemoveProject() ;
	for (String uid: projects) {
	    rpReq.setProjectid(uid);
	    try {
		pStub.removeProject(rpReq);
		log.info("Removed " + uid);
	    }
	    catch (Exception ignored) { }
	}
    }

    /**
     * Remove any project attributes successfully added
     */
    protected void cleanUpProjectAttributes() {
	ProjectsStub pStub = null;
	try {
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// Login as the admin user
	try {
	    login("testadmin", "test");
	}
	catch (Exception ignored ) { }


	ProjectsStub.RemoveProjectAttribute raReq =
	    new ProjectsStub.RemoveProjectAttribute() ;
	for (String a: projectAttrs) {
	    raReq.setName(a);
	    try {
		pStub.removeProjectAttribute(raReq);
		log.info("Removed ProjectAttribute " + a);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Clean up after the run. Remove any realizations successfully added
     */
    protected void cleanUpRealizations() {
	RealizationsStub rStub = null;
	try {
	    rStub = new RealizationsStub(getServiceUrl() + "Realizations");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored) { }
	RealizationsStub.RemoveRealization rReq =
	    new RealizationsStub.RemoveRealization() ;
	for (String rid: realizations) {
	    rReq.setName(rid);
	    try {
		rStub.removeRealization(rReq);
		log.info("Removed " + rid);
	    }
	    catch (Exception ignored) { }
	}
    }

    /**
     * Remove any users successfully added
     */
    protected void cleanUpUsers() {
	UsersStub uStub = null;
	ProjectsStub pStub = null;
	boolean createdTestadmin = false;
	try {
	    login("testadmin", "test");
	}
	catch (DeterFault ignored ) { }
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	    pStub = new ProjectsStub(getServiceUrl() + "Projects");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// remove any users we created other than the testadmin
	UsersStub.RemoveUser ruReq = new UsersStub.RemoveUser() ;
	ProjectsStub.RemoveUsers rReq = new ProjectsStub.RemoveUsers();
	for (String uid: users) {
	    // Remove the testadmin user last, as it needs to exist to remove
	    // others.
	    if ( uid.equals("testadmin")) {
		createdTestadmin = true;
		continue;
	    }
	    ruReq.setUid(uid);
	    try {
		uStub.removeUser(ruReq);
		log.info("Removed " + uid);
	    }
	    catch (Exception ignored) { }
	}

	if (!createdTestadmin) return;

	// Finally pull out the testadmin
	String uid = "testadmin";
	ruReq.setUid(uid);
	try {
	    uStub.removeUser(ruReq);
	    log.info("Removed " + uid);
	}
	catch (Exception ignored) { }
    }
    /**
     * Remove any user attributes successfully added
     */
    protected void cleanUpUserAttributes() {
	UsersStub uStub = null;
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) {
	    log.error("Cleanup failed: cannot get service");
	    return;
	}

	// Login as the admin user
	try {
	    login("testadmin", "test");
	}
	catch (Exception ignored ) { }


	UsersStub.RemoveUserAttribute raReq =
	    new UsersStub.RemoveUserAttribute() ;
	for (String a: userAttrs) {
	    raReq.setName(a);
	    try {
		uStub.removeUserAttribute(raReq);
		log.info("Removed UserAttribute " + a);
	    }
	    catch (Exception ignored) { }
	}
    }
    /**
     * Clean up after the run.
     */
    public void cleanUp() {
	if (!libraries.isEmpty())
	    cleanUpLibraries();
	if (!realizations.isEmpty())
	    cleanUpRealizations();
	if (!experiments.isEmpty())
	    cleanUpExperiments();
	if (!circles.isEmpty())
	    cleanUpCircles();
	if (!projects.isEmpty())
	    cleanUpProjects();
	if (!users.isEmpty())
	    cleanUpUsers();
	if ( !circleAttrs.isEmpty())
	    cleanUpCircleAttributes();
	if ( !experimentAttrs.isEmpty())
	    cleanUpExperimentAttributes();
	if ( !libraryAttrs.isEmpty())
	    cleanUpLibraryAttributes();
	if ( !projectAttrs.isEmpty())
	    cleanUpProjectAttributes();
	if ( !userAttrs.isEmpty())
	    cleanUpUserAttributes();
    }
}
