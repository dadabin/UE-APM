package com.ds.jvm.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ds.jvm.agent.Rule.Action;
import com.ds.jvm.agent.Rule.Option;



/**
 * General utility methods
 * 
 * @author Antonio S. R. Gomes
 */
public class Utils {

    private static Map<String, Pattern> patternCache = new HashMap<String, Pattern>();

    private static final Pattern ruleRegex = Pattern
        .compile("^\\s*([a-zA-Z0-9_\\(\\)\\*\\.\\$]+)\\s*"
                + ":\\s*(accept|reject)\\s*(.*)$");

    private static final OptionParser[] optionHandlers = new OptionParser[]{
            new OptionParser(Option.ACCESS, "access", "private|package|protected|public"),
            new OptionParser(Option.BEANPROPS, "beanprops", "on|off")};

    public static boolean parseBoolean(String v) {
        if ("yes".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v)
                || "true".equalsIgnoreCase(v)) {
            return true;
        }
        if ("no".equalsIgnoreCase(v) || "off".equalsIgnoreCase(v)
                || "false".equalsIgnoreCase(v)) {
            return false;
        }
        throw new Profiler4JError("Invalid boolean value: '" + v + "'");
    }

    public static Pattern getRegex(String s) {
        synchronized (patternCache) {
            Pattern p = patternCache.get(s);
            if (p != null) {
                return p;
            }
            StringBuilder sb = new StringBuilder("^");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '*') {
                    sb.append("[a-zA-Z0-9_\\$\\.\\[\\]\\,]*");
                } else if (c == '.') {
                    sb.append("\\.");
                } else if (c == '$') {
                    sb.append("\\$");
                } else if (c == '(') {
                    sb.append("\\(");
                } else if (c == ')') {
                    sb.append("\\)");
                } else if (c == ']') {
                    sb.append("\\]");
                } else if (c == '[') {
                    sb.append("\\[");
                } else {
                    sb.append(c);
                }
            }
            sb.append("$");
            p = Pattern.compile(sb.toString());
            patternCache.put(s, p);
            return p;
        }
    }

    public static List<Rule> parseRules(String rules) {
        List<Rule> list = new ArrayList<Rule>();
        if (rules != null && rules.trim().length()>0) {
            for (String s : rules.split("\\s*;\\s*")) {
                list.add(parseRule(s));
            }
        }
        return list;
    }

    public static Rule parseRule(String s) throws Profiler4JError {
        Matcher m = ruleRegex.matcher(s);
        if (m.matches()) {
            String methodPattern = m.group(1);
            Rule.Action action = ("accept".equals(m.group(2))) ? Action.ACCEPT
                    : Action.REJECT;
            Map<Option, String> options = parseOptions(m.group(3));
            return new Rule(methodPattern, action, options);
        }
        throw new Profiler4JError("Invalid config line: '" + s + "'");
    }

    /**
     * Parses a list of options represented as strings
     */
    public static Map<Option, String> parseOptions(String optionLine) {
        Map<Option, String> options = new HashMap<Option, String>();
        for (String s : optionLine.split("\\s+")) {
            if (s.length() > 0) {
                boolean valid = false;
                for (OptionParser h : optionHandlers) {
                    if (h.handle(options, s)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new Profiler4JError("Unknown rule option '" + s + "'");
                }
            }
        }
        return options;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Inner classes
    // //////////////////////////////////////////////////////////////////////////

    public static class OptionParser {

        private Option option;
        private Pattern regex;

        public OptionParser(Option option, String name, String valuesRegex) {
            this.option = option;
            this.regex = Pattern.compile("^\\-(" + name + "):(" + valuesRegex + ")$");
        }

        public boolean handle(Map<Option, String> options, String soption) {
            Matcher m = regex.matcher(soption);
            if (m.matches()) {
                options.put(option, m.group(2));
                return true;
            }
            return false;
        }
    }

}
