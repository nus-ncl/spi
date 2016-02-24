package net.deterlab.testbed.util.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.border.Border;

import javax.swing.table.AbstractTableModel;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.client.UsersStub;

/**
 * Dialog that presents a user profile (from a client) and allows simple
 * editing of writable fields.  It keeps track of changes and handles
 * cancellation.
 * @author DETER Team
 * @version 1.0
 */
public class EditProfileDialog extends UtilityDialog {
    /** The table to edit */
    protected AttributeTable t;
    /** 
     * Construct the dialog.
     * @param uid the user being edited (used as a title)
     * @param a the table of profile attributes
     */
    public EditProfileDialog(String uid, Object a) {
	super(uid);
	t = new AttributeTable(a);

	JPanel p = (JPanel) getContentPane();
	JTable tbl = new JTable(t);
	JScrollPane sp = new JScrollPane(tbl);

	p.add(sp, BorderLayout.CENTER);
	setContentPane(p);
	pack();
    }
    /**
     * Return a set of attribute names whose value was changed.
     * @return a set of attribute names whose value was changed.
     */
    public Set<String> getChanged() { return t.getChanged(); }
}
