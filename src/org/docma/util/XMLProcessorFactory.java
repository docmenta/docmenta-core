package org.docma.util;

import org.docma.util.impl.SimpleXMLProcessor;

/**
 *
 * @author MP
 */
public class XMLProcessorFactory 
{
    private XMLProcessorFactory()
    {
    }
    
    public static XMLProcessor newInstance() 
    {
        return new SimpleXMLProcessor();
    }
}
