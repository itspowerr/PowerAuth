package com.pawanpoudel.powerauth.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class DiscordWebhook {

    private final String webhookUrl;

    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    /**
     * Send a Discord embed notification
     */
    public void sendEmbed(String title, String description, int color, String username, String ipAddress) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            return; // Webhook not configured
        }

        try {
            String timestamp = Instant.now().toString();

            String json = String.format(
                    "{\"embeds\": [{" +
                            "\"title\": \"%s\"," +
                            "\"description\": \"%s\"," +
                            "\"color\": %d," +
                            "\"fields\": [" +
                            "{\"name\": \"Username\", \"value\": \"%s\", \"inline\": true}," +
                            "{\"name\": \"IP Address\", \"value\": \"%s\", \"inline\": true}" +
                            "]," +
                            "\"timestamp\": \"%s\"," +
                            "\"footer\": {\"text\": \"PowerAuth Security\"}" +
                            "}]}",
                    escapeJson(title),
                    escapeJson(description),
                    color,
                    escapeJson(username),
                    escapeJson(ipAddress),
                    timestamp);

            sendWebhook(json);
        } catch (Exception e) {
            // Silently fail - don't crash the plugin if Discord is down
            System.err.println("Failed to send Discord webhook: " + e.getMessage());
        }
    }

    /**
     * Send failed login alert
     */
    public void sendFailedLoginAlert(String username, String ipAddress, String reason, String mentions) {
        String description = "**Event:** Failed Login Attempt\\n**Reason:** " + reason;

        // Add mentions if provided
        if (mentions != null && !mentions.isEmpty()) {
            description = mentions + "\\n\\n" + description;
        }

        sendEmbed(
                "🚨 Admin Account Alert",
                description,
                0xFF0000, // Red
                username,
                ipAddress);
    }

    /**
     * Send successful login notification
     */
    public void sendSuccessfulLoginAlert(String username, String ipAddress) {
        sendEmbed(
                "✅ Admin Login",
                "**Event:** Successful Login",
                0x00FF00, // Green
                username,
                ipAddress);
    }

    /**
     * Send password change notification
     */
    public void sendPasswordChangeAlert(String username, String changedBy) {
        sendEmbed(
                "🔄 Password Changed",
                "**Event:** Password Changed\\n**Changed By:** " + changedBy,
                0xFFA500, // Orange
                username,
                "N/A");
    }

    private void sendWebhook(String json) throws Exception {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "PowerAuth-Webhook");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("HTTP " + responseCode);
        }
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
