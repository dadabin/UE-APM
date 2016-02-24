package com.ds.jvm.agent;

/**
 * Exception thrown in the case of an unrecorevable error.
 * 
 * @author Antonio S. R. Gomes
 */
class Profiler4JError extends Error {

    private static final long serialVersionUID = 1L;

    public Profiler4JError(String message) {
        super(message);
    }

    public Profiler4JError(String message, Throwable cause) {
        super(message, cause);
    }

    public Profiler4JError(Throwable cause) {
        super(cause);
    }

}
