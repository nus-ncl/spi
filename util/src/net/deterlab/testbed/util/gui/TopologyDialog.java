package net.deterlab.testbed.util.gui;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import javax.swing.border.Border;

import net.deterlab.testbed.topology.TopologyDescription;

/**
 * A to view and edit a topology
 * @author DETER Team
 * @version 1.0
 */
public class TopologyDialog extends UtilityDialog {
    /** The topology widget */
    protected TopologyPanel tp ;

    /**
     * Create a modal password collecting dialog.  
     */
    public TopologyDialog(TopologyDescription t) {
	super("Edit Topology");
	JPanel p = (JPanel) getContentPane();
	tp = new TopologyPanel(t);
	p.add(tp, BorderLayout.CENTER);
	setContentPane(p);
	pack();
    }
    /**
     * Return the topology
     * @return the topology
     */
    public TopologyDescription getTopology() { return tp.getTopology(); }
}
