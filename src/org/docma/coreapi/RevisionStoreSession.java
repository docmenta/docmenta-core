/*
 * RevisionStoreSession.java
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

import java.util.*;

/**
 *
 * @author MP
 */
public interface RevisionStoreSession
{
    void addRevision(String storeId, DocVersionId verId, String nodeId, String langCode,
                     byte[] content, Date revDate, String userId);
    void addRevision(String storeId, DocVersionId verId, String nodeId, String langCode,
                     String content, Date revDate, String userId);

    DocContentRevision[] getRevisions(String storeId, DocVersionId verId, String nodeId, String langCode);
    SortedSet<String> getRevisionNodeIds(String storeId, DocVersionId verId);

    void deleteRevisions(String storeId, DocVersionId verId, String nodeId);
    void clearRevisions(String storeId, DocVersionId verId);
}
