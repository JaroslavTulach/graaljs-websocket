package org.apidesign.polyfill;

import org.graalvm.polyglot.Context;

public interface Polyfill {

    void initialize(Context ctx);
}
