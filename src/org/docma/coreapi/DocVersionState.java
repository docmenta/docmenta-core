/*
 * DocVersionState.java
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

import org.docma.plugin.VersionState;

/**
 *
 * @author MP
 */
public class DocVersionState
{
    public static final String DRAFT = VersionState.DRAFT.toString();
    public static final String RELEASED = VersionState.RELEASED.toString();
    public static final String TRANSLATION_PENDING = VersionState.TRANSLATION_PENDING.toString();
}
