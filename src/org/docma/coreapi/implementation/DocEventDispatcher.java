/*
 * DocEventDispatcher.java
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

import org.docma.coreapi.DocEvent;
import org.docma.coreapi.DocListener;
import java.util.*;
import org.docma.coreapi.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DocEventDispatcher implements Runnable
{

    private List listeners;
    private List eventQueue;


    public DocEventDispatcher(List listeners, List evtQueue)
    {
        this.listeners = listeners;
        this.eventQueue = evtQueue;
    }

    /* --------------  Public methods ---------------------- */

    public void run()
    {
        EventQueueUtil.compressEvents(eventQueue);
        if (org.docma.coreapi.DocConstants.DEBUG) {
            EventQueueUtil.printEventQueue(eventQueue);
        }
        for (int i=0; i < listeners.size(); i++) {
            DocListener listener = (DocListener) listeners.get(i);
            for (int j=0; j < eventQueue.size(); j++) {
                try {
                    listener.event((DocEvent) eventQueue.get(j));
                } catch (Exception ex) {
                    // ignore
                    ex.printStackTrace();
                    Log.warning("DocListener exception in method event(): " + ex.getMessage());
                }
            }
        }
        eventQueue.clear();
    }

}
