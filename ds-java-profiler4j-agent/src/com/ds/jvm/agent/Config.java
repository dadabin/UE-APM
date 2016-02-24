package com.ds.jvm.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import com.ds.jvm.agent.Rule.Option;


/**
 * Agent configuration.
 * 
 * @author Antonio S. R. Gomes
 */
public class Config {

    private Map<Option, String> defaultRuleOptions = new HashMap<Option, String>();
    private Map<Option, String> lastDefaultRuleOptions = new HashMap<Option, String>();
    private List<Rule> rules;
    private List<Rule> lastRules;

    private boolean traceAllocations = false;
    private int verbosity = 0;
    private boolean enabled = true;
    static int port = 7890;
    private boolean exitVmOnFailure = true;
    private boolean waitConnection = true;
    
    /** If to dump a snapshot in binary format on exit. */
    private boolean saveSnapshotOnExit = false;
    
    private static File tempDir;
    private boolean dumpClasses = false;
    private String password = null;
    private File dumpDir;
    private String[] exclusions;
    private int sessionVersion;
    
    /** If to dump a snapshot in XML format right before exiting. */ 
    private boolean takeSnapshotOnExit = false;
    /** The path where to dump the XML encoded snapshot. */
    private String finalSnapshotPath = System.getProperty("user.home") + File.separator + "snapshotOnExit.p4j-snapshot";

    // //////////////////////////////////////////////////////////////////////////
    // Constructor
    // //////////////////////////////////////////////////////////////////////////

    /**
     * @return Returns the sessionVersion.
     */
    public int getSessionVersion() {
        return this.sessionVersion;
    }

    public Config(String agentArgs) {

        loadExclusions();

        defaultRuleOptions = Utils.parseOptions("-access:public -beanprops:off");
        try {
            File sysTmpDir = new File(System.getProperty("java.io.tmpdir"));
            File seed = File.createTempFile("profiler4j_", "", sysTmpDir);
            tempDir = new File(seed.getAbsolutePath() + ".tmp");
            tempDir.mkdir();
            seed.delete();
        } catch (IOException e) {
            throw new Profiler4JError("Could not create temporary dir", e);
        }
        dumpDir = new File(tempDir, "instrumented_classes");
        if (agentArgs == null) {
            return;
        }
        for (String arg : agentArgs.split(",")) {
            String key;
            String value = null;
            int p = arg.indexOf('=');
            if (p == -1) {
                key = arg;
            } else {
                key = arg.substring(0, p);
                if ((p + 1) < arg.length()) {
                    value = arg.substring(p + 1, arg.length());
                }
            }
            if ("waitconn".equals(key)) {
                waitConnection = Boolean.parseBoolean(value);
            } else if ("verbosity".equals(key)) {
                verbosity = Integer.parseInt(value);
            } else if ("port".equals(key)) {
                port = Integer.parseInt(value);
            } else if ("enabled".equals(key)) {
                enabled = Boolean.parseBoolean(value);
            } else if ("password".equals(key)) {
                password = value;
            } else if ("snapshotOnExit".equals(key)) {
                takeSnapshotOnExit = true;
                if (null != value)
                    finalSnapshotPath = value;
                verifyPath_orThrowException(finalSnapshotPath);
            } else {
                throw new Profiler4JError("Invalid agent option '" + key + "'");
            }
        }

    }

    /**
     * @return Returns the exclusions.
     */
    public String[] getExclusions() {
        return this.exclusions;
    }

    private void loadExclusions() {
        String s = detectInstallDirUrl() + "/p4j-exclusions.txt";
        List<String> exclusions = new ArrayList<String>();
        try {
            URL u = new URL(s);
            Log.print(0, "loading exclusions from " + u);
            BufferedReader r = new BufferedReader(new InputStreamReader(u.openStream()));
            String cn;
            while ((cn = r.readLine()) != null) {
                cn = cn.trim();
                if (cn.startsWith("#") || cn.length() == 0) {
                    continue;
                }
                exclusions.add(cn);
                Log.print(0, "exluding class " + cn);
            }
            this.exclusions = (String[]) exclusions.toArray(new String[0]);
        } catch (Exception e) {
            throw new Profiler4JError("Could not load exclusions");
        }
    }

    private String detectInstallDirUrl() {
        Class c = getClass();
        String cn = "/" + c.getName().replace('.', '/') + ".class";
        URL url = c.getResource(cn);
        String s = url.toString();
        int pos = s.lastIndexOf('!');
        s = s.substring(0, pos);
        pos = s.lastIndexOf('/');
        s = s.substring(4, pos);
        return s;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Public methods
    // //////////////////////////////////////////////////////////////////////////

    /**
     * @return Returns the password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Defines the new set of instrumentation rules.
     * <p>
     * Notice that these rules will only take effect after the classes are redefined. For
     * the sake of consistency, this method should only be called by
     * {@link Agent#startNewSession(String, String, Transformer.Callback)}
     * 
     * @param optionsAsStr
     * @param rulesAsStr
     */

    public void parseRules(String optionsAsStr, String rulesAsStr) {

        // update options
        lastDefaultRuleOptions = defaultRuleOptions;
        defaultRuleOptions = Utils.parseOptions(optionsAsStr);

        // update rules
        lastRules = rules;
        rules = Utils.parseRules(rulesAsStr);

        sessionVersion++;
    }

    /**
     * @return Returns the defaultRuleOptions.
     */
    public Map<Option, String> getDefaultRuleOptions() {
        return this.defaultRuleOptions;
    }
    
    /**
     * Verifies the given path can be used to open a file and write to it.
     * <p>
     * In case this is not possible, an exception is thrown.
     * 
     * @param pathToSnapshot the path to check
     */
    public void verifyPath_orThrowException(String pathToSnapshot) {
        // Could be shorter with apache.commmons but decided against
        // including more libraries in the agent, which should be as
        // tiny as reasonable.
        if (null == pathToSnapshot || 0 == pathToSnapshot.length())
            throw new Profiler4JError("File path for snapshotOnExit feature is empty.");
        
        File file = new File(pathToSnapshot);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch(IOException e) {
                throw new Profiler4JError("Could not create a file for the snapshotOnExit feature: " + pathToSnapshot);
            } finally {
            file.delete();
            }
        } else {
            // File?
            if (!file.isFile() || !file.canWrite())
                throw new Profiler4JError("Cannot open file for snapshotOnExit feature: " + pathToSnapshot);
        }
        
        
    }
    
    
    
    /*****************************************************************
     * PROPERTIES
     */

    /**
     * @return Returns the dumpClasses.
     */
    public boolean isDumpClasses() {
        return this.dumpClasses;
    }

    /**
     * @return Returns the dumpDir.
     */
    public File getDumpDir() {
        return this.dumpDir;
    }

    /**
     * @return Returns the enabled.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * @return Returns the exitVmOnFailure.
     */
    public boolean isExitVmOnFailure() {
        return this.exitVmOnFailure;
    }

    /**
     * @return Returns the port.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @return Returns the rules or <code>null</code> if no rule was defined.
     */
    public List<Rule> getRules() {
        return this.rules;
    }

    public List<Rule> getLastRules() {
        return this.lastRules;
    }

    /**
     * @return Returns the saveSnapshotOnExit.
     */
    public boolean isSaveSnapshotOnExit() {
        return this.saveSnapshotOnExit;
    }

    /**
     * @return Returns the traceAllocations.
     */
    public boolean isTraceAllocations() {
        return this.traceAllocations;
    }

    /**
     * @return Returns the verbosity.
     */
    public int getVerbosity() {
        return this.verbosity;
    }

    /**
     * @return Returns the waitConnection.
     */
    public boolean isWaitConnection() {
        return this.waitConnection;
    }

    /**
     * If the user has requested that a snapshot be dumped before exiting the
     * agent, i.e. before the program closes down completely. If so,
     * the path can be retrieved via {@link #getFinalSnapshotPath()} and is
     * guaranteed to have been meaningful, when the configuration was read.
     * 
     * @return {@code true} in case a snapshot is wanted, {@code false} otherwise.
     */
    public boolean isTakeSnapshotOnExit() {
        return this.takeSnapshotOnExit;
    }

    /**
     * The path to where to dump the final snapshot as XML.
     * <p>
     * If {@link #isTakeSnapshotOnExit()} returns {@code true}, the return
     * value is guaranteed to be non-{@code null}. Otherwise the return value
     * is undefined.
     * 
     * @return the file path for the snapshot 
     */
    public String getFinalSnapshotPath() {
        return this.finalSnapshotPath;
    }

    
}
