package net.deterlab.testbed.util;

import java.lang.reflect.InvocationTargetException;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.deterlab.testbed.util.gui.TopologyDialog;
import net.deterlab.testbed.topology.TopologyDescription;

/**
 * A utility to insert the default circle profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class TopologyViewer extends Utility {
    /**
     * Test a topdl parse
     * @param args are ignored
     */
    static public void main(String[] args) {
	TopologyDescription t = null;
	TopologyDialog td;
	try {
	    t = TopologyDescription.xmlToTopology(
		    new FileInputStream(new File(args[0])), "experiment",
		    false);
	    td = new TopologyDialog(t);
	    td.setVisible(true);
	    if ( td.isCancelled()) {
		td.dispose();
		return;
	    }

	    t = td.getTopology();
	    // Dispose or we don't exit...
	    td.dispose();
	    t.validate(false);
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
