/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.memory;

import java.util.Map;
import java.util.Properties;
import org.graalvm.visualvm.sampler.SamplerParameters;

/**
 *
 * @author Jiri Sedlacek
 */
public final class MemorySamplerParameters extends SamplerParameters {
    
    static final String SAMPLING_RATE = "sampling-rate";                        // NOI18N
    
    
    private MemorySamplerParameters(String parametersS) {
        super(parametersS);
    }
    
    
    public static MemorySamplerParameters parse(String parameters) {
        return new MemorySamplerParameters(parameters);
    }

    
    @Override
    protected void parseParameters(String parametersS, Map<String, String> parameters) {
        if (parametersS.startsWith(FILE + "=")) {                               // NOI18N
            // settings defined in file
            parseParametersFile(decode(parametersS.substring(FILE.length() + 1)), parameters);
        } else {
            for (String parameter : parametersS.split(",")) {                   // NOI18N

                // sampling-rate
                int idx = parameter.indexOf(SAMPLING_RATE + "=");               // NOI18N
                if (idx == 0) parameters.put(SAMPLING_RATE, decode(parameter.substring(SAMPLING_RATE.length() + 1)));

            }
        }
    }
    
    private void parseParametersFile(String file, Map<String, String> parameters) {
        Properties properties = loadProperties(file);
        
        // sampling-rate
        String prop = properties.getProperty(SAMPLING_RATE);
        if (prop != null) parameters.put(SAMPLING_RATE, decode(prop));
    }
    
}
