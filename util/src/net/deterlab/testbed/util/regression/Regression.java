package net.deterlab.testbed.util.regression;

import java.io.File;
import java.io.PrintStream;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.client.ApiInfoStub;
import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

import net.deterlab.testbed.util.Utility;
import net.deterlab.testbed.util.gui.LoginDialog;

import org.apache.axis2.AxisFault;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Regression extends Utility {
    /** Logger to put output to the screen */
    static Logger log = null;

    /**
     * Log msg as ERROR and exit
     * @param msg the message to log
     */
    static public void fatal(String msg) {
	log.error(msg);
	System.exit(20);
    }

    /**
     * Initialize the package log to print to the console with a specific
     * format.  RegressionTests will inherit these settings.
     */
    static public void initLog() {
	log = Logger.getLogger("net.deterlab.testbed.util.regression");
	log.setLevel(Level.INFO);
	log.setAdditivity(false);
	log.removeAllAppenders();
	log.addAppender(new ConsoleAppender(
		    new EnhancedPatternLayout(
			"%d{MM/dd HH:mm:ss} %-5p %c{1}: %m%n")));
    }
    /**
     * Run the regression tests. 
     * First parameter is the output directory
     * @param args the command line arguments
     */
    static public void main(String[] args) {
	initLog();
	if ( args.length < 3 ) 
	    fatal("Usage: Regression output_directory first_prefix " +
		    "data_directory [admin user]");

	String dir = args[0];
	String ddir = args[2];
	int exitCode = 0;
	int n = 0;
	LoginDialog ld = new LoginDialog();
	try {
	    n = Integer.parseInt(args[1]);
	}
	catch (NumberFormatException e ) {
	    fatal("Bad n - not an integer");
	}
	RegressionTest[] tests = new RegressionTest[] {
	    new GetVersion(),
	    new AdminUser(),
	    new UserAttributes(),
	    new ProjectAttributes(),
	    new CircleAttributes(),
	    new ExperimentAttributes(),
	    new LibraryAttributes(),
	    new MakeUsersNoConfirm(),
	    new MakeUsers(),
	    new MakeProjects(),
	    new Notifications(),
	    new ManipulateProjectUsers(),
	    new MakeCircles(),
	    new ManipulateCircleUsers(),
	    new UserLogin(),
	    new ChangePassword(),
	    new PasswordReset(),
	    new UserProfile(),
	    new CircleProfile(),
	    new ProjectProfile(),
	    new MakeExperiments(),
	    new ManipulateExperiments(),
	    new MakeLibraries(),
	    new ManipulateLibraries(),
	    new LibrariesExperiments(),
	    new RealizeExperiments(),
	    new RemoveLibraries(),
	    new RemoveExperiments(),
	    new RemoveCircles(),
	    new RemoveProjects(),
	    new RemoveUsers(),
	    new RemoveUserAttributes(),
	    new RemoveCircleAttributes(),
	    new RemoveExperimentAttributes(),
	    new RemoveLibraryAttributes(),
	    new RemoveProjectAttributes(),
	    new RemoveAdminUsers(),
	};
	String adminUser = (args.length > 3) ? args[3] : "deterboss";

	loadTrust();
	loadID();
	boolean ok = false;

	while ( !ok) {
	    ld.setVisible(true);
	    char[] pwd = ld.getPassword();

	    if (ld.isCancelled() || pwd.length == 0) {
		ld.dispose();
		return;
	    }

	    // Carry out the challenge
	    try {
		login(adminUser, new String(pwd));
		ok = true;
	    } catch (DeterFault df) {
		if (df.getErrorCode() != DeterFault.access )
		    fatal(df.getErrorMessage() + ": " + df.getDetailMessage());
	    } catch (Exception e) {
		e.printStackTrace();
		fatal("unexpected exception");
	    }
	    ld.clearPassword();
	}
	ld.dispose();

	try {
	    for (RegressionTest r: tests) 
		r.runTest(new File(dir, 
			    String.format("%03d-" + r.getName()+".xml", n++)),
			new File(ddir));
	}
	catch (RegressionTest.RegressionException e) {
	    exitCode=20;
	}
	log.info("Cleaning up");
	for ( int i= tests.length-1; i>=0; i--) 
	    tests[i].cleanUp();
	System.exit(exitCode);
    }
}
