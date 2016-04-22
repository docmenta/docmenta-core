/*
 * DefaultProgressCallback.java
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
package org.docma.coreapi.implementation;

import java.util.Arrays;
import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class DefaultProgressCallback implements ProgressCallback
{
    private String messageKey = null;
    private Object[] messageArgs = new Object[0];
    private int percent = 0;
    private int workLevel = -1;
    
    private int[] stepsTotal =    { 0, 0, 0, 0, 0 }; // steps total for up to 5 nested levels
    private int[] stepsFinished = { 0, 0, 0, 0, 0 }; // steps finished for up to 5 nested levels
    
    boolean finishedFlag = false;
    private boolean cancelFlag = false;
    
    protected DefaultLog log;


    public DefaultProgressCallback(DocI18n i18n)
    {
        log = new DefaultLog(i18n);
    }

    public String getMessageKey() 
    {
        return messageKey;
    }

    public Object[] getMessageArgs() 
    {
        if (messageArgs == null) {
            messageArgs = new Object[0];
        }
        return messageArgs;
    }

    public void setMessage(String labelKey, Object... args) 
    {
        setMessage(true, labelKey, args);
    }
    
    protected void setMessage(boolean addToLog, String labelKey, Object... args) 
    {
        messageKey = labelKey;
        messageArgs = args;
        if (addToLog) {
            logInfo(labelKey, args);
        }
    }

    public int getPercent() 
    {
        return percent;
    }

    public void setPercent(int percent) 
    {
        this.percent = percent;
    }

    public void startWork() 
    {
        startWork(1);
    }
    
    public void startWork(int steps_total) 
    {
        if (steps_total <= 0) {
            steps_total = 1;
        }
        
        ++workLevel;
        fitArraySize();
        
        final int max_levels = this.stepsTotal.length;
        // if (workLevel < max_levels) {
        this.stepsTotal[workLevel] = steps_total;
        this.stepsFinished[workLevel] = 0;
        for (int i = workLevel + 1; i < max_levels; i++) {
            this.stepsTotal[i] = 0;
            this.stepsFinished[i] = 0;
        }
        // }
        if (workLevel == 0) {
            this.percent = 0;
        }
    }
    
    public void finishWork()
    {
        if (workLevel >= 0) { 
            stepsFinished[workLevel] = stepsTotal[workLevel];
            calcPercentFromSteps();
            --workLevel;
        }
    }

    public int getStepsTotal() 
    {
        if (workLevel < 0) { 
            return 0;
        } else {
           return stepsTotal[workLevel];
        }
    }

    public void setStepsTotal(int total_count) 
    {
        if (total_count <= 0) {
            total_count = 1;
        }
        if (workLevel >= 0) {
            this.stepsTotal[workLevel] = total_count;
            if (this.stepsFinished[workLevel] > total_count) {
                this.stepsFinished[workLevel] = total_count;
            }
            calcPercentFromSteps();
        }
    }

    public int getStepsFinished() 
    {
        if (workLevel < 0) {
            return 0;
        } else {
            return this.stepsFinished[workLevel];
        }
    }

    public void setStepsFinished(int finishedCount) 
    {
        if (workLevel < 0) {
            return;  // ignore
        }
        set_steps_finished(finishedCount);
    }
        
    public void stepFinished() 
    {
        if (workLevel < 0) {
            return;  // ignore
        }
        set_steps_finished(stepsFinished[workLevel] + 1);
    }

    private void set_steps_finished(int finished_count) 
    {
        if (finished_count < 0) {
            finished_count = 0;
        } else
        if (finished_count > stepsTotal[workLevel]) {
            finished_count = stepsTotal[workLevel];
        }
        this.stepsFinished[workLevel] = finished_count;
        calcPercentFromSteps();
    }

    public void setFinished() 
    {
        percent = 100;
        stepsFinished[0] = stepsTotal[0];
        workLevel = -1;   // finish all open levels
        
        finishedFlag = true;
    }
    
    public boolean isFinished() 
    {
        return finishedFlag;
    }

    public boolean getCancelFlag() 
    {
        return this.cancelFlag;
    }
    
    public void setCancelFlag(boolean flag) 
    {
        this.cancelFlag = flag;
    }

    public void logError(String labelKey, Object... labelArgs) 
    {
        log.error(labelKey, labelArgs);
    }

    public int getErrorCount() 
    {
        return log.getErrorCount();
    }

    public void logWarning(String labelKey, Object... labelArgs) 
    {
        log.warning(labelKey, labelArgs);
    }

    public int getWarningCount() 
    {
        return log.getWarningCount();
    }

    public void logInfo(String labelKey, Object... labelArgs) 
    {
        log.info(labelKey, labelArgs);
    }

    public int getInfoCount() 
    {
        return log.getInfoCount();
    }

    public LogMessage[] getLog(boolean infos, boolean warnings, boolean errors)
    {
        return log.getLog(infos, warnings, errors);
    }

    /**
     * Calculate progress in percent from finished steps up to first 3 levels.
     * @return 
     */
    private void calcPercentFromSteps()
    {
        float level0Delta = (stepsTotal[0] > 0) ? (100 / stepsTotal[0]) : 0;
        float res = stepsFinished[0] * level0Delta;
        if (workLevel >= 1) {
            if (stepsTotal[1] > 0) {
                float level1Delta = level0Delta / stepsTotal[1];
                res += stepsFinished[1] * level1Delta;
                if (workLevel >= 2) {
                    if (stepsTotal[2] > 0) {
                        float level2Delta = level1Delta / stepsTotal[2];
                        res += stepsFinished[2] * level2Delta;
                        if (workLevel >= 3) {
                            if (stepsTotal[3] > 0) {
                                float level3Delta = level2Delta / stepsTotal[3];
                                res += stepsFinished[3] * level3Delta;
                            }
                        }
                    }
                }
            }
        }
        this.percent = Math.round(res);
        // System.out.println("WorkLevel: " + workLevel);
        // System.out.println("WorkLevel Finished: " + stepsFinished[workLevel]);
        // System.out.println("Progress: " + res);
        // System.out.println("Progress percent: " + this.percent);
    }

    private void fitArraySize()
    {
        if (workLevel >= this.stepsTotal.length) {
            final int new_length = workLevel + 1;
            this.stepsTotal = Arrays.copyOf(this.stepsTotal, new_length);
            this.stepsFinished = Arrays.copyOf(this.stepsFinished, new_length);
        }
    }

}
