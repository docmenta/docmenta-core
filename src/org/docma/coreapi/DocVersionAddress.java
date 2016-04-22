/*
 * DocVersionAddress.java
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
public class DocVersionAddress
{
    private String storeId;
    private DocVersionId versionId;

    public DocVersionAddress(String storeId, DocVersionId verId)
    {
        this.storeId = storeId;
        this.versionId = verId;
    }

    public String getStoreId()
    {
        return storeId;
    }

    public DocVersionId getVersionId()
    {
        return versionId;
    }

    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DocVersionAddress other = (DocVersionAddress) obj;
        if ((this.storeId == null) ? (other.storeId != null) : !this.storeId.equals(other.storeId)) {
            return false;
        }
        if (this.versionId != other.versionId && (this.versionId == null || !this.versionId.equals(other.versionId))) {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int hash = (this.storeId != null) ? this.storeId.hashCode() : 0;
        hash = 97 * hash + (this.versionId != null ? this.versionId.hashCode() : 0);
        return hash;
    }

}
