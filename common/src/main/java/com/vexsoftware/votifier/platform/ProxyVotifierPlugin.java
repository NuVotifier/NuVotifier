package com.vexsoftware.votifier.platform;

import java.util.List;
import java.util.Optional;

public interface ProxyVotifierPlugin extends VotifierPlugin {
    List<BackendServer> getAllBackendServers();

    Optional<BackendServer> getServer(String name);
}
