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

package com.sun.tools.visualvm.application.type.custom.images;

import com.sun.tools.visualvm.application.type.custom.cache.AbstractCache;
import com.sun.tools.visualvm.application.type.custom.cache.Entry;
import com.sun.tools.visualvm.application.type.custom.cache.Persistor;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ImageCache extends AbstractCache<URL, BufferedImage> {
    final private ImageResolver resolver = new ImageResolver();

    final private static class Singleton {

        final private static ImageCache INSTANCE = new ImageCache();
    }

    final public static ImageCache getDefault() {
        return Singleton.INSTANCE;
    }

    private ImageCache() {
        try {
            setPersistor(new FileImagePersistor());
        } catch (InstantiationException e) {
            setPersistor(Persistor.DEFAULT);
        }
    }

    @Override
    protected Entry<BufferedImage> cacheMiss(URL key) {
        return new Entry(resolver.resolveImage(key));
    }
}
