/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsyntaxpane.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Wrapper around the Properties class with more functionality
 * This is mainly needed to provide easier support for getting values by an
 * optional prefix.
 * 
 * @author Ayman Al-Sairafi
 */
public class Configuration extends Properties {
    final private Map<String, Pattern> valueSeparatorMap = new HashMap<String, Pattern>();

    public Configuration(Properties defaults) {
        super(defaults);
    }

    private Configuration() {
        super();
    }

    /**
     * Gets the String value for the key prefix.key, or key, or default
     * @param prefix
     * @param key
     * @param Default
     * @return
     */
    public String getPrefixProperty(String prefix, String key, String Default) {
        String v = super.getProperty(prefix + "." + key);
        if (v != null) {
            return v;
        }
        return super.getProperty(key, Default);
    }

    /**
     * Gets a prefixed integer from the properties.  If number cannot be found
     * or if it cannot be decoded, the default is returned
     * The integer is decoded using {@link Integer.decode(String)}
     * @param prefix
     * @param key
     * @param Default
     * @return
     */
    public int getPrefixInteger(String prefix, String key, int Default) {
        String v = getPrefixProperty(prefix, key, null);
        if (v == null) {
            return Default;
        }
        try {
            int i = Integer.decode(v);
            return i;
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, null, e);
            return Default;
        }
    }

    /**
     * Returns a String[] of the comma separated items in the value for
     * prefix.key or key
     * Does NOT return null.  If the prefix.key or key value is not found,
     * then an empty string array is returned.  So the return of this method
     * can be used directly in a foreach loop
     * @param prefix
     * @param key
     * @return non-null String[] 
     */
    public String[] getPrefixPropertyList(String prefix, String key) {
        String v = getProperty(prefix + "." + key);
        if (v == null) {
            v = getProperty(key);
        }
        if (v == null) {
            return EMPTY_LIST;
        }
        return getValueSeparator(prefix).split(v);
    }

    /**
     * Returns a boolean from the configuration
     * @param prefix
     * @param key
     * @param Default
     * @return
     */
    public boolean getPrefixBoolean(String prefix, String key, boolean Default) {
        String b = getPrefixProperty(prefix, key, null);
        if (b == null) {
            return Default;
        }
        return Boolean.parseBoolean(b.trim());
    }

    /**
     * return the COlor that has the given key = prefix.key or key = key or
     * default, in that order
     * @param prefix
     * @param key
     * @param Default
     * @return
     */
    public Color getPrefixColor(String prefix, String key, Color Default) {
        String c = getPrefixProperty(prefix, key, null);
        if (c == null) {
            return Default;
        }
        try {
            return Color.decode(c);
        } catch (NumberFormatException e) {
            return Default;
        }
    }

    /**
     * Return a sub configuration from this instance that has the keys equal to
     * either prefix.keyPrefix or keyPrefix.  The entries of keyPrefix are
     * added first, so they are the defaults if prefix is not found.
     * 
     * @param prefix
     * @param keyPrefix
     * @return
     */
    public Configuration subConfig(String prefix, String keyPrefix) {
        Configuration sub = new Configuration();
        addToSubConf(sub, prefix.length() > 0 ? prefix + "." + keyPrefix : keyPrefix);
        return sub;
    }

    private void addToSubConf(Configuration subConf, String prefix) {
        int prefixLen = prefix.length();
        for (String k : stringPropertyNames()) {
            if (k.startsWith(prefix)) {
                subConf.put(k.substring(prefixLen), getProperty(k));
            }
        }
    }

    public Pattern getValueSeparator(String prefix) {
        synchronized(valueSeparatorMap) {
            Pattern val = valueSeparatorMap.get(prefix);
            if (val == null) {
                val = Pattern.compile("\\s*" + getPrefixProperty(prefix, "ValueSeparator", COMMA_SEPARATOR) + "\\s*");
                valueSeparatorMap.put(prefix, val);
            }
            return val;
        }
    }

    public static final String[] EMPTY_LIST = new String[0];
    public static final String COMMA_SEPARATOR = ",";
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());
}
