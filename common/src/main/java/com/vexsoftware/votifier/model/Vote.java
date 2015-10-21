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

import org.json.JSONObject;

/**
 * A model for a vote.
 *
 * @author Blake Beaupain
 */
public class Vote {

    /** The name of the vote service. */
    private String serviceName;

    /** The username of the voter. */
    private String username;

    /** The address of the voter. */
    private String address;

    /** The date and time of the vote. */
    private String timeStamp;

    @Deprecated
    public Vote() {
    }

    public Vote(String serviceName, String username, String address, String timeStamp) {
        this.serviceName = serviceName;
        this.username = username;
        this.address = address;
        this.timeStamp = timeStamp;
    }

    public Vote(JSONObject jsonObject){
        this(jsonObject.getString("serviceName"),jsonObject.getString("username"),jsonObject.getString("address"),jsonObject.getString("timestamp"));
    }

    @Override
    public String toString() {
        return "Vote (from:" + serviceName + " username:" + username
                + " address:" + address + " timeStamp:" + timeStamp + ")";
    }

    /**
     * Sets the serviceName.
     *
     * @param serviceName
     *            The new serviceName
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
     * @param username
     *            The new username
     */
    @Deprecated
    public void setUsername(String username) {
        this.username = username.length() <= 16 ? username : username.substring(0, 16);
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
     * @param address
     *            The new address
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
     * @param timeStamp
     *            The new time stamp
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


    public JSONObject serialize(){
        JSONObject ret = new JSONObject();
        ret.put("serviceName",serviceName);
        ret.put("username", username);
        ret.put("address", address);
        ret.put("timestamp", timeStamp);
        return ret;
    }
}
