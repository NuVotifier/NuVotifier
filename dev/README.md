## Using this testbed

This testbed runs one Bungeecord as well as two Paper servers behind it, labeled
s1 and s2. Ports for where everything is bound is listed below.

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

| Server | MC Port | Votifier Port |
| ------ | ------- | ------------- |
| proxy  | 25577   | 8192          |
| s1     | 25565   | 8193          |
| s2     | 25566   | 8194          |
