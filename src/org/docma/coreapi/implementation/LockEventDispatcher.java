/*
 * LockEventDispatcher.java
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
public class LockEventDispatcher
{
    public static final String EVENT_ADDED = "lockAdded";
    public static final String EVENT_REMOVED = "lockRemoved";
    public static final String EVENT_TIMEOUT = "lockTimeout";


    public static void dispatch(Lock lock, List listeners, String eventName)
    {
        for (int i=0; i < listeners.size(); i++) {
            try {
                LockListener listener = (LockListener) listeners.get(i);
                if (eventName.equalsIgnoreCase(EVENT_ADDED)) {
                    listener.lockAdded(lock);
                } else
                if (eventName.equalsIgnoreCase(EVENT_REMOVED)) {
                    listener.lockRemoved(lock);
                } else
                if (eventName.equalsIgnoreCase(EVENT_TIMEOUT)) {
                    listener.lockTimeout(lock);
                }
            } catch (Exception ex) {
                // ignore
                Log.warning("LockListener exception in " + eventName + " method: " + ex.getMessage());
            }
        }
    }

}
