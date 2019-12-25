package com.vexsoftware.votifier.support.forwarding;

import java.util.Collection;
import java.util.Collections;

public class ServerFilter {

    public ServerFilter(Collection<String> names, boolean whitelist) {
        if (names == null)
            names = Collections.EMPTY_SET;

        this.names = names;
        this.whitelist = whitelist;
    }

    public ServerFilter() {
        this(Collections.EMPTY_SET, true);
    }

    private final Collection<String> names;
    private final boolean whitelist;

    public boolean isAllowed(String name) {
        return names.contains(name) == whitelist;
    }
}
