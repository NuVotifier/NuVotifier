package io.ibj.nuvotifier.platform;

import java.security.Key;

public interface V2TokenProvider {
    Key getKeyForService(String serviceName);
}
