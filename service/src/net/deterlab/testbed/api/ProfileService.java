package net.deterlab.testbed.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import net.deterlab.testbed.policy.CredentialSet;
import net.deterlab.testbed.profile.ProfileDB;

/**
 * This class encapsulates the routines to manage profiles.  It is invisible to
 * and unneeded by users.  The parameters of the various profiles (table and
 * field names in the DB, for example) are specified in the ProfileDB classes.
 * <p>
 * Note the package scope.
 *
 * @author ISI DETER team
 * @version 1.0
 */
class ProfileService extends DeterService {
    /** log to write errors */
    private Logger log;
    /**
     * Construct a ProfileService.
     */
    public ProfileService() { }

    /**
     * Set the logger for this class.  Subclasses set it so that appropriate
     * prefixes show up in the log file.
     * @param l the new logger
     */
    protected void setLogger(Logger l) { log = l; }

    /**
     * Return an empty profile to find field names or descriptions.  The id
     * field of the Profile returned will be null.  No access control is
     * applied.  The caller needs to close the profile.
     * @param profile the profile to fill (no ID)
     * @return a profile populated with empty fields
     */
    protected Profile getProfileDescription(ProfileDB profile)
	    throws DeterFault {
	log.info("getProfileDescription");
	try {
	    if ( profile == null ) 
		throw new DeterFault(DeterFault.internal, 
			"Could not allocate " + profile.getType() +
			" profile");
	    profile.loadAll();
	    Profile p = profile.export();
	    log.info("getProfileDescription succeeded");
	    return p;
	}
	catch (DeterFault df) {
	    log.error("getProfileDescription failed: " + df);
	    throw df;
	}
    }

    /**
     * Return the profile associated with id, assuming that the caller has
     * permission.  Caller must close profile
     * @param id the profile id
     * @param profile the profile to fill 
     * @return a completed profile for id
     * @throws DeterFault on perimission errors or no such user.
     */
    protected Profile getProfile(String id, ProfileDB profile)
	    throws DeterFault {
	log.info("get" + profile.getTypeCaps() + "Profile for " + id);
	try {
	    if (id == null ) 
		throw new DeterFault(DeterFault.request, 
			"Missing id parameter");
	    // In profiles that check the name syntax this may throw an
	    // exception.  The spec expects the syntax error.
	    profile.setId(id);
	    profile.loadAll();
	    checkAccess(profile.getType() + "_" + id + "_get" +
			profile.getTypeCaps() + "Profile",
		    new CredentialSet(profile.getType(), id),
		    profile.getSharedConnection());
	    Profile p = profile.export();
	    log.info("get" + profile.getTypeCaps() +"Profile for " +
		    id + " succeeded");
	    return p;
	}
	catch (DeterFault df) {
	    log.error("get" + profile.getTypeCaps() + "Profile for " +
		    id + " failed: " + df);
	    throw df;
	}
    }

    /**
     * Log the results of each change.
     * @param profile the the profile being changed
     * @param results the results of the attempted changes
     */
    protected void logChangeResults(ProfileDB profile,
	    Collection<ChangeResult> results) {
	for (ChangeResult r : results) {
	    if ( r.getSuccess()) 
		log.info("change" + profile.getTypeCaps() + "Profile eid " +
			profile.getId() + " attr " +
			r.getName() + " succeeded");
	    else
		log.info("change" + profile.getTypeCaps() + "Profile eid " +
			profile.getId() + " attr " + r.getName() +
			" failed: " + r.getReason());
	}
    }

    /**
     * Process a list of change requests for attributes in eid's profile.  For
     * each request, a ChangeResult is returned, either indicating that the
     * change has gone through or annotated with a reason.  The caller must
     * close profile and all.
     * @param id the id of the profile to change
     * @param profile a profile of the proper type
     * @param all a profile of the proper type
     * @param changes the requested modifications
     * @return an array of results, one for each request
     * @throws DeterFault on database or access errors
     */
    protected ChangeResult[] changeProfile(String id,
	    ProfileDB profile, ProfileDB all,
	    ChangeAttribute[] changes) throws DeterFault {
	ArrayList<ChangeResult> results = new ArrayList<ChangeResult>();
	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> delNames = new ArrayList<String>();

	log.info("change" + profile.getTypeCaps() + "Profile for " + id);
	try {
	    if ( id == null ) 
		throw new DeterFault(DeterFault.request, "Missing id");
	    // Load before the access check to catch name format errors in
	    // Profiles that check
	    profile.setId(id);
	    profile.loadAll();
	    checkAccess(profile.getType() + "_" + id + "_change" +
			profile.getTypeCaps() + "Profile",
		    new CredentialSet(profile.getType(), id),
		    profile.getSharedConnection());
	    all.loadAll();

	    for (ChangeAttribute ca : changes ) {
		Attribute ea = profile.lookupAttribute(ca.getName());

		if ( ca.getDelete() ) {
		    // If the attribute is there, queue it for deletion if
		    // that's allowed.
		    if ( ea != null ) {
			if (!ea.getOptional()) {
			    results.add(new ChangeResult(ca.getName(),
					"Can only delete optional attributes",
					false));
			    continue;
			}
			// queue the deletion
			delNames.add(ca.getName());
		    }
		    // report success and on to the next
		    results.add(new ChangeResult(ca.getName(),
				null,true));
		    continue;
		}

		if ( ea == null ) {
		    Attribute aa = all.lookupAttribute(ca.getName());

		    if ( aa == null ) {
			results.add(new ChangeResult(ca.getName(),
				    "No such attribute", false));
			continue;
		    }
		    // The attribute is known, but there's no entry in this
		    // user's profile.  Add a null enrty that will be set (or
		    // ignored) below.
		    ProfileDB.AttributeDB userAttr = profile.getAttribute(aa);
		    profile.addAttribute(userAttr);
		    ea = userAttr;
		}

		if (Attribute.READ_ONLY.equals(ea.getAccess())) {
		    results.add(new ChangeResult(ca.getName(),
				"Attribute not writeable", false));
		    continue;
		}

		// For formatted options, check the format
		if ( ea.getFormat() != null ) {
		    if ( !Pattern.matches(ea.getFormat(), ca.getValue())) {
			results.add(new ChangeResult(ca.getName(),
				    "Misformatted attribute. Expected " + 
				    ea.getFormatDescription(), false));
			continue;
		    }
		}
		ea.setValue(ca.getValue());
		names.add(ca.getName());
		results.add(new ChangeResult(ca.getName(),null, true));
	    }
	    // Commit the changes to the DB
	    if (names.size() > 0 ) profile.save(names);
	    if (delNames.size() > 0 ) profile.remove(delNames);
	    logChangeResults(profile, results);
	    log.info("change" + profile.getTypeCaps() + "Profile for " +
		    id + " complete");
	    return results.toArray(new ChangeResult[0]);
	}
	catch (DeterFault df) {
	    log.error("change" + profile.getTypeCaps() + "Profile for " +
		    id + " failed: " + df);
	    throw df;
	}
    }
    /**
     * Add a new profile attribute. Caller must close profile.
     * @param name the attribute name
     * @param type the type (STRING, INT, FLOAT, OPAQUE)
     * @param optional true if this attribute is optional
     * @param access the user's ability to modify (READ_WRITE, READ_ONLY,
     *	    WRITE_ONLY, NO_ACCESS)
     * @param description natural language description of the field
     * @param format a regular expression describing the format (optional)
     * @param formatdescription a natural language explanation of the format
     * @param order the ordering of this field relative to others (0 means put
     * it last)
     * @param length a suggestion of the field's length for UI presentation
     * @param def default value of the attribute (will be assigned to all users)
     * @param profile a profile of the type to add the attibute to
     * @throws DeterFault on errors
     */
    protected boolean createAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    String def, ProfileDB profile) 
	throws DeterFault {
	log.info("create" +profile.getTypeCaps() + "Attribute " +
		name + " default "  + def);
	try {
	    if (!Attribute.validateAccess(access))
		throw new DeterFault(DeterFault.request,
			"Bad access type for attribute " + access);
	    checkAccess("create" + profile.getTypeCaps() +"Attribute",
		    new CredentialSet("system", null),
		    profile.getSharedConnection());
	    ProfileDB.AttributeDB ua =
		profile.getAttribute(new Attribute(name, type, optional,
		    access, description, format, formatdescription, order, 
		    length));
	    ua.create(def);
	    log.info("create" + profile.getTypeCaps() + "Attribute " +
		    name + " default "  + def + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("create" + profile.getTypeCaps() + "Attribute " +
		    name + " failed: " + df);
	    throw df;
	}
    }

    /**
     * Modify the schema of an existing attribute.  All fields overwrites
     * existing ones, so to change a subset read the attribute's schema using
     * GetProfileDescription.
     * @param name the attribute name
     * @param type the type (STRING, INT, FLOAT, OPAQUE)
     * @param optional true if this attribute is optional
     * @param access the user's ability to modify (READ_WRITE, READ_ONLY,
     *	    WRITE_ONLY, NO_ACCESS)
     * @param description natural language description of the field
     * @param format a regular expression describing the format (optional)
     * @param formatdescription a natural language explanation of the format
     * @param order the ordering of this field relative to others (0 means put
     * it last)
     * @param length a suggestion of the field's length for UI presentation
     * @param profile a profile of the type to modify the attibute in
     * @throws DeterFault on errors
     */
    protected boolean modifyAttribute(String name, String type, 
	    boolean optional, String access, String description, 
	    String format, String formatdescription, int order, int length,
	    ProfileDB profile) 
	throws DeterFault {
	log.info("modify" + profile.getTypeCaps() + "Attribute " + name );
	try {
	    if (!Attribute.validateAccess(access))
		throw new DeterFault(DeterFault.request,
			"Bad access type for attribute " + access);
	    checkAccess("modify" + profile.getTypeCaps() + "Attribute",
		    new CredentialSet("system", null),
		    profile.getSharedConnection());
	    ProfileDB.AttributeDB ua =
		profile.getAttribute(new Attribute(name, type, optional,
		    access, description, format, formatdescription, order,
		    length));
	    ua.modifySchema();
	    log.info("modify" + profile.getTypeCaps() + "Attribute " +
		    name + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("modify" + profile.getTypeCaps() + "Attribute " +
		    name + " failed: " + df);
	    throw df;
	}
    }

    /**
     * Remove an attribute from all experiment.  The schema is
     * removed.
     * @param name the attribute to remove
     * @param profile a profile of the type to remove the attibute from
     * @throws DeterFault on error
     */
    protected boolean removeAttribute(String name, ProfileDB profile)
	    throws DeterFault {
	log.info("remove" + profile.getTypeCaps() + "Attribute " + name );
	try {
	    checkAccess("remove" + profile.getTypeCaps() + "Attribute",
		    new CredentialSet("system", null),
		    profile.getSharedConnection());
	    ProfileDB.AttributeDB ua =
		profile.getAttribute(new Attribute(name, null, true,
		    Attribute.NO_ACCESS, null, null, null, 0, 0));
	    ua.removeFromSchema();
	    log.info("remove" + profile.getTypeCaps() + "Attribute " +
		    name + " succeeded");
	    return true;
	}
	catch (DeterFault df) {
	    log.error("remove" + profile.getTypeCaps() + "Attribute " +
		    name + " failed: " + df);
	    throw df;
	}
    }

    /**
     * Confirm that the profile supplied is sufficient to create an object and
     * that no extraneous attributes are present.  On completion, newProfile
     * has been constructed from the profile list.  The caller must close
     * newProfile and empty.
     * @param profile the profile (attribute array) to check
     * @param newProfile the new profile to complete
     * @param empty an empty profile to work from
     * @throws DeterFault if there are missing fields, format errors, or extra
     * fields
     */
    protected void checkProfile(Attribute[] profile,
	    ProfileDB newProfile, ProfileDB empty)
	throws DeterFault {

	empty.loadAll();
	Collection<ProfileDB.AttributeDB> allAttrs = empty.getAttributes();
	Set<String> validAttrNames = new TreeSet<String>();

	if ( profile == null || profile.length == 0)
	    throw new DeterFault(DeterFault.request,
		    "No profile passed to profiled object!");

	for (ProfileDB.AttributeDB ua: allAttrs)
	    validAttrNames.add(ua.getName());

	// Move the user-supplied profile into a new profile.  Confirm that the
	// user has not supplied any unnamed or unknown attributes.
	for (Attribute ua: profile) {
	    String aName = ua.getName();

	    if ( aName == null)
		throw new DeterFault(DeterFault.request,
			"Attribute without a name!?");
	    if ( !validAttrNames.contains(aName))
		throw new DeterFault(DeterFault.request,
			"Unknown Attribute in profile: " + aName);
	    if ( ua.getValue() != null)
		newProfile.addAttribute(newProfile.getAttribute(ua));
	}

	// Validate the required fields in the profile
	for (ProfileDB.AttributeDB ua: allAttrs) {
	    if ( ua.getOptional() && ua.getFormat() == null ) continue;
	    Attribute nua = newProfile.lookupAttribute(
		    ua.getName());
	    String value = (nua != null ) ? nua.getValue() : null;
	    String format = (nua != null ) ? nua.getFormat() : null;

	    // Checks for required options
	    if (!ua.getOptional() ) {
		if ( nua == null )
		    throw new DeterFault(DeterFault.request,
			    "Required attribute " + ua.getName() +
			    " not present");
		if ( value == null || value.length() == 0 )
		    throw new DeterFault(DeterFault.request,
			    "Required attribute " + ua.getName() +
			    " NULL or zero-length");
	    }
	    // Check any attribute with a format and a value.
	    if ( format != null && value != null &&
		    !Pattern.matches(format, value))
		throw new DeterFault(DeterFault.request,
			"Attribute " + ua.getName() +
			" badly formatted." + format + " " + value);
	}
    }
}
