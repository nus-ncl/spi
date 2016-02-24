package net.deterlab.testbed.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * A utility to insert the default circle profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class ParseTopdl extends Utility {

    /**
     * Test a topdl parse
     * @param args are ignored
     */
    static public void main(String[] args) {
	try {
	    TopologyDescription t = TopologyDescription.xmlToTopology(
		    new FileInputStream(new File(args[0])), "experiment",
		    false);

	    t.writeXML(new OutputStreamWriter(System.out), "experiment");
	}
	catch (IOException e) {
	    fatal(e.getMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	    fatal(e.getMessage());
	}
    }
}
