package net.deterlab.testbed.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.CirclesDeterFault;
import net.deterlab.testbed.client.CirclesStub;

import net.deterlab.testbed.util.gui.UtilityDialog;
import net.deterlab.testbed.util.gui.EditProfileDialog;

import org.apache.axis2.AxisFault;

import org.apache.log4j.Level;

/**
 * Edit a circle profile.  Pop up a dialog that displays the circle's profile
 * and allow editing of fields that can be edited.  When OK is pressed, submit
 * the changes and report success.
 * @author DETER Team
 * @version 1.0
 */
public class EditCircleProfile extends Utility {
    /**
     * This Comparator sorts CirclesStub.Attributes by OrderingHint
     * @author DETER team
     * @version 1.0
     */
    static public class AttributeComparator implements 
	Comparator<CirclesStub.Attribute> {
	    /**
	     * Constructor.
	     */
	    public AttributeComparator() { }
	    /**
	     * Compares its two arguments for order.
	     * @param a Attribute to compare 
	     * @param b Attribute to compare 
	     * @return a negative integer, zero, or a positive integer as the
	     * first argument is less than, equal to, or greater than the
	     * second. 
	     */
	    public int compare(CirclesStub.Attribute a, 
		    CirclesStub.Attribute b) {
		return a.getOrderingHint() - b.getOrderingHint();
	    }
	    /**
	     * Indicates whether some other object is "equal to" this
	     * comparator. 
	     * @param o the object to test
	     * @return true if o refers to this
	     */
	    public boolean equals(Object o) {
		if ( o == null ) return false;
		if ( !(o instanceof AttributeComparator ) ) return false;
		AttributeComparator a = (AttributeComparator) o;
		return (this == o);
	    }
	}
    /**
     * A table model that displays a results from attempted changes.  These
     * methods provide the information to display the underlying array of
     * results in a JTable.
     * @author DETER Team
     * @version 1.0
     */
    static protected class ResultsTable extends AbstractTableModel {
	/** The results to disiplay */
	protected CirclesStub.ChangeResult[] results;
	/** The names of the columns displayed */
	static protected String[] columnNames = new String[] {
	    "Name", "Success", "Reason (on failure)" };

	/**
	 * Constructor
	 * @param a the underlying array of attributes
	 */
	public ResultsTable(CirclesStub.ChangeResult[] r) {
	    results = r;
	}
	/**
	 * Return the number of rows
	 * @return the number of rows
	 */
	public int getRowCount() { 
	    return (results != null) ? results.length : 0;
	}
	/**
	 * Return the name of column c
	 * @param c the index of the column
	 * @return the name of column c
	 */
	public String getColumnName(int c) { return columnNames[c]; }
	/**
	 * Return the number of rows
	 * @return the number of rows
	 */
	public int getColumnCount() { return 3; }
	/**
	 * Return the value at row r column c.  This translates the attribute
	 * into strings.
	 * @param r the row to get
	 * @param c the column to get
	 * @return the value at row r column c.
	 */
	public Object getValueAt(int r, int c) {
	    CirclesStub.ChangeResult rr = results[r];
	    switch (c) {
		case 0:
		    return rr.getName();
		case 1:
		    return rr.getSuccess();
		case 2:
		    return rr.getReason();
		default:
		    return null;
	    }
	}
	/**
	 * Return the class of data in column c
	 * @param c the column
	 * @return the class of data in column c
	 */
	public Class<?> getColumnClass(int c) {
	    switch (c) {
		case 0:
		case 2:
		default:
		    return String.class;
		case 1:
		    return Boolean.class;
	    }
	}
    }
    /**
     * The dialog used to display change results.
     * @author DETER team
     * @version 1.0
     */
    static protected class ResultsDialog extends UtilityDialog  {
	/** The table to edit */
	protected ResultsTable t;
	/** 
	 * Construct the dialog.
	 * @param circleid the user being edited (used as a title)
	 * @param a the table of change results
	 */
	public ResultsDialog(String circleid, 
		CirclesStub.ChangeResult[] a) {
	    super(circleid);
	    t = new ResultsTable(a);

	    JPanel p = (JPanel) getContentPane();
	    JTable tbl = new JTable(t);
	    JScrollPane sp = new JScrollPane(tbl);
	    p.add(sp, BorderLayout.CENTER);
	    setContentPane(p);
	    pack();
	}
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then get the specific user profile using getCircleProfile.
     * Pop up a dialog and allow editing.  If changes were made, call
     * changeCircleProfile with those changes.  Pop another dialog to let the
     * user know what succeeded and what failed.
     * @param args the circleid to edit is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    if (args.length < 1 ) 
		fatal("Usage EditCircleProfile circleid");

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();


	    // We copy the specific user's values into the array of attributes
	    // returned from GetProfileDescription.  This map lets us easliy
	    // put the values in teh right places.
	    Map<String, CirclesStub.Attribute> nameToAttr = 
		new HashMap<String, CirclesStub.Attribute>();
	    String circleid = args[0]; // nicer than typing args[0] everywhere

	    // The calls to remote procedures all take this form.
	    // * Create a request object
	    // * Fill in parameters to the request object
	    // * Initialize a response object with the result of the call
	    // * Extract and work with the data.
	    //
	    // This is the GetProfileDescription call
	    CirclesStub stub = new CirclesStub(getServiceUrl("Circles"));
	    CirclesStub.GetProfileDescription descReq = 
		new CirclesStub.GetProfileDescription();
	    CirclesStub.GetProfileDescriptionResponse descResp = 
		stub.getProfileDescription(descReq);
	    CirclesStub.Profile up = descResp.get_return();
	    CirclesStub.Attribute[] profile = up.getAttributes();
	    Arrays.sort(profile, new AttributeComparator());
	    // Initialize the nameToAttr Map
	    for (CirclesStub.Attribute a: profile) 
		nameToAttr.put(a.getName(), a);

	    // Call getCircleProfile on circleid
	    CirclesStub.GetCircleProfile req = 
		new CirclesStub.GetCircleProfile();
	    req.setCircleid(args[0]);

	    CirclesStub.GetCircleProfileResponse resp = 
		stub.getCircleProfile(req);

	    up = resp.get_return();
	    CirclesStub.Attribute[] attrs = up.getAttributes();

	    // Copy the values of circleid's profile into the profile array
	    // using nameToAttr
	    for (CirclesStub.Attribute a: attrs)
		nameToAttr.get(a.getName()).setValue(a.getValue());

	    // Edit profile array
	    EditProfileDialog d = new EditProfileDialog(circleid, profile);
	    d.setVisible(true);
	    if (d.isCancelled()) {
		d.dispose();
		return;
	    }

	    Set<String> changed = d.getChanged();
	    List<CirclesStub.ChangeAttribute> changes = 
		new ArrayList<CirclesStub.ChangeAttribute>();

	    d.dispose();
	    if (changed.size() == 0 ) {
		JOptionPane.showMessageDialog(null, "No changes made");
		return;
	    }

	    // Generate the change requests 
	    for ( String name: changed) {
		CirclesStub.ChangeAttribute ch = 
		    new CirclesStub.ChangeAttribute();
		String nv = nameToAttr.get(name).getValue();
		ch.setName(name);

		if ( nv != null && nv.length() > 0 ) {
		    ch.setValue(nameToAttr.get(name).getValue());
		    ch.setDelete(false);
		}
		else ch.setDelete(true);
		changes.add(ch);
	    }

	    // call change CircleProfile
	    CirclesStub.ChangeCircleProfile changeReq = 
		new CirclesStub.ChangeCircleProfile();
	    changeReq.setCircleid(circleid);
	    changeReq.setChanges(changes.toArray(
			new CirclesStub.ChangeAttribute[0]));

	    CirclesStub.ChangeCircleProfileResponse changeResp = 
		stub.changeCircleProfile(changeReq);
	    CirclesStub.ChangeResult[] results = changeResp.get_return();

	    // Display the results
	    ResultsDialog rd = new ResultsDialog(circleid, results);
	    rd.setVisible(true);
	    rd.dispose();

	} catch (CirclesDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
