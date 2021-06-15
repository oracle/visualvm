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

package org.graalvm.visualvm.lib.jfluid;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class serves as a wrapper for logging infrastructure
 * It should be used to log various profiler info messages
 * The logger used is identified as "org.graalvm.visualvm.lib.jfluid.infolog" and its level is automatically set to INFO
 * @author Jaroslav Bachorik
 */
public class ProfilerLogger {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger INSTANCE = Logger.getLogger("org.graalvm.visualvm.lib.jfluid.infolog"); // NOI18N
    private static final Level EXCEPTION_LEVEL = Level.SEVERE;
    private static volatile Level DEFAULT_LEVEL = Level.INFO;
    private static volatile boolean debugFlag = false;

    static {
        Level currentLevel = INSTANCE.getLevel();
        Level newLevel = currentLevel;

        if (DEFAULT_LEVEL.intValue() < EXCEPTION_LEVEL.intValue()) {
            newLevel = DEFAULT_LEVEL;
        } else {
            newLevel = EXCEPTION_LEVEL;
        }

        if ((currentLevel == null) || (newLevel.intValue() < currentLevel.intValue())) {
            INSTANCE.setLevel(newLevel);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static boolean isDebug() {
        return debugFlag;
    }

    public static void setLevel(Level level) {
        INSTANCE.setLevel(level);

        if (level.intValue() <= Level.FINEST.intValue()) {
            debugFlag = true;
        } else {
            debugFlag = false;
        }
    }

    public static Level getLevel() {
        return INSTANCE.getLevel();
    }

    public static void debug(String message) {
        INSTANCE.finest(message);
    }

    public static void info(String message) {
        INSTANCE.info(message);
    }

    public static void log(String message) {
        INSTANCE.log(DEFAULT_LEVEL, message);
    }

    public static void log(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        INSTANCE.log(EXCEPTION_LEVEL, sw.toString());
    }

    public static void severe(String message) {
        INSTANCE.severe(message);
    }

    public static void warning(String message) {
        INSTANCE.warning(message);
    }
}
