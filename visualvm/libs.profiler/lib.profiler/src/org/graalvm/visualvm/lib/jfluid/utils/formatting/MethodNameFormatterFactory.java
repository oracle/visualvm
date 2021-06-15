/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.utils.formatting;

import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MethodNameFormatterFactory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MethodNameFormatterFactory instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // maps the method marks to appropriate formatters
    // @GuardedBy this
    private final Map formatterMap;
    private MethodNameFormatter defaultFormatter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MethodNameFormatterFactory
     */
    private MethodNameFormatterFactory() {
        formatterMap = new HashMap();
    }

    private MethodNameFormatterFactory(MethodNameFormatterFactory template) {
        formatterMap = template.formatterMap;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized MethodNameFormatterFactory getDefault() {
        if (instance == null) {
            instance = new MethodNameFormatterFactory();
            instance.defaultFormatter = new DefaultMethodNameFormatter();
        }

        return instance;
    }

    public static synchronized MethodNameFormatterFactory getDefault(MethodNameFormatter defaultFormatter) {
        MethodNameFormatterFactory factory = new MethodNameFormatterFactory(getDefault());
        factory.defaultFormatter = defaultFormatter;

        return factory;
    }

    public MethodNameFormatter getFormatter() {
        return defaultFormatter;
    }

    public synchronized MethodNameFormatter getFormatter(Mark mark) {
        if ((mark == null) || mark.isDefault()) {
            return defaultFormatter;
        }

        MethodNameFormatter formatter = (MethodNameFormatter) formatterMap.get(mark);

        if (formatter == null) {
            return defaultFormatter;
        }

        return formatter;
    }

    public synchronized void registerFormatter(Mark mark, MethodNameFormatter formatter) {
        formatterMap.put(mark, formatter);
    }
}
