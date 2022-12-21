/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.sampler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SamplerParameters {
    
    private static final Logger LOGGER = Logger.getLogger(SamplerImpl.class.getName());
    
    
    protected static final String FILE = "settings-file";                       // NOI18N
    
    
    private final Map<String, String> parameters;
    
    
    protected SamplerParameters(String parametersS) {
        if (parametersS == null || parametersS.isEmpty()) {
            parameters = null;
        } else {
            parameters = new HashMap<>();
            parseParameters(parametersS, parameters);
        }
    }
    
    
    public final String get(String key) {
        return parameters == null ? null : parameters.get(key);
    }
    
    public final boolean isEmpty() {
        return parameters == null || parameters.isEmpty();
    }
    
    
    public String toString() {
        return parameters == null ? "[no parameters]" : parameters.toString();  // NOI18N
    }
    
    
    protected abstract void parseParameters(String parametersS, Map<String, String> parameters);
    
    
    protected static Properties loadProperties(String file) {
        Properties properties = new Properties();
        
        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(file), "UTF-8")) { // NOI18N
            properties.load(isr);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read Sampler parameters", e); // NOI18N
        }
        
        return properties;
    }
    
    protected static String decode(String value) {
        value = value.replace("%27", "\'");                                     // NOI18N
        value = value.replace("%22", "\"");                                     // NOI18N
        value = value.replace("%20", " ");                                      // NOI18N
        value = value.replace("%2C", ",");                                      // NOI18N
        return value;
    }
    
}
