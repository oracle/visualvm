/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;


/**
 *
 * @author Jaroslav Bachorik
 */
public class DefaultMethodNameFormatter implements MethodNameFormatter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int VERBOSITY_CLASS = 1;
    public static final int VERBOSITY_METHOD = 2;
    public static final int VERBOSITY_CLASSMETHOD = 3;
    public static final int VERBOSITY_FULLMETHOD = 4;
    public static final int VERBOSITY_FULLCLASSMETHOD = 5;
    private static final int VERBOSITY_MIN = 1;
    private static final int VERBOSITY_MAX = 5;
    private static final int VERBOSITY_DEFAULT = VERBOSITY_CLASSMETHOD;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int verbosity;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of DefaultMethodNameFormatter
     * @param verbosity verbosity level: 1 = getFormattedClass(), 2 = getFormattedMethod(), 3 = getFormattedClassAndMethod(),
     *                                   4 = getFullFormattedMethod(), 5 = getFullFormatedClassAndMethod()
     */
    public DefaultMethodNameFormatter(int verbosity) {
        this.verbosity = ((verbosity >= VERBOSITY_MIN) && (verbosity <= VERBOSITY_MAX)) ? verbosity : VERBOSITY_DEFAULT;
    }

    public DefaultMethodNameFormatter() {
        this.verbosity = VERBOSITY_DEFAULT;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Formattable formatMethodName(String className, String methodName, String signature) {
        return new PlainFormattableMethodName(className, methodName, signature, verbosity);
    }

    public Formattable formatMethodName(ClientUtils.SourceCodeSelection method) {
        return new PlainFormattableMethodName(method.getClassName(), method.getMethodName(), method.getMethodSignature(),
                                              verbosity);
    }
}
