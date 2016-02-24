package net.deterlab.testbed.experiment;

import java.util.Collection;
import java.util.List;

import net.deterlab.testbed.api.AccessMember;
import net.deterlab.testbed.api.ChangeResult;
import net.deterlab.testbed.api.ExperimentAspect;
import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.db.SharedConnection;

/**
 * Read only version of an experiment's Database representation.  This is not
 * intended to absolutely prevent DB changes, but do discourage accidental
 * ones.
 * @author DETER team
 * @version 1.1
 */

public class ImmutableExperimentDB extends ExperimentDB {
    /**
     * This is an experiment aspect including both its database and filesystem
     * storage.  This acts as a DBObject, but piggybacks on the shared
     * connection of the enclosing ExperimentDB.  This is an adapter that
     * blocks calls that modify the DB or file system.
     * @author DETER Team
     * @version 1.0
     */
    public class ExperimentAspectDB extends ExperimentDB.ExperimentAspectDB {
	/**
	 * Create an empty ExperimentAspectDB
	 */
	public ExperimentAspectDB() {
	    super();
	}

	/**
	 * Create an ExperimentAspectDB with the naming parameters set.
	 * @param t the type
	 * @param st the subtype
	 * @param n the name
	 */
	public ExperimentAspectDB(String t, String st, String n) {
	    super(t, st, n);
	}

	/**
	 * Store the members in the DB and the filesystem,  The aspect is
	 * overwritten if it exists.
	 * @param putData if true write data to the file system
	 * @param create if true throw an exception if the aspect or the
	 * underlying file exists.
	 * @param force if true, overwrite read only aspects
	 * @throws DeterFault on errors
	 */
	public void save(boolean putData, boolean create, boolean force)
		throws DeterFault {
	    throw new DeterFault(DeterFault.internal,
		    "Attempt to save ImmutableExperimentDB Aspect. Eid:" +
		    getEid());
	}

	/**
	 * Remove the Aspect from the DB and filesystem.
	 * @throws DeterFault on errors
	 */
	public void remove() throws DeterFault {
	    throw new DeterFault(DeterFault.internal,
		    "Attempt to remove ImmutableExperimentDB Aspect. Eid:" +
		    getEid());
	}

    }

    /**
     * Create an ImmutableExperimentDB with the given eid.
     * @param e the eid
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ImmutableExperimentDB(String e) throws DeterFault {
	super(e, null);
    }

    /**
     * Create an ImmutableExperimentDB with the given eid that shares a DB
     * connection
     * @param e the eid
     * @param sc the shared connection
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ImmutableExperimentDB(String e, SharedConnection sc)
	    throws DeterFault {
	super(e, sc);
    }

    /**
     * Create an Immutable version of the given experiment.
     * @param exp experiment from which to derive the immutable experiment
     * @throws DeterFault if the name is badly formatted or there is a DB setup
     * error.
     */
    public ImmutableExperimentDB(ExperimentDB exp) throws DeterFault {
	super(exp.getEid(), exp.getSharedConnection());
    }

    /**
     * Remove and regenerate the credentials for all the circles in this
     * experiment's access control list.
     * @throws DeterFault always
     */
    public void updateCircleCredentials() throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to updateCircleCredentials on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
    /**
     * Clear any old policy for this experiment and insert the current one.
     * @throws DeterFault always
     */
    public void updatePolicyCredentials() throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to updatePolicyCredentials on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }

    /**
     * Remove all credentials attached to this project
     * @throws DeterFault always
     */
    public void removeCredentials() throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to removeCredentials on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }

    /**
     * Update credentials when onwership changes.
     * @param oldOwner the old owner - may be null
     * @param newOwner the new owner
     * @throws DeterFault always
     */
    public void updateOwnerCredentials(String oldOwner, String newOwner)
	    throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to updateOwnerCredentials on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
    /**
     * Store this experimentDB in the database, including the aspects and
     * credentials.
     * @param owner the experiment owner
     * @param aspects the aspects
     * @param inAcl the access control list
     * @throws DeterFault always
     */
    public void create(String owner, ExperimentAspect[] aspects,
	    Collection<AccessMember> inAcl) throws DeterFault {

	throw new DeterFault(DeterFault.internal,
		"Attempt to create on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }

    /**
     * Set the experiment's owner to the new uid.
     * @param o the new onwer's uid
     * @throws DeterFault if there is a DB problem
     */
    public void setOwner(String o) throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to setOwner on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }

    /**
     * Remove a set of aspects.
     * @param aspects ExperimentAspects encoding the aspects to remove.
     * @return the aspects that were successfully removed.
     * @throws DeterFault always
     */
    public List<ChangeResult> removeAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to removeAspects on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
    /**
     * Change a set of aspects.  The aspects to add are passed as
     * as ExperimentAspects that contain the Aspect-specific incremental update
     * instructions in the Data or DataReference field.  Changes may may result
     * in subaspects changing as well.
     * @param aspects ExperimentAspects to change
     * @return the aspects that were successfully changed
     * @throws DeterFault if there is an error
     */
    public List<ChangeResult> changeAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to changeAspects on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }

    /**
     * Remove the experiment from the database and remove config files stored
     * for it.  Errors from the database cause faults to be thrown.
     * @throws DeterFault always
     */
    public void remove() throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to remove " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
    /**
     * Add a set of aspects.
     * @param aspects ExperimentAspects to add
     * @return the aspects that were successfully added.
     * @throws DeterFault always
     */
    public List<ChangeResult> addAspects(
	    Collection<ExperimentAspect> aspects) throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to addAspects to " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
    /**
     * Update the access control list for this object with the values in m.
     * @param m the access info to change
     * @throws DeterFault always
     */
    public void assignPermissions(AccessMember m) throws DeterFault {
	throw new DeterFault(DeterFault.internal,
		"Attempt to assignPermissions on " +
		"ImmutableExperimentDB. Eid: " + getEid());
    }
}
