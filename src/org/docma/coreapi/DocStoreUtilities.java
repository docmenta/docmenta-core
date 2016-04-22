/*
 * DocStoreUtilities.java
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

import java.util.*;
import java.io.*;
import org.docma.util.DocmaUtil;

import org.docma.util.Log;


/**
 *
 * @author MP
 */
public class DocStoreUtilities
{
    private static final int MAX_COPY_DEPTH = 60;
    private static final int MAX_VERIFY_ERRORS = 300;
    private static final int VERIFY_PROGRESS_INCREMENT = 25;  // update progress after 25 nodes

    public static final int COPY_NODE_ID_REASSIGN = 0;
    public static final int COPY_NODE_ID_KEEP = 1;
    public static final int COPY_NODE_ID_TRY_KEEP = 2;

    // public static final int COPY_UUID_REASSIGN = 0;
    // public static final int COPY_UUID_KEEP = 1;


    public static String[] getTranslationLanguagesRecursive(DocGroup group)
    {
        SortedSet langs = new TreeSet();
        get_TranslationLanguages(group, langs);
        return (String[]) langs.toArray(new String[langs.size()]);
    }


    public static void copyDocStore(DocStoreSession sourceSession,
                                    String sourceStoreId,
                                    DocStoreSession targetSession,
                                    String targetStoreId,
                                    DocVersionId[] versionIds,  // null means all versions.
                                    String[] trans,             // null means all translations.
                                    String transAsOrig,         // Convert translation to original language.
                                    String origAsTrans,         // Convert original language to translation.
                                    Set<String> skipStoreProps, // Do not overwrite properties of existing store.
                                    ProgressCallback progress, 
                                    boolean verify)
                                    throws DocException
    {
        progress.startWork();
        try {
            progress.setMessage("text.copy_store_started");

            // Calculate set of versions to be skipped:
            Set<DocVersionId> skipVers = new TreeSet<DocVersionId>();
            DocVersionId[] allVers = sourceSession.listVersions(sourceStoreId);
            if (versionIds != null) {
                // Skip all versions except the versions listed in the supplied list.
                skipVers.addAll(Arrays.asList(allVers));
                skipVers.removeAll(Arrays.asList(versionIds));
            }
            
            // Number of steps: one step for each version to be copied. 
            int count_vers = allVers.length - skipVers.size();  // number of versions to be copied
            int steps = verify ? (2 * count_vers) : count_vers;
            progress.setStepsTotal(steps);  // if verify is true: 2 steps for each version: copy and verify
            
            // If store with Id targetStoreId does not exist, then create new store.
            // Otherwise overwrite existing target store.
            if (Arrays.asList(targetSession.listDocStores()).contains(targetStoreId)) {
                // Store with Id targetStoreId exists.
                // Delete all existing versions in target store:
                targetSession.deleteAllVersions(targetStoreId, progress);

                // Copy store properties of the source store; remove properties in 
                // the target store that do not exist in the source store. 
                TreeSet<String> allnames = new TreeSet<String>();
                allnames.addAll(Arrays.asList(targetSession.getDocStorePropertyNames(targetStoreId)));
                allnames.addAll(Arrays.asList(sourceSession.getDocStorePropertyNames(sourceStoreId)));
                allnames.removeAll(skipStoreProps);
                String[] pnames = allnames.toArray(new String[allnames.size()]);
                String[] pvalues = new String[pnames.length];
                for (int i=0; i < pnames.length; i++) {
                    pvalues[i] = sourceSession.getDocStoreProperty(sourceStoreId, pnames[i]);
                }
                targetSession.setDocStoreProperties(targetStoreId, pnames, pvalues);
            } else {
                // Create store with Id targetStoreId. Set properties of source store.
                String[] pnames = sourceSession.getDocStorePropertyNames(sourceStoreId);
                String[] pvalues = new String[pnames.length];
                for (int i=0; i < pnames.length; i++) {
                    pvalues[i] = sourceSession.getDocStoreProperty(sourceStoreId, pnames[i]);
                }
                targetSession.createDocStore(targetStoreId, pnames, pvalues);
            } 

            checkCanceledByUser(progress);
            progress.setPercent(1);  // indicate that initialization is finished
            
            // Recursively copy source versions, starting with root version(s):
            DocVersionId[] rootVerIds = getRootVersions(sourceSession, sourceStoreId);
            copyVersionsRecursive(sourceSession, sourceStoreId,
                                  targetSession, targetStoreId,
                                  rootVerIds, skipVers,
                                  trans, transAsOrig, origAsTrans, progress, verify);
            progress.setMessage("text.copy_store_versions_finished_success");
        } catch (DocException ex) {
            progress.logError("text.copy_store_finished_error", ex.getLocalizedMessage());
            throw ex;
        } finally {
            progress.finishWork();
        }
    }

    public static DocVersionId[] getRootVersions(DocStoreSession sess, String storeId)
    {
        DocVersionId[] all_ids = sess.listVersions(storeId);
        ArrayList root_list = new ArrayList();
        for (int i=0; i < all_ids.length; i++) {
            if (sess.getVersionDerivedFrom(storeId, all_ids[i]) == null) {
                root_list.add(all_ids[i]);
            }
        }
        return (DocVersionId[]) root_list.toArray(new DocVersionId[root_list.size()]);
    }

    public static int copyNodesToPosition(DocNode[] sourceNodes,
                                          DocStoreSession sourceSession,
                                          DocGroup targetParent,
                                          DocNode nodeAfter,
                                          DocStoreSession targetSession,
                                          String[] languages,
                                          boolean commitEachNode,
                                          int copyNodeIdMode,
                                          Map nodeIdMap,
                                          AliasRenameStrategy aliasRename,
                                          Map aliasMap,
                                          ContentCopyStrategy contentCopy)
                                          throws DocException
    {
        boolean started = false;
        int cnt = 0;
        try {
            for (int i=0; i < sourceNodes.length; i++) {
                if (commitEachNode && !targetSession.runningTransaction()) {
                    targetSession.startTransaction();
                    started = true;
                }
                DocNode source = sourceNodes[i];
                DocNode copy = createNodeOfSameType(source, targetSession, copyNodeIdMode);
                if (nodeIdMap != null) {
                    String source_id = source.getId();
                    String copy_id = copy.getId();
                    if (!source_id.equals(copy_id)) nodeIdMap.put(source_id, copy_id);
                }
                targetParent.insertBefore(copy, nodeAfter);
                cnt += copyNode(source, sourceSession, copy, targetSession, languages,
                                commitEachNode, copyNodeIdMode, nodeIdMap,
                                aliasRename, aliasMap, contentCopy);
                if (commitEachNode && targetSession.runningTransaction()) {
                    targetSession.commitTransaction();
                }
            }
        } catch (Exception ex) {
            if (started && targetSession.runningTransaction()) {
                targetSession.rollbackTransaction();
            }
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocException(ex);
        }
        return cnt;
    }

    public static DocNode createNodeOfSameType(DocNode sourceNode,
                                               DocStoreSession targetSession,
                                               int copyNodeIdMode)
    {
        String node_id;
        if (copyNodeIdMode == COPY_NODE_ID_KEEP) {
            node_id = sourceNode.getId();
        } else
        if (copyNodeIdMode == COPY_NODE_ID_TRY_KEEP) {
            node_id = sourceNode.getId();
            if (targetSession.nodeIdExists(node_id)) {
                node_id = null;   // reassign new id
            }
        } else
        if (copyNodeIdMode == COPY_NODE_ID_REASSIGN) {
            node_id = null;
        } else {
            throw new DocRuntimeException("Invalid copyNodeId mode: " + copyNodeIdMode);
        }
        DocNode node = null;
        if (sourceNode instanceof DocXML) {
            node = (node_id == null) ? targetSession.createXML() : targetSession.createXML(node_id);
        } else
        if (sourceNode instanceof DocGroup) {
            node = (node_id == null) ? targetSession.createGroup() : targetSession.createGroup(node_id);
        } else
        if (sourceNode instanceof DocImage) {
            node = (node_id == null) ? targetSession.createImage() : targetSession.createImage(node_id);
        } else
        if (sourceNode instanceof DocFile) {
            node = (node_id == null) ? targetSession.createFile() : targetSession.createFile(node_id);
        } else
        if (sourceNode instanceof DocReference) {
            node = (node_id == null) ? targetSession.createReference() : targetSession.createReference(node_id);
        }
        return node;
    }

    public static int copyNode(DocNode sourceNode,
                               DocStoreSession sourceSession,
                               DocNode targetNode,
                               DocStoreSession targetSession,
                               String[] languages,
                               boolean commitEachNode,
                               int copyNodeIdMode,
                               Map nodeIdMap,
                               AliasRenameStrategy aliasRename,
                               Map aliasMap,
                               ContentCopyStrategy contentCopy)
                               throws DocException
    {
        boolean started = false;
        if (commitEachNode && !targetSession.runningTransaction()) {
            targetSession.startTransaction();
            started = true;
        }
        try {
            copyNodeShallow(sourceNode, sourceSession, targetNode, targetSession,
                            languages, aliasRename, aliasMap, contentCopy);
            if (commitEachNode) {
                targetSession.commitTransaction();
            }
        } catch (Exception ex) {
            if (started && targetSession.runningTransaction()) {
                targetSession.rollbackTransaction();
            }
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocException(ex);
        }
        int cnt = 1;  // the targetNode itself counts one
        if ((sourceNode instanceof DocGroup) && (targetNode instanceof DocGroup)) {
            cnt += copyNodeChildren((DocGroup) sourceNode, sourceSession,
                                    (DocGroup) targetNode, targetSession,
                                    languages, MAX_COPY_DEPTH, commitEachNode,
                                    copyNodeIdMode, nodeIdMap,
                                    aliasRename, aliasMap, contentCopy);
        }
        return cnt;  // the targetNode itself plus number of children (recursively)
    }

    public static void copyNodeShallow(DocNode sourceNode,
                                       DocStoreSession sourceSession,
                                       DocNode targetNode,
                                       DocStoreSession targetSession,
                                       String[] languages,
                                       AliasRenameStrategy aliasRename,
                                       Map aliasMap,
                                       ContentCopyStrategy contentCopy)
                                       throws DocException
    {
        // targetSession has to be in original mode.
        if (targetSession.getTranslationMode() != null) {
            throw new DocException("copyNodeShallow(): target session has to be in original mode!");
        }
        // languages has to be null if sourceSession is in translation mode.
        boolean sourceTranslationMode = (sourceSession.getTranslationMode() != null);
        if ((languages != null) && sourceTranslationMode) {
            throw new DocException("copyNodeShallow(): languages argument has to be null if source session is in translation mode!");
        }

        // copy alias names; rename alias if name is already used in target session
        String[] aliases = sourceNode.getAliases();
        renameAliasesIfNotUnique(aliases, targetSession, aliasRename, aliasMap);
        targetNode.setAliases(aliases);

        // if DocReference: copy target-alias
        if ((sourceNode instanceof DocReference) && (targetNode instanceof DocReference)) {
            ((DocReference) targetNode).setTargetAlias(((DocReference) sourceNode).getTargetAlias());
        }

        // copy title
        targetNode.setTitle(sourceNode.getTitle());

        // copy attributes
        String[] attnames = sourceNode.getAttributeNames();
        for (int i=0; i < attnames.length; i++) {
            targetNode.setAttribute(attnames[i], sourceNode.getAttribute(attnames[i]));
        }

        // if DocContent: copy content, content-type, file-extension (all languages)
        DocContent sourceCont = null;
        DocContent targetCont = null;
        boolean is_content = (sourceNode instanceof DocContent) && (targetNode instanceof DocContent);
        if (is_content) {
            sourceCont = (DocContent) sourceNode;
            targetCont = (DocContent) targetNode;
            targetCont.setContentType(sourceCont.getContentType());
            targetCont.setFileExtension(sourceCont.getFileExtension());
            if (contentCopy == null) {
                contentCopy = DefaultContentCopyStrategy.getInstance();
            }
            contentCopy.copyContent(sourceCont, sourceSession, targetCont, targetSession);
        }

        // if DocFile: filename is combination of title and file-extension -> nothing has to be done
        // if DocImage: no additional fields -> nothing has to be copied
        // if DocXML: no additional fields -> nothing has to be copied

        // If sourceSession is in original mode and languages is null, then copy all translations of the node.
        // If only original content shall be copied, then empty array has to be provided as languages argument.
        if ((languages == null) && !sourceTranslationMode) {
            languages = sourceNode.getTranslations();
        }
        // Copy translations (only allowed if sourceSession is in original mode)
        if (languages != null) {
            for (int i=0; i < languages.length; i++) {
                String lang_id = languages[i];

                // copy title (translated)
                String trans_title = sourceNode.getTitle(lang_id);
                if (trans_title != null) targetNode.setTitle(trans_title, lang_id);

                // copy attributes (translated)
                attnames = sourceNode.getAttributeNames();
                for (int k=0; k < attnames.length; k++) {
                    String attvalue = sourceNode.getAttribute(attnames[k], lang_id);
                    if (attvalue != null) targetNode.setAttribute(attnames[k], attvalue, lang_id);
                }

                // if DocContent: copy content, content-type, file-extension (translated)
                if (is_content) {
                    if (sourceCont.hasContent(lang_id)) {
                        String orig_type = sourceCont.getContentType();
                        String orig_ext = sourceCont.getFileExtension();
                        try {
                            sourceSession.enterTranslationMode(lang_id);
                            targetSession.enterTranslationMode(lang_id);

                            String trans_type = sourceCont.getContentType();
                            String trans_ext = sourceCont.getFileExtension();
                            if ((trans_type != null) && !trans_type.equals(orig_type)) {
                                targetCont.setContentType(trans_type);
                            }
                            if ((trans_ext != null) && !trans_ext.equals(orig_ext)) {
                                targetCont.setFileExtension(trans_ext);
                            }
                            contentCopy.copyContent(sourceCont, sourceSession, targetCont, targetSession);
                        } finally {
                            sourceSession.leaveTranslationMode();
                            targetSession.leaveTranslationMode();
                        }
                    }
                }
            }  // for all languages
        }  // if (languages != null)
    }

    public static int copyNodeChildren(DocGroup sourceNode,
                                       DocStoreSession sourceSession,
                                       DocGroup targetNode,
                                       DocStoreSession targetSession,
                                       String[] languages,
                                       int max_depth,
                                       boolean commitEachNode,
                                       int copyNodeIdMode,
                                       Map nodeIdMap,
                                       AliasRenameStrategy aliasRename,
                                       Map aliasMap,
                                       ContentCopyStrategy contentCopy)
                                       throws DocException
    {
        if (max_depth < 0) {
            throw new DocException("Reached maximum copy depth!");
        }
        boolean started = false;
        try {
            int cnt = 0;
            DocNode[] children = sourceNode.getChildNodes();
            for (int i=0; i < children.length; i++) {
                if (commitEachNode && !targetSession.runningTransaction()) {
                    targetSession.startTransaction();
                    started = true;
                }
                DocNode child = children[i];
                DocNode clone = createNodeOfSameType(child, targetSession, copyNodeIdMode);
                if (nodeIdMap != null) {
                    String child_id = child.getId();
                    String clone_id = clone.getId();
                    if (!child_id.equals(clone_id)) nodeIdMap.put(child_id, clone_id);
                }
                targetNode.appendChild(clone);
                copyNodeShallow(child, sourceSession, clone, targetSession,
                                languages, aliasRename, aliasMap, contentCopy);
                if (commitEachNode) {
                    targetSession.commitTransaction();
                }
                cnt++;
                if (child instanceof DocGroup) {
                    DocGroup child_group = (DocGroup) child;
                    DocGroup clone_group = (DocGroup) clone;
                    cnt += copyNodeChildren(child_group, sourceSession, 
                                            clone_group, targetSession,
                                            languages, max_depth - 1,
                                            commitEachNode,
                                            copyNodeIdMode, nodeIdMap,
                                            aliasRename, aliasMap, contentCopy);
                }
            }
            return cnt;
        } catch (Exception ex) {
            if (started && targetSession.runningTransaction()) {
                targetSession.rollbackTransaction();
            }
            if (ex instanceof DocException) throw (DocException) ex;
            else throw new DocException(ex);
        }
    }


    /* --------  Private helper methods for copying nodes ------------------ */

    private static void renameAliasesIfNotUnique(String[] aliases,
                                                 DocStoreSession targetSession,
                                                 AliasRenameStrategy aliasStrategy,
                                                 Map aliasMap) throws DocException
    {
        if (aliasStrategy == null) {
            aliasStrategy = DefaultAliasRenameStrategy.getInstance();
        }
        List aliases_list = Arrays.asList(aliases);
        for (int i=0; i < aliases.length; i++) {
            int loop_count = 0;
            String target_alias = aliases[i];
            while (targetSession.getNodeIdByAlias(target_alias) != null) {
                if (++loop_count > 100) {  // avoid infinite loop due to bad rename strategy
                    throw new DocRuntimeException("Could not find valid alias name: " + target_alias);
                }
                target_alias = aliasStrategy.renameAlias(target_alias);
                if (aliases_list.contains(target_alias)) {
                    // If new alias name was already generated before, generate new alias
                    target_alias = aliasStrategy.renameAlias(target_alias);
                }
            }
            if (loop_count > 0) {  // ! target_alias.equals(aliases[i])
                aliasMap.put(aliases[i], target_alias);
                aliases[i] = target_alias;
            }
        }
    }


    /* ----------  Private methods for copying a complete store ------------ */


    private static void checkCanceledByUser(ProgressCallback progress) throws DocException
    {
        if (progress == null) {  // null may be provided on deeper levels of the recursive copy operation 
            return;
        }
        if (progress.getCancelFlag()) {
            throw new DocException("The operation has been canceled by the user!");
        }
    }

    private static void copyVersionsRecursive(DocStoreSession sourceSession,
                                              String sourceStoreId,
                                              DocStoreSession targetSession,
                                              String targetStoreId,
                                              DocVersionId[] verIds, 
                                              Set<DocVersionId> skipVerIds,
                                              String[] trans, 
                                              String transAsOrig, 
                                              String origAsTrans, 
                                              ProgressCallback progress, 
                                              boolean verify)
                                              throws DocException
    {
        // Copy versions
        for (DocVersionId vid : verIds) {
            checkCanceledByUser(progress);
            if (! skipVerIds.contains(vid)) {
                progress.setMessage("text.copy_store_version", vid.toString());
                CopyVersionStatistics stats = null;
                final int MAX_LOOPS = 3;
                boolean copy_failed = false;
                int loop = 0;
                do {
                    ++loop;
                    if (copy_failed) {
                        // Delete content of failed copy operation before retrying.
                        if (progress != null) { 
                            progress.setMessage("text.progress_delete_version", vid.toString());
                        }
                        // If version in target store is still open, close it before deleting it 
                        if (targetSession.getStoreId() != null) {
                            targetSession.closeDocStore();
                        }
                        targetSession.deleteVersion(targetStoreId, vid);
                    }
                    try {
                        stats = copyVersion(sourceSession, sourceStoreId, 
                                            targetSession, targetStoreId, 
                                            vid, trans, transAsOrig, origAsTrans, progress);
                        copy_failed = false;  // copy was successful
                    } catch (Exception ex) {
                        copy_failed = true;
                        if ((loop >= MAX_LOOPS) || ((progress != null) && progress.getCancelFlag())) {
                            // Rethrow exception
                            if (ex instanceof DocException) throw (DocException) ex;
                            else throw new DocException(ex);
                        } else {
                            ex.printStackTrace();
                            // Supress exception and retry copy operation in next loop
                            progress.logInfo("error.exception", ex.getMessage());
                        }
                    }
                } while (copy_failed && (loop < MAX_LOOPS));
                progress.stepFinished();
                checkCanceledByUser(progress);
                
                if (verify) {
                    boolean extended = (transAsOrig == null);
                    verifyVersion(sourceSession, sourceStoreId, 
                                  targetSession, targetStoreId, 
                                  vid, stats, extended, progress);
                    progress.stepFinished();
                    checkCanceledByUser(progress);
                }
            }
            DocVersionId[] subvers = sourceSession.getSubVersions(sourceStoreId, vid);
            copyVersionsRecursive(sourceSession, sourceStoreId, 
                                  targetSession, targetStoreId,
                                  subvers, skipVerIds, 
                                  trans, transAsOrig, origAsTrans, progress, verify);
        }
    }


    private static CopyVersionStatistics copyVersion(DocStoreSession sourceSession,
                                                     String sourceStoreId,
                                                     DocStoreSession targetSession,
                                                     String targetStoreId,
                                                     DocVersionId verId, 
                                                     String[] trans, 
                                                     String transAsOrig, 
                                                     String origAsTrans, 
                                                     ProgressCallback progress)
    throws DocException
    {
        CopyVersionStatistics stats = new CopyVersionStatistics();
        if (targetSession.getTranslationMode() != null) {
            targetSession.leaveTranslationMode();
            Log.warning("copyVersion(): Unexpected translation mode in target session. Leaving translation mode.");
        }
        if (sourceSession.getTranslationMode() != null) {
            sourceSession.leaveTranslationMode();
            Log.warning("copyVersion(): Unexpected translation mode in source session. Leaving translation mode.");
        }
        //
        // Create version in target store
        //
        DocVersionId baseVerId = sourceSession.getVersionDerivedFrom(sourceStoreId, verId);
        Date create_date = sourceSession.getVersionCreationDate(sourceStoreId, verId);

        // Determine base version in target store (if versions have been skipped)
        List targetVers = Arrays.asList(targetSession.listVersions(targetStoreId));
        while ((baseVerId != null) && !targetVers.contains(baseVerId)) {
            baseVerId = sourceSession.getVersionDerivedFrom(sourceStoreId, baseVerId);
        }
        targetSession.createVersion(targetStoreId, baseVerId, verId);
        targetSession.setVersionCreationDate(targetStoreId, verId, create_date);

        //
        // Copy version properties
        //
        Set<String> allnames = new TreeSet<String>();
        // Add property names of target version:
        // NOTE: If the target version is derived from a base version, then the 
        // properties of the base version already exist in the target version.
        // Properties that exist in the target version but not in the
        // source version have to be removed (set to null) in the target
        // version. Therefore the property names of the target version have 
        // to be added to the list of property names. 
        allnames.addAll(Arrays.asList(targetSession.getVersionPropertyNames(targetStoreId, verId)));
        // Add property names of source version:
        String[] pnames = sourceSession.getVersionPropertyNames(sourceStoreId, verId);
        allnames.addAll(Arrays.asList(pnames));
        pnames = allnames.toArray(pnames);
        String[] pvalues = new String[pnames.length];
        for (int i=0; i < pnames.length; i++) {
            pvalues[i] = sourceSession.getVersionProperty(sourceStoreId, verId, pnames[i]);
        }
        targetSession.setVersionProperties(targetStoreId, verId, pnames, pvalues);

        //
        // Synchronize content from source to target recursively (including translated content)
        //
        sourceSession.openDocStore(sourceStoreId, verId);
        targetSession.openDocStore(targetStoreId, verId);
        DocGroup source_root = sourceSession.getRoot();
        DocGroup target_root = targetSession.getRoot();
        
        Set<String> trans_set = new TreeSet<String>();
        if (trans == null) {
            get_TranslationLanguages(sourceSession.getRoot(), trans_set);
        } else {
            trans_set.addAll(Arrays.asList(trans));
        }
        if (transAsOrig != null) {
            trans_set.remove(transAsOrig);
            String st = sourceSession.getVersionState(sourceStoreId, verId, transAsOrig);
            if (DocVersionState.TRANSLATION_PENDING.equals(st)) {
                throw new DocException("Cannot copy pending translation to original language: " + verId + " " + transAsOrig);
            }
        }
        if (origAsTrans != null) {
            trans_set.remove(origAsTrans);
        }
        
        // Exclude pending translations from translations to be copied
        Iterator<String> it = trans_set.iterator();
        while (it.hasNext()) {
            String st = sourceSession.getVersionState(sourceStoreId, verId, it.next());
            if (DocVersionState.TRANSLATION_PENDING.equals(st)) {
                it.remove();
            }
        }
        stats.copiedTranslations = trans_set.toArray(new String[trans_set.size()]);
        
        int cnt = syncNodeRecursive(source_root, sourceSession, 
                                    target_root, targetSession, 
                                    trans_set, transAsOrig, origAsTrans, 
                                    targetSession.isDbStore(targetStoreId), progress);
        stats.copiedNodes = cnt;
        
        if (targetSession.runningTransaction()) {
            targetSession.commitTransaction();
            Log.warning("copyVersion(): Unexpected transaction state: running. Closing transaction.");
        }
        if (targetSession.getTranslationMode() != null) {
            targetSession.leaveTranslationMode();
            Log.warning("copyVersion(): Unexpected translation mode in target session. Leaving translation mode.");
        }
        if (sourceSession.getTranslationMode() != null) {
            sourceSession.leaveTranslationMode();
            Log.warning("copyVersion(): Unexpected translation mode in source session. Leaving translation mode.");
        }

        //
        // Copy image rendition settings
        //
        DocImageRendition[] rends = sourceSession.getImageRenditions();
        targetSession.startTransaction();
        try {
            for (DocImageRendition irend : rends) {
                if (targetSession.getImageRendition(irend.getName()) == null) {
                   targetSession.addImageRendition(irend);
                }
            }
            targetSession.commitTransaction();
        } catch (DocException ex) {
            targetSession.rollbackTransaction();
            throw ex;
        }
        
        //
        // Copy version state and release date (per language)
        //
        
        // Set state and release date of the original language:
        copyVersionStateAndReleaseDate(sourceSession, sourceStoreId, 
                                       targetSession, targetStoreId, verId, null, null);
        
        // Set state and release date of the translation languages:
        for (String lang : trans_set) {
            copyVersionStateAndReleaseDate(sourceSession, sourceStoreId, 
                                           targetSession, targetStoreId, verId, lang, lang);
        }
        if (origAsTrans != null) {
            copyVersionStateAndReleaseDate(sourceSession, sourceStoreId, 
                                           targetSession, targetStoreId, verId, null, origAsTrans);
        }

        sourceSession.closeDocStore();
        targetSession.closeDocStore();

        progress.logInfo("text.copy_store_node_count", cnt);
        String msg_prefix = "Synchronization of version " + verId + " from " +
                            sourceStoreId + " to " + targetStoreId + ": ";
        Log.info(msg_prefix + cnt + " nodes updated!");
        
        return stats;
    }

    private static void copyVersionStateAndReleaseDate(DocStoreSession sourceSession,
                                                       String sourceStoreId,
                                                       DocStoreSession targetSession, 
                                                       String targetStoreId, 
                                                       DocVersionId verId, 
                                                       String sourceLang, 
                                                       String targetLang) 
    throws DocException
    {
        if (sourceLang == null) {
            if (sourceSession.getTranslationMode() != null) { 
                sourceSession.leaveTranslationMode();
            }
        } else {
            sourceSession.enterTranslationMode(sourceLang);
        }
        if (targetLang == null) {
            if (targetSession.getTranslationMode() != null) { 
                targetSession.leaveTranslationMode();
            }
        } else {
            targetSession.enterTranslationMode(targetLang);
        }
        try {
            String sourceState = sourceSession.getVersionState(sourceStoreId, verId);
            String targetState = targetSession.getVersionState(targetStoreId, verId);
            boolean pending = (sourceLang != null) && (sourceState != null) && 
                              sourceState.equals(DocVersionState.TRANSLATION_PENDING);
            // Note: Do not set the the state TRANSLATION_PENDING, as this state
            //       is set automatically in case a version is derived from a 
            //       Draft translation. 
            if ((sourceState != null) && (!pending) && !sourceState.equals(targetState)) {
                targetSession.setVersionState(targetStoreId, verId, sourceState);
            }
            Date sourceDate = sourceSession.getVersionReleaseDate(sourceStoreId, verId);
            Date targetDate = targetSession.getVersionReleaseDate(targetStoreId, verId);
            if ((sourceDate != null) && !sourceDate.equals(targetDate)) {
                targetSession.setVersionReleaseDate(targetStoreId, verId, sourceDate);
            }
        } finally {
            if (sourceLang != null) sourceSession.leaveTranslationMode();
            if (targetLang != null) targetSession.leaveTranslationMode();
        }
    }

    private static int syncNodeRecursive(DocNode sourceNode,
                                         DocStoreSession sourceSession,
                                         DocNode targetNode,
                                         DocStoreSession targetSession,
                                         Set<String> trans,
                                         String transAsOrig,
                                         String origAsTrans, 
                                         boolean isDbTarget,
                                         ProgressCallback progress) 
    throws DocException
    {
        int node_count = 1;  // this node counts 1
        checkCanceledByUser(progress);
            
        boolean started = false;
        try {
            if (! targetSession.runningTransaction()) {
                targetSession.startTransaction();
                started = true;
            }

            // Synchronize node content
            syncNodeShallow(sourceNode, sourceSession, targetNode, targetSession, trans, transAsOrig, origAsTrans, isDbTarget);

            // If the node is a group node, then synchronize the child nodes
            if (targetNode instanceof DocGroup) {
                DocGroup sourceGrp = (DocGroup) sourceNode;
                DocGroup targetGrp = (DocGroup) targetNode;
                DocNode[] srcChildren = sourceGrp.getChildNodes();
                DocNode[] tarChildren = targetGrp.getChildNodes();

                // Synchronize ordering of child nodes and create new nodes
                for (int srcPos = 0; srcPos < srcChildren.length; srcPos++) {
                    DocNode srcChild = srcChildren[srcPos];
                    String src_id = srcChild.getId();
                    // Find src_id in targetSession
                    int targPos = getNodePosInArray(src_id, tarChildren, srcPos);
                    DocNode targNode = (targPos >= 0) ? tarChildren[targPos] : targetSession.getNodeById(src_id);
                    if ((targNode != null) && !nodesHaveSameType(srcChild, targNode)) {
                        // Node with same ID but different type exists in target store.
                        // This can occur if in the derived version of the source 
                        // store, a node has been deleted and a new node with 
                        // the same ID than the deleted node has been created.
                        // -> Delete the node and create a new node of same type (with same ID)
                        deleteSingleNode(targetSession, targNode, targetGrp);
                        tarChildren = targetGrp.getChildNodes();  // refresh array
                        targNode = null;  // create new node of same type (see below)
                        targPos = -1;
                    }
                    if (targNode == null) { // source node does not exist in target store
                        // Create new node of same type and with same id as the source node
                        targNode = createNodeOfSameType(srcChild, targetSession, COPY_NODE_ID_KEEP);
                    }

                    // If the target node is a new node or is not on the same level  
                    // or is on the same level but at a different position, then 
                    // insert target node at the same position as source node.
                    // Note: targPos < srcPos is not possible.
                    if ((targPos < 0) || (targPos > srcPos)) {
                        if (srcPos < tarChildren.length) {
                            targetGrp.insertBefore(targNode, tarChildren[srcPos]);
                        } else {
                            targetGrp.appendChild(targNode);
                        }
                        tarChildren = targetGrp.getChildNodes();  // refresh array
                    }
                }

                // Remove child nodes in target session that do not exist in the source session
                boolean removed = false;
                int i = srcChildren.length;
                while (i < tarChildren.length) {
                    DocNode tarChild = tarChildren[i];
                    if (! sourceSession.nodeIdExists(tarChild.getId())) {
                        deleteSingleNode(targetSession, tarChild, targetGrp); // targetGrp.removeChild(tarChild);
                        tarChildren = targetGrp.getChildNodes();  // refresh array
                        removed = true;
                        // i does not(!) have to be increased as nodes following 
                        // the deleted node move 1 up in the list
                    } else {
                        i++;
                    }
                }

                // Avoid transaction over more than one level
                if (isDbTarget && ((srcChildren.length > 0) || removed) && targetSession.runningTransaction()) {
                    targetSession.commitTransaction();
                    targetSession.startTransaction();
                }

                // Show progress: show how many of the 1st-level nodes have 
                // already been synchronized.
                boolean is_root_level = (progress != null);
                if (is_root_level) {   // true if 1st-level
                    progress.startWork(srcChildren.length);
                }
                // Synchronize child nodes recursively
                try {
                    for (int srcPos = 0; srcPos < srcChildren.length; srcPos++) {
                        DocNode srcChild = srcChildren[srcPos];
                        DocNode tarChild = tarChildren[srcPos];
                        if (! srcChild.getId().equals(tarChild.getId())) {
                            throw new DocRuntimeException("Synchronisation of child nodes failed: ID mismatch!");
                        }
                        node_count += syncNodeRecursive(srcChild, sourceSession, 
                                                        tarChild, targetSession, 
                                                        trans, transAsOrig, origAsTrans, 
                                                        isDbTarget,
                                                        null);  // show progress only for 1st tree-level
                        if (is_root_level) { // show progress for 1st tree-level
                            progress.stepFinished();
                            checkCanceledByUser(progress);
                        }
                    }
                    // Commit all changes done to the child nodes 
                    if (isDbTarget && (srcChildren.length > 0) && targetSession.runningTransaction()) {
                        targetSession.commitTransaction();
                        // targetSession.startTransaction();
                    }
                } finally {
                    if (is_root_level) {   // true if 1st-level
                        progress.finishWork();
                    }
                }
                // All child nodes from 0...srcChildren.length are now synchronized.
                // However, the array tarChildren may have additional nodes 
                // at the end which will be moved to another parent later on.
            }
            if (started && targetSession.runningTransaction()) {
                targetSession.commitTransaction();
            }
        } catch (Exception ex) {
            if (started && targetSession.runningTransaction()) {
                targetSession.rollbackTransaction();
            }
            if (ex instanceof DocException) throw (DocException) ex;
            if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
            else throw new DocRuntimeException(ex);
        }
        return node_count;
    }

    private static int deleteSingleNode(DocStoreSession targetSession, 
                                        DocNode delNode, 
                                        DocGroup replacementGrp) throws DocException
    {
        int moved = 0;
        if (delNode instanceof DocContent) {
            ((DocContent) delNode).deleteContent();
        } else 
        if (delNode instanceof DocGroup) {
            // If a group node shall be deleted, then all child nodes of the
            // group node have to be moved to the replacement group first.
            // Otherwise not only delNode, but also all descendants would be deleted.
            DocGroup delGrp = (DocGroup) delNode;
            DocNode[] narr = delGrp.getChildNodes();
            for (DocNode n : narr) {
                delGrp.removeChild(n);  // remove n from group to be deleted
                replacementGrp.appendChild(n);
                ++moved;
            }
        }
        
        // Delete delNode by removing the node from its parent group
        DocGroup parGrp = delNode.getParentGroup();
        if (parGrp != null) {
            // If two different instances of the same group have been created,
            // remove from the instance that was used as replacement group.
            // (just to avoid caching problems; should not be required as normally  
            // only one instance is returned for the same group)
            DocGroup g = parGrp.getId().equals(replacementGrp.getId()) ? replacementGrp : parGrp;
            g.removeChild(delNode);
        } else {
            Log.error("Node to be deleted has no parent group: " + delNode.getId());
        }

        // Immediatelly delete the node to allow creation of new nodes with the same ID!
        if (targetSession.runningTransaction()) {
            targetSession.commitTransaction();
            targetSession.startTransaction();
        }
        return moved;
    }

    private static void syncNodeShallow(DocNode sourceNode,
                                        DocStoreSession sourceSession,
                                        DocNode targetNode,
                                        DocStoreSession targetSession, 
                                        Set<String> trans,
                                        String transAsOrig,
                                        String origAsTrans, 
                                        boolean isDbTarget) throws DocException
    {
        boolean srcTransactStarted = false;
        try {
            // Note: Following lines are commented out, because starting a 
            //       transaction for a filesystem store means that index.xml file
            //       is written at commit (which is not required for read-only access).
            // if (! sourceSession.runningTransaction()) {
            //     sourceSession.startTransaction();
            //     srcTransactStarted = true;
            // }
        
            // Synchronize original language in target store.
            // Note: If transAsOrig is not null, then the translation transAsOrig in the
            // source store is converted to the original language in the target store.
            syncNodeShallowLang(sourceNode, sourceSession, targetNode, targetSession, transAsOrig, null, isDbTarget);

            if (origAsTrans != null) {
                // Convert the original language in the source store to the translation 
                // language origAsTrans in the target store.
                syncNodeShallowLang(sourceNode, sourceSession, targetNode, targetSession, null, origAsTrans, isDbTarget);
            }

            // Synchronize translations
            if (trans == null) {
                trans = new TreeSet<String>();
                trans.addAll(Arrays.asList(sourceNode.getTranslations()));
                trans.addAll(Arrays.asList(targetNode.getTranslations()));
            }
            for (String lang_code : trans) {
                // The languages transAsOrig and origAsTrans have already been handled above.
                if (! (lang_code.equals(transAsOrig) || lang_code.equals(origAsTrans))) {
                    if (targetNode.hasTranslation(lang_code) && !sourceNode.hasTranslation(lang_code)) {
                        // If the translation lang_code no longer exists in the source
                        // store, then delete the translation in the target store.
                        targetNode.deleteTranslation(lang_code);
                    } else {
                        // Synchronize the translation lang_code in the target store.
                        syncNodeShallowLang(sourceNode, sourceSession, 
                                            targetNode, targetSession, lang_code, lang_code, isDbTarget);
                    }
                }
            }
            if (srcTransactStarted && sourceSession.runningTransaction()) {
                sourceSession.commitTransaction();
            }
        } catch (Exception ex) {
            if (srcTransactStarted && sourceSession.runningTransaction()) {
                sourceSession.rollbackTransaction();
            }
            if (ex instanceof DocException) throw (DocException) ex;
            if (ex instanceof DocRuntimeException) throw (DocRuntimeException) ex;
            else throw new DocRuntimeException(ex);
        }
    }


    private static void syncNodeShallowLang(DocNode sourceNode,
                                            DocStoreSession sourceSession,
                                            DocNode targetNode,
                                            DocStoreSession targetSession, 
                                            String sourceLang, 
                                            String targetLang, 
                                            boolean isDbTarget) throws DocException
    {
        if (DocConstants.DEBUG) {
            System.out.println("SyncNodeShallowLang (" + sourceLang + "/" + targetLang + ")");
        }
        if (sourceLang == null) {
            sourceSession.leaveTranslationMode();
        } else {
            sourceSession.enterTranslationMode(sourceLang);
        }
        if (targetLang == null) {
            targetSession.leaveTranslationMode();
        } else {
            targetSession.enterTranslationMode(targetLang);
        }

        boolean convertTransToOrig = (targetLang == null) && (sourceLang != null);
        // Note: convertTransToOrig is true, if a translation language in the  
        //       source store is converted to the original language in the target store.
        //       In this case, if no translated value exists, then the original 
        //       has to be kept.

        boolean syncOriginal = (targetLang == null);
        if (syncOriginal) {
            // Synchronize alias names
            String[] src_aliases = sourceNode.getAliases();
            String[] tar_aliases = targetNode.getAliases();
            if (! Arrays.equals(src_aliases, tar_aliases)) {
                // Log.info("Source aliases: " + DocmaUtil.concatStrings(src_aliases, ",") + 
                //          "  Target aliases: " + DocmaUtil.concatStrings(tar_aliases, ","));
                targetNode.setAliases(src_aliases);
            }

            // If DocReference: Synchronize target-alias
            if (sourceNode instanceof DocReference) {
                DocReference src_ref = (DocReference) sourceNode;
                DocReference tar_ref = (DocReference) targetNode;
                String src_ref_alias = src_ref.getTargetAlias();
                String tar_ref_alias = tar_ref.getTargetAlias();
                if ((src_ref_alias == null) ? (tar_ref_alias != null) : !src_ref_alias.equals(tar_ref_alias)) {
                    tar_ref.setTargetAlias(src_ref_alias);
                }
            }
        }

        // Synchronize title
        String src_title = sourceNode.getTitle(sourceLang);
        String tar_title = targetNode.getTitle(targetLang);
        if (convertTransToOrig && (src_title == null)) { 
            src_title = sourceNode.getTitle(null);
        }
        if ((src_title == null) ? (tar_title != null) : !src_title.equals(tar_title)) {
            // If source differs from target, then synchronize target title
            targetNode.setTitle(src_title, targetLang);
        }

        // Synchronize attributes
        syncNodeAttributes(sourceNode, sourceLang, targetNode, targetLang);

        // If DocContent: Synchronize content, content-type, file-extension
        boolean is_content = (sourceNode instanceof DocContent);
        if (is_content) {
            DocContent sourceCont = (DocContent) sourceNode;
            DocContent targetCont = (DocContent) targetNode;
                        
            // Synchronize content-type
            String src_ct = sourceCont.getContentType(sourceLang);
            String tar_ct = targetCont.getContentType(targetLang);
            if (convertTransToOrig && (src_ct == null)) {
                src_ct = sourceCont.getContentType(null);
            }
            if ((src_ct == null) ? (tar_ct != null) : !src_ct.equals(tar_ct)) {
                // If source differs from target, then synchronize target content-type
                targetCont.setContentType(src_ct);
            }

            // Synchronize file-extension
            String src_ext = sourceCont.getFileExtension(sourceLang);
            String tar_ext = targetCont.getFileExtension(targetLang);
            if (convertTransToOrig && (src_ext == null)) {
                src_ext = sourceCont.getFileExtension(null);
            }
            if ((src_ext == null) ? (tar_ext != null) : !src_ext.equals(tar_ext)) {
                // If source differs from target, then synchronize target file-extension
                targetCont.setFileExtension(src_ext);
            }
            
            // Synchronize content
            try {
                boolean do_commit = isDbTarget && targetSession.runningTransaction();
                if (do_commit) {
                    targetSession.commitTransaction();
                    // Note: Following content comparison operations are not  
                    // executed inside a long transaction, because MS SQLServer
                    // runs into deadlock if source store and target store are 
                    // inside the same database instance (in case transactions  
                    // of sourceSession and targetSession overlap).
                }
                boolean content_equals = true;
                InputStream src_stream = null; 
                InputStream tar_stream = null;
                boolean src_has_cont = sourceCont.hasContent(sourceLang);
                try {
                    src_stream = (src_has_cont || convertTransToOrig) ? sourceCont.getContentStream() : null;
                    src_has_cont = (src_stream != null);
                    tar_stream = targetCont.hasContent(targetLang) ? targetCont.getContentStream() : null;
                    content_equals = streamsAreEqual(src_stream, tar_stream);
                } finally {
                    if (src_stream != null) { 
                        try { src_stream.close(); } catch (Exception ex2) {}
                    }
                    if (tar_stream != null) { 
                        try { tar_stream.close(); } catch (Exception ex3) {}
                    }
                }
                if (! content_equals) {
                    if (!src_has_cont) {
                        targetCont.deleteContent();
                    } else {
                        src_stream = sourceCont.getContentStream();
                        try {
                            targetCont.setContentStream(src_stream);
                        } finally {
                            src_stream.close();
                        }
                    }
                }
                if (do_commit) {  // restart transaction
                    targetSession.startTransaction();
                }
            } catch (Exception ex) {
                throw new DocRuntimeException(ex);
            }
        } else {
            if (isDbTarget && targetSession.runningTransaction()) {
                // Commit changes. Required, otherwise MS SQLServer might run into deadlock.
                targetSession.commitTransaction();
                targetSession.startTransaction();
            }
        }

        // if DocFile: filename is combination of title and file-extension -> nothing has to be done
        // if DocImage: no additional fields -> nothing has to be copied
        // if DocXML: no additional fields -> nothing has to be copied

        // Reset sessions to original mode 
        sourceSession.leaveTranslationMode();
        targetSession.leaveTranslationMode();
        // if (DocConstants.DEBUG) {
        //     System.out.println("SyncNodeShallowLang finished (" + sourceLang + "/" + targetLang + ")");
        // }
    }
    
    private static void syncNodeAttributes(DocNode sourceNode, 
                                           String sourceLang, 
                                           DocNode targetNode, 
                                           String targetLang)
    {
        boolean convertTransToOrig = (targetLang == null) && (sourceLang != null);

        Map<String, String> srcAttsOrig = sourceNode.getAttributes(null);
        Map<String, String> srcAtts = (sourceLang == null) ? srcAttsOrig : sourceNode.getAttributes(sourceLang);
        Map<String, String> tarAtts = targetNode.getAttributes(targetLang);
        for (String attname : srcAttsOrig.keySet()) {
            String src_att = srcAtts.get(attname);
            String tar_att = tarAtts.get(attname);
            if (convertTransToOrig && (src_att == null)) {
                src_att = srcAttsOrig.get(attname);
            }
            if ((src_att == null) ? (tar_att != null) : !src_att.equals(tar_att)) {
                // If source differs from target, then synchronize target attribute
                targetNode.setAttribute(attname, src_att, targetLang);
            }
        }
        // If in original mode: delete attributes that no longer exist
        if (targetLang == null) {
            for (String tar_name : tarAtts.keySet()) {
                if (! srcAttsOrig.containsKey(tar_name)) { // if attribute does not exist in source node
                    targetNode.setAttribute(tar_name, null, null); // remove attribute (including translations)
                }
            }
        }
    }

    private static void syncNodeAttributes_old(DocNode sourceNode, 
                                               String sourceLang, 
                                               DocNode targetNode, 
                                               String targetLang)
    {
        boolean convertTransToOrig = (targetLang == null) && (sourceLang != null);

        String[] attNames = sourceNode.getAttributeNames();
        String[] targetNames = null;
        for (int i=0; i < attNames.length; i++) {
            String attname = attNames[i];
            String src_att = sourceNode.getAttribute(attname, sourceLang);
            String tar_att = targetNode.getAttribute(attname, targetLang);
            if (convertTransToOrig && (src_att == null)) {
                src_att = sourceNode.getAttribute(attname, null);
            }
            if ((src_att == null) ? (tar_att != null) : !src_att.equals(tar_att)) {
                // If source differs from target, then synchronize target attribute
                targetNode.setAttribute(attname, src_att, targetLang);
            } else {
                // Attribute value in source and target are equal.
                // Note:
                // For the original language the method getAttribute() returns
                // an empty string for non-existing attributes. Therefore,
                // following code is required to set empty string values
                // of attributes in the source store that do not yet exist
                // in the target store: 
                if ((targetLang == null) && (src_att != null) && src_att.equals("")) {
                    if (targetNames == null) {
                        targetNames = targetNode.getAttributeNames();
                        Arrays.sort(targetNames);
                    }
                    if (Arrays.binarySearch(targetNames, attname) < 0) { // if attribute does not exist in target node
                        targetNode.setAttribute(attname, "", null);
                    }
                }
            }
        }
        // If in original mode: delete attributes that no longer exist
        if (targetLang == null) { 
            if (targetNames == null) {
                targetNames = targetNode.getAttributeNames();
            }
            // if (targetNames.length > attNames.length) { // attributes have to be deleted
                Arrays.sort(attNames);  // sort required for binary search
                for (String tar_name : targetNames) {
                    if (Arrays.binarySearch(attNames, tar_name) < 0) { // if attribute does not exist in source node
                        targetNode.setAttribute(tar_name, null, null); // remove attribute (including translations)
                    }
                }
            // }
        }
    }

    private static void verifyVersion(DocStoreSession sourceSession,
                                      String sourceStoreId,
                                      DocStoreSession targetSession,
                                      String targetStoreId,
                                      DocVersionId verId, 
                                      CopyVersionStatistics stats,
                                      boolean extended,
                                      ProgressCallback progress)
    throws DocException
    {
        if (targetSession.getTranslationMode() != null) {
            targetSession.leaveTranslationMode();
            Log.warning("verifyVersion(): Unexpected translation mode in target session. Leaving translation mode.");
        }
        if (sourceSession.getTranslationMode() != null) {
            sourceSession.leaveTranslationMode();
            Log.warning("verifyVersion(): Unexpected translation mode in source session. Leaving translation mode.");
        }
        int steps = 1;
        String[] trans = stats.copiedTranslations;
        if (trans != null) {
            steps += trans.length;
        }
        progress.startWork(steps);
        try {
            sourceSession.openDocStore(sourceStoreId, verId);
            targetSession.openDocStore(targetStoreId, verId);
            // Verify original language
            progress.setMessage("text.copy_store_verify_version", verId.toString(), "original");
            verifyVersionLang(sourceSession, targetSession, null, extended, progress, stats);
            progress.stepFinished();
            // Verify translation languages
            for (String lang_code : trans) {
                progress.setMessage("text.copy_store_verify_version", verId.toString(), lang_code.toUpperCase());
                verifyVersionLang(sourceSession, targetSession, lang_code, extended, progress, stats);
                progress.stepFinished();
            }
        } catch (Exception ex) {
            if (! progress.getCancelFlag()) {  // if exception was not caused by user cancel
                progress.logError("text.copy_store_verify_version_failed", verId.toString(), ex.getMessage());
                stats.verifyErrors++;
            }
        } finally {
            progress.finishWork();
            closeDocStoreSilent(sourceSession, "Verify version: closing of source store failed.");
            closeDocStoreSilent(targetSession, "Verify version: closing of target store failed.");
        }
        if (progress.getCancelFlag()) {
            return;
        }
        if (stats.verifyErrors == 0) {
            progress.logInfo("text.copy_store_verify_finished_success", verId.toString());
        } else {
            progress.logInfo("text.copy_store_verify_finished_error", verId.toString(), stats.verifyErrors);
        }
    }

    private static void verifyVersionProperties(DocStoreSession sourceSession,
                                                String srcStoreId,
                                                DocStoreSession targetSession,
                                                String tarStoreId,
                                                DocVersionId verId, 
                                                CopyVersionStatistics stats,
                                                ProgressCallback progress)
    throws DocException
    {
        String [] tar_names = targetSession.getVersionPropertyNames(tarStoreId, verId);
        String [] src_names = sourceSession.getVersionPropertyNames(srcStoreId, verId);
        if (src_names.length != tar_names.length) {
            if (DocConstants.DEBUG) progress.logInfo("text.copy_store_version_property_count_differs", verId.toString());
        }
        for (String pname : tar_names) {
            String tar_value = targetSession.getVersionProperty(tarStoreId, verId, pname);
            String src_value = targetSession.getVersionProperty(srcStoreId, verId, pname);
            if ((tar_value == null) ? (src_value != null) : !tar_value.equals(src_value)) {
                if ((tar_value == null) && src_value.equals("")) {
                    if (DocConstants.DEBUG) progress.logInfo("text.copy_store_version_property_empty_string_removed", verId.toString(), pname);
                } else {
                    progress.logError("text.copy_store_version_property_differs", verId.toString(), pname);
                    stats.verifyErrors++;
                }
            }
        }
        Set<String> removed_set = new HashSet<String>(Arrays.asList(src_names));
        removed_set.removeAll(Arrays.asList(tar_names));
        for (String pn : removed_set) {
            String src_value = targetSession.getVersionProperty(srcStoreId, verId, pn);
            if ((src_value == null) || src_value.equals("")) {
                if (DocConstants.DEBUG) progress.logInfo("text.copy_store_version_property_empty_string_removed", verId.toString(), pn);
            } else {
                progress.logError("text.copy_store_version_property_removed", verId.toString(), pn);
                stats.verifyErrors++;
            }
        }
    }
    
    private static void verifyVersionLang(DocStoreSession sourceSession,
                                          DocStoreSession targetSession,
                                          String lang,
                                          boolean extended, 
                                          ProgressCallback progress, 
                                          CopyVersionStatistics stats)
    throws DocException
    {
        if (lang == null) {
            if (sourceSession.getTranslationMode() != null) {
                sourceSession.leaveTranslationMode();
            }
            if (targetSession.getTranslationMode() != null) {
                targetSession.leaveTranslationMode();
            }
        } else {
            sourceSession.enterTranslationMode(lang);
            targetSession.enterTranslationMode(lang);
        }
        try {
            String srcStoreId = sourceSession.getStoreId();
            String tarStoreId = targetSession.getStoreId();
            DocVersionId verId = targetSession.getVersionId();  // same as sourceSession.getVersionId()

            // Verify nodes recursively
            int steps = Math.max(1, stats.copiedNodes / VERIFY_PROGRESS_INCREMENT);
            progress.startWork(steps);
            try {
                verifyVersionNode(sourceSession.getRoot(), targetSession.getRoot(), extended, progress, stats, 0);
                if (stats.verifyErrors > MAX_VERIFY_ERRORS) {
                    progress.logInfo("text.copy_store_verify_exceeded_max_errors", MAX_VERIFY_ERRORS, stats.verifyErrors);
                }
            } finally {
                progress.finishWork();
            }
            
            verifyVersionProperties(sourceSession, srcStoreId, targetSession, tarStoreId, verId, stats, progress);
        } finally {
            if (lang != null) { 
                sourceSession.leaveTranslationMode();
                targetSession.leaveTranslationMode();
            }
        }
    }

    private static boolean nodesHaveSameType(DocNode sourceNode, DocNode targetNode) throws DocException
    {
        if (sourceNode instanceof DocXML) {
            return (targetNode instanceof DocXML);
        } else
        if (sourceNode instanceof DocGroup) {
            return (targetNode instanceof DocGroup);
        } else
        if (sourceNode instanceof DocImage) {
            return (targetNode instanceof DocImage);
        } else
        if (sourceNode instanceof DocFile) {
            return (targetNode instanceof DocFile);
        } else
        if (sourceNode instanceof DocReference) {
            return (targetNode instanceof DocReference);
        } else {
            throw new DocException("Unknown node type: " + sourceNode.getClass().getName());
        }
    }
    
    private static String logVal(String val) 
    {
        if (val == null) { 
            return "null";
        } else {
            return (val.length() <= 80) ? val : val.substring(0, 80) + "..."; 
        }
    }
    
    private static void verifyNodeAttribs(DocNode sourceNode,
                                          DocNode targetNode,
                                          ProgressCallback progress, 
                                          CopyVersionStatistics stats)
    throws DocException
    {
        // Verify attributes
        Map<String, String> srcAtts = sourceNode.getAttributes();
        Map<String, String> tarAtts = targetNode.getAttributes();
        for (Map.Entry<String, String> entry : tarAtts.entrySet()) {
            String attname = entry.getKey();
            String tarVal = entry.getValue();
            String srcVal = srcAtts.remove(attname);
            if ((srcVal == null) ? (tarVal != null) : !srcVal.equals(tarVal)) {
                if ((srcVal != null) && srcVal.equals("") && (tarVal == null)) {
                    progress.logInfo("text.copy_store_node_empty_attribute_removed", sourceNode.getId(), attname);
                } else {
                    if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                        progress.logError("text.copy_store_node_attributes_differ", 
                                          sourceNode.getId(), attname, 
                                          logVal(srcVal), logVal(tarVal));
                    }
                }
            }
        }
        // Check attributes that are in source node but not in target node
        // (iterate through remaining attributes in srcAtts that have not been removed in for-loop above)
        for (Map.Entry<String, String> entry : srcAtts.entrySet()) {
            String src_name = entry.getKey();
            String src_value = entry.getValue();
            if ((src_value == null) || src_value.equals("")) {
                progress.logInfo("text.copy_store_node_empty_attribute_removed", sourceNode.getId(), src_name);
            } else {
                progress.logError("text.copy_store_node_attribute_removed", sourceNode.getId(), src_name, logVal(src_value));
                stats.verifyErrors++;
            }
        }
    }
    
//    private static void verifyNodeAttribs(DocNode sourceNode,
//                                          DocNode targetNode,
//                                          ProgressCallback progress, 
//                                          CopyVersionStatistics stats)
//    throws DocException
//    {
//        // Verify attributes
//        String[] srcAtts = sourceNode.getAttributeNames();
//        String[] tarAtts = targetNode.getAttributeNames();
//        Arrays.sort(srcAtts);
//        Arrays.sort(tarAtts);
//        for (String attname : tarAtts) {
//            String srcVal = sourceNode.getAttribute(attname);
//            String tarVal = targetNode.getAttribute(attname);
//            if ((tarVal == null) ? (srcVal != null) : !tarVal.equals(srcVal)) {
//                if ((tarVal == null) && (srcVal != null) && srcVal.equals("")) {
//                    progress.logInfo("text.copy_store_node_empty_attribute_removed", sourceNode.getId(), attname);
//                } else {
//                    if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
//                        progress.logError("text.copy_store_node_attributes_differ", 
//                                          sourceNode.getId(), attname, 
//                                          logVal(srcVal), logVal(tarVal));
//                    }
//                }
//            }
//        }
//        if (! Arrays.equals(srcAtts, tarAtts)) {
//            if (DocConstants.DEBUG) {
//                progress.logInfo("text.copy_store_node_attribute_names_differ", sourceNode.getId(), 
//                                 DocmaUtil.concatStrings(srcAtts, ","), 
//                                 DocmaUtil.concatStrings(tarAtts, ","));
//            }
//            // Check attributes that are in source node but not in target node
//            for (String src_name : srcAtts) {
//                if (Arrays.binarySearch(tarAtts, src_name) < 0) {  // not in target node
//                    String src_value = sourceNode.getAttribute(src_name);
//                    if ((src_value == null) || src_value.equals("")) {
//                        progress.logInfo("text.copy_store_node_empty_attribute_removed", sourceNode.getId(), src_name);
//                    } else {
//                        progress.logError("text.copy_store_node_attribute_removed", sourceNode.getId(), src_name, logVal(src_value));
//                        stats.verifyErrors++;
//                    }
//                }
//            }
//        }
//    }
    
    private static int verifyVersionNode(DocNode sourceNode,
                                         DocNode targetNode,
                                         boolean extended, 
                                         ProgressCallback progress, 
                                         CopyVersionStatistics stats, 
                                         int node_count)
    throws DocException
    {
        checkCanceledByUser(progress);
        String srcId = sourceNode.getId();
        String tarId = targetNode.getId();
        
        // Verify node id
        if (DocConstants.DEBUG) Log.info("Verifying id of node " + srcId);
        if (!srcId.equals(tarId)) {
            if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                progress.logWarning("text.copy_store_node_ids_differ", srcId, tarId);
            }
        }
        
        // Verify that nodes have same type
        if (DocConstants.DEBUG) Log.info("Verifying node-type of node " + srcId);
        try {
            if (! nodesHaveSameType(sourceNode, targetNode)) {
                if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                    progress.logError("text.copy_store_node_types_differ", srcId,
                                      sourceNode.getClass().getName(), targetNode.getClass().getName());
                }
            }
        } catch (DocException dex) {
            if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                progress.logError("text.copy_store_unknown_node_type", srcId, sourceNode.getClass().getName());
            }
        }
        
        // Verify alias names
        if (DocConstants.DEBUG) Log.info("Verifying aliases of node " + srcId);
        String srcAl = sourceNode.getAlias();
        String tarAl = targetNode.getAlias();
        if ((srcAl == null) ? (tarAl != null) : !srcAl.equals(tarAl)) {
            if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                progress.logError("text.copy_store_node_aliases_differ", srcId, srcAl, tarAl);
            }
        }
        String[] srcAliases = sourceNode.getAliases();
        String[] tarAliases = targetNode.getAliases();
        if ((srcAliases == null) ? (tarAliases != null) : 
                                   ((tarAliases == null) || !Arrays.equals(srcAliases, tarAliases))) {
            if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                progress.logError("text.copy_store_node_aliases_differ", srcId, 
                                  DocmaUtil.concatStrings(srcAliases, ","), 
                                  DocmaUtil.concatStrings(tarAliases, ","));
            }
        }
        
        if (extended) {
            // Verify node title
            if (DocConstants.DEBUG) Log.info("Verifying title of node " + srcId);
            String srcTitle = sourceNode.getTitle();
            String tarTitle = targetNode.getTitle();
            if ((srcTitle == null) ? (tarTitle != null) : !srcTitle.equals(tarTitle)) {
                if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                    progress.logError("text.copy_store_node_titles_differ", srcId, srcTitle, tarTitle);
                }
            }

            // Verify node attributes
            if (DocConstants.DEBUG) Log.info("Verifying attributes of node " + srcId);
            verifyNodeAttribs(sourceNode, targetNode, progress, stats);
        
            // Verify content length, content type and file extension
            if ((sourceNode instanceof DocContent) && (targetNode instanceof DocContent)) {
                DocContent srcCont = (DocContent) sourceNode;
                DocContent tarCont = (DocContent) targetNode;
                if (DocConstants.DEBUG) Log.info("Verifying content-length of node " + srcId);
                long srcLen = srcCont.getContentLength();
                long tarLen = tarCont.getContentLength();
                if (srcLen != tarLen) {
                    if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                        progress.logError("text.copy_store_content_length_differs", srcId, srcLen, tarLen);
                    }
                }
                if (DocConstants.DEBUG) Log.info("Verifying content-type of node " + srcId);
                String srcType = srcCont.getContentType();
                String tarType = tarCont.getContentType();
                if ((srcType == null) ? (tarType != null) : !srcType.equals(tarType)) {
                    if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                        progress.logError("text.copy_store_content_types_differ", srcId, srcType, tarType);
                    }
                }
                if (DocConstants.DEBUG) Log.info("Verifying file-extension of node " + srcId);
                String srcExt = srcCont.getFileExtension();
                String tarExt = tarCont.getFileExtension();
                if ((srcExt == null) ? (tarExt != null) : !srcExt.equals(tarExt)) {
                    if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                        progress.logError("text.copy_store_file_ext_differ", srcId, srcExt, tarExt);
                    }
                }
            }
        }
        
        ++node_count;  // node itself has been verified
        
        if ((node_count % VERIFY_PROGRESS_INCREMENT) == 0) {
            progress.stepFinished();
        }
            
        // Recursively verify child nodes
        if ((sourceNode instanceof DocGroup) && (targetNode instanceof DocGroup)) {
            DocGroup srcGroup = (DocGroup) sourceNode;
            DocGroup tarGroup = (DocGroup) targetNode;
            DocNode[] srcChildren = srcGroup.getChildNodes();
            DocNode[] tarChildren = tarGroup.getChildNodes();
            if (srcChildren.length != tarChildren.length) {
                if (++stats.verifyErrors <= MAX_VERIFY_ERRORS) {
                    progress.logError("text.copy_store_child_count_differs", 
                                      srcId, srcChildren.length, tarChildren.length);
                }
            }
            int child_count = Math.min(srcChildren.length, tarChildren.length);
            for (int i = 0; i < child_count; i++) {
                node_count = verifyVersionNode(srcChildren[i], tarChildren[i], 
                                               extended, progress, stats, node_count);
            }
        }
        
        return node_count;  // descendant nodes and the node itself have been verified
    }


    /* ----------------  Private helper methods  --------------------- */

    private static void closeDocStoreSilent(DocStoreSession docSession, String error_msg)
    {
        try { 
            if (docSession.getStoreId() != null) docSession.closeDocStore(); 
        } catch (Exception ex) { 
            Log.warning(error_msg + " Exception: " + ex.getMessage()); 
            if (DocConstants.DEBUG) {
                ex.printStackTrace();
            }
        }
    }

    private static void get_TranslationLanguages(DocGroup group, Set langs)
    {
        langs.addAll(Arrays.asList(group.getTranslations()));
        DocNode[] children = group.getChildNodes();
        for (int i=0; i < children.length; i++) {
            if (children[i] instanceof DocGroup) {
                get_TranslationLanguages((DocGroup) children[i], langs);
            } else {
                langs.addAll(Arrays.asList(children[i].getTranslations()));
            }
        }
    }


    private static int getNodePosInArray(String node_id, DocNode[] nodes, int startIdx)
    {
        for (int i=startIdx; i < nodes.length; i++) {
            if (node_id.equals(nodes[i].getId())) return i;
        }
        return -1;
    }


    private static boolean streamsAreEqual(InputStream stream1, InputStream stream2) throws Exception
    {
        // Handle missing content (null values)
        if ((stream1 == null) || (stream2 == null)) {
            return ((stream1 == null) && (stream2 == null));
        }
        
        byte[] buf1 = new byte[128*1024];
        byte[] buf2 = new byte[128*1024];
        int cnt1 = 0, cnt2 = 0; 
        int pos1 = 0, pos2 = 0; 
        while (true) {
            int remain1 = cnt1 - pos1;
            int remain2 = cnt2 - pos2;
            int remaining = Math.min(remain1, remain2);
            for (int i=0; i < remaining; i++) {  // compare bytes in buffers
                if (buf1[pos1++] != buf2[pos2++]) {
                    return false;  // streams differ
                }
            }
            if (pos1 >= cnt1) {  // all bytes in buf1 have been processed
                do { 
                    cnt1 = stream1.read(buf1);
                } while (cnt1 == 0);
                pos1 = 0;
            }
            if (pos2 >= cnt2) {  // all bytes in buf2 have been processed
                do {
                    cnt2 = stream2.read(buf2);
                } while (cnt2 == 0);
                pos2 = 0;
            }
            boolean finished1 = (cnt1 < 0);
            boolean finished2 = (cnt2 < 0);
            if (finished1 || finished2) { // at least one stream has reached the end
                if (finished1 && finished2) {  // both streams have reached the end
                    break; // streams are equal
                }
                return false;  // streams have different length -> streams differ
            }
        }
        return true; // streams are equal
    }
    
    static class CopyVersionStatistics
    {
        String[] copiedTranslations = null;
        int copiedNodes = 0;
        int verifyErrors = 0;
    }
}
