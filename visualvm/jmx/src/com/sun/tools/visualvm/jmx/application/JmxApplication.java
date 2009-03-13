/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.visualvm.jmx.application;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.host.Host;
import com.sun.tools.visualvm.jmx.JmxApplicationException;
import com.sun.tools.visualvm.jmx.JmxApplicationsSupport;
import com.sun.tools.visualvm.jmx.PasswordAuthJmxEnvironmentFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.io.File;
import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.Map;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.NetBeansProfiler;

/**
 * This type of application represents an application
 * that is built from a {@link JMXServiceURL}.
 *
 * @author Luis-Miguel Alventosa
 * @author Michal Bachorik
 */
public final class JmxApplication extends Application {

    private int pid = UNKNOWN_PID;
    private final JMXServiceURL url;
    private final String username;
    private final String password;
    private final boolean saveCredentials;
    private final Storage storage;
    private final Map<String, Object> env;
    // since getting JVM for the first time can take a long time
    // hard reference jvm from application so we are sure that it is not garbage collected
    Jvm jvm;

    /**
     * A constructor that creates a JmxApplication using password-based
     * authenticated connection.
     *
     * @param host a host that will own the application
     * @param url an instance of JMXServiceURL used for creation of connection
     * @param username an username for connection authentication
     * @param password a password for connection authentication
     * @param saveCredentials if true, credentials will be persisted
     * @param storage may be null, in this case the JmxApplication isn't persistent
     * and creates a temporary storage just like any other regular Application
     */
    public JmxApplication(Host host, JMXServiceURL url, final String username,
            final String password, boolean saveCredentials, Storage storage) {
        super(host, url.toString() + (username == null || username.isEmpty() ? "" : " (" + username + ")"));
        this.url = url;
        this.username = username;
        this.password = password;
        Map<String, Object> tmpEnv = Collections.<String, Object>emptyMap();

        try {
            tmpEnv = PasswordAuthJmxEnvironmentFactory.getSharedInstance().createJmxEnvironment(new CallbackHandler() {

                public void handle(Callback[] callbacks) {
                    for (Callback c : callbacks) {
                        if (c instanceof NameCallback) {
                            NameCallback ncb = (NameCallback) c;
                            ncb.setName(username);
                        } else if (c instanceof PasswordCallback) {
                            PasswordCallback pcb = (PasswordCallback) c;
                            pcb.setPassword(password.toCharArray());
                        }
                        // do not care about other callbacks, they are not needed for this helper method
                    }
                }
            });
        } catch (final JmxApplicationException ex) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    NetBeansProfiler.getDefaultNB().displayError(ex.getMessage());    // NOI18N
                }
            });
        }
        this.env = tmpEnv;
        this.saveCredentials = saveCredentials;
        this.storage = storage;
    }

    /**
     * A constructor that creates a JmxApplication. Supports any type of
     * authentication through mechanism of JmxEnvironmentFactory.
     *
     * For example, let's assume that JMX server expects a certificate for authentication,
     * which is then used by custom 'MyCertAuthenticator'.
     *
     * Following 2 classes are basis:
     * <code>
     * class CertCallback implements Callback {
     *      X509Certificate cert;
     *
     *      CertCallback(...) {
     *        // do all needed stuff, like initialization of keystore access etc.
     *      }
     *
     *      public void setCertificate(X509Certificate aCert) {
     *          cert = aCert;
     *      }
     *
     *      public X509Certificate getCertificate() {
     *          return cert;
     *      }
     * }
     *
     * class MyCertAuthJmxEnvironmentFactory implements JmxEnvironmentFactory {
     *
     *  // ...
     *
     *  public Map<String, Object> createJmxClientContext(CallbackHandler ch) {
     *
     *      CertCallback certCallback = new CertCallback(...);
     *      Callback[] cb = new Callback[]{certCallback};
     *      try {
     *          ch.handle(cb);
     *      } catch (IOException ex) {
     *          // do something if needed
     *      } catch (UnsupportedCallbackException ex) {
     *          // do something if needed
     *      }
     *      Map<String, Object> cred = new HashMap<String, Object>(1);
     *      if (certCallback.getCertificate() != null) {
     *          cred.put(MyCertAuthenticator.CERTIFICATE, certCallback.getCertificate());
     *      }
     *
     *      Map<String, Object> environment = new HashMap<String, Object>(1);
     *      if (certCallback != null) {
     *          environment.put(JMXConnector.CREDENTIALS, cred);
     *      }
     *      return environment;
     *  }
     * }
     * </code>
     * Then, it is needed to implement a CallbackHandler that knows how to
     * work with CertCallback. Implementation of CallbackHandler can get
     * needed information either interactively or silently. For example,
     * something like this is is possible:
     * <code>
     * public final class ApplicationSecurityConfigurator extends JPanel implements CallbackHandler {
     *
     *  // create UI to get the data from an user, using dialogs etc.
     *
     *  private X509Certificate getCertificate() {
     *      // process data from user (e.g. path to keystore, and keystore passphrase)
     *      // and return a certificate stored in keystore
     *  }
     *
     *  private void handle(Callback cb) throws IOException, UnsupportedCallbackException {
     *      if (cb instanceof CertCallback) {
     *          CertCallback cCallback = (CertCallback) cb;
     *          cCallback.setCertificate(this.getCertificate());
     *      } else {
     *          throw new UnsupportedCallbackException(cb);
     *      }
     *  }
     *
     *  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
     *      for (Callback cb : callbacks) {
     *          handle(cb);
     *      }
     *  }
     * }
     * </code>
     *
     * To make it work all-together is quite easy. It is needed to create an
     * instance of ApplicationSecurityConfigurator, which is then passed
     * to MyCertAuthJmxEnvironmentFactory.createJmxClientContext(CallbackHandler ch).
     * The above method call returns instance of 'Map<String, Object>' that
     * can be used as 'env' paramate to the constructor.
     *
     *
     * @param host a host that will own the application
     * @param url an instance of JMXServiceURL used for creation of connection
     * @param env a map holding the credentials needed by JMX authenticator
     * @param saveCredentials if true, credentials will be persisted
     * @param storage may be null, in this case the JmxApplication isn't persistent
     * and creates a temporary storage just like any other regular Application
     */
    public JmxApplication(Host host, JMXServiceURL url, Map<String, Object> env,
            boolean saveCredentials, Storage storage) {
        super(host, url.toString());
        this.url = url;
        this.env = env;
        this.username = null;
        this.password = null;
        this.saveCredentials = saveCredentials;
        this.storage = storage;
    }

    void setStateImpl(int newState) {
        if (newState != Stateful.STATE_AVAILABLE) {
            pid = UNKNOWN_PID;
            jvm = null;
        }
        setState(newState);
    }

    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    public Map<String, Object> getEnvironment() {
        return env;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean getSaveCredentialsFlag() {
        return saveCredentials;
    }

    @Override
    public int getPid() {
        if (pid == UNKNOWN_PID) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(this);
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                if (mxbeans != null) {
                    RuntimeMXBean rt = mxbeans.getRuntimeMXBean();
                    if (rt != null) {
                        String name = rt.getName();
                        if (name != null && name.indexOf("@") != -1) {
                            name = name.substring(0, name.indexOf("@"));
                            pid = Integer.parseInt(name);
                        }
                    }
                }
            }
        }
        return pid;
    }

    @Override
    public boolean supportsUserRemove() {
        return true;
    }

    @Override
    protected Storage createStorage() {
        if (storage == null) {
            File directory = Utils.getUniqueFile(JmxApplicationsSupport.getStorageDirectory(),
                        "" + System.currentTimeMillis(), JmxApplicationProvider.JMX_SUFFIX);    // NOI18N
            return new Storage(directory);
        }
        return storage;
    }

    protected void remove() {
        File appStorage = getStorage().getDirectory();
        if (appStorage.isDirectory()) Utils.delete(appStorage, true);
        }

    @Override
    public String toString() {
        return "JmxApplication [id: " + getId() + "]";   // NOI18N
    }
}
