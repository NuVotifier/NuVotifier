# Votifier 1.0

Votifier is a Bukkit plugin whose purpose is to be notified when a vote is made on a Minecraft server top list for the server.  Votifier creates a lightweight server that waits for connections by Minecraft server lists and uses a simple protocol to get the required information.

# Configuring Votifier

Votifier requires configuration before it can run.  Votifier looks for two keys in your server.properties file, 'votifier_host' and 'votifier_port'.  These values are used to determine the host IP address that Votifier listens on, and the port that Votifier will listen on.  The default Votifier address is *0.0.0.0* (which allows Java to pick the first available address) and the default Votifier port is *8192*.

# Writing vote listeners

Votifier has a simple and easy to use (if you know Java) vote listener system.  In the future, Votifier will come with pre-made listeners - we just need to know what you want them to do.  In order to write a custom vote listener, make a Java class that implements the 'com.vexsoftware.votifier.model.VoteListener' and add it to the list returned by 'com.vexsoftware.votifier.Votifier.getListeners()' when Votifier is initialized.

*If you want to request a vote listener, send an email to votifier@vexsoftware.net and we may code it and package it along with Votifier.*

# Protocol Documentation

The Votifier protocol is string based and simple.

A connection is made to the Votifier server, and immediately Votifier will send its version in the following packet:

    "VERSION <version>"

Votifier then expects the following four strings (separated by newline characters) in response:

	"<serviceName>"
	"<username>"
	"<address>"
	"<timeStamp>"

Where 'serviceName' is the name of the top list service, 'username' is the username (entered by the voter) of the person who voted, 'address' is the IP address of the voter, and 'timeStamp' is the time stamp of the vote.