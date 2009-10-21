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
package com.sun.tools.visualvm.modules.oqlsyntax.lexers;

import jsyntaxpane.DefaultLexer;
import jsyntaxpane.Token;
import jsyntaxpane.TokenType;

%%

%public
%class OqlLexer
%extends DefaultLexer
%final
%unicode
%char
%type Token
%caseless


%{
    /**
     * Default constructor is needed as we will always call the yyreset
     */
    public OqlLexer() {
        super();
    }

    /**
     * Helper method to create and return a new Token from of TokenType
     */
    private Token token(TokenType type) {
        return new Token(type, yychar, yylength());
    }

%}

/* main character classes */
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = {LineTerminator} | [ \t\f]

/* comments */
Comment = {EndOfLineComment}

EndOfLineComment = "--" {InputCharacter}* {LineTerminator}?

/* identifiers */
Identifier = [:jletter:][:jletterdigit:]*

/* integer literals */
DecIntegerLiteral = 0 | [1-9][0-9]*
    
/* floating point literals */        
FloatLiteral  = ({FLit1}|{FLit2}|{FLit3}) {Exponent}? [fF]

FLit1    = [0-9]+ \. [0-9]* 
FLit2    = \. [0-9]+ 
FLit3    = [0-9]+ 
Exponent = [eE] [+-]? [0-9]+

/* string and character literals */
StringCharacter = [^\r\n\"\\]
SingleCharacter = [^\r\n\'\\]

Reserved = 
   "SELECT"              |
   "FROM"                |
   "INSTANCEOF"          |
   "WHERE"
%%

<YYINITIAL> {

  /* keywords */
  {Reserved}                     { return token(TokenType.KEYWORD); }
  
  /* operators */

  "("                            |
  ")"                            |
  "{"                            | 
  "}"                            | 
  "["                            | 
  "]"                            | 
  ";"                            | 
  ","                            | 
  "."                            | 
  "@"                            | 
  "="                            | 
  ">"                            | 
  "<"                            |
  "!"                            | 
  "~"                            | 
  "?"                            | 
  ":"                            { return token(TokenType.OPERATOR); } 

  /* string literal */
  \"{StringCharacter}+\"         | 

  \'{SingleCharacter}+\          { return token(TokenType.STRING); } 

  /* numeric literals */

  {DecIntegerLiteral}            |
 
  {FloatLiteral}                 { return token(TokenType.NUMBER); }
  
  /* comments */
  {Comment}                      { return token(TokenType.COMMENT); }

  /* whitespace */
  {WhiteSpace}+                  { /* skip */ }

  /* identifiers */ 
  {Identifier}                   { return token(TokenType.IDENTIFIER); }

}

/* error fallback */
.|\n                             {  }
<<EOF>>                          { return null; }

