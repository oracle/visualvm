
package org.nb.hostcompare;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;

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

