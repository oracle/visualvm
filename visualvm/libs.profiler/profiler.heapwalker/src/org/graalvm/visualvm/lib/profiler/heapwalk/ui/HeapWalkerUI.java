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

package org.graalvm.visualvm.lib.profiler.heapwalk.ui;

import org.graalvm.visualvm.lib.profiler.heapwalk.HeapWalker;
import org.graalvm.visualvm.lib.profiler.heapwalk.HeapWalkerManager;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.profiler.ProfilerTopComponent;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapWalkerUI_ComponentDescr=Profiler HeapWalker",
    "HeapWalkerUI_LoadingProgress=Loading heap dump..."
})
public class HeapWalkerUI extends ProfilerTopComponent {
    
    private static final byte PERSISTENCE_VERSION_MAJOR = 8;
    private static final byte PERSISTENCE_VERSION_MINOR = 1;
    
    private static final String HELP_CTX_KEY = "HeapWalker.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeapWalker heapWalker;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    
    /**
     * Default constructor, used when restoring persisted heap dumps.
     */
    public HeapWalkerUI() {
        setIcon(Icons.getImage(ProfilerIcons.HEAP_DUMP));
        getAccessibleContext().setAccessibleDescription(Bundle.HeapWalkerUI_ComponentDescr());
        setLayout(new BorderLayout());
    }
    
    public HeapWalkerUI(HeapWalker heapWalker) {
        this();
        initImpl(heapWalker);
    }
    
    private void initImpl(HeapWalker hw) {
        this.heapWalker = hw;

        initDefaults();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    protected Component defaultFocusOwner() {
        return heapWalker == null ? this : heapWalker.getMainHeapWalker().getPanel();
    }

    // --- TopComponent support --------------------------------------------------
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        
        out.writeByte(PERSISTENCE_VERSION_MAJOR);
        out.writeByte(PERSISTENCE_VERSION_MINOR);
        
        out.writeUTF(Utilities.toURI(heapWalker.getHeapDumpFile()).toString());
    }
    
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        try {
            add(new JLabel(Bundle.HeapWalkerUI_LoadingProgress(), JLabel.CENTER), BorderLayout.CENTER);
            invalidate();
            doLayout();
            repaint();
            
            super.readExternal(in);
            
            in.readByte(); // PERSISTENCE_VERSION_MAJOR
            in.readByte(); // PERSISTENCE_VERSION_MINOR
            
            URI uri = new URI(in.readUTF());
            final File file = Utilities.toFile(uri);
            new RequestProcessor("HPROF loader for " + getName()).post(new Runnable() { // NOI18N
                public void run() {
                    try {
                        final HeapWalker hw = new HeapWalker(file);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                removeAll();
                                initImpl(hw);
                            }
                        });
                    } catch (Throwable t) { handleReadThrowable(t); }
                }
            });
        } catch (Throwable t) { handleReadThrowable(t); }
    }
    
    private void handleReadThrowable(Throwable t) {
        ProfilerLogger.info("Restoring heap dump failed: " + t.getMessage()); // NOI18N
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { close(); }
        });
    }

    public int getPersistenceType() {
        return ProfilerIDESettings.getInstance().getReopenHeapDumps()?
               PERSISTENCE_ONLY_OPENED : PERSISTENCE_NEVER;
    }

    protected void componentClosed() {
        if (heapWalker == null) return; // Window closed after persistence failure
        
        HeapWalkerManager.getDefault().heapWalkerClosed(heapWalker);
    }

    protected String preferredID() {
        return this.getClass().getName();
    }
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        add(heapWalker.getMainHeapWalker().getPanel(), BorderLayout.CENTER);
        invalidate();
        doLayout();
        repaint();
    }

    private void initDefaults() {
        setName(heapWalker.getName());
        if (heapWalker.getHeapDumpFile() != null)
            setToolTipText(heapWalker.getHeapDumpFile().getAbsolutePath());
        
        File file = heapWalker.getHeapDumpFile();
        putClientProperty(RECENT_FILE_KEY, file); // #221709, add heap dump to Open Recent File list
    }
}
