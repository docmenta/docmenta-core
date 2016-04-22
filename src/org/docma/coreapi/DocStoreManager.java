/*
 * DocStoreManager.java
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
 * Created on 12. Oktober 2007, 18:40
 *
 */

package org.docma.coreapi;

/**
 *
 * @author MP
 */
public interface DocStoreManager
{
    DocStoreSession connect(String userId) throws DocException;
    String[] getConnectedUsers();
    String[] getConnectedUsers(String storeId);
    String[] getConnectedUsers(String storeId, DocVersionId verId);
}
