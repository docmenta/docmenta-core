/*
 * DocAttributes.java
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

package org.docma.coreapi.implementation;

/**
 * Lists attribute names which are reserved for internal usage of store
 * implementations. The implementation of method DocNode.setAttribute() has to
 * throw an exception if a value for a reserved attribute name is to be set.
 *
 * @author MP
 */
public class DocAttributes
{
    public static final String SYS_PREFIX = "system.";

    public static final String TITLE = "title";
    public static final String CONTENT_TYPE = "contenttype";
    public static final String FILE_EXTENSION = "fileext";

    public static boolean isInternalAttributeName(String attName)
    {
        return attName.equals(TITLE) ||
               attName.equals(CONTENT_TYPE) ||
               attName.equals(FILE_EXTENSION) ||
               attName.startsWith(SYS_PREFIX);
    }

}
