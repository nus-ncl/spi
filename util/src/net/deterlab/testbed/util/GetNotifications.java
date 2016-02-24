package net.deterlab.testbed.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

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
import net.deterlab.testbed.api.NotificationFlag;
import net.deterlab.testbed.api.UserNotification;

import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.gui.UtilityDialog;

import net.deterlab.testbed.util.option.BooleanOption;
import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

import org.apache.axis2.AxisFault;

import org.apache.log4j.Level;

/**
 * Edit a user profile.  Pop up a dialog that displays the user's profile and
 * allow editing of fields that can be edited.  When OK is pressed, submit the
 * changes and report success.
 * @author DETER Team
 * @version 1.0
 */
public class GetNotifications extends Utility {
    /**
     * A table model that displays a table of Notifications.  These methods
     * provide the information to display the underlying array in a JTable.
     * @author DETER Team
     * @version 1.0
     */
    static protected class NotificationTable extends AbstractTableModel {
	/** The notifications to disiplay */
	protected UsersStub.UserNotification[] notes;
	/** changed entries */
	boolean[] changed;
	/** The names of the columns displayed */
	static protected String[] columnNames = new String[] {
	    "ID", "Text", "Sent", "Urgent", "Read" };

	/**
	 * Constructor
	 * @param a the underlying array of notifications
	 */
	public NotificationTable(UsersStub.UserNotification[] n) {
	    notes = n;
	    changed = new boolean[notes.length];
	}
	/**
	 * Return the number of rows
	 * @return the number of rows
	 */
	public int getRowCount() { 
	    return (notes != null) ? notes.length : 0;
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
	public int getColumnCount() { return columnNames.length; }

	/**
	 * Internal routine to get the read and urgent flags
	 * @param n the notification to check
	 * @param tag the tag to check for
	 * @return true if the flag is set
	 */
	private boolean checkFlag(UsersStub.UserNotification n, String tag) {
	    if ( tag == null ) return false;
	    for (UsersStub.NotificationFlag f : n.getFlags())
		if ( tag.equals(f.getTag())) return f.getIsSet();
	    return false;
	}
	/**
	 * Return the value at row r column c.  This translates the
	 * notifications into objects.
	 * @param r the row to get
	 * @param c the column to get
	 * @return the value at row r column c.
	 */
	public Object getValueAt(int r, int c) {
	    UsersStub.UserNotification n = notes[r];
	    switch (c) {
		case 0:
		    return n.getId();
		case 1:
		    return n.getBody();
		case 2:
		    return n.getSent();
		case 3:
		    return Boolean.valueOf(
			    checkFlag(n, NotificationFlag.URGENT_TAG));
		case 4:
		    return Boolean.valueOf(
			    checkFlag(n, NotificationFlag.READ_TAG));
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
		    return Long.class;
		case 1:
		case 2:
		default:
		    return String.class;
		case 3:
		case 4:
		    return Boolean.class;
	    }
	}

	/**
	 * Return true if the column can be edited - in this case flags
	 * @param r the row
	 * @param c the column
	 * @return true if the cell can be edited - in this case flags
	 */
	public boolean isCellEditable(int r, int c) {
	    return c == 3 || c == 4;
	}

	/**
	 * Set the given flag in the notification.  If that flag somehow does
	 * not exist in the flag list, this is a no-nop.
	 * @param n the notification to modify
	 * @param tag the tag of the flag to change
	 * @param v the new flag value
	 */
	private void setFlag(UsersStub.UserNotification n, String tag, 
		boolean v) {
	    if ( n == null || tag == null ) return;
	    for (UsersStub.NotificationFlag f : n.getFlags())
		if (tag.equals(f.getTag())) {
		    f.setIsSet(v);
		    return;
		}
	}

	/**
	 * Set a new value for the editible columns
	 * @param v the new value
	 * @param r the row
	 * @param c the column
	 */
	public void setValueAt(Object v, int r, int c) {
	    if (!isCellEditable(r, c) ) return;
	    UsersStub.UserNotification n = notes[r];
	    Boolean bv = (Boolean) v;
	    String tag = null;

	    if ( c == 3 ) tag = NotificationFlag.URGENT_TAG;
	    else if ( c == 4 ) tag = NotificationFlag.READ_TAG;
	    changed[r] = true;

	    setFlag(n, tag, bv.booleanValue());
	}

	/**
	 * Return a mask of the notifications that have changed.
	 * @return a mask of the notifications that have changed
	 */
	boolean[] getChanged() { return changed; }
    }
    /**
     * The dialog used to display and edit notifications.
     * @author DETER team
     * @version 1.0
     */
    static protected class NotificationDialog extends UtilityDialog  {
	/** The table to edit */
	protected NotificationTable t;
	/** 
	 * Construct the dialog.
	 * @param uid the user being edited (used as a title)
	 * @param n the table of notification
	 */
	public NotificationDialog(String uid, UsersStub.UserNotification[] n) {
	    super(uid);
	    t = new NotificationTable(n);

	    JPanel p = (JPanel) getContentPane();
	    JTable tbl = new JTable(t);
	    JScrollPane sp = new JScrollPane(tbl);
	    p.add(sp, BorderLayout.CENTER);
	    setContentPane(p);
	    pack();
	}

	/**
	 * Return a mask of the notifications that have changed.
	 * @return a mask of the notifications that have changed
	 */
	public boolean[] getChanged() { return t.getChanged(); }
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then get the specific user profile using getUserProfile.
     * Pop up a dialog and allow editing.  If changes were made, call
     * changeUserProfile with those changes.  Pop another dialog to let the
     * user know what succeeded and what failed.
     * @param args the uid to edit is the first parameter.
     */
    static public void main(String[] args) {
	try {
	    ParamOption from = new ParamOption("from");
	    ParamOption to = new ParamOption("to");
	    BooleanOption urgent = new BooleanOption("urgent");
	    BooleanOption read = new BooleanOption("read");
	    List<String> pos = new ArrayList<String>();

	    try {
		Option.parseArgs(args, new Option[] { from, to, urgent, read },
			pos);
	    }
	    catch (Option.OptionException e) {
		fatal(e.getMessage());
	    }

	    if (pos.size() < 1 ) 
		fatal("Usage GetNotifications [options] uid");

	    // Set our ID and trusted certificates.
	    loadID();
	    loadTrust();


	    String uid = args[0];   // nicer than typing args[0] everywhere

	    ArrayList<UsersStub.NotificationFlag> flags = 
		new ArrayList<UsersStub.NotificationFlag>();
	    UsersStub stub = new UsersStub(getServiceUrl("Users"));
	    UsersStub.GetNotifications req = 
		new UsersStub.GetNotifications();

	    req.setUid(uid);
	    // Assemble the request from the Options
	    req.setFrom(from.getValue());
	    req.setTo(to.getValue());
	    if (urgent.getValue() != null ) {
		UsersStub.NotificationFlag nf = 
		    new UsersStub.NotificationFlag();
		nf.setTag(NotificationFlag.URGENT_TAG);
		nf.setIsSet(urgent.getValue().booleanValue());
		flags.add(nf);
	    }
	    if (read.getValue() != null ) {
		UsersStub.NotificationFlag nf = 
		    new UsersStub.NotificationFlag();
		nf.setTag(NotificationFlag.READ_TAG);
		nf.setIsSet(read.getValue().booleanValue());
		flags.add(nf);
	    }
	    req.setFlags(flags.toArray(new UsersStub.NotificationFlag[0]));

	    UsersStub.GetNotificationsResponse resp = 
		stub.getNotifications(req);
	    UsersStub.UserNotification[] un = resp.get_return();
	    if ( un == null ) {
		System.err.println("No notifications");
		System.exit(0);
	    }
	    NotificationDialog d = new NotificationDialog(uid, un);
	    d.setVisible(true);

	    if ( d.isCancelled()) {
		d.dispose();
		return;
	    }

	    /* One could be more thorough, but for each changed flag
	     * configuration, this loop collects the notifications changed to
	     * that flag value and calls markNotifications with an all 1's
	     * mask.  If there were more than 2 flags this would be grossly
	     * prohibitive.
	     */
	    boolean[] changed = d.getChanged();
	    d.dispose();

	    for (int i = 0; i < un.length; i++) {
		if ( !changed[i]) continue;
		UsersStub.MarkNotifications markReq = 
		    new UsersStub.MarkNotifications();

		markReq.setUid(uid);
		markReq.setIds(new long[] { un[i].getId() });
		markReq.setFlags(un[i].getFlags());
		stub.markNotifications(markReq);
	    }
	} catch (UsersDeterFault e) {
	    DeterFault df = getDeterFault(e);
	    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	} catch (AxisFault e) {
	    handleAxisFault(e);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
