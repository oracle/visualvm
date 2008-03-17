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
 *
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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import org.openide.util.NbBundle;
import org.openide.ErrorManager;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * The welcome screen.
 * @author  Richard Gregor, S. Aubrecht
 */
public class WelcomeComponent extends TopComponent {
    static final long serialVersionUID=6021472310161712674L;
    private static WeakReference<WelcomeComponent> component =
                new WeakReference<WelcomeComponent>(null); 
    private JComponent content;

    private boolean initialized = false;
    
    private WelcomeComponent(){
        setLayout(new BorderLayout());
        setName(NbBundle.getMessage(WelcomeComponent.class, "LBL_Tab_Title"));   //NOI18N
        content = null;
        initialized = false;
        putClientProperty( "activateAtStartup", Boolean.TRUE ); //NOI18N
    }
    
    @Override protected String preferredID(){
        return "VisualVMWelcomeComponent";    //NOI18N
    }
    
    /**
     * #38900 - lazy addition of GUI components
     */    
    
    private void doInitialize() {
        initAccessibility();
        
        if( null == content ) {
            content = new StartPageContent();

            add( content, BorderLayout.CENTER );
            setFocusable( false );
        }
    }
        
    /* Singleton accessor. As WelcomeComponent is persistent singleton this
     * accessor makes sure that WelcomeComponent is deserialized by window system.
     * Uses known unique TopComponent ID "Welcome" to get WelcomeComponent instance
     * from window system. "Welcome" is name of settings file defined in module layer.
     */
    public static WelcomeComponent findComp() {
        WelcomeComponent wc = component.get();
        if (wc == null) {
            TopComponent tc = WindowManager.getDefault().findTopComponent("VisualVMWelcome"); // NOI18N
            if (tc != null) {
                if (tc instanceof WelcomeComponent) {
                    wc = (WelcomeComponent)tc;
                    component = new WeakReference<WelcomeComponent>(wc); 
                } else {
                    //Incorrect settings file?
                    IllegalStateException exc = new IllegalStateException
                    ("Incorrect settings file. Unexpected class returned." // NOI18N
                    + " Expected:" + WelcomeComponent.class.getName() // NOI18N
                    + " Returned:" + tc.getClass().getName()); // NOI18N
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, exc);
                    //Fallback to accessor reserved for window system.
                    wc = WelcomeComponent.createComp();
                }
            } else {
                //WelcomeComponent cannot be deserialized
                //Fallback to accessor reserved for window system.
                wc = WelcomeComponent.createComp();
            }
        }       
        return wc;
    }
    
    /* Singleton accessor reserved for window system ONLY. Used by window system to create
     * WelcomeComponent instance from settings file when method is given. Use <code>findComp</code>
     * to get correctly deserialized instance of WelcomeComponent. */
    public static WelcomeComponent createComp() {
        WelcomeComponent wc = component.get();
        if(wc == null) {
            wc = new WelcomeComponent();
            component = new WeakReference<WelcomeComponent>(wc);
        }
        return wc;
    }
    
    /** Overriden to explicitely set persistence type of WelcomeComponent
     * to PERSISTENCE_ALWAYS */
    @Override public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }
    
    private void initAccessibility(){
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WelcomeComponent.class, "ACS_Welcome_DESC")); // NOI18N
    }

    /**
     * Called when <code>TopComponent</code> is about to be shown.
     * Shown here means the component is selected or resides in it own cell
     * in container in its <code>Mode</code>. The container is visible and not minimized.
     * <p><em>Note:</em> component
     * is considered to be shown, even its container window
     * is overlapped by another window.</p>
     * @since 2.18
     *
     * #38900 - lazy addition of GUI components
     *
     */
    @Override protected void componentShowing() {
        if (!initialized) {
            initialized = true;
            doInitialize();
        }
        if( null != content && getComponentCount() == 0 ) {
            //notify components down the hierarchy tree that they should 
            //refresh their content (e.g. RSS feeds)
            add( content, BorderLayout.CENTER );
        }
        super.componentShowing();
//        setActivatedNodes( new Node[] {} );
    }

    private static boolean firstTimeOpen = true;
    @Override 
    protected void componentOpened() {
        super.componentOpened();
        if( firstTimeOpen ) {
            firstTimeOpen = false;
            if( !WelcomeOptions.getDefault().isShowOnStartup() ) {
                close();
            }
        }
    }
    
    @Override protected void componentHidden() {
        super.componentHidden();
        if( null != content ) {
            //notify components down the hierarchy tree that they no long 
            //need to periodically refresh their content (e.g. RSS feeds)
            remove( content );
        }
    }

    @Override
    public void requestFocus() {
        if( null != content )
            content.requestFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        if( null != content )
            return content.requestFocusInWindow();
        return super.requestFocusInWindow();
    }
}

