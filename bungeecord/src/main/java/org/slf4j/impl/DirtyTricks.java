package org.slf4j.impl;

import org.slf4j.Logger;

public class DirtyTricks {
    private DirtyTricks() {
        throw new AssertionError();
    }

    public static Logger getLogger(java.util.logging.Logger source) {
        return new JDK14LoggerAdapter(source);
    }
}
