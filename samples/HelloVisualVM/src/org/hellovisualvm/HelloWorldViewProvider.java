/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hellovisualvm;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author geertjan
 */
public class HelloWorldViewProvider implements DataSourceViewsProvider<Application> {

    private Map<Application, DataSourceView> viewsCache = new HashMap();
    private static DataSourceViewsProvider<Application> instance =  new HelloWorldViewProvider();

    @Override
    public boolean supportsViewsFor(Application application) {
        //Always shown:
        return true;
    }

    @Override
    public synchronized Set getViews(final Application application) {
        
        DataSourceView view = viewsCache.get(application);
        
        if (view == null) {
            view = new HelloWorldView(application) {
                @Override
                public void removed() {
                    super.removed();
                    viewsCache.remove(application);
                }
            };
            viewsCache.put(application, view);
        }
        
        return Collections.singleton(view);
    }

    static void initialize() {
        DataSourceWindowFactory.sharedInstance().addViewProvider(instance, Application.class);
    }
    
    static void unregister() {
        DataSourceWindowFactory.sharedInstance().removeViewProvider(instance);
    }

   
}