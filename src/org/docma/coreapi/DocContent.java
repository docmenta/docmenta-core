/*
 * DocContent.java
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

import org.docma.lockapi.Lock;
import java.io.*;

/**
 *
 * @author MP
 */
public interface DocContent extends DocAtom
{
    String getContentType();
    String getContentType(String lang_code);
    void setContentType(String mime_type);

    String getFileExtension();
    String getFileExtension(String lang_code);
    void setFileExtension(String file_extension);

    byte[] getContent();
    void setContent(byte[] content);

    InputStream getContentStream();
    void setContentStream(InputStream content);

    String getContentString();
    String getContentString(String charsetName);
    void setContentString(String content);
    void setContentString(String content, String charsetName);

    long getContentLength();
    void deleteContent();

    boolean hasContent(String lang_code);
    // DocAtom getTranslation(String lang);

    Lock getLock(String lockname);
    boolean setLock(String lockname, long timeout);
    boolean refreshLock(String lockname, long timeout);
    Lock removeLock(String lockname);

    HistoryEntry[] getHistory();

}
