package org.visualvm.demoapplicationtype.overview;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.application.views.ApplicationViewsSupport;
import com.sun.tools.visualvm.core.ui.DataSourceViewPlugin;
import com.sun.tools.visualvm.core.ui.DataSourceViewPluginProvider;
import org.visualvm.demoapplicationtype.applicationtype.AnagramApplicationType;
import java.util.Collections;
import java.util.Set;

public class AnagramOverviewProvider extends DataSourceViewPluginProvider<Application> {

    private static final AnagramOverviewProvider INSTANCE = new AnagramOverviewProvider();


    protected boolean supportsPluginFor(Application application) {
        if (ApplicationTypeFactory.getApplicationTypeFor(application) instanceof AnagramApplicationType) {
            return true;
        }
        return false;
    }
    
    public DataSourceViewPlugin createPlugin(Application application) {
            return new AnagramOverviewPlugin(application);
    }

    public static void initialize() {
        ApplicationViewsSupport.sharedInstance().getOverviewView().registerPluginProvider(INSTANCE);
    }

    public static void shutdown() {
        ApplicationViewsSupport.sharedInstance().getOverviewView().unregisterPluginProvider(INSTANCE);
    }

}
