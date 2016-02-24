package net.deterlab.testbed.util.option;

import java.util.List;


/**
 * A command line option, specialized for different specific semantics.
 * @author DETER team
 * @version 1.0
 */
public abstract class Option {
    /**
     * Failures to parse options throw these
     * @author DETER team
     * @version 1.0
     */
    public static class OptionException extends Exception {
	public OptionException(String msg) { super(msg); }
    }

    /** 
     * Flag to parseArgs: if set, put unrecognized strings that begin with -
     * into the list of positional arguments.
     */
    public static final int ALLOW_UNKNOWN_FLAGS=2<<0;
    /**
     * Make an option
     */
    public Option() { }

    /**
     * Return the name of the option.
     */
    public abstract String getName();
    /**
     * Return true if this string indicates the presence of the option
     * @param a the string to test
     * @return true if this string indicates the presence of the option
     */
    public abstract boolean match(String a);

    /**
     * Return the number of positional parameters this option takes (the number
     * of times it expects to have addParam called for each true return from
     * match)
     * @return the number of positional parameters this option takes 
     */
    public abstract int nparam();

    /**
     * Add a positional parameter to this option.
     * @param a the parameter to add
     * @throws OptionException if the parameter is misformatted or unexpected.
     */
    public abstract void addParam(String a) throws OptionException;

    /**
     * Parse the command line strings into the array of options.  Command line
     * arguments not consumed by Options are placed into opts.
     * @param args the command line args
     * @param opts the options to parse against. Modified by this call.
     * @param pos the positional (unmatched) command line parameters.  Modified
     * by this call.  May be null.
     * @param flags set of flags that affect parsing. See constants for this
     * class.
     * @throws OptionException on parsing errors
     */
    public static void parseArgs(String[] args, Option[] opts, 
	    List<String> pos, int flags) throws OptionException {
argument_processing:
	for (int i = 0; i < args.length; i++) {
	    for (Option opt: opts) {
		// If this option matches, consume its parameters
		if ( opt.match(args[i]) ) {
		    if ( i + opt.nparam() >= args.length ) 
			throw new OptionException(opt.getName() + 
				" requires " + opt.nparam() + " parameter(s)");

		    for ( int j = 0; j < opt.nparam(); j++)
			opt.addParam(args[++i]);
		    // Done with this argument, on to the next.  This continue
		    // continues the outer for loop.
		    continue argument_processing;
		}
	    }
	    if ( args[i].startsWith("-") && 
		    (flags & ALLOW_UNKNOWN_FLAGS) == 0)
		throw new OptionException("Unrecognized option " + args[i]);
	    // No matches: into the dome! (pos)
	    if ( pos != null ) 
		pos.add(args[i]);
	}
    }
    /**
     * Parse the command line strings into the array of options.  Command line
     * arguments not consumed by Options are placed into opts.  This is
     * parseArgs with 0 flags.
     * @param args the command line args
     * @param opts the options to parse against. Modified by this call.
     * @param pos the positional (unmatched) command line parameters.  Modified
     * by this call.  May be null.
     * @throws OptionException on parsing errors
     */
    public static void parseArgs(String[] args, Option[] opts, 
	    List<String> pos) throws OptionException {
	parseArgs(args, opts, pos, 0);
    }
}
