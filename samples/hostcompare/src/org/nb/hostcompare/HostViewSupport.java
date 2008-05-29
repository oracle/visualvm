
package org.nb.hostcompare;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.PluggableDataSourceViewProvider;

public class HostViewSupport {

    private static HostViewSupport instance;
    private HostViewProvider HostPluggableView = new HostViewProvider();

    public static synchronized HostViewSupport getInstance() {
        if (instance == null) {
            instance = new HostViewSupport();
        }
        return instance;
    }

    public PluggableDataSourceViewProvider getApplicationPluggableView() {
        return HostPluggableView;
    }

    public HostViewSupport() {
        DataSourceViewsManager.sharedInstance().addViewProvider(HostPluggableView, Application.class);
    }
}

