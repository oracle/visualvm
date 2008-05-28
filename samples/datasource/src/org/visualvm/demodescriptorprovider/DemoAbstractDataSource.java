package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.datasource.DataSource;

public class DemoAbstractDataSource extends DataSource {

    private static DemoAbstractDataSource sharedInstance;

    public static synchronized DemoAbstractDataSource sharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new DemoAbstractDataSource();
        }
        return sharedInstance;
    }
    
}
