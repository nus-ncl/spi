package net.deterlab.testbed.util.gui;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import javax.swing.border.Border;

/**
 * A dialog to collect a password.
 * @author DETER Team
 * @version 1.0
 */
public class LoginDialog extends UtilityDialog {
    /** The password widget */
    protected JPasswordField pw;
    /** The text entered into the dialog */
    protected char[] entered;

    /**
     * Create a modal password collecting dialog.  
     */
    public LoginDialog() {
	super("Password");
	JPanel p = (JPanel) getContentPane();
	entered = null;
	pw = new JPasswordField(20);
	pw.setBorder(BorderFactory.createTitledBorder(
		BorderFactory.createLoweredBevelBorder(),"Password"));
	// When the password is entered, set the cancellation flag false and
	// close this dialog.
	pw.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		setCancelled(false);
		setVisible(false);
	    }
	});
	p.add(pw, BorderLayout.CENTER);
	setContentPane(p);
	pack();
    }
    /**
     * Return the password entered, or null if cancelled.
     * @return the password entered, or null if cancelled.
     */
    public char[] getPassword() { return pw.getPassword(); }

    /**
     * Clear out the password field.
     */
    public void clearPassword() { pw.setText(null); }

    /**
     * Simple regression test.  Pop up the dialog and print the results
     * @param args ignored
     */
    static public void main(String[] args) {
	LoginDialog ld = new LoginDialog();
	char[] pwd = null;

	ld.setVisible(true);
	if ( (pwd = ld.getPassword()) == null ) 
	    System.out.println("Cancelled");
	else {
	    System.out.print("Entered: ");
	    System.out.println(pwd );
	}
    }
}
