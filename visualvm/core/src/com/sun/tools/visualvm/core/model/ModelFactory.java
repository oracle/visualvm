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
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 *
 * @author Tomas Hurka
 */
public abstract class ModelFactory<M extends Model,B extends DataSource> {

  private SortedSet<ModelProvider<M, B>> factories = new TreeSet(new SubModelFactoryComparator());
  private Map<B,Reference<M>> modelMap = new WeakHashMap();

  public final synchronized M getModel(B app) {
    Reference<M> modelRef = modelMap.get(app);
    if (modelRef != null) {
      M jvm = modelRef.get();
      if (jvm != null) {
        return jvm;
      }
    }
    for (ModelProvider<M, B> factory : factories) {
      M model = factory.createModelFor(app);
      if (model != null) {
        modelMap.put(app,new SoftReference(model));
        return model;
      }
    }
    return null;
  }
  
  public final synchronized void registerFactory(ModelProvider<M, B> newFactory) {
    clearCache();
    factories.add(newFactory);
  }
  
  public final synchronized boolean unregisterFactory(ModelProvider<M, B> oldFactory) {
   clearCache();
   return factories.remove(oldFactory);
  }
  
  public int depth() {
    return -1;
  }
  
  private void clearCache() {
    if (!modelMap.isEmpty()) modelMap = new WeakHashMap();
  }
  
  private class SubModelFactoryComparator implements Comparator<ModelProvider<M,B>> {

    public int compare(ModelProvider<M, B> factory1, ModelProvider<M, B> factory2) {
      int thisVal = factory1.depth();
      int anotherVal = factory2.depth();

      if (thisVal<anotherVal) {
        return 1;
      }
      if (thisVal>anotherVal) {
        return -1;
      }
      // same depth -> use class name to create artifical ordering
      return factory1.getClass().getName().compareTo(factory2.getClass().getName());
    }
  }
}
