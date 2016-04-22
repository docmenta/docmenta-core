/*
 * Log.java
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
 * Created on 15. Oktober 2007, 17:26
 */

package org.docma.util;

import java.io.IOException;
import java.util.logging.*;

/**
 *
 * @author MP
 */
public class Log {

    private static final String LOGGER_NAME = "org.docma";
    private static Logger logger = Logger.getLogger(LOGGER_NAME);

    public static void initFileLog(Level loglevel, String filepath, int filelimit, int filecount)
    {
        try {
            Logger.getLogger(LOGGER_NAME).setLevel(loglevel);
            Handler handler = new FileHandler(filepath, filelimit, filecount);
            Logger.getLogger(LOGGER_NAME).addHandler(handler);

            logger = Logger.getLogger(LOGGER_NAME);
        } catch (IOException e) {
            Logger.getLogger(LOGGER_NAME).log(Level.SEVERE, "Cannot create Log-FileHandler", e);
        }
    }
    
    public static void setLevel(Level loglevel)
    {
        logger.setLevel(loglevel);
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void warning(String msg) {
        logger.warning(msg);
    }

    public static void error(String msg) {
        logger.severe(msg);
    }

    public static void debug(String msg) {
        logger.finest(msg);
    }
}
