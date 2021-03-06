/*
 * DocVersionId.java
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

/**
 *
 * @author MP
 */
public interface DocVersionId extends Comparable
{
    boolean isLowerThan(DocVersionId verId);
    boolean isHigherThan(DocVersionId verId);
    boolean equals(Object obj);
    String toString();
}
