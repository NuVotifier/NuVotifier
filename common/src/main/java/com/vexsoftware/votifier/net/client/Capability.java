package com.vexsoftware.votifier.net.client;

import io.netty.channel.ChannelHandlerContext;

public interface Capability {
    void onAttach(CapabilityRegistrar.Context c);
}
