package com.ds.jvm.agent;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Serializable class equivalent to {@link java.lang.management.ThreadInfo}, which is
 * declared non-serializable.
 * 
 * @author Antonio S. R. Gomes
 */
public class ThreadInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String threadName;
    private long threadId;
    private long blockedTime;
    private long blockedCount;
    private long waitedTime;
    private long waitedCount;
    private String lockName;
    private long lockOwnerId;
    private String lockOwnerName;
    private boolean inNative;
    private boolean suspended;
    private Thread.State threadState;
    private StackTraceElement[] stackTrace;
    
    public ThreadInfo() {
        // default constructor
    }
    
    public ThreadInfo(java.lang.management.ThreadInfo mti) {
        threadName = mti.getThreadName();
        threadId = mti.getThreadId();
        blockedTime = mti.getBlockedTime();
        blockedCount = mti.getBlockedCount();
        waitedTime = mti.getWaitedTime();
        waitedCount = mti.getWaitedCount();
        lockName = mti.getLockName();
        lockOwnerId = mti.getLockOwnerId();
        lockOwnerName = mti.getLockOwnerName();
        inNative = mti.isInNative();
        suspended = mti.isSuspended();
        threadState = mti.getThreadState();
        stackTrace = mti.getStackTrace();
    }
    
    @Override
    public String toString() {
        return "ThreadInfo [threadName=" + this.threadName + ", threadId=" + this.threadId
                + ", blockedTime=" + this.blockedTime + ", blockedCount="
                + this.blockedCount + ", waitedTime=" + this.waitedTime + ", waitedCount="
                + this.waitedCount + ", lockName=" + this.lockName + ", lockOwnerId="
                + this.lockOwnerId + ", lockOwnerName=" + this.lockOwnerName
                + ", inNative=" + this.inNative + ", suspended=" + this.suspended
                + ", threadState=" + this.threadState + ", stackTrace="
                + Arrays.toString(this.stackTrace) + ", getBlockedCount()="
                + this.getBlockedCount() + ", getBlockedTime()=" + this.getBlockedTime()
                + ", isInNative()=" + this.isInNative() + ", getLockName()="
                + this.getLockName() + ", getLockOwnerId()=" + this.getLockOwnerId()
                + ", getLockOwnerName()=" + this.getLockOwnerName() + ", getStackTrace()="
                + Arrays.toString(this.getStackTrace()) + ", isSuspended()="
                + this.isSuspended() + ", getThreadId()=" + this.getThreadId()
                + ", getThreadName()=" + this.getThreadName() + ", getThreadState()="
                + this.getThreadState() + ", getWaitedCount()=" + this.getWaitedCount()
                + ", getWaitedTime()=" + this.getWaitedTime() + ", getClass()="
                + this.getClass() + ", hashCode()=" + this.hashCode() + ", toString()="
                + super.toString() + "]";
    }

    public long getBlockedCount() {
        return this.blockedCount;
    }
    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }
    public long getBlockedTime() {
        return this.blockedTime;
    }
    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }
    public boolean isInNative() {
        return this.inNative;
    }
    public void setInNative(boolean inNative) {
        this.inNative = inNative;
    }
    public String getLockName() {
        return this.lockName;
    }
    public void setLockName(String lockName) {
        this.lockName = lockName;
    }
    public long getLockOwnerId() {
        return this.lockOwnerId;
    }
    public void setLockOwnerId(long lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }
    public String getLockOwnerName() {
        return this.lockOwnerName;
    }
    public void setLockOwnerName(String lockOwnerName) {
        this.lockOwnerName = lockOwnerName;
    }
    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }
    public void setStackTrace(StackTraceElement[] stackTrace) {
        this.stackTrace = stackTrace;
    }
    public boolean isSuspended() {
        return this.suspended;
    }
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
    public long getThreadId() {
        return this.threadId;
    }
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }
    public String getThreadName() {
        return this.threadName;
    }
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    public Thread.State getThreadState() {
        return this.threadState;
    }
    public void setThreadState(Thread.State threadState) {
        this.threadState = threadState;
    }
    public long getWaitedCount() {
        return this.waitedCount;
    }
    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }
    public long getWaitedTime() {
        return this.waitedTime;
    }
    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

}
