/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import static org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcGraphBuilder.JDBC_LOGGER;

/**
 *
 * @author Tomas Hurka
 */
class SQLStatement {

    static final Object NEW_STATEMENT = new Object();
    static final Object NEW_PREPARED_STATEMENT = new Object();
    static final Object NEW_CALLABLE_STATEMENT = new Object();

    private List batch = new ArrayList();
    private ArrayList parameters = new ArrayList();
    private String sqlTemplate;
    private final int type;

    SQLStatement(int t) {
        type = t;
    }

    SQLStatement(int t, String sql) {
        sqlTemplate = sql;
        type = t;
    }

    String invoke(String methodName, String methodSignature, List parameters) {
        String select = null;

        switch(methodName) {
            case "executeQuery":
                if (parameters.size() == 1) {
                    select = executeQuery();
                } else {
                    select = executeQuery((String) parameters.get(1));
                }
                break;
            case "executeUpdate":
                if (parameters.size() == 1) {
                    select = executeUpdate();
                } else {
                    select = executeUpdate((String) parameters.get(1));
                }
                break;
            case "execute":
                if (parameters.size() == 1) {
                    select = execute();
                } else {
                    select = execute((String) parameters.get(1));
                }
                break;
            case "addBatch":
                if (parameters.size() == 1) {
                    addBatch();
                } else {
                    addBatch((String) parameters.get(1));
                }
                break;
            case "clearBatch":
                clearBatch();
                break;
            case "executeBatch":
                select = executeBatch();
                break;
            case "setDate":
                setDate((Integer)parameters.get(1), (String)parameters.get(2));
                break;
            case "setTimestamp":
                setTimestamp((Integer)parameters.get(1), (String)parameters.get(2));
                break;
            case "setNull":
                setNull((Integer)parameters.get(1), (Integer)parameters.get(2));
                break;
            default:
                if (methodName.startsWith("set") && parameters.size()>=3 && parameters.get(1) instanceof Integer) {
                    setParameter(methodName, (Integer)parameters.get(1), parameters.get(2));
                }
        }
        return select;
    }

    String executeQuery(String sql) {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "executeQuery {0}", new Object[]{sql});
        }
        return sql;
    }

    String executeUpdate(String sql) {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "executeUpdate {0}", new Object[]{sql});
        }
        return sql;
    }

    String execute(String sql) {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "execute {0}", new Object[]{sql});
        }
        return sql;
    }

    void addBatch() {
        batch.add(getFullSql());
    }

    void addBatch(String sql) {
        batch.add(sql);
    }

    void clearBatch() {
        batch.clear();
    }

    String executeBatch() {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "executeBatch {0}", new Object[]{Arrays.toString(batch.toArray())});
        }
        return Arrays.toString(batch.toArray());
    }

    void setNull(int parameterIndex, int sqlType) {
        if (JDBC_LOGGER.isLoggable(Level.FINER)) {
            JDBC_LOGGER.log(Level.FINER, "setNull index:{0} type:{1}", new Object[]{parameterIndex,sqlType});
        }
        ensureCapacity(parameterIndex);
        parameters.set(parameterIndex, "NULL");
    }

    void setBoolean(int parameterIndex, Boolean x) {
        parameters.ensureCapacity(parameterIndex+1);
        parameters.add(x);
    }

    void setByte(int parameterIndex, byte x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setShort(int parameterIndex, short x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setInt(int parameterIndex, int x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setLong(int parameterIndex, long x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setFloat(int parameterIndex, float x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setDouble(int parameterIndex, double x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBigDecimal(int parameterIndex, BigDecimal x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setString(int parameterIndex, String x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBytes(int parameterIndex, byte[] x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setDate(int parameterIndex, String x) {
        ensureCapacity(parameterIndex);
        long time = Long.parseLong(x);
        if (JDBC_LOGGER.isLoggable(Level.FINER)) {
            JDBC_LOGGER.log(Level.FINER, "setDate index:{0} value:{1}", new Object[]{parameterIndex,new Date(time)});
        }
        parameters.set(parameterIndex, new Date(time));
    }

    void setTime(int parameterIndex, Time x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setTimestamp(int parameterIndex, String x) {
        ensureCapacity(parameterIndex);
        long time = Long.parseLong(x);
        if (JDBC_LOGGER.isLoggable(Level.FINER)) {
            JDBC_LOGGER.log(Level.FINER, "setTime index:{0} value:{1}", new Object[]{parameterIndex,new Timestamp(time)});
        }
        parameters.set(parameterIndex, new Timestamp(time));
    }

    void setAsciiStream(int parameterIndex, InputStream x, int length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setUnicodeStream(int parameterIndex, InputStream x, int length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBinaryStream(int parameterIndex, InputStream x, int length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void clearParameters() throws SQLException {
        parameters.clear();
    }

    void setObject(int parameterIndex, Object x, int targetSqlType) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setObject(int parameterIndex, Object x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setCharacterStream(int parameterIndex, Reader reader, int length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setRef(int parameterIndex, Ref x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBlob(int parameterIndex, Blob x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setClob(int parameterIndex, Clob x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setArray(int parameterIndex, Array x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setDate(int parameterIndex, Date x, Calendar cal) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setTime(int parameterIndex, Time x, Calendar cal) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNull(int parameterIndex, int sqlType, String typeName) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setURL(int parameterIndex, URL x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setRowId(int parameterIndex, RowId x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNString(int parameterIndex, String value) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNCharacterStream(int parameterIndex, Reader value, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNClob(int parameterIndex, NClob value) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setClob(int parameterIndex, Reader reader, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBlob(int parameterIndex, InputStream inputStream, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNClob(int parameterIndex, Reader reader, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setSQLXML(int parameterIndex, SQLXML xmlObject) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setAsciiStream(int parameterIndex, InputStream x, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBinaryStream(int parameterIndex, InputStream x, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setCharacterStream(int parameterIndex, Reader reader, long length) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setAsciiStream(int parameterIndex, InputStream x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBinaryStream(int parameterIndex, InputStream x) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setCharacterStream(int parameterIndex, Reader reader) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNCharacterStream(int parameterIndex, Reader value) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setClob(int parameterIndex, Reader reader) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setBlob(int parameterIndex, InputStream inputStream) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    void setNClob(int parameterIndex, Reader reader) {
        parameters.ensureCapacity(parameterIndex+1);
    }

    private void setParameter(String method, Integer parameterIndex, Object p) {
        if (JDBC_LOGGER.isLoggable(Level.FINER)) {
            JDBC_LOGGER.log(Level.FINER, "{0} index:{1} value:{2}", new Object[]{method,parameterIndex,p});
        }
        ensureCapacity(parameterIndex);
        parameters.set(parameterIndex, p);
    }

    String executeQuery() {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "executeQuery {0}", new Object[]{getFullSql()});
        }
        return getFullSql();
    }

    String executeUpdate() {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "executeUpdate {0}", new Object[]{getFullSql()});
        }
        return getFullSql();
    }

    String execute() {
        if (JDBC_LOGGER.isLoggable(Level.FINE)) {
            JDBC_LOGGER.log(Level.FINE, "execute {0}", new Object[]{getFullSql()});
        }
        return getFullSql();
    }

    private void ensureCapacity(Integer parameterIndex) {
        while(parameters.size() <= parameterIndex) {
            parameters.add(null);
        }
    }

    private String getFullSql() {
        if (sqlTemplate == null) return null;
        StringBuilder fullSql = new StringBuilder(sqlTemplate.length());
        int qindex = 0;
        int parindex = 1;
        String par;

        do {
            int lindex = qindex;
            qindex = sqlTemplate.indexOf('?', qindex);
            if (qindex == -1) {
                qindex = sqlTemplate.length();
                par = "";
            } else {
                if (parindex >= parameters.size()) {
                    par = "!!!!!UNSET";     // NOI18N
                } else {
                    par = formatParamter(parameters.get(parindex++));
                }
            }
            fullSql.append(sqlTemplate.substring(lindex, qindex));
            fullSql.append(par);
            qindex++;
        } while (qindex < sqlTemplate.length());
        return fullSql.toString();
    }

    private String formatParamter(Object par) {
        if (par instanceof String) {
            return "'"+par+"'";
        }
        if (par == null) {
            return "*NULL*";    // NOI18N
        }
        return par.toString();
    }

    @Override
    public String toString() {
        return super.toString() + ":" + sqlTemplate;    /// NOI18N
    }

    int getType() {
        return type;
    }

}
