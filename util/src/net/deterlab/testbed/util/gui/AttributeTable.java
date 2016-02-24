package net.deterlab.testbed.util.gui;

import java.util.Set;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

import net.deterlab.testbed.api.Attribute;
import net.deterlab.testbed.client.CirclesStub;
import net.deterlab.testbed.client.ExperimentsStub;
import net.deterlab.testbed.client.LibrariesStub;
import net.deterlab.testbed.client.ProjectsStub;
import net.deterlab.testbed.client.UsersStub;

/**
 * A table model that displays a Profile (array of attributes).  These methods
 * provide the information to display the underlying array of attributes in a
 * JTable.  This is some very clunky polymorphism because the client classes do
 * not recognize that all Attributes have a common interface.
 * @author DETER Team
 * @version 1.0
 */
public class AttributeTable extends AbstractTableModel {
    /** The attributes to display */
    protected CirclesStub.Attribute[] cattrs;
    /** The attributes to display */
    protected ExperimentsStub.Attribute[] eattrs;
    /** The attributes to display */
    protected LibrariesStub.Attribute[] lattrs;
    /** The attributes to display */
    protected ProjectsStub.Attribute[] pattrs;
    /** The attributes to display */
    protected UsersStub.Attribute[] uattrs;
    /** The set of changed attributes */
    protected Set<String> changed;
    /** The names of the columns displayed */
    static protected String[] columnNames = new String[] {
	"Name", "Value", "Description" };

    /**
     * Constructor
     * @param a the underlying array of attributes
     */
    public AttributeTable(Object a) {
	cattrs = null;
	eattrs = null;
	pattrs = null;
	uattrs = null;
	if ( a instanceof CirclesStub.Attribute[])
	    cattrs = (CirclesStub.Attribute[]) a;
	else if ( a instanceof ExperimentsStub.Attribute[])
	    eattrs = (ExperimentsStub.Attribute[]) a;
	else if ( a instanceof LibrariesStub.Attribute[])
	    lattrs = (LibrariesStub.Attribute[]) a;
	else if ( a instanceof ProjectsStub.Attribute[])
	    pattrs = (ProjectsStub.Attribute[]) a;
	else if ( a instanceof UsersStub.Attribute[])
	    uattrs = (UsersStub.Attribute[]) a;
	changed = new TreeSet<String>();
    }

    /**
     * Return the number of rows
     * @return the number of rows
     */
    public int getRowCount() { 
	if ( cattrs != null ) return cattrs.length;
	else if ( eattrs != null ) return eattrs.length;
	else if ( lattrs != null ) return lattrs.length;
	else if ( pattrs != null ) return pattrs.length;
	else if ( uattrs != null ) return uattrs.length;
	else return 0;
    }
    /**
     * Return the number of columns
     * @return the number of columns
     */
    public int getColumnCount() { return 3; }
    /**
     * Return the name of column c
     * @param c the index of the column
     * @return the name of column c
     */
    public String getColumnName(int c) { return columnNames[c]; }
    /**
     * Return the value at row r column c.  This translates the attribute
     * into strings.
     * @param r the row to get
     * @param c the column to get
     * @return the value at row r column c.
     */
    public Object getValueAt(int r, int c) {
	switch (c) {
	    case 0:
		if ( cattrs != null ) return cattrs[r].getName();
		else if ( eattrs != null ) return eattrs[r].getName();
		else if ( lattrs != null ) return lattrs[r].getName();
		else if ( pattrs != null ) return pattrs[r].getName();
		else if ( uattrs != null ) return uattrs[r].getName();
		else return null;
	    case 1:
		if ( cattrs != null ) return cattrs[r].getValue();
		else if ( eattrs != null ) return eattrs[r].getValue();
		else if ( lattrs != null ) return lattrs[r].getValue();
		else if ( pattrs != null ) return pattrs[r].getValue();
		else if ( uattrs != null ) return uattrs[r].getValue();
		else return null;
	    case 2:
		if ( cattrs != null ) return cattrs[r].getDescription();
		else if ( eattrs != null ) return eattrs[r].getDescription();
		else if ( lattrs != null ) return lattrs[r].getDescription();
		else if ( pattrs != null ) return pattrs[r].getDescription();
		else if ( uattrs != null ) return uattrs[r].getDescription();
		else return null;
	    default:
		return null;
	}
    }

    /**
     * Return the class of data in column c
     * @param c the column
     * @return the class of data in column c
     */
    public Class<?> getColumnClass(int c) { return String.class; }

    /**
     * Change the value of the editable value at row r, column c to be v.
     * If the value differs from the current value, save the name of the
     * attribute column as changed.
     * @param v the new value
     * @param r the row to change
     * @param c the column to change
     */
    public void setValueAt(Object v, int r, int c) {
	if ( c != 1 ) return;
	if ( !(v instanceof String) )
	    return;
	String newValue = (String) v;
	String oldValue = null;
	String name = null;

	if ( cattrs != null ) {
	    oldValue = cattrs[r].getValue();
	    name = cattrs[r].getName();
	}
	else if ( eattrs != null ) {
	    oldValue = eattrs[r].getValue();
	    name = eattrs[r].getName();
	}
	else if ( lattrs != null ) {
	    oldValue = lattrs[r].getValue();
	    name = lattrs[r].getName();
	}
	else if ( pattrs != null ) {
	    oldValue = pattrs[r].getValue();
	    name = pattrs[r].getName();
	}
	else if ( uattrs != null ) {
	    oldValue = uattrs[r].getValue();
	    name = uattrs[r].getName();
	}

	if ( newValue != null && oldValue == null ) 
	    changed.add(name);
	else if (!oldValue.equals(newValue))
	    changed.add(name);

	if ( cattrs != null ) cattrs[r].setValue(newValue);
	else if ( eattrs != null ) eattrs[r].setValue(newValue);
	else if ( lattrs != null ) lattrs[r].setValue(newValue);
	else if ( pattrs != null ) pattrs[r].setValue(newValue);
	else if ( uattrs != null ) uattrs[r].setValue(newValue);
    }

    /**
     * Return true if the cell at row r column c is editable.  In this
     * case, that is so for values that are READ_WRITE or WRITE_ONLY.
     * @param r the row 
     * @param c the column
     * @return true if the cell at row r column c is editable.  
     */
    public boolean isCellEditable(int r, int c) {
	String access = null;

	if ( cattrs != null ) access = cattrs[r].getAccess();
	else if ( eattrs != null ) access = eattrs[r].getAccess();
	else if ( lattrs != null ) access = lattrs[r].getAccess();
	else if ( pattrs != null ) access = pattrs[r].getAccess();
	else if ( uattrs != null ) access = uattrs[r].getAccess();
	
	return c == 1 && (Attribute.READ_WRITE.equals(access) || 
		Attribute.WRITE_ONLY.equals(access));
    }
    /**
     * Return a set of attribute names that have had their values changed.
     * @return a set of attribute names that have had their values changed.
     */
    public Set<String> getChanged() { return changed; }
}
