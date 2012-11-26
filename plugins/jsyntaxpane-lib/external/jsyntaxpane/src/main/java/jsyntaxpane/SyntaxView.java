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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.ViewFactory;
import jsyntaxpane.util.Configuration;

public class SyntaxView extends PlainView {

    public static final String PROPERTY_RIGHT_MARGIN_COLOR = "RightMarginColor";
    public static final String PROPERTY_RIGHT_MARGIN_COLUMN = "RightMarginColumn";
    public static final String PROPERTY_SINGLE_COLOR_SELECT = "SingleColorSelect";
    public static final String PROPERTY_TEXTAA = "TextAA";
    
    private static final Logger log = Logger.getLogger(SyntaxView.class.getName());
    private SyntaxStyle DEFAULT_STYLE = SyntaxStyles.getInstance().getStyle(TokenType.DEFAULT);
    private final boolean singleColorSelect;
    private final int rightMarginColumn;
    private final Color rightMarginColor;
    private final Object textAAHint;

    /**
     * Construct a new view using the given configuration and prefix given
     * 
     * @param element
     * @param config
     * @param prefix
     */
    public SyntaxView(Element element, Configuration config, String prefix) {
        super(element);
        singleColorSelect = config.getPrefixBoolean(prefix, PROPERTY_SINGLE_COLOR_SELECT, false);
        rightMarginColor = new Color(config.getPrefixInteger(prefix, PROPERTY_RIGHT_MARGIN_COLOR,
                0xFF7777));
        rightMarginColumn = config.getPrefixInteger(prefix, PROPERTY_RIGHT_MARGIN_COLUMN,
                0);
        String textaa = config.getPrefixProperty(prefix, PROPERTY_TEXTAA,
                "DEFAULT");
        textAAHint = TEXT_AA_HINT_NAMES.get(textaa);
    }

    @Override
    protected int drawUnselectedText(Graphics graphics, int x, int y, int p0,
            int p1) {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                textAAHint);
        Font saveFont = graphics.getFont();
        Color saveColor = graphics.getColor();
        SyntaxDocument doc = (SyntaxDocument) getDocument();
        Segment segment = getLineBuffer();
        // Draw the right margin first, if needed.  This way the text overalys
        // the margin
        if (rightMarginColumn > 0) {
            int m_x = rightMarginColumn * graphics.getFontMetrics().charWidth('m');
            int h = graphics.getFontMetrics().getHeight();
            graphics.setColor(rightMarginColor);
            graphics.drawLine(m_x, y, m_x, y - h);
        }
        try {
            // Colour the parts
            Iterator<Token> i = doc.getTokens(p0, p1);
            int start = p0;
            while (i.hasNext()) {
                Token t = i.next();
                // if there is a gap between the next token start and where we
                // should be starting (spaces not returned in tokens), then draw
                // it in the default type
                if (start < t.start) {
                    doc.getText(start, t.start - start, segment);
                    x = DEFAULT_STYLE.drawText(segment, x, y, graphics, this, start);
                }
                // t and s are the actual start and length of what we should
                // put on the screen.  assume these are the whole token....
                int l = t.length;
                int s = t.start;
                // ... unless the token starts before p0:
                if (s < p0) {
                    // token is before what is requested. adgust the length and s
                    l -= (p0 - s);
                    s = p0;
                }
                // if token end (s + l is still the token end pos) is greater 
                // than p1, then just put up to p1
                if (s + l > p1) {
                    l = p1 - s;
                }
                doc.getText(s, l, segment);
                x = SyntaxStyles.getInstance().drawText(segment, x, y, graphics, this, t);
                start = t.end();
            }
            // now for any remaining text not tokenized:
            if (start < p1) {
                doc.getText(start, p1 - start, segment);
                x = DEFAULT_STYLE.drawText(segment, x, y, graphics, this, start);
            }
        } catch (BadLocationException ex) {
            System.err.println("Requested: " + ex.offsetRequested());
            log.log(Level.SEVERE, null, ex);
        } finally {
            graphics.setFont(saveFont);
            graphics.setColor(saveColor);
        }
        return x;
    }

    @Override
    protected int drawSelectedText(Graphics graphics, int x, int y, int p0, int p1)
            throws BadLocationException {
        if (singleColorSelect) {
            if (rightMarginColumn > 0) {
                int m_x = rightMarginColumn * graphics.getFontMetrics().charWidth('m');
                int h = graphics.getFontMetrics().getHeight();
                graphics.setColor(rightMarginColor);
                graphics.drawLine(m_x, y, m_x, y - h);
            }
            return super.drawUnselectedText(graphics, x, y, p0, p1);
        } else {
            return drawUnselectedText(graphics, x, y, p0, p1);
        }
    }

    @Override
    protected void updateDamage(javax.swing.event.DocumentEvent changes,
            Shape a,
            ViewFactory f) {
        super.updateDamage(changes, a, f);
        java.awt.Component host = getContainer();
        host.repaint();
    }

    /**
     * The values for the string key for Text Anti-Aliasing
     */
    private static Map<String, Object> TEXT_AA_HINT_NAMES =
            new HashMap<String, Object>();

    static {
        TEXT_AA_HINT_NAMES.put("DEFAULT", RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        TEXT_AA_HINT_NAMES.put("GASP", RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        TEXT_AA_HINT_NAMES.put("HBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR);
        TEXT_AA_HINT_NAMES.put("HRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        TEXT_AA_HINT_NAMES.put("VBGR", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
        TEXT_AA_HINT_NAMES.put("VRGB", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);
        TEXT_AA_HINT_NAMES.put("OFF", RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        TEXT_AA_HINT_NAMES.put("ON", RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
