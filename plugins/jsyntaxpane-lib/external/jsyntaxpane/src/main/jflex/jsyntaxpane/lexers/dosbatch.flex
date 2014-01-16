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

package jsyntaxpane.lexers;

import jsyntaxpane.DefaultLexer;
import jsyntaxpane.Token;
import jsyntaxpane.TokenType;

%%

%public
%class DOSBatchLexer
%extends DefaultLexer
%final
%unicode
%char
%type Token
%ignorecase


%{
    /**
     * Create an empty lexer, yyrset will be called later to reset and assign
     * the reader
     */
    public DOSBatchLexer() {
        super();
    }

    private Token token(TokenType type) {
        return new Token(type, yychar, yylength());
    }
%}

StartComment = rem
WhiteSpace = [ \t]
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
KeyCharacter = [a-zA-Z0-9._ ]

Comment = "rem" {InputCharacter}* {LineTerminator}?

%%

<YYINITIAL> 
{
  /* DOS keywords */
  "@"                           |
  "echo"                        |
  "goto"                        |
  "call"                        |
  "exit"                        |
  "if"                          |
  "else"                        |
  "for"                         |
  "copy"                        |
  "set"                         |
  "dir"                         |
  "cd"                          |
  "set"                         |
  "errorlevel"                  { return token(TokenType.KEYWORD); }

  /* DOS commands */
  "append"     |
  "assoc"      |
  "at"         |
  "attrib"     |
  "break"      |
  "cacls"      |
  "cd"         |
  "chcp"       |
  "chdir"      |
  "chkdsk"     |
  "chkntfs"    |
  "cls"        |
  "cmd"        |
  "color"      |
  "comp"       |
  "compact"    |
  "convert"    |
  "copy"       |
  "date"       |
  "del"        |
  "dir"        |
  "diskcomp"   |
  "diskcopy"   |
  "doskey"     |
  "echo"       |
  "endlocal"   |
  "erase"      |
  "fc"         |
  "find"       |
  "findstr"    |
  "format"     |
  "ftype"      |
  "graftabl"   |
  "help"       |
  "keyb"       |
  "label"      |
  "md"         |
  "mkdir"      |
  "mode"       |
  "more"       |
  "move"       |
  "path"       |
  "pause"      |
  "popd"       |
  "print"      |
  "prompt"     |
  "pushd"      |
  "rd"         |
  "recover"    |
  "rem"        |
  "ren"        |
  "rename"     |
  "replace"    |
  "restore"    |
  "rmdir"      |
  "set"        |
  "setlocal"   |
  "shift"      |
  "sort"       |
  "start"      |
  "subst"      |
  "time"       |
  "title"      |
  "tree"       |
  "type"       |
  "ver"        |
  "verify"     |
  "vol"        |
  "xcopy"      { return token(TokenType.KEYWORD); }


  /* labels */
  ":" [a-zA-Z][a-zA-Z0-9_]*     { return token(TokenType.TYPE); }

  /* comments */
  {Comment}                      { return token(TokenType.COMMENT); }
  . | {LineTerminator}           { /* skip */ }
}

<<EOF>>                          { return null; }