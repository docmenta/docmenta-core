/*
 * XMLParser.java
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
 */

package org.docma.util;

import java.util.*;

/**
 *
 * @author MP
 */
public class XMLParser
{
    public static final int FINISHED = 0;
    public static final int START_ELEMENT = 1;
    public static final int END_ELEMENT = 2;
    public static final int COMMENT = 3;
    public static final int PROCESSING_INSTRUCTION = 4;
    public static final int CDATA = 5;

    private String xml;
    private int cursorPos;
    private int nextType;
    private int maxOffset;
    private int startOffset;
    private int endOffset;
    private String elementName;
    private boolean isEmptyElem;
    private List<String> attNames = null;
    private List<String> attValues = null;

    private boolean skipComments = true;
    private boolean skipPIs = true;
    private boolean skipCDATA = true;


    public XMLParser(String xml)
    {
        this(xml, 0);
    }

    public XMLParser(String xml, boolean skipComments, boolean skipPIs, boolean skipCDATA)
    {
        this(xml, 0);
        this.skipComments = skipComments;
        this.skipPIs = skipPIs;
        this.skipCDATA = skipCDATA;
    }

    public XMLParser(String xml, int offset)
    {
        this.xml = xml;
        this.cursorPos = offset;
        this.maxOffset = xml.length();
        this.isEmptyElem = false;
    }

    public int next() throws XMLParseException
    {
        final String DOCTYPE_START = "<!DOCTYPE";
        final String COMMENT_START = "<!--";
        final String COMMENT_END = "-->";
        final String CDATA_START = "<![CDATA[";
        final String CDATA_END = "]]>";

        while (cursorPos < maxOffset) {
            int tag_start = xml.indexOf('<', cursorPos);
            if (tag_start < 0) return FINISHED;

            cursorPos = tag_start;
            int name_start = tag_start + 1;
            char next_ch = xml.charAt(name_start);

            if (next_ch == '!') {
                // XML comment
                if (xml.regionMatches(tag_start, COMMENT_START, 0, COMMENT_START.length())) {
                    // find end of comment
                    int comment_end = xml.indexOf(COMMENT_END, name_start);
                    if (comment_end < 0) createException("Missing end of comment!");
                    cursorPos = comment_end + COMMENT_END.length();  // skip comment
                    if (skipComments) continue;
                    elementName = COMMENT_START;
                    startOffset = tag_start;
                    endOffset = comment_end + COMMENT_END.length();
                    nextType = COMMENT;
                    return nextType;
                }
                // CDATA
                if (xml.regionMatches(tag_start, CDATA_START, 0, CDATA_START.length())) {
                    // find end of CDATA
                    int cd_end = xml.indexOf(CDATA_END, name_start);
                    if (cd_end < 0) createException("Missing end of CDATA!");
                    cursorPos = cd_end + CDATA_END.length();  // skip CDATA
                    if (skipCDATA) continue;
                    elementName = CDATA_START;
                    startOffset = tag_start;
                    endOffset = cd_end + CDATA_END.length();
                    nextType = CDATA;
                    return nextType;
                }
                // DOCTYPE 
                if (xml.regionMatches(tag_start, DOCTYPE_START, 0, DOCTYPE_START.length())) {
                    // find end of DOCTYPE
                    int dt_end = xml.indexOf('>', name_start);
                    if (dt_end < 0) createException("Missing end of DOCTYPE!");
                    cursorPos = dt_end + 1;  // skip DOCTYPE
                    continue;
                }
                createException("Unexpected position of character sequence '<!'");
            }

            // Processing instruction or xml declaration <?xml ...?>
            if (next_ch == '?') {
                // find end
                int pi_end = xml.indexOf("?>", name_start);
                if (pi_end < 0) createException("Missing '?>'");
                cursorPos = pi_end + 2;  // skip processing instruction / xml declaration
                if (skipPIs) continue;
                elementName = "<?";
                startOffset = tag_start;
                endOffset = pi_end + 2;
                nextType = PROCESSING_INSTRUCTION;
                return nextType;
            }

            // Closing tag
            if (next_ch == '/') {
                int tag_end = xml.indexOf('>', name_start);
                if (tag_end < 0) createException("Invalid closing tag!");
                elementName = xml.substring(name_start + 1, tag_end).trim();
                startOffset = tag_start;
                endOffset = tag_end + 1;
                cursorPos = endOffset;
                nextType = END_ELEMENT;
                return nextType;
            }

            // Opening tag
            int name_end = name_start;
            while (name_end < maxOffset) {
                char ch = xml.charAt(name_end);
                if (ch == '>') break;
                if (ch == '/') break;
                if (Character.isWhitespace(ch)) break;
                name_end++;
            }
            elementName = xml.substring(name_start, name_end);
            initAttributes();
            int tag_end = parseTagAttributes(xml, name_end, attNames, attValues);
            if (tag_end < 0) createException("Invalid element attributes!");
            isEmptyElem = (xml.charAt(tag_end - 1) == '/');
            startOffset = tag_start;
            endOffset = tag_end + 1;
            cursorPos = endOffset;
            nextType = START_ELEMENT;
            return nextType;
        }
        return FINISHED;
    }

    public String getElementName()
    {
        return elementName;
    }

    public boolean isEmptyElement()
    {
        return isEmptyElem;
    }

    public void getAttributes(List names, List values)
    {
        if (nextType == START_ELEMENT) {
            names.clear();
            values.clear();
            names.addAll(attNames);
            values.addAll(attValues);
        }
    }

    public void getAttributesLower(List names, List values)
    {
        getAttributes(names, values);
        for (int i = 0; i < names.size(); i++) {
            names.set(i, names.get(i).toString().toLowerCase());
        }
    }

    public void getAttributes(Map<String, String> attMap)
    {
        if (nextType == START_ELEMENT) {
            attMap.clear();
            for (int i = 0; i < attNames.size(); i++) {
                attMap.put(attNames.get(i), attValues.get(i));
            }
        }
    }

    public void getAttributesLower(Map<String, String> attMap)
    {
        if (nextType == START_ELEMENT) {
            attMap.clear();
            for (int i = 0; i < attNames.size(); i++) {
                attMap.put(attNames.get(i).toLowerCase(), attValues.get(i));
            }
        }
    }

    public int getStartOffset()
    {
        return startOffset;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public int readUntilCorrespondingClosingTag() throws Exception
    {
        if (nextType != START_ELEMENT) {
            throw new Exception("Invalid call of XMLParser.readUntilCorrespondingClosingTag()");
        }
        if (isEmptyElement()) return nextType;
        String elemName = getElementName();  // element name of opening tag
        int eventType;
        int level = 0;
        while (true) {
            eventType = next();
            if (eventType == FINISHED) {
                break;
            }
            String tagName = getElementName();
            if ((tagName == null) || !tagName.equalsIgnoreCase(elemName)) {
                continue;  // ignore any other elements
            }
            if (eventType == START_ELEMENT) {
                if (! isEmptyElement()) ++level;
            } else
            if (eventType == END_ELEMENT) {
                if (level == 0) {
                    return eventType;  // matching end tag was found
                }
                --level;
            }
        }
        createException("Could not find matching end-tag for element " + elemName);
        return nextType;  // is never reached
    }

    /* --------------  Public static methods  ---------------------- */

    public static int parseTagAttributes(CharSequence seq, int offset, List nameList, List valueList)
    {
        final int SEARCH_NAME_START = 1;
        final int SEARCH_EQUALS = 2;
        final int SEARCH_VALUE_START = 3;
        final int SEARCH_VALUE_END = 4;
        final int TAG_END = 5;

        if (offset < 0) throw new RuntimeException("Negative offset in ParseTagAttributes()");

        nameList.clear();
        valueList.clear();
        int state = SEARCH_NAME_START;
        int len = seq.length();
        String name = null;
        int name_start = -1;
        int value_start = -1;
        char quote_char = '"';
        while (offset < len) {
            char ch = seq.charAt(offset);
            if (state == SEARCH_NAME_START) {
                if (ch == '>') {  // no more attributes
                    return offset;  // state = TAG_END;
                } else
                if (ch == '/') {       // empty element
                    ++offset;
                    if ((offset < len) && (seq.charAt(offset) == '>')) {
                        return offset;  // state = TAG_END;
                    }
                    return -offset;   // state is not TAG_END invalid syntax will be returned
                } else
                if (Character.isJavaIdentifierStart(ch)) {
                    name_start = offset;
                    state = SEARCH_EQUALS;
                }
                else
                if (! Character.isWhitespace(ch)) return -offset;  // negative value means invalid syntax
            } else
            if (state == SEARCH_EQUALS) {
                boolean was_whitespace = false;
                while (Character.isWhitespace(ch)) {  // on first whitespace: skip remaining whitespace
                    was_whitespace = true;
                    if (++offset < len) {
                        ch = seq.charAt(offset);
                    } else {
                        return -offset;  // invalid syntax 
                    }
                }
                if (ch == '=') {
                    name = seq.subSequence(name_start, offset).toString().trim();
                    state = SEARCH_VALUE_START;
                } else
                if (was_whitespace) {
                    return -offset;  // invalid syntax: whitespace reached, but next non-whitespace is not '='
                } else
                if (! (Character.isJavaIdentifierPart(ch) || (ch == '-'))) { // if not a valid name character
                    // no '=', no whitespace and no valid name character 
                    return -offset; // negative value means invalid syntax
                }
            } else
            if (state == SEARCH_VALUE_START) {
                if ((ch == '"') || (ch == '\'')) {
                    quote_char = ch;
                    value_start = offset + 1;
                    state = SEARCH_VALUE_END;
                } else
                if (! Character.isWhitespace(ch)) return -offset;  // negative value means invalid syntax
            } else
            if (state == SEARCH_VALUE_END) {
                if (ch == quote_char) {
                    String value = seq.subSequence(value_start, offset).toString();
                    // attribs.put(name, value);
                    nameList.add(name);
                    valueList.add(value);
                    state = SEARCH_NAME_START;
                } else 
                if (ch == '<') {  // is not allowed in attribute value; has to be escaped as &lt;
                    return -offset;  // negative value means invalid syntax
                }
            }
            ++offset;  // read next char
        }

        if (state != TAG_END) return -offset;  // negative value means invalid syntax
        else return offset;  // position of '>' character
    }

    /* --------------  Private methods  ---------------------- */

    private void initAttributes()
    {
        if (attNames == null) {
            attNames = new ArrayList<String>(16);
            attValues = new ArrayList<String>(16);
        }
    }

    private void createException(String msg) throws XMLParseException
    {
        throw new XMLParseException("XML parse error at character position " + cursorPos + ": " + msg);
    }

}
