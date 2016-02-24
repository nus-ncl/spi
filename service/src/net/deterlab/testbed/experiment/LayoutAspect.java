package net.deterlab.testbed.experiment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.topology.Fragment;
import net.deterlab.testbed.topology.IsomorphismException;
import net.deterlab.testbed.topology.NameMap;
import net.deterlab.testbed.topology.TopologyDescription;
import net.deterlab.testbed.topology.TopologyException;
import net.deterlab.testbed.topology.TopologyObject;

/**
 * Process a layout aspect of an experiment
 * @author DETER team
 * @version 1.1
 */
public class LayoutAspect implements Aspect {
    /** Type of this Aspect */
    static private String TYPE = "layout";
    /** Type of a full layout aspect */
    static private String FULL_LAYOUT = "full_layout";
    /** Template to generate name for an unnamed aspect */
    static private String NameTemplate = "layout%03d";

    /**
     * This encapsulates the information about a given experiment's layout
     * aspects so the outer class can validate changes and assign names to
     * unnamed aspects.
     * @author DETER team
     * @version 1.1
     */
    protected class ExperimentContext {
	/** The layout - new layouts must be equivalent. */
	private TopologyDescription topo;
	/** Names of existing layout aspects */
	private Set<String> aspectNames;

	/**
	 * Create an empty ExperimentContext
	 */
	public ExperimentContext() {
	    topo = null;
	    aspectNames = new HashSet<String>();
	}

	/**
	 * Return the current layout, or null if no layout
	 * @return the current layout, or null if no layout
	 */
	public TopologyDescription getLayout() { return topo; }

	/**
	 * Set the current layout
	 * @param t the new layout
	 */
	public void setLayout(TopologyDescription t) { topo = t; }

	/**
	 * Return true if an aspect named n is present in this context.
	 * @return true if an aspect named n is present in this context.
	 */
	public boolean containsName(String n) {
	    return aspectNames.contains(n);
	}

	/**
	 * Add an aspect name to this context
	 * @param n the name to add
	 */
	public void addName(String n) { aspectNames.add(n); }

	/**
	 * Remove an aspect name from this context
	 * @param n the name to remove
	 */
	public void removeName(String n) { aspectNames.remove(n); }
    };


    /** The ExperimentContexts keyed by transaction */
    private Map<Long, ExperimentContext> context;

    /**
     * Simple Constructor
     */
    public LayoutAspect() {
	context = new HashMap<Long, ExperimentContext>();
    }

    /**
     * Aspects all take strings in their constructors, but this is ignored.
     * @param ignored nominally the Aspect type. Ignored
     */
    public LayoutAspect(String ignored) { this(); }

    /**
     * Return the type of aspect this class processes.
     * @return the type of aspect this class processes.
     */
    public String getType() { return TYPE; }
    /**
     * Compare the given layouts to the canonical description and throw a
     * fault if they are not isomorphic (or td is invalid)
     * @param td the TopologyDescription to check
     * @param thisCanon the canonical layouts to compare against
     * @throws DeterFault if the TopologyDescription is invalid or not
     * isomprphic to the canon.
     */
    private void validateTopology(TopologyDescription td,
	    TopologyDescription thisCanon)
	throws DeterFault {
	try {
	    TopologyDescription tmp = td.clone();
	    tmp.validate(true);
	    tmp.sameAs(thisCanon);
	}
	catch (TopologyException e) {
	    throw new DeterFault(DeterFault.request, "Invalid layout");
	}
	catch (IsomorphismException e) {
	    throw new DeterFault(DeterFault.request,
		    "Topologies are not isomorphic");
	}
    }

    /**
     * Create an ExperimentAspect containing this TopologyDescription.
     * @param type aspect type
     * @param subType aspect sutype
     * @param name aspect name
     * @param t the object
     * @param label the label for the XML object output
     * @param readOnly the read only value for the aspect
     * @return the new ExperimenttAspec
     * @throws DeterFault on errors
     */
    private ExperimentAspect topologyToExperimentAspect(String type,
	    String subType, String name, TopologyObject t, String label,
	    boolean readOnly)
	throws DeterFault {
	ExperimentAspect aa = null;
	try {
	    ByteArrayOutputStream bs = new ByteArrayOutputStream();
	    OutputStreamWriter out = new OutputStreamWriter(bs);

	    t.writeXML(out, label);
	    out.close();
	    aa = new ExperimentAspect();
	    aa.setType(type);
	    aa.setSubType(subType);
	    aa.setName(name);
	    aa.setData(bs.toByteArray());
	    return aa;
	}
	catch (IOException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot output layout aspect");
	}
    }

    /**
     * Build the list of subtypes from a layout descriprion.
     * @param a the aspect
     * @param td the topology
     * @return a List of ExperimentAspects that are derived from the
     *	layout
     * @throws DeterFault if there is a conversion error.
     */
    private List<ExperimentAspect> topologyToExperimentAspects(
	    ExperimentAspect a, TopologyDescription td) throws DeterFault {
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();

	try {

	    // Just the topology we're given
	    rv.add(topologyToExperimentAspect(a.getType(), null, a.getName(),
			td, "experiment", false));

	    TopologyDescription full = td.clone();
	    TopologyDescription mini = td.clone();

	    // Expand that topology all the way for the full_topology
	    full.validate(true);

	    rv.add(topologyToExperimentAspect(a.getType(), "full_layout",
			a.getName()+"/full_layout", full, "experiment",
			true));

	    // Copy the fragments from the full layout into separate
	    // files and remove them from the mini layouts (if they're
	    // there)
	    List<Fragment> frags = new ArrayList<Fragment>(
		    full.getFragments());
	    for (Fragment f : frags) {
		rv.add(topologyToExperimentAspect(a.getType(), "fragment",
			a.getName()+"/fragment/"+f.getName(),
			f, "fragments", true));
		mini.removeFragment(f);
	    }

	    // Copy the name maps from the full topology into separate
	    // files and remove them from the mini topology (if they're
	    // there)
	    List<NameMap> maps = new ArrayList<NameMap>(
		    full.getNameMaps());
	    for (NameMap nm: maps) {
		rv.add(topologyToExperimentAspect(a.getType(), "namemap",
			a.getName()+"/namemap"+ nm.getPathName(),
			nm, "namemaps", true));
		mini.removeNameMap(nm);
	    }

	    // Save the original topology with no maps or frags as the
	    // minimal topology.
	    rv.add(topologyToExperimentAspect(a.getType(),
			"minimal_layout",
			a.getName()+"/minimal_layout",
			mini, "experiment", true));
	    return rv;
	}
	catch (TopologyException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Bad layout (How'd it get *here*)");
	}
    }

    /**
     * Generate an aspect name not present in context; the name is not added to
     * the context.  The routine simply serially appends integers to the name
     * template, so it is not a high performance routine.
     * @param ctxt the context
     * @return a unique name
     * @throws DeterFault if a unique name cannot be generated in a reasonable
     * number of tries
     */
    private String uniqueName(ExperimentContext ctxt) throws DeterFault {
	final int limit = 10000;
	int i = 0;

	String n = String.format(NameTemplate, i++);
	while ( ctxt.containsName(n) && i < limit )
	    n = String.format(NameTemplate, i++);

	if ( ctxt.containsName(n))
	    throw new DeterFault(DeterFault.request,
		    "Cannot generate unique aspect name for " + getType());
	return n;
    }

    /**
     * Begin an transaction on the experiment.  One or more instances of this
     * aspect are being added, removed, or realized in an experiment.  Calls to
     * the other interfaces will be made with this transactionID and then
     * finalized.  The plugin may cache information to do consistency checking
     * an other operations.  In principle a transactionID can be reused after
     * finalizeTransaction is called on it.  Load current layout aspects for
     * later comparisons or modifications.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void beginTransaction(ImmutableExperimentDB exp, long transactionID)
	throws DeterFault {
	ExperimentContext ctxt = new ExperimentContext();
	ExperimentAspect[] req = new ExperimentAspect[] {
	    new ExperimentAspect()
	};
	req[0].setType(getType());

	// Gather up any layout aspects already present in exp.
	for (ExperimentAspect ea: exp.getAspects(Arrays.asList(req), true)) {
	    if (ctxt.getLayout() == null ) {
		try {
		    TopologyDescription td = TopologyDescription.xmlToTopology(
			    new ByteArrayInputStream(ea.getData()),
			"experiment", false);
		    td.validate(true);
		    ctxt.setLayout(td);
		}
		catch (IOException ie ) {
		    throw new DeterFault(DeterFault.internal,
			    "cannot load layout aspect in experiment " +
			    ea.getName());
		}
		catch (TopologyException te) {
		    // A bad aspect stored in the experiment!?
		    throw new DeterFault(DeterFault.internal,
			    "Bad layout aspect in experiment " + ea.getName());
		}
	    }
	    ctxt.addName(ea.getName());
	}
	context.put(transactionID, ctxt);
    }

    /**
     * Add a new instance of this aspect to the experiment.  Process
     * layouts.  If this is the first one encountered, also transform it
     * into the canonical representation.  If not throw a fault if it is not
     * isomorphic to the canon.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be added to the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> addAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	throws DeterFault {
	List<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();
	TopologyDescription td = null;
	ExperimentContext ctxt = context.get(transactionID);
	TopologyDescription layout;

	if ( ctxt == null )
	    throw new DeterFault(DeterFault.internal,
		    "No context for transaction " + transactionID);
	layout = ctxt.getLayout();

	try {
	    if (inputAspect.getData() == null)
		throw new DeterFault(DeterFault.request,
			"No layout data?");
	    td = TopologyDescription.xmlToTopology(
		    new ByteArrayInputStream(inputAspect.getData()),
		    "experiment", false);
	    td.validate(false);
	    if ( layout == null ) {
		if (inputAspect.getName() == null ||
			inputAspect.getName().isEmpty())
		    inputAspect.setName(uniqueName(ctxt));
		ctxt.addName(inputAspect.getName());

		rv.addAll(topologyToExperimentAspects(inputAspect, td));

		// Expand the topology into a canonical layout (saves having to
		// expand it again for later checks)
		layout = td.clone();
		layout.validate(true);
		ctxt.setLayout(layout);
	    }
	    else {
		// Make sure this layout is isomorphic to other layouts and
		// return it.
		validateTopology(td, layout);
		if (inputAspect.getName() == null ||
			inputAspect.getName().isEmpty())
		    inputAspect.setName(uniqueName(ctxt));
		if (ctxt.containsName(inputAspect.getName()))
		    throw new DeterFault(DeterFault.request,
			    "Layout aspect " + inputAspect.getName() +
			    " already exists");
		ctxt.addName(inputAspect.getName());
		rv.addAll(topologyToExperimentAspects(inputAspect, td));
	    }
	    return rv;
	}
	catch (TopologyException e) {
	    throw new DeterFault(DeterFault.request, "Bad layout:" +
		    e.getMessage());
	}
	catch (IOException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Cannot read layout!?:" + e.getMessage());
	}
    }
    /**
     * Request a change to this aspect to the experiment.  Unimplemented.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param inputAspect the requested aspect addition
     * @return a collection of aspects to be overwritten in the experiment
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> changeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect inputAspect)
	    throws DeterFault {
	throw new DeterFault(DeterFault.unimplemented, "Not implemented");
    }
    /**
     * Remove an instance of this aspect from the experiment.  The removeAspect
     * parameter includes the name and type of the aspect to remove - other
     * fields are not guaranteed to be valid.  If aspect details are needed,
     * they can be requested from the experiment.  Implicit in the removal
     * request is the removal of all sub-aspects with the same type and name.
     * Unless this routine throws a DeterFault, the aspect and subaspect are
     * queued for removal.  Actual removal isn't done until finalizeTransaction
     * is called, so the plugin can veto the removal there as well.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param removeAspect the requested aspect removal, only name and type are
     *	    necessarily valid.
     * @throws DeterFault if the aspect addition is unacceptable
     */
    public Collection<ExperimentAspect> removeAspect(
	    ImmutableExperimentDB exp, long transactionID,
	    ExperimentAspect removeAspect) throws DeterFault {

	ExperimentContext ctxt = context.get(transactionID);
	ExperimentAspect[] req = new ExperimentAspect[] {
	    new ExperimentAspect()
	};
	Collection<ExperimentAspect> rv = new ArrayList<ExperimentAspect>();
	if ( removeAspect.getSubType() != null )
	    throw new DeterFault(DeterFault.request,
		    "Cannot remove layout subtypes");

	if ( ctxt == null )
	    throw new DeterFault(DeterFault.internal,
		    "No context for transaction id " + transactionID);

	if ( !ctxt.containsName(removeAspect.getName()))
	    throw new DeterFault(DeterFault.request,
		    "No such layout " + removeAspect.getName());

	// OK, there is such an aspect.  Return the aspect and all its sub
	// aspects as what to remove.  Construct a request that will return all
	// the layouts and subtypes, then return the ones that are derived from
	// removeAspect.
	req[0].setName(null);
	req[0].setType(getType());
	req[0].setSubType("*");
	String prefix = removeAspect.getName() + "/";

	rv.add(removeAspect);
	for (ExperimentAspect ea : exp.getAspects(Arrays.asList(req), false)) {
	    String name = ea.getName();

	    if (name.startsWith(prefix))
		rv.add(ea);
	}
	return rv;
    }

    /**
     * Realize this aspect.  The first full layout aspect returns its
     * associated topology description.  This is a convention, and may change.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param realizeAspect the requested aspect realization
     * @param realTop the realization layouts
     * @return an new TopologyDescription or null if there are no
     * changes to realTop
     * @throws DeterFault if the aspect realization is unacceptable
     */
    public TopologyDescription realizeAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect realizeAspect,
	    TopologyDescription realTop) throws DeterFault {
	TopologyDescription rv = null;

	if (realTop != null ) return null;
	if (TYPE.equals(realizeAspect.getType()) &&
		FULL_LAYOUT.equals(realizeAspect.getSubType())) {
	    List<ExperimentAspect> aspects = exp.getAspects(
		    Arrays.asList(new ExperimentAspect[] {realizeAspect}),
		    true);
	    for (ExperimentAspect a : aspects) {
		if (rv != null )
		    throw new DeterFault(DeterFault.internal,
			    "Multiple definitions of aspect");
		try {
		    rv = TopologyDescription.xmlToTopology(
			    new ByteArrayInputStream(a.getData()),
			    "experiment", false);
		}
		catch (IOException ie) {
		    throw new DeterFault(DeterFault.internal,
			    "IO error!?: " + ie.getMessage());
		}
		catch (TopologyException te) {
		    throw new DeterFault(DeterFault.internal,
			    "Bad stored topology: " + te.getMessage());
		}
	    }
	}
	return rv;
    }

    /**
     * Resources for the current realization are being released.  The plugin
     * should adjust its internal state in any way necessary.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @param releaseAspect the requested aspect realization, only name and
     *	    type are necessarily valid.
     * @throws DeterFault on catastrophic problems
     */
    public void releaseAspect(ImmutableExperimentDB exp,
	    long transactionID, ExperimentAspect releaseAspect)
	throws DeterFault { }

    /**
     * Finalize the transaction.  Unless the plugin throws a DeterFault, the
     * transaction to date will be carried out.  After this returns, the
     * transactionID can be reused.  The plugin should release any resources it
     * has been using to validate the transaction.
     * @param exp the experiment being operated on
     * @param transactionID a unique identifier for this transaction
     * @throws DeterFault if the transaction cannot be started
     */
    public void finalizeTransaction(ImmutableExperimentDB exp,
	    long transactionID) throws DeterFault {
	context.remove(transactionID);
    }
}
