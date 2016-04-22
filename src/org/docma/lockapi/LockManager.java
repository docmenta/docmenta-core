/*
 * LockManager.java
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

package org.docma.lockapi;


/**
 *
 * @author MP
 */
public interface LockManager {

    boolean setLock(String objId, String lockname, String user, long timeout);

    boolean refreshLock(String objId, String lockname, long timeout);

    // Lock[] getLocks(String objId);

    Lock getLock(String objId, String lockname);

    // Lock[] removeLocks(String objId);

    Lock removeLock(String objId, String lockname);

    void addListener(LockListener listener);

    boolean removeListener(LockListener listener);

    LockListener[] getListeners();

}
