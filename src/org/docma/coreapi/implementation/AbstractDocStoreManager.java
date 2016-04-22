/*
 * AbstractDocStoreManager.java
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
import org.docma.util.*;

/**
 *
 * @author MP
 */
public abstract class AbstractDocStoreManager implements DocStoreManager
{
    private VersionIdFactory verIdFactory = null;
    private List openSessions = new ArrayList(100);
    private Map docStores = new HashMap();

    private long nextSessionId = 0;

    private Map uuidMap = null;


    /* --------------  Private methods  ----------------------------- */

    private String getStoreKey(String storeId, DocVersionId verId)
    {
        return storeId + " " + verId;
    }

    private void deleteStore(String storeId, DocVersionId verId)
    {
        docStores.remove(getStoreKey(storeId, verId));
    }

    private void deleteStores(String storeId)
    {
        Iterator it = docStores.values().iterator();
        while (it.hasNext()) {
            AbstractDocStore store = (AbstractDocStore) it.next();
            if (store.getStoreId().equals(storeId)) {
                it.remove();
            }
        }
    }

    private void initUUIDMap(DocStoreSession sess)
    {
        if (uuidMap == null) {
            uuidMap = new HashMap(300);
            String[] store_ids = sess.listDocStores();
            for (int i=0; i < store_ids.length; i++) {
                String store_id = store_ids[i];
                try {
                    UUID store_uuid = sess.getDocStoreUUID(store_id);
                    // Note: The method sess.getDocStoreUUID() might call
                    //       registerDocStoreUUID() which calls again initUUIDMap().
                    //       However, because uuidMap is no longer null, this does
                    //       not lead to an infinite loop.
                    uuidMap.put(store_uuid, store_id);
                } catch (Exception ex) {
                    Log.error("Could not get UUID of store '" + store_id + "'.");
                    // Note: Maybe the store has invalid configuration, e.g.
                    //       the configured store path that does not exist.
                    continue;
                }
                try {
                    DocVersionId[] ver_ids = sess.listVersions(store_id);
                    for (int k=0; k < ver_ids.length; k++) {
                        DocVersionId v_id = ver_ids[k];
                        DocVersionAddress v_addr = new DocVersionAddress(store_id, v_id);
                        try {
                            UUID v_uuid = sess.getVersionUUID(store_id, v_id);
                            // Note: see note above about avoiding infinite loop.
                            uuidMap.put(v_uuid, v_addr);
                        } catch (Exception ex) {
                            Log.error("Could not get UUID of version '" +
                                      store_id + " V" + v_id + "':" + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    Log.error("Could not list versions of store '" + store_id + "':" + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    /* ------  Package local methods called by AbstractDocStoreSession  ----- */

    synchronized void registerDocStoreUUID(DocStoreSession sess, UUID uuid, String storeId)
    {
        if (uuidMap == null) {
            // initUUIDMap(sess);
            // Note: Calling initUUIDMap() within registerDocStoreUUID() causes
            //       problem, because initUUIDMap() accesses all stores, which
            //       can lead to a blocking call in case of an external database 
            //       store, e.g. if database is temporarily not available.
            //       Therefore, AbstractDocStoreSession.getDocStoreUUID(storeId),
            //       which calls registerDocStoreUUID(), should not require 
            //       access to any other store than the store identified by storeId.
            return;
        }
        Object obj = uuidMap.get(uuid);
        if (obj == null) {
            uuidMap.put(uuid, storeId);
        } else {
            if (! storeId.equals(obj)) throw new DocRuntimeException("Cannot reassign UUID!");
        }
    }

    synchronized void registerVersionUUID(DocStoreSession sess, UUID uuid, DocVersionAddress versionAddr)
    {
        if (uuidMap == null) {
            // initUUIDMap(sess);
            // Note: Calling initUUIDMap() within registerVersionUUID() causes
            //       problem, because initUUIDMap() accesses all stores, which
            //       can lead to a blocking call in case of an external database 
            //       store, e.g. if database is temporarily not available.
            //       Therefore, AbstractDocStoreSession.getVersionUUID(storeId, verId),
            //       which calls registerVersionUUID(), should not require 
            //       access to any other store than the store identified by storeId.
            return;
        }
        Object obj = uuidMap.get(uuid);
        if (obj == null) {
            uuidMap.put(uuid, versionAddr);
        } else {
            if (! versionAddr.equals(obj)) throw new DocRuntimeException("Cannot reassign UUID!");
        }
    }

    synchronized String getDocStoreIdByUUID(DocStoreSession sess, UUID uuid)
    {
        initUUIDMap(sess);
        Object obj = uuidMap.get(uuid);
        return (obj instanceof String) ? (String) obj : null;
    }

    synchronized DocVersionAddress getVersionAddressByUUID(DocStoreSession sess, UUID uuid)
    {
        initUUIDMap(sess);
        Object obj = uuidMap.get(uuid);
        return (obj instanceof DocVersionAddress) ? (DocVersionAddress) obj : null;
    }

    /* ---------  Called by DocStoreSession Implementation  -------------- */

    public synchronized AbstractDocStore acquireStore(DocStoreSession sess, String storeId, DocVersionId verId)
    {
        String key = getStoreKey(storeId, verId);
        AbstractDocStore ds = (AbstractDocStore) docStores.get(key);
        if (ds == null) {
            ds = createStoreInstance(sess, storeId, verId);
            docStores.put(key, ds);
        }
        return ds;
    }

    public synchronized void releaseStore(DocStoreSession sess, String storeId, DocVersionId verId)
    {
        String[] arr = getConnectedUsers(storeId, verId);
        if (arr.length == 0) deleteStore(storeId, verId);
    }

    /**
     * Has to be called from DocStoreSession, when store instance is no longer
     * valid, e.g. when version is renamed or deleted.
     */
    public synchronized void destroyStoreInstance(String storeId, DocVersionId verId)
    {
        String[] arr = getConnectedUsers(storeId, verId);
        if (arr.length > 0) {
            throw new DocRuntimeException("Cannot destroy store instance. Users are still connected!");
        }
        deleteStore(storeId, verId);
    }

    /**
     * Has to be called from DocStoreSession, when store instance is no longer
     * valid, e.g. when store is to be deleted or store-id is to be changed.
     */
    public synchronized void destroyStoreInstances(String storeId)
    {
        String[] arr = getConnectedUsers(storeId);
        if (arr.length > 0) {
            throw new DocRuntimeException("Cannot destroy store instances. Users are still connected!");
        }
        deleteStores(storeId);
    }

    public synchronized void destroySession(DocStoreSession sess)
    {
        openSessions.remove(sess);
    }


    /* --------------  Configuration: injected objects  -------------------- */

    public VersionIdFactory getVersionIdFactory()
    {
        return this.verIdFactory;
    }

    public void setVersionIdFactory(VersionIdFactory verIdFactory)
    {
        this.verIdFactory = verIdFactory;
    }


    /* --------------  Abstract methods ------------------ */

    protected abstract DocStoreSession createSessionInstance(String sessionId, String userId);

    protected abstract AbstractDocStore createStoreInstance(DocStoreSession sess, String storeId, DocVersionId verId);


    /* --------------  Interface DocStoreManager ------------------ */

    public synchronized DocStoreSession connect(String userId) throws DocException {
        String sessionId = Long.toString(++nextSessionId);
        DocStoreSession sess = createSessionInstance(sessionId, userId);
        openSessions.add(sess);
        return sess;
    }

    public String[] getConnectedUsers()
    {
        return getConnectedUsers(null, null);
    }

    public String[] getConnectedUsers(String storeId)
    {
        return getConnectedUsers(storeId, null);
    }

    public synchronized String[] getConnectedUsers(String storeId, DocVersionId verId)
    {
        Set conn_users = new HashSet(openSessions.size());
        for (int i=0; i < openSessions.size(); i++) {
            DocStoreSession sess = (DocStoreSession) openSessions.get(i);
            if (((storeId == null) || storeId.equals(sess.getStoreId())) &&
                ((verId == null) || verId.equals(sess.getVersionId()))) {
                conn_users.add(sess.getUserId());
            }
        }
        String[] uids = new String[conn_users.size()];
        return (String[]) conn_users.toArray(uids);
    }

}
