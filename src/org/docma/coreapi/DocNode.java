/*
 * DocNode.java
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
 * Created on 12. Oktober 2007, 18:01
 *
 */

package org.docma.coreapi;

import java.util.Map;

/**
 *
 * @author MP
 */
public interface DocNode
{
    String getId();

    String getTitle();
    String getTitle(String lang_id);
    void setTitle(String title);
    void setTitle(String title, String lang_id);

    String getAlias();
    String[] getAliases();
    void addAlias(String alias);  // add as first alias in the list
    void setAliases(String[] aliases);
    boolean deleteAlias(String alias);
    boolean hasAlias(String alias);

    String[] getAttributeNames();
    String getAttribute(String name);
    String getAttribute(String name, String lang_id);
    Map<String, String> getAttributes();
    Map<String, String> getAttributes(String lang_id);
    void setAttribute(String name, String value);
    void setAttribute(String name, String value, String lang_id);
    
    DocGroup getParentGroup();

    String[] getTranslations();
    boolean hasTranslation(String lang_code);
    void deleteTranslation(String lang_code);

    void refresh();  // clear cached values
}
