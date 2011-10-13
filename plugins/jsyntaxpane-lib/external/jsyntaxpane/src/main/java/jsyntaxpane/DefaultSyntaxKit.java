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
package jsyntaxpane;

import java.awt.Color;
import java.util.logging.Level;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import jsyntaxpane.actions.SyntaxAction;
import jsyntaxpane.components.SyntaxComponent;
import jsyntaxpane.util.Configuration;
import jsyntaxpane.util.JarServiceProvider;

/**
 * The DefaultSyntaxKit is the main entry to SyntaxPane.  To use the package, just 
 * set the EditorKit of the EditorPane to a new instance of this class.
 * 
 * You need to pass a proper lexer to the class.
 * 
 * @author ayman
 */
public class DefaultSyntaxKit extends DefaultEditorKit implements ViewFactory {

    public static Font DEFAULT_FONT;
    private static Set<String> CONTENTS = new HashSet<String>();
    private static boolean initialized = false;
    private Lexer lexer;
    private static final Logger LOG = Logger.getLogger(DefaultSyntaxKit.class.getName());
    public static final Pattern COMMA_REGEX = Pattern.compile("\\w+,\\w+");
    private List<SyntaxComponent> editorComponents = new ArrayList<SyntaxComponent>();
    private Map<String, SyntaxAction> editorActions = new HashMap<String, SyntaxAction>();
    /**
     * Main Configuration of JSyntaxPane
     */
    private static Configuration CONFIG;


    static {
        initKit();
    }

    /**
     * Create a new Kit for the given language 
     * @param lexer 
     */
    public DefaultSyntaxKit(Lexer lexer) {
        super();
        this.lexer = lexer;
    }

    @Override
    public ViewFactory getViewFactory() {
        return this;
    }

    @Override
    public View create(Element element) {
        return new SyntaxView(element, CONFIG, this.getClass().getSimpleName());
    }

    /**
     * Install the View on the given EditorPane.  This is called by Swing and
     * can be used to do anything you need on the JEditorPane control.  Here
     * I set some default Actions.
     * 
     * @param editorPane
     */
    @Override
    public void install(JEditorPane editorPane) {
        super.install(editorPane);
        editorPane.setFont(DEFAULT_FONT);
        Keymap km_parent = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
        Keymap km_new = JTextComponent.addKeymap(null, km_parent);
        String kitName = this.getClass().getSimpleName();
        Color caretColor = CONFIG.getPrefixColor(kitName, "CaretColor", Color.BLACK);
        editorPane.setCaretColor(caretColor);
        addSyntaxActions(km_new, ""); // shared actions
        addSyntaxActions(km_new, kitName);
        editorPane.setKeymap(km_new);
        // install the components to the editor:
        String[] components = CONFIG.getPrefixPropertyList(kitName, "Components");
        for (String c : components) {
            try {
                @SuppressWarnings("unchecked")
                Class<SyntaxComponent> compClass = (Class<SyntaxComponent>) Class.forName(c);
                SyntaxComponent comp = compClass.newInstance();
                comp.config(CONFIG, kitName);
                comp.install(editorPane);
                editorComponents.add(comp);
            } catch (InstantiationException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void deinstall(JEditorPane editorPane) {
        for (SyntaxComponent c : editorComponents) {
            c.deinstall(editorPane);
        }
        editorComponents.clear();
    }

    /**
     * Add keyboard actions to this control using the Configuration we have
     * @param map
     * @param prefix 
     */
    public void addSyntaxActions(Keymap map, String prefix) {
        // look at all keys that either start with prefix.Action, or
        // that start with Action.

        Pattern splitter = CONFIG.getValueSeparator(prefix);
        Configuration actionsConf = CONFIG.subConfig(prefix, "Action.");

        for (String actionName : actionsConf.stringPropertyNames()) {
            String[] values = splitter.split(
                    actionsConf.getProperty(actionName));
            String actionClass = values[0];
            SyntaxAction action = editorActions.get(actionClass);
            if (action == null) {
                action = createAction(actionClass);
                action.config(CONFIG, prefix, actionName);
            }
            String keyStrokeString = values[1];
            KeyStroke ks = KeyStroke.getKeyStroke(keyStrokeString);
            // KeyEvent.VK_QUOTEDBL
            if (ks == null) {
                throw new IllegalArgumentException("Invalid KeyStroke: " +
                        keyStrokeString);
            }
            TextAction ta = action.getAction(actionName);
            if(ta == null) {
                throw new IllegalArgumentException("Invalid ActionName: " +
                        actionName);
            }
            map.addActionForKeyStroke(ks, ta);
        }
    }

    private SyntaxAction createAction(String actionClassName) {
        SyntaxAction action = null;
        try {
            Class clazz = JarServiceProvider.loadClass(actionClassName);
            action = (SyntaxAction) clazz.newInstance();
            editorActions.put(actionClassName, action);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException("Cannot create action class: " +
                    actionClassName, ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException("Cannot create action class: " +
                    actionClassName, ex);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Cannot create action class: " +
                    actionClassName, ex);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("Cannot create action class: " +
                    actionClassName, ex);
        }
        return action;
    }

    /**
     * This is called by Swing to create a Document for the JEditorPane document
     * This may be called before you actually get a reference to the control.
     * We use it here to create a proper lexer and pass it to the 
     * SyntaxDcument we return.
     * @return
     */
    @Override
    public Document createDefaultDocument() {
        return new SyntaxDocument(lexer);
    }

    /**
     * This is called to initialize the list of <code>Lexer</code>s we have.
     * You can call  this at initialization, or it will be called when needed.
     * The method will also add the appropriate EditorKit classes to the
     * corresponding ContentType of the JEditorPane.  After this is called,
     * you can simply call the editor.setCOntentType("text/java") on the 
     * control and you will be done.
     */
    public static void initKit() {
        // attempt to find a suitable default font
        CONFIG = new Configuration(JarServiceProvider.readProperties("jsyntaxpane.config"));

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fonts = ge.getAvailableFontFamilyNames();
        Arrays.sort(fonts);
        if (Arrays.binarySearch(fonts, "Courier new") >= 0) {
            DEFAULT_FONT = new Font("Courier New", Font.PLAIN, 12);
        } else if (Arrays.binarySearch(fonts, "Courier") >= 0) {
            DEFAULT_FONT = new Font("Courier", Font.PLAIN, 12);
        } else if (Arrays.binarySearch(fonts, "Monospaced") >= 0) {
            DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 13);
        }

        // read the Default Kits and their associated types
        Properties kitsForTypes = JarServiceProvider.readProperties("jsyntaxpane.kitsfortypes");
        for (String type : kitsForTypes.stringPropertyNames()) {
            String classname = kitsForTypes.getProperty(type);
            registerContentType(type, classname);
        }

        initialized = true;
    }

    /**
     * Register the given content type to use the given class name as its kit
     * When this is called, an entry is added into the private HashMap of the
     * registered editors kits.  This is needed so that the SyntaxPane library
     * has it's own registration of all the EditorKits
     * @param type
     * @param classname
     */
    public static void registerContentType(String type, String classname) {
        JEditorPane.registerEditorKitForContentType(type, classname);
        CONTENTS.add(type);
    }

    /**
     * Return all the content types supported by this library.  This will be the
     * content types in the file WEB-INF/services/resources/jsyntaxpane.kitsfortypes
     * @return sorted array of all registered content types
     */
    public static String[] getContentTypes() {
        String[] types = CONTENTS.toArray(new String[0]);
        Arrays.sort(types);
        return types;
    }

    /**
     * returns the current config
     * @return
     */
    public static Configuration getConfig() {
        if (!initialized) {
            initKit();
        }
        return CONFIG;
    }

    /**
     * Merges the given properties with the defaults, which are read from the
     * Jar file
     * @param config
     */
    public static void setConfig(Properties config) {
        DefaultSyntaxKit.CONFIG.putAll(config);
    }

    /**
     * Sets the given property to the given value.  If the kit is not
     * initialized,  then calls initKit
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        if (!initialized) {
            initKit();
        }
        CONFIG.put(key, value);
    }

    /**
     * Return the property with the given key.  If the kit is not
     * initialized,  then calls initKit
     * Be careful when changing property as the default property may be used 
     * @param key
     * @return value for given key
     */
    public static String getProperty(String key) {
        if (!initialized) {
            initKit();
        }
        return CONFIG.getProperty(key);
    }
}
