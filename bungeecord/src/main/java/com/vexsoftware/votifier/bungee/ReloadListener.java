package com.vexsoftware.votifier.bungee;

import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class ReloadListener implements Listener {
    private final NuVotifier nuVotifier;

    public ReloadListener(NuVotifier nuVotifier) {
        this.nuVotifier = nuVotifier;
    }

    @EventHandler
    public void onProxyReload(ProxyReloadEvent event) {
        this.nuVotifier.reload();
    }
}
