/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
