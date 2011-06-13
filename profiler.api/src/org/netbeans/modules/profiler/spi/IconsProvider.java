/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.spi;

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
                    final String packagePrefix = getClass().getPackage().getName().
                                                 replace('.', '/') + "/"; // NOI18N
                    images = new HashMap<String, String>() {
                        public String put(String key, String value) {
                            return super.put(key, packagePrefix + value);
                        }
                    };
                    initStaticImages(images);
                }
            }
            return images;
        }

        protected void initStaticImages(Map<String, String> cache) {}

        protected Image getDynamicImage(String key) { return null; }
        
    }
    
}
