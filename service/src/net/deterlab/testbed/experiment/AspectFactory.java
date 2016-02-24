package net.deterlab.testbed.experiment;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import net.deterlab.testbed.api.Config;
import net.deterlab.testbed.api.DeterFault;

/**
 * Produces Aspects based on the type of the aspect.
 * @author DETER Team
 * @version 1.0
 */
public class AspectFactory {
    /** The map of instantiated aspects */
    static protected Map<String, Aspect> aspects = null;
    /** Constructores for Aspects */
    static protected Map<String, Constructor> constructors = null;
    /** Constructor for default aspects */
    static protected Constructor defaultConstructor = null;
    /** Set of transaction IDs known to be in use */
    static protected Set<Long> tids = new TreeSet<Long>();
    /** This is complex and weird enough that logging may be useful */
    private Logger log;


    /**
     * Create a new AspectFactory.  If this is the first one created, read
     * configuration files and load aspect constructors.  Otherwise, just
     * connect to the static data structures implicitly.  Log problems.
     * @throws DeterFault on initialization errors
     */
    public AspectFactory() throws DeterFault {
	log = Logger.getLogger(this.getClass());

	if ( aspects != null ) return;

	aspects = new HashMap<String, Aspect>();
	constructors = new HashMap<String, Constructor>();

	Config config = new Config();
	String aspectFileName = config.getAspectClassFile();

	if ( aspectFileName == null ) return;

	try {
	    LineNumberReader l = new LineNumberReader(
		    new FileReader(new File(aspectFileName)));
	    String s = null;

	    while ((s = l.readLine()) != null) {
		String parts[] = s.split("\\s+");

		if (parts.length != 2) continue;

		try {
		    Class<?> cl = Class.forName(parts[1]);
		    Constructor c = cl.getConstructor(new Class[] {
			String.class });

		    if (parts[0].equals("*")) defaultConstructor = c;
		    else constructors.put(parts[0], c);
		}
		catch (LinkageError le) {
		    log.error("Error linking " + parts[1] + ": " + le);
		}
		catch (ClassNotFoundException ce) {
		    log.error("Cannot find class " + parts[1] + ": " + ce);
		}
		catch (NoSuchMethodException ne) {
		    log.error("Cannot find constructor for " + parts[1] +
			    ": " + ne);
		}
	    }
	} catch (IOException ie) {
	    log.error("Cannot read Aspect configuration: " + ie);
	}
    }

    /**
     * Produce an Aspect.  If the needed class has been instantiated before,
     * use it.  If not, if there is a specific constructor for this type,
     * instantiate one of those classes.  If not, and there is a default
     * constructor, instantiate one of those.  Log errors.
     * @param type the aspect type
     * @return an appropriate generator
     * @throws DeterFault if no such ASpect can be built
     */
    public Aspect getInstance(String type) throws DeterFault {
	Aspect rv = aspects.get(type);

	if (rv != null ) return rv;

	try {
	    Constructor c = constructors.get(type);

	    if ( c == null ) {
		if (defaultConstructor == null ) return rv;
		rv = (Aspect) defaultConstructor.newInstance(new Object[] {
		    type });
	    }
	    else {
		rv = (Aspect) c.newInstance(new Object[] { type });
	    }
	}
	catch (LinkageError le) {
	    log.error("Error linking on type " + type + ": " + le);
	}
	catch (ReflectiveOperationException re ) {
	    log.error("Reflection Error on type " + type + ": " + re);
	}
	catch (IllegalArgumentException ie) {
	    log.error("Illegal argument on type " + type + ": " + ie);
	}

	if ( rv == null )
	    throw new DeterFault(DeterFault.internal,
		    "Cannot create Aspect for " + type);

	aspects.put(type, rv);
	return rv;
    }

    /**
     * Return a transaction ID not in use.  This does not expect to have
     * billions of tids in use, and if so, performance will suffer.
     * @return a transaction ID not in use.
     */
    public long getTransactionID() {
	Random r = new Random();
	long tid = r.nextLong();

	while (tids.contains(tid) )
	    tid = r.nextLong();
	tids.add(tid);
	return tid;
    }

    /**
     * Indicate that the given transaction ID is no longer in use.
     * finalizeTransaction should have been called on that ID for any aspects
     * using it.
     * @param tid the ID to return
     */
    public void releaseTransactionID(long tid) {
	tids.remove(tid);
    }
}
