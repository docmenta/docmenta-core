/*
 * PropertiesLoader.java
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
 * Created on 13. Oktober 2007, 22:29
 *
 */

package org.docma.util;

import java.util.*;
import java.io.*;
import org.docma.coreapi.DocConstants;
import org.docma.coreapi.DocException;
import org.docma.coreapi.DocRuntimeException;


/**
 *
 * @author MP
 */
public class PropertiesLoader 
{
    protected File propfile = null;
    private Properties props = null;
    private String saveComments = "";
    private long propsModifiedTime = 0;
    // private boolean changed = false;


    protected PropertiesLoader()
    {
    }

    public PropertiesLoader(String propfilename) throws DocException 
    {
        this(new File(propfilename));
    }


    public PropertiesLoader(File propfile) throws DocException 
    {
        this.propfile = propfile;
        loadPropFile();
    }

    protected void setEmptyProps()
    {
        this.props = new Properties();  // newly created store has no properties yet
        propsModifiedTime = 0;
    }

    protected void loadPropFile() throws DocException
    {
        try {
            props = new Properties();
            // ClassLoader cl = PropertiesLoader.class.getClassLoader();
            // InputStream fin = cl.getResourceAsStream(inifilename);
            propsModifiedTime = propfile.lastModified();
            InputStream fin = new FileInputStream(propfile);
            props.load(fin);
            fin.close();
            // changed = false;
        } catch (Exception ex) {
            Log.error("Could not load properties file: " + propfile);
            throw new DocException(ex);
        }
    }


    public void savePropFile(String comments) throws DocException
    {
        // if (! changed) return;
        if (props == null) return;  // if refresh would be required, then do not save (no changes) 
        
        if (DocConstants.DEBUG) {
            Log.info("Saving properties file: " + propfile);
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(propfile);
            props.store(fout, comments);
            fout.close();
            // changed = false;
            propsModifiedTime = propfile.lastModified();
        } catch (Exception ex) {
            Log.error("Could not save properties file: " + propfile);
            if (fout != null) try { fout.close(); } catch (Exception ex2) {}
            throw new DocException(ex);
        }
    }


    public void savePropFile() throws DocException
    {
        savePropFile(saveComments);
    }


    public void setComments(String comments)
    {
        saveComments = comments;
    }


    public void discard() throws DocException
    {
        props = null; // loadPropFile();
    }

    /**
     * If the timestamp of the properties file has changed since the properties
     * have been loaded, then the property chache is cleared. 
     */
    public void refresh()
    {
        if ((props != null) && (propfile != null)) {
            if (propfile.lastModified() != propsModifiedTime) {
                props = null; // loadPropFile();
                if (DocConstants.DEBUG) {
                    Log.info("Mark properties for refresh (changed timestamp): " + propfile);
                }
            }
        }
    }

    /**
     * Get property value.
     */
    public String getProp(String pname)
    {
        checkRefresh();
        return props.getProperty(pname);
    }

    /**
     * Set property value.
     * @param pname
     * @param pvalue
     * @return 
     */
    public String setProp(String pname, String pvalue)
    {
        checkRefresh();
        // changed = true;
        if (pvalue == null) {
            return (String) props.remove(pname);
        } else {
            return (String) props.setProperty(pname, pvalue);
        }
    }

    public String[] getPropNames()
    {
        checkRefresh();
        return (String[]) props.keySet().toArray(new String[props.size()]);
    }

    private void checkRefresh()
    {
        if (props == null) {  // if refresh is required
            if ((propfile != null) && (propfile.exists())) {
                try {
                    loadPropFile();
                } catch (DocException ex) {
                    throw new DocRuntimeException(ex);
                }
            } else {
                setEmptyProps();
            }
        }
    }
    
}
