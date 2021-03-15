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
import java.util.UUID;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.NbBundle;
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
    private static final String COLLECTION_MASK = "java.util.AbstractCollection+";   // NOI18N
    private static final String MAP_MASK = "java.util.AbstractMap+";            // NOI18N
    private static final String A_SET_MASK = "java.util.AbstractSet+";          // NOI18N
    private static final String VECTOR_MASK = "java.util.Vector+";              // NOI18N
    private static final String SET_MASK = "java.util.HashSet+";                 // NOI18N
    private static final String TREESET_MASK = "java.util.TreeSet";             // NOI18N
    private static final String HASHTABLE_MASK = "java.util.Hashtable+";        // NOI18N
    private static final String UUID_MASK = "java.util.UUID";                   // NOI18N
    private static final String UNMOD_COLLECTION_MASK = "java.util.Collections$UnmodifiableCollection+";    // NOI18N
    private static final String UNMOD_MAP_MASK = "java.util.Collections$UnmodifiableMap+";    // NOI18N
    private static final String ARRAYS_LIST_MASK = "java.util.Arrays$ArrayList";       // NOI18N
    private static final String EMPTY_LIST_MASK = "java.util.Collections$EmptyList";   // NOI18N
    private static final String EMPTY_SET_MASK = "java.util.Collections$EmptySet";   // NOI18N
    private static final String EMPTY_MAP_MASK = "java.util.Collections$EmptyMap";   // NOI18N
    private static final String SINGLETON_LIST_MASK = "java.util.Collections$SingletonList";   // NOI18N
    private static final String SINGLETON_SET_MASK = "java.util.Collections$SingletonSet";   // NOI18N
    private static final String SINGLETON_MAP_MASK = "java.util.Collections$SingletonMap";   // NOI18N
    private static final String SYN_COLLECTION_MASK = "java.util.Collections$SynchronizedCollection+";   // NOI18N
    private static final String SYN_MAP_MASK = "java.util.Collections$SynchronizedMap+";   // NOI18N
    private static final String DEQUE_MASK = "java.util.ArrayDeque+";           // NOI18N
    private static final String ENUM_SET_MASK = "java.util.RegularEnumSet";     // NOI18N
    
    private Formatter formatter = new SimpleFormatter();

    public UtilDetailsProvider() {
        super(LOGGER_MASK, LEVEL_MASK, LOCALE_MASK, DATE_MASK, TIMEZONE_MASK,
              PATTERN_MASK, CURRENCY_MASK, ZIPENTRY_MASK, LOGRECORD_MASK,
              ATTR_NAME_MASK, COLLECTION_MASK, MAP_MASK, A_SET_MASK, VECTOR_MASK,
              SET_MASK, TREESET_MASK, HASHTABLE_MASK, UUID_MASK,
              UNMOD_COLLECTION_MASK, UNMOD_MAP_MASK, ARRAYS_LIST_MASK,
              EMPTY_LIST_MASK, EMPTY_MAP_MASK, EMPTY_SET_MASK,
              SINGLETON_LIST_MASK, SINGLETON_MAP_MASK, SINGLETON_SET_MASK,
              SYN_COLLECTION_MASK, SYN_MAP_MASK, DEQUE_MASK, ENUM_SET_MASK);
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
                return String.format("%s, size=%d", name, size);                // NOI18N
            }
            return name;
        } else if (LOGRECORD_MASK.equals(className)) {
            return formatter.format(new DetailsLogRecord(instance, heap));
        } else if (ATTR_NAME_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        } else if (COLLECTION_MASK.equals(className)
                || MAP_MASK.equals(className)) {
            int size = DetailsUtils.getIntFieldValue(instance, "size", -1);  // NOI18N
            if (size != -1) {
                return getElementsString(size);
            }
        } else if (VECTOR_MASK.equals(className)) {
            int elements = DetailsUtils.getIntFieldValue(instance, "elementCount", -1); // NOI18N
            if (elements != -1) {
                return getElementsString(elements);
            }
        } else if (SET_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "map", heap); // NOI18N
        } else if (A_SET_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "this$0", heap); // NOI18N
        } else if (TREESET_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "m", heap);    // NOI18N
        } else if (HASHTABLE_MASK.equals(className)) {
            int elements = DetailsUtils.getIntFieldValue(instance, "count", -1);     // NOI18N
            if (elements != -1) {
                return getElementsString(elements);
            }
        } else if (UUID_MASK.equals(className)) {
            long mostSigBits = DetailsUtils.getLongFieldValue(instance, "mostSigBits", -1);  // NOI18N
            long leastSigBits = DetailsUtils.getLongFieldValue(instance, "leastSigBits", -1);// NOI18N
            if (mostSigBits != -1 && leastSigBits != -1) {
                return new UUID(mostSigBits, leastSigBits).toString();
            }
        } else if (UNMOD_COLLECTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "c", heap);    // NOI18N
        } else if (UNMOD_MAP_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "m", heap);    // NOI18N
        } else if (ARRAYS_LIST_MASK.equals(className)) {
            ObjectArrayInstance arr = (ObjectArrayInstance) instance.getValueOfField("a");  // NOI18N
            if (arr != null) {
               return getElementsString(arr.getLength());
            }
        } else if (EMPTY_LIST_MASK.equals(className)
                || EMPTY_MAP_MASK.equals(className)
                || EMPTY_SET_MASK.equals(className)) {
            return getElementsString(0);
        } else if (SINGLETON_LIST_MASK.equals(className)
                || SINGLETON_MAP_MASK.equals(className)
                || SINGLETON_SET_MASK.equals(className)) {
            return getElementsString(1);
        } else if (DEQUE_MASK.equals(className)) {
            int head = DetailsUtils.getIntFieldValue(instance, "head", -1); // NOI18N
            int tail = DetailsUtils.getIntFieldValue(instance, "tail", -1); // NOI18N
            ObjectArrayInstance arr = (ObjectArrayInstance) instance.getValueOfField("elements");   // NOI18N
            if (head != -1 && tail != -1 && arr != null) {
                int size = (tail - head) & (arr.getLength() - 1);
                return getElementsString(size);
            }
        } else if (SYN_COLLECTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "c", heap);    // NOI18N
        } else if (SYN_MAP_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "m", heap);    // NOI18N
        } else if (ENUM_SET_MASK.equals(className)) {
            Object elements = instance.getValueOfField("elements");             // NOI18N
            if (elements instanceof Long) {
                return getElementsString(Long.bitCount((Long)elements));
            }
        }
        return null;
    }
    
    @NbBundle.Messages({
        "UtilDetailsProvider_OneItemString=1 element",                          // NOI18N
        "UtilDetailsProvider_ItemsNumberString={0} elements"                    // NOI18N
    })
    private static String getElementsString(int length) {
        return length == 1 ? Bundle.UtilDetailsProvider_OneItemString() :
                             Bundle.UtilDetailsProvider_ItemsNumberString(length);
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

                for (Instance o : ((ObjectArrayInstance)pars).getValues()) {
                    String par = null;
                    if (o != null) {
                        par = DetailsUtils.getInstanceString(o, heap);
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
