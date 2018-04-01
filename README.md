# NuVotifier

NuVotifier is a Bukkit plugin whose purpose is to be notified (aka *votified*)
when a vote is made on a Minecraft server top list for the server.  NuVotifier
creates a *lightweight* server that waits for connections by Minecraft server
lists and uses a simple protocol to get the required information.  NuVotifier is
*secure*, and makes sure that all vote notifications are delivered by authentic
top lists.

## Why fork Votifier?

Votifier has lagged in activity, has severe security flaws in its protocol, and
is vulnerable to denial of service attacks.

## Is NuVotifier a drop-in replacement for Votifier?

Yes! Except for some minor non-breaking changes (look into the [technical qa
docs](docs/technical_qa.md) for more info), NuVotifier is 100% compatible with
the old Votifier with many more features.

# Documentation / Configuration Guide

All documentation is located under the [docs](docs/) directory. A server admin
configuration guide is available [here](docs/usage.md).

# Support

Feel free to join the MOSS Discord server using the below link:

[![Discord](https://i.imgur.com/HLPoNnY.png)](https://discord.gg/anUtuAC)

# License

NuVotifier is GNU GPLv3 licensed. This project's license can be viewed
[here](LICENSE).
