package com.crossgate.plugin.aspect.logger;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.slf4j.Marker;

public final class DummyLogger implements Logger {
    private final String name;

    public DummyLogger(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isTraceEnabled() {
        return false;
    }

    public void trace(String msg) {
    }

    public void trace(String format, Object arg) {
    }

    public void trace(String format, Object arg1, Object arg2) {
    }

    public void trace(String format, Object... arguments) {
    }

    public void trace(String msg, Throwable t) {
    }

    public boolean isTraceEnabled(Marker marker) {
        return false;
    }

    public void trace(Marker marker, String msg) {
    }

    public void trace(Marker marker, String format, Object arg) {
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void trace(Marker marker, String format, Object... argArray) {
    }

    public void trace(Marker marker, String msg, Throwable t) {
    }

    public boolean isDebugEnabled() {
        return false;
    }

    public void debug(String msg) {
    }

    public void debug(String format, Object arg) {
    }

    public void debug(String format, Object arg1, Object arg2) {
    }

    public boolean isLifecycleEnabled() {
        return false;
    }

    public void debug(String format, Object... arguments) {
    }

    public void lifecycle(String message) {
    }

    public void lifecycle(String message, Object... objects) {
    }

    public void lifecycle(String message, Throwable throwable) {
    }

    public void debug(String msg, Throwable t) {
    }

    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    public void debug(Marker marker, String msg) {
    }

    public void debug(Marker marker, String format, Object arg) {
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void debug(Marker marker, String format, Object... arguments) {
    }

    public void debug(Marker marker, String msg, Throwable t) {
    }

    public boolean isInfoEnabled() {
        return false;
    }

    public void info(String msg) {
    }

    public void info(String format, Object arg) {
    }

    public void info(String format, Object arg1, Object arg2) {
    }

    public void info(String format, Object... arguments) {
    }

    public boolean isQuietEnabled() {
        return false;
    }

    public void quiet(String message) {
    }

    public void quiet(String message, Object... objects) {
    }

    public void quiet(String message, Throwable throwable) {
    }

    public boolean isEnabled(LogLevel level) {
        return false;
    }

    public void log(LogLevel level, String message) {
    }

    public void log(LogLevel level, String message, Object... objects) {
    }

    public void log(LogLevel level, String message, Throwable throwable) {
    }

    public void info(String msg, Throwable t) {
    }

    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    public void info(Marker marker, String msg) {
    }

    public void info(Marker marker, String format, Object arg) {
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void info(Marker marker, String format, Object... arguments) {
    }

    public void info(Marker marker, String msg, Throwable t) {
    }

    public boolean isWarnEnabled() {
        return false;
    }

    public void warn(String msg) {
    }

    public void warn(String format, Object arg) {
    }

    public void warn(String format, Object... arguments) {
    }

    public void warn(String format, Object arg1, Object arg2) {
    }

    public void warn(String msg, Throwable t) {
    }

    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    public void warn(Marker marker, String msg) {
    }

    public void warn(Marker marker, String format, Object arg) {
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void warn(Marker marker, String format, Object... arguments) {
    }

    public void warn(Marker marker, String msg, Throwable t) {
    }

    public boolean isErrorEnabled() {
        return false;
    }

    public void error(String msg) {
    }

    public void error(String format, Object arg) {
    }

    public void error(String format, Object arg1, Object arg2) {
    }

    public void error(String format, Object... arguments) {
    }

    public void error(String msg, Throwable t) {
    }

    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    public void error(Marker marker, String msg) {
    }

    public void error(Marker marker, String format, Object arg) {
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
    }

    public void error(Marker marker, String format, Object... arguments) {
    }

    public void error(Marker marker, String msg, Throwable t) {
    }
}
