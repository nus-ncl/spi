package net.deterlab.testbed.util;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import net.deterlab.testbed.util.option.Option;
import net.deterlab.testbed.util.option.ParamOption;

/**
 * A utility to add a property to the default utility store.
 * @author the DETER Team
 * @version 1.0
 */
public class SetProperty extends Utility {

    /**
     * Print a usage message and exit.
     */
    static public void usage() {
	fatal("Usage: SetProperty [--file outputFilename] [[key value] ...]");
    }

    /**
     * For each key/value pair on the command line, call setProperty
     * UserAttributes
     * @param args the key/value pairs and an optional --file fn parameter
     */
    static public void main(String[] args) {
	ParamOption file = new ParamOption("file");
	List<String> argv = new ArrayList<String>();

	try {
	    Option.parseArgs(args, new Option[] { file } , argv);
	}
	catch (Option.OptionException e) {
	    System.err.println(e.getMessage());
	    usage();
	}

	if ( argv.size() % 2 != 0 ) usage();

	if (file.getValue() != null ) 
	    setPropertyFile(new File(file.getValue()), true);

	try {
	    for ( int i = 0 ; i < argv.size(); i += 2) 
		setProperty(argv.get(i), argv.get(i+1));
	}
	catch (IOException e) {
	    fatal(e.getMessage());
	}
    }
}
