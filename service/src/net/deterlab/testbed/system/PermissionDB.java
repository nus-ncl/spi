package net.deterlab.testbed.system;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import net.deterlab.testbed.api.DeterFault;

import net.deterlab.testbed.db.DBObject;
import net.deterlab.testbed.db.SharedConnection;

public class PermissionDB extends DBObject {
    /** The name of the permission */
    private String name;
    /** The name of the kind of object to which the permission applies */
    private String obj;

    /**
     * Create a permissionsDB entry with all the trimmings.
     * @param n the name
     * @param o the object
     * @param sc a shared database connection
     * @throws DeterFault if there is difficulty with teh DB
     */
    public PermissionDB(String n, String o, SharedConnection sc)
	throws DeterFault {
	super(sc);
	name = n;
	obj = o;
    }

    /**
     * Create an empty permissionsDB 
     * @throws DeterFault if there is difficulty with teh DB
     */
    public PermissionDB() throws DeterFault { this(null, null, null); }

    /**
     * Create an empty permissionsDB entry with a shared DB connection
     * @param sc a shared database connection
     * @throws DeterFault if there is difficulty with teh DB
     */
    public PermissionDB(SharedConnection sc) throws DeterFault {
	this(null, null, sc);
    }
    /**
     * Create a permissionsDB entry with a name and object.
     * @param n the name
     * @param o the object
     * @throws DeterFault if there is difficulty with teh DB
     */
    public PermissionDB(String n, String o)
	throws DeterFault { this(n, o, null); }

    /**
     * Return the permission name.
     * @return the permission name.
     */
    public String getName() { return name; }
    /**
     * Set the permission name.
     * @param n the new permission name.
     */
    public void setName(String n) { name = n; }
    /**
     * Return the permission object.
     * @return the permission object.
     */
    public String getObject() { return obj; }
    /**
     * Set the permission object.
     * @param o the new permission object.
     */
    public void setObject(String o) { obj = o; }

    /**
     * Add this permission to the DB.
     * @throws DeterFault if the permission is invalid, exists, or if there are
     * other DB problems.
     */
    public void create() throws DeterFault {
	try {
	    if ( getName() == null )
		throw new DeterFault(DeterFault.request,
			"No permission name given");
	    if ( getObject() == null )
		throw new DeterFault(DeterFault.request,
			"No permission object given");
	    PreparedStatement p = getPreparedStatement(
		    "INSERT INTO permissions (name, valid_for) " +
			"VALUES (?, ?)");
	    p.setString(1, getName());
	    p.setString(2, getObject());
	    p.executeUpdate();
	}
	catch (SQLIntegrityConstraintViolationException e) {
	    throw new DeterFault(DeterFault.request, "Permission exists");
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Error creating permission: " +e);
	}
    }

    /**
     * Remove a permission from the system, including revoking the permission
     * to any object to which it has been granted.  This is sort of dangerous.
     * @return true on success
     * @throws DeterFault if the permission is invalid, exists, or if there are
     * other DB problems.
     */
    public boolean remove() throws DeterFault {
	try {
	    if ( getName() == null )
		throw new DeterFault(DeterFault.request,
			"No permission name given");
	    if ( getObject() == null )
		throw new DeterFault(DeterFault.request,
			"No permission object given");
	    PreparedStatement p = getPreparedStatement(
		    "DELETE FROM " + getObject()+"perms " +
		    "WHERE permidx =" +
			"(SELECT idx FROM permissions WHERE name=?" +
			    "AND valid_for=?)");
	    p.setString(1, getName());
	    p.setString(1, getObject());
	    p.executeUpdate();
	    p = getPreparedStatement("DELETE FROM permissions " +
			"WHERE name=? AND valid_for=?)");
	    p.setString(1, getName());
	    p.setString(1, getObject());
	    int rc = p.executeUpdate();
	    if ( rc < 1 ) 
		throw new DeterFault(DeterFault.request, "No such permission");
	    else if (rc > 1) 
		throw new DeterFault(DeterFault.internal,
			"More than one permission!?");
	    return true;
	}
	catch (SQLException e) {
	    throw new DeterFault(DeterFault.internal,
		    "Error removing permission: " +e);
	}
    }
}
