package com.pawanpoudel.powerauth;

import com.pawanpoudel.powerauth.database.DatabaseManager;
import com.pawanpoudel.powerauth.managers.SessionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerAuth extends JavaPlugin {

    private static PowerAuth instance;

    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private com.pawanpoudel.powerauth.managers.LimboManager limboManager;

    @Override
    public void onEnable() {
        instance = this;

        // Display startup banner
        displayBanner();

        // Save default config
        saveDefaultConfig();

        // Initialize Database
        this.databaseManager = new DatabaseManager(this);
        this.sessionManager = new SessionManager();
        this.limboManager = new com.pawanpoudel.powerauth.managers.LimboManager(this);

        // Register Commands
        getCommand("register").setExecutor(new com.pawanpoudel.powerauth.commands.AuthCommand(this, sessionManager));
        getCommand("login").setExecutor(new com.pawanpoudel.powerauth.commands.AuthCommand(this, sessionManager));

        // Register Listeners
        getServer().getPluginManager()
                .registerEvents(new com.pawanpoudel.powerauth.listeners.AuthListener(this, sessionManager), this);

        // Register Premium Authentication Listener
        new com.pawanpoudel.powerauth.listeners.LoginListener(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("PowerAuth has been disabled!");

        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void displayBanner() {
        String[] banner = {
                "",
                "§6╔════════════════════════════════════════════════════════╗",
                "§6║                                                        ║",
                "§6║   §e██████╗  ██████╗ ██╗    ██╗███████╗██████╗        §6║",
                "§6║   §e██╔══██╗██╔═══██╗██║    ██║██╔════╝██╔══██╗       §6║",
                "§6║   §e██████╔╝██║   ██║██║ █╗ ██║█████╗  ██████╔╝       §6║",
                "§6║   §e██╔═══╝ ██║   ██║██║███╗██║██╔══╝  ██╔══██╗       §6║",
                "§6║   §e██║     ╚██████╔╝╚███╔███╔╝███████╗██║  ██║       §6║",
                "§6║   §e╚═╝      ╚═════╝  ╚══╝╚══╝ ╚══════╝╚═╝  ╚═╝       §6║",
                "§6║                                                        ║",
                "§6║   §e █████╗ ██╗   ██╗████████╗██╗  ██╗               §6║",
                "§6║   §e██╔══██╗██║   ██║╚══██╔══╝██║  ██║               §6║",
                "§6║   §e███████║██║   ██║   ██║   ███████║               §6║",
                "§6║   §e██╔══██║██║   ██║   ██║   ██╔══██║               §6║",
                "§6║   §e██║  ██║╚██████╔╝   ██║   ██║  ██║               §6║",
                "§6║   §e╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝               §6║",
                "§6║                                                        ║",
                "§6║              §bPremium Authentication Plugin           §6║",
                "§6║                  §7Version: §f1.0-SNAPSHOT              §6║",
                "§6║                   §7Author: §fPowerSan                  §6║",
                "§6║                                                        ║",
                "§6╚════════════════════════════════════════════════════════╝",
                ""
        };

        for (String line : banner) {
            // Use Bukkit's color code system
            getServer().getConsoleSender().sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('§', line));
        }
    }

    public static PowerAuth getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public com.pawanpoudel.powerauth.managers.LimboManager getLimboManager() {
        return limboManager;
    }
}
