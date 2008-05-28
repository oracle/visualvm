package org.netbeans.helloworldplugin;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceViewPlugin;
import com.sun.tools.visualvm.core.ui.DataSourceViewPluginProvider;
import org.hellovisualvm.HelloWorldViewSupport;

class HelloWorldViewPluginProvider extends DataSourceViewPluginProvider<Application> {

    protected DataSourceViewPlugin createPlugin(Application application) {
        return new HelloWorldViewPlugin(application);
    }

    protected boolean supportsPluginFor(Application application) {
        return true;
    }

    static void initialize() {
      HelloWorldViewSupport.getInstance().getApplicationPluggableView().registerPluginProvider(new HelloWorldViewPluginProvider());
    }

    static void uninitialize() {
        HelloWorldViewSupport.getInstance().getApplicationPluggableView().unregisterPluginProvider(new HelloWorldViewPluginProvider());
    }
}
