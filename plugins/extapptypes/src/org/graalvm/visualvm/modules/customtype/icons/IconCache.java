/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.customtype.icons;

import org.graalvm.visualvm.api.caching.Cache;
import org.graalvm.visualvm.api.caching.CacheFactory;
import org.graalvm.visualvm.api.caching.Entry;
import org.graalvm.visualvm.api.caching.EntryFactory;
import org.graalvm.visualvm.api.caching.Persistor;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 *
 * @author Jaroslav Bachorik
 */
public class IconCache extends Cache<URL, BufferedImage> {
    final private IconResolver resolver = new IconResolver();
    final private Cache<URL, BufferedImage> delegate;

    final private static class Singleton {

        final private static IconCache INSTANCE = new IconCache();
    }

    final public static IconCache getDefault() {
        return Singleton.INSTANCE;
    }

    private IconCache() {
        Persistor<URL, BufferedImage> persistor;
        try {
            persistor = new FileImagePersistor();
            
        } catch (InstantiationException e) {
            persistor = Persistor.DEFAULT;
        }
        delegate = CacheFactory.getInstance().softMapCache(new EntryFactory<URL, BufferedImage>() {
            @Override
            public Entry<BufferedImage> createEntry(URL key) {
                BufferedImage img = resolver.resolveIcon(key);
                if (img != null) {
                    img = ImageUtils.resizeImage(img, 16, 16);
                }
                return new Entry<BufferedImage>(resolver.resolveIcon(key));
            }
        }, persistor);

    }

    @Override
    public long getTTL() {
        return delegate.getTTL();
    }

    @Override
    public BufferedImage invalidateObject(URL key) {
        return delegate.invalidateObject(key);
    }

    @Override
    public BufferedImage retrieveObject(URL key) {
        return delegate.retrieveObject(key);
    }

    @Override
    public void setTTL(long ttl) {
        delegate.setTTL(ttl);
    }
}
