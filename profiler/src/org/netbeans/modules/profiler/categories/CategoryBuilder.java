/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.categories;

import java.util.Enumeration;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.Repository;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 */
public class CategoryBuilder  {
    private static final String CATEGORY_ATTRIB_CUSTOM = "custom";
    private static final String CATEGORY_ATTRIB_EXCLUDES = "excludes";
    private static final String CATEGORY_ATTRIB_INCLUDES = "includes";
    private static final String CATEGORY_ATTRIB_INSTANCENAME = "instanceClass";
    private static final String CATEGORY_ATTRIB_PREFIX = "~";
    private static final String CATEGORY_ATTRIB_SUBTYPES = "subtypes";
    private static final String CATEGORY_ATTRIB_TYPE = "type";
    
    private String defPath;
    private CategoryContainer rootCategory = null;
    
    public CategoryBuilder(Project project, String definitionPath) {
        defPath = definitionPath;
    }
    
    public synchronized Category getRootCategory() {
        if (rootCategory == null) {
            rootCategory = new CategoryContainer("ROOT", "Root category");

            FileSystem fs = Repository.getDefault().getDefaultFileSystem();
            FileObject aoi = fs.findResource("Profiler/" + defPath + "/AreaOfInterest"); //NOI18N
            if (aoi != null) {
                Enumeration<? extends FileObject> folders = aoi.getFolders(false);
                while (folders.hasMoreElements()) {
                    FileObject folder = folders.nextElement();
                    processCategories(rootCategory, folder);
                }
            }
        }
        return rootCategory;
    }
    
    private void processCategories(CategoryContainer container, FileObject node) {
        String bundleName = (String) node.getAttribute("SystemFileSystem.localizingBundle"); // NOI18N
        String label = bundleName != null ? NbBundle.getBundle(bundleName).getString(node.getPath()) : node.getName();
        
        CategoryContainer newCategory = new CategoryContainer(node.getPath(), label);
        container.add(newCategory);
        
        Enumeration<? extends FileObject> subNodes = node.getFolders(false);
        while (subNodes.hasMoreElements()) {
            FileObject subNode = subNodes.nextElement();
            String nodeName = subNode.getName();
            if (nodeName.startsWith(CATEGORY_ATTRIB_PREFIX)) {
                if (nodeName.endsWith(CATEGORY_ATTRIB_SUBTYPES)) {
                    Enumeration<? extends FileObject> marks = subNode.getData(false);
                    while (marks.hasMoreElements()) {
                        FileObject category = marks.nextElement();
                        String excludes = (String)category.getAttribute(CATEGORY_ATTRIB_EXCLUDES);
                        String includes = (String)category.getAttribute(CATEGORY_ATTRIB_INCLUDES);
                        String includesArr[] = includes != null ? includes.split(",") : null;
                        String excludesArr[] = excludes != null ? excludes.split(",") : null;
                        
                        newCategory.getDefinitions().add(new SubtypeCategoryDefinition(newCategory, category.getNameExt(), includesArr, excludesArr));
                    }
                } else if (nodeName.endsWith(CATEGORY_ATTRIB_TYPE)) {
                    Enumeration<? extends FileObject> marks = subNode.getData(false);
                    while (marks.hasMoreElements()) {
                        FileObject mark = marks.nextElement();

                        String excludes = (String)mark.getAttribute(CATEGORY_ATTRIB_EXCLUDES);
                        String includes = (String)mark.getAttribute(CATEGORY_ATTRIB_INCLUDES);
                        String includesArr[] = includes != null ? includes.split(",") : null;
                        String excludesArr[] = excludes != null ? excludes.split(",") : null;
                        
                        newCategory.getDefinitions().add(new SingleTypeCategoryDefinition(newCategory, mark.getNameExt(), includesArr, excludesArr));
                    }
                } else if (nodeName.endsWith(CATEGORY_ATTRIB_CUSTOM)) {
                    System.out.println(subNode.getAttribute(CATEGORY_ATTRIB_INSTANCENAME));
                }
            } else {
                processCategories(newCategory, subNode);
            }
        }
    }
}
