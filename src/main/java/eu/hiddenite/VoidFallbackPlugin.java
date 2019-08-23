package eu.hiddenite;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VoidFallbackPlugin extends Plugin implements Listener {
    private String defaultServerName;
    private String voidServerName;

    private ServerInfo defaultServerInfo;
    private ServerInfo voidServerInfo;

    @Override
    public void onEnable() {
        loadConfiguration();

        getLogger().info("Default server: " + defaultServerName + ", void server: " + voidServerName);

        defaultServerInfo = getProxy().getServerInfo(defaultServerName);
        if (defaultServerInfo == null) {
            getLogger().warning("The server " + defaultServerName + " does not exist.");
            return;
        }

        voidServerInfo = getProxy().getServerInfo(voidServerName);
        if (voidServerInfo == null) {
            getLogger().warning("The server " + voidServerName + " does not exist.");
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);

        getProxy().getScheduler().schedule(this, () -> {
            getProxy().getPlayers().forEach((player) -> {
                if (player == null || player.getServer() == null) return;
                if (player.getServer().getInfo().equals(voidServerInfo)) {
                    player.connect(defaultServerInfo);
                }
            });
        }, 5, 5, TimeUnit.SECONDS);
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent event) {
        if (event.getReason() == ServerConnectEvent.Reason.JOIN_PROXY) {
            if (Objects.equals(event.getTarget(), voidServerInfo)) {
                event.setTarget(defaultServerInfo);
            }
        }
    }

    @EventHandler
    public void onServerKickEvent(ServerKickEvent event) {
        if (event.getState() == ServerKickEvent.State.CONNECTED) {
            if (event.getPlayer().isConnected() && !Objects.equals(event.getKickedFrom(), voidServerInfo)) {
                event.setCancelled(true);
                event.setCancelServer(getProxy().getServerInfo(voidServerName));
            }
        }
    }

    private void loadConfiguration() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

            defaultServerName = configuration.getString("default_server_name");
            voidServerName = configuration.getString("void_server_name");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
