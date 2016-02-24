package com.ds.jvm.agent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.ds.jvm.agent.ThreadProfiler;

/**
 * <b>Important:</b> All times are given in milliseconds.
 * 
 * @author Antonio S. R. Gomes
 */
public class Snapshot {

    public static Snapshot load(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return read(bais);
    }

    public static Snapshot load(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
        try {
            return read(bis);
        } finally {
            bis.close();
        }
    }

    public static Snapshot read(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        Snapshot snapshot = new Snapshot();
        if (dis.readInt() != ThreadProfiler.SNAPSHOT_MAGIC) {
            throw new IOException("Invalid magic number");
        }
        if (dis.readInt() != ThreadProfiler.SNAPSHOT_PROTOCOL_VERSION) {
            throw new IOException("Unsupported protocol version");
        }
        if (dis.readInt() != ThreadProfiler.SNAPSHOT_TYPE_CALLTRACE) {
            throw new IOException("Dump type mismath");
        }
        dis.readInt();
        snapshot.time = dis.readLong();       
        int mCount = dis.readInt();
        snapshot.methods = new LinkedHashMap<Integer, Method>(mCount);
        for (int i = 0; i < mCount; i++) {
            Method m = new Method();
            m.id = dis.readInt();
            m.name = dis.readUTF();
            m.hits = dis.readInt();
            m.netTime = dis.readLong() / 1e6;
            m.selfTime = dis.readLong() / 1e6;
            int childCount = dis.readInt();
            m.tmp_childrenIds = new int[childCount];
            m.tmp_childrenTimes = new long[childCount];
            for (int j = 0; j < childCount; j++) {
                m.tmp_childrenIds[j] = dis.readInt();
                m.tmp_childrenTimes[j] = dis.readLong();
            }
            snapshot.methods.put(m.id, m);
        }
        for (Method m : snapshot.methods.values()) {
            m.childrenTimes = new HashMap<Method, Double>(m.tmp_childrenIds.length);
            for (int i = 0; i < m.tmp_childrenIds.length; i++) {
                m.childrenTimes.put(snapshot.methods.get(m.tmp_childrenIds[i]),
                                    m.tmp_childrenTimes[i] / 1e6);
            }
            m.tmp_childrenIds = null;
            m.tmp_childrenTimes = null;
        }
        return snapshot;
    }

    private Properties systemProperties;
    private Map<Integer, Method> methods;
    private long time;

    /**
     * @return Returns the methods.
     */
    public Map<Integer, Method> getMethods() {
        return this.methods;
    }

    /**
     * @return Returns the systemProperties.
     */
    public Properties getSystemProperties() {
        return this.systemProperties;
    }

    /**
     * @return Returns the time.
     */
    public long getTime() {
        return this.time;
    }

    public static class Method {

        private String className = null;
        private String classSimpleName = null;
        private String methodName = null;

        private int id;
        private String name;
        private double netTime;
        private double selfTime;
        private int hits;
        private Map<Method, Double> childrenTimes;

        private int[] tmp_childrenIds;
        private long[] tmp_childrenTimes;

        public double getTotalChildrenTime() {
            return netTime - selfTime;
        }

        /**
         * Gets the time spent in a child method.
         * 
         * @param method method
         * @return time in nanoseconds
         */
        public double getChildTime(Method method) {
            return childrenTimes.get(method);
        }

        /**
         * @return Returns the childrenTimes.
         */
        public Map<Method, Double> getChildrenTimes() {
            return this.childrenTimes;
        }

        public int getHits() {
            return hits;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMethodName() {
            if (methodName == null) {
                int p1 = name.indexOf('(');
                int p0 = name.substring(0, p1).lastIndexOf('.');
                methodName = name.substring(p0 + 1, p1);
            }
            return methodName;
        }

        public String getClassName() {
            if (className == null) {
                int p1 = name.indexOf('(');
                int p0 = name.substring(0, p1).lastIndexOf('.');
                className = name.substring(0, p0);
            }
            return className;
        }

        /**
         * @return Returns the classSimpleName.
         */
        public String getClassSimpleName() {
            if (classSimpleName == null) {
                int p = getClassName().lastIndexOf('.');
                if (p == -1) {
                    classSimpleName = className;
                } else {
                    classSimpleName = name.substring(p + 1);
                }
            }
            return classSimpleName;
        }

        public double getSelfTime() {
            return selfTime;
        }

        public double getNetTime() {
            return netTime;
        }

        @Override
        public boolean equals(Object obj) {
            return name.equals(((Method) obj).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}