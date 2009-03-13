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

package com.sun.tools.visualvm.jmx;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * A interface defining the factory for constructing a JMX environment.
 * A typicaly to be used with custom CallbackHandler and JmxApplicationsSupport.
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
 * To create a JMX application is then quite straightforward - it is enough
 * to pass instance MyCertAuthJmxEnvironmentFactory and of ApplicationSecurityConfigurator
 * to JmxAaplicationsSupport:
 * <code>
 * Application jmxApp = JmxApplicationsSupport.getInstance().createJmxApplication(connectionString,
            displayName, new MyCertAuthJmxEnvironmentFactory(),
            new ApplicationSecurityConfigurator(), saveCredentials,
            persistent)
 * </code>
 *
 * @author mb199851
 */
public interface JmxEnvironmentFactory {

    /**
     * Factory method for creating a JMX environment.
     *
     * @param callback a callbackhandler able to provide information needed to create
     * JMX environment
     * @return a map of objects describing JMX environment
     * @throws com.sun.tools.visualvm.jmx.JmxApplicationException if JMX environment
     * can not be created
     */
    public Map<String, Object> createJmxEnvironment(CallbackHandler callback) throws JmxApplicationException;
}
