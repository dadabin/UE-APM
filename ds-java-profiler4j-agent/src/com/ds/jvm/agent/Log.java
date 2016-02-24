package com.ds.jvm.agent;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {

    private static String PREFFIX = "[Disruptive4J";
    private static int verbosity = 0;
    private static boolean showThread = true;

    public static void setVerbosity(int verbosity) {
        Log.verbosity = verbosity;
    }

    public static void print(int verbosity, Object s) {
        if (verbosity <= Log.verbosity) {
            System.out.println(preffix() + s);
        }
    }
    public static void print(int verbosity, Object s, Throwable t) {
        if (verbosity <= Log.verbosity) {
            System.out.println(preffix() + s);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            for (String l : sw.toString().split("[\n\r]+")) {
                System.out.println(preffix() + l);
            }

        }
    }

    private static String preffix() {
        return (PREFFIX + ":" + verbosity)
                + (showThread ? (":" + Thread.currentThread().getName()) : "") + "] ";
    }

}
