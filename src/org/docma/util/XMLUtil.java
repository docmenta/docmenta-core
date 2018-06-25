/*
 * XMLUtil.java
 *
 *  Copyright (C) 2013  Manfred Paula, http://www.docmenta.org
 *   
 *  This file is part of Docmenta. Docmenta is free software: you can 
 *  redistribute it and/or modify it under the terms of the GNU Lesser 
 *  General Public License as published by the Free Software Foundation, 
 *  either version 3 of the License, or (at your option) any later version.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Docmenta.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Created on 16. Oktober 2007, 19:00
 *
 */

package org.docma.util;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 *
 * @author MP
 */
public class XMLUtil 
{
    private static XPathFactory xpathFactory = null;

    public static XPathExpression compileXPath(String expression) throws Exception 
    {
        if (xpathFactory == null) initXPath();
        
        XPath xpath = xpathFactory.newXPath();
        return xpath.compile(expression);
    }

    public static boolean evalXPathToBoolean(String xml, String xpath) throws Exception 
    {
        XPathExpression xe = compileXPath(xpath);
        Object res = xe.evaluate(new InputSource(new StringReader(xml)), XPathConstants.BOOLEAN);
        return Boolean.TRUE.equals(res);
    }

    public static String addCSSClass(String content, String elementName, String cssCls) throws Exception
    {
        if ((cssCls == null) || cssCls.equals("") || (elementName == null) || elementName.equals("")) {
            return content;   // content is unchanged
        }
        
        XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
        xmlproc.setCheckWellformed(true);
        xmlproc.setIgnoreElementCase(true);
        xmlproc.setElementHandler(elementName, new AddCSSClsHandler(elementName, cssCls));
        StringBuilder out = new StringBuilder();
        xmlproc.process(content, out);
        return out.toString();
    }

    public static String removeCSSClass(String content, String elementName, String cssCls) throws Exception
    {
        // Remove figure class
        if ((cssCls == null) || cssCls.equals("") || (elementName == null) || elementName.equals("")) {
            return content;   // content is unchanged
        }
        
        XMLProcessor xmlproc = XMLProcessorFactory.newInstance();
        xmlproc.setCheckWellformed(true);
        xmlproc.setIgnoreElementCase(true);
        xmlproc.setElementHandler(elementName, new RemoveCSSClsHandler(elementName, cssCls));
        StringBuilder out = new StringBuilder();
        xmlproc.process(content, out);
        return out.toString();
    }
    
    public static boolean attributeValueExists(String xml, 
                                               String elementName, 
                                               String attributeName, 
                                               String regex) throws Exception
    {
        Pattern valpattern = null;
        XMLParser parser = new XMLParser(xml);
        List<String> attnames = new ArrayList<String>();
        List<String> attvalues = new ArrayList<String>();
        int res;
        do {
            res = parser.next();
            if (res == XMLParser.START_ELEMENT) {
                if ((elementName != null) && elementName.equalsIgnoreCase(parser.getElementName())) {
                    parser.getAttributes(attnames, attvalues);
                    for (int i=0; i < attnames.size(); i++) {
                        String nm = attnames.get(i);
                        if (nm.equalsIgnoreCase(attributeName)) {
                            if (valpattern == null) {
                                valpattern = Pattern.compile(regex);
                            }
                            String val = attvalues.get(i);
                            if ((val != null) && valpattern.matcher(val).matches()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } while (res != XMLParser.FINISHED);
        return false;
    }

    public static String readTextChild(Element elem) {
        if (elem.hasChildNodes()) {
            return elem.getFirstChild().getNodeValue();
        } else {
            return "";
        }        
    }
    

    public static void writeTextChild(Document doc, Element elem, String value) {
        if (elem.hasChildNodes()) {
            elem.getFirstChild().setNodeValue(value);
        } else {
            Text txt = doc.createTextNode(value);
            elem.appendChild(txt);
        }
    }
    

    public static Element getChildByTagName(Element elem, String tagname) {
        NodeList children = elem.getChildNodes();
        for (int i=0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                if (tagname.equals(childElem.getTagName())) return childElem;
            }
        }
        return null;
    }

    public static List getChildrenByTagName(Element elem, String tagname) {
        ArrayList retlist = new ArrayList();
        NodeList children = elem.getChildNodes();
        for (int i=0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElem = (Element) child;
                if (tagname.equals(childElem.getTagName())) retlist.add(childElem);
            }
        }
        return retlist;
    }
    
    public static String escapeDoubleQuotedCDATA(String value) 
    {
        return value.replace("\"", "&quot;");
    }

    public static String escapePCDATA(String value) 
    {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    
    /* --------------  Private methods  ---------------------- */
    
    private static synchronized void initXPath() 
    {
        if (xpathFactory == null) {
            xpathFactory = XPathFactory.newInstance();
        }
    }


    //
    // Private Helper classes 
    // 
    
    private static class AddCSSClsHandler implements XMLElementHandler
    {
        private String elementName;
        private String cssCls;
        
        public AddCSSClsHandler(String elemName, String cssCls)
        {
            this.elementName = elemName;
            this.cssCls = cssCls;
        }
        
        public void processElement(XMLElementContext elemCtx) 
        {
            String ename = elemCtx.getElementName();
            if (! ename.equalsIgnoreCase(elementName)) {
                return;   // do nothing
            }
            
            String clsVal = elemCtx.getAttributeValue("class");
            if (clsVal == null) {
                elemCtx.setAttribute("class", cssCls);
            } else {
                clsVal = clsVal.trim();
                if (! (" " + clsVal + " ").contains(" " + cssCls + " ")) {
                    elemCtx.setAttribute("class", clsVal + " " + cssCls);
                }
            }
        }
    }

    private static class RemoveCSSClsHandler implements XMLElementHandler
    {
        private String elementName;
        private String cssCls;
        
        public RemoveCSSClsHandler(String elemName, String cssCls)
        {
            this.elementName = elemName;
            this.cssCls = cssCls;
        }
        
        public void processElement(XMLElementContext elemCtx) 
        {
            String ename = elemCtx.getElementName();
            if (! ename.equalsIgnoreCase(elementName)) {
                return;   // do nothing
            }
            
            String clsVal = elemCtx.getAttributeValue("class");
            if (clsVal != null) {
                clsVal = clsVal.trim();
                String clsNew = (" " + clsVal + " ").replace(" " + cssCls + " ", " ").trim();
                if (! clsVal.equals(clsNew)) {
                    elemCtx.setAttribute("class", clsNew.equals("") ? null : clsNew);
                }
            }
        }
    }

}
