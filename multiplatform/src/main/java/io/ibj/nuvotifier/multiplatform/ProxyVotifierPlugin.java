package io.ibj.nuvotifier.multiplatform;

import java.util.Collection;
import java.util.Optional;

public interface ProxyVotifierPlugin {
    Collection<BackendServer> getAllBackendServers();

    Optional<BackendServer> getServer(String name);
}
