package org.docma.util;

/**
 *
 * @author MP
 */
public class XMLParseException extends Exception 
{

    /**
     * Creates a new instance of <code>XMLParseException</code> without detail
     * message.
     */
    public XMLParseException() 
    {
    }

    /**
     * Constructs an instance of <code>XMLParseException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public XMLParseException(String msg) 
    {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>XMLParseException</code> with the
     * specified root cause.
     *
     * @param rootCause  the root cause.
     */
    public XMLParseException(Throwable rootCause) 
    {
        super(rootCause);
    }
}
