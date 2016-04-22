/*
 * DocStoreSession.java
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

import org.docma.lockapi.LockListener;
import java.util.List;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author MP
 */
public interface DocStoreSession {
    
    String getSessionId();
    String getUserId();
    
    String getStoreId();
    DocVersionId getVersionId();
    void openDocStore(String storeId, DocVersionId versionId);
    void closeDocStore();
    String[] listDocStores();
    boolean isDbStore(String storeId);

    UUID getDocStoreUUID(String storeId);
    void setDocStoreUUID(String storeId, UUID uuid) throws DocException;
    String getDocStoreIdByUUID(UUID uuid);
    UUID getVersionUUID(String storeId, DocVersionId verId);
    void setVersionUUID(String storeId, DocVersionId verId, UUID uuid) throws DocException;
    DocVersionAddress getVersionAddressByUUID(UUID uuid);
    
    void addDocStore(String storeId, String[] propNames, String[] propValues)  throws DocException;
    void createDocStore(String storeId, String[] propNames, String[] propValues)  throws DocException;
    void deleteDocStore(String storeId)  throws DocException;
    void deleteDocStore(String storeId, boolean remove_connection_only)  throws DocException;
    void changeDocStoreId(String oldId, String newId)  throws DocException;

    String getDocStoreProperty(String storeId, String name);
    void setDocStoreProperty(String storeId, String name, String value) throws DocException;
    void setDocStoreProperties(String storeId, String[] names, String[] values) throws DocException;
    String[] getDocStorePropertyNames(String storeId);

    String getVersionProperty(String storeId, DocVersionId verId, String name);
    void setVersionProperty(String storeId, DocVersionId verId, String name, String value) throws DocException;
    void setVersionProperties(String storeId, DocVersionId verId, String[] names, String[] values) throws DocException;
    String[] getVersionPropertyNames(String storeId, DocVersionId verId);

    DocVersionId createVersionId(String verId) throws DocException;
    DocVersionId[] listVersions(String storeId);
    // DocVersionId[] getRootVersions(String storeId);
    DocVersionId getLatestVersionId(String storeId);
    void renameVersion(String storeId, DocVersionId oldVerId, DocVersionId newVerId) throws DocException;
    void createVersion(String storeId, DocVersionId baseVersion, DocVersionId newVersion) throws DocException;
    void deleteVersion(String storeId, DocVersionId verId) throws DocException;
    int deleteAllVersions(String storeId) throws DocException;
    int deleteAllVersions(String storeId, ProgressCallback progress) throws DocException;
    Date getVersionCreationDate(String storeId, DocVersionId verId);
    void setVersionCreationDate(String storeId, DocVersionId verId, Date creationDate) throws DocException;
    String getVersionState(String storeId, DocVersionId verId);
    String getVersionState(String storeId, DocVersionId verId, String lang);
    void setVersionState(String storeId, DocVersionId verId, String newState) throws DocException;
    Date getVersionReleaseDate(String storeId, DocVersionId verId);
    void setVersionReleaseDate(String storeId, DocVersionId verId, Date releaseDate) throws DocException;
    DocVersionId getVersionDerivedFrom(String storeId, DocVersionId verId);
    DocVersionId[] getSubVersions(String storeId, DocVersionId verId);

    DocGroup getRoot();
    // DocNode getNode(String idOrAlias);
    DocNode getNodeById(String id);
    DocNode getNodeByAlias(String alias);
    String getNodeIdByAlias(String alias);
    String[] listIds(Class node_class);
    String[] listAliases(Class node_class);
    List<NodeInfo> listNodeInfos(Class node_class);
    // DocGroup getImageRoot();   -> DocGroup vom Typ "Images"
    boolean nodeIdExists(String id);
    
    DocGroup createGroup();
    DocGroup createGroup(String node_id);
    DocXML createXML();
    DocXML createXML(String node_id);
    DocImage createImage();
    DocImage createImage(String node_id);
    DocFile createFile();
    DocFile createFile(String node_id);
    DocReference createReference();
    DocReference createReference(String node_id);
    
    // ExternalLink[] getExternalLinks();   -> DocGroup vom Typ "ExternalLinks"

    DocImageRendition[] getImageRenditions();
    DocImageRendition getImageRendition(String name);
    void addImageRendition(DocImageRendition rendition) throws DocException;
    boolean deleteImageRendition(String renditionName);

    void enterTranslationMode(String lang_code);
    void leaveTranslationMode();
    String getTranslationMode();

    void startTransaction() throws DocException;
    void commitTransaction() throws DocException;
    void rollbackTransaction();
    boolean runningTransaction();

    void addDocListener(DocListener listener);
    boolean removeDocListener(DocListener listener);
    DocListener[] getDocListeners();

    void addLockListener(LockListener listener);
    boolean removeLockListener(LockListener listener);
    LockListener[] getLockListeners();

    void closeSession();
}
