/*
 * ZipUtil.java
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

package org.docma.util;

import java.io.*;
import java.util.zip.*;

/**
 *
 * @author MP
 */
public class ZipUtil
{

    public static void extractZipStream(InputStream in, File extractDir) throws Exception
    {
        ZipInputStream zip_in;
        boolean wrapped = false;
        if (in instanceof ZipInputStream) {
            zip_in = (ZipInputStream) in;
        } else {
            zip_in = new ZipInputStream(in);
            wrapped = true;
        }

        ZipEntry entry;
        while ((entry = zip_in.getNextEntry()) != null) {
            String entry_name = entry.getName();
            File out_file = new File(extractDir, entry_name);
            if (entry.isDirectory()) {
                if (! out_file.exists()) out_file.mkdirs();
            } else {
                // BufferedInputStream buf_in = new BufferedInputStream(zip_in);
                File parentDir = out_file.getParentFile();
                if (! parentDir.exists()) parentDir.mkdirs();
                FileOutputStream out = new FileOutputStream(out_file);
                DocmaUtil.copyStream(zip_in, out);
                try { out.close(); } catch (Exception ex) {}  // ignore close exception
            }
            zip_in.closeEntry();
        }

        if (wrapped) {
            zip_in.close();
        }
    }

    public static void addDirectoryToZip(ZipOutputStream zipout, File dir) throws IOException
    {
        addDirectoryToZip(zipout, dir, "");
    }

    public static void addDirectoryToZip(ZipOutputStream zipout, File dir, String path) throws IOException
    {
        if (path.length() > 0) {
            if (!path.endsWith("/")) {
                path += "/";
            }
            // addDirectoryEntry(zipout, path);
        }
        File[] childs = dir.listFiles();
        for (int i=0; i < childs.length; i++) {
            addZipEntry(zipout, childs[i], path);
        }
    }
    
    public static void addFileToZip(ZipOutputStream zipout, File in_file, String entry_path) throws IOException
    {
        FileInputStream fin = new FileInputStream(in_file);
        try {
            addStreamToZip(zipout, fin, entry_path);
        } finally {
            try { fin.close(); } catch (Exception ex) {}
        }
    }
    
    public static void addStreamToZip(ZipOutputStream zipout, InputStream in_stream, String entry_path) throws IOException
    {
        ZipEntry ze = new ZipEntry(entry_path);
        zipout.putNextEntry(ze);
        DocmaUtil.copyStream(in_stream, zipout);
        zipout.closeEntry();
    }

    private static void addZipEntry(ZipOutputStream zipout, File node, String path)
    throws IOException
    {
        if (node.isDirectory()) {
            String dirName = node.getName();
            // foldname = foldname.replace('/', '_').replace('\\', '_').trim();
            String newpath = path + dirName + "/";
            // addDirectoryEntry(zipout, newpath);
            File[] childs = node.listFiles();
            for (int i=0; i < childs.length; i++) {
                addZipEntry(zipout, childs[i], newpath);
            }
        } else
        if (node.isFile()) {
            String nodefn = node.getName();
            // File f = new File(path, nodefn);
            String filename = path + nodefn; // f.getPath();
            ZipEntry ze = new ZipEntry(filename);
            zipout.putNextEntry(ze);
            // Daten an zipout senden
            InputStream in = new FileInputStream(node);
            DocmaUtil.copyStream(in, zipout);
            in.close();
            zipout.closeEntry();
        }
    }

    private static void addDirectoryEntry(ZipOutputStream zipout, String path)
    throws IOException
    {
        ZipEntry ze = new ZipEntry(path);
        zipout.putNextEntry(ze);
        zipout.closeEntry();
    }
}
