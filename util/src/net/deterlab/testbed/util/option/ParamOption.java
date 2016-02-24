package net.deterlab.testbed.util.option;

/**
 * Command option of the form --flag value
 * @author The DETETR Team
 * @version 1.0
 */
public class ParamOption extends Option {
    /** Option name */
    private String name;
    /** Option value or null if unset */
    private String value;

    /**
     * Set the option name and a default value
     * @param n the name (the command line option will ne --name)
     * @param d the default value
     */
    public ParamOption(String n, String d) { 
	super();
	name = "--" + n;
	value = d;
    }

    /**
     * Set the option name.  The value will be null unless set.
     * @param n the name (the command line option will ne --name)
     */
    public ParamOption(String n) { this(n, null); }

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
     * @throws OptionException in principle, but in reality does not.
     */
    public void addParam(String s) throws OptionException { 
	value = s;
    }

    /**
     * Return the value - null if unset and no default.
     * @return the value
     */
    public String getValue() { return value; }
}
