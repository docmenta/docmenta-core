package org.docma.util.impl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.docma.util.XMLElementHandler;
import org.docma.util.XMLParseException;
import org.docma.util.XMLParser;
import org.docma.util.XMLProcessor;

/**
 *
 * @author MP
 */
public class SimpleXMLProcessor implements XMLProcessor
{
    private XMLParser xmlParser = null;
    private String input = null;
    private boolean ignoreElementCase = true;
    private boolean ignoreAttributeCase = true;
    private XMLElementHandler default_handler = null;
    private final Map<String, XMLElementHandler> handlers = new HashMap<String, XMLElementHandler>();
    private final List<XMLElementContextImpl> elements = new ArrayList<XMLElementContextImpl>();

    public void setElementHandler(String elementName, XMLElementHandler handler) 
    {
        String key = elementKey(elementName);
        if (handler == null) {
            handlers.remove(key);
        } else {
            handlers.put(key, handler);
        }
    }

    public void setElementHandler(XMLElementHandler handler) 
    {
        default_handler = handler;
    }

    public boolean isIgnoreElementCase() 
    {
        return ignoreElementCase;
    }
    
    public void setIgnoreElementCase(boolean ignore) 
    {
        ignoreElementCase = ignore;
    }
    
    public boolean isIgnoreAttributeCase() 
    {
        return ignoreAttributeCase;
    }

    public void setIgnoreAttributeCase(boolean ignore) 
    {
        ignoreAttributeCase = ignore;
    }

    public void process(String input) throws XMLParseException 
    {
        try {
            process(input, null);
        } catch (IOException ex) {}
    }

    public void process(String input, Appendable output) throws XMLParseException, IOException 
    {
        this.input = input;
        this.xmlParser = new XMLParser(input);
        if (! elements.isEmpty()) {
            elements.clear();
        }
        int currentIdx = 0;
        
        try {
            parseInput();
            
            int copyPos = 0;
            while (currentIdx < elements.size()) {
                XMLElementContextImpl ctx = elements.get(currentIdx++);
                // Copy input up to the start of the element
                if (output != null) {
                    int elemStart = ctx.getOpenTagStart();
                    output.append(input.substring(copyPos, elemStart));
                    copyPos = elemStart;
                }
                String key = elementKey(ctx.getElementName());
                XMLElementHandler h = handlers.get(key);
                if (h == null) {
                    h = default_handler;
                }
                if (h != null) {
                    h.processElement(ctx);
                    if (output != null) {
                        String newElem = ctx.getReplacedElement();
                        boolean isEmpty = ctx.isEmptyElement();
                        int elemEnd = isEmpty ? ctx.getOpenTagEnd() : ctx.getCloseTagEnd();
                        boolean replaced = false;
                        if (newElem != null) {
                            output.append(newElem);
                            copyPos = elemEnd;
                            replaced = true;
                        } else {
                            String newCont = ctx.getReplacedContent();
                            if (ctx.attributesChanged()) {
                                // Rewrite start tag with changed attributes
                                output.append("<").append(ctx.getElementName());
                                writeAttributes(ctx, output);
                                output.append((isEmpty && (newCont == null)) ? "/>" : ">");
                                copyPos = ctx.getOpenTagEnd();
                            } else if (newCont != null) {
                                // Attributes are unchanged but content changed
                                if (isEmpty) {
                                    int idx = input.lastIndexOf("/>", ctx.getOpenTagEnd() - 1);
                                    if (idx > ctx.getOpenTagStart()) { // should always be true
                                        output.append(input.substring(copyPos, idx));
                                        output.append(">");
                                    }
                                } else {
                                    output.append(input.substring(copyPos, ctx.getOpenTagEnd()));
                                }
                                // copyPos = ctx.getOpenTagEnd();
                            }
                            if (newCont != null) {
                                output.append(newCont);
                                output.append("</").append(ctx.getElementName()).append(">");
                                copyPos = elemEnd;
                                replaced = true;
                            }
                        }
                        
                        if (replaced && !isEmpty) {
                            // Skip replaced elements
                            while (currentIdx < elements.size()) {
                                XMLElementContextImpl next = elements.get(currentIdx);
                                if (next.getOpenTagStart() >= elemEnd) {
                                    break;
                                }
                                currentIdx++;
                            }
                        }
                    }
                }
            }
            // Copy remaining characters to output
            if ((output != null) && (copyPos < input.length())) {
                output.append(input.substring(copyPos));
            }
        } catch (XMLParseException xmlex) {
            throw xmlex;
        } catch (Exception ex) {
            throw new XMLParseException(ex);
        }
    }

    private void writeAttributes(XMLElementContextImpl ctx, Appendable output) throws IOException
    {
        int cnt = ctx.getAttributeCount();
        for (int i = 0; i < cnt; i++) {
            String val = ctx.getAttributeValue(i).replace("\"", "&quot;");
            output.append(" ").append(ctx.getAttributeName(i)).append("=\"")
                  .append(val).append("\"");
        }
    }
    
    private void parseInput() throws Exception
    {
        Deque<XMLElementContextImpl> openElements = new ArrayDeque<XMLElementContextImpl>();
        boolean finished = false;
        while (! finished) {
            int nextType = xmlParser.next();
            if (nextType == XMLParser.START_ELEMENT) {
                String tagName = xmlParser.getElementName();
                String key = elementKey(tagName);
                if ((default_handler != null) || handlers.containsKey(key)) {
                    int tagStart = xmlParser.getStartOffset();
                    int tagEnd = xmlParser.getEndOffset();
                    boolean isEmpty = xmlParser.isEmptyElement();
                    List<String> attNames = new ArrayList<String>();
                    List<String> attValues = new ArrayList<String>();
                    xmlParser.getAttributes(attNames, attValues);
                    XMLElementContextImpl ctx = 
                      new XMLElementContextImpl(input, tagStart, tagEnd, isEmpty, tagName, attNames, attValues);
                    ctx.setIgnoreAttributeCase(ignoreAttributeCase);
                    elements.add(ctx);
                    if (! isEmpty) {
                        openElements.addLast(ctx);
                    }
                }
            } else if (nextType == XMLParser.END_ELEMENT) {
                String tagName = xmlParser.getElementName();
                XMLElementContextImpl ctx;
                while ((ctx = openElements.pollLast()) != null) {
                    if (sameElementName(tagName, ctx.getElementName())) {
                        int tagStart = xmlParser.getStartOffset();
                        int tagEnd = xmlParser.getEndOffset();
                        ctx.setClosingTagOffset(tagStart, tagEnd);
                        break;
                    }
                }
            } else if (nextType == XMLParser.FINISHED) {
                finished = true;
            }
        }
        
    }

    private boolean sameElementName(String name1, String name2) 
    {
        return isIgnoreElementCase()? name1.equalsIgnoreCase(name2) : name1.equals(name2);
    }
    
    private String elementKey(String name)
    {
        return isIgnoreElementCase() ? name.toLowerCase() : name;
    }

}
