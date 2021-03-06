/*
 * LogMessage.java
 * 
 *  Copyright (C) 2014  Manfred Paula, http://www.docmenta.org
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

import org.docma.plugin.LogEntry;
import org.docma.plugin.LogLevel;

/**
 * This is the application internal interface for log messages.
 * This interface extends the <code>org.docma.plugin.LogEntry</code>
 * interface, which is visible by plug-ins.
 *
 * @author MP
 * @see org.docma.coreapi.ExportLog
 */
public interface LogMessage extends LogEntry
{
    String   getMessage();
    LogLevel getLevel();
    boolean  isError();
    boolean  isWarning();
    boolean  isInfo();
    long     getTimestamp();
    String   getGenerator();
}
