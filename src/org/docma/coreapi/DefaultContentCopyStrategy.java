/*
 * DefaultContentCopyStrategy.java
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

import java.io.*;
import org.docma.util.Log;

/**
 *
 * @author MP
 */
public class DefaultContentCopyStrategy implements ContentCopyStrategy
{
    private static DefaultContentCopyStrategy instance = new DefaultContentCopyStrategy();

    public void copyContent(DocContent sourceContent,
                            DocStoreSession sourceSession,
                            DocContent targetContent, 
                            DocStoreSession targetSession) throws DocException
    {
        InputStream in = sourceContent.getContentStream();
        if (in != null) {
            targetContent.setContentStream(in);
            try {
                in.close();
            } catch (Exception ex) {
                Log.warning("Could not close content stream in copyContent(): " + ex.getMessage());
            }
        }
    }


    public static DefaultContentCopyStrategy getInstance()
    {
        return instance;
    }
}
