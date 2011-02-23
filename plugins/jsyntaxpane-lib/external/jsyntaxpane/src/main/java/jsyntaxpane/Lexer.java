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

import java.io.Reader;

/**
 * Lexers must implement these methods.  These are used in the Tokenizer 
 * 
 * A Lexer should be tied to one document.
 * 
 * @author Ayman Al-Sairafi
 */
public interface Lexer {

    /**
     * This will be called to reset the the lexer, generally whenever a
     * document is changed
     * @param reader
     */
    public void yyreset(Reader reader);

    /**
     * This is called to return the next Token from the Input Reader
     * @return next token, or null if no more tokens.
     * @throws java.io.IOException
     */
    public Token yylex() throws java.io.IOException;

    /**
     * Returns the character at position <tt>pos</tt> from the
     * matched text.
     *
     * It is equivalent to yytext().charAt(pos), but faster
     *
     * @param pos the position of the character to fetch.
     *            A value from 0 to yylength()-1.
     *
     * @return the character at position pos
     */
    public char yycharat(int pos);

    /**
     * Returns the length of the matched text region.
     */
    public int yylength();

    /**
     * Returns the text matched by the current regular expression.
     */
    public String yytext();
}
