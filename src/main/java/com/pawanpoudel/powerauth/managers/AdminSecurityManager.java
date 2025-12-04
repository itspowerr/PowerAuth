package com.pawanpoudel.powerauth.managers;

import com.pawanpoudel.powerauth.PowerAuth;
import com.pawanpoudel.powerauth.utils.DiscordWebhook;

import java.util.List;

public class AdminSecurityManager {

    private final PowerAuth plugin;
    private final DiscordWebhook webhook;
    private final boolean enabled;
    private final List<String> monitoredAccounts;
    private final boolean notifyFailedLogin;
    private final boolean notifySuccessfulLogin;
    private final boolean pingOnFailedLogin;
    private final List<String> pingMentions;

    public AdminSecurityManager(PowerAuth plugin) {
        this.plugin = plugin;

        this.enabled = plugin.getConfig().getBoolean("security.admin-protection.enabled", false);
        this.monitoredAccounts = plugin.getConfig().getStringList("security.admin-protection.monitored-accounts");
        this.notifyFailedLogin = plugin.getConfig().getBoolean("security.admin-protection.notify-on.failed-login",
                true);
        this.notifySuccessfulLogin = plugin.getConfig()
                .getBoolean("security.admin-protection.notify-on.successful-login", false);
        this.pingOnFailedLogin = plugin.getConfig()
                .getBoolean("security.admin-protection.discord-webhook.ping-on-failed-login.enabled", false);
        this.pingMentions = plugin.getConfig()
                .getStringList("security.admin-protection.discord-webhook.ping-on-failed-login.mentions");

        String webhookUrl = plugin.getConfig().getString("security.admin-protection.discord-webhook.url", "");
        boolean webhookEnabled = plugin.getConfig().getBoolean("security.admin-protection.discord-webhook.enabled",
                false);

        this.webhook = webhookEnabled ? new DiscordWebhook(webhookUrl) : null;
    }

    /**
     * Check if a username is being monitored
     */
    public boolean isMonitored(String username) {
        if (!enabled)
            return false;

        for (String monitored : monitoredAccounts) {
            if (monitored.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Log and notify failed login attempt
     */
    public void logFailedLogin(String username, String ipAddress, String reason) {
        if (!enabled || !isMonitored(username))
            return;

        // Console log
        plugin.getLogger().warning("[ADMIN SECURITY] Failed login attempt for admin account: " + username +
                " from IP: " + ipAddress + " - Reason: " + reason);

        // Discord notification
        if (webhook != null && notifyFailedLogin) {
            String mentions = "";

            // Build mentions string if pinging is enabled
            if (pingOnFailedLogin && pingMentions != null && !pingMentions.isEmpty()) {
                mentions = String.join(" ", pingMentions);
            }

            webhook.sendFailedLoginAlert(username, ipAddress, reason, mentions);
        }
    }

    /**
     * Log and notify successful login
     */
    public void logSuccessfulLogin(String username, String ipAddress) {
        if (!enabled || !isMonitored(username))
            return;

        // Console log
        plugin.getLogger().info("[ADMIN SECURITY] Successful login for admin account: " + username +
                " from IP: " + ipAddress);

        // Discord notification
        if (webhook != null && notifySuccessfulLogin) {
            webhook.sendSuccessfulLoginAlert(username, ipAddress);
        }
    }

    /**
     * Log password change
     */
    public void logPasswordChange(String username, String changedBy) {
        if (!enabled || !isMonitored(username))
            return;

        // Console log
        plugin.getLogger().warning("[ADMIN SECURITY] Password changed for admin account: " + username +
                " by: " + changedBy);

        // Discord notification
        if (webhook != null) {
            webhook.sendPasswordChangeAlert(username, changedBy);
        }
    }
}
