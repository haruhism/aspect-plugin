package com.crossgate.plugin.aspect.logger;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;

public class GLog {

    /**
     * VM栈帧索引 - 线程
     */
    public static final int INDEX_THREAD = 0;
    /**
     * VM栈帧索引 - [utils.Log.getCallerStackLabel]方法栈
     */
    public static final int INDEX_STACK_TAG = INDEX_THREAD + 1;
    /**
     * VM栈帧索引 - 当前栈帧
     */
    public static final int INDEX_CURRENT_STACK = INDEX_STACK_TAG + 1;
    /**
     * VM栈帧索引 - 调用方栈帧
     */
    public static final int INDEX_CALLER_STACK = INDEX_CURRENT_STACK + 1;
    /**
     * 日志分段长度
     */
    public static final int SEGMENT_SIZE = 1024 * 3;

    public static final String STACK_INFO_FORMAT = "%s.%s(%s:%d)";

    private static final String LOG_UTIL_NAME;
    private static final Logger NO_OP_LOGGER;

    static Logger sLogger;

    static {
        LOG_UTIL_NAME = GLog.class.getName();
        NO_OP_LOGGER = new DummyLogger(LOG_UTIL_NAME);
        sLogger = NO_OP_LOGGER;
    }

    public static void setLogger(Logger logger) {
        sLogger = logger;
    }

    public static boolean isLoggable(LogLevel level) {
        switch (level) {
            case DEBUG:
                return sLogger.isDebugEnabled();
            case INFO:
                return sLogger.isInfoEnabled();
            case LIFECYCLE:
                return sLogger.isLifecycleEnabled();
            case WARN:
                return sLogger.isWarnEnabled();
            case QUIET:
                return sLogger.isQuietEnabled();
            case ERROR:
                return sLogger.isErrorEnabled();
        }
        return false;
    }

    public static void d(String msg) {
        String label = getCallerStackLabel();
        log(label, msg, LogLevel.DEBUG);
    }

    public static void d(String tag, String msg) {
        log(tag, msg, LogLevel.DEBUG);
    }

    public static void i(String msg) {
        String label = getCallerStackLabel();
        log(label, msg, LogLevel.INFO);
    }

    public static void i(String tag, String msg) {
        log(tag, msg, LogLevel.INFO);
    }

    public static void l(String msg) {
        String label = getCallerStackLabel();
        log(label, msg, LogLevel.LIFECYCLE);
    }

    public static void l(String tag, String msg) {
        log(tag, msg, LogLevel.LIFECYCLE);
    }

    public static void q(String msg) {
        String label = getCallerStackLabel();
        log(label, msg, LogLevel.QUIET);
    }

    public static void q(String tag, String msg) {
        log(tag, msg, LogLevel.QUIET);
    }

    public static void w(String msg) {
        String label = getCallerStackLabel();
        String message = getCallerStackInfo() + ", " + msg;
        log(label, message, LogLevel.WARN);
    }

    public static void w(Throwable tr) {
        String label = getCallerStackLabel();
        log(label, getStackTraceString(tr), LogLevel.WARN);
    }

    public static void w(String tag, String msg) {
        String message = getCallerStackInfo() + ", " + msg;
        log(tag, message, LogLevel.WARN);
    }

    public static void w(String msg, Throwable tr) {
        String label = getCallerStackLabel();
        String message = getCallerStackInfo() + ", " + msg;
        if (tr != null) {
            message += ", " + getStackTraceString(tr);
        }
        log(label, message, LogLevel.WARN);
    }

    public static void w(String tag, String msg, Throwable tr) {
        String message = getCallerStackInfo() + ", " + msg;
        if (tr != null) {
            message += ", " + getStackTraceString(tr);
        }
        log(tag, message, LogLevel.WARN);
    }

    public static void e(String msg) {
        String label = getCallerStackLabel();
        log(label, msg, LogLevel.ERROR);
    }

    public static void e(Throwable tr) {
        String label = getCallerStackLabel();
        String message = getStackTraceString(tr);
        log(label, message, LogLevel.ERROR);
    }

    public static void e(String tag, String msg) {
        String message = getCallerStackInfo() + ", " + msg;
        log(tag, message, LogLevel.ERROR);
    }

    public static void e(String msg, Throwable tr) {
        String label = getCallerStackLabel();
        String message = tr != null ?
                msg + ", " + getStackTraceString(tr) :
                getCallerStackInfo() + ", " + msg;
        log(label, message, LogLevel.ERROR);
    }

    public static void e(String tag, String msg, Throwable tr) {
        String message = tr != null ?
                msg + ", " + getStackTraceString(tr) :
                getCallerStackInfo() + ", " + msg;
        log(tag, message, LogLevel.ERROR);
    }

    private static int log(String tag, String msg, LogLevel level) {
        if (msg == null || msg.isEmpty()) {
            return 0;
        }
        int segmentSize = SEGMENT_SIZE;
        int length = msg.length();
        if (length <= segmentSize) {
            // 长度小于等于限制直接打印
            return doLog(tag, msg, level);
        } else {
            // 循环分段打印日志
            int start = 0;
            int num = 0;
            do {
                int end = Math.min(start + segmentSize, length);
                String segment = msg.substring(start, end);
                num += doLog(tag, segment, level);
                start += segmentSize;
            } while (start < length);
            return num;
        }
    }

    static int doLog(String tag, String message, LogLevel level) {
        String s = tag + "  " + getLevelText(level) + "  " + message;
        sLogger.log(level, s);
        return s.length();
    }

    private static String getLevelText(LogLevel level) {
        switch (level) {
            case DEBUG:
                return "D";
            case INFO:
                return "I";
            case LIFECYCLE:
                return "L";
            case WARN:
                return "W";
            case QUIET:
                return "Q";
            case ERROR:
                return "E";
            default:
                return "?";
        }
    }

    /**
     * 获取调用栈中指定栈帧的标签
     * <p>
     * index=0  VMStack
     * index=1  Thread
     * index=2  getStackElementLabel()
     * index=3  CurrentStack
     * index=4  CallerStack
     * ...
     *
     * @param index 指定栈帧索引
     *
     * @return 返回调用日志打印的标签
     */
    public static String getStackElementLabel(int index) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (index < 0 || index >= stackTrace.length) {
            return "";
        }
        String className = stackTrace[index].getClassName();
        int dotIndex = className.lastIndexOf('.');
        return dotIndex == -1 ? className : className.substring(dotIndex + 1);
    }

    /**
     * 获取调用方的栈帧标签
     *
     * @return 返回调用日志打印的标签
     */
    public static String getCallerStackLabel() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = INDEX_CALLER_STACK;
        if (startIndex >= stackTrace.length) {
            return "";
        }
        for (int i = startIndex; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (Objects.equals(LOG_UTIL_NAME, className)) {
                continue;
            }
            int dotIndex = className.lastIndexOf('.');
            return dotIndex == -1 ? className : className.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * 获取调用方的栈帧信息
     *
     * @return 返回调用日志打印的栈帧信息（类名、方法名、文件名、行数）
     */
    private static String getCallerStackInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = INDEX_CALLER_STACK;
        if (startIndex >= stackTrace.length) {
            return "";
        }
        for (int i = startIndex; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (Objects.equals(LOG_UTIL_NAME, className)) {
                continue;
            }
            int dotIndex = className.lastIndexOf('.');
            String simpleName = dotIndex == -1 ? className : className.substring(dotIndex + 1);
            return String.format(Locale.ROOT,
                    STACK_INFO_FORMAT,
                    simpleName,
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());
        }
        return "";
    }

    /**
     * 打印方法调用栈信息，支持指定打印栈帧层数
     *
     * @param depth 调用栈层数
     */
    public static void printStackTrace(int depth) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = INDEX_CURRENT_STACK;
        if (startIndex >= stackTrace.length) {
            return;
        }
        int end = Math.min(startIndex + depth, stackTrace.length);
        StringBuilder sb = new StringBuilder(C.NEWLINE);
        String label = C.EMPTY;
        for (int i = startIndex; i < end; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (LOG_UTIL_NAME.equals(className)) {
                continue;
            }
            if (i == startIndex) {
                int dotIndex = className.lastIndexOf(C.DOT_CHAR);
                label = dotIndex == -1 ? className : className.substring(dotIndex + 1);
            }
            sb.append(className)
                    .append(C.DOT_CHAR)
                    .append(element.getMethodName())
                    .append("(")
                    .append(element.getFileName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append(")")
                    .append(C.NEWLINE);
        }
        log(label, sb.toString(), LogLevel.LIFECYCLE);
    }

    /**
     * 打印方法调用栈信息，默认打印包括调用方在内的6层栈帧
     */
    public static void printStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = INDEX_CURRENT_STACK;
        if (startIndex >= stackTrace.length) {
            return;
        }
        int end = Math.min(startIndex + 6, stackTrace.length);
        StringBuilder sb = new StringBuilder(C.NEWLINE);
        String label = C.EMPTY;
        for (int i = startIndex; i < end; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (LOG_UTIL_NAME.equals(className)) {
                continue;
            }
            if (i == startIndex) {
                int dotIndex = className.lastIndexOf(C.DOT_CHAR);
                label = dotIndex == -1 ? className : className.substring(dotIndex + 1);
            }
            sb.append(className)
                    .append(C.DOT_CHAR)
                    .append(element.getMethodName())
                    .append("(")
                    .append(element.getFileName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append(")")
                    .append(C.NEWLINE);
        }
        log(label, sb.toString(), LogLevel.LIFECYCLE);
    }

    /**
     * 打印方法调用栈完整信息
     */
    public static void printAllStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int startIndex = INDEX_THREAD;
        if (startIndex == stackTrace.length) {
            return;
        }
        StringBuilder sb = new StringBuilder(C.NEWLINE);
        String label = C.EMPTY;
        for (int i = startIndex; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (LOG_UTIL_NAME.equals(className)) {
                continue;
            }
            if (i == startIndex) {
                int dotIndex = className.lastIndexOf(C.DOT_CHAR);
                label = dotIndex == -1 ? className : className.substring(dotIndex + 1);
            }
            sb.append(className)
                    .append(C.DOT_CHAR)
                    .append(element.getMethodName())
                    .append("(")
                    .append(element.getFileName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append(")")
                    .append(C.NEWLINE);
        }
        log(label, sb.toString(), LogLevel.LIFECYCLE);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     *
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

}
