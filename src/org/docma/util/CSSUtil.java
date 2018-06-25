/*
 * CSSUtil.java
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 *
 * @author MP
 */
public class CSSUtil
{
    private static NumberFormat numberFormat = null;

    
    public static String mergeCSSClasses(String... clsNames) 
    {
        ArrayList<String> res = new ArrayList<String>();
        for (String val : clsNames) {
            if (val != null) {
                StringTokenizer st = new StringTokenizer(val);
                while (st.hasMoreTokens()) {
                    String clsName = st.nextToken();
                    if (! res.contains(clsName)) res.add(clsName);
                }
            }
        }
        return DocmaUtil.concatStrings(res, " ");
    }
    
    public static float getSizeFloat(String padd)
    {
        int idx = padd.length() - 1;
        while (idx >= 0) {
            char ch = padd.charAt(idx);
            if (Character.isLetter(ch) || ch == '%') idx--;
            else break;
        }

        try {
            return Float.parseFloat(padd.substring(0, idx + 1));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static int getSizeInt(String padd)
    {
        int idx = padd.length() - 1;
        while (idx >= 0) {
            char ch = padd.charAt(idx);
            if (Character.isLetter(ch) || ch == '%') idx--;
            else break;
        }

        try {
            return Integer.parseInt(padd.substring(0, idx + 1));
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public static String getSizeUnit(String padd)
    {
        int idx = padd.length() - 1;
        while (idx >= 0) {
            char ch = padd.charAt(idx);
            if (Character.isLetter(ch) || ch == '%') idx--;
            else break;
        }

        return padd.substring(idx + 1);
    }

    public static synchronized String formatFloatSize(float num)
    {
        if (numberFormat == null) {
            numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
            numberFormat.setParseIntegerOnly(false);
            numberFormat.setGroupingUsed(false);
            numberFormat.setMinimumFractionDigits(0);
            numberFormat.setMaximumFractionDigits(2);
        }
        return numberFormat.format(num);
    }

}
