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
package org.graalvm.visualvm.lib.jfluid.results.jdbc;

import org.graalvm.visualvm.lib.jfluid.results.CCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileProvider;
import org.graalvm.visualvm.lib.jfluid.results.memory.RuntimeMemoryCCTNode;

/**
 *
 * @author Tomas Hurka
 */
public interface JdbcCCTProvider extends CCTProvider, FlatProfileProvider {
    public static final int SQL_STATEMENT_UNKNOWN = -1;
    public static final int SQL_STATEMENT = 0;
    public static final int SQL_PREPARED_STATEMENT = 1;
    public static final int SQL_CALLABLE_STATEMENT = 2;

    public static final int SQL_COMMAND_BATCH = -2;
    public static final int SQL_COMMAND_OTHER = -1;
    public static final int SQL_COMMAND_ALTER = 0;
    public static final int SQL_COMMAND_CREATE = 1;
    public static final int SQL_COMMAND_DELETE = 2;
    public static final int SQL_COMMAND_DESCRIBE = 3;
    public static final int SQL_COMMAND_INSERT = 4;
    public static final int SQL_COMMAND_SELECT = 5;
    public static final int SQL_COMMAND_SET = 6;
    public static final int SQL_COMMAND_UPDATE = 7;
        
    public static final String STATEMENT_INTERFACE = java.sql.Statement.class.getName();
    public static final String PREPARED_STATEMENT_INTERFACE = java.sql.PreparedStatement.class.getName();
    public static final String CALLABLE_STATEMENT_INTERFACE = java.sql.CallableStatement.class.getName();
    public static final String CONNECTION_INTERFACE = java.sql.Connection.class.getName();
    public static final String DRIVER_INTERFACE = java.sql.Driver.class.getName();

    public static interface Listener extends CCTProvider.Listener {
    }
    
    RuntimeMemoryCCTNode[] getStacksForSelects();
    int getCommandType(int selectId);
    int getSQLCommand(int selectId);
    String[] getTables(int selectId);
    void updateInternals();
    void beginTrans(boolean mutable);
    void endTrans();
}
