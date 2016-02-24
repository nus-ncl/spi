package net.deterlab.testbed.util;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.net.URL;
import javax.net.ssl.SSLHandshakeException;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.ParamOption;

import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * XMLRPC test stub
 * @author DETER Team
 * @version 1.0
 */
public class RpcStub extends Utility {

    static public void usage() {
	fatal("Usage: RemoveLibrary libraryname");
    }

    /**
     * XMLRPS test stub
     * @param args the library to remove is the first and only parameter.
     */
    static public void main(String[] args) {
	try {
	    ParamOption certOption = new ParamOption("cert", "xmlrpc");
	    ParamOption certPwOption = new ParamOption("certpw", "changeit");
	    BooleanOption debugOption = new BooleanOption("debug", false);
	    ParamOption trustOption = new ParamOption("trust", "xmlrpc_trust");
	    ParamOption trustPwOption = new ParamOption("trustpw", "changeit");
	    ParamOption urlOption = new ParamOption("url",
		    "https://boss.isi.deterlab.net:3070/usr/testbed");
	    List<String> argv = new ArrayList<>();
	    File certStore = null;
	    File trustStore = null;

	    Option.parseArgs(args, new Option[] {
		certOption, certPwOption, debugOption,
		    trustOption, trustPwOption, urlOption}, argv);


	    certStore = new File(System.getProperty("user.home"),
		    certOption.getValue());

	    trustStore = new File(System.getProperty("user.home"),
		    trustOption.getValue());

	    // Set trusted certificates.
	    loadTrust(trustStore.getCanonicalPath(), trustPwOption.getValue());
	    loadID(certStore.getCanonicalPath(), certPwOption.getValue());
	    System.err.println(debugOption.getValue());
	    if ( debugOption.getValue() )
		System.setProperty("javax.net.debug", "all");
	    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
	    config.setServerURL(new URL(urlOption.getValue()));
	    XmlRpcClient client = new XmlRpcClient();
	    client.setConfig(config);
	    Map<String, String> params = new HashMap<>();
	    params.put("exp", "faber-cont");
	    params.put("proj", "DeterTest");
	    Object res = client.execute("experiment.virtual_topology",
		    new Object[] { new Double(0.1), params });
	    System.err.println("Result = " + res);


	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    System.exit(20);
	    // e.printStackTrace();
	}
    }
}
