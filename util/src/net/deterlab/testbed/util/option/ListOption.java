package net.deterlab.testbed.util.option;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Command option of the form --flag value that can be repeated
 * @author The DETETR Team
 * @version 1.0
 */
public class ListOption extends Option {
    /** Option name */
    private String name;
    /** Option value or null if unset */
    private List<String> value;

    /**
     * Set the option name 
     * @param n the name (the command line option will be --name)
     */
    public ListOption(String n) { 
	super();
	name = "--" + n;
	value = new ArrayList<String>();
    }

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
     * Append the value s of the option value.
     * @param s the new value
     * @throws OptionException in principle, but in reality does not.
     */
    public void addParam(String s) throws OptionException { 
	value.add(s);
    }

    /**
     * Return the value - empty collection if unset
     * @return the value
     */
    public Collection<String> getValue() { return value; }
}
