/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.lib.profiler.ui.jdbc;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class SQLFormatter {
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.jdbc.Bundle"); // NOI18N
    private static final String DATABASE_PING = messages.getString("SQLFormatter_DatabasePing"); // NOI18N

    private static final String PING_TEXT = " - <b>"+DATABASE_PING+"</b>";  // NOI18N

    private static String keywords[] = {
        "AS",
        "ALL",
        "AND",
        "ASC",
        "AVG",
        "BY",
        "COUNT",
        "CROSS",
        "DESC",
        "DISTINCT",
        "FROM",
        "FULL",
        "GROUP",
        "HAVING",
        "INNER",
        "LEFT",
        "JOIN",
        "MAX",
        "MIN",
        "NATURAL",
        "NOT",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "RIGHT",
        "SELECT",
        "SUM",
        "WHERE",
        "CREATE TABLE",
        "ALTER TABLE",
        "TRUNCATE TABLE",
        "DROP TABLE",
        "INSERT INTO",
        "ALTER SESSION",
        "DELETE",
        "UPDATE",
        "VALUES",
        "SET",
        "'[^']*'"
    };

    private static final Pattern keywordsPattern = Pattern.compile(getPattern(keywords), Pattern.CASE_INSENSITIVE);

    private static String getPattern(String[] patterns) {
        StringBuilder pattern = new StringBuilder();

        for (String patternString : patterns) {
            pattern.append("(");    // NOI18N
            if (Character.isLetter(patternString.charAt(0))) {
                pattern.append("\\b");  // NOI18N
                pattern.append(patternString);
                pattern.append("\\b");  // NOI18N
            } else {
                pattern.append(patternString);
            }
            pattern.append(")|");   // NOI18N
        }
        return pattern.substring(0, pattern.length()-1);
    }

    private static final String pingSQL[] = {
        "^SELECT\\s+1",
        "^VALUES\\s*\\(\\s*1\\s*\\)"
    };

    private static final Pattern pingSQLPattern = Pattern.compile(getPattern(pingSQL), Pattern.CASE_INSENSITIVE);


    static String format(String command) {
        String formattedCommand;
        StringBuilder s = new StringBuilder();
        int offset = 0;
        Matcher m;
        
        command = htmlize(command);
        m = keywordsPattern.matcher(command);
        s.append("<html>"); // NOI18N
        while(m.find()) {
            String kw = m.group();
            s.append(command.substring(offset, m.start()));
            if (kw.startsWith("'")) {       // NOI18N
                // string literal
                s.append(kw);
            } else {
                s.append("<b>");    // NOI18N
                s.append(kw);
                s.append("</b>");   // NOI18N
            }
            offset = m.end();
        }
        s.append(command.substring(offset,command.length()));
        s.append(checkPingSQL(command));
        s.append("</html>"); // NOI18N

        formattedCommand = s.toString();
        formattedCommand = formattedCommand.replace(")", ")</font>");   // NOI18N
        formattedCommand = formattedCommand.replace("(", "<font color='" + getGrayHTMLString() + "'>(");   // NOI18N
        return formattedCommand;
    }

    private static String htmlize(String value) {
        return value.replace(">", "&gt;").replace("<", "&lt;");     // NOI18N
    }
    
    private static String checkPingSQL(String command) {                         
        Matcher m = pingSQLPattern.matcher(command);
        if (m.find()) {
            return PING_TEXT;
        }
        return "";
    }
    
    
    private static String grayHTMLString;
    private static String getGrayHTMLString() {
        if (grayHTMLString == null) {
            Color grayColor = UIUtils.getDisabledForeground(new JLabel().getForeground());
            grayHTMLString = "rgb(" + grayColor.getRed() + "," + grayColor.getGreen() + "," + grayColor.getBlue() + ")"; //NOI18N
        }
        return grayHTMLString;
    }
    
}
