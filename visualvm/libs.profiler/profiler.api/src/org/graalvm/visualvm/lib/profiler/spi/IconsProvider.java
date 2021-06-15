/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.spi;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class IconsProvider {

    /**
     * Returns an Image instance according to the provided key.
     *
     * @param key image key
     * @return Image instance according to the provided key or null if no image is provided for the key by this provider
     */
    public abstract Image getImage(String key);

    /**
     * Returns path to image resource without leading slash according to the provided key.
     *
     * @param key image key
     * @return path to image resource without leading slash according to the provided key or null if no image is provided for the key by this provider
     */
    public abstract String getResource(String key);


    /**
     * Basic implementation of a simple IconsProvider supporting statically defined and dynamically generated images.
     */
    public static abstract class Basic extends IconsProvider {

        private Map<String, String> images;

        @Override
        public final Image getImage(String key) {
            String resource = getResource(key);
            if (resource == null) return getDynamicImage(key);
            else return ImageUtilities.loadImage(resource, true);
        }

        @Override
        public final String getResource(String key) {
            return getImageCache().get(key);
        }

        private Map<String, String> getImageCache() {
            synchronized (this) {
                if (images == null) {
                    images = new HashMap<String, String>() {
                        public String put(String key, String value) {
                            return super.put(key, getImagePath(value));
                        }
                    };
                    initStaticImages(images);
                }
            }
            return images;
        }
        
        protected String getImagePath(String imageFile) {
            String packagePrefix = getClass().getPackage().getName().
                                   replace('.', '/') + "/"; // NOI18N
            return packagePrefix + imageFile;
        }

        protected void initStaticImages(Map<String, String> cache) {}

        protected Image getDynamicImage(String key) { return null; }
        
    }
    
}
