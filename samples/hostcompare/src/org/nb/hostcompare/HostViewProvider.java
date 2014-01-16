
package org.nb.hostcompare;

import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.PluggableDataSourceViewProvider;
import com.sun.tools.visualvm.host.Host;
import java.util.Set;

public class HostViewProvider extends PluggableDataSourceViewProvider<Host> {

    private static DataSourceViewProvider<Host> instance = new HostViewProvider();

    @Override
    public boolean supportsViewFor(Host MyHost) {
        //Always shown:
        return true;
    }

    @Override
    public synchronized DataSourceView createView(final Host MyHost) {
        return new HostView(MyHost);
    }

    static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(instance, Host.class);
    }

    static void unregister() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(instance);
    }

    @Override
    public Set<Integer> getPluggableLocations(DataSourceView arg0) {
        return ALL_LOCATIONS;
    }
}

