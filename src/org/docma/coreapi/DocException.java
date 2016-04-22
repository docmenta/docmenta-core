/*
 * DocException.java
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
 * Created on 15. Oktober 2007, 18:43
 *
 */

package org.docma.coreapi;

/**
 *
 * @author MP
 */
public class DocException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>DocException</code> without detail message.
     */
    public DocException() {
    }
    
    
    /**
     * Constructs an instance of <code>DocException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public DocException(String msg) {
        super(msg);
    }
    
    
    /**
     * Constructs an instance of <code>DocException</code> with the specified cause.
     * @param cause the cause.
     */
    public DocException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an instance of <code>DocException</code> with the specified 
     * detail message and cause.
     * @param msg the detail message.
     * @param cause the cause.
     */
    public DocException(String msg, Throwable cause) {
        super(msg, cause);
    }
    

}
