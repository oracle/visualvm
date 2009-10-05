/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class serves as a wrapper for logging infrastructure
 * It should be used to log various profiler info messages
 * The logger used is identified as "org.netbeans.lib.profiler.infolog" and its level is automatically set to INFO
 * @author Jaroslav Bachorik
 */
public class ProfilerLogger {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger INSTANCE = Logger.getLogger("org.netbeans.lib.profiler.infolog"); // NOI18N
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
