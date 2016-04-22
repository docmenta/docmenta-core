/*
 * LockImpl.java
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

import org.docma.lockapi.*;

/**
 *
 * @author MP
 */
public class LockImpl implements Lock {
    private String objId;
    private String name;
    private String user;
    private long creation;
    private long timeout;


    public LockImpl(String objId, String name, String user, long creation, long timeout) {
        this.objId = objId;
        this.name = name;
        this.user = user;
        this.creation = creation;
        this.timeout = timeout;
    }

    public String getLockedObjectId() {
        return objId;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return user;
    }

    public long getCreationTime() {
        return creation;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }
}
