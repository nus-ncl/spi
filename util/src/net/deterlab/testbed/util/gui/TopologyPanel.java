package net.deterlab.testbed.util.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Shape;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;

import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;

import edu.uci.ics.jung.graph.util.Pair;

import edu.uci.ics.jung.visualization.MultiLayerTransformer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;

import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.EditingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.EditingPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;

import edu.uci.ics.jung.visualization.picking.PickedState;

import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

import net.deterlab.testbed.topology.Computer;
import net.deterlab.testbed.topology.ConnectedObject;
import net.deterlab.testbed.topology.Element;
import net.deterlab.testbed.topology.Fragment;
import net.deterlab.testbed.topology.Interface;
import net.deterlab.testbed.topology.NameMap;
import net.deterlab.testbed.topology.OtherElement;
import net.deterlab.testbed.topology.Region;
import net.deterlab.testbed.topology.Substrate;
import net.deterlab.testbed.topology.Topology;
import net.deterlab.testbed.topology.TopologyDescription;
import net.deterlab.testbed.topology.TopologyException;
import net.deterlab.testbed.topology.Util;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

/**
 * A utility to insert the default circle profile fields into the deterDB
 * @author the DETER Team
 * @version 1.0
 */
public class TopologyPanel extends JPanel {

    /** The size of the panel */
    static protected final Dimension preferredSize = new Dimension(350, 350);
    /** Size of the initial layout */
    static protected final double initialLayoutSize = 1000.0;

    /** The topology to draw/edit */
    private TopologyDescription top;
    /** Graph representation of the topology */
    private UndirectedGraph<ConnectedObject, Interface> g;
    /** The visualizer that draws the graph */
    private VisualizationViewer<ConnectedObject, Interface> vv;
    /** The layout actually rendered */
    private Layout<ConnectedObject, Interface> layout;
    /** Current layout size - the old size when things change */
    private Dimension layoutSize;
    /** Current layout center - the old center when things change */
    private Point2D oldCenter;
    /** The regions expanded */
    private Map<String, Region> regions;

    /** Scaled shape to draw for a substrate */
    private Shape subShape;
    /** Scaled shape to draw for a computer */
    private Shape compShape;
    /** Scaled shape to draw for a region */
    private Shape regionShape;
    /** Scaled shape to draw for something unknown */
    private Shape otherShape;

    /**
     * Add new nodes from top into g.
     */
    protected void updateTopologyGraph() {
	Set<ConnectedObject> newVertices = new HashSet<ConnectedObject>();

	// Remove connected objects from g that have been removed from the
	// topology.  This removes dangling edges.
	for ( ConnectedObject v : 
		new ArrayList<ConnectedObject>(g.getVertices()) ) {
	    if ( v instanceof Element ) {
		Element e = (Element) v;
		if ( top.getElement(e.getName()) == null ) 
		    g.removeVertex(v);
	    }
	    else if ( v instanceof Substrate ) {
		Substrate s = (Substrate) v;
		if ( top.getSubstrate(s.getName()) == null )
		    g.removeVertex(v);
	    }
	    else System.err.println("Thats weird - vertex of unknown type");
	}

	// Now add anything that's new in top to g
	for (Substrate s: top.getSubstrates())
	    if ( !g.containsVertex(s)) { g.addVertex(s); newVertices.add(s); }
	for ( Element e : top.getElements()) {
	    if ( !g.containsVertex(e)) { g.addVertex(e); newVertices.add(e); }
	    for ( Interface i: e.getInterfaces()) {
		if ( !g.containsEdge(i)) 
		    g.addEdge(i, e, i.getSubstrate());
	    }
	}

	// If no new vertices, or no layout at all has been done, bail.
	if (newVertices.isEmpty() || layout == null) return;
	addToLayout(g, newVertices);
    }

    /**
     * Layout the graph and return a static layout.
     * @param lg the graph to layout
     */
    protected Layout<ConnectedObject, Interface> layoutGraph(
	    Graph<ConnectedObject, Interface> lg) {
	// Use ISOMLayout to lay out the graph in a large space.
	ISOMLayout<ConnectedObject, Interface> tlayout =
	    new ISOMLayout<ConnectedObject, Interface>(lg);
	tlayout.initialize();

	// Calculate the positions
	tlayout.setSize(new Dimension((int) initialLayoutSize,
		    (int) initialLayoutSize));
	while (!tlayout.done())
	    tlayout.step();

	// Translate to the simpler display layout
	StaticLayout<ConnectedObject, Interface> layout =
	    new StaticLayout<ConnectedObject, Interface>(lg,
		new ScaleTransform(tlayout,
		    (0.85 * layoutSize.getWidth())/initialLayoutSize,
		    (0.85 * layoutSize.getHeight())/initialLayoutSize,
		    new Point2D.Double(initialLayoutSize/2,initialLayoutSize/2),
		    new Point2D.Double(layoutSize.getWidth()/2,
			layoutSize.getHeight()/2)));
	return layout;
    }

    /**
     * Layout new nodes added to lg that have been added to the panel's layout
     * randomly.  Create a new ISOMLayout with all old nodes locked and new
     * nodes free and lay it out.  Then put the new nodes into the panel's
     * layout.
     * @param lg the graph to layout
     * @param nv the new vertices
     */
    protected void addToLayout(Graph<ConnectedObject, Interface> lg,
	    Set<ConnectedObject> nv) {
	// Use ISOMLayout to lay out the graph in a large space.
	ISOMLayout<ConnectedObject, Interface> tlayout =
	    new ISOMLayout<ConnectedObject, Interface>(lg);
	tlayout.initialize();
	tlayout.setSize(layout.getSize());
	for (ConnectedObject v: lg.getVertices()) {
	    if ( nv.contains(v)) continue;
	    tlayout.setLocation(v, layout.transform(v));
	    tlayout.lock(v, true);
	}

	while (!tlayout.done())
	    tlayout.step();

	for (ConnectedObject v: lg.getVertices()) {
	    if (!nv.contains(v)) continue;
	    layout.setLocation(v, tlayout.transform(v));
	}
    }

    /**
     * Redo the graph layout and move vertices to their new locations. Called
     * from the menu.
     */
    protected void redraw() {
	Layout<ConnectedObject, Interface> l = layoutGraph(g);
	for (ConnectedObject c: g.getVertices())
	    layout.setLocation(c, l.transform(c));
	vv.repaint();
    }


    /**
     * Transformer that paints the vertices according to their values
     */
    protected class VertexPaint implements 
	Transformer<ConnectedObject, Paint> {
	PickedState<ConnectedObject> state;

	final private Color subColor = new Color(128, 128, 255);
	final private Color compColor = new Color(128, 255, 128);
	final private Color regionColor = new Color(255, 128, 255);
	final private Color unknownColor = new Color(255, 128, 128);
	final private Color subSelectedColor = new Color(225, 128, 255);
	final private Color compSelectedColor = new Color(225, 255, 128);
	final private Color regionSelectedColor = new Color(255, 255, 255);
	final private Color unknownSelectedColor = new Color(255, 225, 225);

	public VertexPaint(PickedState<ConnectedObject> ps) {
	    state = ps;
	}

	public Paint transform(ConnectedObject o) {
	    if (o instanceof Substrate ) 
		return state.isPicked(o) ? subSelectedColor : subColor;
	    else if (o instanceof Computer ) 
		return state.isPicked(o) ? compSelectedColor: compColor;
	    else if (o instanceof Region ) 
		return state.isPicked(o) ? regionSelectedColor: regionColor;
	    else 
		return state.isPicked(o) ? unknownSelectedColor: unknownColor;
	}
    }
    /**
     * Transformer that produces the vertex shapes from their values
     */
    protected class VertexShape implements 
	Transformer<ConnectedObject, Shape> {
	public Shape transform(ConnectedObject o) {
	    if (o instanceof Substrate ) 
		return subShape;
	    else if (o instanceof Computer )
		return compShape;
	    else if (o instanceof Region )
		return regionShape;
	    else 
		return otherShape;
	}
    }
    /**
     * Transformer that produces the vertex labels from their values
     */
    protected class VertexLabel implements 
	Transformer<ConnectedObject, String> {

	public String transform(ConnectedObject o) {
	    Graphics g = getGraphics();
	    FontMetrics fm = g.getFontMetrics();
	    String label = null;
	    Shape shape = null;

	    if (o instanceof Substrate ) {
		Substrate s = (Substrate) o;
		label = s.getName();
		shape = subShape;
	    }
	    else if (o instanceof Element ) {
		Element e = (Element) o;
		label = e.getName();

		if (label == null) label = "";

		if ( o instanceof Computer ) shape = compShape;
		else if ( o instanceof Region ) shape = regionShape;
		else shape = otherShape;
	    }
	    else return "";

	    if (fm.stringWidth(label) < shape.getBounds().getWidth()) 
		return label;
	    else 
		return "";
	    
	}
    }

    /**
     * Provide a tooltip for thsi vertex.  Right now just the name. can be
     * expanded later.
     */
    protected class VertexToolTip implements
	Transformer<ConnectedObject, String> {

	/**
	 * Get the tooltip.
	 * @param o the object to convert
	 * @return the tooltip string.
	 */
	public String transform(ConnectedObject o) {
	    if (o instanceof Substrate ) {
		Substrate s = (Substrate) o;
		return "Substrate: " + s.getName();
	    }
	    else if (o instanceof Computer ) {
		Computer c = (Computer) o;
		return "Computer " + c.getName();
	    }
	    else if (o instanceof Region ) {
		Region r = (Region) o;
		return "Region " + r.getName();
	    }
	    else return "Bad node?";
	}
    }

    /**
     * Initialize the shapes returned by the VertexShape class when drawing the
     * graph.  These are dynamically sized based on the size of the canvas, the
     * number fo vertices and a pinch of fairy dust.
     */
    protected void setShapes() {
	// The 15 is the pinch of fairy dust.  The formula divides the area of
	// the drawing area by the number of things to draw, multiplied by the
	// magical 15.  That looked good by eye using the ISOMLayout on both
	// big and small graphs.  A different pinch of dust would be needed for
	// other layouts.
	final double ss = Math.sqrt(
		(layoutSize.getWidth() * layoutSize.getHeight())/
		(15 * g.getVertexCount()));

	subShape = new Ellipse2D.Double(-ss/2, -ss/2, ss, ss);
	compShape = new RoundRectangle2D.Double(-ss/2, -ss/2, ss, ss,
		0.2* ss, 0.2*ss);
	regionShape = new Ellipse2D.Double(-ss, -ss/2, 2*ss, ss);
	otherShape = new Rectangle2D.Double(-ss/2, -ss/2, ss, ss);
    }

    /**
     * Rescale the graph to the new window size or new graph size (after a
     * region expansion.)
     */
    public void rescale() {
	// Determine the factor by which the size has changed for scaling.
	Dimension newLayoutSize = vv.getSize();
	double sx = newLayoutSize.getWidth()/layoutSize.getWidth();
	double sy = newLayoutSize.getHeight()/layoutSize.getHeight();

	// When the layout size is reset, it recenters the points in the
	// layout to a new center.  This transform rescales the points
	// relative to the old center.  Concatenate reads backward, so the
	// sequence carried out by the transform is:
	//
	// 1. Move the points to a position around the origin relative to
	//	    their position about the old center
	// 2. Rescale them by the factor above
	// 3. Move them back to their rescaled position relative to the
	//	    same center.
	AffineTransform scale =
	    AffineTransform.getTranslateInstance(oldCenter.getX(),
		    oldCenter.getY());
	scale.concatenate(AffineTransform.getScaleInstance(sx, sy));
	scale.concatenate(
	    AffineTransform.getTranslateInstance(-oldCenter.getX(),
		    -oldCenter.getY()));

	// Update the state.
	oldCenter = vv.getCenter();
	layoutSize = newLayoutSize;

	// Rescale all the points
	for (ConnectedObject ob : layout.getGraph().getVertices())
	    layout.setLocation(ob, scale.transform(layout.transform(ob),null));

	// Recenters the points around the new center as a side effect
	layout.setSize(layoutSize);
	// Now set the shape sizes - it picks up the new values set above
	setShapes();
    }

    /**
     * This class is used to rescale and recenter the initial layout into a
     * static layout.  It is passed to the initializer parameter of the
     * StaticLayout constructor after being initialized with the initial layout
     * (a Transformer), the new scale, old
     * center point and new center point.
     */
    protected class ScaleTransform implements 
	Transformer<ConnectedObject, Point2D> {
	/** The post layout transform - scaling and moving to a new center */
	private AffineTransform scaleXform;
	/** The layout being transformed */
	private Layout<ConnectedObject, Interface> xform;

	/**
	 * Build the transformer.
	 * @param t the layout being rescaled and recentered.
	 * @param sx the scale factor on the x axis.
	 * @param sy the scale factor on the y axis.
	 * @param oldCenter the center of the initial layout.
	 * @param newCenter the center of the new layout
	 */
	public ScaleTransform(Layout<ConnectedObject, Interface> t, 
		double sx, double sy, Point2D oldCenter, Point2D newCenter) { 
	    // Concatenate reads backward.  This sequence of transforms 
	    //  1. Moves the point from its position relative to the old center
	    //	    to the origin
	    //  2. Applies the scaling factor
	    //  3. Moves the point to its new position relative to the new
	    //	    center.
	    scaleXform = AffineTransform.getTranslateInstance(newCenter.getX(),
		    newCenter.getY());
	    scaleXform.concatenate(AffineTransform.getScaleInstance(sx, sy));
	    scaleXform.concatenate(
		    AffineTransform.getTranslateInstance(
			-oldCenter.getX(), -oldCenter.getY()));
	    xform = t;
	}
	/**
	 * Carry out the transform.   Get the point from the existing layout
	 * and use xform to move it to its new home.
	 * @param o the object that names the vertex in the Layout
	 * @return the rescaled and recentered position.
	 */
	public Point2D transform(ConnectedObject o) {
	    return scaleXform.transform(xform.transform(o), null);
	}
    }

    /**
     * Factory that throws out unititialized OtherElements to act as
     * placeholders.  FinishAddingVertex will replace them with something
     * useful
     */
    protected class VertexFactory implements Factory<ConnectedObject> {
	/**
	 * Return an new OtherElement
	 * @return an new OtherElement
	 */
	public ConnectedObject create() {
	    return new OtherElement();
	}
    }

    /**
     * Factory that produces Interfaces.
     */
    protected class InterfaceFactory implements Factory<Interface> {
	/**
	 * Return an new Interface
	 * @return an new Interface
	 */
	public Interface create() {
	    return new Interface();
	}
    }

    /**
     * A Custom mouse handler to do the simple stuff we want to do
     */
    protected class Mouse extends PluggableGraphMouse {
	protected class Popup extends 
	    AbstractPopupGraphMousePlugin {
	    public Popup() {
		super();
	    }

	    protected void handlePopup(MouseEvent e) {
		Point2D p = e.getPoint();
		PickedState<ConnectedObject> pickedVertexState =
		    vv.getPickedVertexState();
		GraphElementAccessor<ConnectedObject,Interface> picked =
		    vv.getPickSupport();
		ConnectedObject v = picked.getVertex(layout,p.getX(), p.getY());
		Interface i = picked.getEdge(layout, p.getX(), p.getY());
		JPopupMenu jp = new JPopupMenu();
		Set<ConnectedObject> pickedVertices =
		    pickedVertexState.getPicked();

		if ( v!= null && i != null ) return;
		else if ( v == null && i == null ) {
		    jp.add(new AddComputer(p));
		    jp.add(new AddSubstrate(p));
		    jp.add(new AbstractAction("Layout Graph Again") {
			public void actionPerformed(ActionEvent ae) {
			    redraw();
			}
		    });
		}
		else if (v != null ) {
		    String obj = null;

		    if ( v instanceof Substrate ) obj = "Substrate";
		    else if (v instanceof Computer) {
			obj = "Computer";
			String pathAttr = v.getAttribute("path");
			String[] path = (pathAttr != null) ?
			    pathAttr.split("/") : new String[0];

			for (String pc: path) {
			    Region r = regions.get(pc);
			    if (r == null ) continue;
			    jp.add(new CollapseRegion("Collapse Region " + pc,
					r));
			}
		    }
		    else if ( v instanceof Region) {
			obj = "Region";
			jp.add(new ExpandRegion("Expand Region", (Region) v));
		    }
		    else obj = "Thing";

		    if ( pickedVertices.contains(v)) {
			jp.add(new PickedRegion("Make Region from picked",
				    pickedVertices));
		    }
		    jp.add(new RenameVertex(obj, v));
		    jp.add(new DeleteVertex(obj, v));
		}
		else if ( e != null ) {
		    jp.add(new DeleteEdge(i));
		}
		jp.show(vv, e.getX(), e.getY());
	    }
	}

	/**
	 * Configure the set of tools we want
	 * @param vertexFactory makes new vertices
	 * @param edgeFactory makes new edges
	 */
	public Mouse(Factory<ConnectedObject> vertexFactory, 
		Factory<Interface> edgeFactory) {
	    super();
	   add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0));
	   // add(new TranslatingGraphMousePlugin());
	   add(new PickingGraphMousePlugin());
	   if ( vertexFactory != null && edgeFactory != null ) {
	       add(new EditingGraphMousePlugin<ConnectedObject,Interface>(
			   InputEvent.CTRL_MASK, vertexFactory, edgeFactory));
	       add(new Popup());
	   }
	}
    }

    /**
     * This class renames a vertex
     */
    protected class RenameVertex extends AbstractAction {
	private String kind;
	private ConnectedObject v;

	public RenameVertex(String k, ConnectedObject vv) {
	    super("Rename " + k);
	    kind = k;
	    v = vv;
	}

	protected void error(String msg) {
	    JOptionPane.showMessageDialog(vv,
		    "Error renaming " + kind + ": " + msg,
		    "Expansion Error",
		    JOptionPane.ERROR_MESSAGE);
	}

	protected boolean renameSub(Substrate s, String n, String msg) {
	    top.removeSubstrate(s);
	    s.setName(n);
	    try {
		top.addSubstrate(s);
		return true;
	    }
	    catch (TopologyException e) {
		if ( msg == null ) error(e.getMessage());
		else error(msg);
		return false;
	    }
	}

	protected boolean renameElem(Element e, String n, String msg) {
	    top.removeElement(e);
	    e.setName(n);
	    try {
		top.addElement(e);
		return true;
	    }
	    catch (TopologyException te) {
		if ( msg == null ) error(te.getMessage());
		else error(msg);
		return false;
	    }
	}

	public void actionPerformed(ActionEvent ae) {
	    String n = JOptionPane.showInputDialog(vv, "New Name");
	    if (n == null ) return;
	    if ( v instanceof Substrate) {
		Substrate s = (Substrate) v;
		String origName = s.getName();
		if ( !renameSub(s, n, null))
		    renameSub(s, origName,
			    "Internal error: failed to restore old name");
	    }
	    else if ( v instanceof Element) {
		Element e = (Element) v;
		String origName = e.getName();
		if ( !renameElem(e, n, null))
		    renameElem(e, origName,
			    "Internal error: failed to restore old name");
	    }
	    updateTopologyGraph();
	    vv.repaint();
	}
    }

    /**
     * This class deletes a vertex
     */
    protected class DeleteVertex extends AbstractAction {
	private ConnectedObject v;

	public DeleteVertex(String kind, ConnectedObject vv) {
	    super("Delete " + kind);
	    v = vv;
	}
	public void actionPerformed(ActionEvent ae) {
	    PickedState<ConnectedObject> pickedVertexState =
		vv.getPickedVertexState();
	    Graph<ConnectedObject,Interface> graph = layout.getGraph();
	    pickedVertexState.pick(v, false);
	    graph.removeVertex(v);
	    vv.repaint();
	}
    }

    /**
     * This class deletes an edge
     */
    protected class DeleteEdge extends AbstractAction {
	private Interface i;

	public DeleteEdge(Interface e) {
	    super("Delete Interface");
	    i = e;
	}
	public void actionPerformed(ActionEvent ae) {
	    PickedState<Interface> pickedEdgeState = vv.getPickedEdgeState();
	    Graph<ConnectedObject,Interface> graph = layout.getGraph();
	    pickedEdgeState.pick(i, false);
	    graph.removeEdge(i);
	    vv.repaint();
	}
    }

    /**
     * This class adds a Computer element at the given point
     */
    protected class AddComputer extends AbstractAction {
	private Point2D p;

	public AddComputer(Point2D pp) { super("Add Computer"); p = pp; }

	public void actionPerformed(ActionEvent ae) {
	    Graph<ConnectedObject,Interface> graph = layout.getGraph();
	    RenderContext<ConnectedObject, Interface> rc =
		vv.getRenderContext();
	    MultiLayerTransformer trans = rc.getMultiLayerTransformer();
	    Computer nv = new Computer();

	    graph.addVertex(nv);
	    layout.setLocation(nv, trans.inverseTransform(p));
	    vv.repaint();
	}
    }

    /**
     * This class adds a Substrate element at the given point
     */
    protected class AddSubstrate extends AbstractAction {
	private Point2D p;

	public AddSubstrate(Point2D pp) { super("Add Substrate"); p = pp; }

	public void actionPerformed(ActionEvent ae) {
	    Graph<ConnectedObject,Interface> graph = layout.getGraph();
	    RenderContext<ConnectedObject, Interface> rc =
		vv.getRenderContext();
	    MultiLayerTransformer trans = rc.getMultiLayerTransformer();
	    Substrate nv = new Substrate();

	    graph.addVertex(nv);
	    layout.setLocation(nv, trans.inverseTransform(p));
	    vv.repaint();
	}
    }
    /**
     * This class expands a region into its given fragment.
     */
    protected class ExpandRegion extends AbstractAction {
	private Region r;

	public ExpandRegion(String name, Region rr) { 
	    super(name);
	    r = rr ;
	}

	public void actionPerformed(ActionEvent ae) {
	    String fname =r.getFragmentName();

	    if ( fname == null ) {
		JOptionPane.showMessageDialog(vv,
			"No fragment name in " + r.getName() + "!?",
			"Expansion Error",
			JOptionPane.ERROR_MESSAGE);
		return;
	    }
	    Fragment f = top.getFragment(r.getFragmentName());

	    if ( f == null ) {
		JOptionPane.showMessageDialog(vv,
			"Cannot find fragment " + fname + " for " +
			    r.getName(),
			"Expansion Error",
			JOptionPane.ERROR_MESSAGE);
		return;
	    }

	    String pathname = r.getAttribute("path");

	    if ( pathname == null ) pathname = "/" + r.getName();
	    else pathname = pathname + r.getName();

	    NameMap inMap = top.getNameMap(pathname);
	    NameMap outMap = new NameMap(pathname, null, null);

	    try {
		r.expand(f, inMap, top, outMap);
		if ( inMap != null ) 
		    top.removeNameMap(inMap);
		top.addNameMap(outMap);
		regions.put(r.getName(), r);
	    }
	    catch (TopologyException e) {
		JOptionPane.showMessageDialog(vv,
			"Error expanding fragment " + r.getName() + ":" +
			    e.getMessage(),
			"Expansion Error",
			JOptionPane.ERROR_MESSAGE);
		return;
	    }
	    updateTopologyGraph();
	    rescale();
	    vv.repaint();
	}
    }

    /**
     * This class collapses a region that has previously been expanded
     */
    protected class CollapseRegion extends AbstractAction {
	private Region r;

	public CollapseRegion(String name, Region rr) {
	    super(name);
	    r = rr; 
	}
	public void actionPerformed(ActionEvent e) {
	    try { 
		Util.collapseExistingRegion(top, r);
	    }
	    catch (TopologyException te) {
		JOptionPane.showMessageDialog(vv,
			"Error collapsing Region " + r.getName() + ": " +
			    te.getMessage(),
			"Expansion Error",
			JOptionPane.ERROR_MESSAGE);
		return;
	    }

	    updateTopologyGraph();
	    rescale();
	    vv.repaint();
	}
    }

    /**
     * This class makes a new region consisting of the selected elements
     */
    protected class PickedRegion extends AbstractAction {

	/**
	 * A region predicate that returns true if the given element is in the
	 * set of elements that initialized it.
	 */
	private class PickedPredicate implements Util.RegionPredicate {
	    private Set<Element> picked;
	    public PickedPredicate(Set<Element> p) {
		picked = p;
	    }

	    public boolean insideRegion(Element e) {
		return picked.contains(e);
	    }
	}

	/** The predicate that defines the region to create */
	private PickedPredicate pickedPred;

	/** 
	 * Initializer: transforms the set of ConnectedObjects into a set of
	 * Elements - the graph does not distinguish, but region utilities do.
	 * @param name the action name (e.g. for use in a menu)
	 * @param picked the set of picked vertices (ConnectedObjects)
	 */
	public PickedRegion(String name, Set<ConnectedObject> picked) {
	    super(name);
	    Set<Element> p = new HashSet<Element>();
	    for ( ConnectedObject o : picked)
		if ( o instanceof Element) p.add((Element) o);

	    pickedPred = new PickedPredicate(p);
	}

	/**
	 * Make a copy of the region that will be created and check if it
	 * contains regions at different levels of recursion or of it collapses
	 * elements generated by expanding other regions.  The first we could
	 * resolve messily by adding dummy regions, but the second can make it
	 * impossible to collapse back to the original correctly.  If the
	 * elements to collapse are from different regions, we offer the user
	 * the chance to clear all expanded regions in the graph.  If so, we do
	 * it.
	 * @return the level of all regions inside the proposed region
	 * @throws TopologyException if gathering the topology fails
	 */
	private int checkRegion() throws TopologyException {
	    int level = 0;
	    int regions = 0;
	    Topology t = Util.gatherRegionTopology(top, pickedPred);
	    boolean checkPaths = true;

	    for (Element e : t.getElements()) {
		if ( e instanceof Region ) {
		    Region ir = (Region) e;

		    if ( regions++ > 0 ) {
			if ( ir.getLevel() != level) {
			    JOptionPane.showMessageDialog(vv,
				    "Two regions in the picked items " +
				    "with different levels.  Cannot " +
				    "create region containing both",
				    "Create Region Error",
				    JOptionPane.ERROR_MESSAGE);
			    return -1;
			}
		    }
		    else level = ir.getLevel();
		}

		String path = e.getAttribute("path");
		if ( path != null && !path.equals("/")) {
		    int opt = JOptionPane.showConfirmDialog(
			    vv, "Trying to make a region from nodes " +
			    "contained in other regions.  " +
			    "Clear other regions?",
			    "Remove existing regions?",
			    JOptionPane.YES_NO_CANCEL_OPTION ,
			    JOptionPane.QUESTION_MESSAGE);
		    if ( opt != JOptionPane.YES_OPTION)
			return -1;
		    for (Element ee : t.getElements())
			ee.removeAttribute("path");
		    for (Element ee : top.getElements())
			ee.removeAttribute("path");
		    JOptionPane.showMessageDialog(vv,
			    "All paths cleared.  Topology is flat.",
			    "Paths cleared",
			    JOptionPane.INFORMATION_MESSAGE);
		}
	    }
	    return level;
	}

	/**
	 * Try to create the region.  Pick a new fragment name, scan the
	 * proposed region for acceptability - which can result in flattening
	 * of other regions - and create it if possible.
	 * @param ae ignored
	 */
	public void actionPerformed(ActionEvent ae) { 
	    String fPat = "frag-%d";
	    Set<String> used = new HashSet<String>();
	    int i = 0;
	    String fName = String.format(fPat, i++);

	    for (Fragment f: top.getFragments())
		used.add(f.getName());

	    while ( used.contains(fName))
		fName = String.format(fPat, i++);

	    try {
		int level = checkRegion();

		if (level == -1 ) return;
		Util.NewRegion rv = Util.collapseNewRegion(top,
			null, level+1, fName, pickedPred);
		top.addFragment(rv.getFragment());
	    }
	    catch (TopologyException e) {
		JOptionPane.showMessageDialog(vv,
			"Unexpected topology error: " + e.getMessage(), 
			"Create Region Error",
			JOptionPane.ERROR_MESSAGE);
	    }
	    updateTopologyGraph();
	    rescale();
	    vv.repaint();
	}
    }

    /**
     * This class listens for events generated by changes to the graph through
     * the GUI and cleans them up or finishes them.
     */
    protected class ChangeHandler implements 
	GraphEventListener<ConnectedObject, Interface> {
	/**
	 * A return value from parsing a valid edge into an element substrate
	 * pair.  Very simple, direct access.
	 */
	protected class IfEdge {
	    public Element e;
	    public Substrate s;
	    public IfEdge(Element ee, Substrate ss) {
		e = ee; s = ss;
	    }
	}

	/**
	 * Turn a new edge into the Element, Substrate pair that all valid
	 * edges consist of.  Invalid edges will be reomved.
	 * @param i the edge (in g)
	 * @return the ordered pair
	 */
	protected IfEdge edgeToIfEdge(Interface i) {
	    Pair<ConnectedObject> p = g.getEndpoints(i);
	    ConnectedObject f = p.getFirst();
	    ConnectedObject s = p.getSecond();

	    if ( f instanceof Element && s instanceof Substrate)  {
		return new IfEdge((Element) f, (Substrate) s);
	    }
	    else if ( s instanceof Element && f instanceof Substrate)  {
		return new IfEdge((Element) s, (Substrate) f);
	    }
	    else return null;
	}


	/**
	 * Respond to changes in the observable graph - the one manipulated by
	 * the VisualizationServer - validate changes and reflect them into the
	 * topology.
	 */
	public void handleGraphEvent(
		GraphEvent<ConnectedObject, Interface> e) {

	    // This is a lot of words to convert the event to its subtype, if
	    // any.
	    GraphEvent.Vertex<ConnectedObject, Interface> ve = 
		(e instanceof GraphEvent.Vertex) ?
		    (GraphEvent.Vertex<ConnectedObject, Interface>) e :
		    null;
	    GraphEvent.Edge<ConnectedObject, Interface> ee = 
		(e instanceof GraphEvent.Edge) ?
		    (GraphEvent.Edge<ConnectedObject, Interface>) e :
		    null;

	    if ( e.getType() == GraphEvent.Type.VERTEX_ADDED ) {
		ConnectedObject oo = ve.getVertex();
		// Add to the topology
		try {
		    if ( oo instanceof Substrate)
			top.addSubstrate((Substrate) oo);
		    else if ( oo instanceof Element)
			top.addElement((Element) oo);
		}
		catch (TopologyException te) {
		    System.err.println("Unexpected exception adding vertex" +
			    te.getMessage());
		}
	    }
	    else if ( e.getType() == GraphEvent.Type.VERTEX_REMOVED ) {
		ConnectedObject oo = ve.getVertex();
		// Pull it out of the topology
		if ( oo instanceof Substrate)
		    top.removeSubstrate((Substrate) oo);
		else if ( oo instanceof Element) 
		    top.removeElement((Element) oo);
	    }
	    else if ( e.getType() == GraphEvent.Type.EDGE_ADDED ) {
		Interface edge = ee.getEdge();
		IfEdge ie = edgeToIfEdge(edge);

		if ( ie == null ) {
		    JOptionPane.showMessageDialog(vv,
			    "Edges must connect Elements and Substrates",
			    "Error", JOptionPane.ERROR_MESSAGE);
		    g.removeEdge(edge);
		    vv.repaint();
		    return;
		}
		// Valid edge, connect it.  This generates a name.
		edge.connect(ie.e, ie.s);
	    }
	    else if ( e.getType() == GraphEvent.Type.EDGE_REMOVED ) {
		Interface edge = ee.getEdge();

		// Disconnect us
		edge.disconnectAll();
	    }
	    else {
		System.err.println("That was unexpected " + e);
	    }
	}
    }

    /**
     * Create the panel to display the topology - use a computationally
     * intensive layout to arrive at an initial position for the vertices, then
     * map them into a static layout for display.  Set up the various
     * connections for dynamic scaling on window resize.
     */
    public TopologyPanel(TopologyDescription t) {
	// When we attach the visualization server, the GridLayout will keep it
	// taking up virtually all the space in the panel.
	super(new GridLayout(1,1));
	top = t;
	g = new UndirectedSparseMultigraph<ConnectedObject, Interface>();
	regions = new HashMap<String, Region>();
	updateTopologyGraph();
	ObservableGraph<ConnectedObject, Interface> og = 
	    new ObservableGraph<ConnectedObject, Interface>(g);


	layoutSize = new Dimension(preferredSize);
	oldCenter = new Point2D.Double(preferredSize.getWidth()/2, 
		preferredSize.getHeight()/2);
	setShapes();
	layout = layoutGraph(og);
	layout.setSize(layoutSize);

	// Initialize the viewer
	vv = new VisualizationViewer<ConnectedObject, Interface>(layout);

	RenderContext<ConnectedObject, Interface> rc = vv.getRenderContext();
	Renderer<ConnectedObject, Interface> rd = vv.getRenderer();

	rc.setVertexFillPaintTransformer(
		new VertexPaint(vv.getPickedVertexState()));
	rc.setVertexLabelTransformer(new VertexLabel());
	rc.setVertexShapeTransformer(new VertexShape());
	rd.getVertexLabelRenderer().setPosition(Position.CNTR);
	vv.setPreferredSize(preferredSize);
	vv.addComponentListener( new ComponentAdapter() {
	    public void componentResized(ComponentEvent ignored) { rescale(); }
	});
	vv.setGraphMouse(new Mouse(new VertexFactory(),new InterfaceFactory()));
	vv.setVertexToolTipTransformer(new VertexToolTip());
	og.addGraphEventListener(new ChangeHandler());
	add(vv);
	validate();
    }

    /**
     * Return the topology
     */
    public TopologyDescription getTopology() { return top; }
}
