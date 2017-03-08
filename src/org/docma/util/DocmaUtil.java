/*
 * DocmaUtil.java
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
import java.util.*;
import org.docma.coreapi.*;

/**
 *
 * @author MP
 */
public class DocmaUtil
{

    public static boolean recursiveFileDelete(File f)
    {
        if (! f.isAbsolute()) {
            throw new DocRuntimeException("Recursive file deletion is not allowed for relative path names: " + f);
        }
        boolean del_okay = true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            for (int i=0; i < children.length; i++) {
                if (! recursiveFileDelete(children[i])) del_okay = false;
            }
        }
        if (! f.delete()) del_okay = false;

        return del_okay;
    }

    public static boolean recursiveFileCopy(File sourceDir, File destDir, boolean overwrite)
    {
        if (sourceDir.isDirectory()) {
            if (! destDir.exists()) {
                destDir.mkdirs();
            }
            String[] fnames = sourceDir.list();
            for (int i=0; i < fnames.length; i++) {
                File sourceChild = new File(sourceDir, fnames[i]);
                File destChild = new File(destDir, fnames[i]);
                if (! recursiveFileCopy(sourceChild, destChild, overwrite)) {
                    return false;
                }
            }
            return true;
        } else {
            return fileCopy(sourceDir, destDir, overwrite);
        }
    }

    public static boolean fileCopy(File sourceFile, File destFile, boolean overwrite)
    {
        if (destFile.exists()) {
            if (overwrite) {
                if (! destFile.delete()) return false;
            } else {
                return false;
            }
        }
        try {
            FileInputStream fin = new FileInputStream(sourceFile);
            FileOutputStream fout = new FileOutputStream(destFile);
            byte[] buf = new byte[4096];
            int cnt;
            while((cnt = fin.read(buf)) >= 0) {
                fout.write(buf, 0, cnt);
            }
            fin.close();
            fout.close();
            return true;
        } catch(Exception ex) {
            throw new DocRuntimeException(ex);
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException
    {
        byte[] buf = new byte[64*1024];
        int cnt;
        while ((cnt = in.read(buf)) >= 0) {
            if (cnt > 0) out.write(buf, 0, cnt);
        }
        // in.close();
    }

    public static String concatStrings(List str_list, String sep)
    {
        StringBuilder buf = new StringBuilder();
        if (str_list.size() > 0) {
            buf.append(str_list.get(0));
            for (int i=1; i < str_list.size(); i++) {
                buf.append(sep).append(str_list.get(i));
            }
        }
        return buf.toString();
    }

    public static String concatStrings(String[] str_arr, String sep)
    {
        StringBuilder buf = new StringBuilder();
        if (str_arr.length > 0) {
            buf.append(str_arr[0]);
            for (int i=1; i < str_arr.length; i++) {
                buf.append(sep).append(str_arr[i]);
            }
        }
        return buf.toString();
    }

    public static String formatByteSize(long size)
    {
        if (size < 100000) return (size + " Bytes");
        else if (size < 100000*1000) return (size / 1000) + " KB";
        else return (size / 1000000) + " MB";
    }

    public static String readStreamToString(InputStream in, String encoding) throws IOException
    {
        InputStreamReader reader = new InputStreamReader(in, encoding);
        StringBuilder outbuf = new StringBuilder();
        char[] buf = new char[16 * 1024];
        int cnt;
        while ((cnt = reader.read(buf)) >= 0) {
            outbuf.append(buf, 0, cnt);
        }
        return outbuf.toString();
    }

    public static byte[] readStreamToByteArray(InputStream in) throws IOException
    {
        final int min_sz = 32*1024;
        int sz = Math.max(in.available() + min_sz, min_sz);  // just in case available() returns negative value (for some unknown reason)
        ByteArrayOutputStream bout = new ByteArrayOutputStream(sz);
        copyStream(in, bout);
        return bout.toByteArray();
    }
    
    public static String readFileToString(File f) throws IOException
    {
        FileInputStream fin = new FileInputStream(f);
        String s = readStreamToString(fin, "UTF-8");
        try { fin.close(); } catch (Exception ex) {}
        return s;
        // StringBuilder buf = new StringBuilder();
        // BufferedReader in = new BufferedReader(new FileReader(f));
        // String line;
        // while ((line = in.readLine()) != null) {
        //     buf.append(line);
        // }
        // return buf.toString();
    }

    public static String readFilesToString(File[] files, String sep) throws IOException
    {
        if ((files == null) || (files.length == 0)) {
            return "";
        } else if (files.length == 1) {
            return DocmaUtil.readFileToString(files[0]);
        } else {
            StringBuilder sb = new StringBuilder(DocmaUtil.readFileToString(files[0]));
            for (int i = 1; i < files.length; i++) {
                if (sep != null) { 
                    sb.append(sep);
                }
                sb.append(DocmaUtil.readFileToString(files[i]));
            }
            return sb.toString();
        }
    }
    
    public static void writeStringToFile(String str, File fileout, String charsetName)
    throws IOException
    {
        FileOutputStream fout = new FileOutputStream(fileout);
        try {
            Writer w = new OutputStreamWriter(fout, charsetName);
            w.write(str);
            w.close();
        } finally {
            try { fout.close(); } catch (Exception ex) {}
        }
    }

    public static void writeStreamToFile(InputStream in, File fileout)
    throws IOException
    {
        FileOutputStream fout = new FileOutputStream(fileout, false);
        try {
            byte[] buf = new byte[64 * 1024];
            int cnt;
            while ((cnt = in.read(buf)) >= 0) {
                fout.write(buf, 0, cnt);
            }
        } finally {
            try { fout.close(); } catch (Exception ex) {}
        }
    }
    
    public static void checkWellFormedXML(String xml) throws Exception
    {
        int startpos = xml.indexOf('<');
        if (startpos < 0) { 
            xml = xml.trim();
            if (! xml.equals("")) {
                // non-whitespace string without tags is not considered well-formed
                throw new Exception("String does not contain well-formed XML: " + extractStringStart(xml, 60));
            } else {
                return; // string that contains only whitespace is considered well-formed
            }
        }
        String before = xml.substring(0, startpos).trim();
        if (! before.equals("")) {
            throw new Exception("String does not start with XML tag: " + extractStringStart(xml, 60));
        }
        XMLParser parser = new XMLParser(xml, startpos);
        int res;
        do {
            res = parser.next();
        } while (res != XMLParser.FINISHED);
        int endpos = parser.getEndOffset();  // position after the last xml element
        if (endpos < xml.length()) {
            String after = xml.substring(endpos).trim();
            if (! after.equals("")) {
                throw new Exception("String is not well-formed XML: " + extractStringStart(after, 60));
            }
        }
    }
    
    public static String extractStringStart(String str, int maxlen) 
    {
        if (str.length() <= maxlen) {
            return str;
        } else {
            return str.substring(0, maxlen) + "...";
        }
    }
    
    public static boolean checkDirReadWriteDeleteAccess(File dir) 
    {
        File test_file = null;
        boolean check_result;
        try {
            String temp_name = "testaccess" + System.currentTimeMillis() + ".tmp";
            test_file = new File(dir, temp_name);
            final String TEST_STRING = "write succeeded";
            writeStringToFile(TEST_STRING, test_file, "UTF-8");
            check_result = TEST_STRING.equals(readFileToString(test_file));
        } catch (Throwable ex) {
            check_result = false;
        }
        // check_result is true if read/write was successful
        if (test_file != null) {
            try {
                if (! test_file.delete()) {
                    check_result = false;   // could not delete file
                }
            } catch (Throwable ex2) {
                check_result = false;
            }
        }
        return check_result;
    }
    
}
