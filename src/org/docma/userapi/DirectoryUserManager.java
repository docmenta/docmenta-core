/*
 * DirectoryUserManager.java
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
package org.docma.userapi;

import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public interface DirectoryUserManager extends UserManager
{
    final String PROPERTY_USER_DN = "dir_user_dn";
    final String PROPERTY_GROUP_DN = "dir_group_dn";
    
    boolean isDirectoryGroup(String groupId);
    int synchronizeDirectoryGroupMembers(String groupId) throws DocException;
    String getGroupDN(String groupId);
    void setGroupDN(String groupId, String dn) throws DocException;
    boolean groupDNExists(String dn);
    
    boolean isDirectoryUser(String userId);
    String getUserDN(String userId);
    void setUserDN(String userId, String dn) throws DocException;
    boolean userDNExists(String dn);
}
