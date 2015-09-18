/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.SmallButton;
import org.netbeans.lib.profiler.ui.swing.renderer.JavaNameRenderer;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.Wildcards;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassMethodList_addMethod=Add method",
    "ClassMethodList_removeMethods=Remove selected methods",
    "ClassMethodList_addClass=Add class",
    "ClassMethodList_removeClasses=Remove selected classes",
    "ClassMethodList_selectedMethods=Selected methods:",
    "ClassMethodList_selectedClasses=Selected classes:"
})
public final class ClassMethodList {
    
    public static void showClasses(ProfilerSession session, Set<ClientUtils.SourceCodeSelection> selection, Component invoker) {
        UI.forClasses(session, selection).show(invoker);
    }
    
    public static void showMethods(ProfilerSession session, Set<ClientUtils.SourceCodeSelection> selection, Component invoker) {
        UI.forMethods(session, selection).show(invoker);
    }
    
    
    private ClassMethodList() {}
    
    private static class UI extends JPopupMenu {
        
        private boolean addingEntry = false;
        
        static UI forClasses(ProfilerSession session, Set<ClientUtils.SourceCodeSelection> selection) {
            return new UI(session, selection, false);
        }
        
        static UI forMethods(ProfilerSession session, Set<ClientUtils.SourceCodeSelection> selection) {
            return new UI(session, selection, true);
        }
        
        
        private UI(final ProfilerSession session, final Set<ClientUtils.SourceCodeSelection> selection, final boolean methods) {
            
            JPanel content = new JPanel(new BorderLayout(8, 3));
            content.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
            
            JLabel caption = new JLabel(methods ? Bundle.ClassMethodList_selectedMethods() :
                                                  Bundle.ClassMethodList_selectedClasses(), JLabel.LEADING);
            content.add(caption, BorderLayout.NORTH);
            
            class XListModel extends AbstractListModel<ClientUtils.SourceCodeSelection> {
                public int getSize() {
                    return selection.size();
                }
                public ClientUtils.SourceCodeSelection getElementAt(int index) {
                    return (ClientUtils.SourceCodeSelection)new ArrayList(selection).get(index);
                }
                public void refresh() {
                    super.fireContentsChanged(this, 0, getSize());
                }
            }
            
            final XListModel xmodel = new XListModel();
            final JList list = new JList(xmodel) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = 420;
                    return dim;
                }
            };
            list.setBackground(UIUtils.getProfilerResultsBackground());
            int format = methods ? DefaultMethodNameFormatter.VERBOSITY_CLASSMETHOD :
                                   DefaultMethodNameFormatter.VERBOSITY_CLASS;
            final MethodNameFormatter formatter = new DefaultMethodNameFormatter(format);
            final JavaNameRenderer renderer = new JavaNameRenderer();
            list.setCellRenderer(new ListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    renderer.setValue(formatter.formatMethodName((ClientUtils.SourceCodeSelection)value).toFormatted(), index);
                    JComponent c = renderer.getComponent();
                    if (isSelected && isEnabled()) {
                        c.setForeground(list.getSelectionForeground());
                        c.setBackground(list.getSelectionBackground());
                    } else if (!isEnabled()) {
                        c.setForeground(UIManager.getColor("TextField.inactiveForeground")); // NOI18N
                        c.setBackground(UIManager.getColor("TextField.inactiveBackground")); // NOI18N
                    } else {
                        c.setForeground(list.getForeground());
                        c.setBackground((index & 0x1) == 0 ? list.getBackground() :
                                         UIUtils.getDarker(list.getBackground()));
                    }
                    return c;
                }
            });
            
            String iconMask = methods ? LanguageIcons.METHOD : LanguageIcons.CLASS;
            Image baseIcon = Icons.getImage(iconMask);
            Image addBadge = Icons.getImage(GeneralIcons.BADGE_ADD);
            Image addImage = ImageUtilities.mergeImages(baseIcon, addBadge, 0, 0);
            final JButton addB = new SmallButton(ImageUtilities.image2Icon(addImage)) {
                protected void fireActionPerformed(ActionEvent e) {
                    final Component invoker = getInvoker();
                    addingEntry = true;
                    Collection<ClientUtils.SourceCodeSelection> sel = null;
                    
                    if (methods) {
                        if (Platform.isMac()) addingEntry = false; // Workaround to hide the popup window on Mac
                        
                        Collection<SourceMethodInfo> mtd = ClassMethodSelector.selectMethods(session);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                addingEntry = false;
                                UI.this.setVisible(false);
                                invoker.repaint();
                                UI.this.show(invoker);
                            }
                        });

                        if (!mtd.isEmpty()) {
                            sel = new HashSet();
                            for (SourceMethodInfo smi : mtd) sel.add(
                                    new ClientUtils.SourceCodeSelection(smi.getClassName(),
                                                                        smi.getName(), smi.getSignature()));
                        }
                    } else {
                        if (Platform.isMac()) addingEntry = false; // Workaround to hide the popup window on Mac
                        
                        Collection<SourceClassInfo> cls = ClassMethodSelector.selectClasses(session);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                addingEntry = false;
                                UI.this.setVisible(false);
                                invoker.repaint();
                                UI.this.show(invoker);
                            }
                        });

                        if (!cls.isEmpty()) {
                            sel = new HashSet();
                            for (SourceClassInfo sci : cls) sel.add(new ClientUtils.SourceCodeSelection(
                                    sci.getQualifiedName(), Wildcards.ALLWILDCARD, null));
                        }
                    }
                    
                    if (sel != null) {
                        selection.addAll(sel);
                        xmodel.refresh();
                    }
                }   
            };
            addB.setToolTipText(methods ? Bundle.ClassMethodList_addMethod() :
                                          Bundle.ClassMethodList_addClass());
            
            Image removeBadge = Icons.getImage(GeneralIcons.BADGE_REMOVE);
            Image removeImage = ImageUtilities.mergeImages(baseIcon, removeBadge, 0, 0);
            final JButton removeB = new SmallButton(ImageUtilities.image2Icon(removeImage)) {
                protected void fireActionPerformed(ActionEvent e) {
                    final Component invoker = getInvoker();
                    
                    selection.removeAll(list.getSelectedValuesList());
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { invoker.repaint(); }
                    });
                    
                    xmodel.refresh();
                    list.clearSelection();
                    setEnabled(false);
                }
            };
            removeB.setToolTipText(methods ? Bundle.ClassMethodList_removeMethods() :
                                             Bundle.ClassMethodList_removeClasses());
            removeB.setEnabled(false);
            
            
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) return;
                    removeB.setEnabled(list.getSelectedValue() != null);
                }
            });
            
            JScrollPane scroll = new JScrollPane(list);
            caption.setLabelFor(scroll);
            content.add(scroll, BorderLayout.CENTER);
            
            JPanel buttons = new JPanel(null);
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
            buttons.add(addB);
            buttons.add(removeB);
            content.add(buttons, BorderLayout.EAST);
            
            add(content);
            
        }
        
        public void setVisible(boolean b) {
            if (!addingEntry) super.setVisible(b);
        }
        
        void show(Component invoker) {
            show(invoker, -5, invoker.getHeight() - 1);
        }
        
    }
    
}
