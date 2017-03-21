package org.docma.util;

import java.io.IOException;

/**
 * Interface for reading and modifying XML. 
 * 
 * @author MP
 */
public interface XMLProcessor 
{
    /**
     * Registers a handler for the given element name.
     * Note that an element can only be processed by one handler.
     * That means, any previous registrations for the same element name
     * are overwritten by the invocation of this method.
     * <p>
     * If <code>handler</code> is <code>null</code>, then any previously
     * registered handler for this element name is unregistered.
     * </p>
     * 
     * @param elementName  the name of the element to be handled 
     * @param handler  the handler instance, or <code>null</code>
     * @see #setElementHandler(XMLElementHandler)
     */
    void setElementHandler(String elementName, XMLElementHandler handler);

    /**
     * Registers a default handler for all elements.
     * Note that an element can only be processed by one handler.
     * If a handler is registered for a specific element through
     * {@link #setElementHandler(String, XMLElementHandler)}, then
     * this handler is used instead of the default handler.
     * <p>
     * If <code>handler</code> is <code>null</code>, then any previously
     * registered default handler is unregistered.
     * </p>
     * 
     * @param handler  the default handler, or <code>null</code>
     * @see #setElementHandler(String, XMLElementHandler)
     */
    void setElementHandler(XMLElementHandler handler);
    
    boolean isIgnoreElementCase();
    void setIgnoreElementCase(boolean ignore);
    boolean isIgnoreAttributeCase();
    void setIgnoreAttributeCase(boolean ignore);
    void process(String input) throws XMLParseException;
    void process(String input, Appendable output) throws XMLParseException, IOException;
}
