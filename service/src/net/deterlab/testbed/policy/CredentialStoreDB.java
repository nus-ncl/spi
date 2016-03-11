package net.deterlab.testbed.policy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.deterlab.abac.Context;
import net.deterlab.abac.Credential;
import net.deterlab.abac.Identity;
import net.deterlab.abac.InternalCredential;
import net.deterlab.abac.Role;
import net.deterlab.testbed.api.DeterFault;
import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

public class CredentialStoreDB extends DBObject {

    /**
     * This describes the database implementation of a credential set.  It
     * holds the table in which the set is defined, and the linked table name
     * and column name in which to look up identifiers to produce indices.  For
     * example a set of users would be linked to the "users" table and the id
     * column is "uid."
     * @author DETER Team
     * @version 1.1
     */
    static private class CredSet {
	/** The table defining the set */
	public String table;
	/** Table used to resolve string identifiers */
	public String linkedTable;
	/** Column that holds string identifiers */
	public String linkedId;

	/**
	 * Make a CredSet
	 * @param t the table defining the set
	 * @param lt the linked table for resolving a string id
	 * @param lid the linked column for resolving a string id
	 */
	public CredSet(String t, String lt, String lid) {
	    table = t; linkedTable = lt; linkedId = lid;
	}

	/**
	 * Return the select statement that selects the index from the
	 * subtable.  If no subtable is given the credential set is not
	 * parameterizes - e.g., logins.
	 * @return the select statement that selects the index from the
	 * subtable
	 */
	public String select() {
	    if ( linkedTable == null || linkedId == null) 
		return "select 0";
	    return "SELECT idx FROM " + linkedTable + " WHERE " + 
		linkedId + "=?";
	}
    }

    /** The testbed configuration , used to open the database */
    //private Config config;
    
    /** A table mapping setname to the CredSet that defines it. */
    static private Map<String, CredSet> typeToCredSet =
	new HashMap<String, CredSet>();
    /**
     * A cache of credentials the store has already parsed from the DB.  It
     * tuens out that parsing XML is slow, even without the signature check.
     * This keeps copies of the Credential objects parsed earlier for import
     * into an ABAC Context when needed.  The various add/remove/and expire
     * routines also manipulate the cache.
     */
    static private Map<String, Credential[]> credCache =
	new HashMap<String, Credential[]>();

    /*
     * Build the typeToCredSet table that defines knows sets.  A new set has to
     * be added here and to the DB.
     */
    static { 
	typeToCredSet.put("circle",
		new CredSet("circlecreds", "circles", "circleid"));
	typeToCredSet.put("circlemembers",
		new CredSet("circlemembercreds", "circles", "circleid"));
	typeToCredSet.put("circleowner",
		new CredSet("circleownercreds", "circles", "circleid"));
	typeToCredSet.put("experiment",
		new CredSet("experimentcreds", "experiments", "eid"));
	typeToCredSet.put("library",
		new CredSet("librarycreds", "libraries", "libid"));
	typeToCredSet.put("project",
		new CredSet("projectcreds", "projects", "projectid"));
	typeToCredSet.put("projectmembers",
		new CredSet("projectmembercreds", "projects", "projectid"));
	typeToCredSet.put("projectapproval",
		new CredSet("projectapprovalcreds", "projects", "projectid"));
	typeToCredSet.put("resource",
		new CredSet("resourcecreds", "resources", "name"));
	typeToCredSet.put("realization",
		new CredSet("realizationcreds", "realizations", "name"));
	typeToCredSet.put("user",
		new CredSet("usercreds", "users", "uid"));
	typeToCredSet.put("login",
		new CredSet("logincreds", null, null));
	typeToCredSet.put("system",
		new CredSet("systemcreds", null, null));
    }

    /**
     * A CredentialSet that has been linked to the CredSet describing
     * the underlying set implementation.  This class uses ResolvedCredSets
     * internally whenever a CredentialSet is passed in.
     * @author DETER Team
     * @version 1.1
     */
    static private class ResolvedCredSet {
	/** The name of the identitifier defining the set */
	private String name;
	/** The DB parameters for the set */
	private CredSet cset;

	/**
	 * Resolve a CredentialSet
	 * @param r the user-supplied set to resolve
	 * @throws DeterFault if the type is unknown
	 */
	public ResolvedCredSet(CredentialSet r) 
	    throws DeterFault{
	    name = r.getName();
	    cset = typeToCredSet.get(r.getType());
	    if (cset == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Unknown credential set type " + r.getType());
	}
	/**
	 * Return the name
	 * @return the name
	 */
	public String getName() { return name; }
	/**
	 * Return the table name
	 * @return the table name
	 */
	public String getTable() { return cset.table; }
	/**
	 * Return the select statement that resolves name into an index.  A ?
	 * is inserted for the name itself which will must be used as a
	 * PreparedStatement to properly escape the name.
	 * @return the select statement that resolves name in the table
	 */
	public String select() throws DeterFault { 
	    return cset.select();
	}
    }

    /**
     * Make a new CredentialStoreDB, an interface to the credential store.
     * @throws DeterFault if the testbed is misconfigured (cannot create a
     * Config or DB connection)
     */
    public CredentialStoreDB() throws DeterFault {
	super();
    }

    /**
     * Make a new CredentialStoreDB, an interface to the credential store that
     * shares a DB connection.
     * @param sc the shrared connection
     * @throws DeterFault if the testbed is misconfigured (cannot create a
     * Config or DB connection)
     */
    public CredentialStoreDB(SharedConnection sc) throws DeterFault {
	super(sc);
    }

    /**
     * Write a credential to a string and return it.
     * @param cred the credential
     * @return the contents of a credential file
     * @throws DeterFault if the writing fails
     */
    protected String credentialToString(Credential cred) throws DeterFault {
	/*try {
	    ByteArrayOutputStream cs = new ByteArrayOutputStream();

	    cred.write(cs);
	    return cs.toString();
	}
	catch (IOException e) {
	    throw new DeterFault(DeterFault.internal, 
		    "Cannot write credential");
	}*/
    return cred.toString();
    }

    /**
     * Remove credentials from the store if they have expired
     * @throws DeterFault if there is a DB problem
     */
    protected void expireCredentials() throws DeterFault {
	PreparedStatement p = null;
	try {
	    // Remove all expired entries from the cache.  If nothing's
	    // expired, return.
	    p = getPreparedStatement(
		    "SELECT cred FROM credentials WHERE expiration < NOW()");
	    ResultSet r = p.executeQuery();
	    int count = 0;
	    while (r.next()) {
		credCache.remove(r.getString(1));
		count ++;
	    }
	    if ( count == 0 ) return;

	    // Remove expired credentials from sets
	    for (CredSet s : typeToCredSet.values()) {
		p = getPreparedStatement(
			"DELETE FROM " + s.table + " WHERE cidx IN " +
			"(SELECT idx FROM credentials WHERE expiration<NOW())");
		p.executeUpdate();
	    }
	    // Remove the credentials
	    p = getPreparedStatement(
			"DELETE FROM credentials WHERE expiration<NOW()");
	    p.executeUpdate();

	    // unbind any expired bindings
	    p = getPreparedStatement(
		    "DELETE FROM keytouser WHERE expiration < NOW()");
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return the contents of database column field for credentials that are in
     * the intersection of the given sets.
     * @param sets the ParameterziedCredSets to intersect
     * @param field the column to return
     * @return the contents of the column as Strings
     * @throws DeterFault on errors
     */
    protected Collection<String> findCredentialsIntersection(
	    Collection<CredentialSet> sets, String field) 
	throws DeterFault {
	PreparedStatement p = null;
	List<ResolvedCredSet> csets = new ArrayList<ResolvedCredSet>();
	List<String> rv = new ArrayList<String>();

	// Resolve the sets
	for (CredentialSet ts : sets) 
	    csets.add(new ResolvedCredSet(ts));

	// Construct a query that joins the sets into one table in preparation
	// for cutting it down with WHERE clauses.  Each table in csets is
	// joined in on the credential index.
	StringBuilder qs = new StringBuilder("SELECT c.");
	qs.append(field);
	qs.append(" FROM credentials AS c ");
	int i =0;
	for (ResolvedCredSet s: csets ) {
	    qs.append(" INNER JOIN ");
	    qs.append(s.getTable());
	    qs.append(" AS s");
	    qs.append(i);
	    if ( i > 0 ) {
		qs.append( " ON s");
		qs.append(i-1);
		qs.append(".cidx=s");
	    }
	    else {
		qs.append( " ON c.idx=s");
	    }
	    qs.append(i);
	    qs.append(".cidx");
	    i++;
	}
	// Add the WHERE clauses that limit each joined table's linked index
	// based on the parameter passes in for that set.
	i =0;
	for (ResolvedCredSet s: csets ) {
	    if (i==0) qs.append(" WHERE s");
	    else qs.append(" AND s");
	    qs.append(i);
	    qs.append(".lidx=(");
	    qs.append(s.select());
	    qs.append(")");
	    i++;
	}

	// Run the query and collect results.
	try {
	    p = getPreparedStatement(qs.toString());
	    i = 1;
	    for (ResolvedCredSet s: csets ) {
		if (s.getName() != null ) 
		    p.setString(i++, s.getName());
	    }
	    ResultSet r = p.executeQuery();
	    while (r.next()) 
		rv.add(r.getString(1));
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return the contents of database column field for credentials that are in
     * the union of the given sets.
     * @param sets the ParameterziedCredSets to intersect
     * @param field the column to return
     * @return the contents of the column as Strings
     * @throws DeterFault on errors
     */
    protected Collection<String> findCredentialsUnion(
	    Collection<CredentialSet> sets, String field) throws DeterFault {
	PreparedStatement p = null;
	List<ResolvedCredSet> csets = new ArrayList<ResolvedCredSet>();
	List<String> rv = new ArrayList<String>();

	// Resolve the sets
	for (CredentialSet ts : sets)
	    csets.add(new ResolvedCredSet(ts));

	// Construct a query that joins the sets into one table in preparation
	// for cutting it down with WHERE clauses.  Each table in csets is
	// joined in on the credential index.
	StringBuilder qs = new StringBuilder();
	for (ResolvedCredSet s: csets ) {
	    if ( qs.length() != 0 ) qs.append(" UNION ");
	    qs.append("SELECT c.");
	    qs.append(field);
	    qs.append(" FROM credentials AS c ");
	    qs.append(" INNER JOIN ");
	    qs.append(s.getTable());
	    qs.append(" AS s ON c.idx=s.cidx");
	    qs.append(" WHERE s.lidx=(");
	    qs.append(s.select());
	    qs.append(")");
	}

	// Run the query and collect results.
	try {
	    p = getPreparedStatement(qs.toString());
	    int i = 1;
	    for (ResolvedCredSet s: csets ) {
		if (s.getName() != null )
		    p.setString(i++, s.getName());
	    }
	    ResultSet r = p.executeQuery();
	    while (r.next())
		rv.add(r.getString(1));
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
    /** 
     * Remove the credentials in the intersection of the paramteterized sets.
     * @param sets the sets to intersect
     * @throws DeterFault if there is an error.
     */
    public void removeCredentials(Collection<CredentialSet> sets) 
	    throws DeterFault {
	PreparedStatement p = null;

	expireCredentials();

	// Get the indices of the credentials to remove
	Collection<String> idx = findCredentialsIntersection(sets, "idx");
	Collection<String> keys = findCredentialsIntersection(sets, "cred");
	StringBuilder d = new StringBuilder();

	// Make a string with the indices formatted as for a SQL set.
	for (String i: idx ) {
	    if ( d.length() > 0 ) d.append(",");
	    d.append(i);
	}
	if ( d.length() == 0 ) return; // Nothing to remove.
	String delSet = d.toString();

	try {
	    // Remove the creds from all sets
	    for (CredSet s: typeToCredSet.values()) {
		p = getPreparedStatement(
			"DELETE FROM " + s.table + 
			    " WHERE cidx IN (" + delSet + ")" );
		p.executeUpdate();
	    }
	    // Remove the creds from the table
	    p = getPreparedStatement(
		    "DELETE FROM credentials WHERE idx IN (" + delSet + ")" );
	    p.executeUpdate();

	    // Remove credentials from the cache
	    for ( String k : keys)
		credCache.remove(k);
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }


    /**
     * Add the credentials to each set in sets.  Each credential is saved once
     * and linked into sets.
     * @param creds the credentials to add
     * @param sets the sets to add them to
     * @throws DeterFault on error.
     */
    public void addCredentials(Collection<Credential> creds, 
	    Collection<CredentialSet> sets) throws DeterFault {
	List<ResolvedCredSet> csets = new ArrayList<ResolvedCredSet>();
	int i = 0;

	expireCredentials();
	if (creds.size() == 0 ) return;
	for (CredentialSet ts : sets) 
	    csets.add(new ResolvedCredSet(ts));

	try {
	    // Build a single insert statement to insert all the new
	    // credentials into the credentials table at once.
	    
	    StringBuilder qb = new StringBuilder(
		    "INSERT INTO credentials (expiration, cred) VALUES ");
	    for (i =0; i < creds.size(); i++ ) {
		if ( i != 0 ) qb.append(", ");
		qb.append("(?, ?)");
	    }

	    PreparedStatement p = getPreparedStatement(qb.toString(), 1);

	    i = 0;
	    for ( Credential cred: creds ) {
		String credChunk = credentialToString(cred);
		//p.setTimestamp(1+2*i, new Timestamp(cred.expiration().getTime()));
		Calendar date = Calendar.getInstance();
	    date.setTime(new Date());
	    date.add(Calendar.YEAR,1);
	    p.setTimestamp(1+2*i, new Timestamp(date.getTime().getTime()));
		p.setString(2+2*i, credChunk);
		// Cache the new credentials as well
		credCache.put(credChunk, new Credential[] { cred });
		i++;
	    }
	    p.executeUpdate();

	    // Now, for each credential added, add its index to the relevant
	    // credential set tables.
	    ResultSet r = p.getGeneratedKeys();
	    while ( r.next()) {
		int cridx = r.getInt(1);
		for ( ResolvedCredSet rs : csets ){
		    p = getPreparedStatement(
			    "INSERT INTO " + rs.getTable() +
				" (cidx, lidx) " +
				" VALUES (?, (" +  
				rs.select() + "))");
		    p.setInt(1, cridx);
		    if (rs.getName() != null)
			p.setString(2, rs.getName());
		    p.executeUpdate();
		}
	    }
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Load the context with credentials in the union of the given sets.
     * @param c the context to load
     * @param sets the sets to combine
     * @throws DeterFault if there is an error
     */
    public void loadContext(Context c, Collection<CredentialSet> sets) 
    throws DeterFault {
    expireCredentials();
    
    //CredentialFactory cf = c.getCredentialFactory();
    Pattern rule = Pattern.compile("^\\w+\\.([\\w:]+\\.)*[\\w:]+\\s*<-+\\s*.+");
    
    Collection <String> cstr = findCredentialsUnion(sets, "cred");
    for (String cs: cstr ) {
        Credential[] creds = credCache.get(cs);
        if ( creds == null ) {
            /*try {
                creds = cf.parseCredential(cs, c.identities());
            }
            catch (ABACException e) {
                continue;
            }*/
            
            Matcher rm = rule.matcher(cs);
            if (rm.find()) {
            	String[] roles = cs.split("<-+");
            	Credential cred = new InternalCredential(new Role(roles[0].trim()), 
            	                                            new Role(roles[1].trim()));
            	creds = new Credential[]{cred};
            }
            
            if (creds == null) continue;
            credCache.put(cs, creds);
        }
        for (Credential cr: creds)
    	c.load_attribute_chunk(cr);
    }
    }

    /**
     * Bind identity k to userid uid for duration seconds.  If the identity was
     * bound, it is unbound and rebound to uid.
     * @param k the identity
     * @param uid the userid
     * @param duration time in seconds for the binding to last (unless unbound)
     * @throws DeterFault if there is a problem.
     */
    public void bindKey(Identity k, String uid, int duration) 
	    throws DeterFault {
	expireCredentials();

	if (k == null) 
	    throw new DeterFault(DeterFault.internal, "No key to bind?");
	if (uid == null) 
	    throw new DeterFault(DeterFault.internal, "No uid to bind?");

	unbindKey(k);

	try {
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO keytouser (ukey, uidx, expiration) " +
			"VALUES (?, (SELECT idx FROM users WHERE uid=?), " +
			    "TIMESTAMPADD(SECOND, ?, NOW()))");
	    p.setString(1, k.getKeyID());
	    p.setString(2, uid);
	    p.setInt(3, duration);
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove any and all bindings of identity k to any uids.
     * @param k the identity
     * @throws DeterFault if there are problems
     */
    public void unbindKey(Identity k) throws DeterFault {
	expireCredentials();

	if (k == null) 
	    throw new DeterFault(DeterFault.internal, "No key to bind?");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM  keytouser WHERE ukey=?");
	    p.setString(1, k.getKeyID());
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Remove any and all bindings of user to key - this logs all user logins
     * out.  It is used when removing a user.
     * @param uid the uid
     * @throws DeterFault if there are problems
     */
    public void unbindUid(String uid) throws DeterFault {
	expireCredentials();

	if (uid == null) 
	    throw new DeterFault(DeterFault.internal, "No key to bind?");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM  keytouser WHERE uidx=" +
			"(SELECT idx FROM users WHERE uid=?)");
	    p.setString(1, uid);
	    p.executeUpdate();
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }
    /**
     * Return the uid to which keyid k is bound, if any.
     * @param k the string key identifier
     * @return the uid to which keyid k is bound, null if none
     * @throws DeterFault on internal errors
     */
    public String keyToUid(String k) throws DeterFault {
	String rv = null;
	int i =0;
	expireCredentials();

	if (k == null) 
	    throw new DeterFault(DeterFault.internal, "No key to look up?");

	try {
	    PreparedStatement p = getPreparedStatement(
		    "SELECT uid FROM keytouser LEFT JOIN users ON uidx=idx " +
			"WHERE ukey=?");
	    p.setString(1, k);
	    ResultSet r = p.executeQuery();
	    while (r.next()) {
		rv = r.getString(1);
		i++;
	    }
	    if (i > 1 ) 
		throw new DeterFault(DeterFault.internal, 
			"Key mapped to multiple uids!?");
	    return rv;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal, e.getMessage());
	}
    }

    /**
     * Return the uid to which identity k is bound, if any.
     * @param k the identity
     * @return the uid to which keyid k is bound, null if none
     * @throws DeterFault on internal errors
     */
    public String keyToUid(Identity k) throws DeterFault {
	return keyToUid(k.getKeyID());
    }

    /**
     * Clear the global credential cache.
     */
    public void clearCache() {
	credCache.clear();
    }
}
