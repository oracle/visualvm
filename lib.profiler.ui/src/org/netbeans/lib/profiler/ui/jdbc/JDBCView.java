/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ResourceBundle;
import org.netbeans.lib.profiler.results.jdbc.JdbcCCTProvider;
import org.netbeans.lib.profiler.ui.results.DataView;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class JDBCView extends DataView {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.jdbc.Bundle"); // NOI18N
    
    static final String COLUMN_NAME = messages.getString("JDBCView_ColumnName"); // NOI18N
    static final String COLUMN_TOTALTIME = messages.getString("JDBCView_TimeColumnName"); // NOI18N
    static final String COLUMN_INVOCATIONS = messages.getString("JDBCView_InvocationsColumnName"); // NOI18N
    static final String COLUMN_COMMANDS = messages.getString("JDBCView_ColumnCommands"); // NOI18N
    static final String COLUMN_TABLES = messages.getString("JDBCView_ColumnTables"); // NOI18N
    static final String COLUMN_STATEMENTS = messages.getString("JDBCView_ColumnStatements"); // NOI18N
    static final String NAME_COLUMN_TOOLTIP = messages.getString("JDBCView_ColumnToolTip"); // NOI18N
    static final String TOTAL_TIME_COLUMN_TOOLTIP = messages.getString("JDBCView_TimeColumnToolTip"); // NOI18N
    static final String INVOCATIONS_COLUMN_TOOLTIP = messages.getString("JDBCView_InvocationsColumnToolTip"); // NOI18N
    static final String COMMANDS_COLUMN_TOOLTIP = messages.getString("JDBCView_ColumnCommandsToolTip"); // NOI18N
    static final String TABLES_COLUMN_TOOLTIP = messages.getString("JDBCView_ColumnTablesToolTip"); // NOI18N
    static final String STATEMENTS_COLUMN_TOOLTIP = messages.getString("JDBCView_ColumnStatementsToolTip"); // NOI18N
    
    static final String OTHER_COMMAND = messages.getString("JDBCView_OtherCommand"); // NOI18N
    
    static final String STATEMENT_REGULAR = messages.getString("JDBCView_RegularStatement"); // NOI18N
    static final String STATEMENT_PREPARED = messages.getString("JDBCView_PreparedStatement"); // NOI18N
    static final String STATEMENT_CALLABLE = messages.getString("JDBCView_CallableStatement"); // NOI18N
    
    static final String ACTION_VIEWSQLQUERY = messages.getString("JDBCView_ActionViewSqlQuery"); // NOI18N
    
    
    static final String EXPORT_TOOLTIP = messages.getString("JDBCView_ExportTooltip"); // NOI18N
    static final String EXPORT_LBL = messages.getString("JDBCView_ExportLbl"); // NOI18N
    static final String EXPORT_QUERIES = messages.getString("JDBCView_ExportQueries"); // NOI18N
    static final String ACTION_GOTOSOURCE = messages.getString("JDBCView_ActionGoToSource"); // NOI18N
    static final String ACTION_PROFILE_METHOD = messages.getString("JDBCView_ActionProfileMethod"); // NOI18N
    static final String ACTION_PROFILE_CLASS = messages.getString("JDBCView_ActionProfileClass"); // NOI18N
    static final String SEARCH_QUERIES_SCOPE = messages.getString("JDBCView_SearchQueriesScope"); // NOI18N
    static final String SEARCH_CALLERS_SCOPE = messages.getString("JDBCView_SearchCallersScope"); // NOI18N
    static final String SEARCH_SCOPE_TOOLTIP = messages.getString("JDBCView_SearchScopeTooltip"); // NOI18N
    // -----
    
    static String commandString(int command) {
        switch (command) {
            case JdbcCCTProvider.SQL_COMMAND_ALTER: return "ALTER"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_CREATE: return "CREATE"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_DELETE: return "DELETE"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_DESCRIBE: return "DESCRIBE"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_INSERT: return "INSERT"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_SELECT: return "SELECT"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_SET: return "SET"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_UPDATE: return "UPDATE"; // NOI18N
            case JdbcCCTProvider.SQL_COMMAND_BATCH: return "BATCH"; // NOI18N
            default: return OTHER_COMMAND;
        }
    }
    
}
