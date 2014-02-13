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
package org.netbeans.modules.profiler.heapwalk.details.spi;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "BrowserUtils_Loading=Loading content..."                                   // NOI18N
})
public abstract class DetailsProvider {
    
    // [Event Dispatch Thread / Worker Thread] List of supported classes, null for all
    public String[] getSupportedClasses() {
        return null;
    }
    
    // [Worker Thread] Short string representing the instance
    public String getDetailsString(String className, Instance instance, Heap heap) {
        return null;
    }
    
    // [Event Dispatch Thread] UI to visualize the selected instance
    public View getDetailsView(String className, Instance instance, Heap heap) {
        return null;
    }
    
    
    public static abstract class Basic extends DetailsProvider {
        
        private final String[] supportedClasses;
        
        // Use to register for all classes
        public Basic() {
            this((String[])null);
        }
        
        // Use to register for defined classes
        protected Basic(String... supportedClasses) {
            this.supportedClasses = supportedClasses;
        }
        
        public final String[] getSupportedClasses() {
            return supportedClasses;
        }
        
    }
    
    
    public static abstract class View extends JPanel {
        
        private RequestProcessor.Task workerTask;
        private Instance instance;
        private Heap heap;
        
        // [Event Dispatch Thread] Constructor for default initial UI ("Loading content...")
        protected View(Instance instance, Heap heap) {
            this(instance, heap, new JLabel(Bundle.BrowserUtils_Loading(), JLabel.CENTER) {
                public void addNotify() { setEnabled(false); }
            });
        }
        
        // [Event Dispatch Thread] Constructor for custom initial UI
        protected View(Instance instance, Heap heap, Component initialView) {
            super(new BorderLayout());
            add(initialView, BorderLayout.CENTER);
            
            this.instance = instance;
            this.heap = heap;
        }
        
        // [Worker Thread] Compute the view here, check Thread.interrupted(),
        // use SwingUtilities.invokeLater() to display the result
        protected abstract void computeView(Instance instance, Heap heap);
        
        public final void addNotify() {
            super.addNotify();
            
            // #241316, this can't be called from constructor!
            workerTask = BrowserUtils.performTask(new Runnable() {
                public void run() {
                    if (!Thread.interrupted()) computeView(instance, heap);
                }
            });
        }
        
        // [Event Dispatch Thread] Do any cleanup here if needed
        protected void removed() {}
        
        public final void removeNotify() {
            workerTask.cancel();
            super.removeNotify();
            removed();
        }
        
    }
    
}
