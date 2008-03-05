/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.model;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.ClassNameComparator;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataChangeSupport;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Tomas Hurka
 */
public abstract class ModelFactory<M extends Model,D extends DataSource> {
    
    private SortedSet<ModelProvider<M, D>> factories = new TreeSet(new ModelProviderComparator());
    private Map<DataSourceKey<D>,Reference<M>> modelMap = new HashMap();
    private DataChangeSupport<ModelProvider<M, D>> factoryChange = new DataChangeSupport();
    
    public final synchronized M getModel(D dataSource) {
        DataSourceKey<D> key = new DataSourceKey(dataSource);
        Reference<M> modelRef = modelMap.get(key);
        M model = null;
        
        if (modelRef != null) {
            model = modelRef.get();
            if (model != null) {
                return model;
            }
        }
        for (ModelProvider<M, D> factory : factories) {
            model = factory.createModelFor(dataSource);
            if (model != null) {
                modelMap.put(key,new SoftReference(model));
                break;
            }
        }
        return model;
    }
    
    public final synchronized boolean registerFactory(ModelProvider<M, D> newFactory) {
        //        System.out.println("Registering Class "+newFactory.getClass().getName());
        //        Class superClass = newFactory.getClass();
        //        ParameterizedType type = null;
        //        while(!superClass.equals(Object.class)) {
        //            Type genType =  superClass.getGenericSuperclass();
        //            if (genType instanceof ParameterizedType) {
        //                type = (ParameterizedType) genType;
        //                break;
        //            } else if (genType instanceof Class) {
        //                superClass = (Class) genType;
        //            }
        //        }
        //        if (type != null) {
        //            Type[] types = type.getActualTypeArguments();
        //            for (int i = 0; i < types.length; i++) {
        //                System.out.println("Type "+types[i]);
        //            }
        //        }
        boolean added = factories.add(newFactory);
        if (added) {
            clearCache();
            factoryChange.fireChange(factories,Collections.singleton(newFactory),null);
        }
        return added;
    }
    
    public final synchronized boolean unregisterFactory(ModelProvider<M, D> oldFactory) {
        boolean removed = factories.remove(oldFactory);
        if (removed) {
            clearCache();
            factoryChange.fireChange(factories,null,Collections.singleton(oldFactory));
        }
        return removed;
    }
    
    public void addFactoryChangeListener(DataChangeListener<ModelProvider<M, D>> listener) {
        factoryChange.addChangeListener(listener);
    }
    
    public void removeFactoryChangeListener(DataChangeListener<ModelProvider<M, D>> listener) {
        factoryChange.removeChangeListener(listener);
    }
    
    public int depth() {
        return -1;
    }
    
    private void clearCache() {
        if (!modelMap.isEmpty()) modelMap = new HashMap();
    }
    
    private class ModelProviderComparator implements Comparator<ModelProvider<M,D>> {
        
        public int compare(ModelProvider<M, D> factory1, ModelProvider<M, D> factory2) {
            int thisVal = factory1.depth();
            int anotherVal = factory2.depth();
            
            if (thisVal<anotherVal) {
                return 1;
            }
            if (thisVal>anotherVal) {
                return -1;
            }
            // same depth -> use class name to create artifical ordering
            return ClassNameComparator.INSTANCE.compare(factory1, factory2);
        }
    }
    
    private static class DataSourceKey<D extends DataSource>  {
        Reference<D> weakReference;
        
        DataSourceKey(D ds) {
            weakReference = new WeakReference(ds);
        }
        
        public int hashCode() {
            D ds = weakReference.get();
            if (ds != null) {
                return ds.hashCode();
            }
            return 0;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof DataSourceKey) {
                D ds = weakReference.get();
                D otherDs = ((DataSourceKey<D>)obj).weakReference.get();
                
                return ds != null && ds == otherDs;
            }
            throw new IllegalArgumentException(obj.getClass().getName());
        }
        
        public String toString() {
            return "DataSourceKey for "+weakReference.toString();
        }
    }
}
