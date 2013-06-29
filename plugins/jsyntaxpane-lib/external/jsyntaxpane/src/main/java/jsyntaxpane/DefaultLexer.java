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

/**
 * This is a default, and abstract implemenatation of a Lexer with
 * some utility methods that Lexers can implement.
 *
 * @author Ayman Al-Sairafi
 */
public abstract class DefaultLexer implements Lexer {
    
    protected int tokenStart;
    protected int tokenLength;

    /**
     * Helper method to create and return a new Token from of TokenType
     * @param type
     * @param tStart
     * @param tLength
     * @param newStart
     * @param newLength
     * @return
     */
    protected Token token(TokenType type, int tStart, int tLength,
            int newStart, int newLength) {
        tokenStart = newStart;
        tokenLength = newLength;
        return new Token(type, tStart, tLength);
    }

    /**
     * Return the current matched token as a string.  This is <b>expensive</b>
     * as it creates a new String object for the token.  Use with care.
     *
     * @return
     */
    protected CharSequence getTokenSrring() {
        return yytext();
    }
}
