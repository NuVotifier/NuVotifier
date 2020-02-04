package io.ibj.nuvotifier.platform;

import java.security.PrivateKey;

public interface V1KeyProvider {
    PrivateKey getPrivateKey();
}
