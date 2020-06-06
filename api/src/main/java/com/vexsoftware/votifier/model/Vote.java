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

import java.util.Arrays;
import java.util.Base64;

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

    private byte[] additionalData;

    @Deprecated
    public Vote() {
    }

    public Vote(String serviceName, String username, String address, String timeStamp) {
        this.serviceName = serviceName;
        this.username = username;
        this.address = address;
        this.timeStamp = timeStamp;
        this.additionalData = null;
    }

    public Vote(String serviceName, String username, String address, String timeStamp, byte[] additionalData) {
        this.serviceName = serviceName;
        this.username = username;
        this.address = address;
        this.timeStamp = timeStamp;
        this.additionalData = additionalData == null ? null : additionalData.clone();
    }

    public Vote(Vote vote) {
        this(vote.getServiceName(), vote.getUsername(), vote.getAddress(), vote.getTimeStamp(),
                vote.getAdditionalData() == null ? null : vote.getAdditionalData().clone());
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
                getTimestamp(jsonObject.get("timestamp")));
        if (jsonObject.has("additionalData"))
            additionalData = Base64.getDecoder().decode(jsonObject.get("additionalData").getAsString());
    }

    @Override
    public String toString() {
        String data;
        if (additionalData == null)
            data = "null";
        else
            data = Base64.getEncoder().encodeToString(additionalData);

        return "Vote (from:" + serviceName + " username:" + username
                + " address:" + address + " timeStamp:" + timeStamp
                + " additionalData:" + data + ")";
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
     * Returns additional data sent with the vote, if it exists.
     *
     * @return Additional data sent with the vote
     */
    public byte[] getAdditionalData() {
        return additionalData == null ? null : additionalData.clone();
    }

    public JsonObject serialize() {
        JsonObject ret = new JsonObject();
        ret.addProperty("serviceName", serviceName);
        ret.addProperty("username", username);
        ret.addProperty("address", address);
        ret.addProperty("timestamp", timeStamp);
        if (additionalData != null)
            ret.addProperty("additionalData", Base64.getEncoder().encodeToString(additionalData));
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vote)) return false;

        Vote vote = (Vote) o;

        if (!serviceName.equals(vote.serviceName)) return false;
        if (!username.equals(vote.username)) return false;
        if (!address.equals(vote.address)) return false;
        if (!timeStamp.equals(vote.timeStamp)) return false;
        return Arrays.equals(additionalData, vote.additionalData);
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
