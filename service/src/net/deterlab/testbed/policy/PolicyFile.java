package net.deterlab.testbed.policy;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;

import net.deterlab.testbed.api.DeterFault;

/**
 * Encapsulates a policy file.  Includes methods to convert the policy file
 * into a set of credentials.
 * @author DETER Team
 * @version 1.1
 */
public class PolicyFile {

    /** The file holding the policy */
    private File file;
    /** Matched a variable name */
    static private Pattern var = Pattern.compile("\\$([A-Z]+)");
    /** Matches a comment */
    static private Pattern commentLine = Pattern.compile("^#");
    /** Matches a blank line */
    static private Pattern blankLine = Pattern.compile("^\\s*$");
    /** Matches a set definition line
     * Set: name(param)
     */
    static private Pattern setLine = 
	Pattern.compile("set:\\s*(\\w+)\\(([\\w\\$]*)\\)", 
		Pattern.CASE_INSENSITIVE);

    /**
     * New Policy file backed by the given file
     * @param f the file that defines the policy
     */
    public PolicyFile(File f) {
	file = f;
    }

    /**
     * Expand variables found in str.  Because variables (like permissions) may
     * expand to more than one value, a collection is returned.  str is
     * expanded for each value of each variable.
     * @param str the string in which to expand embedded variables
     * @param vars a map from variable names to values
     * @return the expanded strings
     * @throws DeterFault if things are very weird
     */
    protected Collection<String> expandVariables(String str, 
	    Map<String, Collection<String> > vars) throws DeterFault {
	ArrayList<String> rv = new ArrayList<String>();
	boolean matched = true;

	rv.add(str);
	while (matched ) {
	    ArrayList<String> nrv = new ArrayList<String>();
	    matched = false;
	    for ( String s: rv ) {
		Matcher m = var.matcher(s);
		if (!m.find()) {
		    nrv.add(s);
		    continue;
		}

		matched = true;
		String vname = m.group(1);
		Collection <String> reps = vars.get(vname);
		m = Pattern.compile("\\$"+ vname).matcher(s);
		if (reps == null ) {
		    nrv.add(m.replaceAll(""));
		}
		else {
		    if (!m.find() ) 
			throw new DeterFault(DeterFault.internal,
				"Second match fails in policy replacement?");
		    for (String repl: reps) 
			nrv.add(m.replaceAll(repl));
		}
	    }
	    rv = nrv;
	}
	return rv;
    }

    /**
     * Add the matched set to the collection of sets - assumes m results from a
     * match of setLine.  A set definition is type(name) where type and name
     * are strings and name may have variables expanded.
     * @param m the match from a setLine
     * @param vars the map of variable names to values
     * @param sets the collection of sets to add to.
     * @throws DeterFault if things are very weird in expanding variables
     */
    protected void addCredentialSets(Matcher m, Map<String,
	    Collection<String> > vars,
	    Collection<CredentialSet> sets) throws DeterFault {
	String cl = m.group(1);	    // The set type/class
	String param = m.group(2);  // The set name/parameter may be empty

	if ( param == null || param.length() == 0) {
	    sets.add(new CredentialSet(cl, null));
	}
	else {
	    for (String p : expandVariables(param, vars))
		sets.add(new CredentialSet(cl, p));
	}
    }

    /**
     * This does all the work of updateCredentials and addCredentials, the only
     * difference being whether the exsting credentials are cleared.
     * @param cdb the credential store to modify
     * @param name the value of the NAME variable in interpreting the file
     * @param uid the value of the UID variable in interpreting the file
     * @param perms the value of the PERMS variable in interpreting the file
     * @param id the key used to derive the KEYID variable
     * @param clearExisting clear the existing credentials from the store if set
     * @throws DeterFault if the credentials can't be added or the file was
     * bogus
     */
    protected void changeCredentials(CredentialStoreDB cdb, String name,
	    String uid, Collection<String> perms, Identity id,
	    boolean clearExisting)
	throws DeterFault {
	Credentials cr = new Credentials();
	List<Credential> creds = new ArrayList<Credential>();
	List<CredentialSet> sets = new ArrayList<CredentialSet>();
	Map<String, Collection<String> > vars = 
	    new TreeMap<String, Collection<String> >();

	if (file == null ) 
	    throw new DeterFault(DeterFault.internal,
		    "PolicyFile without a filename!?");

	if ( name != null ) 
	    vars.put("NAME", Arrays.asList(new String[] { name }));
	if ( uid != null ) 
	    vars.put("UID", Arrays.asList(new String[] { uid }));
	if ( perms != null ) 
	    vars.put("PERMS", perms);
	if ( id != null )
	    vars.put("KEYID", Arrays.asList(new String[] { id.getKeyID() }));

	try {
	    LineNumberReader r = new LineNumberReader(new FileReader(file));
	    String l = null;

	    while ( (l = r.readLine()) != null ) {
		Matcher m = commentLine.matcher(l);

		if (m.find()) continue;
		m = blankLine.matcher(l);
		if (m.find()) continue;

		m = setLine.matcher(l);
		if (m.find() ) {
		    addCredentialSets(m, vars, sets);
		    continue;
		}

		// This is a credential
		Collection<String> credStrs = expandVariables(l, vars);

		for ( String c : credStrs ) {
		    String[] ht = c.split("\\s*<-\\s*");
		    if ( ht.length != 2) 
			throw new DeterFault(DeterFault.internal,
				"PolicyFile syntax error in " + file + " " + c);

		    // If this credential is an assignment to a keyid, build a
		    // credential using that special interface.  Otherwise use
		    // the more general.
		    if ( ht[1].length() == 40 &&
			    ht[1].matches("^[abcdef0123456789]{40}"))
			creds.add(cr.makeCredentialKeyID(ht[0], ht[1]));
		    else
			creds.add(cr.makeCredential(ht[0],
				Arrays.asList(ht[1].split("\\s*&\\s*"))));
		}
	    }
	    // Replace old with new.
	    if (clearExisting) cdb.removeCredentials(sets);
	    cdb.addCredentials(creds, sets);
	    
	    r.close();
	    
	}
	catch (IOException e) {
	    throw new DeterFault(DeterFault.internal,
		    "PolicyFile error in " + file + " " + e.getMessage());
	}
    }

    /**
     * Read the file and generate credentials from them, adding to the
     * credential DB; remove the existing entries from the set intersection.
     * name, uid, and perms are the values of the NAME, UID and PERMS variables
     * in the credentials and sets.
     * @param cdb the credential store to modify
     * @param name the value of the NAME variable in interpreting the file
     * @param uid the value of the UID variable in interpreting the file
     * @param perms the value of the PERMS variable in interpreting the file
     * @param id the key used to derive the KEYID variable
     * @throws DeterFault if the credentials can't be added or the file was
     * bogus
     */
    public void updateCredentials(CredentialStoreDB cdb, String name,
	    String uid, Collection<String> perms, Identity id)
	throws DeterFault {
	changeCredentials(cdb, name, uid, perms, id, true);
    }

    /**
     * Read the file and generate credentials from them, adding to the
     * credential DB.  name, uid, and perms are the values of the NAME, UID and
     * PERMS variables in the credentials and sets.
     * @param cdb the credential store to modify
     * @param name the value of the NAME variable in interpreting the file
     * @param uid the value of the UID variable in interpreting the file
     * @param perms the value of the PERMS variable in interpreting the file
     * @param id the key used to derive the KEYID variable
     * @throws DeterFault if the credentials can't be added or the file was
     * bogus
     */
    public void addCredentials(CredentialStoreDB cdb, String name,
	    String uid, Collection<String> perms, Identity id)
	throws DeterFault {
	changeCredentials(cdb, name, uid, perms, id, false);
    }

    /**
     * Read the file and remove any credentials it would have generated based
     * on the CredentialSets in there.
     * @param cdb the credential store to modify
     * @param name the value of the NAME variable in interpreting the file
     * @param uid the value of the UID variable in interpreting the file
     * @throws DeterFault if the credentials can't be added or the file was
     * bogus
     */
    public void removeCredentials(CredentialStoreDB cdb, 
	    String name, String uid) throws DeterFault {
	if (file == null ) 
	    throw new DeterFault(DeterFault.internal,
		    "PolicyFile without a filename!?");
	List<CredentialSet> sets = new ArrayList<CredentialSet>();
	Map<String, Collection<String> > vars = 
	    new TreeMap<String, Collection<String> >();

	if ( name != null ) 
	    vars.put("NAME", Arrays.asList(new String[] { name }));
	if ( uid != null ) 
	    vars.put("UID", Arrays.asList(new String[] { uid }));

	try {
	    LineNumberReader r = new LineNumberReader(new FileReader(file));
	    String l = null;


	    while ( (l = r.readLine()) != null ) {
		Matcher m = commentLine.matcher(l);

		if (m.find()) continue;
		m = blankLine.matcher(l);
		if (m.find()) continue;

		m = setLine.matcher(l);
		if (m.find() ) {
		    addCredentialSets(m, vars, sets);
		    continue;
		}

		// This is a credential - ignore it.
	    }
	    cdb.removeCredentials(sets);
	    r.close();
	}
	catch (IOException e) {
	    throw new DeterFault(DeterFault.internal,
		    "PolicyFile error in " + file + " " + e);
	}
    }
}

