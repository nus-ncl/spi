
package net.deterlab.testbed.util;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * A utility to validat an xml file against a schema
 * @author the DETER Team
 * @version 1.0
 */
public class ValidateXML extends Utility {

    /**
     * Test a topdl parse
     * @param args are ignored
     */
    static public void main(String[] args) {
	try {
	    if (args.length < 2) 
		fatal("Usage: xsd file");

	    SchemaFactory fac = SchemaFactory.newInstance(
		    XMLConstants.W3C_XML_SCHEMA_NS_URI);
	    Schema schema = fac.newSchema(new StreamSource(new File(args[0])));
	    Validator v = schema.newValidator();

	    v.validate(new StreamSource(new File(args[1])));
	    System.out.println("OK!");
	}
	catch (IOException e) {
	    fatal(e.getMessage());
	}
	catch (SAXException e) {
	    fatal(e.getMessage());
	}
	catch (Exception e) {
	    e.printStackTrace();
	    fatal(e.getMessage());
	}
    }
}
