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
    
    void     logError(String labelKey, Object... args);
    int      getErrorCount();

    void     logWarning(String labelKey, Object... args);
    int      getWarningCount();
    
    void     logInfo(String labelKey, Object... args);
    int      getInfoCount();
    
    LogMessage[] getLog(boolean infos, boolean warnings, boolean errors);

    boolean getCancelFlag();
    void    setCancelFlag(boolean cancelFlag); 
}
