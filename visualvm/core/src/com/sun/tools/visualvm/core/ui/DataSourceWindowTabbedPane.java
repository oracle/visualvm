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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.Exceptions;

/**
 * Copy of org.openide.awt.CloseButtonTabbedPane allowing to control if tabs can be closed or not
 *
 * @author Tran Duc Trung
 * @author S. Aubrecht
 * @author Jiri Sedlacek (copied from org.openide.awt.CloseButtonTabbedPane)
 *
 */
class DataSourceWindowTabbedPane extends JTabbedPane {
  
  private Image closeTabImage;
  private Image closeTabPressedImage;
  private Image closeTabMouseOverImage;
  
  static final String PROP_CLOSE = "close";  // NOI18N
  
  
  DataSourceWindowTabbedPane() {
    addChangeListener( new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            reset();
        }
    });
    CloseButtonListener.install();
    //Bugfix #28263: Disable focus.
    setFocusable(false);
    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new CBTPPolicy());
  }
  
  private Component sel() {
      Component c = getSelectedComponent();
      return c == null ? this : c;
  }

  private class CBTPPolicy extends FocusTraversalPolicy {
      public Component getComponentAfter(Container aContainer, Component aComponent) {
          return sel();
      }

      public Component getComponentBefore(Container aContainer, Component aComponent) {
          return sel();
      }

      public Component getFirstComponent(Container aContainer) {
          return sel();
      }

      public Component getLastComponent(Container aContainer) {
          return sel();
      }

      public Component getDefaultComponent(Container aContainer) {
          return sel();
      }
  }
  
  
  private int pressedCloseButtonIndex = -1;
  private int mouseOverCloseButtonIndex = -1;
  private boolean draggedOut = false;
  
  public void addViewTab(DataSource dataSource, DataSourceView view) {
      ViewContainer container = new ViewContainer(new DataSourceCaption(dataSource), view);
      super.add(container);
      boolean notGTK = !UIUtils.isGTKLookAndFeel();
      setTitleAt(getComponentCount() - 1, view.getName() + ((view.isClosable() && notGTK) ? "  " : ""));  // NOI18N
      ImageIcon tabIcon = notGTK ? new ImageIcon(view.getImage()) :
          new ImageIcon(view.getImage()) {
              public int getIconWidth() { return super.getIconWidth() + 12; }
          };
      super.setIconAt(getComponentCount() - 1, tabIcon);
  }
  
  public void removeTabAt(int index) {
      ViewContainer container = (ViewContainer)getComponentAt(index);
      super.removeTabAt(index);
      container.getCaption().finish();
  }
  
  public DataSourceView getDataSourceView(ViewContainer container) {
      return container.getView();
  }
  
  public int indexOfView(final DataSourceView view) {
      final int[] index = new int[1];
      index[0] = -1;
      IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
          public void run() {
              for (int i = 0; i < getTabCount(); i++)
                  if (((ViewContainer)getComponentAt(i)).getViewComponent() == view.getView()) index[0] = i;
          }
      });
      return index[0];
  }
  
  public Set<DataSourceView> getViews() {
      Set<DataSourceView> views = new HashSet();
      
      for (Component component : getComponents()) {
          ViewContainer container = (ViewContainer)component;
          views.add(container.getView());
      }
      
      return views;
  }
  
  // NOTE: has to be allowed, is called from super.add() needed in addViewTab()
  public void addTab(String title, Component component) {
    if (component instanceof ViewContainer) super.addTab(title, component);
    else throw new RuntimeException("Not supported for this component");    // NOI18N
  }
  
  public Component add(Component component) { throw new RuntimeException("Not supported for this component"); } // NOI18N
  public Component add(Component component, int index) { throw new RuntimeException("Not supported for this component"); }  // NOI18N
  public void add(Component component, Object constraints) { throw new RuntimeException("Not supported for this component"); }  // NOI18N
  public void add(Component component, Object constraints, int index) { throw new RuntimeException("Not supported for this component"); }   // NOI18N
  public Component add(String title, Component component) { throw new RuntimeException("Not supported for this component"); }   // NOI18N
  public void addTab(String title, Icon icon, Component component) { throw new RuntimeException("Not supported for this component"); }  // NOI18N
  public void addTab(String title, Icon icon, Component component, String tip) { throw new RuntimeException("Not supported for this component"); }  // NOI18N
  
  
  public void setTitleAt(int idx, String title) {
    ViewContainer container = (ViewContainer)getComponentAt(idx);
    if (container.getView().isClosable()) {
      String nue = title.indexOf("</html>") != -1 ? // NOI18N
        Utilities.replaceString(title, "</html>", "&nbsp;&nbsp;</html>") // NOI18N
        : title + "  "; // NOI18N
      if (!title.equals(getTitleAt(idx))) {
        super.setTitleAt(idx, nue);
      }
    } else {
      super.setTitleAt(idx, title);
    }
  }
  
  private void reset() {
    setMouseOverCloseButtonIndex(-1);
    setPressedCloseButtonIndex(-1);
    draggedOut = false;
  }
  
  private Rectangle getCloseButtonBoundsAt(int i) {
    Rectangle b = getBoundsAt(i);
    if (b == null)
      return null;
    else {
      ViewContainer container = (ViewContainer)getComponentAt(i);
      if (!container.getView().isClosable()) return null;
      
      b = new Rectangle(b);
      fixGetBoundsAt(b);
      
      Dimension tabsz = getSize();
      if (b.x + b.width >= tabsz.width
        || b.y + b.height >= tabsz.height)
        return null;
      // bugfix #110654
      if (b.width == 0 || b.height == 0) {
        return null;
      }
      if( (isWindowsVistaLaF() || isWindowsXPLaF() || isWindowsLaF()) && i == getSelectedIndex() ) {
        b.x -= 3;
        b.y -= 2;
      } else if( isWindowsXPLaF() || isWindowsLaF() || isAquaLaF() ) {
        b.x -= 2;
      }
      if( i == getTabCount()-1 ) {
        if( isMetalLaF() )
          b.x--;
        else if( isAquaLaF() )
          b.x -= 3;
      }
      return new Rectangle(b.x + b.width - (UIUtils.isGTKLookAndFeel() ? 14 : 13),
        b.y + b.height / 2 - 5,
        12,
        12);
    }
  }
  
  
  private boolean isWindowsVistaLaF() {
    String osName = System.getProperty("os.name");  // NOI18N
    return osName.indexOf("Vista") >= 0     // NOI18N
      || (osName.equals( "Windows NT (unknown)" ) && "6.0".equals( System.getProperty("os.version") ));  // NOI18N
  }
  
  private boolean isWindowsXPLaF() {
    Boolean isXP = (Boolean)Toolkit.getDefaultToolkit().
      getDesktopProperty("win.xpstyle.themeActive"); // NOI18N
    return isWindowsLaF() && (isXP == null ? false : isXP.booleanValue());
  }
  
  private boolean isWindowsLaF() {
    String lfID = UIManager.getLookAndFeel().getID();
    return lfID.endsWith("Windows"); // NOI18N
  }
  
  private boolean isAquaLaF() {
    return "Aqua".equals( UIManager.getLookAndFeel().getID() );  // NOI18N
  }
  
  private boolean isMetalLaF() {
    String lfID = UIManager.getLookAndFeel().getID();
    return "Metal".equals( lfID ); // NOI18N
  }
  
  public void paint(Graphics g) {
    super.paint(g);
    
    // Have a look at
    // http://ui.netbeans.org/docs/ui/closeButton/closeButtonUISpec.html
    // to see how the buttons are specified to be drawn.
    
    for (int i = 0, n = getTabCount(); i < n; i++) {
      Rectangle r = getCloseButtonBoundsAt(i);
      if (r == null)
        continue;
      
      if (i == mouseOverCloseButtonIndex
        || (i == pressedCloseButtonIndex && draggedOut)) {
        g.drawImage(getCloseTabMouseOverImage(), r.x, r.y , this);
      } else if (i == pressedCloseButtonIndex) {
        g.drawImage(getCloseTabPressedImage(), r.x, r.y , this);
      } else {
        g.drawImage(getCloseTabImage(), r.x, r.y , this);
      }
    }
  }
  
  private Image getCloseTabImage() {
    if( null == closeTabImage ) {
      if( isWindowsVistaLaF() ) {
        closeTabImage = Utilities.loadImage("org/openide/awt/resources/vista_close_enabled.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabImage = Utilities.loadImage("org/openide/awt/resources/xp_close_enabled.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabImage = Utilities.loadImage("org/openide/awt/resources/win_close_enabled.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabImage = Utilities.loadImage("org/openide/awt/resources/mac_close_enabled.png"); // NOI18N
      } else {
        closeTabImage = Utilities.loadImage("org/openide/awt/resources/metal_close_enabled.png"); // NOI18N
      }
    }
    return closeTabImage;
  }
  
  private Image getCloseTabPressedImage() {
    if( null == closeTabPressedImage ) {
      if( isWindowsVistaLaF() ) {
        closeTabPressedImage = Utilities.loadImage("org/openide/awt/resources/vista_close_pressed.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabPressedImage =Utilities.loadImage("org/openide/awt/resources/xp_close_pressed.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabPressedImage = Utilities.loadImage("org/openide/awt/resources/win_close_pressed.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabPressedImage = Utilities.loadImage("org/openide/awt/resources/mac_close_pressed.png"); // NOI18N
      } else {
        closeTabPressedImage = Utilities.loadImage("org/openide/awt/resources/metal_close_pressed.png"); // NOI18N
      }
    }
    return closeTabPressedImage;
  }
  
  private Image getCloseTabMouseOverImage() {
    if( null == closeTabMouseOverImage ) {
      if( isWindowsVistaLaF() ) {
        closeTabMouseOverImage = Utilities.loadImage("org/openide/awt/resources/vista_close_rollover.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabMouseOverImage = Utilities.loadImage("org/openide/awt/resources/xp_close_rollover.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabMouseOverImage = Utilities.loadImage("org/openide/awt/resources/win_close_rollover.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabMouseOverImage = Utilities.loadImage("org/openide/awt/resources/mac_close_rollover.png"); // NOI18N
      } else {
        closeTabMouseOverImage = Utilities.loadImage("org/openide/awt/resources/metal_close_rollover.png"); // NOI18N
      }
    }
    return closeTabMouseOverImage;
  }
  
  private void setPressedCloseButtonIndex(int index) {
    if (pressedCloseButtonIndex == index)
      return;
    
    if (pressedCloseButtonIndex >= 0
      && pressedCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(pressedCloseButtonIndex);
      if (r != null) {
        repaint(r.x, r.y, r.width + 2, r.height + 2);
      }
      
      JComponent c = _getJComponentAt(pressedCloseButtonIndex);
      if( c != null )
        setToolTipTextAt(pressedCloseButtonIndex, c.getToolTipText());
    }
    
    pressedCloseButtonIndex = index;
    
    if (pressedCloseButtonIndex >= 0
      && pressedCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(pressedCloseButtonIndex);
      if (r != null) {
        repaint(r.x, r.y, r.width + 2, r.height + 2);
      }
      setMouseOverCloseButtonIndex(-1);
      setToolTipTextAt(pressedCloseButtonIndex, null);
    }
  }
  
  private void setMouseOverCloseButtonIndex(int index) {
    if (mouseOverCloseButtonIndex == index)
      return;
    
    if (mouseOverCloseButtonIndex >= 0
      && mouseOverCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(mouseOverCloseButtonIndex);
      if (r != null) {
        repaint(r.x, r.y, r.width + 2, r.height + 2);
      }
      JComponent c = _getJComponentAt(mouseOverCloseButtonIndex);
      if( c != null )
        setToolTipTextAt(mouseOverCloseButtonIndex, c.getToolTipText());
    }
    
    mouseOverCloseButtonIndex = index;
    
    if (mouseOverCloseButtonIndex >= 0
      && mouseOverCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(mouseOverCloseButtonIndex);
      if (r != null) {
        repaint(r.x, r.y, r.width + 2, r.height + 2);
      }
      setPressedCloseButtonIndex(-1);
      setToolTipTextAt(mouseOverCloseButtonIndex, null);
    }
  }
  
  private JComponent _getJComponentAt( int tabIndex ) {
    Component c = getComponentAt( tabIndex );
    return c instanceof JComponent ? (JComponent)c : null;
  }
  
  private void fireCloseRequest(Component c) {
    firePropertyChange(PROP_CLOSE, null, c);
  }
  
  static void fixGetBoundsAt(Rectangle b) {
    if (b.y < 0)
      b.y = -b.y;
    if (b.x < 0)
      b.x = -b.x;
  }
  
  static int findTabForCoordinate(JTabbedPane tab, int x, int y) {
    for (int i = 0; i < tab.getTabCount(); i++) {
      Rectangle b = tab.getBoundsAt(i);
      if (b != null) {
        b = new Rectangle(b);
        fixGetBoundsAt(b);
        
        if (b.contains(x, y)) {
          return i;
        }
      }
    }
    return -1;
  }
  
  
  protected void processMouseEvent(MouseEvent me) {
    try {
      super.processMouseEvent(me);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //Bug in BasicTabbedPaneUI$Handler:  The focusIndex field is not
      //updated when tabs are removed programmatically, so it will try to
      //repaint a tab that's not there
      Exceptions.attachLocalizedMessage(aioobe,
        "Suppressed AIOOBE bug in BasicTabbedPaneUI"); // NOI18N
      Logger.getAnonymousLogger().log(Level.WARNING, null, aioobe);
    }
  }
  
  protected void fireStateChanged() {
        try {
            super.fireStateChanged();
        } catch( ArrayIndexOutOfBoundsException e ) {
            if( Utilities.isMac() ) {
                //#126651 - JTabbedPane is buggy on Mac OS
            } else {
                throw e;
            }
        }
    }
  
  private static class CloseButtonListener implements AWTEventListener {
    private static boolean installed = false;
    
    private CloseButtonListener() {}
    
    private static synchronized void install() {
      if (installed)
        return;
      
      installed = true;
      Toolkit.getDefaultToolkit().addAWTEventListener(
        new CloseButtonListener(),
        AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }
    
    public void eventDispatched(AWTEvent ev) {
      MouseEvent e = (MouseEvent) ev;
      //#118828
      if (! (ev.getSource() instanceof Component)) {
        return;
      }
      
      Component c = (Component) e.getSource();
      while (c != null && !(c instanceof DataSourceWindowTabbedPane))
        c = c.getParent();
      if (c == null)
        return;
      final DataSourceWindowTabbedPane tab = (DataSourceWindowTabbedPane) c;
      
      Point p = SwingUtilities.convertPoint((Component) e.getSource(),
        e.getPoint(),
        tab);
      
      if (e.getID() == MouseEvent.MOUSE_CLICKED) {
        //Not interested in clicked, and it can cause an NPE
        return;
      }
      
      int index = findTabForCoordinate(tab, p.x, p.y);
      
      Rectangle r = null;
      if (index >= 0)
        r = tab.getCloseButtonBoundsAt(index);
      if (r == null)
        r = new Rectangle(0,0,0,0);
      
      switch(e.getID()) {
        case MouseEvent.MOUSE_PRESSED:
          if (r.contains(p)) {
            tab.setPressedCloseButtonIndex(index);
            tab.draggedOut = false;
            e.consume();
            return;
          }
          break;
          
        case MouseEvent.MOUSE_RELEASED:
          if (r.contains(p) && tab.pressedCloseButtonIndex >= 0) {
            Component tc =
              tab.getComponentAt(tab.pressedCloseButtonIndex);
            tab.reset();
            
            tab.fireCloseRequest(tc);
            e.consume();
            return;
          } else {
            tab.reset();
          }
          break;
          
        case MouseEvent.MOUSE_ENTERED:
          break;
          
        case MouseEvent.MOUSE_EXITED:
          //tab.reset();
          
          // XXX(-ttran) when the user clicks on the close button on
          // an unfocused (internal) frame the focus is transferred
          // to the frame and an unexpected MOUSE_EXITED event is
          // fired.  If we call reset() at every MOUSE_EXITED event
          // then when the mouse button is released the tab is not
          // closed.  See bug #24450
          
          break;
          
        case MouseEvent.MOUSE_MOVED:
          if (r.contains(p)) {
            tab.setMouseOverCloseButtonIndex(index);
            tab.draggedOut = false;
            e.consume();
            return;
          } else if (tab.mouseOverCloseButtonIndex >= 0) {
            tab.setMouseOverCloseButtonIndex(-1);
            tab.draggedOut = false;
            e.consume();
          }
          break;
          
        case MouseEvent.MOUSE_DRAGGED:
          if (tab.pressedCloseButtonIndex >= 0) {
            if (tab.draggedOut != !r.contains(p)) {
              tab.draggedOut = !r.contains(p);
              tab.repaint(r.x, r.y, r.width + 2+6, r.height + 2+6);
            }
            e.consume();
            return;
          }
          break;
      }
    }
  }
  
  static class ViewContainer extends JPanel {
      
      private DataSourceCaption caption;
      private DataSourceView view;
      private DataViewComponent viewComponent;
      
      public ViewContainer(DataSourceCaption caption, DataSourceView view) {
          this.caption = caption;
          this.view = view;
          this.viewComponent = view.getView();
          setLayout(new BorderLayout());
          setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.WHITE));
          setBackground(Color.WHITE);
          
          add(viewComponent, BorderLayout.CENTER);
          if (caption != null) {
              caption.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
              caption.setBackground(Color.WHITE);
              add(caption, BorderLayout.NORTH);
          }
      }
      
      public DataSourceCaption getCaption() { return caption; }
      
      public DataSourceView getView() { return view; }
      
      public DataViewComponent getViewComponent() { return viewComponent; }
  }
  
}
