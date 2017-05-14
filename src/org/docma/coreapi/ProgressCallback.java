/*
 * ProgressCallback.java
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

import org.docma.plugin.LogEntry;
import org.docma.plugin.LogLevel;

/**
 *
 * @author MP
 */
public interface ProgressCallback 
{
    void     setMessage(String labelKey, Object... args);
    String   getMessageKey();
    Object[] getMessageArgs();
    
    int  getPercent();
    void setPercent(int percent);
    
    void startWork();
    void startWork(int stepsTotal);
    void finishWork();
    void setStepsTotal(int stepsTotal);
    int  getStepsTotal();
    void setStepsFinished(int stepsFinished);
    void stepFinished();
    int  getStepsFinished();
    
    void    setFinished();
    boolean isFinished();
    
    void log(LogEntry entry);
    void log(LogLevel level, String msg, Object[] args);
    void log(LogLevel level, String generator, String msg, Object[] args);
    void log(LogLevel level, String generator, String location, String msg, Object[] args);
    
    void     logError(String msg, Object... args);
    int      getErrorCount();

    void     logWarning(String msg, Object... args);
    int      getWarningCount();
    
    void     logInfo(String msg, Object... args);
    int      getInfoCount();

    void     logHeader(int headLevel, String msg, Object... args);
    void     logText(String headline, String txt);
    
    int getLogCount();
    LogEntry[] getLog();
    LogEntry[] getLog(int fromIndex, int toIndex);
    LogEntry[] getLog(boolean infos, boolean warnings, boolean errors);

    boolean getCancelFlag();
    void    setCancelFlag(boolean cancelFlag); 
}
