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

/**
 *
 * @author MP
 */
public class DocmaLogMessage implements LogMessage
{
    public static final int SEVERITY_INFO = 1;
    public static final int SEVERITY_WARNING = 2;
    public static final int SEVERITY_ERROR = 3;

    private long timestamp;
    private int severity;
    private String message;

    public DocmaLogMessage(long timestamp, int severity, String message)
    {
        this.timestamp = timestamp;
        this.severity = severity;
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }

    public int getSeverity()
    {
        return severity;
    }

    public boolean isInfo()
    {
        return severity == SEVERITY_INFO;
    }

    public boolean isWarning()
    {
        return severity == SEVERITY_WARNING;
    }

    public boolean isError()
    {
        return severity == SEVERITY_ERROR;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

}
