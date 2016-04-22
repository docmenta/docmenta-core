/*
 * EventQueueUtil.java
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

import org.docma.coreapi.*;
import java.util.*;

/**
 *
 * @author MP
 */
public class EventQueueUtil
{

    public static void compressEvents(List eventQueue)
    {
        // Collections.sort(eventQueue);
        int i = eventQueue.size() - 2;
        while (i >= 0) {
            Object evt_obj = eventQueue.get(i);
            Object nextevt_obj = eventQueue.get(i + 1);

            if (isMergeable(evt_obj, nextevt_obj)) {

                DocEventImpl evt = (DocEventImpl) evt_obj;
                DocEventImpl nextevt = (DocEventImpl) nextevt_obj;

                nextevt.addNodeIds(evt.getNodeIds());
                eventQueue.set(i, nextevt);
                eventQueue.remove(i + 1);
            }
            i--;
        }
        
        // Following is a workaround for the ZK tree rendering bug in version 6.5.7.
        // This can be removed as soon as the bug is fixed.
        int cnt_changed = 0;
        boolean requires_restructure = false;
        for (Object evt_obj : eventQueue) {
            if (evt_obj instanceof DocEvent) {
                String evt_name = ((DocEvent) evt_obj).getEventName();
                if (DocEvent.NODES_CHANGED.equals(evt_name)) {
                    cnt_changed++;
                    if (cnt_changed > 1) {
                        requires_restructure = true;
                        break;
                    }
                }
            }
        }
        if (requires_restructure) {
            for (int pos = eventQueue.size() - 1; pos >= 0; pos--) {
                Object evt_obj = eventQueue.get(pos);
                if (evt_obj instanceof DocEvent) {
                    eventQueue.remove(pos);
                }
            }
            eventQueue.add(new DocEventImpl(DocEvent.NODES_STRUCTURE_CHANGED, null, null, null));
        }
    }


    private static boolean isMergeable(Object event1, Object event2)
    {
        if ((event1 instanceof DocEvent) && (event2 instanceof DocEvent)) {
            DocEvent evt = (DocEvent) event1;
            DocEvent nextevt = (DocEvent) event2;
            String pId = evt.getParentId();
            String nextpId = nextevt.getParentId();
            if ((pId != null) && (nextpId != null)) {
                if (pId.equals(nextpId) &&
                    evt.getEventName().equals(nextevt.getEventName())) {
                    String lg = evt.getLang();
                    String nextlg = nextevt.getLang();
                    if (((lg == null) && (nextlg == null)) ||
                        ((lg != null) && lg.equals(nextlg))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static void printEventQueue(List eventQueue)
    {
        System.out.println();
        System.out.println("EventQueue:");
        System.out.println("-----------");
        for (int i=0; i < eventQueue.size(); i++) {
            System.out.println(eventQueue.get(i).toString());
        }
    }

}
