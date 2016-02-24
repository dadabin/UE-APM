package com.ds.jvm.agent;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


import com.ds.jvm.agent.CFlow.ThreadLocalMethod;
import com.ds.jvm.agent.Snapshot;

/**
 * The class <code>ThreadProfiler</code> is responsible for measuring the time spent in
 * method invocations. Instances of this are created per-thread.
 * 
 * @author Antonio S. R. Gomes
 */
public class ThreadProfiler {

    // //////////////////////////////////////////////////////////////////////////
    // Constants
    // //////////////////////////////////////////////////////////////////////////

    private volatile static boolean enabled = true;

    /**
     * Magic number that identifies snapshots created by Profiler4j. This should never
     * change.
     */
    public static final int SNAPSHOT_MAGIC = 0xbabaca00;
    /**
     * Current version of the protocol. Different versions may have nothing in common, so
     * there is no assumption of backwards compatiblity. This is expected to change only
     * in drastic cases.
     */
    public static final int SNAPSHOT_PROTOCOL_VERSION = 0x00000001;
    /**
     * Type of snaphot that contains the statistics of methods timings. Currently this is
     * the only type supported.
     */
    public static final int SNAPSHOT_TYPE_CALLTRACE = 0x00000001;

    private static final int MAX_METHODS = 65535;
    private static final int MAX_CALL_DEPTH = 1024;
    private static final int INITIAL_CHILDREN_PER_METHOD = 8;

    // /////////////////////////////////////////////////////////////////////////
    // Static members
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Global monitor used to implement mutual-exclusion. In the future this single
     * monitor may be broken up into many different monitors to reduce contention.
     */
    static final Object globalLock = new GlobalLock();

    private static class GlobalLock {
    }

    /**
     * The session is defined from bits 0 thru 8, and represents the number of times the
     * agent redefined the application classes. This value always starts from 0 and is
     * increased as classes are redefined.
     */
    private static volatile int sessionId = 0;
    /**
     * Current number of instrumented methods in current {@link #sessionId}. Whenever a
     * new session is created this value is set to 0.
     */
    private static int methodCount = 0;
    /**
     * Array of method stats. When a new session is created all elements are set to
     * <code>null</code>.
     */
    private static final MethodGroup[] globalMethods = new MethodGroup[MAX_METHODS];
    /**
     * Map of thread infos. Whenever a new session is created this map is cleared.
     */
    private static final Map<Thread, ThreadProfiler> globalThreadInfos = new HashMap<Thread, ThreadProfiler>();

    /**
     * Creates a new method object assigning a new GMId (Global Method Id)
     * <p>
     * A global method ID has two fundamental parts: bits 0-15 represent the method ID,
     * bits 16-23 represent the {@link #sessionId}. This means that global IDs are unique
     * for any method and for any session in the same JVM execution.
     * 
     * @param methodName name of the method to trace
     * @return global method id
     */
    public static int newMethod(String methodName) {
        synchronized (globalLock) {
            int globalMethodId = (sessionId << 16) | methodCount;
            globalMethods[methodCount++] = new MethodGroup(globalMethodId, methodName);
            if (methodCount >= globalMethods.length) {
                throw new Profiler4JError("[CRITICAL] Reached limit of traced methods");
            }
            return globalMethodId;
        }
    }

    /**
     * @return Returns the sessionId.
     */
    public static int getSessionId() {
        return sessionId;
    }

    // private static class Key {
    // ThreadProfiler tp;
    // Thread t;
    // public Key(ThreadProfiler tp) {
    // this.tp = tp;
    // }
    // }

    // private static Key[] keys;

    // private static ThreadProfiler getCurrentThreadProfiler() {
    // Thread ct = Thread.currentThread();
    // ct.
    // }

    private static final Object rl = new Object();

    private static ThreadGroup systemThreadGroup;
    static {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getThreadGroup().getName().equals("system")) {
                systemThreadGroup = t.getThreadGroup();
                break;
            }
        }
        if (systemThreadGroup == null) {
            throw new Profiler4JError(
                    "[CRITICAL] Could not determine system thread group");
        }
    }

    /**
     * Records a method's entry. This method is called by instrumented code.
     * 
     * @param globalMethodId
     * @see #newMethod(String)
     */
    public static void enterMethod(int globalMethodId) {
        final Thread ct = Thread.currentThread();
        if (ct == Agent.server || Thread.holdsLock(rl) || Thread.holdsLock(globalLock)
                || ct.getThreadGroup() == systemThreadGroup
                || ct == Transformer.transformerThread) {
            return;
        }
        if (!enabled) {
            return;
        }
        synchronized (globalLock) {
            int sessionIdOfMethod = globalMethodId >> 16;
            if (sessionIdOfMethod != sessionId) {
                return;
            }
            int mid = globalMethodId & 0xffff;
            ThreadProfiler ti = null;
            synchronized (rl) {
                ti = globalThreadInfos.get(ct);
                if (ti == null) {
                    ti = new ThreadProfiler();
                    globalThreadInfos.put(ct, ti);
                }
            }
            ti.enter0(globalMethods[mid]);
        }
    }
    /**
     * Records a method's exit. This method is called by instrumented code.
     * 
     * @param globalMethodId
     * @see #newMethod(String)
     */
    public static void exitMethod(int globalMethodId) {
        final Thread ct = Thread.currentThread();
        if (ct == Agent.server || Thread.holdsLock(rl) || Thread.holdsLock(globalLock)
                || ct.getThreadGroup() == systemThreadGroup
                || ct == Transformer.transformerThread) {
            return;
        }
        // if the current session is transient just return
        if (!enabled) {
            return;
        }
        synchronized (globalLock) {
            int sessionIdOfMethod = globalMethodId >> 16;
            if (sessionIdOfMethod != sessionId) {
                return;
            }
            ThreadProfiler ti = null;
            synchronized (rl) {
                ti = globalThreadInfos.get(ct);
            }
            if (ti != null) {
                ti.exit0();
            }
        }
    }
    /**
     * Creates a new session and increments the session counter. The newly created session
     * is transient.
     */
    public static void startSessionConfig() {
        synchronized (globalLock) {
            enabled = false;
            for (int i = 0; i < methodCount; i++) {
                globalMethods[i] = null;
            }
            methodCount = 0;
            sessionId = (sessionId + 1) & 0xff;
            Log.print(0, "Starting new session: sessionId=" + sessionId);
        }
    }

    public static void endSessionConfig() {
        synchronized (globalLock) {
            for (ThreadProfiler ti : globalThreadInfos.values()) {
                synchronized (ti) {
                    ti.depth = 0;
                }
            }
            globalThreadInfos.clear();
            enabled = true;
            Log.print(0, "Completed session configuration");
        }
    }

    /**
     * Resets all method counters. Currently active methods are not affected.
     */
    public static void resetStats() {
        synchronized (globalLock) {
            for (MethodGroup m : globalMethods) {
                if (m == null) {
                    // this happens when we pass the last method
                    break;
                }
                m.selfTime = 0;
                m.netTime = 0;
                m.childRecursiveTime = 0;
                m.hits = 0;
                for (int i = 0; i < m.childCount; i++) {
                    m.children[i] = null;
                    m.childrenTimes[i] = 0;
                }
                m.childCount = 0;
            }
            for (ThreadProfiler ti : globalThreadInfos.values()) {
                for (int i = 0; i < ti.depth; i++) {
                    ti.startTimes[i] = System.nanoTime();
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Instance members
    // /////////////////////////////////////////////////////////////////////////

    private int depth = 0;
    private long[] startTimes = new long[MAX_CALL_DEPTH];
    private MethodGroup[] stack = new MethodGroup[MAX_CALL_DEPTH];
    private Thread thread;

    /**
     * Private constructor (should be called only be the threadlocal
     * <code>initialValue()</code> method).
     */
    private ThreadProfiler() {
        this.thread = Thread.currentThread();
    }

    private void enter0(MethodGroup m) {
        try {
            ThreadLocalMethod tlm = m.cflow.get();
            if (tlm.enter() == 1) {
                startTimes[depth] = System.nanoTime();
            }
            stack[depth++] = m;
        } catch (ArrayIndexOutOfBoundsException e) {
            enabled = false;
            for (int i = 0; i < depth; i++) {
                System.err.format("[%4d] %s\n", i, stack[i].name);
            }
            throw e;
        }
    }

    public final void exit0() {
        if (depth == 0) {
            return;
        }
        depth--;
        MethodGroup m = stack[depth];
        ThreadLocalMethod tlm = m.cflow.get();
        if (tlm.leave() > 0) {
            return;
        }
        long t = System.nanoTime();
        long netTime = t - startTimes[depth];
        m.hits++;
        m.netTime += netTime;
        if (depth > 0) {
            MethodGroup pm = stack[depth - 1];
            pm.addChildTime(m, netTime);
        }
    }

    /**
     * Simulates an exit() in the whole call stack.
     * 
     */
    private static void exitWholeStack(ThreadProfiler ti, MethodGroup[] globalMethods_) {
        int depth_ = ti.depth;
        long t = System.nanoTime();
        while (depth_ > 0) {
            depth_--;
            long netTime = t - ti.startTimes[depth_];
            MethodGroup m = ti.stack[depth_];
            m.hits++;
            m.netTime += netTime;
            if (depth_ > 0) {
                MethodGroup pm = ti.stack[depth_ - 1];
                pm.addChildTime(m, netTime);
            }
        }
    }

    /**
     * Creates a snapshot with all methods. If a method is currently active it is assumed
     * a <code>methodExit</code> now.
     */
    public static void createSnapshot(OutputStream os) throws IOException {
        MethodGroup[] methods_ = null;
        synchronized (globalLock) {
            methods_ = new MethodGroup[methodCount];
            for (int i = 0; i < methodCount; i++) {
                methods_[i] = new MethodGroup(globalMethods[i]);
            }
            for (ThreadProfiler ti : globalThreadInfos.values()) {
                if (ti.thread.isAlive()) {
                    exitWholeStack(ti, methods_);
                }
            }
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            serialize(dos, methods_);
            dos.flush();
        }
    }
    
    public static void createSnapshot(StringBuilder sb)throws IOException{
    	 MethodGroup[] methods_ = null;
         synchronized (globalLock) {
             methods_ = new MethodGroup[methodCount];
             for (int i = 0; i < methodCount; i++) {
                 methods_[i] = new MethodGroup(globalMethods[i]);
             }
             for (ThreadProfiler ti : globalThreadInfos.values()) {
                 if (ti.thread.isAlive()) {
                     exitWholeStack(ti, methods_);
                 }
             }
             serialize(sb, methods_);
         }
        
    }

    /**
     * Simulates a binary transfer of snapshot information to create
     * a new Snapshot.
     */
    public static Snapshot createSnapshot() throws IOException {
        MethodGroup[] methods_ = null;
        synchronized (globalLock) {
            // Gather all necessary information.
            methods_ = new MethodGroup[methodCount];
            for (int i = 0; i < methodCount; i++) {
                methods_[i] = new MethodGroup(globalMethods[i]);
            }
            for (ThreadProfiler ti : globalThreadInfos.values()) {
                if (ti.thread.isAlive()) {
                    exitWholeStack(ti, methods_);
                }
            }
            
            // Serialize the snapshot to a in-memory-stream.
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            serialize(dos, methods_);
            dos.flush();
            
            // Deserialize it using the Snapshot internal method.
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            return Snapshot.read(is);
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // Inner classes
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Internal data structure that contains statistics about a single method. It is
     * optimized for performance and should not be used outside this file.
     */
    private static class MethodGroup {

        public int globalId;
        public String name;
        public long netTime;
        public long childRecursiveTime;
        public long selfTime;
        public int hits;
        public int childCount;
        public MethodGroup[] children = new MethodGroup[INITIAL_CHILDREN_PER_METHOD];
        public long[] childrenTimes = new long[INITIAL_CHILDREN_PER_METHOD];
        public CFlow cflow = new CFlow();

        /**
         * Constructor.
         * @param composedId
         * @param name
         */
        public MethodGroup(int composedId, String name) {
            this.globalId = composedId;
            this.name = name;
        }

        /**
         * Copy constructor.
         * @param m prototype instance
         */
        public MethodGroup(MethodGroup m) {
            globalId = m.globalId;
            name = m.name;
            netTime = m.netTime;
            selfTime = m.selfTime;
            hits = m.hits;
            childCount = m.childCount;
            children = new MethodGroup[m.children.length];
            System.arraycopy(m.children, 0, children, 0, m.childCount);
            childrenTimes = new long[m.childrenTimes.length];
            System.arraycopy(m.childrenTimes, 0, childrenTimes, 0, m.childCount);
        }

        /**
         * Updates this method's info with the time spent in a given child.
         * 
         * @param child child method
         * @param time time spent in the child
         */
        public void addChildTime(MethodGroup child, long time) {
            /*
             * We may need to improve this search in the future. However, as far as I�ve
             * measured, it does not seem have a significant perfomance penalty right now,
             * as most of the time the child count are really small (less than 10). It is
             * worth noting that some complex programs suchs as JBoss require very large
             * child arrays.
             */
            int index = -1;
            for (int i = 0; i < childCount; i++) {
                if (children[i] == child) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                // Resize children arrays if required
                if (childCount == children.length) {
                    int newChildCount = childCount + (childCount >> 1);
                    MethodGroup[] oldChildren = children;
                    children = new MethodGroup[newChildCount];
                    System.arraycopy(oldChildren, 0, children, 0, childCount);
                    long[] oldChildrenTimes = childrenTimes;
                    childrenTimes = new long[newChildCount];
                    System.arraycopy(oldChildrenTimes, 0, childrenTimes, 0, childCount);
                }
                // If the supplied childId was not found then add it to this group�s list.
                children[childCount] = child;
                index = childCount++;
            }
            childrenTimes[index] += time;
        }

        public void reset() {
            hits = 0;
            netTime = 0;
            selfTime = 0;
        }

        @Override
        public boolean equals(Object obj) {
            return globalId == ((MethodGroup) obj).globalId;
        }

        @Override
        public int hashCode() {
            return globalId;
        }
    }

    /**
     * Serializes the statistics about a group of methods.
     * 
     * @param out output to write
     * @param methods array of methods
     * @throws IOException
     */
    private static void serialize(DataOutputStream out, MethodGroup[] methods)
        throws IOException {
        out.writeInt(SNAPSHOT_MAGIC);
        out.writeInt(SNAPSHOT_PROTOCOL_VERSION);
        out.writeInt(SNAPSHOT_TYPE_CALLTRACE);
        out.writeInt(sessionId);
        out.writeLong(System.currentTimeMillis());
        int n = 0;
        for (MethodGroup m : methods) {
            if (m.hits > 0) {
                n++;
            }
        }
        Log.print(0, "Retrieving " + n + " methods");
        out.writeInt(n);
        for (MethodGroup m : methods) {
            if (m.hits == 0) {
                continue;
            }
            out.writeInt(m.globalId & 0xffff);
            out.writeUTF(m.name);
            out.writeInt(m.hits);
            out.writeLong(m.netTime);
            out.writeLong(m.selfTime);
            out.writeInt(m.childCount);
            for (int i = 0; i < m.childCount; i++) {
                out.writeInt(m.children[i].globalId & 0xffff);
                out.writeLong(m.childrenTimes[i]);
            }
        }
    }
    
    /**
     * Serializes the statistics about a group of methods.
     * 
     * @param out output to write
     * @param methods array of methods
     * @throws IOException
     */
    private static void serialize(StringBuilder sb, MethodGroup[] methods)
        throws IOException {
    	sb.append("snapshot_magic:"+SNAPSHOT_MAGIC+",snapshot_protocol_version:"+SNAPSHOT_PROTOCOL_VERSION
    			+",snapshot_type_calltrace:"+SNAPSHOT_TYPE_CALLTRACE+",session_id:"+sessionId+"\n");
    	sb.append(System.currentTimeMillis()+"\n");
        int n = 0;
        for (MethodGroup m : methods) {
            if (m.hits > 0) {
                n++;
            }
        }
        Log.print(0, "Retrieving " + n + " methods");
        sb.append(n+"\n");
        
        
        for (MethodGroup m : methods) {
            if (m.hits == 0) {
                continue;
            }
            sb.append(
            		"globalId:"+(m.globalId & 0xffff)
            		+",name:"+m.name
            		+",hits:"+m.hits
            		+",netTime:"+m.netTime
            		+",childCout:"+m.childCount
            		+"\n");
            sb.append("@["+m.childCount+"\n");
            for (int i = 0; i < m.childCount; i++) {
            	sb.append((m.children[i].globalId & 0xffff)+","+m.childrenTimes[i]+"\n");
            }
        }
    }

}
