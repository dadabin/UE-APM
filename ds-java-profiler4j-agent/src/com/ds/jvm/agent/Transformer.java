package com.ds.jvm.agent;

import static com.ds.jvm.agent.Log.print;
import static com.ds.jvm.agent.ThreadProfiler.globalLock;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * Class file transformer (this class is tricky).
 * 
 * @author Antonio S. R. Gomes
 */
public class Transformer implements ClassFileTransformer {

    private Config config;

    /**
     * Timesstamp of the last call to
     * {@link #scheduleRedefine(ClassLoader, String, Callback)}
     */
    private static long lastTimestamp = System.currentTimeMillis();

    /** List of classes scheduled to be reloaded by the transformer thread */
    private static List<ClassBatchEntry> scheduledClasses = new ArrayList<ClassBatchEntry>();

    static final Thread transformerThread = new TransformerThread();
    static {
        transformerThread.start();
    }

    /**
     * 
     * @param config 
     */
    public Transformer(Config config) {
        this.config = config;
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] currentBytes) throws IllegalClassFormatException {
        if (Agent.beingShutdown) {
            return null;
        }
        className = className.replace('/', '.');
        if (rejectByDefault(className)) {
            return null;
        }
        try {
            synchronized (globalLock) {
                if (!ClassUtil.getClassFile(className, loader).exists()) {
                    ClassUtil.saveClassBackup(className, loader, currentBytes);
                }
                if (classBeingRedefined == null) {
                    scheduleRedefine(loader, className, null);
                    return null;
                } else {
                    if (Thread.currentThread() != transformerThread) {
                        throw new Profiler4JError(
                                "[CRITICAL] Cannot redefine from any other thread than "
                                        + transformerThread.getName());
                    }
                    return BytecodeTransformer.transform(className,
                                                         loader,
                                                         currentBytes,
                                                         config);
                }
            }
        } catch (Throwable t) {
            print(0, "Could not transform class " + className, t);
            if (config.isExitVmOnFailure()) {
                System.exit(1);
            }
        }
        return null;
    }

    /**
     * Checks whether a class should be simply ignored by the profiler in every possible
     * situation.
     * 
     * @param className Name of the class to check
     * @return <code>true</code> if the class should be rejected, <code>false</code>
     *         otherwise
     */
    public boolean rejectByDefault(String className) {
        if (className == null) {
            return true;
        }
        if (className.startsWith("net.sf.profiler4j.test.")) {
            return false;
        }
        //
        // *** WARNING ****
        // These entries were carefully chosen in order to avoid critical problems (JVM
        // crashes, stack overflows, cyclic dependencies, etc.). Only remove one of these
        // if you are really sure.
        // 
        // You've been warned, that is, _DON'T EMAIL ME ASKING FOR HELP_
        //
        if (className.startsWith("[")
                // Packages org.objectweb.asm.** are repackaged to be under
                // net.sf.profiler4j.agent., so we must comment this out
                // || className.startsWith("org.objectweb.asm.")
                || className.startsWith("net.sf.profiler4j.agent.")
                // These cause recursive calls through the profiler. Some of these can be
                // avoided (java.util) with some tweaks in the class ThreadProfiler. The
                // others are much more complex and would be viable only with a profiler
                // written entirely in native code. However, they don't seem that
                // important anyway.
                || className.startsWith("java.util.")
                || className.startsWith("java.lang.")
                || className.startsWith("java.lang.Thread")
                || className.startsWith("java.lang.reflect.")
                || className.startsWith("java.lang.ref.")

                // These misteriously raise an java.lang.NoClassDefFoundError
                || className.equals("com.sun.tools.javac.util.DefaultFileManager")

                // These cause an ugly JVM crash! Pretty weird.
                || className.startsWith("sun.security.")
                || className.startsWith("java.security.MessageDigest$")
                || className.startsWith("sun.reflect.")

                // Dynamic proxies
                || className.startsWith("$Proxy") || className.contains("ByCGLIB$$")) {
            return true;
        }
        if (config.getExclusions() != null) {
            for (String cn : config.getExclusions()) {
                if (className.startsWith(cn)) {
                    return true;
                }
            }
        }
        return false;

    }
    /**
     * Requests a class to be redefined.
     * <p>
     * Redefinition will be performed in the near feature (typically a second or so) by
     * the TransformerThread. If the class identified by <code>className</code> matches
     * at least one ACCEPT rule then it will be instrumented as a consequence of being
     * reloaded.
     * 
     * @param loader Class loader or <code>null</code> (boot class loader)
     * @param className Fully qualified name of the class
     * @param callback 
     */
    public static void scheduleRedefine(ClassLoader loader,
                                        String className,
                                        Callback callback) {
        if (Agent.beingShutdown) {
            return;
        }
        synchronized (scheduledClasses) {
            ClassBatchEntry key = new ClassBatchEntry();
            key.className = className;
            key.loader = new WeakReference<ClassLoader>((loader == null) ? ClassLoader
                .getSystemClassLoader() : loader);
            key.callback = callback;
            lastTimestamp = System.currentTimeMillis();
            scheduledClasses.add(key);
            Log.print(1, "Scheduling redefinition of " + className);
        }
    }

    /**
     * Simple strucuture that holds the information required to redefine a class.
     * @author Antonio S. R. Gomes
     */
    private static class ClassBatchEntry {
        String className;
        WeakReference<ClassLoader> loader;
        Callback callback;
    }

    /**
     * 
     */
    public static interface Callback {
        /**
         * 
         * @param className 
         * @param backSequence 
         * @param bachSize 
         */
        void notifyClassTransformed(String className, int backSequence, int bachSize);
    }

    /**
     * Thread responsible for reloading classes in background, triggering bytecode
     * transformation if required.
     * <p>
     * This thread will only reload after a certain minimum interval of inactivity of
     * class loading. This is done so in order to avoid dependency problems.
     * 
     * @author Antonio S. R. Gomes
     */
    private static class TransformerThread extends Thread {
        /**
         * Number of milliseconds the thread sleeps after running a batch or verifying
         * that the queue is empty
         */
        private static final int SLEEP_INTERVAL = 100;
        /**
         * Minimum number of milliseconds after the last call to
         * {@link Transformer#scheduleRedefine(ClassLoader, String, Callback)} this thread
         * will require in order to start processing the redefinition queue.
         */
        private static final int STABILIZATION_INTERVAL = 1000;
        //
        public TransformerThread() {
            super("P4J_TRANSFORMER");
            setPriority(MIN_PRIORITY);
            setDaemon(true);
        }
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException e) {
                    // ignore
                }
                if (Agent.beingShutdown) {
                    return;
                }
                if ((System.currentTimeMillis() - lastTimestamp) < STABILIZATION_INTERVAL) {
                    // Go to sleep if classes are still being scheduled...
                    continue;
                }
                final ClassBatchEntry[] array;
                synchronized (scheduledClasses) {
                    if (scheduledClasses.isEmpty()) {
                        continue;
                    }
                    array = scheduledClasses.toArray(new ClassBatchEntry[0]);
                    scheduledClasses.clear();
                }
                print(0, "Starting to reload scheduled classes (" + array.length
                        + " in queue)...");
                int errors = 0;
                int n = 0;
                long t0 = System.currentTimeMillis();
                int batchSequence = 0;
                for (ClassBatchEntry key : array) {
                    if (Agent.beingShutdown) {
                        return;
                    }
                    try {
                        Class cl = null;
                        ClassLoader loader = key.loader.get();
                        if (loader != null) {
                            try {
                                // It may happen that the class loader was already reaped
                                // by the garbagge collector, so we must be careful here
                                cl = Class.forName(key.className, false, loader);
                            } catch (NoClassDefFoundError e) {
                                // Probably the other class which was available when
                                // the current class was compiled is not available. In
                                // this case just ignore it (leave cl == null)
                            }
                        }
                        if (cl != null && Agent.mustRedefineClass(cl)) {
                            n++;
                            byte[] bytes = ClassUtil.loadClassBackup(cl);
                            print(1, "Reloading class " + key.className);
                            ClassDefinition cd = new ClassDefinition(cl, bytes);
                            Agent.inst.redefineClasses(new ClassDefinition[]{cd});
                        }
                    } catch (Throwable e) {
                        errors++;
                        print(0, "*** ERROR reloading " + key.className, e);
                    }
                    batchSequence++;
                    if (key.callback != null) {
                        key.callback.notifyClassTransformed(key.className,
                                                            batchSequence,
                                                            array.length);
                    }
                }
                long dt = System.currentTimeMillis() - t0;
                if (errors == 0) {
                    if (n == 0) {
                        print(0, "No classes needed to be reloaded");
                    } else {
                        print(0, "Reloaded " + n + " classes in " + dt + "ms");
                    }
                } else {
                    print(0, "Reloaded " + n + " classes in " + dt + "ms  (" + errors
                            + " error(s) found)");
                }
            }
        }
    }

}
