/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.datasupport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *
 * @author Jiri Sedlacek
 */
public final class PropertiesSupport {
    
    private final File file;
    private Properties properties;
    
    
    public PropertiesSupport(File file) {
        this(file, null);
    }
    
    public PropertiesSupport(File file, Properties props) {
        this.file = file;
        if (props != null) setProperties(props);
    }
    
    
    public String getProperty(String key) {
        return getProperties(new String[] { key })[0];
    }
    
    public String[] getProperties(String[] keys) {
        String[] values = new String[keys.length];
        Properties prop = getProperties();
        for (int i = 0; i < keys.length; i++) values[i] = prop.getProperty(keys[i]);
        return values;
    }
    
    public void setProperty(String key, String value) {
        setProperties(new String[] { key }, new String[] { value });
    }
    
    public void setProperties(String[] keys, String[] values) {
        Properties prop = getProperties();
        for (int i = 0; i < keys.length; i++) prop.put(keys[i], values[i]);
        storeProperties();
    }
    
    public void setProperties(Properties props) {
        int propsCount = props.size();
        String[] keys = new String[propsCount];
        String[] values = new String[propsCount];
        
        int index = 0;
        for (String propName : props.stringPropertyNames()) {
            keys[index] = propName;
            values[index] = props.getProperty(propName);
            index++;
        }
        
        setProperties(keys, values);
    }
    
    
    private void storeProperties() {
        storeProperties(getProperties(), file);
    }
    
    private Properties getProperties() {
        if (properties == null) properties = loadProperties(file);
        if (properties == null) properties = new Properties();
        return properties;
    }
    
    private static Properties loadProperties(File file) {
        if (!file.exists() || !file.isFile()) return null;
            
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = new FileInputStream(file);
            bis = new BufferedInputStream(is);
            Properties properties = new Properties();
            properties.loadFromXML(bis);
            return properties;
        } catch (Exception e) {
            System.err.println("Error loading properties: " + e.getMessage());
            return null;
        } finally {
            try {
                if (bis != null) bis.close();
                if (is != null) is.close();
            } catch (Exception e) {
                System.err.println("Problem closing input stream: " + e.getMessage());
            }
        }
    }
    
    private static void storeProperties(Properties properties, File file) {
        OutputStream os = null;
        BufferedOutputStream bos = null;
        try {
            os = new FileOutputStream(file);
            bos = new BufferedOutputStream(os);
            properties.storeToXML(os, null);
        } catch (Exception e) {
            System.err.println("Error storing properties: " + e.getMessage());
        } finally {
            try {
                if (bos != null) bos.close();
                if (os != null) os.close();
            } catch (Exception e) {
                System.err.println("Problem closing output stream: " + e.getMessage());
            }
        }
    }

}
