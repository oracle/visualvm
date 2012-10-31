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

package com.sun.tools.visualvm.modules.security;

import com.sun.tools.visualvm.core.options.UISupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.LifecycleManager;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;


class SecurityOptionsPanelController extends OptionsPanelController {

    private SecurityModel model = SecurityModel.getInstance();
    private SecurityOptionsPanel panel;
    private JComponent component;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;


    public void update() {
        SecurityOptionsPanel p = getPanel();
        p.setKeyStore(model.getKeyStore());
        p.setKeyStorePassword(model.getKeyStorePassword());
        p.setKeyStoreType(model.getKeyStoreType());
        p.setTrustStore(model.getTrustStore());
        p.setTrustStorePassword(model.getTrustStorePassword());
        p.setTrustStoreType(model.getTrustStoreType());
        p.setEnabledProtocols(model.getEnabledProtocols());
        p.setEnabledCipherSuites(model.getEnabledCipherSuites());
        p.resetRestart();
    }

    public void applyChanges() {
        SecurityOptionsPanel p = getPanel();
        model.setKeyStore(p.getKeyStore());
        model.setKeyStorePassword(p.getKeyStorePassword());
        model.setKeyStoreType(p.getKeyStoreType());
        model.setTrustStore(p.getTrustStore());
        model.setTrustStorePassword(p.getTrustStorePassword());
        model.setTrustStoreType(p.getTrustStoreType());
        model.setEnabledProtocols(p.getEnabledProtocols());
        model.setEnabledCipherSuites(p.getEnabledCipherSuites());

        if (p.shouldRestart() && differsFromEnv()) {
            LifecycleManager lcm = LifecycleManager.getDefault();
            lcm.markForRestart();
            lcm.exit();
        }
    }

    public void cancel() {
        getPanel().cleanup();
    }

    public boolean isValid() {
        return getPanel().dataValid();
    }

    public boolean isChanged() {
        SecurityOptionsPanel p = getPanel();
        if (!equals(p.getKeyStore(), model.getKeyStore())) return true;
        if (!equals(p.getKeyStorePassword(), model.getKeyStorePassword())) return true;
        if (!equals(p.getKeyStoreType(), model.getKeyStoreType())) return true;
        if (!equals(p.getTrustStore(), model.getTrustStore())) return true;
        if (!equals(p.getTrustStorePassword(), model.getTrustStorePassword())) return true;
        if (!equals(p.getTrustStoreType(), model.getTrustStoreType())) return true;
        if (!equals(p.getEnabledProtocols(), model.getEnabledProtocols())) return true;
        if (!equals(p.getEnabledCipherSuites(), model.getEnabledCipherSuites())) return true;
        return false;
    }

    public boolean differsFromEnv() {
        SecurityOptionsPanel p = getPanel();
        if (!equals(p.getKeyStore(), SecurityModel.getKeyStoreEnv())) return true;
        if (!equals(p.getKeyStorePassword(), SecurityModel.getKeyStorePasswordEnv())) return true;
        if (!equals(p.getKeyStoreType(), SecurityModel.getKeyStoreTypeEnv())) return true;
        if (!equals(p.getTrustStore(), SecurityModel.getTrustStoreEnv())) return true;
        if (!equals(p.getTrustStorePassword(), SecurityModel.getTrustStorePasswordEnv())) return true;
        if (!equals(p.getTrustStoreType(), SecurityModel.getTrustStoreTypeEnv())) return true;
        if (!equals(p.getEnabledProtocols(), SecurityModel.getEnabledProtocolsEnv())) return true;
        if (!equals(p.getEnabledCipherSuites(), SecurityModel.getEnabledCipherSuitesEnv())) return true;
        return false;
    }


    public HelpCtx getHelpCtx() {
        return null;

    }


    public JComponent getComponent(Lookup masterLookup) {
        return getComponent();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }


    SecurityModel getModel() {
        return SecurityModel.getInstance();
    }


    private SecurityOptionsPanel getPanel() {
        if (panel == null) panel = new SecurityOptionsPanel(this);
        return panel;
    }

    private JComponent getComponent() {
        if (component == null) {
            component = UISupport.createScrollableContainer(getPanel());
        }
        return component;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

    private boolean equals(String o1, String o2) {
        if (o1 == null && o2 == null) return true;
        if (o1 != null && o1.equals(o2)) return true;
        return false;
    }

    private boolean equals(char[] ch1, char[] ch2) {
        if (ch1 == null && ch2 == null) return true;
        return Arrays.equals(ch1, ch2);
    }

}
