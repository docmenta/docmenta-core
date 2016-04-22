/*
 * AbstractLockManager.java
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

import java.util.*;
import org.docma.lockapi.*;
import org.docma.util.Log;


/**
 *
 * @author MP
 */
public abstract class AbstractLockManager implements LockManager
{
    protected List listeners = new ArrayList();

    /* --------------  Protected methods  --------------- */

    protected static boolean checkLockTimeout(long currentTime, Lock lock)
    {
        if ((lock.getTimeout() > 0) &&
            (currentTime > lock.getCreationTime() + lock.getTimeout())) {
            return true;
        } else {
            return false;
        }
    }

    protected void lockAddedEvent(Lock lock)
    {
        // LockEventDispatcher.dispatch(lock, listeners, LockEventThread.EVENT_ADDED);
        for (int i=0; i < listeners.size(); i++) {
            try {
                ((LockListener) listeners.get(i)).lockAdded(lock);
            } catch (Exception ex) {
                Log.warning("LockListener exception in lockAdded method: " + ex.getMessage());
            }
        }
    }

    protected void lockRemovedEvent(Lock lock)
    {
        // LockEventDispatcher.dispatch(lock, listeners, LockEventThread.EVENT_REMOVED);
        for (int i = 0; i < listeners.size(); i++) {
            try {
                ((LockListener) listeners.get(i)).lockRemoved(lock);
            } catch (Exception ex) {
                Log.warning("LockListener exception in lockRemoved method: " + ex.getMessage());
            }
        }
    }

    protected void lockTimeoutEvent(Lock lock)
    {
        // LockEventDispatcher.dispatch(lock, listeners, LockEventThread.EVENT_TIMEOUT);
        for (int i = 0; i < listeners.size(); i++) {
            try {
                ((LockListener) listeners.get(i)).lockTimeout(lock);
            } catch (Exception ex) {
                Log.warning("LockListener exception in lockTimeout method: " + ex.getMessage());
            }
        }
    }


    /* --------------  Interface LockManager  --------------- */

    public void addListener(LockListener listener)
    {
        removeListener(listener);
        listeners.add(listener);
    }

    public boolean removeListener(LockListener listener)
    {
        for (int i=0; i < listeners.size(); i++) {
            if (listeners.get(i) == listener) {
                listeners.remove(i);
                return true;
            }
        }
        return false;
    }

    public LockListener[] getListeners()
    {
        LockListener[] arr = new LockListener[listeners.size()];
        return (LockListener[]) listeners.toArray(arr);
    }


}
