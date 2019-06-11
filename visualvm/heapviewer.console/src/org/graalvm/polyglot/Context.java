/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graalvm.polyglot;

import java.io.OutputStream;

/**
 *
 * @author thurka
 */
public class Context {

    public static Builder newBuilder(String... permittedLanguages) {
        return null;
    }

    public Value eval(String languageId, CharSequence source) {
        return null;
    }

    public Value getPolyglotBindings() {
        return null;
    }

    public final class Builder {

        public Builder allowAllAccess(boolean b) {
            return null;
        }

        public Builder out(OutputStream outStream) {
            return null;
        }

        public Context build() {
            return null;
        }

    }
}
