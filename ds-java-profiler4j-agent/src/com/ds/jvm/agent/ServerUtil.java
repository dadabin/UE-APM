package com.ds.jvm.agent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for client-server related operations.
 * 
 * @author Antonio S. R. Gomes
 */
public class ServerUtil {

    private ServerUtil() {
        // empty;
    }

    public static ThreadInfo[] makeSerializable(java.lang.management.ThreadInfo[] mti) {
        ThreadInfo[] ti = new ThreadInfo[mti.length];
        for (int i = 0; i < ti.length; i++) {
            ti[i] = new ThreadInfo(mti[i]);
        }
        return ti;
    }

    public static void writeStringList(ObjectOutputStream out, List<String> list)
        throws IOException {
        out.writeInt(list.size());
        for (String v : list) {
            out.writeUTF(v);
        }
    }

    public static List<String> readStringList(ObjectInputStream in) throws IOException {
        int n = in.readInt();
        List<String> list = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            list.add(in.readUTF());
        }
        return list;
    }

    public static Map<String, String> readStringMap(ObjectInputStream in)
        throws IOException {
        int n = in.readInt();
        Map<String, String> map = new LinkedHashMap<String, String>(n);
        for (int i = 0; i < n; i++) {
            map.put(in.readUTF(), in.readUTF());
        }
        return map;
    }

    public static void writeStringMap(ObjectOutputStream out, Map<String, String> map)
        throws IOException {
        out.writeInt(map.size());
        for (String v : map.keySet()) {
            out.writeUTF(v);
            out.writeUTF(map.get(v));
        }
    }

    public static void writeMemoryUsage(ObjectOutputStream out, MemoryUsage mu)
        throws IOException {
        out.writeLong(mu.getInit());
        out.writeLong(mu.getUsed());
        out.writeLong(mu.getCommitted());
        out.writeLong(mu.getMax());
    }

    public static MemoryUsage readMemoryUsage(ObjectInputStream in) throws IOException {
        return new MemoryUsage(in.readLong(), in.readLong(), in.readLong(), in.readLong());
    }

}
