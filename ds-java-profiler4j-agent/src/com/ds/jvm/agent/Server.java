package com.ds.jvm.agent;

import static com.ds.jvm.agent.Log.print;




/**
 * Single-thread daemon that allow remote connections from the Profiler4j Console.
 *
 * @author Antonio S. R. Gomes
 */
class Server extends Thread {

    private Config config;

    public Server(Config config) {
        super("Disruptive4J_SERVER");
        this.config = config;
        setDaemon(true);
        setPriority(MAX_PRIORITY);
    }

    @Override
    public void run() {
        try {
        	new HttpServer().service();
        } catch (Throwable e) {
            print(0, "Server exception", e);
            if (config.isExitVmOnFailure()) {
                print(0, "Aborting JVM...");
                System.exit(3);
            }
        } finally {
            print(0, "Server exiting");
        }
    }
}
