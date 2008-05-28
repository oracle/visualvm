package org.visualvm.demoapplicationtype;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.application.views.ApplicationViewsSupport;
import com.sun.tools.visualvm.core.ui.DataSourceViewPlugin;
import com.sun.tools.visualvm.core.ui.DataSourceViewPluginProvider;

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
