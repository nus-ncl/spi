package net.deterlab.testbed.util.option;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Command option of the form --flag value that takes only numeric parameters.
 * @author The DETETR Team
 * @version 1.0
 */
public class NumberOption extends Option {
    /** Option name */
    private String name;
    /** Option value or null if unset */
    private Number value;
    /** number parser */
    NumberFormat fmt;

    /**
     * Set the option name and a default value
     * @param n the name (the command line option will ne --name)
     * @param d the default value
     */
    public NumberOption(String n, Number d) { 
	super();
	name = "--" + n;
	value = d;
	fmt = NumberFormat.getInstance();
    }

    /**
     * Set the option name.  The value will be null unless set.
     * @param n the name (the command line option will ne --name)
     */
    public NumberOption(String n) { this(n, null); }

    /**
     * Return the option name including the leading --.
     * @Return the option name including the leading --.
     */
    public String getName() { return name; } 

    /**
     * Return true if a matches the name (including the leading --)
     * @param a the string to macth against
     * @return true if a matches the name (including the leading --)
     */
    public boolean match(String a) { return name.equals(a); }

    /**
     * Return the number of parameters this option expects (1)
     * @return the number of parameters this option expects (1)
     */
    public int nparam() { return 1; }

    /**
     * Set the value of the option to s.
     * @param s the new value
     * @throws OptionException if the string is not parsable into a number
     */
    public void addParam(String s) throws OptionException { 
	try {
	    value = fmt.parse(s);
	}
	catch (ParseException e) {
	    throw new OptionException(e.getMessage());
	}
    }

    /**
     * Return the value - null if unset and no default.
     * @return the value
     */
    public Number getValue() { return value; }
}
