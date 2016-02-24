package com.ds.jvm.agent;

/**
 * Thread-local class that contains the recursive depth of a given method.
 * 
 * @author Antonio S. R. Gomes
 */
public class CFlow extends ThreadLocal<CFlow.ThreadLocalMethod> {

    @Override
    protected synchronized ThreadLocalMethod initialValue() {
        return new ThreadLocalMethod();
    }

    /**
     * Holds the recursive depth.
     */
    public static class ThreadLocalMethod {
        private int depth;
        /**
         * Gets the current depth.
         * @return depth
         */
        public int getDepth() {
            return depth;
        }
        public boolean isRecursive() {
            return depth > 1;
        }
        /**
         * Entes the methdod.
         * @return new depth
         */
        public int enter() {
            return ++depth;
        }
        /**
         * Leaves the method
         * @return new depth
         */
        public int leave() {
            return --depth;
        }
    }
}
