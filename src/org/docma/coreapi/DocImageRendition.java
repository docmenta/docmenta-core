/*
 * DocImageRendition.java
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
public class DocImageRendition
{
    public static final String FORMAT_JPEG = "jpeg";
    public static final String FORMAT_GIF = "gif";
    public static final String FORMAT_BMP = "bmp";
    public static final String FORMAT_PNG = "png";
    public static final String FORMAT_TIFF = "tiff";


    private String name;
    private String format;
    private int maxWidth;
    private int maxHeight;

    public DocImageRendition(String name, String format, int maxWidth, int maxHeight)
    throws DocException
    {
        this.name = name;
        if (! name.matches("[A-Za-z][0-9A-Za-z_-]+")) {
            throw new DocException("Invalid image rendition name.");
        }
        this.format = format;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public String getFormat()
    {
        return format;
    }

    public int getMaxHeight()
    {
        return maxHeight;
    }

    public int getMaxWidth()
    {
        return maxWidth;
    }

    public String getName()
    {
        return name;
    }

    public static String getMIMETypeFromFormat(String format) throws DocException
    {
        return "image/" + format;
    }

    public static String getFormatFromMIMEType(String mime_type) throws DocException
    {
        if (! mime_type.startsWith("image/")) {
            throw new DocException("Invalid image MIME-type: " + mime_type);
        }
        return mime_type.substring("image/".length());
    }
}
