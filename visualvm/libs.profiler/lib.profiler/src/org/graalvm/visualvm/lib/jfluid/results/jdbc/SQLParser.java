/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.jdbc;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcCCTProvider.*;

/**
 *
 * @author Tomas Hurka
 */
class SQLParser {

    Object[] commands = {
        "ALTER", SQL_COMMAND_ALTER,     // NOI18N
        "CREATE", SQL_COMMAND_CREATE,   // NOI18N
        "DELETE", SQL_COMMAND_DELETE,   // NOI18N
        "DESCRIBE", SQL_COMMAND_DESCRIBE,   // NOI18N
        "INSERT", SQL_COMMAND_INSERT,   // NOI18N
        "SELECT", SQL_COMMAND_SELECT,   // NOI18N
        "SET", SQL_COMMAND_SET,         // NOI18N
        "UPDATE", SQL_COMMAND_UPDATE    // NOI18N
    };
    private static final String fromRegexp = "(^\\bSELECT\\b)|" +
            "(\\bFROM\\b)|" +
            "(\\bWHERE\\b)|" +
            "(\\bGROUP\\sBY\\b)|" +
            "(\\bORDER\\sBY\\b)|" +
            "(^\\bUPDATE\\b)|" +
            "(^\\bINSERT INTO\\b)|" +
            "('[^']*')";
    private static final String wordRegexp = "\\b\\w+\\.?\\w*\\b";

    private final Pattern commandsPattern;
    private final Pattern fromPattern;
    private final Pattern wordPattern;
    private final StringCache strings;

    SQLParser() {
        StringBuilder pattern = new StringBuilder();

        for (int i =0; i < commands.length; i+=2) {
            pattern.append("(^\\b");    // NOI18N
            pattern.append(commands[i]);
            pattern.append("\\b)|");    // NOI18N
        }
        commandsPattern = Pattern.compile(pattern.substring(0, pattern.length()-1), Pattern.CASE_INSENSITIVE);
        fromPattern = Pattern.compile(fromRegexp, Pattern.CASE_INSENSITIVE);
        wordPattern = Pattern.compile(wordRegexp, Pattern.CASE_INSENSITIVE);
        strings = new StringCache();
    }

    int extractSQLCommandType(String sql) {
        if (sql != null && sql.startsWith("[")) {
            return SQL_COMMAND_BATCH;
        }
        Matcher m = commandsPattern.matcher(sql);
        
        if (m.find()) {
            for (int i=0; i < commands.length; i+=2) {
                if (m.start(i/2+1) != -1) {
                    return ((Integer)commands[i+1]).intValue();
                }
            }
            throw new IllegalArgumentException(m.toString());
        }
        return SQL_COMMAND_OTHER;
    }
    
    String[] extractTables(String sql) {
        String fromClause = extractFromClause(sql);
        
        if (fromClause != null) {
            String[] tablesRefs = fromClause.trim().split(",");
            Set<String> tables = new HashSet(tablesRefs.length);
            
            for (String tablesRef : tablesRefs) {
                Matcher m = wordPattern.matcher(tablesRef);
                if (m.find()) {
                    tables.add(strings.intern(m.group()));
                }
            }
            return tables.toArray(new String[0]);
        }
        return new String[0];
    }
    
    
    String extractFromClause(String sql) {
        Matcher m = fromPattern.matcher(sql);
        if (m.find()) {
            if (m.start(1) != -1) { // SELECT 
                int fromStart = -1;
                int fromEnd = -1;
                while (m.find()) {
                    if (m.end(2) != -1) { // FROM
                        fromStart = m.end(2);
                    } else if (m.start(3) != -1) {    // WHERE
                        fromEnd = m.start(3);
                        break;
                    } else if (m.start(4) != -1) {    // GROUP BY
                        fromEnd = m.start(4);
                        break;
                    } else if (m.start(5) != -1) {    // ORDER BY
                        fromEnd = m.start(5);
                        break;
                    }
                }
                if (fromStart < fromEnd) {
                    return sql.substring(fromStart+1, fromEnd);
                } else if (fromStart != -1 && fromEnd == -1) { // just FROM without WHERE
                    return sql.substring(fromStart+1);
                }
            } else if (m.start(6) != -1) {        // UPDATE
                Matcher mw = wordPattern.matcher(sql.substring(m.end(6)+1));
                if (mw.find()) {
                    return mw.group();
                }
            } else if (m.start(7) != -1) {        // INSERT INTO
                Matcher mw = wordPattern.matcher(sql.substring(m.end(7)+1));
                if (mw.find()) {
                    return mw.group();
                }
            }
        }
        return null;
    }
    
}
