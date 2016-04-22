/*
 * AbstractDocStoreSession.java
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
import org.docma.lockapi.LockListener;
import org.docma.util.*;

/**
 *
 * @author MP
 */
public abstract class AbstractDocStoreSession implements DocStoreSession
{
    protected static final String PROP_STORE_PREFIX = "docstore.";
    protected static final String PROP_STORE_RENDITION_NAMES      = PROP_STORE_PREFIX + "imagerendition.names";
    protected static final String PROP_STORE_RENDITION_FORMAT     = PROP_STORE_PREFIX + "imagerendition.format";
    protected static final String PROP_STORE_RENDITION_MAX_WIDTH  = PROP_STORE_PREFIX + "imagerendition.max_width";
    protected static final String PROP_STORE_RENDITION_MAX_HEIGHT = PROP_STORE_PREFIX + "imagerendition.max_height";
    protected static final String PROP_STORE_UUID                 = PROP_STORE_PREFIX + "uuid";
    protected static final String PROP_VERSION_PREFIX = "docversion.";
    public static final String PROP_VERSION_UUID = PROP_VERSION_PREFIX + "uuid";
    public static final String PROP_VERSION_STATE = PROP_VERSION_PREFIX + "state";
    public static final String PROP_VERSION_CREATION_DATE = PROP_VERSION_PREFIX + "creationdate";
    public static final String PROP_VERSION_RELEASE_DATE = PROP_VERSION_PREFIX + "releasedate";

    protected AbstractDocStoreManager storeManager;
    protected AbstractDocStore docStore = null;

    private String sessionId;
    private String userId;
    private String translationMode = null;

    protected List docListeners = new ArrayList();
    protected List lockListeners = new ArrayList();

    protected static final Set<String> internalStoreProps;
    protected static final Set<String> internalVersionProps;
    
    
    static {
        internalStoreProps = new LinkedHashSet<String>();
        internalStoreProps.add(PROP_STORE_RENDITION_NAMES);
        internalStoreProps.add(PROP_STORE_RENDITION_FORMAT + ".");
        internalStoreProps.add(PROP_STORE_RENDITION_MAX_WIDTH + ".");
        internalStoreProps.add(PROP_STORE_RENDITION_MAX_HEIGHT + ".");
        internalStoreProps.add(PROP_STORE_UUID);
        
        internalVersionProps = new LinkedHashSet<String>();
        internalVersionProps.add(PROP_VERSION_STATE);
        internalVersionProps.add(PROP_VERSION_STATE + ".");
        internalVersionProps.add(PROP_VERSION_CREATION_DATE);
        internalVersionProps.add(PROP_VERSION_RELEASE_DATE);
        internalVersionProps.add(PROP_VERSION_RELEASE_DATE + ".");
        internalVersionProps.add(PROP_VERSION_UUID);
    }

    public AbstractDocStoreSession(AbstractDocStoreManager dsm, String sessionId, String userId)
    {
        this.storeManager = dsm;
        this.sessionId = sessionId;
        this.userId = userId;
    }

    /* --------------  Abstract methods ------------------ */

    // abstract protected void refreshAliasList();
    
    abstract protected void onReleaseTranslation(String storeId, DocVersionId verId, String lang)
    throws DocException;

    abstract protected void setTranslationBackToPending(String storeId, DocVersionId verId, String lang)
    throws DocException;

    /* --------------  Public methods ------------------ */

    public static boolean isInternalStoreProperty(String propName)
    {
        if (internalStoreProps.contains(propName)) {
            return true;
        }
        for (String internal_name : internalStoreProps) {
            if (internal_name.endsWith(".") && propName.startsWith(internal_name)) {
                return true;
            } 
        }
        return false;
    }
    
    public static boolean isInternalVersionProperty(String propName)
    {
        if (internalVersionProps.contains(propName)) {
            return true;
        }
        for (String internal_name : internalVersionProps) {
            if (internal_name.endsWith(".") && propName.startsWith(internal_name)) {
                return true;
            } 
        }
        return false;
    }


    public void checkRenameVersion(String storeId, DocVersionId oldVerId, DocVersionId newVerId)
    throws DocException
    {
        String[] uids = storeManager.getConnectedUsers(storeId, oldVerId);
        if (uids.length > 0) {
            throw new DocException("Cannot rename version. Users are still connected.");
        }
        DocVersionId[] verIds = listVersions(storeId);
        int idx = Arrays.asList(verIds).indexOf(oldVerId);
        if (idx < 0) {
            throw new DocRuntimeException("Version '" + oldVerId + "' is not contained in version list.");
        }
        if (idx > 0) {
            DocVersionId minId = verIds[idx-1];
            if (! newVerId.isHigherThan(minId)) {
                throw new DocException("New version ID must be higher than '" + minId + "'.");
            }
        }
        if (idx < verIds.length-1) {
            DocVersionId maxId = verIds[idx+1];
            if (! newVerId.isLowerThan(maxId)) {
                throw new DocException("New version ID must be lower than '" + maxId + "'.");
            }
        }
    }

    public void checkCreateVersion(String storeId, DocVersionId baseVersion, DocVersionId newVersion)
    throws DocException
    {
        if (baseVersion != null) {
            if (! getVersionState(storeId, baseVersion).equalsIgnoreCase(DocVersionState.RELEASED)) {
                throw new DocException("Cannot derive version from draft version.");
            }
            if (! newVersion.isHigherThan(baseVersion)) {
                throw new DocException("New version ID has to be higher than base version ID.");
            }
        }
    }


    public synchronized boolean startLocalTransaction()
    {
        // if a transaction is not already running, then start
        // a "local" transaction
        if (runningTransaction()) {
            return false;
        } else {
            try {
                startTransaction();
                return true;
            } catch (DocException dex) {
                throw new DocRuntimeException(dex);
            }
        }
    }

    public void commitLocalTransaction(boolean started)
    {
        try {
            if (started) commitTransaction();
        } catch (DocException dex) {
            throw new DocRuntimeException(dex);
        }
    }

    public void rollbackLocalTransaction(boolean started)
    {
        if (started) rollbackTransaction();
    }

    public void rollbackLocalTransactionRuntime(boolean started, Exception ex)
    {
        if (started) rollbackTransaction();
        if (ex instanceof RuntimeException) throw (RuntimeException) ex;
        else throw new DocRuntimeException(ex);
    }

    public void rollbackLocalTransactionRethrow(boolean started, Exception ex) throws DocException
    {
        if (started) rollbackTransaction();
        if (ex instanceof DocException) throw (DocException) ex;
        else throw new DocException(ex);
    }

    public VersionIdFactory getVersionIdFactory()
    {
        return storeManager.getVersionIdFactory();
    }

    public void nodeAddedEvent(DocGroup parent, DocNode node)
    {
        if (docStore != null) {
            docStore.nodeAddedEvent(parent, node);
            if (! runningTransaction()) {
                docStore.dispatchEventQueue();
            }
        }
    }

    public void nodeRemovedEvent(DocGroup parent, DocNode node)
    {
        if (docStore != null) {
            docStore.nodeRemovedEvent(parent, node);
            if (! runningTransaction()) {
                docStore.dispatchEventQueue();
            }
        }
    }

    public void nodeChangedEvent(DocGroup parent, DocNode node, String lang)
    {
        if (docStore != null) {
            docStore.nodeChangedEvent(parent, node, lang);
            if (! runningTransaction()) { 
                docStore.dispatchEventQueue();
            }
        }
    }
    
    public void onOpenDocStore()
    {
        for (int i=0; i < docListeners.size(); i++) {
            docStore.addDocListener((DocListener) docListeners.get(i));
        }
        for (int i=0; i < lockListeners.size(); i++) {
            docStore.getLockManager().addListener((LockListener) lockListeners.get(i));
        }
    }
    
    public void onCloseDocStore()
    {
        for (int i=0; i < docListeners.size(); i++) {
            docStore.removeDocListener((DocListener) docListeners.get(i));
        }
        for (int i=0; i < lockListeners.size(); i++) {
            docStore.getLockManager().removeListener((LockListener) lockListeners.get(i));
        }
    }

    public Set<DocVersionId> deleteVersionsRecursive(String storeId, 
                                                     DocVersionId[] vers, 
                                                     ProgressCallback progress) 
                                                     throws DocException
    {
        Set<DocVersionId> deletedVers = new TreeSet<DocVersionId>();
        if ((vers != null) && (vers.length > 0)) {
            for (DocVersionId vid : vers) {
                // Delete all sub-versions before deleting the version itself
                DocVersionId[] subvers = getSubVersions(storeId, vid);
                if ((subvers != null) && (subvers.length > 0)) {
                    deletedVers.addAll(deleteVersionsRecursive(storeId, subvers, progress));
                }
                if (! deletedVers.contains(vid)) {
                    if (progress != null) {
                        if (progress.getCancelFlag()) {
                            throw new DocException("Canceled by user.");
                        }
                        progress.setMessage("text.progress_delete_version", vid.toString());
                    }
                    deleteVersion(storeId, vid);
                    deletedVers.add(vid);
                }
            }
        }
        return deletedVers;
    }

    /* --------------  Interface DocStoreSession ------------------ */

    public String getSessionId()
    {
        return sessionId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void enterTranslationMode(String lang_code)
    {
        translationMode = lang_code;
    }

    public void leaveTranslationMode()
    {
        translationMode = null;
    }

    public String getTranslationMode()
    {
        return translationMode;
    }

    public DocVersionId createVersionId(String verId) throws DocException
    {
        return storeManager.getVersionIdFactory().createVersionId(verId);
    }

    public Date getVersionCreationDate(String storeId, DocVersionId verId)
    {
        String millis = getVersionProperty(storeId, verId, PROP_VERSION_CREATION_DATE);
        try {
            return new Date(Long.parseLong(millis));
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVersionCreationDate(String storeId, DocVersionId verId, Date creationDate)
    throws DocException
    {
        String millis = String.valueOf(creationDate.getTime());
        setVersionProperty(storeId, verId, PROP_VERSION_CREATION_DATE, millis);
    }

    public String getVersionState(String storeId, DocVersionId verId)
    {
        String lang = getTranslationMode();
        return getVersionState(storeId, verId, lang);
    }

    public synchronized String getVersionState(String storeId, DocVersionId verId, String lang)
    {
        String st = null;
        
        // Note: Transaction is commented out, because read-only operations
        //       should not be executed in a transaction. This is because  
        //       filesystem-based stores write the index.xml file at every commit.
        //       Future solution is to introduce read-only transactions.
        
        // boolean started = startLocalTransaction();
        // try {
            if (lang == null) {
                st = getVersionProperty(storeId, verId, PROP_VERSION_STATE);
            } else {
                st = getVersionProperty(storeId, verId, PROP_VERSION_STATE + "." + lang.toLowerCase());
            }
            if ((st == null) || st.equals("")) {
                st = DocVersionState.DRAFT;
                if (lang != null) {
                    DocVersionId v_from = getVersionDerivedFrom(storeId, verId);
                    if (v_from != null) {
                        String from_state = getVersionProperty(storeId, v_from, PROP_VERSION_STATE + "." + lang.toLowerCase());
                        boolean from_released = (from_state != null) &&
                                                 from_state.equalsIgnoreCase(DocVersionState.RELEASED);
                        if (! from_released) st = DocVersionState.TRANSLATION_PENDING;
                    }
                }
            }
        //     commitLocalTransaction(started);
        // } catch (Exception ex) {
        //     rollbackLocalTransactionRuntime(started, ex);  // rollback and throw runtime exception
        // }
        return st;
    }

    public synchronized void setVersionState(String storeId, DocVersionId verId, String newState)
    throws DocException
    {
        // Note: Transaction is commented out, because filesystem-based stores
        //       write the index.xml file at every commit. But this is not
        //       required as only version properties are updated.
        
        // boolean started = startLocalTransaction();
        // try {
            String oldState = getVersionState(storeId, verId);
            if (newState.equalsIgnoreCase(oldState)) {
                return;
            }
            String lang = getTranslationMode();
            boolean is_translation = (lang != null);
            boolean was_draft = oldState.equalsIgnoreCase(DocVersionState.DRAFT);
            boolean was_released = oldState.equalsIgnoreCase(DocVersionState.RELEASED);
            boolean was_pending = oldState.equalsIgnoreCase(DocVersionState.TRANSLATION_PENDING);
            boolean tobe_draft = newState.equalsIgnoreCase(DocVersionState.DRAFT);
            boolean tobe_released = newState.equalsIgnoreCase(DocVersionState.RELEASED);
            if (was_pending) {
                throw new DocException("Cannot change version state " + oldState);
            }
            if (! (tobe_draft || tobe_released)) {
                throw new DocException("Cannot change version state: invalid state " + newState);
            }

            // boolean local_transaction = !runningTransaction();
            // if (local_transaction) startTransaction();

            if (was_draft && tobe_released) {   // release
                if (is_translation) {
                    onReleaseTranslation(storeId, verId, lang);
                }
            }
            else
            if (was_released && tobe_draft) {  // unrelease
                DocVersionId[] subs = getSubVersions(storeId, verId);
                if (! is_translation) {  // original mode
                    if (subs.length > 0) {
                        throw new DocException(
                        "Cannot unrelease version. Another version is derived from this version.");
                    }
                } else {   // translation mode
                    boolean has_released = false;
                    for (int i=0; i < subs.length; i++) {
                        if (getVersionState(storeId, subs[i]).equalsIgnoreCase(DocVersionState.RELEASED)) {
                            has_released = true;
                            break;
                        }
                    }
                    if (has_released) {
                        throw new DocException(
                        "Cannot unrelease version: a derived version is already released.");
                    } else {
                        // Set derived versions back to pending and delete translated files of derived version
                        for (int i=0; i < subs.length; i++) {
                            setTranslationBackToPending(storeId, subs[i], lang);
                        }
                    }
                }
            } else {
                Log.error("Invalid state change from " + oldState + " to " + newState);
            }

            // Set new state
            if (lang == null) lang = "";
            else lang = "." + lang.toLowerCase();
            String[] names = {
                PROP_VERSION_STATE + lang,
                PROP_VERSION_RELEASE_DATE + lang };
            String reldate = (tobe_released) ? Long.toString(System.currentTimeMillis()) : null;
            String[] values = { newState, reldate };
            setVersionProperties(storeId, verId, names, values);

        //     commitLocalTransaction(started);
        // } catch (Exception ex) {
        //     rollbackLocalTransactionRethrow(started, ex);  // rollback and rethrow exception
        // }
    }


    public Date getVersionReleaseDate(String storeId, DocVersionId verId)
    {
        String lang = getTranslationMode();
        String nm = PROP_VERSION_RELEASE_DATE;
        if (lang != null) {
            nm += "." + lang;
        }
        String millis = getVersionProperty(storeId, verId, nm);
        try {
            return new Date(Long.parseLong(millis));
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVersionReleaseDate(String storeId, DocVersionId verId, Date releaseDate)
    throws DocException
    {
        String lang = getTranslationMode();
        String nm = PROP_VERSION_RELEASE_DATE;
        if (lang != null) {
            nm += "." + lang;
        }
        String millis = String.valueOf(releaseDate.getTime());
        setVersionProperty(storeId, verId, nm, millis);
    }

    public DocVersionId[] getRootVersions(String storeId)
    {
        return DocStoreUtilities.getRootVersions(this, storeId);
    }

    public int deleteAllVersions(String storeId, ProgressCallback progress) throws DocException
    {
        DocVersionId[] rootVerIds = getRootVersions(storeId);
        if (rootVerIds.length > 0) {
            Set<DocVersionId> deleted = deleteVersionsRecursive(storeId, rootVerIds, progress);
            return deleted.size();
        } else {
            return 0;
        }
    }

    public synchronized DocImageRendition[] getImageRenditions()
    {
        DocImageRendition[] rend_arr = null;
        
        // Note: Transaction is commented out, because read-only operations
        //       should not be executed in a transaction. This is because  
        //       filesystem-based stores write the index.xml file at every commit.
        //       Future solution is to introduce read-only transactions.
        
        // boolean started = startLocalTransaction();
        // try {
            String rend_ids = getDocStoreProperty(getStoreId(), PROP_STORE_RENDITION_NAMES);
            if ((rend_ids != null) && (rend_ids.trim().length() > 0)) {
                String[] id_arr = rend_ids.split(",");
                rend_arr = new DocImageRendition[id_arr.length];
                for (int i=0; i < id_arr.length; i++) {
                    rend_arr[i] = getImageRendition(id_arr[i].trim());
                }
            } else {
                rend_arr = new DocImageRendition[0];
            }
        //     commitLocalTransaction(started);
        // } catch (Exception ex) {
        //     rollbackLocalTransactionRuntime(started, ex);  // rollback and throw runtime exception
        // }
        return rend_arr;
    }

    public synchronized DocImageRendition getImageRendition(String name)
    {
        DocImageRendition result = null;
        // Note: Transaction is commented out, because read-only operations
        //       should not be executed in a transaction. This is because  
        //       filesystem-based stores write the index.xml file at every commit.
        //       Future solution is to introduce read-only transactions.
        
        // boolean started = startLocalTransaction();
        // try {
            String format = getDocStoreProperty(getStoreId(), 
                                                PROP_STORE_RENDITION_FORMAT + "." + name);
            if ((format != null) && !format.trim().equals("")) {
                String w_str = getDocStoreProperty(getStoreId(), 
                                                   PROP_STORE_RENDITION_MAX_WIDTH + "." + name);
                String h_str = getDocStoreProperty(getStoreId(), 
                                                   PROP_STORE_RENDITION_MAX_HEIGHT + "." + name);
                int max_width = 0;
                int max_height = 0;
                try {
                    max_width = Integer.parseInt(w_str);
                    max_height = Integer.parseInt(h_str);
                } catch (Exception ex) {}
                try {
                    result = new DocImageRendition(name, format, max_width, max_height);
                } catch (DocException dex) {
                    throw new DocRuntimeException("Could not load image rendition: " + name);
                }
            }
        //     commitLocalTransaction(started);
        // } catch (Exception ex) {
        //     rollbackLocalTransactionRuntime(started, ex);  // rollback and throw runtime exception
        // }
        return result;
    }

    public synchronized void addImageRendition(DocImageRendition rendition) throws DocException
    {
        // Note: Transaction is commented out, because filesystem-based stores
        //       write the index.xml file at every commit. But this is not
        //       required as only store properties are updated.
        
        // boolean started = startLocalTransaction();
        // try {
            String rend_ids = getDocStoreProperty(getStoreId(), PROP_STORE_RENDITION_NAMES);
            String[] name_arr;
            if ((rend_ids == null) || (rend_ids.trim().length() == 0)) {
                name_arr = new String[0];
            } else {
                name_arr = rend_ids.split(",");
            }
            List name_list = Arrays.asList(name_arr);
            if (! name_list.contains(rendition.getName())) {
                // add new rendition to list
                List names_new = new ArrayList(name_list);
                names_new.add(rendition.getName());
                Collections.sort(names_new);
                String names_str = DocmaUtil.concatStrings(names_new, ",");
                String[] prop_names = {
                    PROP_STORE_RENDITION_NAMES,
                    PROP_STORE_RENDITION_FORMAT + "." + rendition.getName(),
                    PROP_STORE_RENDITION_MAX_WIDTH + "." + rendition.getName(),
                    PROP_STORE_RENDITION_MAX_HEIGHT + "." + rendition.getName() };
                String[] prop_values = {
                    names_str,
                    rendition.getFormat(),
                    "" + rendition.getMaxWidth(),
                    "" + rendition.getMaxHeight() };
                setDocStoreProperties(getStoreId(), prop_names, prop_values);
            } else {
                throw new DocException("Rendition already exists: " + rendition.getName());
            }

        //     commitLocalTransaction(started);
        // } catch (Exception ex) {
        //     rollbackLocalTransactionRethrow(started, ex);  // rollback and rethrow exception
        // }
    }

    public synchronized boolean deleteImageRendition(String renditionName)
    {
        boolean delete_okay = false;
        
        // Note: Transaction is commented out, because filesystem-based stores
        //       write the index.xml file at every commit. But this is not
        //       required as only store properties are updated.
        
        // boolean started = startLocalTransaction();
        try {
            String rend_ids = getDocStoreProperty(getStoreId(), PROP_STORE_RENDITION_NAMES);
            if ((rend_ids != null) && (rend_ids.trim().length() > 0)) {
                String[] name_arr = rend_ids.split(",");
                List name_list = new ArrayList(Arrays.asList(name_arr));
                if (name_list.remove(renditionName)) {
                    String name_str = DocmaUtil.concatStrings(name_list, ",");
                    String[] prop_names = {
                        PROP_STORE_RENDITION_NAMES,
                        PROP_STORE_RENDITION_FORMAT + "." + renditionName,
                        PROP_STORE_RENDITION_MAX_WIDTH + "." + renditionName,
                        PROP_STORE_RENDITION_MAX_HEIGHT + "." + renditionName };
                    String[] prop_values = new String[] { name_str, null, null, null };
                    setDocStoreProperties(getStoreId(), prop_names, prop_values);
                    delete_okay = true;
                }
            }
        //     commitLocalTransaction(started);
        } catch (DocException dex) {
            throw new DocRuntimeException(dex);
        //   rollbackLocalTransactionRuntime(started, ex);  // rollback and throw runtime exception
        }
        return delete_okay;
    }


    public UUID getDocStoreUUID(String storeId)
    {
        String uuid_str = getDocStoreProperty(storeId, PROP_STORE_UUID);
        if ((uuid_str != null) && (uuid_str.length() > 0)) {
            return UUID.fromString(uuid_str);
        } else {
            String name_id = storeId + "_" + System.currentTimeMillis();
            try {
                UUID uuid_new = UUID.nameUUIDFromBytes(name_id.getBytes("UTF-8"));
                setDocStoreProperty(storeId, PROP_STORE_UUID, uuid_new.toString());
                storeManager.registerDocStoreUUID(this, uuid_new, storeId);
                return uuid_new;
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
    }


    public void setDocStoreUUID(String storeId, UUID uuid) throws DocException
    {
        String uuid_str = getDocStoreProperty(storeId, PROP_STORE_UUID);
        if ((uuid_str != null) && (uuid_str.length() > 0)) {
            throw new DocException("Cannot set UUID of store. UUID is already assigned.");
        } else {
            setDocStoreProperty(storeId, PROP_STORE_UUID, uuid.toString());
            storeManager.registerDocStoreUUID(this, uuid, storeId);
        }
    }


    public String getDocStoreIdByUUID(UUID uuid)
    {
        return storeManager.getDocStoreIdByUUID(this, uuid);
    }


    public UUID getVersionUUID(String storeId, DocVersionId verId)
    {
        String uuid_str = getVersionProperty(storeId, verId, PROP_VERSION_UUID);
        if ((uuid_str != null) && (uuid_str.length() > 0)) {
            return UUID.fromString(uuid_str);
        } else {
            String name_id = storeId + "_" + verId + "_" + System.currentTimeMillis();
            try {
                UUID uuid_new = UUID.nameUUIDFromBytes(name_id.getBytes("UTF-8"));
                setVersionProperty(storeId, verId, PROP_VERSION_UUID, uuid_new.toString());
                storeManager.registerVersionUUID(this, uuid_new, new DocVersionAddress(storeId, verId));
                return uuid_new;
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        }
    }


    public void setVersionUUID(String storeId, DocVersionId verId, UUID uuid) throws DocException
    {
        String uuid_str = getVersionProperty(storeId, verId, PROP_VERSION_UUID);
        if ((uuid_str != null) && (uuid_str.length() > 0)) {
            throw new DocException("Cannot set UUID of version. UUID is already assigned.");
        } else {
            setVersionProperty(storeId, verId, PROP_VERSION_UUID, uuid.toString());
            storeManager.registerVersionUUID(this, uuid, new DocVersionAddress(storeId, verId));
        }
    }


    public DocVersionAddress getVersionAddressByUUID(UUID uuid)
    {
        return storeManager.getVersionAddressByUUID(this, uuid);
    }


    public void addDocListener(DocListener listener) 
    {
        docListeners.add(listener);
        if (docStore != null) docStore.addDocListener(listener);
    }

    public boolean removeDocListener(DocListener listener) 
    {
        if (docStore != null) docStore.removeDocListener(listener);
        return docListeners.remove(listener);
    }

    public DocListener[] getDocListeners() 
    {
        DocListener[] arr = new DocListener[docListeners.size()];
        return (DocListener[]) docListeners.toArray(arr);
    }

    public void addLockListener(LockListener listener) 
    {
        lockListeners.add(listener);
        if (docStore != null) docStore.getLockManager().addListener(listener);
    }

    public boolean removeLockListener(LockListener listener) 
    {
        if (docStore != null) docStore.getLockManager().removeListener(listener);
        return lockListeners.remove(listener);
    }

    public LockListener[] getLockListeners() 
    {
        LockListener[] arr = new LockListener[lockListeners.size()];
        return (LockListener[]) lockListeners.toArray(arr);
    }


}
