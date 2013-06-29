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

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class Token implements Serializable, Comparable {

    public final TokenType type;
    public final int start;
    public final int length;
    /**
     * the pair value to use if this token is one of a pair:
     * This is how it is used:
     * The openning part will have a positive number X
     * The closing part will have a negative number X
     * X should be unique for a pair:
     *   e.g. for [ pairValue = +1
     *        for ] pairValue = -1
     */
    public final byte pairValue;

    /**
     * Constructs a new token
     * @param type
     * @param start
     * @param length
     */
    public Token(TokenType type, int start, int length) {
        this.type = type;
        this.start = start;
        this.length = length;
        this.pairValue = 0;
    }

    /**
     * Construct a new part of pair token
     * @param type
     * @param start
     * @param length
     * @param pairValue
     */
    public Token(TokenType type, int start, int length, byte pairValue) {
        this.type = type;
        this.start = start;
        this.length = length;
        this.pairValue = pairValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Object) {
            Token token = (Token) obj;
            return ((this.start == token.start) &&
                    (this.length == token.length) &&
                    (this.type.equals(token.type)));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return start;
    }

    @Override
    public String toString() {
        return String.format("%s (%d, %d) (%d)", type, start, length, pairValue);
    }

    @Override
    public int compareTo(Object o) {
        Token t = (Token) o;
        if (this.start !=  t.start) {
            return (this.start - t.start);
        } else if(this.length != t.length) {
            return (this.length - t.length);
        } else {
            return this.type.compareTo(t.type);
        }
    }

    /**
     * return the end position of the token.
     * @return start + length
     */
    public int end() {
        return start + length;
    }
    
    /**
     * Get the text of the token from this document
     * @param doc
     * @return
     */
    public String getText(Document doc) {
        String text = null;
        try {
            text = doc.getText(start, length);
        } catch (BadLocationException ex) {
            Logger.getLogger(Token.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text;
    }
}
