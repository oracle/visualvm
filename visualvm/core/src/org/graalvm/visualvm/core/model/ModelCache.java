/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.core.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;

/**
 *
 * @author Tomas Hurka
 */
final class ModelCache<D extends DataSource, M extends Model> {

    private final static Logger LOGGER = Logger.getLogger(ModelFactory.class.getName());

    /**
     * special marker for null model
     */
    final ModelReference<M> NULL_MODEL;
    private final Map<DataSourceKey<D>, ModelReference<M>> modelCache;

    ModelCache() {
        modelCache = Collections.synchronizedMap(new HashMap());
        NULL_MODEL = new ModelReference(null, 0);
    }

    Reference<M> get(DataSourceKey<D> key) {
        ModelReference<M> valueRef = modelCache.get(key);
        if (valueRef != null && valueRef.modCount < key.modCount) {
            Reference<M> removed = modelCache.remove(key);
            LOGGER.finer("Invalid mod count " + key + " " + (removed != null ? "removed" : "not removed"));
            return null;
        }
        return valueRef;
    }

    Reference<M> put(DataSourceKey<D> key, M value) {
        ModelReference<M> ref;
        DataSource ds = key.weakReference.get();
        if (ds instanceof Stateful && value != null) {
            ds.addPropertyChangeListener(Stateful.PROPERTY_STATE, new StateListener(key));
            LOGGER.finer("Registered listener for " + key + " val " + value.getClass());
        }
        if (value == null) {
            ref = NULL_MODEL;
        } else {
            ref = new ModelReference(value, key.modCount);
        }
        return modelCache.put(key, ref);
    }

    void clear() {
        modelCache.clear();
    }

    /**
     * DataSource wrapper object, which weakly reference datasource and uses
     * reference-equality of DataSources when implementing hashCode and equals
     * this class is used as keys in modelCache
     */
    static class DataSourceKey<D extends DataSource> {

        Reference<D> weakReference;
        int modCount;

        DataSourceKey(D ds) {
            weakReference = new WeakReference(ds);
            if (ds instanceof Stateful) {
                modCount = ((Stateful) ds).getModCount();
            }
        }

        public int hashCode() {
            D ds = weakReference.get();
            if (ds != null) {
                return ds.hashCode();
            }
            return 0;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof DataSourceKey) {
                D ds = weakReference.get();
                D otherDs = ((DataSourceKey<D>) obj).weakReference.get();

                return ds != null && ds == otherDs;
            }
            throw new IllegalArgumentException(obj.getClass().getName());
        }

        public String toString() {
            DataSource ds = weakReference.get();
            return "DataSourceKey for " + System.identityHashCode(this) + " for " + ds == null ? "NULL" : ds.toString();    // NOI18N
        }
    }

    private static class ModelReference<T> extends SoftReference<T> {

        private int modCount;

        private ModelReference(T ref, int count) {
            super(ref);
            modCount = count;
        }
    }

    private class StateListener implements PropertyChangeListener {

        DataSourceKey<D> key;

        StateListener(DataSourceKey<D> k) {
            key = k;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            int newState = (Integer) evt.getNewValue();
            if (newState == Stateful.STATE_AVAILABLE) {
                DataSource ds = key.weakReference.get();
                if (ds != null) {
                    ds.removePropertyChangeListener(Stateful.PROPERTY_STATE, this);
                }
                if (ds == null || key.modCount < ((Stateful)ds).getModCount()) {
                    Reference<M> removed = modelCache.remove(key);
                    LOGGER.finer(key + " " + (removed != null ? "removed" : "not removed"));
                } else {
                    LOGGER.finer(key + " newer model found");
                }
            }
        }
    }
}
