package net.deterlab.testbed.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Flushable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.IOException;

import java.rmi.RemoteException;

import java.security.KeyStore;
import java.security.GeneralSecurityException;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.deterlab.abac.Identity;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.Attribute;

import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.ExperimentsDeterFault;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.LibrariesDeterFault;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.ProjectsDeterFault;
import net.deterlab.testbed.client.ResourcesStub;
import net.deterlab.testbed.client.ResourcesDeterFault;
import net.deterlab.testbed.client.UsersStub;
import net.deterlab.testbed.client.UsersDeterFault;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ListOption;
import net.deterlab.testbed.util.option.ParamOption;

import net.deterlab.testbed.util.gui.LoginDialog;

import org.apache.axis2.AxisFault;

/**
 * A utility to insert the default user profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class EmulabToDeter extends Utility {
    /** All valid attributes.  */
    static protected Attribute[] userAttributes = new Attribute[] {
	new Attribute("name", "STRING", false, 
		Attribute.READ_WRITE, "Name", 
		null, null, 100, 0),
	new Attribute("title", "STRING", true, 
		Attribute.READ_WRITE, "Title",
		null, null, 200, 0),
	new Attribute("email", "STRING", false, 
		Attribute.READ_ONLY, "E-mail",
		"[^\\s@]+@[^\\s@]+", "A valid e-mail address", 1100, 0),
	new Attribute("affiliation", "STRING", true, 
		Attribute.READ_WRITE, "Affiliation",
		null, null, 3000, 0),
	new Attribute("affiliation_abbrev", "STRING", true, 
		Attribute.READ_WRITE, "Affiliation (abbreviated)",
		null, null, 4000, 5),
	new Attribute("URL", "STRING", true, 
		Attribute.READ_WRITE, "URL",
		null, null, 1200, 0),
	new Attribute("address1", "STRING", true, 
		Attribute.READ_WRITE, "Address",
		null, null, 500, 0),
	new Attribute("address2", "STRING", true, 
		Attribute.READ_WRITE, "Address Line 2",
		null, null, 600, 0),
	new Attribute("city", "STRING", true, 
		Attribute.READ_WRITE, "City",
		null, null, 700, 0),
	new Attribute("state", "STRING", true, 
		Attribute.READ_WRITE, "State",
		null, null, 800, 0),
	new Attribute("zip", "STRING", true, 
		Attribute.READ_WRITE, "Postal Code",
		null, null, 900, 0),
	new Attribute("country", "STRING", true, 
		Attribute.READ_WRITE, "Country",
		null, null, 1000, 0),
	new Attribute("phone", "STRING", false, 
		Attribute.READ_WRITE, "Phone",
		"[0-9-\\s\\.\\(\\)\\+]+", 
		"Numbers, whitespace, parens, and dots or dashes", 
		1300, 15),
    };

    static protected Attribute[] projectAttributes = new Attribute[] {
	new Attribute("funders", "string", true, 
		Attribute.READ_WRITE, "Funders",
		null, null, 400, 0),
	new Attribute("affiliation", "string", true, 
		Attribute.READ_WRITE, "Affiliation",
		null, null, 300, 0),
	new Attribute("URL", "string", true, 
		Attribute.READ_WRITE, "URL",
		null, null, 200, 0),
	new Attribute("description", "string", false, 
		Attribute.READ_WRITE, "Description",
		null, null, 200, 0),
    };

    static protected Attribute[] circleAttributes  = new Attribute[] {
	new Attribute("description", "string", false, 
		Attribute.READ_WRITE, "Description", 
		null, null, 100, 0),
	new Attribute("email", "string", true, 
		Attribute.READ_WRITE, "Email", 
		"[0-9-\\s\\.]+", "A valid e-mail address", 200, 0),
    };

    static protected Attribute[] experimentAttributes  = new Attribute[] {
	new Attribute("description", "string", false,
		Attribute.READ_WRITE, "Description",
		null, null, 100, 0),
    };

    static protected Attribute[] libraryAttributes  = new Attribute[] {
	new Attribute("description", "string", false,
		Attribute.READ_WRITE, "Description",
		null, null, 100, 0),
    };


    static public void addUserAttributes(Attribute[] attrs)
	    throws RemoteException, AxisFault, UsersDeterFault  {
	UsersStub stub = new UsersStub(getServiceUrl("Users"));

	for (Attribute a: attrs) {
	    try {
		UsersStub.CreateUserAttribute req = 
		    new UsersStub.CreateUserAttribute();
		req.setName(a.getName());
		req.setType(a.getDataType());
		req.setOptional(a.getOptional());
		req.setAccess(a.getAccess());
		req.setDescription(a.getDescription());
		req.setFormat(a.getFormat());
		req.setFormatdescription(a.getFormatDescription());
		req.setOrder(a.getOrderingHint());
		req.setLength(a.getLengthHint());
		req.setDef("");

		stub.createUserAttribute(req);
		continue;
	    }
	    catch (UsersDeterFault e) {
		DeterFault df = getDeterFault(e);
		if (df.getErrorCode() != DeterFault.request) 
		    throw e;
	    }
	    UsersStub.ModifyUserAttribute req = 
		new UsersStub.ModifyUserAttribute();
	    req.setName(a.getName());
	    req.setType(a.getDataType());
	    req.setOptional(a.getOptional());
	    req.setAccess(a.getAccess());
	    req.setDescription(a.getDescription());
	    req.setFormat(a.getFormat());
	    req.setFormatdescription(a.getFormatDescription());
	    req.setOrder(a.getOrderingHint());
	    req.setLength(a.getLengthHint());

	    stub.modifyUserAttribute(req);
	}
    }

    static public void addProjectAttributes(Attribute[] attrs)
	    throws RemoteException, AxisFault, ProjectsDeterFault  {
	ProjectsStub stub = new ProjectsStub(getServiceUrl("Projects"));

	for (Attribute a: attrs) {
	    try {
		ProjectsStub.CreateProjectAttribute req = 
		    new ProjectsStub.CreateProjectAttribute();
		req.setName(a.getName());
		req.setType(a.getDataType());
		req.setOptional(a.getOptional());
		req.setAccess(a.getAccess());
		req.setDescription(a.getDescription());
		req.setFormat(a.getFormat());
		req.setFormatdescription(a.getFormatDescription());
		req.setOrder(a.getOrderingHint());
		req.setLength(a.getLengthHint());
		req.setDef("");

		stub.createProjectAttribute(req);
		continue;
	    }
	    catch (ProjectsDeterFault e) {
		DeterFault df = getDeterFault(e);
		if (df.getErrorCode() != DeterFault.request) 
		    throw e;
	    }
	    ProjectsStub.ModifyProjectAttribute req = 
		new ProjectsStub.ModifyProjectAttribute();
	    req.setName(a.getName());
	    req.setType(a.getDataType());
	    req.setOptional(a.getOptional());
	    req.setAccess(a.getAccess());
	    req.setDescription(a.getDescription());
	    req.setFormat(a.getFormat());
	    req.setFormatdescription(a.getFormatDescription());
	    req.setOrder(a.getOrderingHint());
	    req.setLength(a.getLengthHint());

	    stub.modifyProjectAttribute(req);
	}
    }
    static public void addCircleAttributes(Attribute[] attrs)
	    throws RemoteException, AxisFault, CirclesDeterFault  {
	CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));

	for (Attribute a: attrs) {
	    try {
		CirclesStub.CreateCircleAttribute req = 
		    new CirclesStub.CreateCircleAttribute();
		req.setName(a.getName());
		req.setType(a.getDataType());
		req.setOptional(a.getOptional());
		req.setAccess(a.getAccess());
		req.setDescription(a.getDescription());
		req.setFormat(a.getFormat());
		req.setFormatdescription(a.getFormatDescription());
		req.setOrder(a.getOrderingHint());
		req.setLength(a.getLengthHint());
		req.setDef("");

		stub.createCircleAttribute(req);
		continue;
	    }
	    catch (CirclesDeterFault e) {
		DeterFault df = getDeterFault(e);
		if (df.getErrorCode() != DeterFault.request) 
		    throw e;
	    }
	    CirclesStub.ModifyCircleAttribute req = 
		new CirclesStub.ModifyCircleAttribute();
	    req.setName(a.getName());
	    req.setType(a.getDataType());
	    req.setOptional(a.getOptional());
	    req.setAccess(a.getAccess());
	    req.setDescription(a.getDescription());
	    req.setFormat(a.getFormat());
	    req.setFormatdescription(a.getFormatDescription());
	    req.setOrder(a.getOrderingHint());
	    req.setLength(a.getLengthHint());

	    stub.modifyCircleAttribute(req);
	}
    }

    static public void addExperimentAttributes(Attribute[] attrs)
	    throws RemoteException, AxisFault, ExperimentsDeterFault  {
	ExperimentsStub stub = new ExperimentsStub(
		getServiceUrl("Experiments"));

	for (Attribute a: attrs) {
	    try {
		ExperimentsStub.CreateExperimentAttribute req =
		    new ExperimentsStub.CreateExperimentAttribute();
		req.setName(a.getName());
		req.setType(a.getDataType());
		req.setOptional(a.getOptional());
		req.setAccess(a.getAccess());
		req.setDescription(a.getDescription());
		req.setFormat(a.getFormat());
		req.setFormatdescription(a.getFormatDescription());
		req.setOrder(a.getOrderingHint());
		req.setLength(a.getLengthHint());
		req.setDef("");

		stub.createExperimentAttribute(req);
		continue;
	    }
	    catch (ExperimentsDeterFault e) {
		DeterFault df = getDeterFault(e);
		if (df.getErrorCode() != DeterFault.request)
		    throw e;
	    }
	    ExperimentsStub.ModifyExperimentAttribute req =
		new ExperimentsStub.ModifyExperimentAttribute();
	    req.setName(a.getName());
	    req.setType(a.getDataType());
	    req.setOptional(a.getOptional());
	    req.setAccess(a.getAccess());
	    req.setDescription(a.getDescription());
	    req.setFormat(a.getFormat());
	    req.setFormatdescription(a.getFormatDescription());
	    req.setOrder(a.getOrderingHint());
	    req.setLength(a.getLengthHint());

	    stub.modifyExperimentAttribute(req);
	}
    }

    static public void addLibraryAttributes(Attribute[] attrs)
	    throws RemoteException, AxisFault, LibrariesDeterFault  {
	LibrariesStub stub = new LibrariesStub(
		getServiceUrl("Libraries"));

	for (Attribute a: attrs) {
	    try {
		LibrariesStub.CreateLibraryAttribute req =
		    new LibrariesStub.CreateLibraryAttribute();
		req.setName(a.getName());
		req.setType(a.getDataType());
		req.setOptional(a.getOptional());
		req.setAccess(a.getAccess());
		req.setDescription(a.getDescription());
		req.setFormat(a.getFormat());
		req.setFormatdescription(a.getFormatDescription());
		req.setOrder(a.getOrderingHint());
		req.setLength(a.getLengthHint());
		req.setDef("");

		stub.createLibraryAttribute(req);
		continue;
	    }
	    catch (LibrariesDeterFault e) {
		DeterFault df = getDeterFault(e);
		if (df.getErrorCode() != DeterFault.request)
		    throw e;
	    }
	    LibrariesStub.ModifyLibraryAttribute req =
		new LibrariesStub.ModifyLibraryAttribute();
	    req.setName(a.getName());
	    req.setType(a.getDataType());
	    req.setOptional(a.getOptional());
	    req.setAccess(a.getAccess());
	    req.setDescription(a.getDescription());
	    req.setFormat(a.getFormat());
	    req.setFormatdescription(a.getFormatDescription());
	    req.setOrder(a.getOrderingHint());
	    req.setLength(a.getLengthHint());

	    stub.modifyLibraryAttribute(req);
	}
    }

    static public byte[] runProcess(String[] cmd, boolean captureOut,
	    boolean showErr, byte[] input)
	throws InterruptedException, IOException {
	ProcessBuilder pb = new ProcessBuilder(cmd);
	Process proc = pb.start();
	OutputStream stdin = proc.getOutputStream();
	InputStream stdout = proc.getInputStream();
	InputStream stderr = proc.getErrorStream();
	ByteArrayOutputStream output = new ByteArrayOutputStream();
	LineNumberReader lines =
	    new LineNumberReader(new InputStreamReader(stderr));
	String line = null;
	final int BUFSIZ = 4096;
	byte[] buf = new byte[BUFSIZ];
	int r = 0;
	int rv = 0;

	if ( input != null)
	    stdin.write(input);
	stdin.close();
	rv = proc.waitFor();
	while ( showErr && (line = lines.readLine()) != null)
	    System.err.println(line);

	if ( rv != 0) return null;
	while ( captureOut && ( r = stdout.read(buf)) >= 0)
	    output.write(buf, 0, r);
	return output.toByteArray();
    }


    /**
     * Fail with a usage message
     */
    static public void usage() {
	fatal("EumlabToDeter [--pass deterboss-password] [adminuser ...]");
    }

    /**
     * Enacpsulate a stateful log of counted additions, even if the additions
     * are batched.
     */
    static private class LineLogger {
	/** Lines logged */
	private int line;
	/** Output stream */
	private PrintStream p;
	/**
	 * Connect an empty logger to out - default to System.err
	 * @param out the logger PrintStream
	 */
	public LineLogger(PrintStream out) {
	    line = 0;
	    p = (out != null) ? out : System.err;
	}
	/**
	 * Increment the logged events by lines.
	 * @param lines the number of events
	 */
	public void logLine(int lines) {
	    while ( lines-- > 0) {
		line++;
		if (line % 500 == 0) p.println(line);
		else if (line % 100 == 0) p.print(line);
		else if (line % 10 == 0) {
		    p.print(".");
		    if ( p instanceof Flushable )
			p.flush();
		}
	    }
	}
    }

    /**
     * Build a deter database from an exisyting database.
     * @param args are the users to add to the admin project
     */
    static public void main(String[] args) {
	Connection c =null;
	ParamOption passwd = new ParamOption("pass");
	ListOption admin = new ListOption("admin");
	ListOption proj = new ListOption("proj");
	ParamOption sw = new ParamOption("switch");
	List<String> argv = new ArrayList<String>();
	LineLogger lineLogger = new LineLogger(System.out);

	try {
	    Option.parseArgs(args,
		    new Option[] {passwd, admin, proj, sw}, argv);
	}
	catch (Option.OptionException e) {
	    usage();
	}

	try {
	    loadTrust();
	    loadID();
	    Config config = new Config();
	   
	    String pwd = null;
	    String swRegexp = sw.getValue();

	    if (passwd.getValue() == null)  {
		LoginDialog ld = new LoginDialog();
		ld.setVisible(true);
		pwd = new String(ld.getPassword());

		if (ld.isCancelled() || pwd.length() == 0) {
		    ld.dispose();
		    return;
		}
		ld.dispose();
	    }
	    else {
		pwd = passwd.getValue();
	    }

	    // Carry out the challenge
	    try {
		Identity id  = login("deterboss", new String(pwd));
		if (id != null ) {
		    identityToKeyStore(id, new File(getUserIDFilename()), 
			    "changeit".toCharArray());
		    fatal("This run created an identity file in " + 
			    getUserIDFilename() +"\n" +
			    "Now rerun to create the database");
		}
	    } catch (DeterFault df) {
		fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	    } catch (Exception e) {
		e.printStackTrace();
		fatal("unexpected exception");
	    }

	    addUserAttributes(userAttributes);
	    addProjectAttributes(projectAttributes);
	    addCircleAttributes(circleAttributes);
	    addExperimentAttributes(experimentAttributes);
	    addLibraryAttributes(libraryAttributes);

	    // Pull users out of the DB
	    c = DriverManager.getConnection(config.getEmulabDbUrl());
	    PreparedStatement p = null;

	    List<String> projs = (List<String>) proj.getValue();
	    String pset = null;

	    if (projs != null && projs.size() != 0 ) {
		StringBuilder sb = new StringBuilder("(");
		boolean first = true;

		for (String pr : projs) {
		    if ( !first ) sb.append(", ");
		    else first = false;
		    sb.append("'");
		    sb.append(pr);
		    sb.append("'");
		}
		sb.append(")");
		pset = sb.toString();
	    }

	    if (pset == null ) {
		p = c.prepareStatement(
			"SELECT u.uid, usr_name, usr_title, usr_affil, " +
			    "usr_affil_abbrev, usr_email, usr_URL, usr_addr, " +
			    "usr_addr2, usr_city, usr_state, usr_zip, " +
			    "usr_country, usr_phone, usr_pswd, cert, " +
			    "privkey " +
			"FROM users AS u LEFT JOIN user_sslcerts AS c " +
			    "ON u.uid_idx = c.uid_idx " +
			"WHERE created <= NOW() AND expires > NOW()");
	    }
	    else {
		p = c.prepareStatement(
			"SELECT distinct u.uid, usr_name, usr_title, " +
			    "usr_affil, usr_affil_abbrev, usr_email, usr_URL, "+
			    "usr_addr, usr_addr2, usr_city, usr_state, " +
			    "usr_zip, usr_country, usr_phone, usr_pswd, " +
			    "cert, privkey " +
			"FROM users AS u LEFT JOIN group_membership AS g " +
			    "ON u.uid = g.uid " +
			"LEFT JOIN user_sslcerts AS c " +
			    "ON u.uid_idx = c.uid_idx " +
			"WHERE g.gid = g.pid AND g.pid IN " + pset + " " +
			    "AND created <= NOW() AND expires > NOW()");
	    }

	    ResultSet r = p.executeQuery();

	    UsersStub uStub = new UsersStub(getServiceUrl("Users"));
	    ProjectsStub pStub = new ProjectsStub(getServiceUrl("Projects"));
	    ProjectsStub.AddUsersNoConfirm aReq = 
		new ProjectsStub.AddUsersNoConfirm();
	    ResourcesStub rStub = new ResourcesStub(
		    getServiceUrl("Resources"));
	    ResourcesStub.CreateResource cRes =
		new ResourcesStub.CreateResource();
	    Set<String> liveUsers = new HashSet<>();	// Keep track of valid
							// users
    
	    while (r.next()) {
		UsersStub.GetProfileDescription req = 
		    new UsersStub.GetProfileDescription();
		UsersStub.GetProfileDescriptionResponse resp = 
		    uStub.getProfileDescription(req);
		UsersStub.Profile up = resp.get_return();
		UsersStub.Attribute[] profile = up.getAttributes();

		for (UsersStub.Attribute a : profile ) {
		    String name = a.getName();

		    if ( name.equals("name") ) 
			a.setValue(r.getString("usr_name"));
		    else if ( name.equals("title") ) 
			a.setValue(r.getString("usr_title"));
		    else if ( name.equals("email") ) 
			a.setValue(r.getString("usr_email"));
		    else if ( name.equals("affiliation") ) 
			a.setValue(r.getString("usr_affil"));
		    else if ( name.equals("affiliation_abbrev") ) 
			a.setValue(r.getString("usr_affil_abbrev"));
		    else if ( name.equals("URL") ) 
			a.setValue(r.getString("usr_URL"));
		    else if ( name.equals("address1") ) 
			a.setValue(r.getString("usr_addr"));
		    else if ( name.equals("address2") ) 
			a.setValue(r.getString("usr_addr2"));
		    else if ( name.equals("city") ) 
			a.setValue(r.getString("usr_city"));
		    else if ( name.equals("state") ) 
			a.setValue(r.getString("usr_state"));
		    else if ( name.equals("zip") ) 
			a.setValue(r.getString("usr_zip"));
		    else if ( name.equals("country") ) 
			a.setValue(r.getString("usr_country"));
		    else if ( name.equals("phone") ) 
			a.setValue(r.getString("usr_phone"));
		    else
			System.err.println("Huh?: " + name);
		}
		for (UsersStub.Attribute a : profile ) {
		    if (a.getOptional()) continue;
		    if (a.getValue() == null ) 
			a.setValue("123");
		}
		UsersStub.CreateUserNoConfirm cReq = 
		    new UsersStub.CreateUserNoConfirm();
		String uid = r.getString("u.uid");
		Blob certBlob = r.getBlob("cert");
		Blob keyBlob = r.getBlob("privkey");
		byte[] cert = certBlob.getBytes(1, (int) certBlob.length());;
		byte[] key = keyBlob.getBytes(1, (int) keyBlob.length());

		cReq.setUid(uid);
		cReq.setProfile(profile);
		cReq.setHash(r.getString("usr_pswd"));
		cReq.setHashtype("crypt");

		UsersStub.CreateUserNoConfirmResponse cResp = 
		    uStub.createUserNoConfirm(cReq);

		System.out.println(cResp.get_return());
		liveUsers.add(uid);

		// Construct and store a jks keystore

		KeyStore.PasswordProtection passPro =
		    new KeyStore.PasswordProtection("changeit".toCharArray());
		KeyStore pkcs12ks = KeyStore.getInstance("PKCS12");
		KeyStore jks = KeyStore.getInstance(KeyStore.getDefaultType());
		ByteArrayOutputStream pemOutStream =
		    new ByteArrayOutputStream();
		ByteArrayOutputStream jksOutStream =
		    new ByteArrayOutputStream();

		pemOutStream.write(
			"-----BEGIN RSA PRIVATE KEY-----\n".getBytes());
		pemOutStream.write(key);
		pemOutStream.write(
			"-----END RSA PRIVATE KEY-----\n".getBytes());
		pemOutStream.write("-----BEGIN CERTIFICATE-----\n".getBytes());
		pemOutStream.write(cert);
		pemOutStream.write("-----END CERTIFICATE-----\n".getBytes());
		pemOutStream.close();

		byte[] pkcs12Bytes = runProcess(new String[] {
		    "/usr/bin/openssl", "pkcs12", "-export",
			"-passout", "pass:changeit", },
			true, true, pemOutStream.toByteArray());

		ByteArrayInputStream pkcs12InStream =
		    new ByteArrayInputStream(pkcs12Bytes);

		pkcs12ks.load(pkcs12InStream, passPro.getPassword());
		jks.load(null, passPro.getPassword());
		Enumeration<String> al = pkcs12ks.aliases();
		while (al.hasMoreElements()) {
		    String a = al.nextElement();

		    jks.setEntry(a, pkcs12ks.getEntry(a, passPro), passPro);
		}
		jks.store(jksOutStream, passPro.getPassword());

		cRes.setName(uid + ":keystore");
		cRes.setType("keystore");
		cRes.setPersist(true);
		cRes.setData(putBytes(jksOutStream.toByteArray()));
		ResourcesStub.CreateResourceResponse rResp =
		    rStub.createResource(cRes);
	    }
	    cRes.setName("system:datastore");
	    cRes.setType("TestbedData");
	    cRes.setPersist(true);
	    ResourcesStub.CreateResourceResponse rResp =
		rStub.createResource(cRes);

	    cRes.setName("system:unknownResource");
	    cRes.setType("testnode");
	    cRes.setPersist(true);
	    rResp = rStub.createResource(cRes);

	    if ( pset == null )
		p = c.prepareStatement(
			"SELECT pid, head_uid, funders, why, url FROM projects"
			);
	    else
		p = c.prepareStatement(
			"SELECT pid, head_uid, funders, why, url " +
			"FROM projects WHERE pid IN " + pset);
	    r = p.executeQuery();
	    while (r.next()) {
		ProjectsStub.GetProfileDescription req = 
		    new ProjectsStub.GetProfileDescription();
		ProjectsStub.GetProfileDescriptionResponse resp = 
		    pStub.getProfileDescription(req);
		ProjectsStub.Profile up = resp.get_return();
		ProjectsStub.Attribute[] profile = up.getAttributes();

		for (ProjectsStub.Attribute a : profile ) {
		    String name = a.getName();
		    if ( name.equals("funders")) 
			a.setValue(r.getString("funders"));
		    else if ( name.equals("description")) 
			a.setValue(r.getString("why"));
		    else if ( name.equals("URL")) 
			a.setValue(r.getString("URL"));
		}
		for (ProjectsStub.Attribute a : profile ) {
		    if (a.getOptional()) continue;
		    if (a.getValue() == null ) 
			a.setValue("A project");
		}
		ProjectsStub.CreateProject cReq = 
		    new ProjectsStub.CreateProject();
		// Make sure that the owner is a valid user.  In not, use
		// deterboss.  This came up as elabman's cert is expired.
		String owner = r.getString("head_uid");

		if ( !liveUsers.contains(owner)) owner = "deterboss";
		cReq.setProjectid(r.getString("pid"));
		cReq.setOwner(owner);
		cReq.setProfile(profile);

		ProjectsStub.CreateProjectResponse cResp = 
		    pStub.createProject(cReq);

		ProjectsStub.ApproveProject appReq = 
		    new ProjectsStub.ApproveProject();
		appReq.setProjectid(r.getString("pid"));
		appReq.setApproved(true);
		pStub.approveProject(appReq);
	    }


	    if ( pset == null )
		p = c.prepareStatement(
			"SELECT uid, gid, pid FROM group_membership");
	    else
		p = c.prepareStatement(
			"SELECT uid, gid, pid FROM group_membership " +
			    "WHERE gid = pid AND pid IN " + pset);

	    r = p.executeQuery();
	    Map<String, List<String> > projects = 
		new HashMap<String, List<String> >();
	    while (r.next()) {
		String uid = r.getString("uid");
		String gid = r.getString("gid");
		String pid = r.getString("pid");

		if (!gid.equals(pid)) {
		    System.err.println("Not ready for subgroups");
		    continue;
		}
		if (!projects.containsKey(pid))
		    projects.put(pid, new ArrayList<String>());
		projects.get(pid).add(uid);
	    }
	    for (String k: projects.keySet()) {
		// Convert any non-existent members to deterboss
		String[] members = projects.get(k).toArray(new String[0]);

		for ( int i = 0; i < members.length; i++)
		    if (!liveUsers.contains(members[i]))
			members[i] = "deterboss";

		aReq.setProjectid(k);
		aReq.setUids(members);
		aReq.setPerms(new String[] { "ALL_PERMS" });

		ProjectsStub.AddUsersNoConfirmResponse aResp = 
		    pStub.addUsersNoConfirm(aReq);

		for (ProjectsStub.ChangeResult rr : aResp.get_return())
		    if ( rr.getSuccess()) 
			System.out.println(rr.getName() + " added");
		    else
			System.out.println(rr.getName() + " failed: " + 
				rr.getReason());
	    }

	    List<String> admins = (List<String>) admin.getValue();

	    if (admins == null || admins.size() == 0 ) System.exit(0);

	    aReq.setProjectid("admin");
	    aReq.setUids(admins.toArray(new String[0]));
	    aReq.setPerms(new String[] { "ALL_PERMS" });

	    ProjectsStub.AddUsersNoConfirmResponse aResp = 
		pStub.addUsersNoConfirm(aReq);

	    for (ProjectsStub.ChangeResult rr : aResp.get_return())
		if ( rr.getSuccess()) 
		    System.out.println(rr.getName() + " added");
		else
		    System.out.println(rr.getName() + " failed: " + 
			    rr.getReason());

	    if ( swRegexp != null ) {
		p = c.prepareStatement(
			"SELECT node_id1, n1.role,n1.type, card1, port1, " +
			    "node_id2, n2.role, n2.type, card2, port2 "+
			"FROM wires LEFT JOIN nodes AS n1 " +
				"ON n1.node_id=node_id1 " +
			    "LEFT JOIN nodes AS n2 ON n2.node_id=node_id2 " +
			"WHERE n1.role = 'testnode' " +
			    "AND node_id2 REGEXP ?");
		p.setString(1, swRegexp);
	    }
	    else {
		p = c.prepareStatement(
			"SELECT node_id1, n1.role, n1.type, card1, port1, " +
			    "node_id2, n2.role, n2.type, card2, port2 "+
			"FROM wires LEFT JOIN nodes AS n1 " +
				"ON n1.node_id=node_id1 " +
			    "LEFT JOIN nodes AS n2 ON n2.node_id=node_id2 " +
			"WHERE n1.role REGEXP 'testnode'");
	    }

	    r = p.executeQuery();
	    System.out.println();
	    System.out.println("Installing resources");
	    Set<String> nodesAdded = new HashSet<>();
	    while (r.next()) {
		ResourcesStub.ResourceTag[] tags =
		    new ResourcesStub.ResourceTag[6];
		String nodeName = "system:" + r.getString("node_id1");
		String nodeIntName = nodeName+"-"+ r.getInt("card1") + "-" +
		    r.getInt("port1");
		String switchName = "system:" + r.getString("node_id2");
		String switchIntName = switchName +"-"+ r.getInt("card2") +
		    "-" + r.getInt("port2");
		int rows = 0;

		if (!nodesAdded.contains(nodeName)) {
		    cRes.setName(nodeName);
		    cRes.setType(r.getString("n1.role"));
		    cRes.setPersist(true);
		    rStub.createResource(cRes);
		    rows++;
		    nodesAdded.add(nodeName);
		}

		if (!nodesAdded.contains(switchName)) {
		    cRes.setName(switchName);
		    cRes.setType(r.getString("n2.role"));
		    cRes.setPersist(true);
		    rStub.createResource(cRes);
		    rows++;
		    nodesAdded.add(switchName);
		}

		cRes.setName(nodeIntName);
		cRes.setType("interface");
		cRes.setPersist(true);
		for (int i = 0; i < tags.length; i ++)
		    tags[i] = new ResourcesStub.ResourceTag();
		tags[0].setName("card");
		tags[0].setValue(r.getString("card1"));
		tags[1].setName("port");
		tags[1].setValue(r.getString("port1"));
		tags[2].setName("peer");
		tags[2].setValue(switchIntName);
		tags[3].setName("host");
		tags[3].setValue(nodeIntName);
		tags[4].setName("peername");
		tags[4].setValue(switchName);
		tags[5].setName("hostname");
		tags[5].setValue(nodeName);
		cRes.setTags(tags);
		rStub.createResource(cRes);
		cRes.setName(switchIntName);
		cRes.setType("interface");
		cRes.setPersist(true);
		tags[0].setName("card");
		tags[0].setValue(r.getString("card2"));
		tags[1].setName("port");
		tags[1].setValue(r.getString("port2"));
		tags[2].setName("peer");
		tags[2].setValue(nodeIntName);
		tags[3].setName("host");
		tags[3].setValue(switchIntName);
		tags[4].setName("hostname");
		tags[4].setValue(switchName);
		tags[5].setName("peername");
		tags[5].setValue(nodeName);
		cRes.setTags(tags);
		rStub.createResource(cRes);
		rows+=2;
		lineLogger.logLine(rows);
	    }
	    System.out.println();
	}
	catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} 
	catch (ExperimentsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (LibrariesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (ProjectsDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} 
	catch (ResourcesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} 
	catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	}
	catch (DeterFault df) {
	    fatal("DeterFault: " + df.getDetailMessage());
	} 
	catch (SQLException e) {
	    fatal("SQLException: "+ e.getMessage());
	}
	catch (RemoteException e) {
	    fatal("RemoteException: " + e.getMessage());
	}
	catch (IOException e) {
	    fatal("IOExecption: " + e.getMessage());
	}
	catch (InterruptedException e) {
	    fatal("InterruptedException: " + e.getMessage());
	}
	catch (GeneralSecurityException e) {
	    fatal("GeneralSecurityException: " + e);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    throw e;
	}
	finally {
	    try {
		if ( c != null ) c.close();
	    } catch (SQLException ignored) { }
	}
    }
}
