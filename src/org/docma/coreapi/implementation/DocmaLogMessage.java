/*
 * DocmaLogMessage.java
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
import org.docma.plugin.LogLevel;

/**
 *
 * @author MP
 */
public class DocmaLogMessage implements LogMessage
{
    private final long timestamp;
    private final LogLevel level;
    private final String message;
    private final String generator;

    public DocmaLogMessage(long timestamp, LogLevel level, String message)
    {
        this(timestamp, level, message, null);
    }
    
    public DocmaLogMessage(long timestamp, LogLevel level, String message, String generator)
    {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.generator = generator;
    }

    public String getMessage()
    {
        return message;
    }

    public LogLevel getLevel()
    {
        return level;
    }

    public boolean isInfo()
    {
        return level == LogLevel.INFO;
    }

    public boolean isWarning()
    {
        return level == LogLevel.WARNING;
    }

    public boolean isError()
    {
        return level == LogLevel.ERROR;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String getGenerator()
    {
        return generator;
    }
}
