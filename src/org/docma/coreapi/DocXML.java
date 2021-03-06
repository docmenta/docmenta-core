/*
 * DocXML.java
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
 * 
 * Created on 17. Oktober 2007, 12:47
 *
 */

package org.docma.coreapi;

import org.w3c.dom.*;


/**
 *
 * @author MP
 */
public interface DocXML extends DocContent
{
    
    Document getContentDOM();
    void setContentDOM(Document xmldoc);

}
