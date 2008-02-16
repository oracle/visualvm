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
import com.sun.tools.visualvm.core.ui.DataSourceCaptionFactory;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
  
  
  private Map<DataSourceViewContainer, DataSourceView> mapping = new HashMap();
  
  
  DataSourceWindowTabbedPane() {
    addChangeListener( new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        reset();
      }
    });
    CloseButtonListener.install();
    
    setFocusable(true);
    setBorder(javax.swing.BorderFactory.createEmptyBorder());
    setFocusCycleRoot(false);
  }
  
  
  private int pressedCloseButtonIndex = -1;
  private int mouseOverCloseButtonIndex = -1;
  private boolean draggedOut = false;
  
  public void addViewTab(DataSource dataSource, DataSourceView view) {
    DataSourceViewContainer container = new DataSourceViewContainer(DataSourceCaptionFactory.getInstance().getDataSourcePresenter(dataSource), view.getView());
    mapping.put(container, view);
    super.add(container);
    setTitleAt(getComponentCount() - 1, view.getName() + (view.isClosable() ? "  " : ""));
    super.setIconAt(getComponentCount() - 1, new ImageIcon(view.getImage()));
  }
  
  public void removeTabAt(int index) {
      DataSourceViewContainer container = (DataSourceViewContainer)getComponentAt(index);
      super.removeTabAt(index);
      mapping.remove(container);
  }
  
  public DataSourceView getDataSourceView(DataSourceViewContainer container) {
      return mapping.get(container);
  }
  
  public int indexOfView(DataSourceView view) {
      for (int i = 0; i < getTabCount(); i++)
          if (((DataSourceViewContainer)getComponentAt(i)).getView() == view.getView()) return i;
      return -1;
  }
  
  public Set<DataSourceView> getViews() {
      Set<DataSourceView> views = new HashSet();
      
      for (Component component : getComponents()) {
          DataSourceViewContainer container = (DataSourceViewContainer)component;
          DataSourceView view = mapping.get(container);
          views.add(view);
      }
      
      return views;
  }
  
  // NOTE: has to be allowed, is called from super.add() needed in addViewTab()
  public void addTab(String title, Component component) {
    if (component instanceof DataSourceViewContainer) super.addTab(title, component);
    else throw new RuntimeException("Not supported for this component");
  }
  
  public Component add(Component component) { throw new RuntimeException("Not supported for this component"); }
  public Component add(Component component, int index) { throw new RuntimeException("Not supported for this component"); }
  public void add(Component component, Object constraints) { throw new RuntimeException("Not supported for this component"); }
  public void add(Component component, Object constraints, int index) { throw new RuntimeException("Not supported for this component"); }
  public Component add(String title, Component component) { throw new RuntimeException("Not supported for this component"); }
  public void addTab(String title, Icon icon, Component component) { throw new RuntimeException("Not supported for this component"); }
  public void addTab(String title, Icon icon, Component component, String tip) { throw new RuntimeException("Not supported for this component"); }
  
  
  public void setTitleAt(int idx, String title) {
    DataSourceViewContainer view = (DataSourceViewContainer)getComponentAt(idx);
    if (mapping.get(view).isClosable()) {
      String nue = title.indexOf("</html>") != -1 ? //NOI18N
        Utilities.replaceString(title, "</html>", "&nbsp;&nbsp;</html>") //NOI18N
        : title + "  "; //NOI18N
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
      DataSourceViewContainer view = (DataSourceViewContainer)getComponentAt(i);
      if (!mapping.get(view).isClosable()) return null;
      
      b = new Rectangle(b);
      fixGetBoundsAt(b);
      
      Dimension tabsz = getSize();
      if (b.x + b.width >= tabsz.width
        || b.y + b.height >= tabsz.height)
        return null;
      
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
      return new Rectangle(b.x + b.width - 13,
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
      getDesktopProperty("win.xpstyle.themeActive"); //NOI18N
    return isWindowsLaF() && (isXP == null ? false : isXP.booleanValue());
  }
  
  private boolean isWindowsLaF() {
    String lfID = UIManager.getLookAndFeel().getID();
    return lfID.endsWith("Windows"); //NOI18N
  }
  
  private boolean isAquaLaF() {
    return "Aqua".equals( UIManager.getLookAndFeel().getID() );  // NOI18N
  }
  
  private boolean isMetalLaF() {
    String lfID = UIManager.getLookAndFeel().getID();
    return "Metal".equals( lfID ); //NOI18N
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
        closeTabImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/vista_close_enabled.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/xp_close_enabled.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/win_close_enabled.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/mac_close_enabled.png"); // NOI18N
      } else {
        closeTabImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/metal_close_enabled.png"); // NOI18N
      }
    }
    return closeTabImage;
  }
  
  private Image getCloseTabPressedImage() {
    if( null == closeTabPressedImage ) {
      if( isWindowsVistaLaF() ) {
        closeTabPressedImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/vista_close_pressed.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabPressedImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/xp_close_pressed.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabPressedImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/win_close_pressed.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabPressedImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/mac_close_pressed.png"); // NOI18N
      } else {
        closeTabPressedImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/metal_close_pressed.png"); // NOI18N
      }
    }
    return closeTabPressedImage;
  }
  
  private Image getCloseTabMouseOverImage() {
    if( null == closeTabMouseOverImage ) {
      if( isWindowsVistaLaF() ) {
        closeTabMouseOverImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/vista_close_rollover.png"); // NOI18N
      } else if( isWindowsXPLaF() ) {
        closeTabMouseOverImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/xp_close_rollover.png"); // NOI18N
      } else if( isWindowsLaF() ) {
        closeTabMouseOverImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/win_close_rollover.png"); // NOI18N
      } else if( isAquaLaF() ) {
        closeTabMouseOverImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/mac_close_rollover.png"); // NOI18N
      } else {
        closeTabMouseOverImage = org.openide.util.Utilities.loadImage("org/openide/awt/resources/metal_close_rollover.png"); // NOI18N
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
      repaint(r.x, r.y, r.width + 2, r.height + 2);
      
      JComponent c = _getJComponentAt(pressedCloseButtonIndex);
      if( c != null )
        setToolTipTextAt(pressedCloseButtonIndex, c.getToolTipText());
    }
    
    pressedCloseButtonIndex = index;
    
    if (pressedCloseButtonIndex >= 0
      && pressedCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(pressedCloseButtonIndex);
      repaint(r.x, r.y, r.width + 2, r.height + 2);
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
      repaint(r.x, r.y, r.width + 2, r.height + 2);
      JComponent c = _getJComponentAt(mouseOverCloseButtonIndex);
      if( c != null )
        setToolTipTextAt(mouseOverCloseButtonIndex, c.getToolTipText());
    }
    
    mouseOverCloseButtonIndex = index;
    
    if (mouseOverCloseButtonIndex >= 0
      && mouseOverCloseButtonIndex < getTabCount()) {
      Rectangle r = getCloseButtonBoundsAt(mouseOverCloseButtonIndex);
      repaint(r.x, r.y, r.width + 2, r.height + 2);
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
        "Suppressed AIOOBE bug in BasicTabbedPaneUI"); //NOI18N
      Logger.getAnonymousLogger().log(Level.WARNING, null, aioobe);
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
  
  static class DataSourceViewContainer extends JPanel {
      
      private DataViewComponent view;
      
      public DataSourceViewContainer(JComponent caption, DataViewComponent view) {
          this.view = view;
          setLayout(new BorderLayout());
          setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.WHITE));
          setBackground(Color.WHITE);
          
          add(view, BorderLayout.CENTER);
          if (caption != null) {
              caption.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
              caption.setBackground(Color.WHITE);
              add(caption, BorderLayout.NORTH);
          }
      }
      
      public DataViewComponent getView() { return view; }
  }
  
}
