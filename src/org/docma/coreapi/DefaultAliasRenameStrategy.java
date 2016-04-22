/*
 * DefaultAliasRenameStrategy.java
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

package org.docma.coreapi;

/**
 *
 * @author MP
 */
public class DefaultAliasRenameStrategy implements AliasRenameStrategy
{
    private static DefaultAliasRenameStrategy instance = new DefaultAliasRenameStrategy();

    public String renameAlias(String alias) throws DocException
    {
        int p = alias.lastIndexOf('_');
        if (p < 0) {
            return alias + "_2";
        } else {
            String numstr = alias.substring(p + 1);
            String prefix = alias.substring(0, p + 1);
            try {
                int next_number = Integer.parseInt(numstr) + 1;
                return prefix + next_number;
            } catch (Exception ex) {
                return alias + "_2";
            }
        }
    }

    public static DefaultAliasRenameStrategy getInstance()
    {
        return instance;
    }

}
