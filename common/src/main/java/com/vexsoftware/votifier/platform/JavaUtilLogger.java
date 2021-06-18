package com.vexsoftware.votifier.platform;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JavaUtilLogger implements LoggingAdapter {

    private final Logger logger;

    public JavaUtilLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(String s) {
        logger.severe(s);
    }

    @Override
    public void error(String s, Object... o) {
        logger.log(Level.SEVERE, s, o);
    }

    @Override
    public void error(String s, Throwable e, Object... o) {
        LogRecord lr = new LogRecord(Level.SEVERE, s);
        lr.setParameters(o);
        lr.setThrown(e);
        logger.log(lr);
    }

    @Override
    public void warn(String s) {
        logger.warning(s);
    }

    @Override
    public void warn(String s, Object... o) {
        logger.log(Level.WARNING, s, o);
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object... o) {
        logger.log(Level.INFO, s, o);
    }
}
