package com.extracraft.realmaddon.bungee;

import com.extracraft.realmaddon.common.PluginChannels;
import com.extracraft.realmaddon.common.PluginMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class RealmAddonBungeePlugin extends Plugin implements Listener {
    @Override
    public void onEnable() {
        ProxyServer.getInstance().registerChannel(PluginChannels.REALMADDON);
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().unregisterChannel(PluginChannels.REALMADDON);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(PluginChannels.REALMADDON)) {
            return;
        }
        PluginMessage.Message message = PluginMessage.decode(event.getData());
        if (message == null) {
            return;
        }
        String targetServer = message.targetServer();
        if (targetServer != null && !targetServer.isBlank()) {
            sendToServer(targetServer, event.getData());
            return;
        }
        ProxyServer.getInstance().getServers().keySet().forEach(server -> sendToServer(server, event.getData()));
    }

    private void sendToServer(String serverName, byte[] data) {
        TaskScheduler scheduler = ProxyServer.getInstance().getScheduler();
        scheduler.schedule(this, () -> {
            if (ProxyServer.getInstance().getServers().get(serverName) != null) {
                ProxyServer.getInstance().getServers().get(serverName).sendData(PluginChannels.REALMADDON, data);
            }
        }, 0, TimeUnit.MILLISECONDS);
    }
}
