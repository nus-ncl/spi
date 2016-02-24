
package net.deterlab.testbed.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.deterlab.testbed.topology.Element;
import net.deterlab.testbed.topology.IsomorphismException;
import net.deterlab.testbed.topology.Substrate;
import net.deterlab.testbed.topology.Topology;
import net.deterlab.testbed.topology.TopologyDescription;
import net.deterlab.testbed.topology.TopologyException;
import net.deterlab.testbed.topology.TopologyObject;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ListOption;


/**
 * A utility to compare two topologies for isomorphism. The nodes san
 * interfaces should be the same and have the same attributes.  --ignore gives
 * an attribute to ignore in the comparison.  --ignore can be specified
 * multiple times.
 * @author the DETER Team
 * @version 1.0
 */
public class IsomorphismTest extends Utility {

    /**
     * Remove the attribues from substrates and elements.
     * @param t the topology to scrub
     * @param ignored the attribute names to remove
     */
    static void scrubAttrs(Topology t, Collection<String> ignored) {
	for (Element e: t.getElements())
	    for (String ia: ignored)
		e.removeAttribute(ia);
	for (Substrate s: t.getSubstrates())
	    for (String ia: ignored)
		s.removeAttribute(ia);
    }

    /**
     * Test a topdl parse
     * @param args are ignored
     */
    static public void main(String[] args) {
	try {
	    ListOption ignoreAttrs = new ListOption("ignore");
	    List<String> argv = new ArrayList<String>();

	    Option.parseArgs(args, new Option[] { ignoreAttrs }, argv);

	    if (argv.size() < 2)
		fatal("Usage: [--ignore attr] topology1 topology2");

	    TopologyDescription t0 = TopologyDescription.xmlToTopology(
		    new FileInputStream(new File(argv.get(0))), "experiment",
		    false);

	    TopologyDescription t1 = TopologyDescription.xmlToTopology(
		    new FileInputStream(new File(argv.get(1))), "experiment",
		    false);

	    t0.validate(true);
	    t1.validate(true);

	    // Get the topologies separately so we do not compare maps, etc
	    Topology top0 = new Topology(t0);
	    Topology top1 = new Topology(t1);

	    // Remove ignored attributes from the substrates and elements
	    scrubAttrs(top0, ignoreAttrs.getValue());
	    scrubAttrs(top1, ignoreAttrs.getValue());

	    top0.sameAs(top1);
	    System.out.println("Isomorphic");

	}
	catch (IOException e) {
	    fatal(e.getMessage());
	}
	catch (TopologyException e) {
	    fatal(e.getMessage());
	}
	catch (IsomorphismException e) {
	    System.err.println(e.getMessage());
	    e.printTrace(System.err);
	    fatal("Failed");
	}
	catch (Exception e) {
	    e.printStackTrace();
	    fatal(e.getMessage());
	}
    }
}
