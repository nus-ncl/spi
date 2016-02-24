package net.deterlab.testbed.util;

import org.apache.log4j.Level;

import net.deterlab.testbed.client.ApiInfoStub;

import org.apache.axis2.AxisFault;

/**
 * Utility that calls ApiInfo/getVersion and prints the results
 * @author DETER Team
 * @version 1.0
 */
public class GetVersion extends Utility {
    /**
     * Call getVersion and print the results.
     * @param args command line parameters (ignored)
     */
    static public void main(String[] args) {
	loadTrust();
	loadID();

	if ( args.length > 0 && args[0].equals("--debug"))
	    setAxis2LoggingLevel(Level.DEBUG);

	try {
	    ApiInfoStub stub = new ApiInfoStub(getServiceUrl("ApiInfo"));
	    ApiInfoStub.GetVersion req = new ApiInfoStub.GetVersion();
	    ApiInfoStub.GetVersionResponse resp = stub.getVersion(req);

	    ApiInfoStub.VersionDescription r = resp.get_return();
	    System.out.println("Version: " + r.getVersion());
	    System.out.println("PatchLevel: " + r.getPatchLevel());
	    if (r.isKeyIDSpecified()) 
		System.out.println("Keyid: " + r.getKeyID());
	    else
		System.out.println("No Keyid");
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
