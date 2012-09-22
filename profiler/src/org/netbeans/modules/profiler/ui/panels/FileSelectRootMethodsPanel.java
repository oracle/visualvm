/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.profiler.ui.panels;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilderFactory;
import org.netbeans.modules.profiler.selector.spi.SelectionTreeBuilder;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilderType;
import org.netbeans.modules.profiler.api.ProgressDisplayer;
import org.netbeans.modules.profiler.api.java.ExternalPackages;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourcePackageInfo;
import org.netbeans.modules.profiler.selector.ui.RootSelectorTree;
import org.netbeans.modules.profiler.selector.ui.TreePathSearch;
import org.netbeans.modules.profiler.ui.ProfilerProgressDisplayer;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "SelectRootMethodsPanel_OkButtonText=OK",
    "SelectRootMethodsPanel_ParsingProjectStructureMessage=Parsing project structure...",
    "SelectRootMethodsPanel_Title=Edit Profiling Roots"
})
final public class FileSelectRootMethodsPanel extends JPanel {

    final private static class Singleton {

        final private static FileSelectRootMethodsPanel INSTANCE = new FileSelectRootMethodsPanel();
    }

    final public static FileSelectRootMethodsPanel getDefault() {
        return Singleton.INSTANCE;
    }
    private static final String HELP_CTX_KEY = "ClassSelectRootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static final Dimension PREFERRED_TOPTREE_DIMENSION = new Dimension(500, 250);
    private JButton okButton;
    private RootSelectorTree fileTreeView;

    private FileSelectRootMethodsPanel() {
        init(this);
    }

    private void init(final Container container) {
        GridBagConstraints gridBagConstraints;

        okButton = new JButton(Bundle.SelectRootMethodsPanel_OkButtonText());

        ProgressDisplayer pd = ProfilerProgressDisplayer.getDefault();

        fileTreeView = new RootSelectorTree(pd, new TreePathSearch.ClassIndex() {

            @Override
            public List<SourceClassInfo> getClasses(String pattern, Lookup context) {
                Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                List<SourceClassInfo> rslt = new ArrayList<SourceClassInfo>();
                SourceClassInfo clz = context.lookup(SourceClassInfo.class);
                if (clz != null) {                    
                    addClassRecursively(clz, rslt);
                } else {
                    FileObject fo = context.lookup(FileObject.class);
                    if (FileUtil.isArchiveFile(fo) || fo.isFolder()) {
                        for(SourcePackageInfo spi : ExternalPackages.forPath(fo)) {
                            addPackageRecursively(spi, rslt);
                        }
                    }
                }
                for(Iterator<SourceClassInfo> iter = rslt.iterator();iter.hasNext();) {
                    clz=iter.next();
                    if (!p.matcher(clz.getSimpleName()).matches()) {
                        iter.remove();
                    }
                }
                Collections.sort(rslt, SourceClassInfo.COMPARATOR);
                return rslt;
            }
            
            private void addPackageRecursively(SourcePackageInfo spi, List<SourceClassInfo> clzs) {
                for(SourcePackageInfo sub : spi.getSubpackages()) {
                    addPackageRecursively(sub, clzs);
                }
                for(SourceClassInfo sci : spi.getClasses()) {
                    addClassRecursively(sci, clzs);
                }
            }
            
            private void addClassRecursively(SourceClassInfo sc, List<SourceClassInfo> clzs) {
                clzs.add(sc);
                for(SourceClassInfo scInner : sc.getInnerClases()) {
                    addClassRecursively(scInner, clzs);
                }
            }
        });

        container.setLayout(new GridBagLayout());

        fileTreeView.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        fileTreeView.setPreferredSize(PREFERRED_TOPTREE_DIMENSION);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 10, 0, 10);
        container.add(fileTreeView, gridBagConstraints);
    }

    public ClientUtils.SourceCodeSelection[] getRootMethods(final FileObject javaFile,
            final ClientUtils.SourceCodeSelection[] currentSelection) {
        fileTreeView.reset();

        updateSelector(new Runnable() {

            public void run() {
                List<SelectionTreeBuilder> builders = SelectionTreeBuilderFactory.buildersFor(javaFile);
                fileTreeView.setSelection(currentSelection, Lookups.fixed((Object[])builders.toArray(new SelectionTreeBuilder[builders.size()])));

                List<SelectionTreeBuilderType> builderTypes = fileTreeView.getBuilderTypes();
                if (builderTypes.size() > 0) {
                    fileTreeView.setBuilderType(builderTypes.get(0));
                }
            }
        });

        final DialogDescriptor dd = new DialogDescriptor(this,
                Bundle.SelectRootMethodsPanel_Title(),
                true, new Object[]{okButton, DialogDescriptor.CANCEL_OPTION},
                okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);

//            Object[] additionalOptions = getAdditionalOptions();
//
//            if ((additionalOptions != null) && (additionalOptions.length > 0)) {
//                dd.setAdditionalOptions(additionalOptions);
//            }

        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        
        Cancellable c = new Cancellable() {
            @Override
            public boolean cancel() {
                dd.setValue(DialogDescriptor.CANCEL_OPTION);
                d.setVisible(false);
                fileTreeView.setCancelHandler(null);
                return true;
            }
        };
            
        fileTreeView.setCancelHandler(c);
        
        d.pack(); // To properly layout HTML hint area
        d.setVisible(true);

        if (dd.getValue().equals(okButton)) {
            ClientUtils.SourceCodeSelection[] selection = fileTreeView.getSelection();
            return selection;
        }
        return null;
    }

    private void updateSelector(final Runnable updater) {        
        try {
            setUIEnabled(false);
            updater.run();
        } finally {
            setUIEnabled(true);
        }
    }

    private void setUIEnabled(final boolean val) {
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() {
                okButton.setEnabled(val);
                fileTreeView.setEnabled(val);
            }
        });
    }
}
