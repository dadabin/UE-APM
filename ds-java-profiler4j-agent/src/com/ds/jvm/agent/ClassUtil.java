package com.ds.jvm.agent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility methods for class manipulation.
 * @author Antonio S. R. Gomes
 */
public class ClassUtil {

    public static final AtomicLong totalWriteTime = new AtomicLong(0);
    public static final AtomicLong totalReadTime = new AtomicLong(0);

    /**
     * Saves the bytes of a given class in the backup directory.
     * @param className
     * @param loader
     * @param classBytes
     * @throws IOException
     */
    public static void saveClassBackup(String className,
                                       ClassLoader loader,
                                       byte[] classBytes) throws IOException {
        long t0 = System.nanoTime();
        try {
            File bkpFile = getClassFile(className, loader);
            if (bkpFile.exists()) {
                throw new Profiler4JError("Backup file already exists: " + bkpFile);
            }
            Log.print(2, "saving backup " + bkpFile);
            FileOutputStream fos = new FileOutputStream(bkpFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(classBytes);
            bos.close();
        } finally {
            long dt = System.nanoTime() - t0;
            totalWriteTime.addAndGet(dt);
        }
    }

    /**
     * Gets the bytes that originally defined a given class.
     * <p>
     * This method tries first to load the class from a backup file created in the
     * filesystem just before that class was enhanced.
     * 
     * @param c class to load
     * @return class bytes or <code>null</code> if class could not be loaded
     * @throws IOException if an I/O occurred
     * 
     * @see #getClassFile(String, ClassLoader)
     */
    public static byte[] loadClassBackup(Class c) throws IOException {
        long t0 = System.nanoTime();
        try {
            File classFile = getClassFile(c);
            if (classFile.exists()) {
                Log.print(2, "retrieving backup " + classFile);
                byte[] buffer = new byte[(int) classFile.length()];
                FileInputStream fis = new FileInputStream(classFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(buffer);
                bis.close();
                return buffer;
            }
            return null;
        } finally {
            long dt = System.nanoTime() - t0;
            totalReadTime.addAndGet(dt);
        }
    }

    /**
     * Gets a file that is uniquely assigned to a given class.
     * @param clazz class
     * @return assigned file (which is always the same given the class name and the
     *         classloader)
     * @see #getClassFile(String, ClassLoader)
     */
    public static File getClassFile(Class clazz) {
        ClassLoader loader = clazz.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        return getClassFile(clazz.getName(), loader);
    }

    private static final byte[] classBuffer = new byte[64000];
    public static byte[] loadClassBytesAsResource(Class clazz) throws IOException {
        if (clazz.isSynthetic()) {
            throw new Profiler4JError("Cannot load a synthetic class");
        }
        String name = "/" + clazz.getName().replaceAll("\\.", "/") + ".class";
        InputStream is = clazz.getResourceAsStream(name);
        if (is == null) {
            throw new Profiler4JError("Could not find resource for " + clazz);
        }
        BufferedInputStream bis = new BufferedInputStream(is);
        synchronized (classBuffer) {
            int n = bis.read(classBuffer);
            byte[] buffer = new byte[n];
            System.arraycopy(classBuffer, 0, buffer, 0, n);
            // Log.print(1, String.format("Retrieving as resource class %s (%d bytes)",
            // clazz.getName(),
            // n));
            // ClassUtil.saveClassBackup(Object.class.getName(), null, buffer);
            return buffer;
        }
    }

    /**
     * Gets a file that is uniquely assigned to a given class name in a given class
     * loader.
     * 
     * @param className class name
     * @param loader class loaded in which the class is defined
     * @return assigned file (which is always the same given the class name and the
     *         classloader)
     */
    public static File getClassFile(String className, ClassLoader loader) {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        String preffix = (loader == null) ? "BOOTCL_" : String.format("CL@%08X_", System
            .identityHashCode(loader));
        return new File(getUninstrumentedDir(), preffix + className.replace('.', '_'));
    }

    public static File getUninstrumentedDir() {
        return new File(getTempDir().getAbsolutePath(), "uninstrumented_classes");
    }

    public static File getTempDir() {
        return new File(System.getProperty("java.io.tmpdir"), "profiler4j.tmp");
    }

    private static File lockFile;
    private static FileLock lock;
    private static FileChannel channel;

    public static void releaseLock() {
        if (lock != null) {
            try {
                lock.release();
                channel.close();
                lock = null;
                channel = null;
                lockFile.delete();
                _removeTempDir();
                Log.print(0, "Lock released successfully");
            } catch (IOException e) {
                Log.print(0, "Caught an I/O error while releasing lock: "
                        + e.getMessage());
            }
        }
    }

    private static void _removeTempDir() {
        Log.print(0, "Cleaning work directory...");
        final File[] files = getUninstrumentedDir().listFiles();
        if (files != null) {
            int n = 0;
            for (File f : files) {
                if (f.delete()) {
                    n++;
                } else {
                    f.deleteOnExit();
                }
            }
            Log.print(0, "Removed " + n + " classes in backup dir");
        }
        getUninstrumentedDir().deleteOnExit();
        getTempDir().deleteOnExit();
    }

    public static boolean acquireLock() {
        getTempDir().mkdirs();
        Log.print(0, "Work dir set to " + getTempDir());
        try {
            Log.print(0, "Acquiring lock on work directory...");
            lockFile = new File(getTempDir(), ".lock");
            channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
        } catch (IOException e) {
            throw new Profiler4JError("I/O error while acquiring lock", e);
        }
        if (lock == null) {
            Log.print(0, "ERROR: Another Profiler4j Agent is using the current work dir");
            return false;
        }
        if (getTempDir().exists()) {
            _removeTempDir();
        }
        getUninstrumentedDir().mkdirs();
        Log.print(0, "Lock acquired successfully");
        return true;
    }
}
