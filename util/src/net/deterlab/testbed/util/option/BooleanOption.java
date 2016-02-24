package net.deterlab.testbed.util.option;

/**
 * This class parses command line options that are either present or not.  The
 * sense is negated if preceeded with --no-
 * @author DETER team
 * @version 1.0
 */
public class BooleanOption extends Option {
    /** Name of the option */
    private String name;
    /** Name of the anti-option */
    private String noName;
    /** The value of the flag */
    private Boolean value;

    /**
     * Set the option name and a default value.
     * @param n the name (the command line option will ne --name)
     * @param d the default value
     */
    public BooleanOption(String n, Boolean d) { 
	super();
	name = "--" + n;
	noName = "--no-" + n;
	value = d;
    }

    /**
     * Set the option name.
     * @param n the name (the command line option will be --name)
     */
    public BooleanOption(String n) { this(n, null); }

    /**
     * Return the option name (including the leading --.
     * @return the option name (including the leading --.
     */
    public String getName() { return name; } 

    /**
     * Return true if either the positive or negative flag matches.  The value
     * is set here.
     * @return true if either the positive or negative flag matches.
     */
    public boolean match(String a) {
	if (name.equals(a)) {
	    value = new Boolean(true);
	    return true;
	}
	if (noName.equals(a)) {
	    value = new Boolean(false);
	    return true;
	}
	return false;
    }

    /**
     * Return the numbe rof positional parameters (0)
     * @return the numbe rof positional parameters (0)
     */
    public int nparam() { return 0; }

    /**
     * Add a parameter.  This should never be called and always throws an
     * exception.
     * @param a the ignored parameter
     * @throws OptionException always
     */
    public void addParam(String s) throws OptionException { 
	throw new OptionException(getName() + " takes no parameters");
    }

    /**
     * Return the value of the option.  Will by null if unset and no default.
     * @return true the value of the option.
     */
    public Boolean getValue() { return value; }
}
