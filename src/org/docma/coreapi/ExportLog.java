/*
 * ExportLog.java
 * 
 *  Copyright (C) 2016  Manfred Paula, http://www.docmenta.org
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
public interface ExportLog 
{

    void error(String location, String message_key, Object[] args);

    void error(String message_key, Object[] args);

    void error(String message_key);

    void errorMsg(String message);

    int getErrorCount();

    int getInfoCount();

    LogMessage[] getLog(boolean infos, boolean warnings, boolean errors);

    int getWarningCount();

    boolean hasError();

    boolean hasInfo();

    boolean hasWarning();

    void info(String location, String message_key, Object[] args);

    void info(String message_key, Object[] args);

    void info(String message_key);

    void infoMsg(String message);

    void warning(String location, String message_key, Object[] args);

    void warning(String message_key, Object[] args);

    void warning(String message_key);

    void warningMsg(String message);
    
}
