/*
 * DocmaExportLog.java
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

import org.docma.coreapi.implementation.DefaultLog;
import java.io.*;


/**
 *
 * @author MP
 */
public class DocmaExportLog extends DefaultLog
{

    private DocmaExportLog()
    {
        super();
    }

    public DocmaExportLog(DocI18n i18n)
    {
        super(i18n);
    }

    public static DocmaExportLog loadFromXML(InputStream in) 
    throws IOException, DocException
    {
        DocmaExportLog export_log = new DocmaExportLog();
        loadFromXML(in, export_log);
        return export_log;
    }

}
