/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.jdbc;

import java.awt.Color;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class SQLFormatter {
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.jdbc.Bundle"); // NOI18N
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
            s.append(command, offset, m.start());
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
        s.append(command, offset, command.length());
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
