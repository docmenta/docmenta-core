/*
 * AbstractDocStore.java
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
import org.docma.coreapi.*;
import org.docma.lockapi.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public abstract class AbstractDocStore
{
    protected String storeId;
    protected DocVersionId verId;
    protected LockManager lockManager;
    protected List docListeners = new ArrayList();

    private List eventQueue = new ArrayList();
    private boolean dispatch_running = false;


    protected AbstractDocStore(String storeId, DocVersionId verId)
    {
        this.storeId = storeId;
        this.verId = verId;
    }

    /* ----------------  Called from sub-class  ------------------ */

    protected void setLockManager(LockManager lockMgr)
    {
        this.lockManager = lockMgr;
    }


    /* ----------------  Package local methods  ------------------ */
    
    synchronized void nodeAddedEvent(DocGroup parent, DocNode node)
    {
        if (! docListeners.isEmpty()) {
            String pId = (parent == null) ? null : parent.getId();
            String nodeId = (node == null) ? null : node.getId();
            DocEvent evt = new DocEventImpl(DocEvent.NODES_ADDED, pId, nodeId, null);
            eventQueue.add(evt);
        }
    }

    synchronized void nodeRemovedEvent(DocGroup parent, DocNode node)
    {
        if (! docListeners.isEmpty()) {
            String pId = (parent == null) ? null : parent.getId();
            String nodeId = (node == null) ? null : node.getId();
            DocEvent evt = new DocEventImpl(DocEvent.NODES_REMOVED, pId, nodeId, null);
            eventQueue.add(evt);
        }
    }

    synchronized void nodeChangedEvent(DocGroup parent, DocNode node, String lang)
    {
        if (! docListeners.isEmpty()) {
            String pId = (parent == null) ? null : parent.getId();
            String nodeId = (node == null) ? null : node.getId();
            DocEvent evt = new DocEventImpl(DocEvent.NODES_CHANGED, pId, nodeId, lang);
            eventQueue.add(evt);
        }
    }


    /* ----------------  Public methods  ------------------ */

    public String getStoreId()
    {
        return storeId;
    }

    public DocVersionId getVersionId()
    {
        return verId;
    }

    public LockManager getLockManager()
    {
        return this.lockManager;
    }

    public void addDocListener(DocListener listener)
    {
        removeDocListener(listener);
        docListeners.add(listener);
    }

    public boolean removeDocListener(DocListener listener)
    {
        for (int i=0; i < docListeners.size(); i++) {
            if (docListeners.get(i) == listener) {
                docListeners.remove(i);
                return true;
            }
        }
        return false;
    }

    public DocListener[] getDocListeners()
    {
        DocListener[] arr = new DocListener[docListeners.size()];
        return (DocListener[]) docListeners.toArray(arr);
    }

    public synchronized void dispatchEventQueue()
    {
        if (dispatch_running) {
            if (DocConstants.DEBUG) {
                Log.warning("Nested call of dispatchEventQueue()!");
            }
            return;  // avoid recursive calls
        }
        dispatch_running = true;
        try {
            if (! (docListeners.isEmpty() || eventQueue.isEmpty())) {
                List q = new ArrayList(eventQueue);  // q is copy of eventQueue
                // Thread th = new Thread(new DocEventDispatcher(docListeners, q));
                // th.start();
                DocEventDispatcher dp = new DocEventDispatcher(docListeners, q);
                dp.run();
            }
            eventQueue.clear();
        } finally {
            dispatch_running = false;
        }
    }

    public synchronized void discardEventQueue()
    {
        this.eventQueue.clear();
    }

}
