package com.vexsoftware.votifier.platform;

public interface LoggingAdapter {
    void error(String s);
    void error(String s, Object... o);

    void warn(String s);
    void warn(String s, Object... o);

    void info(String s);
    void info(String s, Object... o);
}

