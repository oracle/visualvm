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
package org.graalvm.visualvm.lib.profiler.api.icons;

import java.awt.Image;
import java.util.Collection;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.profiler.spi.IconsProvider;
import org.openide.util.Lookup;

/**
 * Support for predefined icons and images.
 *
 * @author Jiri Sedlacek
 */
public final class Icons {

    /**
     * Returns an Icon instance according to the provided key.
     *
     * @param key icon key
     * @return Icon instance according to the provided key
     */
    public static Icon getIcon(String key) {
        return getImageIcon(key);
    }

    /**
     * Returns an ImageIcon instance according to the provided key.
     *
     * @param key icon key
     * @return ImageIcon instance according to the provided key
     */
    public static ImageIcon getImageIcon(String key) {
        Image image = getImage(key);
        if (image == null) return null;
        else return new ImageIcon(image);
    }

    /**
     * Returns an Image instance according to the provided key.
     *
     * @param key image key
     * @return Image instance according to the provided key
     */
    public static Image getImage(String key) {
        Collection<? extends IconsProvider> ps = providers();
        for (IconsProvider p : ps) {
            Image image = p.getImage(key);
            if (image != null) return image;
        }
        return null;
    }

    /**
     * Returns path to image resource without leading slash according to the provided key.
     *
     * @param key image key
     * @return path to image resource without leading slash according to the provided key
     */
    public static String getResource(String key) {
        Collection<? extends IconsProvider> ps = providers();
        for (IconsProvider p : ps) {
            String resource = p.getResource(key);
            if (resource != null) return resource;
        }
        return null;
    }
    
    private static Collection<? extends IconsProvider> providers() {
        return Lookup.getDefault().lookupAll(IconsProvider.class);
    }
    
    
    public static interface Keys {}
    
}
