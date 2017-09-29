package org.docma.util.impl;

import java.util.List;
import org.docma.util.XMLElementContext;

/**
 *
 * @author MP
 */
public class XMLElementContextImpl implements XMLElementContext
{
    private String input;
    private int openTagStart;
    private int openTagEnd;
    private int closeTagStart;
    private int closeTagEnd;
    private boolean emptyElement;
    private String tagName;
    private List<String> attNames; 
    private List<String> attValues;
    private boolean ignoreAttributeCase = true;
    
    private String replacedElem = null;
    private String replacedCont = null;
    private boolean attribChanged = false;
    

    XMLElementContextImpl(String input, int tagStart, int tagEnd, 
                          boolean isEmpty, String tagName, 
                          List<String> attNames, List<String> attValues) 
    {
        this.input = input;
        this.openTagStart = tagStart;
        this.openTagEnd = tagEnd;
        this.closeTagStart = -1;
        this.closeTagEnd = -1;
        this.emptyElement = isEmpty;
        this.tagName = tagName;
        this.attNames = attNames;
        this.attValues = attValues;
    }
    
    void setClosingTagOffset(int tagStart, int tagEnd)
    {
        this.closeTagStart = tagStart;
        this.closeTagEnd = tagEnd;
    }

    void setIgnoreAttributeCase(boolean ignore) 
    {
        ignoreAttributeCase = ignore;
    }

    int getOpenTagStart() 
    {
        return openTagStart;
    }

    int getOpenTagEnd() 
    {
        return openTagEnd;
    }

    int getCloseTagStart() 
    {
        return closeTagStart;
    }

    int getCloseTagEnd() 
    {
        return closeTagEnd;
    }

    String getReplacedElement()
    {
        return replacedElem;
    }
    
    String getReplacedContent()
    {
        return replacedCont;
    }
    
    boolean attributesChanged()
    {
        return attribChanged;
    }
    
    /* ----------- Interface methods -------------- */

    public String getElementName() 
    {
        return tagName;
    }

    public boolean isEmptyElement() 
    {
        return emptyElement;
    }

    public int getAttributeCount() 
    {
        return (attNames == null) ? 0 : attNames.size();
    }

    public int getAttributeIndex(String name) 
    {
        if (attNames == null) {
            return -1;
        }
        if (ignoreAttributeCase) {
            for (int i = 0; i < attNames.size(); i++) {
                if (name.equalsIgnoreCase(attNames.get(i))) {
                    return i;
                }
            }
            return -1;
        } else {
            return attNames.indexOf(name);
        }
    }

    public String getAttributeName(int idx) 
    {
        return attNames.get(idx);
    }

    public String getAttributeValue(int idx) 
    {
        return attValues.get(idx);
    }

    public String getAttributeValue(String attName) 
    {
        int idx = getAttributeIndex(attName);
        return (idx < 0) ? null : attValues.get(idx);
    }

    public int getCharacterOffset() 
    {
        return openTagStart;
    }

    public String getElement()
    {
        if (emptyElement || (closeTagEnd <= openTagEnd)) {
            return input.substring(openTagStart, openTagEnd);
        } else {
            return input.substring(openTagStart, closeTagEnd);
        }
    }
    
    public String getElementContent() 
    {
        if (emptyElement || (closeTagStart <= openTagEnd)) {
            return "";
        } else {
            return input.substring(openTagEnd, closeTagStart);
        }
    }

    public void replaceElement(String xml) 
    {
        replacedElem = xml;
    }

    public void replaceElementContent(String content) 
    {
        replacedCont = content;
    }

    public void setAttribute(String name, String value) 
    {
        int idx = getAttributeIndex(name);
        if (value == null) {
            if (idx >= 0) {
                attNames.remove(idx);
                attValues.remove(idx);
                attribChanged = true;
            }
        } else {
            if (idx >= 0) {
                attValues.set(idx, value);
            } else {
                attNames.add(name);
                attValues.add(value);
            }
            attribChanged = true;
        }
    }
    
}
