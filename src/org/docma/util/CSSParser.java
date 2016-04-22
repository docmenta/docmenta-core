/*
 * CSSParser.java
 * 
 *  Copyright (C) 2015  Manfred Paula, http://www.docmenta.org
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
public class CSSParser 
{
    public static SortedMap<String, String> parseCSSProperties(String css_props)
    {
        SortedMap<String, String> res = new TreeMap<String, String>();
        final int last_pos = css_props.length() - 1;
        int start_pos = 0;
        while (start_pos < css_props.length()) {
            // Skip whitespace and semicolon
            char ch = css_props.charAt(start_pos);
            if ((ch == ';') || Character.isWhitespace(ch)) {
                start_pos++;
                continue;
            }
            
            // Skip comments between properties
            if ((ch == '/') && (start_pos < last_pos) && (css_props.charAt(start_pos + 1) == '*')) {
                int cend = css_props.indexOf("*/", start_pos + 2);
                start_pos = (cend < 0) ? css_props.length() : cend + 2;
            }
            
            int p = css_props.indexOf(':', start_pos);
            if (p < 0) {
                break;
            }
            String pname = css_props.substring(start_pos, p).trim();
            int end_pos = parsePropValue(css_props, p + 1);
            // end_pos is position of closing semicolon or is end of string 
            String pval = css_props.substring(p + 1, end_pos).trim();
            res.put(pname, pval);
            start_pos = end_pos;
        }
        return res;
    }
    
    private static int parsePropValue(CharSequence str, int offset) 
    {
        final int STATE_OTHER = 0;
        final int STATE_COMMENT = 1;
        final int STATE_QUOTED = 2;
        
        int state = STATE_OTHER;
        char quote_char = '"';
        
        final int last_pos = str.length() - 1;
        int pos = offset;
        boolean end_of_prop = false;
        while (pos < str.length()) {
            char ch = str.charAt(pos);
            switch (state) {
                case STATE_OTHER:
                    if (ch == ';') {
                        end_of_prop = true;
                    } else 
                    if ((ch == '"') || (ch == '\'')) {
                        state = STATE_QUOTED;
                        quote_char = ch;
                    } else 
                    if ((ch == '/') && 
                        (pos < last_pos) && (str.charAt(pos + 1) == '*')) {
                        state = STATE_COMMENT;
                        pos++;
                    }
                    break;
                case STATE_QUOTED:
                    if (ch == quote_char) {
                        state = STATE_OTHER;
                    }
                    break;
                case STATE_COMMENT:
                    if ((ch == '*') && 
                        (pos < last_pos) && (str.charAt(pos + 1) == '/')) {
                        state = STATE_OTHER;
                        pos++;
                    }
                    break;
            }
            if (end_of_prop) {
                break;
            } else {
                pos++;
            }
        }
        return pos;
    }
    
}
