/*
 * Copyright (C) 2011 Vex Software LLC
 * This file is part of Votifier.
 *
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A model for a vote.
 *
 * @author Blake Beaupain
 */
public class Vote {

    /**
     * The name of the vote service.
     */
    private String serviceName;

    /**
     * The username of the voter.
     */
    private String username;

    /**
     * The address of the voter.
     */
    private String address;

    /**
     * The date and time of the vote.
     */
    private String timeStamp;

    /**
     * Timestamp (unix-millis) normalized and taken from a known source
     */
    private final long localTimestamp;

    @Deprecated
    public Vote() {
        localTimestamp = System.currentTimeMillis();
    }

    public Vote(String serviceName, String username, String address, String timeStamp) {
        this(serviceName, username, address, timeStamp, System.currentTimeMillis());
    }

    public Vote(String serviceName, String username, String address, String timeStamp, long localTimestamp) {
        this.serviceName = serviceName;
        this.username = username;
        this.address = address;
        this.timeStamp = timeStamp;
        this.localTimestamp = localTimestamp;
    }

    private static String getTimestamp(JsonElement object) {
        try {
            return Long.toString(object.getAsLong());
        } catch (Exception e) {
            return object.getAsString();
        }
    }

    public Vote(JsonObject jsonObject) {
        this(jsonObject.get("serviceName").getAsString(),
                jsonObject.get("username").getAsString(),
                jsonObject.get("address").getAsString(),
                getTimestamp(jsonObject.get("timestamp")),
                // maintained for backwards compatibility with <2.3.6 peers
                (jsonObject.has("localTimestamp") ? jsonObject.get("localTimestamp").getAsLong() : System.currentTimeMillis()));
    }

    @Override
    public String toString() {
        return "Vote (from:" + serviceName + " username:" + username
                + " address:" + address + " timeStamp:" + timeStamp
                + " localTimestamp:" + localTimestamp + ")";
    }

    /**
     * Sets the serviceName.
     *
     * @param serviceName The new serviceName
     */
    @Deprecated
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Gets the serviceName.
     *
     * @return The serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the username.
     *
     * @param username The new username
     */
    @Deprecated
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the address.
     *
     * @param address The new address
     */
    @Deprecated
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the address.
     *
     * @return The address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the time stamp.
     *
     * @param timeStamp The new time stamp
     */
    @Deprecated
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Gets the time stamp.
     *
     * @return The time stamp
     */
    public String getTimeStamp() {
        return timeStamp;
    }

    /**
     * Gets the local timestamp, in unix-millis. Calculated locally by a NuVotifier instance
     *
     * @return The local timestamp
     */
    public long getLocalTimestamp() {
        return localTimestamp;
    }

    public JsonObject serialize() {
        JsonObject ret = new JsonObject();
        ret.addProperty("serviceName", serviceName);
        ret.addProperty("username", username);
        ret.addProperty("address", address);
        ret.addProperty("timestamp", timeStamp);
        ret.addProperty("localTimestamp", localTimestamp);
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vote vote = (Vote) o;

        if (!serviceName.equals(vote.serviceName)) return false;
        if (!username.equals(vote.username)) return false;
        if (!address.equals(vote.address)) return false;
        return timeStamp.equals(vote.timeStamp);
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + timeStamp.hashCode();
        return result;
    }
}
