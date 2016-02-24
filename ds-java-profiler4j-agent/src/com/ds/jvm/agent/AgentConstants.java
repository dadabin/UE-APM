package com.ds.jvm.agent;

/**
 * Constants used by the agent.
 * 
 * @author Antonio S. R. Gomes
 */
public class AgentConstants {

    public static final String VERSION = "1.0-alpha4";
    
    public static final int CMD_GC = 1;
    public static final int CMD_SNAPSHOT = 2;
    public static final int CMD_RESET_STATS = 3;
    public static final int CMD_DISCONNECT = 4;
    public static final int CMD_APPLY_RULES = 5;
    public static final int CMD_LIST_CLASSES = 6;
    public static final int CMD_GET_RUNTIME_INFO = 7;
    public static final int CMD_GET_MEMORY_INFO = 8;
    public static final int CMD_GET_THREAD_INFO = 9;
    public static final int CMD_SET_THREAD_MONITORING = 10;

    public static final int COMMAND_ACK = 0x00;
    public static final int STATUS_ERROR = 0x01;
    public static final int STATUS_UNKNOWN_CMD = 0x02;

}
