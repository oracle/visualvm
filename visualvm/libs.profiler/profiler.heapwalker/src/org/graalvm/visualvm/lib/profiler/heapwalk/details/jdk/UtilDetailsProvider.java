/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class UtilDetailsProvider extends DetailsProvider.Basic {
    
    private static final String LOGGER_MASK = "java.util.logging.Logger+";      // NOI18N
    private static final String LEVEL_MASK = "java.util.logging.Level+";        // NOI18N
    private static final String LOCALE_MASK = "java.util.Locale";               // NOI18N
    private static final String DATE_MASK = "java.util.Date+";                  // NOI18N
    private static final String TIMEZONE_MASK = "java.util.TimeZone+";          // NOI18N
    private static final String PATTERN_MASK = "java.util.regex.Pattern";       // NOI18N
    private static final String CURRENCY_MASK = "java.util.Currency";           // NOI18N
    private static final String ZIPENTRY_MASK = "java.util.zip.ZipEntry+";      // NOI18N
    private static final String LOGRECORD_MASK = "java.util.logging.LogRecord"; // NOI18N
    private static final String ATTR_NAME_MASK = "java.util.jar.Attributes$Name";    // NOI18N
    
    private Formatter formatter = new SimpleFormatter();

    public UtilDetailsProvider() {
        super(LOGGER_MASK, LEVEL_MASK, LOCALE_MASK, DATE_MASK, TIMEZONE_MASK,
              PATTERN_MASK, CURRENCY_MASK, ZIPENTRY_MASK, LOGRECORD_MASK,
              ATTR_NAME_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (LOGGER_MASK.equals(className) ||
            LEVEL_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        } else if (LOCALE_MASK.equals(className)) {
            String language = DetailsUtils.getInstanceFieldString(
                    instance, "language", heap);                                // NOI18N
            if (language == null) language = "";                                // NOI18N
            String country = DetailsUtils.getInstanceFieldString(
                    instance, "country", heap);                                 // NOI18N
            if (country == null) country = "";                                  // NOI18N
            if (!language.isEmpty() || !country.isEmpty()) {
                if (language.isEmpty() || country.isEmpty())
                    return language + country;
                else
                    return language + "_" + country;                            // NOI18N
            }
        } else if (DATE_MASK.equals(className)) {
            long fastTime = DetailsUtils.getLongFieldValue(
                    instance, "fastTime", -1);                                  // NOI18N
            return new Date(fastTime).toString();
        } else if (TIMEZONE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(
                    instance, "ID", heap);                                      // NOI18N
        } else if (PATTERN_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(
                    instance, "pattern", heap);                                 // NOI18N
        } else if (CURRENCY_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(
                    instance, "currencyCode", heap);                            // NOI18N
        } else if (ZIPENTRY_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(
                    instance, "name", heap);                                    // NOI18N
            long size = DetailsUtils.getLongFieldValue(
                    instance, "size", -1);                                      // NOI18N
            if (name != null && size != -1) {
                return String.format("%s, size=%d", name, size);               // NOI18N
            }
            return name;
        } else if (LOGRECORD_MASK.equals(className)) {
            return formatter.format(new DetailsLogRecord(instance, heap));
        } else if (ATTR_NAME_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        return null;
    }
    
    private class DetailsLogRecord extends LogRecord {
        private final Instance record;
        private final Heap heap;

        private DetailsLogRecord(Instance rec, Heap h) {
            super(Level.ALL, null);
            record = rec;
            heap = h;
        }

        @Override
        public long getMillis() {
            Object time = record.getValueOfField("millis");
            if (time instanceof Number) {
                return ((Number)time).longValue();
            }
            return 0;
        }

        @Override
        public String getSourceClassName() {
            return DetailsUtils.getInstanceFieldString(record, "sourceClassName", heap);    // NOI18N
        }

        @Override
        public String getSourceMethodName() {
            return DetailsUtils.getInstanceFieldString(record, "sourceMethodName", heap);   // NOI18N
        }

        @Override
        public String getLoggerName() {
            return DetailsUtils.getInstanceFieldString(record, "loggerName", heap); // NOI18N
        }

        @Override
        public String getMessage() {
            return DetailsUtils.getInstanceFieldString(record, "message", heap);    // NOI18N
        }

        @Override
        public Object[] getParameters() {
            Object pars = record.getValueOfField("parameters");
            if (pars instanceof ObjectArrayInstance) {
                List parameters = new ArrayList();

                for (Object o : ((ObjectArrayInstance)pars).getValues()) {
                    String par = null;
                    if (o instanceof Instance) {
                        par = DetailsUtils.getInstanceString((Instance) o, heap);
                    }
                    if (par == null) par = "";
                    parameters.add(par);
                }
                return parameters.toArray();
            }
            return null;
        }

        @Override
        public Level getLevel() {
            String level = DetailsUtils.getInstanceFieldString(record, "level", heap);  // NOI18N
            return Level.parse(level);
        }
    }
}
