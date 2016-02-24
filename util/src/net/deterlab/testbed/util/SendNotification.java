package net.deterlab.testbed.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.NotificationFlag;
import net.deterlab.testbed.api.UserNotification;

import net.deterlab.testbed.client.UsersDeterFault;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.gui.UtilityDialog;

import org.apache.axis2.AxisFault;

/**
 * Send a notification to a list of users.
 * @author DETER Team
 * @version 1.0
 */
public class SendNotification extends Utility {

    public static class NotificationDialog extends UtilityDialog {
	JTextField users;
	JCheckBox read;
	JCheckBox urgent;
	JTextArea body;

	public NotificationDialog() {
	    super("Test");
	    JPanel content = (JPanel) getContentPane();
	    JPanel p = new JPanel();
	    JPanel boxes = new JPanel(new FlowLayout());

	    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
	    users = new JTextField(50);
	    users.setBorder(BorderFactory.createTitledBorder(
		BorderFactory.createLoweredBevelBorder(),"Recipients"));
	    p.add(users);

	    body = new JTextArea(10, 50);
	    body.setBorder(BorderFactory.createTitledBorder(
		BorderFactory.createLoweredBevelBorder(),"Body"));
	    p.add(body);

	    read = new JCheckBox("Read");
	    boxes.add(read);
	    urgent = new JCheckBox("Urgent");
	    boxes.add(urgent);
	    p.add(boxes);

	    content.add(p, BorderLayout.CENTER);
	    setContentPane(content);
	    pack();
	}

	public String getRecipients() { return users.getText(); }
	public String getBody() { return body.getText(); }
	public boolean getUrgent() { return urgent.isSelected(); }
	public boolean getRead() { return read.isSelected(); }
    }

    /**
     * Do the profile editing.  Call getProfileDescription to get all valid
     * attributes, then pop up a dialog and allow editing.  Make sure that the
     * required fields are initialized and send the request
     * @param args the uid to create is the first parameter.
     */
    static public void main(String[] args) {
	try {

	    // Set trusted certificates.
	    loadTrust();
	    loadID();
	    NotificationDialog d = new NotificationDialog();

	    d.setVisible(true);

	    if (d.isCancelled()) {
		d.dispose();
		return;
	    }

	    String allUsers = d.getRecipients();
	    String[] users;
	    String body = d.getBody();
	    ArrayList<UsersStub.NotificationFlag> flags = 
		new ArrayList<UsersStub.NotificationFlag>();

	    if (d.getUrgent()) {
		UsersStub.NotificationFlag nf = 
		    new UsersStub.NotificationFlag();
		nf.setTag(NotificationFlag.URGENT_TAG);
		nf.setIsSet(true);
		flags.add(nf);
	    }
	    if (d.getRead()) {
		UsersStub.NotificationFlag nf = 
		    new UsersStub.NotificationFlag();
		nf.setTag(NotificationFlag.READ_TAG);
		nf.setIsSet(true);
		flags.add(nf);
	    }

	    d.dispose();

	    if ( allUsers == null ) 
		fatal("No recipients");
	    users = allUsers.split("\\s*,\\s*");

	    if (users == null || users.length == 0 ) 
		fatal("No recipients");

	    if ( body == null || body.length() == 0) 
		fatal("No notification text");

	    UsersStub stub = new UsersStub(getServiceUrl() + "Users");
	    UsersStub.SendNotification req = new UsersStub.SendNotification();

	    req.setUsers(users);
	    req.setText(body);
	    req.setFlags(flags.toArray(new UsersStub.NotificationFlag[0]));

	    UsersStub.SendNotificationResponse resp = 
		stub.sendNotification(req);

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
