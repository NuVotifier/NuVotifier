This document outlines how to use NuVotifier as a plugin on both Bukkit based servers, as well as BungeeCord/Bukkit based
networks.

# Installation

The NuVotifier Universal jar (the default jar that is distributed on spigotmc.org) can be installed on Bukkit, BungeeCord,
and Sponge servers as-is.

The Universal JAR can be downloaded off our [SpigotMC resource page](https://www.spigotmc.org/resources/nuvotifier.13449/).

# Votifier v1 vs NuVotifier v2 Protocols

NuVotifier introduces a more secure and failure tolerant protocol for sending votes. In this document, this new protocol
will be referred to as v2. NuVotifier also supports the legacy Votifier protocol (using public key encryption). In this document,
the legacy protocol will be referred to v1. As of writing, most if not all vote websites still use v1.

# Configuration

## Enabling the NuVotifier Server port

NuVotifier allows for the server port to be turned off when NuVotifier is only receiving forwarded votes. This is only
an option on Bukkit based servers, since it would not make logical sense for the port to be turned off on Bungee servers.

If you are receiving votes from a server list directly on the server in question (votes are not being forwarded through plugin
messaging), or if votes are being forwarded through the `proxy` method, then `enableExternal` should be set to true. Only
if you are receiving forwarded votes from your bungee through the `pluginMessaging` forwarding method should `enableExternal`
be set to false.

## Port Configuration

NuVotifier's main function is acting as a vote server (receiver). The IP for NuVotifier to bind to is configured
by the `host` and `ip` configuration values within the config.yml files within the NuVotifier plugin folder.
Most times, you will want this `host` value to be set to `0.0.0.0` or your server's ip. `0.0.0.0` will bind to all
network addresses on your machine, while specifying a certain IP will only bind to that ip. If you don't know what to set this
to, `0.0.0.0` is a safe bet. By default, NuVotifier listens on port 8192. This is the same as the default Votifier port.
If you change this, you will have to remember this when setting up forwarding or server lists.

## Debug mode

When first setting up NuVotifier, you will probably want debug mode to be enabled. This prints extra information to the
console, ensuring that votes are being processed as they should. If you are experiencing an error, or having issues with
votes being processed, you should first enable this. Debug mode can be turned on by setting `debug` in the config.yml
to true.

## Tokens vs Public Key

As explained earlier, NuVotifier supports 2 different protocols for receiving votes, v1 and v2. v1 uses the private and public
key pair located in `rsa/` in NuVotifier's plugin folder. When setting up voting websites, the public key, `rsa/public.key`
should be used. When setting up the `proxy` forwarding method, tokens should be used.

Making a new token is as easy as adding another entry into `tokens` with the serviceName as the key and whatever string you want
as the value. You can usually find the serviceName of a vote provider on their setup page if they use v2.

Unless a website specifies it, you should assume that the website is using v1, and you should use `rsa/public.key`.

## Forwarding

NuVotifier allows for various methods for forwarding the vote from an upstream Bungeecord instance. Currently, 2 methods
are supported: `proxy` and `pluginMessaging`. These are configured through the `forwarding` configuration section in your
config.yml files.

### Plugin Messaging Based Forwarding

Plugin Messaging based forwarding works by sending a forwarded vote over Minecraft's plugin messaging system. NuVotifier
is only capable of forwarding the vote if a player is on each backend server through the BungeeCord running NuVotifier.
Plugin Messaging can be enabled by setting `method` in `forwarding` to `pluginMessaging`. You may also set the plugin messaging
channel NuVotifier is to use by setting `channel` in the `pluginMessaging` section. This may be whatever you want, but it
must be the same on your BungeeCord as well as your backend Bukkit servers. If they are not the same, the backend will not
be able to receive the vote.

#### Filtering servers

If you only wish to send votes to some servers, but not all, there is a section within `pluginMessaging` called `excludedServers`.
By default, this section is commented out, and will never filter out any servers. If you wish to filter out servers, uncomment
this list and fill it with the names of servers you do not want to send votes to.

#### Plugin Messaging Vote Caching

In the event that the backend server does not have anyone on it from the BungeeCord instance, the vote will need to be cached
until a player joins and the vote may be sent. There are 3 options for caching, however one is only suitable for actual
production.

##### Memory Vote Caching

For votes to simply be cached in memory, you can set the `cache` setting of the `pluginMessaging` section of your BungeeCord's
NuVotifier's config.yml to `memory`. This stores votes in memory. The votes are lost when the BungeeCord server shuts down.
This is suitable for testing, but not for production system.

##### File Vote Caching

Votes can also be cached in a file located in the NuVotifier plugin folder. To enable this, you can set `cache` to file.
If you want to change the file location from the default, you can set this under the `name` config under the `file` section.
This caching method is suitable for most people. This is the default option.

##### No Caching

Caching can also be turned off. You can do this by setting `cache` to `none`. This is also not recommended, and is only
suitable for a certain number of setups.

### Proxy Based Forwarding

You can also forward votes using the proxy method. This will cause the BungeeCord to act like a voting website, sending votes
to each server's NuVotifier port. To enable this method, you must set the `method` under `forwarding` to `proxy` in your
BungeeCord config.yml. Within the server's config.yml, `forwarding` should be set to `none`.

For each server you wish to forward to, you must add each to your BungeeCord config.yml under the `proxy` section of `forwarding`.
For each, you must specify the ip address (`address`), port (`port`) and token (`token`). Each backend Bukkit server should have
1 default token under the `tokens` section. This is the token that should be used as the token for the `token` in that server's
section within the `proxy` section. If the server is on the same server (same ip) as the BungeeCord server, you may
use the `127.0.0.1` or `localhost` address, setting both the `proxy` forwarding address and Bukkit config.yml address to it.

In order for vote proxying to work, the backend server __must__ have `enableExternal` set to true. If it is set to
false, the Bungee server will not be able to forward the vote. 


## Default Configurations

The default configuration for Bukkit can be found [here](https://github.com/NuVotifier/NuVotifier/blob/master/bukkit/src/main/resources/bukkitConfig.yml),
for BungeeCord [here](https://github.com/NuVotifier/NuVotifier/blob/master/bungeecord/src/main/resources/bungeeConfig.yml),
and for Sponge [here](https://github.com/NuVotifier/NuVotifier/blob/master/sponge/src/main/resources/com/vexsoftware/votifier/sponge/spongeConfig.yml).


## Testing

There is a free resource for testing your NuVotifier setups at [votifier.inaptbox.com](https://votifier.inaptbox.com). If you are setting up your
network or server and want a server to get test votes on, you are more than welcome to use it.


## Basic Setups

### Single Server Quickstart

A single server setup should have `enableExternal` set to `true`, and `forwarding.method` set to `none`. The IP and port
should also be set to appropriate values. For server lists, you should use the public key found in `rsa/public.key`.

## Advanced Setups

### Multi BungeeCord Setups

Using NuVotifier with multiple BungeeCords is a more tricky setup. You can use one of your player-facing proxies for the
NuVotifier server, or use a separate one. You are not guaranteed to have players on all of your servers to allow votes to
be dispatched, so proxy forwarding (requiring `enableExternal` set to `true` on your Bukkit servers) is highly recommended.

## Contributing

If you wish to contribute code wise, feel free to submit a pull request!

## Client Libraries

NuVotifier has libraries available for [NodeJS](https://github.com/NuVotifier/votifier2-js) and [PHP](https://github.com/NuVotifier/votifier2-php).
Both are free to use by serverlists.