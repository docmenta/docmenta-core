package org.docma.util;

/**
 *
 * @author g710545
 */
public interface XMLElementContext 
{
    String getElementName();
    boolean isEmptyElement();
    int getAttributeCount();
    int getAttributeIndex(String attName);
    String getAttributeName(int idx);
    String getAttributeValue(int idx);
    int getCharacterOffset();
    String getElementContent();
    
    void replaceElement(String xml);
    void replaceElementContent(String content);
    void setAttribute(String name, String value);
    
}
