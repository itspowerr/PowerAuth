package com.pawanpoudel.powerauth.commands;

import com.pawanpoudel.powerauth.PowerAuth;
import com.pawanpoudel.powerauth.managers.SessionManager;
import com.pawanpoudel.powerauth.utils.PasswordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuthCommand implements CommandExecutor {

    private final PowerAuth plugin;
    private final SessionManager sessionManager;

    public AuthCommand(PowerAuth plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("register")) {
            if (sessionManager.isLoggedIn(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are already logged in!");
                return true;
            }

            if (plugin.getDatabaseManager().isRegistered(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are already registered! Use /login.");
                return true;
            }

            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /register <password> <confirmPassword>");
                return true;
            }

            if (!args[0].equals(args[1])) {
                player.sendMessage(ChatColor.RED + "Passwords do not match!");
                return true;
            }

            if (args[0].length() < 6) {
                player.sendMessage(ChatColor.RED + "Password must be at least 6 characters long!");
                return true;
            }

            String hash = PasswordUtils.hash(args[0]);
            plugin.getDatabaseManager().registerPlayer(player.getUniqueId(), player.getName(), hash,
                    player.getAddress().getAddress().getHostAddress());
            sessionManager.login(player.getUniqueId());
            plugin.getLimboManager().sendToMainWorld(player);
            player.sendMessage(ChatColor.GREEN + "Successfully registered and logged in!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("login")) {
            if (sessionManager.isLoggedIn(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are already logged in!");
                return true;
            }

            if (!plugin.getDatabaseManager().isRegistered(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You are not registered! Use /register.");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "Usage: /login <password>");
                return true;
            }

            String storedHash = plugin.getDatabaseManager().getPasswordHash(player.getUniqueId());
            if (PasswordUtils.check(args[0], storedHash)) {
                sessionManager.login(player.getUniqueId());
                plugin.getDatabaseManager().updateIp(player.getUniqueId(),
                        player.getAddress().getAddress().getHostAddress());
                plugin.getLimboManager().sendToMainWorld(player);
                player.sendMessage(ChatColor.GREEN + "Successfully logged in!");
            } else {
                player.sendMessage(ChatColor.RED + "Incorrect password!");
            }
            return true;
        }

        return false;
    }
}
