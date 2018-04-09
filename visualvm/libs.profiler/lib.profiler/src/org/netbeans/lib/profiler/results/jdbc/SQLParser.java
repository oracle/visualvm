/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.jdbc;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider.*;

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
        StringBuffer pattern = new StringBuffer();
        
        for (int i =0; i < commands.length; i+=2) {
            pattern.append("(^\\b");    // NOI18N
            pattern.append(commands[i]);
            pattern.append("\\b)|");    // NOI18N
        }
        commandsPattern = Pattern.compile(pattern.substring(0, pattern.length()-1).toString(), Pattern.CASE_INSENSITIVE);
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
            
            for (int i = 0; i < tablesRefs.length; i++) {
                Matcher m = wordPattern.matcher(tablesRefs[i]);
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
