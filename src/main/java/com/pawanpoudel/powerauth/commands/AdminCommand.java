package com.pawanpoudel.powerauth.commands;

import com.pawanpoudel.powerauth.PowerAuth;
import com.pawanpoudel.powerauth.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AdminCommand implements CommandExecutor {

    private final PowerAuth plugin;

    public AdminCommand(PowerAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "forcelogin":
                return handleForceLogin(sender, args);
            case "changepassword":
            case "changepass":
                return handleChangePassword(sender, args);
            case "unregister":
            case "delete":
                return handleUnregister(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleForceLogin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powerauth.admin.forcelogin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerauth forcelogin <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }

        UUID uuid = target.getUniqueId();

        // Check if player is registered
        if (!plugin.getDatabaseManager().isRegistered(uuid)) {
            sender.sendMessage(ChatColor.RED + "Player is not registered!");
            return true;
        }

        // Check if player is premium
        if (plugin.getDatabaseManager().isPremium(uuid)) {
            sender.sendMessage(ChatColor.RED + "Cannot force login premium players!");
            return true;
        }

        // Force login
        plugin.getSessionManager().login(uuid);
        plugin.getLimboManager().sendToMainWorld(target);

        target.sendMessage(ChatColor.GREEN + "You have been logged in by an administrator.");
        sender.sendMessage(ChatColor.GREEN + "Successfully force-logged in " + target.getName());

        // Audit log
        plugin.getLogger().info("[ADMIN] " + sender.getName() + " force-logged in " + target.getName());

        return true;
    }

    private boolean handleChangePassword(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powerauth.admin.changepassword")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerauth changepassword <player> <newpassword>");
            return true;
        }

        String playerName = args[1];
        String newPassword = args[2];

        // Validate password length
        int minLength = plugin.getConfig().getInt("authentication.offline.min-password-length", 6);
        int maxLength = plugin.getConfig().getInt("authentication.offline.max-password-length", 32);

        if (newPassword.length() < minLength) {
            sender.sendMessage(ChatColor.RED + "Password must be at least " + minLength + " characters long!");
            return true;
        }

        if (newPassword.length() > maxLength) {
            sender.sendMessage(ChatColor.RED + "Password must be at most " + maxLength + " characters long!");
            return true;
        }

        // Get player UUID (online or offline)
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null ? onlinePlayer.getUniqueId()
                : Bukkit.getOfflinePlayer(playerName).getUniqueId();

        // Check if player is registered
        if (!plugin.getDatabaseManager().isRegistered(uuid)) {
            sender.sendMessage(ChatColor.RED + "Player is not registered!");
            return true;
        }

        // Check if player is premium
        if (plugin.getDatabaseManager().isPremium(uuid)) {
            sender.sendMessage(ChatColor.RED + "Cannot change password for premium players!");
            return true;
        }

        // Change password
        String newHash = PasswordUtils.hash(newPassword);
        plugin.getDatabaseManager().changePassword(uuid, newHash);

        sender.sendMessage(ChatColor.GREEN + "Successfully changed password for " + playerName);

        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(ChatColor.YELLOW + "Your password has been changed by an administrator.");
        }

        // Audit log
        plugin.getLogger().info("[ADMIN] " + sender.getName() + " changed password for " + playerName);

        return true;
    }

    private boolean handleUnregister(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powerauth.admin.unregister")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerauth unregister <player>");
            return true;
        }

        String playerName = args[1];

        // Get player UUID (online or offline)
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null ? onlinePlayer.getUniqueId()
                : Bukkit.getOfflinePlayer(playerName).getUniqueId();

        // Check if player is registered
        if (!plugin.getDatabaseManager().isRegistered(uuid)) {
            sender.sendMessage(ChatColor.RED + "Player is not registered!");
            return true;
        }

        // Check if player is premium
        if (plugin.getDatabaseManager().isPremium(uuid)) {
            sender.sendMessage(ChatColor.RED + "Cannot unregister premium players!");
            return true;
        }

        // Unregister player
        plugin.getDatabaseManager().unregisterPlayer(uuid);
        plugin.getSessionManager().logout(uuid);

        sender.sendMessage(ChatColor.GREEN + "Successfully unregistered " + playerName);

        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(ChatColor.RED + "Your account has been deleted by an administrator.");
            onlinePlayer.sendMessage(ChatColor.YELLOW + "Please register again using /register <password> <password>");
            plugin.getLimboManager().sendToLimbo(onlinePlayer);
        }

        // Audit log
        plugin.getLogger().info("[ADMIN] " + sender.getName() + " unregistered " + playerName);

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("powerauth.admin.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /powerauth info <player>");
            return true;
        }

        String playerName = args[1];

        // Get player UUID (online or offline)
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        UUID uuid = onlinePlayer != null ? onlinePlayer.getUniqueId()
                : Bukkit.getOfflinePlayer(playerName).getUniqueId();

        // Check if player is registered
        if (!plugin.getDatabaseManager().isRegistered(uuid)) {
            sender.sendMessage(ChatColor.RED + "Player is not registered!");
            return true;
        }

        // Get player info
        boolean isPremium = plugin.getDatabaseManager().isPremium(uuid);
        String lastIp = plugin.getDatabaseManager().getLastIp(uuid);
        boolean isLoggedIn = plugin.getSessionManager().isLoggedIn(uuid);

        // Display info
        sender.sendMessage(ChatColor.GOLD + "===== Player Info: " + playerName + " =====");
        sender.sendMessage(ChatColor.YELLOW + "UUID: " + ChatColor.WHITE + uuid.toString());
        sender.sendMessage(ChatColor.YELLOW + "Registered: " + ChatColor.GREEN + "Yes");
        sender.sendMessage(
                ChatColor.YELLOW + "Premium: " + (isPremium ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.YELLOW + "Last IP: " + ChatColor.WHITE + (lastIp != null ? lastIp : "N/A"));
        sender.sendMessage(ChatColor.YELLOW + "Currently Logged In: "
                + (isLoggedIn ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.GOLD + "================================");

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("powerauth.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "PowerAuth configuration reloaded!");

        // Audit log
        plugin.getLogger().info("[ADMIN] " + sender.getName() + " reloaded the configuration");

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== PowerAuth Admin Commands =====");

        if (sender.hasPermission("powerauth.admin.forcelogin")) {
            sender.sendMessage(
                    ChatColor.YELLOW + "/pa forcelogin <player>" + ChatColor.GRAY + " - Force login a player");
        }

        if (sender.hasPermission("powerauth.admin.changepassword")) {
            sender.sendMessage(
                    ChatColor.YELLOW + "/pa changepassword <player> <newpass>" + ChatColor.GRAY + " - Change password");
        }

        if (sender.hasPermission("powerauth.admin.unregister")) {
            sender.sendMessage(ChatColor.YELLOW + "/pa unregister <player>" + ChatColor.GRAY + " - Delete account");
        }

        if (sender.hasPermission("powerauth.admin.info")) {
            sender.sendMessage(ChatColor.YELLOW + "/pa info <player>" + ChatColor.GRAY + " - View account info");
        }

        if (sender.hasPermission("powerauth.admin.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/pa reload" + ChatColor.GRAY + " - Reload configuration");
        }

        sender.sendMessage(ChatColor.GOLD + "====================================");
    }
}
