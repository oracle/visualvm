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
package org.netbeans.modules.profiler.impl.icons;

import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.profiler.api.GeneralIcons;
import org.netbeans.modules.profiler.api.ProfilerIcons;
import org.netbeans.modules.profiler.spi.IconsProvider;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.spi.IconsProvider.class)
public final class IconsProviderImpl extends IconsProvider {
    
    private Map<String, String> images;

    @Override
    public Image getImage(String key) {
        String imagePath = getImageCache().get(key);
        return imagePath == null ? null : ImageUtilities.loadImage(imagePath, true);
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
                initImageCache(images);
            }
        }
        return images;
    }
    
    private static void initImageCache(Map<String, String> cache) {
        cache.put(GeneralIcons.SET_FILTER, "setFilter.png");
        cache.put(GeneralIcons.CLEAR_FILTER, "clearFilter.png");
        cache.put(GeneralIcons.CLOSE_PANEL, "closePanel.png");
        cache.put(GeneralIcons.FILTER_CONTAINS, "filterContains.png");
        cache.put(GeneralIcons.FILTER_ENDS_WITH, "filterEndsWith.png");
        cache.put(GeneralIcons.FILTER_REG_EXP, "filterRegExp.png");
        cache.put(GeneralIcons.FILTER_STARTS_WITH, "filterStartsWith.png");
        cache.put(GeneralIcons.COLLAPSED_SNIPPET, "collapsedSnippet.png");
        cache.put(GeneralIcons.EXPANDED_SNIPPET, "expandedSnippet.png");
        cache.put(GeneralIcons.HIDE_COLUMN, "hideColumn.png");
        cache.put(GeneralIcons.MAXIMIZE_PANEL, "maximizePanel.png");
        cache.put(GeneralIcons.MINIMIZE_PANEL, "minimizePanel.png");
        cache.put(GeneralIcons.RESTORE_PANEL, "restorePanel.png");
        cache.put(GeneralIcons.SORT_ASCENDING, "sortAsc.png");
        cache.put(GeneralIcons.SORT_DESCENDING, "sortDesc.png");
        cache.put(GeneralIcons.POPUP_ARROW, "popupArrow.png");
        cache.put(GeneralIcons.ZOOM, "zoom.png");
        cache.put(GeneralIcons.ZOOM_IN, "zoomIn.png");
        cache.put(GeneralIcons.ZOOM_OUT, "zoomOut.png");
        cache.put(GeneralIcons.SCALE_TO_FIT, "scaleToFit.png");
        cache.put(GeneralIcons.INFO, "infoIcon.png");
        
        cache.put(ProfilerIcons.NODE_FORWARD, "forwardNode.png");
        cache.put(ProfilerIcons.NODE_REVERSE, "reverseNode.png");
        cache.put(ProfilerIcons.NODE_LEAF, "leafNode.png");
        cache.put(ProfilerIcons.SNAPSHOT_MEMORY_BIG, "memorySnapshotBig.png");
        cache.put(ProfilerIcons.VIEW_THREADS_BIG, "threadsViewBig.png");
        cache.put(ProfilerIcons.THREAD, "thread.png");
        cache.put(ProfilerIcons.ALL_THREADS, "allThreads.png");
    }
    
}
