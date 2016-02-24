package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.deterlab.testbed.api.ApiObject;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.api.NotificationFlag;
import net.deterlab.testbed.api.UserNotification;

import net.deterlab.testbed.client.UsersStub;

import org.apache.axis2.AxisFault;

/**
 * Regression to test the Notification system.
 * @author DETER team
 * @version 1.0
 */
public class Notifications extends RegressionTest {
    /**
     * Create a new UserLogin regression test
     */
    public Notifications() { super(); }

    /**
     * Get the test name.
     * @return the test name
     */
    public String getName() { return "Notifications"; }

    /**
     * Print a description of this test, enclosed in XML comments, to the given
     * PrintStream.
     * @param p the destination stream
     */
    protected void summary(PrintStream p) {
	p.print("<!-- \n" +
	    "This test calls sendNotification, markNotifications, and\n"+
	    "getNotifications from the Users service.  First\n"+
	    "sendNotifications parameters are checked, missing uid, missing\n"+
	    "body.  Then a good notification is sent to users test and\n"+
	    "test0.  Then 3 notifications are added a few seconds apart to\n"+
	    "test the time-based filtering later.  Then getNotifications is\n"+
	    "called without a uid.  Then the time-based filtering is tested\n"+
	    "with from and to nulled, just from, both from and to, and\n"+
	    "finally just to.  Then markNotifications is tested for\n"+
	    "parameter checking (null uid, null ids) and for marking.\n"+
	    "Notifications after the \"from\" time are marked as read and\n"+
	    "then a call to getNotifications for unread messages confirms\n"+
	    "that they are not returned.  Finally the mask parameter to\n"+
	    "markNotifications is tested by asking to mark the messages as\n"+
	    "read with the wrong mask.\n"+
	    "-->\n");
    }
    /**
     * Run the test
     * @param trace the XML SOAP tracefile
     * @param dataDir a directory containing supporting data for the test
     * @throws RegressionException if the test fails
     */
    public void runTest(File trace, File dataDir) throws RegressionException {
	UsersStub uStub = null;
	try {
	    uStub = new UsersStub(getServiceUrl() + "Users");
	}
	catch (AxisFault e) { 
	    failed("Could not access service!?");
	}
	// Set up to serialize the XML
	SerializeEnvelope s = logSOAP(uStub, trace);
	if ( s== null) failed("Could not trace to: " + trace);
	PrintStream p = s.getStream();
	summary(p);

	regressionLogin(uStub, "testadmin", "test", p);
	sendNotification(uStub, null, "This is a failing notification",
		new UsersStub.NotificationFlag[0], 
		"no users", p, false, DeterFault.request);

	sendNotification(uStub, new String[] { "test", "test0" }, 
		null, 
		new UsersStub.NotificationFlag[0], 
		"no body", p, false, DeterFault.request);

	regressionLogin(uStub, "testnotadmin", "test", p);
	sendNotification(uStub, new String[] { "test", "test0" }, 
		"This is a notification", 
		new UsersStub.NotificationFlag[0], 
		"bad permissions", p,
		false, DeterFault.access);

	regressionLogout(uStub, p);
	sendNotification(uStub, new String[] { "test", "test0" }, 
		"This is a notification", 
		new UsersStub.NotificationFlag[0], 
		"not logged in", p,
		false, DeterFault.login);

	regressionLogin(uStub, "testadmin", "test", p);
	sendNotification(uStub, new String[] { "test", "test0" }, 
		"This is a notification", 
		new UsersStub.NotificationFlag[0], 
		"good", p, true,
		DeterFault.none);

	p.println("<!-- set up an array of notifications to test reading -->");
	
	sendNotification(uStub, new String[] { "test", "test0" }, 
		"Early", 
		new UsersStub.NotificationFlag[0], 
		"early", p, true, DeterFault.none);

	try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
	String from = ApiObject.dateToString(new Date());
	try { Thread.sleep(1500); } catch (InterruptedException ignored) { }

	sendNotification(uStub, new String[] { "test", "test0" }, 
		"Middle", 
		new UsersStub.NotificationFlag[0],
		"middle", p, true, DeterFault.none);

	try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
	String to = ApiObject.dateToString(new Date());
	try { Thread.sleep(1500); } catch (InterruptedException ignored) { }

	sendNotification(uStub, new String[] { "test", "test0" }, 
		"Late", 
		new UsersStub.NotificationFlag[0],
		"late", p, true, DeterFault.none);

	regressionLogin(uStub, "test", "test", p);
	getNotifications(uStub, null, null, null, 
		new UsersStub.NotificationFlag[0], "no uid", 
		p, false, DeterFault.request);

	getNotifications(uStub, "admin0", null, null, 
		new UsersStub.NotificationFlag[0],
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	getNotifications(uStub, "test", null, null, 
		new UsersStub.NotificationFlag[0],
		"not logged in", p, false, DeterFault.login);

	regressionLogin(uStub, "test", "test", p);
	UsersStub.UserNotification[] un = 
	    getNotifications(uStub, "test", null, null, 
		    new UsersStub.NotificationFlag[0],
		    "all", p, true, DeterFault.none);

	if ( un == null || un.length != 4 ) 
	    failed("Wrong number of notifications");

	un = getNotifications(uStub, "test", from, null, 
		new UsersStub.NotificationFlag[0],
		"from",
		p, true, DeterFault.none);

	if ( un == null || un.length != 2 ) 
	    failed("Wrong number of notifications");

	// Collect the ids of the messages sent after "from"
	long[] newMsgs = new long[un.length];
	for (int i = 0; i < un.length; i++) 
	    newMsgs[i] = un[i].getId();

	un = getNotifications(uStub, "test", from, to, 
		new UsersStub.NotificationFlag[0],
		"from and to", p, true, DeterFault.none);

	if ( un == null || un.length != 1 ) 
	    failed("Wrong number of notifications");

	un = getNotifications(uStub, "test", null, to, 
		new UsersStub.NotificationFlag[0],
		"to", p, true, DeterFault.none);

	if ( un == null || un.length != 3 ) 
	    failed("Wrong number of notifications");

	// Collect the ids of the messages sent before and "to"
	long[] oldMsgs = new long[un.length];
	for (int i = 0; i < un.length; i++) 
	    oldMsgs[i] = un[i].getId();


	markNotifications(uStub, null, newMsgs, 
		new UsersStub.NotificationFlag[0],
		"no uids",
		p, false, DeterFault.request);
	markNotifications(uStub, "test", null, 
		new UsersStub.NotificationFlag[0],
		"no notification ids", p, false, DeterFault.request);
	markNotifications(uStub, "admin0", newMsgs, 
		new UsersStub.NotificationFlag[0],
		"bad permissions", p, false, DeterFault.access);

	regressionLogout(uStub, p);
	markNotifications(uStub, "test", newMsgs, 
		new UsersStub.NotificationFlag[0],
		"not logged in", p, false, DeterFault.login);

	// Mark newMsgs as read
	regressionLogin(uStub, "test", "test", p);
	ArrayList<UsersStub.NotificationFlag> flags = 
	    new ArrayList<UsersStub.NotificationFlag>();
	UsersStub.NotificationFlag f = new UsersStub.NotificationFlag();
	f.setTag(NotificationFlag.READ_TAG);
	f.setIsSet(true);
	flags.add(f);

	markNotifications(uStub, "test", newMsgs, 
		flags.toArray(new UsersStub.NotificationFlag[0]),
		"after from", p, true, DeterFault.none);

	f.setIsSet(false);
	un = getNotifications(uStub, "test", null, null, 
		flags.toArray(new UsersStub.NotificationFlag[0]),
		"all unread", p, 
		true, DeterFault.none);
	if ( un == null || un.length != 2 ) 
	    failed("Wrong number of notifications");

	regressionLogout(uStub, p);
    }
}
