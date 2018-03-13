This document outlines how to configure NuVotifier for a variety of different
configurations. It should be used by server administrators looking to set up
NuVotifier on their network.

# Key vs Token

This document uses the terms `token` and `key`. These are two separate terms,
and it is important to understand the differences.

## Key

Keys in terms of NuVotifier refer to the old Votifier style RSA keys. They
are typically distributed as a pair of files - a public and private key. When
dealing with RSA keys, you can share your public key with server lists, but make
sure to keep your private key safe with you. You can find these keys
auto-generated under `plugins/(Nu)Votifier/rsa/`, where your public key is
named `public.key`, while your private key is labeled `private.key`. Make sure
not to separate the two, as they won't work together.

## Token

Tokens in terms of NuVotifier refer to a new short string of characters. This
string of characters is secret, and should never be exposed to anyone but you or
server lists which support NuVotifier voting (check with the server list
first). Tokens are most commonly used internally within a network for use when
using the proxy vote forwarding method.

# Single Server Configuration (Bukkit / Spigot / Sponge)

You will want to set up your server using these instructions when you only have
one Minecraft server (Bukkit / Sponge). These are the most trivial installation
steps.

## Installing the plugin on a server

To install NuVotifier, you should get a copy from one of the following
locations:

+ [Github](https://github.com/parallelblock/NuVotifier/releases) - Official
  release location
+ [SpigotMC](https://www.spigotmc.org/resources/nuvotifier.13449/) - SpigotMC
  release mirror
+ [repo.parallelblock.com](https://repo.parallelblock.com/repository/public-raw/nuvotifier/nuvotifier.jar) - ParallelBlock CI 'hot off the presses' releases

The NuVotifier jar should then be placed within the `plugins/` folder of your
Minecraft server. When you then start your server, NuVotifier will generate
the necessary configuration and initial keys for you.

## Configuring the plugin

When running in a single server configuration, NuVotifier's default
configuration works well. The only value which you may need to change is the
`port` configuration value - this specifies the port on which NuVotifier
listens to new votes. This port cannot be any port you are already using. For
example, if Minecraft is running on the default port (25565), you cannot set
`port` also to 25565. Make sure port is a number from 1-65535.

You can then add your server to server lists using the port you configured while
also supplying your public key. At this point, you should be able to send a test
vote to your server!

# Multi-server / Network configuration

You will want to use these instructions when you have more than one server and
are using a Minecraft proxy (Bungeecord / Lilypad)

## Single Bungeecord Networks

When running a single-bungeecord network, NuVotifier is able to operate as both
a Bungeecord vote receiver (emitting events for Bungeecord based vote listeners)
as well as a vote forwarder. NuVotifier's vote forwarding logic is also advanced
enough to be used in a wide variety of ways.

### Installing the plugin on the servers

NuVotifier should be installed on each of the servers you wish to process votes
on, as well as the Bungeecord server. The Bungeecord server will act as the
public facing Votifier instance, where as the Minecraft servers will instead
receive instructions from the Bungeecord Nuvotifier for forwarding votes.

Like, the single server installation, you can download NuVotifier at any one of
the following locations:

+ [Github](https://github.com/parallelblock/NuVotifier/releases) - Official
  release location
+ [SpigotMC](https://www.spigotmc.org/resources/nuvotifier.13449/) - SpigotMC
  release mirror
+ [repo.parallelblock.com](https://repo.parallelblock.com/repository/public-raw/nuvotifier/nuvotifier.jar) - ParallelBlock CI 'hot off the presses' releases

The plugin should then be placed in the `plugins/` directory of the Bungeecord
server as well as each of the Minecraft servers.

### Vote Forwarding

The following sections describe how vote forwarding works. Vote forwarding is an
advanced configurable operation, and requires a lot of configuration to get
correct.

### Plugin messaging based forwarding

This section describes how to set up plugin messaging forwarding. This will only
work correctly if you have one Bungeecord server, however may also work with
some exceptions in multi Bungeecord networks.

First off, we should configure each of the Minecraft servers. Open each of the
servers' NuVotifier configurations. Each one should have `method` under the
`forwarding` configuration section set to `pluginMessaging`. Under the
`pluginMessaging` configuration section, `channel` is set to `NuVotifier`. This
is a safe default (and is protected from injection by a hacked Minecraft
client), but feel free to change it. If you choose to change it, make sure you
change it everywhere all around your network!

Since we are receiving forwarded votes through plugin messaging and not through
network ports, we should set `port` on each of the Minecraft servers to -1. This
will disable NuVotifier's port, while still allowing it to receive forwarded
plugin messaging votes. This will only work when using pluginMessaging!

Since we have all of that configured, we can now move to the Bungeecord
NuVotifier configuration. It is once again located under `plugins/NuVotifier/`
directory (`config.yml`). Like the single server, `port` should be set to an
unbound port. The default `8192` works well, however you may have to change it
depending on your hosting provider.

Since we are using pluginMessaging based forwarding, lets set `method` under
`forwarding` to `pluginMessaging`. Make sure that `channel` is the same channel
that you configured your servers to listen to - if you didn't change this value,
then the default will work.

NuVotifier saves when it can't immediately forward in a user defined cache. In
almost all installations this should be `file`, however, some installations may
instead want `none` or `memory`. If you don't know, the `cache` should be set to
it's default, `file`.

By default, NuVotifier will forward the vote to all of the connected Minecraft
servers (all servers you can get to when typing /server). You can manually
exclude some of these servers by first uncommenting `excludedServers`, then
adding the server's name to the `excludedServers` list. After adding, NuVotifier
will not send any votes to this server. If you only send the vote to the server
the player joined, these servers are ignored.

NuVotifier can also selectively send the vote to only the server which the
player has joined. This option can be turned on by setting
`onlySendToJoinedServer` to true. When this option is on, the vote will attempt
to go to the server which the player is currently on. If the player is currently
not on a server (offline), then NuVotifier will then attempt to send the vote to
the `joinedServerFallback`. If the vote cannot be sent to the
`joinedServerFallback`, then the vote is saved in the cache until the vote can
be sent to the `joinedServerFallback` server - note this will not send it to the
next server the player joins! If you do not want a fallback server and would
rather the vote be applied when the player next joins, set
`joinedServerFallback` to `''`. This will cause NuVotifier to save the vote
until the player joins again.

By default, votes are saved for 10 days in the cache. This time can be increased
or decreased by changing the `voteTTL` field. This number can be as high or low
as you want. It is measured in days within the cache.

### Proxy based vote forwarding

In more complex network situations, it may be required to instead use the proxy
based vote forwarding. This configuration is less flexible and more difficult to
set up properly - most people should use plugin messaging based forwarding.
Proxy based forwarding does not support vote caching or advanced forwarding
configurations. If you cannot use plugin messaging forwarding, then the
following section will describe how to set up proxy based vote forwarding. If
you still require advanced forwarding features or more advanced behaviors, it
may be worthwhile to write your own Bungeecord plugin to perform the proper
forwarding logic (you will know if you need to do this).

In proxy based vote forwarding, one of your Bungeecord servers acts as the head
vote receiver. This server will be the one you configure the voting websites to
vote to. You can set up multiple vote receivers and load balance between them,
however this configuration has limited benefit and is probably more work than it
is worth. Instead of communicating through plugin messaging like the plugin
messaging forwarding strategy, proxy forwarding instead acts like a voting
website to each of the backend servers and duplicates the vote it receives to
each of the backend servers it is configured with.

First, we will set up all of the Minecraft servers before setting up the head
vote receiver. For each of the servers, NuVotifier should be configured to bind
to an unused port. This port must be valid. If you have multiple votifiers on
the same IP/server, make sure that the port which you are using between
NuVotifier instances are different. Remember where to find the port number as we
will need it later to insert into the vote receiver Bungeecord NuVotifier
configuration. In addition to the port number, you will also need the default
token - this is the string auto generated by NuVotifier under the `default` key
under `tokens`. Since we are using proxy forwarding, change
`disable-v1-protocol` to true - we will only be using the new NuVotifier
votifier protocol within our network (vote receiver -> minecraft server). Once
all configured, that is all we need to do. The rest of the defaults will work.

Now we will configure the vote receiving NuVotifier Bungeecord server. The port
should be configured as described many times above, being a unique port. Instead
of setting `method` to `pluginMessaging` like above, we will set `method` to
`proxy`. This will tell NuVotifier that we instead want to proxy the votes
instead of forward them through plugin messaging channels.

The last step is to set up the proxy forwaring settings. Under the `proxy`
section is a place to enter each server. Create a unique name for each of the
server's you will be forwarding votes to. The default Bungeecord NuVotifier
configuration already has two Minecraft servers filled in - Hub and Hub2. You
should replace these with your own servers - the names of the servers do not
matter. Under each of these servers, we need to configure `address`, `port`, and
`token`. `address` is the IP address of the Bukkit/Sponge server. This may be
`localhost` or `127.0.0.1` if the Minecraft server is on the same physical
server as the vote receiving NuVotifier server. `port` is the port of the
Bukkit/Sponge NuVotifier server. `token` is the default token of the
Bukkit/Sponge NuVotifier server. This can be found within its configuration,
usually under the `token` section. Once the servers are configured, NuVotifier
will forward all votes it receives to the servers configured under the `proxy`
section.

# Debugging NuVotifier

NuVotifier can be difficult to set up in some configurations. If you find you
don't know what is wrong, you may want to try setting `debug` in each of the
server's NuVotifier configurations to `true`. This will increase logging within
your console. 

# Default Configurations

If you find you have messed up NuVotifier beyond repair, you can grab a default
configuration from the following locations (depending on the server it is
installed on):

+ [Bukkit](https://github.com/parallelblock/NuVotifier/blob/master/bukkit/src/main/resources/bukkitConfig.yml)
+ [Sponge](https://github.com/parallelblock/NuVotifier/blob/master/sponge/src/main/resources/com/vexsoftware/votifier/sponge/spongeConfig.yml)
+ [Bungeecord](https://github.com/parallelblock/NuVotifier/blob/master/bungeecord/src/main/resources/bungeeConfig.yml)

You can also regenerate this configuration by deleting the old configuration and
restarting the server.
