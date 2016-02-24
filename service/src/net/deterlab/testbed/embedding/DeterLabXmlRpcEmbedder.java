package net.deterlab.testbed.embedding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.api.RealizationDescription;
import net.deterlab.testbed.api.ResourceFacet;
import net.deterlab.testbed.api.ResourceTag;
import net.deterlab.testbed.circle.CircleDB;
import net.deterlab.testbed.db.SharedConnection;
import net.deterlab.testbed.experiment.ExperimentDB;
import net.deterlab.testbed.realization.RealizationDB;
import net.deterlab.testbed.resource.ResourceDB;
import net.deterlab.testbed.topology.TopologyDescription;
import net.deterlab.testbed.user.UserDB;

/**
 * The interface for classes that embed topologies into testbeds.
 * @author the DETER Team
 * @version 1.1
 */
public class DeterLabXmlRpcEmbedder implements Embedder {

    /** The JAVA_HOME variable */
    private String javaHome;
    /** Path to the java executable */
    private String javaPath;
    /** Path to the service aar/jar file */
    private String jarPath;
    /** Separator for constructing pathnames */
    private String fileSeparator;
    /** The base package name for these classes */
    private String classBase;
    /** The trusted keystore contents*/
    private byte[] trustedContents;

    /** User keystore default name */
    private static final String userKsName = "user.jks";
    /** Trusted keystore default name */
    private static final String trustedKsName = "trusted.jks";
    /** Standard password */
    private static final String defaultKeystorePassword = "changeit";
    /** Standard topology filename */
    private static final String topdlName = "experiment.xml";

    /**
     * Encapsulation of the environment of a child process
     * @author the DETER Team
     * @version 1.1
     */
    private static class ProcessEnvironment {
	/** Contents of the users key store */
	private byte[] userKs;
	/** Contents of a keystore used to validate the server */
	private byte[] trustKs;
	/** The temporary directory containing parameters and certificates */
	private File tmpDir;
	/** The topology to create */
	private TopologyDescription top;

	/**
	 * Initialize an empty environment
	 */
	public ProcessEnvironment() {
	    userKs = null;
	    trustKs = null;
	    tmpDir = null;
	    top = null;
	}

	/**
	 * Initialize a process environment with substantive elements.
	 * @param uks user keystore contents
	 * @param tks trusted keystore contents
	 * @param dir the working directory in which to start the process
	 * @param td the topology to instantiate
	 */
	public ProcessEnvironment(byte[] uks, byte[] tks, File dir,
		TopologyDescription td) {
	    this();
	    if (uks != null) userKs = uks;
	    if (tks != null) trustKs = tks;
	    if (dir != null) tmpDir = dir;
	    if (td != null) top = td;
	}
	/**
	 * Return the contents of the user's key store
	 * @return the contents of the user's key store
	 */
	public byte[] getUserKeyStore() { return userKs; }
	/**
	 * Set the contents of the user's key store
	 * @param uks the new contents of the user's key store
	 */
	public void setUserKeyStore(byte[] uks) { userKs = uks; }
	/**
	 * Return the contents of the trusted key store
	 * @return the contents of the trusted key store
	 */
	public byte[] getTrustKeyStore() { return trustKs; }
	/**
	 * Set the contents of the trusted key store
	 * @param uks the new contents of the trusted key store
	 */
	public void setTrustKeyStore(byte[] tks) { trustKs = tks; }
	/**
	 * Return the working directory in which to start the process
	 * @return the working directory in which to start the process
	 */
	public File getTempDir() { return tmpDir; }
	/**
	 * Set the working directory in which to start the process
	 * @param d the new working directory in which to start the process
	 */
	public void setTempDir(File d) { tmpDir = d; }
	/**
	 * Return the topology to create
	 * @return the topology to create
	 */
	public TopologyDescription getTopology() { return top; }
	/**
	 * Set the topology to create
	 * @param td the new topology to create
	 */
	public void setTopology(TopologyDescription td) { top = td; }
    }


    /**
     * Encapsulates the routines used to manipulate the testbed.
     * @author the DETER Team
     * @version 1.1
     */
    public static class TestbedManipulator {

	/**
	 * Exception thrown if an unexpected value is encountered converting an
	 * XmlRpc response into a class.
	 * @author the DETER Team
	 * @version 1.1
	 */
	protected static class BadConversionException extends Exception {
	    /**
	     * Create a BadConversionException with a given message.
	     * @param m the message
	     */
	    public BadConversionException(String m) {
		super(m);
	    }
	}

	/**
	 * Base class for an XmlRpcResponse.  This is a read only data
	 * structure.
	 * @author the DETER Team
	 * @version 1.1
	 */
	protected static class XmlRpcResponse {
	    /** The emulab error code */
	    private int code;
	    /** Error log */
	    private String output;
	    /** An undiferentiatied retrun value.  Subclasses convert this into
	     * known forms. */
	    private Object uValue;

	    /**
	     * Initialize a response.
	     * @param c the emulab error code
	     * @param out the error log
	     * @param v an object that subclasses will convert into the real
	     * return values.
	     * @throws BadConversionException if a parameter has an unexpected
	     * form
	     */
	    public XmlRpcResponse(Object c, Object out, Object v)
		throws BadConversionException {
		try {
		    Number cc = (Number) c;

		    code = cc.intValue();
		    output = (String) out;
		    uValue = v;
		}
		catch (ClassCastException e) {
		    throw new BadConversionException(e.getMessage());
		}
	    }

	    /**
	     * Return the Emulab error code.
	     * @return the Emulab error code
	     */
	    public int getCode() { return code; }
	    /**
	     * Return the error log
	     * @return the error log
	     */
	    public String getOutput() { return output; }
	    /**
	     * Return the return value as an undifferentiated object.
	     * Subclasses use this to access and convert the code.
	     * @return the return value as an undifferentiated object.
	     */
	    protected Object getUndifferentiatedValue() { return uValue; }
	}

	/**
	 * Encapsulate return values that contain an integer.
	 * @author the DETER Team
	 * @version 1.1
	 */
	protected static class XmlRpcIntValueResponse extends XmlRpcResponse {
	    /** The return value */
	    private int value;

	    /**
	     * Initialize a response.
	     * @param c the emulab error code
	     * @param out the error log
	     * @param v an object that will become an integer.
	     * @throws BadConversionException if a parameter has an unexpected
	     * form
	     */
	    public XmlRpcIntValueResponse(Object c, Object out, Object v)
		throws BadConversionException {
		super(c, out, v);
		try {
		    Number ival = (Number) getUndifferentiatedValue();
		    value = ival.intValue();
		}
		catch (ClassCastException e) {
		    throw new BadConversionException(e.getMessage());
		}
	    }

	    /**
	     * Convert an undifferentiated result into one of these.
	     * @param r the generic response
	     * @throws BadConversionException if a parameter has an unexpected
	     * form
	     */
	    public XmlRpcIntValueResponse(XmlRpcResponse r)
		throws BadConversionException {
		this(new Integer(r.getCode()), r.getOutput(),
			r.getUndifferentiatedValue());
	    }
	    /**
	     * Return the integer value.
	     * @return the integer value.
	     */
	    public int getValue() { return value; }
	}

	/**
	 * Encapsulate the response to an info request for node or container
	 * mapping
	 * @author the DETER Team
	 * @version 1.1
	 */
	protected static class XmlRpcInfoResponse extends XmlRpcResponse {
	    /** The content a map of parameters indexed by node name */
	    private Map<String, Map<String, Object>> value;

	    /**
	     * Initialize a response.
	     * @param c the emulab error code
	     * @param out the error log
	     * @param v an object that will become the two-level map
	     * @throws BadConversionException if a parameter has an unexpected
	     * form
	     */
	    public XmlRpcInfoResponse(Object c, Object out,
		    Object v) throws BadConversionException {
		super(c, out, v);
		try {
		    value = (Map) getUndifferentiatedValue();
		}
		catch (ClassCastException e) {
		    throw new BadConversionException(e.getMessage());
		}
	    }

	    /**
	     * Convert an undifferentiated result into one of these.
	     * @param r the generic response
	     * @throws BadConversionException if a parameter has an unexpected
	     * form
	     */
	    public XmlRpcInfoResponse(XmlRpcResponse r)
		throws BadConversionException {
		this(new Integer(r.getCode()), r.getOutput(),
			r.getUndifferentiatedValue());
	    }
	    /**
	     * Return the two-level map with node assignment info.
	     * @return the two-level map with node assignment info.
	     */
	    public Map<String, Map<String, Object>> getValue() { return value; }
	}

	/**
	 * Make an Emulab call to the server at url on the given method.
	 * Parameters are passed as a map of string keys to objects.
	 * @param url the server to contact
	 * @param method the method to invoke
	 * @param params the parameters to pass
	 * @return a generic XmlRpcResponse object
	 */
	protected static XmlRpcResponse xmlRpcCall(String url, String method,
		Map<String, Object> params)
	    throws XmlRpcException, IOException, BadConversionException  {
	    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	    config.setServerURL(new URL(url));
	    XmlRpcClient client = new XmlRpcClient();
	    client.setConfig(config);
	    Map<String, Object> resultMap = null;
	    Integer codeObject = null;
	    String out = null;

	    Object res =  client.execute(method, new Object[] {
		new Double(0.1), params });

	    resultMap = (Map) res;
	    return new XmlRpcResponse(resultMap.get("code"),
			resultMap.get("output"), resultMap.get("value"));
	}

	/**
	 * Store the output in an error log resource.
	 * @param eid the experiment that is having the log saved
	 * @param rdb the ResourceDB object that had a failure
	 * @param resp the response containing the error log
	 * @throws DeterFault on errors constructing the new resource
	 */
	protected static void storeErrorLog(String eid, RealizationDB rdb,
		XmlRpcResponse resp, SharedConnection sc) throws DeterFault {
	    ResourceDB res = null;
	    String rName = eid + ":errorLog";
	    String log = resp.getOutput();
	    res = new ResourceDB(rName, sc);
	    res.setPersist(false);
	    res.setType("errorLog");
	    res.setData(log.getBytes());
	    res.create(new ResourceFacet[0],
		    new ArrayList<AccessMember>(),
		    new ResourceTag[0]);
	    rdb.addContainmentEntry("system:datastore", rName);
	    rdb.setStatus("Failed");
	    rdb.save();
	}
    }

    /**
     * Encapsulates the process run to interface to DeterLab and create the new
     * experiment.
     * @author the DETER Team
     * @version 1.1
     */
    public static class Realizer extends TestbedManipulator {
	/**
	 * The realizer main line.
	 * @param args the command line arguments.  In order, the name of a
	 * trusted keystore, the user's keystore, the server url, the user
	 * identifier, the experiment id to create, the project to create it
	 * in, the file name for the topology description, the realization name.
	 */
	public static void main(String[] args) {
	    String trustedName = (args.length > 0 ) ? args[0] : null;
	    String userName = (args.length > 1 ) ? args[1] : null;
	    String url = (args.length > 2) ? args[2] : null;
	    String uid = (args.length > 3) ? args[3] : null;
	    String eid = (args.length > 4) ? args[4] : null;
	    String proj = (args.length > 5) ? args[5] : null;
	    String topdlName = (args.length > 6) ? args[6] : null;
	    String realizationName = (args.length > 7) ? args[7] : null;
	    int exitCode = 0;

	    if ( args.length != 8) {
		System.err.println("Wrong number of parameters");
		System.exit(20);
	    }

	    eid = eid.replaceAll("[^A-Za-z0-9\\-]", "-");
	    proj = proj.replaceFirst("[^:]*:", "");

	    System.setProperty("javax.net.ssl.trustStore", trustedName);
	    System.setProperty("javax.net.ssl.trustStorePassword",
		    defaultKeystorePassword);

	    System.setProperty("javax.net.ssl.keyStore", userName);
	    System.setProperty("javax.net.ssl.keyStorePassword",
		    defaultKeystorePassword);

	    List<RealizationDB> rList = new ArrayList<>();
	    RealizationDB rdb = null;
	    SharedConnection sc = null;
	    Map<String, ResourceDB> nodes = new HashMap<>();

	    try {
		FileReader fileReader = new FileReader(topdlName);
		TopologyDescription top =
		    TopologyDescription.xmlToTopology(
			    new FileReader(topdlName), "experiment", false);
		StringWriter stringWriter = new StringWriter();
		XmlRpcResponse resp = null;
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> resultMap = null;
		XmlRpcInfoResponse vr = null;
		Map<String, Map<String, Object>> m = null;
		final int BUFSIZ = 4096;
		char[] buf = new char[BUFSIZ];
		int r;

		sc = new SharedConnection();
		sc.open();

		rList = RealizationDB.getRealizations(uid,
			"^" + realizationName+"$", -1, -1, null);

		if ( rList.size() != 1 )
		    throw new DeterFault(DeterFault.internal,
			    "More than one realization??");
		rdb = rList.remove(0);

		while ( ( r = fileReader.read(buf)) >=0 )
		    stringWriter.write(buf, 0, r);

		String virtualMachinesStr = top.getAttribute("virtualMachines");
		String swapInStr = top.getAttribute("allocateResources");
		boolean containers = (virtualMachinesStr != null) ?
		    !virtualMachinesStr.equals("false") : true;
		boolean swapIn = (swapInStr != null) ?
		    !swapInStr.equals("false") : true;

		params.put("exp", eid);
		params.put("proj", proj);
		// NB that the containerize branch does not convert the topdl
		// file and it should be passed in as an nsfile.  FIXME: unify
		// that.
		if (containers)
		    params.put("nsfilestr", stringWriter.toString());
		else {
		    params.put("topdlfilestr", stringWriter.toString());
		    params.put("batch", false);
		    params.put("wait", true);
		}

		params.put("containerize", containers);
		params.put("noswapin",true);
		resp = xmlRpcCall(url, "experiment.startexp", params);
		if (resp.getCode() != 0 ) {
		    storeErrorLog(eid, rdb, resp, sc);
		    exitCode = 20;
		    return;
		}

		params.clear();
		if ( containers ) {
		    params.put("exp", eid);
		    params.put("proj", proj);
		    params.put("aspect", "cmapping");
		    resp = xmlRpcCall(url, "experiment.info", params);
		    vr = new XmlRpcInfoResponse(resp);

		    m = vr.getValue();
		    for (Map.Entry<String, Map<String, Object>> e :
			    m.entrySet()) {
			ResourceDB res = null;
			String rName = eid + ":" + e.getKey();
			String vhost = eid + ":" +
			    (String) e.getValue().get("vnode");

			if ( !nodes.containsKey(vhost)) {
			    res = new ResourceDB(vhost, sc);
			    res.setPersist(false);
			    res.setType("container_host");
			    res.create(new ResourceFacet[0],
				    new ArrayList<AccessMember>(),
				    new ResourceTag[0]);
			    nodes.put(vhost, res);
			}
			res = new ResourceDB(rName, sc);
			res.setPersist(false);
			res.setType((String) e.getValue().get("node_type"));
			res.create(new ResourceFacet[0],
				new ArrayList<AccessMember>(),
				new ResourceTag[0]);
			nodes.put(e.getKey(), res);
			rdb.addMappingEntry(e.getKey(), rName);
			rdb.addContainmentEntry(vhost, rName);
		    }
		    params.clear();
		    params.put("exp", eid);
		    params.put("proj", proj);
		    params.put("aspect", "cthumbnail");
		    resp = xmlRpcCall(url, "experiment.info", params);
		    vr = new XmlRpcInfoResponse(resp);

		    m = vr.getValue();

		    if ( m.containsKey("thumbnail")) {
			ResourceDB res = null;
			String rName = eid + ":thumbnail";
			byte[] image = (byte[]) m.get("thumbnail").get("png");
			res = new ResourceDB(rName, sc);
			res.setPersist(false);
			res.setType("png");
			res.setData(image);
			res.create(new ResourceFacet[0],
				new ArrayList<AccessMember>(),
				new ResourceTag[0]);
			rdb.addContainmentEntry("system:datastore", rName);
		    }
		    rdb.save();
		}


		if ( !swapIn ) return;
		rdb.setStatus("Initializing");
		rdb.save();

		params.clear();
		params.put("exp", eid);
		params.put("proj", proj);
		params.put("direction", "in");
		params.put("wait", true);
		resp = xmlRpcCall(url, "experiment.swapexp", params);
		if (resp.getCode() != 0 ) {
		    storeErrorLog(eid, rdb, resp, sc);
		    exitCode = 20;
		    return;
		}

		params.clear();
		params.put("exp", eid);
		params.put("proj", proj);
		params.put("aspect", "mapping");
		resp = xmlRpcCall(url, "experiment.info", params);
		vr = new XmlRpcInfoResponse(resp);

		m = vr.getValue();
		for ( Map.Entry<String, Map<String, Object>> e : m.entrySet()) {
		    String hostName = eid + ":" + e.getKey();
		    String rName = "system:" +
			(String) e.getValue().get("node");

		    ResourceDB res = new ResourceDB(rName, sc);

		    if (!res.isValid())
			rName = "system:unknownResource";

		    res.close();
		    res = new ResourceDB(hostName, sc);

		    if (!res.isValid()) {
			res.setPersist(false);
			res.setType("embedded_pnode");
			res.create(new ResourceFacet[0],
				new ArrayList<AccessMember>(),
				new ResourceTag[0]);
		    }
		    res.close();

		    rdb.addContainmentEntry(rName, hostName);
		}
		rdb.setStatus("Active");
		rdb.save();
	    }
	    catch (BadConversionException be) {
		System.err.println("Conversion");
		be.printStackTrace();
		exitCode = 20;
	    }
	    catch (XmlRpcException re) {
		System.err.println("RPC error: " + re.getMessage());
		exitCode = 20;
	    }
	    catch (IOException ie) {
		System.err.println("RPC error: " + ie.getMessage());
		exitCode = 20;
	    }
	    catch (DeterFault df) {
		df.printStackTrace();
		System.err.println("DeterFault: " + df);
		exitCode = 20;
	    }
	    catch (Throwable e) {
		System.err.println("Throwable!?!?");
		e.printStackTrace();
		exitCode = 50;
	    }
	    finally {
		for (RealizationDB r : rList) {
		    try {
			if (r != null ) r.close();
		    }
		    catch (DeterFault ignored) { }
		}
		try {
		    if (rdb != null ) rdb.close();
		}
		catch (DeterFault ignored) { }
		for (ResourceDB r : nodes.values()) {
		    try {
			if ( r != null ) r.close();
		    }
		    catch (DeterFault ignored) { }
		}
		try {
		    if ( sc != null ) sc.close();
		}
		catch (DeterFault ignored) { }
		System.exit(exitCode);
	    }
	}
    }

    /**
     * Append the files in dir to the path in base, separated by pSep.
     * @param base the base path
     * @param sharedDir the directory ro add
     * @param pSep path separator
     * @return the extended path
     * @throws IOException on errors in the file system.
     */
    protected String appendJars(String base, File sharedDir, String pSep)
	throws IOException {
	if ( sharedDir == null || pSep == null) return base;
	File[] jars = sharedDir.listFiles();

	if ( jars == null ) return base;

	for ( File f : jars )
	    base += pSep + f.getPath();

	return base;
    }

    /**
     * Initialize the local paths.
     */
    public DeterLabXmlRpcEmbedder() {
	super();
	final int BUFSIZ = 4096;
	Properties sysProps = System.getProperties();
	byte[] buf = new byte[BUFSIZ];

	fileSeparator = sysProps.getProperty("file.separator", "/");
	javaHome = sysProps.getProperty("java.home");
	javaPath = String.join(fileSeparator, javaHome, "bin", "java");
	jarPath = String.join(fileSeparator,
		sysProps.getProperty("catalina.home"),
		"webapps", "axis2","WEB-INF", "services", "DeterAPI.aar");
	classBase = this.getClass().getPackage().getName();

	try {
	    Config config = new Config();
	    String pSep = sysProps.getProperty("path.separator", ":");
	    String prop = config.getProperty("xmlrpcTrust");
	    File trustFile = new File((prop != null) ? prop : "");
	    FileInputStream ins = new FileInputStream(
		    (trustFile != null) ? trustFile : null);
	    ByteArrayOutputStream outb = new ByteArrayOutputStream();
	    int r = 0;

	    if ( ins == null || outb == null) return;
	    while ( (r = ins.read(buf)) >=0 )
		outb.write(buf, 0, r);
	    outb.close();
	    trustedContents = outb.toByteArray();

	    
	    if ( (prop = config.getProperty("embedderClasses")) != null ) 
		for (String p: prop.split(":") )
		    jarPath = appendJars(jarPath, new File(p), pSep);
	}
	catch (IOException ie) {
	    ie.printStackTrace();
	    trustedContents = null;
	}
	catch (DeterFault df) {
	    df.printStackTrace();
	    trustedContents = null;
	}
    }

    /**
     * Save a configuration file - primarily a key store.
     * @param data the contents
     * @param name the File in which to store it.
     */
    private void saveConfigFile(byte[] data, File name) {
	try {
	    FileOutputStream out = new FileOutputStream(name);

	    out.write(data);
	    out.close();
	}
	catch (IOException ie) {
	    ie.printStackTrace();
	}
    }

    /**
     * Establish the environment for a subprocess to safely run in a given
     * directory.
     * @param pb the builder to modify
     * @param pe the environment to build
     */
    private void establishProcessEnvironment(ProcessBuilder pb,
	    ProcessEnvironment pe) throws IOException {
	Map<String, String> penv = pb.environment();

	if (pe.getTempDir() == null ) {
	    Path tp = Files.createTempDirectory("realiz");

	    pe.setTempDir((tp != null) ? tp.toFile() : null);
	}

	penv.put("JAVA_HOME", javaHome);
	if ( pe.getTempDir() != null) {
	    byte[] ks = pe.getUserKeyStore();
	    TopologyDescription td = pe.getTopology();

	    if ( ks != null )
		saveConfigFile(ks, new File(pe.getTempDir(), userKsName));
	    if ( (ks = pe.getTrustKeyStore()) != null)
		saveConfigFile(ks, new File(pe.getTempDir(), trustedKsName));
	    if ( td != null ) {
		FileWriter topdlFile =
		    new FileWriter(new File(pe.getTempDir(), topdlName));
		td.writeXML(topdlFile, "experiment");
		topdlFile.close();
	    }
	}
	pb.directory(pe.getTempDir());
    }

    /**
     * Put the state of the builder to the given PrintStream.
     * @param pb the builder to dump
     * @param p the stream to print to (System.err if p is null)
     */
    private void dumpBuilder(ProcessBuilder pb, PrintStream p) {
	boolean first = true;
	if ( p == null)
	    p = System.err;

	p.print("Command: " );
	for ( String s : pb.command() ) {
	    if ( !first ) p.print(" ");
	    else first = false;
	    p.print(s);
	}
	p.println();
	p.println("Directory: " + pb.directory());
    }

    /**
     * Begin realizing an experiment on the testbed in the given circle.  This
     * actually starts the realization process.  Users will need to poll the
     * realization to determine when the realization is complete.
     * @param uid user id realizing the experiment
     * @param eid experiment ID to realize
     * @param cid the circle in which to realize the experiment
     * @param acl the initial access control list
     * @param td a description of the topology to create
     * @param sendNotifications if true send notifications when state changes
     * @return a description of the realization.
     * @throws DeterFault on errors
     */
    public RealizationDescription startRealization(String uid, String eid,
	    String cid, AccessMember[] acl, TopologyDescription td,
	    boolean sendNotifications) throws DeterFault {
	RealizationDB nr = null;
	RealizationDescription rd = new RealizationDescription();
	ResourceDB rdb = null;
	UserDB udb = null;
	CircleDB cdb = null;
	ExperimentDB edb = null;
	try {
	    udb = new UserDB(uid);
	    if ( !udb.isValid())
		throw new DeterFault(DeterFault.request, "Bad user name");

	    cdb = new CircleDB(cid);
	    if ( !cdb.isValid())
		throw new DeterFault(DeterFault.request, "Bad circle name");

	    edb = new ExperimentDB(eid);
	    if ( !edb.isValid())
		throw new DeterFault(DeterFault.request, "Bad Experiment name");


	    Config config = new Config();

	    String tbUrl = config.getProperty("testbedUrl");
	    nr = new RealizationDB();
	    rd = new RealizationDescription();
	    ExperimentAspect layoutAspect = new ExperimentAspect();
	    rdb = new ResourceDB(uid + ":keystore", nr.getSharedConnection());

	    rdb.load();

	    nr.create(eid, cid, td, Arrays.asList(acl), uid);
	    nr.setEmbedderName(getClass().getName());
	    nr.save();

	    String[] cmd =  new String[] {
		javaPath, "-cp", jarPath,
		classBase + ".DeterLabXmlRpcEmbedder$Realizer",
		String.join(fileSeparator, ".", trustedKsName),
		String.join(fileSeparator, ".", userKsName),
		tbUrl, uid, eid, cid,
		String.join(fileSeparator, ".", topdlName),
		nr.getName()
	    };
	    ProcessBuilder pb = new ProcessBuilder(cmd);
	    Process p = null;

	    rd.setName(nr.getName());
	    rd.setExperiment(nr.getExperimentID());
	    rd.setCircle(nr.getCircleID());
	    rd.setStatus(nr.getStatus());
	    rd.setACL(acl);

	    layoutAspect.setType("layout");
	    layoutAspect.setSubType("full_layout");

	    establishProcessEnvironment(pb,
		    new ProcessEnvironment(rdb.getData(),
			trustedContents, null, td));

	    pb.inheritIO();
	    p = pb.start();
	    return rd;
	}
	catch (DeterFault df) {
	    throw df;
	}
	catch (IOException ie) {
	    ie.printStackTrace();
	    throw new DeterFault(DeterFault.internal, ie.getMessage());
	}
	finally {
	    if ( udb != null) udb.close();
	    if ( edb != null) edb.close();
	    if ( cdb != null) cdb.close();
	    if ( rdb != null) rdb.close();
	    if ( nr != null ) nr.close();
	}
    }


    /**
     * Encapsulates the process run to interface to DeterLab and remove the
     * experiment.
     * @author the DETER Team
     * @version 1.1
     */
    public static class UnRealizer extends TestbedManipulator {
	/**
	 * The realizer main line.
	 * @param args the command line arguments.  In order, the name of a
	 * trusted keystore, the user's keystore, the server url, the user
	 * identifier, the experiment id to create, the project to create it
	 * in, the file name for the topology description, the realization name.
	 */
	public static void main(String[] args) {
	    String trustedName = (args.length > 0 ) ? args[0] : null;
	    String userName = (args.length > 1 ) ? args[1] : null;
	    String url = (args.length > 2) ? args[2] : null;
	    String uid = (args.length > 3) ? args[3] : null;
	    String realizationName = (args.length > 4) ? args[4] : null;
	    int exitCode = 0;

	    if ( args.length != 5) {
		System.err.println("Wrong number of parameters");
		System.exit(20);
	    }

	    System.setProperty("javax.net.ssl.trustStore", trustedName);
	    System.setProperty("javax.net.ssl.trustStorePassword",
		    defaultKeystorePassword);

	    System.setProperty("javax.net.ssl.keyStore", userName);
	    System.setProperty("javax.net.ssl.keyStorePassword",
		    defaultKeystorePassword);

	    List<RealizationDB> rList = new ArrayList<>();
	    RealizationDB rdb = null;
	    SharedConnection sc = null;
	    String proj = null;
	    String eid = null;
	    Map<String, ResourceDB> nodes = new HashMap<>();

	    try {
		XmlRpcResponse resp = null;
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> resultMap = null;
		XmlRpcInfoResponse vr = null;
		Map<String, Map<String, Object>> m = null;

		sc = new SharedConnection();
		sc.open();

		rList = RealizationDB.getRealizations(uid,
			"^" + realizationName+"$", -1, -1, null);

		if ( rList.size() == 0 )
		    throw new DeterFault(DeterFault.internal,
			    "No realization");
		else if ( rList.size() > 1 )
		    throw new DeterFault(DeterFault.internal,
			    "More than one realization??");
		rdb = rList.remove(0);

		eid = rdb.getExperimentID();
		proj = rdb.getCircleID();

		if ( eid == null || proj == null)
		    throw new DeterFault(DeterFault.internal,
			    "Corrupt realization: missing eid or project");

		eid = eid.replaceAll("[^A-Za-z0-9\\-]", "-");
		proj = proj.replaceFirst("[^:]*:", "");

		params.put("exp", eid);
		params.put("proj", proj);
		params.put("wait", true);
		resp = xmlRpcCall(url, "experiment.endexp", params);
		if (resp.getCode() != 0 ) {
		    storeErrorLog(eid, rdb, resp, sc);
		    // Fall through to remove the database entries.
		}
		rdb.setStatus("Empty");
		rdb.save();
		rdb.remove();
	    }
	    catch (BadConversionException be) {
		System.err.println("Conversion");
		be.printStackTrace();
		exitCode = 20;
	    }
	    catch (XmlRpcException re) {
		System.err.println("RPC error: " + re.getMessage());
		exitCode = 20;
	    }
	    catch (DeterFault df) {
		df.printStackTrace();
		System.err.println("DeterFault: " + df);
		exitCode = 20;
	    }
	    catch (Throwable e) {
		System.err.println("Throwable!?!?");
		e.printStackTrace();
		exitCode = 50;
	    }
	    finally {
		for (RealizationDB r : rList) {
		    try {
			if (r != null ) r.close();
		    }
		    catch (DeterFault ignored) { }
		}
		try {
		    if (rdb != null ) rdb.close();
		}
		catch (DeterFault ignored) { }
		try {
		    if ( sc != null ) sc.close();
		}
		catch (DeterFault ignored) { }
		System.exit(exitCode);
	    }
	}
    }
    /**
     * Terminate the realization, whether in process or complete.  Release
     * resources and cancel the process.  Status remains live, and the
     * realization can be restarted.
     * @param uid the user calling
     * @param name the realization to terminate
     * @return current realization description
     * @throws DeterFault on errors
     */
    public RealizationDescription terminateRealization(String uid, String name)
	throws DeterFault {
	List<RealizationDB> rList = new ArrayList<>();
	List<ResourceDB> resources = new ArrayList<>();
	RealizationDB rdb = null;
	ResourceDB rrdb = null;
	RealizationDescription rd = new RealizationDescription();

	try {
	    Config config = new Config();

	    String tbUrl = config.getProperty("testbedUrl");

	    rList = RealizationDB.getRealizations(uid,
		    "^" + name + "$", -1, -1, null);

	    if ( rList.size() == 0 )
		throw new DeterFault(DeterFault.request,
			"No such realization: " + name);
	    else if ( rList.size() > 1 )
		throw new DeterFault(DeterFault.internal,
			"Multiple realizations (!?)): " + name);

	    String[] cmd =  new String[] {
		javaPath, "-cp", jarPath,
		classBase + ".DeterLabXmlRpcEmbedder$UnRealizer",
		String.join(fileSeparator, ".", trustedKsName),
		String.join(fileSeparator, ".", userKsName),
		tbUrl, uid, name
	    };
	    ProcessBuilder pb = new ProcessBuilder(cmd);
	    Process p = null;

	    rrdb = new ResourceDB(uid + ":keystore");
	    rrdb.load();

	    establishProcessEnvironment(pb,
		    new ProcessEnvironment(rrdb.getData(),
			trustedContents, null, null));

	    rrdb = null;
	    pb.inheritIO();
	    p = pb.start();

	    rdb = rList.get(0);
	    rdb.load();
	    rdb.setStatus("Releasing");
	    rdb.save();

	    // Order is important.  Get the allocated virtual resources,
	    // disconnect them, then delete them.
	    resources = ResourceDB.getResources(null, null, null, name, false,
		    new ArrayList<ResourceTag>(), -1, -1, null);
	    rdb.setMapping(new HashMap<String, Set<String>>());
	    rdb.setContainment(new HashMap<String, Set<String>>());
	    // Remove the containments and mappings to pass mysql constraints
	    rdb.save();

	    for (ResourceDB r : resources) {
		r.remove();
		r.close();
	    }

	    rdb.setStatus("Empty");
	    rdb.save();

	    rd.setName(rdb.getName());
	    rd.setExperiment(rdb.getExperimentID());
	    rd.setCircle(rdb.getCircleID());
	    rd.setStatus(rdb.getStatus());
	    rd.setACL(rdb.getACL());
	    rd.setContainment(rdb.getContainment());
	    rd.setMapping(rdb.getMapping());
	}
	catch (DeterFault df) {
	    df.printStackTrace();
	}
	finally {
	    for ( ResourceDB r : resources) {
		try { if ( r != null) r.close(); }
		catch (DeterFault ignored) { }
	    }
	    if (rrdb != null) rrdb.close();
	    if (rdb != null) rdb.close();
	    return rd;
	}
    }
}
