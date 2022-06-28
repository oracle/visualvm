/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.oql;

import org.graalvm.visualvm.lib.profiler.oql.repository.api.OQLQueryDefinition;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public final class OQLQuery {

    private String script;
    private String name;
    private String description;

    public OQLQuery(OQLQueryDefinition qdef) {
        this(qdef.getContent(), qdef.getName(), qdef.getDescription());
    }

    public OQLQuery(String script, String name, String description) {
        setScript(script);
        setName(name);
        setDescription(description);
    }

    public void setScript(String script) {
        if (script == null) {
            throw new IllegalArgumentException("Script cannot be null"); // NOI18N
        }
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setName(String name) {
        this.name = normalizeString(name);
        if (this.name == null) {
            throw new IllegalArgumentException("Name cannot be null"); // NOI18N
        }
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = normalizeString(description);
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return name;
    }

    private static String normalizeString(String string) {
        String normalizedString = null;
        if (string != null) {
            normalizedString = string.trim();
            if (normalizedString.isEmpty()) {
                normalizedString = null;
            }
        }
        return normalizedString;
    }
}
