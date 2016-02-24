package net.deterlab.testbed.util.gui;

import javax.swing.JTextField;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.text.Document;

public class UtilityTextField extends JTextField {
    protected String typed;

    /**
     * JTextFields erase the text typed into the field unless the  user types
     * return.  That's counter-intuitive, so this class  updates a shadow copy
     * of the text.
     */
    private class UpdateTyped implements DocumentListener {
	/**
	 * Attributes of the Document have changed, we don't care.  
	 * @param e an ignored event
	 * */
	public void changedUpdate(DocumentEvent e) { }
	/**
	 * Text added to the Document, save the current text.
	 * @param e an ignored event
	 */
	public void insertUpdate(DocumentEvent e) { typed = getText(); }
	/**
	 * Text removed from the Document, save the current text.
	 * @param e an ignored event
	 */
	public void removeUpdate(DocumentEvent e) { typed = getText(); }
    }

    /** Set up listeners to keep typed updated */
    private void init() {
	getDocument().addDocumentListener(new UpdateTyped());
    }

    /**
     * New UtilityTextField
     */
    public UtilityTextField() {
	super();
	init();
    }

    /**
     * New UtilityTextField that uses the given text storage model and the
     * given number of columns.
     * @param doc the document model
     * @param text the initial text
     * @param columns the number of columns
     */
    public UtilityTextField(Document doc, String text, int columns) {
	super(doc, text, columns);
	init();
    }

    /**
     * New UtilityTextField with the specified number of columns.
     * @param columns the number of columns
     */
    public UtilityTextField(int columns) {
	super(columns);
	init();
    }

    /**
     * New UtilityTextField initialized with the specified text.
     * @param text the initial text
     */
    public UtilityTextField(String text) {
	super(text);
	init();
    }

    /**
     * New UtilityTextField initialized with the specified text and columns.
     * @param text the initial text
     * @param columns the number of columns
     */
    public UtilityTextField(String text, int columns) {
	super(text, columns);
	init();
    }

    /**
     * Return the text that reflects the most recent typing with or without a
     * return.
     * @return the text that reflects the most recent typing 
     */
    public String getValue() { return typed; }
}
