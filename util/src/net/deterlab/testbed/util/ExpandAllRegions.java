package net.deterlab.testbed.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.deterlab.testbed.topology.Element;
import net.deterlab.testbed.topology.Fragment;
import net.deterlab.testbed.topology.NameMap;
import net.deterlab.testbed.topology.Region;
import net.deterlab.testbed.topology.TopologyDescription;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * Expand regions in the input file and output an expanded file (and an
 * annotated file) if requested.  The expanded filename has all regions
 * expanded.  The annotated file has additional name maps added for each
 * expansion performed.
 * @author the DETER Team
 * @version 1.0
 */
public class ExpandAllRegions extends Utility {

    static public void usage() {
	fatal("Usage: RegionTest [--expanded fn] [--annotated fn] infile\n" +
		"\t annotated filename and infile may be the same to replace");
    }

    /**
     * Test a topdl parse
     * @param args are ignored
     */
    static public void main(String[] args) {
	try {
	    ParamOption exp = new ParamOption("expanded");
	    ParamOption annotated = new ParamOption("annotated");
	    Option[] opts = new Option[] { exp, annotated};
	    List<String> argv = new ArrayList<String>();

	    Option.parseArgs(args, opts, argv);

	    if ( argv.size() != 1) 
		usage();

	    TopologyDescription t = TopologyDescription.xmlToTopology(
		    new FileInputStream(new File(argv.get(0))), "experiment",
		    false);
	    TopologyDescription n = t.clone();
	    boolean expanded = true;
	    int expd =0;

	    do {
		expanded = false;
		for (Element e: new ArrayList<Element>(n.getElements())) {
		    if ( ! (e instanceof Region)) continue;
		    Region r = (Region) e;
		    String fname = r.getFragmentName();
		    Fragment f = n.getFragment(fname);
		    String path = r.getAttribute("path");
		    String pathname = (path != null) ? 
			path + r.getName() : "/" + r.getName();

		    if ( f == null )
			fatal("No fragment for " + r.getFragmentName());

		    NameMap nm = new NameMap(pathname, null, null);

		    r.expand(f, null, n, nm);
		    t.addNameMap(nm);
		    expanded = true;
		    break;
		}
	    } while ( expanded);

	    if ( exp.getValue() != null ) 
		n.writeXML(new FileWriter(exp.getValue()), "experiment");
	    if ( annotated.getValue() != null ) 
		t.writeXML(new FileWriter(annotated.getValue()), "experiment");
	}
	catch (IOException e) {
	    fatal(e.getMessage());
	}
	catch (Option.OptionException e) {
	    fatal(e.getMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	    fatal(e.getMessage());
	}
    }
}
