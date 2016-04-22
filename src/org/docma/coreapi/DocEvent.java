/*
 * DocEvent.java
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

import java.util.Set;

/**
 *
 * @author MP
 */
public interface DocEvent
{
    final String NODES_ADDED = "nodesAdded";
    final String NODES_REMOVED = "nodesRemoved";
    final String NODES_CHANGED = "nodesChanged";
    final String NODES_STRUCTURE_CHANGED = "nodesStructureChanged";   // required to fix ZK tree rendering bug

    String   getEventName();
    String   getParentId();
    Set      getNodeIds();
    String   getLang();

}
