/*
 * DocEventImpl.java
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
public class DocEventImpl implements DocEvent
{
    private String eventName;
    private String parentId;
    private Set nodeIds;
    private String lang;

    /* --------------  Constructor  ---------------------- */

    public DocEventImpl(String evtName, String parentId, String nodeId, String lang)
    {
        this.nodeIds = new HashSet();
        this.eventName = evtName;
        this.parentId = parentId;
        if (nodeId != null) {
            this.nodeIds.add(nodeId);
        }
        this.lang = lang;
    }

    /* --------------  Package local ---------------------- */

    void addNodeIds(Set node_ids) {
        this.nodeIds.addAll(node_ids);
    }

    /* --------------  Interface DocEvent ---------------------- */

    public String getEventName() {
        return eventName;
    }

    public String getParentId() {
        return parentId;
    }

    public Set getNodeIds() {
        return nodeIds;
    }

    public String getLang() {
        return lang;
    }

    /* --------------  Interface Comparable  ---------------------- */

    /*
    public int compareTo(Object obj) {
        DocEvent other = (DocEvent) obj;
        String pid = getParentId();
        String otherpid = other.getParentId();
        if (pid == null) pid = "";
        if (otherpid == null) otherpid = "";
        int i = pid.compareTo(otherpid);
        if (i == 0) {
            i = getEventName().compareTo(other.getEventName());
            if (i == 0) {
                String lg = getLang();
                String otherlg = other.getLang();
                if (lg == null) lg = "";
                if (otherlg == null) otherlg = "";
                i = lg.compareTo(otherlg);
                if (i == 0) {
                    i = getIndexFrom() - other.getIndexFrom();
                    if (i == 0) {
                        i = getIndexTo() - other.getIndexTo();
                    }
                }
            }
        }
        return i;
    }
    * /

    /* --------------  Public methods  ---------------------- */

    public String toString() {
        StringBuilder nd_ids = new StringBuilder();
        Iterator it = nodeIds.iterator();
        while (it.hasNext()) {
            Object nid = it.next();
            nd_ids.append((nid == null) ? "null" : nid.toString()).append(" ");
        }
        return "PARENT: " + getParentId() + " EVENT: " + getEventName() +
               " LANG: " + getLang() + " NODES: " + nd_ids;
    }
}
