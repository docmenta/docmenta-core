/*
 * PublicationArchive.java
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

/**
 *
 * @author MP
 */
public interface PublicationArchive
{
    final String ATTRIBUTE_PUBLICATION_FILENAME = "publication.filename";
    final String ATTRIBUTE_PUBLICATION_LANGUAGE = "publication.language";
    final String ATTRIBUTE_PUBLICATION_CREATION_TIME = "publication.creation.time";
    final String ATTRIBUTE_PUBLICATION_CLOSING_TIME = "publication.closing.time";

    String getDocStoreId();
    DocVersionId getVersionId();

    String createPublication(String publicationId, String language, String filename);
    String createPublication(String language, String filename);
    void deletePublication(String publicationId);
    String[] listPublications();

    String getAttribute(String publicationId, String attName);
    String[] getAttributeNames(String publicationId);
    void setAttribute(String publicationId, String attName, String attValue);
    void setAttributes(String publicationId, String[] attNames, String[] attValues);

    InputStream readPublicationStream(String publicationId);
    OutputStream openPublicationOutputStream(String publicationId);
    void closePublicationOutputStream(String publicationId);
    boolean hasPublicationStream(String publicationId);
    long getPublicationSize(String publicationId);
    // void setPublicationStream(String publicationId, InputStream pubStream, DocmaExportLog log);

    DocmaExportLog readExportLog(String publicationId);
    void writeExportLog(String publicationId, DocmaExportLog log);
    boolean hasExportLog(String publicationId);

    void refresh(String publicationId);
}
