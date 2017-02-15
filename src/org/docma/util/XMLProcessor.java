package org.docma.util;

import java.io.IOException;

/**
 * Interface for reading and modifying XML. 
 * 
 * @author MP
 */
public interface XMLProcessor 
{
    void setElementHandler(String elementName, XMLElementHandler handler);
    boolean isIgnoreElementCase();
    void setIgnoreElementCase(boolean ignore);
    boolean isIgnoreAttributeCase();
    void setIgnoreAttributeCase(boolean ignore);
    void process(String input) throws XMLParseException;
    void process(String input, Appendable output) throws XMLParseException, IOException;
}
