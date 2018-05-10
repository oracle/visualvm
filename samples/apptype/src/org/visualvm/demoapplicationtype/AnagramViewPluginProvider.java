package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.application.views.ApplicationViewsSupport;
import org.graalvm.visualvm.core.ui.DataSourceViewPlugin;
import org.graalvm.visualvm.core.ui.DataSourceViewPluginProvider;

/**
 *
 * @author Jiri Sedlacek
 */
class AnagramViewPluginProvider extends DataSourceViewPluginProvider<Application> {

    protected DataSourceViewPlugin createPlugin(Application application) {
        return new AnagramOverview(application);
    }

    protected boolean supportsPluginFor(Application application) {
        if (ApplicationTypeFactory.getApplicationTypeFor(application) instanceof AnagramApplicationType) {
            return true;
        }
        return false;
    }

    static void initialize() {
        ApplicationViewsSupport.sharedInstance().getOverviewView().
                registerPluginProvider(new AnagramViewPluginProvider());
    }

    static void uninitialize() {
        ApplicationViewsSupport.sharedInstance().getMonitorView().unregisterPluginProvider(new AnagramViewPluginProvider());
    }
}
