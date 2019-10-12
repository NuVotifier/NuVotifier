package com.vexsoftware.votifier.util;

import com.vexsoftware.votifier.model.Vote;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgsToVote {

    private static final Pattern argumentPattern = Pattern.compile("([a-zA-Z]+)=(\\S*)");

    public static Vote parse(String[] arguments)  {
        return parse(arguments, null);
    }

    public static Vote parse(String[] arguments, Map<String, String> additionalArgs) {
        String serviceName = "TestVote";
        String username = null;
        String address = "127.0.0.1";
        long localTimestamp = System.currentTimeMillis();
        String timestamp = Long.toString(localTimestamp, 10);

        for (String s : arguments) {
            Matcher m = argumentPattern.matcher(s);
            if (m.matches()) {
                String key = m.group(1).toLowerCase();
                String v = m.group(2);
                if (key.equals("servicename")) {
                    serviceName = v;
                } else if (key.equals("username")) {
                    if (v.length() > 16)
                        throw new IllegalArgumentException("Illegal username - must be less than 16 characters long.");
                    username = v;
                } else if (key.equals("address")) {
                    address = v;
                } else if (key.equals("timestamp")) {
                    timestamp = v;
                } else {
                    if (additionalArgs != null)
                        additionalArgs.put(key, v);
                }

            } else {
                if (s.length() > 16)
                    throw new IllegalArgumentException("Illegal username - must be less than 16 characters long.");
                username = s; // ezpz
            }
        }

        if (username == null)
            throw new IllegalArgumentException("Username not specified!");

        return new Vote(serviceName, username, address, timestamp);
    }
}
