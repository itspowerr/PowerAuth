package com.pawanpoudel.powerauth.listeners;

import com.pawanpoudel.powerauth.PowerAuth;
import com.pawanpoudel.powerauth.managers.SessionManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class AuthListener implements Listener {

    private final PowerAuth plugin;
    private final SessionManager sessionManager;

    public AuthListener(PowerAuth plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String currentIp = player.getAddress().getAddress().getHostAddress();

        // Skip this check for premium players - they are handled by LoginListener
        if (plugin.getDatabaseManager().isPremium(uuid)) {
            return;
        }

        if (plugin.getDatabaseManager().isRegistered(uuid)) {
            String lastIp = plugin.getDatabaseManager().getLastIp(uuid);
            if (currentIp.equals(lastIp)) {
                sessionManager.login(uuid);
                plugin.getLimboManager().sendToMainWorld(player);
                player.sendMessage(ChatColor.GREEN + "Auto-logged in via IP!");
                return;
            }
            plugin.getLimboManager().sendToLimbo(player);
            player.sendMessage(ChatColor.YELLOW + "Please log in using /login <password>");
        } else {
            plugin.getLimboManager().sendToLimbo(player);
            player.sendMessage(ChatColor.YELLOW + "Please register using /register <password> <confirmPassword>");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sessionManager.logout(uuid);
        plugin.getLimboManager().cleanup(uuid);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId())) {
            // Prevent movement but allow looking around
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You must be logged in to chat!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.isLoggedIn(player.getUniqueId()))
            return;

        String message = event.getMessage().toLowerCase();
        if (!message.startsWith("/login") && !message.startsWith("/register")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be logged in to use commands!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!sessionManager.isLoggedIn(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }
}
