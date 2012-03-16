/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hellovisualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.PluggableDataSourceViewProvider;

/**
 *
 * @author geertjan
 */
public class HelloWorldViewSupport {

    private static HelloWorldViewSupport instance;
    
    private HelloWorldViewProvider helloWorldPluggableView = new HelloWorldViewProvider();
    

    public static synchronized HelloWorldViewSupport getInstance() {
        if (instance == null) {
            instance = new HelloWorldViewSupport();
        }
        return instance;
    }

    public PluggableDataSourceViewProvider getApplicationPluggableView() {
        return helloWorldPluggableView;
    }

    public HelloWorldViewSupport() {
        DataSourceViewsManager.sharedInstance().addViewProvider(helloWorldPluggableView, Application.class);
    }
    
}
