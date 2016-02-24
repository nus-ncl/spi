package net.deterlab.testbed.util;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A class that dispatches other members of this package that have a static
 * public main method that takes a string array as an argument.  This is the
 * Main-Class of the util.jar file from which utilities are run.  This uses
 * reflection to find classes in the runtime so that it does not need to be
 * recompiled when a new utility is added.
 * @author DETER Team
 * @version 1.1
 */
public class Dispatch {

    /**
     * Print a message and exit with a bad return code.
     * @param msg the message to print
     */
    static void fatal(String msg) {
	System.err.println(msg);
	System.exit(20);
    }

    /**
     * Look up the first argument as a class name in this package, and if that
     * class has a main method that takes an array of strings, call it with the
     * remaining arguments.  Catch a bunch of different exceptions and let the
     * user know what, if anything, went wrong.
     * @param args the command line arguments
     */
    static public void main(String[] args) {
	if (args.length == 0 )
	    fatal("Supply a class name as the first argument");

	String name = "net.deterlab.testbed.util." + args[0];
	String[] newArgs = new String[args.length-1];

	for (int i = 1; i < args.length; i++) 
	    newArgs[i-1] = args[i];

	try {

	    Class<?> utilClass = Class.forName(name);
	    Method main = utilClass.getMethod("main", 
		    new Class<?>[] { newArgs.getClass()});

	    main.invoke(null, new Object[] { newArgs } );
	}
	catch (ClassNotFoundException e) {
	    fatal("Unknown utility " + name);
	}
	catch (NoSuchMethodException e) {
	    fatal(name + " has no main?");
	}
	catch (IllegalAccessException e) {
	    fatal("Cannot access main in " + name + ": " + e.getMessage());
	}
	catch (IllegalArgumentException e) {
	    fatal("Cannot call main in " + name + ": " + e.getMessage());
	}
	catch (InvocationTargetException e) {
	    fatal("main in " + name + "threw: " + e.getCause().getMessage());
	}
    }
}
