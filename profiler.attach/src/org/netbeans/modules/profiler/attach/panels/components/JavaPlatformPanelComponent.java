/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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


package org.netbeans.modules.profiler.attach.panels.components;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataListener;
import org.netbeans.api.java.platform.PlatformsCustomizer;
import org.netbeans.modules.profiler.attach.providers.TargetPlatform;
import org.openide.util.NbBundle;

/**
 *
 * @author  Jaroslav Bachorik
 */
public class JavaPlatformPanelComponent extends javax.swing.JPanel {
  // <editor-fold defaultstate="collapsed" desc="JvmComboBoxModel">
  private interface JvmComboBoxModel extends ComboBoxModel {
    public void setJvmList(final Vector jvmList);
    public boolean isEmpty();
  }
  
  private class JvmComboBoxModelProxy implements JvmComboBoxModel {
    private final JvmComboBoxModel empty = new JvmComboBoxModelEmpty();
    private final JvmComboBoxModel normal = new JvmComboBoxModelImpl();
    
    private JvmComboBoxModel proxied;
    
    public JvmComboBoxModelProxy() {
      proxied = empty;
    }
    
    public void addListDataListener(ListDataListener l) {
      proxied.addListDataListener(l);
    }
    public Object getElementAt(int index) {
      return proxied.getElementAt(index);
    }
    public Object getSelectedItem() {
      return proxied.getSelectedItem();
    }
    public int getSize() {
      return proxied.getSize();
    }
    public void removeListDataListener(ListDataListener l) {
      proxied.removeListDataListener(l);
    }
    public void setSelectedItem(Object anItem) {
      proxied.setSelectedItem(anItem);
    }
    
    public void setJvmList(final Vector jvmList) {
      if (jvmList != null && jvmList.size() > 0) {
        proxied = normal;
      } else {
        proxied = empty;
      }
      proxied.setJvmList(jvmList);
    }
    
    public boolean isEmpty() {
      return proxied.isEmpty();
    }
  }
  
  private class JvmComboBoxModelEmpty extends AbstractListModel implements JvmComboBoxModel {
    public void setSelectedItem(Object anItem) {
//      firePropertyChange(JAVA_PLATFORM_PROPERTY, selectedPlatform, anItem);
//      selectedPlatform = (TargetPlatform)anItem;
    }
    
    public Object getElementAt(int index) {
      return NO_SUPPORTED_JVM_MSG;
    }
    
    public int getSize() {
      return 1;
    }
    
    public Object getSelectedItem() {
      return NO_SUPPORTED_JVM_MSG;
    }
    
    public void setJvmList(final Vector jvmList) {
      firePropertyChange(JAVA_PLATFORM_PROPERTY, selectedPlatform, null);
    }
    
    public boolean isEmpty() {
      return true;
    }
  }
  
  private class JvmComboBoxModelImpl implements JvmComboBoxModel {
    private List platformList = null;
    private TargetPlatform defaultPlatform;
    
    private DefaultComboBoxModel delegate = null;
    private boolean isSelectionMade = false;
    
    private DefaultComboBoxModel getDelegate() {
      if (delegate == null) {
        delegate = new DefaultComboBoxModel();
      }
      return delegate;
    }
    
    public Object getElementAt(int index) {
      return getDelegate().getElementAt(index);
    }
    public Object getSelectedItem() {
      return getDelegate().getSelectedItem();
//      if (selectedPlatform == null || !platformList.contains(selectedPlatform)) {
//        if (defaultPlatform != null) {
//          setSelectedItem(defaultPlatform);
//        } else {
//          setSelectedItem((TargetPlatform)platformList.get(0));
//        }
//      }
//      return selectedPlatform;
    }
    
    public int getSize() {
      return getDelegate().getSize();
    }
    
    public void setSelectedItem(Object anItem) {
      getDelegate().setSelectedItem(anItem);
      firePropertyChange(JAVA_PLATFORM_PROPERTY, null, getDelegate().getSelectedItem());
      selectedPlatform = (TargetPlatform)anItem;
    }
    
    public void setJvmList(final Vector jvmList) {
      Object oldSelection = getDelegate().getSelectedItem();
      
      delegate = new DefaultComboBoxModel(jvmList);

      if (oldSelection == null || !jvmList.contains(oldSelection)) {
        boolean foundDefault = false;
        for (Iterator it = jvmList.iterator(); it.hasNext();) {
          TargetPlatform platform = (TargetPlatform) it.next();
          if (platform.isDefault()) {
            setSelectedItem(platform);
            foundDefault = true;
            break;
          }
        }
        if (!foundDefault && jvmList.size() > 0) {
          setSelectedItem(jvmList.get(0));
        }
      } else {
        setSelectedItem(oldSelection);
      }
      
    }
    
    public boolean isEmpty() {
      return false;
    }
    
    public void removeListDataListener(ListDataListener l) {
      getDelegate().removeListDataListener(l);
    }
    
    public void addListDataListener(ListDataListener l) {
      getDelegate().addListDataListener(l);
    }
  }
  // </editor-fold>
  
  private final String NO_SUPPORTED_JVM_MSG = NbBundle.getMessage(JavaPlatformPanelComponent.class, "JavaPlatformPanelComponent_NoSupportedJvmFoundString"); // NOI18N
  public static final String JAVA_PLATFORM_PROPERTY = "jvmPlatform"; // NOI18N
  
  private final TargetPlatform.TargetPlatformFilter defaultPlatformFilter = new TargetPlatform.TargetPlatformFilter() {
    public boolean isSupported(TargetPlatform javaPlatform) {
      return true;
    }
  };
  
  private TargetPlatform.TargetPlatformFilter platformFilter = defaultPlatformFilter;
  
  private TargetPlatform selectedPlatform = null;
  private List lastPlatformList = new Vector();
  
  private JvmComboBoxModel jvmModel = new JvmComboBoxModelProxy();
  private final JvmComboBoxModel emptyModel = new JvmComboBoxModelEmpty();
  
  /** Creates new form JavaPlatformPanelComponent */
  public JavaPlatformPanelComponent() {
    initComponents();
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelHint = new javax.swing.JTextArea();
        comboJvm = new javax.swing.JComboBox();
        buttonManage = new javax.swing.JButton();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/attach/panels/components/JavaPlatformPanelComponent"); // NOI18N
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("SELECT_PLATFORM"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, UIManager.getFont("TitledBorder.font").deriveFont(Font.BOLD))); // NOI18N

        labelHint.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        labelHint.setColumns(20);
        labelHint.setEditable(false);
        labelHint.setForeground(new java.awt.Color(204, 204, 0));
        labelHint.setLineWrap(true);
        labelHint.setRows(5);
        labelHint.setText(bundle.getString("PLATFORMS_HINT")); // NOI18N
        labelHint.setWrapStyleWord(true);
        labelHint.setDisabledTextColor(new java.awt.Color(116, 122, 128));
        labelHint.setEnabled(false);
        labelHint.setFocusable(false);
        labelHint.setMaximumSize(new java.awt.Dimension(800, 14));
        labelHint.setPreferredSize(new java.awt.Dimension(400, 20));
        labelHint.setRequestFocusEnabled(false);
        labelHint.setSelectionColor(new java.awt.Color(230, 238, 246));
        labelHint.setVerifyInputWhenFocusTarget(false);

        comboJvm.setModel(jvmModel);
        comboJvm.setMaximumSize(new java.awt.Dimension(400, 24));
        comboJvm.setMinimumSize(new java.awt.Dimension(160, 24));
        comboJvm.setPreferredSize(new java.awt.Dimension(180, 24));

        org.openide.awt.Mnemonics.setLocalizedText(buttonManage, bundle.getString("MANAGE_PLATFORMS")); // NOI18N
        buttonManage.setMaximumSize(new java.awt.Dimension(156, 24));
        buttonManage.setMinimumSize(new java.awt.Dimension(156, 24));
        buttonManage.setPreferredSize(new java.awt.Dimension(156, 24));
        buttonManage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonManageActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, labelHint, 0, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(comboJvm, 0, 272, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(buttonManage, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(comboJvm, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(buttonManage, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(labelHint, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                .addContainerGap())
        );

        labelHint.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JavaPlatformPanelComponent.class, "JavaPlatformPanelComponent.labelHint.AccessibleContext.accessibleName")); // NOI18N
        comboJvm.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(JavaPlatformPanelComponent.class, "JavaPlatformPanelComponent.comboJvm.AccessibleContext.accessibleName")); // NOI18N
        comboJvm.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JavaPlatformPanelComponent.class, "JavaPlatformPanelComponent.comboJvm.AccessibleContext.accessibleDescription")); // NOI18N
        buttonManage.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JavaPlatformPanelComponent.class, "JavaPlatformPanelComponent.buttonManage.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
  
    private void buttonManageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonManageActionPerformed
      PlatformsCustomizer.showCustomizer(null);
      TargetPlatform newPlatform = findNewItem(lastPlatformList, TargetPlatform.getPlatformList(getPlatformFilter(), false));
      refresh(newPlatform);
//      List platformList = TargetPlatform.getPlatformList(getPlatformFilter(), false);
//      
//      jvmModel.setJvmList((Vector)platformList);
//      comboJvm.invalidate();
    }//GEN-LAST:event_buttonManageActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonManage;
    private javax.swing.JComboBox comboJvm;
    private javax.swing.JTextArea labelHint;
    // End of variables declaration//GEN-END:variables
  
  public String getTitle() {
    return ((TitledBorder)this.getBorder()).getTitle();
  }
  public void setTitle(String title) {
    ((TitledBorder)this.getBorder()).setTitle(title);
  }
  
  public String getHint() {
    return labelHint.getText();
  }
  public void setHint(String hint) {
    labelHint.setText(hint);
  }
  
  public Color getHintForeground() {
    return labelHint.getDisabledTextColor();
  }
  public void setHintForeground(Color color) {
    labelHint.setDisabledTextColor(color);
  }
  
  public Color getHintBackground() {
    return labelHint.getBackground();
  }
  public void setHintBackground(Color bgcolor) {
    labelHint.setBackground(bgcolor);
  }
  
  public TargetPlatform getSelectedPlatform() {
    if (!jvmModel.isEmpty())
      return (TargetPlatform)jvmModel.getSelectedItem();
    else
      return null;
  }
  public void setSelectedPlatform(TargetPlatform platform) {
    this.selectedPlatform = platform;
  }
  
  public TargetPlatform.TargetPlatformFilter getPlatformFilter() {
    if (this.platformFilter == null) {
      this.platformFilter = defaultPlatformFilter;
    }
    return this.platformFilter;
  }
  public void setPlatformFilter(TargetPlatform.TargetPlatformFilter javaPlatformFilter) {
    this.platformFilter = javaPlatformFilter;
  }
  
  public void refresh() {
    this.refresh(null);
  }
  
  public void refresh(final TargetPlatform preselectedPlatform) {
    List platformList = TargetPlatform.getPlatformList(getPlatformFilter(), false);
    
    comboJvm.setModel(emptyModel);
    jvmModel.setJvmList((Vector)platformList);
    comboJvm.setModel(jvmModel);
    if (preselectedPlatform != null && platformList.contains(preselectedPlatform)) {
      jvmModel.setSelectedItem(preselectedPlatform);
    }
    lastPlatformList = platformList;
  }
  
  private static TargetPlatform findNewItem(final List oldItemList, final List newItemList) {
    if (oldItemList == null || newItemList == null)
      return null;
    
    if (newItemList.size() == 0)
      return null;
    
    for (Iterator it = newItemList.iterator(); it.hasNext();) {
      Object elem = (Object) it.next();
      if(!oldItemList.contains(elem)) {
        return (TargetPlatform)elem;
      }
    }
    return null;
  }
}
