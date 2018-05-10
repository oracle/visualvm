
package org.nb.hostcompare;

import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceViewProvider;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;
import org.graalvm.visualvm.host.Host;
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

