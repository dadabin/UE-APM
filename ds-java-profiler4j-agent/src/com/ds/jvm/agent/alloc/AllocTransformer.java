/*
 * Copyright 2006 Antonio S. R. Gomes
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.ds.jvm.agent.alloc;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * IMPORTANT: THIS IS EXPERIMENTAL CODE
 * 
 * @author Antonio S. R. Gomes
 */
public class AllocTransformer {

    // private static final byte[] classBuffer = new byte[64000];

    /**
     * Premain method called before the application being profile is initialized.
     * 
     * @param inst instance of the instrumentation service
     * 
     * @throws IOException
     * @throws UnmodifiableClassException
     * @throws ClassNotFoundException
     */
    public static void instrument(Instrumentation inst)
        throws ClassNotFoundException, UnmodifiableClassException, IOException {

        inst.redefineClasses(new ClassDefinition[]{getRedefinedClass(Object.class)});

    }

    private static Thread dumpThread = new Thread() {
        public void run() {
            long last = 0;
            while (true) {
                synchronized (AllocTransformer.class) {
                    lastSecCount = count - last;
                    // System.err.println("INSTANCE: COUNT = " + count + " DIFF=" +
                    // lastSecCount);
                    last = count;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    };

    static {
        dumpThread.setName("P4j_ALLOC_TRACER");
        dumpThread.setDaemon(true);
        dumpThread.start();
    }

    private static ClassDefinition getRedefinedClass(Class c) throws IOException {
        // ClassPool classPool = new ClassPool();
        // classPool.appendSystemPath();
        // classPool.insertClassPath(new ByteArrayClassPath(c.getName(),
        // getClassBytes(c)));
        // classPool.appendClassPath(new LoaderClassPath(c.getClassLoader()));
        //
        // CtClass cc = classPool.get(c.getName());
        // Log.print(2, "Class " + cc.getName());
        // for (CtConstructor cm : cc.getDeclaredConstructors()) {
        // Log.print(2, String.format(" Constructor %s%s\n", cm.getName(),
        // cm.getSignature()));
        // CodeAttribute codeAttr = cm.getMethodInfo().getCodeAttribute();
        // CodeIterator it = codeAttr.iterator();
        // it.skipConstructor();
        // Bytecode code = new Bytecode(cc.getClassFile().getConstPool());
        // code.addInvokestatic(AllocTransformer.class.getName(), "newInstance", "()V");
        // it.insert(code.get());
        // }
        //
        // byte[] b = cc.toBytecode();
        // // cc.writeFile("c:\\temp\\classes_debug");
        // ClassDefinition cd = new ClassDefinition(c, cc.toBytecode());
        // Log.print(2, String.format(" Instrumented %s (%d bytes)\n", c.getName(),
        // b.length));
        // return cd;
        return null;
    }

    private static long count = 0;
    private static long lastSecCount = 0;

    public static long[] getInstanceCount() {
        synchronized (AllocTransformer.class) {
            return new long[]{count, lastSecCount};
        }
    }

    public static void newInstance() {
        synchronized (AllocTransformer.class) {
            count++;
            if ((count % 10000) == 0) {
                StackTraceElement[] st = Thread.currentThread().getStackTrace();
                /*
                 * [0] = java.lang.Thread.dumpThreads(Native Method) [1] =
                 * java.lang.Thread.getStackTrace(Thread.java:1383) [2] =
                 * net.sf.profiler4j.other.Transformer.newInstance(Transformer.java:126)
                 * [3] = java.lang.Object.<init>(Object.java:20)
                 */
                for (int i = 3; i < st.length; i++) {
                    if (!st[i].getMethodName().equals("<init>")) {
                        // System.err.println(st[i]);
                        break;
                    }
                }
            }
        }
    }
}
