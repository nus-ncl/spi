package net.deterlab.testbed.util.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Generic dialog that puts OK cancel buttons up and handles their operation.
 * @author DETER Team
 * @version 1.0
 */
public class UtilityDialog extends JDialog {
    /** True if this is cancelled */
    private boolean cancelled;
    /** 
     * Construct the dialog.  It makes the content pane a BorderLayout defined
     * panel and uses the BorderLayout.SOUTH area to hold the OK/cancel
     * buttons.  Sub-classes should get that content pane and add their
     * features.
     * @param title the title
     */
    public UtilityDialog(String title) {
	super((JFrame) null, title, true);
	cancelled = true;

	JPanel p = new JPanel(new BorderLayout());
	JPanel buttonBox = new JPanel(new FlowLayout());
	// An anonymous listener to handle button presses.  We only need one
	// for both buttons. Set cancelled to true only if the OK button is
	// used.
	ActionListener al = new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		Object src = e.getSource();

		setVisible(false);
		if (cmd != null && cmd.equals("OK")) cancelled = false;
	    }
	};

	// Add OK and Cancel buttons
	for ( String label : new String[] { "OK", "Cancel" }) {
	    JButton b = new JButton(label);
	    b.addActionListener(al);
	    buttonBox.add(b);
	}
	// Clear cancelled member every time the dialog is shown.
	addComponentListener(new ComponentAdapter() {
	    public void componentShown(ComponentEvent e) { 
		cancelled = true;
	    }
	});
	p.add(buttonBox, BorderLayout.SOUTH);
	setContentPane(p);
	pack();
    }
    /**
     * Return true if the dialog was closed with anything but OK.
     * @return true if the dialog was closed with anything but OK.
     */
    public boolean isCancelled() { return cancelled; }

    /**
     * Set the cancelled flag to have the given value.
     * @param b the new cancelled value
     */
    public void setCancelled(boolean b) { cancelled = b; }
}
