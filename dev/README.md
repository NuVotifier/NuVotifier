## Using this testbed

This testbed runs two proxies, labeled p1 and p2, as well as two servers behind it,
labeled s1 and s2. Ports for where everything is bound is listed below.

First, download required sources using the following command:

```
make
```

Then, each time you want to launch a session, run:

```
make run
```

FYI, this only works in Linux afaik because thats what I do my development on.

# Ports

| Server | Server Software        | MC Port | Votifier Port |
| ------ | ---------------------- | ------- | ------------- |
| p1     | Waterfall/BungeeCord   | 25577   | 8192          |
| p2     | Velocity               | 25578   | 8193          |
| s1     | Paper (1.16.1)         | 25565   | 8194          |
| s2     | SpongeVanilla (1.12.2) | 25566   | 8195          |

For compatibility with 1.16.1, s2 includes ViaVersion.